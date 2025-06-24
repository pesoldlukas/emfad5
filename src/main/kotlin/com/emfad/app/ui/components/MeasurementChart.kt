package com.emfad.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emfad.app.models.MeasurementResult
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MeasurementChart(
    measurements: List<MeasurementResult>,
    modifier: Modifier = Modifier,
    showGrid: Boolean = true,
    showLabels: Boolean = true
) {
    if (measurements.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Keine Messdaten verfügbar",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val values = measurements.map { it.signalStrength.toFloat() }
    val minValue = values.minOrNull() ?: 0f
    val maxValue = values.maxOrNull() ?: 0f
    val valueRange = maxValue - minValue

    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timestamps = measurements.map { dateFormat.format(it.timestamp) }

    Column(modifier = modifier) {
        if (showLabels) {
            Text(
                text = "Messwerte über Zeit",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val xStep = width / (measurements.size - 1)
                val yStep = height / valueRange

                // Grid
                if (showGrid) {
                    val gridColor = Color.Gray.copy(alpha = 0.2f)
                    val gridStroke = Stroke(width = 1f)
                    
                    // Horizontal grid lines
                    for (i in 0..4) {
                        val y = height * (1 - i / 4f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }

                    // Vertical grid lines
                    for (i in 0..4) {
                        val x = width * (i / 4f)
                        drawLine(
                            color = gridColor,
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = 1f
                        )
                    }
                }

                // Data line
                val path = Path()
                measurements.forEachIndexed { index, measurement ->
                    val x = index * xStep
                    val y = height - (measurement.signalStrength.toFloat() - minValue) * yStep
                    
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = MaterialTheme.colorScheme.primary,
                    style = Stroke(
                        width = 2f,
                        cap = StrokeCap.Round
                    )
                )
            }
        }

        if (showLabels) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = timestamps.firstOrNull() ?: "",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = timestamps.lastOrNull() ?: "",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format("%.1f", minValue),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = String.format("%.1f", maxValue),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
} 