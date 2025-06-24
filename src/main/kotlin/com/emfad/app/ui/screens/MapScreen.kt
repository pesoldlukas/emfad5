package com.emfad.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.emfad.app.services.gps.EMFADMeasurementPoint
import com.emfad.app.services.gps.GPSCoordinate
import com.emfad.app.services.gps.LocationAccuracy
import com.emfad.app.ui.theme.EMFADColors
import com.emfad.app.viewmodels.MapType
import com.emfad.app.viewmodels.MapViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * EMFAD® Map Screen
 * Implementiert GPS-Tracking mit OpenStreetMap (OSMDroid)
 * Zeigt EMFAD-Messpunkte und Pfade auf interaktiver Karte
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: androidx.navigation.NavController,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    // State Collection
    val currentLocation by viewModel.currentLocation.collectAsState()
    val isLocationEnabled by viewModel.isLocationEnabled.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()
    val measurementPoints by viewModel.measurementPoints.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val selectedPoint by viewModel.selectedPoint.collectAsState()
    val showMeasurementPoints by viewModel.showMeasurementPoints.collectAsState()
    val showPath by viewModel.showPath.collectAsState()
    val mapType by viewModel.mapType.collectAsState()
    val isPermissionGranted by viewModel.isPermissionGranted.collectAsState()
    
    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                     permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.setPermissionGranted(granted)
    }
    
    // OSMDroid Configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = "EMFAD_Android_App"
    }
    
    // Request permissions on first launch
    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EMFADColors.BackgroundPrimary)
    ) {
        // Header
        EMFADMapHeader(
            onBack = { navController.popBackStack() },
            isTracking = isTracking,
            isLocationEnabled = isLocationEnabled,
            currentLocation = currentLocation
        )
        
        // Map Container
        Box(
            modifier = Modifier.weight(1f)
        ) {
            if (isPermissionGranted) {
                // OSMDroid Map
                EMFADMapView(
                    currentLocation = currentLocation,
                    measurementPoints = measurementPoints,
                    currentPath = currentPath,
                    showMeasurementPoints = showMeasurementPoints,
                    showPath = showPath,
                    mapType = mapType,
                    onPointSelected = viewModel::selectMeasurementPoint,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Map Controls
                EMFADMapControls(
                    onCenterOnLocation = viewModel::centerOnCurrentLocation,
                    onCenterOnAllPoints = viewModel::centerOnAllPoints,
                    onToggleTracking = {
                        if (isTracking) viewModel.stopLocationTracking()
                        else viewModel.startLocationTracking()
                    },
                    onTogglePoints = viewModel::toggleMeasurementPoints,
                    onTogglePath = viewModel::togglePath,
                    isTracking = isTracking,
                    showPoints = showMeasurementPoints,
                    showPath = showPath,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )
            } else {
                // Permission Request UI
                EMFADPermissionRequest(
                    onRequestPermission = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
            }
        }
        
        // Bottom Panel
        if (selectedPoint != null || currentPath != null) {
            EMFADMapBottomPanel(
                selectedPoint = selectedPoint,
                pathStatistics = viewModel.getPathStatistics(),
                onExportPath = viewModel::exportCurrentPath,
                onNewSession = viewModel::startNewMeasurementSession
            )
        }
    }
}

@Composable
private fun EMFADMapHeader(
    onBack: () -> Unit,
    isTracking: Boolean,
    isLocationEnabled: Boolean,
    currentLocation: GPSCoordinate?
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
                    text = "EMFAD® GPS Map",
                    style = MaterialTheme.typography.headlineMedium,
                    color = EMFADColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "GPS-Tracking und Messpunkt-Visualisierung",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EMFADColors.TextSecondary
                )
            }
            
            // GPS Status
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isLocationEnabled) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                        contentDescription = "GPS Status",
                        modifier = Modifier.size(16.dp),
                        tint = if (isLocationEnabled) EMFADColors.StatusConnected else EMFADColors.StatusDisconnected
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = if (isTracking) "Tracking" else "Bereit",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isTracking) EMFADColors.StatusConnected else EMFADColors.TextSecondary
                    )
                }
                
                currentLocation?.let { location ->
                    Text(
                        text = "±${location.accuracy.toInt()}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            location.accuracy < 5 -> EMFADColors.SignalGreen
                            location.accuracy < 15 -> EMFADColors.EMFADYellow
                            else -> EMFADColors.SignalOrange
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EMFADMapView(
    currentLocation: GPSCoordinate?,
    measurementPoints: List<EMFADMeasurementPoint>,
    currentPath: com.emfad.app.services.gps.MeasurementPath?,
    showMeasurementPoints: Boolean,
    showPath: Boolean,
    mapType: MapType,
    onPointSelected: (EMFADMeasurementPoint?) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            MapView(context).apply {
                // Map Configuration
                setTileSource(when (mapType) {
                    MapType.STANDARD -> TileSourceFactory.MAPNIK
                    MapType.SATELLITE -> TileSourceFactory.USGS_SAT
                    MapType.TERRAIN -> TileSourceFactory.USGS_TOPO
                    MapType.HYBRID -> TileSourceFactory.MAPNIK
                })
                
                setMultiTouchControls(true)
                controller.setZoom(15.0)
                
                // Initial position (falls GPS verfügbar)
                currentLocation?.let { location ->
                    controller.setCenter(GeoPoint(location.latitude, location.longitude))
                }
            }
        },
        update = { mapView ->
            // Clear existing overlays
            mapView.overlays.clear()
            
            // Add current location marker
            currentLocation?.let { location ->
                val currentLocationMarker = Marker(mapView).apply {
                    position = GeoPoint(location.latitude, location.longitude)
                    title = "Aktuelle Position"
                    snippet = "Genauigkeit: ±${location.accuracy.toInt()}m"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(currentLocationMarker)
            }
            
            // Add measurement points
            if (showMeasurementPoints) {
                measurementPoints.forEach { point ->
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(point.gpsCoordinate.latitude, point.gpsCoordinate.longitude)
                        title = "Messpunkt ${point.sequenceNumber}"
                        snippet = "Tiefe: ${String.format("%.2f", point.emfReading.depth)}m\n" +
                                "Signal: ${String.format("%.1f", point.emfReading.signalStrength)}"
                        
                        setOnMarkerClickListener { _, _ ->
                            onPointSelected(point)
                            true
                        }
                    }
                    mapView.overlays.add(marker)
                }
            }
            
            // Add path
            if (showPath && currentPath != null && currentPath.points.size > 1) {
                val pathLine = Polyline().apply {
                    color = EMFADColors.EMFADBlue.hashCode()
                    width = 5f
                    
                    val geoPoints = currentPath.points.map { point ->
                        GeoPoint(point.gpsCoordinate.latitude, point.gpsCoordinate.longitude)
                    }
                    setPoints(geoPoints)
                }
                mapView.overlays.add(pathLine)
            }
            
            mapView.invalidate()
        },
        modifier = modifier
    )
}

