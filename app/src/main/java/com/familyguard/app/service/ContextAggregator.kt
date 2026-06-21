package com.familyguard.app.service

import android.util.Log
import com.familyguard.app.data.local.PreferencesManager
import com.familyguard.app.data.local.dao.*
import com.familyguard.app.domain.model.*
import com.familyguard.app.security.DataValidator
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextAggregator @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val appUsageStatsDao: AppUsageStatsDao,
    private val notificationCountDao: NotificationCountDao,
    private val deviceProfileDao: DeviceProfileDao,
    private val callLogMetadataDao: CallLogMetadataDao,
    private val dataValidator: DataValidator
) {
    companion object {
        private const val TAG = "ContextAggregator"
        
        private val MONITORED_APPS = mapOf(
            "com.whatsapp" to "WhatsApp",
            "org.telegram.messenger" to "Telegram",
            "com.facebook.messenger" to "Messenger",
            "com.instagram.android" to "Instagram",
            "com.twitter.android" to "Twitter",
            "com.snapchat.android" to "Snapchat",
            "com.discord" to "Discord",
            "com.zhiliaoapp.musically" to "TikTok",
            "com.google.android.gm" to "Gmail",
            "com.google.android.apps.messaging" to "Messages"
        )
    }

    suspend fun generateContextualReport(childDeviceId: String): ContextualStateReport {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - (60 * 60 * 1000)
        val startOfDay = getStartOfDay()

        // Get child device info
        val deviceProfile = deviceProfileDao.getDevice(childDeviceId)
        val childName = deviceProfile?.deviceName ?: "Child"

        // Get foreground app context
        val currentForegroundApp = getCurrentForegroundApp(childDeviceId, now, oneHourAgo)

        // Get recent app activity
        val recentActivity = getRecentAppActivity(childDeviceId, oneHourAgo)

        // Get notification summary
        val notificationSummary = getNotificationSummary(childDeviceId, startOfDay, oneHourAgo)

        // Get usage summary
        val usageSummary = getUsageSummary(childDeviceId, startOfDay, oneHourAgo)

        // Get daily insights
        val dailyInsights = getDailyInsights(childDeviceId, startOfDay)

        // Get call log summary
        val callLogSummary = getCallLogSummary(childDeviceId, startOfDay, oneHourAgo)
 
        val report = ContextualStateReport(
            childDeviceId = childDeviceId,
            childName = childName,
            timestamp = now,
            currentForegroundApp = currentForegroundApp,
            recentAppActivity = recentActivity,
            notificationSummary = notificationSummary,
            usageSummary = usageSummary,
            dailyInsights = dailyInsights,
            callLogSummary = callLogSummary
        )

        // Validate the report data before returning
        val validationData = mutableMapOf<String, String>()
        report.currentForegroundApp?.let {
            validationData["packageName"] = it.packageName
            validationData["appName"] = it.appName
        }
        report.notificationSummary?.let {
            validationData["totalNotificationsToday"] = it.totalNotificationsToday.toString()
        }
        report.usageSummary?.let {
            validationData["totalScreenTimeToday"] = it.totalScreenTimeToday.toString()
        }
        report.dailyInsights?.let {
            validationData["mostUsedApp"] = it.mostUsedApp ?: ""
        }
        report.callLogSummary?.let {
            validationData["totalCallsToday"] = it.totalCallsToday.toString()
            validationData["callsLastHour"] = it.callsLastHour.toString()
        }
 
        if (validationData.isNotEmpty()) {
            val validationResult = dataValidator.validateData(validationData, TAG)
            if (validationResult is DataValidator.ValidationResult.Invalid) {
                Log.e(TAG, "Contextual report validation failed: ${validationResult.reason}")
                return ContextualStateReport(
                    childDeviceId = childDeviceId,
                    childName = childName,
                    timestamp = now,
                    currentForegroundApp = null,
                    recentAppActivity = emptyList(),
                    notificationSummary = NotificationSummary(
                        totalNotificationsToday = 0,
                        notificationsLastHour = 0,
                        topAppsByNotifications = emptyList(),
                        notificationTrend = TrendDirection.STABLE
                    ),
                    usageSummary = UsageSummary(
                        totalScreenTimeToday = 0,
                        screenTimeLastHour = 0,
                        topAppsByUsage = emptyList(),
                        usageTrend = TrendDirection.STABLE
                    ),
                    dailyInsights = DailyInsights(
                        mostUsedApp = null,
                        peakActivityHour = 0,
                        totalAppsUsed = 0,
                        averageSessionLength = 0,
                        earlyMorningActivity = false,
                        lateNightActivity = false
                    ),
                    callLogSummary = CallLogSummary(
                        totalCallsToday = 0,
                        callsLastHour = 0,
                        totalDurationSecondsToday = 0,
                        durationSecondsLastHour = 0
                    )
                )
            }
        }

        return report
    }

    private suspend fun getCurrentForegroundApp(childId: String, now: Long, oneHourAgo: Long): AppContext? {
        val recentUsage = appUsageStatsDao.getUsageStatsSince(childId, oneHourAgo)

        val foregroundApp = recentUsage.maxByOrNull { it.lastUsedTimestamp }

        return foregroundApp?.let {
            val totalUsageToday = appUsageStatsDao.getTotalUsageForApp(
                childId, it.packageName, getStartOfDay()
            ) ?: 0

            val notificationsLastHour = notificationCountDao.getTotalNotificationsForApp(
                childId, it.packageName, oneHourAgo
            ) ?: 0

            AppContext(
                packageName = it.packageName,
                appName = it.appName,
                isCurrentlyActive = true,
                usageMinutesToday = totalUsageToday,
                usageMinutesLastHour = it.usageTimeMinutes,
                notificationCountLastHour = notificationsLastHour,
                lastUsedTimestamp = it.lastUsedTimestamp
            )
        }
    }

    private suspend fun getRecentAppActivity(childId: String, oneHourAgo: Long): List<AppContext> {
        val recentUsage = appUsageStatsDao.getUsageStatsSince(childId, oneHourAgo)

        return recentUsage.groupBy { it.packageName }.map { (packageName, stats) ->
            val latestStat = stats.maxByOrNull { it.lastUsedTimestamp } ?: return@map null
            val totalUsage = stats.sumOf { it.usageTimeMinutes }
            val notifications = notificationCountDao.getTotalNotificationsForApp(
                childId, packageName, oneHourAgo
            ) ?: 0

            AppContext(
                packageName = packageName,
                appName = latestStat.appName,
                isCurrentlyActive = false,
                usageMinutesToday = totalUsage,
                usageMinutesLastHour = totalUsage,
                notificationCountLastHour = notifications,
                lastUsedTimestamp = latestStat.lastUsedTimestamp
            )
        }.filterNotNull().sortedByDescending { it.lastUsedTimestamp }
    }

    private suspend fun getNotificationSummary(childId: String, startOfDay: Long, oneHourAgo: Long): NotificationSummary {
        val notificationsToday = notificationCountDao.getNotificationCountsSince(childId, startOfDay)
        val notificationsLastHour = notificationCountDao.getNotificationCountsSince(childId, oneHourAgo)

        val totalToday = notificationsToday.sumOf { it.notificationCount }
        val lastHourCount = notificationsLastHour.sumOf { it.notificationCount }

        val topApps = notificationsToday
            .groupBy { it.packageName }
            .map { (packageName, counts) ->
                AppNotificationCount(
                    packageName = packageName,
                    appName = counts.first().appName,
                    count = counts.sumOf { it.notificationCount }
                )
            }
            .sortedByDescending { it.count }
            .take(5)

        val trend = calculateNotificationTrend(notificationsLastHour)

        return NotificationSummary(
            totalNotificationsToday = totalToday,
            notificationsLastHour = lastHourCount,
            topAppsByNotifications = topApps,
            notificationTrend = trend
        )
    }

    private suspend fun getUsageSummary(childId: String, startOfDay: Long, oneHourAgo: Long): UsageSummary {
        val usageToday = appUsageStatsDao.getUsageStatsSince(childId, startOfDay)
        val usageLastHour = appUsageStatsDao.getUsageStatsSince(childId, oneHourAgo)

        val totalScreenTime = usageToday.sumOf { it.usageTimeMinutes }
        val screenTimeLastHour = usageLastHour.sumOf { it.usageTimeMinutes }

        val topApps = usageToday
            .groupBy { it.packageName }
            .map { (packageName, stats) ->
                AppUsageCount(
                    packageName = packageName,
                    appName = stats.first().appName,
                    minutes = stats.sumOf { it.usageTimeMinutes }
                )
            }
            .sortedByDescending { it.minutes }
            .take(5)

        val trend = calculateUsageTrend(usageLastHour)

        return UsageSummary(
            totalScreenTimeToday = totalScreenTime,
            screenTimeLastHour = screenTimeLastHour,
            topAppsByUsage = topApps,
            usageTrend = trend
        )
    }

    private suspend fun getDailyInsights(childId: String, startOfDay: Long): DailyInsights {
        val usageToday = appUsageStatsDao.getUsageStatsSince(childId, startOfDay)

        val mostUsedApp = usageToday
            .groupBy { it.packageName }
            .maxByOrNull { it.value.sumOf { stat -> stat.usageTimeMinutes } }
            ?.value?.first()?.appName

        val peakHour = usageToday
            .groupBy { getHourFromTimestamp(it.lastUsedTimestamp) }
            .maxByOrNull { it.value.size }
            ?.key ?: 12

        val totalAppsUsed = usageToday.map { it.packageName }.distinct().size

        val avgSessionLength = if (usageToday.isNotEmpty()) {
            usageToday.sumOf { it.usageTimeMinutes } / usageToday.size
        } else 0L

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val earlyMorningActivity = usageToday.any { getHourFromTimestamp(it.lastUsedTimestamp) in 4..6 }
        val lateNightActivity = usageToday.any { getHourFromTimestamp(it.lastUsedTimestamp) in 23..23 || getHourFromTimestamp(it.lastUsedTimestamp) in 0..3 }

        return DailyInsights(
            mostUsedApp = mostUsedApp,
            peakActivityHour = peakHour,
            totalAppsUsed = totalAppsUsed,
            averageSessionLength = avgSessionLength,
            earlyMorningActivity = earlyMorningActivity,
            lateNightActivity = lateNightActivity
        )
    }

    private fun calculateNotificationTrend(recentNotifications: List<com.familyguard.app.data.local.entity.NotificationCountEntity>): TrendDirection {
        if (recentNotifications.size < 2) return TrendDirection.STABLE

        val recentHour = recentNotifications.takeLast(recentNotifications.size / 2)
        val olderHour = recentNotifications.take(recentNotifications.size / 2)

        val recentCount = recentHour.sumOf { it.notificationCount }
        val olderCount = olderHour.sumOf { it.notificationCount }

        return when {
            recentCount > olderCount * 2 -> TrendDirection.SPIKE
            recentCount > olderCount * 1.5 -> TrendDirection.INCREASING
            recentCount < olderCount * 0.5 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
    }

    private fun calculateUsageTrend(recentUsage: List<com.familyguard.app.data.local.entity.AppUsageStatsEntity>): TrendDirection {
        if (recentUsage.size < 2) return TrendDirection.STABLE

        val recentHour = recentUsage.takeLast(recentUsage.size / 2)
        val olderHour = recentUsage.take(recentUsage.size / 2)

        val recentMinutes = recentHour.sumOf { it.usageTimeMinutes }
        val olderMinutes = olderHour.sumOf { it.usageTimeMinutes }

        return when {
            recentMinutes > olderMinutes * 2 -> TrendDirection.SPIKE
            recentMinutes > olderMinutes * 1.5 -> TrendDirection.INCREASING
            recentMinutes < olderMinutes * 0.5 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
    }

    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private suspend fun getCallLogSummary(childId: String, startOfDay: Long, oneHourAgo: Long): CallLogSummary {
        return try {
            val callsToday = callLogMetadataDao.getCallsSince(childId, startOfDay)
            val callsLastHour = callLogMetadataDao.getCallsSince(childId, oneHourAgo)

            val totalCalls = callsToday.size
            val lastHourCalls = callsLastHour.size
            val totalDuration = callsToday.sumOf { it.durationSeconds }
            val lastHourDuration = callsLastHour.sumOf { it.durationSeconds }

            CallLogSummary(
                totalCallsToday = totalCalls,
                callsLastHour = lastHourCalls,
                totalDurationSecondsToday = totalDuration,
                durationSecondsLastHour = lastHourDuration
            )
        } catch (e: Exception) {
            CallLogSummary(0, 0, 0, 0)
        }
    }

    private fun getHourFromTimestamp(timestamp: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.HOUR_OF_DAY)
    }
}
