package com.emfad.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emfad.app.ui.theme.EMFADColors
import kotlin.math.*

/**
 * EMFAD® Measurement Recorder Screen
 * Basiert auf Survey-Screen der originalen Windows-Software
 * Implementiert Step/Auto-Messung mit Live-Z-Wert-Anzeige
 */

data class EMFMeasurement(
    val timestamp: Long,
    val frequency: Double,
    val zValue: Double,
    val amplitude: Double,
    val phase: Double,
    val depth: Double,
    val quality: Double
)

enum class MeasurementMode {
    STEP, AUTO, PAUSE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementRecorderScreen(
    navController: androidx.navigation.NavController,
    modifier: Modifier = Modifier
) {
    // Verwende das ECHTE ViewModel mit echten Backend-Services
    val viewModel: com.emfad.app.viewmodels.MeasurementRecorderViewModel = androidx.hilt.navigation.compose.hiltViewModel()

    // Sammle echte Daten vom Backend
    val currentMeasurement by viewModel.currentMeasurement.collectAsState()
    val measurementHistory by viewModel.measurementHistory.collectAsState()
    val measurementMode by viewModel.measurementMode.collectAsState()
    val isDeviceConnected by viewModel.isDeviceConnected.collectAsState()
    val selectedFrequency by viewModel.selectedFrequency.collectAsState()

    // Konvertiere EMFReading zu EMFMeasurement für UI
    val uiMeasurement = currentMeasurement?.let { reading ->
        EMFMeasurement(
            timestamp = reading.timestamp,
            frequency = reading.frequency,
            zValue = reading.magnitude, // Z-Wert aus Magnitude
            amplitude = reading.amplitude,
            phase = reading.phase,
            depth = reading.depth,
            quality = reading.confidence
        )
    }

    val uiHistory = measurementHistory.map { reading ->
        EMFMeasurement(
            timestamp = reading.timestamp,
            frequency = reading.frequency,
            zValue = reading.magnitude,
            amplitude = reading.amplitude,
            phase = reading.phase,
            depth = reading.depth,
            quality = reading.confidence
        )
    }

    // Echte UI mit echten Backend-Daten
    RealMeasurementRecorderUI(
        currentMeasurement = uiMeasurement,
        measurementHistory = uiHistory,
        measurementMode = measurementMode,
        isDeviceConnected = isDeviceConnected,
        selectedFrequency = selectedFrequency,
        onModeChange = { mode -> viewModel.changeMeasurementMode(mode) },
        onStepMeasurement = { viewModel.performStepMeasurement() },
        onSaveMeasurement = { viewModel.saveMeasurement() },
        onNewSession = { viewModel.startNewSession() },
        onBack = { navController.popBackStack() },
        modifier = modifier
    )
}

@Composable
private fun RealMeasurementRecorderUI(
    currentMeasurement: EMFMeasurement?,
    measurementHistory: List<EMFMeasurement>,
    measurementMode: MeasurementMode,
    isDeviceConnected: Boolean,
    selectedFrequency: Double,
    onModeChange: (MeasurementMode) -> Unit,
    onStepMeasurement: () -> Unit,
    onSaveMeasurement: () -> Unit,
    onNewSession: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EMFADColors.BackgroundPrimary)
            .padding(16.dp)
    ) {
        // Header mit Navigation
        EMFADMeasurementHeader(
            onBack = onBack,
            isDeviceConnected = isDeviceConnected,
            selectedFrequency = selectedFrequency
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Live Z-Wert Anzeige (Hauptkomponente)
        EMFADZValueDisplay(
            currentMeasurement = currentMeasurement,
            modifier = Modifier.weight(0.4f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Signal-Chart (Live-Anzeige)
        EMFADSignalChart(
            measurements = measurementHistory,
            modifier = Modifier.weight(0.3f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Mess-Kontrollen (Step/Auto/Pause)
        EMFADMeasurementControls(
            measurementMode = measurementMode,
            isDeviceConnected = isDeviceConnected,
            onModeChange = onModeChange,
            onStepMeasurement = onStepMeasurement,
            onSaveMeasurement = onSaveMeasurement,
            onNewSession = onNewSession
        )
    }
}

@Composable
private fun EMFADMeasurementHeader(
    onBack: () -> Unit,
    isDeviceConnected: Boolean,
    selectedFrequency: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EMFADColors.SurfacePrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Zurück",
                    tint = EMFADColors.TextPrimary
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "EMFAD® Survey",
                    style = MaterialTheme.typography.headlineMedium,
                    color = EMFADColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Elektromagnetische Feldmessung",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EMFADColors.TextSecondary
                )
            }
            
            // Geräte- und Frequenzstatus
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isDeviceConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                        contentDescription = "Device Status",
                        modifier = Modifier.size(16.dp),
                        tint = if (isDeviceConnected) EMFADColors.StatusConnected else EMFADColors.StatusDisconnected
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = if (isDeviceConnected) "Verbunden" else "Getrennt",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDeviceConnected) EMFADColors.StatusConnected else EMFADColors.StatusDisconnected
                    )
                }
                
                Text(
                    text = "${selectedFrequency.toInt()} Hz",
                    style = MaterialTheme.typography.labelMedium,
                    color = EMFADColors.EMFADBlue,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EMFADZValueDisplay(
    currentMeasurement: EMFMeasurement?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EMFADColors.SurfacePrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Z-Wert (Live)",
                style = MaterialTheme.typography.titleLarge,
                color = EMFADColors.TextSecondary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Großer Z-Wert (wie in originaler Software)
            Text(
                text = currentMeasurement?.zValue?.let { "%.2f".format(it) } ?: "---",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp),
                color = EMFADColors.EMFADBlue,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Ω·m",
                style = MaterialTheme.typography.titleMedium,
                color = EMFADColors.TextSecondary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Zusätzliche Messwerte in Reihe
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EMFADMeasurementValue(
                    label = "Amplitude",
                    value = currentMeasurement?.amplitude?.let { "%.1f".format(it) } ?: "---",
                    unit = "mV"
                )
                
                EMFADMeasurementValue(
                    label = "Phase",
                    value = currentMeasurement?.phase?.let { "%.1f".format(it) } ?: "---",
                    unit = "°"
                )
                
                EMFADMeasurementValue(
                    label = "Tiefe",
                    value = currentMeasurement?.depth?.let { "%.2f".format(it) } ?: "---",
                    unit = "m"
                )
            }
        }
    }
}

