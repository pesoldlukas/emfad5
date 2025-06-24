package com.emfad.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import com.emfad.app.ui.theme.EMFADColors
import kotlin.math.*

/**
 * EMFAD® Profile View Screen
 * Basiert auf Profile-View der originalen Windows-Software
 * Implementiert 2D/3D-Höhenprofil, Heatmap und Werte-Anzeige
 */

data class ProfilePoint(
    val x: Double,
    val y: Double,
    val z: Double,
    val depth: Double,
    val amplitude: Double,
    val quality: Double,
    val timestamp: Long
)

data class ProfileData(
    val points: List<ProfilePoint>,
    val gridWidth: Int,
    val gridHeight: Int,
    val minValue: Double,
    val maxValue: Double,
    val averageValue: Double
)

enum class ProfileViewMode {
    HEATMAP_2D,
    CONTOUR_2D,
    PROFILE_3D,
    CROSS_SECTION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileViewScreen(
    navController: androidx.navigation.NavController,
    modifier: Modifier = Modifier
) {
    // Verwende das ECHTE AnalysisViewModel mit echten Messdaten
    val analysisViewModel: com.emfad.app.viewmodels.analysis.AnalysisViewModel = androidx.hilt.navigation.compose.hiltViewModel()

    // Sammle echte Daten vom Backend
    val analysisState by analysisViewModel.analysisState.collectAsState()
    val uiState by analysisViewModel.uiState.collectAsState()

    // UI State für Profile View
    var viewMode by remember { mutableStateOf(ProfileViewMode.HEATMAP_2D) }
    var selectedPoint by remember { mutableStateOf<ProfilePoint?>(null) }

    // Konvertiere echte EMF-Messungen zu Profile-Daten
    val profileData = remember(analysisState.sessionReadings) {
        createProfileDataFromReadings(analysisState.sessionReadings)
    }

    // Echte UI mit echten Backend-Daten
    RealProfileViewUI(
        profileData = profileData,
        viewMode = viewMode,
        selectedPoint = selectedPoint,
        onViewModeChange = { mode -> viewMode = mode },
        onPointSelected = { point -> selectedPoint = point },
        onExportProfile = { analysisViewModel.exportAnalysis(com.emfad.app.viewmodels.analysis.ExportFormat.CSV) },
        onSaveProfile = { /* TODO: Implementiere Save */ },
        onClearProfile = { /* TODO: Implementiere Clear */ },
        onBack = { navController.popBackStack() },
        modifier = modifier
    )
}

@Composable
private fun RealProfileViewUI(
    profileData: ProfileData,
    viewMode: ProfileViewMode,
    selectedPoint: ProfilePoint?,
    onViewModeChange: (ProfileViewMode) -> Unit,
    onPointSelected: (ProfilePoint?) -> Unit,
    onExportProfile: () -> Unit,
    onSaveProfile: () -> Unit,
    onClearProfile: () -> Unit,
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
        EMFADProfileHeader(
            onBack = onBack,
            pointCount = profileData.points.size,
            averageValue = profileData.averageValue
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // View-Mode-Auswahl
        EMFADViewModeSelector(
            selectedMode = viewMode,
            onModeSelected = onViewModeChange
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Haupt-Visualisierung
        EMFADProfileVisualization(
            profileData = profileData,
            viewMode = viewMode,
            selectedPoint = selectedPoint,
            onPointSelected = onPointSelected,
            modifier = Modifier.weight(0.6f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Punkt-Details und Statistiken
        EMFADProfileDetails(
            profileData = profileData,
            selectedPoint = selectedPoint,
            modifier = Modifier.weight(0.2f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Kontrollen
        EMFADProfileControls(
            onExportProfile = onExportProfile,
            onSaveProfile = onSaveProfile,
            onClearProfile = onClearProfile
        )
    }
}

@Composable
private fun EMFADProfileHeader(
    onBack: () -> Unit,
    pointCount: Int,
    averageValue: Double
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
                    text = "EMFAD® Profile View",
                    style = MaterialTheme.typography.headlineMedium,
                    color = EMFADColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "2D/3D-Höhenprofil und Heatmap-Analyse",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EMFADColors.TextSecondary
                )
            }
            
            // Statistiken
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$pointCount Punkte",
                    style = MaterialTheme.typography.labelMedium,
                    color = EMFADColors.EMFADBlue,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "Ø %.2f Ω·m".format(averageValue),
                    style = MaterialTheme.typography.labelMedium,
                    color = EMFADColors.EMFADYellow,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EMFADViewModeSelector(
    selectedMode: ProfileViewMode,
    onModeSelected: (ProfileViewMode) -> Unit
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
                text = "Darstellungsmodus",
                style = MaterialTheme.typography.titleMedium,
                color = EMFADColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ProfileViewMode.values()) { mode ->
                    EMFADViewModeButton(
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
private fun EMFADViewModeButton(
    mode: ProfileViewMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (icon, text) = when (mode) {
        ProfileViewMode.HEATMAP_2D -> Icons.Default.Gradient to "Heatmap"
        ProfileViewMode.CONTOUR_2D -> Icons.Default.Terrain to "Kontur"
        ProfileViewMode.PROFILE_3D -> Icons.Default.View3D to "3D-Profil"
        ProfileViewMode.CROSS_SECTION -> Icons.Default.Timeline to "Schnitt"
    }
    
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) EMFADColors.EMFADBlue else EMFADColors.EMFADGray
        ),
        modifier = Modifier.height(48.dp)
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

@Composable
private fun EMFADProfileVisualization(
    profileData: ProfileData,
    viewMode: ProfileViewMode,
    selectedPoint: ProfilePoint?,
    onPointSelected: (ProfilePoint?) -> Unit,
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
            Text(
                text = when (viewMode) {
                    ProfileViewMode.HEATMAP_2D -> "2D-Heatmap"
                    ProfileViewMode.CONTOUR_2D -> "2D-Konturlinien"
                    ProfileViewMode.PROFILE_3D -> "3D-Höhenprofil"
                    ProfileViewMode.CROSS_SECTION -> "Querschnitt-Ansicht"
                },
                style = MaterialTheme.typography.titleMedium,
                color = EMFADColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Visualisierungs-Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(EMFADColors.BackgroundSecondary)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            // Finde nächsten Punkt zur Touch-Position
                            val nearestPoint = findNearestPoint(profileData.points, offset, size)
                            onPointSelected(nearestPoint)
                        }
                    }
            ) {
                when (viewMode) {
                    ProfileViewMode.HEATMAP_2D -> drawHeatmap(profileData, selectedPoint)
                    ProfileViewMode.CONTOUR_2D -> drawContourLines(profileData, selectedPoint)
                    ProfileViewMode.PROFILE_3D -> draw3DProfile(profileData, selectedPoint)
                    ProfileViewMode.CROSS_SECTION -> drawCrossSection(profileData, selectedPoint)
                }
            }
        }
    }
}

private fun findNearestPoint(
    points: List<ProfilePoint>,
    touchOffset: Offset,
    canvasSize: androidx.compose.ui.geometry.Size
): ProfilePoint? {
    if (points.isEmpty()) return null
    
    val padding = 40f
    val width = canvasSize.width - 2 * padding
    val height = canvasSize.height - 2 * padding
    
    val minX = points.minOfOrNull { it.x } ?: 0.0
    val maxX = points.maxOfOrNull { it.x } ?: 1.0
    val minY = points.minOfOrNull { it.y } ?: 0.0
    val maxY = points.maxOfOrNull { it.y } ?: 1.0
    
    return points.minByOrNull { point ->
        val screenX = padding + ((point.x - minX) / (maxX - minX) * width).toFloat()
        val screenY = padding + ((point.y - minY) / (maxY - minY) * height).toFloat()
        
        val dx = touchOffset.x - screenX
        val dy = touchOffset.y - screenY
        sqrt(dx * dx + dy * dy)
    }
}

private fun DrawScope.drawHeatmap(profileData: ProfileData, selectedPoint: ProfilePoint?) {
    if (profileData.points.isEmpty()) return
    
    val padding = 40f
    val width = size.width - 2 * padding
    val height = size.height - 2 * padding
    
    val minX = profileData.points.minOfOrNull { it.x } ?: 0.0
    val maxX = profileData.points.maxOfOrNull { it.x } ?: 1.0
    val minY = profileData.points.minOfOrNull { it.y } ?: 0.0
    val maxY = profileData.points.maxOfOrNull { it.y } ?: 1.0
    
    // Heatmap-Punkte zeichnen
    profileData.points.forEach { point ->
        val x = padding + ((point.x - minX) / (maxX - minX) * width).toFloat()
        val y = padding + ((point.y - minY) / (maxY - minY) * height).toFloat()
        
        // Farbe basierend auf Z-Wert
        val normalizedValue = ((point.z - profileData.minValue) / (profileData.maxValue - profileData.minValue))
            .coerceIn(0.0, 1.0)
        
        val color = interpolateHeatmapColor(normalizedValue.toFloat())
        
        drawCircle(
            color = color,
            radius = 8f,
            center = Offset(x, y)
        )
        
        // Ausgewählten Punkt hervorheben
        if (point == selectedPoint) {
            drawCircle(
                color = EMFADColors.EMFADYellow,
                radius = 12f,
                center = Offset(x, y),
                style = Stroke(width = 3f)
            )
        }
    }
    
    // Achsen und Labels
    drawProfileAxes(minX, maxX, minY, maxY, padding, width, height)
}

private fun DrawScope.drawContourLines(profileData: ProfileData, selectedPoint: ProfilePoint?) {
    // Vereinfachte Konturlinien-Darstellung
    drawHeatmap(profileData, selectedPoint) // Basis-Heatmap
    
    // Konturlinien (vereinfacht)
    val contourLevels = 5
    for (i in 1 until contourLevels) {
        val level = profileData.minValue + (profileData.maxValue - profileData.minValue) * i / contourLevels
        
        // Zeichne Konturlinie für dieses Level (vereinfacht als Kreise)
        profileData.points.filter { abs(it.z - level) < (profileData.maxValue - profileData.minValue) * 0.1 }
            .forEach { point ->
                val padding = 40f
                val width = size.width - 2 * padding
                val height = size.height - 2 * padding
                
                val minX = profileData.points.minOfOrNull { it.x } ?: 0.0
                val maxX = profileData.points.maxOfOrNull { it.x } ?: 1.0
                val minY = profileData.points.minOfOrNull { it.y } ?: 0.0
                val maxY = profileData.points.maxOfOrNull { it.y } ?: 1.0
                
                val x = padding + ((point.x - minX) / (maxX - minX) * width).toFloat()
                val y = padding + ((point.y - minY) / (maxY - minY) * height).toFloat()
                
                drawCircle(
                    color = EMFADColors.TextPrimary,
                    radius = 2f,
                    center = Offset(x, y)
                )
            }
    }
}

private fun DrawScope.draw3DProfile(profileData: ProfileData, selectedPoint: ProfilePoint?) {
    // Vereinfachte 3D-Darstellung als isometrische Projektion
    val padding = 40f
    val width = size.width - 2 * padding
    val height = size.height - 2 * padding
    
    val minX = profileData.points.minOfOrNull { it.x } ?: 0.0
    val maxX = profileData.points.maxOfOrNull { it.x } ?: 1.0
    val minY = profileData.points.minOfOrNull { it.y } ?: 0.0
    val maxY = profileData.points.maxOfOrNull { it.y } ?: 1.0
    
    // 3D-Punkte mit isometrischer Projektion
    profileData.points.forEach { point ->
        val normalizedX = (point.x - minX) / (maxX - minX)
        val normalizedY = (point.y - minY) / (maxY - minY)
        val normalizedZ = (point.z - profileData.minValue) / (profileData.maxValue - profileData.minValue)
        
        // Isometrische Projektion
        val isoX = (normalizedX - normalizedY) * cos(PI / 6) * width * 0.5
        val isoY = (normalizedX + normalizedY) * sin(PI / 6) * height * 0.3 - normalizedZ * height * 0.3
        
        val screenX = padding + width * 0.5 + isoX.toFloat()
        val screenY = padding + height * 0.7 + isoY.toFloat()
        
        val color = interpolateHeatmapColor(normalizedZ.toFloat())
        
        drawCircle(
            color = color,
            radius = 6f,
            center = Offset(screenX, screenY)
        )
        
        if (point == selectedPoint) {
            drawCircle(
                color = EMFADColors.EMFADYellow,
                radius = 10f,
                center = Offset(screenX, screenY),
                style = Stroke(width = 2f)
            )
        }
    }
}

private fun DrawScope.drawCrossSection(profileData: ProfileData, selectedPoint: ProfilePoint?) {
    if (profileData.points.isEmpty()) return
    
    val padding = 40f
    val width = size.width - 2 * padding
    val height = size.height - 2 * padding
    
    // Sortiere Punkte nach X-Koordinate für Querschnitt
    val sortedPoints = profileData.points.sortedBy { it.x }
    
    if (sortedPoints.size < 2) return
    
    // Zeichne Querschnitt-Linie
    val path = Path()
    sortedPoints.forEachIndexed { index, point ->
        val x = padding + (index.toFloat() / (sortedPoints.size - 1)) * width
        val normalizedZ = ((point.z - profileData.minValue) / (profileData.maxValue - profileData.minValue))
            .coerceIn(0.0, 1.0)
        val y = padding + height - (normalizedZ * height).toFloat()
        
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
        
        // Datenpunkte
        drawCircle(
            color = EMFADColors.EMFADBlue,
            radius = 4f,
            center = Offset(x, y)
        )
        
        if (point == selectedPoint) {
            drawCircle(
                color = EMFADColors.EMFADYellow,
                radius = 8f,
                center = Offset(x, y),
                style = Stroke(width = 2f)
            )
        }
    }
    
    // Zeichne Profil-Linie
    drawPath(
        path = path,
        color = EMFADColors.EMFADBlue,
        style = Stroke(width = 2f)
    )
    
    // Achsen
    drawLine(
        color = EMFADColors.TextTertiary,
        start = Offset(padding, padding + height),
        end = Offset(padding + width, padding + height),
        strokeWidth = 2f
    )
    
    drawLine(
        color = EMFADColors.TextTertiary,
        start = Offset(padding, padding),
        end = Offset(padding, padding + height),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawProfileAxes(
    minX: Double, maxX: Double, minY: Double, maxY: Double,
    padding: Float, width: Float, height: Float
) {
    // X-Achse
    drawLine(
        color = EMFADColors.TextTertiary,
        start = Offset(padding, padding + height),
        end = Offset(padding + width, padding + height),
        strokeWidth = 2f
    )
    
    // Y-Achse
    drawLine(
        color = EMFADColors.TextTertiary,
        start = Offset(padding, padding),
        end = Offset(padding, padding + height),
        strokeWidth = 2f
    )
}

private fun interpolateHeatmapColor(value: Float): Color {
    return when {
        value < 0.2f -> Color.Blue.copy(alpha = 0.8f)
        value < 0.4f -> Color.Cyan.copy(alpha = 0.8f)
        value < 0.6f -> Color.Green.copy(alpha = 0.8f)
        value < 0.8f -> Color.Yellow.copy(alpha = 0.8f)
        else -> Color.Red.copy(alpha = 0.8f)
    }
}

@Composable
private fun EMFADProfileDetails(
    profileData: ProfileData,
    selectedPoint: ProfilePoint?,
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
                text = if (selectedPoint != null) "Punkt-Details" else "Profil-Statistiken",
                style = MaterialTheme.typography.titleMedium,
                color = EMFADColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedPoint != null) {
                // Ausgewählter Punkt Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EMFADDetailValue(
                        label = "Position",
                        value = "(%.1f, %.1f)".format(selectedPoint.x, selectedPoint.y),
                        color = EMFADColors.EMFADBlue
                    )

                    EMFADDetailValue(
                        label = "Z-Wert",
                        value = "%.2f Ω·m".format(selectedPoint.z),
                        color = EMFADColors.EMFADYellow
                    )

                    EMFADDetailValue(
                        label = "Tiefe",
                        value = "%.2f m".format(selectedPoint.depth),
                        color = EMFADColors.StatusInfo
                    )

                    EMFADDetailValue(
                        label = "Qualität",
                        value = "%.1f%%".format(selectedPoint.quality * 100),
                        color = if (selectedPoint.quality > 0.8) EMFADColors.SignalGreen else EMFADColors.SignalOrange
                    )
                }
            } else {
                // Profil-Statistiken
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EMFADDetailValue(
                        label = "Min-Wert",
                        value = "%.2f Ω·m".format(profileData.minValue),
                        color = EMFADColors.SignalBlue
                    )

                    EMFADDetailValue(
                        label = "Max-Wert",
                        value = "%.2f Ω·m".format(profileData.maxValue),
                        color = EMFADColors.SignalRed
                    )

                    EMFADDetailValue(
                        label = "Durchschnitt",
                        value = "%.2f Ω·m".format(profileData.averageValue),
                        color = EMFADColors.EMFADYellow
                    )

                    EMFADDetailValue(
                        label = "Punkte",
                        value = "${profileData.points.size}",
                        color = EMFADColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun EMFADDetailValue(
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
private fun EMFADProfileControls(
    onExportProfile: () -> Unit,
    onSaveProfile: () -> Unit,
    onClearProfile: () -> Unit
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
                text = "Profil-Kontrollen",
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
                Button(
                    onClick = onExportProfile,
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
                    onClick = onSaveProfile,
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
                    onClick = onClearProfile,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADColors.StatusWarning
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Löschen")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info-Text
            Text(
                text = "Profile können als EGD/ESD-Dateien exportiert oder für spätere Analyse gespeichert werden.",
                style = MaterialTheme.typography.bodySmall,
                color = EMFADColors.TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Konvertiert echte EMF-Messungen in Profile-Daten für die Visualisierung
 */
private fun createProfileDataFromReadings(readings: List<com.emfad.app.models.EMFReading>): ProfileData {
    if (readings.isEmpty()) {
        return ProfileData(
            points = emptyList(),
            gridWidth = 0,
            gridHeight = 0,
            minValue = 0.0,
            maxValue = 0.0,
            averageValue = 0.0
        )
    }

    // Konvertiere EMFReading zu ProfilePoint
    val points = readings.mapIndexed { index, reading ->
        ProfilePoint(
            x = reading.position?.x ?: (index % 10).toDouble(), // Fallback Grid-Position
            y = reading.position?.y ?: (index / 10).toDouble(),
            z = reading.magnitude, // Z-Wert aus Magnitude
            depth = reading.depth,
            amplitude = reading.amplitude,
            quality = reading.confidence,
            timestamp = reading.timestamp
        )
    }

    // Berechne Statistiken
    val zValues = points.map { it.z }
    val minValue = zValues.minOrNull() ?: 0.0
    val maxValue = zValues.maxOrNull() ?: 1.0
    val averageValue = zValues.average()

    // Schätze Grid-Dimensionen
    val uniqueX = points.map { it.x }.distinct().size
    val uniqueY = points.map { it.y }.distinct().size

    return ProfileData(
        points = points,
        gridWidth = maxOf(uniqueX, 1),
        gridHeight = maxOf(uniqueY, 1),
        minValue = minValue,
        maxValue = maxValue,
        averageValue = averageValue
    )
}
