package com.familyguard.app.domain.model

enum class ContextualConsentType(
    val id: String,
    val displayName: String,
    val description: String,
    val dataCollected: List<String>,
    val explicitStatement: String
) {
    APP_USAGE_TRACKING(
        id = "app_usage_tracking",
        displayName = "App Usage Tracking",
        description = "Track which apps are used and for how long",
        dataCollected = listOf(
            "App names (e.g., WhatsApp, Instagram)",
            "Usage duration (minutes per app)",
            "Timestamp of last use",
            "Number of app launches"
        ),
        explicitStatement = "We collect ONLY app names and usage times. We do NOT read message content, view photos, or access any private data within apps."
    ),
    NOTIFICATION_COUNTING(
        id = "notification_counting",
        displayName = "Notification Counting",
        description = "Count notifications received from apps",
        dataCollected = listOf(
            "Number of notifications per app",
            "Hour of day notifications received",
            "App name (e.g., WhatsApp, Telegram)"
        ),
        explicitStatement = "We count ONLY the number of notifications. We do NOT read notification content, message text, or sender names."
    ),
    LOCATION_SHARING(
        id = "location_sharing",
        displayName = "Location Sharing",
        description = "Share device location for safety",
        dataCollected = listOf(
            "GPS coordinates (latitude, longitude)",
            "Accuracy level",
            "Timestamp of location update"
        ),
        explicitStatement = "We collect ONLY GPS coordinates for safety purposes. Location is shared with parents only."
    ),
    ANOMALY_DETECTION(
        id = "anomaly_detection",
        displayName = "Safety Pattern Detection",
        description = "Detect unusual activity patterns for safety alerts",
        dataCollected = listOf(
            "Aggregated usage patterns",
            "Notification frequency analysis",
            "Time-of-day activity patterns"
        ),
        explicitStatement = "We analyze ONLY aggregated patterns to detect potential safety concerns. No individual messages or content are ever analyzed."
    ),
    REAL_TIME_SYNC(
        id = "real_time_sync",
        displayName = "Real-Time Status Updates",
        description = "Share current activity status with parents",
        dataCollected = listOf(
            "Currently active app name",
            "Current usage session duration",
            "Notification count in last hour"
        ),
        explicitStatement = "We share ONLY the name of the currently active app and usage duration. No content from within apps is shared."
    ),
    COMMUNICATION_METADATA(
        id = "communication_metadata",
        displayName = "Communication Activity Insights",
        description = "Capture messaging-app activity metadata for safety insights",
        dataCollected = listOf(
            "App name (e.g., WhatsApp, Telegram)",
            "Number of communication events per app",
            "Whether a notification contained media (true/false, no file)",
            "Notification category (e.g., msg, social)",
            "A short (<=40 character) notification preview"
        ),
        explicitStatement = "We collect app names, event counts, a media-presence flag, and a short notification preview (<=40 characters). We do NOT read full message text, access photos/videos/audio files, contact names, or phone numbers. Previews are filtered to strip phone numbers, emails, and URLs."
    )
}

data class ContextualConsentRecord(
    val consentType: ContextualConsentType,
    val grantedAt: Long,
    val revokedAt: Long?,
    val consentVersion: String,
    val grantedBy: String,
    val childDeviceId: String,
    val isActive: Boolean,
    val explicitAcknowledgment: Boolean
)

object ConsentConstants {
    const val CONSENT_VERSION = "2.0"
    const val EXPLICIT_CONSENT_REQUIRED = true
    
    val REQUIRED_CONSENTS_FOR_CONTEXTUAL_AWARENESS = listOf(
        ContextualConsentType.APP_USAGE_TRACKING,
        ContextualConsentType.NOTIFICATION_COUNTING,
        ContextualConsentType.ANOMALY_DETECTION,
        ContextualConsentType.REAL_TIME_SYNC
    )
    
    val REQUIRED_CONSENTS_FOR_FULL_FEATURES = REQUIRED_CONSENTS_FOR_CONTEXTUAL_AWARENESS + listOf(
        ContextualConsentType.LOCATION_SHARING,
        ContextualConsentType.COMMUNICATION_METADATA
    )
}

object PrivacyPromises {
    const val LIMITED_MESSAGE_PREVIEWS = "We may store short (<=40 character) notification previews from messaging apps; we never read or store full message text"
    const val NEVER_COLLECTS_CONTACT_NAMES = "We never access contact lists or names"
    const val NEVER_COLLECTS_MEDIA_FILES = "We never access photo, video, or audio file contents; we only record whether a notification contained media"
    const val NEVER_COLLECTS_BROWSING_HISTORY = "We never track web browsing activity"
    const val NEVER_COLLECTS_PASSWORDS = "We never access passwords or credentials"
    const val METADATA_FIRST = "We primarily collect app names, usage times, and notification counts; short previews are optional and filtered to remove phone numbers, emails, and URLs"
    const val ON_DEVICE_PROCESSING = "All analysis happens on-device"
    const val USER_CONTROL = "You can revoke any consent at any time"
    const val TRANSPARENT_NOTIFICATION = "App always shows when monitoring is active"

    val ALL_PROMISES = listOf(
        LIMITED_MESSAGE_PREVIEWS,
        NEVER_COLLECTS_CONTACT_NAMES,
        NEVER_COLLECTS_MEDIA_FILES,
        NEVER_COLLECTS_BROWSING_HISTORY,
        NEVER_COLLECTS_PASSWORDS,
        METADATA_FIRST,
        ON_DEVICE_PROCESSING,
        USER_CONTROL,
        TRANSPARENT_NOTIFICATION
    )
}
