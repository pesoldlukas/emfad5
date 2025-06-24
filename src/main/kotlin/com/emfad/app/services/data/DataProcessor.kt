package com.emfad.app.services.data

import android.util.Log
import com.emfad.app.models.EMFReading
import com.emfad.app.models.MaterialType
import kotlin.math.*

/**
 * EMFAD Data Processor
 * Echtzeit-Datenverarbeitung mit ursprünglichen Algorithmen
 * Samsung S21 Ultra optimiert
 */
class DataProcessor {
    
    companion object {
        private const val TAG = "DataProcessor"
        private const val SMOOTHING_FACTOR = 0.1
        private const val NOISE_THRESHOLD = 50.0
        private const val CALIBRATION_REFERENCE = 1000.0
        private const val TEMPERATURE_COMPENSATION_FACTOR = 0.002
        private const val FREQUENCY_NORMALIZATION_BASE = 100.0
    }
    
    // Glättungsfilter-Zustand
    private var previousSignalStrength = 0.0
    private var previousPhase = 0.0
    private var previousAmplitude = 0.0
    
    // Kalibrierungsdaten
    private var calibrationOffset = 0.0
    private var gainCorrection = 1.0
    private var temperatureReference = 25.0
    
    // Rauschfilter
    private val signalHistory = mutableListOf<Double>()
    private val phaseHistory = mutableListOf<Double>()
    private val maxHistorySize = 10
    
    /**
     * EMF-Messung verarbeiten
     */
    fun processReading(reading: EMFReading): EMFReading {
        Log.d(TAG, "Verarbeite Messung: Signal=${reading.signalStrength}")
        
        // 1. Rauschfilterung
        val filteredReading = applyNoiseFilter(reading)
        
        // 2. Kalibrierung anwenden
        val calibratedReading = applyCalibration(filteredReading)
        
        // 3. Temperaturkompensation
        val temperatureCompensated = applyTemperatureCompensation(calibratedReading)
        
        // 4. Signalglättung
        val smoothedReading = applySmoothing(temperatureCompensated)
        
        // 5. Erweiterte Berechnungen
        val enhancedReading = calculateEnhancedParameters(smoothedReading)
        
        // 6. Qualitätsbewertung
        val finalReading = calculateQualityScore(enhancedReading)
        
        Log.d(TAG, "Verarbeitung abgeschlossen: Signal=${finalReading.signalStrength}, Qualität=${finalReading.qualityScore}")
        
        return finalReading
    }
    
    /**
     * Rauschfilter anwenden
     */
    private fun applyNoiseFilter(reading: EMFReading): EMFReading {
        // Signal-Historie aktualisieren
        signalHistory.add(reading.signalStrength)
        phaseHistory.add(reading.phase)
        
        if (signalHistory.size > maxHistorySize) {
            signalHistory.removeAt(0)
            phaseHistory.removeAt(0)
        }
        
        // Median-Filter für Rauschunterdrückung
        val filteredSignal = if (signalHistory.size >= 3) {
            medianFilter(signalHistory.takeLast(3))
        } else {
            reading.signalStrength
        }
        
        val filteredPhase = if (phaseHistory.size >= 3) {
            medianFilter(phaseHistory.takeLast(3))
        } else {
            reading.phase
        }
        
        // Rauschpegel berechnen
        val noiseLevel = calculateNoiseLevel(signalHistory)
        
        return reading.copy(
            signalStrength = filteredSignal,
            phase = filteredPhase,
            noiseLevel = noiseLevel
        )
    }
    
    /**
     * Kalibrierung anwenden
     */
    private fun applyCalibration(reading: EMFReading): EMFReading {
        // Offset-Korrektur
        val offsetCorrectedSignal = reading.signalStrength - calibrationOffset
        
        // Verstärkungskorrektur
        val gainCorrectedSignal = offsetCorrectedSignal * gainCorrection
        
        // Frequenz-abhängige Korrektur
        val frequencyCorrection = calculateFrequencyCorrection(reading.frequency)
        val frequencyCorrectedSignal = gainCorrectedSignal * frequencyCorrection
        
        return reading.copy(
            signalStrength = frequencyCorrectedSignal,
            calibrationOffset = calibrationOffset
        )
    }
    
