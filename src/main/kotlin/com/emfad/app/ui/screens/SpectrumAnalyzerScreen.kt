package com.emfad.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emfad.app.ui.theme.EMFADColors
import kotlin.math.*

/**
 * EMFAD® Spectrum Analyzer Screen
 * Basiert auf "Spec"-View der originalen Windows-Software
 * Implementiert Frequenzspektrum-Visualisierung mit Interaktion
 */

data class SpectrumData(
    val frequency: Double,
    val amplitude: Double,
    val phase: Double,
    val quality: Double,
    val isSelected: Boolean = false
)

data class SpectrumAnalysis(
    val spectrumData: List<SpectrumData>,
    val peakFrequency: Double,
    val averageAmplitude: Double,
    val noiseFloor: Double,
    val snrRatio: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpectrumAnalyzerScreen(
    navController: androidx.navigation.NavController,
    modifier: Modifier = Modifier
) {
    // Verwende das ECHTE AnalysisViewModel mit echten AI-Komponenten
    val analysisViewModel: com.emfad.app.viewmodels.analysis.AnalysisViewModel = androidx.hilt.navigation.compose.hiltViewModel()

    // Sammle echte Daten vom Backend
    val analysisState by analysisViewModel.analysisState.collectAsState()
    val uiState by analysisViewModel.uiState.collectAsState()
    val materialAnalyses by analysisViewModel.materialAnalyses.collectAsState()
    val clusterResults by analysisViewModel.clusterResults.collectAsState()

    // Erstelle Spektrum-Daten aus echten Messungen
    val spectrumAnalysis = remember(analysisState.sessionReadings) {
        createSpectrumFromReadings(analysisState.sessionReadings)
    }

    // Echte UI mit echten Backend-Daten
    RealSpectrumAnalyzerUI(
        spectrumAnalysis = spectrumAnalysis,
        isRecording = uiState.isAnalyzing,
        selectedFrequency = null, // TODO: Implementiere Frequenz-Auswahl
        onFrequencySelected = { /* TODO: Implementiere */ },
        onStartRecording = { analysisViewModel.performCompleteAnalysis() },
        onStopRecording = { /* TODO: Implementiere Stop */ },
        onExportSpectrum = { analysisViewModel.exportAnalysis(com.emfad.app.viewmodels.analysis.ExportFormat.CSV) },
        onClearSpectrum = { /* TODO: Implementiere Clear */ },
        onBack = { navController.popBackStack() },
        modifier = modifier
    )
}

@Composable
private fun RealSpectrumAnalyzerUI(
    spectrumAnalysis: SpectrumAnalysis,
    isRecording: Boolean,
    selectedFrequency: Double?,
    onFrequencySelected: (Double) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onExportSpectrum: () -> Unit,
    onClearSpectrum: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EMFADColors.BackgroundPrimary)
            .padding(16.dp)
    ) {
        // Header
        EMFADSpectrumHeader(
            onBack = onBack,
            isRecording = isRecording,
            peakFrequency = spectrumAnalysis.peakFrequency
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Spektrum-Anzeige (Hauptkomponente)
        EMFADSpectrumDisplay(
            spectrumData = spectrumAnalysis.spectrumData,
            selectedFrequency = selectedFrequency,
            onFrequencySelected = onFrequencySelected,
            modifier = Modifier.weight(0.6f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Spektrum-Analyse-Info
        EMFADSpectrumAnalysisInfo(
            analysis = spectrumAnalysis,
            modifier = Modifier.weight(0.2f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Kontrollen
        EMFADSpectrumControls(
            isRecording = isRecording,
            onStartRecording = onStartRecording,
            onStopRecording = onStopRecording,
            onExportSpectrum = onExportSpectrum,
            onClearSpectrum = onClearSpectrum
        )
    }
}

@Composable
private fun EMFADSpectrumHeader(
    onBack: () -> Unit,
    isRecording: Boolean,
    peakFrequency: Double
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
                    text = "EMFAD® Spektrum-Analyzer",
                    style = MaterialTheme.typography.headlineMedium,
                    color = EMFADColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Frequenzspektrum-Analyse (19-135.6 kHz)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EMFADColors.TextSecondary
                )
            }
            
            // Status und Peak-Frequenz
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isRecording) {
                        Icon(
                            imageVector = Icons.Default.FiberManualRecord,
                            contentDescription = "Recording",
                            modifier = Modifier.size(12.dp),
                            tint = EMFADColors.SignalRed
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Aufzeichnung",
                            style = MaterialTheme.typography.labelSmall,
                            color = EMFADColors.SignalRed
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Spectrum",
                            modifier = Modifier.size(16.dp),
                            tint = EMFADColors.EMFADBlue
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Bereit",
                            style = MaterialTheme.typography.labelSmall,
                            color = EMFADColors.EMFADBlue
                        )
                    }
                }
                
                Text(
                    text = "Peak: ${peakFrequency.toInt()} Hz",
                    style = MaterialTheme.typography.labelMedium,
                    color = EMFADColors.EMFADYellow,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EMFADSpectrumDisplay(
    spectrumData: List<SpectrumData>,
    selectedFrequency: Double?,
    onFrequencySelected: (Double) -> Unit,
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
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Frequenzspektrum",
                    style = MaterialTheme.typography.titleLarge,
                    color = EMFADColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                
                if (selectedFrequency != null) {
                    Text(
                        text = "Ausgewählt: ${selectedFrequency.toInt()} Hz",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EMFADColors.EMFADYellow,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Spektrum-Canvas mit Touch-Interaktion
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(EMFADColors.BackgroundSecondary)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            // Berechne angeklickte Frequenz basierend auf Touch-Position
                            val width = size.width.toFloat()
                            val padding = 40f
                            val relativeX = (offset.x - padding) / (width - 2 * padding)
                            
                            if (relativeX >= 0f && relativeX <= 1f && spectrumData.isNotEmpty()) {
                                val index = (relativeX * (spectrumData.size - 1)).toInt()
                                    .coerceIn(0, spectrumData.size - 1)
                                onFrequencySelected(spectrumData[index].frequency)
                            }
                        }
                    }
            ) {
                drawEMFSpectrum(spectrumData, selectedFrequency)
            }
        }
    }
}

