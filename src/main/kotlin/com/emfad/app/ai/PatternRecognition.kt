package com.emfad.app.ai

import kotlin.math.*
import com.emfad.app.models.*

/**
 * Erweiterte Mustererkennung für EMF-Daten
 * Implementiert verschiedene Algorithmen zur Mustererkennung
 */
class PatternRecognition {
    
    /**
     * Hauptmethode für Mustererkennung
     */
    fun recognizePatterns(data: List<Float>): PatternRecognitionResult {
        val periodicPatterns = detectPeriodicPatterns(data)
        val spikes = detectSpikes(data)
        val trends = detectTrends(data)
        val clusters = detectClusters(data)
        val harmonics = detectHarmonics(data)
        
        return PatternRecognitionResult(
            periodicPatterns = periodicPatterns,
            spikes = spikes,
            trends = trends,
            clusters = clusters,
            harmonics = harmonics,
            complexity = calculateComplexity(data)
        )
    }
    
    /**
     * Erweiterte periodische Mustererkennung
     */
    private fun detectPeriodicPatterns(data: List<Float>): List<PeriodicPattern> {
        val patterns = mutableListOf<PeriodicPattern>()
        val maxPeriod = min(data.size / 3, 100)
        
        for (period in 2..maxPeriod) {
            val correlation = calculatePeriodCorrelation(data, period)
            if (correlation > 0.6) {
                val phase = calculatePhase(data, period)
                val amplitude = calculateAmplitude(data, period)
                
                patterns.add(
                    PeriodicPattern(
                        period = period,
                        correlation = correlation,
                        phase = phase,
                        amplitude = amplitude,
                        confidence = calculatePatternConfidence(correlation, amplitude)
                    )
                )
            }
        }
        
        return patterns.sortedByDescending { it.confidence }
    }
    
    /**
     * Periodenkorrelation berechnen
     */
    private fun calculatePeriodCorrelation(data: List<Float>, period: Int): Float {
        val segments = data.size / period
        if (segments < 2) return 0f
        
        var totalCorrelation = 0f
        var count = 0
        
        for (i in 0 until segments - 1) {
            val segment1 = data.subList(i * period, (i + 1) * period)
            val segment2 = data.subList((i + 1) * period, (i + 2) * period)
            
            totalCorrelation += calculateCorrelation(segment1, segment2)
            count++
        }
        
        return if (count > 0) totalCorrelation / count else 0f
    }
    
    /**
     * Korrelation zwischen zwei Segmenten
     */
    private fun calculateCorrelation(segment1: List<Float>, segment2: List<Float>): Float {
        if (segment1.size != segment2.size) return 0f
        
        val mean1 = segment1.average().toFloat()
        val mean2 = segment2.average().toFloat()
        
        var numerator = 0f
        var denominator1 = 0f
        var denominator2 = 0f
        
        for (i in segment1.indices) {
            val diff1 = segment1[i] - mean1
            val diff2 = segment2[i] - mean2
            
            numerator += diff1 * diff2
            denominator1 += diff1 * diff1
            denominator2 += diff2 * diff2
        }
        
        val denominator = sqrt(denominator1 * denominator2)
        return if (denominator > 0) numerator / denominator else 0f
    }
    
    /**
     * Phase berechnen
     */
    private fun calculatePhase(data: List<Float>, period: Int): Float {
        val segments = data.size / period
        if (segments < 1) return 0f
        
        val referenceSegment = data.subList(0, period)
        var bestPhase = 0f
        var maxCorrelation = -1f
        
        for (phase in 0 until period) {
            val shiftedSegment = shiftSegment(referenceSegment, phase)
            val correlation = calculateCorrelation(referenceSegment, shiftedSegment)
            
            if (correlation > maxCorrelation) {
                maxCorrelation = correlation
                bestPhase = phase.toFloat()
            }
        }
        
        return bestPhase
    }
    
