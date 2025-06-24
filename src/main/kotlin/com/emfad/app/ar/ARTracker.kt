package com.emfad.app.ar

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.*

/**
 * AR-Tracking für Gerätepositioning und Orientierung
 * Implementiert Sensor-Fusion für präzise Positionsbestimmung
 */
class ARTracker : SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var gyroscope: Sensor? = null
    
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    
    private val gyroMatrix = FloatArray(9)
    private val fusedMatrix = FloatArray(9)
    private var timestamp = 0L
    
    private val position = FloatArray(3) // x, y, z
    private val velocity = FloatArray(3)
    private val acceleration = FloatArray(3)
    
    private var isTracking = false
    private var trackingListener: ARTrackingListener? = null
    
    // Kalman Filter für Sensor-Fusion
    private val kalmanFilter = KalmanFilter()
    
    /**
     * Tracking initialisieren
     */
    fun initialize(sensorManager: SensorManager): Boolean {
        this.sensorManager = sensorManager
        
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        
        return accelerometer != null && magnetometer != null && gyroscope != null
    }
    
    /**
     * Tracking starten
     */
    fun startTracking() {
        if (!isTracking) {
            sensorManager?.let { sm ->
                accelerometer?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
                magnetometer?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
                gyroscope?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            }
            isTracking = true
            kalmanFilter.reset()
        }
    }
    
    /**
     * Tracking stoppen
     */
    fun stopTracking() {
        if (isTracking) {
            sensorManager?.unregisterListener(this)
            isTracking = false
        }
    }
    
    /**
     * Sensor-Events verarbeiten
     */
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                processAccelerometerData(event.values.clone())
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                processmagnetometerData(event.values.clone())
            }
            Sensor.TYPE_GYROSCOPE -> {
                processGyroscopeData(event.values.clone(), event.timestamp)
            }
        }
        
        updatePose()
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Genauigkeitsänderungen behandeln
    }
    
    /**
     * Beschleunigungsmesser-Daten verarbeiten
     */
    private fun processAccelerometerData(values: FloatArray) {
        // Low-pass Filter für Schwerkraft
        val alpha = 0.8f
        gravity[0] = alpha * gravity[0] + (1 - alpha) * values[0]
        gravity[1] = alpha * gravity[1] + (1 - alpha) * values[1]
        gravity[2] = alpha * gravity[2] + (1 - alpha) * values[2]
        
        // Lineare Beschleunigung (ohne Schwerkraft)
        acceleration[0] = values[0] - gravity[0]
        acceleration[1] = values[1] - gravity[1]
        acceleration[2] = values[2] - gravity[2]
        
        // Kalman Filter anwenden
        kalmanFilter.updateAcceleration(acceleration)
    }
    
    /**
     * Magnetometer-Daten verarbeiten
     */
    private fun processmagnetometerData(values: FloatArray) {
        System.arraycopy(values, 0, geomagnetic, 0, 3)
        
        // Rotationsmatrix berechnen
        if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
        }
    }
    
    /**
     * Gyroskop-Daten verarbeiten
     */
    private fun processGyroscopeData(values: FloatArray, timestamp: Long) {
        if (this.timestamp != 0L) {
            val dt = (timestamp - this.timestamp) * 1e-9f // Nanosekunden zu Sekunden
            
            // Rotationsmatrix mit Gyroskop-Daten aktualisieren
            updateGyroMatrix(values, dt)
            
            // Sensor-Fusion
            fuseSensorData()
        }
        this.timestamp = timestamp
    }
    
    /**
     * Gyroskop-Rotationsmatrix aktualisieren
     */
    private fun updateGyroMatrix(gyroValues: FloatArray, dt: Float) {
        val magnitude = sqrt(gyroValues[0] * gyroValues[0] + 
                           gyroValues[1] * gyroValues[1] + 
                           gyroValues[2] * gyroValues[2])
        
        if (magnitude > 0.01f) {
            val angle = magnitude * dt
            val axisX = gyroValues[0] / magnitude
            val axisY = gyroValues[1] / magnitude
            val axisZ = gyroValues[2] / magnitude
            
            val rotationDelta = FloatArray(9)
            createRotationMatrix(rotationDelta, axisX, axisY, axisZ, angle)
            
            val temp = FloatArray(9)
            multiplyMatrices(gyroMatrix, rotationDelta, temp)
            System.arraycopy(temp, 0, gyroMatrix, 0, 9)
        }
    }
    
    /**
     * Rotationsmatrix erstellen
     */
    private fun createRotationMatrix(matrix: FloatArray, x: Float, y: Float, z: Float, angle: Float) {
        val c = cos(angle)
        val s = sin(angle)
        val oneMinusC = 1 - c
        
        matrix[0] = c + x * x * oneMinusC
        matrix[1] = x * y * oneMinusC - z * s
        matrix[2] = x * z * oneMinusC + y * s
        matrix[3] = y * x * oneMinusC + z * s
        matrix[4] = c + y * y * oneMinusC
        matrix[5] = y * z * oneMinusC - x * s
        matrix[6] = z * x * oneMinusC - y * s
        matrix[7] = z * y * oneMinusC + x * s
        matrix[8] = c + z * z * oneMinusC
    }
    
    /**
     * Matrizen multiplizieren
     */
    private fun multiplyMatrices(a: FloatArray, b: FloatArray, result: FloatArray) {
        for (i in 0..2) {
            for (j in 0..2) {
                result[i * 3 + j] = a[i * 3] * b[j] + 
                                   a[i * 3 + 1] * b[3 + j] + 
                                   a[i * 3 + 2] * b[6 + j]
            }
        }
    }
    
    /**
     * Sensor-Fusion durchführen
     */
    private fun fuseSensorData() {
        val alpha = 0.98f // Gewichtung für Gyroskop vs. Accelerometer/Magnetometer
        
        for (i in 0..8) {
            fusedMatrix[i] = alpha * gyroMatrix[i] + (1 - alpha) * rotationMatrix[i]
        }
        
        // Normalisierung der Matrix
        normalizeRotationMatrix(fusedMatrix)
    }
    
    /**
     * Rotationsmatrix normalisieren
     */
    private fun normalizeRotationMatrix(matrix: FloatArray) {
        // Erste Spalte normalisieren
        var norm = sqrt(matrix[0] * matrix[0] + matrix[3] * matrix[3] + matrix[6] * matrix[6])
        if (norm > 0) {
            matrix[0] /= norm
            matrix[3] /= norm
            matrix[6] /= norm
        }
        
        // Zweite Spalte orthogonalisieren und normalisieren
        val dot = matrix[0] * matrix[1] + matrix[3] * matrix[4] + matrix[6] * matrix[7]
        matrix[1] -= dot * matrix[0]
        matrix[4] -= dot * matrix[3]
        matrix[7] -= dot * matrix[6]
        
        norm = sqrt(matrix[1] * matrix[1] + matrix[4] * matrix[4] + matrix[7] * matrix[7])
        if (norm > 0) {
            matrix[1] /= norm
            matrix[4] /= norm
            matrix[7] /= norm
        }
        
        // Dritte Spalte als Kreuzprodukt
        matrix[2] = matrix[3] * matrix[7] - matrix[6] * matrix[4]
        matrix[5] = matrix[6] * matrix[1] - matrix[0] * matrix[7]
        matrix[8] = matrix[0] * matrix[4] - matrix[3] * matrix[1]
    }
    
    /**
     * Position und Orientierung aktualisieren
     */
    private fun updatePose() {
        // Position durch Integration der Beschleunigung
        val dt = 0.02f // Angenommene Zeitschritte
        
        // Geschwindigkeit aktualisieren
        velocity[0] += acceleration[0] * dt
        velocity[1] += acceleration[1] * dt
        velocity[2] += acceleration[2] * dt
        
        // Position aktualisieren
        position[0] += velocity[0] * dt
        position[1] += velocity[1] * dt
        position[2] += velocity[2] * dt
        
        // Drift-Korrektur
        applyDriftCorrection()
        
        // Tracking-Event senden
        trackingListener?.onPoseUpdated(getCurrentPose())
    }
    
    /**
     * Drift-Korrektur anwenden
     */
    private fun applyDriftCorrection() {
        val velocityThreshold = 0.1f
        val accelerationThreshold = 0.5f
        
        // Geschwindigkeit dämpfen wenn Beschleunigung gering ist
        for (i in 0..2) {
            if (abs(acceleration[i]) < accelerationThreshold) {
                velocity[i] *= 0.95f // Dämpfung
                
                if (abs(velocity[i]) < velocityThreshold) {
                    velocity[i] = 0f
                }
            }
        }
    }
    
    /**
     * Aktuelle Pose abrufen
     */
    fun getCurrentPose(): ARPose {
        val orientation = FloatArray(3)
        SensorManager.getOrientation(fusedMatrix, orientation)
        
        return ARPose(
            position = position.clone(),
            orientation = orientation,
            rotationMatrix = fusedMatrix.clone(),
            confidence = calculateTrackingConfidence()
        )
    }
    
    /**
     * Tracking-Konfidenz berechnen
     */
    private fun calculateTrackingConfidence(): Float {
        // Basierend auf Sensor-Stabilität und Konsistenz
        val accelerationMagnitude = sqrt(acceleration[0] * acceleration[0] + 
                                       acceleration[1] * acceleration[1] + 
                                       acceleration[2] * acceleration[2])
        
        val velocityMagnitude = sqrt(velocity[0] * velocity[0] + 
                                   velocity[1] * velocity[1] + 
                                   velocity[2] * velocity[2])
        
        // Hohe Konfidenz bei geringer Bewegung
        val motionFactor = 1f - (accelerationMagnitude / 10f + velocityMagnitude / 5f).coerceIn(0f, 1f)
        
        return motionFactor.coerceIn(0.1f, 1f)
    }
    
    /**
     * Position zurücksetzen
     */
    fun resetPosition() {
        position.fill(0f)
        velocity.fill(0f)
        acceleration.fill(0f)
        kalmanFilter.reset()
    }
    
    /**
     * Tracking-Listener setzen
     */
    fun setTrackingListener(listener: ARTrackingListener) {
        this.trackingListener = listener
    }
    
    /**
     * Ressourcen freigeben
     */
    fun cleanup() {
        stopTracking()
        trackingListener = null
    }
}

