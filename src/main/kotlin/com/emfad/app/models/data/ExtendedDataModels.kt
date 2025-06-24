package com.emfad.app.models.data

import com.emfad.app.models.enums.*
import kotlinx.serialization.Serializable
import java.util.*

/**
 * Extended Data Models
 * Erweiterte Datenmodelle für Samsung S21 Ultra optimiert
 */

// Bluetooth Device Information
@Serializable
data class BluetoothDevice(
    val name: String,
    val address: String,
    val deviceType: DeviceType = DeviceType.UNKNOWN,
    val rssi: Int = 0,
    val isConnected: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val services: List<String> = emptyList(),
    val characteristics: Map<String, String> = emptyMap(),
    val firmwareVersion: String = "",
    val hardwareVersion: String = "",
    val serialNumber: String = "",
    val batteryLevel: Int = -1,
    val connectionQuality: Double = 0.0,
    val supportedFeatures: List<String> = emptyList()
) {
    fun isEMFADDevice(): Boolean = deviceType.isProduction()
    fun isLowBattery(): Boolean = batteryLevel in 1..20
    fun hasGoodSignal(): Boolean = rssi > -70
    fun getDisplayName(): String = if (name.isNotBlank()) name else "Unbekanntes Gerät"
}

// Device Capabilities
@Serializable
data class DeviceCapabilities(
    val maxFrequency: Double = 1000.0,
    val minFrequency: Double = 1.0,
    val maxGain: Double = 100.0,
    val minGain: Double = 0.1,
    val supportedFilters: List<FilterType> = emptyList(),
    val supportedModes: List<MeasurementMode> = emptyList(),
    val hasTemperatureSensor: Boolean = true,
    val hasHumiditySensor: Boolean = false,
    val hasPressureSensor: Boolean = false,
    val hasGPS: Boolean = false,
    val hasAccelerometer: Boolean = false,
    val hasGyroscope: Boolean = false,
    val maxSamplingRate: Double = 100.0,
    val bufferSize: Int = 1000,
    val memorySize: Long = 1024 * 1024, // 1MB
    val storageSize: Long = 16 * 1024 * 1024, // 16MB
    val batteryCapacity: Int = 3000, // mAh
    val chargingSupported: Boolean = true,
    val wirelessSupported: Boolean = true
)

// Measurement Parameters
@Serializable
data class MeasurementParameters(
    val frequency: Double = 100.0,
    val gain: Double = 1.0,
    val filterType: FilterType = FilterType.NONE,
    val filterParameters: Map<String, Double> = emptyMap(),
    val samplingRate: Double = 10.0,
    val measurementMode: MeasurementMode = MeasurementMode.SINGLE_POINT,
    val integrationTime: Double = 1.0,
    val averagingCount: Int = 1,
    val autoRange: Boolean = true,
    val temperatureCompensation: Boolean = true,
    val backgroundSubtraction: Boolean = false,
    val noiseReduction: Boolean = true,
    val calibrationEnabled: Boolean = true,
    val qualityThreshold: Double = 0.7,
    val timeoutSeconds: Int = 30
) {
    fun isValid(): Boolean {
        return frequency > 0 && gain > 0 && samplingRate > 0 && 
               integrationTime > 0 && averagingCount > 0 && 
               qualityThreshold in 0.0..1.0
    }
    
    fun getEstimatedDuration(): Double {
        return integrationTime * averagingCount + 1.0 // +1s für Setup
    }
}

// Calibration Data
@Serializable
data class CalibrationData(
    val id: String = UUID.randomUUID().toString(),
    val type: CalibrationType,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceId: String,
    val operatorName: String,
    val temperature: Double,
    val humidity: Double = 0.0,
    val pressure: Double = 1013.25,
    val referenceValues: Map<String, Double> = emptyMap(),
    val measuredValues: Map<String, Double> = emptyMap(),
    val correctionFactors: Map<String, Double> = emptyMap(),
    val validUntil: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000, // 24h
    val isValid: Boolean = true,
    val notes: String = "",
    val qualityScore: Double = 1.0,
    val uncertainties: Map<String, Double> = emptyMap()
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > validUntil
    fun isStillValid(): Boolean = isValid && !isExpired()
    fun getAgeHours(): Double = (System.currentTimeMillis() - timestamp) / (1000.0 * 3600.0)
}

// Environmental Conditions
@Serializable
data class EnvironmentalConditions(
    val timestamp: Long = System.currentTimeMillis(),
    val temperature: Double,
    val humidity: Double = 0.0,
    val pressure: Double = 1013.25,
    val ambientLight: Double = 0.0,
    val magneticField: Triple<Double, Double, Double> = Triple(0.0, 0.0, 0.0),
    val vibrationLevel: Double = 0.0,
    val noiseLevel: Double = 0.0,
    val airQuality: Double = 0.0,
    val windSpeed: Double = 0.0,
    val windDirection: Double = 0.0,
    val location: GeoLocation? = null
) {
    fun isStable(): Boolean {
        return vibrationLevel < 0.1 && noiseLevel < 50.0
    }
    
    fun isOptimal(): Boolean {
        return temperature in 15.0..35.0 && 
               humidity in 30.0..70.0 && 
               isStable()
    }
}

