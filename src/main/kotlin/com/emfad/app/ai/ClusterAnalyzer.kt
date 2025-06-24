package com.emfad.app.ai

import kotlin.math.*
import com.emfad.app.models.*

/**
 * Cluster-Analyse für EMF-Daten
 * Implementiert DBSCAN und K-Means Algorithmen für Materialgruppierung
 */
class ClusterAnalyzer {
    
    /**
     * Hauptanalyse-Methode
     */
    fun analyze(data: List<Double>): ClusterAnalysisResult {
        val points = data.mapIndexed { index, value -> 
            DataPoint(index.toDouble(), value, index) 
        }
        
        // DBSCAN für Anomalie-Erkennung
        val dbscanClusters = performDBSCAN(points)
        
        // K-Means für Hauptgruppierung
        val kMeansClusters = performKMeans(points, 3)
        
        // Statistische Analyse
        val statistics = calculateStatistics(data)
        
        // Muster-Erkennung
        val patterns = detectPatterns(data)
        
        return ClusterAnalysisResult(
            dbscanClusters = dbscanClusters,
            kMeansClusters = kMeansClusters,
            statistics = statistics,
            patterns = patterns,
            anomalies = detectAnomalies(data, statistics),
            quality = assessClusterQuality(dbscanClusters, kMeansClusters)
        )
    }
    
    /**
     * DBSCAN-Algorithmus
     */
    private fun performDBSCAN(points: List<DataPoint>, eps: Double = 0.5, minPts: Int = 3): List<Cluster> {
        val clusters = mutableListOf<Cluster>()
        val visited = mutableSetOf<Int>()
        val noise = mutableSetOf<DataPoint>()
        var clusterId = 0
        
        for (point in points) {
            if (point.id in visited) continue
            
            visited.add(point.id)
            val neighbors = getNeighbors(point, points, eps)
            
            if (neighbors.size < minPts) {
                noise.add(point)
            } else {
                val cluster = expandCluster(point, neighbors, points, eps, minPts, visited, clusterId++)
                if (cluster.points.isNotEmpty()) {
                    clusters.add(cluster)
                }
            }
        }
        
        // Noise-Cluster hinzufügen
        if (noise.isNotEmpty()) {
            clusters.add(Cluster(
                id = -1,
                points = noise.toList(),
                centroid = calculateCentroid(noise.toList()),
                type = ClusterType.NOISE
            ))
        }
        
        return clusters
    }
    
    /**
     * Cluster erweitern (DBSCAN)
     */
    private fun expandCluster(
        point: DataPoint,
        neighbors: List<DataPoint>,
        allPoints: List<DataPoint>,
        eps: Double,
        minPts: Int,
        visited: MutableSet<Int>,
        clusterId: Int
    ): Cluster {
        val clusterPoints = mutableListOf(point)
        val neighborQueue = neighbors.toMutableList()
        
        var i = 0
        while (i < neighborQueue.size) {
            val neighbor = neighborQueue[i]
            
            if (neighbor.id !in visited) {
                visited.add(neighbor.id)
                val newNeighbors = getNeighbors(neighbor, allPoints, eps)
                
                if (newNeighbors.size >= minPts) {
                    neighborQueue.addAll(newNeighbors.filter { it !in neighborQueue })
                }
            }
            
            if (neighbor !in clusterPoints) {
                clusterPoints.add(neighbor)
            }
            
            i++
        }
        
        return Cluster(
            id = clusterId,
            points = clusterPoints,
            centroid = calculateCentroid(clusterPoints),
            type = determineClusterType(clusterPoints)
        )
    }
    
    /**
     * Nachbarn finden
     */
    private fun getNeighbors(point: DataPoint, allPoints: List<DataPoint>, eps: Double): List<DataPoint> {
        return allPoints.filter { other ->
            euclideanDistance(point, other) <= eps
        }
    }
    
    /**
     * K-Means-Algorithmus
     */
    private fun performKMeans(points: List<DataPoint>, k: Int, maxIterations: Int = 100): List<Cluster> {
        if (points.size < k) return emptyList()
        
        // Initiale Zentroide zufällig wählen
        var centroids = initializeCentroids(points, k)
        var clusters = emptyList<Cluster>()
        
        repeat(maxIterations) {
            // Punkte zu nächstem Zentroid zuordnen
            clusters = assignPointsToCentroids(points, centroids)
            
            // Neue Zentroide berechnen
            val newCentroids = clusters.map { it.centroid }
            
            // Konvergenz prüfen
            if (centroidsConverged(centroids, newCentroids)) {
                return@repeat
            }
            
            centroids = newCentroids
        }
        
        return clusters.mapIndexed { index, cluster ->
            cluster.copy(
                id = index,
                type = determineClusterType(cluster.points)
            )
        }
    }
    
