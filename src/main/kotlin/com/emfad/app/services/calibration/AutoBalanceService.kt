package com.emfad.app.services.calibration

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.emfad.app.services.communication.DeviceCommunicationService
import com.emfad.app.services.communication.EMFADCommand
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * EMFAD® AutoBalance Service
 * Basiert auf TfrmAutoBalance der originalen Windows-Software
 * Implementiert XY-Kalibrierung, Kompass-Ausgleich und Datei-Management
 * Rekonstruiert aus EMUNIX07EXE.c "autobalance values; version 1.0"
 */

@Serializable
data class AutoBalanceData(
    val version: String = "autobalance values; version 1.0",
    val compassCalibrationStatus: CalibrationStatus = CalibrationStatus.NOT_STARTED,
    val horizontalCalibrationStatus: CalibrationStatus = CalibrationStatus.NOT_STARTED,
    val verticalCalibrationStatus: CalibrationStatus = CalibrationStatus.NOT_STARTED,
    val horizontalOffsetX: Float = 0.0f,
    val horizontalOffsetY: Float = 0.0f,
    val horizontalScaleX: Float = 1.0f,
    val horizontalScaleY: Float = 1.0f,
    val verticalOffsetZ: Float = 0.0f,
    val verticalScaleZ: Float = 1.0f,
    val compassHeading: Float = 0.0f,
    val compassAccuracy: Float = 0.0f,
    val magneticDeclination: Float = 0.0f,
    val lastCalibrationTime: Long = 0L,
    val calibrationDataPoints: List<CalibrationPoint> = emptyList()
)

@Serializable
data class CalibrationPoint(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long,
    val type: CalibrationType
)

enum class CalibrationStatus(val message: String) {
    NOT_STARTED("Not started"),
    STARTED("Compass calibration started"),
    COLLECTING_HORIZONTAL("collecting data horizontal calibration"),
    COLLECTING_VERTICAL("collecting data vertical calibration"),
    HORIZONTAL_FINISHED("horizontal calibration finished"),
    VERTICAL_FINISHED("vertical calibration finished"),
    COMPASS_FINISHED("compass calibration finished"),
    SAVED("calibration saved")
}

enum class CalibrationType {
    COMPASS,
    HORIZONTAL,
    VERTICAL
}

