package com.familyguard.app.ui.child.sos

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.familyguard.app.ui.theme.Error
import com.familyguard.app.ui.theme.SOSRed
import com.familyguard.app.ui.viewmodel.ChildDashboardViewModel

@Composable
fun SosButtonScreen(
    onBack: () -> Unit,
    viewModel: ChildDashboardViewModel = hiltViewModel()
) {
    var isTriggered by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(3) }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(isTriggered) {
        if (isTriggered && countdown > 0) {
            kotlinx.coroutines.delay(1000)
            countdown--
            if (countdown == 0) {
                viewModel.triggerSos()
            }
        }
    }

    LaunchedEffect(uiState.sosTriggered) {
        if (uiState.sosTriggered) {
            // SOS sent successfully
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!isTriggered) {
            Text(
                text = "Emergency SOS",
                style = MaterialTheme.typography.headlineLarge,
                color = Error
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Press the button below to send an emergency alert to your parents with your current location.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { isTriggered = true },
                modifier = Modifier.size(200.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SOSRed
                )
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(onClick = onBack) {
                Text("Cancel")
            }
        } else {
            if (uiState.sosTriggered) {
                Text(
                    text = "SOS Alert Sent!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Your parents have been notified with your location.",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(onClick = onBack) {
                    Text("Back to Dashboard")
                }
            } else if (uiState.isLoading) {
                Text(
                    text = "Sending SOS in $countdown...",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Error
                )

                Spacer(modifier = Modifier.height(32.dp))

                CircularProgressIndicator(
                    modifier = Modifier.size(100.dp),
                    color = Error
                )

                Spacer(modifier = Modifier.height(32.dp))

                TextButton(
                    onClick = {
                        isTriggered = false
                        countdown = 3
                    }
                ) {
                    Text("Cancel SOS", color = Error)
                }
            }
        }
    }
}
