package com.emfad.app.services.frequency

import android.util.Log
import com.emfad.app.services.communication.DeviceCommunicationService
import com.emfad.app.services.communication.EMFADCommand
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EMFAD® Frequency Manager
 * Basiert auf Frequenzsteuerung der originalen Windows-Software
 * Implementiert 7 EMFAD-Frequenzen und Auto-Scan-Modus
 * Rekonstruiert aus EMFAD3EXE.c und TfrmFrequencyModeSelect
 */

@Serializable
data class EMFADFrequency(
    val index: Int,
    val value: Double, // in Hz
    val label: String,
    val isActive: Boolean = true,
    val description: String = ""
) {
    companion object {
        // 7 EMFAD-Frequenzen aus originaler Software
        val EMFAD_FREQUENCIES = listOf(
            EMFADFrequency(0, 19000.0, "f0 - 19.0 kHz", true, "Niedrigste Frequenz"),
            EMFADFrequency(1, 23400.0, "f1 - 23.4 kHz", true, "Niedrige Frequenz"),
            EMFADFrequency(2, 70000.0, "f2 - 70.0 kHz", true, "Mittlere Frequenz 1"),
            EMFADFrequency(3, 77500.0, "f3 - 77.5 kHz", true, "Mittlere Frequenz 2"),
            EMFADFrequency(4, 124000.0, "f4 - 124.0 kHz", true, "Hohe Frequenz 1"),
            EMFADFrequency(5, 129100.0, "f5 - 129.1 kHz", true, "Hohe Frequenz 2"),
            EMFADFrequency(6, 135600.0, "f6 - 135.6 kHz", true, "Höchste Frequenz")
        )
    }
}

enum class FrequencyMode {
    SINGLE,      // Einzelfrequenz
    AUTO_SCAN,   // Automatischer Frequenz-Scan
    CUSTOM       // Benutzerdefinierte Auswahl
}

enum class ScanPattern {
    SEQUENTIAL,  // f0 -> f1 -> f2 -> ... -> f6
    OPTIMIZED,   // Optimierte Reihenfolge für beste Ergebnisse
    REVERSE,     // f6 -> f5 -> f4 -> ... -> f0
    RANDOM       // Zufällige Reihenfolge
}

@Serializable
data class FrequencyConfig(
    val mode: FrequencyMode = FrequencyMode.SINGLE,
    val selectedFrequency: EMFADFrequency = EMFADFrequency.EMFAD_FREQUENCIES[0],
    val activeFrequencies: List<Int> = listOf(0, 1, 2, 3, 4, 5, 6),
    val scanPattern: ScanPattern = ScanPattern.SEQUENTIAL,
    val scanInterval: Long = 1000L, // ms zwischen Frequenzwechseln
    val autoScanEnabled: Boolean = false
)

