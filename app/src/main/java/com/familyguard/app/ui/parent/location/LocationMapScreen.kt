package com.familyguard.app.ui.parent.location

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
import com.familyguard.app.ui.theme.Success

data class ChildLocationUi(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val lastUpdate: String,
    val isInSafeZone: Boolean,
    val nearestZone: String?,
    val isOnline: Boolean = true
)

@Composable
fun LocationMapScreen(onBack: () -> Unit) {
    val children = remember {
        mutableStateListOf(
            ChildLocationUi("Sarah", 37.7749, -122.4194, "2 min ago", true, "Home"),
            ChildLocationUi("Mike", 37.7849, -122.4094, "15 min ago", false, null)
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
                text = "Family Locations",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Location Privacy Notice",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Locations are shown with reduced precision (~111m) to protect privacy. Full precision is only stored locally on the child's device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Family Members",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(children) { child ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (child.isInSafeZone) Success else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(child.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Last updated: ${child.lastUpdate}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Coordinates: ${String.format("%.3f", child.latitude)}, ${String.format("%.3f", child.longitude)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (child.nearestZone != null) {
                                Text(
                                    "Zone: ${child.nearestZone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Success
                                )
                            }
                        }
                        Icon(
                            Icons.Default.Circle,
                            contentDescription = null,
                            tint = if (child.isOnline) Success else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
