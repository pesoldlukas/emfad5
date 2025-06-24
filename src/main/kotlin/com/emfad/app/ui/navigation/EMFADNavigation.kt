package com.emfad.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.emfad.app.ui.screens.dashboard.DashboardScreen
import com.emfad.app.ui.screens.measurement.MeasurementScreen
import com.emfad.app.ui.screens.analysis.AnalysisScreen
import com.emfad.app.ui.screens.ar.ARScreen
import com.emfad.app.ui.screens.export.ExportScreen
import com.emfad.app.ui.screens.settings.SettingsScreen

/**
 * EMFAD Navigation
 * Zentrale Navigation für Samsung S21 Ultra optimiert
 */

// Navigation Routes
sealed class EMFADScreen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val showInBottomNav: Boolean = true
) {
    object Dashboard : EMFADScreen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Measurement : EMFADScreen("measurement", "Messung", Icons.Default.Science)
    object Analysis : EMFADScreen("analysis", "Analyse", Icons.Default.Analytics)
    object AR : EMFADScreen("ar", "AR-Ansicht", Icons.Default.ViewInAr)
    object Export : EMFADScreen("export", "Export", Icons.Default.FileDownload)
    object Settings : EMFADScreen("settings", "Einstellungen", Icons.Default.Settings, false)
    
    // Detail-Screens (nicht in Bottom Navigation)
    object SessionDetail : EMFADScreen("session_detail/{sessionId}", "Session Details", Icons.Default.Info, false)
    object BluetoothSetup : EMFADScreen("bluetooth_setup", "Bluetooth Setup", Icons.Default.Bluetooth, false)
    object Calibration : EMFADScreen("calibration", "Kalibrierung", Icons.Default.Tune, false)
    object Help : EMFADScreen("help", "Hilfe", Icons.Default.Help, false)
    object About : EMFADScreen("about", "Über", Icons.Default.Info, false)
}

// Bottom Navigation Items
val bottomNavItems = listOf(
    EMFADScreen.Dashboard,
    EMFADScreen.Measurement,
    EMFADScreen.Analysis,
    EMFADScreen.AR,
    EMFADScreen.Export
)

@Composable
fun EMFADNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = EMFADScreen.Dashboard.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Dashboard
        composable(EMFADScreen.Dashboard.route) {
            DashboardScreen(
                onNavigateToMeasurement = {
                    navController.navigate(EMFADScreen.Measurement.route)
                },
                onNavigateToAnalysis = {
                    navController.navigate(EMFADScreen.Analysis.route)
                },
                onNavigateToSessionDetail = { sessionId ->
                    navController.navigate("session_detail/$sessionId")
                },
                onNavigateToSettings = {
                    navController.navigate(EMFADScreen.Settings.route)
                },
                onNavigateToBluetoothSetup = {
                    navController.navigate(EMFADScreen.BluetoothSetup.route)
                }
            )
        }
        
        // Measurement
        composable(EMFADScreen.Measurement.route) {
            MeasurementScreen(
                onNavigateToAR = {
                    navController.navigate(EMFADScreen.AR.route)
                },
                onNavigateToCalibration = {
                    navController.navigate(EMFADScreen.Calibration.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Analysis
        composable(EMFADScreen.Analysis.route) {
            AnalysisScreen(
                onNavigateToExport = {
                    navController.navigate(EMFADScreen.Export.route)
                },
                onNavigateToSessionDetail = { sessionId ->
                    navController.navigate("session_detail/$sessionId")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // AR View
        composable(EMFADScreen.AR.route) {
            ARScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Export
        composable(EMFADScreen.Export.route) {
            ExportScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Settings
        composable(EMFADScreen.Settings.route) {
            SettingsScreen(
                onNavigateToHelp = {
                    navController.navigate(EMFADScreen.Help.route)
                },
                onNavigateToAbout = {
                    navController.navigate(EMFADScreen.About.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Session Detail
        composable(EMFADScreen.SessionDetail.route) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")?.toLongOrNull() ?: 0L
            SessionDetailScreen(
                sessionId = sessionId,
                onNavigateToAnalysis = {
                    navController.navigate(EMFADScreen.Analysis.route)
                },
                onNavigateToExport = {
                    navController.navigate(EMFADScreen.Export.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Bluetooth Setup
        composable(EMFADScreen.BluetoothSetup.route) {
            BluetoothSetupScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSetupComplete = {
                    navController.popBackStack()
                }
            )
        }
        
        // Calibration
        composable(EMFADScreen.Calibration.route) {
            CalibrationScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onCalibrationComplete = {
                    navController.popBackStack()
                }
            )
        }
        
        // Help
        composable(EMFADScreen.Help.route) {
            HelpScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // About
        composable(EMFADScreen.About.route) {
            AboutScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun EMFADBottomNavigation(
    navController: NavHostController,
    currentRoute: String?
) {
    NavigationBar {
        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title
                    )
                },
                label = {
                    Text(text = screen.title)
                },
                selected = currentRoute == screen.route,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            // Pop up to start destination to avoid building up a large stack
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun getCurrentRoute(navController: NavHostController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}

// Navigation Helper Functions
fun NavHostController.navigateToSessionDetail(sessionId: Long) {
    navigate("session_detail/$sessionId")
}

fun NavHostController.navigateToMeasurement() {
    navigate(EMFADScreen.Measurement.route)
}

fun NavHostController.navigateToAnalysis() {
    navigate(EMFADScreen.Analysis.route)
}

fun NavHostController.navigateToAR() {
    navigate(EMFADScreen.AR.route)
}

fun NavHostController.navigateToExport() {
    navigate(EMFADScreen.Export.route)
}

fun NavHostController.navigateToSettings() {
    navigate(EMFADScreen.Settings.route)
}

fun NavHostController.navigateToBluetoothSetup() {
    navigate(EMFADScreen.BluetoothSetup.route)
}

fun NavHostController.navigateToCalibration() {
    navigate(EMFADScreen.Calibration.route)
}

// Placeholder Screens (werden später implementiert)
@Composable
private fun SessionDetailScreen(
    sessionId: Long,
    onNavigateToAnalysis: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateBack: () -> Unit
) {
    // Placeholder - wird später implementiert
}

@Composable
private fun BluetoothSetupScreen(
    onNavigateBack: () -> Unit,
    onSetupComplete: () -> Unit
) {
    // Placeholder - wird später implementiert
}

@Composable
private fun CalibrationScreen(
    onNavigateBack: () -> Unit,
    onCalibrationComplete: () -> Unit
) {
    // Placeholder - wird später implementiert
}

@Composable
private fun HelpScreen(
    onNavigateBack: () -> Unit
) {
    // Placeholder - wird später implementiert
}

@Composable
private fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    // Placeholder - wird später implementiert
}
