package com.emfad.app.models.ui

import com.emfad.app.models.*
import com.emfad.app.models.data.*
import com.emfad.app.models.enums.*
import kotlinx.serialization.Serializable

/**
 * UI State Models
 * UI-spezifische State-Modelle für Samsung S21 Ultra optimiert
 */

// Base UI State
@Serializable
data class BaseUIState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    val networkAvailable: Boolean = true,
    val isOfflineMode: Boolean = false
) {
    fun hasError(): Boolean = error != null
    fun isIdle(): Boolean = !isLoading && !isRefreshing
}

// Dashboard UI State
@Serializable
data class DashboardUIState(
    val baseState: BaseUIState = BaseUIState(),
    val bluetoothState: BluetoothUIState = BluetoothUIState(),
    val recentSessions: List<MeasurementSession> = emptyList(),
    val dashboardStats: DashboardStatistics = DashboardStatistics(),
    val quickActions: List<QuickAction> = emptyList(),
    val notifications: List<AppNotification> = emptyList(),
    val systemHealth: SystemHealth = SystemHealth()
) {
    val isLoading: Boolean get() = baseState.isLoading
    val error: String? get() = baseState.error
    
    fun hasActiveSessions(): Boolean = recentSessions.any { it.status.isActive() }
    fun hasNotifications(): Boolean = notifications.isNotEmpty()
}

// Bluetooth UI State
@Serializable
data class BluetoothUIState(
    val isEnabled: Boolean = false,
    val hasPermissions: Boolean = false,
    val isScanning: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val connectedDevice: BluetoothDevice? = null,
    val availableDevices: List<BluetoothDevice> = emptyList(),
    val scanResults: List<BluetoothDevice> = emptyList(),
    val lastScanTime: Long = 0,
    val connectionAttempts: Int = 0,
    val signalStrength: Int = 0,
    val batteryLevel: Int = -1,
    val deviceCapabilities: DeviceCapabilities? = null,
    val connectionError: String? = null
) {
    val isConnected: Boolean get() = connectionState.isConnected()
    val isConnecting: Boolean get() = connectionState.isConnecting()
    val hasError: Boolean get() = connectionState.hasError() || connectionError != null
    
    fun canScan(): Boolean = isEnabled && hasPermissions && !isScanning
    fun canConnect(): Boolean = isEnabled && hasPermissions && !isConnected
    fun needsSetup(): Boolean = !isEnabled || !hasPermissions
}

// Measurement UI State
@Serializable
data class MeasurementUIState(
    val baseState: BaseUIState = BaseUIState(),
    val measurementState: MeasurementState = MeasurementState(),
    val currentSession: MeasurementSession? = null,
    val currentReading: EMFReading? = null,
    val measurementHistory: List<EMFReading> = emptyList(),
    val sessionStats: SessionStatistics = SessionStatistics(),
    val measurementParameters: MeasurementParameters = MeasurementParameters(),
    val calibrationStatus: CalibrationStatus = CalibrationStatus(),
    val environmentalConditions: EnvironmentalConditions? = null,
    val realTimeChart: ChartData = ChartData(),
    val alerts: List<MeasurementAlert> = emptyList()
) {
    val isLoading: Boolean get() = baseState.isLoading
    val error: String? get() = baseState.error
    val isMeasuring: Boolean get() = measurementState.isMeasuring
    val isSessionActive: Boolean get() = measurementState.isSessionActive
    
    fun hasActiveAlerts(): Boolean = alerts.any { it.isActive }
    fun needsCalibration(): Boolean = !calibrationStatus.isValid
}

// Analysis UI State
@Serializable
data class AnalysisUIState(
    val baseState: BaseUIState = BaseUIState(),
    val sessions: List<MeasurementSession> = emptyList(),
    val selectedSession: MeasurementSession? = null,
    val analysisResults: List<MaterialAnalysis> = emptyList(),
    val aiAnalysisResult: AIAnalysisResult? = null,
    val chartData: List<ChartData> = emptyList(),
    val filterOptions: AnalysisFilterOptions = AnalysisFilterOptions(),
    val sortOptions: AnalysisSortOptions = AnalysisSortOptions(),
    val exportOptions: ExportConfiguration = ExportConfiguration(),
    val comparisonMode: Boolean = false,
    val selectedSessions: List<Long> = emptyList()
) {
    val isLoading: Boolean get() = baseState.isLoading
    val error: String? get() = baseState.error
    val hasData: Boolean get() = sessions.isNotEmpty()
    val hasAnalysis: Boolean get() = analysisResults.isNotEmpty()
    
    fun canCompare(): Boolean = selectedSessions.size >= 2
    fun hasAIResults(): Boolean = aiAnalysisResult != null
}