@Composable
private fun EMFADMeasurementValue(
    label: String,
    value: String,
    unit: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = EMFADColors.TextTertiary
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = EMFADColors.TextPrimary,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = EMFADColors.TextSecondary
        )
    }
}

@Composable
private fun EMFADSignalChart(
    measurements: List<EMFMeasurement>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EMFADColors.SurfacePrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Signal-Verlauf",
                style = MaterialTheme.typography.titleMedium,
                color = EMFADColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Chart-Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(EMFADColors.BackgroundSecondary)
            ) {
                drawEMFSignalChart(measurements)
            }
        }
    }
}

private fun DrawScope.drawEMFSignalChart(measurements: List<EMFMeasurement>) {
    if (measurements.isEmpty()) return
    
    val width = size.width
    val height = size.height
    val padding = 40f
    
    // Achsen zeichnen
    drawLine(
        color = EMFADColors.TextTertiary,
        start = Offset(padding, height - padding),
        end = Offset(width - padding, height - padding),
        strokeWidth = 2f
    )
    
    drawLine(
        color = EMFADColors.TextTertiary,
        start = Offset(padding, padding),
        end = Offset(padding, height - padding),
        strokeWidth = 2f
    )
    
    if (measurements.size < 2) return
    
    // Signal-Linie zeichnen
    val maxValue = measurements.maxOfOrNull { it.zValue } ?: 1.0
    val minValue = measurements.minOfOrNull { it.zValue } ?: 0.0
    val valueRange = maxValue - minValue
    
    val path = Path()
    measurements.forEachIndexed { index, measurement ->
        val x = padding + (index.toFloat() / (measurements.size - 1)) * (width - 2 * padding)
        val y = height - padding - ((measurement.zValue - minValue) / valueRange * (height - 2 * padding)).toFloat()
        
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    
    drawPath(
        path = path,
        color = EMFADColors.EMFADBlue,
        style = Stroke(width = 3f)
    )
    
    // Datenpunkte zeichnen
    measurements.forEachIndexed { index, measurement ->
        val x = padding + (index.toFloat() / (measurements.size - 1)) * (width - 2 * padding)
        val y = height - padding - ((measurement.zValue - minValue) / valueRange * (height - 2 * padding)).toFloat()
        
        drawCircle(
            color = EMFADColors.EMFADYellow,
            radius = 4f,
            center = Offset(x, y)
        )
    }
}

@Composable
private fun EMFADMeasurementControls(
    measurementMode: MeasurementMode,
    isDeviceConnected: Boolean,
    onModeChange: (MeasurementMode) -> Unit,
    onStepMeasurement: () -> Unit,
    onSaveMeasurement: () -> Unit,
    onNewSession: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EMFADColors.SurfaceSecondary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Mess-Kontrollen",
                style = MaterialTheme.typography.titleMedium,
                color = EMFADColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Modus-Buttons (Step/Auto/Pause)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EMFADModeButton(
                    text = "Step",
                    icon = Icons.Default.SkipNext,
                    isSelected = measurementMode == MeasurementMode.STEP,
                    isEnabled = isDeviceConnected,
                    onClick = { onModeChange(MeasurementMode.STEP) },
                    modifier = Modifier.weight(1f)
                )
                
                EMFADModeButton(
                    text = "Auto",
                    icon = Icons.Default.PlayArrow,
                    isSelected = measurementMode == MeasurementMode.AUTO,
                    isEnabled = isDeviceConnected,
                    onClick = { onModeChange(MeasurementMode.AUTO) },
                    modifier = Modifier.weight(1f)
                )
                
                EMFADModeButton(
                    text = "Pause",
                    icon = Icons.Default.Pause,
                    isSelected = measurementMode == MeasurementMode.PAUSE,
                    isEnabled = isDeviceConnected,
                    onClick = { onModeChange(MeasurementMode.PAUSE) },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Aktions-Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStepMeasurement,
                    enabled = isDeviceConnected && measurementMode == MeasurementMode.STEP,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADColors.EMFADBlue
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = "Step",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Messen")
                }
                
                Button(
                    onClick = onSaveMeasurement,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADColors.StatusConnected
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Speichern")
                }
                
                Button(
                    onClick = onNewSession,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADColors.EMFADYellowDark
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Neu")
                }
            }
        }
    }
}

@Composable
private fun EMFADModeButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) EMFADColors.EMFADBlue else EMFADColors.EMFADGray,
            contentColor = EMFADColors.TextPrimary
        ),
        modifier = modifier.height(48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