    /**
     * Initiale Zentroide
     */
    private fun initializeCentroids(points: List<DataPoint>, k: Int): List<DataPoint> {
        val shuffled = points.shuffled()
        return shuffled.take(k)
    }
    
    /**
     * Punkte zu Zentroiden zuordnen
     */
    private fun assignPointsToCentroids(points: List<DataPoint>, centroids: List<DataPoint>): List<Cluster> {
        val clusters = centroids.mapIndexed { index, centroid ->
            Cluster(
                id = index,
                points = mutableListOf(),
                centroid = centroid,
                type = ClusterType.UNKNOWN
            )
        }.toMutableList()
        
        for (point in points) {
            val nearestClusterIndex = centroids.indices.minByOrNull { index ->
                euclideanDistance(point, centroids[index])
            } ?: 0
            
            (clusters[nearestClusterIndex].points as MutableList).add(point)
        }
        
        // Zentroide neu berechnen
        return clusters.map { cluster ->
            if (cluster.points.isNotEmpty()) {
                cluster.copy(centroid = calculateCentroid(cluster.points))
            } else {
                cluster
            }
        }
    }
    
    /**
     * Konvergenz prüfen
     */
    private fun centroidsConverged(old: List<DataPoint>, new: List<DataPoint>, threshold: Double = 0.001): Boolean {
        return old.zip(new).all { (oldCentroid, newCentroid) ->
            euclideanDistance(oldCentroid, newCentroid) < threshold
        }
    }
    
    /**
     * Zentroid berechnen
     */
    private fun calculateCentroid(points: List<DataPoint>): DataPoint {
        if (points.isEmpty()) return DataPoint(0.0, 0.0, -1)
        
        val avgX = points.map { it.x }.average()
        val avgY = points.map { it.y }.average()
        
        return DataPoint(avgX, avgY, -1)
    }
    
    /**
     * Cluster-Typ bestimmen
     */
    private fun determineClusterType(points: List<DataPoint>): ClusterType {
        if (points.isEmpty()) return ClusterType.UNKNOWN
        
        val values = points.map { it.y }
        val mean = values.average()
        val std = sqrt(values.map { (it - mean).pow(2) }.average())
        
        return when {
            values.all { it > mean + std } -> ClusterType.HIGH_INTENSITY
            values.all { it < mean - std } -> ClusterType.LOW_INTENSITY
            std < 0.1 -> ClusterType.STABLE
            std > 0.5 -> ClusterType.VARIABLE
            else -> ClusterType.NORMAL
        }
    }
    
    /**
     * Euklidische Distanz
     */
    private fun euclideanDistance(p1: DataPoint, p2: DataPoint): Double {
        return sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
    }
    
    /**
     * Statistische Analyse
     */
    private fun calculateStatistics(data: List<Double>): ClusterStatistics {
        if (data.isEmpty()) return ClusterStatistics(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        
        val sorted = data.sorted()
        val mean = data.average()
        val variance = data.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance)
        val median = if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
        } else {
            sorted[sorted.size / 2]
        }
        
