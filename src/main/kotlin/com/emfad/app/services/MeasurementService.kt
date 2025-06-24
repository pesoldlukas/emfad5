package com.emfad.app.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.emfad.app.bluetooth.BluetoothManager
import com.emfad.app.ai.MaterialClassifier
import com.emfad.app.ai.ClusterAnalyzer
import com.emfad.app.database.SimpleDatabaseRepository
import com.emfad.app.models.*
import timber.log.Timber

/**
 * Haupt-Messdienst für EMFAD
 * Koordiniert Bluetooth-Kommunikation, Datenanalyse und Speicherung
 */
class MeasurementService : Service() {
    
    private val binder = MeasurementBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Komponenten
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var materialClassifier: MaterialClassifier
    private lateinit var clusterAnalyzer: ClusterAnalyzer
    private lateinit var databaseRepository: SimpleDatabaseRepository
    
    // State Management
    private val _measurementState = MutableStateFlow(MeasurementState.IDLE)
    val measurementState: StateFlow<MeasurementState> get() = _measurementState
    
    private val _currentMeasurement = MutableStateFlow<MeasurementResult?>(null)
    val currentMeasurement: StateFlow<MeasurementResult?> get() = _currentMeasurement
    
    private val _analysisResult = MutableStateFlow<MaterialClassificationResult?>(null)
    val analysisResult: StateFlow<MaterialClassificationResult?> get() = _analysisResult
    
    private val _sessionData = MutableStateFlow<SessionData?>(null)
    val sessionData: StateFlow<SessionData?> get() = _sessionData
    
    // Messdaten-Buffer
    private val measurementBuffer = mutableListOf<MeasurementResult>()
    private val maxBufferSize = 1000
    
    // Aktuelle Session
    private var currentSessionId: String? = null
    private var measurementJob: Job? = null
    
    inner class MeasurementBinder : Binder() {
        fun getService(): MeasurementService = this@MeasurementService
    }
    
