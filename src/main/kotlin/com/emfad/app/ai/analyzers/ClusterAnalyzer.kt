package com.emfad.app.ai.analyzers

import android.util.Log
import com.emfad.app.models.EMFReading
import com.google.gson.Gson
import kotlin.math.*

/**
 * DBSCAN Cluster Analyzer für EMFAD
 * Basiert auf ursprünglichen Algorithmen aus Chat-File
 * Samsung S21 Ultra optimiert
 */
class ClusterAnalyzer {
    
    companion object {
        private const val TAG = "ClusterAnalyzer"
        private const val DEFAULT_EPS = 0.5
        private const val DEFAULT_MIN_POINTS = 3
        private const val NOISE_LABEL = -1
        private const val UNCLASSIFIED = 0
    }
    
    private val gson = Gson()
    
    /**
     * Cluster-Analyse Ergebnis
     */
    data class ClusterResult(
        val clusterCount: Int,
        val clusterDensity: Double,
        val clusterSeparation: Double,
        val clusterCoherence: Double,
        val clusters: List<Cluster>,
        val noisePoints: List<DataPoint>,
        val silhouetteScore: Double,
        val dbIndex: Double
    )
    
    /**
     * Cluster-Datenstruktur
     */
    data class Cluster(
        val id: Int,
        val points: List<DataPoint>,
        val centroid: DataPoint,
        val radius: Double,
        val density: Double,
        val coherence: Double
    )
    
    /**
     * Datenpunkt für Clustering
     */
    data class DataPoint(
        val x: Double,
        val y: Double,
        val z: Double,
        val signalStrength: Double,
        val frequency: Double,
        val phase: Double,
        val originalIndex: Int,
        var clusterId: Int = UNCLASSIFIED,
        var visited: Boolean = false
    )
    
    /**
     * DBSCAN Clustering auf EMF-Messungen durchführen
     */
    fun performClustering(
        readings: List<EMFReading>,
        eps: Double = DEFAULT_EPS,
        minPoints: Int = DEFAULT_MIN_POINTS
    ): ClusterResult {
        Log.d(TAG, "Starte DBSCAN Clustering mit ${readings.size} Datenpunkten")
        
        if (readings.isEmpty()) {
            return ClusterResult(0, 0.0, 0.0, 0.0, emptyList(), emptyList(), 0.0, 0.0)
        }
        
        // Datenpunkte vorbereiten
        val dataPoints = prepareDataPoints(readings)
        
        // DBSCAN Algorithmus ausführen
        val clusters = dbscan(dataPoints, eps, minPoints)
        
        // Cluster-Metriken berechnen
        val clusterMetrics = calculateClusterMetrics(clusters, dataPoints)
        
        // Noise-Punkte identifizieren
        val noisePoints = dataPoints.filter { it.clusterId == NOISE_LABEL }
        
        Log.d(TAG, "Clustering abgeschlossen: ${clusters.size} Cluster, ${noisePoints.size} Noise-Punkte")
        
        return ClusterResult(
            clusterCount = clusters.size,
            clusterDensity = clusterMetrics.averageDensity,
            clusterSeparation = clusterMetrics.averageSeparation,
            clusterCoherence = clusterMetrics.averageCoherence,
            clusters = clusters,
            noisePoints = noisePoints,
            silhouetteScore = calculateSilhouetteScore(dataPoints, clusters),
            dbIndex = calculateDaviesBouldinIndex(clusters)
        )
    }
    
    /**
     * EMF-Messungen zu Datenpunkten konvertieren
     */
    private fun prepareDataPoints(readings: List<EMFReading>): MutableList<DataPoint> {
        return readings.mapIndexed { index, reading ->
            DataPoint(
                x = reading.positionX,
                y = reading.positionY,
                z = reading.positionZ,
                signalStrength = reading.signalStrength,
                frequency = reading.frequency,
                phase = reading.phase,
                originalIndex = index
            )
        }.toMutableList()
    }
    
    /**
     * DBSCAN Algorithmus Implementation
     */
    private fun dbscan(
        dataPoints: MutableList<DataPoint>,
        eps: Double,
        minPoints: Int
    ): List<Cluster> {
        var clusterId = 1
        val clusters = mutableListOf<Cluster>()
        
        for (point in dataPoints) {
            if (point.visited) continue
            
            point.visited = true
            val neighbors = getNeighbors(point, dataPoints, eps)
            
            if (neighbors.size < minPoints) {
                point.clusterId = NOISE_LABEL
            } else {
                val clusterPoints = expandCluster(point, neighbors, dataPoints, eps, minPoints, clusterId)
                if (clusterPoints.isNotEmpty()) {
                    val cluster = createCluster(clusterId, clusterPoints)
                    clusters.add(cluster)
                    clusterId++
                }
            }
        }
        
        return clusters
    }
    
