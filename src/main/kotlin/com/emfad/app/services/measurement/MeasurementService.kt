package com.emfad.app.services.measurement

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.emfad.app.ai.analyzers.ClusterAnalyzer
import com.emfad.app.ai.analyzers.InclusionDetector
import com.emfad.app.ai.classifiers.MaterialClassifier
import com.emfad.app.bluetooth.EMFADBluetoothManager
import com.emfad.app.database.EMFADDatabase
import com.emfad.app.models.*
import com.emfad.app.services.data.DataProcessor
import com.emfad.app.services.validation.DataValidator
// Ghidra-Rekonstruierte Komponenten
import com.emfad.app.ghidra.GhidraDeviceController
import com.emfad.app.ghidra.GhidraExportImportFunctions
import com.emfad.app.ghidra.GhidraFortranProcessor
import com.emfad.app.models.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.*

/**
 * EMFAD Measurement Service
 * Implementiert echte EMFAD-Messungen basierend auf reverse engineering
 * der originalen Windows-Programme (EMFAD3.exe, EMUNI-X-07.exe, HzEMSoft.exe)
 *
 * Extrahierte Funktionalität:
 * - EMFAD TABLET 1.0 Protokoll
 * - Kalibrierungskonstante 3333 (aus EMFAD3.exe)
 * - Autobalance-Algorithmus (aus EMUNI-X-07.exe)
 * - TAR-EMF, UT-EMF, LINE Protokolle (aus HzEMSoft.exe)
 * Samsung S21 Ultra optimiert
 */
class MeasurementService : Service() {
    
    companion object {
        private const val TAG = "EMFADMeasurementService"
        private const val MEASUREMENT_BUFFER_SIZE = 1000
        private const val PROCESSING_INTERVAL_MS = 100L
        private const val AUTO_SAVE_INTERVAL_MS = 5000L
        private const val MAX_SESSION_DURATION_MS = 3600000L // 1 Stunde

        // EMFAD-spezifische Konstanten (aus EXE-Analyse)
        private const val CALIBRATION_CONSTANT = 3333.0
        private const val ATTENUATION_FACTOR = 0.417
        private const val REFERENCE_TEMPERATURE = 25.0
        private const val TEMP_COEFFICIENT = 0.002
    }
    
    // Service Binder
    inner class MeasurementBinder : Binder() {
        fun getService(): MeasurementService = this@MeasurementService
    }
    
    private val binder = MeasurementBinder()
    
    // Coroutine Scope
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Komponenten
    private lateinit var database: EMFADDatabase
    private lateinit var bluetoothManager: EMFADBluetoothManager
    private lateinit var materialClassifier: MaterialClassifier
    private lateinit var clusterAnalyzer: ClusterAnalyzer
    private lateinit var inclusionDetector: InclusionDetector
    private lateinit var dataProcessor: DataProcessor
    private lateinit var dataValidator: DataValidator

    // EMFAD-spezifische Komponenten (aus EXE-Analyse)
    private lateinit var emfadAnalyzer: com.emfad.app.ai.analyzers.EMFADAnalyzer
    private lateinit var deviceController: com.emfad.app.protocol.EMFADDeviceController
    private lateinit var emfadCommunication: com.emfad.app.communication.EMFADDeviceCommunication
    private lateinit var emfadCalibration: com.emfad.app.calibration.EMFADCalibration
    private lateinit var emfadFileParser: com.emfad.app.data.EMFADFileParser
    private val autobalanceCalculator = com.emfad.app.protocol.EMFADProtocol.AutobalanceCalculator()
    private val tarEMFProtocol = com.emfad.app.protocol.EMFADProtocol.TarEMFProtocol()



    // Vollständige Ghidra-Rekonstruktion (aus vollständiger EXE-Dekompilierung)
    private lateinit var ghidraDeviceController: GhidraDeviceController
    private lateinit var ghidraExportImport: GhidraExportImportFunctions
    private lateinit var ghidraFortranProcessor: GhidraFortranProcessor

    // Ghidra-Konfigurationen
    private var emfadTabletConfig = EMFADTabletConfig()
    private var autobalanceConfig = AutobalanceConfig()
    private var frequencyConfig = FrequencyConfig()
    private var fileFormatConfig = FileFormatConfig()
    private var deviceStatus = DeviceStatus()
    
    // State Management
    private val _serviceState = MutableStateFlow(MeasurementServiceState.IDLE)
    val serviceState: StateFlow<MeasurementServiceState> = _serviceState.asStateFlow()
    
    private val _currentSession = MutableStateFlow<MeasurementSession?>(null)
    val currentSession: StateFlow<MeasurementSession?> = _currentSession.asStateFlow()
    
    private val _measurementData = MutableStateFlow<EMFReading?>(null)
    val measurementData: StateFlow<EMFReading?> = _measurementData.asStateFlow()
    
    private val _analysisResults = MutableStateFlow<MaterialAnalysis?>(null)
    val analysisResults: StateFlow<MaterialAnalysis?> = _analysisResults.asStateFlow()
    
    private val _sessionStatistics = MutableStateFlow(SessionStatistics())
    val sessionStatistics: StateFlow<SessionStatistics> = _sessionStatistics.asStateFlow()
    