/**
 * Kalman Filter für Sensor-Fusion
 */
class KalmanFilter {
    private val stateSize = 9 // 3D Position, Geschwindigkeit, Beschleunigung
    private val measurementSize = 3 // 3D Beschleunigung
    
    private val state = FloatArray(stateSize) // [x, y, z, vx, vy, vz, ax, ay, az]
    private val covariance = Array(stateSize) { FloatArray(stateSize) }
    private val processNoise = Array(stateSize) { FloatArray(stateSize) }
    private val measurementNoise = Array(measurementSize) { FloatArray(measurementSize) }
    
    init {
        reset()
    }
    
    /**
     * Filter zurücksetzen
     */
    fun reset() {
        state.fill(0f)
        
        // Kovarianz-Matrix initialisieren
        for (i in 0 until stateSize) {
            for (j in 0 until stateSize) {
                covariance[i][j] = if (i == j) 1f else 0f
            }
        }
        
        // Prozess-Rauschen
        for (i in 0 until stateSize) {
            for (j in 0 until stateSize) {
                processNoise[i][j] = if (i == j) 0.01f else 0f
            }
        }
        
        // Mess-Rauschen
        for (i in 0 until measurementSize) {
            for (j in 0 until measurementSize) {
                measurementNoise[i][j] = if (i == j) 0.1f else 0f
            }
        }
    }
    
