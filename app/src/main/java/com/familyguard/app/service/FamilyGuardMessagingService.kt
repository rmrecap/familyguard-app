package com.familyguard.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.familyguard.app.R
import com.familyguard.app.data.local.PreferencesManager
import com.familyguard.app.ui.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FamilyGuardMessagingService : FirebaseMessagingService() {

    @Inject lateinit var preferencesManager: PreferencesManager

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        preferencesManager.saveDeviceId(token)
        // TODO: Send token to server for push notifications
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val type = data["type"] ?: return

        when (type) {
            "SOS_ALERT" -> handleSosAlert(data)
            "GEOFENCE_ALERT" -> handleGeofenceAlert(data)
            "LOCATION_UPDATE" -> handleLocationUpdate(data)
            else -> handleGenericNotification(message)
        }
    }

    private fun handleSosAlert(data: Map<String, String>) {
        val alertId = data["alertId"] ?: return
        val childDeviceId = data["childDeviceId"] ?: return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "alerts")
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "alerts")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Emergency SOS Alert!")
            .setContentText("Your child has triggered an emergency alert!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_stop, "View", pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(alertId.hashCode(), notification)
    }

    private fun handleGeofenceAlert(data: Map<String, String>) {
        val zoneName = data["zoneName"] ?: "Unknown Zone"
        val transitionType = data["transitionType"] ?: "entered"

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "geofence")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Geofence Alert")
            .setContentText("Your child has $transitionType $zoneName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun handleLocationUpdate(data: Map<String, String>) {
        // Location updates are handled silently, no notification needed
    }

    private fun handleGenericNotification(message: RemoteMessage) {
        val notification = message.notification ?: return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(this, "alerts")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notif)
    }
}
