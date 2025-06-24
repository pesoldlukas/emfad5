package com.emfad.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.emfad.app.services.MeasurementService
import com.emfad.app.services.DataService
import com.emfad.app.database.SimpleDatabaseRepository
// ConnectionState aus models verwenden
import com.emfad.app.models.*
import timber.log.Timber

/**
 * Haupt-ViewModel für EMFAD-App
 * Koordiniert alle UI-States und Business Logic
 */
class MainViewModel(
    private val measurementService: MeasurementService,
    private val dataService: DataService,
    private val databaseRepository: SimpleDatabaseRepository
) : ViewModel() {
    
    // UI State Management
    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> get() = _uiState
    
    private val _currentScreen = MutableStateFlow(Screen.MAIN)
    val currentScreen: StateFlow<Screen> get() = _currentScreen
    
    // Measurement State
    private val _measurementState = MutableStateFlow(MeasurementState.IDLE)
    val measurementState: StateFlow<MeasurementState> get() = _measurementState
    
    private val _currentMeasurement = MutableStateFlow<MeasurementResult?>(null)
    val currentMeasurement: StateFlow<MeasurementResult?> get() = _currentMeasurement
    
    private val _analysisResult = MutableStateFlow<MaterialClassificationResult?>(null)
    val analysisResult: StateFlow<MaterialClassificationResult?> get() = _analysisResult
    
    // Session Management
    private val _currentSession = MutableStateFlow<SessionData?>(null)
    val currentSession: StateFlow<SessionData?> get() = _currentSession
    
    private val _allSessions = MutableStateFlow<List<MeasurementSession>>(emptyList())
    val allSessions: StateFlow<List<MeasurementSession>> get() = _allSessions
    
    // Device Management
    private val _connectedDevice = MutableStateFlow<DeviceInfo?>(null)
    val connectedDevice: StateFlow<DeviceInfo?> get() = _connectedDevice
    
    private val _availableDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val availableDevices: StateFlow<List<DeviceInfo>> get() = _availableDevices
    
    // Export/Import Status
    private val _exportStatus = MutableStateFlow<ExportStatus>(ExportStatus.Idle)
    val exportStatus: StateFlow<ExportStatus> get() = _exportStatus
    
    // Error Handling
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> get() = _errorMessage
    
    init {
        initializeApp()
        observeServiceStates()
    }
    
    /**
     * App initialisieren
     */
    fun initializeApp() {
        viewModelScope.launch {
            try {
                // Bluetooth-Verfügbarkeit prüfen
                val bluetoothAvailable = checkBluetoothRequirements()
                
                if (bluetoothAvailable) {
                    // Gespeicherte Geräte laden
                    loadSavedDevices()
                    
                    // Sessions laden
                    loadSessions()
                    
                    _uiState.value = MainUiState.Ready(ReadyState())
                } else {
                    _uiState.value = MainUiState.Error("Bluetooth nicht verfügbar")
                }
            } catch (e: Exception) {
                Timber.e(e, "Fehler bei der App-Initialisierung")
                _uiState.value = MainUiState.Error("Initialisierungsfehler: ${e.message}")
            }
        }
    }
    
    /**
     * Service-States beobachten
     */
    private fun observeServiceStates() {
        viewModelScope.launch {
            // Measurement Service State
            measurementService.measurementState.collect { state ->
                _measurementState.value = state
            }
        }
        
        viewModelScope.launch {
            // Current Measurement
            measurementService.currentMeasurement.collect { measurement ->
                _currentMeasurement.value = measurement
            }
        }
        
        viewModelScope.launch {
            // Analysis Results
            measurementService.analysisResult.collect { result ->
                _analysisResult.value = result
            }
        }
        
        viewModelScope.launch {
            // Session Data
            measurementService.sessionData.collect { session ->
                _currentSession.value = session
            }
        }
        
        viewModelScope.launch {
            // Export Status
            dataService.exportStatus.collect { status ->
                _exportStatus.value = status
            }
        }
    }
    
    /**
     * Bluetooth-Voraussetzungen prüfen
     */
    private suspend fun checkBluetoothRequirements(): Boolean {
        // Vereinfachte Implementierung
        return try {
            // Hier würde normalerweise die Bluetooth-Verfügbarkeit geprüft
            true
        } catch (e: Exception) {
            Timber.e(e, "Bluetooth-Prüfung fehlgeschlagen")
            false
        }
    }
    
    /**
     * Gespeicherte Geräte laden
     */
    private suspend fun loadSavedDevices() {
        try {
            databaseRepository.getAllDevices().collect { devices ->
                _availableDevices.value = devices
                
                // Aktives Gerät finden
                val activeDevice = devices.find { it.isConnected }
                _connectedDevice.value = activeDevice
            }
        } catch (e: Exception) {
            Timber.e(e, "Fehler beim Laden der Geräte")
        }
    }
    
    /**
     * Sessions laden
     */
    private suspend fun loadSessions() {
        try {
            databaseRepository.getAllSessions().collect { sessions ->
                _allSessions.value = sessions
            }
        } catch (e: Exception) {
            Timber.e(e, "Fehler beim Laden der Sessions")
        }
    }
    
    /**
     * Navigation
     */
    fun navigateToScreen(screen: Screen) {
        _currentScreen.value = screen
    }
    
    fun navigateBack() {
        _currentScreen.value = Screen.MAIN
    }
    
    /**
     * Session Management
     */
    fun startNewSession(sessionName: String, location: String = "") {
        viewModelScope.launch {
            try {
                val deviceId = _connectedDevice.value?.id ?: "unknown"
                val sessionId = measurementService.startNewSession(sessionName, deviceId, location)
                
                Timber.d("Neue Session gestartet: $sessionId")
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Starten der Session")
                _errorMessage.value = "Session konnte nicht gestartet werden: ${e.message}"
            }
        }
    }
    
    fun endCurrentSession() {
        viewModelScope.launch {
            try {
                measurementService.stopMeasurement()
                _currentSession.value = null
                
                Timber.d("Session beendet")
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Beenden der Session")
                _errorMessage.value = "Session konnte nicht beendet werden: ${e.message}"
            }
        }
    }
    
    /**
     * Measurement Control
     */
    fun startMeasurement() {
        if (_currentSession.value == null) {
            _errorMessage.value = "Keine aktive Session - bitte zuerst Session starten"
            return
        }
        
        try {
            measurementService.startMeasurement()
        } catch (e: Exception) {
            Timber.e(e, "Fehler beim Starten der Messung")
            _errorMessage.value = "Messung konnte nicht gestartet werden: ${e.message}"
        }
    }
    
    fun stopMeasurement() {
        try {
            measurementService.stopMeasurement()
        } catch (e: Exception) {
            Timber.e(e, "Fehler beim Stoppen der Messung")
            _errorMessage.value = "Messung konnte nicht gestoppt werden: ${e.message}"
        }
    }
    
    fun pauseMeasurement() {
        // Pause-Funktionalität
        stopMeasurement()
    }
    
    /**
     * Calibration
     */
    fun startCalibration() {
        viewModelScope.launch {
            try {
                val success = measurementService.performCalibration()
                if (success) {
                    Timber.d("Kalibrierung erfolgreich")
                } else {
                    _errorMessage.value = "Kalibrierung fehlgeschlagen"
                }
            } catch (e: Exception) {
                Timber.e(e, "Fehler bei der Kalibrierung")
                _errorMessage.value = "Kalibrierung fehlgeschlagen: ${e.message}"
            }
        }
    }
    
    /**
     * Data Export
     */
    fun exportCurrentSession(format: ExportFormat) {
        val sessionId = _currentSession.value?.id
        if (sessionId == null) {
            _errorMessage.value = "Keine aktive Session zum Exportieren"
            return
        }
        
        viewModelScope.launch {
            try {
                val result = when (format) {
                    ExportFormat.CSV -> dataService.exportSessionAsCSV(sessionId)
                    ExportFormat.JSON -> dataService.exportSessionAsJSON(sessionId)
                    ExportFormat.MATLAB -> dataService.exportSessionAsMAT(sessionId)
                    ExportFormat.PDF -> dataService.exportSessionAsPDF(sessionId)
                }
                
                when (result) {
                    is ExportResult.Success -> {
                        Timber.d("Export erfolgreich: ${result.filePath}")
                    }
                    is ExportResult.Error -> {
                        _errorMessage.value = "Export fehlgeschlagen: ${result.message}"
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Export")
                _errorMessage.value = "Export fehlgeschlagen: ${e.message}"
            }
        }
    }
    
    fun exportAllSessions() {
        viewModelScope.launch {
            try {
                val result = dataService.exportAllSessions()
                when (result) {
                    is ExportResult.Success -> {
                        Timber.d("Gesamt-Export erfolgreich: ${result.filePath}")
                    }
                    is ExportResult.Error -> {
                        _errorMessage.value = "Export fehlgeschlagen: ${result.message}"
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Gesamt-Export")
                _errorMessage.value = "Export fehlgeschlagen: ${e.message}"
            }
        }
    }
    
    /**
     * Device Management
     */
    fun connectToDevice(device: DeviceInfo) {
        viewModelScope.launch {
            try {
                // Gerät in Datenbank speichern/aktualisieren
                databaseRepository.saveDevice(device)
                databaseRepository.updateDeviceStatus(device.id, device.batteryLevel, true)
                
                _connectedDevice.value = device
                
                Timber.d("Gerät verbunden: ${device.name}")
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Verbinden des Geräts")
                _errorMessage.value = "Gerät konnte nicht verbunden werden: ${e.message}"
            }
        }
    }
    
    fun disconnectDevice() {
        viewModelScope.launch {
            try {
                _connectedDevice.value?.let { device ->
                    databaseRepository.updateDeviceStatus(device.id, device.batteryLevel, false)
                }
                
                _connectedDevice.value = null
                
                Timber.d("Gerät getrennt")
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Trennen des Geräts")
                _errorMessage.value = "Gerät konnte nicht getrennt werden: ${e.message}"
            }
        }
    }
    
    /**
     * Statistics
     */
    fun getSessionStatistics(sessionId: String): Flow<MeasurementStatistics> {
        return flow {
            try {
                val stats = databaseRepository.getMeasurementStatistics(sessionId)
                emit(stats)
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Laden der Statistiken")
                emit(MeasurementStatistics(0, 0.0, 0.0, 0.0))
            }
        }
    }
    
    fun getMaterialTypeStatistics(): Flow<Map<String, Int>> {
        return flow {
            try {
                val stats = databaseRepository.getMaterialTypeStatistics()
                emit(stats)
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Laden der Materialstatistiken")
                emit(emptyMap())
            }
        }
    }
    
    /**
     * Error Handling
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun handleError(error: Throwable) {
        Timber.e(error, "Unbehandelter Fehler")
        _errorMessage.value = error.message ?: "Unbekannter Fehler"
    }
    
    /**
     * Settings
     */
    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch {
            try {
                // Settings speichern
                // Vereinfachte Implementierung
                Timber.d("Einstellungen aktualisiert")
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Speichern der Einstellungen")
                _errorMessage.value = "Einstellungen konnten nicht gespeichert werden: ${e.message}"
            }
        }
    }
    
    /**
     * Cleanup
     */
    override fun onCleared() {
        super.onCleared()
        // Cleanup wenn ViewModel zerstört wird
    }
}

// UI State Definitionen wurden nach com.emfad.app.models verschoben
