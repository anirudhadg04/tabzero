package com.example.detoxproject.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val activeIcon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Outlined.Home, Icons.Filled.Home)
    object Focus : Screen("focus", "Focus", Icons.Outlined.Timer, Icons.Filled.Timer)
    object Discover : Screen("discover", "Mindful", Icons.Outlined.Spa, Icons.Filled.Spa)
    object Progress : Screen("progress", "Progress", Icons.Outlined.BarChart, Icons.Filled.BarChart)
    object Profile : Screen("profile", "Profile", Icons.Outlined.Person, Icons.Filled.Person)

    // Secondary routes
    object Fitness : Screen("fitness", "Fitness", Icons.Outlined.DirectionsRun, Icons.Filled.DirectionsRun)
    object Settings : Screen("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
    object CreatePledge : Screen("create_pledge", "Create Plan", Icons.Outlined.Add, Icons.Filled.Add)
    object AppSelection : Screen("app_selection", "Select Apps", Icons.Outlined.Apps, Icons.Filled.Apps)
    object ActiveSession : Screen("active_session", "Session Active", Icons.Outlined.SelfImprovement, Icons.Filled.SelfImprovement)
}

val bottomNavScreens: List<Screen> by lazy {
    listOf(Screen.Home, Screen.Focus, Screen.Discover, Screen.Progress, Screen.Profile)
}
