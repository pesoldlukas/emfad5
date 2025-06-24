package com.emfad.app.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * EMFAD Data Models
 * Rekonstruiert aus echten EMFAD-Dateiformaten (.EGD, .ESD, .FADS, .CAL, .INI)
 * Basiert auf reverse engineering der originalen Windows-Programme
 * Samsung S21 Ultra optimiert - Alle Definitionen konsolidiert
 */

/**
 * Material-Typ für Klassifikation
 */
enum class MaterialType {
    FERROUS_METAL,
    NON_FERROUS_METAL,
    CRYSTALLINE_METAL,
    CRYSTALLINE_NON_METAL,
    CAVITY,
    VOID,
    PARTICLE_COMPOSITE,
    CONDUCTIVE_NON_METAL,
    INSULATOR,
    UNKNOWN
}

/**
 * Material-Physik-Analyse
 */
@Parcelize
data class MaterialPhysicsAnalysis(
    val symmetryScore: Double,
    val hollownessScore: Double,
    val conductivity: Double,
    val magneticPermeability: Double = 1.0,
    val signalStrength: Double,
    val depth: Double,
    val size: Double,
    val particleDensity: Double = 0.0,
    val confidence: Double
) : Parcelable

/**
 * Material-Klassifikations-Ergebnis
 */
