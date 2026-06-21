package com.familyguard.app.ui.parent.dashboard

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
import com.familyguard.app.ui.theme.Warning

data class ChildUi(
    val deviceId: String,
    val name: String,
    val lastSeen: String,
    val isOnline: Boolean,
    val lastLocation: String?
)

@Composable
fun ParentDashboardScreen(
    onNavigateToLocation: () -> Unit,
    onNavigateToAlerts: () -> Unit
) {
    val children = remember {
        mutableStateListOf(
            ChildUi("1", "Sarah", "2 min ago", true, "Home"),
            ChildUi("2", "Mike", "15 min ago", true, "School")
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Family Dashboard",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Card(
                onClick = onNavigateToLocation,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("View All Locations", style = MaterialTheme.typography.titleMedium)
                        Text("See where your family is", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            Text(
                text = "Family Members",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(children) { child ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(child.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Last seen: ${child.lastSeen}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (child.lastLocation != null) {
                            Text(
                                "Location: ${child.lastLocation}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        Icons.Default.Circle,
                        contentDescription = null,
                        tint = if (child.isOnline) Success else Warning,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
