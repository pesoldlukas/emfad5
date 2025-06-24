package com.emfad.app.viewmodels.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emfad.app.bluetooth.EMFADBluetoothManager
import com.emfad.app.bluetooth.EMFADBluetoothScanner
import com.emfad.app.database.EMFADDatabase
import com.emfad.app.models.*
import com.emfad.app.services.measurement.MeasurementService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Dashboard ViewModel für EMFAD App
 * Samsung S21 Ultra optimiert mit MVVM Pattern
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val database: EMFADDatabase,
    private val bluetoothManager: EMFADBluetoothManager,
    private val bluetoothScanner: EMFADBluetoothScanner
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    // Bluetooth State
    private val _bluetoothState = MutableStateFlow(BluetoothState())
    val bluetoothState: StateFlow<BluetoothState> = _bluetoothState.asStateFlow()
    
    // Recent Sessions
    private val _recentSessions = MutableStateFlow<List<MeasurementSession>>(emptyList())
    val recentSessions: StateFlow<List<MeasurementSession>> = _recentSessions.asStateFlow()
    
    // Statistics
    private val _dashboardStats = MutableStateFlow(DashboardStatistics())
    val dashboardStats: StateFlow<DashboardStatistics> = _dashboardStats.asStateFlow()
    
    // Service Connection
    private var measurementService: MeasurementService? = null
    
    init {
        initializeViewModel()
        observeBluetoothState()
        loadDashboardData()
    }
    
    /**
     * ViewModel initialisieren
     */
    private fun initializeViewModel() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Bluetooth-Scanner initialisieren
                if (!bluetoothScanner.hasRequiredPermissions()) {
                    _bluetoothState.value = _bluetoothState.value.copy(
                        hasPermissions = false,
                        permissionError = "Bluetooth-Berechtigungen fehlen"
                    )
                }
                
                _uiState.value = _uiState.value.copy(isLoading = false)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Initialisierungsfehler: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Bluetooth-Zustand überwachen
     */
    private fun observeBluetoothState() {
        viewModelScope.launch {
            // Verbindungsstatus überwachen
            bluetoothManager.connectionState.collect { connectionState ->
                _bluetoothState.value = _bluetoothState.value.copy(
                    connectionState = connectionState,
                    isConnected = connectionState == ConnectionState.CONNECTED
                )
            }
        }
        
        viewModelScope.launch {
            // Geräteinformationen überwachen
            bluetoothManager.deviceInfo.collect { deviceInfo ->
                _bluetoothState.value = _bluetoothState.value.copy(
                    deviceInfo = deviceInfo
                )
            }
        }
        
        viewModelScope.launch {
            // Batteriestand überwachen
            bluetoothManager.batteryLevel.collect { batteryLevel ->
                _bluetoothState.value = _bluetoothState.value.copy(
                    batteryLevel = batteryLevel
                )
            }
        }
        
        viewModelScope.launch {
            // Entdeckte Geräte überwachen
            bluetoothScanner.discoveredDevices.collect { devices ->
                _bluetoothState.value = _bluetoothState.value.copy(
                    availableDevices = devices
                )
            }
        }
        
        viewModelScope.launch {
            // Scan-Status überwachen
            bluetoothScanner.isScanning.collect { isScanning ->
                _bluetoothState.value = _bluetoothState.value.copy(
                    isScanning = isScanning
                )
            }
        }
    }
    
    /**
     * Dashboard-Daten laden
     */
    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                // Aktuelle Sessions laden
                loadRecentSessions()
                
                // Statistiken berechnen
                calculateDashboardStatistics()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Fehler beim Laden der Dashboard-Daten: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Aktuelle Sessions laden
     */
    private suspend fun loadRecentSessions() {
        try {
            val sessions = database.measurementSessionDao().getLatest(10)
                .map { it.toDomainModel() }
            
            _recentSessions.value = sessions
            
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Fehler beim Laden der Sessions: ${e.message}"
            )
        }
    }
    
    /**
     * Dashboard-Statistiken berechnen
     */
    private suspend fun calculateDashboardStatistics() {
        try {
            val totalSessions = database.measurementSessionDao().getCount()
            val totalMeasurements = database.emfReadingDao().getCount()
            val totalAnalyses = database.materialAnalysisDao().getCount()
            
            val activeSessions = database.measurementSessionDao()
                .getByStatus(SessionStatus.ACTIVE).size
            
            val completedSessions = database.measurementSessionDao()
                .getByStatus(SessionStatus.COMPLETED).size
            
            val avgMeasurementsPerSession = database.measurementSessionDao()
                .getAverageMeasurementCount() ?: 0.0
            
            val stats = DashboardStatistics(
                totalSessions = totalSessions,
                totalMeasurements = totalMeasurements,
                totalAnalyses = totalAnalyses,
                activeSessions = activeSessions,
                completedSessions = completedSessions,
                averageMeasurementsPerSession = avgMeasurementsPerSession
            )
            
            _dashboardStats.value = stats
            
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Fehler beim Berechnen der Statistiken: ${e.message}"
            )
        }
    }
    
    /**
     * Bluetooth-Scan starten
     */
    fun startBluetoothScan() {
        viewModelScope.launch {
            try {
                if (!bluetoothScanner.hasRequiredPermissions()) {
                    _bluetoothState.value = _bluetoothState.value.copy(
                        permissionError = "Bluetooth-Berechtigungen erforderlich"
                    )
                    return@launch
                }
                
                if (!bluetoothScanner.isBluetoothEnabled()) {
                    _bluetoothState.value = _bluetoothState.value.copy(
                        permissionError = "Bluetooth ist nicht aktiviert"
                    )
                    return@launch
                }
                
                bluetoothScanner.startScan()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Fehler beim Starten des Bluetooth-Scans: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Bluetooth-Scan stoppen
     */
    fun stopBluetoothScan() {
        bluetoothScanner.stopScan()
    }
    
    /**
     * Mit Gerät verbinden
     */
    fun connectToDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            try {
                _bluetoothState.value = _bluetoothState.value.copy(
                    connectionState = ConnectionState.CONNECTING
                )
                
                // Hier würde die tatsächliche Verbindung hergestellt
                // bluetoothManager.connectToEMFADDevice(device.toAndroidBluetoothDevice())
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Verbindungsfehler: ${e.message}"
                )
                _bluetoothState.value = _bluetoothState.value.copy(
                    connectionState = ConnectionState.DISCONNECTED
                )
            }
        }
    }
    
    /**
     * Verbindung trennen
     */
    fun disconnectDevice() {
        viewModelScope.launch {
            try {
                // bluetoothManager.disconnect()
                _bluetoothState.value = _bluetoothState.value.copy(
                    connectionState = ConnectionState.DISCONNECTED,
                    isConnected = false
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Fehler beim Trennen: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Session löschen
     */
    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            try {
                database.measurementSessionDao().deleteById(sessionId)
                loadRecentSessions()
                calculateDashboardStatistics()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Fehler beim Löschen der Session: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Session exportieren
     */
    fun exportSession(sessionId: Long) {
        viewModelScope.launch {
            try {
                // Export-Logik würde hier implementiert
                _uiState.value = _uiState.value.copy(
                    message = "Session wird exportiert..."
                )
                
                // Placeholder für Export-Funktionalität
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Export-Fehler: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Daten aktualisieren
     */
    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            
            try {
                loadDashboardData()
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    message = "Daten aktualisiert"
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = "Aktualisierungsfehler: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Fehler löschen
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Nachricht löschen
     */
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
    
    /**
     * Measurement Service verbinden
     */
    fun bindMeasurementService(service: MeasurementService) {
        measurementService = service
        
        viewModelScope.launch {
            // Service-Status überwachen
            service.serviceState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    measurementServiceState = state
                )
            }
        }
    }
    
    /**
     * Measurement Service trennen
     */
    fun unbindMeasurementService() {
        measurementService = null
    }
}

/**
 * Dashboard UI State
 */
data class DashboardUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val measurementServiceState: com.emfad.app.services.measurement.MeasurementServiceState? = null
)

/**
 * Bluetooth State
 */
data class BluetoothState(
    val hasPermissions: Boolean = true,
    val permissionError: String? = null,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val isConnected: Boolean = false,
    val isScanning: Boolean = false,
    val availableDevices: List<BluetoothDevice> = emptyList(),
    val deviceInfo: Map<String, Any> = emptyMap(),
    val batteryLevel: Int = 0
)

/**
 * Dashboard Statistiken
 */
data class DashboardStatistics(
    val totalSessions: Int = 0,
    val totalMeasurements: Int = 0,
    val totalAnalyses: Int = 0,
    val activeSessions: Int = 0,
    val completedSessions: Int = 0,
    val averageMeasurementsPerSession: Double = 0.0
)
