package com.familyguard.app.domain.usecase.consent

import com.familyguard.app.data.local.dao.ConsentDao
import com.familyguard.app.data.local.entity.ConsentRecordEntity
import com.familyguard.app.domain.model.ConsentRecord
import com.familyguard.app.domain.model.Feature
import com.familyguard.app.security.AuditAction
import com.familyguard.app.security.AuditLogger
import com.familyguard.app.security.Actor
import java.time.Instant
import javax.inject.Inject

class GrantConsentUseCase @Inject constructor(
    private val consentDao: ConsentDao,
    private val auditLogger: AuditLogger
) {
    suspend operator fun invoke(
        feature: Feature,
        childDeviceId: String,
        grantedBy: String
    ): Result<Unit> {
        return try {
            val record = ConsentRecordEntity(
                featureId = feature.id,
                featureName = feature.displayName,
                description = feature.description,
                grantedAt = System.currentTimeMillis(),
                revokedAt = null,
                consentVersion = "1.0",
                grantedBy = grantedBy,
                childDeviceId = childDeviceId,
                isActive = true
            )

            consentDao.insertConsent(record)

            auditLogger.log(
                action = AuditAction.CONSENT_GRANTED,
                actor = Actor.CHILD,
                details = "Consent granted for: ${feature.displayName}"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class RevokeConsentUseCase @Inject constructor(
    private val consentDao: ConsentDao,
    private val auditLogger: AuditLogger
) {
    suspend operator fun invoke(
        featureId: String,
        childDeviceId: String
    ): Result<Unit> {
        return try {
            consentDao.revokeConsent(
                featureId = featureId,
                childId = childDeviceId,
                revokedAt = System.currentTimeMillis()
            )

            auditLogger.log(
                action = AuditAction.CONSENT_REVOKED,
                actor = Actor.CHILD,
                details = "Consent revoked for feature: $featureId"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetConsentStatusUseCase @Inject constructor(
    private val consentDao: ConsentDao
) {
    suspend operator fun invoke(childDeviceId: String): List<ConsentRecord> {
        return consentDao.getActiveConsents(childDeviceId).map { entity ->
            ConsentRecord(
                featureId = entity.featureId,
                featureName = entity.featureName,
                description = entity.description,
                grantedAt = Instant.ofEpochMilli(entity.grantedAt),
                revokedAt = entity.revokedAt?.let { Instant.ofEpochMilli(it) },
                consentVersion = entity.consentVersion,
                grantedBy = entity.grantedBy,
                childDeviceId = entity.childDeviceId,
                isActive = entity.isActive
            )
        }
    }

    suspend fun isFeatureEnabled(childDeviceId: String, featureId: String): Boolean {
        val consent = consentDao.getConsentForFeature(childDeviceId, featureId)
        return consent?.isActive == true
    }
}
