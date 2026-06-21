package com.familyguard.app.data.remote.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        const val DEVICES_COLLECTION = "devices"
        const val LOCATIONS_COLLECTION = "locations"
        const val ALERTS_COLLECTION = "alerts"
        const val SAFE_ZONES_COLLECTION = "safe_zones"
    }

    // Device operations
    suspend fun registerDevice(deviceId: String, deviceData: Map<String, Any>) {
        firestore.collection(DEVICES_COLLECTION)
            .document(deviceId)
            .set(deviceData)
            .await()
    }

    suspend fun updateDeviceLocation(deviceId: String, locationData: Map<String, Any>) {
        firestore.collection(LOCATIONS_COLLECTION)
            .document(deviceId)
            .set(locationData)
            .await()
    }

    suspend fun getDeviceLocation(deviceId: String): Map<String, Any>? {
        return firestore.collection(LOCATIONS_COLLECTION)
            .document(deviceId)
            .get()
            .await()
            .data
    }

    // Family group operations
    suspend fun createFamilyGroup(groupId: String, groupData: Map<String, Any>) {
        firestore.collection("family_groups")
            .document(groupId)
            .set(groupData)
            .await()
    }

    suspend fun joinFamilyGroup(groupId: String, deviceId: String) {
        firestore.collection("family_groups")
            .document(groupId)
            .update("childDeviceIds", com.google.firebase.firestore.FieldValue.arrayUnion(deviceId))
            .await()
    }

    suspend fun getFamilyMembers(groupId: String): List<Map<String, Any>> {
        return firestore.collection(DEVICES_COLLECTION)
            .whereEqualTo("groupId", groupId)
            .get()
            .await()
            .documents
            .mapNotNull { it.data }
    }

    // Alert operations
    suspend fun createSosAlert(alertId: String, alertData: Map<String, Any>) {
        firestore.collection(ALERTS_COLLECTION)
            .document(alertId)
            .set(alertData)
            .await()
    }

    suspend fun getPendingAlerts(deviceId: String): List<Map<String, Any>> {
        return firestore.collection(ALERTS_COLLECTION)
            .whereEqualTo("childDeviceId", deviceId)
            .whereEqualTo("status", "TRIGGERED")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { it.data }
    }

    suspend fun acknowledgeAlert(alertId: String, acknowledgedBy: String) {
        firestore.collection(ALERTS_COLLECTION)
            .document(alertId)
            .update(
                mapOf(
                    "status" to "ACKNOWLEDGED",
                    "acknowledgedBy" to acknowledgedBy,
                    "acknowledgedAt" to com.google.firebase.Timestamp.now()
                )
            )
            .await()
    }

    suspend fun cancelAlert(alertId: String) {
        firestore.collection(ALERTS_COLLECTION)
            .document(alertId)
            .update("status", "CANCELLED")
            .await()
    }

    // Safe zone operations
    suspend fun createSafeZone(zoneId: String, zoneData: Map<String, Any>) {
        firestore.collection(SAFE_ZONES_COLLECTION)
            .document(zoneId)
            .set(zoneData)
            .await()
    }

    suspend fun getSafeZones(groupId: String): List<Map<String, Any>> {
        return firestore.collection(SAFE_ZONES_COLLECTION)
            .whereEqualTo("groupId", groupId)
            .whereEqualTo("isActive", true)
            .get()
            .await()
            .documents
            .mapNotNull { it.data }
    }

    suspend fun updateSafeZone(zoneId: String, zoneData: Map<String, Any>) {
        firestore.collection(SAFE_ZONES_COLLECTION)
            .document(zoneId)
            .update(zoneData)
            .await()
    }

    suspend fun deleteSafeZone(zoneId: String) {
        firestore.collection(SAFE_ZONES_COLLECTION)
            .document(zoneId)
            .delete()
            .await()
    }
}
