package com.emfad.app.utils.config

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * EMFAD Configuration Manager
 * Zentrale Konfigurationsverwaltung für alle App-Einstellungen
 * Samsung S21 Ultra optimiert
 */
class ConfigurationManager(context: Context) {
    
    companion object {
        private const val TAG = "ConfigurationManager"
        private const val PREFS_NAME = "emfad_config"
        private const val CONFIG_VERSION = "1.0.0"
    }
    
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // Configuration State
    private val _config = MutableStateFlow(EMFADConfiguration())
    val config: StateFlow<EMFADConfiguration> = _config.asStateFlow()
    
    init {
        loadConfiguration()
    }
    
    /**
     * Konfiguration laden
     */
    private fun loadConfiguration() {
        try {
            Log.d(TAG, "Lade Konfiguration")
            
            val config = EMFADConfiguration(
                // Bluetooth-Einstellungen
                bluetoothConfig = BluetoothConfiguration(
                    autoConnect = sharedPrefs.getBoolean("bluetooth_auto_connect", true),
                    scanTimeout = sharedPrefs.getLong("bluetooth_scan_timeout", 30000L),
                    connectionTimeout = sharedPrefs.getLong("bluetooth_connection_timeout", 15000L),
                    retryAttempts = sharedPrefs.getInt("bluetooth_retry_attempts", 3),
                    preferredDevices = getStringList("bluetooth_preferred_devices")
                ),
                
                // Mess-Einstellungen
                measurementConfig = MeasurementConfiguration(
                    defaultFrequency = sharedPrefs.getFloat("measurement_default_frequency", 100.0f).toDouble(),
                    defaultGain = sharedPrefs.getFloat("measurement_default_gain", 1.0f).toDouble(),
                    samplingRate = sharedPrefs.getFloat("measurement_sampling_rate", 10.0f).toDouble(),
                    bufferSize = sharedPrefs.getInt("measurement_buffer_size", 1000),
                    autoSaveInterval = sharedPrefs.getLong("measurement_auto_save_interval", 5000L),
                    qualityThreshold = sharedPrefs.getFloat("measurement_quality_threshold", 0.7f).toDouble(),
                    noiseThreshold = sharedPrefs.getFloat("measurement_noise_threshold", 50.0f).toDouble()
                ),
                
                // AI-Einstellungen
                aiConfig = AIConfiguration(
                    enableMaterialClassification = sharedPrefs.getBoolean("ai_enable_material_classification", true),
                    enableClusterAnalysis = sharedPrefs.getBoolean("ai_enable_cluster_analysis", true),
                    enableInclusionDetection = sharedPrefs.getBoolean("ai_enable_inclusion_detection", true),
                    confidenceThreshold = sharedPrefs.getFloat("ai_confidence_threshold", 0.7f).toDouble(),
                    processingThreads = sharedPrefs.getInt("ai_processing_threads", 4),
                    useGPUAcceleration = sharedPrefs.getBoolean("ai_use_gpu_acceleration", true)
                ),
                
                // AR-Einstellungen
                arConfig = ARConfiguration(
                    enableAR = sharedPrefs.getBoolean("ar_enable", true),
                    fallbackMode = sharedPrefs.getBoolean("ar_fallback_mode", true),
                    visualizationMode = VisualizationMode.valueOf(
                        sharedPrefs.getString("ar_visualization_mode", "SIGNAL_STRENGTH") ?: "SIGNAL_STRENGTH"
                    ),
                    scaleFactor = sharedPrefs.getFloat("ar_scale_factor", 1.0f),
                    pointSize = sharedPrefs.getFloat("ar_point_size", 10.0f),
                    showMaterialTypes = sharedPrefs.getBoolean("ar_show_material_types", true),
                    showInclusions = sharedPrefs.getBoolean("ar_show_inclusions", true)
                ),
                
                // Export-Einstellungen
                exportConfig = ExportConfiguration(
                    defaultFormat = ExportFormat.valueOf(
                        sharedPrefs.getString("export_default_format", "CSV") ?: "CSV"
                    ),
                    includeRawData = sharedPrefs.getBoolean("export_include_raw_data", true),
                    includeAnalyses = sharedPrefs.getBoolean("export_include_analyses", true),
                    compressionEnabled = sharedPrefs.getBoolean("export_compression_enabled", false),
                    autoExport = sharedPrefs.getBoolean("export_auto_export", false),
                    exportPath = sharedPrefs.getString("export_path", "") ?: ""
                ),
                
                // UI-Einstellungen
                uiConfig = UIConfiguration(
                    theme = Theme.valueOf(sharedPrefs.getString("ui_theme", "SYSTEM") ?: "SYSTEM"),
                    language = sharedPrefs.getString("ui_language", "de") ?: "de",
                    showAdvancedOptions = sharedPrefs.getBoolean("ui_show_advanced_options", false),
                    enableHapticFeedback = sharedPrefs.getBoolean("ui_enable_haptic_feedback", true),
                    chartAnimations = sharedPrefs.getBoolean("ui_chart_animations", true),
                    refreshRate = sharedPrefs.getInt("ui_refresh_rate", 60)
                ),
                
                // Sicherheits-Einstellungen
                securityConfig = SecurityConfiguration(
                    enableDataEncryption = sharedPrefs.getBoolean("security_enable_data_encryption", false),
                    requireAuthentication = sharedPrefs.getBoolean("security_require_authentication", false),
                    sessionTimeout = sharedPrefs.getLong("security_session_timeout", 3600000L),
                    enableAuditLog = sharedPrefs.getBoolean("security_enable_audit_log", true),
                    dataRetentionDays = sharedPrefs.getInt("security_data_retention_days", 365)
                ),
                
                // Performance-Einstellungen
                performanceConfig = PerformanceConfiguration(
                    enablePerformanceMonitoring = sharedPrefs.getBoolean("performance_enable_monitoring", true),
                    maxMemoryUsage = sharedPrefs.getLong("performance_max_memory_usage", 512L * 1024 * 1024),
                    enableBackgroundProcessing = sharedPrefs.getBoolean("performance_enable_background_processing", true),
                    cpuThrottling = sharedPrefs.getBoolean("performance_cpu_throttling", false),
                    batteryOptimization = sharedPrefs.getBoolean("performance_battery_optimization", true)
                ),
                
                // Debug-Einstellungen
                debugConfig = DebugConfiguration(
                    enableDebugMode = sharedPrefs.getBoolean("debug_enable_debug_mode", false),
                    logLevel = LogLevel.valueOf(sharedPrefs.getString("debug_log_level", "INFO") ?: "INFO"),
                    enableVerboseLogging = sharedPrefs.getBoolean("debug_enable_verbose_logging", false),
                    saveLogsToFile = sharedPrefs.getBoolean("debug_save_logs_to_file", false),
                    enableCrashReporting = sharedPrefs.getBoolean("debug_enable_crash_reporting", true)
                ),
                
                // Metadaten
                configVersion = sharedPrefs.getString("config_version", CONFIG_VERSION) ?: CONFIG_VERSION,
                lastModified = sharedPrefs.getLong("config_last_modified", System.currentTimeMillis())
            )
            
            _config.value = config
            Log.d(TAG, "Konfiguration erfolgreich geladen")
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Laden der Konfiguration", e)
            _config.value = EMFADConfiguration() // Default-Konfiguration
        }
    }
    
