package com.emfad.app.services.validation

import android.util.Log
import com.emfad.app.models.EMFReading
import com.emfad.app.models.MaterialType
import kotlin.math.*

/**
 * EMFAD Data Validator
 * Datenvalidierung mit ursprünglichen Algorithmen
 * Samsung S21 Ultra optimiert
 */
class DataValidator {
    
    companion object {
        private const val TAG = "DataValidator"
        
        // Validierungsgrenzen
        private const val MIN_SIGNAL_STRENGTH = 0.0
        private const val MAX_SIGNAL_STRENGTH = 2000.0
        private const val MIN_FREQUENCY = 1.0
        private const val MAX_FREQUENCY = 10000.0
        private const val MIN_PHASE = -360.0
        private const val MAX_PHASE = 360.0
        private const val MIN_AMPLITUDE = 0.0
        private const val MAX_AMPLITUDE = 2000.0
        private const val MIN_TEMPERATURE = -40.0
        private const val MAX_TEMPERATURE = 85.0
        private const val MIN_HUMIDITY = 0.0
        private const val MAX_HUMIDITY = 100.0
        private const val MIN_PRESSURE = 800.0
        private const val MAX_PRESSURE = 1200.0
        private const val MIN_DEPTH = 0.0
        private const val MAX_DEPTH = 100.0
        private const val MAX_NOISE_LEVEL = 500.0
        
        // Konsistenz-Prüfungen
        private const val MAX_SIGNAL_CHANGE_RATE = 0.5 // 50% Änderung pro Messung
        private const val MAX_FREQUENCY_DRIFT = 0.1 // 10% Drift
        private const val MAX_PHASE_JUMP = 45.0 // 45° Sprung
        private const val MIN_SNR = 3.0 // Signal-zu-Rausch-Verhältnis
    }
    
    // Validierungshistorie
    private val validationHistory = mutableListOf<ValidationResult>()
    private val maxHistorySize = 100
    
    // Letzte gültige Werte für Konsistenz-Prüfung
    private var lastValidReading: EMFReading? = null
    
    /**
     * EMF-Messung validieren
     */
    fun validateReading(reading: EMFReading): EMFReading? {
        Log.d(TAG, "Validiere Messung: Signal=${reading.signalStrength}, Frequenz=${reading.frequency}")
        
        val validationResult = ValidationResult(
            timestamp = reading.timestamp,
            originalReading = reading
        )
        
        // 1. Grundlegende Bereichsprüfungen
        val rangeValidation = validateRanges(reading)
        validationResult.rangeErrors.addAll(rangeValidation.errors)
        
        // 2. Physikalische Konsistenz prüfen
        val physicsValidation = validatePhysics(reading)
        validationResult.physicsErrors.addAll(physicsValidation.errors)
        
        // 3. Zeitliche Konsistenz prüfen
        val temporalValidation = validateTemporal(reading)
        validationResult.temporalErrors.addAll(temporalValidation.errors)
        
        // 4. Signal-Qualität prüfen
        val qualityValidation = validateQuality(reading)
        validationResult.qualityErrors.addAll(qualityValidation.errors)
        
        // 5. Gesamtbewertung
        val isValid = validationResult.isValid()
        validationResult.isValid = isValid
        
        // Historie aktualisieren
        addToHistory(validationResult)
        
        if (isValid) {
            lastValidReading = reading
            Log.d(TAG, "Messung gültig")
            return reading.copy(isValidated = true)
        } else {
            Log.w(TAG, "Messung ungültig: ${validationResult.getAllErrors()}")
            return null
        }
    }
    
    /**
     * Bereichsprüfungen
     */
    private fun validateRanges(reading: EMFReading): ValidationCheck {
        val errors = mutableListOf<String>()
        
        // Signalstärke
        if (reading.signalStrength < MIN_SIGNAL_STRENGTH || reading.signalStrength > MAX_SIGNAL_STRENGTH) {
            errors.add("Signalstärke außerhalb des gültigen Bereichs: ${reading.signalStrength}")
        }
        
        // Frequenz
        if (reading.frequency < MIN_FREQUENCY || reading.frequency > MAX_FREQUENCY) {
            errors.add("Frequenz außerhalb des gültigen Bereichs: ${reading.frequency}")
        }
        
        // Phase
        if (reading.phase < MIN_PHASE || reading.phase > MAX_PHASE) {
            errors.add("Phase außerhalb des gültigen Bereichs: ${reading.phase}")
        }
        
        // Amplitude
        if (reading.amplitude < MIN_AMPLITUDE || reading.amplitude > MAX_AMPLITUDE) {
            errors.add("Amplitude außerhalb des gültigen Bereichs: ${reading.amplitude}")
        }
        
        // Temperatur
        if (reading.temperature < MIN_TEMPERATURE || reading.temperature > MAX_TEMPERATURE) {
            errors.add("Temperatur außerhalb des gültigen Bereichs: ${reading.temperature}")
        }
        
        // Luftfeuchtigkeit
        if (reading.humidity < MIN_HUMIDITY || reading.humidity > MAX_HUMIDITY) {
            errors.add("Luftfeuchtigkeit außerhalb des gültigen Bereichs: ${reading.humidity}")
        }
        
        // Luftdruck
        if (reading.pressure < MIN_PRESSURE || reading.pressure > MAX_PRESSURE) {
            errors.add("Luftdruck außerhalb des gültigen Bereichs: ${reading.pressure}")
        }
        
        // Tiefe
        if (reading.depth < MIN_DEPTH || reading.depth > MAX_DEPTH) {
            errors.add("Tiefe außerhalb des gültigen Bereichs: ${reading.depth}")
        }
        
        // Rauschpegel
        if (reading.noiseLevel > MAX_NOISE_LEVEL) {
            errors.add("Rauschpegel zu hoch: ${reading.noiseLevel}")
        }
        
        return ValidationCheck(errors)
    }
    