    /**
     * Segment verschieben
     */
    private fun shiftSegment(segment: List<Float>, shift: Int): List<Float> {
        val shifted = mutableListOf<Float>()
        for (i in segment.indices) {
            shifted.add(segment[(i + shift) % segment.size])
        }
        return shifted
    }
    
    /**
     * Amplitude berechnen
     */
    private fun calculateAmplitude(data: List<Float>, period: Int): Float {
        val segments = data.size / period
        if (segments < 1) return 0f
        
        var totalAmplitude = 0f
        
        for (i in 0 until segments) {
            val segment = data.subList(i * period, min((i + 1) * period, data.size))
            val max = segment.maxOrNull() ?: 0f
            val min = segment.minOrNull() ?: 0f
            totalAmplitude += (max - min)
        }
        
        return totalAmplitude / segments
    }
    
    /**
     * Muster-Konfidenz berechnen
     */
    private fun calculatePatternConfidence(correlation: Float, amplitude: Float): Float {
        return (correlation * 0.7f + (amplitude / 100f) * 0.3f).coerceIn(0f, 1f)
    }
    
    /**
     * Spike-Erkennung
     */
    private fun detectSpikes(data: List<Float>): List<SpikePattern> {
        val spikes = mutableListOf<SpikePattern>()
        val threshold = calculateDynamicThreshold(data)
        val mean = data.average().toFloat()
        
        var i = 1
        while (i < data.size - 1) {
            val current = data[i]
            val prev = data[i - 1]
            val next = data[i + 1]
            
            // Positive Spike
            if (current > mean + threshold && current > prev && current > next) {
                val width = calculateSpikeWidth(data, i, threshold)
                val intensity = (current - mean) / threshold
                
                spikes.add(
                    SpikePattern(
                        position = i,
                        intensity = intensity,
                        width = width,
                        type = SpikeType.POSITIVE,
                        confidence = calculateSpikeConfidence(intensity, width)
                    )
                )
                
                i += width // Skip spike width
            }
            // Negative Spike
            else if (current < mean - threshold && current < prev && current < next) {
                val width = calculateSpikeWidth(data, i, threshold)
                val intensity = (mean - current) / threshold
                
                spikes.add(
                    SpikePattern(
                        position = i,
                        intensity = intensity,
                        width = width,
                        type = SpikeType.NEGATIVE,
                        confidence = calculateSpikeConfidence(intensity, width)
                    )
                )
                
                i += width // Skip spike width
            } else {
                i++
            }
        }
        
        return spikes
    }
    
    /**
     * Dynamischen Schwellwert berechnen
     */
    private fun calculateDynamicThreshold(data: List<Float>): Float {
        val std = calculateStandardDeviation(data)
        val mad = calculateMAD(data) // Median Absolute Deviation
        return max(std * 2f, mad * 3f)
    }
    
    /**
     * Median Absolute Deviation
     */
    private fun calculateMAD(data: List<Float>): Float {
        val median = data.sorted()[data.size / 2]
        val deviations = data.map { abs(it - median) }.sorted()
        return deviations[deviations.size / 2]
    }
    
    /**
     * Spike-Breite berechnen
     */
    private fun calculateSpikeWidth(data: List<Float>, center: Int, threshold: Float): Int {
        val mean = data.average().toFloat()
        var width = 1
        
        // Nach links erweitern
        var left = center - 1
        while (left >= 0 && abs(data[left] - mean) > threshold * 0.5f) {
            width++
            left--
        }
        
        // Nach rechts erweitern
        var right = center + 1
        while (right < data.size && abs(data[right] - mean) > threshold * 0.5f) {
            width++
            right++
        }
        
        return width
    }
    
    /**
     * Spike-Konfidenz berechnen
     */
    private fun calculateSpikeConfidence(intensity: Float, width: Int): Float {
        val intensityScore = (intensity / 5f).coerceIn(0f, 1f)
        val widthScore = (width.toFloat() / 10f).coerceIn(0f, 1f)
        return (intensityScore * 0.8f + widthScore * 0.2f)
    }
    
