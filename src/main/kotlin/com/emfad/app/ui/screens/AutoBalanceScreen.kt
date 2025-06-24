package com.emfad.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emfad.app.ui.theme.EMFADColors
import kotlin.math.*

/**
 * EMFAD® AutoBalance Screen
 * Basiert auf TfrmAutoBalance der originalen Windows-Software
 * Implementiert XY-Kalibrierung, Kompass-Ausgleich und Datei-Management
 * Rekonstruiert aus EMUNIX07EXE.c "autobalance values; version 1.0"
 */

data class AutoBalanceData(
    val version: String = "autobalance values; version 1.0",
    val compassCalibrationStatus: CalibrationStatus = CalibrationStatus.NOT_STARTED,
    val horizontalCalibrationStatus: CalibrationStatus = CalibrationStatus.NOT_STARTED,
    val verticalCalibrationStatus: CalibrationStatus = CalibrationStatus.NOT_STARTED,
    val horizontalOffsetX: Float = 0.0f,
    val horizontalOffsetY: Float = 0.0f,
    val horizontalScaleX: Float = 1.0f,
    val horizontalScaleY: Float = 1.0f,
    val verticalOffsetZ: Float = 0.0f,
    val verticalScaleZ: Float = 1.0f,
    val compassHeading: Float = 0.0f,
    val compassAccuracy: Float = 0.0f,
    val lastCalibrationTime: Long = 0L
)

enum class CalibrationStatus(val message: String) {
    NOT_STARTED("Not started"),
    STARTED("Compass calibration started"),
    COLLECTING_HORIZONTAL("collecting data horizontal calibration"),
    COLLECTING_VERTICAL("collecting data vertical calibration"),
    HORIZONTAL_FINISHED("horizontal calibration finished"),
    VERTICAL_FINISHED("vertical calibration finished"),
    COMPASS_FINISHED("compass calibration finished"),
    SAVED("calibration saved")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoBalanceScreen(
    navController: androidx.navigation.NavController,
    modifier: Modifier = Modifier
) {
    // Verwende das ECHTE AutoBalanceViewModel mit echtem AutoBalanceService
    val viewModel: com.emfad.app.viewmodels.AutoBalanceViewModel = androidx.hilt.navigation.compose.hiltViewModel()

    // Sammle echte Daten vom Backend
    val serviceAutoBalanceData by viewModel.autoBalanceData.collectAsState()
    val isCalibrating by viewModel.isCalibrating.collectAsState()
    val calibrationMessage by viewModel.calibrationMessage.collectAsState()
    val showCalibrationDialog by viewModel.showCalibrationDialog.collectAsState()

    // Konvertiere Service-Daten zu UI-Daten
    val uiAutoBalanceData = AutoBalanceData(
        version = serviceAutoBalanceData.version,
        compassCalibrationStatus = serviceAutoBalanceData.compassCalibrationStatus,
        horizontalCalibrationStatus = serviceAutoBalanceData.horizontalCalibrationStatus,
        verticalCalibrationStatus = serviceAutoBalanceData.verticalCalibrationStatus,
        horizontalOffsetX = serviceAutoBalanceData.horizontalOffsetX,
        horizontalOffsetY = serviceAutoBalanceData.horizontalOffsetY,
        horizontalScaleX = serviceAutoBalanceData.horizontalScaleX,
        horizontalScaleY = serviceAutoBalanceData.horizontalScaleY,
        verticalOffsetZ = serviceAutoBalanceData.verticalOffsetZ,
        verticalScaleZ = serviceAutoBalanceData.verticalScaleZ,
        compassHeading = serviceAutoBalanceData.compassHeading,
        compassAccuracy = serviceAutoBalanceData.compassAccuracy,
        lastCalibrationTime = serviceAutoBalanceData.lastCalibrationTime
    )

    // Echte UI mit echten Backend-Daten
    RealAutoBalanceUI(
        autoBalanceData = uiAutoBalanceData,
        isCalibrating = isCalibrating,
        onStartCompassCalibration = { viewModel.startCompassCalibration() },
        onStartHorizontalCalibration = { viewModel.startHorizontalCalibration() },
        onStartVerticalCalibration = { viewModel.startVerticalCalibration() },
        onSaveCalibration = { viewModel.saveCalibration() },
        onLoadCalibration = { viewModel.loadCalibration() },
        onDeleteAllCalibration = { viewModel.deleteAllCalibration() },
        onBack = { navController.popBackStack() },
        modifier = modifier
    )

    // Zeige Kalibrierungs-Nachrichten
    if (calibrationMessage.isNotEmpty()) {
        LaunchedEffect(calibrationMessage) {
            // TODO: Zeige Snackbar oder Toast
        }
    }

    // Zeige Lösch-Dialog
    if (showCalibrationDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteCalibration() },
            title = { Text("Alle Kalibrierungen löschen?") },
            text = { Text("Diese Aktion kann nicht rückgängig gemacht werden.") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDeleteAllCalibration() }) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDeleteCalibration() }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun RealAutoBalanceUI(
    autoBalanceData: AutoBalanceData,
    isCalibrating: Boolean,
    onStartCompassCalibration: () -> Unit,
    onStartHorizontalCalibration: () -> Unit,
    onStartVerticalCalibration: () -> Unit,
    onSaveCalibration: () -> Unit,
    onLoadCalibration: () -> Unit,
    onDeleteAllCalibration: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EMFADColors.BackgroundPrimary)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        EMFADAutoBalanceHeader(
            onBack = onBack,
            version = autoBalanceData.version
        )
        
        // Kompass-Kalibrierung (Hauptkomponente)
        EMFADCompassCalibration(
            compassHeading = autoBalanceData.compassHeading,
            compassAccuracy = autoBalanceData.compassAccuracy,
            calibrationStatus = autoBalanceData.compassCalibrationStatus,
            isCalibrating = isCalibrating,
            onStartCalibration = onStartCompassCalibration,
            modifier = Modifier.weight(0.4f)
        )
        
        // Horizontale und Vertikale Kalibrierung
        Row(
            modifier = Modifier.weight(0.3f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EMFADHorizontalCalibration(
                offsetX = autoBalanceData.horizontalOffsetX,
                offsetY = autoBalanceData.horizontalOffsetY,
                scaleX = autoBalanceData.horizontalScaleX,
                scaleY = autoBalanceData.horizontalScaleY,
                calibrationStatus = autoBalanceData.horizontalCalibrationStatus,
                onStartCalibration = onStartHorizontalCalibration,
                modifier = Modifier.weight(1f)
            )
            
            EMFADVerticalCalibration(
                offsetZ = autoBalanceData.verticalOffsetZ,
                scaleZ = autoBalanceData.verticalScaleZ,
                calibrationStatus = autoBalanceData.verticalCalibrationStatus,
                onStartCalibration = onStartVerticalCalibration,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Kalibrierungs-Kontrollen
        EMFADCalibrationControls(
            onSaveCalibration = onSaveCalibration,
            onLoadCalibration = onLoadCalibration,
            onDeleteAllCalibration = onDeleteAllCalibration,
            lastCalibrationTime = autoBalanceData.lastCalibrationTime
        )
    }
}

@Composable
private fun EMFADAutoBalanceHeader(
    onBack: () -> Unit,
    version: String
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
                    text = "EMFAD® AutoBalance",
                    style = MaterialTheme.typography.headlineMedium,
                    color = EMFADColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Automatische Kalibrierung und Kompass-Ausgleich",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EMFADColors.TextSecondary
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Icon(
                    imageVector = Icons.Default.Balance,
                    contentDescription = "AutoBalance",
                    modifier = Modifier.size(32.dp),
                    tint = EMFADColors.EMFADYellow
                )
                
                Text(
                    text = version,
                    style = MaterialTheme.typography.labelSmall,
                    color = EMFADColors.TextTertiary
                )
            }
        }
    }
}

