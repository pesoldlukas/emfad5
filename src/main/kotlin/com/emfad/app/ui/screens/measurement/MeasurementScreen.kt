package com.emfad.app.ui.screens.measurement

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emfad.app.models.EMFReading
import com.emfad.app.services.measurement.MeasurementServiceState
import com.emfad.app.ui.components.*
import com.emfad.app.ui.theme.*
import com.emfad.app.viewmodels.measurement.MeasurementViewModel

/**
 * Measurement Screen
 * Echtzeit-Messungen für Samsung S21 Ultra optimiert
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementScreen(
    onNavigateToAR: () -> Unit,
    onNavigateToCalibration: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: MeasurementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val measurementState by viewModel.measurementState.collectAsStateWithLifecycle()
    val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()
    val currentReading by viewModel.currentReading.collectAsStateWithLifecycle()
    val sessionStats by viewModel.sessionStats.collectAsStateWithLifecycle()
    val measurementHistory by viewModel.measurementHistory.collectAsStateWithLifecycle()
    
    // Session Dialog State
    var showSessionDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            MeasurementTopBar(
                sessionName = currentSession?.name ?: "Keine Session",
                isSessionActive = measurementState.isSessionActive,
                onNavigateBack = onNavigateBack,
                onCalibrationClick = onNavigateToCalibration
            )
        },
        floatingActionButton = {
            MeasurementFAB(
                measurementState = measurementState,
                onStartSession = { showSessionDialog = true },
                onStartMeasurement = { viewModel.startMeasurement() },
                onStopMeasurement = { viewModel.stopMeasurement() },
                onPauseMeasurement = { viewModel.pauseMeasurement() },
                onResumeMeasurement = { viewModel.resumeMeasurement() }
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
            // Service Status
            item {
                ServiceStatusCard(
                    serviceState = measurementState.serviceState,
                    isServiceReady = measurementState.isServiceReady
                )
            }
            
            // Current Reading Display
            item {
                CurrentReadingCard(
                    reading = currentReading,
                    isMeasuring = measurementState.isMeasuring
                )
            }
            
            // Session Statistics
            if (measurementState.isSessionActive) {
                item {
                    SessionStatisticsCard(
                        stats = sessionStats,
                        session = currentSession
                    )
                }
            }
            
            // Real-time Chart
            if (measurementHistory.isNotEmpty()) {
                item {
                    RealtimeChartCard(
                        measurements = measurementHistory,
                        onARViewClick = onNavigateToAR
                    )
                }
            }
            
            // Measurement Controls
            item {
                MeasurementControlsCard(
                    measurementState = measurementState,
                    onParameterChange = { freq, gain, filter ->
                        viewModel.updateMeasurementParameters(freq, gain, filter)
                    }
                )
            }
            
            // Recent Measurements List
            if (measurementHistory.isNotEmpty()) {
                item {
                    RecentMeasurementsCard(
                        measurements = measurementHistory.takeLast(10)
                    )
                }
            }
        }
    }
    
    // New Session Dialog
    if (showSessionDialog) {
        NewSessionDialog(
            onDismiss = { showSessionDialog = false },
            onConfirm = { sessionName, description, operator, location, project, sample ->
                viewModel.startNewSession(sessionName, description, operator, location, project, sample)
                showSessionDialog = false
            }
        )
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
private fun MeasurementTopBar(
    sessionName: String,
    isSessionActive: Boolean,
    onNavigateBack: () -> Unit,
    onCalibrationClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Messung",
                    style = MaterialTheme.typography.titleMedium
                )
                if (isSessionActive) {
                    Text(
                        text = sessionName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Zurück"
                )
            }
        },
        actions = {
            IconButton(onClick = onCalibrationClick) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Kalibrierung"
                )
            }
        }
    )
}

@Composable
private fun MeasurementFAB(
    measurementState: com.emfad.app.viewmodels.measurement.MeasurementState,
    onStartSession: () -> Unit,
    onStartMeasurement: () -> Unit,
    onStopMeasurement: () -> Unit,
    onPauseMeasurement: () -> Unit,
    onResumeMeasurement: () -> Unit
) {
    when {
        !measurementState.isSessionActive -> {
            ExtendedFloatingActionButton(
                onClick = onStartSession,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Neue Session"
                    )
                },
                text = { Text("Neue Session") }
            )
        }
        measurementState.isMeasuring && !measurementState.isPaused -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = onPauseMeasurement,
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pausieren"
                    )
                }
                FloatingActionButton(
                    onClick = onStopMeasurement,
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stoppen"
                    )
                }
            }
        }
        measurementState.isPaused -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = onResumeMeasurement,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Fortsetzen"
                    )
                }
                FloatingActionButton(
                    onClick = onStopMeasurement,
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stoppen"
                    )
                }
            }
        }
        else -> {
            FloatingActionButton(
                onClick = onStartMeasurement
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Messung starten"
                )
            }
        }
    }
}

@Composable
private fun ServiceStatusCard(
    serviceState: MeasurementServiceState,
    isServiceReady: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (serviceState) {
                MeasurementServiceState.MEASURING -> MaterialTheme.colorScheme.primaryContainer
                MeasurementServiceState.ERROR -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Service Status",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = when (serviceState) {
                        MeasurementServiceState.IDLE -> "Bereit"
                        MeasurementServiceState.READY -> "Vorbereitet"
                        MeasurementServiceState.MEASURING -> "Messung läuft"
                        MeasurementServiceState.PROCESSING -> "Verarbeitung"
                        MeasurementServiceState.ERROR -> "Fehler"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Icon(
                imageVector = when (serviceState) {
                    MeasurementServiceState.MEASURING -> Icons.Default.Science
                    MeasurementServiceState.ERROR -> Icons.Default.Error
                    else -> Icons.Default.CheckCircle
                },
                contentDescription = null,
                tint = when (serviceState) {
                    MeasurementServiceState.MEASURING -> MaterialTheme.colorScheme.primary
                    MeasurementServiceState.ERROR -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun CurrentReadingCard(
    reading: EMFReading?,
    isMeasuring: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = EMFADCustomShapes.measurementCard
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
                    text = "Aktuelle Messung",
                    style = MaterialTheme.typography.titleMedium
                )
                
                if (isMeasuring) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                )
                        )
                        Text(
                            text = "LIVE",
                            style = EMFADTextStyles.statusText,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            if (reading != null) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        MeasurementValueDisplay(
                            label = "Signalstärke",
                            value = "${"%.1f".format(reading.signalStrength)}",
                            unit = "dB",
                            color = getSignalStrengthColor(reading.signalStrength)
                        )
                    }
                    item {
                        MeasurementValueDisplay(
                            label = "Frequenz",
                            value = "${"%.1f".format(reading.frequency)}",
                            unit = "Hz"
                        )
                    }
                    item {
                        MeasurementValueDisplay(
                            label = "Phase",
                            value = "${"%.1f".format(reading.phase)}",
                            unit = "°"
                        )
                    }
                    item {
                        MeasurementValueDisplay(
                            label = "Tiefe",
                            value = "${"%.2f".format(reading.depth)}",
                            unit = "m"
                        )
                    }
                    item {
                        MeasurementValueDisplay(
                            label = "Temperatur",
                            value = "${"%.1f".format(reading.temperature)}",
                            unit = "°C"
                        )
                    }
                }
                
                if (reading.materialType.toString() != "UNKNOWN") {
                    Spacer(modifier = Modifier.height(8.dp))
                    MaterialTypeChip(
                        materialType = reading.materialType,
                        confidence = reading.confidence
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isMeasuring) "Warte auf Daten..." else "Keine Messung aktiv",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionStatisticsCard(
    stats: com.emfad.app.services.measurement.SessionStatistics,
    session: com.emfad.app.models.MeasurementSession?
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Session Statistiken",
                style = MaterialTheme.typography.titleMedium
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    StatCard(
                        title = "Messungen",
                        value = stats.totalMeasurements.toString(),
                        icon = Icons.Default.Science
                    )
                }
                item {
                    StatCard(
                        title = "Ø Signal",
                        value = "${"%.1f".format(stats.averageSignalStrength)}",
                        icon = Icons.Default.SignalCellularAlt
                    )
                }
                item {
                    StatCard(
                        title = "Max Signal",
                        value = "${"%.1f".format(stats.maxSignalStrength)}",
                        icon = Icons.Default.TrendingUp
                    )
                }
                item {
                    StatCard(
                        title = "Qualität",
                        value = "${"%.0f".format(stats.dataQuality * 100)}%",
                        icon = Icons.Default.HighQuality
                    )
                }
            }
        }
    }
}