    /**
     * Konfiguration speichern
     */
    fun saveConfiguration(config: EMFADConfiguration) {
        try {
            Log.d(TAG, "Speichere Konfiguration")
            
            val editor = sharedPrefs.edit()
            
            // Bluetooth-Einstellungen
            with(config.bluetoothConfig) {
                editor.putBoolean("bluetooth_auto_connect", autoConnect)
                editor.putLong("bluetooth_scan_timeout", scanTimeout)
                editor.putLong("bluetooth_connection_timeout", connectionTimeout)
                editor.putInt("bluetooth_retry_attempts", retryAttempts)
                putStringList(editor, "bluetooth_preferred_devices", preferredDevices)
            }
            
            // Mess-Einstellungen
            with(config.measurementConfig) {
                editor.putFloat("measurement_default_frequency", defaultFrequency.toFloat())
                editor.putFloat("measurement_default_gain", defaultGain.toFloat())
                editor.putFloat("measurement_sampling_rate", samplingRate.toFloat())
                editor.putInt("measurement_buffer_size", bufferSize)
                editor.putLong("measurement_auto_save_interval", autoSaveInterval)
                editor.putFloat("measurement_quality_threshold", qualityThreshold.toFloat())
                editor.putFloat("measurement_noise_threshold", noiseThreshold.toFloat())
            }
            
            // AI-Einstellungen
            with(config.aiConfig) {
                editor.putBoolean("ai_enable_material_classification", enableMaterialClassification)
                editor.putBoolean("ai_enable_cluster_analysis", enableClusterAnalysis)
                editor.putBoolean("ai_enable_inclusion_detection", enableInclusionDetection)
                editor.putFloat("ai_confidence_threshold", confidenceThreshold.toFloat())
                editor.putInt("ai_processing_threads", processingThreads)
                editor.putBoolean("ai_use_gpu_acceleration", useGPUAcceleration)
            }
            
            // AR-Einstellungen
            with(config.arConfig) {
                editor.putBoolean("ar_enable", enableAR)
                editor.putBoolean("ar_fallback_mode", fallbackMode)
                editor.putString("ar_visualization_mode", visualizationMode.name)
                editor.putFloat("ar_scale_factor", scaleFactor)
                editor.putFloat("ar_point_size", pointSize)
                editor.putBoolean("ar_show_material_types", showMaterialTypes)
                editor.putBoolean("ar_show_inclusions", showInclusions)
            }
            
            // Export-Einstellungen
            with(config.exportConfig) {
                editor.putString("export_default_format", defaultFormat.name)
                editor.putBoolean("export_include_raw_data", includeRawData)
                editor.putBoolean("export_include_analyses", includeAnalyses)
                editor.putBoolean("export_compression_enabled", compressionEnabled)
                editor.putBoolean("export_auto_export", autoExport)
                editor.putString("export_path", exportPath)
            }
            
            // UI-Einstellungen
            with(config.uiConfig) {
                editor.putString("ui_theme", theme.name)
                editor.putString("ui_language", language)
                editor.putBoolean("ui_show_advanced_options", showAdvancedOptions)
                editor.putBoolean("ui_enable_haptic_feedback", enableHapticFeedback)
                editor.putBoolean("ui_chart_animations", chartAnimations)
                editor.putInt("ui_refresh_rate", refreshRate)
            }
            
            // Sicherheits-Einstellungen
            with(config.securityConfig) {
                editor.putBoolean("security_enable_data_encryption", enableDataEncryption)
                editor.putBoolean("security_require_authentication", requireAuthentication)
                editor.putLong("security_session_timeout", sessionTimeout)
                editor.putBoolean("security_enable_audit_log", enableAuditLog)
                editor.putInt("security_data_retention_days", dataRetentionDays)
            }
            
            // Performance-Einstellungen
            with(config.performanceConfig) {
                editor.putBoolean("performance_enable_monitoring", enablePerformanceMonitoring)
                editor.putLong("performance_max_memory_usage", maxMemoryUsage)
                editor.putBoolean("performance_enable_background_processing", enableBackgroundProcessing)
                editor.putBoolean("performance_cpu_throttling", cpuThrottling)
                editor.putBoolean("performance_battery_optimization", batteryOptimization)
            }
            
            // Debug-Einstellungen
            with(config.debugConfig) {
                editor.putBoolean("debug_enable_debug_mode", enableDebugMode)
                editor.putString("debug_log_level", logLevel.name)
                editor.putBoolean("debug_enable_verbose_logging", enableVerboseLogging)
                editor.putBoolean("debug_save_logs_to_file", saveLogsToFile)
                editor.putBoolean("debug_enable_crash_reporting", enableCrashReporting)
            }
            
            // Metadaten
            editor.putString("config_version", CONFIG_VERSION)
            editor.putLong("config_last_modified", System.currentTimeMillis())
            
            editor.apply()
            _config.value = config.copy(lastModified = System.currentTimeMillis())
            
            Log.d(TAG, "Konfiguration erfolgreich gespeichert")
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Speichern der Konfiguration", e)
        }
    }
    