    /**
     * Nachbarn eines Punktes finden
     */
    private fun getNeighbors(
        point: DataPoint,
        dataPoints: List<DataPoint>,
        eps: Double
    ): MutableList<DataPoint> {
        val neighbors = mutableListOf<DataPoint>()
        
        for (otherPoint in dataPoints) {
            if (calculateDistance(point, otherPoint) <= eps) {
                neighbors.add(otherPoint)
            }
        }
        
        return neighbors
    }
    
    /**
     * Cluster erweitern
     */
    private fun expandCluster(
        point: DataPoint,
        neighbors: MutableList<DataPoint>,
        dataPoints: MutableList<DataPoint>,
        eps: Double,
        minPoints: Int,
        clusterId: Int
    ): List<DataPoint> {
        val clusterPoints = mutableListOf<DataPoint>()
        point.clusterId = clusterId
        clusterPoints.add(point)
        
        var i = 0
        while (i < neighbors.size) {
            val neighbor = neighbors[i]
            
            if (!neighbor.visited) {
                neighbor.visited = true
                val neighborNeighbors = getNeighbors(neighbor, dataPoints, eps)
                
                if (neighborNeighbors.size >= minPoints) {
                    neighbors.addAll(neighborNeighbors.filter { it !in neighbors })
                }
            }
            
            if (neighbor.clusterId == UNCLASSIFIED || neighbor.clusterId == NOISE_LABEL) {
                neighbor.clusterId = clusterId
                clusterPoints.add(neighbor)
            }
            
            i++
        }
        
        return clusterPoints
    }
    
    /**
     * Euklidische Distanz zwischen zwei Punkten berechnen
     */
    private fun calculateDistance(point1: DataPoint, point2: DataPoint): Double {
        // Gewichtete Distanz mit EMF-spezifischen Features
        val spatialDistance = sqrt(
            (point1.x - point2.x).pow(2) +
            (point1.y - point2.y).pow(2) +
            (point1.z - point2.z).pow(2)
        )
        
        val signalDistance = abs(point1.signalStrength - point2.signalStrength) / 1000.0
        val frequencyDistance = abs(point1.frequency - point2.frequency) / 1000.0
        val phaseDistance = abs(point1.phase - point2.phase) / 360.0
        
        // Gewichtete Kombination
        return spatialDistance * 0.4 + signalDistance * 0.3 + frequencyDistance * 0.2 + phaseDistance * 0.1
    }
    
    /**
     * Cluster aus Punkten erstellen
     */
    private fun createCluster(clusterId: Int, points: List<DataPoint>): Cluster {
        val centroid = calculateCentroid(points)
        val radius = calculateClusterRadius(points, centroid)
        val density = calculateClusterDensity(points, radius)
        val coherence = calculateClusterCoherence(points, centroid)
        
        return Cluster(
            id = clusterId,
            points = points,
            centroid = centroid,
            radius = radius,
            density = density,
            coherence = coherence
        )
    }
    
    /**
     * Cluster-Zentroid berechnen
     */
    private fun calculateCentroid(points: List<DataPoint>): DataPoint {
        val avgX = points.map { it.x }.average()
        val avgY = points.map { it.y }.average()
        val avgZ = points.map { it.z }.average()
        val avgSignal = points.map { it.signalStrength }.average()
        val avgFreq = points.map { it.frequency }.average()
        val avgPhase = points.map { it.phase }.average()
        
        return DataPoint(
            x = avgX,
            y = avgY,
            z = avgZ,
            signalStrength = avgSignal,
            frequency = avgFreq,
            phase = avgPhase,
            originalIndex = -1
        )
    }
    
    /**
     * Cluster-Radius berechnen
     */
    private fun calculateClusterRadius(points: List<DataPoint>, centroid: DataPoint): Double {
        return points.maxOfOrNull { calculateDistance(it, centroid) } ?: 0.0
    }
    
    /**
     * Cluster-Dichte berechnen
     */
    private fun calculateClusterDensity(points: List<DataPoint>, radius: Double): Double {
        return if (radius > 0) points.size / (4.0 / 3.0 * PI * radius.pow(3)) else 0.0
    }
    
    /**
     * Cluster-Kohärenz berechnen
     */
    private fun calculateClusterCoherence(points: List<DataPoint>, centroid: DataPoint): Double {
        if (points.isEmpty()) return 0.0
        
        val avgDistance = points.map { calculateDistance(it, centroid) }.average()
        val maxDistance = points.maxOfOrNull { calculateDistance(it, centroid) } ?: 1.0
        
        return 1.0 - (avgDistance / maxDistance)
    }
    
