package com.familyguard.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.familyguard.app.domain.usecase.security.ActivateKillSwitchUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class KillSwitchReceiver : BroadcastReceiver() {

    @Inject lateinit var activateKillSwitchUseCase: ActivateKillSwitchUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val deviceId = context.getSharedPreferences(
                    "familyguard_secure_prefs", Context.MODE_PRIVATE
                ).getString("device_id", "") ?: ""

                activateKillSwitchUseCase(deviceId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
