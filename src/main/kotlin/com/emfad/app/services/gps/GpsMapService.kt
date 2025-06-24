package com.emfad.app.services.gps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.emfad.app.services.analysis.EMFReading
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EMFAD® GPS Map Service
 * Implementiert GPS-Tracking mit FusedLocationProvider
 * Integriert OpenStreetMap (OSMDroid) für Kartenvisualisierung
 * Synchronisiert GPS-Koordinaten mit EMFAD-Messdaten
 */

@Serializable
data class GPSCoordinate(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Float,
    val timestamp: Long,
    val provider: String = "fused"
)

@Serializable
data class EMFADMeasurementPoint(
    val gpsCoordinate: GPSCoordinate,
    val emfReading: EMFReading,
    val pointId: String,
    val sessionId: Long,
    val sequenceNumber: Int
)

@Serializable
data class MeasurementPath(
    val sessionId: Long,
    val startTime: Long,
    val endTime: Long,
    val points: List<EMFADMeasurementPoint>,
    val totalDistance: Double,
    val averageSpeed: Double,
    val boundingBox: BoundingBox
)

@Serializable
data class BoundingBox(
    val northLatitude: Double,
    val southLatitude: Double,
    val eastLongitude: Double,
    val westLongitude: Double
)

enum class LocationAccuracy {
    HIGH,      // GPS + Network
    MEDIUM,    // Network only
    LOW,       // Passive
    BATTERY_OPTIMIZED
}

