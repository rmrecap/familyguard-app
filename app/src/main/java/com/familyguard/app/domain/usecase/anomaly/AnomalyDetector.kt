package com.familyguard.app.domain.usecase.anomaly

import com.familyguard.app.data.local.dao.*
import com.familyguard.app.data.local.entity.*
import com.familyguard.app.domain.model.AnomalyType
import com.familyguard.app.domain.model.Severity
import java.util.*
import javax.inject.Inject

data class AnomalyResult(
    val type: AnomalyType,
    val severity: Severity,
    val packageName: String,
    val appName: String,
    val description: String,
    val metadata: Map<String, Any>
)

class AnomalyDetector @Inject constructor(
    private val appUsageStatsDao: AppUsageStatsDao,
    private val notificationCountDao: NotificationCountDao,
    private val communicationMetadataDao: CommunicationMetadataDao,
    private val usageBaselineDao: UsageBaselineDao,
    private val anomalyAlertDao: AnomalyAlertDao
) {
    companion object {
        // Detection thresholds
        private const val NOTIFICATION_SPIKE_MULTIPLIER = 5.0 // 5x normal
        private const val USAGE_SPIKE_MULTIPLIER = 3.0 // 3x normal
        private const val SUSTAINED_HIGH_NOTIFICATIONS_MINUTES = 60
        private const val UNUSUAL_HOUR_START = 23 // 11 PM
        private const val UNUSUAL_HOUR_END = 5 // 5 AM
        private const val BASELINE_UPDATE_INTERVAL_DAYS = 7
        private const val ANALYSIS_WINDOW_HOURS = 3 // Last 3 hours
    }

    suspend fun analyze(childDeviceId: String): List<AnomalyResult> {
        val anomalies = mutableListOf<AnomalyResult>()

        // Update baseline if needed
        updateBaselineIfNeeded(childDeviceId)

        // Get baselines
        val baselines = usageBaselineDao.getAllBaselines(childDeviceId)

        // Check each monitored app
        val monitoredApps = mapOf(
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
        for ((packageName, appName) in monitoredApps) {
            val baseline = baselines.find { it.packageName == packageName }

            // 1. Notification Spike Detection
            val notificationAnomaly = detectNotificationSpike(childDeviceId, packageName, appName, baseline)
            notificationAnomaly?.let { anomalies.add(it) }

            // 2. Usage Spike Detection
            val usageAnomaly = detectUsageSpike(childDeviceId, packageName, appName, baseline)
            usageAnomaly?.let { anomalies.add(it) }

            // 3. Sustained High Activity
            val sustainedAnomaly = detectSustainedHighActivity(childDeviceId, packageName, appName)
            sustainedAnomaly?.let { anomalies.add(it) }

            // 4. Unusual Hour Activity
            val unusualHourAnomaly = detectUnusualHourActivity(childDeviceId, packageName, appName)
            unusualHourAnomaly?.let { anomalies.add(it) }
        }

        // 5. Cross-App Correlation
        val crossAppAnomaly = detectCrossAppCorrelation(childDeviceId)
        crossAppAnomaly?.let { anomalies.add(it) }

        // Save detected anomalies
        anomalies.forEach { result ->
            val alert = AnomalyAlertEntity(
                alertId = UUID.randomUUID().toString(),
                childDeviceId = childDeviceId,
                anomalyType = result.type.name,
                severity = result.severity.name,
                packageName = result.packageName,
                appName = result.appName,
                description = result.description,
                metadata = result.metadata.toString(),
                detectedAt = System.currentTimeMillis(),
                acknowledgedBy = null,
                acknowledgedAt = null,
                isAcknowledged = false
            )
            anomalyAlertDao.insertAnomalyAlert(alert)
        }

        return anomalies
    }

    private suspend fun detectNotificationSpike(
        childDeviceId: String,
        packageName: String,
        appName: String,
        baseline: UsageBaselineEntity?
    ): AnomalyResult? {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - (60 * 60 * 1000)

        val recentNotifications = notificationCountDao.getTotalNotificationsForApp(
            childId = childDeviceId,
            packageName = packageName,
            since = oneHourAgo
        ) ?: 0

        if (baseline == null || baseline.avgHourlyNotifications == 0f) {
            // No baseline yet, use absolute threshold
            if (recentNotifications > 20) {
                return AnomalyResult(
                    type = AnomalyType.NOTIFICATION_SPIKE,
                    severity = Severity.MEDIUM,
                    packageName = packageName,
                    appName = appName,
                    description = "High notification activity: $recentNotifications notifications in the last hour",
                    metadata = mapOf(
                        "recentCount" to recentNotifications,
                        "threshold" to 20
                    )
                )
            }
            return null
        }

        val multiplier = recentNotifications.toFloat() / baseline.avgHourlyNotifications
        if (multiplier >= NOTIFICATION_SPIKE_MULTIPLIER && recentNotifications > 10) {
            val severity = when {
                multiplier >= 10.0 -> Severity.HIGH
                multiplier >= 7.0 -> Severity.HIGH
                else -> Severity.MEDIUM
            }

            return AnomalyResult(
                type = AnomalyType.NOTIFICATION_SPIKE,
                severity = severity,
                packageName = packageName,
                appName = appName,
                description = "Notification spike detected: ${recentNotifications} notifications (${multiplier.toInt()}x normal) in the last hour",
                metadata = mapOf(
                    "recentCount" to recentNotifications,
                    "baselineAvg" to baseline.avgHourlyNotifications,
                    "multiplier" to multiplier
                )
            )
        }

        return null
    }

    private suspend fun detectUsageSpike(
        childDeviceId: String,
        packageName: String,
        appName: String,
        baseline: UsageBaselineEntity?
    ): AnomalyResult? {
        val now = System.currentTimeMillis()
        val threeHoursAgo = now - (3 * 60 * 60 * 1000)

        val recentUsage = appUsageStatsDao.getTotalUsageForApp(
            childId = childDeviceId,
            packageName = packageName,
            since = threeHoursAgo
        ) ?: 0

        if (baseline == null || baseline.avgDailyUsageMinutes == 0L) {
            if (recentUsage > 90) { // More than 90 minutes in 3 hours
                return AnomalyResult(
                    type = AnomalyType.USAGE_SPIKE,
                    severity = Severity.MEDIUM,
                    packageName = packageName,
                    appName = appName,
                    description = "High app usage: ${recentUsage} minutes in the last 3 hours",
                    metadata = mapOf(
                        "recentUsageMinutes" to recentUsage,
                        "threshold" to 90
                    )
                )
            }
            return null
        }

        val hourlyBaseline = baseline.avgDailyUsageMinutes / 24.0
        val recentHourly = recentUsage / 3.0
        val multiplier = recentHourly / hourlyBaseline

        if (multiplier >= USAGE_SPIKE_MULTIPLIER && recentUsage > 30) {
            val severity = when {
                multiplier >= 5.0 -> Severity.HIGH
                else -> Severity.MEDIUM
            }

            return AnomalyResult(
                type = AnomalyType.USAGE_SPIKE,
                severity = severity,
                packageName = packageName,
                appName = appName,
                description = "Usage spike detected: ${recentUsage} minutes in 3 hours (${multiplier.toInt()}x normal)",
                metadata = mapOf(
                    "recentUsageMinutes" to recentUsage,
                    "baselineHourly" to hourlyBaseline,
                    "multiplier" to multiplier
                )
            )
        }

        return null
    }

    private suspend fun detectSustainedHighActivity(
        childDeviceId: String,
        packageName: String,
        appName: String
    ): AnomalyResult? {
        val now = System.currentTimeMillis()
        val twoHoursAgo = now - (2 * 60 * 60 * 1000)

        val metadata = communicationMetadataDao.getMetadataForApp(
            childId = childDeviceId,
            packageName = packageName,
            since = twoHoursAgo
        )

        if (metadata.size >= 4) { // Activity in 4+ consecutive collection periods
            val totalNotifications = metadata.sumOf { it.hourlyNotificationCount }
            if (totalNotifications > 30) {
                return AnomalyResult(
                    type = AnomalyType.SUSTAINED_HIGH_ACTIVITY,
                    severity = Severity.HIGH,
                    packageName = packageName,
                    appName = appName,
                    description = "Sustained high activity: $totalNotifications notifications over ${metadata.size * 15} minutes",
                    metadata = mapOf(
                        "totalNotifications" to totalNotifications,
                        "durationMinutes" to (metadata.size * 15),
                        "collectionPeriods" to metadata.size
                    )
                )
            }
        }

        return null
    }

    private suspend fun detectUnusualHourActivity(
        childDeviceId: String,
        packageName: String,
        appName: String
    ): AnomalyResult? {
        val now = System.currentTimeMillis()
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Check if current time is unusual
        val isUnusualHour = currentHour >= UNUSUAL_HOUR_START || currentHour < UNUSUAL_HOUR_END
        if (!isUnusualHour) return null

        val twoHoursAgo = now - (2 * 60 * 60 * 1000)
        val recentMetadata = communicationMetadataDao.getMetadataForApp(
            childId = childDeviceId,
            packageName = packageName,
            since = twoHoursAgo
        )

        val totalNotifications = recentMetadata.sumOf { it.hourlyNotificationCount }
        if (totalNotifications > 10) {
            return AnomalyResult(
                type = AnomalyType.UNUSUAL_HOUR_ACTIVITY,
                severity = Severity.MEDIUM,
                packageName = packageName,
                appName = appName,
                description = "Late-night activity: $totalNotifications notifications between ${UNUSUAL_HOUR_START}:00 and ${UNUSUAL_HOUR_END}:00",
                metadata = mapOf(
                    "totalNotifications" to totalNotifications,
                    "hourOfDay" to currentHour,
                    "unusualHourRange" to "${UNUSUAL_HOUR_START}-${UNUSUAL_HOUR_END}"
                )
            )
        }

        return null
    }

    private suspend fun detectCrossAppCorrelation(childDeviceId: String): AnomalyResult? {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - (60 * 60 * 1000)

        var highActivityApps = 0
        val appDetails = mutableListOf<String>()

        val monitoredAppsList = mapOf(
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

        for ((packageName, appName) in monitoredAppsList) {
            val notifications = notificationCountDao.getTotalNotificationsForApp(
                childId = childDeviceId,
                packageName = packageName,
                since = oneHourAgo
            ) ?: 0

            if (notifications > 15) {
                highActivityApps++
                appDetails.add("$appName: $notifications")
            }
        }

        if (highActivityApps >= 3) {
            return AnomalyResult(
                type = AnomalyType.CROSS_APP_CORRELATION,
                severity = Severity.HIGH,
                packageName = "multiple",
                appName = "Multiple Apps",
                description = "High activity across $highActivityApps apps simultaneously",
                metadata = mapOf(
                    "highActivityCount" to highActivityApps,
                    "apps" to appDetails.joinToString(", ")
                )
            )
        }

        return null
    }

    private suspend fun updateBaselineIfNeeded(childDeviceId: String) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        val monitoredAppsList2 = mapOf(
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

        for ((packageName, appName) in monitoredAppsList2) {
            val existingBaseline = usageBaselineDao.getBaseline(childDeviceId, packageName)

            if (existingBaseline == null || 
                (now - existingBaseline.lastUpdated) > BASELINE_UPDATE_INTERVAL_DAYS * 24 * 60 * 60 * 1000L) {
                
                // Calculate new baseline from last 7 days
                val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000L)
                val usageStats = appUsageStatsDao.getUsageStatsForApp(childDeviceId, packageName, sevenDaysAgo)
                val notificationStats = notificationCountDao.getNotificationCountsForApp(childDeviceId, packageName, sevenDaysAgo)

                if (usageStats.isNotEmpty() || notificationStats.isNotEmpty()) {
                    val avgDailyUsage = if (usageStats.isNotEmpty()) {
                        usageStats.sumOf { it.usageTimeMinutes } / 7
                    } else 0L

                    val avgHourlyNotifications = if (notificationStats.isNotEmpty()) {
                        notificationStats.sumOf { it.notificationCount }.toFloat() / (7 * 24)
                    } else 0f

                    val baseline = UsageBaselineEntity(
                        packageName = packageName,
                        appName = appName,
                        childDeviceId = childDeviceId,
                        avgDailyUsageMinutes = avgDailyUsage,
                        avgHourlyNotifications = avgHourlyNotifications,
                        peakUsageHour = hour,
                        baselinePeriodDays = 7,
                        lastUpdated = now
                    )
                    usageBaselineDao.insertBaseline(baseline)
                }
            }
        }
    }
}
