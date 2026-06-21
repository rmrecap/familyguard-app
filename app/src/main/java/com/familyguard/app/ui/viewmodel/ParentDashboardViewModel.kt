package com.familyguard.app.ui.viewmodel

import android.content.Intent
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyguard.app.data.local.PreferencesManager
import com.familyguard.app.data.local.dao.DeviceProfileDao
import com.familyguard.app.data.local.dao.LocationDao
import com.familyguard.app.data.local.entity.DeviceProfileEntity
import com.familyguard.app.domain.model.DeviceRole
import com.familyguard.app.service.LocationTrackingService
import com.familyguard.app.service.SyncService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChildUi(
    val deviceId: String,
    val name: String,
    val lastSeen: String,
    val isOnline: Boolean,
    val lastLocation: String
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

    init {
        loadDashboard()
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

                ChildUi(
                    deviceId = device.deviceId,
                    name = device.deviceName,
                    lastSeen = timeAgo,
                    isOnline = isOnline,
                    lastLocation = if (latestLocation != null) {
                        "${String.format("%.4f", latestLocation.latitude)}, ${String.format("%.4f", latestLocation.longitude)}"
                    } else "Unknown"
                )
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                children = children,
                inviteCode = inviteCode
            )
        }
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