// Geographic Location
@Serializable
data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val accuracy: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val provider: String = "unknown",
    val bearing: Double = 0.0,
    val speed: Double = 0.0
) {
    fun isValid(): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }
    
    fun distanceTo(other: GeoLocation): Double {
        // Haversine formula
        val R = 6371000.0 // Earth radius in meters
        val lat1Rad = Math.toRadians(latitude)
        val lat2Rad = Math.toRadians(other.latitude)
        val deltaLatRad = Math.toRadians(other.latitude - latitude)
        val deltaLonRad = Math.toRadians(other.longitude - longitude)
        
        val a = kotlin.math.sin(deltaLatRad / 2) * kotlin.math.sin(deltaLatRad / 2) +
                kotlin.math.cos(lat1Rad) * kotlin.math.cos(lat2Rad) *
                kotlin.math.sin(deltaLonRad / 2) * kotlin.math.sin(deltaLonRad / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        
        return R * c
    }
}

// Signal Processing Results
@Serializable
data class SignalProcessingResult(
    val timestamp: Long = System.currentTimeMillis(),
    val rawSignal: List<Double> = emptyList(),
    val filteredSignal: List<Double> = emptyList(),
    val frequencySpectrum: Map<Double, Double> = emptyMap(),
    val phaseSpectrum: Map<Double, Double> = emptyMap(),
    val signalToNoiseRatio: Double = 0.0,
    val totalHarmonicDistortion: Double = 0.0,
    val peakFrequency: Double = 0.0,
    val bandwidth: Double = 0.0,
    val rmsValue: Double = 0.0,
    val peakValue: Double = 0.0,
    val crestFactor: Double = 0.0,
    val zeroCrossings: Int = 0,
    val autocorrelation: List<Double> = emptyList(),
    val crossCorrelation: List<Double> = emptyList(),
    val coherence: Double = 0.0,
    val processingTime: Long = 0
) {
    fun hasGoodQuality(): Boolean = signalToNoiseRatio > 20.0
    fun isStable(): Boolean = totalHarmonicDistortion < 0.1
}

// AI Analysis Results
@Serializable
data class AIAnalysisResult(
    val timestamp: Long = System.currentTimeMillis(),
    val materialClassification: MaterialClassificationResult,
    val clusterAnalysis: ClusterAnalysisResult? = null,
    val inclusionDetection: InclusionDetectionResult? = null,
    val anomalyDetection: AnomalyDetectionResult? = null,
    val qualityAssessment: QualityAssessmentResult,
    val processingTime: Long = 0,
    val modelVersion: String = "1.0.0",
    val confidence: Double = 0.0,
    val uncertainties: Map<String, Double> = emptyMap()
) {
    fun isReliable(): Boolean = confidence > 0.7 && qualityAssessment.overallQuality > 0.8
    fun hasAnomalies(): Boolean = anomalyDetection?.anomaliesDetected == true
}

// Material Classification Result
@Serializable
data class MaterialClassificationResult(
    val primaryMaterial: MaterialType,
    val confidence: Double,
    val alternativeMaterials: List<Pair<MaterialType, Double>> = emptyList(),
    val properties: MaterialProperties,
    val classificationMethod: String = "neural_network",
    val featureVector: List<Double> = emptyList(),
    val decisionBoundary: Double = 0.5
) {
    fun isConfident(): Boolean = confidence > 0.8
    fun hasAlternatives(): Boolean = alternativeMaterials.isNotEmpty()
}

// Material Properties
@Serializable
data class MaterialProperties(
    val conductivity: Double,
    val permeability: Double,
    val density: Double,
    val thickness: Double = 0.0,
    val roughness: Double = 0.0,
    val hardness: Double = 0.0,
    val elasticity: Double = 0.0,
    val thermalConductivity: Double = 0.0,
    val specificHeat: Double = 0.0,
    val meltingPoint: Double = 0.0,
    val crystallineStructure: String = "",
    val grainSize: Double = 0.0,
    val porosity: Double = 0.0,
    val surfaceFinish: String = ""
)

// Cluster Analysis Result
@Serializable
data class ClusterAnalysisResult(
    val clusterCount: Int,
    val clusters: List<DataCluster>,
    val silhouetteScore: Double,
    val inertia: Double,
    val algorithm: String = "dbscan",
    val parameters: Map<String, Double> = emptyMap()
)

// Data Cluster
@Serializable
data class DataCluster(
    val id: Int,
    val center: List<Double>,
    val points: List<Int>, // Indices of points in cluster
    val radius: Double,
    val density: Double,
    val coherence: Double
)

