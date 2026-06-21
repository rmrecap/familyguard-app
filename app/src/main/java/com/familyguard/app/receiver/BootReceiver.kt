package com.familyguard.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.familyguard.app.data.local.dao.ConsentDao
import com.familyguard.app.service.LocationTrackingService
import com.familyguard.app.service.SyncService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var consentDao: ConsentDao

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val deviceId = context.getSharedPreferences(
                    "familyguard_secure_prefs", Context.MODE_PRIVATE
                ).getString("device_id", "") ?: ""

                if (deviceId.isNotEmpty()) {
                    val hasLocationConsent = consentDao.getConsentForFeature(
                        deviceId, "location_sharing"
                    )?.isActive == true

                    if (hasLocationConsent) {
                        context.startForegroundService(
                            Intent(context, LocationTrackingService::class.java)
                        )
                    }

                    context.startForegroundService(
                        Intent(context, SyncService::class.java)
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
