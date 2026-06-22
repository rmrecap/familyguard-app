package com.familyguard.app.service

import android.util.Log
import com.familyguard.app.data.local.PreferencesManager
import com.familyguard.app.data.local.dao.CommunicationEventDao
import com.familyguard.app.data.local.dao.CommunicationMetadataDao
import com.familyguard.app.data.local.entity.CommunicationMetadataEntity
import com.familyguard.app.security.Actor
import com.familyguard.app.security.AuditAction
import com.familyguard.app.security.AuditLogger
import com.familyguard.app.security.DataValidator
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Report-facing aggregator for communication activity.
 *
 * Reads raw per-notification events captured by [NotificationCountCollector]
 * from the `communication_events` table and refreshes the aggregated
 * `communication_metadata` rows used by the contextual report. Metadata only —
 * no message content, contacts, or media bytes ever leave this layer.
 */
@Singleton
class CommunicationDataCollector @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val communicationEventDao: CommunicationEventDao,
    private val communicationMetadataDao: CommunicationMetadataDao,
    private val dataValidator: DataValidator,
    private val auditLogger: AuditLogger
) {
    companion object {
        private const val TAG = "CommunicationDataCollector"
        private const val RETENTION_DAYS = 30
    }

    /**
     * Rolls up communication events for the current hour into the aggregated
     * `communication_metadata` table. Called by [ContextualSyncService] before
     * report generation, mirroring the other collectors' contract.
     */
    suspend fun collectCommunication() {
        val childId = preferencesManager.getDeviceId()
        if (childId.isEmpty()) return

        try {
            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis
            calendar.add(Calendar.HOUR_OF_DAY, -1)
            val oneHourAgo = calendar.timeInMillis

            val recentEvents = communicationEventDao.getEventsSince(childId, oneHourAgo)
            if (recentEvents.isEmpty()) {
                cleanupOldData()
                return
            }

            val hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

            // Aggregate per-package: total events + media events for the hour.
            val metadataList = recentEvents
                .groupBy { it.packageName }
                .map { (packageName, events) ->
                    val appName = events.first().appName
                    val eventCount = events.size
                    val mediaCount = events.count { it.hasMedia }

                    val validationData = mapOf<String, Any>(
                        "packageName" to packageName,
                        "appName" to appName,
                        "notificationCount" to eventCount.toString(),
                        "hourOfDay" to hourOfDay.toString(),
                        "dayOfWeek" to dayOfWeek.toString()
                    )
                    val validation = dataValidator.validateData(validationData, TAG)
                    if (validation is DataValidator.ValidationResult.Invalid) {
                        Log.e(TAG, "Communication metadata validation failed: ${validation.reason}")
                        return@map null
                    }

                    CommunicationMetadataEntity(
                        packageName = packageName,
                        appName = appName,
                        hourlyNotificationCount = eventCount,
                        hourlyUsageMinutes = 0L,
                        hourOfDay = hourOfDay,
                        dayOfWeek = dayOfWeek,
                        collectedAt = now,
                        childDeviceId = childId
                    )
                }
                .filterNotNull()

            if (metadataList.isNotEmpty()) {
                communicationMetadataDao.insertAllMetadata(metadataList)
            }

            auditLogger.log(
                action = AuditAction.COMMUNICATION_METADATA_ACCESSED,
                actor = Actor.SYSTEM,
                details = "Aggregated ${recentEvents.size} communication events into report"
            )

            cleanupOldData()
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting communication data", e)
        }
    }

    private suspend fun cleanupOldData() {
        val cutoff = System.currentTimeMillis() - (RETENTION_DAYS * 24 * 60 * 60 * 1000L)
        communicationEventDao.deleteOldEvents(cutoff)
        communicationMetadataDao.deleteOldMetadata(cutoff)
    }
}
