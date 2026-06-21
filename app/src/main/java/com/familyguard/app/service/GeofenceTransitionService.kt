package com.familyguard.app.service

import android.app.IntentService
import android.app.NotificationManager
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.familyguard.app.R
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceTransitionService : IntentService("GeofenceTransitionService") {

    companion object {
        const val CHANNEL_ID = "geofence"
        const val NOTIFICATION_ID = 2001
    }

    @Suppress("DEPRECATION")
    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return

        val event = GeofencingEvent.fromIntent(intent) ?: return

        if (event.hasError()) {
            return
        }

        val transitionType = event.geofenceTransition
        val triggeringGeofences = event.triggeringGeofences ?: return

        for (geofence in triggeringGeofences) {
            val zoneName = geofence.requestId

            when (transitionType) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    showGeofenceNotification("Entered safe zone: $zoneName")
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    showGeofenceNotification("Left safe zone: $zoneName")
                }
            }
        }
    }

    private fun showGeofenceNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("FamilyGuard - Safe Zone Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }
}
