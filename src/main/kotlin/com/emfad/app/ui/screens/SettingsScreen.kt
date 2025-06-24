package com.emfad.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.emfad.app.ui.components.BottomNavBar
import com.emfad.app.viewmodels.ExportFormat
import com.emfad.app.viewmodels.MeasurementUnit
import com.emfad.app.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportData(emptyList(), it) }
    }

    LaunchedEffect(state.exportSuccess) {
        if (state.exportSuccess) {
            showExportDialog = false
            viewModel.clearExportSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Exportieren")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavBar(navController)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Messungseinstellungen
            SettingsSection(title = "Messungseinstellungen") {
                SettingsSlider(
                    value = state.measurementInterval.toFloat(),
                    onValueChange = { viewModel.updateMeasurementInterval(it.toInt()) },
                    valueRange = 100f..5000f,
                    steps = 49,
                    label = "Messintervall (ms)"
                )
                
                SettingsDropdown(
                    value = state.measurementUnit,
                    onValueChange = { viewModel.updateUnit(it) },
                    options = listOf("µT", "mG", "V/m"),
                    label = "Messeinheit"
                )
            }

            // Verbindungseinstellungen
            SettingsSection(title = "Verbindungseinstellungen") {
                SettingsSwitch(
                    checked = state.autoConnect,
                    onCheckedChange = { viewModel.toggleAutoConnect() },
                    label = "Automatisch verbinden"
                )
            }

            // Erscheinungsbild
            SettingsSection(title = "Erscheinungsbild") {
                SettingsSwitch(
                    checked = state.darkMode,
                    onCheckedChange = { viewModel.toggleDarkMode() },
                    label = "Dunkles Design"
                )
            }

            // Dateneinstellungen
            SettingsSection(title = "Dateneinstellungen") {
                SettingsSlider(
                    value = state.dataRetentionDays.toFloat(),
                    onValueChange = { viewModel.updateDataRetention(it.toInt()) },
                    valueRange = 1f..365f,
                    steps = 364,
                    label = "Datenaufbewahrung (Tage)"
                )

                SettingsDropdown(
                    value = state.exportFormat,
                    onValueChange = { viewModel.updateExportFormat(it) },
                    options = listOf("CSV", "JSON"),
                    label = "Export-Format"
                )
            }

            // Fehleranzeige
            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            // Speicherindikator
            if (state.isExporting) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Daten exportieren") },
            text = { Text("Möchten Sie die Messdaten exportieren?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        exportLauncher.launch("emfad_export.${state.exportFormat.lowercase()}")
                    }
                ) {
                    Text("Exportieren")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun SettingsSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    label: String
) {
    Column {
        Text(text = "$label: ${value.toInt()}")
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SettingsSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    options: List<String>,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(text = label)
        Box {
            TextButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = value,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
} 