    /**
     * Einzelne Konfiguration aktualisieren
     */
    fun updateBluetoothConfig(bluetoothConfig: BluetoothConfiguration) {
        val updatedConfig = _config.value.copy(bluetoothConfig = bluetoothConfig)
        saveConfiguration(updatedConfig)
    }
    
    fun updateMeasurementConfig(measurementConfig: MeasurementConfiguration) {
        val updatedConfig = _config.value.copy(measurementConfig = measurementConfig)
        saveConfiguration(updatedConfig)
    }
    
    fun updateAIConfig(aiConfig: AIConfiguration) {
        val updatedConfig = _config.value.copy(aiConfig = aiConfig)
        saveConfiguration(updatedConfig)
    }
    
    fun updateARConfig(arConfig: ARConfiguration) {
        val updatedConfig = _config.value.copy(arConfig = arConfig)
        saveConfiguration(updatedConfig)
    }
    
    fun updateUIConfig(uiConfig: UIConfiguration) {
        val updatedConfig = _config.value.copy(uiConfig = uiConfig)
        saveConfiguration(updatedConfig)
    }
    
    /**
     * Konfiguration auf Standard zurücksetzen
     */
    fun resetToDefaults() {
        Log.d(TAG, "Setze Konfiguration auf Standard zurück")
        sharedPrefs.edit().clear().apply()
        _config.value = EMFADConfiguration()
        saveConfiguration(_config.value)
    }
    
    /**
     * String-Liste speichern
     */
    private fun putStringList(editor: SharedPreferences.Editor, key: String, list: List<String>) {
        val json = gson.toJson(list)
        editor.putString(key, json)
    }
    
    /**
     * String-Liste laden
     */
    private fun getStringList(key: String): List<String> {
        val json = sharedPrefs.getString(key, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Laden der String-Liste für $key", e)
            emptyList()
        }
    }
    
    /**
     * Konfiguration exportieren
     */
    fun exportConfiguration(): String {
        return gson.toJson(_config.value)
    }
    
    /**
     * Konfiguration importieren
     */
    fun importConfiguration(configJson: String): Boolean {
        return try {
            val config = gson.fromJson(configJson, EMFADConfiguration::class.java)
            saveConfiguration(config)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Importieren der Konfiguration", e)
            false
        }
    }
}
