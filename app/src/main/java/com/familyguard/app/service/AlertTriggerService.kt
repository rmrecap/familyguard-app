package com.familyguard.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.familyguard.app.R
import com.familyguard.app.data.local.PreferencesManager
import com.familyguard.app.data.local.dao.AnomalyAlertDao
import com.familyguard.app.data.local.dao.DeviceProfileDao
import com.familyguard.app.data.local.entity.AnomalyAlertEntity
import com.familyguard.app.domain.model.AnomalyType
import com.familyguard.app.domain.model.Severity
import com.familyguard.app.ui.MainActivity
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertTriggerService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val anomalyAlertDao: AnomalyAlertDao,
    private val deviceProfileDao: DeviceProfileDao
) {
    companion object {
        private const val ALERT_CHANNEL = "anomaly_alerts"
        private const val CRITICAL_CHANNEL = "critical_alerts"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun processAnomalyAlerts(anomalies: List<com.familyguard.app.domain.usecase.anomaly.AnomalyResult>) {
        for (anomaly in anomalies) {
            triggerAlert(anomaly)
        }
    }

    private suspend fun triggerAlert(anomaly: com.familyguard.app.domain.usecase.anomaly.AnomalyResult) {
        val childDeviceId = preferencesManager.getDeviceId()

        // Get parent device for notification
        val parentDevices = deviceProfileDao.getDevicesByRole("PARENT")

        // Create notification for parent
        val notification = createAnomalyNotification(anomaly)

        // Show local notification
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(anomaly.hashCode(), notification)

        // Also send via FCM to parent devices (if configured)
        // This would require the server to route the notification
    }

    private fun createAnomalyNotification(anomaly: com.familyguard.app.domain.usecase.anomaly.AnomalyResult): android.app.Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "anomaly_alerts")
        }

        val pendingIntent = PendingIntent.getActivity(
            context, anomaly.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (anomaly.severity) {
            Severity.CRITICAL -> "CRITICAL: ${anomaly.appName} Alert"
            Severity.HIGH -> "HIGH: ${anomaly.appName} Alert"
            Severity.MEDIUM -> "MEDIUM: ${anomaly.appName} Alert"
            Severity.LOW -> "LOW: ${anomaly.appName} Alert"
        }

        val channelId = when (anomaly.severity) {
            Severity.CRITICAL, Severity.HIGH -> CRITICAL_CHANNEL
            else -> ALERT_CHANNEL
        }

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(anomaly.description)
            .setPriority(when (anomaly.severity) {
                Severity.CRITICAL -> NotificationCompat.PRIORITY_MAX
                Severity.HIGH -> NotificationCompat.PRIORITY_HIGH
                Severity.MEDIUM -> NotificationCompat.PRIORITY_DEFAULT
                Severity.LOW -> NotificationCompat.PRIORITY_LOW
            })
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(anomaly.description))
            .build()
    }

    fun createAlertChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val anomalyChannel = android.app.NotificationChannel(
            ALERT_CHANNEL,
            "Anomaly Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts for unusual activity patterns"
        }

        val criticalChannel = android.app.NotificationChannel(
            CRITICAL_CHANNEL,
            "Critical Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Critical safety alerts requiring immediate attention"
        }

        manager.createNotificationChannel(anomalyChannel)
        manager.createNotificationChannel(criticalChannel)
    }

    fun destroy() {
        scope.cancel()
    }
}
