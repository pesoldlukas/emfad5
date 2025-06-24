package com.emfad.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emfad.app.services.analysis.EMFReading
import com.emfad.app.services.gps.EMFADMeasurementPoint
import com.emfad.app.services.gps.GPSCoordinate
import com.emfad.app.services.gps.GpsMapService
import com.emfad.app.services.gps.LocationAccuracy
import com.emfad.app.services.gps.MeasurementPath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * EMFAD® Map ViewModel
 * Verbindet MapScreen mit GpsMapService
 * Implementiert MVVM-Pattern für GPS und Kartenvisualisierung
 */

@HiltViewModel
class MapViewModel @Inject constructor(
    private val gpsMapService: GpsMapService
) : ViewModel() {
    
    // GPS State direkt von Service
    val currentLocation: StateFlow<GPSCoordinate?> = gpsMapService.currentLocation
    val isLocationEnabled: StateFlow<Boolean> = gpsMapService.isLocationEnabled
    val locationAccuracy: StateFlow<LocationAccuracy> = gpsMapService.locationAccuracy
    val measurementPoints: StateFlow<List<EMFADMeasurementPoint>> = gpsMapService.measurementPoints
    val currentPath: StateFlow<MeasurementPath?> = gpsMapService.currentPath
    val isTracking: StateFlow<Boolean> = gpsMapService.isTracking
    
    // UI State
    private val _mapCenter = MutableStateFlow<GPSCoordinate?>(null)
    val mapCenter: StateFlow<GPSCoordinate?> = _mapCenter.asStateFlow()
    
    private val _zoomLevel = MutableStateFlow(15.0)
    val zoomLevel: StateFlow<Double> = _zoomLevel.asStateFlow()
    
    private val _showMeasurementPoints = MutableStateFlow(true)
    val showMeasurementPoints: StateFlow<Boolean> = _showMeasurementPoints.asStateFlow()
    
    private val _showPath = MutableStateFlow(true)
    val showPath: StateFlow<Boolean> = _showPath.asStateFlow()
    
    private val _selectedPoint = MutableStateFlow<EMFADMeasurementPoint?>(null)
    val selectedPoint: StateFlow<EMFADMeasurementPoint?> = _selectedPoint.asStateFlow()
    
    private val _mapType = MutableStateFlow(MapType.STANDARD)
    val mapType: StateFlow<MapType> = _mapType.asStateFlow()
    
    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()
    
    init {
        observeLocationUpdates()
    }
    
    private fun observeLocationUpdates() {
        viewModelScope.launch {
            // Karte auf aktuelle Position zentrieren
            currentLocation.collect { location ->
                location?.let {
                    if (_mapCenter.value == null) {
                        _mapCenter.value = it
                    }
                }
            }
        }
    }
    
    /**
     * Startet GPS-Tracking
     */
    fun startLocationTracking() {
        viewModelScope.launch {
            val success = gpsMapService.startLocationTracking()
            if (!success) {
                // Handle permission error
            }
        }
    }
    
    /**
     * Stoppt GPS-Tracking
     */
    fun stopLocationTracking() {
        gpsMapService.stopLocationTracking()
    }
    
    /**
     * Fügt Messpunkt zur Karte hinzu
     */
    fun addMeasurementPoint(emfReading: EMFReading) {
        gpsMapService.addMeasurementPoint(emfReading)
    }
    
    /**
     * Startet neue Mess-Session
     */
    fun startNewMeasurementSession() {
        gpsMapService.startNewMeasurementSession()
        _selectedPoint.value = null
    }
    
    /**
     * Zentriert Karte auf aktuelle Position
     */
    fun centerOnCurrentLocation() {
        currentLocation.value?.let { location ->
            _mapCenter.value = location
        }
    }
    
    /**
     * Zentriert Karte auf alle Messpunkte
     */
    fun centerOnAllPoints() {
        val path = currentPath.value
        if (path != null && path.points.isNotEmpty()) {
            val boundingBox = path.boundingBox
            val centerLat = (boundingBox.northLatitude + boundingBox.southLatitude) / 2
            val centerLon = (boundingBox.eastLongitude + boundingBox.westLongitude) / 2
            
            _mapCenter.value = GPSCoordinate(
                latitude = centerLat,
                longitude = centerLon,
                altitude = 0.0,
                accuracy = 0f,
                timestamp = System.currentTimeMillis()
            )
            
            // Zoom-Level basierend auf Bounding Box berechnen
            val latSpan = boundingBox.northLatitude - boundingBox.southLatitude
            val lonSpan = boundingBox.eastLongitude - boundingBox.westLongitude
            val maxSpan = maxOf(latSpan, lonSpan)
            
            val newZoom = when {
                maxSpan > 0.1 -> 10.0
                maxSpan > 0.01 -> 13.0
                maxSpan > 0.001 -> 16.0
                else -> 18.0
            }
            
            _zoomLevel.value = newZoom
        }
    }
    
    /**
     * Wählt einen Messpunkt aus
     */
    fun selectMeasurementPoint(point: EMFADMeasurementPoint?) {
        _selectedPoint.value = point
        point?.let {
            _mapCenter.value = it.gpsCoordinate
        }
    }
    
    /**
     * Ändert Zoom-Level
     */
    fun setZoomLevel(zoom: Double) {
        _zoomLevel.value = zoom.coerceIn(1.0, 20.0)
    }
    
    /**
     * Ändert Karten-Typ
     */
    fun setMapType(type: MapType) {
        _mapType.value = type
    }
    
    /**
     * Schaltet Messpunkt-Anzeige um
     */
    fun toggleMeasurementPoints() {
        _showMeasurementPoints.value = !_showMeasurementPoints.value
    }
    
    /**
     * Schaltet Pfad-Anzeige um
     */
    fun togglePath() {
        _showPath.value = !_showPath.value
    }
    
    /**
     * Setzt Location-Genauigkeit
     */
    fun setLocationAccuracy(accuracy: LocationAccuracy) {
        gpsMapService.setLocationAccuracy(accuracy)
    }
    
    /**
     * Exportiert aktuellen Pfad
     */
    fun exportCurrentPath(): String? {
        return gpsMapService.exportCurrentPath()
    }
    
    /**
     * Berechnet Entfernung zwischen zwei Punkten
     */
    fun calculateDistance(point1: GPSCoordinate, point2: GPSCoordinate): Double {
        return gpsMapService.calculateDistance(point1, point2)
    }
    
    /**
     * Setzt Permission-Status
     */
    fun setPermissionGranted(granted: Boolean) {
        _isPermissionGranted.value = granted
        if (granted) {
            startLocationTracking()
        }
    }
    
    /**
     * Berechnet Statistiken für aktuellen Pfad
     */
    fun getPathStatistics(): PathStatistics? {
        val path = currentPath.value ?: return null
        
        val depths = path.points.map { it.emfReading.depth }
        val signalStrengths = path.points.map { it.emfReading.signalStrength }
        
        return PathStatistics(
            totalPoints = path.points.size,
            totalDistance = path.totalDistance,
            averageSpeed = path.averageSpeed,
            minDepth = depths.minOrNull() ?: 0.0,
            maxDepth = depths.maxOrNull() ?: 0.0,
            averageDepth = depths.average(),
            minSignalStrength = signalStrengths.minOrNull() ?: 0.0,
            maxSignalStrength = signalStrengths.maxOrNull() ?: 0.0,
            averageSignalStrength = signalStrengths.average(),
            duration = (path.endTime - path.startTime) / 1000.0 // Sekunden
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        gpsMapService.stopLocationTracking()
    }
}

enum class MapType {
    STANDARD,
    SATELLITE,
    TERRAIN,
    HYBRID
}

data class PathStatistics(
    val totalPoints: Int,
    val totalDistance: Double,
    val averageSpeed: Double,
    val minDepth: Double,
    val maxDepth: Double,
    val averageDepth: Double,
    val minSignalStrength: Double,
    val maxSignalStrength: Double,
    val averageSignalStrength: Double,
    val duration: Double
)