private fun DrawScope.drawEMFSpectrum(
    spectrumData: List<SpectrumData>,
    selectedFrequency: Double?
) {
    if (spectrumData.isEmpty()) return
    
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
    
    // Gitter zeichnen
    val gridLines = 5
    for (i in 1 until gridLines) {
        val y = padding + (i * (height - 2 * padding) / gridLines)
        drawLine(
            color = EMFADColors.EMFADGray.copy(alpha = 0.3f),
            start = Offset(padding, y),
            end = Offset(width - padding, y),
            strokeWidth = 1f
        )
    }
    
    // Spektrum-Balken zeichnen
    val maxAmplitude = spectrumData.maxOfOrNull { it.amplitude } ?: 1.0
    val barWidth = (width - 2 * padding) / spectrumData.size
    
    spectrumData.forEachIndexed { index, data ->
        val x = padding + index * barWidth
        val barHeight = ((data.amplitude / maxAmplitude) * (height - 2 * padding)).toFloat()
        val y = height - padding - barHeight
        
        // Balken-Farbe basierend auf Qualität und Auswahl
        val barColor = when {
            data.frequency == selectedFrequency -> EMFADColors.EMFADYellow
            data.quality > 0.8 -> EMFADColors.SignalGreen
            data.quality > 0.5 -> EMFADColors.EMFADBlue
            data.quality > 0.2 -> EMFADColors.SignalOrange
            else -> EMFADColors.SignalRed
        }
        
        // Spektrum-Balken
        drawRect(
            color = barColor,
            topLeft = Offset(x, y),
            size = Size(barWidth * 0.8f, barHeight)
        )
        
        // Ausgewählte Frequenz hervorheben
        if (data.frequency == selectedFrequency) {
            drawRect(
                color = EMFADColors.EMFADYellow,
                topLeft = Offset(x - 2f, y - 2f),
                size = Size(barWidth * 0.8f + 4f, barHeight + 4f),
                style = Stroke(width = 3f)
            )
        }
    }
    
    // Frequenz-Labels (nur für EMFAD-Hauptfrequenzen)
    val emfadFrequencies = listOf(19000.0, 23400.0, 70000.0, 77500.0, 124000.0, 129100.0, 135600.0)
    emfadFrequencies.forEach { freq ->
        val index = spectrumData.indexOfFirst { abs(it.frequency - freq) < 1000 }
        if (index >= 0) {
            val x = padding + index * barWidth + barWidth * 0.4f
            
            // Frequenz-Marker
            drawLine(
                color = EMFADColors.EMFADBlue,
                start = Offset(x, height - padding),
                end = Offset(x, height - padding + 10f),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
private fun EMFADSpectrumAnalysisInfo(
    analysis: SpectrumAnalysis,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EMFADColors.SurfaceSecondary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Spektrum-Analyse",
                style = MaterialTheme.typography.titleMedium,
                color = EMFADColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Analyse-Werte in Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EMFADAnalysisValue(
                    label = "Peak-Frequenz",
                    value = "${analysis.peakFrequency.toInt()} Hz",
                    color = EMFADColors.EMFADYellow
                )
                
                EMFADAnalysisValue(
                    label = "Ø Amplitude",
                    value = "%.1f mV".format(analysis.averageAmplitude),
                    color = EMFADColors.EMFADBlue
                )
                
                EMFADAnalysisValue(
                    label = "Rauschpegel",
                    value = "%.1f mV".format(analysis.noiseFloor),
                    color = EMFADColors.TextSecondary
                )
                
                EMFADAnalysisValue(
                    label = "SNR",
                    value = "%.1f dB".format(analysis.snrRatio),
                    color = if (analysis.snrRatio > 20) EMFADColors.SignalGreen else EMFADColors.SignalOrange
                )
            }
        }
    }
}

@Composable
private fun EMFADAnalysisValue(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = EMFADColors.TextTertiary,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = color,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EMFADSpectrumControls(
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onExportSpectrum: () -> Unit,
    onClearSpectrum: () -> Unit
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
                text = "Spektrum-Kontrollen",
                style = MaterialTheme.typography.titleMedium,
                color = EMFADColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Kontroll-Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isRecording) {
                    Button(
                        onClick = onStopRecording,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EMFADColors.SignalRed
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop")
                    }
                } else {
                    Button(
                        onClick = onStartRecording,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EMFADColors.SignalGreen
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start")
                    }
                }
                
                Button(
                    onClick = onExportSpectrum,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADColors.EMFADBlue
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export")
                }
                
                Button(
                    onClick = onClearSpectrum,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADColors.EMFADYellowDark
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear")
                }
            }
        }
    }
}