    /**
     * Physikalische Konsistenz prüfen
     */
    private fun validatePhysics(reading: EMFReading): ValidationCheck {
        val errors = mutableListOf<String>()
        
        // Magnitude vs. Real/Imaginary Parts
        val calculatedMagnitude = sqrt(reading.realPart * reading.realPart + reading.imaginaryPart * reading.imaginaryPart)
        val magnitudeDifference = abs(reading.magnitude - calculatedMagnitude)
        if (magnitudeDifference > reading.magnitude * 0.1) { // 10% Toleranz
            errors.add("Magnitude inkonsistent mit Real-/Imaginärteil: ${reading.magnitude} vs $calculatedMagnitude")
        }
        
        // Amplitude vs. Signalstärke
        val amplitudeRatio = if (reading.amplitude > 0) reading.signalStrength / reading.amplitude else 0.0
        if (amplitudeRatio > 2.0 || amplitudeRatio < 0.1) {
            errors.add("Signalstärke/Amplitude-Verhältnis unplausibel: $amplitudeRatio")
        }
        
        // Phase vs. Real/Imaginary Parts
        if (reading.magnitude > 0) {
            val calculatedPhase = atan2(reading.imaginaryPart, reading.realPart) * 180.0 / PI
            val phaseDifference = abs(reading.phase - calculatedPhase)
            if (phaseDifference > 30.0) { // 30° Toleranz
                errors.add("Phase inkonsistent mit Real-/Imaginärteil: ${reading.phase}° vs $calculatedPhase°")
            }
        }
        
        // Skin-Effekt Konsistenz
        val skinDepthCheck = validateSkinDepth(reading)
        if (!skinDepthCheck) {
            errors.add("Skin-Effekt Parameter inkonsistent")
        }
        
        // Leitfähigkeits-Plausibilität
        val conductivityCheck = validateConductivity(reading)
        if (!conductivityCheck) {
            errors.add("Leitfähigkeits-Parameter unplausibel")
        }
        
        return ValidationCheck(errors)
    }
    
    /**
     * Zeitliche Konsistenz prüfen
     */
    private fun validateTemporal(reading: EMFReading): ValidationCheck {
        val errors = mutableListOf<String>()
        
        lastValidReading?.let { lastReading ->
            val timeDelta = reading.timestamp - lastReading.timestamp
            
            // Zeitstempel-Plausibilität
            if (timeDelta <= 0) {
                errors.add("Zeitstempel nicht monoton steigend")
            }
            
            if (timeDelta > 10000) { // 10 Sekunden
                errors.add("Zeitstempel-Sprung zu groß: ${timeDelta}ms")
            }
            
            // Signal-Änderungsrate
            val signalChangeRate = abs(reading.signalStrength - lastReading.signalStrength) / lastReading.signalStrength
            if (signalChangeRate > MAX_SIGNAL_CHANGE_RATE) {
                errors.add("Signalstärke ändert sich zu schnell: ${signalChangeRate * 100}%")
            }
            
            // Frequenz-Drift
            val frequencyDrift = abs(reading.frequency - lastReading.frequency) / lastReading.frequency
            if (frequencyDrift > MAX_FREQUENCY_DRIFT) {
                errors.add("Frequenz-Drift zu groß: ${frequencyDrift * 100}%")
            }
            
            // Phasen-Sprung
            val phaseJump = abs(reading.phase - lastReading.phase)
            if (phaseJump > MAX_PHASE_JUMP) {
                errors.add("Phasen-Sprung zu groß: $phaseJump°")
            }
            
            // Temperatur-Änderungsrate
            val tempChangeRate = abs(reading.temperature - lastReading.temperature) / (timeDelta / 1000.0)
            if (tempChangeRate > 5.0) { // 5°C pro Sekunde
                errors.add("Temperatur ändert sich zu schnell: $tempChangeRate°C/s")
            }
        }
        
        return ValidationCheck(errors)
    }
    
