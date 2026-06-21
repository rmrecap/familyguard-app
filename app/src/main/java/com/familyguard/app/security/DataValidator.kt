package com.familyguard.app.security

import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataValidator - The Final Firewall for Privacy Enforcement
 * 
 * This component enforces the "No Content" rule by programmatically checking
 * all data before it is encrypted or synced. It performs:
 * 1. Blacklist Check: Ensures no forbidden patterns appear in data
 * 2. Field Whitelist Check: Only explicitly allowed fields pass through
 * 3. Pattern Detection: Catches attempts to sneak in private content
 * 
 * FAILURE MODE: This validator FAILS SAFE - if anything suspicious is detected,
 * the data is BLOCKED and an alert is raised.
 */
@Singleton
class DataValidator @Inject constructor() {

    companion object {
        /**
         * BLACKLISTED PATTERNS - These patterns MUST NEVER appear in collected data
         * 
         * If any of these patterns are detected in any data structure,
         * the validation FAILS and the data is BLOCKED.
         */
        val FORBIDDEN_PATTERNS = setOf(
            // Message content patterns
            "message_content",
            "message_text",
            "sms_content",
            "chat_content",
            "text_content",
            "body_text",
            "message_body",
            "conversation",
            "thread_content",
            
            // Contact patterns
            "contact_name",
            "contact_number",
            "phone_number",
            "email_address",
            "contact_list",
            "address_book",
            "sender_name",
            "recipient_name",
            
            // Media patterns
            "photo",
            "image",
            "video",
            "audio",
            "media_file",
            "attachment",
            "file_content",
            "document",
            
            // Browsing patterns
            "url",
            "website",
            "browser_history",
            "search_query",
            "browsing_data",
            
            // Password/credential patterns
            "password",
            "credential",
            "token",
            "secret",
            "api_key",
            "private_key",
            
            // Location patterns (beyond what's explicitly collected)
            "home_address",
            "work_address",
            "gps_raw",
            
            // Private data patterns
            "financial",
            "medical",
            "health",
            "insurance",
            "social_security",
            "credit_card",
            "bank_account"
        )

        /**
         * ALLOWED FIELDS - These are the ONLY fields permitted in collected data
         * 
         * Any field NOT in this list is BLOCKED.
         * New fields require mandatory Legal Review Gate approval.
         */
        val ALLOWED_FIELDS = setOf(
            // App identification
            "packageName",
            "appName",
            
            // Usage metrics
            "usageTimeMinutes",
            "usageTimeMs",
            "usageTimeSeconds",
            "lastUsedTimestamp",
            "launchCount",
            
            // Notification metrics
            "notificationCount",
            "hourOfDay",
            "dayOfWeek",
            
            // Aggregated metrics
            "totalNotificationsToday",
            "notificationsLastHour",
            "totalScreenTimeToday",
            "screenTimeLastHour",
            
            // Trend indicators
            "notificationTrend",
            "usageTrend",
            
            // Daily insights
            "mostUsedApp",
            "peakActivityHour",
            "totalAppsUsed",
            "averageSessionLength",
            "earlyMorningActivity",
            "lateNightActivity",
            
            // Device metadata (non-sensitive)
            "childDeviceId",
            "childName",
            "timestamp",
            "collectedAt",
            
            // Anomaly detection results
            "anomalyType",
            "severity",
            "description",
            "alertId",
            "detectedAt",
            
            // Consent metadata
            "consentType",
            "consentVersion",
            "grantedAt",
            "revokedAt",
            "isActive",

            // Call log metadata
            "durationSeconds",
            "callType",
            "totalCallsToday",
            "callsLastHour"
        )

        /**
         * REGEX PATTERNS for detecting content-like data
         * These patterns catch attempts to sneak in private content
         */
        val CONTENT_DETECTION_PATTERNS = setOf(
            // Message-like content (long strings that look like messages)
            Regex("""^.{50,}$"""),  // Strings longer than 50 chars (likely content)
            
            // Phone number patterns
            Regex("""\+?\d{10,15}"""),  // Phone numbers
            
            // Email patterns
            Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}"""),  // Email addresses
            
            // URL patterns
            Regex("""https?://[^\s]+"""),  // URLs
            
            // Base64 encoded data (could be encrypted content)
            Regex("""^[A-Za-z0-9+/]{40,}={0,2}$"""),  // Base64 strings
            
            // JSON-like structures (could contain nested content)
            Regex("""\{[^}]*"[^"]*":\s*"[^"]{20,}"[^}]*\}"""),  // JSON with long values
        )
    }

    /**
     * Validation result types
     */
    sealed class ValidationResult {
        data object Valid : ValidationResult()
        data class Invalid(val reason: String, val blockedField: String? = null) : ValidationResult()
        data class Warning(val reason: String) : ValidationResult()
    }

    /**
     * Main validation entry point
     * Validates a data map before it's allowed to proceed
     */
    fun validateData(data: Map<String, Any>, context: String = ""): ValidationResult {
        // Step 1: Check for blacklisted patterns in all values
        val blacklistResult = checkBlacklistedPatterns(data, context)
        if (blacklistResult is ValidationResult.Invalid) return blacklistResult

        // Step 2: Check that only allowed fields are present
        val whitelistResult = checkAllowedFields(data, context)
        if (whitelistResult is ValidationResult.Invalid) return whitelistResult

        // Step 3: Check for content-like patterns
        val contentResult = checkContentPatterns(data, context)
        if (contentResult is ValidationResult.Invalid) return contentResult

        return ValidationResult.Valid
    }

    /**
     * Validates a single string value
     */
    fun validateValue(value: String, fieldName: String, context: String = ""): ValidationResult {
        // Check blacklist
        for (pattern in FORBIDDEN_PATTERNS) {
            if (value.lowercase().contains(pattern.lowercase())) {
                return ValidationResult.Invalid(
                    reason = "FORBIDDEN PATTERN DETECTED: '$pattern' found in field '$fieldName'",
                    blockedField = fieldName
                )
            }
        }

        // Check content patterns
        for (pattern in CONTENT_DETECTION_PATTERNS) {
            if (pattern.matches(value)) {
                return ValidationResult.Invalid(
                    reason = "CONTENT-LIKE DATA DETECTED: Field '$fieldName' contains data that looks like private content",
                    blockedField = fieldName
                )
            }
        }

        return ValidationResult.Valid
    }

    /**
     * Validates that a field name is in the allowed list
     */
    fun validateFieldName(fieldName: String, context: String = ""): ValidationResult {
        if (fieldName !in ALLOWED_FIELDS) {
            return ValidationResult.Invalid(
                reason = "DISALLOWED FIELD: '$fieldName' is not in the allowed fields whitelist. New fields require Legal Review Gate approval.",
                blockedField = fieldName
            )
        }
        return ValidationResult.Valid
    }

    private fun checkBlacklistedPatterns(data: Map<String, Any>, context: String): ValidationResult {
        for ((key, value) in data) {
            val valueStr = value.toString()
            
            for (pattern in FORBIDDEN_PATTERNS) {
                if (valueStr.lowercase().contains(pattern.lowercase())) {
                    return ValidationResult.Invalid(
                        reason = "FORBIDDEN PATTERN DETECTED: '$pattern' found in field '$key' (context: $context)",
                        blockedField = key
                    )
                }
            }
        }
        return ValidationResult.Valid
    }

    private fun checkAllowedFields(data: Map<String, Any>, context: String): ValidationResult {
        for (key in data.keys) {
            if (key !in ALLOWED_FIELDS) {
                return ValidationResult.Invalid(
                    reason = "DISALLOWED FIELD: '$key' is not in the allowed fields whitelist (context: $context). New fields require Legal Review Gate approval.",
                    blockedField = key
                )
            }
        }
        return ValidationResult.Valid
    }

    private fun checkContentPatterns(data: Map<String, Any>, context: String): ValidationResult {
        for ((key, value) in data) {
            val valueStr = value.toString()
            
            // Check for content-like patterns (skip short strings)
            if (valueStr.length > 50) {
                return ValidationResult.Invalid(
                    reason = "POTENTIAL CONTENT: Field '$key' contains a string longer than 50 characters, which may contain private content (context: $context)",
                    blockedField = key
                )
            }

            // Check for specific content patterns
            for (pattern in CONTENT_DETECTION_PATTERNS) {
                if (pattern.matches(valueStr)) {
                    return ValidationResult.Invalid(
                        reason = "CONTENT-LIKE DATA: Field '$key' matches pattern that may contain private content (context: $context)",
                        blockedField = key
                    )
                }
            }
        }
        return ValidationResult.Valid
    }

    /**
     * Sanitizes a data map by removing any disallowed fields
     * Returns only the allowed fields
     */
    fun sanitizeData(data: Map<String, Any>): Map<String, Any> {
        return data.filter { (key, _) -> key in ALLOWED_FIELDS }
    }

    /**
     * Gets a report of what was validated
     */
    fun getValidationReport(data: Map<String, Any>, context: String = ""): String {
        val sb = StringBuilder()
        sb.appendLine("=== Data Validation Report ===")
        sb.appendLine("Context: $context")
        sb.appendLine("Fields in input: ${data.keys.size}")
        sb.appendLine("Allowed fields: ${ALLOWED_FIELDS.size}")
        sb.appendLine("Blacklisted patterns: ${FORBIDDEN_PATTERNS.size}")
        sb.appendLine("Content detection patterns: ${CONTENT_DETECTION_PATTERNS.size}")
        
        val result = validateData(data, context)
        sb.appendLine("Result: ${result::class.simpleName}")
        if (result is ValidationResult.Invalid) {
            sb.appendLine("Reason: ${result.reason}")
            sb.appendLine("Blocked field: ${result.blockedField}")
        }
        
        return sb.toString()
    }
}
