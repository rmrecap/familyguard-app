package com.familyguard.app.service

import android.content.Context
import android.database.Cursor
import android.provider.CallLog
import android.util.Log
import com.familyguard.app.data.local.PreferencesManager
import com.familyguard.app.data.local.dao.CallLogMetadataDao
import com.familyguard.app.data.local.entity.CallLogMetadataEntity
import com.familyguard.app.security.DataValidator
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallLogCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val callLogMetadataDao: CallLogMetadataDao,
    private val dataValidator: DataValidator
) {
    companion object {
        private const val TAG = "CallLogCollector"
        private const val RETENTION_DAYS = 30
    }

    suspend fun collectCallLogs() {
        val childId = preferencesManager.getDeviceId()
        if (childId.isEmpty()) return

        try {
            val contentResolver = context.contentResolver
            val projection = arrayOf(
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.TYPE
            )

            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.add(Calendar.HOUR_OF_DAY, -1) // Fetch last 1 hour
            val startTime = calendar.timeInMillis

            val selection = "${CallLog.Calls.DATE} > ?"
            val selectionArgs = arrayOf(startTime.toString())
            val sortOrder = "${CallLog.Calls.DATE} DESC"

            val cursor: Cursor? = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            val callsList = mutableListOf<CallLogMetadataEntity>()

            cursor?.use { c ->
                val dateIdx = c.getColumnIndex(CallLog.Calls.DATE)
                val durationIdx = c.getColumnIndex(CallLog.Calls.DURATION)
                val typeIdx = c.getColumnIndex(CallLog.Calls.TYPE)

                while (c.moveToNext()) {
                    val date = c.getLong(dateIdx)
                    val duration = c.getLong(durationIdx)
                    val typeVal = c.getInt(typeIdx)

                    val typeStr = when (typeVal) {
                        CallLog.Calls.INCOMING_TYPE -> "INCOMING"
                        CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
                        CallLog.Calls.MISSED_TYPE -> "MISSED"
                        CallLog.Calls.VOICEMAIL_TYPE -> "VOICEMAIL"
                        CallLog.Calls.REJECTED_TYPE -> "REJECTED"
                        CallLog.Calls.BLOCKED_TYPE -> "BLOCKED"
                        else -> "UNKNOWN"
                    }

                    val cal = Calendar.getInstance().apply { timeInMillis = date }
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

                    // Validate call metadata
                    val validationData = mapOf(
                        "collectedAt" to date.toString(),
                        "durationSeconds" to duration.toString(),
                        "callType" to typeStr,
                        "hourOfDay" to hour.toString(),
                        "dayOfWeek" to dayOfWeek.toString()
                    )

                    val validationResult = dataValidator.validateData(validationData, TAG)
                    if (validationResult is DataValidator.ValidationResult.Valid) {
                        callsList.add(
                            CallLogMetadataEntity(
                                durationSeconds = duration,
                                callType = typeStr,
                                collectedAt = date,
                                hourOfDay = hour,
                                dayOfWeek = dayOfWeek,
                                childDeviceId = childId
                            )
                        )
                    } else {
                        Log.e(TAG, "Call log metadata validation failed: ${(validationResult as DataValidator.ValidationResult.Invalid).reason}")
                    }
                }
            }

            if (callsList.isNotEmpty()) {
                callLogMetadataDao.insertAllCalls(callsList)
            }

            cleanupOldData()
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing READ_CALL_LOG permission", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting call logs", e)
        }
    }

    private suspend fun cleanupOldData() {
        val cutoff = System.currentTimeMillis() - (RETENTION_DAYS * 24 * 60 * 60 * 1000L)
        callLogMetadataDao.deleteOldCalls(cutoff)
    }
}
