package com.familyguard.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.familyguard.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageStatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageStats(stats: AppUsageStatsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllUsageStats(stats: List<AppUsageStatsEntity>)

    @Query("SELECT * FROM app_usage_stats WHERE childDeviceId = :childId AND collectedAt > :since ORDER BY collectedAt DESC")
    suspend fun getUsageStatsSince(childId: String, since: Long): List<AppUsageStatsEntity>

    @Query("SELECT * FROM app_usage_stats WHERE childDeviceId = :childId AND packageName = :packageName AND collectedAt > :since ORDER BY collectedAt DESC")
    suspend fun getUsageStatsForApp(childId: String, packageName: String, since: Long): List<AppUsageStatsEntity>

    @Query("SELECT SUM(usageTimeMinutes) FROM app_usage_stats WHERE childDeviceId = :childId AND packageName = :packageName AND collectedAt > :since")
    suspend fun getTotalUsageForApp(childId: String, packageName: String, since: Long): Long?

    @Query("DELETE FROM app_usage_stats WHERE collectedAt < :before")
    suspend fun deleteOldStats(before: Long)
}

@Dao
interface NotificationCountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationCount(count: NotificationCountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNotificationCounts(counts: List<NotificationCountEntity>)

    @Query("SELECT * FROM notification_counts WHERE childDeviceId = :childId AND collectedAt > :since ORDER BY collectedAt DESC")
    suspend fun getNotificationCountsSince(childId: String, since: Long): List<NotificationCountEntity>

    @Query("SELECT * FROM notification_counts WHERE childDeviceId = :childId AND packageName = :packageName AND collectedAt > :since ORDER BY collectedAt DESC")
    suspend fun getNotificationCountsForApp(childId: String, packageName: String, since: Long): List<NotificationCountEntity>

    @Query("SELECT SUM(notificationCount) FROM notification_counts WHERE childDeviceId = :childId AND packageName = :packageName AND collectedAt > :since")
    suspend fun getTotalNotificationsForApp(childId: String, packageName: String, since: Long): Int?

    @Query("DELETE FROM notification_counts WHERE collectedAt < :before")
    suspend fun deleteOldCounts(before: Long)
}

@Dao
interface AnomalyAlertDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnomalyAlert(alert: AnomalyAlertEntity)

    @Query("SELECT * FROM anomaly_alerts WHERE childDeviceId = :childId ORDER BY detectedAt DESC")
    suspend fun getAlertsForChild(childId: String): List<AnomalyAlertEntity>

    @Query("SELECT * FROM anomaly_alerts WHERE childDeviceId = :childId AND isAcknowledged = 0 ORDER BY detectedAt DESC")
    suspend fun getUnacknowledgedAlerts(childId: String): List<AnomalyAlertEntity>

    @Query("UPDATE anomaly_alerts SET isAcknowledged = 1, acknowledgedBy = :by, acknowledgedAt = :at WHERE alertId = :alertId")
    suspend fun acknowledgeAlert(alertId: String, by: String, at: Long)

    @Query("SELECT * FROM anomaly_alerts ORDER BY detectedAt DESC LIMIT :limit")
    suspend fun getRecentAlerts(limit: Int): List<AnomalyAlertEntity>

    @Query("DELETE FROM anomaly_alerts WHERE detectedAt < :before")
    suspend fun deleteOldAlerts(before: Long)
}

@Dao
interface UsageBaselineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBaseline(baseline: UsageBaselineEntity)

    @Query("SELECT * FROM usage_baseline WHERE childDeviceId = :childId AND packageName = :packageName")
    suspend fun getBaseline(childId: String, packageName: String): UsageBaselineEntity?

    @Query("SELECT * FROM usage_baseline WHERE childDeviceId = :childId")
    suspend fun getAllBaselines(childId: String): List<UsageBaselineEntity>

    @Query("DELETE FROM usage_baseline WHERE childDeviceId = :childId")
    suspend fun deleteBaselines(childId: String)
}

@Dao
interface CommunicationMetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: CommunicationMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMetadata(metadata: List<CommunicationMetadataEntity>)

    @Query("SELECT * FROM communication_metadata WHERE childDeviceId = :childId AND collectedAt > :since ORDER BY collectedAt DESC")
    suspend fun getMetadataSince(childId: String, since: Long): List<CommunicationMetadataEntity>

    @Query("SELECT * FROM communication_metadata WHERE childDeviceId = :childId AND packageName = :packageName AND collectedAt > :since ORDER BY collectedAt DESC")
    suspend fun getMetadataForApp(childId: String, packageName: String, since: Long): List<CommunicationMetadataEntity>

    @Query("DELETE FROM communication_metadata WHERE collectedAt < :before")
    suspend fun deleteOldMetadata(before: Long)
}
