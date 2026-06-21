package com.familyguard.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FamilyGuardApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val transparencyChannel = NotificationChannel(
            "transparency",
            "Transparency",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Always visible when monitoring is active"
            setShowBadge(false)
        }

        val alertsChannel = NotificationChannel(
            "alerts",
            "Safety Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Important safety alerts from your family"
        }

        val geofenceChannel = NotificationChannel(
            "geofence",
            "Geofence Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when entering or leaving safe zones"
        }

        manager.createNotificationChannel(transparencyChannel)
        manager.createNotificationChannel(alertsChannel)
        manager.createNotificationChannel(geofenceChannel)
    }
}
