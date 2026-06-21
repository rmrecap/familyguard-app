package com.familyguard.app.ui.child.dashboard

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
import com.familyguard.app.domain.model.Feature
import com.familyguard.app.ui.theme.Error
import com.familyguard.app.ui.theme.Success

data class FeatureToggleUi(
    val feature: Feature,
    val enabled: Boolean
)

@Composable
fun ChildDashboardScreen(
    onNavigateToSos: () -> Unit,
    onNavigateToAuditLog: () -> Unit
) {
    var features by remember {
        mutableStateOf(
            listOf(
                FeatureToggleUi(Feature.LOCATION_SHARING, true),
                FeatureToggleUi(Feature.SOS, true),
                FeatureToggleUi(Feature.GEOFENCE, false),
                FeatureToggleUi(Feature.SCREEN_TIME, false)
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "FamilyGuard Active",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Monitoring since 10:30 AM",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Last synced: Just now",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            Text(
                text = "Active Features",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(features) { feature ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = feature.feature.displayName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = feature.feature.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = feature.enabled,
                        onCheckedChange = { checked ->
                            features = features.map {
                                if (it.feature.id == feature.feature.id) {
                                    it.copy(enabled = checked)
                                } else it
                            }
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onNavigateToSos,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Error
                )
            ) {
                Icon(Icons.Default.Warning, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Emergency SOS")
            }
        }

        item {
            OutlinedButton(
                onClick = onNavigateToAuditLog,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.List, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Activity Log")
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { /* Kill switch */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop All Monitoring")
            }
        }
    }
}
