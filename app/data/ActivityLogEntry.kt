package com.example.datacollector.data

data class ActivityLogEntry(
    val timestamp: Long,
    val type: String, // e.g., "LOCATION_UPDATE", "APP_START"
    val latitude: Double? = null,
    val longitude: Double? = null,
    val deviceId: String = "device_xyz"
)