@Composable
private fun EMFADMapControls(
    onCenterOnLocation: () -> Unit,
    onCenterOnAllPoints: () -> Unit,
    onToggleTracking: () -> Unit,
    onTogglePoints: () -> Unit,
    onTogglePath: () -> Unit,
    isTracking: Boolean,
    showPoints: Boolean,
    showPath: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Tracking Toggle
        FloatingActionButton(
            onClick = onToggleTracking,
            containerColor = if (isTracking) EMFADColors.SignalRed else EMFADColors.EMFADBlue,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isTracking) "Stop Tracking" else "Start Tracking",
                tint = EMFADColors.TextPrimary
            )
        }
        
        // Center on Location
        FloatingActionButton(
            onClick = onCenterOnLocation,
            containerColor = EMFADColors.EMFADGray,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Center on Location",
                tint = EMFADColors.TextPrimary
            )
        }
        
        // Center on All Points
        FloatingActionButton(
            onClick = onCenterOnAllPoints,
            containerColor = EMFADColors.EMFADGray,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusWeak,
                contentDescription = "Center on All Points",
                tint = EMFADColors.TextPrimary
            )
        }
        
        // Toggle Points
        FloatingActionButton(
            onClick = onTogglePoints,
            containerColor = if (showPoints) EMFADColors.EMFADBlue else EMFADColors.EMFADGray,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = "Toggle Points",
                tint = EMFADColors.TextPrimary
            )
        }
        
        // Toggle Path
        FloatingActionButton(
            onClick = onTogglePath,
            containerColor = if (showPath) EMFADColors.EMFADBlue else EMFADColors.EMFADGray,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timeline,
                contentDescription = "Toggle Path",
                tint = EMFADColors.TextPrimary
            )
        }
    }
}

