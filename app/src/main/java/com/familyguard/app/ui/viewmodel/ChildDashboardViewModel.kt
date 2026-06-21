package com.familyguard.app.ui.viewmodel

import android.content.Intent
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyguard.app.data.local.PreferencesManager
import com.familyguard.app.data.local.dao.ConsentDao
import com.familyguard.app.data.local.dao.LocationDao
import com.familyguard.app.data.local.entity.LocationSnapshotEntity
import com.familyguard.app.domain.model.Feature
import com.familyguard.app.domain.usecase.consent.RevokeConsentUseCase
import com.familyguard.app.domain.usecase.security.ActivateKillSwitchUseCase
import com.familyguard.app.domain.usecase.sos.TriggerSosUseCase
import com.familyguard.app.security.AuditAction
import com.familyguard.app.security.AuditLogger
import com.familyguard.app.security.Actor
import com.familyguard.app.service.AnomalyDetectionService
import com.familyguard.app.service.LocationTrackingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChildDashboardUiState(
    val isLoading: Boolean = false,
    val isTracking: Boolean = false,
    val childName: String = "Child",
    val monitoringSince: String = "",
    val lastSynced: String = "",
    val consents: Map<String, Boolean> = emptyMap(),
    val error: String? = null,
    val sosTriggered: Boolean = false
)

@HiltViewModel
class ChildDashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val consentDao: ConsentDao,
    private val locationDao: LocationDao,
    private val triggerSosUseCase: TriggerSosUseCase,
    private val activateKillSwitchUseCase: ActivateKillSwitchUseCase,
    private val revokeConsentUseCase: RevokeConsentUseCase,
    private val auditLogger: AuditLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChildDashboardUiState())
    val uiState: StateFlow<ChildDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            val deviceId = preferencesManager.getDeviceId()
            val consents = consentDao.getActiveConsents(deviceId)

            val consentMap = Feature.entries.associate { feature ->
                feature.id to consents.any { it.featureId == feature.id && it.isActive }
            }

            val isTracking = consentMap[Feature.LOCATION_SHARING.id] == true

            val isContextualActive = consentMap[Feature.SCREEN_TIME.id] == true || consentMap[Feature.COMMUNICATION_TRACKING.id] == true
            if (isContextualActive) {
                startContextualSync()
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isTracking = isTracking,
                consents = consentMap,
                monitoringSince = if (isTracking) formatTime(System.currentTimeMillis()) else "",
                lastSynced = formatTime(System.currentTimeMillis())
            )
        }
    }

    fun toggleFeature(feature: Feature, enabled: Boolean) {
        val deviceId = preferencesManager.getDeviceId()
        viewModelScope.launch {
            if (enabled) {
                consentDao.insertConsent(
                    com.familyguard.app.data.local.entity.ConsentRecordEntity(
                        featureId = feature.id,
                        featureName = feature.displayName,
                        description = feature.description,
                        grantedAt = System.currentTimeMillis(),
                        revokedAt = null,
                        consentVersion = "1.0",
                        grantedBy = deviceId,
                        childDeviceId = deviceId,
                        isActive = true
                    )
                )

                if (feature == Feature.LOCATION_SHARING) {
                    startLocationTracking()
                }
                if (feature == Feature.SCREEN_TIME || feature == Feature.COMMUNICATION_TRACKING) {
                    startContextualSync()
                }
            } else {
                revokeConsentUseCase(feature.id, deviceId)

                if (feature == Feature.LOCATION_SHARING) {
                    stopLocationTracking()
                }
                val activeConsents = consentDao.getActiveConsents(deviceId)
                val hasOtherActive = activeConsents.any { 
                    (it.featureId == Feature.SCREEN_TIME.id || it.featureId == Feature.COMMUNICATION_TRACKING.id) && 
                    it.featureId != feature.id && it.isActive 
                }
                if (!hasOtherActive && (feature == Feature.SCREEN_TIME || feature == Feature.COMMUNICATION_TRACKING)) {
                    stopContextualSync()
                }
            }

            loadDashboard()
        }
    }

    fun triggerSos() {
        val deviceId = preferencesManager.getDeviceId()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = triggerSosUseCase(deviceId)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        sosTriggered = true
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }

    fun activateKillSwitch() {
        val deviceId = preferencesManager.getDeviceId()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = activateKillSwitchUseCase(deviceId)
            result.fold(
                onSuccess = {
                    stopLocationTracking()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isTracking = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }

    private fun startLocationTracking() {
        val intent = Intent(context, LocationTrackingService::class.java)
        context.startForegroundService(intent)

        // Also start anomaly detection service
        val anomalyIntent = Intent(context, AnomalyDetectionService::class.java)
        context.startForegroundService(anomalyIntent)
    }

    private fun stopLocationTracking() {
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = "ACTION_STOP"
        }
        context.startService(intent)

        // Also stop anomaly detection service
        val anomalyIntent = Intent(context, AnomalyDetectionService::class.java).apply {
            action = "ACTION_STOP"
        }
        context.startService(anomalyIntent)
    }

    private fun startContextualSync() {
        val intent = Intent(context, com.familyguard.app.service.ContextualSyncService::class.java)
        context.startForegroundService(intent)
    }

    private fun stopContextualSync() {
        val intent = Intent(context, com.familyguard.app.service.ContextualSyncService::class.java).apply {
            action = "ACTION_STOP"
        }
        context.startService(intent)
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