@Composable
private fun EMFADCompassCalibration(
    compassHeading: Float,
    compassAccuracy: Float,
    calibrationStatus: CalibrationStatus,
    isCalibrating: Boolean,
    onStartCalibration: () -> Unit,
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Kompass-Kalibrierung",
                style = MaterialTheme.typography.titleLarge,
                color = EMFADColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = calibrationStatus.message,
                style = MaterialTheme.typography.bodyMedium,
                color = when (calibrationStatus) {
                    CalibrationStatus.COMPASS_FINISHED -> EMFADColors.SignalGreen
                    CalibrationStatus.STARTED, CalibrationStatus.COLLECTING_HORIZONTAL, CalibrationStatus.COLLECTING_VERTICAL -> EMFADColors.EMFADYellow
                    else -> EMFADColors.TextSecondary
                },
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Kompass-Anzeige
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(EMFADColors.BackgroundSecondary),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    drawCompass(compassHeading, compassAccuracy, isCalibrating)
                }
                
                // Kompass-Werte
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${compassHeading.toInt()}°",
                        style = MaterialTheme.typography.headlineSmall,
                        color = EMFADColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Genauigkeit: ${(compassAccuracy * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (compassAccuracy > 0.8f) EMFADColors.SignalGreen else EMFADColors.SignalOrange
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Kalibrierungs-Button
            Button(
                onClick = onStartCalibration,
                enabled = !isCalibrating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (calibrationStatus == CalibrationStatus.COMPASS_FINISHED) 
                        EMFADColors.SignalGreen else EMFADColors.EMFADBlue
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isCalibrating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = EMFADColors.TextPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kalibriere...")
                } else {
                    Icon(
                        imageVector = if (calibrationStatus == CalibrationStatus.COMPASS_FINISHED) 
                            Icons.Default.CheckCircle else Icons.Default.Explore,
                        contentDescription = "Compass",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (calibrationStatus == CalibrationStatus.COMPASS_FINISHED) 
                            "Kalibrierung abgeschlossen" else "Kompass kalibrieren"
                    )
                }
            }
        }
    }
}

