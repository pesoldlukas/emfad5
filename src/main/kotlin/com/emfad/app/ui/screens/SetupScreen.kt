package com.emfad.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emfad.app.ui.theme.EMFADColors

/**
 * EMFAD® Setup Screen
 * Basiert auf Setup-Dialog der originalen Windows-Software
 * Implementiert Frequenzwahl, Gain/Offset, Modus-Einstellungen
 */

data class EMFADFrequency(
    val value: Double,
    val label: String,
    val isActive: Boolean = true
)

enum class EMFADMeasurementMode(val displayName: String) {
    A("A"),
    B("B"),
    A_MINUS_B("A - B"),
    B_MINUS_A("B - A"),
    A_AND_B("A & B"),
    FULL_SPECTRUM("full spectrum")
}

enum class EMFADScanPattern(val displayName: String) {
    PARALLEL("parallel"),
    MEANDER("meander"),
    HORIZONTAL("horizontal"),
    VERTICAL("vertical")
}

data class EMFADSetupConfig(
    val selectedFrequency: EMFADFrequency,
    val measurementMode: EMFADMeasurementMode,
    val scanPattern: EMFADScanPattern,
    val gainValue: Float,
    val offsetValue: Float,
    val autoInterval: Int,
    val isAutoModeEnabled: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    navController: androidx.navigation.NavController,
    modifier: Modifier = Modifier
) {
    // Verwende das ECHTE SetupViewModel mit echtem FrequencyManager
    val viewModel: com.emfad.app.viewmodels.SetupViewModel = androidx.hilt.navigation.compose.hiltViewModel()

    // Sammle echte Daten vom Backend
    val setupConfig by viewModel.setupConfig.collectAsState()
    val availableFrequencies by viewModel.availableFrequencies.collectAsState()
    val isConfigSaved by viewModel.isConfigSaved.collectAsState()

    // Echte UI mit echten Backend-Daten
    RealSetupUI(
        setupConfig = setupConfig,
        availableFrequencies = availableFrequencies,
        onConfigChange = { config -> viewModel.updateSetupConfig(config) },
        onSaveConfig = { viewModel.saveConfiguration() },
        onLoadConfig = { viewModel.loadConfiguration() },
        onResetConfig = { viewModel.resetConfiguration() },
        onBack = { navController.popBackStack() },
        modifier = modifier
    )

    // Zeige Speicher-Bestätigung
    if (isConfigSaved) {
        LaunchedEffect(isConfigSaved) {
            // TODO: Zeige Snackbar oder Toast
        }
    }
}

@Composable
private fun RealSetupUI(
    setupConfig: EMFADSetupConfig,
    availableFrequencies: List<com.emfad.app.services.frequency.EMFADFrequency>,
    onConfigChange: (EMFADSetupConfig) -> Unit,
    onSaveConfig: () -> Unit,
    onLoadConfig: () -> Unit,
    onResetConfig: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(EMFADColors.BackgroundPrimary)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header
            EMFADSetupHeader(onBack = onBack)
        }
        
        item {
            // Frequenzwahl (7 EMFAD-Frequenzen)
            EMFADFrequencySelection(
                availableFrequencies = availableFrequencies,
                selectedFrequency = setupConfig.selectedFrequency,
                onFrequencySelected = { frequency ->
                    onConfigChange(setupConfig.copy(selectedFrequency = frequency))
                }
            )
        }
        
        item {
            // Messmodus-Auswahl
            EMFADMeasurementModeSelection(
                selectedMode = setupConfig.measurementMode,
                onModeSelected = { mode ->
                    onConfigChange(setupConfig.copy(measurementMode = mode))
                }
            )
        }
        
        item {
            // Gain und Offset Einstellungen
            EMFADGainOffsetSettings(
                gainValue = setupConfig.gainValue,
                offsetValue = setupConfig.offsetValue,
                onGainChange = { gain ->
                    onConfigChange(setupConfig.copy(gainValue = gain))
                },
                onOffsetChange = { offset ->
                    onConfigChange(setupConfig.copy(offsetValue = offset))
                }
            )
        }
        
        item {
            // Scan-Pattern Auswahl
            EMFADScanPatternSelection(
                selectedPattern = setupConfig.scanPattern,
                onPatternSelected = { pattern ->
                    onConfigChange(setupConfig.copy(scanPattern = pattern))
                }
            )
        }
        
        item {
            // Auto-Intervall Einstellungen
            EMFADAutoIntervalSettings(
                autoInterval = setupConfig.autoInterval,
                isAutoModeEnabled = setupConfig.isAutoModeEnabled,
                onIntervalChange = { interval ->
                    onConfigChange(setupConfig.copy(autoInterval = interval))
                },
                onAutoModeToggle = { enabled ->
                    onConfigChange(setupConfig.copy(isAutoModeEnabled = enabled))
                }
            )
        }
        
        item {
            // Aktions-Buttons
            EMFADSetupActions(
                onSaveConfig = onSaveConfig,
                onLoadConfig = onLoadConfig,
                onResetConfig = onResetConfig
            )
        }
    }
}

