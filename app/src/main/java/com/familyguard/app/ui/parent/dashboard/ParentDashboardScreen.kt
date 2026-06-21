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
import androidx.hilt.navigation.compose.hiltViewModel
import com.familyguard.app.domain.model.TrendDirection
import com.familyguard.app.ui.theme.Success
import com.familyguard.app.ui.theme.Warning
import com.familyguard.app.ui.viewmodel.ParentDashboardViewModel

@Composable
fun ParentDashboardScreen(
    onNavigateToLocation: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    viewModel: ParentDashboardViewModel = hiltViewModel()
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
            Card(
                onClick = onNavigateToAlerts,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("View Alerts", style = MaterialTheme.typography.titleMedium)
                        Text("Check safety alerts", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (uiState.inviteCode.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("Family Invite Code", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.inviteCode,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Share this code with your child to pair their device",
                            style = MaterialTheme.typography.bodySmall
                        )
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

        if (uiState.isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (uiState.children.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No children paired yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Share your invite code with your child",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        } else {
            items(uiState.children) { child ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Child header with status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                if (child.lastLocation.isNotEmpty()) {
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

                        // Contextual Awareness Section
                        child.contextualReport?.let { report ->
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                "Contextual Awareness",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Current Activity
                            report.currentForegroundApp?.let { app ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "Currently using: ${app.appName}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            "Session: ${app.usageMinutesLastHour} min | Today: ${app.usageMinutesToday} min",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Notification Summary
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "Notifications Today",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${report.notificationSummary.totalNotificationsToday}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "Last Hour",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${report.notificationSummary.notificationsLastHour}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }

                            // Usage Summary
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "Screen Time Today",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${report.usageSummary.totalScreenTimeToday} min",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "Most Used",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        report.dailyInsights.mostUsedApp ?: "N/A",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }

                            // Trend Indicators
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TrendChip(
                                    label = "Usage",
                                    trend = report.usageSummary.usageTrend
                                )
                                TrendChip(
                                    label = "Notifications",
                                    trend = report.notificationSummary.notificationTrend
                                )
                                if (report.dailyInsights.lateNightActivity) {
                                    Icon(
                                        Icons.Default.NightsStay,
                                        contentDescription = "Late night activity detected",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrendChip(label: String, trend: TrendDirection) {
    val (icon, color, text) = when (trend) {
        TrendDirection.SPIKE -> Triple(Icons.Default.TrendingUp, MaterialTheme.colorScheme.error, "Spike")
        TrendDirection.INCREASING -> Triple(Icons.Default.TrendingUp, MaterialTheme.colorScheme.tertiary, "Rising")
        TrendDirection.STABLE -> Triple(Icons.Default.TrendingFlat, MaterialTheme.colorScheme.outline, "Stable")
        TrendDirection.DECREASING -> Triple(Icons.Default.TrendingDown, MaterialTheme.colorScheme.primary, "Falling")
        TrendDirection.UNUSUAL -> Triple(Icons.Default.Warning, MaterialTheme.colorScheme.error, "Unusual")
    }

    AssistChip(
        onClick = { },
        label = {
            Text(
                "$label: $text",
                style = MaterialTheme.typography.labelSmall
            )
        },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
        }
    )
}