@Singleton
class GpsMapService @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "EMFADGpsMapService"
        private const val LOCATION_UPDATE_INTERVAL = 1000L // 1 second
        private const val LOCATION_FASTEST_INTERVAL = 500L // 0.5 seconds
        private const val MIN_DISTANCE_BETWEEN_POINTS = 1.0f // 1 meter
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    // State Management
    private val _currentLocation = MutableStateFlow<GPSCoordinate?>(null)
    val currentLocation: StateFlow<GPSCoordinate?> = _currentLocation.asStateFlow()
    
    private val _isLocationEnabled = MutableStateFlow(false)
    val isLocationEnabled: StateFlow<Boolean> = _isLocationEnabled.asStateFlow()
    
    private val _locationAccuracy = MutableStateFlow(LocationAccuracy.HIGH)
    val locationAccuracy: StateFlow<LocationAccuracy> = _locationAccuracy.asStateFlow()
    
    private val _measurementPoints = MutableStateFlow<List<EMFADMeasurementPoint>>(emptyList())
    val measurementPoints: StateFlow<List<EMFADMeasurementPoint>> = _measurementPoints.asStateFlow()
    
    private val _currentPath = MutableStateFlow<MeasurementPath?>(null)
    val currentPath: StateFlow<MeasurementPath?> = _currentPath.asStateFlow()
    
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()
    
    // Location Request Configuration
    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        LOCATION_UPDATE_INTERVAL
    ).apply {
        setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
        setMinUpdateDistanceMeters(MIN_DISTANCE_BETWEEN_POINTS)
        setMaxUpdateDelayMillis(LOCATION_UPDATE_INTERVAL * 2)
        setWaitForAccurateLocation(true)
    }.build()
    
    // Location Callback
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                updateCurrentLocation(location)
            }
        }
        
        override fun onLocationAvailability(availability: LocationAvailability) {
            _isLocationEnabled.value = availability.isLocationAvailable
            Log.d(TAG, "Location availability: ${availability.isLocationAvailable}")
        }
    }
    
    private var currentSessionId: Long = 0L
    private var sequenceCounter = 0
    
    /**
     * Startet GPS-Tracking
     */
    suspend fun startLocationTracking(): Boolean {
        return try {
            if (!hasLocationPermission()) {
                Log.w(TAG, "Location permission not granted")
                return false
            }
            
            Log.d(TAG, "Starting location tracking")
            
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            
            _isTracking.value = true
            currentSessionId = System.currentTimeMillis()
            sequenceCounter = 0
            
            // Aktuelle Position abrufen
            getCurrentLocation()
            
            true
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception starting location tracking", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location tracking", e)
            false
        }
    }
    
    /**
     * Stoppt GPS-Tracking
     */
    fun stopLocationTracking() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            _isTracking.value = false
            
            // Aktuellen Pfad finalisieren
            finalizeCurrentPath()
            
            Log.d(TAG, "Location tracking stopped")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location tracking", e)
        }
    }
    
    /**
     * Fügt einen EMFAD-Messpunkt hinzu
     */
    fun addMeasurementPoint(emfReading: EMFReading) {
        val currentGPS = _currentLocation.value
        if (currentGPS == null) {
            Log.w(TAG, "No GPS location available for measurement point")
            return
        }
        
        serviceScope.launch {
            try {
                val measurementPoint = EMFADMeasurementPoint(
                    gpsCoordinate = currentGPS,
                    emfReading = emfReading,
                    pointId = "${currentSessionId}_${sequenceCounter}",
                    sessionId = currentSessionId,
                    sequenceNumber = sequenceCounter++
                )
                
                val currentPoints = _measurementPoints.value.toMutableList()
                currentPoints.add(measurementPoint)
                _measurementPoints.value = currentPoints
                
                // Pfad aktualisieren
                updateCurrentPath(currentPoints)
                
                Log.d(TAG, "Added measurement point: ${measurementPoint.pointId}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error adding measurement point", e)
            }
        }
    }
    
    /**
     * Startet neue Mess-Session
     */
    fun startNewMeasurementSession() {
        serviceScope.launch {
            currentSessionId = System.currentTimeMillis()
            sequenceCounter = 0
            _measurementPoints.value = emptyList()
            _currentPath.value = null
            
            Log.d(TAG, "Started new measurement session: $currentSessionId")
        }
    }
    
    /**
     * Exportiert aktuellen Pfad
     */
    fun exportCurrentPath(): String? {
        val path = _currentPath.value ?: return null
        
        return buildString {
            appendLine("# EMFAD GPS Path Export")
            appendLine("# Session ID: ${path.sessionId}")
            appendLine("# Start Time: ${java.util.Date(path.startTime)}")
            appendLine("# End Time: ${java.util.Date(path.endTime)}")
            appendLine("# Total Points: ${path.points.size}")
            appendLine("# Total Distance: ${String.format("%.2f", path.totalDistance)} m")
            appendLine("# Average Speed: ${String.format("%.2f", path.averageSpeed)} m/s")
            appendLine("#")
            appendLine("PointID\tSequence\tLatitude\tLongitude\tAltitude\tAccuracy\tTimestamp\tFrequency\tDepth\tSignalStrength")
            
            path.points.forEach { point ->
                appendLine("${point.pointId}\t${point.sequenceNumber}\t" +
                         "${point.gpsCoordinate.latitude}\t${point.gpsCoordinate.longitude}\t" +
                         "${point.gpsCoordinate.altitude}\t${point.gpsCoordinate.accuracy}\t" +
                         "${point.gpsCoordinate.timestamp}\t${point.emfReading.frequency}\t" +
                         "${point.emfReading.depth}\t${point.emfReading.signalStrength}")
            }
        }
    }
    
    /**
     * Berechnet Entfernung zwischen zwei GPS-Punkten
     */
    fun calculateDistance(point1: GPSCoordinate, point2: GPSCoordinate): Double {
        val results = FloatArray(1)
        Location.distanceBetween(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude,
            results
        )
        return results[0].toDouble()
    }
    
    /**
     * Setzt Location-Genauigkeit
     */
    fun setLocationAccuracy(accuracy: LocationAccuracy) {
        _locationAccuracy.value = accuracy
        
        val priority = when (accuracy) {
            LocationAccuracy.HIGH -> Priority.PRIORITY_HIGH_ACCURACY
            LocationAccuracy.MEDIUM -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
            LocationAccuracy.LOW -> Priority.PRIORITY_LOW_POWER
            LocationAccuracy.BATTERY_OPTIMIZED -> Priority.PRIORITY_PASSIVE
        }
        
        val newRequest = LocationRequest.Builder(priority, LOCATION_UPDATE_INTERVAL).apply {
            setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
            setMinUpdateDistanceMeters(MIN_DISTANCE_BETWEEN_POINTS)
        }.build()
        
        // Location Request aktualisieren wenn Tracking aktiv
        if (_isTracking.value && hasLocationPermission()) {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                fusedLocationClient.requestLocationUpdates(
                    newRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception updating location request", e)
            }
        }
    }
    
    private suspend fun getCurrentLocation() {
        if (!hasLocationPermission()) return
        
        try {
            val locationTask: Task<Location> = fusedLocationClient.lastLocation
            locationTask.addOnSuccessListener { location ->
                location?.let { updateCurrentLocation(it) }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting current location", e)
        }
    }
    
    private fun updateCurrentLocation(location: Location) {
        val gpsCoordinate = GPSCoordinate(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            accuracy = location.accuracy,
            timestamp = location.time,
            provider = location.provider ?: "unknown"
        )
        
        _currentLocation.value = gpsCoordinate
        
        Log.d(TAG, "Location updated: ${location.latitude}, ${location.longitude}, accuracy: ${location.accuracy}m")
    }
    
    private fun updateCurrentPath(points: List<EMFADMeasurementPoint>) {
        if (points.isEmpty()) return
        
        val startTime = points.first().gpsCoordinate.timestamp
        val endTime = points.last().gpsCoordinate.timestamp
        
        // Gesamtdistanz berechnen
        var totalDistance = 0.0
        for (i in 1 until points.size) {
            totalDistance += calculateDistance(
                points[i-1].gpsCoordinate,
                points[i].gpsCoordinate
            )
        }
        
        // Durchschnittsgeschwindigkeit berechnen
        val timeSpan = (endTime - startTime) / 1000.0 // Sekunden
        val averageSpeed = if (timeSpan > 0) totalDistance / timeSpan else 0.0
        
        // Bounding Box berechnen
        val latitudes = points.map { it.gpsCoordinate.latitude }
        val longitudes = points.map { it.gpsCoordinate.longitude }
        
        val boundingBox = BoundingBox(
            northLatitude = latitudes.maxOrNull() ?: 0.0,
            southLatitude = latitudes.minOrNull() ?: 0.0,
            eastLongitude = longitudes.maxOrNull() ?: 0.0,
            westLongitude = longitudes.minOrNull() ?: 0.0
        )
        
        val path = MeasurementPath(
            sessionId = currentSessionId,
            startTime = startTime,
            endTime = endTime,
            points = points,
            totalDistance = totalDistance,
            averageSpeed = averageSpeed,
            boundingBox = boundingBox
        )
        
        _currentPath.value = path
    }
    
    private fun finalizeCurrentPath() {
        val points = _measurementPoints.value
        if (points.isNotEmpty()) {
            updateCurrentPath(points)
            Log.d(TAG, "Path finalized with ${points.size} points")
        }
    }
    
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun cleanup() {
        stopLocationTracking()
        serviceScope.cancel()
    }
}