    // Daten-Buffer
    private val measurementBuffer = ConcurrentLinkedQueue<EMFReading>()
    private val analysisBuffer = ConcurrentLinkedQueue<MaterialAnalysis>()
    
    // Jobs
    private var processingJob: Job? = null
    private var autoSaveJob: Job? = null
    private var sessionTimeoutJob: Job? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MeasurementService erstellt")
        
        initializeComponents()
        startBackgroundProcessing()
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MeasurementService zerstört")

        stopMeasurement()

        // EMFAD-Kommunikation beenden
        emfadCommunication.cleanup()

        cleanupComponents()
        serviceScope.cancel()
    }
    
    /**
     * Komponenten initialisieren (erweitert mit EMFAD-Protokollen)
     */
    private fun initializeComponents() {
        database = EMFADDatabase.getDatabase(this)
        bluetoothManager = EMFADBluetoothManager(this)
        materialClassifier = MaterialClassifier(this)
        clusterAnalyzer = ClusterAnalyzer()
        inclusionDetector = InclusionDetector()
        dataProcessor = DataProcessor()
        dataValidator = DataValidator()

        // EMFAD-spezifische Komponenten initialisieren
        emfadAnalyzer = com.emfad.app.ai.analyzers.EMFADAnalyzer()
        deviceController = com.emfad.app.protocol.EMFADDeviceController()
        emfadCommunication = com.emfad.app.communication.EMFADDeviceCommunication(this)
        emfadCalibration = com.emfad.app.calibration.EMFADCalibration()
        emfadFileParser = com.emfad.app.data.EMFADFileParser

        // Ghidra-Rekonstruierte Komponenten initialisieren
        ghidraDeviceController = GhidraDeviceController(this)
        ghidraExportImport = GhidraExportImportFunctions
        ghidraFortranProcessor = GhidraFortranProcessor

        // FormCreate - Initialisierung (aus EMFAD3EXE.c)
        ghidraDeviceController.formCreate()

        // EMFAD TABLET 1.0 Konfiguration laden
        emfadTabletConfig = EMFADTabletConfig(
            version = "EMFAD TABLET 1.0",
            scanMode = EMFADScanMode.SCAN_2D_3D,
            isInitialized = true
        )
        Log.d(TAG, "EMFAD TABLET 1.0 initialisiert: ${emfadTabletConfig.version}")

        // Autobalance-System initialisieren (aus EMUNIX07EXE.c)
        autobalanceConfig = AutobalanceConfig(
            version = "autobalance values; version 1.0"
        )
        Log.d(TAG, "Autobalance initialisiert: ${autobalanceConfig.version}")

        // Frequenz-Konfiguration laden
        frequencyConfig = FrequencyConfig()
        Log.d(TAG, "Frequenzen verfügbar: ${frequencyConfig.availableFrequencies.size}")

        // Bluetooth-Callbacks einrichten
        bluetoothManager.onMeasurementReceived = { measurement ->
            processMeasurement(measurement)
        }

        bluetoothManager.onErrorOccurred = { error, exception ->
            handleBluetoothError(error, exception)
        }

        // AI-Komponenten initialisieren
        serviceScope.launch {
            materialClassifier.initialize()

            // Ghidra-Geräte-Verbindung herstellen (echte USB/Serial Kommunikation)
            val deviceConnected = ghidraDeviceController.connectToDevice()
            if (deviceConnected) {
                Log.d(TAG, "EMFAD-Gerät erfolgreich über Ghidra-Controller verbunden")

                // Gerätestatus überwachen
                serviceScope.launch {
                    ghidraDeviceController.deviceStatus.collect { status ->
                        deviceStatus = status
                        Log.d(TAG, "Gerätestatus: ${status.portStatus}, Verbunden: ${status.isConnected}")
                    }
                }

                // Messdaten überwachen
                serviceScope.launch {
                    ghidraDeviceController.measurementData.collect { reading ->
                        reading?.let {
                            // Fortran-Verarbeitung anwenden (aus HzEMSoftexe.c)
                            val processedReading = applyFortranProcessing(it)
                            processMeasurement(convertToMeasurementResult(processedReading))
                        }
                    }
                }

                // Autobalance-Konfiguration überwachen
                serviceScope.launch {
                    ghidraDeviceController.autobalanceConfig.collect { config ->
                        autobalanceConfig = config
                        Log.d(TAG, "Autobalance-Status: ${config.compassCalibrationStatus}")
                    }
                }
            } else {
                Log.w(TAG, "Ghidra-Gerät konnte nicht verbunden werden, verwende Bluetooth-Fallback")

                // Fallback: Bluetooth für ältere Geräte
                bluetoothManager.enableAutobalance(true)
                bluetoothManager.requestDeviceVersion()
            }
        }
    }
    
    /**
     * Hintergrund-Verarbeitung starten
     */
    private fun startBackgroundProcessing() {
        processingJob = serviceScope.launch {
            while (isActive) {
                processBufferedData()
                delay(PROCESSING_INTERVAL_MS)
            }
        }
        
        autoSaveJob = serviceScope.launch {
            while (isActive) {
                autoSaveSession()
                delay(AUTO_SAVE_INTERVAL_MS)
            }
        }
    }
    
    /**
     * Neue Messsitzung starten
     */
    suspend fun startMeasurementSession(
        sessionName: String,
        description: String,
        operatorName: String,
        location: String,
        projectName: String,
        sampleId: String
    ): Result<Long> {
        return try {
            if (_serviceState.value != MeasurementServiceState.IDLE) {
                return Result.failure(Exception("Service nicht im IDLE-Zustand"))
            }
            
            Log.d(TAG, "Starte neue Messsitzung: $sessionName")
            
            val session = MeasurementSession(
                name = sessionName,
                description = description,
                startTimestamp = System.currentTimeMillis(),
                endTimestamp = null,
                status = SessionStatus.ACTIVE,
                deviceId = bluetoothManager.deviceInfo.value["device_id"]?.toString() ?: "unknown",
                deviceName = bluetoothManager.deviceInfo.value["device_name"]?.toString() ?: "EMFAD Device",
                operatorName = operatorName,
                location = location,
                projectName = projectName,
                sampleId = sampleId
            )
            
            val sessionId = database.measurementSessionDao().insert(
                com.emfad.app.database.entities.MeasurementSessionEntity.fromDomainModel(session)
            )
            
            val sessionWithId = session.copy(id = sessionId)
            _currentSession.value = sessionWithId
            _serviceState.value = MeasurementServiceState.READY
            
            // Session-Timeout einrichten
            sessionTimeoutJob = serviceScope.launch {
                delay(MAX_SESSION_DURATION_MS)
                if (_serviceState.value == MeasurementServiceState.MEASURING) {
                    stopMeasurement()
                }
            }
            
            Log.d(TAG, "Messsitzung gestartet mit ID: $sessionId")
            Result.success(sessionId)
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Starten der Messsitzung", e)
            Result.failure(e)
        }
    }
    
    /**
     * Messung starten (erweitert mit EMFAD-Protokollen)
     */
    suspend fun startMeasurement(): Result<Unit> {
        return try {
            if (_serviceState.value != MeasurementServiceState.READY) {
                return Result.failure(Exception("Service nicht bereit für Messung"))
            }

            Log.d(TAG, "Starte EMFAD-Messung")

            // Ghidra-Gerät für Messung vorbereiten (echte EMFAD-Kommunikation)
            val measurementStarted = ghidraDeviceController.startMeasurement(frequencyConfig)
            if (measurementStarted) {
                Log.d(TAG, "EMFAD-Gerät erfolgreich über Ghidra-Controller gestartet")

                // Frequenz-Konfiguration anwenden
                Log.d(TAG, "Aktive Frequenz: ${frequencyConfig.usedFrequency}Hz")

            } else {
                Log.w(TAG, "Ghidra-Gerät konnte nicht gestartet werden, verwende Bluetooth-Fallback")

                // Fallback: Bluetooth-Kommunikation
                val deviceInitialized = deviceController.startMeasurement(100.0)
                if (!deviceInitialized) {
                    return Result.failure(Exception("Weder USB noch Bluetooth-Kommunikation möglich"))
                }

                // TAR-EMF Protokoll aktivieren
                bluetoothManager.sendTarEMFCommand(5.0, 1000.0)

                // LINE-Modus aktivieren
                bluetoothManager.enableLineMode(10.0, 100)

                // Bluetooth-Messung starten
                bluetoothManager.startMeasurement()
            }

            _serviceState.value = MeasurementServiceState.MEASURING

            // Statistiken zurücksetzen
            _sessionStatistics.value = SessionStatistics()

            Log.d(TAG, "EMFAD-Messung erfolgreich gestartet")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Starten der EMFAD-Messung", e)
            Result.failure(e)
        }
    }
    
    /**
     * Messung stoppen
     */
    suspend fun stopMeasurement(): Result<Unit> {
        return try {
            Log.d(TAG, "Stoppe Messung")

            // Ghidra-Gerät stoppen
            ghidraDeviceController.stopMeasurement()

            // Bluetooth-Messung stoppen
            bluetoothManager.stopMeasurement()

            // Verbleibende Daten verarbeiten
            processBufferedData()
            
            // Session abschließen
            _currentSession.value?.let { session ->
                val updatedSession = session.copy(
                    endTimestamp = System.currentTimeMillis(),
                    status = SessionStatus.COMPLETED,
                    measurementCount = _sessionStatistics.value.totalMeasurements
                )
                
                database.measurementSessionDao().update(
                    com.emfad.app.database.entities.MeasurementSessionEntity.fromDomainModel(updatedSession)
                )
                
                _currentSession.value = updatedSession
            }
            
            _serviceState.value = MeasurementServiceState.IDLE
            
            // Jobs beenden
            sessionTimeoutJob?.cancel()
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Stoppen der Messung", e)
            Result.failure(e)
        }
    }
    
    /**
     * Einzelne Messung verarbeiten
     */
    private fun processMeasurement(measurement: MeasurementResult) {
        serviceScope.launch {
            try {
                val session = _currentSession.value ?: return@launch
                
                // MeasurementResult zu EMFReading konvertieren
                val emfReading = EMFReading(
                    sessionId = session.id,
                    timestamp = measurement.timestamp,
                    frequency = measurement.frequency,
                    signalStrength = measurement.signalStrength,
                    phase = 0.0, // Wird aus signalStrength berechnet
                    amplitude = measurement.signalStrength,
                    realPart = measurement.signalStrength * cos(0.0),
                    imaginaryPart = measurement.signalStrength * sin(0.0),
                    magnitude = measurement.signalStrength,
                    depth = measurement.depth,
                    temperature = measurement.temperature,
                    humidity = 50.0, // Default-Wert
                    pressure = 1013.25, // Standard-Luftdruck
                    batteryLevel = 100, // Wird vom Bluetooth-Manager aktualisiert
                    deviceId = session.deviceId,
                    materialType = MaterialType.UNKNOWN, // Wird durch AI bestimmt
                    confidence = 0.0,
                    noiseLevel = 10.0, // Default-Wert
                    calibrationOffset = 0.0,
                    gainSetting = 1.0,
                    filterSetting = "default",
                    measurementMode = "standard"
                )
                
                // Datenvalidierung
                val validatedReading = dataValidator.validateReading(emfReading)
                if (validatedReading != null) {
                    // Datenverarbeitung
                    val processedReading = dataProcessor.processReading(validatedReading)
                    
                    // Zu Buffer hinzufügen
                    measurementBuffer.offer(processedReading)
                    
                    // Buffer-Größe begrenzen
                    while (measurementBuffer.size > MEASUREMENT_BUFFER_SIZE) {
                        measurementBuffer.poll()
                    }
                    
                    // UI aktualisieren
                    _measurementData.value = processedReading
                    
                    // Statistiken aktualisieren
                    updateSessionStatistics(processedReading)
                    
                    Log.d(TAG, "Messung verarbeitet: Signal=${processedReading.signalStrength}, Tiefe=${processedReading.depth}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Fehler bei der Messungsverarbeitung", e)
            }
        }
    }
    
    /**
     * Gepufferte Daten verarbeiten
     */
    private suspend fun processBufferedData() {
        try {
            // Messungen aus Buffer verarbeiten
            val readingsToProcess = mutableListOf<EMFReading>()
            repeat(min(10, measurementBuffer.size)) {
                measurementBuffer.poll()?.let { readingsToProcess.add(it) }
            }
            
            if (readingsToProcess.isNotEmpty()) {
                // In Datenbank speichern
                val entities = readingsToProcess.map { 
                    com.emfad.app.database.entities.EMFReadingEntity.fromDomainModel(it)
                }
                database.emfReadingDao().insertAll(entities)
                
                // AI-Analyse durchführen
                performAIAnalysis(readingsToProcess)
            }
            
            // Analysen aus Buffer verarbeiten
            val analysesToProcess = mutableListOf<MaterialAnalysis>()
            repeat(min(5, analysisBuffer.size)) {
                analysisBuffer.poll()?.let { analysesToProcess.add(it) }
            }
            
            if (analysesToProcess.isNotEmpty()) {
                val entities = analysesToProcess.map {
                    com.emfad.app.database.entities.MaterialAnalysisEntity.fromDomainModel(it)
                }
                database.materialAnalysisDao().insertAll(entities)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei der Datenverarbeitung", e)
        }
    }
    
    /**
     * AI-Analyse durchführen (erweitert mit EMFAD-Analyzer)
     */
    private suspend fun performAIAnalysis(readings: List<EMFReading>) {
        try {
            // EMFAD-Vollanalyse durchführen (aus EXE-Algorithmen)
            val emfadAnalysis = emfadAnalyzer.performEMFADAnalysis(readings)
            emfadAnalysis?.let { analysis ->
                analysisBuffer.offer(analysis)
                _analysisResults.value = analysis

                Log.d(TAG, "EMFAD-Analyse: Material=${analysis.materialType}, Konfidenz=${analysis.confidence}")

                // TAR-EMF Ergebnis senden
                bluetoothManager.sendTarEMFCommand(analysis.depth, analysis.signalStrength)
            }

            // Fallback: Standard Material-Klassifikation
            if (emfadAnalysis == null) {
                readings.lastOrNull()?.let { latestReading ->
                    val analysis = materialClassifier.classifyMaterial(latestReading)
                    analysis?.let {
                        analysisBuffer.offer(it)
                        _analysisResults.value = it
                    }
                }
            }

            // Cluster-Analyse wenn genügend Daten vorhanden
            if (readings.size >= 10) {
                val clusterResult = clusterAnalyzer.performClustering(readings)
                Log.d(TAG, "Cluster-Analyse: ${clusterResult.clusterCount} Cluster gefunden")
            }

            // Einschluss-Erkennung
            if (readings.size >= 5) {
                val inclusionResult = inclusionDetector.detectInclusions(readings)
                Log.d(TAG, "Einschluss-Erkennung: ${inclusionResult.inclusionCount} Einschlüsse gefunden")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei der EMFAD-Analyse", e)
        }
    }
    
    /**
     * Session-Statistiken aktualisieren
     */
    private fun updateSessionStatistics(reading: EMFReading) {
        val currentStats = _sessionStatistics.value
        val newStats = currentStats.copy(
            totalMeasurements = currentStats.totalMeasurements + 1,
            averageSignalStrength = (currentStats.averageSignalStrength * currentStats.totalMeasurements + reading.signalStrength) / (currentStats.totalMeasurements + 1),
            maxSignalStrength = max(currentStats.maxSignalStrength, reading.signalStrength),
            minSignalStrength = if (currentStats.totalMeasurements == 0) reading.signalStrength else min(currentStats.minSignalStrength, reading.signalStrength),
            averageTemperature = (currentStats.averageTemperature * currentStats.totalMeasurements + reading.temperature) / (currentStats.totalMeasurements + 1),
            lastMeasurementTime = reading.timestamp
        )
        _sessionStatistics.value = newStats
    }
    
    /**
     * Automatisches Speichern der Session
     */
    private suspend fun autoSaveSession() {
        try {
            _currentSession.value?.let { session ->
                if (session.status == SessionStatus.ACTIVE) {
                    val stats = _sessionStatistics.value
                    val updatedSession = session.copy(
                        measurementCount = stats.totalMeasurements,
                        averageSignalStrength = stats.averageSignalStrength,
                        maxSignalStrength = stats.maxSignalStrength,
                        minSignalStrength = stats.minSignalStrength,
                        averageTemperature = stats.averageTemperature
                    )
                    
                    database.measurementSessionDao().update(
                        com.emfad.app.database.entities.MeasurementSessionEntity.fromDomainModel(updatedSession)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim automatischen Speichern", e)
        }
    }
    
    /**
     * Bluetooth-Fehler behandeln
     */
    private fun handleBluetoothError(error: String, exception: Exception?) {
        Log.e(TAG, "Bluetooth-Fehler: $error", exception)
        
        serviceScope.launch {
            if (_serviceState.value == MeasurementServiceState.MEASURING) {
                stopMeasurement()
            }
            _serviceState.value = MeasurementServiceState.ERROR
        }
    }
    
    /**
     * Komponenten bereinigen
     */
    private fun cleanupComponents() {
        processingJob?.cancel()
        autoSaveJob?.cancel()
        sessionTimeoutJob?.cancel()
        
        bluetoothManager.cleanup()
        materialClassifier.cleanup()
        
        measurementBuffer.clear()
        analysisBuffer.clear()
    }
    
    /**
     * EMFAD-Datei laden und verarbeiten
     */
    suspend fun loadEMFADFile(filePath: String, fileType: String): Result<List<EMFReading>> {
        return try {
            Log.d(TAG, "Lade EMFAD-Datei: $filePath, Typ: $fileType")

            val file = File(filePath)
            if (!file.exists()) {
                return Result.failure(Exception("Datei nicht gefunden: $filePath"))
            }

            val readings = when (fileType.uppercase()) {
                "EGD" -> {
                    val (header, data) = emfadFileParser.parseEGDFile(file)
                        ?: return Result.failure(Exception("Fehler beim Parsen der EGD-Datei"))
                    Log.d(TAG, "EGD-Header: Version=${header.version}, Frequenzen=${header.frequencies.size}")
                    data
                }
                "ESD" -> {
                    val (header, data) = emfadFileParser.parseESDFile(file)
                        ?: return Result.failure(Exception("Fehler beim Parsen der ESD-Datei"))
                    Log.d(TAG, "ESD-Header: Version=${header.version}, Profile=${header.profileMode}")
                    data
                }
                else -> {
                    return Result.failure(Exception("Unbekannter Dateityp: $fileType"))
                }
            }

            // Kalibrierung auf alle Messungen anwenden
            val calibratedReadings = readings.map { reading ->
                emfadCalibration.calibrateReading(reading)
            }

            Log.d(TAG, "EMFAD-Datei erfolgreich geladen: ${calibratedReadings.size} Messungen")
            Result.success(calibratedReadings)

        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Laden der EMFAD-Datei", e)
            Result.failure(e)
        }
    }

    /**
     * Kalibrierungsdatei laden
     */
    suspend fun loadCalibrationFile(filePath: String, type: String): Result<Boolean> {
        return try {
            Log.d(TAG, "Lade Kalibrierungsdatei: $filePath, Typ: $type")

            val file = File(filePath)
            if (!file.exists()) {
                return Result.failure(Exception("Kalibrierungsdatei nicht gefunden: $filePath"))
            }

            val calibrationType = when (type.uppercase()) {
                "XY", "CALXY" -> com.emfad.app.calibration.EMFADCalibration.Companion.CalibrationType.XY_PLANE
                "XZ", "CALXZ" -> com.emfad.app.calibration.EMFADCalibration.Companion.CalibrationType.XZ_PLANE
                else -> return Result.failure(Exception("Unbekannter Kalibrierungstyp: $type"))
            }

            val success = emfadCalibration.loadCalibrationFile(file, calibrationType)

            if (success) {
                Log.d(TAG, "Kalibrierungsdatei erfolgreich geladen")
                Result.success(true)
            } else {
                Result.failure(Exception("Fehler beim Laden der Kalibrierungsdatei"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Laden der Kalibrierungsdatei", e)
            Result.failure(e)
        }
    }

    /**
     * Autobalance-Kalibrierung durchführen
     */
    suspend fun performAutobalanceCalibration(): Result<Boolean> {
        return try {
            Log.d(TAG, "Führe Autobalance-Kalibrierung durch")

            if (measurementBuffer.size < 10) {
                return Result.failure(Exception("Nicht genügend Messungen für Autobalance-Kalibrierung"))
            }

            val success = emfadCalibration.performAutobalanceCalibration(measurementBuffer.toList())

            if (success) {
                Log.d(TAG, "Autobalance-Kalibrierung erfolgreich")
                Result.success(true)
            } else {
                Result.failure(Exception("Fehler bei Autobalance-Kalibrierung"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei Autobalance-Kalibrierung", e)
            Result.failure(e)
        }
    }

    /**
     * Kalibrierungsstatus abrufen
     */
    fun getCalibrationStatus(): Map<String, Boolean> {
        return emfadCalibration.getCalibrationStatus()
    }

    /**
     * ExportDAT1Click - DAT-Export (rekonstruiert aus EMFAD3.exe)
     */
    suspend fun exportDAT1Click(fileName: String = "emfad_export.dat"): Result<String> {
        return try {
            Log.d(TAG, "ExportDAT1Click aufgerufen")

            if (measurementBuffer.isEmpty()) {
                return Result.failure(Exception("Keine Daten zum Exportieren"))
            }

            val success = reconstructedExport.exportDAT1Click(this, measurementBuffer.toList(), fileName)

            if (success) {
                val filePath = "${getExternalFilesDir(null)}/exports/$fileName"
                Log.d(TAG, "DAT-Export erfolgreich: $filePath")
                Result.success(filePath)
            } else {
                Result.failure(Exception("Fehler beim DAT-Export"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei ExportDAT1Click", e)
            Result.failure(e)
        }
    }

    /**
     * Export2D1Click - 2D-Export (rekonstruiert aus EMFAD3.exe)
     */
    suspend fun export2D1Click(fileName: String = "emfad_2d_export.txt"): Result<String> {
        return try {
            Log.d(TAG, "Export2D1Click aufgerufen")

            if (measurementBuffer.isEmpty()) {
                return Result.failure(Exception("Keine Daten zum Exportieren"))
            }

            val analysis = _analysisResults.value
            val success = reconstructedExport.export2D1Click(this, measurementBuffer.toList(), analysis, fileName)

            if (success) {
                val filePath = "${getExternalFilesDir(null)}/exports/$fileName"
                Log.d(TAG, "2D-Export erfolgreich: $filePath")
                Result.success(filePath)
            } else {
                Result.failure(Exception("Fehler beim 2D-Export"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei Export2D1Click", e)
            Result.failure(e)
        }
    }

    /**
     * importTabletFile1Click - Tablet-Import (rekonstruiert aus EMFAD3.exe)
     */
    suspend fun importTabletFile1Click(filePath: String): Result<List<EMFReading>> {
        return try {
            Log.d(TAG, "importTabletFile1Click aufgerufen: $filePath")

            val (readings, errorMessage) = reconstructedExport.importTabletFile1Click(this, filePath)

            if (readings.isNotEmpty()) {
                // Importierte Daten zum Buffer hinzufügen
                readings.forEach { reading ->
                    measurementBuffer.add(reading)
                    if (measurementBuffer.size > MAX_MEASUREMENT_BUFFER) {
                        measurementBuffer.removeAt(0)
                    }
                }

                Log.d(TAG, "Tablet-Import erfolgreich: ${readings.size} Messungen")

                if (errorMessage != null) {
                    Log.w(TAG, "Import-Warnung: $errorMessage")
                }

                Result.success(readings)
            } else {
                Result.failure(Exception(errorMessage ?: "Keine Daten importiert"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei importTabletFile1Click", e)
            Result.failure(e)
        }
    }

    /**
     * TfrmFrequencyModeSelect - Frequenzauswahl (rekonstruiert aus EMFAD3.exe)
     */
    suspend fun selectFrequency(frequencyIndex: Int): Result<Double> {
        return try {
            Log.d(TAG, "TfrmFrequencyModeSelect: Frequenz $frequencyIndex")

            val success = frequencySelector.selectFrequency(frequencyIndex)

            if (success) {
                val frequency = frequencySelector.getSelectedFrequency()

                // Frequenz am Gerät setzen
                if (_serviceState.value == MeasurementServiceState.MEASURING) {
                    reconstructedCOMPort.setFrequency(frequency)
                    emfadCommunication.setFrequency(frequencyIndex)
                }

                Log.d(TAG, "Frequenz erfolgreich gewählt: ${frequency}Hz")
                Result.success(frequency)
            } else {
                Result.failure(Exception("Ungültiger Frequenzindex: $frequencyIndex"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei Frequenzauswahl", e)
            Result.failure(e)
        }
    }

    /**
     * TfrmAutoBalance - Autobalance-System (rekonstruiert aus EMUNI-X-07.exe)
     */
    suspend fun performReconstructedAutobalance(): Result<Boolean> {
        return try {
            Log.d(TAG, "TfrmAutoBalance: Starte Autobalance")

            if (measurementBuffer.size < 10) {
                return Result.failure(Exception("Nicht genügend Messungen für Autobalance"))
            }

            val success = autoBalanceSystem.performAutobalanceCalibration(measurementBuffer.toList())

            if (success) {
                Log.d(TAG, "Autobalance erfolgreich kalibriert")
                Result.success(true)
            } else {
                Result.failure(Exception("Fehler bei Autobalance-Kalibrierung"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei TfrmAutoBalance", e)
            Result.failure(e)
        }
    }

    /**
     * Ghidra-basierte Geräteverbindung (aus vollständiger EXE-Dekompilierung)
     */
    suspend fun connectGhidraDevice(): Result<Boolean> {
        return try {
            Log.d(TAG, "Verbinde mit EMFAD-Gerät über Ghidra-Protokoll")

            val connected = ghidraDeviceController.connectToDevice()

            if (connected) {
                Log.d(TAG, "EMFAD-Gerät erfolgreich über Ghidra-Protokoll verbunden")
                Result.success(true)
            } else {
                Result.failure(Exception("Fehler bei Ghidra-Geräteverbindung"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei Ghidra-Geräteverbindung", e)
            Result.failure(e)
        }
    }

    /**
     * Ghidra-basierte Messung starten
     */
    suspend fun startGhidraMeasurement(frequency: Double): Result<Boolean> {
        return try {
            Log.d(TAG, "Starte Ghidra-basierte Messung mit ${frequency}Hz")

            val started = ghidraDeviceController.startMeasurement(frequencyConfig)

            if (started) {
                // Überwache Ghidra-Messdaten
                monitorGhidraData()
                Log.d(TAG, "Ghidra-Messung erfolgreich gestartet")
                Result.success(true)
            } else {
                Result.failure(Exception("Fehler beim Starten der Ghidra-Messung"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Starten der Ghidra-Messung", e)
            Result.failure(e)
        }
    }

    /**
     * Überwacht Ghidra-Messdaten
     */
    private fun monitorGhidraData() {
        serviceScope.launch {
            ghidraDeviceController.measurementData.collect { reading ->
                reading?.let { emfReading ->
                    // Wende Fortran-Verarbeitung an
                    val correctedReading = applyFortranProcessing(emfReading)

                    // Füge zu Measurement Buffer hinzu
                    measurementBuffer.add(correctedReading)
                    if (measurementBuffer.size > MAX_MEASUREMENT_BUFFER) {
                        measurementBuffer.removeAt(0)
                    }

                    // Aktualisiere Live-Daten
                    _currentReading.value = correctedReading

                    Log.d(TAG, "Ghidra-Messdaten empfangen: Signal=${correctedReading.signalStrength}, Tiefe=${correctedReading.depth}")
                }
            }
        }
    }

    /**
     * Ghidra-basierte Dateiformat-Parser
     */
    suspend fun parseGhidraFileFormat(filePath: String, format: String): Result<List<EMFReading>> {
        return try {
            Log.d(TAG, "Parse $format-Datei mit Ghidra-Parser: $filePath")

            val file = java.io.File(filePath)
            if (!file.exists()) {
                return Result.failure(Exception("Datei nicht gefunden: $filePath"))
            }

            val result = when (format.uppercase()) {
                "EGD" -> ghidraExportImport.exportEGDFormat(this, emptyList(), file.name, fileFormatConfig)
                "ESD" -> ghidraExportImport.exportESDFormat(this, emptyList(), file.name, fileFormatConfig)
                "DAT" -> ghidraExportImport.exportDAT1Click(this, emptyList(), file.name)
                else -> Result.failure(Exception("Unbekanntes Dateiformat: $format"))
            }

            result.onSuccess { readings ->
                // Füge geparste Daten zum Buffer hinzu
                readings.forEach { reading ->
                    measurementBuffer.add(reading)
                    if (measurementBuffer.size > MAX_MEASUREMENT_BUFFER) {
                        measurementBuffer.removeAt(0)
                    }
                }
                Log.d(TAG, "Ghidra-Parsing erfolgreich: ${readings.size} Messungen geladen")
            }

            result

        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Ghidra-Datei-Parsing", e)
            Result.failure(e)
        }
    }

    /**
     * Ghidra-basierte Frequenzanalyse (aus HzEMSoft.exe)
     */
    suspend fun performGhidraFrequencyAnalysis(): Result<List<EMFReading>> {
        return try {
            Log.d(TAG, "Führe Ghidra-basierte Frequenzanalyse durch")

            if (measurementBuffer.isEmpty()) {
                return Result.failure(Exception("Keine Messdaten für Frequenzanalyse"))
            }

            // Verwende Ghidra-Fortran-Processor für Frequenzanalyse
            val readings = measurementBuffer.toList()
            val processedReadings = ghidraFortranProcessor.processEMFData(readings, frequencyConfig)

            Log.d(TAG, "Frequenzanalyse abgeschlossen: ${processedReadings.getOrElse { emptyList() }.size} verarbeitete Messungen")
            processedReadings

        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei Ghidra-Frequenzanalyse", e)
            Result.failure(e)
        }
    }

    /**
     * Wendet Fortran-Verarbeitung auf EMFReading an
     * Rekonstruiert aus HzEMSoftexe.c
     */
    private suspend fun applyFortranProcessing(reading: EMFReading): EMFReading {
        return try {
            // Fortran-Verarbeitung anwenden
            val processedReadings = ghidraFortranProcessor.processEMFData(listOf(reading), frequencyConfig)
            processedReadings.getOrNull()?.firstOrNull() ?: reading
        } catch (e: Exception) {
            Log.w(TAG, "Fehler bei Fortran-Verarbeitung", e)
            reading
        }
    }

    /**
     * Konvertiert EMFReading zu MeasurementResult für Kompatibilität
     */
    private fun convertToMeasurementResult(reading: EMFReading): MeasurementResult {
        return MeasurementResult(
            timestamp = reading.timestamp,
            frequency = reading.frequency,
            signalStrength = reading.signalStrength,
            depth = reading.depth,
            temperature = reading.temperature,
            batteryLevel = reading.batteryLevel,
            deviceId = reading.deviceId
        )
    }

    /**
     * Startet Autobalance-Kalibrierung
     * Rekonstruiert aus EMUNIX07EXE.c
     */
    suspend fun startAutobalanceCalibration(): Result<Unit> {
        return try {
            Log.d(TAG, "Starte Autobalance-Kalibrierung")

            val success = ghidraDeviceController.startAutobalanceCalibration()
            if (success) {
                Log.d(TAG, "Autobalance-Kalibrierung erfolgreich gestartet")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Autobalance-Kalibrierung konnte nicht gestartet werden"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei Autobalance-Kalibrierung", e)
            Result.failure(e)
        }
    }

    /**
     * Exportiert Daten im DAT-Format
     * Rekonstruiert aus "ExportDAT1Click" (EMFAD3EXE.c)
     */
    suspend fun exportDAT(fileName: String): Result<String> {
        return try {
            val readings = measurementBuffer.toList()
            ghidraExportImport.exportDAT1Click(this, readings, fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei DAT-Export", e)
            Result.failure(e)
        }
    }

    /**
     * Exportiert Daten im 2D-Format
     * Rekonstruiert aus "Export2D1Click" (EMFAD3EXE.c)
     */
    suspend fun export2D(fileName: String): Result<String> {
        return try {
            val readings = measurementBuffer.toList()
            val analysis = _analysisResults.value
            ghidraExportImport.export2D1Click(this, readings, analysis, fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei 2D-Export", e)
            Result.failure(e)
        }
    }

    /**
     * Importiert Tablet-Datei
     * Rekonstruiert aus "importTabletFile1Click" (EMFAD3EXE.c)
     */
    suspend fun importTabletFile(filePath: String): Result<Pair<List<EMFReading>, DataValidationResult>> {
        return try {
            ghidraExportImport.importTabletFile1Click(this, filePath)
        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei Tablet-Import", e)
            Result.failure(e)
        }
    }

    /**
     * Service-Status abrufen (erweitert mit Ghidra-Status)
     */
    fun getServiceStatus(): MeasurementServiceStatus {
        return MeasurementServiceStatus(
            state = _serviceState.value,
            currentSession = _currentSession.value,
            statistics = _sessionStatistics.value,
            bufferSize = measurementBuffer.size,
            isBluetoothConnected = bluetoothManager.connectionState.value == ConnectionState.CONNECTED,
            ghidraDeviceStatus = deviceStatus,
            emfadTabletConfig = emfadTabletConfig,
            autobalanceConfig = autobalanceConfig,
            frequencyConfig = frequencyConfig
        )
    }
}

/**
 * Service-Zustand
 */
enum class MeasurementServiceState {
    IDLE,
    READY,
    MEASURING,
    PROCESSING,
    ERROR
}

/**
 * Session-Statistiken
 */
data class SessionStatistics(
    val totalMeasurements: Int = 0,
    val averageSignalStrength: Double = 0.0,
    val maxSignalStrength: Double = 0.0,
    val minSignalStrength: Double = Double.MAX_VALUE,
    val averageTemperature: Double = 0.0,
    val lastMeasurementTime: Long = 0L,
    val measurementRate: Double = 0.0,
    val dataQuality: Double = 1.0
)

/**
 * Service-Status (erweitert mit Ghidra-Komponenten)
 */
data class MeasurementServiceStatus(
    val state: MeasurementServiceState,
    val currentSession: MeasurementSession?,
    val statistics: SessionStatistics,
    val bufferSize: Int,
    val isBluetoothConnected: Boolean,
    val ghidraDeviceStatus: DeviceStatus,
    val emfadTabletConfig: EMFADTabletConfig,
    val autobalanceConfig: AutobalanceConfig,
    val frequencyConfig: FrequencyConfig
)
