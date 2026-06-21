package com.familyguard.app.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.familyguard.app.domain.model.Feature

data class ConsentItem(
    val feature: Feature,
    val icon: ImageVector,
    val granted: Boolean = false
)

@Composable
fun ConsentSetupScreen(onComplete: () -> Unit) {
    var consentItems by remember {
        mutableStateOf(
            listOf(
                ConsentItem(Feature.LOCATION_SHARING, Icons.Default.LocationOn),
                ConsentItem(Feature.SOS, Icons.Default.Warning),
                ConsentItem(Feature.GEOFENCE, Icons.Default.Fence),
                ConsentItem(Feature.SCREEN_TIME, Icons.Default.Timer)
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Choose Your Permissions",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Select which features you want to enable. You can change these anytime in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(consentItems) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.feature.displayName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = item.feature.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = item.granted,
                            onCheckedChange = { checked ->
                                consentItems = consentItems.map {
                                    if (it.feature.id == item.feature.id) {
                                        it.copy(granted = checked)
                                    } else it
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth(),
            enabled = consentItems.any { it.granted }
        ) {
            Text("Continue")
        }
    }
}