// AR UI State
@Serializable
data class ARUIState(
    val baseState: BaseUIState = BaseUIState(),
    val isARSupported: Boolean = false,
    val isARInitialized: Boolean = false,
    val isARActive: Boolean = false,
    val isFallbackMode: Boolean = false,
    val trackingState: String = "STOPPED",
    val visualizationMode: VisualizationMode = VisualizationMode.SIGNAL_STRENGTH,
    val scaleFactor: Float = 1.0f,
    val showMaterialTypes: Boolean = true,
    val showInclusions: Boolean = true,
    val arObjects: List<ARObject> = emptyList(),
    val cameraPermission: Boolean = false,
    val planeDetection: Boolean = false,
    val lightEstimation: Boolean = false,
    val renderingStats: RenderingStats = RenderingStats()
) {
    val isLoading: Boolean get() = baseState.isLoading
    val error: String? get() = baseState.error
    val canUseAR: Boolean get() = isARSupported && cameraPermission
    val isReady: Boolean get() = isARInitialized && (isARSupported || isFallbackMode)
    
    fun needsPermissions(): Boolean = !cameraPermission
    fun hasObjects(): Boolean = arObjects.isNotEmpty()
}

// Export UI State
@Serializable
data class ExportUIState(
    val baseState: BaseUIState = BaseUIState(),
    val availableSessions: List<MeasurementSession> = emptyList(),
    val selectedSessions: List<Long> = emptyList(),
    val exportFormats: List<ExportFormat> = ExportFormat.values().toList(),
    val selectedFormat: ExportFormat = ExportFormat.CSV,
    val exportConfiguration: ExportConfiguration = ExportConfiguration(),
    val exportProgress: ExportProgress? = null,
    val exportHistory: List<ExportRecord> = emptyList(),
    val storageInfo: StorageInfo = StorageInfo(),
    val previewData: String? = null
) {
    val isLoading: Boolean get() = baseState.isLoading
    val error: String? get() = baseState.error
    val isExporting: Boolean get() = exportProgress != null
    val canExport: Boolean get() = selectedSessions.isNotEmpty() && !isExporting
    
    fun hasSelection(): Boolean = selectedSessions.isNotEmpty()
    fun hasHistory(): Boolean = exportHistory.isNotEmpty()
}

// Settings UI State
@Serializable
data class SettingsUIState(
    val baseState: BaseUIState = BaseUIState(),
    val userPreferences: UserPreferences = UserPreferences(),
    val systemInfo: SystemInformation? = null,
    val performanceMetrics: PerformanceMetrics? = null,
    val storageInfo: StorageInfo = StorageInfo(),
    val permissionStatus: PermissionStatus = PermissionStatus(),
    val updateInfo: UpdateInfo? = null,
    val debugInfo: DebugInfo = DebugInfo(),
    val backupInfo: BackupInfo = BackupInfo()
) {
    val isLoading: Boolean get() = baseState.isLoading
    val error: String? get() = baseState.error
    val hasUpdateAvailable: Boolean get() = updateInfo?.hasUpdate == true
    val needsPermissions: Boolean get() = !permissionStatus.allGranted
}

// Supporting Data Classes

@Serializable
data class DashboardStatistics(
    val totalSessions: Int = 0,
    val totalMeasurements: Int = 0,
    val totalAnalyses: Int = 0,
    val activeSessions: Int = 0,
    val todayMeasurements: Int = 0,
    val weekMeasurements: Int = 0,
    val averageSessionDuration: Double = 0.0,
    val dataQualityAverage: Double = 0.0,
    val storageUsed: Long = 0,
    val lastActivity: Long = 0
)

@Serializable
data class QuickAction(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val action: String,
    val isEnabled: Boolean = true,
    val badge: String? = null
)

@Serializable
data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val priority: NotificationPriority,
    val timestamp: Long,
    val isRead: Boolean = false,
    val actionLabel: String? = null,
    val actionData: String? = null
)

