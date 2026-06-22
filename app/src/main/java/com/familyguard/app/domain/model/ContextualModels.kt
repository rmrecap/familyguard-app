package com.familyguard.app.domain.model

data class ContextualStateReport(
    val childDeviceId: String,
    val childName: String,
    val timestamp: Long,
    val currentForegroundApp: AppContext?,
    val recentAppActivity: List<AppContext>,
    val notificationSummary: NotificationSummary,
    val usageSummary: UsageSummary,
    val dailyInsights: DailyInsights,
    val callLogSummary: CallLogSummary? = null,
    val communicationSummary: CommunicationSummary? = null
)

data class CallLogSummary(
    val totalCallsToday: Int,
    val callsLastHour: Int,
    val totalDurationSecondsToday: Long,
    val durationSecondsLastHour: Long
)

data class CommunicationSummary(
    val totalEventsToday: Int,
    val eventsLastHour: Int,
    val mediaEventsLastHour: Int,
    val topAppsByEvents: List<AppEventCount>,
    val communicationTrend: TrendDirection
)

data class AppEventCount(
    val packageName: String,
    val appName: String,
    val eventCount: Int,
    val mediaCount: Int
)

data class AppContext(
    val packageName: String,
    val appName: String,
    val isCurrentlyActive: Boolean,
    val usageMinutesToday: Long,
    val usageMinutesLastHour: Long,
    val notificationCountLastHour: Int,
    val lastUsedTimestamp: Long
)

data class NotificationSummary(
    val totalNotificationsToday: Int,
    val notificationsLastHour: Int,
    val topAppsByNotifications: List<AppNotificationCount>,
    val notificationTrend: TrendDirection
)

data class AppNotificationCount(
    val packageName: String,
    val appName: String,
    val count: Int
)

data class UsageSummary(
    val totalScreenTimeToday: Long,
    val screenTimeLastHour: Long,
    val topAppsByUsage: List<AppUsageCount>,
    val usageTrend: TrendDirection
)

data class AppUsageCount(
    val packageName: String,
    val appName: String,
    val minutes: Long
)

data class DailyInsights(
    val mostUsedApp: String?,
    val peakActivityHour: Int,
    val totalAppsUsed: Int,
    val averageSessionLength: Long,
    val earlyMorningActivity: Boolean,
    val lateNightActivity: Boolean
)

enum class TrendDirection {
    INCREASING,
    STABLE,
    DECREASING,
    SPIKE,
    UNUSUAL
}

data class ContextualAlert(
    val alertId: String,
    val childDeviceId: String,
    val alertType: ContextualAlertType,
    val severity: AlertSeverity,
    val title: String,
    val description: String,
    val appContext: AppContext?,
    val detectedAt: Long,
    val isAcknowledged: Boolean
)

enum class ContextualAlertType {
    APP_USAGE_SPIKE,
    NOTIFICATION_FLOOD,
    UNUSUAL_HOUR_ACTIVITY,
    MULTIPLE_APP_HIGH_ACTIVITY,
    SUSTAINED_INTENSIVE_USE,
    NEW_APP_INSTALLED
}

enum class AlertSeverity {
    INFO,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
