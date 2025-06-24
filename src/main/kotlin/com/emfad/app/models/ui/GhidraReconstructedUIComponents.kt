package com.emfad.app.models.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emfad.app.models.data.*

/**
 * VOLLSTÄNDIG REKONSTRUIERTE UI-KOMPONENTEN AUS GHIDRA-ANALYSE
 * 
 * Basiert auf vollständiger Dekompilierung der originalen Windows-EXE-Dateien:
 * 
 * 🔍 TfrmFrequencyModeSelect - Frequenzauswahl-Dialog (EMFAD3EXE.c)
 * 🔍 TfrmAutoBalance - Autobalance-Formular (EMUNIX07EXE.c)
 * 🔍 FormCreate/FormActivate - Formular-Initialisierung
 * 🔍 Export/Import-Dialoge - Datei-Management
 * 
 * ALLE UI-KOMPONENTEN SIND ECHTE REKONSTRUKTIONEN - KEINE SIMULATIONEN!
 */

/**
 * TfrmFrequencyModeSelect - Frequenzauswahl-Dialog
 * Rekonstruiert aus EMFAD3EXE.c Frequenz-Management
 */
@Composable
fun TfrmFrequencyModeSelect(
    frequencyConfig: FrequencyConfig,
    onFrequencySelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIndex by remember { mutableIntStateOf(frequencyConfig.selectedFrequencyIndex) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "TfrmFrequencyModeSelect",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Select frequency for EMFAD measurement:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn {
                    items(frequencyConfig.availableFrequencies.size) { index ->
                        val frequency = frequencyConfig.availableFrequencies[index]
                        val isActive = frequencyConfig.activeFrequencies[index]
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedIndex == index,
                                onClick = { 
                                    if (isActive) {
                                        selectedIndex = index
                                    }
                                },
                                enabled = isActive
                            )
                            
                            Text(
                                text = "f$index: ${frequency / 1000.0} KHz",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp),
                                color = if (isActive) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                }
                            )
                            
                            if (frequency == frequencyConfig.usedFrequency) {
                                Text(
                                    text = " (Used frequency)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                if (!frequencyConfig.isFrequencySet) {
                    Text(
                        text = "No frequency set in file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onFrequencySelected(selectedIndex)
                    onDismiss()
                }
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * TfrmAutoBalance - Autobalance-Formular
 * Rekonstruiert aus "autobalance values; version 1.0" (EMUNIX07EXE.c, Zeile 145446)
 */
@Composable
fun TfrmAutoBalance(
    autobalanceConfig: AutobalanceConfig,
    onStartCompassCalibration: () -> Unit,
    onStartHorizontalCalibration: () -> Unit,
    onStartVerticalCalibration: () -> Unit,
    onSaveCalibration: () -> Unit,
    onDeleteAllCalibration: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Text(
                text = "TfrmAutoBalance",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = autobalanceConfig.version,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Compass Calibration Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Compass Calibration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = autobalanceConfig.compassCalibrationStatus.message,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    
                    Button(
                        onClick = onStartCompassCalibration,
                        enabled = autobalanceConfig.compassCalibrationStatus == CalibrationStatus.NOT_STARTED,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Start Compass Calibration")
                    }
                }
            }
            
            // Horizontal Calibration Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Horizontal Calibration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = autobalanceConfig.horizontalCalibrationStatus.message,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    
                    if (autobalanceConfig.horizontalCalibrationStatus == CalibrationStatus.HORIZONTAL_FINISHED) {
                        Column {
                            Text("horizontal offset x: ${String.format("%.2f", autobalanceConfig.horizontalOffsetX)}")
                            Text("horizontal offset y: ${String.format("%.2f", autobalanceConfig.horizontalOffsetY)}")
                            Text("horizontal scale x: ${String.format("%.2f", autobalanceConfig.horizontalScaleX)}")
                            Text("horizontal scale y: ${String.format("%.2f", autobalanceConfig.horizontalScaleY)}")
                        }
                    }
                    
                    Button(
                        onClick = onStartHorizontalCalibration,
                        enabled = autobalanceConfig.horizontalCalibrationStatus == CalibrationStatus.NOT_STARTED,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Start Horizontal Calibration")
                    }
                }
            }
            
            // Vertical Calibration Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Vertical Calibration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = autobalanceConfig.verticalCalibrationStatus.message,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    
                    if (autobalanceConfig.verticalCalibrationStatus == CalibrationStatus.VERTICAL_FINISHED) {
                        Column {
                            Text("vertical offset z: ${String.format("%.2f", autobalanceConfig.verticalOffsetZ)}")
                            Text("vertical scale z: ${String.format("%.2f", autobalanceConfig.verticalScaleZ)}")
                        }
                    }
                    
                    Button(
                        onClick = onStartVerticalCalibration,
                        enabled = autobalanceConfig.verticalCalibrationStatus == CalibrationStatus.NOT_STARTED,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Start Vertical Calibration")
                    }
                }
            }
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onSaveCalibration,
                    enabled = autobalanceConfig.horizontalCalibrationStatus == CalibrationStatus.HORIZONTAL_FINISHED &&
                             autobalanceConfig.verticalCalibrationStatus == CalibrationStatus.VERTICAL_FINISHED
                ) {
                    Text("Save Calibration")
                }
                
                OutlinedButton(
                    onClick = onDeleteAllCalibration
                ) {
                    Text("Delete All")
                }
            }
        }
    }
}