// Inclusion Detection Result
@Serializable
data class InclusionDetectionResult(
    val inclusionsFound: Boolean,
    val inclusionCount: Int,
    val inclusions: List<Inclusion>,
    val confidence: Double,
    val detectionMethod: String = "statistical_analysis"
)

// Inclusion
@Serializable
data class Inclusion(
    val id: String = UUID.randomUUID().toString(),
    val position: Triple<Double, Double, Double>,
    val size: Double,
    val shape: String,
    val materialType: MaterialType,
    val confidence: Double,
    val depth: Double,
    val orientation: Triple<Double, Double, Double> = Triple(0.0, 0.0, 0.0)
)

// Anomaly Detection Result
@Serializable
data class AnomalyDetectionResult(
    val anomaliesDetected: Boolean,
    val anomalyCount: Int,
    val anomalies: List<Anomaly>,
    val overallAnomalyScore: Double,
    val detectionMethod: String = "isolation_forest"
)

// Anomaly
@Serializable
data class Anomaly(
    val id: String = UUID.randomUUID().toString(),
    val position: Triple<Double, Double, Double>,
    val anomalyScore: Double,
    val anomalyType: String,
    val description: String,
    val severity: ErrorSeverity,
    val confidence: Double
)

// Quality Assessment Result
@Serializable
data class QualityAssessmentResult(
    val overallQuality: Double,
    val dataCompleteness: Double,
    val measurementStability: Double,
    val noiseLevel: Double,
    val calibrationAccuracy: Double,
    val temperatureStability: Double,
    val signalQuality: Double,
    val spatialConsistency: Double,
    val temporalConsistency: Double,
    val qualityFlags: List<String> = emptyList()
) {
    fun getQualityLevel(): DataQuality = DataQuality.fromScore(overallQuality)
    fun hasIssues(): Boolean = qualityFlags.isNotEmpty()
}

// Export Configuration
@Serializable
data class ExportConfiguration(
    val format: ExportFormat = ExportFormat.CSV,
    val includeRawData: Boolean = true,
    val includeProcessedData: Boolean = true,
    val includeAnalyses: Boolean = true,
    val includeMetadata: Boolean = true,
    val includeEnvironmental: Boolean = false,
    val includeCalibration: Boolean = false,
    val compressionEnabled: Boolean = false,
    val compressionLevel: Int = 6,
    val encryptionEnabled: Boolean = false,
    val passwordProtected: Boolean = false,
    val digitalSignature: Boolean = false,
    val customFields: Map<String, String> = emptyMap(),
    val outputPath: String = "",
    val filenameTemplate: String = "EMFAD_{session}_{timestamp}",
    val splitLargeFiles: Boolean = false,
    val maxFileSize: Long = 100 * 1024 * 1024 // 100MB
)

// System Information
@Serializable
data class SystemInformation(
    val appVersion: String,
    val buildNumber: String,
    val deviceModel: String,
    val androidVersion: String,
    val apiLevel: Int,
    val totalMemory: Long,
    val availableMemory: Long,
    val totalStorage: Long,
    val availableStorage: Long,
    val cpuCores: Int,
    val cpuFrequency: Long,
    val gpuModel: String = "",
    val screenResolution: Pair<Int, Int>,
    val screenDensity: Float,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val networkType: String,
    val bluetoothVersion: String,
    val hasNFC: Boolean,
    val hasGPS: Boolean,
    val sensors: List<String> = emptyList()
)

// Performance Metrics
@Serializable
data class PerformanceMetrics(
    val timestamp: Long = System.currentTimeMillis(),
    val cpuUsage: Double,
    val memoryUsage: Long,
    val memoryPeak: Long,
    val batteryDrain: Double,
    val networkUsage: Long,
    val diskIO: Long,
    val frameRate: Double,
    val responseTime: Long,
    val throughput: Double,
    val errorRate: Double,
    val crashCount: Int,
    val anrCount: Int, // Application Not Responding
    val gcCount: Int, // Garbage Collection
    val gcTime: Long
) {
    fun isPerformanceGood(): Boolean {
        return cpuUsage < 80.0 && 
               memoryUsage < memoryPeak * 0.8 && 
               frameRate > 55.0 && 
               responseTime < 100
    }
}

// User Preferences
@Serializable
data class UserPreferences(
    val theme: Theme = Theme.SYSTEM,
    val language: Language = Language.GERMAN,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val notificationEnabled: Boolean = true,
    val notificationPriority: NotificationPriority = NotificationPriority.NORMAL,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val autoSaveEnabled: Boolean = true,
    val autoSaveInterval: Int = 300, // seconds
    val autoExportEnabled: Boolean = false,
    val defaultExportFormat: ExportFormat = ExportFormat.CSV,
    val showAdvancedOptions: Boolean = false,
    val enableAnalytics: Boolean = true,
    val enableCrashReporting: Boolean = true,
    val dataRetentionDays: Int = 365,
    val customSettings: Map<String, String> = emptyMap()
)