@Parcelize
data class MaterialClassificationResult(
    val materialType: MaterialType,
    val confidence: Double,
    val properties: Map<String, String>, // Simplified for Parcelable
    val recommendations: List<String>,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Mess-Statistiken
 */
@Parcelize
data class MeasurementStatistics(
    val count: Int,
    val averageSignalStrength: Double,
    val maxSignalStrength: Double,
    val minSignalStrength: Double,
    val averageDepth: Double = 0.0,
    val averageTemperature: Double = 0.0
) : Parcelable

/**
 * Mess-Session
 */
@Parcelize
data class MeasurementSession(
    val id: String,
    val name: String,
    val startTimestamp: Long,
    val endTimestamp: Long?,
    val deviceId: String,
    val measurementCount: Int,
    val notes: String,
    val location: String
) : Parcelable

/**
 * Kalibrierungs-Daten
 */
@Parcelize
data class CalibrationData(
    val frequency: Double,
    val offsetX: Double,
    val offsetY: Double,
    val offsetZ: Double,
    val gainX: Double,
    val gainY: Double,
    val gainZ: Double,
    val temperature: Double,
    val timestamp: Long
) : Parcelable

/**
 * Geräte-Info
 */
@Parcelize
data class DeviceInfo(
    val id: String,
    val name: String,
    val macAddress: String,
    val firmwareVersion: String,
    val batteryLevel: Int,
    val isConnected: Boolean,
    val lastConnected: Long
) : Parcelable

/**
 * Export-Status (temporäre Definition)
 */
sealed class ExportStatus {
    object Idle : ExportStatus()
    data class InProgress(val progress: Float) : ExportStatus()
    object Completed : ExportStatus()
    data class Error(val message: String) : ExportStatus()
}

/**
 * Export-Ergebnis (temporäre Definition)
 */
sealed class ExportResult {
    data class Success(val filePath: String, val recordCount: Int) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

/**
 * Import-Ergebnis (temporäre Definition)
 */
sealed class ImportResult {
    data class Success(val sessionId: String, val recordCount: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

/**
 * Sync-Ergebnis (temporäre Definition)
 */
sealed class SyncResult {
    data class Success(val message: String) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

/**
 * Wartungs-Ergebnis (temporäre Definition)
 */
sealed class MaintenanceResult {
    data class Success(val spaceSaved: Long) : MaintenanceResult()
    data class Error(val message: String) : MaintenanceResult()
}

/**
 * Messergebnis vom EMFAD-Gerät
 */
@Parcelize
data class MeasurementResult(
    val timestamp: Long,
    val frequency: Double,
    val signalStrength: Double,
    val depth: Double,
    val temperature: Double
) : Parcelable

/**
 * Verbindungsstatus
 */
enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, FAILED
}

/**
 * Messmodus
 */
enum class MeasurementMode {
    B_A_VERTICAL, A_B_HORIZONTAL, ANTENNA_A, DEPTH_PRO
}

/**
 * Geräte-Status
 */
@Parcelize
data class DeviceStatus(
    val status: Byte,
    val batteryLevel: Int,
    val firmwareVersion: String
) : Parcelable

/**
 * Pattern-Typ für Cluster-Analyse
 */
enum class PatternType {
    TREND, PERIODIC, SPIKES, NOISE
}

/**
 * Trend-Typ
 */
enum class TrendType {
    INCREASING, DECREASING, STABLE, NONE
}

/**
 * Anomalie-Typ
 */
enum class AnomalyType {
    HIGH_VALUE, LOW_VALUE, SUDDEN_CHANGE
}

/**
 * Anomalie-Schweregrad
 */
enum class AnomalySeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * Cluster-Typ
 */
enum class ClusterType {
    HIGH_INTENSITY, LOW_INTENSITY, STABLE, VARIABLE, NORMAL, NOISE, UNKNOWN
}

/**
 * Spike-Typ
 */
enum class SpikeType {
    POSITIVE, NEGATIVE
}

/**
 * Messzustand
 */
enum class MeasurementState {
    IDLE,
    STARTING,
    MEASURING,
    STOPPING,
    CALIBRATING,
    CONNECTION_LOST,
    ERROR
}

/**
 * Session-Daten für aktive Messung
 */
@Parcelize
data class SessionData(
    val id: String,
    val name: String,
    val deviceId: String,
    val location: String,
    val startTime: Long,
    val measurementCount: Int,
    val lastMeasurement: MeasurementResult? = null
) : Parcelable

// DeviceStatus bereits oben definiert - doppelte Definition entfernt

/**
 * AR-Visualisierungsdaten
 */
data class EMFVisualizationData(
    val sources: List<EMFSource>,
    val fieldStrength: Float,
    val heatmapData: Array<Array<Float>>,
    val measurements: List<EMFMeasurement>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as EMFVisualizationData
        
        if (sources != other.sources) return false
        if (fieldStrength != other.fieldStrength) return false
        if (!heatmapData.contentDeepEquals(other.heatmapData)) return false
        if (measurements != other.measurements) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = sources.hashCode()
        result = 31 * result + fieldStrength.hashCode()
        result = 31 * result + heatmapData.contentDeepHashCode()
        result = 31 * result + measurements.hashCode()
        return result
    }
}

/**
 * EMF-Quelle für AR-Visualisierung
 */
@Parcelize
data class EMFSource(
    val x: Float,
    val y: Float,
    val z: Float,
    val intensity: Float,
    val type: EMFSourceType
) : Parcelable

/**
 * EMF-Messung für AR
 */
@Parcelize
data class EMFMeasurement(
    val x: Float,
    val y: Float,
    val z: Float,
    val value: Float,
    val timestamp: Long
) : Parcelable

/**
 * EMF-Quellen-Typ
 */
enum class EMFSourceType {
    ELECTRICAL_DEVICE,
    WIRELESS_TRANSMITTER,
    POWER_LINE,
    UNKNOWN
}

/**
 * AR-Pose für Tracking
 */
data class ARPose(
    val position: FloatArray,
    val orientation: FloatArray,
    val rotationMatrix: FloatArray,
    val confidence: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as ARPose
        
        if (!position.contentEquals(other.position)) return false
        if (!orientation.contentEquals(other.orientation)) return false
        if (!rotationMatrix.contentEquals(other.rotationMatrix)) return false
        if (confidence != other.confidence) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = position.contentHashCode()
        result = 31 * result + orientation.contentHashCode()
        result = 31 * result + rotationMatrix.contentHashCode()
        result = 31 * result + confidence.hashCode()
        return result
    }
}

/**
 * Service-Status
 */
data class ServiceStatus(
    val measurementState: MeasurementState,
    val currentSession: SessionData?,
    val bufferSize: Int,
    val lastMeasurement: MeasurementResult?,
    val lastAnalysis: MaterialClassificationResult?
)

/**
 * Export-Formate
 */
enum class ExportFormat {
    CSV, JSON, MATLAB, PDF
}

/**
 * App-Einstellungen
 */
@Parcelize
data class AppSettings(
    val autoSave: Boolean = true,
    val measurementInterval: Long = 1000,
    val maxBufferSize: Int = 1000,
    val enableNotifications: Boolean = true,
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: String = "de",
    val units: MeasurementUnits = MeasurementUnits.METRIC,
    val precision: Int = 3,
    val autoExport: Boolean = false,
    val exportFormat: ExportFormat = ExportFormat.CSV
) : Parcelable

/**
 * App-Theme
 */
enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

/**
 * Maßeinheiten
 */
enum class MeasurementUnits {
    METRIC, IMPERIAL
}

// ConnectionState und MeasurementMode bereits oben definiert - doppelte Definitionen entfernt

/**
 * Kalibrierungs-Status
 */
enum class CalibrationStatus {
    NOT_CALIBRATED, CALIBRATING, CALIBRATED, FAILED
}

/**
 * Analyse-Typ
 */
enum class AnalysisType {
    MATERIAL_CLASSIFICATION,
    CLUSTER_ANALYSIS,
    PATTERN_RECOGNITION,
    ANOMALY_DETECTION
}

/**
 * Visualisierungs-Modus
 */
enum class VisualizationMode {
    REAL_TIME,
    HISTORICAL,
    COMPARISON,
    AR_OVERLAY
}

/**
 * Daten-Export-Status
 */
sealed class DataExportStatus {
    object Idle : DataExportStatus()
    data class InProgress(val progress: Float, val currentFile: String) : DataExportStatus()
    data class Completed(val filePath: String, val recordCount: Int) : DataExportStatus()
    data class Error(val message: String) : DataExportStatus()
}

/**
 * Netzwerk-Status
 */
enum class NetworkStatus {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}

/**
 * Benutzer-Präferenzen
 */
@Parcelize
data class UserPreferences(
    val userId: String,
    val userName: String,
    val email: String = "",
    val organization: String = "",
    val role: UserRole = UserRole.USER,
    val preferences: AppSettings = AppSettings(),
    val lastLogin: Long = 0L,
    val isFirstTime: Boolean = true
) : Parcelable

/**
 * Benutzer-Rolle
 */
enum class UserRole {
    USER, TECHNICIAN, ADMIN, RESEARCHER
}

/**
 * Projekt-Informationen
 */
@Parcelize
data class ProjectInfo(
    val id: String,
    val name: String,
    val description: String,
    val createdBy: String,
    val createdAt: Long,
    val lastModified: Long,
    val sessionIds: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val location: String = "",
    val notes: String = ""
) : Parcelable

/**
 * Statistik-Daten
 */
data class StatisticsData(
    val totalMeasurements: Int,
    val totalSessions: Int,
    val totalDevices: Int,
    val averageSessionDuration: Long,
    val mostUsedMaterialType: String?,
    val averageConfidence: Double,
    val dataSize: Long,
    val lastActivity: Long
)

/**
 * Fehler-Informationen
 */
data class ErrorInfo(
    val code: Int,
    val message: String,
    val timestamp: Long,
    val source: ErrorSource,
    val severity: ErrorSeverity,
    val stackTrace: String? = null
)

/**
 * Fehler-Quelle
 */
enum class ErrorSource {
    BLUETOOTH, DATABASE, AI_ANALYSIS, AR_TRACKING, FILE_IO, NETWORK, UNKNOWN
}

/**
 * Fehler-Schweregrad
 */
enum class ErrorSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * Benachrichtigungs-Typ
 */
enum class NotificationType {
    MEASUREMENT_COMPLETE,
    CALIBRATION_REQUIRED,
    BATTERY_LOW,
    CONNECTION_LOST,
    ANALYSIS_COMPLETE,
    EXPORT_COMPLETE,
    ERROR_OCCURRED
}

/**
 * Benachrichtigung
 */
@Parcelize
data class AppNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val actionRequired: Boolean = false,
    val data: String? = null
) : Parcelable

/**
 * Update-Informationen
 */
data class UpdateInfo(
    val version: String,
    val buildNumber: Int,
    val releaseNotes: String,
    val downloadUrl: String,
    val isRequired: Boolean,
    val releaseDate: Long
)

/**
 * Performance-Metriken
 */
data class PerformanceMetrics(
    val cpuUsage: Float,
    val memoryUsage: Long,
    val batteryLevel: Int,
    val temperature: Float,
    val frameRate: Float,
    val networkLatency: Long,
    val storageUsed: Long,
    val timestamp: Long
)

/**
 * Samsung S21 Ultra spezifische Konfiguration
 */
data class DeviceConfiguration(
    val deviceModel: String = "SM-G998B", // Samsung S21 Ultra
    val androidVersion: String,
    val apiLevel: Int,
    val screenDensity: Float,
    val screenWidth: Int,
    val screenHeight: Int,
    val hasNFC: Boolean,
    val hasBluetooth5: Boolean,
    val hasWiFi6: Boolean,
    val has5G: Boolean,
    val ramSize: Long,
    val storageSize: Long,
    val processorCores: Int,
    val gpuModel: String
)

/**
 * Backup-Informationen
 */
data class BackupInfo(
    val id: String,
    val name: String,
    val createdAt: Long,
    val size: Long,
    val sessionCount: Int,
    val measurementCount: Int,
    val filePath: String,
    val checksum: String,
    val isEncrypted: Boolean = false
)