@Singleton
class AutoBalanceService @Inject constructor(
    private val context: Context,
    private val deviceCommunicationService: DeviceCommunicationService
) : SensorEventListener {
    
    companion object {
        private const val TAG = "EMFADAutoBalance"
        private const val CALIBRATION_FILE = "emfad_autobalance.json"
        private const val MIN_CALIBRATION_POINTS = 20
        private const val CALIBRATION_TIMEOUT_MS = 60000L
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    // Sensoren
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    
    // State Management
    private val _autoBalanceData = MutableStateFlow(AutoBalanceData())
    val autoBalanceData: StateFlow<AutoBalanceData> = _autoBalanceData.asStateFlow()
    
    private val _isCalibrating = MutableStateFlow(false)
    val isCalibrating: StateFlow<Boolean> = _isCalibrating.asStateFlow()
    
    private val _calibrationProgress = MutableStateFlow(0)
    val calibrationProgress: StateFlow<Int> = _calibrationProgress.asStateFlow()
    
    // Sensor-Daten
    private var magneticField = FloatArray(3)
    private var gravity = FloatArray(3)
    private var rotationMatrix = FloatArray(9)
    private var orientation = FloatArray(3)
    
    // Kalibrierungs-Daten
    private val calibrationPoints = mutableListOf<CalibrationPoint>()
    private var currentCalibrationType: CalibrationType? = null
    private var calibrationJob: Job? = null
    
    init {
        loadCalibrationData()
    }
    
    /**
     * Startet Kompass-Kalibrierung
     * Rekonstruiert aus "Compass calibration started"
     */
    fun startCompassCalibration() {
        serviceScope.launch {
            try {
                Log.d(TAG, "Starting compass calibration")
                
                _isCalibrating.value = true
                currentCalibrationType = CalibrationType.COMPASS
                calibrationPoints.clear()
                
                updateCalibrationStatus(CalibrationStatus.STARTED)
                
                // Sensoren registrieren
                registerSensors()
                
                // Kalibrierungs-Timeout
                calibrationJob = serviceScope.launch {
                    delay(CALIBRATION_TIMEOUT_MS)
                    if (_isCalibrating.value) {
                        finishCompassCalibration()
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting compass calibration", e)
                stopCalibration()
            }
        }
    }
    
    /**
     * Startet horizontale Kalibrierung
     * Rekonstruiert aus "collecting data horizontal calibration"
     */
    fun startHorizontalCalibration() {
        serviceScope.launch {
            try {
                Log.d(TAG, "Starting horizontal calibration")
                
                _isCalibrating.value = true
                currentCalibrationType = CalibrationType.HORIZONTAL
                calibrationPoints.clear()
                
                updateCalibrationStatus(CalibrationStatus.COLLECTING_HORIZONTAL)
                
                // Sensoren registrieren
                registerSensors()
                
                // EMFAD-Gerät in Kalibrierungs-Modus setzen
                val command = EMFADCommand(
                    command = "CALIBRATE",
                    parameters = mapOf(
                        "type" to "horizontal",
                        "mode" to "start"
                    )
                )
                deviceCommunicationService.sendCommand(command)
                
                // Kalibrierungs-Timeout
                calibrationJob = serviceScope.launch {
                    delay(CALIBRATION_TIMEOUT_MS)
                    if (_isCalibrating.value) {
                        finishHorizontalCalibration()
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting horizontal calibration", e)
                stopCalibration()
            }
        }
    }
    
    /**
     * Startet vertikale Kalibrierung
     * Rekonstruiert aus "collecting data vertical calibration"
     */
    fun startVerticalCalibration() {
        serviceScope.launch {
            try {
                Log.d(TAG, "Starting vertical calibration")
                
                _isCalibrating.value = true
                currentCalibrationType = CalibrationType.VERTICAL
                calibrationPoints.clear()
                
                updateCalibrationStatus(CalibrationStatus.COLLECTING_VERTICAL)
                
                // Sensoren registrieren
                registerSensors()
                
                // EMFAD-Gerät in Kalibrierungs-Modus setzen
                val command = EMFADCommand(
                    command = "CALIBRATE",
                    parameters = mapOf(
                        "type" to "vertical",
                        "mode" to "start"
                    )
                )
                deviceCommunicationService.sendCommand(command)
                
                // Kalibrierungs-Timeout
                calibrationJob = serviceScope.launch {
                    delay(CALIBRATION_TIMEOUT_MS)
                    if (_isCalibrating.value) {
                        finishVerticalCalibration()
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting vertical calibration", e)
                stopCalibration()
            }
        }
    }
    
    /**
     * Beendet Kompass-Kalibrierung
     */
    private fun finishCompassCalibration() {
        serviceScope.launch {
            try {
                if (calibrationPoints.size >= MIN_CALIBRATION_POINTS) {
                    // Kompass-Kalibrierung berechnen
                    val compassCalibration = calculateCompassCalibration(calibrationPoints)
                    
                    val updatedData = _autoBalanceData.value.copy(
                        compassCalibrationStatus = CalibrationStatus.COMPASS_FINISHED,
                        magneticDeclination = compassCalibration.declination,
                        lastCalibrationTime = System.currentTimeMillis(),
                        calibrationDataPoints = calibrationPoints.toList()
                    )
                    
                    _autoBalanceData.value = updatedData
                    saveCalibrationData()
                    
                    Log.d(TAG, "Compass calibration finished successfully")
                } else {
                    Log.w(TAG, "Insufficient calibration points: ${calibrationPoints.size}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error finishing compass calibration", e)
            } finally {
                stopCalibration()
            }
        }
    }
    
    /**
     * Beendet horizontale Kalibrierung
     */
    private fun finishHorizontalCalibration() {
        serviceScope.launch {
            try {
                if (calibrationPoints.size >= MIN_CALIBRATION_POINTS) {
                    // Horizontale Kalibrierung berechnen
                    val horizontalCalibration = calculateHorizontalCalibration(calibrationPoints)
                    
                    val updatedData = _autoBalanceData.value.copy(
                        horizontalCalibrationStatus = CalibrationStatus.HORIZONTAL_FINISHED,
                        horizontalOffsetX = horizontalCalibration.offsetX,
                        horizontalOffsetY = horizontalCalibration.offsetY,
                        horizontalScaleX = horizontalCalibration.scaleX,
                        horizontalScaleY = horizontalCalibration.scaleY,
                        lastCalibrationTime = System.currentTimeMillis()
                    )
                    
                    _autoBalanceData.value = updatedData
                    saveCalibrationData()
                    
                    // EMFAD-Gerät Kalibrierung übertragen
                    sendCalibrationToDevice(horizontalCalibration)
                    
                    Log.d(TAG, "Horizontal calibration finished successfully")
                } else {
                    Log.w(TAG, "Insufficient calibration points: ${calibrationPoints.size}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error finishing horizontal calibration", e)
            } finally {
                stopCalibration()
            }
        }
    }
    
    /**
     * Beendet vertikale Kalibrierung
     */
    private fun finishVerticalCalibration() {
        serviceScope.launch {
            try {
                if (calibrationPoints.size >= MIN_CALIBRATION_POINTS) {
                    // Vertikale Kalibrierung berechnen
                    val verticalCalibration = calculateVerticalCalibration(calibrationPoints)
                    
                    val updatedData = _autoBalanceData.value.copy(
                        verticalCalibrationStatus = CalibrationStatus.VERTICAL_FINISHED,
                        verticalOffsetZ = verticalCalibration.offsetZ,
                        verticalScaleZ = verticalCalibration.scaleZ,
                        lastCalibrationTime = System.currentTimeMillis()
                    )
                    
                    _autoBalanceData.value = updatedData
                    saveCalibrationData()
                    
                    // EMFAD-Gerät Kalibrierung übertragen
                    sendCalibrationToDevice(verticalCalibration)
                    
                    Log.d(TAG, "Vertical calibration finished successfully")
                } else {
                    Log.w(TAG, "Insufficient calibration points: ${calibrationPoints.size}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error finishing vertical calibration", e)
            } finally {
                stopCalibration()
            }
        }
    }
    
    /**
     * Stoppt aktuelle Kalibrierung
     */
    fun stopCalibration() {
        calibrationJob?.cancel()
        unregisterSensors()
        _isCalibrating.value = false
        _calibrationProgress.value = 0
        currentCalibrationType = null
        calibrationPoints.clear()
    }
    
    /**
     * Speichert Kalibrierung
     * Rekonstruiert aus "calibration saved"
     */
    fun saveCalibration() {
        serviceScope.launch {
            try {
                val updatedData = _autoBalanceData.value.copy(
                    compassCalibrationStatus = if (_autoBalanceData.value.compassCalibrationStatus == CalibrationStatus.COMPASS_FINISHED) 
                        CalibrationStatus.SAVED else _autoBalanceData.value.compassCalibrationStatus,
                    horizontalCalibrationStatus = if (_autoBalanceData.value.horizontalCalibrationStatus == CalibrationStatus.HORIZONTAL_FINISHED) 
                        CalibrationStatus.SAVED else _autoBalanceData.value.horizontalCalibrationStatus,
                    verticalCalibrationStatus = if (_autoBalanceData.value.verticalCalibrationStatus == CalibrationStatus.VERTICAL_FINISHED) 
                        CalibrationStatus.SAVED else _autoBalanceData.value.verticalCalibrationStatus,
                    lastCalibrationTime = System.currentTimeMillis()
                )
                
                _autoBalanceData.value = updatedData
                saveCalibrationData()
                
                Log.d(TAG, "Calibration saved successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error saving calibration", e)
            }
        }
    }
    
    /**
     * Lädt Kalibrierung
     */
    fun loadCalibration() {
        loadCalibrationData()
    }
    
    /**
     * Löscht alle Kalibrierungen
     */
    fun deleteAllCalibration() {
        serviceScope.launch {
            try {
                val resetData = AutoBalanceData()
                _autoBalanceData.value = resetData
                
                // Kalibrierungs-Datei löschen
                val file = File(context.filesDir, CALIBRATION_FILE)
                if (file.exists()) {
                    file.delete()
                }
                
                Log.d(TAG, "All calibration data deleted")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting calibration data", e)
            }
        }
    }
    
    // SensorEventListener Implementation
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    magneticField = it.values.clone()
                    updateCompassReading()
                }
                Sensor.TYPE_ACCELEROMETER -> {
                    gravity = it.values.clone()
                    updateCompassReading()
                }
                Sensor.TYPE_GYROSCOPE -> {
                    // Gyroscope-Daten für Stabilität
                }
            }
            
            // Kalibrierungs-Punkt hinzufügen
            if (_isCalibrating.value && currentCalibrationType != null) {
                addCalibrationPoint(it)
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Sensor-Genauigkeit überwachen
    }
    
    private fun updateCompassReading() {
        if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, magneticField)) {
            SensorManager.getOrientation(rotationMatrix, orientation)
            
            val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
            val normalizedAzimuth = (azimuth + 360) % 360
            
            val updatedData = _autoBalanceData.value.copy(
                compassHeading = normalizedAzimuth,
                compassAccuracy = calculateCompassAccuracy()
            )
            
            _autoBalanceData.value = updatedData
        }
    }
    
    private fun calculateCompassAccuracy(): Float {
        // Vereinfachte Genauigkeits-Berechnung
        val magneticStrength = sqrt(
            magneticField[0] * magneticField[0] +
            magneticField[1] * magneticField[1] +
            magneticField[2] * magneticField[2]
        )
        
        return when {
            magneticStrength > 45 -> 1.0f
            magneticStrength > 35 -> 0.8f
            magneticStrength > 25 -> 0.6f
            else -> 0.4f
        }
    }
    
    private fun addCalibrationPoint(event: SensorEvent) {
        val point = CalibrationPoint(
            x = event.values[0],
            y = event.values[1],
            z = if (event.values.size > 2) event.values[2] else 0f,
            timestamp = System.currentTimeMillis(),
            type = currentCalibrationType!!
        )
        
        calibrationPoints.add(point)
        
        // Progress aktualisieren
        val progress = (calibrationPoints.size * 100) / MIN_CALIBRATION_POINTS
        _calibrationProgress.value = minOf(progress, 100)
        
        // Auto-Finish bei genügend Punkten
        if (calibrationPoints.size >= MIN_CALIBRATION_POINTS * 2) {
            when (currentCalibrationType) {
                CalibrationType.COMPASS -> finishCompassCalibration()
                CalibrationType.HORIZONTAL -> finishHorizontalCalibration()
                CalibrationType.VERTICAL -> finishVerticalCalibration()
                else -> {}
            }
        }
    }
    
    private fun registerSensors() {
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }
    
    private fun unregisterSensors() {
        sensorManager.unregisterListener(this)
    }
    
    private fun updateCalibrationStatus(status: CalibrationStatus) {
        val updatedData = when (currentCalibrationType) {
            CalibrationType.COMPASS -> _autoBalanceData.value.copy(compassCalibrationStatus = status)
            CalibrationType.HORIZONTAL -> _autoBalanceData.value.copy(horizontalCalibrationStatus = status)
            CalibrationType.VERTICAL -> _autoBalanceData.value.copy(verticalCalibrationStatus = status)
            else -> _autoBalanceData.value
        }
        
        _autoBalanceData.value = updatedData
    }
    
    private fun saveCalibrationData() {
        try {
            val json = Json.encodeToString(AutoBalanceData.serializer(), _autoBalanceData.value)
            val file = File(context.filesDir, CALIBRATION_FILE)
            file.writeText(json)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving calibration data", e)
        }
    }
    
    private fun loadCalibrationData() {
        try {
            val file = File(context.filesDir, CALIBRATION_FILE)
            if (file.exists()) {
                val json = file.readText()
                val data = Json.decodeFromString(AutoBalanceData.serializer(), json)
                _autoBalanceData.value = data
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading calibration data", e)
        }
    }
    
    // Kalibrierungs-Berechnungen (vereinfacht)
    private fun calculateCompassCalibration(points: List<CalibrationPoint>): CompassCalibration {
        // Vereinfachte Kompass-Kalibrierung
        return CompassCalibration(declination = 0.0f)
    }
    
    private fun calculateHorizontalCalibration(points: List<CalibrationPoint>): HorizontalCalibration {
        // Vereinfachte horizontale Kalibrierung
        val avgX = points.map { it.x }.average().toFloat()
        val avgY = points.map { it.y }.average().toFloat()
        
        return HorizontalCalibration(
            offsetX = -avgX,
            offsetY = -avgY,
            scaleX = 1.0f,
            scaleY = 1.0f
        )
    }
    
    private fun calculateVerticalCalibration(points: List<CalibrationPoint>): VerticalCalibration {
        // Vereinfachte vertikale Kalibrierung
        val avgZ = points.map { it.z }.average().toFloat()
        
        return VerticalCalibration(
            offsetZ = -avgZ,
            scaleZ = 1.0f
        )
    }
    
    private suspend fun sendCalibrationToDevice(calibration: Any) {
        // Kalibrierung an EMFAD-Gerät senden
        val command = EMFADCommand(
            command = "CALIBRATE",
            parameters = mapOf(
                "type" to "apply",
                "data" to calibration.toString()
            )
        )
        deviceCommunicationService.sendCommand(command)
    }
    
    fun cleanup() {
        stopCalibration()
        serviceScope.cancel()
    }
}

// Hilfsklassen für Kalibrierungs-Ergebnisse
data class CompassCalibration(val declination: Float)
data class HorizontalCalibration(val offsetX: Float, val offsetY: Float, val scaleX: Float, val scaleY: Float)
data class VerticalCalibration(val offsetZ: Float, val scaleZ: Float)
