package com.familyguard.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_snapshots")
data class LocationSnapshotEntity(
    @PrimaryKey val snapshotId: String,
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double?,
    val speed: Float?,
    val timestamp: Long,
    val batteryLevel: Int,
    val synced: Boolean = false
)

@Entity(tableName = "safe_zones")
data class SafeZoneEntity(
    @PrimaryKey val zoneId: String,
    val name: String,
    val centerLatitude: Double,
    val centerLongitude: Double,
    val radiusMeters: Float,
    val isActive: Boolean,
    val notifyOnEntry: Boolean,
    val notifyOnExit: Boolean,
    val createdBy: String
)

@Entity(tableName = "sos_alerts")
data class SosAlertEntity(
    @PrimaryKey val alertId: String,
    val childDeviceId: String,
    val triggeredAt: Long,
    val latitude: Double,
    val longitude: Double,
    val alertType: String,
    val status: String,
    val acknowledgedBy: String?,
    val acknowledgedAt: Long?
)

@Entity(tableName = "consent_records")
data class ConsentRecordEntity(
    @PrimaryKey val featureId: String,
    val featureName: String,
    val description: String,
    val grantedAt: Long,
    val revokedAt: Long?,
    val consentVersion: String,
    val grantedBy: String,
    val childDeviceId: String,
    val isActive: Boolean
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val dataType: String,
    val payload: String,
    val createdAt: Long,
    val attempts: Int = 0,
    val lastAttemptAt: Long?,
    val synced: Boolean = false
)

@Entity(tableName = "device_profiles")
data class DeviceProfileEntity(
    @PrimaryKey val deviceId: String,
    val userId: String,
    val role: String,
    val deviceName: String,
    val deviceModel: String,
    val osVersion: String,
    val fcmToken: String,
    val registeredAt: Long,
    val lastSeenAt: Long
)

@Entity(tableName = "family_groups")
data class FamilyGroupEntity(
    @PrimaryKey val groupId: String,
    val parentDeviceIds: String,
    val childDeviceIds: String,
    val createdAt: Long,
    val inviteCode: String
)
