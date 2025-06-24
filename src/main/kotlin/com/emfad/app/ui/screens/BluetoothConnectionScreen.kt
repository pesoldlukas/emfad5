package com.emfad.app.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emfad.app.ui.theme.EMFADColors

/**
 * EMFAD® Bluetooth-Verbindungsscreen
 * Basiert auf COM-Port-Erkennung der originalen Windows-Software
 * Optimiert für Samsung S21 Ultra mit BLE-Unterstützung
 */

data class EMFADDevice(
    val name: String,
    val address: String,
    val deviceType: String,
    val rssi: Int = -50,
    val isConnected: Boolean = false,
    val isSupported: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothConnectionScreen(
    navController: androidx.navigation.NavController,
    modifier: Modifier = Modifier
) {
    // Verwende das ECHTE DashboardViewModel mit echten Bluetooth-Services
    val dashboardViewModel: com.emfad.app.viewmodels.dashboard.DashboardViewModel = androidx.hilt.navigation.compose.hiltViewModel()

    // Sammle echte Daten vom Backend
    val bluetoothState by dashboardViewModel.bluetoothState.collectAsState()
    val uiState by dashboardViewModel.uiState.collectAsState()

    // Konvertiere echte BluetoothDevice zu UI-EMFADDevice
    val devices = bluetoothState.availableDevices.map { device ->
        EMFADDevice(
            name = device.name ?: "Unbekanntes Gerät",
            address = device.address,
            deviceType = "EMFAD-UG12 DS WL",
            rssi = device.rssi,
            isConnected = bluetoothState.isConnected && device.address == bluetoothState.deviceInfo["address"],
            isSupported = device.name?.contains("EMFAD") == true || device.name?.contains("UG12") == true
        )
    }

    // Echte UI mit echten Backend-Daten
    RealBluetoothConnectionUI(
        devices = devices,
        isScanning = bluetoothState.isScanning,
        onStartScan = { dashboardViewModel.startBluetoothScan() },
        onStopScan = { dashboardViewModel.stopBluetoothScan() },
        onConnectDevice = { device ->
            // Konvertiere zurück zu echtem BluetoothDevice
            val realDevice = bluetoothState.availableDevices.find { it.address == device.address }
            realDevice?.let { dashboardViewModel.connectToDevice(it) }
        },
        onDisconnectDevice = { /* TODO: Implementiere Disconnect */ },
        onBack = { navController.popBackStack() },
        modifier = modifier
    )
}

@Composable
private fun RealBluetoothConnectionUI(
    devices: List<EMFADDevice>,
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnectDevice: (EMFADDevice) -> Unit,
    onDisconnectDevice: (EMFADDevice) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EMFADColors.BackgroundPrimary)
            .padding(16.dp)
    ) {
        // Header mit Zurück-Button
        EMFADConnectionHeader(
            onBack = onBack,
            isScanning = isScanning
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Scan-Kontrollen
        EMFADScanControls(
            isScanning = isScanning,
            onStartScan = onStartScan,
            onStopScan = onStopScan
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Geräteliste
        EMFADDeviceList(
            devices = devices,
            onConnectDevice = onConnectDevice,
            onDisconnectDevice = onDisconnectDevice,
            modifier = Modifier.weight(1f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Verbindungsinfo
        EMFADConnectionInfo()
    }
}

@Composable
private fun EMFADConnectionHeader(
    onBack: () -> Unit,
    isScanning: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EMFADColors.SurfacePrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Zurück",
                    tint = EMFADColors.TextPrimary
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "EMFAD® Geräteverbindung",
                    style = MaterialTheme.typography.headlineMedium,
                    color = EMFADColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = if (isScanning) "Suche nach EMFAD-Geräten..." else "Bluetooth-Geräte verwalten",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EMFADColors.TextSecondary
                )
            }
            
            // Scan-Status-Indikator
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = EMFADColors.EMFADBlue,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = "Bluetooth",
                    modifier = Modifier.size(24.dp),
                    tint = EMFADColors.EMFADBlue
                )
            }
        }
    }
}

