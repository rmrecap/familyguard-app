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
    fun `validateData returns Invalid for blacklisted value pattern`() {
        // PII (phone numbers) is still blocked even when the field name is allowed
        val data = mapOf(
            "packageName" to "+15551234567"
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Invalid)
    }

    @Test
    fun `validateData returns Invalid for disallowed field name`() {
        // Field name not on the allowlist is rejected (the field-name gate stays)
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
        // password remains on the blacklist
        val result = dataValidator.validateValue("mysecret", "password", "Test")
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
    fun `validateData returns Invalid for file_content pattern`() {
        // file_content remains blacklisted (raw file/media content identifiers)
        val data = mapOf(
            "appName" to "file_content"
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

    @Test
    fun `validateData allows communication metadata fields`() {
        val data = mapOf(
            "eventCategory" to "msg",
            "hasMedia" to "true",
            "mediaCount" to "3",
            "eventCount" to "12",
            "totalEventsToday" to "45",
            "eventsLastHour" to "7",
            "mediaEventsLastHour" to "2",
            "communicationTrend" to "INCREASING"
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Valid)
    }

    @Test
    fun `validateData allows short notification snippet`() {
        val data = mapOf(
            "packageName" to "com.whatsapp",
            "snippet" to "New message"
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Valid)
    }

    @Test
    fun `validateData rejects snippet containing a phone number`() {
        // PII regex still strips phone numbers out of the relaxed snippet field
        val data = mapOf(
            "snippet" to "Call me +15551234567"
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Invalid)
    }

    @Test
    fun `validateData rejects snippet longer than max value length`() {
        val overLimit = "a".repeat(DataValidator.MAX_VALUE_LENGTH + 1)
        val data = mapOf(
            "snippet" to overLimit
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Invalid)
    }

    @Test
    fun `validateData rejects email that sneaks into allowed field`() {
        val data = mapOf(
            "appName" to "user@example.com"
        )

        val result = dataValidator.validateData(data, "Test")
        assertTrue(result is DataValidator.ValidationResult.Invalid)
    }
}
