package com.emfad.app.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emfad.app.models.MeasurementSession
import com.emfad.app.models.ConnectionState
import com.emfad.app.ui.components.*
import com.emfad.app.ui.theme.EMFADCustomShapes
import com.emfad.app.ui.theme.EMFADTextStyles
import com.emfad.app.viewmodels.dashboard.DashboardViewModel

/**
 * Dashboard Screen
 * Hauptübersicht für EMFAD App - Samsung S21 Ultra optimiert
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToMeasurement: () -> Unit,
    onNavigateToAnalysis: () -> Unit,
    onNavigateToSessionDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBluetoothSetup: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bluetoothState by viewModel.bluetoothState.collectAsStateWithLifecycle()
    val recentSessions by viewModel.recentSessions.collectAsStateWithLifecycle()
    val dashboardStats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    
    // Error Handling
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            // Snackbar oder Error Dialog anzeigen
        }
    }
    
    Scaffold(
        topBar = {
            DashboardTopBar(
                onSettingsClick = onNavigateToSettings,
                onRefreshClick = { viewModel.refreshData() }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToMeasurement,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = "Neue Messung"
                    )
                },
                text = { Text("Neue Messung") },
                shape = EMFADCustomShapes.extendedFab
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bluetooth Status Card
            item {
                BluetoothStatusCard(
                    bluetoothState = bluetoothState,
                    onSetupClick = onNavigateToBluetoothSetup,
                    onScanClick = { viewModel.startBluetoothScan() },
                    onConnectClick = { device -> viewModel.connectToDevice(device) },
                    onDisconnectClick = { viewModel.disconnectDevice() }
                )
            }
            
            // Quick Stats Cards
            item {
                QuickStatsSection(
                    stats = dashboardStats,
                    isLoading = uiState.isLoading
                )
            }
            
            // Recent Sessions
            item {
                RecentSessionsSection(
                    sessions = recentSessions,
                    onSessionClick = onNavigateToSessionDetail,
                    onAnalysisClick = onNavigateToAnalysis,
                    isLoading = uiState.isLoading
                )
            }
            
            // Quick Actions
            item {
                QuickActionsSection(
                    onMeasurementClick = onNavigateToMeasurement,
                    onAnalysisClick = onNavigateToAnalysis,
                    onBluetoothSetupClick = onNavigateToBluetoothSetup
                )
            }
        }
    }
    
    // Loading Overlay
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    onSettingsClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "EMFAD Dashboard",
                style = EMFADTextStyles.sessionName
            )
        },
        actions = {
            IconButton(onClick = onRefreshClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Aktualisieren"
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Einstellungen"
                )
            }
        }
    )
}

@Composable
private fun BluetoothStatusCard(
    bluetoothState: com.emfad.app.viewmodels.dashboard.BluetoothState,
    onSetupClick: () -> Unit,
    onScanClick: () -> Unit,
    onConnectClick: (com.emfad.app.models.BluetoothDevice) -> Unit,
    onDisconnectClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = EMFADCustomShapes.deviceCard
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bluetooth Status",
                    style = MaterialTheme.typography.titleMedium
                )
                
                ConnectionStatusIndicator(
                    connectionState = bluetoothState.connectionState,
                    isScanning = bluetoothState.isScanning
                )
            }
            
            when {
                !bluetoothState.hasPermissions -> {
                    BluetoothPermissionPrompt(onSetupClick = onSetupClick)
                }
                bluetoothState.isConnected -> {
                    ConnectedDeviceInfo(
                        deviceInfo = bluetoothState.deviceInfo,
                        batteryLevel = bluetoothState.batteryLevel,
                        onDisconnectClick = onDisconnectClick
                    )
                }
                bluetoothState.availableDevices.isNotEmpty() -> {
                    AvailableDevicesList(
                        devices = bluetoothState.availableDevices,
                        onConnectClick = onConnectClick
                    )
                }
                else -> {
                    BluetoothScanPrompt(
                        isScanning = bluetoothState.isScanning,
                        onScanClick = onScanClick
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStatsSection(
    stats: com.emfad.app.viewmodels.dashboard.DashboardStatistics,
    isLoading: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Übersicht",
            style = MaterialTheme.typography.titleMedium
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                StatCard(
                    title = "Sessions",
                    value = stats.totalSessions.toString(),
                    icon = Icons.Default.Folder,
                    isLoading = isLoading
                )
            }
            item {
                StatCard(
                    title = "Messungen",
                    value = stats.totalMeasurements.toString(),
                    icon = Icons.Default.Science,
                    isLoading = isLoading
                )
            }
            item {
                StatCard(
                    title = "Analysen",
                    value = stats.totalAnalyses.toString(),
                    icon = Icons.Default.Analytics,
                    isLoading = isLoading
                )
            }
            item {
                StatCard(
                    title = "Aktiv",
                    value = stats.activeSessions.toString(),
                    icon = Icons.Default.PlayArrow,
                    isLoading = isLoading
                )
            }
        }
    }
}

@Composable
private fun RecentSessionsSection(
    sessions: List<MeasurementSession>,
    onSessionClick: (Long) -> Unit,
    onAnalysisClick: () -> Unit,
    isLoading: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Letzte Sessions",
                style = MaterialTheme.typography.titleMedium
            )
            
            TextButton(onClick = onAnalysisClick) {
                Text("Alle anzeigen")
            }
        }
        
        if (isLoading) {
            repeat(3) {
                SessionCardSkeleton()
            }
        } else if (sessions.isEmpty()) {
            EmptySessionsPrompt()
        } else {
            sessions.take(5).forEach { session ->
                SessionCard(
                    session = session,
                    onClick = { onSessionClick(session.id) }
                )
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    onMeasurementClick: () -> Unit,
    onAnalysisClick: () -> Unit,
    onBluetoothSetupClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Schnellzugriff",
            style = MaterialTheme.typography.titleMedium
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = "Neue Messung",
                icon = Icons.Default.Science,
                onClick = onMeasurementClick
            )
            
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = "Analyse",
                icon = Icons.Default.Analytics,
                onClick = onAnalysisClick
            )
            
            QuickActionCard(
                modifier = Modifier.weight(1f),
                title = "Bluetooth",
                icon = Icons.Default.Bluetooth,
                onClick = onBluetoothSetupClick
            )
        }
    }
}

// Helper Composables
@Composable
private fun ConnectionStatusIndicator(
    connectionState: ConnectionState,
    isScanning: Boolean
) {
    val (color, text) = when {
        isScanning -> MaterialTheme.colorScheme.primary to "Suche..."
        connectionState == ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary to "Verbunden"
        connectionState == ConnectionState.CONNECTING -> MaterialTheme.colorScheme.secondary to "Verbinde..."
        else -> MaterialTheme.colorScheme.error to "Getrennt"
    }
    
    Surface(
        color = color,
        shape = EMFADCustomShapes.statusIndicator
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = EMFADTextStyles.statusText,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun EmptySessionsPrompt() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Science,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Noch keine Sessions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Starten Sie Ihre erste Messung",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
