package com.familyguard.app.ui.viewmodel

import android.content.Intent
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyguard.app.data.local.PreferencesManager
import com.familyguard.app.data.local.dao.DeviceProfileDao
import com.familyguard.app.data.local.dao.LocationDao
import com.familyguard.app.domain.model.*
import com.familyguard.app.service.ContextualSyncService
import com.familyguard.app.service.LocationTrackingService
import com.familyguard.app.service.SyncService
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.tasks.await
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ChildUi(
    val deviceId: String,
    val name: String,
    val lastSeen: String,
    val isOnline: Boolean,
    val lastLocation: String,
    val contextualReport: ContextualStateReport? = null
)

data class ParentDashboardUiState(
    val isLoading: Boolean = true,
    val children: List<ChildUi> = emptyList(),
    val parentName: String = "Parent",
    val inviteCode: String = "",
    val error: String? = null
)

@HiltViewModel
class ParentDashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val deviceProfileDao: DeviceProfileDao,
    private val locationDao: LocationDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParentDashboardUiState())
    val uiState: StateFlow<ParentDashboardUiState> = _uiState.asStateFlow()

    private val firestore = FirebaseFirestore.getInstance()

    init {
        loadDashboard()
        startContextualSync()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val inviteCode = preferencesManager.getInviteCode()
            val devices = deviceProfileDao.getDevicesByRole("CHILD")

            val children = devices.map { device ->
                val latestLocation = locationDao.getLatestLocation(device.deviceId)
                val lastSeenMs = device.lastSeenAt
                val timeAgo = formatTimeAgo(lastSeenMs)
                val isOnline = (System.currentTimeMillis() - lastSeenMs) < 5 * 60 * 1000

                val contextualReport = loadContextualReport(device.deviceId)

                ChildUi(
                    deviceId = device.deviceId,
                    name = device.deviceName,
                    lastSeen = timeAgo,
                    isOnline = isOnline,
                    lastLocation = if (latestLocation != null) {
                        "${String.format("%.4f", latestLocation.latitude)}, ${String.format("%.4f", latestLocation.longitude)}"
                    } else "Unknown",
                    contextualReport = contextualReport
                )
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                children = children,
                inviteCode = inviteCode
            )
        }
    }

    private suspend fun loadContextualReport(childDeviceId: String): ContextualStateReport? {
        return try {
            val doc = firestore.collection("contextual_reports")
                .document(childDeviceId)
                .get()
                .await()

            if (doc.exists()) {
                val data = doc.data ?: return null

                val currentForegroundAppData = data["currentForegroundApp"] as? Map<String, Any>
                val currentForegroundApp = currentForegroundAppData?.let { app ->
                    AppContext(
                        packageName = app["packageName"] as? String ?: "",
                        appName = app["appName"] as? String ?: "",
                        isCurrentlyActive = app["isCurrentlyActive"] as? Boolean ?: false,
                        usageMinutesToday = app["usageMinutesToday"] as? Long ?: 0,
                        usageMinutesLastHour = app["usageMinutesLastHour"] as? Long ?: 0,
                        notificationCountLastHour = app["notificationCountLastHour"] as? Int ?: 0,
                        lastUsedTimestamp = System.currentTimeMillis()
                    )
                }

                val notificationSummaryData = data["notificationSummary"] as? Map<String, Any>
                val notificationSummary = notificationSummaryData?.let { ns ->
                    NotificationSummary(
                        totalNotificationsToday = ns["totalNotificationsToday"] as? Int ?: 0,
                        notificationsLastHour = ns["notificationsLastHour"] as? Int ?: 0,
                        topAppsByNotifications = emptyList(),
                        notificationTrend = try {
                            TrendDirection.valueOf(ns["notificationTrend"] as? String ?: "STABLE")
                        } catch (e: Exception) { TrendDirection.STABLE }
                    )
                }

                val usageSummaryData = data["usageSummary"] as? Map<String, Any>
                val usageSummary = usageSummaryData?.let { us ->
                    UsageSummary(
                        totalScreenTimeToday = us["totalScreenTimeToday"] as? Long ?: 0,
                        screenTimeLastHour = us["screenTimeLastHour"] as? Long ?: 0,
                        topAppsByUsage = emptyList(),
                        usageTrend = try {
                            TrendDirection.valueOf(us["usageTrend"] as? String ?: "STABLE")
                        } catch (e: Exception) { TrendDirection.STABLE }
                    )
                }

                val dailyInsightsData = data["dailyInsights"] as? Map<String, Any>
                val dailyInsights = dailyInsightsData?.let { di ->
                    DailyInsights(
                        mostUsedApp = di["mostUsedApp"] as? String,
                        peakActivityHour = di["peakActivityHour"] as? Int ?: 12,
                        totalAppsUsed = di["totalAppsUsed"] as? Int ?: 0,
                        averageSessionLength = 0,
                        earlyMorningActivity = di["earlyMorningActivity"] as? Boolean ?: false,
                        lateNightActivity = di["lateNightActivity"] as? Boolean ?: false
                    )
                }

                ContextualStateReport(
                    childDeviceId = childDeviceId,
                    childName = data["childName"] as? String ?: "Child",
                    timestamp = data["timestamp"] as? Long ?: System.currentTimeMillis(),
                    currentForegroundApp = currentForegroundApp,
                    recentAppActivity = emptyList(),
                    notificationSummary = notificationSummary ?: NotificationSummary(0, 0, emptyList(), TrendDirection.STABLE),
                    usageSummary = usageSummary ?: UsageSummary(0, 0, emptyList(), TrendDirection.STABLE),
                    dailyInsights = dailyInsights ?: DailyInsights(null, 12, 0, 0, false, false)
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun startContextualSync() {
        val intent = Intent(context, ContextualSyncService::class.java)
        context.startForegroundService(intent)
    }

    fun startLocationTracking() {
        val intent = Intent(context, LocationTrackingService::class.java)
        context.startForegroundService(intent)

        val syncIntent = Intent(context, SyncService::class.java)
        context.startForegroundService(syncIntent)
    }

    fun stopLocationTracking() {
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = "ACTION_STOP"
        }
        context.startService(intent)

        val syncIntent = Intent(context, ContextualSyncService::class.java).apply {
            action = "ACTION_STOP"
        }
        context.startService(syncIntent)
    }

    private fun formatTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000} min ago"
            diff < 86400_000 -> "${diff / 3600_000} hours ago"
            else -> "${diff / 86400_000} days ago"
        }
    }
}