@Serializable
enum class NotificationType {
    INFO, WARNING, ERROR, SUCCESS, MEASUREMENT, BLUETOOTH, EXPORT, UPDATE
}

@Serializable
data class SystemHealth(
    val overallHealth: Double = 1.0,
    val memoryHealth: Double = 1.0,
    val storageHealth: Double = 1.0,
    val batteryHealth: Double = 1.0,
    val networkHealth: Double = 1.0,
    val bluetoothHealth: Double = 1.0,
    val lastCheck: Long = System.currentTimeMillis(),
    val issues: List<String> = emptyList()
) {
    fun isHealthy(): Boolean = overallHealth > 0.8
    fun hasIssues(): Boolean = issues.isNotEmpty()
}

@Serializable
data class MeasurementState(
    val isServiceReady: Boolean = false,
    val isSessionActive: Boolean = false,
    val isMeasuring: Boolean = false,
    val isPaused: Boolean = false,
    val serviceState: String = "IDLE",
    val measurementCount: Int = 0,
    val sessionDuration: Long = 0,
    val lastMeasurementTime: Long = 0,
    val dataRate: Double = 0.0,
    val bufferUsage: Double = 0.0
)

@Serializable
data class SessionStatistics(
    val totalMeasurements: Int = 0,
    val validMeasurements: Int = 0,
    val averageSignalStrength: Double = 0.0,
    val maxSignalStrength: Double = 0.0,
    val minSignalStrength: Double = 0.0,
    val averageTemperature: Double = 0.0,
    val dataQuality: Double = 0.0,
    val spatialCoverage: Double = 0.0,
    val temporalConsistency: Double = 0.0,
    val detectedMaterials: Map<MaterialType, Int> = emptyMap()
)

@Serializable
data class CalibrationStatus(
    val isValid: Boolean = false,
    val lastCalibration: Long = 0,
    val expiresAt: Long = 0,
    val calibrationType: CalibrationType = CalibrationType.ZERO_OFFSET,
    val accuracy: Double = 0.0,
    val drift: Double = 0.0,
    val temperature: Double = 0.0,
    val isRequired: Boolean = false
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
    fun needsRecalibration(): Boolean = !isValid || isExpired() || isRequired
}

@Serializable
data class ChartData(
    val labels: List<String> = emptyList(),
    val datasets: List<ChartDataset> = emptyList(),
    val xAxisLabel: String = "",
    val yAxisLabel: String = "",
    val title: String = "",
    val type: ChartType = ChartType.LINE,
    val timeRange: TimeRange = TimeRange.LAST_HOUR
)

@Serializable
data class ChartDataset(
    val label: String,
    val data: List<Double>,
    val color: String,
    val lineWidth: Float = 2.0f,
    val fillArea: Boolean = false,
    val showPoints: Boolean = true
)

@Serializable
enum class ChartType {
    LINE, BAR, SCATTER, HEATMAP, CONTOUR, HISTOGRAM
}

@Serializable
enum class TimeRange {
    LAST_MINUTE, LAST_5_MINUTES, LAST_HOUR, LAST_DAY, LAST_WEEK, CUSTOM
}

@Serializable
data class MeasurementAlert(
    val id: String,
    val type: AlertType,
    val severity: ErrorSeverity,
    val message: String,
    val timestamp: Long,
    val isActive: Boolean = true,
    val autoResolve: Boolean = false,
    val actionRequired: Boolean = false
)

@Serializable
enum class AlertType {
    CALIBRATION_REQUIRED, BATTERY_LOW, SIGNAL_WEAK, TEMPERATURE_HIGH, 
    DATA_QUALITY_LOW, STORAGE_FULL, CONNECTION_LOST, SENSOR_ERROR
}

@Serializable
data class AnalysisFilterOptions(
    val dateRange: Pair<Long, Long>? = null,
    val materialTypes: List<MaterialType> = emptyList(),
    val qualityThreshold: Double = 0.0,
    val signalStrengthRange: Pair<Double, Double>? = null,
    val operators: List<String> = emptyList(),
    val projects: List<String> = emptyList(),
    val locations: List<String> = emptyList()
)

@Serializable
data class AnalysisSortOptions(
    val sortBy: SortField = SortField.TIMESTAMP,
    val sortOrder: SortOrder = SortOrder.DESCENDING,
    val groupBy: GroupField? = null
)

