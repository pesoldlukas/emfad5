package com.emfad.app.viewmodels

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emfad.app.BuildConfig
import com.emfad.app.services.communication.DeviceCommunicationService
import com.emfad.app.services.frequency.FrequencyManager
import com.emfad.app.services.gps.GpsMapService
import com.emfad.app.utils.PerformanceOptimizer
import com.emfad.app.utils.PerformanceStats
import com.emfad.app.utils.PerformanceWarning
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * EMFAD® Debug ViewModel
 * Sammelt und verwaltet Debug-Informationen für Live-Testing
 */

@HiltViewModel
class DebugViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val performanceOptimizer: PerformanceOptimizer,
    private val deviceCommunicationService: DeviceCommunicationService,
    private val frequencyManager: FrequencyManager,
    private val gpsMapService: GpsMapService
) : ViewModel() {
    
    // Performance Monitoring
    val performanceStats: StateFlow<PerformanceStats?> = 
        performanceOptimizer.memoryUsage
            .combine(performanceOptimizer.cpuUsage) { memory, cpu ->
                performanceOptimizer.getPerformanceStats()
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    
    val performanceWarnings: StateFlow<List<PerformanceWarning>> = 
        performanceOptimizer.performanceWarnings
            .scan(emptyList<PerformanceWarning>()) { acc, warning ->
                (acc + warning).takeLast(10) // Behalte nur die letzten 10 Warnungen
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    
    // Device Information
    val deviceInfo: StateFlow<Map<String, String>> = flowOf(
        mapOf(
            "Model" to Build.MODEL,
            "Manufacturer" to Build.MANUFACTURER,
            "Android Version" to Build.VERSION.RELEASE,
            "API Level" to Build.VERSION.SDK_INT.toString(),
            "CPU ABI" to Build.SUPPORTED_ABIS[0],
            "RAM Class" to "${context.getSystemService(Context.ACTIVITY_SERVICE)?.let { 
                (it as android.app.ActivityManager).memoryClass 
            }}MB",
            "Screen Density" to "${context.resources.displayMetrics.densityDpi} dpi",
            "Screen Size" to "${context.resources.displayMetrics.widthPixels}x${context.resources.displayMetrics.heightPixels}"
        )
    ).stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())
    
    // App Information
    val appInfo: StateFlow<Map<String, String>> = flowOf(
        mapOf(
            "Package" to BuildConfig.APPLICATION_ID,
            "Version Name" to BuildConfig.VERSION_NAME,
            "Version Code" to BuildConfig.VERSION_CODE.toString(),
            "Build Type" to BuildConfig.BUILD_TYPE,
            "Debug" to BuildConfig.DEBUG.toString(),
            "Flavor" to BuildConfig.FLAVOR.ifEmpty { "default" },
            "Min SDK" to "26",
            "Target SDK" to "34"
        )
    ).stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())
    
    // Services Status
    val servicesStatus: StateFlow<Map<String, Boolean>> = 
        combine(
            deviceCommunicationService.connectionStatus,
            frequencyManager.isScanning,
            gpsMapService.isTracking
        ) { connectionStatus, isScanning, isTracking ->
            mapOf(
                "Device Communication" to (connectionStatus == com.emfad.app.services.communication.ConnectionStatus.CONNECTED),
                "Frequency Manager" to true, // Service ist immer aktiv
                "Frequency Scanning" to isScanning,
                "GPS Tracking" to isTracking,
                "Performance Optimizer" to true // Service ist immer aktiv
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyMap())
    
    // Monitoring State
    private val _isMonitoring = MutableStateFlow(true)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    
    init {
        startDebugMonitoring()
    }
    
    /**
     * Startet Debug-Monitoring
     */
    private fun startDebugMonitoring() {
        viewModelScope.launch {
            try {
                // Performance Optimizer für Samsung S21 Ultra optimieren
                performanceOptimizer.optimizeForSamsungS21Ultra()
                
                Timber.d("Debug monitoring started")
                
            } catch (e: Exception) {
                Timber.e(e, "Error starting debug monitoring")
            }
        }
    }
    
    /**
     * Schaltet Monitoring ein/aus
     */
    fun toggleMonitoring() {
        viewModelScope.launch {
            val newState = !_isMonitoring.value
            _isMonitoring.value = newState
            
            if (newState) {
                performanceOptimizer.startPerformanceMonitoring()
                Timber.d("Debug monitoring enabled")
            } else {
                performanceOptimizer.stopPerformanceMonitoring()
                Timber.d("Debug monitoring disabled")
            }
        }
    }
    
    /**
     * Sammelt detaillierte System-Informationen
     */
    fun collectSystemInfo(): Map<String, Any> {
        return try {
            val runtime = Runtime.getRuntime()
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memoryInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            mapOf(
                "device" to mapOf(
                    "model" to Build.MODEL,
                    "manufacturer" to Build.MANUFACTURER,
                    "brand" to Build.BRAND,
                    "product" to Build.PRODUCT,
                    "hardware" to Build.HARDWARE,
                    "board" to Build.BOARD,
                    "bootloader" to Build.BOOTLOADER,
                    "fingerprint" to Build.FINGERPRINT
                ),
                "android" to mapOf(
                    "version" to Build.VERSION.RELEASE,
                    "sdk" to Build.VERSION.SDK_INT,
                    "codename" to Build.VERSION.CODENAME,
                    "incremental" to Build.VERSION.INCREMENTAL,
                    "security_patch" to Build.VERSION.SECURITY_PATCH
                ),
                "cpu" to mapOf(
                    "abis" to Build.SUPPORTED_ABIS.toList(),
                    "abi" to Build.SUPPORTED_ABIS[0],
                    "cores" to runtime.availableProcessors()
                ),
                "memory" to mapOf(
                    "total_mb" to runtime.totalMemory() / 1024 / 1024,
                    "free_mb" to runtime.freeMemory() / 1024 / 1024,
                    "max_mb" to runtime.maxMemory() / 1024 / 1024,
                    "available_mb" to memoryInfo.availMem / 1024 / 1024,
                    "threshold_mb" to memoryInfo.threshold / 1024 / 1024,
                    "low_memory" to memoryInfo.lowMemory
                ),
                "display" to mapOf(
                    "width" to context.resources.displayMetrics.widthPixels,
                    "height" to context.resources.displayMetrics.heightPixels,
                    "density" to context.resources.displayMetrics.density,
                    "dpi" to context.resources.displayMetrics.densityDpi,
                    "scaled_density" to context.resources.displayMetrics.scaledDensity
                ),
                "app" to mapOf(
                    "package" to BuildConfig.APPLICATION_ID,
                    "version_name" to BuildConfig.VERSION_NAME,
                    "version_code" to BuildConfig.VERSION_CODE,
                    "build_type" to BuildConfig.BUILD_TYPE,
                    "debug" to BuildConfig.DEBUG
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Error collecting system info")
            emptyMap()
        }
    }
    
    /**
     * Exportiert Debug-Informationen
     */
    fun exportDebugInfo(): String {
        val systemInfo = collectSystemInfo()
        val performanceStats = performanceStats.value
        val warnings = performanceWarnings.value
        
        return buildString {
            appendLine("# EMFAD® Debug Report")
            appendLine("Generated: ${java.util.Date()}")
            appendLine()
            
            appendLine("## Device Information")
            systemInfo["device"]?.let { device ->
                (device as Map<*, *>).forEach { (key, value) ->
                    appendLine("$key: $value")
                }
            }
            appendLine()
            
            appendLine("## Performance Stats")
            performanceStats?.let { stats ->
                appendLine("Memory Usage: ${stats.memoryUsageMB} MB")
                appendLine("CPU Usage: ${String.format("%.1f", stats.cpuUsagePercent)}%")
                appendLine("Optimized: ${stats.isOptimized}")
            }
            appendLine()
            
            appendLine("## Performance Warnings")
            warnings.forEach { warning ->
                when (warning) {
                    is PerformanceWarning.HighMemoryUsage -> 
                        appendLine("High Memory: ${warning.currentMB}MB (limit: ${warning.limitMB}MB)")
                    is PerformanceWarning.HighCpuUsage -> 
                        appendLine("High CPU: ${String.format("%.1f", warning.currentPercent)}%")
                    PerformanceWarning.MemoryOptimizationTriggered -> 
                        appendLine("Memory optimization triggered")
                    PerformanceWarning.CpuOptimizationTriggered -> 
                        appendLine("CPU optimization triggered")
                }
            }
            appendLine()
            
            appendLine("## Services Status")
            servicesStatus.value.forEach { (service, status) ->
                appendLine("$service: ${if (status) "Running" else "Stopped"}")
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        performanceOptimizer.cleanup()
    }
}
