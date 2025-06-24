package com.emfad.app.models.config

import com.emfad.app.models.enums.*
import kotlinx.serialization.Serializable

/**
 * Configuration Models
 * Konfigurationsmodelle für Samsung S21 Ultra optimiert
 */

// Main Configuration
@Serializable
data class EMFADConfiguration(
    val bluetoothConfig: BluetoothConfiguration = BluetoothConfiguration(),
    val measurementConfig: MeasurementConfiguration = MeasurementConfiguration(),
    val aiConfig: AIConfiguration = AIConfiguration(),
    val arConfig: ARConfiguration = ARConfiguration(),
    val exportConfig: ExportConfiguration = ExportConfiguration(),
    val uiConfig: UIConfiguration = UIConfiguration(),
    val securityConfig: SecurityConfiguration = SecurityConfiguration(),
    val performanceConfig: PerformanceConfiguration = PerformanceConfiguration(),
    val debugConfig: DebugConfiguration = DebugConfiguration(),
    val configVersion: String = "1.0.0",
    val lastModified: Long = System.currentTimeMillis()
) {
    fun isValid(): Boolean {
        return bluetoothConfig.isValid() && 
               measurementConfig.isValid() && 
               aiConfig.isValid() && 
               arConfig.isValid()
    }
    
    fun needsUpdate(): Boolean {
        return configVersion != "1.0.0" || 
               (System.currentTimeMillis() - lastModified) > 30 * 24 * 60 * 60 * 1000L // 30 days
    }
}

// Bluetooth Configuration
@Serializable
data class BluetoothConfiguration(
    val autoConnect: Boolean = true,
    val scanTimeout: Long = 30000L, // 30 seconds
    val connectionTimeout: Long = 15000L, // 15 seconds
    val retryAttempts: Int = 3,
    val retryDelay: Long = 2000L, // 2 seconds
    val preferredDevices: List<String> = emptyList(),
    val blacklistedDevices: List<String> = emptyList(),
    val enableLowEnergyMode: Boolean = true,
    val enableBackgroundScanning: Boolean = false,
    val scanInterval: Long = 60000L, // 1 minute
    val connectionKeepAlive: Boolean = true,
    val keepAliveInterval: Long = 30000L, // 30 seconds
    val enableNotifications: Boolean = true,
    val enableIndications: Boolean = true,
    val mtu: Int = 512, // Maximum Transmission Unit
    val connectionPriority: Int = 1, // 0=Low, 1=Balanced, 2=High
    val enableBonding: Boolean = false,
    val securityLevel: Int = 1, // 0=None, 1=Encrypted, 2=Authenticated
    val enableLogging: Boolean = true,
    val logLevel: LogLevel = LogLevel.INFO
) {
    fun isValid(): Boolean {
        return scanTimeout > 0 && 
               connectionTimeout > 0 && 
               retryAttempts >= 0 && 
               mtu in 23..512 && 
               connectionPriority in 0..2
    }
    
    fun getOptimalSettings(): BluetoothConfiguration {
        return copy(
            scanTimeout = 20000L,
            connectionTimeout = 10000L,
            retryAttempts = 2,
            mtu = 247, // Optimal for most devices
            connectionPriority = 1
        )
    }
}