@Composable
private fun EMFADHorizontalCalibration(
    offsetX: Float,
    offsetY: Float,
    scaleX: Float,
    scaleY: Float,
    calibrationStatus: CalibrationStatus,
    onStartCalibration: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = EMFADColors.SurfaceSecondary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Horizontal-Kalibrierung",
                style = MaterialTheme.typography.titleMedium,
                color = EMFADColors.TextPrimary,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Text(
                text = "X-Y Achsen-Ausgleich",
                style = MaterialTheme.typography.bodySmall,
                color = EMFADColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Kalibrierungs-Werte
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EMFADCalibrationValue(
                    label = "Offset X:",
                    value = "%.3f".format(offsetX),
                    unit = "mV"
                )

                EMFADCalibrationValue(
                    label = "Offset Y:",
                    value = "%.3f".format(offsetY),
                    unit = "mV"
                )

                EMFADCalibrationValue(
                    label = "Scale X:",
                    value = "%.3f".format(scaleX),
                    unit = ""
                )

                EMFADCalibrationValue(
                    label = "Scale Y:",
                    value = "%.3f".format(scaleY),
                    unit = ""
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Status-Indikator
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (calibrationStatus) {
                        CalibrationStatus.HORIZONTAL_FINISHED -> Icons.Default.CheckCircle
                        CalibrationStatus.COLLECTING_HORIZONTAL -> Icons.Default.Sync
                        else -> Icons.Default.RadioButtonUnchecked
                    },
                    contentDescription = "Status",
                    modifier = Modifier.size(16.dp),
                    tint = when (calibrationStatus) {
                        CalibrationStatus.HORIZONTAL_FINISHED -> EMFADColors.SignalGreen
                        CalibrationStatus.COLLECTING_HORIZONTAL -> EMFADColors.EMFADYellow
                        else -> EMFADColors.TextTertiary
                    }
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = when (calibrationStatus) {
                        CalibrationStatus.HORIZONTAL_FINISHED -> "Fertig"
                        CalibrationStatus.COLLECTING_HORIZONTAL -> "Läuft"
                        else -> "Bereit"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (calibrationStatus) {
                        CalibrationStatus.HORIZONTAL_FINISHED -> EMFADColors.SignalGreen
                        CalibrationStatus.COLLECTING_HORIZONTAL -> EMFADColors.EMFADYellow
                        else -> EMFADColors.TextTertiary
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Kalibrierungs-Button
            Button(
                onClick = onStartCalibration,
                enabled = calibrationStatus != CalibrationStatus.COLLECTING_HORIZONTAL,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (calibrationStatus == CalibrationStatus.HORIZONTAL_FINISHED)
                        EMFADColors.SignalGreen else EMFADColors.EMFADBlue
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Horizontal",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Kalibrieren",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun EMFADVerticalCalibration(
    offsetZ: Float,
    scaleZ: Float,
    calibrationStatus: CalibrationStatus,
    onStartCalibration: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = EMFADColors.SurfaceSecondary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Vertikal-Kalibrierung",
                style = MaterialTheme.typography.titleMedium,
                color = EMFADColors.TextPrimary,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Z-Achsen-Ausgleich",
                style = MaterialTheme.typography.bodySmall,
                color = EMFADColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Kalibrierungs-Werte
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EMFADCalibrationValue(
                    label = "Offset Z:",
                    value = "%.3f".format(offsetZ),
                    unit = "mV"
                )

                EMFADCalibrationValue(
                    label = "Scale Z:",
                    value = "%.3f".format(scaleZ),
                    unit = ""
                )

                // Platzhalter für symmetrische Darstellung
                Spacer(modifier = Modifier.height(32.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Status-Indikator
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (calibrationStatus) {
                        CalibrationStatus.VERTICAL_FINISHED -> Icons.Default.CheckCircle
                        CalibrationStatus.COLLECTING_VERTICAL -> Icons.Default.Sync
                        else -> Icons.Default.RadioButtonUnchecked
                    },
                    contentDescription = "Status",
                    modifier = Modifier.size(16.dp),
                    tint = when (calibrationStatus) {
                        CalibrationStatus.VERTICAL_FINISHED -> EMFADColors.SignalGreen
                        CalibrationStatus.COLLECTING_VERTICAL -> EMFADColors.EMFADYellow
                        else -> EMFADColors.TextTertiary
                    }
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = when (calibrationStatus) {
                        CalibrationStatus.VERTICAL_FINISHED -> "Fertig"
                        CalibrationStatus.COLLECTING_VERTICAL -> "Läuft"
                        else -> "Bereit"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (calibrationStatus) {
                        CalibrationStatus.VERTICAL_FINISHED -> EMFADColors.SignalGreen
                        CalibrationStatus.COLLECTING_VERTICAL -> EMFADColors.EMFADYellow
                        else -> EMFADColors.TextTertiary
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Kalibrierungs-Button
            Button(
                onClick = onStartCalibration,
                enabled = calibrationStatus != CalibrationStatus.COLLECTING_VERTICAL,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (calibrationStatus == CalibrationStatus.VERTICAL_FINISHED)
                        EMFADColors.SignalGreen else EMFADColors.EMFADBlue
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Vertical",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Kalibrieren",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun EMFADCalibrationValue(
    label: String,
    value: String,
    unit: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = EMFADColors.TextSecondary
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = EMFADColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )

            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = EMFADColors.TextTertiary
                )
            }
        }
    }
}

@Composable
private fun EMFADCalibrationControls(
    onSaveCalibration: () -> Unit,
    onLoadCalibration: () -> Unit,
    onDeleteAllCalibration: () -> Unit,
    lastCalibrationTime: Long
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
                text = "Kalibrierungs-Verwaltung",
                style = MaterialTheme.typography.titleMedium,
                color = EMFADColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )

            if (lastCalibrationTime > 0) {
                Text(
                    text = "Letzte Kalibrierung: ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(lastCalibrationTime))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = EMFADColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Kontroll-Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSaveCalibration,
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
                    onClick = onLoadCalibration,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADColors.EMFADBlue
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Load",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Laden")
                }

                Button(
                    onClick = onDeleteAllCalibration,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADColors.StatusWarning
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Löschen")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info-Text
            Text(
                text = "Kalibrierungsdaten werden automatisch gespeichert und bei jedem Start geladen. Manuelle Verwaltung für Backup und Wiederherstellung.",
                style = MaterialTheme.typography.bodySmall,
                color = EMFADColors.TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun DrawScope.drawCompass(heading: Float, accuracy: Float, isCalibrating: Boolean) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = size.minDimension / 2 - 20f
    
    // Kompass-Ring
    drawCircle(
        color = EMFADColors.EMFADGray,
        radius = radius,
        center = center,
        style = Stroke(width = 4f)
    )
    
    // Himmelsrichtungen
    val directions = listOf("N", "E", "S", "W")
    directions.forEachIndexed { index, direction ->
        val angle = index * 90f
        val radians = Math.toRadians(angle.toDouble())
        val textX = center.x + (radius - 30f) * cos(radians).toFloat()
        val textY = center.y + (radius - 30f) * sin(radians).toFloat()
        
        // Markierungen
        val startX = center.x + (radius - 15f) * cos(radians).toFloat()
        val startY = center.y + (radius - 15f) * sin(radians).toFloat()
        val endX = center.x + radius * cos(radians).toFloat()
        val endY = center.y + radius * sin(radians).toFloat()
        
        drawLine(
            color = EMFADColors.TextSecondary,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 2f
        )
    }
    
    // Kompass-Nadel
    val needleAngle = Math.toRadians((heading - 90).toDouble())
    val needleLength = radius * 0.7f
    val needleEndX = center.x + needleLength * cos(needleAngle).toFloat()
    val needleEndY = center.y + needleLength * sin(needleAngle).toFloat()
    
    // Nadel-Farbe basierend auf Genauigkeit
    val needleColor = when {
        accuracy > 0.8f -> EMFADColors.SignalGreen
        accuracy > 0.5f -> EMFADColors.EMFADYellow
        else -> EMFADColors.SignalRed
    }
    
    drawLine(
        color = needleColor,
        start = center,
        end = Offset(needleEndX, needleEndY),
        strokeWidth = 6f
    )
    
    // Zentrum
    drawCircle(
        color = needleColor,
        radius = 8f,
        center = center
    )
    
    // Kalibrierungs-Animation
    if (isCalibrating) {
        drawCircle(
            color = EMFADColors.EMFADBlue.copy(alpha = 0.3f),
            radius = radius * 0.9f,
            center = center,
            style = Stroke(width = 8f)
        )
    }
}
