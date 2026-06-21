package com.familyguard.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyguard.app.data.local.PreferencesManager
import com.familyguard.app.data.local.dao.SosDao
import com.familyguard.app.data.local.entity.SosAlertEntity
import com.familyguard.app.domain.usecase.sos.AcknowledgeSosUseCase
import com.familyguard.app.domain.usecase.sos.CancelSosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertUi(
    val alertId: String,
    val childName: String,
    val alertType: String,
    val message: String,
    val time: String,
    val isAcknowledged: Boolean
)

data class AlertsUiState(
    val isLoading: Boolean = true,
    val alerts: List<AlertUi> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val sosDao: SosDao,
    private val acknowledgeSosUseCase: AcknowledgeSosUseCase,
    private val cancelSosUseCase: CancelSosUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        loadAlerts()
    }

    fun loadAlerts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val alerts = sosDao.getActiveAlerts()
            val formattedAlerts = alerts.map { alert ->
                AlertUi(
                    alertId = alert.alertId,
                    childName = alert.childDeviceId,
                    alertType = alert.alertType,
                    message = "${alert.alertType} triggered",
                    time = formatTime(alert.triggeredAt),
                    isAcknowledged = alert.status == "ACKNOWLEDGED"
                )
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                alerts = formattedAlerts
            )
        }
    }

    fun acknowledgeAlert(alertId: String) {
        val deviceId = preferencesManager.getDeviceId()
        viewModelScope.launch {
            acknowledgeSosUseCase(alertId, deviceId)
            loadAlerts()
        }
    }

    fun cancelAlert(alertId: String) {
        viewModelScope.launch {
            cancelSosUseCase(alertId)
            loadAlerts()
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}
