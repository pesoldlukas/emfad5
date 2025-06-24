package com.emfad.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emfad.app.models.MeasurementStatistics

@Composable
fun StatisticsCard(
    statistics: MeasurementStatistics,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Statistiken",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = "Durchschnitt",
                    value = statistics.average,
                    unit = unit
                )
                StatisticItem(
                    label = "Minimum",
                    value = statistics.min,
                    unit = unit
                )
                StatisticItem(
                    label = "Maximum",
                    value = statistics.max,
                    unit = unit
                )
            }
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: Float,
    unit: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = String.format("%.1f %s", value, unit),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }
} 