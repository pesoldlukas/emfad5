package com.emfad.app.services.analysis

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * EMFAD® Signal Analyzer
 * Basiert auf Signalanalyse der originalen Windows-Software
 * Implementiert echte Tiefenberechnung und Signal-Processing
 * Rekonstruiert aus HzEMSoftexe.c und EMFAD3EXE.c
 */

@Serializable
data class EMFReading(
    val sessionId: Long,
    val timestamp: Long,
    val frequency: Double,
    val signalStrength: Double,
    val phase: Double,
    val amplitude: Double,
    val realPart: Double,
    val imaginaryPart: Double,
    val magnitude: Double,
    val depth: Double,
    val temperature: Double,
    val humidity: Double,
    val pressure: Double,
    val batteryLevel: Int,
    val deviceId: String,
    val materialType: MaterialType,
    val confidence: Double,
    val noiseLevel: Double,
    val calibrationOffset: Double,
    val gainSetting: Double,
    val filterSetting: String,
    val measurementMode: String,
    val qualityScore: Double,
    val xCoordinate: Double = 0.0,
    val yCoordinate: Double = 0.0,
    val zCoordinate: Double = 0.0,
    val gpsData: String = ""
)

enum class MaterialType(val displayName: String, val conductivity: Double) {
    UNKNOWN("Unbekannt", 0.0),
    AIR("Luft", 0.0),
    WATER("Wasser", 0.0005),
    SOIL_DRY("Trockener Boden", 0.001),
    SOIL_WET("Feuchter Boden", 0.01),
    CLAY("Ton", 0.02),
    SAND("Sand", 0.0001),
    ROCK("Gestein", 0.00001),
    CONCRETE("Beton", 0.0001),
    METAL("Metall", 1000000.0),
    WOOD("Holz", 0.00001),
    PLASTIC("Kunststoff", 0.000000001)
}

@Serializable
data class SignalQuality(
    val snrRatio: Double,
    val noiseFloor: Double,
    val signalStability: Double,
    val phaseCoherence: Double,
    val overallQuality: Double
)

@Serializable
data class DepthAnalysis(
    val calculatedDepth: Double,
    val confidence: Double,
    val method: String,
    val calibrationFactor: Double,
    val attenuationCoefficient: Double
)

@Singleton
class SignalAnalyzer @Inject constructor() {
    
    companion object {
        private const val TAG = "EMFADSignalAnalyzer"
        
        // EMFAD-Kalibrierungskonstanten aus originaler Software
        private const val CALIBRATION_CONSTANT_DEFAULT = 3333.0
        private const val ATTENUATION_FACTOR_DEFAULT = 0.417
        
        // Frequenz-spezifische Kalibrierungskonstanten
        private val FREQUENCY_CALIBRATION_CONSTANTS = mapOf(
            19000.0 to 3333.0,
            23400.0 to 3200.0,
            70000.0 to 2800.0,
            77500.0 to 2750.0,
            124000.0 to 2400.0,
            129100.0 to 2350.0,
            135600.0 to 2300.0
        )
        
        // Material-spezifische Dämpfungskoeffizienten
        private val MATERIAL_ATTENUATION_FACTORS = mapOf(
            MaterialType.AIR to 0.1,
            MaterialType.WATER to 0.3,
            MaterialType.SOIL_DRY to 0.417,
            MaterialType.SOIL_WET to 0.6,
            MaterialType.CLAY to 0.8,
            MaterialType.SAND to 0.35,
            MaterialType.ROCK to 0.25,
            MaterialType.CONCRETE to 0.45,
            MaterialType.METAL to 2.0,
            MaterialType.WOOD to 0.2,
            MaterialType.PLASTIC to 0.15
        )
    }
    
    private val analyzerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Signal-Processing State
    private val _processedReadings = MutableSharedFlow<EMFReading>()
    val processedReadings: SharedFlow<EMFReading> = _processedReadings.asSharedFlow()
    
    // Moving Average Buffer für Signal-Smoothing
    private val signalBuffer = mutableMapOf<Double, LinkedList<Double>>()
    private val bufferSize = 10
    