// Measurement Configuration
@Serializable
data class MeasurementConfiguration(
    val defaultFrequency: Double = 100.0,
    val frequencyRange: Pair<Double, Double> = Pair(1.0, 1000.0),
    val defaultGain: Double = 1.0,
    val gainRange: Pair<Double, Double> = Pair(0.1, 100.0),
    val samplingRate: Double = 10.0,
    val samplingRateRange: Pair<Double, Double> = Pair(0.1, 100.0),
    val bufferSize: Int = 1000,
    val maxBufferSize: Int = 10000,
    val autoSaveInterval: Long = 5000L, // 5 seconds
    val autoSaveEnabled: Boolean = true,
    val qualityThreshold: Double = 0.7,
    val noiseThreshold: Double = 50.0,
    val temperatureCompensation: Boolean = true,
    val backgroundSubtraction: Boolean = false,
    val autoCalibration: Boolean = true,
    val calibrationInterval: Long = 24 * 60 * 60 * 1000L, // 24 hours
    val defaultFilter: FilterType = FilterType.LOW_PASS,
    val filterParameters: Map<String, Double> = mapOf(
        "cutoff_frequency" to 50.0,
        "order" to 4.0,
        "ripple" to 0.1
    ),
    val measurementTimeout: Long = 30000L, // 30 seconds
    val maxMeasurementDuration: Long = 3600000L, // 1 hour
    val enableRealTimeProcessing: Boolean = true,
    val enableDataValidation: Boolean = true,
    val validationRules: List<ValidationRule> = getDefaultValidationRules(),
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val precision: Int = 3, // decimal places
    val enableStatistics: Boolean = true,
    val statisticsInterval: Long = 1000L // 1 second
) {
    fun isValid(): Boolean {
        return defaultFrequency in frequencyRange.first..frequencyRange.second &&
               defaultGain in gainRange.first..gainRange.second &&
               samplingRate in samplingRateRange.first..samplingRateRange.second &&
               bufferSize > 0 && bufferSize <= maxBufferSize &&
               qualityThreshold in 0.0..1.0 &&
               precision in 0..6
    }
    
    companion object {
        fun getDefaultValidationRules(): List<ValidationRule> {
            return listOf(
                ValidationRule("signal_range", "Signal strength must be between 0 and 2000", 
                              mapOf("min" to 0.0, "max" to 2000.0)),
                ValidationRule("frequency_range", "Frequency must be between 1 and 1000 Hz", 
                              mapOf("min" to 1.0, "max" to 1000.0)),
                ValidationRule("temperature_range", "Temperature must be between -40 and 85°C", 
                              mapOf("min" to -40.0, "max" to 85.0)),
                ValidationRule("quality_threshold", "Quality score must be above 0.5", 
                              mapOf("min" to 0.5))
            )
        }
    }
}

// AI Configuration
@Serializable
data class AIConfiguration(
    val enableMaterialClassification: Boolean = true,
    val enableClusterAnalysis: Boolean = true,
    val enableInclusionDetection: Boolean = true,
    val enableAnomalyDetection: Boolean = true,
    val confidenceThreshold: Double = 0.7,
    val processingThreads: Int = 4,
    val maxProcessingThreads: Int = 8,
    val useGPUAcceleration: Boolean = true,
    val enableNNAPI: Boolean = true,
    val modelPath: String = "models/",
    val materialClassifierModel: String = "material_classifier_v2.tflite",
    val clusterAnalysisModel: String = "cluster_analysis_v1.tflite",
    val inclusionDetectorModel: String = "inclusion_detector_v1.tflite",
    val batchSize: Int = 32,
    val maxBatchSize: Int = 128,
    val enableModelCaching: Boolean = true,
    val cacheSize: Long = 100 * 1024 * 1024L, // 100MB
    val enableQuantization: Boolean = true,
    val quantizationBits: Int = 8,
    val enablePruning: Boolean = false,
    val pruningRatio: Double = 0.1,
    val enableDistillation: Boolean = false,
    val teacherModelPath: String = "",
    val enableOnlineTraining: Boolean = false,
    val trainingDataPath: String = "training_data/",
    val maxTrainingData: Long = 1024 * 1024 * 1024L, // 1GB
    val enableFederatedLearning: Boolean = false,
    val federatedLearningServer: String = "",
    val enablePrivacyPreservation: Boolean = true,
    val enableExplainableAI: Boolean = true,
    val explanationMethod: String = "LIME",
    val enableUncertaintyQuantification: Boolean = true,
    val uncertaintyMethod: String = "Monte Carlo Dropout",
    val enableActivelearning: Boolean = false,
    val activeLearningStrategy: String = "uncertainty_sampling"
) {
    fun isValid(): Boolean {
        return confidenceThreshold in 0.0..1.0 &&
               processingThreads > 0 && processingThreads <= maxProcessingThreads &&
               batchSize > 0 && batchSize <= maxBatchSize &&
               quantizationBits in 1..32 &&
               pruningRatio in 0.0..1.0
    }
    
    fun getOptimalSettings(): AIConfiguration {
        val cores = Runtime.getRuntime().availableProcessors()
        return copy(
            processingThreads = minOf(cores, 6),
            batchSize = if (useGPUAcceleration) 64 else 16,
            enableQuantization = true,
            quantizationBits = 8
        )
    }
}

