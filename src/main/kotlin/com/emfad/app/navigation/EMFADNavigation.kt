package com.emfad.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.emfad.app.ui.screens.*

/**
 * EMFAD® Navigation
 * Implementiert Navigation Compose für alle App-Screens
 * Navigation Flow: Start → Bluetooth → Recorder → Setup → Spectrum → Profile → AutoBalance → Map
 */

sealed class EMFADScreen(val route: String) {
    object Start : EMFADScreen("start")
    object BluetoothConnection : EMFADScreen("bluetooth_connection")
    object MeasurementRecorder : EMFADScreen("measurement_recorder")
    object Setup : EMFADScreen("setup")
    object SpectrumAnalyzer : EMFADScreen("spectrum_analyzer")
    object ProfileView : EMFADScreen("profile_view")
    object AutoBalance : EMFADScreen("auto_balance")
    object Map : EMFADScreen("map")
}

@Composable
fun EMFADNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = EMFADScreen.Start.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Start Screen - Verwende echte Navigation
        composable(EMFADScreen.Start.route) {
            StartScreen(navController = navController)
        }
        
        // Bluetooth Connection Screen - Verwende echte Navigation
        composable(EMFADScreen.BluetoothConnection.route) {
            BluetoothConnectionScreen(navController = navController)
        }
        
        // Measurement Recorder Screen - Verwende echte Navigation
        composable(EMFADScreen.MeasurementRecorder.route) {
            MeasurementRecorderScreen(navController = navController)
        }
        
        // Setup Screen - Verwende echte Navigation
        composable(EMFADScreen.Setup.route) {
            SetupScreen(navController = navController)
        }
        
        // Spectrum Analyzer Screen - Verwende echte Navigation
        composable(EMFADScreen.SpectrumAnalyzer.route) {
            SpectrumAnalyzerScreen(navController = navController)
        }
        
        // Profile View Screen - Verwende echte Navigation
        composable(EMFADScreen.ProfileView.route) {
            ProfileViewScreen(navController = navController)
        }
        
        // AutoBalance Screen - Verwende echte Navigation
        composable(EMFADScreen.AutoBalance.route) {
            AutoBalanceScreen(navController = navController)
        }
        
        // Map Screen - Verwende echte Navigation
        composable(EMFADScreen.Map.route) {
            MapScreen(navController = navController)
        }

        // Analysis Screen - Füge fehlende Navigation hinzu
        composable("analysis") {
            AnalysisScreen(navController = navController)
        }

        // AR Screen - Füge fehlende Navigation hinzu
        composable("ar") {
            com.emfad.app.ui.ARScreen(navController = navController)
        }
    }
}

/**
 * Navigation Extensions für einfachere Navigation
 */
fun NavHostController.navigateToBluetoothConnection() {
    navigate(EMFADScreen.BluetoothConnection.route)
}

fun NavHostController.navigateToMeasurementRecorder() {
    navigate(EMFADScreen.MeasurementRecorder.route)
}

fun NavHostController.navigateToSetup() {
    navigate(EMFADScreen.Setup.route)
}

fun NavHostController.navigateToSpectrumAnalyzer() {
    navigate(EMFADScreen.SpectrumAnalyzer.route)
}

fun NavHostController.navigateToProfileView() {
    navigate(EMFADScreen.ProfileView.route)
}

fun NavHostController.navigateToAutoBalance() {
    navigate(EMFADScreen.AutoBalance.route)
}

fun NavHostController.navigateToMap() {
    navigate(EMFADScreen.Map.route)
}

/**
 * Navigation Helper für Deep Links
 */
object EMFADDeepLinks {
    const val START = "emfad://start"
    const val BLUETOOTH = "emfad://bluetooth"
    const val MEASUREMENT = "emfad://measurement"
    const val SETUP = "emfad://setup"
    const val SPECTRUM = "emfad://spectrum"
    const val PROFILE = "emfad://profile"
    const val AUTOBALANCE = "emfad://autobalance"
    const val MAP = "emfad://map"
}
