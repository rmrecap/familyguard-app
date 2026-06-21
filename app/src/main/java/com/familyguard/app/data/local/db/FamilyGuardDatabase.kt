package com.familyguard.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.familyguard.app.data.local.dao.*
import com.familyguard.app.data.local.entity.*

@Database(
    entities = [
        LocationSnapshotEntity::class,
        SafeZoneEntity::class,
        SosAlertEntity::class,
        ConsentRecordEntity::class,
        SyncQueueEntity::class,
        DeviceProfileEntity::class,
        FamilyGroupEntity::class,
        AppUsageStatsEntity::class,
        NotificationCountEntity::class,
        AnomalyAlertEntity::class,
        UsageBaselineEntity::class,
        CommunicationMetadataEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class FamilyGuardDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun safeZoneDao(): SafeZoneDao
    abstract fun sosDao(): SosDao
    abstract fun consentDao(): ConsentDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun deviceProfileDao(): DeviceProfileDao
    abstract fun familyGroupDao(): FamilyGroupDao
    abstract fun appUsageStatsDao(): AppUsageStatsDao
    abstract fun notificationCountDao(): NotificationCountDao
    abstract fun anomalyAlertDao(): AnomalyAlertDao
    abstract fun usageBaselineDao(): UsageBaselineDao
    abstract fun communicationMetadataDao(): CommunicationMetadataDao
}