    /**
     * Signal-Qualität prüfen
     */
    private fun validateQuality(reading: EMFReading): ValidationCheck {
        val errors = mutableListOf<String>()
        
        // Signal-zu-Rausch-Verhältnis
        val snr = if (reading.noiseLevel > 0) reading.signalStrength / reading.noiseLevel else Double.MAX_VALUE
        if (snr < MIN_SNR) {
            errors.add("Signal-zu-Rausch-Verhältnis zu niedrig: $snr")
        }
        
        // Qualitätsscore-Plausibilität
        if (reading.qualityScore < 0.0 || reading.qualityScore > 1.0) {
            errors.add("Qualitätsscore außerhalb des gültigen Bereichs: ${reading.qualityScore}")
        }
        
        // Kalibrierungs-Konsistenz
        if (abs(reading.calibrationOffset) > 1000.0) {
            errors.add("Kalibrierungs-Offset zu groß: ${reading.calibrationOffset}")
        }
        
        // Verstärkungs-Plausibilität
        if (reading.gainSetting < 0.1 || reading.gainSetting > 100.0) {
            errors.add("Verstärkungseinstellung unplausibel: ${reading.gainSetting}")
        }
        
        return ValidationCheck(errors)
    }
    
    /**
     * Skin-Tiefe validieren
     */
    private fun validateSkinDepth(reading: EMFReading): Boolean {
        if (reading.frequency <= 0) return false
        
        // Theoretische Skin-Tiefe für typische Materialien
        val omega = 2 * PI * reading.frequency
        val mu0 = 4 * PI * 1e-7
        val typicalConductivity = 1e6 // Kupfer als Referenz
        
        val theoreticalSkinDepth = sqrt(2.0 / (omega * mu0 * typicalConductivity))
        
        // Plausibilitätsprüfung basierend auf Signalabschwächung
        val expectedAttenuation = exp(-reading.depth / theoreticalSkinDepth)
        val actualAttenuation = reading.signalStrength / 1000.0 // Normalisiert
        
        return abs(expectedAttenuation - actualAttenuation) < 0.5
    }
    
    /**
     * Leitfähigkeit validieren
     */
    private fun validateConductivity(reading: EMFReading): Boolean {
        // Leitfähigkeits-Bereich für typische Materialien
        val minConductivity = 1e-10 // Isolatoren
        val maxConductivity = 1e8   // Supraleitende Materialien
        
        // Geschätzte Leitfähigkeit aus Messdaten
        val omega = 2 * PI * reading.frequency
        val mu0 = 4 * PI * 1e-7
        val attenuationFactor = reading.signalStrength / reading.amplitude
        
        if (attenuationFactor <= 0) return false
        
        val estimatedConductivity = 2.0 / (omega * mu0 * (1.0 / attenuationFactor).pow(2))
        
        return estimatedConductivity >= minConductivity && estimatedConductivity <= maxConductivity
    }
    
    /**
     * Validierungshistorie hinzufügen
     */
    private fun addToHistory(result: ValidationResult) {
        validationHistory.add(result)
        if (validationHistory.size > maxHistorySize) {
            validationHistory.removeAt(0)
        }
    }
    
    /**
     * Validierungsstatistiken abrufen
     */
    fun getValidationStats(): ValidationStats {
        val totalValidations = validationHistory.size
        val validCount = validationHistory.count { it.isValid }
        val invalidCount = totalValidations - validCount
        
        val errorCounts = mutableMapOf<String, Int>()
        validationHistory.forEach { result ->
            result.getAllErrors().forEach { error ->
                errorCounts[error] = errorCounts.getOrDefault(error, 0) + 1
            }
        }
        
        return ValidationStats(
            totalValidations = totalValidations,
            validCount = validCount,
            invalidCount = invalidCount,
            validationRate = if (totalValidations > 0) validCount.toDouble() / totalValidations else 0.0,
            commonErrors = errorCounts.toList().sortedByDescending { it.second }.take(5)
        )
    }
    
    /**
     * Validierungshistorie zurücksetzen
     */
    fun resetValidation() {
        validationHistory.clear()
        lastValidReading = null
        Log.d(TAG, "Validierung zurückgesetzt")
    }
}

/**
 * Validierungsergebnis
 */
data class ValidationResult(
    val timestamp: Long,
    val originalReading: EMFReading,
    var isValid: Boolean = false,
    val rangeErrors: MutableList<String> = mutableListOf(),
    val physicsErrors: MutableList<String> = mutableListOf(),
    val temporalErrors: MutableList<String> = mutableListOf(),
    val qualityErrors: MutableList<String> = mutableListOf()
) {
    fun isValid(): Boolean {
        return rangeErrors.isEmpty() && physicsErrors.isEmpty() && 
               temporalErrors.isEmpty() && qualityErrors.isEmpty()
    }
    
    fun getAllErrors(): List<String> {
        return rangeErrors + physicsErrors + temporalErrors + qualityErrors
    }
}

/**
 * Validierungsprüfung
 */
data class ValidationCheck(
    val errors: List<String>
)

/**
 * Validierungsstatistiken
 */
data class ValidationStats(
    val totalValidations: Int,
    val validCount: Int,
    val invalidCount: Int,
    val validationRate: Double,
    val commonErrors: List<Pair<String, Int>>
)
