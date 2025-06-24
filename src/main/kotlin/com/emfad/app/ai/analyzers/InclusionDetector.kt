package com.emfad.app.ai.analyzers

import android.util.Log
import com.emfad.app.models.EMFReading
import com.google.gson.Gson
import kotlin.math.*

/**
 * Einschluss-Detektor für EMFAD
 * Basiert auf ursprünglichen Algorithmen aus Chat-File
 * Samsung S21 Ultra optimiert
 */
class InclusionDetector {
    
    companion object {
        private const val TAG = "InclusionDetector"
        private const val INCLUSION_THRESHOLD = 0.3
        private const val MIN_INCLUSION_SIZE = 0.1
        private const val MAX_INCLUSION_SIZE = 10.0
        private const val SIGNAL_ANOMALY_THRESHOLD = 0.2
    }
    
    private val gson = Gson()
    
    /**
     * Einschluss-Datenstruktur
     */
    data class Inclusion(
        val id: Int,
        val type: InclusionType,
        val position: Position3D,
        val size: Double,
        val volume: Double,
        val signalStrength: Double,
        val confidence: Double,
        val shape: InclusionShape,
        val materialType: String,
        val depth: Double,
        val orientation: String,
        val boundingBox: BoundingBox
    )
    
    /**
     * Einschluss-Typ
     */
    enum class InclusionType {
        METALLIC,
        NON_METALLIC,
        VOID,
        CRACK,
        FOREIGN_MATERIAL,
        CORROSION,
        UNKNOWN
    }
    
    /**
     * Einschluss-Form
     */
    enum class InclusionShape {
        SPHERICAL,
        CYLINDRICAL,
        PLANAR,
        IRREGULAR,
        LINEAR,
        UNKNOWN
    }
    
    /**
     * 3D Position
     */
    data class Position3D(
        val x: Double,
        val y: Double,
        val z: Double
    )
    
    /**
     * Bounding Box
     */
    data class BoundingBox(
        val minX: Double,
        val maxX: Double,
        val minY: Double,
        val maxY: Double,
        val minZ: Double,
        val maxZ: Double
    )
    
    /**
     * Einschluss-Erkennungsergebnis
     */
    data class InclusionDetectionResult(
        val inclusionCount: Int,
        val inclusions: List<Inclusion>,
        val totalVolume: Double,
        val averageSize: Double,
        val detectionConfidence: Double,
        val analysisQuality: Double,
        val processingTime: Long
    )
    
    /**
     * Einschlüsse in EMF-Messungen erkennen
     */
    fun detectInclusions(readings: List<EMFReading>): InclusionDetectionResult {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "Starte Einschluss-Erkennung mit ${readings.size} Messungen")
        
        if (readings.isEmpty()) {
            return InclusionDetectionResult(0, emptyList(), 0.0, 0.0, 0.0, 0.0, 0L)
        }
        
        // Signal-Anomalien identifizieren
        val anomalies = identifySignalAnomalies(readings)
        
        // Einschlüsse aus Anomalien extrahieren
        val inclusions = extractInclusionsFromAnomalies(anomalies, readings)
        
        // Einschlüsse klassifizieren
        val classifiedInclusions = classifyInclusions(inclusions, readings)
        
        // Ergebnis zusammenstellen
        val processingTime = System.currentTimeMillis() - startTime
        val result = InclusionDetectionResult(
            inclusionCount = classifiedInclusions.size,
            inclusions = classifiedInclusions,
            totalVolume = classifiedInclusions.sumOf { it.volume },
            averageSize = if (classifiedInclusions.isNotEmpty()) classifiedInclusions.map { it.size }.average() else 0.0,
            detectionConfidence = calculateDetectionConfidence(classifiedInclusions),
            analysisQuality = calculateAnalysisQuality(readings, classifiedInclusions),
            processingTime = processingTime
        )
        
