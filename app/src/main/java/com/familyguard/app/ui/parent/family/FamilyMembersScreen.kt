package com.familyguard.app.ui.parent.family

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

data class FamilyMemberUi(
    val deviceId: String,
    val name: String,
    val role: String,
    val isOnline: Boolean
)

@Composable
fun FamilyMembersScreen(onBack: () -> Unit) {
    val members = remember {
        mutableStateListOf(
            FamilyMemberUi("1", "Dad", "Parent", true),
            FamilyMemberUi("2", "Mom", "Parent", true),
            FamilyMemberUi("3", "Sarah", "Child", true),
            FamilyMemberUi("4", "Mike", "Child", false)
        )
    }

    var showInviteDialog by remember { mutableStateOf(false) }

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
                text = "Family Members",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { showInviteDialog = true }) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Member")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(members) { member ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (member.role == "Parent") Icons.Default.SupervisorAccount else Icons.Default.ChildCare,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(member.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                member.role,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.Circle,
                            contentDescription = null,
                            tint = if (member.isOnline) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }

    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Add Family Member") },
            text = {
                Column {
                    Text("Share this code with family members to add them:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "ABC123",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}
