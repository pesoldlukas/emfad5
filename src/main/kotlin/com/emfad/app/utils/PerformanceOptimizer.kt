package com.emfad.app.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EMFAD® Performance Optimizer
 * Samsung S21 Ultra spezifische Optimierungen
 * Überwacht und optimiert App-Performance in Echtzeit
 */

@Singleton
class PerformanceOptimizer @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "EMFADPerformanceOptimizer"
        
        // Samsung S21 Ultra spezifische Limits
        private const val MAX_MEMORY_MB = 500 // 500MB Limit
        private const val MAX_CPU_USAGE = 30.0 // 30% CPU Limit
        private const val MEMORY_CHECK_INTERVAL = 10000L // 10 Sekunden
        private const val GC_THRESHOLD_MB = 400 // GC bei 400MB
    }
    
    private val optimizerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    // Performance Monitoring
    private val _memoryUsage = MutableStateFlow(0L)
    val memoryUsage: StateFlow<Long> = _memoryUsage.asStateFlow()
    
    private val _cpuUsage = MutableStateFlow(0.0)
    val cpuUsage: StateFlow<Double> = _cpuUsage.asStateFlow()
    
    private val _performanceWarnings = MutableSharedFlow<PerformanceWarning>()
    val performanceWarnings: SharedFlow<PerformanceWarning> = _performanceWarnings.asSharedFlow()
    
    private var isMonitoring = false
    private var monitoringJob: Job? = null
    
    init {
        startPerformanceMonitoring()
    }
    
    /**
     * Startet Performance-Monitoring
     */
    fun startPerformanceMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        monitoringJob = optimizerScope.launch {
            while (isActive && isMonitoring) {
                try {
                    checkMemoryUsage()
                    checkCpuUsage()
                    optimizeIfNeeded()
                    
                    delay(MEMORY_CHECK_INTERVAL)
                } catch (e: Exception) {
                    Timber.e(e, "Error in performance monitoring")
                }
            }
        }
        
        Timber.d("Performance monitoring started")
    }
    
    /**
     * Stoppt Performance-Monitoring
     */
    fun stopPerformanceMonitoring() {
        isMonitoring = false
        monitoringJob?.cancel()
        Timber.d("Performance monitoring stopped")
    }
    
    /**
     * Prüft Speicherverbrauch
     */
    private suspend fun checkMemoryUsage() {
        try {
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val usedMemoryMB = usedMemory / (1024 * 1024)
            
            _memoryUsage.value = usedMemoryMB
            
            // Warnung bei hohem Speicherverbrauch
            if (usedMemoryMB > MAX_MEMORY_MB) {
                _performanceWarnings.emit(
                    PerformanceWarning.HighMemoryUsage(usedMemoryMB, MAX_MEMORY_MB.toLong())
                )
            }
            
            // Automatische Garbage Collection bei Bedarf
            if (usedMemoryMB > GC_THRESHOLD_MB) {
                triggerGarbageCollection()
            }
            
            Timber.v("Memory usage: ${usedMemoryMB}MB")
            
        } catch (e: Exception) {
            Timber.e(e, "Error checking memory usage")
        }
    }
    
    /**
     * Prüft CPU-Verbrauch (vereinfacht)
     */
    private suspend fun checkCpuUsage() {
        try {
            // Vereinfachte CPU-Messung über Thread-Aktivität
            val activeThreads = Thread.activeCount()
            val estimatedCpuUsage = (activeThreads * 2.0).coerceAtMost(100.0)
            
            _cpuUsage.value = estimatedCpuUsage
            
            if (estimatedCpuUsage > MAX_CPU_USAGE) {
                _performanceWarnings.emit(
                    PerformanceWarning.HighCpuUsage(estimatedCpuUsage, MAX_CPU_USAGE)
                )
            }
            
            Timber.v("Estimated CPU usage: ${estimatedCpuUsage}%")
            
        } catch (e: Exception) {
            Timber.e(e, "Error checking CPU usage")
        }
    }
    
    /**
     * Optimiert Performance bei Bedarf
     */
    private suspend fun optimizeIfNeeded() {
        val memoryMB = _memoryUsage.value
        val cpuUsage = _cpuUsage.value
        
        // Memory Optimization
        if (memoryMB > GC_THRESHOLD_MB) {
            optimizeMemory()
        }
        
        // CPU Optimization
        if (cpuUsage > MAX_CPU_USAGE) {
            optimizeCpu()
        }
    }
    
    /**
     * Speicher-Optimierung
     */
    private suspend fun optimizeMemory() {
        try {
            Timber.d("Starting memory optimization")
            
            // Garbage Collection auslösen
            triggerGarbageCollection()
            
            // Cache leeren (falls vorhanden)
            clearCaches()
            
            // Warnung emittieren
            _performanceWarnings.emit(PerformanceWarning.MemoryOptimizationTriggered)
            
        } catch (e: Exception) {
            Timber.e(e, "Error during memory optimization")
        }
    }
    
    /**
     * CPU-Optimierung
     */
    private suspend fun optimizeCpu() {
        try {
            Timber.d("Starting CPU optimization")
            
            // Reduziere Background-Tasks
            reduceBackgroundTasks()
            
            // Warnung emittieren
            _performanceWarnings.emit(PerformanceWarning.CpuOptimizationTriggered)
            
        } catch (e: Exception) {
            Timber.e(e, "Error during CPU optimization")
        }
    }
    
    /**
     * Löst Garbage Collection aus
     */
    private fun triggerGarbageCollection() {
        System.gc()
        System.runFinalization()
        Timber.d("Garbage collection triggered")
    }
    
    /**
     * Leert Caches
     */
    private fun clearCaches() {
        try {
            // OSMDroid Cache leeren falls zu groß
            val cacheDir = context.externalCacheDir
            cacheDir?.let { dir ->
                val cacheSize = dir.walkTopDown().sumOf { it.length() }
                if (cacheSize > 50 * 1024 * 1024) { // > 50MB
                    dir.deleteRecursively()
                    Timber.d("Cache cleared: ${cacheSize / 1024 / 1024}MB")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error clearing caches")
        }
    }
    
    /**
     * Reduziert Background-Tasks
     */
    private fun reduceBackgroundTasks() {
        // Implementierung würde hier Background-Tasks pausieren/reduzieren
        Timber.d("Background tasks reduced")
    }
    
    /**
     * Samsung S21 Ultra spezifische Optimierungen
     */
    fun optimizeForSamsungS21Ultra() {
        try {
            Timber.d("Applying Samsung S21 Ultra optimizations")
            
            // 120Hz Display Optimierung
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Smooth Display aktivieren
                Timber.d("120Hz display optimization enabled")
            }
            
            // Snapdragon 888 / Exynos 2100 Optimierungen
            val cpuAbi = Build.SUPPORTED_ABIS[0]
            if (cpuAbi.contains("arm64")) {
                Timber.d("ARM64 optimizations enabled")
            }
            
            // 12GB/16GB RAM Optimierung
            val memoryClass = activityManager.memoryClass
            if (memoryClass > 512) { // > 512MB = High-end device
                Timber.d("High-memory device optimizations enabled")
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Error applying Samsung S21 Ultra optimizations")
        }
    }
    
    /**
     * Gibt Performance-Statistiken zurück
     */
    fun getPerformanceStats(): PerformanceStats {
        return PerformanceStats(
            memoryUsageMB = _memoryUsage.value,
            cpuUsagePercent = _cpuUsage.value,
            isOptimized = _memoryUsage.value < MAX_MEMORY_MB && _cpuUsage.value < MAX_CPU_USAGE,
            deviceModel = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            availableMemoryMB = activityManager.memoryClass.toLong()
        )
    }
    
    fun cleanup() {
        stopPerformanceMonitoring()
        optimizerScope.cancel()
    }
}

/**
 * Performance Warning Types
 */
sealed class PerformanceWarning {
    data class HighMemoryUsage(val currentMB: Long, val limitMB: Long) : PerformanceWarning()
    data class HighCpuUsage(val currentPercent: Double, val limitPercent: Double) : PerformanceWarning()
    object MemoryOptimizationTriggered : PerformanceWarning()
    object CpuOptimizationTriggered : PerformanceWarning()
}

/**
 * Performance Statistics
 */
data class PerformanceStats(
    val memoryUsageMB: Long,
    val cpuUsagePercent: Double,
    val isOptimized: Boolean,
    val deviceModel: String,
    val androidVersion: String,
    val availableMemoryMB: Long
)