    /**
     * Temperaturkompensation anwenden
     */
    private fun applyTemperatureCompensation(reading: EMFReading): EMFReading {
        val temperatureDelta = reading.temperature - temperatureReference
        val compensationFactor = 1.0 + (temperatureDelta * TEMPERATURE_COMPENSATION_FACTOR)
        
        val compensatedSignal = reading.signalStrength / compensationFactor
        val compensatedPhase = reading.phase - (temperatureDelta * 0.1) // Phasenkorrektur
        
        return reading.copy(
            signalStrength = compensatedSignal,
            phase = compensatedPhase
        )
    }
    
    /**
     * Signalglättung anwenden
     */
    private fun applySmoothing(reading: EMFReading): EMFReading {
        // Exponentieller gleitender Durchschnitt
        val smoothedSignal = if (previousSignalStrength > 0) {
            SMOOTHING_FACTOR * reading.signalStrength + (1 - SMOOTHING_FACTOR) * previousSignalStrength
        } else {
            reading.signalStrength
        }
        
        val smoothedPhase = if (previousPhase > 0) {
            SMOOTHING_FACTOR * reading.phase + (1 - SMOOTHING_FACTOR) * previousPhase
        } else {
            reading.phase
        }
        
        val smoothedAmplitude = if (previousAmplitude > 0) {
            SMOOTHING_FACTOR * reading.amplitude + (1 - SMOOTHING_FACTOR) * previousAmplitude
        } else {
            reading.amplitude
        }
        
        // Zustand aktualisieren
        previousSignalStrength = smoothedSignal
        previousPhase = smoothedPhase
        previousAmplitude = smoothedAmplitude
        
        return reading.copy(
            signalStrength = smoothedSignal,
            phase = smoothedPhase,
            amplitude = smoothedAmplitude
        )
    }
    
    /**
     * Erweiterte Parameter berechnen (ursprüngliche Algorithmen)
     */
    private fun calculateEnhancedParameters(reading: EMFReading): EMFReading {
        // Komplexe Zahlen-Darstellung
        val phaseRad = reading.phase * PI / 180.0
        val realPart = reading.amplitude * cos(phaseRad)
        val imaginaryPart = reading.amplitude * sin(phaseRad)
        val magnitude = sqrt(realPart * realPart + imaginaryPart * imaginaryPart)
        
        // Leitfähigkeit berechnen (ursprünglicher Algorithmus)
        val conductivity = calculateConductivity(reading)
        
        // Magnetische Permeabilität
        val magneticPermeability = calculateMagneticPermeability(reading)
        
        // Skin-Tiefe
        val skinDepth = calculateSkinDepth(reading, conductivity, magneticPermeability)
        
        // Impedanz
        val impedanceReal = realPart / magnitude
        val impedanceImaginary = imaginaryPart / magnitude
        
        // Tiefenschätzung basierend auf Signalabschwächung
        val estimatedDepth = calculateDepthEstimate(reading)
        
        return reading.copy(
            realPart = realPart,
            imaginaryPart = imaginaryPart,
            magnitude = magnitude,
            depth = estimatedDepth
        )
    }
    
    /**
     * Qualitätsbewertung berechnen
     */
    private fun calculateQualityScore(reading: EMFReading): EMFReading {
        var qualityScore = 1.0
        
        // Signal-zu-Rausch-Verhältnis
        val snr = if (reading.noiseLevel > 0) {
            reading.signalStrength / reading.noiseLevel
        } else {
            Double.MAX_VALUE
        }
        
        // SNR-basierte Qualität (0-1)
        val snrQuality = min(1.0, snr / 20.0) // 20 dB als Referenz
        qualityScore *= snrQuality
        
        // Signalstärke-basierte Qualität
        val signalQuality = min(1.0, reading.signalStrength / 1000.0)
        qualityScore *= signalQuality
        
        // Frequenz-Stabilität
        val frequencyQuality = if (reading.frequency > 0) {
            min(1.0, FREQUENCY_NORMALIZATION_BASE / reading.frequency)
        } else {
            0.0
        }
        qualityScore *= frequencyQuality
        
        // Temperatur-Stabilität
        val temperatureQuality = 1.0 - min(0.5, abs(reading.temperature - 25.0) / 50.0)
        qualityScore *= temperatureQuality
        
        return reading.copy(qualityScore = max(0.0, min(1.0, qualityScore)))
    }
    
