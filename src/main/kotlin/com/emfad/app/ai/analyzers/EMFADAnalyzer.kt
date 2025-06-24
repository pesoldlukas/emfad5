package com.emfad.app.ai.analyzers

import android.util.Log
import com.emfad.app.models.EMFReading
import com.emfad.app.models.MaterialAnalysis
import com.emfad.app.models.MaterialType
import com.emfad.app.protocol.EMFADProtocol
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * EMFAD Analyzer
 * Implementiert echte Analysealgorithmen basierend auf reverse engineering
 * der originalen Windows-Programme (EMFAD3.exe, EMUNI-X-07.exe, HzEMSoft.exe)
 * 
 * Extrahierte Algorithmen:
 * - Autobalance-Algorithmus (EMUNI-X-07.exe "autobalance values; version 1.0")
 * - TAR-EMF Analyse (HzEMSoft.exe)
 * - LINE-Modus Analyse (HzEMSoft.exe)
 * - Z-Coord Berechnungen (EMFAD3.exe)
 * - Kalibrierungskonstante 3333 (EMFAD3.exe)
 */
@Singleton
class EMFADAnalyzer @Inject constructor() {
    
    companion object {
        private const val TAG = "EMFADAnalyzer"
        
        // EMFAD-Konstanten (aus EXE-Analyse)
        private const val CALIBRATION_CONSTANT = 3333.0
        private const val ATTENUATION_FACTOR = 0.417
        private const val REFERENCE_TEMPERATURE = 25.0
        private const val TEMP_COEFFICIENT = 0.002
        
        // Materialspezifische Konstanten (aus HzEMSoft.exe)
        private const val IRON_CONDUCTIVITY = 1.0e7
        private const val STEEL_CONDUCTIVITY = 1.4e7
        private const val ALUMINUM_CONDUCTIVITY = 3.5e7
        private const val COPPER_CONDUCTIVITY = 5.96e7
    }
    
    private val autobalanceCalculator = EMFADProtocol.AutobalanceCalculator()
    private val lineMeasurement = EMFADProtocol.LineMeasurement()
    private val tarEMFProtocol = EMFADProtocol.TarEMFProtocol()
    
