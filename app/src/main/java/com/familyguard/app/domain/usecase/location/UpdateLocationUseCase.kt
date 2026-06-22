package com.familyguard.app.domain.usecase.location

import com.familyguard.app.data.local.dao.LocationDao
import com.familyguard.app.data.local.dao.SafeZoneDao
import com.familyguard.app.data.local.dao.SyncQueueDao
import com.familyguard.app.data.local.entity.LocationSnapshotEntity
import com.familyguard.app.data.local.entity.SyncQueueEntity
import com.familyguard.app.domain.model.LocationSync
import com.familyguard.app.security.AuditAction
import com.familyguard.app.security.AuditLogger
import com.familyguard.app.security.Actor
import com.familyguard.app.security.E2EEncryptionManager
import com.google.gson.Gson
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToInt

class UpdateLocationUseCase @Inject constructor(
    private val locationDao: LocationDao,
    private val safeZoneDao: SafeZoneDao,
    private val syncQueueDao: SyncQueueDao,
    private val encryptionManager: E2EEncryptionManager,
    private val auditLogger: AuditLogger
) {
    companion object {
        /**
         * Number of decimal places used when rounding coordinates for the
         * transmitted (encrypted) payload. 3 dp ≈ 111 m precision.
         * Kept as a named constant so the minimization level is auditable.
         */
        const val LOCATION_DECIMAL_PLACES = 3

        /** Defense-in-depth accuracy gate (meters). Matches the service-level gate. */
        const val MAX_ACCEPTABLE_ACCURACY_M = 100f

        /**
         * Minimum time between accepted location fixes in milliseconds.
         * Prevents burst double-fixes from the FusedLocationProvider.
         */
        const val MIN_LOCATION_INTERVAL_MS = 10_000L
    }

    /** Timestamp of the last location that was accepted and persisted. */
    @Volatile
    private var lastAcceptedTimestamp: Long = 0L

    private val gson = Gson()

    suspend operator fun invoke(
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        altitude: Double? = null,
        speed: Float? = null,
        batteryLevel: Int = 0,
        deviceId: String
    ): Result<LocationSync> {
        return try {
            val now = System.currentTimeMillis()

            // ── Accuracy gate (defense-in-depth) ────────────────────────
            if (accuracy > MAX_ACCEPTABLE_ACCURACY_M) {
                return Result.failure(
                    IllegalArgumentException("Accuracy $accuracy > $MAX_ACCEPTABLE_ACCURACY_M — discarded")
                )
            }

            // ── Minimum-interval throttle ───────────────────────────────
            if (now - lastAcceptedTimestamp < MIN_LOCATION_INTERVAL_MS) {
                return Result.failure(
                    IllegalArgumentException("Fix too close to last accepted fix — throttled")
                )
            }

            // ── Persist raw snapshot to Room (full precision for local use) ──
            val snapshot = LocationSnapshotEntity(
                snapshotId = UUID.randomUUID().toString(),
                deviceId = deviceId,
                latitude = latitude,
                longitude = longitude,
                accuracy = accuracy,
                altitude = altitude,
                speed = speed,
                timestamp = now,
                batteryLevel = batteryLevel
            )
            locationDao.insertLocation(snapshot)
            lastAcceptedTimestamp = now

            // ── Safe-zone proximity check ────────────────────────────────
            val zones = safeZoneDao.getActiveZones()
            var nearestZone: com.familyguard.app.data.local.entity.SafeZoneEntity? = null
            var minDistance = Float.MAX_VALUE

            for (zone in zones) {
                val distance = distanceTo(
                    latitude, longitude,
                    zone.centerLatitude, zone.centerLongitude
                )
                if (distance < minDistance) {
                    minDistance = distance
                    nearestZone = zone
                }
            }

            val isInSafeZone = nearestZone != null && minDistance <= nearestZone.radiusMeters

            // ── Data minimization: coarsen coordinates for transmission ──
            val factor = 10.0.pow(LOCATION_DECIMAL_PLACES.toDouble())
            val reducedLat = (latitude * factor).roundToInt() / factor
            val reducedLng = (longitude * factor).roundToInt() / factor

            val sync = LocationSync(
                deviceId = deviceId,
                latitude = reducedLat,
                longitude = reducedLng,
                accuracy = accuracy,
                timestamp = java.time.Instant.ofEpochMilli(now),
                isInSafeZone = isInSafeZone,
                nearestSafeZoneName = nearestZone?.name
            )

            // ── Encrypt & enqueue for network transmission ──────────────
            val json = gson.toJson(sync)
            val encrypted = encryptionManager.encrypt(json.toByteArray(Charsets.UTF_8))
            val transmitString = encrypted.toTransmitFormat()

            syncQueueDao.insertItem(
                SyncQueueEntity(
                    id = UUID.randomUUID().toString(),
                    dataType = "location",
                    payload = transmitString,
                    createdAt = now,
                    attempts = 0,
                    lastAttemptAt = null,
                    synced = false
                )
            )

            // ── Audit logging (reduced coordinates only) ────────────────
            auditLogger.log(
                action = AuditAction.LOCATION_ACCESSED,
                actor = Actor.SYSTEM,
                details = "Location updated: ($reducedLat, $reducedLng), inSafeZone=$isInSafeZone"
            )
            auditLogger.log(
                action = AuditAction.LOCATION_SYNCED,
                actor = Actor.SYSTEM,
                details = "Encrypted location payload enqueued for sync"
            )

            Result.success(sync)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun distanceTo(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
}
