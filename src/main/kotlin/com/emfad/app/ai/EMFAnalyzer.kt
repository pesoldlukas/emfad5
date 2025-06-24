package com.emfad.app.ai

import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*
import com.emfad.app.models.*

/**
 * AI-basierter EMF-Analyzer mit TensorFlow Lite
 * Implementiert Machine Learning Algorithmen für EMF-Datenanalyse
 */
class EMFAnalyzer {
    private var interpreter: Interpreter? = null
    private val inputSize = 100 // Anzahl der Eingabewerte
    private val outputSize = 3  // Anzahl der Ausgabeklassen
    
    /**
     * Initialisiert den TensorFlow Lite Interpreter
     */
    fun initialize(modelBuffer: ByteBuffer): Boolean {
        return try {
            interpreter = Interpreter(modelBuffer)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Analysiert EMF-Daten mit AI-Algorithmus
     */
    fun analyzeEMFData(measurements: List<Float>): EMFAnalysisResult {
        val processedData = preprocessData(measurements)
        val prediction = runInference(processedData)
        return interpretResults(prediction, measurements)
    }
    
    /**
     * Datenvorverarbeitung für AI-Modell
     */
    private fun preprocessData(measurements: List<Float>): FloatArray {
        val normalized = normalizeData(measurements)
        val features = extractFeatures(normalized)
        return padOrTruncate(features, inputSize)
    }
    
    /**
     * Normalisierung der Messdaten
     */
    private fun normalizeData(data: List<Float>): List<Float> {
        val max = data.maxOrNull() ?: 1f
        val min = data.minOrNull() ?: 0f
        val range = max - min
        
        return if (range > 0) {
            data.map { (it - min) / range }
        } else {
            data.map { 0.5f }
        }
    }
    
    /**
     * Feature-Extraktion aus EMF-Daten
     */
    private fun extractFeatures(data: List<Float>): FloatArray {
        val features = mutableListOf<Float>()
        
        // Statistische Features
        features.add(data.average().toFloat())
        features.add(calculateStandardDeviation(data))
        features.add(data.maxOrNull() ?: 0f)
        features.add(data.minOrNull() ?: 0f)
        
        // Frequenz-Domain Features (vereinfacht)
        features.addAll(calculateFFTFeatures(data))
        
        // Zeitbereich Features
        features.addAll(calculateTimeFeatures(data))
        
        return features.toFloatArray()
    }
    
    /**
     * Berechnung der Standardabweichung
     */
    private fun calculateStandardDeviation(data: List<Float>): Float {
        val mean = data.average()
        val variance = data.map { (it - mean).pow(2) }.average()
        return sqrt(variance).toFloat()
    }
    
    /**
     * Vereinfachte FFT-Features
     */
    private fun calculateFFTFeatures(data: List<Float>): List<Float> {
        val features = mutableListOf<Float>()
        
        // Vereinfachte Frequenzanalyse
        for (i in 1..10) {
            val frequency = i * 2.0 * PI / data.size
            var real = 0.0
            var imag = 0.0
            
            for (j in data.indices) {
                val angle = frequency * j
                real += data[j] * cos(angle)
                imag += data[j] * sin(angle)
            }
            
            val magnitude = sqrt(real * real + imag * imag).toFloat()
            features.add(magnitude)
        }
        
        return features
    }
    
    /**
     * Zeitbereich-Features
     */
    private fun calculateTimeFeatures(data: List<Float>): List<Float> {
        val features = mutableListOf<Float>()
        
        // Gradient-Features
        val gradients = data.zipWithNext { a, b -> b - a }
        features.add(gradients.average().toFloat())
        features.add(calculateStandardDeviation(gradients))
        
        // Peak-Detection
        val peaks = detectPeaks(data)
        features.add(peaks.size.toFloat())
        
        // Zero-Crossings
        val zeroCrossings = countZeroCrossings(data)
        features.add(zeroCrossings.toFloat())
        
        return features
    }
    
    /**
     * Peak-Detection Algorithmus
     */
    private fun detectPeaks(data: List<Float>): List<Int> {
        val peaks = mutableListOf<Int>()
        val threshold = calculateStandardDeviation(data) * 2
        
        for (i in 1 until data.size - 1) {
            if (data[i] > data[i-1] && data[i] > data[i+1] && data[i] > threshold) {
                peaks.add(i)
            }
        }
        
        return peaks
    }
    
    /**
     * Zero-Crossing Zählung
     */
    private fun countZeroCrossings(data: List<Float>): Int {
        var count = 0
        val mean = data.average().toFloat()
        
        for (i in 1 until data.size) {
            if ((data[i-1] - mean) * (data[i] - mean) < 0) {
                count++
            }
        }
        
        return count
    }
    
    /**
     * Padding oder Truncation auf gewünschte Größe
     */
    private fun padOrTruncate(data: FloatArray, targetSize: Int): FloatArray {
        return when {
            data.size == targetSize -> data
            data.size > targetSize -> data.sliceArray(0 until targetSize)
            else -> {
                val padded = FloatArray(targetSize)
                data.copyInto(padded)
                padded
            }
        }
    }
    
    /**
     * TensorFlow Lite Inferenz
     */
    private fun runInference(input: FloatArray): FloatArray {
        val interpreter = this.interpreter ?: return FloatArray(outputSize) { 0f }
        
        val inputBuffer = ByteBuffer.allocateDirect(input.size * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        input.forEach { inputBuffer.putFloat(it) }
        
        val outputBuffer = ByteBuffer.allocateDirect(outputSize * 4)
        outputBuffer.order(ByteOrder.nativeOrder())
        
        interpreter.run(inputBuffer, outputBuffer)
        
        outputBuffer.rewind()
        return FloatArray(outputSize) { outputBuffer.float }
    }
    
    /**
     * Interpretation der AI-Ergebnisse
     */
    private fun interpretResults(prediction: FloatArray, originalData: List<Float>): EMFAnalysisResult {
        val maxIndex = prediction.indices.maxByOrNull { prediction[it] } ?: 0
        val confidence = prediction[maxIndex]
        
        val classification = when (maxIndex) {
            0 -> EMFClassification.NORMAL
            1 -> EMFClassification.ELEVATED
            2 -> EMFClassification.CRITICAL
            else -> EMFClassification.UNKNOWN
        }
        
        val anomalies = detectAnomalies(originalData)
        val patterns = identifyPatterns(originalData)
        
        return EMFAnalysisResult(
            classification = classification,
            confidence = confidence,
            anomalies = anomalies,
            patterns = patterns,
            recommendations = generateRecommendations(classification, confidence)
        )
    }
    
    /**
     * Anomalie-Erkennung
     */
    private fun detectAnomalies(data: List<Float>): List<AnomalyPoint> {
        val anomalies = mutableListOf<AnomalyPoint>()
        val threshold = calculateStandardDeviation(data) * 3
        val mean = data.average().toFloat()
        
        data.forEachIndexed { index, value ->
            if (abs(value - mean) > threshold) {
                anomalies.add(
                    AnomalyPoint(
                        index = index,
                        value = value,
                        severity = when {
                            abs(value - mean) > threshold * 2 -> AnomalySeverity.HIGH
                            abs(value - mean) > threshold * 1.5 -> AnomalySeverity.MEDIUM
                            else -> AnomalySeverity.LOW
                        }
                    )
                )
            }
        }
        
        return anomalies
    }
    
    /**
     * Muster-Identifikation
     */
    private fun identifyPatterns(data: List<Float>): List<EMFPattern> {
        val patterns = mutableListOf<EMFPattern>()
        
        // Periodische Muster
        val periodicPattern = detectPeriodicPattern(data)
        if (periodicPattern != null) {
            patterns.add(periodicPattern)
        }
        
        // Trend-Muster
        val trendPattern = detectTrendPattern(data)
        if (trendPattern != null) {
            patterns.add(trendPattern)
        }
        
        return patterns
    }
    
    /**
     * Periodische Muster erkennen
     */
    private fun detectPeriodicPattern(data: List<Float>): EMFPattern? {
        // Vereinfachte Autokorrelation
        val maxLag = min(data.size / 4, 50)
        var bestLag = 0
        var maxCorrelation = 0.0
        
        for (lag in 1..maxLag) {
            val correlation = calculateAutoCorrelation(data, lag)
            if (correlation > maxCorrelation) {
                maxCorrelation = correlation
                bestLag = lag
            }
        }
        
        return if (maxCorrelation > 0.7) {
            EMFPattern(
                type = PatternType.PERIODIC,
                period = bestLag,
                strength = maxCorrelation.toFloat(),
                description = "Periodisches Muster mit Periode $bestLag"
            )
        } else null
    }
    
    /**
     * Autokorrelation berechnen
     */
    private fun calculateAutoCorrelation(data: List<Float>, lag: Int): Double {
        if (lag >= data.size) return 0.0
        
        val n = data.size - lag
        val mean = data.average()
        
        var numerator = 0.0
        var denominator = 0.0
        
        for (i in 0 until n) {
            numerator += (data[i] - mean) * (data[i + lag] - mean)
            denominator += (data[i] - mean).pow(2)
        }
        
        return if (denominator > 0) numerator / denominator else 0.0
    }
    
    /**
     * Trend-Muster erkennen
     */
    private fun detectTrendPattern(data: List<Float>): EMFPattern? {
        val n = data.size
        val x = (0 until n).map { it.toDouble() }
        val y = data.map { it.toDouble() }
        
        // Lineare Regression
        val xMean = x.average()
        val yMean = y.average()
        
        var numerator = 0.0
        var denominator = 0.0
        
        for (i in x.indices) {
            numerator += (x[i] - xMean) * (y[i] - yMean)
            denominator += (x[i] - xMean).pow(2)
        }
        
        val slope = if (denominator > 0) numerator / denominator else 0.0
        
        return if (abs(slope) > 0.01) {
            EMFPattern(
                type = PatternType.TREND,
                period = 0,
                strength = abs(slope).toFloat(),
                description = when {
                    slope > 0 -> "Steigender Trend"
                    else -> "Fallender Trend"
                }
            )
        } else null
    }
    
    /**
     * Empfehlungen generieren
     */
    private fun generateRecommendations(classification: EMFClassification, confidence: Float): List<String> {
        val recommendations = mutableListOf<String>()
        
        when (classification) {
            EMFClassification.NORMAL -> {
                recommendations.add("EMF-Werte im normalen Bereich")
                recommendations.add("Regelmäßige Überwachung empfohlen")
            }
            EMFClassification.ELEVATED -> {
                recommendations.add("Erhöhte EMF-Werte erkannt")
                recommendations.add("Quellen identifizieren und minimieren")
                recommendations.add("Häufigere Messungen durchführen")
            }
            EMFClassification.CRITICAL -> {
                recommendations.add("Kritische EMF-Werte!")
                recommendations.add("Sofortige Maßnahmen erforderlich")
                recommendations.add("Professionelle Beratung einholen")
            }
            EMFClassification.UNKNOWN -> {
                recommendations.add("Unklare Messwerte")
                recommendations.add("Kalibrierung überprüfen")
            }
        }
        
        if (confidence < 0.7f) {
            recommendations.add("Niedrige Vorhersagegenauigkeit - mehr Daten sammeln")
        }
        
        return recommendations
    }
    
    /**
     * Ressourcen freigeben
     */
    fun cleanup() {
        interpreter?.close()
        interpreter = null
    }
}

/**
 * Datenklassen für AI-Analyse
 */
data class EMFAnalysisResult(
    val classification: EMFClassification,
    val confidence: Float,
    val anomalies: List<AnomalyPoint>,
    val patterns: List<EMFPattern>,
    val recommendations: List<String>
)

data class AnomalyPoint(
    val index: Int,
    val value: Float,
    val severity: AnomalySeverity
)

data class EMFPattern(
    val type: PatternType,
    val period: Int,
    val strength: Float,
    val description: String
)

enum class EMFClassification {
    NORMAL, ELEVATED, CRITICAL, UNKNOWN
}

// Enums wurden nach com.emfad.app.models verschoben