// AR Configuration
@Serializable
data class ARConfiguration(
    val enableAR: Boolean = true,
    val fallbackMode: Boolean = true,
    val visualizationMode: VisualizationMode = VisualizationMode.SIGNAL_STRENGTH,
    val scaleFactor: Float = 1.0f,
    val scaleRange: Pair<Float, Float> = Pair(0.1f, 10.0f),
    val pointSize: Float = 10.0f,
    val pointSizeRange: Pair<Float, Float> = Pair(1.0f, 50.0f),
    val showMaterialTypes: Boolean = true,
    val showInclusions: Boolean = true,
    val showCoordinates: Boolean = false,
    val showGrid: Boolean = false,
    val showAxes: Boolean = true,
    val enablePlaneDetection: Boolean = true,
    val enableLightEstimation: Boolean = true,
    val enableOcclusion: Boolean = false,
    val enableShadows: Boolean = true,
    val enableReflections: Boolean = false,
    val renderDistance: Float = 100.0f,
    val maxObjects: Int = 1000,
    val lodEnabled: Boolean = true, // Level of Detail
    val lodDistances: List<Float> = listOf(10.0f, 50.0f, 100.0f),
    val enableCulling: Boolean = true,
    val cullingDistance: Float = 200.0f,
    val frameRate: Int = 60,
    val targetFrameRate: Int = 60,
    val enableVSync: Boolean = true,
    val antiAliasing: Boolean = true,
    val antialiasingLevel: Int = 4,
    val textureQuality: String = "HIGH", // LOW, MEDIUM, HIGH, ULTRA
    val shaderQuality: String = "HIGH",
    val enablePostProcessing: Boolean = true,
    val bloomEnabled: Boolean = false,
    val bloomIntensity: Float = 0.5f,
    val enableMotionBlur: Boolean = false,
    val enableDepthOfField: Boolean = false,
    val enableColorGrading: Boolean = false,
    val enableHDR: Boolean = false,
    val enableToneMapping: Boolean = false,
    val enableGammaCorrection: Boolean = true,
    val gamma: Float = 2.2f
) {
    fun isValid(): Boolean {
        return scaleFactor in scaleRange.first..scaleRange.second &&
               pointSize in pointSizeRange.first..pointSizeRange.second &&
               renderDistance > 0 &&
               maxObjects > 0 &&
               frameRate > 0 &&
               antialiasingLevel in 0..16 &&
               gamma > 0
    }
    
    fun getPerformanceSettings(): ARConfiguration {
        return copy(
            textureQuality = "MEDIUM",
            shaderQuality = "MEDIUM",
            antiAliasing = false,
            enablePostProcessing = false,
            enableShadows = false,
            lodEnabled = true,
            enableCulling = true,
            maxObjects = 500
        )
    }
    
    fun getQualitySettings(): ARConfiguration {
        return copy(
            textureQuality = "ULTRA",
            shaderQuality = "ULTRA",
            antiAliasing = true,
            antialiasingLevel = 8,
            enablePostProcessing = true,
            enableShadows = true,
            enableReflections = true,
            enableHDR = true,
            maxObjects = 2000
        )
    }
}

// UI Configuration
@Serializable
data class UIConfiguration(
    val theme: Theme = Theme.SYSTEM,
    val language: Language = Language.GERMAN,
    val showAdvancedOptions: Boolean = false,
    val enableHapticFeedback: Boolean = true,
    val enableSoundEffects: Boolean = true,
    val chartAnimations: Boolean = true,
    val animationDuration: Int = 300, // milliseconds
    val refreshRate: Int = 60,
    val enableAutoRefresh: Boolean = true,
    val autoRefreshInterval: Long = 5000L, // 5 seconds
    val enableNotifications: Boolean = true,
    val notificationSound: Boolean = true,
    val notificationVibration: Boolean = true,
    val enableTooltips: Boolean = true,
    val tooltipDelay: Int = 1000, // milliseconds
    val enableGestures: Boolean = true,
    val swipeToRefresh: Boolean = true,
    val pullToRefresh: Boolean = true,
    val enableKeyboardShortcuts: Boolean = true,
    val fontSize: String = "MEDIUM", // SMALL, MEDIUM, LARGE, EXTRA_LARGE
    val fontFamily: String = "DEFAULT", // DEFAULT, ROBOTO, OPEN_SANS
    val enableHighContrast: Boolean = false,
    val enableColorBlindSupport: Boolean = false,
    val colorBlindType: String = "NONE", // NONE, PROTANOPIA, DEUTERANOPIA, TRITANOPIA
    val enableReducedMotion: Boolean = false,
    val enableScreenReader: Boolean = false,
    val enableVoiceControl: Boolean = false,
    val gridSize: Int = 8, // dp
    val cornerRadius: Int = 8, // dp
    val elevation: Int = 4, // dp
    val enableMaterialYou: Boolean = true,
    val enableDynamicColors: Boolean = true,
    val customColorScheme: Map<String, String> = emptyMap(),
    val enableCustomThemes: Boolean = false,
    val customThemes: List<String> = emptyList()
) {
    fun isValid(): Boolean {
        return animationDuration > 0 &&
               refreshRate > 0 &&
               autoRefreshInterval > 0 &&
               tooltipDelay >= 0 &&
               gridSize > 0 &&
               cornerRadius >= 0 &&
               elevation >= 0
    }
    
    fun getAccessibilitySettings(): UIConfiguration {
        return copy(
            enableHighContrast = true,
            enableReducedMotion = true,
            enableScreenReader = true,
            fontSize = "LARGE",
            animationDuration = 150,
            chartAnimations = false
        )
    }
}

