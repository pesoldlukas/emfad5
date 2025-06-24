package com.emfad.app.services.simulation

import android.util.Log
import com.emfad.app.services.communication.EMFADCommand
import com.emfad.app.services.communication.EMFADResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*
import kotlin.random.Random

/**
 * EMFAD® Device Simulator
 * Simuliert ein echtes EMFAD-Gerät für Tests und Development
 * Basiert auf den rekonstruierten Ghidra-Funktionen
 */

@Serializable
data class SimulatedMeasurement(
    val frequency: Double,
    val depth: Double,
    val signalStrength: Double,
    val phase: Double,
    val materialType: String,
    val noiseLevel: Double,
    val timestamp: Long
)

@Serializable
data class SimulationConfig(
    val enableNoise: Boolean = true,
    val noiseLevel: Double = 0.1,
    val responseDelay: Long = 100L,
    val batteryLevel: Int = 100,
    val temperature: Double = 20.0,
    val simulateErrors: Boolean = false,
    val errorRate: Double = 0.05
)

@Singleton
class EMFADDeviceSimulator @Inject constructor() {
    
    companion object {
        private const val TAG = "EMFADSimulator"
        
        // EMFAD-Frequenzen
        private val EMFAD_FREQUENCIES = listOf(
            19000.0, 23400.0, 70000.0, 77500.0, 124000.0, 129100.0, 135600.0
        )
        
        // Simulierte Untergrund-Schichten
        private val GROUND_LAYERS = listOf(
            GroundLayer("Topsoil", 0.0, 0.3, 1000.0, "soil_dry"),
            GroundLayer("Clay", 0.3, 1.2, 2000.0, "clay"),
            GroundLayer("Sand", 1.2, 2.5, 800.0, "sand"),
            GroundLayer("Rock", 2.5, 5.0, 500.0, "rock"),
            GroundLayer("Bedrock", 5.0, 10.0, 200.0, "rock")
        )
    }
    
    private val simulatorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Simulation State
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _currentFrequency = MutableStateFlow(77500.0)
    val currentFrequency: StateFlow<Double> = _currentFrequency.asStateFlow()
    
    private val _simulationConfig = MutableStateFlow(SimulationConfig())
    val simulationConfig: StateFlow<SimulationConfig> = _simulationConfig.asStateFlow()
    
    private val _responses = MutableSharedFlow<EMFADResponse>()
    val responses: SharedFlow<EMFADResponse> = _responses.asSharedFlow()
    
    // Simulation Parameters
    private var currentPosition = Position(0.0, 0.0)
    private var measurementCounter = 0
    
    /**
     * Simuliert Geräte-Verbindung
     */
    suspend fun connect(): Boolean {
        return try {
            delay(1000) // Verbindungszeit simulieren
            _isConnected.value = true
            
            // Verbindungs-Response senden
            val response = EMFADResponse(
                command = "CONNECT",
                status = "OK",
                data = "EMFAD UG12 DS WL Simulator v1.0",
                timestamp = System.currentTimeMillis(),
                deviceId = "SIM_DEVICE_001"
            )
            
            _responses.emit(response)
            
            Log.d(TAG, "EMFAD Device Simulator connected")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect simulator", e)
            false
        }
    }
    
    /**
     * Simuliert Geräte-Trennung
     */
    fun disconnect() {
        _isConnected.value = false
        Log.d(TAG, "EMFAD Device Simulator disconnected")
    }
    
    /**
     * Verarbeitet EMFAD-Kommandos
     */
    suspend fun processCommand(command: EMFADCommand): EMFADResponse {
        if (!_isConnected.value) {
            return EMFADResponse(
                command = command.command,
                status = "ERROR",
                data = "Device not connected",
                timestamp = System.currentTimeMillis(),
                deviceId = "SIM_DEVICE_001"
            )
        }
        
        // Response-Delay simulieren
        delay(_simulationConfig.value.responseDelay)
        
        return when (command.command) {
            "SET_FREQUENCY" -> handleSetFrequency(command)
            "START_MEASUREMENT" -> handleStartMeasurement(command)
            "STOP_MEASUREMENT" -> handleStopMeasurement(command)
            "CALIBRATE" -> handleCalibrate(command)
            "GET_STATUS" -> handleGetStatus(command)
            "GET_BATTERY" -> handleGetBattery(command)
            else -> EMFADResponse(
                command = command.command,
                status = "ERROR",
                data = "Unknown command",
                timestamp = System.currentTimeMillis(),
                deviceId = "SIM_DEVICE_001"
            )
        }
    }
    
    private fun handleSetFrequency(command: EMFADCommand): EMFADResponse {
        val frequency = command.frequency
        
        return if (frequency != null && EMFAD_FREQUENCIES.contains(frequency)) {
            _currentFrequency.value = frequency
            
            EMFADResponse(
                command = "SET_FREQUENCY",
                status = "OK",
                data = "Frequency set to ${frequency}Hz",
                timestamp = System.currentTimeMillis(),
                deviceId = "SIM_DEVICE_001",
                frequency = frequency
            )
        } else {
            EMFADResponse(
                command = "SET_FREQUENCY",
                status = "ERROR",
                data = "Invalid frequency",
                timestamp = System.currentTimeMillis(),
                deviceId = "SIM_DEVICE_001"
            )
        }
    }
    
