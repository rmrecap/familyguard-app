package com.familyguard.app.domain.usecase.location

import com.familyguard.app.data.local.dao.LocationDao
import com.familyguard.app.data.local.dao.SafeZoneDao
import com.familyguard.app.data.local.entity.LocationSnapshotEntity
import com.familyguard.app.domain.model.LocationSync
import com.familyguard.app.security.AuditAction
import com.familyguard.app.security.AuditLogger
import com.familyguard.app.security.E2EEncryptionManager
import com.google.gson.Gson
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToInt

class UpdateLocationUseCase @Inject constructor(
    private val locationDao: LocationDao,
    private val safeZoneDao: SafeZoneDao,
    private val encryptionManager: E2EEncryptionManager,
    private val auditLogger: AuditLogger
) {
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
            val snapshot = LocationSnapshotEntity(
                snapshotId = UUID.randomUUID().toString(),
                deviceId = deviceId,
                latitude = latitude,
                longitude = longitude,
                accuracy = accuracy,
                altitude = altitude,
                speed = speed,
                timestamp = System.currentTimeMillis(),
                batteryLevel = batteryLevel
            )
            locationDao.insertLocation(snapshot)

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

            val reducedLat = (latitude * 1000).roundToInt() / 1000.0
            val reducedLng = (longitude * 1000).roundToInt() / 1000.0

            val sync = LocationSync(
                deviceId = deviceId,
                latitude = reducedLat,
                longitude = reducedLng,
                accuracy = accuracy,
                timestamp = java.time.Instant.now(),
                isInSafeZone = isInSafeZone,
                nearestSafeZoneName = nearestZone?.name
            )

            val json = gson.toJson(sync)
            val encrypted = encryptionManager.encrypt(json.toByteArray())

            auditLogger.log(
                action = AuditAction.LOCATION_ACCESSED,
                actor = com.familyguard.app.security.Actor.SYSTEM,
                details = "Location updated: ($reducedLat, $reducedLng), inSafeZone=$isInSafeZone"
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
