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
import com.familyguard.app.domain.usecase.anomaly.AnomalyDetector
import com.familyguard.app.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class AnomalyDetectionService : Service() {

    @Inject lateinit var anomalyDetector: AnomalyDetector
    @Inject lateinit var alertTriggerService: AlertTriggerService
    @Inject lateinit var preferencesManager: PreferencesManager

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val NOTIFICATION_ID = 1003
        const val CHANNEL_ID = "transparency"
        const val DETECTION_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        alertTriggerService.createAlertChannels()
    }

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
                    detectAnomalies()
                } catch (e: Exception) {
                    // Log error but continue
                }
                delay(DETECTION_INTERVAL_MS)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun detectAnomalies() {
        val childId = preferencesManager.getDeviceId()
        if (childId.isEmpty()) return

        val anomalies = anomalyDetector.analyze(childId)
        if (anomalies.isNotEmpty()) {
            alertTriggerService.processAnomalyAlerts(anomalies)
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
            .setContentText("Monitoring for safety patterns")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
