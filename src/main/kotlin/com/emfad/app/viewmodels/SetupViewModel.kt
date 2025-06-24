package com.emfad.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emfad.app.services.frequency.EMFADFrequency
import com.emfad.app.services.frequency.FrequencyConfig
import com.emfad.app.services.frequency.FrequencyManager
import com.emfad.app.services.frequency.FrequencyMode
import com.emfad.app.services.frequency.ScanPattern
import com.emfad.app.ui.screens.EMFADMeasurementMode
import com.emfad.app.ui.screens.EMFADScanPattern
import com.emfad.app.ui.screens.EMFADSetupConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * EMFAD® Setup ViewModel
 * Verbindet SetupScreen mit FrequencyManager und anderen Services
 * Implementiert MVVM-Pattern für Konfiguration
 */

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val frequencyManager: FrequencyManager
) : ViewModel() {
    
    // UI State
    private val _setupConfig = MutableStateFlow(
        EMFADSetupConfig(
            selectedFrequency = EMFADFrequency.EMFAD_FREQUENCIES[0],
            measurementMode = EMFADMeasurementMode.A,
            scanPattern = EMFADScanPattern.PARALLEL,
            gainValue = 1.0f,
            offsetValue = 0.0f,
            autoInterval = 5,
            isAutoModeEnabled = false
        )
    )
    val setupConfig: StateFlow<EMFADSetupConfig> = _setupConfig.asStateFlow()
    
    private val _availableFrequencies = MutableStateFlow(EMFADFrequency.EMFAD_FREQUENCIES)
    val availableFrequencies: StateFlow<List<EMFADFrequency>> = _availableFrequencies.asStateFlow()
    
    private val _isConfigSaved = MutableStateFlow(false)
    val isConfigSaved: StateFlow<Boolean> = _isConfigSaved.asStateFlow()
    
    init {
        // Backend-Services überwachen
        observeBackendServices()
    }
    
    private fun observeBackendServices() {
        viewModelScope.launch {
            // Frequenz-Konfiguration vom FrequencyManager
            frequencyManager.frequencyConfig.collect { config ->
                updateFromFrequencyConfig(config)
            }
        }
        
        viewModelScope.launch {
            // Verfügbare Frequenzen
            frequencyManager.availableFrequencies.collect { frequencies ->
                _availableFrequencies.value = frequencies
            }
        }
    }
    
    /**
     * Aktualisiert Setup-Konfiguration
     */
    fun updateSetupConfig(config: EMFADSetupConfig) {
        viewModelScope.launch {
            _setupConfig.value = config
            _isConfigSaved.value = false
            
            // Frequenz-Manager aktualisieren
            val frequencyConfig = FrequencyConfig(
                mode = if (config.isAutoModeEnabled) FrequencyMode.AUTO_SCAN else FrequencyMode.SINGLE,
                selectedFrequency = config.selectedFrequency,
                scanPattern = mapScanPattern(config.scanPattern),
                scanInterval = config.autoInterval * 1000L,
                autoScanEnabled = config.isAutoModeEnabled
            )
            
            frequencyManager.updateFrequencyConfig(frequencyConfig)
        }
    }
    
    /**
     * Speichert Konfiguration
     */
    fun saveConfiguration() {
        viewModelScope.launch {
            try {
                // Konfiguration in Preferences speichern
                // TODO: Implementiere SharedPreferences oder DataStore
                
                _isConfigSaved.value = true
                
            } catch (e: Exception) {
                // Error handling
            }
        }
    }
    
    /**
     * Lädt Konfiguration
     */
    fun loadConfiguration() {
        viewModelScope.launch {
            try {
                // Konfiguration aus Preferences laden
                // TODO: Implementiere SharedPreferences oder DataStore
                
            } catch (e: Exception) {
                // Error handling
            }
        }
    }
    
    /**
     * Setzt Konfiguration zurück
     */
    fun resetConfiguration() {
        viewModelScope.launch {
            val defaultConfig = EMFADSetupConfig(
                selectedFrequency = EMFADFrequency.EMFAD_FREQUENCIES[0],
                measurementMode = EMFADMeasurementMode.A,
                scanPattern = EMFADScanPattern.PARALLEL,
                gainValue = 1.0f,
                offsetValue = 0.0f,
                autoInterval = 5,
                isAutoModeEnabled = false
            )
            
            _setupConfig.value = defaultConfig
            _isConfigSaved.value = false
        }
    }
    
    /**
     * Wählt eine Frequenz aus
     */
    fun selectFrequency(frequency: EMFADFrequency) {
        viewModelScope.launch {
            val updatedConfig = _setupConfig.value.copy(selectedFrequency = frequency)
            updateSetupConfig(updatedConfig)
            
            // Frequenz sofort setzen
            frequencyManager.setFrequency(frequency)
        }
    }
    
    /**
     * Ändert Messmodus
     */
    fun changeMeasurementMode(mode: EMFADMeasurementMode) {
        val updatedConfig = _setupConfig.value.copy(measurementMode = mode)
        updateSetupConfig(updatedConfig)
    }
    
    /**
     * Ändert Gain-Wert
     */
    fun changeGainValue(gain: Float) {
        val updatedConfig = _setupConfig.value.copy(gainValue = gain)
        updateSetupConfig(updatedConfig)
    }
    
    /**
     * Ändert Offset-Wert
     */
    fun changeOffsetValue(offset: Float) {
        val updatedConfig = _setupConfig.value.copy(offsetValue = offset)
        updateSetupConfig(updatedConfig)
    }
    
    /**
     * Ändert Scan-Pattern
     */
    fun changeScanPattern(pattern: EMFADScanPattern) {
        val updatedConfig = _setupConfig.value.copy(scanPattern = pattern)
        updateSetupConfig(updatedConfig)
    }
    
    /**
     * Ändert Auto-Intervall
     */
    fun changeAutoInterval(interval: Int) {
        val updatedConfig = _setupConfig.value.copy(autoInterval = interval)
        updateSetupConfig(updatedConfig)
    }
    
    /**
     * Schaltet Auto-Modus um
     */
    fun toggleAutoMode(enabled: Boolean) {
        val updatedConfig = _setupConfig.value.copy(isAutoModeEnabled = enabled)
        updateSetupConfig(updatedConfig)
    }
    
    /**
     * Konvertiert UI-ScanPattern zu Service-ScanPattern
     */
    private fun mapScanPattern(uiPattern: EMFADScanPattern): ScanPattern {
        return when (uiPattern) {
            EMFADScanPattern.PARALLEL -> ScanPattern.SEQUENTIAL
            EMFADScanPattern.MEANDER -> ScanPattern.OPTIMIZED
            EMFADScanPattern.HORIZONTAL -> ScanPattern.SEQUENTIAL
            EMFADScanPattern.VERTICAL -> ScanPattern.REVERSE
        }
    }
    
    /**
     * Aktualisiert UI-Config basierend auf FrequencyManager-Config
     */
    private fun updateFromFrequencyConfig(config: FrequencyConfig) {
        val currentConfig = _setupConfig.value
        
        val updatedConfig = currentConfig.copy(
            selectedFrequency = config.selectedFrequency,
            isAutoModeEnabled = config.autoScanEnabled,
            autoInterval = (config.scanInterval / 1000L).toInt()
        )
        
        if (updatedConfig != currentConfig) {
            _setupConfig.value = updatedConfig
        }
    }
    
    /**
     * Exportiert aktuelle Konfiguration als String
     */
    fun exportConfiguration(): String {
        val config = _setupConfig.value
        return buildString {
            appendLine("# EMFAD Setup Configuration")
            appendLine("# Generated: ${java.util.Date()}")
            appendLine()
            appendLine("Selected Frequency: ${config.selectedFrequency.label}")
            appendLine("Measurement Mode: ${config.measurementMode.displayName}")
            appendLine("Scan Pattern: ${config.scanPattern.displayName}")
            appendLine("Gain: ${config.gainValue}")
            appendLine("Offset: ${config.offsetValue}")
            appendLine("Auto Interval: ${config.autoInterval}s")
            appendLine("Auto Mode: ${config.isAutoModeEnabled}")
        }
    }
}