@Composable
private fun EMFADPermissionRequest(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocationOff,
            contentDescription = "Location Permission",
            modifier = Modifier.size(64.dp),
            tint = EMFADColors.TextTertiary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "GPS-Berechtigung erforderlich",
            style = MaterialTheme.typography.headlineSmall,
            color = EMFADColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Für die Kartenansicht und GPS-Tracking benötigt die App Zugriff auf Ihren Standort.",
            style = MaterialTheme.typography.bodyMedium,
            color = EMFADColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = EMFADColors.EMFADBlue
            )
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Enable Location",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Berechtigung erteilen")
        }
    }
}

@Composable
private fun EMFADMapBottomPanel(
    selectedPoint: EMFADMeasurementPoint?,
    pathStatistics: com.emfad.app.viewmodels.PathStatistics?,
    onExportPath: () -> String?,
    onNewSession: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = EMFADColors.SurfaceSecondary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (selectedPoint != null) {
                // Selected Point Details
                Text(
                    text = "Messpunkt ${selectedPoint.sequenceNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    color = EMFADColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EMFADMapStatItem(
                        label = "Tiefe",
                        value = "${String.format("%.2f", selectedPoint.emfReading.depth)} m"
                    )
                    
                    EMFADMapStatItem(
                        label = "Signal",
                        value = "${String.format("%.1f", selectedPoint.emfReading.signalStrength)}"
                    )
                    
                    EMFADMapStatItem(
                        label = "Frequenz",
                        value = "${selectedPoint.emfReading.frequency.toInt()} Hz"
                    )
                    
                    EMFADMapStatItem(
                        label = "Genauigkeit",
                        value = "±${selectedPoint.gpsCoordinate.accuracy.toInt()} m"
                    )
                }
            } else if (pathStatistics != null) {
                // Path Statistics
                Text(
                    text = "Pfad-Statistiken",
                    style = MaterialTheme.typography.titleMedium,
                    color = EMFADColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EMFADMapStatItem(
                        label = "Punkte",
                        value = "${pathStatistics.totalPoints}"
                    )
                    
                    EMFADMapStatItem(
                        label = "Distanz",
                        value = "${String.format("%.0f", pathStatistics.totalDistance)} m"
                    )
                    
                    EMFADMapStatItem(
                        label = "Ø Tiefe",
                        value = "${String.format("%.2f", pathStatistics.averageDepth)} m"
                    )
                    
                    EMFADMapStatItem(
                        label = "Dauer",
                        value = "${String.format("%.0f", pathStatistics.duration)} s"
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNewSession,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADColors.EMFADYellowDark
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Session",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Neue Session")
                }
                
                Button(
                    onClick = { onExportPath() },
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
            }
        }
    }
}

@Composable
private fun EMFADMapStatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = EMFADColors.TextTertiary
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = EMFADColors.TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}