@Composable
private fun EMFADSetupHeader(onBack: () -> Unit) {
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
                    text = "EMFAD® Setup",
                    style = MaterialTheme.typography.headlineMedium,
                    color = EMFADColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Geräte- und Messkonfiguration",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EMFADColors.TextSecondary
                )
            }
            
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Setup",
                modifier = Modifier.size(32.dp),
                tint = EMFADColors.EMFADBlue
            )
        }
    }
}

@Composable
private fun EMFADFrequencySelection(
    availableFrequencies: List<EMFADFrequency>,
    selectedFrequency: EMFADFrequency,
    onFrequencySelected: (EMFADFrequency) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EMFADColors.SurfacePrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Frequenzwahl",
                style = MaterialTheme.typography.titleLarge,
                color = EMFADColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "EMFAD-Frequenzen (19 - 135.6 kHz)",
                style = MaterialTheme.typography.bodyMedium,
                color = EMFADColors.TextSecondary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 7 Frequenz-Buttons in Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(200.dp)
            ) {
                items(availableFrequencies) { frequency ->
                    EMFADFrequencyButton(
                        frequency = frequency,
                        isSelected = frequency.value == selectedFrequency.value,
                        onClick = { onFrequencySelected(frequency) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EMFADFrequencyButton(
    frequency: EMFADFrequency,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) EMFADColors.EMFADBlue else EMFADColors.SurfaceSecondary
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = frequency.label,
                style = MaterialTheme.typography.titleSmall,
                color = if (isSelected) EMFADColors.TextPrimary else EMFADColors.TextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "${frequency.value.toInt()} Hz",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) EMFADColors.TextPrimary else EMFADColors.TextTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EMFADScanPatternSelection(
    selectedPattern: EMFADScanPattern,
    onPatternSelected: (EMFADScanPattern) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EMFADColors.SurfacePrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Scan-Muster",
                style = MaterialTheme.typography.titleLarge,
                color = EMFADColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Messraster-Pattern für 2D/3D-Scans",
                style = MaterialTheme.typography.bodyMedium,
                color = EMFADColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pattern-Buttons
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(120.dp)
            ) {
                items(EMFADScanPattern.values().toList()) { pattern ->
                    EMFADPatternButton(
                        pattern = pattern,
                        isSelected = pattern == selectedPattern,
                        onClick = { onPatternSelected(pattern) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EMFADPatternButton(
    pattern: EMFADScanPattern,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) EMFADColors.StatusInfo else EMFADColors.SurfaceSecondary
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        ),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = pattern.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) EMFADColors.TextPrimary else EMFADColors.TextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EMFADAutoIntervalSettings(
    autoInterval: Int,
    isAutoModeEnabled: Boolean,
    onIntervalChange: (Int) -> Unit,
    onAutoModeToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EMFADColors.SurfacePrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Auto-Messung",
                style = MaterialTheme.typography.titleLarge,
                color = EMFADColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Automatische Messintervalle konfigurieren",
                style = MaterialTheme.typography.bodyMedium,
                color = EMFADColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Auto-Modus Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Auto-Modus aktivieren",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EMFADColors.TextPrimary
                    )

                    Text(
                        text = "Kontinuierliche Messungen",
                        style = MaterialTheme.typography.bodySmall,
                        color = EMFADColors.TextSecondary
                    )
                }

                Switch(
                    checked = isAutoModeEnabled,
                    onCheckedChange = onAutoModeToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EMFADColors.EMFADBlue,
                        checkedTrackColor = EMFADColors.EMFADBlue.copy(alpha = 0.5f),
                        uncheckedThumbColor = EMFADColors.EMFADGray,
                        uncheckedTrackColor = EMFADColors.EMFADGray.copy(alpha = 0.5f)
                    )
                )
            }

            if (isAutoModeEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                // Intervall-Einstellung
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Messintervall:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EMFADColors.TextPrimary
                        )

                        Text(
                            text = "${autoInterval}s",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EMFADColors.EMFADBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Slider(
                        value = autoInterval.toFloat(),
                        onValueChange = { onIntervalChange(it.toInt()) },
                        valueRange = 1f..60f,
                        steps = 59,
                        colors = SliderDefaults.colors(
                            thumbColor = EMFADColors.EMFADBlue,
                            activeTrackColor = EMFADColors.EMFADBlue,
                            inactiveTrackColor = EMFADColors.EMFADGray
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "1s",
                            style = MaterialTheme.typography.labelSmall,
                            color = EMFADColors.TextTertiary
                        )

                        Text(
                            text = "60s",
                            style = MaterialTheme.typography.labelSmall,
                            color = EMFADColors.TextTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EMFADSetupActions(
    onSaveConfig: () -> Unit,
    onLoadConfig: () -> Unit,
    onResetConfig: () -> Unit
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
                text = "Konfiguration verwalten",
                style = MaterialTheme.typography.titleMedium,
                color = EMFADColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Aktions-Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSaveConfig,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADColors.StatusConnected
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Speichern",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Speichern")
                }

                Button(
                    onClick = onLoadConfig,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADColors.EMFADBlue
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Laden",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Laden")
                }

                Button(
                    onClick = onResetConfig,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADColors.StatusWarning
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info-Text
            Text(
                text = "Konfigurationen werden lokal gespeichert und können zwischen Sessions wiederverwendet werden.",
                style = MaterialTheme.typography.bodySmall,
                color = EMFADColors.TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EMFADMeasurementModeSelection(
    selectedMode: EMFADMeasurementMode,
    onModeSelected: (EMFADMeasurementMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EMFADColors.SurfacePrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Messmodus",
                style = MaterialTheme.typography.titleLarge,
                color = EMFADColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Auswahl der Messkanäle (A, B, A-B, B-A)",
                style = MaterialTheme.typography.bodyMedium,
                color = EMFADColors.TextSecondary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Modus-Buttons
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(120.dp)
            ) {
                items(EMFADMeasurementMode.values().toList()) { mode ->
                    EMFADModeButton(
                        mode = mode,
                        isSelected = mode == selectedMode,
                        onClick = { onModeSelected(mode) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EMFADModeButton(
    mode: EMFADMeasurementMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) EMFADColors.EMFADYellow else EMFADColors.SurfaceSecondary
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        ),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = mode.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) EMFADColors.EMFADBlack else EMFADColors.TextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EMFADGainOffsetSettings(
    gainValue: Float,
    offsetValue: Float,
    onGainChange: (Float) -> Unit,
    onOffsetChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EMFADColors.SurfacePrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Verstärkung & Offset",
                style = MaterialTheme.typography.titleLarge,
                color = EMFADColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Signalverstärkung und Nullpunkt-Korrektur",
                style = MaterialTheme.typography.bodyMedium,
                color = EMFADColors.TextSecondary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Gain Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gain:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EMFADColors.TextPrimary
                    )
                    
                    Text(
                        text = "%.1fx".format(gainValue),
                        style = MaterialTheme.typography.bodyMedium,
                        color = EMFADColors.EMFADBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Slider(
                    value = gainValue,
                    onValueChange = onGainChange,
                    valueRange = 0.1f..10.0f,
                    steps = 99,
                    colors = SliderDefaults.colors(
                        thumbColor = EMFADColors.EMFADBlue,
                        activeTrackColor = EMFADColors.EMFADBlue,
                        inactiveTrackColor = EMFADColors.EMFADGray
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Offset Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Offset:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EMFADColors.TextPrimary
                    )
                    
                    Text(
                        text = "%.2f mV".format(offsetValue),
                        style = MaterialTheme.typography.bodyMedium,
                        color = EMFADColors.EMFADBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Slider(
                    value = offsetValue,
                    onValueChange = onOffsetChange,
                    valueRange = -100.0f..100.0f,
                    steps = 200,
                    colors = SliderDefaults.colors(
                        thumbColor = EMFADColors.EMFADYellow,
                        activeTrackColor = EMFADColors.EMFADYellow,
                        inactiveTrackColor = EMFADColors.EMFADGray
                    )
                )
            }
        }
    }
}
