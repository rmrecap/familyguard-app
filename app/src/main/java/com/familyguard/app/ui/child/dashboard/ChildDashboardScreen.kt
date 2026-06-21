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
import androidx.hilt.navigation.compose.hiltViewModel
import com.familyguard.app.domain.model.Feature
import com.familyguard.app.ui.theme.Error
import com.familyguard.app.ui.viewmodel.ChildDashboardViewModel

@Composable
fun ChildDashboardScreen(
    onNavigateToSos: () -> Unit,
    onNavigateToAuditLog: () -> Unit,
    viewModel: ChildDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
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
                    if (uiState.isTracking) {
                        Text(
                            text = "Monitoring since ${uiState.monitoringSince}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Last synced: ${uiState.lastSynced}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text(
                            text = "Location sharing is off",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
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

        items(Feature.entries.toList()) { feature ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = feature.displayName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = feature.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.consents[feature.id] ?: false,
                        onCheckedChange = { checked ->
                            viewModel.toggleFeature(feature, checked)
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
                onClick = { viewModel.activateKillSwitch() },
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

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            viewModel.clearError()
        }
    }
}