    /**
     * Leitfähigkeit berechnen (ursprünglicher Algorithmus)
     */
    private fun calculateConductivity(reading: EMFReading): Double {
        val omega = 2 * PI * reading.frequency
        val mu0 = 4 * PI * 1e-7 // Permeabilität des freien Raums
        
        // Skin-Effekt basierte Berechnung
        val attenuationFactor = reading.signalStrength / reading.amplitude
        val skinDepthEstimate = 1.0 / attenuationFactor
        
        return 2.0 / (omega * mu0 * skinDepthEstimate * skinDepthEstimate)
    }
    
    /**
     * Magnetische Permeabilität berechnen
     */
    private fun calculateMagneticPermeability(reading: EMFReading): Double {
        val phaseRad = reading.phase * PI / 180.0
        val mu0 = 4 * PI * 1e-7
        
        // Phasenverschiebung-basierte Berechnung
        val permeabilityFactor = abs(cos(phaseRad))
        return mu0 * (1.0 + permeabilityFactor)
    }
    
    /**
     * Skin-Tiefe berechnen
     */
    private fun calculateSkinDepth(reading: EMFReading, conductivity: Double, permeability: Double): Double {
        val omega = 2 * PI * reading.frequency
        return sqrt(2.0 / (omega * permeability * conductivity))
    }
    
    /**
     * Tiefe schätzen
     */
    private fun calculateDepthEstimate(reading: EMFReading): Double {
        // Exponentieller Abfall des Signals mit der Tiefe
        val attenuationCoefficient = 0.1 // Empirischer Wert
        val referenceSignal = 1000.0
        
        return if (reading.signalStrength > 0 && reading.signalStrength < referenceSignal) {
            -ln(reading.signalStrength / referenceSignal) / attenuationCoefficient
        } else {
            0.0
        }
    }
    
    /**
     * Median-Filter
     */
    private fun medianFilter(values: List<Double>): Double {
        val sorted = values.sorted()
        return when (sorted.size) {
            0 -> 0.0
            1 -> sorted[0]
            2 -> (sorted[0] + sorted[1]) / 2.0
            else -> sorted[sorted.size / 2]
        }
    }
    
    /**
     * Rauschpegel berechnen
     */
    private fun calculateNoiseLevel(signalHistory: List<Double>): Double {
        if (signalHistory.size < 2) return 0.0
        
        val mean = signalHistory.average()
        val variance = signalHistory.map { (it - mean).pow(2) }.average()
        return sqrt(variance)
    }
    
    /**
     * Frequenzkorrektur berechnen
     */
    private fun calculateFrequencyCorrection(frequency: Double): Double {
        // Frequenz-abhängige Verstärkungskorrektur
        val normalizedFreq = frequency / FREQUENCY_NORMALIZATION_BASE
        return 1.0 / sqrt(normalizedFreq)
    }
    
    /**
     * Kalibrierung setzen
     */
    fun setCalibration(offset: Double, gain: Double, tempRef: Double) {
        calibrationOffset = offset
        gainCorrection = gain
        temperatureReference = tempRef
        Log.d(TAG, "Kalibrierung gesetzt: Offset=$offset, Gain=$gain, TempRef=$tempRef")
    }
    
    /**
     * Filter zurücksetzen
     */
    fun resetFilters() {
        previousSignalStrength = 0.0
        previousPhase = 0.0
        previousAmplitude = 0.0
        signalHistory.clear()
        phaseHistory.clear()
        Log.d(TAG, "Filter zurückgesetzt")
    }
    
    /**
     * Verarbeitungsstatistiken abrufen
     */
    fun getProcessingStats(): ProcessingStats {
        return ProcessingStats(
            calibrationOffset = calibrationOffset,
            gainCorrection = gainCorrection,
            temperatureReference = temperatureReference,
            historySize = signalHistory.size,
            averageNoiseLevel = if (signalHistory.isNotEmpty()) calculateNoiseLevel(signalHistory) else 0.0
        )
    }
}

/**
 * Verarbeitungsstatistiken
 */
data class ProcessingStats(
    val calibrationOffset: Double,
    val gainCorrection: Double,
    val temperatureReference: Double,
    val historySize: Int,
    val averageNoiseLevel: Double
)
