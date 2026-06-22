package com.familyguard.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.familyguard.app.R
import com.familyguard.app.domain.usecase.location.UpdateLocationUseCase
import com.familyguard.app.ui.MainActivity
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject lateinit var updateLocationUseCase: UpdateLocationUseCase

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "transparency"
        const val LOCATION_INTERVAL_MS = 15000L // 15 seconds
        /** Maximum acceptable horizontal accuracy in meters. Fixes worse than this are discarded. */
        const val MAX_ACCEPTABLE_ACCURACY_M = 100f
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_STOP" -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val notification = createTransparencyNotification()
        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )

        startLocationUpdates()

        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
        super.onDestroy()
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MS)
            .setMinUpdateIntervalMillis(LOCATION_INTERVAL_MS / 2)
            .setMinUpdateDistanceMeters(10f)  // Suppress redundant near-duplicate fixes
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    // Accuracy gate: discard garbage fixes before any processing
                    if (location.accuracy > MAX_ACCEPTABLE_ACCURACY_M) return
                    scope.launch {
                        val deviceId = loadDeviceId()
                        updateLocationUseCase(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy,
                            altitude = location.altitude,
                            speed = location.speed,
                            batteryLevel = getBatteryLevel(),
                            deviceId = deviceId
                        )
                    }
                }
            }
        }

        try {
            fusedLocationClient?.requestLocationUpdates(
                request,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    private fun createTransparencyNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LocationTrackingService::class.java).apply {
            action = "ACTION_STOP"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("FamilyGuard Active")
            .setContentText("Location monitoring is active. Tap to open dashboard or disable.")
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

    private fun getBatteryLevel(): Int {
        val bm = getSystemService(BATTERY_SERVICE) as android.os.BatteryManager
        return bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
}