    /**
     * Beschleunigungsmessung aktualisieren
     */
    fun updateAcceleration(acceleration: FloatArray) {
        // Vereinfachte Kalman-Filter Implementierung
        val dt = 0.02f
        
        // Vorhersage-Schritt
        predict(dt)
        
        // Update-Schritt mit Beschleunigungsmessung
        update(acceleration)
    }
    
    /**
     * Vorhersage-Schritt
     */
    private fun predict(dt: Float) {
        // Position = Position + Geschwindigkeit * dt + 0.5 * Beschleunigung * dt²
        state[0] += state[3] * dt + 0.5f * state[6] * dt * dt
        state[1] += state[4] * dt + 0.5f * state[7] * dt * dt
        state[2] += state[5] * dt + 0.5f * state[8] * dt * dt
        
        // Geschwindigkeit = Geschwindigkeit + Beschleunigung * dt
        state[3] += state[6] * dt
        state[4] += state[7] * dt
        state[5] += state[8] * dt
        
        // Beschleunigung bleibt konstant (wird durch Messung aktualisiert)
    }
    
    /**
     * Update-Schritt
     */
    private fun update(measurement: FloatArray) {
        // Vereinfachtes Update: Beschleunigung direkt übernehmen mit Gewichtung
        val alpha = 0.7f
        
        state[6] = alpha * state[6] + (1 - alpha) * measurement[0]
        state[7] = alpha * state[7] + (1 - alpha) * measurement[1]
        state[8] = alpha * state[8] + (1 - alpha) * measurement[2]
    }
    
    /**
     * Gefilterte Position abrufen
     */
    fun getPosition(): FloatArray {
        return floatArrayOf(state[0], state[1], state[2])
    }
    
    /**
     * Gefilterte Geschwindigkeit abrufen
     */
    fun getVelocity(): FloatArray {
        return floatArrayOf(state[3], state[4], state[5])
    }
}

/**
 * Datenklassen für AR-Tracking
 */
data class ARPose(
    val position: FloatArray,
    val orientation: FloatArray, // Euler-Winkel: Azimut, Pitch, Roll
    val rotationMatrix: FloatArray,
    val confidence: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as ARPose
        
        if (!position.contentEquals(other.position)) return false
        if (!orientation.contentEquals(other.orientation)) return false
        if (!rotationMatrix.contentEquals(other.rotationMatrix)) return false
        if (confidence != other.confidence) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = position.contentHashCode()
        result = 31 * result + orientation.contentHashCode()
        result = 31 * result + rotationMatrix.contentHashCode()
        result = 31 * result + confidence.hashCode()
        return result
    }
}

/**
 * Interface für Tracking-Events
 */
interface ARTrackingListener {
    fun onPoseUpdated(pose: ARPose)
    fun onTrackingLost()
    fun onTrackingRecovered()
}