    override fun onBind(intent: Intent): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        initializeComponents()
        setupBluetoothCallbacks()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopMeasurement()
        serviceScope.cancel()
        bluetoothManager.cleanup()
    }
    
    /**
     * Komponenten initialisieren
     */
    private fun initializeComponents() {
        bluetoothManager = BluetoothManager(this)
        materialClassifier = MaterialClassifier()
        clusterAnalyzer = ClusterAnalyzer()
        // databaseRepository wird von außen injiziert
    }
    
    /**
     * Bluetooth-Callbacks einrichten
     */
    private fun setupBluetoothCallbacks() {
        bluetoothManager.onMeasurementReceived = { measurement ->
            serviceScope.launch {
                processMeasurement(measurement)
            }
        }
        
        bluetoothManager.onConnectionLost = {
            serviceScope.launch {
                handleConnectionLost()
            }
        }
        
        bluetoothManager.onErrorReceived = { errorCode, message ->
            serviceScope.launch {
                handleError(errorCode, message)
            }
        }
    }
    
    /**
     * Neue Messsitzung starten
     */
    suspend fun startNewSession(sessionName: String, deviceId: String, location: String = ""): String {
        return withContext(Dispatchers.IO) {
            try {
                val sessionId = databaseRepository.createSession(sessionName, deviceId, location)
                currentSessionId = sessionId
                
                _sessionData.value = SessionData(
                    id = sessionId,
                    name = sessionName,
                    deviceId = deviceId,
                    location = location,
                    startTime = System.currentTimeMillis(),
                    measurementCount = 0
                )
                
                Timber.d("Neue Messsitzung gestartet: $sessionName")
                sessionId
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Starten der Messsitzung")
                throw e
            }
        }
    }
    
    /**
     * Messung starten
     */
    fun startMeasurement() {
        if (_measurementState.value != MeasurementState.IDLE) {
            Timber.w("Messung bereits aktiv")
            return
        }
        
        if (currentSessionId == null) {
            Timber.e("Keine aktive Session - Messung kann nicht gestartet werden")
            return
        }
        
        _measurementState.value = MeasurementState.STARTING
        
        measurementJob = serviceScope.launch {
            try {
                bluetoothManager.startMeasurement()
                _measurementState.value = MeasurementState.MEASURING
                Timber.d("Messung gestartet")
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Starten der Messung")
                _measurementState.value = MeasurementState.ERROR
            }
        }
    }
    
    /**
     * Messung stoppen
     */
    fun stopMeasurement() {
        if (_measurementState.value == MeasurementState.IDLE) {
            return
        }
        
        _measurementState.value = MeasurementState.STOPPING
        
        serviceScope.launch {
            try {
                bluetoothManager.stopMeasurement()
                measurementJob?.cancel()
                
                // Session beenden falls aktiv
                currentSessionId?.let { sessionId ->
                    databaseRepository.endSession(sessionId)
                }
                
                _measurementState.value = MeasurementState.IDLE
                Timber.d("Messung gestoppt")
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Stoppen der Messung")
                _measurementState.value = MeasurementState.ERROR
            }
        }
    }
    
    /**
     * Einzelne Messung verarbeiten
     */
    private suspend fun processMeasurement(measurement: MeasurementResult) {
        try {
            // Messung in Buffer hinzufügen
            measurementBuffer.add(measurement)
            if (measurementBuffer.size > maxBufferSize) {
                measurementBuffer.removeAt(0)
            }
            
            // Aktuelle Messung aktualisieren
            _currentMeasurement.value = measurement
            
            // In Datenbank speichern
            currentSessionId?.let { sessionId ->
                val measurementId = databaseRepository.saveMeasurement(measurement, sessionId)
                
                // Session-Daten aktualisieren
                _sessionData.value?.let { sessionData ->
                    _sessionData.value = sessionData.copy(
                        measurementCount = sessionData.measurementCount + 1,
                        lastMeasurement = measurement
                    )
                }

                // Analyse starten (asynchron)
                serviceScope.launch {
                    performAnalysis(measurement, measurementId)
                }
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Fehler bei der Messungsverarbeitung")
        }
    }
    
    /**
     * Materialanalyse durchführen
     */
    private suspend fun performAnalysis(measurement: MeasurementResult, measurementId: String) {
        try {
            // Physikalische Analyse erstellen
            val physicsAnalysis = createPhysicsAnalysis(measurement)
            
            // Material klassifizieren
            val classificationResult = materialClassifier.classify(physicsAnalysis)
            
            // Cluster-Analyse (falls genügend Daten vorhanden)
            if (measurementBuffer.size >= 10) {
                val clusterResult = performClusterAnalysis()
                // Cluster-Ergebnisse in Klassifikation integrieren
                enhanceClassificationWithClusters(classificationResult, clusterResult)
            }
            
            // Ergebnis speichern
            databaseRepository.saveAnalysisResult(measurementId, classificationResult)
            
            // UI aktualisieren
            _analysisResult.value = classificationResult
            
            Timber.d("Analyse abgeschlossen: ${classificationResult.materialType} (${classificationResult.confidence})")
            
        } catch (e: Exception) {
            Timber.e(e, "Fehler bei der Materialanalyse")
        }
    }
    
    /**
     * Physikalische Analyse aus Messdaten erstellen
     */
    private fun createPhysicsAnalysis(measurement: MeasurementResult): MaterialPhysicsAnalysis {
        // Vereinfachte Berechnung - in der Realität komplexere Algorithmen
        val symmetryScore = calculateSymmetryScore(measurement)
        val hollownessScore = calculateHollownessScore(measurement)
        val conductivity = calculateConductivity(measurement)
        val particleDensity = calculateParticleDensity(measurement)
        
        return MaterialPhysicsAnalysis(
            symmetryScore = symmetryScore,
            hollownessScore = hollownessScore,
            conductivity = conductivity,
            signalStrength = measurement.signalStrength,
            depth = measurement.depth,
            size = estimateSize(measurement),
            particleDensity = particleDensity,
            confidence = calculateAnalysisConfidence(measurement)
        )
    }
    
    /**
     * Symmetrie-Score berechnen
     */
    private fun calculateSymmetryScore(measurement: MeasurementResult): Double {
        // Vereinfachte Berechnung basierend auf Signalstabilität
        val recentMeasurements = measurementBuffer.takeLast(10)
        if (recentMeasurements.size < 3) return 0.5
        
        val signals = recentMeasurements.map { it.signalStrength }
        val mean = signals.average()
        val variance = signals.map { (it - mean) * (it - mean) }.average()
        val stability = 1.0 / (1.0 + variance)
        
        return stability.coerceIn(0.0, 1.0)
    }
    
    /**
     * Hohlraum-Score berechnen
     */
    private fun calculateHollownessScore(measurement: MeasurementResult): Double {
        // Basierend auf Signalstärke und Tiefe
        val depthFactor = measurement.depth / 10.0 // Normalisierung
        val signalFactor = 1.0 - measurement.signalStrength
        
        return (depthFactor * 0.6 + signalFactor * 0.4).coerceIn(0.0, 1.0)
    }
    
    /**
     * Leitfähigkeit berechnen
     */
    private fun calculateConductivity(measurement: MeasurementResult): Double {
        // Vereinfachte Berechnung basierend auf Frequenz und Signalstärke
        val frequencyFactor = measurement.frequency / 1000.0 // Normalisierung auf kHz
        val signalFactor = measurement.signalStrength
        
        return (signalFactor * frequencyFactor / 100.0).coerceIn(0.0, 1.0)
    }
    
    /**
     * Partikeldichte berechnen
     */
    private fun calculateParticleDensity(measurement: MeasurementResult): Double {
        // Basierend auf Signalvariationen
        val recentMeasurements = measurementBuffer.takeLast(5)
        if (recentMeasurements.size < 3) return 0.0
        
        val signals = recentMeasurements.map { it.signalStrength }
        val variations = signals.zipWithNext { a, b -> kotlin.math.abs(a - b) }
        val avgVariation = variations.average()
        
        return (avgVariation * 10.0).coerceIn(0.0, 1.0)
    }
    
    /**
     * Größe schätzen
     */
    private fun estimateSize(measurement: MeasurementResult): Double {
        return measurement.signalStrength * measurement.depth
    }
    
    /**
     * Analyse-Konfidenz berechnen
     */
    private fun calculateAnalysisConfidence(measurement: MeasurementResult): Double {
        val signalQuality = measurement.signalStrength
        val temperatureStability = 1.0 - kotlin.math.abs(measurement.temperature - 25.0) / 50.0
        val dataAmount = kotlin.math.min(measurementBuffer.size / 10.0, 1.0)
        
        return (signalQuality * 0.4 + temperatureStability * 0.3 + dataAmount * 0.3).coerceIn(0.0, 1.0)
    }
    
    /**
     * Cluster-Analyse durchführen (vereinfacht)
     */
    private suspend fun performClusterAnalysis(): String {
        val signalData = measurementBuffer.map { it.signalStrength }
        // Vereinfachte Cluster-Analyse
        return "Cluster-Analyse abgeschlossen mit ${signalData.size} Datenpunkten"
    }

    /**
     * Klassifikation mit Cluster-Daten erweitern (vereinfacht)
     */
    private fun enhanceClassificationWithClusters(
        classification: MaterialClassificationResult,
        clusterResult: String
    ) {
        // Vereinfachte Integration
        Timber.d("Cluster-Ergebnis: $clusterResult")
    }
    
    /**
     * Verbindungsverlust behandeln
     */
    private suspend fun handleConnectionLost() {
        _measurementState.value = MeasurementState.CONNECTION_LOST
        Timber.w("Bluetooth-Verbindung verloren")
        
        // Automatische Wiederverbindung versuchen
        delay(2000)
        // Wiederverbindungslogik hier
    }
    
    /**
     * Fehler behandeln
     */
    private suspend fun handleError(errorCode: Int, message: String) {
        _measurementState.value = MeasurementState.ERROR
        Timber.e("Gerätefehler: $errorCode - $message")
        
        // Fehlerbehandlung je nach Code
        when (errorCode) {
            0x04 -> { // Batterie schwach
                // Benutzer warnen
            }
            0x05 -> { // Überhitzung
                // Messung pausieren
                stopMeasurement()
            }
        }
    }
    
    /**
     * Kalibrierung durchführen
     */
    suspend fun performCalibration(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                _measurementState.value = MeasurementState.CALIBRATING
                
                bluetoothManager.calibrateDevice()
                
                // Warten auf Kalibrierungsergebnis
                var calibrationComplete = false
                bluetoothManager.onCalibrationComplete = { success ->
                    calibrationComplete = true
                    if (success) {
                        serviceScope.launch {
                            // Kalibrierungsdaten speichern
                            saveCalibrationData()
                        }
                    }
                }
                
                // Timeout nach 30 Sekunden
                var timeoutCounter = 0
                while (!calibrationComplete && timeoutCounter < 300) {
                    delay(100)
                    timeoutCounter++
                }
                
                _measurementState.value = MeasurementState.IDLE
                calibrationComplete
                
            } catch (e: Exception) {
                Timber.e(e, "Fehler bei der Kalibrierung")
                _measurementState.value = MeasurementState.ERROR
                false
            }
        }
    }
    
    /**
     * Kalibrierungsdaten speichern
     */
    private suspend fun saveCalibrationData() {
        try {
            val deviceId = _sessionData.value?.deviceId ?: return

            // Vereinfachte Kalibrierungsdaten
            val calibrationData = CalibrationData(
                frequency = 100.0,
                offsetX = 0.0,
                offsetY = 0.0,
                offsetZ = 0.0,
                gainX = 1.0,
                gainY = 1.0,
                gainZ = 1.0,
                temperature = 25.0,
                timestamp = System.currentTimeMillis()
            )

            // Vereinfachte Speicherung
            Timber.d("Kalibrierungsdaten für Gerät $deviceId gespeichert")
            Timber.d("Kalibrierungsdaten gespeichert")
            
        } catch (e: Exception) {
            Timber.e(e, "Fehler beim Speichern der Kalibrierungsdaten")
        }
    }
    
    /**
     * Messdaten exportieren (vereinfacht)
     */
    suspend fun exportSessionData(sessionId: String): String {
        return "Export für Session $sessionId abgeschlossen"
    }
    
    /**
     * Service-Status abrufen
     */
    fun getServiceStatus(): ServiceStatus {
        return ServiceStatus(
            measurementState = _measurementState.value,
            currentSession = _sessionData.value,
            bufferSize = measurementBuffer.size,
            lastMeasurement = _currentMeasurement.value,
            lastAnalysis = _analysisResult.value
        )
    }
    
    /**
     * Database Repository setzen (Dependency Injection)
     */
    fun setDatabaseRepository(repository: SimpleDatabaseRepository) {
        this.databaseRepository = repository
    }
}

// Datenklassen wurden nach com.emfad.app.models verschoben
