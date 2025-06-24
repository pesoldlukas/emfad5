package com.emfad.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.emfad.app.ui.components.MeasurementChart
import com.emfad.app.ui.components.StatisticsCard
import com.emfad.app.viewmodels.AnalysisViewModel
import com.emfad.app.viewmodels.ChartType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    navController: NavController,
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analyse") },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavBar(navController)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                if (state.selectedSession != null) {
                    // Statistik-Karte
                    state.sessionStatistics?.let { statistics ->
                        StatisticsCard(
                            statistics = statistics,
                            unit = state.measurementUnit,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    // Messwert-Grafik
                    MeasurementChart(
                        measurements = state.sessionMeasurements,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Wählen Sie eine Messung aus",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Messungsliste
            items(state.sessionMeasurements) { measurement ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = dateFormat.format(Date(measurement.timestamp)),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Wert: ${measurement.value} ${state.measurementUnit}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        measurement.location?.let { location ->
                            Text(
                                text = "Standort: $location",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // Ladeindikator
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Fehleranzeige
        state.error?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(error)
            }
        }
    }
}

@Composable
private fun StatisticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
} 