        Log.d(TAG, "Einschluss-Erkennung abgeschlossen: ${result.inclusionCount} Einschlüsse gefunden")
        return result
    }
    
    /**
     * Signal-Anomalien identifizieren
     */
    private fun identifySignalAnomalies(readings: List<EMFReading>): List<SignalAnomaly> {
        val anomalies = mutableListOf<SignalAnomaly>()
        
        // Baseline-Statistiken berechnen
        val avgSignalStrength = readings.map { it.signalStrength }.average()
        val stdSignalStrength = calculateStandardDeviation(readings.map { it.signalStrength })
        val avgPhase = readings.map { it.phase }.average()
        val stdPhase = calculateStandardDeviation(readings.map { it.phase })
        
        // Anomalien basierend auf statistischen Abweichungen finden
        readings.forEachIndexed { index, reading ->
            val signalDeviation = abs(reading.signalStrength - avgSignalStrength) / stdSignalStrength
            val phaseDeviation = abs(reading.phase - avgPhase) / stdPhase
            
            if (signalDeviation > SIGNAL_ANOMALY_THRESHOLD || phaseDeviation > SIGNAL_ANOMALY_THRESHOLD) {
                anomalies.add(
                    SignalAnomaly(
                        index = index,
                        reading = reading,
                        signalDeviation = signalDeviation,
                        phaseDeviation = phaseDeviation,
                        anomalyScore = max(signalDeviation, phaseDeviation)
                    )
                )
            }
        }
        
        // Räumliche Clustering der Anomalien
        return clusterAnomalies(anomalies)
    }
    
    /**
     * Anomalien räumlich clustern
     */
    private fun clusterAnomalies(anomalies: List<SignalAnomaly>): List<SignalAnomaly> {
        // Einfaches räumliches Clustering basierend auf Position
        val clusteredAnomalies = mutableListOf<SignalAnomaly>()
        val processed = mutableSetOf<Int>()
        
        for (i in anomalies.indices) {
            if (i in processed) continue
            
            val currentAnomaly = anomalies[i]
            val cluster = mutableListOf(currentAnomaly)
            processed.add(i)
            
            // Nachbarn finden
            for (j in i + 1 until anomalies.size) {
                if (j in processed) continue
                
                val otherAnomaly = anomalies[j]
                val distance = calculateSpatialDistance(currentAnomaly.reading, otherAnomaly.reading)
                
                if (distance < 1.0) { // 1 Meter Clustering-Radius
                    cluster.add(otherAnomaly)
                    processed.add(j)
                }
            }
            
            // Cluster-Repräsentant erstellen
            if (cluster.size >= 2) {
                val avgAnomaly = createAverageAnomaly(cluster)
                clusteredAnomalies.add(avgAnomaly)
            } else {
                clusteredAnomalies.add(currentAnomaly)
            }
        }
        
        return clusteredAnomalies
    }
    
    /**
     * Einschlüsse aus Anomalien extrahieren
     */
    private fun extractInclusionsFromAnomalies(
        anomalies: List<SignalAnomaly>,
        readings: List<EMFReading>
    ): List<Inclusion> {
        val inclusions = mutableListOf<Inclusion>()
        
        anomalies.forEachIndexed { index, anomaly ->
            val reading = anomaly.reading
            
            // Einschluss-Größe basierend auf Signal-Anomalie schätzen
            val size = estimateInclusionSize(anomaly, readings)
            
            if (size >= MIN_INCLUSION_SIZE && size <= MAX_INCLUSION_SIZE) {
                val inclusion = Inclusion(
                    id = index,
                    type = InclusionType.UNKNOWN, // Wird später klassifiziert
                    position = Position3D(reading.positionX, reading.positionY, reading.positionZ),
                    size = size,
                    volume = calculateInclusionVolume(size, InclusionShape.SPHERICAL),
                    signalStrength = reading.signalStrength,
                    confidence = anomaly.anomalyScore,
                    shape = estimateInclusionShape(anomaly, readings),
                    materialType = "unknown",
                    depth = reading.depth,
                    orientation = estimateInclusionOrientation(anomaly, readings),
                    boundingBox = calculateBoundingBox(reading, size)
                )
                
                inclusions.add(inclusion)
            }
        }
        
        return inclusions
    }
    
    /**
     * Einschlüsse klassifizieren
     */
    private fun classifyInclusions(
        inclusions: List<Inclusion>,
        readings: List<EMFReading>
    ): List<Inclusion> {
        return inclusions.map { inclusion ->
            val type = classifyInclusionType(inclusion, readings)
            val materialType = estimateMaterialType(inclusion, readings)
            
            inclusion.copy(
                type = type,
                materialType = materialType
            )
        }
    }
    
    /**
     * Einschluss-Typ klassifizieren
     */
    private fun classifyInclusionType(inclusion: Inclusion, readings: List<EMFReading>): InclusionType {
        val signalStrength = inclusion.signalStrength
        val depth = inclusion.depth
        
        return when {
            signalStrength > 800.0 -> InclusionType.METALLIC
            signalStrength < 200.0 && depth > 5.0 -> InclusionType.VOID
            signalStrength < 300.0 -> InclusionType.NON_METALLIC
            inclusion.shape == InclusionShape.LINEAR -> InclusionType.CRACK
            signalStrength in 300.0..500.0 -> InclusionType.FOREIGN_MATERIAL
            else -> InclusionType.UNKNOWN
        }
    }
    
    /**
     * Einschluss-Größe schätzen
     */
    private fun estimateInclusionSize(anomaly: SignalAnomaly, readings: List<EMFReading>): Double {
        // Größe basierend auf Signal-Anomalie und Frequenz
        val reading = anomaly.reading
        val wavelength = 3e8 / reading.frequency // Wellenlänge in Metern
        val anomalyFactor = anomaly.anomalyScore
        
        return wavelength * anomalyFactor * 0.1 // Empirische Formel
    }
    
    /**
     * Einschluss-Form schätzen
     */
    private fun estimateInclusionShape(anomaly: SignalAnomaly, readings: List<EMFReading>): InclusionShape {
        val reading = anomaly.reading
        val aspectRatio = reading.depth / reading.signalStrength * 1000.0
        
        return when {
            aspectRatio > 5.0 -> InclusionShape.LINEAR
            aspectRatio > 2.0 -> InclusionShape.CYLINDRICAL
            aspectRatio < 0.5 -> InclusionShape.PLANAR
            aspectRatio in 0.8..1.2 -> InclusionShape.SPHERICAL
            else -> InclusionShape.IRREGULAR
        }
    }
    
    /**
     * Einschluss-Orientierung schätzen
     */
    private fun estimateInclusionOrientation(anomaly: SignalAnomaly, readings: List<EMFReading>): String {
        val reading = anomaly.reading
        val phase = reading.phase
        
        return when {
            phase < 90.0 -> "horizontal"
            phase > 270.0 -> "horizontal"
            phase in 90.0..270.0 -> "vertical"
            else -> "diagonal"
        }
    }
    
    /**
     * Material-Typ schätzen
     */
    private fun estimateMaterialType(inclusion: Inclusion, readings: List<EMFReading>): String {
        return when (inclusion.type) {
            InclusionType.METALLIC -> when {
                inclusion.signalStrength > 900.0 -> "iron"
                inclusion.signalStrength > 800.0 -> "steel"
                else -> "aluminum"
            }
            InclusionType.NON_METALLIC -> "ceramic"
            InclusionType.VOID -> "air"
            InclusionType.CRACK -> "air_gap"
            InclusionType.FOREIGN_MATERIAL -> "polymer"
            InclusionType.CORROSION -> "oxide"
            else -> "unknown"
        }
    }
    
    /**
     * Einschluss-Volumen berechnen
     */
    private fun calculateInclusionVolume(size: Double, shape: InclusionShape): Double {
        return when (shape) {
            InclusionShape.SPHERICAL -> (4.0 / 3.0) * PI * (size / 2.0).pow(3)
            InclusionShape.CYLINDRICAL -> PI * (size / 2.0).pow(2) * size
            InclusionShape.PLANAR -> size * size * (size * 0.1)
            InclusionShape.LINEAR -> PI * (size * 0.1).pow(2) * size
            else -> size.pow(3) // Würfel-Approximation
        }
    }
    
    /**
     * Bounding Box berechnen
     */
    private fun calculateBoundingBox(reading: EMFReading, size: Double): BoundingBox {
        val halfSize = size / 2.0
        return BoundingBox(
            minX = reading.positionX - halfSize,
            maxX = reading.positionX + halfSize,
            minY = reading.positionY - halfSize,
            maxY = reading.positionY + halfSize,
            minZ = reading.positionZ - halfSize,
            maxZ = reading.positionZ + halfSize
        )
    }
    
    // Hilfsfunktionen
    private fun calculateStandardDeviation(values: List<Double>): Double {
        val mean = values.average()
        val variance = values.map { (it - mean).pow(2) }.average()
        return sqrt(variance)
    }
    
    private fun calculateSpatialDistance(reading1: EMFReading, reading2: EMFReading): Double {
        return sqrt(
            (reading1.positionX - reading2.positionX).pow(2) +
            (reading1.positionY - reading2.positionY).pow(2) +
            (reading1.positionZ - reading2.positionZ).pow(2)
        )
    }
    
    private fun createAverageAnomaly(cluster: List<SignalAnomaly>): SignalAnomaly {
        val avgReading = cluster.first().reading // Vereinfachung
        val avgScore = cluster.map { it.anomalyScore }.average()
        return SignalAnomaly(
            index = cluster.first().index,
            reading = avgReading,
            signalDeviation = cluster.map { it.signalDeviation }.average(),
            phaseDeviation = cluster.map { it.phaseDeviation }.average(),
            anomalyScore = avgScore
        )
    }
    
    private fun calculateDetectionConfidence(inclusions: List<Inclusion>): Double {
        return if (inclusions.isNotEmpty()) {
            inclusions.map { it.confidence }.average()
        } else 0.0
    }
    
    private fun calculateAnalysisQuality(readings: List<EMFReading>, inclusions: List<Inclusion>): Double {
        val avgQuality = readings.map { it.qualityScore }.average()
        val detectionRate = inclusions.size.toDouble() / readings.size
        return avgQuality * (1.0 - detectionRate * 0.1) // Penalty für zu viele Detektionen
    }
    
    /**
     * Signal-Anomalie Datenklasse
     */
    private data class SignalAnomaly(
        val index: Int,
        val reading: EMFReading,
        val signalDeviation: Double,
        val phaseDeviation: Double,
        val anomalyScore: Double
    )
    
    /**
     * Ergebnis zu JSON konvertieren
     */
    fun resultToJson(result: InclusionDetectionResult): String {
        return gson.toJson(result)
    }
    
    /**
     * JSON zu Ergebnis konvertieren
     */
    fun jsonToResult(json: String): InclusionDetectionResult? {
        return try {
            gson.fromJson(json, InclusionDetectionResult::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Parsen des Einschluss-Ergebnisses", e)
            null
        }
    }
}
