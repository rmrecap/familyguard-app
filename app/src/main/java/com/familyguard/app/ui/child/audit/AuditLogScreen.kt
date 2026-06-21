package com.familyguard.app.ui.child.audit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.familyguard.app.security.AuditAction
import com.familyguard.app.security.Actor
import com.familyguard.app.ui.theme.Success

data class AuditLogEntry(
    val action: String,
    val actor: String,
    val details: String,
    val timestamp: String
)

@Composable
fun AuditLogScreen(onBack: () -> Unit) {
    val auditLog = remember {
        mutableStateListOf(
            AuditLogEntry("LOCATION_ACCESSED", "SYSTEM", "Location updated: (37.774, -122.419)", "3:45 PM"),
            AuditLogEntry("DATA_VIEWED", "PARENT", "Parent checked location", "3:42 PM"),
            AuditLogEntry("CONSENT_GRANTED", "CHILD", "Consent granted for: Location Sharing", "10:30 AM"),
            AuditLogEntry("MONITORING_REENABLED", "CHILD", "Monitoring re-enabled with consent", "10:28 AM"),
            AuditLogEntry("KILL_SWITCH_ACTIVATED", "CHILD", "Kill switch activated - all monitoring stopped", "10:25 AM")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Activity Log",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "This log shows all data access and system events. It cannot be cleared by either party.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(auditLog) { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            when (entry.action) {
                                "LOCATION_ACCESSED", "LOCATION_SYNCED" -> Icons.Default.LocationOn
                                "SOS_TRIGGERED", "SOS_ACKNOWLEDGED" -> Icons.Default.Warning
                                "KILL_SWITCH_ACTIVATED" -> Icons.Default.Stop
                                "CONSENT_GRANTED", "CONSENT_REVOKED" -> Icons.Default.Security
                                "DATA_VIEWED" -> Icons.Default.Visibility
                                else -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = when {
                                entry.action.contains("KILL") -> MaterialTheme.colorScheme.error
                                entry.action.contains("SOS") -> MaterialTheme.colorScheme.error
                                entry.actor == "PARENT" -> MaterialTheme.colorScheme.primary
                                else -> Success
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.action.replace("_", " ").lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = entry.details,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = entry.timestamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