@Singleton
class FrequencyManager @Inject constructor(
    private val deviceCommunicationService: DeviceCommunicationService
) {
    companion object {
        private const val TAG = "EMFADFrequencyManager"
    }
    
    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // State Management
    private val _currentFrequency = MutableStateFlow(EMFADFrequency.EMFAD_FREQUENCIES[0])
    val currentFrequency: StateFlow<EMFADFrequency> = _currentFrequency.asStateFlow()
    
    private val _frequencyConfig = MutableStateFlow(FrequencyConfig())
    val frequencyConfig: StateFlow<FrequencyConfig> = _frequencyConfig.asStateFlow()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    private val _scanProgress = MutableStateFlow(0)
    val scanProgress: StateFlow<Int> = _scanProgress.asStateFlow()
    
    private val _availableFrequencies = MutableStateFlow(EMFADFrequency.EMFAD_FREQUENCIES)
    val availableFrequencies: StateFlow<List<EMFADFrequency>> = _availableFrequencies.asStateFlow()
    
    // Auto-Scan Job
    private var autoScanJob: Job? = null
    
    /**
     * Setzt eine einzelne Frequenz
     */
    suspend fun setFrequency(frequency: EMFADFrequency): Boolean {
        return try {
            Log.d(TAG, "Setting frequency to ${frequency.label}")
            
            // Kommando an Gerät senden
            val command = EMFADCommand(
                command = "SET_FREQUENCY",
                frequency = frequency.value,
                parameters = mapOf(
                    "index" to frequency.index.toString(),
                    "label" to frequency.label
                )
            )
            
            val response = deviceCommunicationService.sendCommand(command)
            
            if (response?.status == "OK") {
                _currentFrequency.value = frequency
                
                // Konfiguration aktualisieren
                _frequencyConfig.value = _frequencyConfig.value.copy(
                    selectedFrequency = frequency,
                    mode = FrequencyMode.SINGLE
                )
                
                Log.d(TAG, "Frequency set successfully: ${frequency.label}")
                true
            } else {
                Log.w(TAG, "Failed to set frequency: ${response?.status}")
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting frequency", e)
            false
        }
    }
    
    /**
     * Startet Auto-Scan über alle aktiven Frequenzen
     */
    fun startAutoScan(config: FrequencyConfig = _frequencyConfig.value) {
        stopAutoScan() // Stoppe vorherigen Scan
        
        _frequencyConfig.value = config.copy(
            mode = FrequencyMode.AUTO_SCAN,
            autoScanEnabled = true
        )
        
        autoScanJob = managerScope.launch {
            try {
                _isScanning.value = true
                Log.d(TAG, "Starting auto scan with pattern: ${config.scanPattern}")
                
                val activeFreqs = getActiveFrequencies(config)
                val scanSequence = generateScanSequence(activeFreqs, config.scanPattern)
                
                while (_isScanning.value && currentCoroutineContext().isActive) {
                    for ((index, frequency) in scanSequence.withIndex()) {
                        if (!_isScanning.value) break
                        
                        // Frequenz setzen
                        setFrequency(frequency)
                        
                        // Progress aktualisieren
                        _scanProgress.value = ((index + 1) * 100) / scanSequence.size
                        
                        // Warten vor nächster Frequenz
                        delay(config.scanInterval)
                    }
                    
                    // Scan-Zyklus abgeschlossen
                    _scanProgress.value = 0
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during auto scan", e)
            } finally {
                _isScanning.value = false
                _scanProgress.value = 0
            }
        }
    }
    
    /**
     * Stoppt Auto-Scan
     */
    fun stopAutoScan() {
        autoScanJob?.cancel()
        autoScanJob = null
        _isScanning.value = false
        _scanProgress.value = 0
        
        _frequencyConfig.value = _frequencyConfig.value.copy(
            autoScanEnabled = false
        )
        
        Log.d(TAG, "Auto scan stopped")
    }
    
    /**
     * Aktualisiert die Frequenz-Konfiguration
     */
    fun updateFrequencyConfig(config: FrequencyConfig) {
        _frequencyConfig.value = config
        
        // Wenn Auto-Scan aktiviert ist, neu starten
        if (config.autoScanEnabled && config.mode == FrequencyMode.AUTO_SCAN) {
            startAutoScan(config)
        } else if (!config.autoScanEnabled) {
            stopAutoScan()
        }
    }
    
    /**
     * Aktiviert/Deaktiviert eine Frequenz
     */
    fun toggleFrequency(frequencyIndex: Int, active: Boolean) {
        val currentConfig = _frequencyConfig.value
        val newActiveFrequencies = currentConfig.activeFrequencies.toMutableList()
        
        if (active && !newActiveFrequencies.contains(frequencyIndex)) {
            newActiveFrequencies.add(frequencyIndex)
        } else if (!active) {
            newActiveFrequencies.remove(frequencyIndex)
        }
        
        _frequencyConfig.value = currentConfig.copy(
            activeFrequencies = newActiveFrequencies.sorted()
        )
        
        Log.d(TAG, "Frequency $frequencyIndex toggled: $active")
    }
    
    /**
     * Nächste Frequenz im Scan
     */
    suspend fun nextFrequency(): Boolean {
        val currentConfig = _frequencyConfig.value
        val activeFreqs = getActiveFrequencies(currentConfig)
        
        if (activeFreqs.isEmpty()) return false
        
        val currentIndex = activeFreqs.indexOfFirst { it.index == _currentFrequency.value.index }
        val nextIndex = (currentIndex + 1) % activeFreqs.size
        
        return setFrequency(activeFreqs[nextIndex])
    }
    
    /**
     * Vorherige Frequenz im Scan
     */
    suspend fun previousFrequency(): Boolean {
        val currentConfig = _frequencyConfig.value
        val activeFreqs = getActiveFrequencies(currentConfig)
        
        if (activeFreqs.isEmpty()) return false
        
        val currentIndex = activeFreqs.indexOfFirst { it.index == _currentFrequency.value.index }
        val prevIndex = if (currentIndex <= 0) activeFreqs.size - 1 else currentIndex - 1
        
        return setFrequency(activeFreqs[prevIndex])
    }
    
    /**
     * Prüft ob eine Frequenz verfügbar ist
     */
    fun isFrequencyAvailable(frequency: EMFADFrequency): Boolean {
        return _availableFrequencies.value.contains(frequency)
    }
    
    /**
     * Optimiert Frequenz-Reihenfolge basierend auf Messbedingungen
     */
    fun optimizeFrequencyOrder(
        targetDepth: Double? = null,
        materialType: String? = null
    ): List<EMFADFrequency> {
        val activeFreqs = getActiveFrequencies(_frequencyConfig.value)
        
        return when {
            targetDepth != null && targetDepth < 1.0 -> {
                // Für geringe Tiefen: höhere Frequenzen bevorzugen
                activeFreqs.sortedByDescending { it.value }
            }
            targetDepth != null && targetDepth > 5.0 -> {
                // Für große Tiefen: niedrigere Frequenzen bevorzugen
                activeFreqs.sortedBy { it.value }
            }
            materialType == "metal" -> {
                // Für Metall: mittlere Frequenzen optimal
                activeFreqs.sortedBy { kotlin.math.abs(it.value - 77500.0) }
            }
            else -> {
                // Standard-Reihenfolge
                activeFreqs.sortedBy { it.index }
            }
        }
    }
    
    private fun getActiveFrequencies(config: FrequencyConfig): List<EMFADFrequency> {
        return EMFADFrequency.EMFAD_FREQUENCIES.filter { freq ->
            config.activeFrequencies.contains(freq.index)
        }
    }
    
    private fun generateScanSequence(
        frequencies: List<EMFADFrequency>,
        pattern: ScanPattern
    ): List<EMFADFrequency> {
        return when (pattern) {
            ScanPattern.SEQUENTIAL -> frequencies.sortedBy { it.index }
            ScanPattern.REVERSE -> frequencies.sortedByDescending { it.index }
            ScanPattern.OPTIMIZED -> optimizeFrequencyOrder()
            ScanPattern.RANDOM -> frequencies.shuffled()
        }
    }
    
    /**
     * Kalibriert Frequenz-spezifische Parameter
     */
    suspend fun calibrateFrequency(frequency: EMFADFrequency): Boolean {
        return try {
            Log.d(TAG, "Calibrating frequency: ${frequency.label}")
            
            val command = EMFADCommand(
                command = "CALIBRATE",
                frequency = frequency.value,
                parameters = mapOf(
                    "type" to "frequency",
                    "index" to frequency.index.toString()
                )
            )
            
            val response = deviceCommunicationService.sendCommand(command)
            response?.status == "OK"
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calibrating frequency", e)
            false
        }
    }
    
    /**
     * Exportiert Frequenz-Konfiguration
     */
    fun exportFrequencyConfig(): String {
        val config = _frequencyConfig.value
        return buildString {
            appendLine("# EMFAD Frequency Configuration")
            appendLine("# Generated: ${System.currentTimeMillis()}")
            appendLine()
            appendLine("Mode: ${config.mode}")
            appendLine("Selected: ${config.selectedFrequency.label}")
            appendLine("Active Frequencies:")
            
            config.activeFrequencies.forEach { index ->
                val freq = EMFADFrequency.EMFAD_FREQUENCIES[index]
                appendLine("  ${freq.index}: ${freq.label} (${freq.value} Hz)")
            }
            
            appendLine()
            appendLine("Scan Pattern: ${config.scanPattern}")
            appendLine("Scan Interval: ${config.scanInterval} ms")
            appendLine("Auto Scan: ${config.autoScanEnabled}")
        }
    }
    
    fun cleanup() {
        stopAutoScan()
        managerScope.cancel()
    }
}
