package com.emfad.app.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.emfad.app.BuildConfig
import com.emfad.app.ui.theme.EMFADColors
import com.emfad.app.utils.PerformanceStats
import com.emfad.app.utils.PerformanceWarning
import com.emfad.app.viewmodels.DebugViewModel

/**
 * EMFAD® Debug Dashboard
 * Live-Monitoring für Samsung S21 Ultra Testing
 * Nur in Debug-Builds verfügbar
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugDashboard(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DebugViewModel = hiltViewModel()
) {
    // Nur in Debug-Builds anzeigen
    if (!BuildConfig.DEBUG) {
        return
    }
    
    val performanceStats by viewModel.performanceStats.collectAsState()
    val performanceWarnings by viewModel.performanceWarnings.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val appInfo by viewModel.appInfo.collectAsState()
    val isMonitoring by viewModel.isMonitoring.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EMFADColors.BackgroundPrimary)
            .padding(16.dp)
    ) {
        // Header
        DebugHeader(
            onClose = onClose,
            isMonitoring = isMonitoring,
            onToggleMonitoring = viewModel::toggleMonitoring
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Performance Stats
            item {
                PerformanceStatsCard(performanceStats)
            }
            
            // Performance Warnings
            if (performanceWarnings.isNotEmpty()) {
                item {
                    PerformanceWarningsCard(performanceWarnings)
                }
            }
            
            // Device Info
            item {
                DeviceInfoCard(deviceInfo)
            }
            
            // App Info
            item {
                AppInfoCard(appInfo)
            }
            
            // EMFAD Services Status
            item {
                EMFADServicesCard(viewModel)
            }
        }
    }
}

@Composable
private fun DebugHeader(
    onClose: () -> Unit,
    isMonitoring: Boolean,
    onToggleMonitoring: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EMFADColors.SurfacePrimary
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🔧 EMFAD Debug Dashboard",
                    style = MaterialTheme.typography.headlineSmall,
                    color = EMFADColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Samsung S21 Ultra Live Monitoring",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EMFADColors.TextSecondary
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Monitoring Toggle
                IconButton(
                    onClick = onToggleMonitoring,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isMonitoring) EMFADColors.SignalGreen else EMFADColors.EMFADGray
                    )
                ) {
                    Icon(
                        imageVector = if (isMonitoring) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isMonitoring) "Stop Monitoring" else "Start Monitoring",
                        tint = EMFADColors.TextPrimary
                    )
                }
                
                // Close Button
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Debug Dashboard",
                        tint = EMFADColors.TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun PerformanceStatsCard(stats: PerformanceStats?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EMFADColors.SurfaceSecondary
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Performance",
                    tint = EMFADColors.EMFADBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Performance Stats",
                    style = MaterialTheme.typography.titleMedium,
                    color = EMFADColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (stats != null) {
                // Memory Usage
                DebugMetric(
                    label = "Memory Usage",
                    value = "${stats.memoryUsageMB} MB",
                    isGood = stats.memoryUsageMB < 400,
                    icon = Icons.Default.Memory
                )
                
                // CPU Usage
                DebugMetric(
                    label = "CPU Usage",
                    value = "${String.format("%.1f", stats.cpuUsagePercent)}%",
                    isGood = stats.cpuUsagePercent < 30.0,
                    icon = Icons.Default.Computer
                )
                
                // Overall Status
                DebugMetric(
                    label = "Overall Status",
                    value = if (stats.isOptimized) "Optimized" else "Needs Optimization",
                    isGood = stats.isOptimized,
                    icon = if (stats.isOptimized) Icons.Default.CheckCircle else Icons.Default.Warning
                )
            } else {
                Text(
                    text = "No performance data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EMFADColors.TextTertiary
                )
            }
        }
    }
}

@Composable
private fun PerformanceWarningsCard(warnings: List<PerformanceWarning>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EMFADColors.SignalOrange.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warnings",
                    tint = EMFADColors.SignalOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Performance Warnings (${warnings.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = EMFADColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            warnings.forEach { warning ->
                val warningText = when (warning) {
                    is PerformanceWarning.HighMemoryUsage -> 
                        "High Memory: ${warning.currentMB}MB (limit: ${warning.limitMB}MB)"
                    is PerformanceWarning.HighCpuUsage -> 
                        "High CPU: ${String.format("%.1f", warning.currentPercent)}% (limit: ${warning.limitPercent}%)"
                    PerformanceWarning.MemoryOptimizationTriggered -> 
                        "Memory optimization triggered"
                    PerformanceWarning.CpuOptimizationTriggered -> 
                        "CPU optimization triggered"
                }
                
                Text(
                    text = "⚠️ $warningText",
                    style = MaterialTheme.typography.bodySmall,
                    color = EMFADColors.SignalOrange,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(deviceInfo: Map<String, String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EMFADColors.SurfaceSecondary
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = "Device Info",
                    tint = EMFADColors.EMFADBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Device Information",
                    style = MaterialTheme.typography.titleMedium,
                    color = EMFADColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            deviceInfo.forEach { (key, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.bodySmall,
                        color = EMFADColors.TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = EMFADColors.TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppInfoCard(appInfo: Map<String, String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EMFADColors.SurfaceSecondary
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = "App Info",
                    tint = EMFADColors.EMFADBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "App Information",
                    style = MaterialTheme.typography.titleMedium,
                    color = EMFADColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            appInfo.forEach { (key, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.bodySmall,
                        color = EMFADColors.TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = EMFADColors.TextPrimary,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EMFADServicesCard(viewModel: DebugViewModel) {
    val servicesStatus by viewModel.servicesStatus.collectAsState()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EMFADColors.SurfaceSecondary
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "EMFAD Services",
                    tint = EMFADColors.EMFADBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EMFAD Services Status",
                    style = MaterialTheme.typography.titleMedium,
                    color = EMFADColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            servicesStatus.forEach { (serviceName, isRunning) ->
                DebugMetric(
                    label = serviceName,
                    value = if (isRunning) "Running" else "Stopped",
                    isGood = isRunning,
                    icon = if (isRunning) Icons.Default.CheckCircle else Icons.Default.Cancel
                )
            }
        }
    }
}

@Composable
private fun DebugMetric(
    label: String,
    value: String,
    isGood: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isGood) EMFADColors.SignalGreen else EMFADColors.SignalRed,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = EMFADColors.TextSecondary
            )
        }
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = if (isGood) EMFADColors.SignalGreen else EMFADColors.SignalRed,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}