    /**
     * Cluster-Metriken berechnen
     */
    private fun calculateClusterMetrics(clusters: List<Cluster>, dataPoints: List<DataPoint>): ClusterMetrics {
        if (clusters.isEmpty()) {
            return ClusterMetrics(0.0, 0.0, 0.0)
        }
        
        val averageDensity = clusters.map { it.density }.average()
        val averageCoherence = clusters.map { it.coherence }.average()
        val averageSeparation = calculateAverageClusterSeparation(clusters)
        
        return ClusterMetrics(averageDensity, averageSeparation, averageCoherence)
    }
    
    /**
     * Durchschnittliche Cluster-Trennung berechnen
     */
    private fun calculateAverageClusterSeparation(clusters: List<Cluster>): Double {
        if (clusters.size < 2) return 0.0
        
        var totalSeparation = 0.0
        var pairCount = 0
        
        for (i in clusters.indices) {
            for (j in i + 1 until clusters.size) {
                totalSeparation += calculateDistance(clusters[i].centroid, clusters[j].centroid)
                pairCount++
            }
        }
        
        return if (pairCount > 0) totalSeparation / pairCount else 0.0
    }
    
    /**
     * Silhouette Score berechnen
     */
    private fun calculateSilhouetteScore(dataPoints: List<DataPoint>, clusters: List<Cluster>): Double {
        val clusterMap = clusters.associateBy { it.id }
        var totalScore = 0.0
        var validPoints = 0
        
        for (point in dataPoints) {
            if (point.clusterId == NOISE_LABEL) continue
            
            val cluster = clusterMap[point.clusterId] ?: continue
            if (cluster.points.size <= 1) continue
            
            val a = calculateIntraClusterDistance(point, cluster)
            val b = calculateNearestClusterDistance(point, clusters, point.clusterId)
            
            val silhouette = (b - a) / max(a, b)
            totalScore += silhouette
            validPoints++
        }
        
        return if (validPoints > 0) totalScore / validPoints else 0.0
    }
    
    /**
     * Intra-Cluster Distanz berechnen
     */
    private fun calculateIntraClusterDistance(point: DataPoint, cluster: Cluster): Double {
        val otherPoints = cluster.points.filter { it.originalIndex != point.originalIndex }
        return if (otherPoints.isNotEmpty()) {
            otherPoints.map { calculateDistance(point, it) }.average()
        } else 0.0
    }
    
    /**
     * Distanz zum nächsten Cluster berechnen
     */
    private fun calculateNearestClusterDistance(point: DataPoint, clusters: List<Cluster>, currentClusterId: Int): Double {
        return clusters
            .filter { it.id != currentClusterId }
            .minOfOrNull { cluster ->
                cluster.points.map { calculateDistance(point, it) }.minOrNull() ?: Double.MAX_VALUE
            } ?: Double.MAX_VALUE
    }
    
    /**
     * Davies-Bouldin Index berechnen
     */
    private fun calculateDaviesBouldinIndex(clusters: List<Cluster>): Double {
        if (clusters.size < 2) return 0.0
        
        var totalIndex = 0.0
        
        for (i in clusters.indices) {
            var maxRatio = 0.0
            
            for (j in clusters.indices) {
                if (i != j) {
                    val avgIntraI = clusters[i].points.map { calculateDistance(it, clusters[i].centroid) }.average()
                    val avgIntraJ = clusters[j].points.map { calculateDistance(it, clusters[j].centroid) }.average()
                    val interClusterDist = calculateDistance(clusters[i].centroid, clusters[j].centroid)
                    
                    val ratio = (avgIntraI + avgIntraJ) / interClusterDist
                    maxRatio = max(maxRatio, ratio)
                }
            }
            
            totalIndex += maxRatio
        }
        
        return totalIndex / clusters.size
    }
    
    /**
     * Cluster-Metriken Datenklasse
     */
    private data class ClusterMetrics(
        val averageDensity: Double,
        val averageSeparation: Double,
        val averageCoherence: Double
    )
    
    /**
     * Cluster-Ergebnis zu JSON konvertieren
     */
    fun clusterResultToJson(result: ClusterResult): String {
        return gson.toJson(result)
    }
    
    /**
     * JSON zu Cluster-Ergebnis konvertieren
     */
    fun jsonToClusterResult(json: String): ClusterResult? {
        return try {
            gson.fromJson(json, ClusterResult::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Parsen des Cluster-Ergebnisses", e)
            null
        }
    }
}
