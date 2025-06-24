package com.emfad.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emfad.app.models.*
import com.emfad.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * EMFAD UI Components
 * Wiederverwendbare Komponenten für Samsung S21 Ultra optimiert
 */

// Stat Card für Dashboard
@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(120.dp),
        shape = EMFADCustomShapes.measurementCard
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary
            )
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .height(20.dp)
                        .width(40.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            EMFADCustomShapes.confidenceBar
                        )
                )
            } else {
                Text(
                    text = value,
                    style = EMFADTextStyles.measurementValue
                )
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Session Card
@Composable
fun SessionCard(
    session: MeasurementSession,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = EMFADCustomShapes.sessionCard
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = session.name,
                    style = EMFADTextStyles.sessionName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                SessionStatusChip(status = session.status)
            }
            
            Text(
                text = session.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SessionInfoChip(
                    icon = Icons.Default.Person,
                    text = session.operatorName
                )
                
                SessionInfoChip(
                    icon = Icons.Default.Schedule,
                    text = formatTimestamp(session.startTimestamp)
                )
                
                SessionInfoChip(
                    icon = Icons.Default.Science,
                    text = "${session.measurementCount}"
                )
            }
        }
    }
}

// Session Status Chip
@Composable
fun SessionStatusChip(
    status: SessionStatus,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (status) {
        SessionStatus.ACTIVE -> MaterialTheme.colorScheme.primary to "Aktiv"
        SessionStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary to "Abgeschlossen"
        SessionStatus.PAUSED -> MaterialTheme.colorScheme.secondary to "Pausiert"
        SessionStatus.ERROR -> MaterialTheme.colorScheme.error to "Fehler"
    }
    
    Surface(
        modifier = modifier,
        color = color,
        shape = EMFADCustomShapes.statusIndicator
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = EMFADTextStyles.statusText,
            color = getContentColor(color)
        )
    }
}

// Session Info Chip
@Composable
fun SessionInfoChip(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Quick Action Card
@Composable
fun QuickActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = EMFADCustomShapes.deviceCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Measurement Value Display
@Composable
fun MeasurementValueDisplay(
    label: String,
    value: String,
    unit: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.Baseline,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = EMFADTextStyles.measurementValue,
                color = color
            )
            Text(
                text = unit,
                style = EMFADTextStyles.measurementUnit,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Signal Strength Indicator
@Composable
fun SignalStrengthIndicator(
    strength: Double,
    maxStrength: Double = 1000.0,
    modifier: Modifier = Modifier
) {
    val normalizedStrength = (strength / maxStrength).coerceIn(0.0, 1.0)
    val color = getSignalStrengthColor(strength)
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(8.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    EMFADCustomShapes.confidenceBar
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(normalizedStrength.toFloat())
                    .background(color, EMFADCustomShapes.confidenceBar)
            )
        }
        
        Text(
            text = "${"%.0f".format(strength)}",
            style = EMFADTextStyles.technicalData,
            color = color
        )
    }
}

// Material Type Chip
@Composable
fun MaterialTypeChip(
    materialType: MaterialType,
    confidence: Double,
    modifier: Modifier = Modifier
) {
    val color = getMaterialTypeColor(materialType.toString())
    
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = EMFADCustomShapes.materialChip,
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = materialType.toString(),
                style = EMFADTextStyles.materialType,
                color = color
            )
            
            Text(
                text = "${"%.0f".format(confidence * 100)}%",
                style = EMFADTextStyles.confidenceValue,
                color = color
            )
        }
    }
}

// Bluetooth Permission Prompt
@Composable
fun BluetoothPermissionPrompt(
    onSetupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Bluetooth-Berechtigungen erforderlich",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            Text(
                text = "Für die Verbindung mit EMFAD-Geräten werden Bluetooth-Berechtigungen benötigt.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Button(
                onClick = onSetupClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Setup starten")
            }
        }
    }
}

// Connected Device Info
@Composable
fun ConnectedDeviceInfo(
    deviceInfo: Map<String, Any>,
    batteryLevel: Int,
    onDisconnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = deviceInfo["device_name"]?.toString() ?: "EMFAD Gerät",
                    style = EMFADTextStyles.deviceName,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                BatteryIndicator(
                    level = batteryLevel,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Text(
                text = "ID: ${deviceInfo["device_id"]?.toString() ?: "Unbekannt"}",
                style = EMFADTextStyles.technicalData,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            OutlinedButton(
                onClick = onDisconnectClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("Trennen")
            }
        }
    }
}

// Available Devices List
@Composable
fun AvailableDevicesList(
    devices: List<BluetoothDevice>,
    onConnectClick: (BluetoothDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Verfügbare Geräte:",
            style = MaterialTheme.typography.labelMedium
        )
        
        devices.forEach { device ->
            DeviceListItem(
                device = device,
                onConnectClick = { onConnectClick(device) }
            )
        }
    }
}

// Device List Item
@Composable
fun DeviceListItem(
    device: BluetoothDevice,
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = device.address,
                    style = EMFADTextStyles.technicalData,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Button(
                onClick = onConnectClick,
                size = ButtonDefaults.SmallButtonSize
            ) {
                Text("Verbinden")
            }
        }
    }
}

// Bluetooth Scan Prompt
@Composable
fun BluetoothScanPrompt(
    isScanning: Boolean,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isScanning) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Text(
                text = "Suche nach Geräten...",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Text(
                text = "Keine Geräte gefunden",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onScanClick) {
                Text("Scan starten")
            }
        }
    }
}

// Battery Indicator
@Composable
fun BatteryIndicator(
    level: Int,
    color: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = when {
                level > 75 -> Icons.Default.BatteryFull
                level > 50 -> Icons.Default.Battery6Bar
                level > 25 -> Icons.Default.Battery3Bar
                else -> Icons.Default.Battery1Bar
            },
            contentDescription = "Batterie",
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Text(
            text = "$level%",
            style = EMFADTextStyles.technicalData,
            color = color
        )
    }
}

// Session Card Skeleton (Loading)
@Composable
fun SessionCardSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = EMFADCustomShapes.sessionCard
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(20.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        EMFADCustomShapes.confidenceBar
                    )
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        EMFADCustomShapes.confidenceBar
                    )
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(14.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                EMFADCustomShapes.confidenceBar
                            )
                    )
                }
            }
        }
    }
}

// Helper Functions
private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
