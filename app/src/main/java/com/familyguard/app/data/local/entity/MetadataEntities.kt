package com.familyguard.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage_stats")
data class AppUsageStatsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val usageTimeMinutes: Long,
    val lastUsedTimestamp: Long,
    val launchCount: Int,
    val collectedAt: Long,
    val childDeviceId: String
)

@Entity(tableName = "notification_counts")
data class NotificationCountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val notificationCount: Int,
    val collectedAt: Long,
    val childDeviceId: String,
    val hourOfDay: Int,
    val dayOfWeek: Int
)

@Entity(tableName = "anomaly_alerts")
data class AnomalyAlertEntity(
    @PrimaryKey val alertId: String,
    val childDeviceId: String,
    val anomalyType: String,
    val severity: String,
    val packageName: String,
    val appName: String,
    val description: String,
    val metadata: String,
    val detectedAt: Long,
    val acknowledgedBy: String?,
    val acknowledgedAt: Long?,
    val isAcknowledged: Boolean = false
)

@Entity(tableName = "usage_baseline")
data class UsageBaselineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val childDeviceId: String,
    val avgDailyUsageMinutes: Long,
    val avgHourlyNotifications: Float,
    val peakUsageHour: Int,
    val baselinePeriodDays: Int,
    val lastUpdated: Long
)

@Entity(tableName = "communication_metadata")
data class CommunicationMetadataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val hourlyNotificationCount: Int,
    val hourlyUsageMinutes: Long,
    val hourOfDay: Int,
    val dayOfWeek: Int,
    val collectedAt: Long,
    val childDeviceId: String
)

@Entity(tableName = "call_log_metadata")
data class CallLogMetadataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val durationSeconds: Long,
    val callType: String,
    val collectedAt: Long,
    val hourOfDay: Int,
    val dayOfWeek: Int,
    val childDeviceId: String
)

/**
 * Per-notification communication metadata captured by the notification listener.
 *
 * Privacy: only metadata is stored. [snippet] is a length-capped (<=40 chars)
 * preview, only populated when the DataValidator allows it. No full message
 * content, contact names, phone numbers, or media bytes are ever stored.
 */
@Entity(tableName = "communication_events")
data class CommunicationEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val eventCategory: String,
    val hasMedia: Boolean,
    val snippet: String?,
    val hourOfDay: Int,
    val dayOfWeek: Int,
    val collectedAt: Long,
    val childDeviceId: String
)