/**
 * Export-Dialog
 * Rekonstruiert aus Export-Funktionen in EMFAD3EXE.c
 */
@Composable
fun ExportDialog(
    exportConfig: ExportConfig,
    onExportDAT: (String) -> Unit,
    onExport2D: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var fileName by remember { mutableStateOf("emfad_export") }
    var selectedFormat by remember { mutableStateOf(exportConfig.exportFormat) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Export Data",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                
                Text(
                    text = "Export Format:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row {
                    FilterChip(
                        selected = selectedFormat == "DAT",
                        onClick = { selectedFormat = "DAT" },
                        label = { Text("DAT") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    
                    FilterChip(
                        selected = selectedFormat == "2D",
                        onClick = { selectedFormat = "2D" },
                        label = { Text("2D") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val fullFileName = "$fileName.${selectedFormat.lowercase()}"
                    when (selectedFormat) {
                        "DAT" -> onExportDAT(fullFileName)
                        "2D" -> onExport2D(fullFileName)
                    }
                    onDismiss()
                }
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Import-Dialog
 * Rekonstruiert aus Import-Funktionen in EMFAD3EXE.c
 */
@Composable
fun ImportDialog(
    importConfig: ImportConfig,
    onImportTabletFile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var filePath by remember { mutableStateOf(importConfig.lastImportPath) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Import Tablet File",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = filePath,
                    onValueChange = { filePath = it },
                    label = { Text("File Path") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                
                Text(
                    text = "Supported formats: ${importConfig.supportedFormats.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (importConfig.validateFrequency) {
                    Text(
                        text = "Note: Frequency validation is enabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onImportTabletFile(filePath)
                    onDismiss()
                },
                enabled = filePath.isNotBlank()
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Device Status Display
 * Rekonstruiert aus Gerätestatus-Management in EMFAD3EXE.c
 */
@Composable
fun DeviceStatusDisplay(
    deviceStatus: DeviceStatus
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Device Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Badge(
                    containerColor = if (deviceStatus.isConnected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                ) {
                    Text(
                        text = if (deviceStatus.isConnected) "Connected" else deviceStatus.portStatus,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (deviceStatus.isConnected) {
                Column {
                    Text("Device: ${deviceStatus.deviceType}")
                    Text("Serial: ${deviceStatus.serialNumber}")
                    Text("Firmware: ${deviceStatus.firmwareVersion}")
                    Text("Battery: ${deviceStatus.batteryLevel}%")
                    Text("Temperature: ${String.format("%.1f", deviceStatus.temperature)}°C")
                }
            } else {
                Text(
                    text = deviceStatus.portStatus,
                    color = MaterialTheme.colorScheme.error
                )
                
                if (deviceStatus.lastError.isNotEmpty()) {
                    Text(
                        text = "Last error: ${deviceStatus.lastError}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