@Composable
private fun EMFADScanControls(
    isScanning: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EMFADColors.SurfaceSecondary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    text = "Geräte-Scan",
                    style = MaterialTheme.typography.titleMedium,
                    color = EMFADColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "EMFAD-UG12 DS WL Geräte suchen",
                    style = MaterialTheme.typography.bodySmall,
                    color = EMFADColors.TextSecondary
                )
            }
            
            if (isScanning) {
                Button(
                    onClick = onStopScan,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADColors.SignalRed
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop")
                }
            } else {
                Button(
                    onClick = onStartScan,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EMFADColors.EMFADBlue
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Scan",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan")
                }
            }
        }
    }
}

@Composable
private fun EMFADDeviceList(
    devices: List<EMFADDevice>,
    onConnectDevice: (EMFADDevice) -> Unit,
    onDisconnectDevice: (EMFADDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EMFADColors.SurfacePrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Gefundene Geräte (${devices.size})",
                style = MaterialTheme.typography.titleMedium,
                color = EMFADColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (devices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BluetoothSearching,
                            contentDescription = "Keine Geräte",
                            modifier = Modifier.size(48.dp),
                            tint = EMFADColors.TextTertiary
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Keine EMFAD-Geräte gefunden",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EMFADColors.TextTertiary,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "Starten Sie einen Scan",
                            style = MaterialTheme.typography.bodySmall,
                            color = EMFADColors.TextTertiary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(devices) { device ->
                        EMFADDeviceItem(
                            device = device,
                            onConnect = { onConnectDevice(device) },
                            onDisconnect = { onDisconnectDevice(device) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EMFADDeviceItem(
    device: EMFADDevice,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (device.isConnected) 
                EMFADColors.StatusConnected.copy(alpha = 0.1f) 
            else 
                EMFADColors.SurfaceSecondary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Geräte-Icon
            Icon(
                imageVector = when {
                    device.isConnected -> Icons.Default.BluetoothConnected
                    device.isSupported -> Icons.Default.Bluetooth
                    else -> Icons.Default.BluetoothDisabled
                },
                contentDescription = "Device Type",
                modifier = Modifier.size(32.dp),
                tint = when {
                    device.isConnected -> EMFADColors.StatusConnected
                    device.isSupported -> EMFADColors.EMFADBlue
                    else -> EMFADColors.StatusDisconnected
                }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Geräte-Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = EMFADColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = EMFADColors.TextSecondary
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = device.deviceType,
                        style = MaterialTheme.typography.labelSmall,
                        color = EMFADColors.EMFADBlue
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "RSSI: ${device.rssi} dBm",
                        style = MaterialTheme.typography.labelSmall,
                        color = EMFADColors.TextTertiary
                    )
                }
            }
            
            // Verbindungs-Button
            if (device.isSupported) {
                if (device.isConnected) {
                    Button(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EMFADColors.SignalRed
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "Trennen",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    Button(
                        onClick = onConnect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EMFADColors.EMFADBlue
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "Verbinden",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            } else {
                Text(
                    text = "Nicht unterstützt",
                    style = MaterialTheme.typography.labelSmall,
                    color = EMFADColors.TextTertiary
                )
            }
        }
    }
}

@Composable
private fun EMFADConnectionInfo() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = EMFADColors.SurfaceSecondary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Verbindungsinfo",
                style = MaterialTheme.typography.titleSmall,
                color = EMFADColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Unterstützte Geräte:",
                    style = MaterialTheme.typography.bodySmall,
                    color = EMFADColors.TextSecondary
                )
                
                Text(
                    text = "EMFAD-UG12 DS WL",
                    style = MaterialTheme.typography.bodySmall,
                    color = EMFADColors.EMFADBlue
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Protokoll:",
                    style = MaterialTheme.typography.bodySmall,
                    color = EMFADColors.TextSecondary
                )
                
                Text(
                    text = "Bluetooth LE",
                    style = MaterialTheme.typography.bodySmall,
                    color = EMFADColors.EMFADBlue
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Frequenzbereich:",
                    style = MaterialTheme.typography.bodySmall,
                    color = EMFADColors.TextSecondary
                )
                
                Text(
                    text = "19-135.6 kHz",
                    style = MaterialTheme.typography.bodySmall,
                    color = EMFADColors.EMFADBlue
                )
            }
        }
    }
}
