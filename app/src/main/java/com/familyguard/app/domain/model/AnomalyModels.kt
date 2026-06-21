package com.familyguard.app.domain.model

enum class AnomalyType {
    NOTIFICATION_SPIKE,
    USAGE_SPIKE,
    SUSTAINED_HIGH_ACTIVITY,
    UNUSUAL_HOUR_ACTIVITY,
    CROSS_APP_CORRELATION,
    UNUSUAL_CONTACT_PATTERN,
    EMERGENCY_KEYWORD_DETECTED
}

enum class Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

data class AnomalyAlert(
    val alertId: String,
    val childDeviceId: String,
    val anomalyType: AnomalyType,
    val severity: Severity,
    val packageName: String,
    val appName: String,
    val description: String,
    val detectedAt: Long,
    val isAcknowledged: Boolean
)
