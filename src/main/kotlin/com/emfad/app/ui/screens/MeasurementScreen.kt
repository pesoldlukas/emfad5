package com.emfad.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.emfad.app.viewmodels.MeasurementViewModel
import kotlinx.coroutines.flow.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementScreen(
    navController: NavController,
    viewModel: MeasurementViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EMF Measurement") },
                actions = {
                    if (state.isConnected) {
                        IconButton(onClick = { viewModel.disconnect() }) {
                            Icon(Icons.Default.BluetoothDisabled, "Disconnect")
                        }
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current Measurement Display
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Current Measurement",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${state.currentValue} ${state.unit}",
                        style = MaterialTheme.typography.headlineLarge
                    )
                }
            }

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { viewModel.toggleRecording() },
                    enabled = state.isConnected
                ) {
                    Icon(
                        if (state.isRecording) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (state.isRecording) "Stop Recording" else "Start Recording"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (state.isRecording) "Stop" else "Record")
                }

                Button(
                    onClick = { 
                        if (state.isScanning) viewModel.stopScan() else viewModel.startScan()
                    }
                ) {
                    Icon(
                        if (state.isScanning) Icons.Default.Stop else Icons.Default.Search,
                        contentDescription = if (state.isScanning) "Stop Scanning" else "Start Scanning"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (state.isScanning) "Stop Scan" else "Scan")
                }
            }

            // Error Display
            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Recorded Values
            if (state.recordedValues.isNotEmpty()) {
                Text(
                    text = "Recorded Values",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(state.recordedValues) { value ->
                        Text(
                            text = "$value ${state.unit}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
} 