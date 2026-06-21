package com.familyguard.app.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.familyguard.app.domain.model.DeviceRole

@Composable
fun RoleSelectionScreen(
    isLoading: Boolean,
    onSelectParent: () -> Unit,
    onSelectChild: () -> Unit,
    onRoleSelected: (DeviceRole) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Who is using this device?",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(48.dp))

        Card(
            onClick = {
                if (!isLoading) {
                    onRoleSelected(DeviceRole.PARENT)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            enabled = !isLoading
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SupervisorAccount,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Parent / Guardian",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Monitor and manage your family's safety",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (isLoading) {
                    Spacer(modifier = Modifier.weight(1f))
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            onClick = {
                if (!isLoading) {
                    onRoleSelected(DeviceRole.CHILD)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            enabled = !isLoading
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ChildCare,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Child / Teen",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Share your location and stay safe",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
