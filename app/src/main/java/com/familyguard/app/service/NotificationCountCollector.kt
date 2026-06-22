package com.familyguard.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.familyguard.app.data.local.PreferencesManager
import com.familyguard.app.data.local.dao.CommunicationEventDao
import com.familyguard.app.data.local.dao.NotificationCountDao
import com.familyguard.app.data.local.entity.CommunicationEventEntity
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
    @Inject lateinit var communicationEventDao: CommunicationEventDao
    @Inject lateinit var dataValidator: DataValidator

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "NotificationCountCollector"

        /** Maximum characters retained for a notification preview snippet. */
        private const val MAX_SNIPPET_LENGTH = 40

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
        val now = System.currentTimeMillis()

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
            collectedAt = now,
            childDeviceId = childId,
            hourOfDay = hourOfDay,
            dayOfWeek = dayOfWeek
        )

        scope.launch {
            notificationCountDao.insertNotificationCount(countEntity)
            insertCommunicationEvent(sbn, appName, hourOfDay, dayOfWeek, now, childId)
        }
    }

    /**
     * Captures per-notification communication metadata (category, media presence,
     * and a length-capped preview) into the communication_events table.
     *
     * Privacy: only the notification [Notification.category], a boolean media flag,
     * and a <=[MAX_SNIPPET_LENGTH] char preview are stored. The preview is validated
     * field-by-field and dropped entirely if the DataValidator rejects it.
     */
    private suspend fun insertCommunicationEvent(
        sbn: StatusBarNotification,
        appName: String,
        hourOfDay: Int,
        dayOfWeek: Int,
        now: Long,
        childId: String
    ) {
        try {
            val notification = sbn.notification ?: return
            val category = notification.category?.take(40) ?: "unknown"
            val hasMedia = detectMedia(notification)

            val rawSnippet = extractSnippet(notification)
            val snippet = rawSnippet?.take(MAX_SNIPPET_LENGTH)?.takeIf { it.isNotBlank() }

            // Validate every field individually; the validator keeps PII (phone
            // numbers, emails, base64) out of the stored snippet even when relaxed.
            val validationData = mutableMapOf<String, Any>(
                "packageName" to sbn.packageName,
                "appName" to appName,
                "eventCategory" to category,
                "hasMedia" to hasMedia.toString(),
                "hourOfDay" to hourOfDay.toString(),
                "dayOfWeek" to dayOfWeek.toString()
            )
            snippet?.let { validationData["snippet"] = it }

            val snippetValidation = dataValidator.validateData(validationData, TAG)
            if (snippetValidation is DataValidator.ValidationResult.Invalid) {
                Log.e(TAG, "Communication event validation failed: ${snippetValidation.reason}")
                return
            }

            val event = CommunicationEventEntity(
                packageName = sbn.packageName,
                appName = appName,
                eventCategory = category,
                hasMedia = hasMedia,
                snippet = snippet,
                hourOfDay = hourOfDay,
                dayOfWeek = dayOfWeek,
                collectedAt = now,
                childDeviceId = childId
            )
            communicationEventDao.insertEvent(event)
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing communication event", e)
        }
    }

    /** Returns true when the notification carries a picture/media style. */
    private fun detectMedia(notification: Notification): Boolean {
        val extras = notification.extras ?: return false
        if (extras.get(NotificationCompat.EXTRA_PICTURE) != null) return true
        if (extras.get(NotificationCompat.EXTRA_BIG_TEXT) != null &&
            extras.get(NotificationCompat.EXTRA_PICTURE) != null) return true
        // Messaging-style conversations sometimes bundle a large icon (e.g. photo)
        val styleClass = notification.javaClass.name
        return styleClass.contains("Picture", ignoreCase = true)
    }

    /** Extracts a short preview text from the notification extras (ticker/title/text). */
    private fun extractSnippet(notification: Notification): String? {
        val extras = notification.extras ?: return null
        val title = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString()
        return text ?: bigText ?: title
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No action needed on removal
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
