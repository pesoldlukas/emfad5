package com.emfad.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emfad.app.services.calibration.AutoBalanceData
import com.emfad.app.services.calibration.AutoBalanceService
import com.emfad.app.services.calibration.CalibrationStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * EMFAD® AutoBalance ViewModel
 * Verbindet AutoBalanceScreen mit AutoBalanceService
 * Implementiert MVVM-Pattern für Kalibrierung
 */

@HiltViewModel
class AutoBalanceViewModel @Inject constructor(
    private val autoBalanceService: AutoBalanceService
) : ViewModel() {
    
    // UI State direkt von Service
    val autoBalanceData: StateFlow<AutoBalanceData> = autoBalanceService.autoBalanceData
    val isCalibrating: StateFlow<Boolean> = autoBalanceService.isCalibrating
    val calibrationProgress: StateFlow<Int> = autoBalanceService.calibrationProgress
    
    // Zusätzliche UI State
    private val _calibrationMessage = MutableStateFlow("")
    val calibrationMessage: StateFlow<String> = _calibrationMessage.asStateFlow()
    
    private val _showCalibrationDialog = MutableStateFlow(false)
    val showCalibrationDialog: StateFlow<Boolean> = _showCalibrationDialog.asStateFlow()
    
    init {
        observeCalibrationStatus()
    }
    
    private fun observeCalibrationStatus() {
        viewModelScope.launch {
            autoBalanceData.collect { data ->
                updateCalibrationMessage(data)
            }
        }
    }
    
    /**
     * Startet Kompass-Kalibrierung
     */
    fun startCompassCalibration() {
        viewModelScope.launch {
            try {
                _calibrationMessage.value = "Kompass-Kalibrierung wird gestartet..."
                autoBalanceService.startCompassCalibration()
                
            } catch (e: Exception) {
                _calibrationMessage.value = "Fehler beim Starten der Kompass-Kalibrierung"
            }
        }
    }
    
    /**
     * Startet horizontale Kalibrierung
     */
    fun startHorizontalCalibration() {
        viewModelScope.launch {
            try {
                _calibrationMessage.value = "Horizontale Kalibrierung wird gestartet..."
                autoBalanceService.startHorizontalCalibration()
                
            } catch (e: Exception) {
                _calibrationMessage.value = "Fehler beim Starten der horizontalen Kalibrierung"
            }
        }
    }
    
    /**
     * Startet vertikale Kalibrierung
     */
    fun startVerticalCalibration() {
        viewModelScope.launch {
            try {
                _calibrationMessage.value = "Vertikale Kalibrierung wird gestartet..."
                autoBalanceService.startVerticalCalibration()
                
            } catch (e: Exception) {
                _calibrationMessage.value = "Fehler beim Starten der vertikalen Kalibrierung"
            }
        }
    }
    
    /**
     * Speichert Kalibrierung
     */
    fun saveCalibration() {
        viewModelScope.launch {
            try {
                _calibrationMessage.value = "Kalibrierung wird gespeichert..."
                autoBalanceService.saveCalibration()
                _calibrationMessage.value = "Kalibrierung erfolgreich gespeichert"
                
            } catch (e: Exception) {
                _calibrationMessage.value = "Fehler beim Speichern der Kalibrierung"
            }
        }
    }
    
    /**
     * Lädt Kalibrierung
     */
    fun loadCalibration() {
        viewModelScope.launch {
            try {
                _calibrationMessage.value = "Kalibrierung wird geladen..."
                autoBalanceService.loadCalibration()
                _calibrationMessage.value = "Kalibrierung erfolgreich geladen"
                
            } catch (e: Exception) {
                _calibrationMessage.value = "Fehler beim Laden der Kalibrierung"
            }
        }
    }
    
    /**
     * Löscht alle Kalibrierungen
     */
    fun deleteAllCalibration() {
        viewModelScope.launch {
            try {
                _showCalibrationDialog.value = true
                
            } catch (e: Exception) {
                _calibrationMessage.value = "Fehler beim Löschen der Kalibrierung"
            }
        }
    }
    
    /**
     * Bestätigt das Löschen aller Kalibrierungen
     */
    fun confirmDeleteAllCalibration() {
        viewModelScope.launch {
            try {
                _calibrationMessage.value = "Alle Kalibrierungen werden gelöscht..."
                autoBalanceService.deleteAllCalibration()
                _calibrationMessage.value = "Alle Kalibrierungen erfolgreich gelöscht"
                _showCalibrationDialog.value = false
                
            } catch (e: Exception) {
                _calibrationMessage.value = "Fehler beim Löschen der Kalibrierungen"
                _showCalibrationDialog.value = false
            }
        }
    }
    
    /**
     * Bricht das Löschen ab
     */
    fun cancelDeleteCalibration() {
        _showCalibrationDialog.value = false
    }
    
    /**
     * Stoppt aktuelle Kalibrierung
     */
    fun stopCalibration() {
        viewModelScope.launch {
            try {
                autoBalanceService.stopCalibration()
                _calibrationMessage.value = "Kalibrierung gestoppt"
                
            } catch (e: Exception) {
                _calibrationMessage.value = "Fehler beim Stoppen der Kalibrierung"
            }
        }
    }
    
    /**
     * Aktualisiert Kalibrierungs-Nachricht basierend auf Status
     */
    private fun updateCalibrationMessage(data: AutoBalanceData) {
        val message = when {
            data.compassCalibrationStatus == CalibrationStatus.STARTED -> 
                "Bewegen Sie das Gerät in einer 8-Form für die Kompass-Kalibrierung"
            
            data.compassCalibrationStatus == CalibrationStatus.COLLECTING_HORIZONTAL -> 
                "Sammle horizontale Kalibrierungsdaten... ${calibrationProgress.value}%"
            
            data.compassCalibrationStatus == CalibrationStatus.COLLECTING_VERTICAL -> 
                "Sammle vertikale Kalibrierungsdaten... ${calibrationProgress.value}%"
            
            data.compassCalibrationStatus == CalibrationStatus.COMPASS_FINISHED -> 
                "Kompass-Kalibrierung erfolgreich abgeschlossen"
            
            data.horizontalCalibrationStatus == CalibrationStatus.HORIZONTAL_FINISHED -> 
                "Horizontale Kalibrierung erfolgreich abgeschlossen"
            
            data.verticalCalibrationStatus == CalibrationStatus.VERTICAL_FINISHED -> 
                "Vertikale Kalibrierung erfolgreich abgeschlossen"
            
            data.compassCalibrationStatus == CalibrationStatus.SAVED -> 
                "Kalibrierung gespeichert"
            
            else -> ""
        }
        
        if (message.isNotEmpty()) {
            _calibrationMessage.value = message
        }
    }
    
    /**
     * Prüft ob alle Kalibrierungen abgeschlossen sind
     */
    fun areAllCalibrationsComplete(): Boolean {
        val data = autoBalanceData.value
        return data.compassCalibrationStatus == CalibrationStatus.COMPASS_FINISHED &&
               data.horizontalCalibrationStatus == CalibrationStatus.HORIZONTAL_FINISHED &&
               data.verticalCalibrationStatus == CalibrationStatus.VERTICAL_FINISHED
    }
    
    /**
     * Exportiert Kalibrierungsdaten
     */
    fun exportCalibrationData(): String {
        val data = autoBalanceData.value
        return buildString {
            appendLine("# EMFAD AutoBalance Export")
            appendLine("# ${data.version}")
            appendLine("# Generated: ${java.util.Date()}")
            appendLine()
            appendLine("Compass Status: ${data.compassCalibrationStatus.message}")
            appendLine("Horizontal Status: ${data.horizontalCalibrationStatus.message}")
            appendLine("Vertical Status: ${data.verticalCalibrationStatus.message}")
            appendLine()
            appendLine("Compass Heading: ${data.compassHeading}°")
            appendLine("Compass Accuracy: ${(data.compassAccuracy * 100).toInt()}%")
            appendLine("Magnetic Declination: ${data.magneticDeclination}°")
            appendLine()
            appendLine("Horizontal Offset X: ${data.horizontalOffsetX}")
            appendLine("Horizontal Offset Y: ${data.horizontalOffsetY}")
            appendLine("Horizontal Scale X: ${data.horizontalScaleX}")
            appendLine("Horizontal Scale Y: ${data.horizontalScaleY}")
            appendLine()
            appendLine("Vertical Offset Z: ${data.verticalOffsetZ}")
            appendLine("Vertical Scale Z: ${data.verticalScaleZ}")
            appendLine()
            if (data.lastCalibrationTime > 0) {
                appendLine("Last Calibration: ${java.util.Date(data.lastCalibrationTime)}")
            }
            appendLine("Calibration Points: ${data.calibrationDataPoints.size}")
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Cleanup wenn ViewModel zerstört wird
        autoBalanceService.stopCalibration()
    }
}
