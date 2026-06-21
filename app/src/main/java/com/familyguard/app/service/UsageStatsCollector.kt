package com.familyguard.app.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import com.familyguard.app.data.local.PreferencesManager
import com.familyguard.app.data.local.dao.AppUsageStatsDao
import com.familyguard.app.data.local.dao.CommunicationMetadataDao
import com.familyguard.app.data.local.entity.AppUsageStatsEntity
import com.familyguard.app.data.local.entity.CommunicationMetadataEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageStatsCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val appUsageStatsDao: AppUsageStatsDao,
    private val communicationMetadataDao: CommunicationMetadataDao
) {
    companion object {
        // Messaging and social media apps to monitor (metadata only)
        val MONITORED_APPS = mapOf(
            "com.whatsapp" to "WhatsApp",
            "org.telegram.messenger" to "Telegram",
            "com.facebook.messenger" to "Messenger",
            "com.instagram.android" to "Instagram",
            "com.twitter.android" to "Twitter",
            "com.snapchat.android" to "Snapchat",
            "com.discord" to "Discord",
            "com.zhiliaoapp.musically" to "TikTok",
            "com.google.android.gm" to "Gmail",
            "com.google.android.apps.messaging" to "Messages"
        )

        private const val COLLECTION_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes
        private const val RETENTION_DAYS = 30
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var collectionJob: Job? = null

    fun startCollecting() {
        collectionJob?.cancel()
        collectionJob = scope.launch {
            while (isActive) {
                collectUsageStats()
                delay(COLLECTION_INTERVAL_MS)
            }
        }
    }

    fun stopCollecting() {
        collectionJob?.cancel()
    }

    suspend fun collectUsageStats() {
        val childId = preferencesManager.getDeviceId()
        if (childId.isEmpty()) return

        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return

            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.add(Calendar.HOUR_OF_DAY, -1)
            val startTime = calendar.timeInMillis

            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )

            val now = System.currentTimeMillis()
            val usageStatsList = mutableListOf<AppUsageStatsEntity>()
            val metadataList = mutableListOf<CommunicationMetadataEntity>()

            stats?.filter { it.lastTimeUsed > startTime }?.forEach { usageStats ->
                val packageName = usageStats.packageName
                if (MONITORED_APPS.containsKey(packageName)) {
                    val usageMinutes = (usageStats.totalTimeInForeground / 60000)
                    val appStats = AppUsageStatsEntity(
                        packageName = packageName,
                        appName = MONITORED_APPS[packageName] ?: packageName,
                        usageTimeMinutes = usageMinutes,
                        lastUsedTimestamp = usageStats.lastTimeUsed,
                        launchCount = 0,
                        collectedAt = now,
                        childDeviceId = childId
                    )
                    usageStatsList.add(appStats)

                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

                    val metadata = CommunicationMetadataEntity(
                        packageName = packageName,
                        appName = MONITORED_APPS[packageName] ?: packageName,
                        hourlyNotificationCount = 0,
                        hourlyUsageMinutes = usageMinutes,
                        hourOfDay = hour,
                        dayOfWeek = dayOfWeek,
                        collectedAt = now,
                        childDeviceId = childId
                    )
                    metadataList.add(metadata)
                }
            }

            if (usageStatsList.isNotEmpty()) {
                appUsageStatsDao.insertAllUsageStats(usageStatsList)
            }
            if (metadataList.isNotEmpty()) {
                communicationMetadataDao.insertAllMetadata(metadataList)
            }

            cleanupOldData()
        } catch (e: Exception) {
            // Log error but don't crash
        }
    }

    private suspend fun cleanupOldData() {
        val cutoff = System.currentTimeMillis() - (RETENTION_DAYS * 24 * 60 * 60 * 1000L)
        appUsageStatsDao.deleteOldStats(cutoff)
        communicationMetadataDao.deleteOldMetadata(cutoff)
    }

    fun destroy() {
        scope.cancel()
    }
}
