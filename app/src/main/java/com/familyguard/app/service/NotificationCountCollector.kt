package com.familyguard.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.familyguard.app.data.local.PreferencesManager
import com.familyguard.app.data.local.dao.NotificationCountDao
import com.familyguard.app.data.local.entity.NotificationCountEntity
import com.familyguard.app.security.DataValidator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class NotificationCountCollector : NotificationListenerService() {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var notificationCountDao: NotificationCountDao
    @Inject lateinit var dataValidator: DataValidator

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "NotificationCountCollector"
        
        private val MONITORED_PACKAGES = setOf(
            "com.whatsapp",
            "org.telegram.messenger",
            "com.facebook.messenger",
            "com.instagram.android",
            "com.twitter.android",
            "com.snapchat.android",
            "com.discord",
            "com.zhiliaoapp.musically",
            "com.google.android.gm",
            "com.google.android.apps.messaging"
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (!MONITORED_PACKAGES.contains(sbn.packageName)) return

        val childId = preferencesManager.getDeviceId()
        if (childId.isEmpty()) return

        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        val appName = when (sbn.packageName) {
            "com.whatsapp" -> "WhatsApp"
            "org.telegram.messenger" -> "Telegram"
            "com.facebook.messenger" -> "Messenger"
            "com.instagram.android" -> "Instagram"
            "com.twitter.android" -> "Twitter"
            "com.snapchat.android" -> "Snapchat"
            "com.discord" -> "Discord"
            "com.zhiliaoapp.musically" -> "TikTok"
            "com.google.android.gm" -> "Gmail"
            "com.google.android.apps.messaging" -> "Messages"
            else -> sbn.packageName
        }

        // Validate data before creating entity
        val validationData = mapOf(
            "packageName" to sbn.packageName,
            "appName" to appName,
            "notificationCount" to "1",
            "hourOfDay" to hourOfDay.toString(),
            "dayOfWeek" to dayOfWeek.toString()
        )
        
        val validationResult = dataValidator.validateData(validationData, TAG)
        if (validationResult is DataValidator.ValidationResult.Invalid) {
            Log.e(TAG, "Data validation failed: ${validationResult.reason}")
            return
        }

        val countEntity = NotificationCountEntity(
            packageName = sbn.packageName,
            appName = appName,
            notificationCount = 1,
            collectedAt = System.currentTimeMillis(),
            childDeviceId = childId,
            hourOfDay = hourOfDay,
            dayOfWeek = dayOfWeek
        )

        scope.launch {
            notificationCountDao.insertNotificationCount(countEntity)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No action needed on removal
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
