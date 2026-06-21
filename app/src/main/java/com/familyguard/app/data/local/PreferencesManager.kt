package com.familyguard.app.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "familyguard_secure_prefs", Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_FAMILY_GROUP_ID = "family_group_id"
        private const val KEY_INVITE_CODE = "invite_code"
        private const val KEY_IS_ONBOARDED = "is_onboarded"
        private const val KEY_CHILD_DEVICE_ID = "child_device_id"
    }

    fun saveDeviceId(deviceId: String) {
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
    }

    fun getDeviceId(): String = prefs.getString(KEY_DEVICE_ID, "") ?: ""

    fun saveUserRole(role: String) {
        prefs.edit().putString(KEY_USER_ROLE, role).apply()
    }

    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, "") ?: ""

    fun saveFamilyGroupId(groupId: String) {
        prefs.edit().putString(KEY_FAMILY_GROUP_ID, groupId).apply()
    }

    fun getFamilyGroupId(): String = prefs.getString(KEY_FAMILY_GROUP_ID, "") ?: ""

    fun saveInviteCode(code: String) {
        prefs.edit().putString(KEY_INVITE_CODE, code).apply()
    }

    fun getInviteCode(): String = prefs.getString(KEY_INVITE_CODE, "") ?: ""

    fun setOnboarded() {
        prefs.edit().putBoolean(KEY_IS_ONBOARDED, true).apply()
    }

    fun isOnboarded(): Boolean = prefs.getBoolean(KEY_IS_ONBOARDED, false)

    fun saveChildDeviceId(deviceId: String) {
        prefs.edit().putString(KEY_CHILD_DEVICE_ID, deviceId).apply()
    }

    fun getChildDeviceId(): String = prefs.getString(KEY_CHILD_DEVICE_ID, "") ?: ""

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