// Security Configuration
@Serializable
data class SecurityConfiguration(
    val enableDataEncryption: Boolean = false,
    val encryptionAlgorithm: String = "AES-256-GCM",
    val keyDerivationFunction: String = "PBKDF2",
    val keyIterations: Int = 100000,
    val requireAuthentication: Boolean = false,
    val authenticationType: String = "BIOMETRIC", // BIOMETRIC, PIN, PASSWORD, PATTERN
    val sessionTimeout: Long = 3600000L, // 1 hour
    val maxFailedAttempts: Int = 5,
    val lockoutDuration: Long = 300000L, // 5 minutes
    val enableAuditLog: Boolean = true,
    val auditLogLevel: LogLevel = LogLevel.INFO,
    val auditLogRetention: Int = 90, // days
    val enableIntegrityCheck: Boolean = true,
    val enableTamperDetection: Boolean = false,
    val enableSecureStorage: Boolean = true,
    val enableSecureTransmission: Boolean = true,
    val tlsVersion: String = "TLS_1_3",
    val certificatePinning: Boolean = false,
    val enableObfuscation: Boolean = false,
    val enableAntiDebugging: Boolean = false,
    val enableRootDetection: Boolean = false,
    val dataRetentionDays: Int = 365,
    val enableDataAnonymization: Boolean = false,
    val enableDataPseudonymization: Boolean = false,
    val enableGDPRCompliance: Boolean = true,
    val enableCCPACompliance: Boolean = false,
    val privacyPolicyVersion: String = "1.0",
    val termsOfServiceVersion: String = "1.0"
) {
    fun isValid(): Boolean {
        return keyIterations > 0 &&
               sessionTimeout > 0 &&
               maxFailedAttempts > 0 &&
               lockoutDuration > 0 &&
               auditLogRetention > 0 &&
               dataRetentionDays > 0
    }
    
    fun getHighSecuritySettings(): SecurityConfiguration {
        return copy(
            enableDataEncryption = true,
            requireAuthentication = true,
            sessionTimeout = 900000L, // 15 minutes
            maxFailedAttempts = 3,
            enableIntegrityCheck = true,
            enableTamperDetection = true,
            enableSecureStorage = true,
            enableSecureTransmission = true,
            certificatePinning = true,
            enableRootDetection = true
        )
    }
}

