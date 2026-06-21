package com.familyguard.app.security

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DataValidatorTest {

    private lateinit var dataValidator: DataValidator

    @Before
    fun setUp() {
        dataValidator = DataValidator()
    }

    @Test
    fun `validateData returns Valid for allowed metadata`() {
        val data = mapOf(
            "packageName" to "com.whatsapp",
            "appName" to "WhatsApp",
            "usageTimeMinutes" to "30",
            "notificationCount" to "5"
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Valid)
    }

    @Test
    fun `validateData returns Invalid for blacklisted pattern in value`() {
        val data = mapOf(
            "packageName" to "com.whatsapp",
            "message_content" to "Hello, how are you?"
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Invalid)
        assertEquals("message_content", (result as DataValidator.ValidationResult.Invalid).blockedField)
    }

    @Test
    fun `validateData returns Invalid for disallowed field`() {
        val data = mapOf(
            "packageName" to "com.whatsapp",
            "sms_content" to "Private message"
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Invalid)
    }

    @Test
    fun `validateData returns Invalid for phone number pattern`() {
        val data = mapOf(
            "phoneNumber" to "+15551234567"
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Invalid)
    }

    @Test
    fun `validateData returns Invalid for email pattern`() {
        val data = mapOf(
            "emailAddress" to "user@example.com"
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Invalid)
    }

    @Test
    fun `validateData returns Invalid for URL pattern`() {
        val data = mapOf(
            "websiteUrl" to "https://www.example.com"
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Invalid)
    }

    @Test
    fun `validateData returns Invalid for long string content`() {
        val longString = "This is a very long string that contains more than fifty characters and could be message content"
        val data = mapOf(
            "packageName" to longString
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Invalid)
    }

    @Test
    fun `validateValue returns Valid for safe value`() {
        val result = dataValidator.validateValue("WhatsApp", "appName", "Test")
        assertTrue(result is DataValidator.ValidationResult.Valid)
    }

    @Test
    fun `validateValue returns Invalid for forbidden pattern in value`() {
        val result = dataValidator.validateValue("Hello world", "message_content", "Test")
        assertTrue(result is DataValidator.ValidationResult.Invalid)
    }

    @Test
    fun `validateFieldName returns Valid for allowed field`() {
        val result = dataValidator.validateFieldName("packageName", "Test")
        assertTrue(result is DataValidator.ValidationResult.Valid)
    }

    @Test
    fun `validateFieldName returns Invalid for unknown field`() {
        val result = dataValidator.validateFieldName("unknownField", "Test")
        assertTrue(result is DataValidator.ValidationResult.Invalid)
    }

    @Test
    fun `sanitizeData removes disallowed fields`() {
        val data = mapOf(
            "packageName" to "com.whatsapp",
            "unknownField" to "value",
            "appName" to "WhatsApp"
        )

        val sanitized = dataValidator.sanitizeData(data)
        assertEquals(2, sanitized.size)
        assertTrue(sanitized.containsKey("packageName"))
        assertTrue(sanitized.containsKey("appName"))
        assertFalse(sanitized.containsKey("unknownField"))
    }

    @Test
    fun `validateData returns Invalid for contact_name pattern`() {
        val data = mapOf(
            "senderName" to "John Doe"
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Invalid)
    }

    @Test
    fun `validateData returns Invalid for photo pattern`() {
        val data = mapOf(
            "mediaFile" to "photo.jpg"
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Invalid)
    }

    @Test
    fun `validateData returns Invalid for password pattern`() {
        val data = mapOf(
            "password" to "secret123"
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Invalid)
    }

    @Test
    fun `validateData returns Invalid for Base64 string`() {
        val base64String = "SGVsbG8gV29ybGQhIFRoaXMgaXMgYSB0ZXN0IHN0cmluZw=="
        val data = mapOf(
            "encodedData" to base64String
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Invalid)
    }

    @Test
    fun `getValidationReport contains expected information`() {
        val data = mapOf(
            "packageName" to "com.whatsapp"
        )

        val report = dataValidator.getValidationReport(data, "Test")
        assertTrue(report.contains("Data Validation Report"))
        assertTrue(report.contains("Test"))
        assertTrue(report.contains("Valid"))
    }

    @Test
    fun `validateData allows numeric usage time fields`() {
        val data = mapOf(
            "usageTimeMinutes" to "30",
            "usageTimeMs" to "1800000",
            "usageTimeSeconds" to "1800",
            "lastUsedTimestamp" to "1706745600000",
            "launchCount" to "5"
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Valid)
    }

    @Test
    fun `validateData allows aggregated metrics`() {
        val data = mapOf(
            "totalNotificationsToday" to "45",
            "notificationsLastHour" to "12",
            "totalScreenTimeToday" to "180",
            "screenTimeLastHour" to "30"
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Valid)
    }

    @Test
    fun `validateData allows trend indicators`() {
        val data = mapOf(
            "notificationTrend" to "INCREASING",
            "usageTrend" to "STABLE"
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Valid)
    }

    @Test
    fun `validateData allows daily insights`() {
        val data = mapOf(
            "mostUsedApp" to "WhatsApp",
            "peakActivityHour" to "14",
            "totalAppsUsed" to "8",
            "averageSessionLength" to "25",
            "earlyMorningActivity" to "false",
            "lateNightActivity" to "true"
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Valid)
    }

    @Test
    fun `validateData allows device metadata`() {
        val data = mapOf(
            "childDeviceId" to "device123",
            "childName" to "Sarah",
            "timestamp" to "1706745600000",
            "collectedAt" to "1706745600000"
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Valid)
    }

    @Test
    fun `validateData allows anomaly detection results`() {
        val data = mapOf(
            "anomalyType" to "NOTIFICATION_SPIKE",
            "severity" to "HIGH",
            "description" to "High notification activity detected",
            "alertId" to "alert123",
            "detectedAt" to "1706745600000"
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Valid)
    }

    @Test
    fun `validateData allows consent metadata`() {
        val data = mapOf(
            "consentType" to "USAGE_STATS",
            "consentVersion" to "2.0",
            "grantedAt" to "1706745600000",
            "revokedAt" to "",
            "isActive" to "true"
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Valid)
    }

    @Test
    fun `validateData allows call log metadata`() {
        val data = mapOf(
            "durationSeconds" to "120",
            "callType" to "INCOMING",
            "totalCallsToday" to "5",
            "callsLastHour" to "2"
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Valid)
    }
}