        return ClusterStatistics(
            mean = mean,
            median = median,
            standardDeviation = stdDev,
            variance = variance,
            minimum = sorted.first(),
            maximum = sorted.last()
        )
    }
    
    /**
     * Muster-Erkennung
     */
    private fun detectPatterns(data: List<Double>): List<Pattern> {
        val patterns = mutableListOf<Pattern>()
        
        // Trend-Erkennung
        val trend = detectTrend(data)
        if (trend != TrendType.NONE) {
            patterns.add(Pattern(
                type = PatternType.TREND,
                description = "Trend erkannt: $trend",
                confidence = calculateTrendConfidence(data, trend),
                parameters = mapOf("trend_type" to trend.name)
            ))
        }
        
        // Periodizität
        val period = detectPeriodicity(data)
        if (period > 0) {
            patterns.add(Pattern(
                type = PatternType.PERIODIC,
                description = "Periodisches Muster mit Periode $period",
                confidence = calculatePeriodicityConfidence(data, period),
                parameters = mapOf("period" to period)
            ))
        }
        
        // Spikes
        val spikes = detectSpikes(data)
        if (spikes.isNotEmpty()) {
            patterns.add(Pattern(
                type = PatternType.SPIKES,
                description = "${spikes.size} Spikes erkannt",
                confidence = 0.8,
                parameters = mapOf("spike_positions" to spikes)
            ))
        }
        
        return patterns
    }
    
    /**
     * Trend-Erkennung
     */
    private fun detectTrend(data: List<Double>): TrendType {
        if (data.size < 3) return TrendType.NONE
        
        val x = (0 until data.size).map { it.toDouble() }
        val y = data
        
        // Lineare Regression
        val n = data.size
        val sumX = x.sum()
        val sumY = y.sum()
        val sumXY = x.zip(y) { xi, yi -> xi * yi }.sum()
        val sumX2 = x.map { it.pow(2) }.sum()
        
        val slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX.pow(2))
        
        return when {
            slope > 0.01 -> TrendType.INCREASING
            slope < -0.01 -> TrendType.DECREASING
            else -> TrendType.STABLE
        }
    }
    
    /**
     * Trend-Konfidenz berechnen
     */
    private fun calculateTrendConfidence(data: List<Double>, trend: TrendType): Double {
        // R-Quadrat berechnen
        val x = (0 until data.size).map { it.toDouble() }
        val y = data
        val yMean = y.average()
        
        val n = data.size
        val sumX = x.sum()
        val sumY = y.sum()
        val sumXY = x.zip(y) { xi, yi -> xi * yi }.sum()
        val sumX2 = x.map { it.pow(2) }.sum()
        
        val slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX.pow(2))
        val intercept = (sumY - slope * sumX) / n
        
        val ssRes = x.zip(y) { xi, yi ->
            val predicted = slope * xi + intercept
            (yi - predicted).pow(2)
        }.sum()
        
        val ssTot = y.map { (it - yMean).pow(2) }.sum()
        
        val rSquared = if (ssTot > 0) 1 - (ssRes / ssTot) else 0.0
        
        return rSquared.coerceIn(0.0, 1.0)
    }
    
    /**
     * Periodizität erkennen
     */
    private fun detectPeriodicity(data: List<Double>): Int {
        if (data.size < 6) return 0
        
        val maxPeriod = data.size / 3
        var bestPeriod = 0
        var bestCorrelation = 0.0
        
        for (period in 2..maxPeriod) {
            val correlation = calculateAutoCorrelation(data, period)
            if (correlation > bestCorrelation && correlation > 0.5) {
                bestCorrelation = correlation
                bestPeriod = period
            }
        }
        
        return bestPeriod
    }
    
    /**
     * Auto-Korrelation berechnen
     */
    private fun calculateAutoCorrelation(data: List<Double>, lag: Int): Double {
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
     * Periodizitäts-Konfidenz
     */
    private fun calculatePeriodicityConfidence(data: List<Double>, period: Int): Double {
        return calculateAutoCorrelation(data, period)
    }
    
    /**
     * Spike-Erkennung
     */
    private fun detectSpikes(data: List<Double>): List<Int> {
        if (data.size < 3) return emptyList()
        
        val mean = data.average()
        val std = sqrt(data.map { (it - mean).pow(2) }.average())
        val threshold = mean + 2 * std
        
        val spikes = mutableListOf<Int>()
        
        for (i in 1 until data.size - 1) {
            if (data[i] > threshold && data[i] > data[i-1] && data[i] > data[i+1]) {
                spikes.add(i)
            }
        }
        
        return spikes
    }
    
    /**
     * Anomalien erkennen
     */
    private fun detectAnomalies(data: List<Double>, statistics: ClusterStatistics): List<Anomaly> {
        val anomalies = mutableListOf<Anomaly>()
        val threshold = statistics.mean + 2 * statistics.standardDeviation
        val lowerThreshold = statistics.mean - 2 * statistics.standardDeviation
        
        data.forEachIndexed { index, value ->
            when {
                value > threshold -> {
                    anomalies.add(Anomaly(
                        index = index,
                        value = value,
                        type = AnomalyType.HIGH_VALUE,
                        severity = calculateAnomalySeverity(value, statistics)
                    ))
                }
                value < lowerThreshold -> {
                    anomalies.add(Anomaly(
                        index = index,
                        value = value,
                        type = AnomalyType.LOW_VALUE,
                        severity = calculateAnomalySeverity(value, statistics)
                    ))
                }
            }
        }
        
        return anomalies
    }
    
    /**
     * Anomalie-Schweregrad berechnen
     */
    private fun calculateAnomalySeverity(value: Double, statistics: ClusterStatistics): AnomalySeverity {
        val deviation = abs(value - statistics.mean) / statistics.standardDeviation
        
        return when {
            deviation > 3.0 -> AnomalySeverity.CRITICAL
            deviation > 2.5 -> AnomalySeverity.HIGH
            deviation > 2.0 -> AnomalySeverity.MEDIUM
            else -> AnomalySeverity.LOW
        }
    }
    
    /**
     * Cluster-Qualität bewerten
     */
    private fun assessClusterQuality(dbscanClusters: List<Cluster>, kMeansClusters: List<Cluster>): ClusterQuality {
        val dbscanSilhouette = calculateSilhouetteScore(dbscanClusters)
        val kMeansSilhouette = calculateSilhouetteScore(kMeansClusters)
        
        return ClusterQuality(
            dbscanSilhouette = dbscanSilhouette,
            kMeansSilhouette = kMeansSilhouette,
            overallQuality = (dbscanSilhouette + kMeansSilhouette) / 2,
            recommendation = when {
                dbscanSilhouette > kMeansSilhouette -> "DBSCAN empfohlen"
                kMeansSilhouette > dbscanSilhouette -> "K-Means empfohlen"
                else -> "Beide Methoden gleichwertig"
            }
        )
    }
    
    /**
     * Silhouette-Score berechnen
     */
    private fun calculateSilhouetteScore(clusters: List<Cluster>): Double {
        if (clusters.isEmpty() || clusters.all { it.points.isEmpty() }) return 0.0
        
        val allPoints = clusters.flatMap { it.points }
        var totalScore = 0.0
        var pointCount = 0
        
        for (cluster in clusters) {
            for (point in cluster.points) {
                val a = calculateIntraClusterDistance(point, cluster)
                val b = calculateNearestClusterDistance(point, clusters.filter { it != cluster })
                
                val silhouette = if (max(a, b) > 0) (b - a) / max(a, b) else 0.0
                totalScore += silhouette
                pointCount++
            }
        }
        
        return if (pointCount > 0) totalScore / pointCount else 0.0
    }
    
    /**
     * Intra-Cluster-Distanz
     */
    private fun calculateIntraClusterDistance(point: DataPoint, cluster: Cluster): Double {
        val otherPoints = cluster.points.filter { it != point }
        return if (otherPoints.isNotEmpty()) {
            otherPoints.map { euclideanDistance(point, it) }.average()
        } else {
            0.0
        }
    }
    
    /**
     * Nächste Cluster-Distanz
     */
    private fun calculateNearestClusterDistance(point: DataPoint, otherClusters: List<Cluster>): Double {
        return otherClusters.filter { it.points.isNotEmpty() }.minOfOrNull { cluster ->
            cluster.points.map { euclideanDistance(point, it) }.average()
        } ?: Double.MAX_VALUE
    }
}

