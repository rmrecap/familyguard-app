package com.familyguard.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.familyguard.app.domain.model.DeviceRole
import com.familyguard.app.ui.navigation.Routes
import com.familyguard.app.ui.navigation.Screen
import com.familyguard.app.ui.onboarding.OnboardingScreen
import com.familyguard.app.ui.onboarding.RoleSelectionScreen
import com.familyguard.app.ui.onboarding.ConsentSetupScreen
import com.familyguard.app.ui.onboarding.FamilyPairingScreen
import com.familyguard.app.ui.parent.dashboard.ParentDashboardScreen
import com.familyguard.app.ui.child.dashboard.ChildDashboardScreen
import com.familyguard.app.ui.child.sos.SosButtonScreen
import com.familyguard.app.ui.viewmodel.RegistrationViewModel
import com.familyguard.app.ui.theme.FamilyGuardTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FamilyGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FamilyGuardApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyGuardApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val registrationViewModel: RegistrationViewModel = hiltViewModel()
    val uiState by registrationViewModel.uiState.collectAsState()

    val bottomBarScreens = listOf(
        Screen.ParentDashboard,
        Screen.LocationMap,
        Screen.Alerts,
        Screen.FamilyMembers,
        Screen.Settings
    )

    val showBottomBar = currentRoute in bottomBarScreens.map { it.route }

    // Auto-navigate after registration/pairing
    LaunchedEffect(uiState.isRegistered, uiState.isPaired, uiState.consentsGranted) {
        if (uiState.isRegistered && uiState.role == DeviceRole.PARENT && uiState.isPaired) {
            navController.navigate(Routes.PARENT_DASHBOARD) {
                popUpTo(Routes.ONBOARDING) { inclusive = true }
            }
        } else if (uiState.isRegistered && uiState.role == DeviceRole.CHILD && uiState.isPaired) {
            if (uiState.consentsGranted) {
                navController.navigate(Routes.CHILD_DASHBOARD) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            } else {
                navController.navigate(Routes.CONSENT_SETUP) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomBarScreens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.ONBOARDING,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onNavigateToRoleSelection = {
                        navController.navigate(Routes.ROLE_SELECTION)
                    }
                )
            }

            composable(Routes.ROLE_SELECTION) {
                RoleSelectionScreen(
                    isLoading = uiState.isLoading,
                    onSelectParent = {
                        registrationViewModel.createFamily()
                    },
                    onSelectChild = {
                        navController.navigate(Routes.FAMILY_PAIRING)
                    },
                    onRoleSelected = { role ->
                        registrationViewModel.selectRole(role)
                        if (role == DeviceRole.PARENT) {
                            registrationViewModel.createFamily()
                        } else {
                            navController.navigate(Routes.FAMILY_PAIRING)
                        }
                    }
                )
            }

            composable(Routes.FAMILY_PAIRING) {
                FamilyPairingScreen(
                    isLoading = uiState.isLoading,
                    error = uiState.error,
                    onJoinFamily = { code ->
                        registrationViewModel.joinFamily(code)
                    },
                    onBack = {
                        navController.popBackStack()
                    },
                    onClearError = {
                        registrationViewModel.clearError()
                    }
                )
            }

            composable(Routes.CONSENT_SETUP) {
                ConsentSetupScreen(
                    consentsGranted = uiState.consentsGranted,
                    onGrantConsents = { features ->
                        registrationViewModel.grantConsents(features)
                    },
                    onComplete = {
                        navController.navigate(Routes.CHILD_DASHBOARD) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.PARENT_DASHBOARD) {
                ParentDashboardScreen(
                    onNavigateToLocation = { navController.navigate(Routes.LOCATION_MAP) },
                    onNavigateToAlerts = { navController.navigate(Routes.ALERTS) }
                )
            }

            composable(Routes.CHILD_DASHBOARD) {
                ChildDashboardScreen(
                    onNavigateToSos = { navController.navigate(Routes.SOS_BUTTON) },
                    onNavigateToAuditLog = { navController.navigate(Routes.AUDIT_LOG) }
                )
            }

            composable(Routes.SOS_BUTTON) {
                SosButtonScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.AUDIT_LOG) {
                com.familyguard.app.ui.child.audit.AuditLogScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.LOCATION_MAP) {
                com.familyguard.app.ui.parent.location.LocationMapScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.ALERTS) {
                com.familyguard.app.ui.parent.alerts.AlertsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.FAMILY_MEMBERS) {
                com.familyguard.app.ui.parent.family.FamilyMembersScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.SAFE_ZONES) {
                com.familyguard.app.ui.parent.geofence.SafeZonesScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.SETTINGS) {
                com.familyguard.app.ui.parent.settings.SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