/**
 * Konvertiert echte EMF-Messungen in Spektrum-Daten für die Visualisierung
 */
private fun createSpectrumFromReadings(readings: List<com.emfad.app.models.EMFReading>): SpectrumAnalysis {
    if (readings.isEmpty()) {
        return SpectrumAnalysis(
            spectrumData = emptyList(),
            peakFrequency = 0.0,
            averageAmplitude = 0.0,
            noiseFloor = 0.0,
            snrRatio = 0.0
        )
    }

    // Gruppiere Messungen nach Frequenz
    val frequencyGroups = readings.groupBy { it.frequency }

    // Erstelle Spektrum-Daten
    val spectrumData = frequencyGroups.map { (frequency, readingsAtFreq) ->
        val avgAmplitude = readingsAtFreq.map { it.amplitude }.average()
        val avgPhase = readingsAtFreq.map { it.phase }.average()
        val avgQuality = readingsAtFreq.map { it.confidence }.average()

        SpectrumData(
            frequency = frequency,
            amplitude = avgAmplitude,
            phase = avgPhase,
            quality = avgQuality
        )
    }.sortedBy { it.frequency }

    // Berechne Analyse-Werte
    val peakFrequency = spectrumData.maxByOrNull { it.amplitude }?.frequency ?: 0.0
    val averageAmplitude = spectrumData.map { it.amplitude }.average()
    val noiseFloor = spectrumData.map { it.amplitude }.minOrNull() ?: 0.0
    val maxAmplitude = spectrumData.map { it.amplitude }.maxOrNull() ?: 1.0
    val snrRatio = if (noiseFloor > 0) 20 * log10(maxAmplitude / noiseFloor) else 0.0

    return SpectrumAnalysis(
        spectrumData = spectrumData,
        peakFrequency = peakFrequency,
        averageAmplitude = averageAmplitude,
        noiseFloor = noiseFloor,
        snrRatio = snrRatio
    )
}
