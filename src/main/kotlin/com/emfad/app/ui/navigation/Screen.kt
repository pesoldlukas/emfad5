package com.emfad.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Measurement : Screen("measurement", "Measurement", Icons.Default.Bluetooth)
    object Analysis : Screen("analysis", "Analysis", Icons.Default.Analytics)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
} 