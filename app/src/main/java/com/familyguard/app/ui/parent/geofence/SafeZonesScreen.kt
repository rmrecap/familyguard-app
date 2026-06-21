package com.familyguard.app.ui.parent.geofence

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

data class SafeZoneUi(
    val zoneId: String,
    val name: String,
    val radius: Int,
    val isActive: Boolean
)

@Composable
fun SafeZonesScreen(onBack: () -> Unit) {
    val zones = remember {
        mutableStateListOf(
            SafeZoneUi("1", "Home", 100, true),
            SafeZoneUi("2", "School", 200, true),
            SafeZoneUi("3", "Grandma's House", 150, false)
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
                text = "Safe Zones",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { /* Add zone */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add Zone")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Safe zones trigger alerts when family members enter or leave these areas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(zones) { zone ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Fence,
                            contentDescription = null,
                            tint = if (zone.isActive) Success else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(zone.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Radius: ${zone.radius}m",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = zone.isActive,
                            onCheckedChange = { /* Toggle zone */ }
                        )
                    }
                }
            }
        }
    }
}