    /**
     * Trend-Erkennung
     */
    private fun detectTrends(data: List<Float>): List<TrendPattern> {
        val trends = mutableListOf<TrendPattern>()
        val windowSize = max(data.size / 10, 5)
        
        for (start in 0 until data.size - windowSize step windowSize / 2) {
            val end = min(start + windowSize, data.size)
            val segment = data.subList(start, end)
            
            val slope = calculateSlope(segment)
            val rSquared = calculateRSquared(segment, slope)
            
            if (rSquared > 0.7) {
                trends.add(
                    TrendPattern(
                        startIndex = start,
                        endIndex = end - 1,
                        slope = slope,
                        rSquared = rSquared,
                        type = when {
                            slope > 0.01 -> TrendType.INCREASING
                            slope < -0.01 -> TrendType.DECREASING
                            else -> TrendType.STABLE
                        },
                        confidence = rSquared
                    )
                )
            }
        }
        
        return mergeTrends(trends)
    }
    
    /**
     * Steigung berechnen
     */
    private fun calculateSlope(data: List<Float>): Float {
        val n = data.size
        val x = (0 until n).map { it.toFloat() }
        val y = data
        
        val xMean = x.average().toFloat()
        val yMean = y.average()
        
        var numerator = 0f
        var denominator = 0f
        
        for (i in x.indices) {
            numerator += (x[i] - xMean.toFloat()) * (y[i] - yMean.toFloat())
            denominator += (x[i] - xMean.toFloat()).pow(2)
        }
        
        return if (denominator > 0) numerator / denominator else 0f
    }
    
    /**
     * R-Quadrat berechnen
     */
    private fun calculateRSquared(data: List<Float>, slope: Float): Float {
        val n = data.size
        val yMean = data.average()
        
        var ssRes = 0f // Sum of squares of residuals
        var ssTot = 0f // Total sum of squares
        
        for (i in data.indices) {
            val predicted = slope * i + (yMean.toFloat() - slope * (n - 1) / 2)
            ssRes += (data[i] - predicted).pow(2)
            ssTot += (data[i] - yMean.toFloat()).pow(2)
        }
        
        return if (ssTot > 0) 1f - (ssRes / ssTot) else 0f
    }
    