    /**
     * Vollständige EMFAD-Analyse durchführen
     */
    fun performEMFADAnalysis(readings: List<EMFReading>): MaterialAnalysis? {
        if (readings.isEmpty()) return null
        
        try {
            Log.d(TAG, "Starte EMFAD-Analyse mit ${readings.size} Messungen")
            
            // Autobalance anwenden (aus EMUNI-X-07.exe)
            val balancedReadings = applyAutobalance(readings)
            
            // TAR-EMF Analyse (aus HzEMSoft.exe)
            val tarEmfResult = performTarEMFAnalysis(balancedReadings)
            
            // LINE-Modus Analyse (falls aktiviert)
            val lineAnalysis = performLineAnalysis(balancedReadings)
            
            // Material-Klassifikation mit EMFAD-Algorithmen
            val materialType = classifyMaterialEMFAD(balancedReadings.last())
            
            // Tiefenanalyse mit Z-Coord Berechnungen
            val depthAnalysis = performDepthAnalysis(balancedReadings)
            
            // Anomalie-Erkennung
            val anomalies = detectAnomaliesEMFAD(balancedReadings)
            
            return createMaterialAnalysis(
                readings = balancedReadings,
                materialType = materialType,
                tarEmfResult = tarEmfResult,
                lineAnalysis = lineAnalysis,
                depthAnalysis = depthAnalysis,
                anomalies = anomalies
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei EMFAD-Analyse", e)
            return null
        }
    }
    
    /**
     * Autobalance anwenden (aus EMUNI-X-07.exe "autobalance values; version 1.0")
     */
    private fun applyAutobalance(readings: List<EMFReading>): List<EMFReading> {
        if (readings.size < 10) return readings
        
        // Kalibriere Autobalance mit ersten 10 Messungen
        autobalanceCalculator.calibrateAutobalance(readings.take(10))
        
        // Wende Autobalance auf alle Messungen an
        return readings.map { reading ->
            autobalanceCalculator.applyAutobalance(reading)
        }
    }
    
    /**
     * TAR-EMF Analyse (aus HzEMSoft.exe)
     */
    private fun performTarEMFAnalysis(readings: List<EMFReading>): TarEMFResult {
        val lastReading = readings.last()
        
        // Kalibrierte Signalstärke berechnen
        val calibratedSignal = applyCalibratedSignalStrength(
            lastReading.signalStrength, 
            lastReading.temperature
        )
        
        // Zieltiefe berechnen
        val targetDepth = calculateEMFADDepth(calibratedSignal)
        
        // Material-Konfidenz berechnen
        val materialConfidence = calculateMaterialConfidence(lastReading)
        
        return TarEMFResult(
            targetDepth = targetDepth,
            signalStrength = calibratedSignal,
            materialType = classifyMaterialEMFAD(lastReading),
            confidence = materialConfidence
        )
    }
    
    /**
     * LINE-Modus Analyse (aus HzEMSoft.exe)
     */
    private fun performLineAnalysis(readings: List<EMFReading>): LineAnalysisResult {
        // Füge Punkte zur Linienmessung hinzu
        readings.forEachIndexed { index, reading ->
            val distance = index * 0.1 // 10cm Abstand zwischen Punkten
            lineMeasurement.addPoint(distance, reading)
        }
        
        // Erkenne Anomalien entlang der Linie
        val anomalies = lineMeasurement.detectAnomalies(threshold = 2.0)
        
        // Berechne Linienprofil-Statistiken
        val lineProfile = lineMeasurement.getLineProfile()
        val averageSignal = lineProfile.map { it.reading.signalStrength }.average()
        val maxSignal = lineProfile.maxOfOrNull { it.reading.signalStrength } ?: 0.0
        val minSignal = lineProfile.minOfOrNull { it.reading.signalStrength } ?: 0.0
        
        return LineAnalysisResult(
            totalPoints = lineProfile.size,
            anomalyCount = anomalies.size,
            averageSignal = averageSignal,
            maxSignal = maxSignal,
            minSignal = minSignal,
            signalVariation = maxSignal - minSignal,
            anomalyPositions = anomalies.map { it.distance }
        )
    }
    
    /**
     * Material-Klassifikation mit EMFAD-Algorithmen
     */
    private fun classifyMaterialEMFAD(reading: EMFReading): MaterialType {
        val calibratedSignal = applyCalibratedSignalStrength(reading.signalStrength, reading.temperature)
        val depth = calculateEMFADDepth(calibratedSignal)
        val signalRatio = calibratedSignal / reading.frequency
        val phaseShift = reading.phase
        
        // EMFAD-spezifische Klassifikationslogik (aus EXE-Analyse)
        return when {
            // Eisen: Hohe Signalstärke, geringe Tiefe, charakteristische Phase
            signalRatio > 10.0 && depth < 2.0 && phaseShift > 45.0 -> MaterialType.IRON
            
            // Stahl: Mittlere Signalstärke, mittlere Tiefe
            signalRatio > 5.0 && depth < 3.0 && phaseShift > 30.0 -> MaterialType.STEEL
            
            // Aluminium: Mittlere Signalstärke, größere Tiefe, niedrige Phase
            signalRatio > 2.0 && depth < 5.0 && phaseShift < 30.0 -> MaterialType.ALUMINUM
            
            // Kupfer: Hohe Leitfähigkeit, charakteristische Signatur
            signalRatio > 1.0 && depth < 8.0 && phaseShift < 45.0 -> MaterialType.COPPER
            
            // Bronze: Ähnlich Kupfer aber schwächeres Signal
            signalRatio > 0.5 && depth < 10.0 && phaseShift < 40.0 -> MaterialType.BRONZE
            
            // Gold: Sehr hohe Leitfähigkeit, spezifische Signatur
            signalRatio > 8.0 && depth < 1.5 && phaseShift < 20.0 -> MaterialType.GOLD
            
            // Silber: Höchste Leitfähigkeit
            signalRatio > 12.0 && depth < 1.0 && phaseShift < 15.0 -> MaterialType.SILVER
            
            else -> MaterialType.UNKNOWN
        }
    }
    
    /**
     * Tiefenanalyse mit Z-Coord Berechnungen (aus EMFAD3.exe)
     */
    private fun performDepthAnalysis(readings: List<EMFReading>): DepthAnalysisResult {
        val depths = readings.map { reading ->
            calculateEMFADDepth(applyCalibratedSignalStrength(reading.signalStrength, reading.temperature))
        }
        
        val averageDepth = depths.average()
        val maxDepth = depths.maxOrNull() ?: 0.0
        val minDepth = depths.minOrNull() ?: 0.0
        val depthVariation = maxDepth - minDepth
        
        // Z-Coord Berechnung (aus EMFAD3.exe)
        val zCoords = readings.mapIndexed { index, reading ->
            calculateZCoordinate(reading, index.toDouble())
        }
        
        return DepthAnalysisResult(
            averageDepth = averageDepth,
            maxDepth = maxDepth,
            minDepth = minDepth,
            depthVariation = depthVariation,
            zCoordinates = zCoords,
            depthProfile = depths
        )
    }
    
    /**
     * Anomalie-Erkennung mit EMFAD-Algorithmen
     */
    private fun detectAnomaliesEMFAD(readings: List<EMFReading>): List<AnomalyResult> {
        val anomalies = mutableListOf<AnomalyResult>()
        
        if (readings.size < 3) return anomalies
        
        val signals = readings.map { 
            applyCalibratedSignalStrength(it.signalStrength, it.temperature) 
        }
        
        val averageSignal = signals.average()
        val standardDeviation = calculateStandardDeviation(signals)
        val threshold = 2.0 * standardDeviation
        
        readings.forEachIndexed { index, reading ->
            val calibratedSignal = applyCalibratedSignalStrength(reading.signalStrength, reading.temperature)
            val deviation = abs(calibratedSignal - averageSignal)
            
            if (deviation > threshold) {
                anomalies.add(
                    AnomalyResult(
                        position = index,
                        timestamp = reading.timestamp,
                        signalStrength = calibratedSignal,
                        deviation = deviation,
                        severity = when {
                            deviation > 3.0 * standardDeviation -> "HIGH"
                            deviation > 2.5 * standardDeviation -> "MEDIUM"
                            else -> "LOW"
                        },
                        type = determineAnomalyType(reading, calibratedSignal, averageSignal)
                    )
                )
            }
        }
        
        return anomalies
    }
    
    // EMFAD-Hilfsfunktionen
    
    private fun applyCalibratedSignalStrength(rawSignal: Double, temperature: Double): Double {
        val calibrationFactor = CALIBRATION_CONSTANT / 1000.0
        val tempCompensation = 1.0 + (temperature - REFERENCE_TEMPERATURE) * TEMP_COEFFICIENT
        return rawSignal * calibrationFactor * tempCompensation
    }
    
    private fun calculateEMFADDepth(calibratedSignal: Double): Double {
        if (calibratedSignal <= 0) return 0.0
        return -ln(calibratedSignal / 1000.0) / ATTENUATION_FACTOR
    }
    
    private fun calculateZCoordinate(reading: EMFReading, position: Double): Double {
        // Z-Coord Berechnung aus EMFAD3.exe
        val depth = calculateEMFADDepth(applyCalibratedSignalStrength(reading.signalStrength, reading.temperature))
        return position * cos(reading.phase * PI / 180.0) + depth * sin(reading.phase * PI / 180.0)
    }
    
    private fun calculateMaterialConfidence(reading: EMFReading): Double {
        val signalQuality = 1.0 - (reading.noiseLevel / reading.signalStrength)
        val temperatureStability = 1.0 - abs(reading.temperature - REFERENCE_TEMPERATURE) / 50.0
        val phaseConsistency = 1.0 - abs(reading.phase % 90.0) / 90.0
        
        return (signalQuality + temperatureStability + phaseConsistency) / 3.0
    }
    
    private fun calculateStandardDeviation(values: List<Double>): Double {
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }
    
    private fun determineAnomalyType(reading: EMFReading, signal: Double, average: Double): String {
        return when {
            signal > average * 1.5 -> "STRONG_SIGNAL"
            signal < average * 0.5 -> "WEAK_SIGNAL"
            reading.phase > 180.0 -> "PHASE_ANOMALY"
            reading.temperature > REFERENCE_TEMPERATURE + 20.0 -> "TEMPERATURE_ANOMALY"
            else -> "UNKNOWN"
        }
    }
    
    private fun createMaterialAnalysis(
        readings: List<EMFReading>,
        materialType: MaterialType,
        tarEmfResult: TarEMFResult,
        lineAnalysis: LineAnalysisResult,
        depthAnalysis: DepthAnalysisResult,
        anomalies: List<AnomalyResult>
    ): MaterialAnalysis {
        val lastReading = readings.last()
        
        return MaterialAnalysis(
            sessionId = lastReading.sessionId,
            timestamp = System.currentTimeMillis(),
            materialType = materialType,
            confidence = tarEmfResult.confidence,
            
            // EMFAD-spezifische Eigenschaften
            signalStrength = tarEmfResult.signalStrength,
            depth = tarEmfResult.targetDepth,
            symmetryScore = lineAnalysis.signalVariation,
            hollownessScore = depthAnalysis.depthVariation,
            conductivity = calculateConductivityFromMaterial(materialType),
            magneticPermeability = calculatePermeabilityFromMaterial(materialType),
            
            // Weitere Eigenschaften...
            size = depthAnalysis.averageDepth,
            particleDensity = tarEmfResult.signalStrength / tarEmfResult.targetDepth,
            crystallineStructure = materialType != MaterialType.UNKNOWN,
            crystalSymmetry = "cubic",
            latticeParameter = 0.0,
            grainSize = 0.0,
            crystallineOrientation = "random",
            
            clusterCount = anomalies.size,
            clusterDensity = anomalies.size.toDouble() / readings.size,
            clusterSeparation = 0.0,
            clusterCoherence = 0.0,
            clusterData = "",
            
            skinDepth = calculateSkinDepth(lastReading),
            impedanceReal = lastReading.realPart,
            impedanceImaginary = lastReading.imaginaryPart,
            frequencyResponse = "",
            
            cavityDetected = depthAnalysis.depthVariation > 2.0,
            cavityVolume = if (depthAnalysis.depthVariation > 2.0) depthAnalysis.depthVariation else 0.0,
            cavityDepth = depthAnalysis.maxDepth,
            cavityShape = "unknown",
            cavityOrientation = "vertical",
            
            layerCount = 1,
            layerThickness = "",
            layerMaterials = "",
            layerInterfaces = "",
            
            inclusionCount = anomalies.count { it.type == "STRONG_SIGNAL" },
            inclusionTypes = "",
            inclusionSizes = "",
            inclusionPositions = "",
            
            analysisQuality = tarEmfResult.confidence,
            dataCompleteness = 1.0,
            measurementStability = 1.0 - (lineAnalysis.signalVariation / lineAnalysis.averageSignal),
            noiseLevel = lastReading.noiseLevel,
            calibrationAccuracy = 1.0,
            
            algorithmVersion = "EMFAD-1.0.0",
            processingTime = 0L,
            rawAnalysisData = "",
            notes = "EMFAD-Analyse basierend auf reverse engineering",
            isValidated = false,
            validatedBy = "",
            validationTimestamp = null
        )
    }
    
    private fun calculateConductivityFromMaterial(materialType: MaterialType): Double {
        return when (materialType) {
            MaterialType.IRON -> IRON_CONDUCTIVITY
            MaterialType.STEEL -> STEEL_CONDUCTIVITY
            MaterialType.ALUMINUM -> ALUMINUM_CONDUCTIVITY
            MaterialType.COPPER -> COPPER_CONDUCTIVITY
            else -> 1.0e6
        }
    }
    
    private fun calculatePermeabilityFromMaterial(materialType: MaterialType): Double {
        return when (materialType) {
            MaterialType.IRON -> 5000.0 * 4 * PI * 1e-7
            MaterialType.STEEL -> 1000.0 * 4 * PI * 1e-7
            else -> 4 * PI * 1e-7
        }
    }
    
    private fun calculateSkinDepth(reading: EMFReading): Double {
        val omega = 2 * PI * reading.frequency
        val conductivity = 1.0e7 // Annahme
        val mu = 4 * PI * 1e-7
        return sqrt(2 / (omega * mu * conductivity))
    }
}

// Datenklassen für EMFAD-Analyse
data class TarEMFResult(
    val targetDepth: Double,
    val signalStrength: Double,
    val materialType: MaterialType,
    val confidence: Double
)

data class LineAnalysisResult(
    val totalPoints: Int,
    val anomalyCount: Int,
    val averageSignal: Double,
    val maxSignal: Double,
    val minSignal: Double,
    val signalVariation: Double,
    val anomalyPositions: List<Double>
)

data class DepthAnalysisResult(
    val averageDepth: Double,
    val maxDepth: Double,
    val minDepth: Double,
    val depthVariation: Double,
    val zCoordinates: List<Double>,
    val depthProfile: List<Double>
)

data class AnomalyResult(
    val position: Int,
    val timestamp: Long,
    val signalStrength: Double,
    val deviation: Double,
    val severity: String,
    val type: String
)
