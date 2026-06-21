package com.familyguard.app.domain.usecase.security

import com.familyguard.app.data.local.dao.ConsentDao
import com.familyguard.app.security.AuditAction
import com.familyguard.app.security.AuditLogger
import com.familyguard.app.security.Actor
import com.familyguard.app.service.LocationTrackingService
import com.familyguard.app.service.SyncService
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ActivateKillSwitchUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val consentDao: ConsentDao,
    private val auditLogger: AuditLogger
) {
    suspend operator fun invoke(childDeviceId: String): Result<Unit> {
        return try {
            context.stopService(android.content.Intent(context, LocationTrackingService::class.java))
            context.stopService(android.content.Intent(context, SyncService::class.java))

            val features = listOf(
                "location_sharing", "sos", "geofence", "contact_whitelist", "screen_time"
            )
            for (featureId in features) {
                consentDao.revokeConsent(
                    featureId = featureId,
                    childId = childDeviceId,
                    revokedAt = System.currentTimeMillis()
                )
            }

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.cancelAll()

            auditLogger.log(
                action = AuditAction.KILL_SWITCH_ACTIVATED,
                actor = Actor.CHILD,
                details = "Kill switch activated - all monitoring stopped"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetAuditLogUseCase @Inject constructor(
    private val auditLogger: AuditLogger
) {
    operator fun invoke(): List<com.familyguard.app.security.AuditEntry> {
        return auditLogger.getAuditLog()
    }
}
