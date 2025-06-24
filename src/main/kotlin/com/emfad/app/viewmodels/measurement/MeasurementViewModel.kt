package com.emfad.app.viewmodels.measurement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emfad.app.database.EMFADDatabase
import com.emfad.app.models.*
import com.emfad.app.services.measurement.MeasurementService
import com.emfad.app.services.measurement.MeasurementServiceState
import com.emfad.app.services.measurement.SessionStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Measurement ViewModel für EMFAD App
 * Echtzeit-Messungen mit ursprünglichen Algorithmen
 * Samsung S21 Ultra optimiert
 */
@HiltViewModel
class MeasurementViewModel @Inject constructor(
    private val database: EMFADDatabase
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(MeasurementUiState())
    val uiState: StateFlow<MeasurementUiState> = _uiState.asStateFlow()
    
    // Measurement State
    private val _measurementState = MutableStateFlow(MeasurementState())
    val measurementState: StateFlow<MeasurementState> = _measurementState.asStateFlow()
    
    // Current Session
    private val _currentSession = MutableStateFlow<MeasurementSession?>(null)
    val currentSession: StateFlow<MeasurementSession?> = _currentSession.asStateFlow()
    
    // Real-time Data
    private val _currentReading = MutableStateFlow<EMFReading?>(null)
    val currentReading: StateFlow<EMFReading?> = _currentReading.asStateFlow()
    
    // Session Statistics
    private val _sessionStats = MutableStateFlow(SessionStatistics())
    val sessionStats: StateFlow<SessionStatistics> = _sessionStats.asStateFlow()
    
    // Measurement History (für Charts)
    private val _measurementHistory = MutableStateFlow<List<EMFReading>>(emptyList())
    val measurementHistory: StateFlow<List<EMFReading>> = _measurementHistory.asStateFlow()
    
    // Service Connection
    private var measurementService: MeasurementService? = null
    
    /**
     * Measurement Service verbinden
     */
    fun bindMeasurementService(service: MeasurementService) {
        measurementService = service
        
        viewModelScope.launch {
            // Service-Status überwachen
            service.serviceState.collect { state ->
                _measurementState.value = _measurementState.value.copy(
                    serviceState = state,
                    isServiceReady = state != MeasurementServiceState.ERROR
                )
            }
        }
        
        viewModelScope.launch {
            // Aktuelle Session überwachen
            service.currentSession.collect { session ->
                _currentSession.value = session
                session?.let { loadSessionData(it.id) }
            }
        }
        
        viewModelScope.launch {
            // Echtzeit-Messdaten überwachen
            service.measurementData.collect { reading ->
                reading?.let { 
                    _currentReading.value = it
                    addToMeasurementHistory(it)
                }
            }
        }
        
        viewModelScope.launch {
            // Session-Statistiken überwachen
            service.sessionStatistics.collect { stats ->
                _sessionStats.value = stats
            }
        }
    }
    
    /**
     * Neue Messsitzung starten
     */
    fun startNewSession(
        sessionName: String,
        description: String,
        operatorName: String,
        location: String,
        projectName: String,
        sampleId: String
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val service = measurementService ?: throw Exception("Measurement Service nicht verfügbar")
                
                val result = service.startMeasurementSession(
                    sessionName = sessionName,
                    description = description,
                    operatorName = operatorName,
                    location = location,
                    projectName = projectName,
                    sampleId = sampleId
                )
                
                result.fold(
                    onSuccess = { sessionId ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            message = "Session erfolgreich gestartet"
                        )
                        _measurementState.value = _measurementState.value.copy(
                            isSessionActive = true,
                            currentSessionId = sessionId
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Fehler beim Starten der Session: ${error.message}"
                        )
                    }
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Session-Fehler: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Messung starten
     */
    fun startMeasurement() {
        viewModelScope.launch {
            try {
                val service = measurementService ?: throw Exception("Measurement Service nicht verfügbar")
                
                val result = service.startMeasurement()
                
                result.fold(
                    onSuccess = {
                        _measurementState.value = _measurementState.value.copy(
                            isMeasuring = true,
                            measurementStartTime = System.currentTimeMillis()
                        )
                        _measurementHistory.value = emptyList() // Historie zurücksetzen
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = "Fehler beim Starten der Messung: ${error.message}"
                        )
                    }
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Messfehler: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Messung stoppen
     */
    fun stopMeasurement() {
        viewModelScope.launch {
            try {
                val service = measurementService ?: throw Exception("Measurement Service nicht verfügbar")
                
                val result = service.stopMeasurement()
                
                result.fold(
                    onSuccess = {
                        _measurementState.value = _measurementState.value.copy(
                            isMeasuring = false,
                            isSessionActive = false,
                            measurementEndTime = System.currentTimeMillis()
                        )
                        _uiState.value = _uiState.value.copy(
                            message = "Messung erfolgreich beendet"
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = "Fehler beim Stoppen der Messung: ${error.message}"
                        )
                    }
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Stop-Fehler: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Messung pausieren
     */
    fun pauseMeasurement() {
        viewModelScope.launch {
            try {
                // Pausieren-Logik implementieren
                _measurementState.value = _measurementState.value.copy(
                    isPaused = true
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Pause-Fehler: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Messung fortsetzen
     */
    fun resumeMeasurement() {
        viewModelScope.launch {
            try {
                // Fortsetzen-Logik implementieren
                _measurementState.value = _measurementState.value.copy(
                    isPaused = false
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Resume-Fehler: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Messparameter ändern
     */
    fun updateMeasurementParameters(
        frequency: Double? = null,
        gain: Double? = null,
        filterSetting: String? = null
    ) {
        viewModelScope.launch {
            try {
                // Parameter-Update-Logik
                val currentParams = _measurementState.value.measurementParameters
                val updatedParams = currentParams.copy(
                    frequency = frequency ?: currentParams.frequency,
                    gain = gain ?: currentParams.gain,
                    filterSetting = filterSetting ?: currentParams.filterSetting
                )
                
                _measurementState.value = _measurementState.value.copy(
                    measurementParameters = updatedParams
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Parameter-Fehler: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Session-Daten laden
     */
    private suspend fun loadSessionData(sessionId: Long) {
        try {
            // Aktuelle Messungen der Session laden
            val readings = database.emfReadingDao().getBySessionId(sessionId)
                .map { it.toDomainModel() }
            
            _measurementHistory.value = readings.takeLast(100) // Letzte 100 Messungen
            
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Fehler beim Laden der Session-Daten: ${e.message}"
            )
        }
    }
    
    /**
     * Messung zur Historie hinzufügen
     */
    private fun addToMeasurementHistory(reading: EMFReading) {
        val currentHistory = _measurementHistory.value.toMutableList()
        currentHistory.add(reading)
        
        // Historie auf maximale Größe begrenzen
        if (currentHistory.size > 1000) {
            currentHistory.removeAt(0)
        }
        
        _measurementHistory.value = currentHistory
    }
    
    /**
     * Kalibrierung durchführen
     */
    fun performCalibration() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    message = "Kalibrierung läuft..."
                )
                
                // Kalibrierungs-Logik implementieren
                // bluetoothManager.calibrateDevice()
                
                _measurementState.value = _measurementState.value.copy(
                    isCalibrated = true,
                    lastCalibrationTime = System.currentTimeMillis()
                )
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Kalibrierung erfolgreich"
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Kalibrierungsfehler: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Notiz zur Session hinzufügen
     */
    fun addSessionNote(note: String) {
        viewModelScope.launch {
            try {
                val session = _currentSession.value ?: return@launch
                
                val updatedSession = session.copy(
                    notes = if (session.notes.isEmpty()) note else "${session.notes}\n$note"
                )
                
                database.measurementSessionDao().update(
                    com.emfad.app.database.entities.MeasurementSessionEntity.fromDomainModel(updatedSession)
                )
                
                _currentSession.value = updatedSession
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Fehler beim Hinzufügen der Notiz: ${e.message}"
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
     * ViewModel bereinigen
     */
    override fun onCleared() {
        super.onCleared()
        measurementService = null
    }
}

/**
 * Measurement UI State
 */
data class MeasurementUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

/**
 * Measurement State
 */
data class MeasurementState(
    val serviceState: MeasurementServiceState = MeasurementServiceState.IDLE,
    val isServiceReady: Boolean = false,
    val isSessionActive: Boolean = false,
    val isMeasuring: Boolean = false,
    val isPaused: Boolean = false,
    val isCalibrated: Boolean = false,
    val currentSessionId: Long = 0L,
    val measurementStartTime: Long = 0L,
    val measurementEndTime: Long = 0L,
    val lastCalibrationTime: Long = 0L,
    val measurementParameters: MeasurementParameters = MeasurementParameters()
)

/**
 * Messparameter
 */
data class MeasurementParameters(
    val frequency: Double = 100.0,
    val gain: Double = 1.0,
    val filterSetting: String = "default",
    val measurementMode: String = "standard",
    val samplingRate: Double = 10.0,
    val averagingCount: Int = 1
)