// Performance Configuration
@Serializable
data class PerformanceConfiguration(
    val enablePerformanceMonitoring: Boolean = true,
    val monitoringInterval: Long = 1000L, // 1 second
    val maxMemoryUsage: Long = 512L * 1024 * 1024, // 512MB
    val memoryWarningThreshold: Double = 0.8,
    val memoryCriticalThreshold: Double = 0.95,
    val enableBackgroundProcessing: Boolean = true,
    val maxBackgroundTasks: Int = 4,
    val backgroundTaskTimeout: Long = 30000L, // 30 seconds
    val enableCpuThrottling: Boolean = false,
    val cpuThrottleThreshold: Double = 0.8,
    val enableBatteryOptimization: Boolean = true,
    val batteryOptimizationLevel: String = "BALANCED", // PERFORMANCE, BALANCED, BATTERY_SAVER
    val enableNetworkOptimization: Boolean = true,
    val networkTimeout: Long = 10000L, // 10 seconds
    val maxConcurrentConnections: Int = 5,
    val enableCaching: Boolean = true,
    val cacheSize: Long = 50L * 1024 * 1024, // 50MB
    val cacheEvictionPolicy: String = "LRU", // LRU, LFU, FIFO
    val enableCompression: Boolean = true,
    val compressionLevel: Int = 6,
    val enableLazyLoading: Boolean = true,
    val enablePrefetching: Boolean = false,
    val prefetchDistance: Int = 10,
    val enableGarbageCollection: Boolean = true,
    val gcStrategy: String = "GENERATIONAL", // GENERATIONAL, CONCURRENT, PARALLEL
    val enableJIT: Boolean = true,
    val jitThreshold: Int = 1000,
    val enableProfileGuidedOptimization: Boolean = false
) {
    fun isValid(): Boolean {
        return monitoringInterval > 0 &&
               maxMemoryUsage > 0 &&
               memoryWarningThreshold in 0.0..1.0 &&
               memoryCriticalThreshold in 0.0..1.0 &&
               maxBackgroundTasks > 0 &&
               backgroundTaskTimeout > 0 &&
               networkTimeout > 0 &&
               maxConcurrentConnections > 0 &&
               cacheSize > 0 &&
               compressionLevel in 0..9
    }
    
    fun getBatteryOptimizedSettings(): PerformanceConfiguration {
        return copy(
            batteryOptimizationLevel = "BATTERY_SAVER",
            enableBackgroundProcessing = false,
            enableCpuThrottling = true,
            enableNetworkOptimization = true,
            enableCaching = true,
            enableLazyLoading = true,
            enablePrefetching = false
        )
    }
    
    fun getPerformanceOptimizedSettings(): PerformanceConfiguration {
        return copy(
            batteryOptimizationLevel = "PERFORMANCE",
            enableBackgroundProcessing = true,
            maxBackgroundTasks = 8,
            enableCpuThrottling = false,
            enableCaching = true,
            cacheSize = 100L * 1024 * 1024, // 100MB
            enablePrefetching = true,
            enableJIT = true
        )
    }
}

// Debug Configuration
@Serializable
data class DebugConfiguration(
    val enableDebugMode: Boolean = false,
    val logLevel: LogLevel = LogLevel.INFO,
    val enableVerboseLogging: Boolean = false,
    val enableFileLogging: Boolean = false,
    val logFilePath: String = "logs/",
    val maxLogFileSize: Long = 10L * 1024 * 1024, // 10MB
    val maxLogFiles: Int = 5,
    val enableConsoleLogging: Boolean = true,
    val enableRemoteLogging: Boolean = false,
    val remoteLoggingServer: String = "",
    val enableCrashReporting: Boolean = true,
    val crashReportingService: String = "Firebase", // Firebase, Bugsnag, Sentry
    val enablePerformanceProfiling: Boolean = false,
    val profilingInterval: Long = 100L, // milliseconds
    val enableMemoryProfiling: Boolean = false,
    val enableNetworkProfiling: Boolean = false,
    val enableDatabaseProfiling: Boolean = false,
    val enableUIDebugging: Boolean = false,
    val showLayoutBounds: Boolean = false,
    val showPerformanceOverlay: Boolean = false,
    val enableStrictMode: Boolean = false,
    val strictModePolicy: String = "LOG", // LOG, CRASH, DIALOG
    val enableLeakCanary: Boolean = false,
    val enableMethodTracing: Boolean = false,
    val methodTracingDuration: Long = 10000L, // 10 seconds
    val enableSQLiteDebugging: Boolean = false,
    val enableBluetoothDebugging: Boolean = false,
    val enableAIDebugging: Boolean = false,
    val enableARDebugging: Boolean = false
) {
    fun isValid(): Boolean {
        return maxLogFileSize > 0 &&
               maxLogFiles > 0 &&
               profilingInterval > 0 &&
               methodTracingDuration > 0
    }
    
    fun getProductionSettings(): DebugConfiguration {
        return copy(
            enableDebugMode = false,
            logLevel = LogLevel.WARNING,
            enableVerboseLogging = false,
            enableFileLogging = false,
            enableConsoleLogging = false,
            enableCrashReporting = true,
            enablePerformanceProfiling = false,
            enableStrictMode = false,
            enableLeakCanary = false
        )
    }
    
    fun getDevelopmentSettings(): DebugConfiguration {
        return copy(
            enableDebugMode = true,
            logLevel = LogLevel.DEBUG,
            enableVerboseLogging = true,
            enableFileLogging = true,
            enableConsoleLogging = true,
            enablePerformanceProfiling = true,
            enableUIDebugging = true,
            enableStrictMode = true,
            enableLeakCanary = true
        )
    }
}

// Validation Rule
@Serializable
data class ValidationRule(
    val name: String,
    val description: String,
    val parameters: Map<String, Double>,
    val isEnabled: Boolean = true,
    val severity: ErrorSeverity = ErrorSeverity.WARNING
)
