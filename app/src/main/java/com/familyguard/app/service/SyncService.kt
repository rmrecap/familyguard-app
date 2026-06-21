package com.familyguard.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.familyguard.app.R
import com.familyguard.app.data.local.dao.SyncQueueDao
import com.familyguard.app.data.remote.api.SyncApi
import com.familyguard.app.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class SyncService : Service() {

    @Inject lateinit var syncApi: SyncApi
    @Inject lateinit var syncQueueDao: SyncQueueDao

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val NOTIFICATION_ID = 1002
        const val CHANNEL_ID = "transparency"
        const val SYNC_INTERVAL_MS = 30000L
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createTransparencyNotification()
        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )

        scope.launch {
            while (isActive) {
                try {
                    syncPendingItems()
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

    private suspend fun syncPendingItems() {
        val items = syncQueueDao.getPendingItems()
        for (item in items) {
            try {
                syncApi.syncLocation(
                    com.familyguard.app.data.remote.api.SyncRequest(
                        deviceId = loadDeviceId(),
                        payload = item.payload,
                        timestamp = item.createdAt,
                        signature = ""
                    )
                )
                syncQueueDao.markAsSynced(item.id, System.currentTimeMillis())
            } catch (e: Exception) {
                // Will retry on next cycle
            }
        }
    }

    private fun createTransparencyNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, SyncService::class.java).apply {
            action = "ACTION_STOP"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("FamilyGuard Active")
            .setContentText("Data sync is active. Tap to open dashboard or disable.")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun loadDeviceId(): String {
        return getSharedPreferences("familyguard_secure_prefs", MODE_PRIVATE)
            .getString("device_id", "") ?: ""
    }
}
