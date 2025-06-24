package com.emfad.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emfad.app.services.analysis.EMFReading
import com.emfad.app.services.analysis.SignalAnalyzer
import com.emfad.app.services.communication.DeviceCommunicationService
import com.emfad.app.services.communication.EMFADCommand
import com.emfad.app.services.frequency.FrequencyManager
import com.emfad.app.ui.screens.MeasurementMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * EMFAD® Measurement Recorder ViewModel
 * Verbindet MeasurementRecorderScreen mit Backend-Services
 * Implementiert MVVM-Pattern für Live-Messungen
 */

@HiltViewModel
class MeasurementRecorderViewModel @Inject constructor(
    private val deviceCommunicationService: DeviceCommunicationService,
    private val frequencyManager: FrequencyManager,
    private val signalAnalyzer: SignalAnalyzer
) : ViewModel() {
    
    // UI State
    private val _currentMeasurement = MutableStateFlow<EMFReading?>(null)
    val currentMeasurement: StateFlow<EMFReading?> = _currentMeasurement.asStateFlow()
    
    private val _measurementHistory = MutableStateFlow<List<EMFReading>>(emptyList())
    val measurementHistory: StateFlow<List<EMFReading>> = _measurementHistory.asStateFlow()
    
    private val _measurementMode = MutableStateFlow(MeasurementMode.STEP)
    val measurementMode: StateFlow<MeasurementMode> = _measurementMode.asStateFlow()
    
    private val _isDeviceConnected = MutableStateFlow(false)
    val isDeviceConnected: StateFlow<Boolean> = _isDeviceConnected.asStateFlow()
    
    private val _selectedFrequency = MutableStateFlow(19000.0)
    val selectedFrequency: StateFlow<Double> = _selectedFrequency.asStateFlow()
    
    private val _sessionId = MutableStateFlow(System.currentTimeMillis())
    val sessionId: StateFlow<Long> = _sessionId.asStateFlow()
    
    init {
        // Backend-Services überwachen
        observeBackendServices()
    }
    
    private fun observeBackendServices() {
        viewModelScope.launch {
            // Geräte-Verbindungsstatus
            deviceCommunicationService.connectionStatus.collect { status ->
                _isDeviceConnected.value = status == com.emfad.app.services.communication.ConnectionStatus.CONNECTED
            }
        }
        
        viewModelScope.launch {
            // Aktuelle Frequenz
            frequencyManager.currentFrequency.collect { frequency ->
                _selectedFrequency.value = frequency.value
            }
        }
        
        viewModelScope.launch {
            // Verarbeitete Messdaten
            signalAnalyzer.processedReadings.collect { reading ->
                _currentMeasurement.value = reading
                
                // Zur Historie hinzufügen
                val currentHistory = _measurementHistory.value.toMutableList()
                currentHistory.add(reading)
                
                // Maximal 1000 Messungen in Historie behalten
                if (currentHistory.size > 1000) {
                    currentHistory.removeAt(0)
                }
                
                _measurementHistory.value = currentHistory
            }
        }
    }
    
    /**
     * Ändert den Messmodus
     */
    fun changeMeasurementMode(mode: MeasurementMode) {
        viewModelScope.launch {
            _measurementMode.value = mode
            
            when (mode) {
                MeasurementMode.AUTO -> startAutoMeasurement()
                MeasurementMode.PAUSE -> pauseMeasurement()
                MeasurementMode.STEP -> stopAutoMeasurement()
            }
        }
    }
    
    /**
     * Führt eine Einzelmessung durch (Step-Modus)
     */
    fun performStepMeasurement() {
        if (_measurementMode.value != MeasurementMode.STEP) return
        
        viewModelScope.launch {
            try {
                val command = EMFADCommand(
                    command = "START_MEASUREMENT",
                    frequency = _selectedFrequency.value,
                    mode = "SINGLE"
                )
                
                deviceCommunicationService.sendCommand(command)
                
            } catch (e: Exception) {
                // Error handling
            }
        }
    }
    
    /**
     * Startet automatische Messungen
     */
    private fun startAutoMeasurement() {
        viewModelScope.launch {
            try {
                val command = EMFADCommand(
                    command = "START_MEASUREMENT",
                    frequency = _selectedFrequency.value,
                    mode = "CONTINUOUS"
                )
                
                deviceCommunicationService.sendCommand(command)
                
            } catch (e: Exception) {
                // Error handling
            }
        }
    }
    
    /**
     * Pausiert Messungen
     */
    private fun pauseMeasurement() {
        viewModelScope.launch {
            try {
                val command = EMFADCommand(
                    command = "STOP_MEASUREMENT"
                )
                
                deviceCommunicationService.sendCommand(command)
                
            } catch (e: Exception) {
                // Error handling
            }
        }
    }
    
    /**
     * Stoppt automatische Messungen
     */
    private fun stopAutoMeasurement() {
        viewModelScope.launch {
            try {
                val command = EMFADCommand(
                    command = "STOP_MEASUREMENT"
                )
                
                deviceCommunicationService.sendCommand(command)
                
            } catch (e: Exception) {
                // Error handling
            }
        }
    }
    
    /**
     * Speichert aktuelle Messung
     */
    fun saveMeasurement() {
        viewModelScope.launch {
            // TODO: Implementiere Speicherung in Database
        }
    }
    
    /**
     * Startet neue Mess-Session
     */
    fun startNewSession() {
        viewModelScope.launch {
            _sessionId.value = System.currentTimeMillis()
            _measurementHistory.value = emptyList()
            _currentMeasurement.value = null
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Cleanup wenn ViewModel zerstört wird
        stopAutoMeasurement()
    }
}