    /**
     * Analysiert ein EMF-Signal und berechnet alle Parameter
     */
    suspend fun analyzeSignal(
        rawSignal: ByteArray,
        frequency: Double,
        sessionId: Long,
        deviceId: String,
        measurementMode: String = "A",
        calibrationOffset: Double = 0.0,
        gainSetting: Double = 1.0
    ): EMFReading? {
        return withContext(Dispatchers.Default) {
            try {
                // Raw-Signal zu komplexen Zahlen konvertieren
                val complexSignal = parseComplexSignal(rawSignal)
                if (complexSignal.isEmpty()) return@withContext null
                
                // Grundlegende Signal-Parameter berechnen
                val realPart = complexSignal.map { it.real }.average()
                val imaginaryPart = complexSignal.map { it.imaginary }.average()
                val magnitude = sqrt(realPart * realPart + imaginaryPart * imaginaryPart)
                val phase = atan2(imaginaryPart, realPart) * 180.0 / PI
                val amplitude = magnitude
                
                // Signal-Qualität analysieren
                val signalQuality = calculateSignalQuality(complexSignal, frequency)
                
                // Tiefe berechnen (Kern-Algorithmus aus originaler Software)
                val depthAnalysis = calculateDepth(magnitude, frequency, measurementMode)
                
                // Material-Typ schätzen
                val materialType = estimateMaterialType(magnitude, phase, frequency)
                
                // Signal-Smoothing anwenden
                val smoothedMagnitude = applyMovingAverage(magnitude, frequency)
                
                // Rauschen analysieren
                val noiseLevel = calculateNoiseLevel(complexSignal)
                
                // EMF-Reading erstellen
                val reading = EMFReading(
                    sessionId = sessionId,
                    timestamp = System.currentTimeMillis(),
                    frequency = frequency,
                    signalStrength = smoothedMagnitude,
                    phase = phase,
                    amplitude = amplitude,
                    realPart = realPart,
                    imaginaryPart = imaginaryPart,
                    magnitude = magnitude,
                    depth = depthAnalysis.calculatedDepth,
                    temperature = 20.0, // TODO: Von Sensor lesen
                    humidity = 50.0,    // TODO: Von Sensor lesen
                    pressure = 1013.25, // TODO: Von Sensor lesen
                    batteryLevel = 100, // TODO: Von Gerät lesen
                    deviceId = deviceId,
                    materialType = materialType,
                    confidence = depthAnalysis.confidence,
                    noiseLevel = noiseLevel,
                    calibrationOffset = calibrationOffset,
                    gainSetting = gainSetting,
                    filterSetting = "default",
                    measurementMode = measurementMode,
                    qualityScore = signalQuality.overallQuality
                )
                
                // Verarbeitetes Signal emittieren
                _processedReadings.emit(reading)
                
                Log.d(TAG, "Signal analyzed: f=${frequency}Hz, depth=${depthAnalysis.calculatedDepth}m, quality=${signalQuality.overallQuality}")
                
                reading
                
            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing signal", e)
                null
            }
        }
    }
    
    /**
     * Berechnet die Tiefe basierend auf EMFAD-Algorithmus
     * Rekonstruiert aus originaler Windows-Software
     */
    private fun calculateDepth(
        magnitude: Double,
        frequency: Double,
        measurementMode: String
    ): DepthAnalysis {
        // Frequenz-spezifische Kalibrierungskonstante
        val calibrationConstant = FREQUENCY_CALIBRATION_CONSTANTS[frequency] 
            ?: CALIBRATION_CONSTANT_DEFAULT
        
        // Dämpfungskoeffizient (Standard für trockenen Boden)
        val attenuationFactor = ATTENUATION_FACTOR_DEFAULT
        
        // Kalibriertes Signal berechnen
        val calibratedSignal = magnitude * (calibrationConstant / 1000.0)
        
        // Tiefenberechnung: depth = -ln(calibratedSignal / 1000) / attenuationFactor
        val depth = if (calibratedSignal > 0) {
            -ln(calibratedSignal / 1000.0) / attenuationFactor
        } else {
            0.0
        }
        
        // Konfidenz basierend auf Signal-Stärke und Frequenz
        val confidence = calculateDepthConfidence(magnitude, frequency, depth)
        
        return DepthAnalysis(
            calculatedDepth = maxOf(0.0, depth),
            confidence = confidence,
            method = "EMFAD_LOGARITHMIC",
            calibrationFactor = calibrationConstant,
            attenuationCoefficient = attenuationFactor
        )
    }
    
    /**
     * Berechnet die Konfidenz der Tiefenmessung
     */
    private fun calculateDepthConfidence(
        magnitude: Double,
        frequency: Double,
        depth: Double
    ): Double {
        // Signal-Stärke-Faktor
        val signalFactor = when {
            magnitude > 1000 -> 1.0
            magnitude > 100 -> 0.8
            magnitude > 10 -> 0.6
            magnitude > 1 -> 0.4
            else -> 0.2
        }
        
        // Frequenz-Faktor (mittlere Frequenzen sind zuverlässiger)
        val frequencyFactor = when {
            frequency in 70000.0..80000.0 -> 1.0
            frequency in 20000.0..130000.0 -> 0.8
            else -> 0.6
        }
        
        // Tiefen-Faktor (sehr geringe oder sehr große Tiefen sind unsicherer)
        val depthFactor = when {
            depth in 0.1..10.0 -> 1.0
            depth in 0.01..20.0 -> 0.8
            else -> 0.5
        }
        
        return (signalFactor * frequencyFactor * depthFactor).coerceIn(0.0, 1.0)
    }
    
    /**
     * Schätzt den Material-Typ basierend auf Signal-Charakteristika
     */
    private fun estimateMaterialType(
        magnitude: Double,
        phase: Double,
        frequency: Double
    ): MaterialType {
        return when {
            magnitude > 10000 && abs(phase) < 10 -> MaterialType.METAL
            magnitude > 1000 && phase > 45 -> MaterialType.WATER
            magnitude > 500 && phase in 20.0..45.0 -> MaterialType.SOIL_WET
            magnitude > 100 && phase in 10.0..30.0 -> MaterialType.SOIL_DRY
            magnitude > 50 && phase < 15 -> MaterialType.SAND
            magnitude > 20 && phase > 60 -> MaterialType.CLAY
            magnitude < 10 -> MaterialType.AIR
            else -> MaterialType.UNKNOWN
        }
    }
    