    /**
     * Trends zusammenführen
     */
    private fun mergeTrends(trends: List<TrendPattern>): List<TrendPattern> {
        if (trends.isEmpty()) return trends
        
        val merged = mutableListOf<TrendPattern>()
        var current = trends[0]
        
        for (i in 1 until trends.size) {
            val next = trends[i]
            
            // Trends zusammenführen wenn sie ähnlich sind und sich überlappen
            if (current.endIndex >= next.startIndex - 5 &&
                current.type == next.type &&
                abs(current.slope - next.slope) < 0.005) {
                
                current = current.copy(
                    endIndex = next.endIndex,
                    slope = (current.slope + next.slope) / 2,
                    rSquared = (current.rSquared + next.rSquared) / 2,
                    confidence = (current.confidence + next.confidence) / 2
                )
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        
        return merged
    }
    
    /**
     * Cluster-Erkennung
     */
    private fun detectClusters(data: List<Float>): List<ClusterPattern> {
        val clusters = mutableListOf<ClusterPattern>()
        val sortedData = data.withIndex().sortedBy { it.value }
        
        var currentCluster = mutableListOf<IndexedValue<Float>>()
        var lastValue = Float.NEGATIVE_INFINITY
        val threshold = calculateStandardDeviation(data) * 0.5f
        
        for (indexedValue in sortedData) {
            if (indexedValue.value - lastValue <= threshold) {
                currentCluster.add(indexedValue)
            } else {
                if (currentCluster.size >= 3) {
                    clusters.add(createClusterPattern(currentCluster))
                }
                currentCluster = mutableListOf(indexedValue)
            }
            lastValue = indexedValue.value
        }
        
        // Letzten Cluster hinzufügen
        if (currentCluster.size >= 3) {
            clusters.add(createClusterPattern(currentCluster))
        }
        
        return clusters.sortedByDescending { it.density }
    }
    
    /**
     * Cluster-Muster erstellen
     */
    private fun createClusterPattern(cluster: List<IndexedValue<Float>>): ClusterPattern {
        val values = cluster.map { it.value }
        val indices = cluster.map { it.index }
        
        return ClusterPattern(
            centerValue = values.average().toFloat(),
            size = cluster.size,
            density = cluster.size.toFloat() / (values.maxOrNull()!! - values.minOrNull()!! + 1),
            indices = indices,
            spread = calculateStandardDeviation(values)
        )
    }
    
    /**
     * Harmonische Erkennung
     */
    private fun detectHarmonics(data: List<Float>): List<HarmonicPattern> {
        val harmonics = mutableListOf<HarmonicPattern>()
        val fftResult = performSimpleFFT(data)
        
        // Grundfrequenz finden
        val fundamentalFreq = findFundamentalFrequency(fftResult)
        if (fundamentalFreq > 0) {
            // Harmonische suchen
            for (harmonic in 2..5) {
                val harmonicFreq = fundamentalFreq * harmonic
                val amplitude = getAmplitudeAtFrequency(fftResult, harmonicFreq)
                
                if (amplitude > 0.1f) {
                    harmonics.add(
                        HarmonicPattern(
                            fundamentalFreq = fundamentalFreq,
                            harmonicNumber = harmonic,
                            frequency = harmonicFreq,
                            amplitude = amplitude,
                            phase = getPhaseAtFrequency(fftResult, harmonicFreq)
                        )
                    )
                }
            }
        }
        
        return harmonics
    }
    
    /**
     * Vereinfachte FFT
     */
    private fun performSimpleFFT(data: List<Float>): List<Complex> {
        val n = data.size
        val result = mutableListOf<Complex>()
        
        for (k in 0 until n / 2) {
            var real = 0.0
            var imag = 0.0
            
            for (j in 0 until n) {
                val angle = -2.0 * PI * k * j / n
                real += data[j] * cos(angle)
                imag += data[j] * sin(angle)
            }
            
            result.add(Complex(real, imag))
        }
        
        return result
    }
    
    /**
     * Grundfrequenz finden
     */
    private fun findFundamentalFrequency(fftResult: List<Complex>): Int {
        var maxAmplitude = 0.0
        var fundamentalIndex = 0
        
        for (i in 1 until fftResult.size) {
            val amplitude = fftResult[i].magnitude()
            if (amplitude > maxAmplitude) {
                maxAmplitude = amplitude
                fundamentalIndex = i
            }
        }
        
        return fundamentalIndex
    }
    
    /**
     * Amplitude bei bestimmter Frequenz
     */
    private fun getAmplitudeAtFrequency(fftResult: List<Complex>, frequency: Int): Float {
        return if (frequency < fftResult.size) {
            fftResult[frequency].magnitude().toFloat()
        } else 0f
    }
    
    /**
     * Phase bei bestimmter Frequenz
     */
    private fun getPhaseAtFrequency(fftResult: List<Complex>, frequency: Int): Float {
        return if (frequency < fftResult.size) {
            fftResult[frequency].phase().toFloat()
        } else 0f
    }
    
    /**
     * Komplexitätsberechnung
     */
    private fun calculateComplexity(data: List<Float>): Float {
        val entropy = calculateEntropy(data)
        val fractalDimension = calculateFractalDimension(data)
        val variability = calculateStandardDeviation(data) / (data.average().toFloat() + 1f)
        
        return (entropy * 0.4f + fractalDimension * 0.3f + variability * 0.3f).coerceIn(0f, 1f)
    }
    
    /**
     * Entropie berechnen
     */
    private fun calculateEntropy(data: List<Float>): Float {
        val bins = 20
        val min = data.minOrNull() ?: 0f
        val max = data.maxOrNull() ?: 1f
        val binSize = (max - min) / bins
        
        val histogram = IntArray(bins)
        
        for (value in data) {
            val binIndex = ((value - min) / binSize).toInt().coerceIn(0, bins - 1)
            histogram[binIndex]++
        }
        
        var entropy = 0.0
        val total = data.size.toDouble()
        
        for (count in histogram) {
            if (count > 0) {
                val probability = count / total
                entropy -= probability * log2(probability)
            }
        }
        
        return (entropy / log2(bins.toDouble())).toFloat()
    }
    
    /**
     * Fraktale Dimension berechnen (vereinfacht)
     */
    private fun calculateFractalDimension(data: List<Float>): Float {
        val scales = listOf(2, 4, 8, 16)
        val lengths = mutableListOf<Double>()
        
        for (scale in scales) {
            var length = 0.0
            for (i in 0 until data.size - scale step scale) {
                val segment = data.subList(i, min(i + scale, data.size))
                length += calculatePathLength(segment)
            }
            lengths.add(length)
        }
        
        // Lineare Regression für log-log Plot
        val logScales = scales.map { log2(it.toDouble()) }
        val logLengths = lengths.map { log2(it) }
        
        val slope = calculateSlope(logScales.map { it.toFloat() }, logLengths.map { it.toFloat() })
        return abs(slope).coerceIn(0f, 2f) / 2f
    }
    
    /**
     * Pfadlänge berechnen
     */
    private fun calculatePathLength(segment: List<Float>): Double {
        var length = 0.0
        for (i in 1 until segment.size) {
            length += abs(segment[i] - segment[i - 1])
        }
        return length
    }
    
    /**
     * Steigung für zwei Listen berechnen
     */
    private fun calculateSlope(x: List<Float>, y: List<Float>): Float {
        val n = x.size
        val xMean = x.average()
        val yMean = y.average()
        
        var numerator = 0f
        var denominator = 0f
        
        for (i in x.indices) {
            numerator += (x[i] - xMean.toFloat()) * (y[i] - yMean.toFloat())
            denominator += (x[i] - xMean.toFloat()).pow(2)
        }
        
        return if (denominator > 0) numerator / denominator else 0f
    }
    
    /**
     * Standardabweichung berechnen
     */
    private fun calculateStandardDeviation(data: List<Float>): Float {
        val mean = data.average()
        val variance = data.map { (it - mean).pow(2) }.average()
        return sqrt(variance).toFloat()
    }
}

/**
 * Datenklassen für Mustererkennung
 */
data class PatternRecognitionResult(
    val periodicPatterns: List<PeriodicPattern>,
    val spikes: List<SpikePattern>,
    val trends: List<TrendPattern>,
    val clusters: List<ClusterPattern>,
    val harmonics: List<HarmonicPattern>,
    val complexity: Float
)

data class PeriodicPattern(
    val period: Int,
    val correlation: Float,
    val phase: Float,
    val amplitude: Float,
    val confidence: Float
)

data class SpikePattern(
    val position: Int,
    val intensity: Float,
    val width: Int,
    val type: SpikeType,
    val confidence: Float
)

data class TrendPattern(
    val startIndex: Int,
    val endIndex: Int,
    val slope: Float,
    val rSquared: Float,
    val type: TrendType,
    val confidence: Float
)

data class ClusterPattern(
    val centerValue: Float,
    val size: Int,
    val density: Float,
    val indices: List<Int>,
    val spread: Float
)

data class HarmonicPattern(
    val fundamentalFreq: Int,
    val harmonicNumber: Int,
    val frequency: Int,
    val amplitude: Float,
    val phase: Float
)

data class Complex(val real: Double, val imag: Double) {
    fun magnitude(): Double = sqrt(real * real + imag * imag)
    fun phase(): Double = atan2(imag, real)
}

enum class SpikeType {
    POSITIVE, NEGATIVE
}

// Enums wurden nach com.emfad.app.models verschoben
