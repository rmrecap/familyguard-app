package com.familyguard.app.ui.parent.alerts

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
import com.familyguard.app.ui.theme.Error
import com.familyguard.app.ui.theme.Warning

data class AlertUi(
    val childName: String,
    val type: String,
    val message: String,
    val timestamp: String,
    val isAcknowledged: Boolean
)

@Composable
fun AlertsScreen(onBack: () -> Unit) {
    val alerts = remember {
        mutableStateListOf(
            AlertUi("Sarah", "SOS", "Emergency SOS triggered", "3:45 PM", false),
            AlertUi("Mike", "Geofence", "Left safe zone: School", "2:30 PM", true)
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
                text = "Alerts",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (alerts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No alerts",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(alerts) { alert ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (!alert.isAcknowledged) {
                            CardDefaults.cardColors(
                                containerColor = if (alert.type == "SOS") {
                                    Error.copy(alpha = 0.1f)
                                } else {
                                    Warning.copy(alpha = 0.1f)
                                }
                            )
                        } else {
                            CardDefaults.cardColors()
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                when (alert.type) {
                                    "SOS" -> Icons.Default.Warning
                                    "Geofence" -> Icons.Default.Fence
                                    else -> Icons.Default.Notifications
                                },
                                contentDescription = null,
                                tint = if (alert.type == "SOS") Error else Warning,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${alert.childName} - ${alert.type}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = alert.message,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = alert.timestamp,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!alert.isAcknowledged) {
                                TextButton(
                                    onClick = { /* Acknowledge */ }
                                ) {
                                    Text("Acknowledge")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