    /**
     * Berechnet Signal-Qualität
     */
    private fun calculateSignalQuality(
        complexSignal: List<ComplexNumber>,
        frequency: Double
    ): SignalQuality {
        if (complexSignal.isEmpty()) {
            return SignalQuality(0.0, 0.0, 0.0, 0.0, 0.0)
        }
        
        val magnitudes = complexSignal.map { sqrt(it.real * it.real + it.imaginary * it.imaginary) }
        val phases = complexSignal.map { atan2(it.imaginary, it.real) }
        
        // SNR berechnen
        val signalPower = magnitudes.average()
        val noisePower = magnitudes.map { (it - signalPower).pow(2) }.average()
        val snrRatio = if (noisePower > 0) 10 * log10(signalPower / noisePower) else 60.0
        
        // Rauschpegel
        val noiseFloor = sqrt(noisePower)
        
        // Signal-Stabilität
        val signalStability = 1.0 - (magnitudes.maxOrNull()!! - magnitudes.minOrNull()!!) / signalPower
        
        // Phasen-Kohärenz
        val phaseVariance = phases.map { (it - phases.average()).pow(2) }.average()
        val phaseCoherence = 1.0 - (phaseVariance / (PI * PI))
        
        // Gesamt-Qualität
        val overallQuality = (snrRatio / 60.0 * 0.4 + 
                             signalStability * 0.3 + 
                             phaseCoherence * 0.3).coerceIn(0.0, 1.0)
        
        return SignalQuality(
            snrRatio = snrRatio,
            noiseFloor = noiseFloor,
            signalStability = signalStability,
            phaseCoherence = phaseCoherence,
            overallQuality = overallQuality
        )
    }
    
    /**
     * Wendet Moving Average für Signal-Smoothing an
     */
    private fun applyMovingAverage(magnitude: Double, frequency: Double): Double {
        val buffer = signalBuffer.getOrPut(frequency) { LinkedList() }
        
        buffer.add(magnitude)
        if (buffer.size > bufferSize) {
            buffer.removeFirst()
        }
        
        return buffer.average()
    }
    
    /**
     * Berechnet Rauschpegel
     */
    private fun calculateNoiseLevel(complexSignal: List<ComplexNumber>): Double {
        if (complexSignal.size < 2) return 0.0
        
        val magnitudes = complexSignal.map { sqrt(it.real * it.real + it.imaginary * it.imaginary) }
        val mean = magnitudes.average()
        val variance = magnitudes.map { (it - mean).pow(2) }.average()
        
        return sqrt(variance)
    }
    
    /**
     * Parst Raw-Signal zu komplexen Zahlen
     */
    private fun parseComplexSignal(rawSignal: ByteArray): List<ComplexNumber> {
        val complexNumbers = mutableListOf<ComplexNumber>()
        
        try {
            // Annahme: Raw-Signal enthält abwechselnd Real- und Imaginär-Teile als Float-Werte
            val buffer = java.nio.ByteBuffer.wrap(rawSignal).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            
            while (buffer.remaining() >= 8) { // 2 * 4 Bytes für Real + Imaginär
                val real = buffer.float.toDouble()
                val imaginary = buffer.float.toDouble()
                complexNumbers.add(ComplexNumber(real, imaginary))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing complex signal", e)
        }
        
        return complexNumbers
    }
    
    /**
     * Kalibriert Signal basierend auf Material-Typ
     */
    fun calibrateForMaterial(
        reading: EMFReading,
        materialType: MaterialType
    ): EMFReading {
        val attenuationFactor = MATERIAL_ATTENUATION_FACTORS[materialType] 
            ?: ATTENUATION_FACTOR_DEFAULT
        
        // Tiefe neu berechnen mit material-spezifischem Dämpfungskoeffizienten
        val calibrationConstant = FREQUENCY_CALIBRATION_CONSTANTS[reading.frequency] 
            ?: CALIBRATION_CONSTANT_DEFAULT
        val calibratedSignal = reading.magnitude * (calibrationConstant / 1000.0)
        
        val newDepth = if (calibratedSignal > 0) {
            -ln(calibratedSignal / 1000.0) / attenuationFactor
        } else {
            0.0
        }
        
        return reading.copy(
            depth = maxOf(0.0, newDepth),
            materialType = materialType,
            confidence = calculateDepthConfidence(reading.magnitude, reading.frequency, newDepth)
        )
    }
    
    fun cleanup() {
        signalBuffer.clear()
        analyzerScope.cancel()
    }
}

/**
 * Hilfsklasse für komplexe Zahlen
 */
data class ComplexNumber(
    val real: Double,
    val imaginary: Double
) {
    val magnitude: Double get() = sqrt(real * real + imaginary * imaginary)
    val phase: Double get() = atan2(imaginary, real)
}
