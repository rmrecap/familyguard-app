package com.familyguard.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.familyguard.app.R
import com.familyguard.app.data.local.PreferencesManager
import com.familyguard.app.domain.model.ContextualStateReport
import com.familyguard.app.ui.MainActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class ContextualSyncService : Service() {

    @Inject lateinit var contextAggregator: ContextAggregator
    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var usageStatsCollector: UsageStatsCollector
    @Inject lateinit var callLogCollector: CallLogCollector
    @Inject lateinit var encryptionManager: com.familyguard.app.security.E2EEncryptionManager

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val firestore = FirebaseFirestore.getInstance()
    private val gson = Gson()

    companion object {
        const val NOTIFICATION_ID = 1004
        const val CHANNEL_ID = "transparency"
        const val SYNC_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes for contextual updates
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_STOP") {
            scope.cancel()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createTransparencyNotification()
        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )

        scope.launch {
            while (isActive) {
                try {
                    syncContextualReport()
                } catch (e: Exception) {
                    // Log error but continue
                }
                delay(SYNC_INTERVAL_MS)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun syncContextualReport() {
        val childId = preferencesManager.getDeviceId()
        if (childId.isEmpty()) return
 
        // Run collectors to get latest statistics before aggregating
        try {
            usageStatsCollector.collectUsageStats()
            callLogCollector.collectCallLogs()
        } catch (e: Exception) {
            // Ignore collection failures
        }

        val report = contextAggregator.generateContextualReport(childId)
        syncToFirestore(report)
    }

    private suspend fun syncToFirestore(report: ContextualStateReport) {
        try {
            val reportData = mapOf(
                "childDeviceId" to report.childDeviceId,
                "childName" to report.childName,
                "timestamp" to report.timestamp,
                "currentForegroundApp" to report.currentForegroundApp?.let { app ->
                    mapOf(
                        "packageName" to app.packageName,
                        "appName" to app.appName,
                        "isCurrentlyActive" to app.isCurrentlyActive,
                        "usageMinutesToday" to app.usageMinutesToday,
                        "usageMinutesLastHour" to app.usageMinutesLastHour,
                        "notificationCountLastHour" to app.notificationCountLastHour
                    )
                },
                "notificationSummary" to mapOf(
                    "totalNotificationsToday" to report.notificationSummary.totalNotificationsToday,
                    "notificationsLastHour" to report.notificationSummary.notificationsLastHour,
                    "notificationTrend" to report.notificationSummary.notificationTrend.name
                ),
                "usageSummary" to mapOf(
                    "totalScreenTimeToday" to report.usageSummary.totalScreenTimeToday,
                    "screenTimeLastHour" to report.usageSummary.screenTimeLastHour,
                    "usageTrend" to report.usageSummary.usageTrend.name
                ),
                "dailyInsights" to mapOf(
                    "mostUsedApp" to report.dailyInsights.mostUsedApp,
                    "peakActivityHour" to report.dailyInsights.peakActivityHour,
                    "totalAppsUsed" to report.dailyInsights.totalAppsUsed,
                    "earlyMorningActivity" to report.dailyInsights.earlyMorningActivity,
                    "lateNightActivity" to report.dailyInsights.lateNightActivity
                ),
                "callLogSummary" to report.callLogSummary?.let { cls ->
                    mapOf(
                        "totalCallsToday" to cls.totalCallsToday,
                        "callsLastHour" to cls.callsLastHour,
                        "totalDurationSecondsToday" to cls.totalDurationSecondsToday,
                        "durationSecondsLastHour" to cls.durationSecondsLastHour
                    )
                }
            )

            // Serialize & Encrypt the contextual report
            val json = gson.toJson(reportData)
            val encryptedPayload = encryptionManager.encrypt(json.toByteArray(Charsets.UTF_8))
            val transmitString = encryptedPayload.toTransmitFormat()

            val firestorePayload = mapOf(
                "childDeviceId" to report.childDeviceId,
                "timestamp" to report.timestamp,
                "encryptedData" to transmitString
            )

            firestore.collection("contextual_reports")
                .document(report.childDeviceId)
                .set(firestorePayload)
                .await()
 
            // Also store in time-series collection for historical data
            firestore.collection("contextual_history")
                .document("${report.childDeviceId}_${report.timestamp}")
                .set(firestorePayload)
                .await()

        } catch (e: Exception) {
            // Handle sync failure
        }
    }

    private fun createTransparencyNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("FamilyGuard Active")
            .setContentText("Syncing contextual awareness data")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
