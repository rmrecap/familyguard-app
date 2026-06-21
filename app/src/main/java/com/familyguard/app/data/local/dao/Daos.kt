package com.familyguard.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.familyguard.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationSnapshotEntity)

    @Query("SELECT * FROM location_snapshots WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestLocation(deviceId: String): LocationSnapshotEntity?

    @Query("SELECT * FROM location_snapshots WHERE deviceId = :deviceId AND timestamp > :since ORDER BY timestamp DESC")
    suspend fun getLocationHistory(deviceId: String, since: Long): List<LocationSnapshotEntity>

    @Query("SELECT * FROM location_snapshots WHERE synced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedLocations(): List<LocationSnapshotEntity>

    @Query("UPDATE location_snapshots SET synced = 1 WHERE snapshotId IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)

    @Query("DELETE FROM location_snapshots WHERE timestamp < :before")
    suspend fun deleteOldLocations(before: Long)
}

@Dao
interface SafeZoneDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSafeZone(zone: SafeZoneEntity)

    @Query("SELECT * FROM safe_zones WHERE isActive = 1")
    suspend fun getActiveZones(): List<SafeZoneEntity>

    @Query("SELECT * FROM safe_zones WHERE zoneId = :zoneId")
    suspend fun getZoneById(zoneId: String): SafeZoneEntity?

    @Query("DELETE FROM safe_zones WHERE zoneId = :zoneId")
    suspend fun deleteZone(zoneId: String)

    @Query("SELECT * FROM safe_zones")
    fun getAllZones(): Flow<List<SafeZoneEntity>>
}

@Dao
interface SosDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: SosAlertEntity)

    @Query("SELECT * FROM sos_alerts WHERE childDeviceId = :childId ORDER BY triggeredAt DESC")
    suspend fun getAlertsForChild(childId: String): List<SosAlertEntity>

    @Query("SELECT * FROM sos_alerts WHERE status = 'TRIGGERED' ORDER BY triggeredAt DESC")
    suspend fun getActiveAlerts(): List<SosAlertEntity>

    @Query("UPDATE sos_alerts SET status = :status, acknowledgedBy = :by, acknowledgedAt = :at WHERE alertId = :alertId")
    suspend fun updateAlertStatus(alertId: String, status: String, by: String?, at: Long?)
}

@Dao
interface ConsentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsent(consent: ConsentRecordEntity)

    @Query("SELECT * FROM consent_records WHERE childDeviceId = :childId AND isActive = 1")
    suspend fun getActiveConsents(childId: String): List<ConsentRecordEntity>

    @Query("SELECT * FROM consent_records WHERE childDeviceId = :childId AND featureId = :featureId")
    suspend fun getConsentForFeature(childId: String, featureId: String): ConsentRecordEntity?

    @Query("UPDATE consent_records SET isActive = 0, revokedAt = :revokedAt WHERE featureId = :featureId AND childDeviceId = :childId")
    suspend fun revokeConsent(featureId: String, childId: String, revokedAt: Long)

    @Query("SELECT * FROM consent_records WHERE childDeviceId = :childId")
    fun getAllConsents(childId: String): Flow<List<ConsentRecordEntity>>
}

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue WHERE synced = 0 ORDER BY createdAt ASC")
    suspend fun getPendingItems(): List<SyncQueueEntity>

    @Query("UPDATE sync_queue SET synced = 1, attempts = attempts + 1, lastAttemptAt = :now WHERE id = :id")
    suspend fun markAsSynced(id: String, now: Long)

    @Query("DELETE FROM sync_queue WHERE synced = 1")
    suspend fun deleteSyncedItems()

    @Query("SELECT COUNT(*) FROM sync_queue WHERE synced = 0")
    suspend fun getPendingCount(): Int
}

@Dao
interface DeviceProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceProfileEntity)

    @Query("SELECT * FROM device_profiles WHERE deviceId = :deviceId")
    suspend fun getDevice(deviceId: String): DeviceProfileEntity?

    @Query("SELECT * FROM device_profiles WHERE role = :role")
    suspend fun getDevicesByRole(role: String): List<DeviceProfileEntity>

    @Query("UPDATE device_profiles SET lastSeenAt = :now WHERE deviceId = :deviceId")
    suspend fun updateLastSeen(deviceId: String, now: Long)

    @Query("SELECT * FROM device_profiles")
    fun getAllDevices(): Flow<List<DeviceProfileEntity>>
}

@Dao
interface FamilyGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyGroup(group: FamilyGroupEntity)

    @Query("SELECT * FROM family_groups WHERE groupId = :groupId")
    suspend fun getFamilyGroup(groupId: String): FamilyGroupEntity?

    @Query("SELECT * FROM family_groups")
    fun getAllFamilyGroups(): Flow<List<FamilyGroupEntity>>
}