@Serializable
enum class SortField {
    TIMESTAMP, NAME, OPERATOR, QUALITY, SIGNAL_STRENGTH, MEASUREMENT_COUNT
}

@Serializable
enum class SortOrder {
    ASCENDING, DESCENDING
}

@Serializable
enum class GroupField {
    DATE, OPERATOR, PROJECT, LOCATION, MATERIAL_TYPE
}

@Serializable
data class ARObject(
    val id: String,
    val type: ARObjectType,
    val position: Triple<Float, Float, Float>,
    val rotation: Triple<Float, Float, Float>,
    val scale: Triple<Float, Float, Float>,
    val data: Any? = null,
    val isVisible: Boolean = true,
    val isInteractive: Boolean = true
)

@Serializable
enum class ARObjectType {
    EMF_POINT, MATERIAL_MARKER, INCLUSION_MARKER, MEASUREMENT_PLANE, 
    COORDINATE_SYSTEM, TEXT_LABEL, PROGRESS_BAR, HEATMAP_OVERLAY
}

@Serializable
data class RenderingStats(
    val frameRate: Double = 0.0,
    val renderTime: Double = 0.0,
    val triangleCount: Int = 0,
    val textureMemory: Long = 0,
    val vertexMemory: Long = 0,
    val drawCalls: Int = 0
)

@Serializable
data class ExportProgress(
    val sessionId: Long,
    val format: ExportFormat,
    val progress: Double = 0.0,
    val currentStep: String = "",
    val totalSteps: Int = 0,
    val currentStep: Int = 0,
    val estimatedTimeRemaining: Long = 0,
    val bytesProcessed: Long = 0,
    val totalBytes: Long = 0,
    val startTime: Long = System.currentTimeMillis()
) {
    fun isComplete(): Boolean = progress >= 1.0
    fun getElapsedTime(): Long = System.currentTimeMillis() - startTime
}

@Serializable
data class ExportRecord(
    val id: String,
    val sessionId: Long,
    val sessionName: String,
    val format: ExportFormat,
    val filePath: String,
    val fileSize: Long,
    val exportTime: Long,
    val duration: Long,
    val isSuccessful: Boolean,
    val error: String? = null
)

@Serializable
data class StorageInfo(
    val totalSpace: Long = 0,
    val freeSpace: Long = 0,
    val usedSpace: Long = 0,
    val appDataSize: Long = 0,
    val cacheSize: Long = 0,
    val exportSize: Long = 0,
    val databaseSize: Long = 0,
    val logSize: Long = 0
) {
    val usagePercentage: Double get() = if (totalSpace > 0) usedSpace.toDouble() / totalSpace else 0.0
    val isLowSpace: Boolean get() = usagePercentage > 0.9
}

@Serializable
data class PermissionStatus(
    val bluetoothPermission: Boolean = false,
    val locationPermission: Boolean = false,
    val cameraPermission: Boolean = false,
    val storagePermission: Boolean = false,
    val notificationPermission: Boolean = false,
    val phonePermission: Boolean = false
) {
    val allGranted: Boolean get() = bluetoothPermission && locationPermission && 
                                   cameraPermission && storagePermission
    val criticalMissing: Boolean get() = !bluetoothPermission || !storagePermission
}

@Serializable
data class UpdateInfo(
    val hasUpdate: Boolean = false,
    val currentVersion: String = "",
    val latestVersion: String = "",
    val updateSize: Long = 0,
    val releaseNotes: String = "",
    val isForced: Boolean = false,
    val downloadUrl: String = ""
)

@Serializable
data class DebugInfo(
    val isDebugMode: Boolean = false,
    val logLevel: LogLevel = LogLevel.INFO,
    val enableVerboseLogging: Boolean = false,
    val enablePerformanceMonitoring: Boolean = false,
    val enableCrashReporting: Boolean = true,
    val lastCrashTime: Long = 0,
    val crashCount: Int = 0,
    val memoryLeaks: Int = 0
)

@Serializable
data class BackupInfo(
    val lastBackup: Long = 0,
    val backupSize: Long = 0,
    val autoBackupEnabled: Boolean = false,
    val backupLocation: String = "",
    val backupCount: Int = 0,
    val cloudBackupEnabled: Boolean = false
)
