package com.familyguard.app.domain.usecase.sos

import com.familyguard.app.data.local.dao.SosDao
import com.familyguard.app.data.local.entity.SosAlertEntity
import com.familyguard.app.domain.model.AlertStatus
import com.familyguard.app.domain.model.SosType
import com.familyguard.app.security.AuditAction
import com.familyguard.app.security.AuditLogger
import com.familyguard.app.security.Actor
import java.util.UUID
import javax.inject.Inject

class TriggerSosUseCase @Inject constructor(
    private val sosDao: SosDao,
    private val auditLogger: AuditLogger
) {
    suspend operator fun invoke(
        childDeviceId: String,
        latitude: Double,
        longitude: Double,
        alertType: SosType = SosType.PANIC_BUTTON
    ): Result<String> {
        return try {
            val alertId = UUID.randomUUID().toString()

            val alert = SosAlertEntity(
                alertId = alertId,
                childDeviceId = childDeviceId,
                triggeredAt = System.currentTimeMillis(),
                latitude = latitude,
                longitude = longitude,
                alertType = alertType.name,
                status = AlertStatus.TRIGGERED.name,
                acknowledgedBy = null,
                acknowledgedAt = null
            )

            sosDao.insertAlert(alert)

            auditLogger.log(
                action = AuditAction.SOS_TRIGGERED,
                actor = Actor.CHILD,
                details = "SOS triggered: type=$alertType, location=($latitude, $longitude)"
            )

            Result.success(alertId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class AcknowledgeSosUseCase @Inject constructor(
    private val sosDao: SosDao,
    private val auditLogger: AuditLogger
) {
    suspend operator fun invoke(
        alertId: String,
        parentDeviceId: String
    ): Result<Unit> {
        return try {
            sosDao.updateAlertStatus(
                alertId = alertId,
                status = AlertStatus.ACKNOWLEDGED.name,
                by = parentDeviceId,
                at = System.currentTimeMillis()
            )

            auditLogger.log(
                action = AuditAction.SOS_ACKNOWLEDGED,
                actor = Actor.PARENT,
                details = "SOS acknowledged by parent"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class CancelSosUseCase @Inject constructor(
    private val sosDao: SosDao,
    private val auditLogger: AuditLogger
) {
    suspend operator fun invoke(
        alertId: String,
        cancelledBy: String
    ): Result<Unit> {
        return try {
            sosDao.updateAlertStatus(
                alertId = alertId,
                status = AlertStatus.CANCELLED.name,
                by = cancelledBy,
                at = System.currentTimeMillis()
            )

            auditLogger.log(
                action = AuditAction.SOS_CANCELLED,
                actor = Actor.CHILD,
                details = "SOS cancelled by child"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
