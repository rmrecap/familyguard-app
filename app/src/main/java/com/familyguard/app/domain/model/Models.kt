package com.familyguard.app.domain.model

import java.time.Instant

enum class DeviceRole {
    PARENT,
    CHILD
}

data class DeviceProfile(
    val deviceId: String,
    val userId: String,
    val role: DeviceRole,
    val deviceName: String,
    val deviceModel: String,
    val osVersion: String,
    val fcmToken: String,
    val registeredAt: Instant,
    val lastSeenAt: Instant
)

data class FamilyGroup(
    val groupId: String,
    val parentDeviceIds: List<String>,
    val childDeviceIds: List<String>,
    val createdAt: Instant,
    val inviteCode: String
)

data class LocationSnapshot(
    val snapshotId: String,
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double?,
    val speed: Float?,
    val timestamp: Instant,
    val batteryLevel: Int
)

data class LocationSync(
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Instant,
    val isInSafeZone: Boolean,
    val nearestSafeZoneName: String?
)

data class SafeZone(
    val zoneId: String,
    val name: String,
    val centerLatitude: Double,
    val centerLongitude: Double,
    val radiusMeters: Float,
    val isActive: Boolean,
    val notifyOnEntry: Boolean,
    val notifyOnExit: Boolean
)

enum class SosType {
    PANIC_BUTTON,
    FALL_DETECTED,
    MANUAL
}

enum class AlertStatus {
    TRIGGERED,
    DELIVERED,
    ACKNOWLEDGED,
    CANCELLED
}

data class SosAlert(
    val alertId: String,
    val childDeviceId: String,
    val triggeredAt: Instant,
    val latitude: Double,
    val longitude: Double,
    val alertType: SosType,
    val status: AlertStatus,
    val acknowledgedBy: String?,
    val acknowledgedAt: Instant?
)

enum class Feature(
    val id: String,
    val displayName: String,
    val description: String,
    val icon: String
) {
    LOCATION_SHARING(
        "location_sharing",
        "Location Sharing",
        "Share your location with your parents for safety",
        "location_on"
    ),
    SOS(
        "sos",
        "Emergency SOS",
        "Send emergency alerts with your location",
        "emergency"
    ),
    GEOFENCE(
        "geofence",
        "Safe Zones",
        "Get alerts when entering or leaving safe zones",
        "fence"
    ),
    CONTACT_WHITELIST(
        "contact_whitelist",
        "Contact Whitelist",
        "Filter who can contact you",
        "contacts"
    ),
    SCREEN_TIME(
        "screen_time",
        "Screen Time",
        "Track and manage app usage",
        "timer"
    )
}

data class ConsentRecord(
    val featureId: String,
    val featureName: String,
    val description: String,
    val grantedAt: Instant,
    val revokedAt: Instant?,
    val consentVersion: String,
    val grantedBy: String,
    val childDeviceId: String,
    val isActive: Boolean
)
