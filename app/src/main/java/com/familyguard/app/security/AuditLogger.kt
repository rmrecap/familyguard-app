package com.familyguard.app.security

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class AuditAction {
    LOCATION_ACCESSED,
    LOCATION_SYNCED,
    SOS_TRIGGERED,
    SOS_ACKNOWLEDGED,
    SOS_CANCELLED,
    FEATURE_ENABLED,
    FEATURE_DISABLED,
    KILL_SWITCH_ACTIVATED,
    MONITORING_REENABLED,
    CONSENT_GRANTED,
    CONSENT_REVOKED,
    DATA_VIEWED,
    FAMILY_MEMBER_ADDED,
    FAMILY_MEMBER_REMOVED,
    DEVICE_REGISTERED
}

enum class Actor {
    CHILD,
    PARENT,
    SYSTEM
}

@Singleton
class AuditLogger @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("audit_log", Context.MODE_PRIVATE)
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun log(
        action: AuditAction,
        actor: Actor,
        details: String,
        deviceId: String = getDeviceId()
    ) {
        val entry = AuditEntry(
            id = UUID.randomUUID().toString(),
            action = action.name,
            actor = actor.name,
            details = details,
            deviceId = deviceId,
            timestamp = System.currentTimeMillis()
        )

        val logString = "${dateFormat.format(Date(entry.timestamp))}|${entry.action}|${entry.actor}|${entry.details}|${entry.deviceId}"

        val existingLog = prefs.getString("audit_entries", "") ?: ""
        val newLog = if (existingLog.isEmpty()) {
            logString
        } else {
            "$existingLog\n$logString"
        }

        prefs.edit().putString("audit_entries", newLog).apply()
    }

    fun getAuditLog(): List<AuditEntry> {
        val logString = prefs.getString("audit_entries", "") ?: ""
        if (logString.isEmpty()) return emptyList()

        return logString.split("\n").mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size >= 5) {
                AuditEntry(
                    id = UUID.randomUUID().toString(),
                    action = parts[1],
                    actor = parts[2],
                    details = parts[3],
                    deviceId = parts[4],
                    timestamp = dateFormat.parse(parts[0])?.time ?: 0L
                )
            } else null
        }
    }

    fun clearLog() {
        prefs.edit().remove("audit_entries").apply()
    }

    private fun getDeviceId(): String {
        return prefs.getString("device_id", "") ?: ""
    }
}

data class AuditEntry(
    val id: String,
    val action: String,
    val actor: String,
    val details: String,
    val deviceId: String,
    val timestamp: Long
)
