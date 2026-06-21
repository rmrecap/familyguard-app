package com.familyguard.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyguard.app.data.local.PreferencesManager
import com.familyguard.app.data.local.dao.LocationDao
import com.familyguard.app.data.local.entity.LocationSnapshotEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LocationUi(
    val snapshotId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: String,
    val batteryLevel: Int
)

data class LocationMapUiState(
    val isLoading: Boolean = true,
    val locations: List<LocationUi> = emptyList(),
    val currentLocation: LocationUi? = null,
    val error: String? = null
)

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val locationDao: LocationDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationMapUiState())
    val uiState: StateFlow<LocationMapUiState> = _uiState.asStateFlow()

    init {
        loadLocations()
    }

    fun loadLocations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val deviceId = preferencesManager.getDeviceId()
            val latestLocation = locationDao.getLatestLocation(deviceId)

            val locationHistory = locationDao.getLocationHistory(
                deviceId = deviceId,
                since = System.currentTimeMillis() - 24 * 60 * 60 * 1000
            )

            val currentLoc = latestLocation?.let {
                LocationUi(
                    snapshotId = it.snapshotId,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    accuracy = it.accuracy,
                    timestamp = formatTime(it.timestamp),
                    batteryLevel = it.batteryLevel
                )
            }

            val history = locationHistory.map {
                LocationUi(
                    snapshotId = it.snapshotId,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    accuracy = it.accuracy,
                    timestamp = formatTime(it.timestamp),
                    batteryLevel = it.batteryLevel
                )
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                currentLocation = currentLoc,
                locations = history
            )
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}