    private suspend fun handleStartMeasurement(command: EMFADCommand): EMFADResponse {
        val frequency = _currentFrequency.value
        val measurement = simulateMeasurement(frequency)
        
        // Messdaten als ByteArray simulieren
        val measurementData = createSimulatedSignalData(measurement)
        
        return EMFADResponse(
            command = "START_MEASUREMENT",
            status = "OK",
            data = "Measurement completed",
            timestamp = System.currentTimeMillis(),
            deviceId = "SIM_DEVICE_001",
            frequency = frequency,
            rawData = measurementData
        )
    }
    
    private fun handleStopMeasurement(command: EMFADCommand): EMFADResponse {
        return EMFADResponse(
            command = "STOP_MEASUREMENT",
            status = "OK",
            data = "Measurement stopped",
            timestamp = System.currentTimeMillis(),
            deviceId = "SIM_DEVICE_001"
        )
    }
    
    private fun handleCalibrate(command: EMFADCommand): EMFADResponse {
        val calibrationType = command.parameters?.get("type") ?: "unknown"
        
        return EMFADResponse(
            command = "CALIBRATE",
            status = "OK",
            data = "Calibration completed: $calibrationType",
            timestamp = System.currentTimeMillis(),
            deviceId = "SIM_DEVICE_001"
        )
    }
    
    private fun handleGetStatus(command: EMFADCommand): EMFADResponse {
        val status = mapOf(
            "frequency" to _currentFrequency.value,
            "battery" to _simulationConfig.value.batteryLevel,
            "temperature" to _simulationConfig.value.temperature,
            "connected" to _isConnected.value
        )
        
        return EMFADResponse(
            command = "GET_STATUS",
            status = "OK",
            data = status.toString(),
            timestamp = System.currentTimeMillis(),
            deviceId = "SIM_DEVICE_001"
        )
    }
    
    private fun handleGetBattery(command: EMFADCommand): EMFADResponse {
        return EMFADResponse(
            command = "GET_BATTERY",
            status = "OK",
            data = "${_simulationConfig.value.batteryLevel}%",
            timestamp = System.currentTimeMillis(),
            deviceId = "SIM_DEVICE_001"
        )
    }
    
    /**
     * Simuliert eine EMFAD-Messung basierend auf Position und Frequenz
     */
    private fun simulateMeasurement(frequency: Double): SimulatedMeasurement {
        measurementCounter++
        
        // Simuliere verschiedene Untergründe basierend auf Position
        val layer = findGroundLayer(currentPosition)
        val baseSignal = layer.baseSignalStrength
        
        // Frequenz-abhängige Dämpfung
        val frequencyFactor = when {
            frequency < 30000 -> 1.2 // Niedrige Frequenzen dringen tiefer ein
            frequency > 100000 -> 0.8 // Hohe Frequenzen werden stärker gedämpft
            else -> 1.0
        }
        
        // Tiefe basierend auf Signal berechnen (vereinfacht)
        val depth = layer.startDepth + Random.nextDouble() * (layer.endDepth - layer.startDepth)
        
        // Signal-Stärke mit Tiefe und Frequenz
        val signalStrength = baseSignal * frequencyFactor * exp(-depth * 0.3)
        
        // Phase basierend auf Material und Tiefe
        val phase = when (layer.materialType) {
            "metal" -> 10.0 + Random.nextDouble() * 10.0
            "water" -> 45.0 + Random.nextDouble() * 20.0
            "clay" -> 60.0 + Random.nextDouble() * 15.0
            else -> 30.0 + Random.nextDouble() * 20.0
        }
        
        // Rauschen hinzufügen
        val noise = if (_simulationConfig.value.enableNoise) {
            Random.nextDouble() * _simulationConfig.value.noiseLevel * signalStrength
        } else 0.0
        
        return SimulatedMeasurement(
            frequency = frequency,
            depth = depth,
            signalStrength = signalStrength + noise,
            phase = phase,
            materialType = layer.materialType,
            noiseLevel = noise,
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun findGroundLayer(position: Position): GroundLayer {
        // Simuliere verschiedene Geologie basierend auf Position
        val layerIndex = (abs(position.x + position.y) % GROUND_LAYERS.size).toInt()
        return GROUND_LAYERS[layerIndex]
    }
    
    private fun createSimulatedSignalData(measurement: SimulatedMeasurement): ByteArray {
        // Simuliert Raw-Signal-Daten als komplexe Zahlen
        val sampleCount = 1024
        val signal = ByteArray(sampleCount * 8) // 2 * 4 Bytes pro Sample
        
        val buffer = java.nio.ByteBuffer.wrap(signal).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        
        for (i in 0 until sampleCount) {
            val real = (measurement.signalStrength * cos(toRadians(measurement.phase)) + 
                       Random.nextDouble() * measurement.noiseLevel).toFloat()
            val imaginary = (measurement.signalStrength * sin(toRadians(measurement.phase)) + 
                           Random.nextDouble() * measurement.noiseLevel).toFloat()
            
            buffer.putFloat(real)
            buffer.putFloat(imaginary)
        }
        
        return signal
    }
    
    /**
     * Setzt Simulation-Position (für verschiedene Untergründe)
     */
    fun setPosition(x: Double, y: Double) {
        currentPosition = Position(x, y)
    }
    
    /**
     * Aktualisiert Simulation-Konfiguration
     */
    fun updateConfig(config: SimulationConfig) {
        _simulationConfig.value = config
    }
    
    fun cleanup() {
        disconnect()
        simulatorScope.cancel()
    }
}

data class Position(val x: Double, val y: Double)

data class GroundLayer(
    val name: String,
    val startDepth: Double,
    val endDepth: Double,
    val baseSignalStrength: Double,
    val materialType: String
)