/**
 * Datenklassen für Cluster-Analyse
 */
data class DataPoint(
    val x: Double,
    val y: Double,
    val id: Int
)

data class Cluster(
    val id: Int,
    val points: List<DataPoint>,
    val centroid: DataPoint,
    val type: ClusterType
)

data class ClusterAnalysisResult(
    val dbscanClusters: List<Cluster>,
    val kMeansClusters: List<Cluster>,
    val statistics: ClusterStatistics,
    val patterns: List<Pattern>,
    val anomalies: List<Anomaly>,
    val quality: ClusterQuality
)

data class ClusterStatistics(
    val mean: Double,
    val median: Double,
    val standardDeviation: Double,
    val variance: Double,
    val minimum: Double,
    val maximum: Double
)

data class Pattern(
    val type: PatternType,
    val description: String,
    val confidence: Double,
    val parameters: Map<String, Any>
)

data class Anomaly(
    val index: Int,
    val value: Double,
    val type: AnomalyType,
    val severity: AnomalySeverity
)

data class ClusterQuality(
    val dbscanSilhouette: Double,
    val kMeansSilhouette: Double,
    val overallQuality: Double,
    val recommendation: String
)

enum class ClusterType {
    HIGH_INTENSITY, LOW_INTENSITY, STABLE, VARIABLE, NORMAL, NOISE, UNKNOWN
}

// Enums wurden nach com.emfad.app.models verschoben
