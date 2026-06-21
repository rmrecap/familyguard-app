package com.familyguard.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object ParentDashboard : Screen("parent_dashboard", "Dashboard", Icons.Default.Home)
    data object LocationMap : Screen("location_map", "Location", Icons.Default.LocationOn)
    data object SafeZones : Screen("safe_zones", "Safe Zones", Icons.Default.Fence)
    data object Alerts : Screen("alerts", "Alerts", Icons.Default.Notifications)
    data object FamilyMembers : Screen("family_members", "Family", Icons.Default.People)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    data object ChildDashboard : Screen("child_dashboard", "Dashboard", Icons.Default.Home)
    data object SosButton : Screen("sos_button", "SOS", Icons.Default.Warning)
    data object AuditLog : Screen("audit_log", "Activity Log", Icons.Default.List)

    data object Onboarding : Screen("onboarding", "Welcome", Icons.Default.Star)
    data object RoleSelection : Screen("role_selection", "Select Role", Icons.Default.Person)
    data object FamilyPairing : Screen("family_pairing", "Join Family", Icons.Default.GroupAdd)
    data object ConsentSetup : Screen("consent_setup", "Permissions", Icons.Default.Security)
}

object Routes {
    const val PARENT_DASHBOARD = "parent_dashboard"
    const val LOCATION_MAP = "location_map"
    const val SAFE_ZONES = "safe_zones"
    const val ALERTS = "alerts"
    const val FAMILY_MEMBERS = "family_members"
    const val SETTINGS = "settings"
    const val CHILD_DASHBOARD = "child_dashboard"
    const val SOS_BUTTON = "sos_button"
    const val AUDIT_LOG = "audit_log"
    const val ONBOARDING = "onboarding"
    const val ROLE_SELECTION = "role_selection"
    const val FAMILY_PAIRING = "family_pairing"
    const val CONSENT_SETUP = "consent_setup"
}
