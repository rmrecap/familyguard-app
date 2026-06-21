package com.familyguard.app.ui.viewmodel

import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyguard.app.data.local.PreferencesManager
import com.familyguard.app.data.local.dao.ConsentDao
import com.familyguard.app.data.local.dao.DeviceProfileDao
import com.familyguard.app.data.local.dao.FamilyGroupDao
import com.familyguard.app.data.local.entity.ConsentRecordEntity
import com.familyguard.app.data.local.entity.DeviceProfileEntity
import com.familyguard.app.data.local.entity.FamilyGroupEntity
import com.familyguard.app.data.remote.api.SyncApi
import com.familyguard.app.data.remote.api.DeviceRegistrationRequest
import com.familyguard.app.data.remote.api.FamilyPairingRequest
import com.familyguard.app.domain.model.DeviceRole
import com.familyguard.app.domain.model.Feature
import com.familyguard.app.security.AuditAction
import com.familyguard.app.security.AuditLogger
import com.familyguard.app.security.Actor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class RegistrationUiState(
    val isLoading: Boolean = false,
    val isRegistered: Boolean = false,
    val isPaired: Boolean = false,
    val error: String? = null,
    val deviceId: String = "",
    val role: DeviceRole? = null,
    val inviteCode: String = "",
    val groupId: String = "",
    val consentsGranted: Boolean = false
)

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val deviceProfileDao: DeviceProfileDao,
    private val familyGroupDao: FamilyGroupDao,
    private val consentDao: ConsentDao,
    private val syncApi: SyncApi,
    private val auditLogger: AuditLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        val savedDeviceId = preferencesManager.getDeviceId()
        val savedRole = preferencesManager.getUserRole()
        val isOnboarded = preferencesManager.isOnboarded()

        if (isOnboarded && savedDeviceId.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                deviceId = savedDeviceId,
                role = if (savedRole == "PARENT") DeviceRole.PARENT else DeviceRole.CHILD,
                isRegistered = true,
                isPaired = preferencesManager.getFamilyGroupId().isNotEmpty(),
                inviteCode = preferencesManager.getInviteCode(),
                groupId = preferencesManager.getFamilyGroupId()
            )
        }
    }

    fun selectRole(role: DeviceRole) {
        _uiState.value = _uiState.value.copy(role = role, isLoading = true)
        viewModelScope.launch {
            try {
                val deviceId = UUID.randomUUID().toString()
                val deviceName = Build.MODEL
                val deviceModel = Build.MANUFACTURER
                val osVersion = "Android ${Build.VERSION.RELEASE}"

                // Register with server
                val response = syncApi.registerDevice(
                    DeviceRegistrationRequest(
                        deviceName = deviceName,
                        deviceModel = deviceModel,
                        osVersion = osVersion,
                        fcmToken = "",
                        role = role.name
                    )
                )

                if (response.isSuccessful) {
                    val serverData = response.body()?.data as? Map<*, *>
                    val serverDeviceId = serverData?.get("device_id") as? String ?: deviceId

                    // Save locally
                    preferencesManager.saveDeviceId(serverDeviceId)
                    preferencesManager.saveUserRole(role.name)

                    // Save to Room
                    deviceProfileDao.insertDevice(
                        DeviceProfileEntity(
                            deviceId = serverDeviceId,
                            userId = "",
                            role = role.name,
                            deviceName = deviceName,
                            deviceModel = deviceModel,
                            osVersion = osVersion,
                            fcmToken = "",
                            registeredAt = System.currentTimeMillis(),
                            lastSeenAt = System.currentTimeMillis()
                        )
                    )

                    auditLogger.log(
                        action = AuditAction.DEVICE_REGISTERED,
                        actor = Actor.SYSTEM,
                        details = "Device registered as ${role.name}"
                    )

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRegistered = true,
                        deviceId = serverDeviceId,
                        role = role
                    )
                } else {
                    // Fallback: use local UUID if server fails
                    preferencesManager.saveDeviceId(deviceId)
                    preferencesManager.saveUserRole(role.name)

                    deviceProfileDao.insertDevice(
                        DeviceProfileEntity(
                            deviceId = deviceId,
                            userId = "",
                            role = role.name,
                            deviceName = deviceName,
                            deviceModel = deviceModel,
                            osVersion = osVersion,
                            fcmToken = "",
                            registeredAt = System.currentTimeMillis(),
                            lastSeenAt = System.currentTimeMillis()
                        )
                    )

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRegistered = true,
                        deviceId = deviceId,
                        role = role
                    )
                }
            } catch (e: Exception) {
                // Fallback: use local UUID
                val deviceId = UUID.randomUUID().toString()
                preferencesManager.saveDeviceId(deviceId)
                preferencesManager.saveUserRole(role.name)

                deviceProfileDao.insertDevice(
                    DeviceProfileEntity(
                        deviceId = deviceId,
                        userId = "",
                        role = role.name,
                        deviceName = Build.MODEL,
                        deviceModel = Build.MANUFACTURER,
                        osVersion = "Android ${Build.VERSION.RELEASE}",
                        fcmToken = "",
                        registeredAt = System.currentTimeMillis(),
                        lastSeenAt = System.currentTimeMillis()
                    )
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRegistered = true,
                    deviceId = deviceId,
                    role = role
                )
            }
        }
    }

    fun createFamily() {
        val deviceId = preferencesManager.getDeviceId()
        if (deviceId.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = syncApi.createFamily(
                    DeviceRegistrationRequest(
                        deviceName = Build.MODEL,
                        deviceModel = Build.MANUFACTURER,
                        osVersion = "Android ${Build.VERSION.RELEASE}",
                        fcmToken = "",
                        role = "PARENT"
                    )
                )

                if (response.isSuccessful) {
                    val data = response.body()?.data as? Map<*, *>
                    val groupId = data?.get("groupId") as? String ?: UUID.randomUUID().toString()
                    val inviteCode = data?.get("inviteCode") as? String ?: generateLocalCode()

                    preferencesManager.saveFamilyGroupId(groupId)
                    preferencesManager.saveInviteCode(inviteCode)

                    familyGroupDao.insertFamilyGroup(
                        FamilyGroupEntity(
                            groupId = groupId,
                            parentDeviceIds = deviceId,
                            childDeviceIds = "",
                            createdAt = System.currentTimeMillis(),
                            inviteCode = inviteCode
                        )
                    )

                    preferencesManager.setOnboarded()

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        inviteCode = inviteCode,
                        groupId = groupId,
                        isPaired = true
                    )
                } else {
                    val groupId = UUID.randomUUID().toString()
                    val inviteCode = generateLocalCode()

                    preferencesManager.saveFamilyGroupId(groupId)
                    preferencesManager.saveInviteCode(inviteCode)

                    familyGroupDao.insertFamilyGroup(
                        FamilyGroupEntity(
                            groupId = groupId,
                            parentDeviceIds = deviceId,
                            childDeviceIds = "",
                            createdAt = System.currentTimeMillis(),
                            inviteCode = inviteCode
                        )
                    )

                    preferencesManager.setOnboarded()

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        inviteCode = inviteCode,
                        groupId = groupId,
                        isPaired = true
                    )
                }
            } catch (e: Exception) {
                val groupId = UUID.randomUUID().toString()
                val inviteCode = generateLocalCode()

                preferencesManager.saveFamilyGroupId(groupId)
                preferencesManager.saveInviteCode(inviteCode)

                familyGroupDao.insertFamilyGroup(
                    FamilyGroupEntity(
                        groupId = groupId,
                        parentDeviceIds = deviceId,
                        childDeviceIds = "",
                        createdAt = System.currentTimeMillis(),
                        inviteCode = inviteCode
                    )
                )

                preferencesManager.setOnboarded()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    inviteCode = inviteCode,
                    groupId = groupId,
                    isPaired = true
                )
            }
        }
    }

    fun joinFamily(inviteCode: String) {
        val deviceId = preferencesManager.getDeviceId()
        if (deviceId.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = syncApi.joinFamily(
                    FamilyPairingRequest(
                        inviteCode = inviteCode,
                        deviceId = deviceId
                    )
                )

                if (response.isSuccessful) {
                    val data = response.body()?.data as? Map<*, *>
                    val groupId = data?.get("groupId") as? String ?: ""

                    preferencesManager.saveFamilyGroupId(groupId)

                    preferencesManager.setOnboarded()

                    auditLogger.log(
                        action = AuditAction.DEVICE_REGISTERED,
                        actor = Actor.CHILD,
                        details = "Child joined family group: $groupId"
                    )

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isPaired = true,
                        groupId = groupId
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Invalid invite code"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Connection failed. Please try again."
                )
            }
        }
    }

    fun grantConsents(features: List<Feature>) {
        val deviceId = preferencesManager.getDeviceId()
        if (deviceId.isEmpty()) return

        viewModelScope.launch {
            features.forEach { feature ->
                consentDao.insertConsent(
                    ConsentRecordEntity(
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
            }

            preferencesManager.setOnboarded()

            _uiState.value = _uiState.value.copy(
                consentsGranted = true,
                isPaired = true
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun generateLocalCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
