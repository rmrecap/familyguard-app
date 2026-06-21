package com.familyguard.app.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class SyncRequest(
    val deviceId: String,
    val payload: String,
    val timestamp: Long,
    val signature: String
)

data class SyncResponse(
    val success: Boolean,
    val message: String?,
    val data: Any?
)

data class DeviceRegistrationRequest(
    val deviceName: String,
    val deviceModel: String,
    val osVersion: String,
    val fcmToken: String,
    val role: String
)

data class FamilyPairingRequest(
    val inviteCode: String,
    val deviceId: String
)

interface SyncApi {

    @POST("device/register")
    suspend fun registerDevice(@Body request: DeviceRegistrationRequest): Response<SyncResponse>

    @POST("family/create")
    suspend fun createFamily(@Body request: DeviceRegistrationRequest): Response<SyncResponse>

    @POST("family/join")
    suspend fun joinFamily(@Body request: FamilyPairingRequest): Response<SyncResponse>

    @GET("family/members/{groupId}")
    suspend fun getFamilyMembers(@Path("groupId") groupId: String): Response<SyncResponse>

    @POST("sync/location")
    suspend fun syncLocation(@Body request: SyncRequest): Response<SyncResponse>

    @POST("sync/sos")
    suspend fun syncSosAlert(@Body request: SyncRequest): Response<SyncResponse>

    @POST("sync/geofence")
    suspend fun syncGeofenceStatus(@Body request: SyncRequest): Response<SyncResponse>

    @POST("device/heartbeat/{deviceId}")
    suspend fun sendHeartbeat(@Path("deviceId") deviceId: String): Response<SyncResponse>

    @GET("sync/pull/{deviceId}")
    suspend fun pullPendingData(@Path("deviceId") deviceId: String): Response<SyncResponse>
}
