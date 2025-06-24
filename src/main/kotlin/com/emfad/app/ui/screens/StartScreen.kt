package com.emfad.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emfad.app.R
import com.emfad.app.ui.theme.EMFADColors

/**
 * EMFAD® Startscreen
 * Basiert auf der originalen Windows-Software EMFAD-EMUNI
 * Optimiert für Samsung S21 Ultra (2400x1080, 6.8", HDR10+)
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(
    navController: androidx.navigation.NavController,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EMFADColors.BackgroundPrimary)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // EMFAD® Header mit Logo
        EMFADHeader()
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Hauptmenü-Buttons (wie in originaler Software) - mit echter Navigation
        EMFADMainMenu(
            onStartScan = { navController.navigate("measurement_recorder") },
            onStartProfile = { navController.navigate("profile_view") },
            onOpen = { navController.navigate("analysis") },
            onSetup = { navController.navigate("setup") },
            onAutoBalance = { navController.navigate("auto_balance") },
            onMapView = { navController.navigate("map") }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Status-Leiste (wie in originaler Software)
        EMFADStatusBar()
    }
}

@Composable
private fun EMFADHeader() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = EMFADColors.SurfacePrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // EMFAD® Logo/Icon
            Icon(
                imageVector = Icons.Default.Radar,
                contentDescription = "EMFAD Logo",
                modifier = Modifier.size(64.dp),
                tint = EMFADColors.EMFADBlue
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // EMFAD® Titel
            Text(
                text = "EMFAD®",
                style = MaterialTheme.typography.displayLarge,
                color = EMFADColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Elektromagnetische Feldanalyse und Detektion",
                style = MaterialTheme.typography.titleMedium,
                color = EMFADColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "EMFAD-UG12 DS WL • Android Version",
                style = MaterialTheme.typography.bodyMedium,
                color = EMFADColors.EMFADBlue,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EMFADMainMenu(
    onStartScan: () -> Unit,
    onStartProfile: () -> Unit,
    onOpen: () -> Unit,
    onSetup: () -> Unit,
    onAutoBalance: () -> Unit,
    onMapView: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Erste Reihe: Start Scan, Start Profile
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EMFADMenuButton(
                text = "Start Scan",
                icon = Icons.Default.PlayArrow,
                onClick = onStartScan,
                modifier = Modifier.weight(1f),
                backgroundColor = EMFADColors.EMFADBlue
            )
            
            EMFADMenuButton(
                text = "Start Profile",
                icon = Icons.Default.Timeline,
                onClick = onStartProfile,
                modifier = Modifier.weight(1f),
                backgroundColor = EMFADColors.EMFADBlue
            )
        }
        
        // Zweite Reihe: Open, Setup
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EMFADMenuButton(
                text = "Open",
                icon = Icons.Default.FolderOpen,
                onClick = onOpen,
                modifier = Modifier.weight(1f),
                backgroundColor = EMFADColors.EMFADGray
            )
            
            EMFADMenuButton(
                text = "Setup",
                icon = Icons.Default.Settings,
                onClick = onSetup,
                modifier = Modifier.weight(1f),
                backgroundColor = EMFADColors.EMFADGray
            )
        }
        
        // Dritte Reihe: AutoBalance, Map View
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EMFADMenuButton(
                text = "AutoBalance",
                icon = Icons.Default.Balance,
                onClick = onAutoBalance,
                modifier = Modifier.weight(1f),
                backgroundColor = EMFADColors.EMFADYellowDark
            )
            
            EMFADMenuButton(
                text = "Map View",
                icon = Icons.Default.Map,
                onClick = onMapView,
                modifier = Modifier.weight(1f),
                backgroundColor = EMFADColors.StatusInfo
            )
        }
    }
}

@Composable
private fun EMFADMenuButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = EMFADColors.EMFADBlue
) {
    Card(
        modifier = modifier
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(24.dp),
                tint = EMFADColors.TextPrimary
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = EMFADColors.TextPrimary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EMFADStatusBar() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
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
            // Gerätestatus
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BluetoothDisabled,
                    contentDescription = "Device Status",
                    modifier = Modifier.size(16.dp),
                    tint = EMFADColors.StatusDisconnected
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "No Device",
                    style = MaterialTheme.typography.bodySmall,
                    color = EMFADColors.TextSecondary
                )
            }
            
            // Frequenz-Status
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Frequency Status",
                    modifier = Modifier.size(16.dp),
                    tint = EMFADColors.EMFADBlue
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "19-135 kHz",
                    style = MaterialTheme.typography.bodySmall,
                    color = EMFADColors.TextSecondary
                )
            }
            
            // App-Version
            Text(
                text = "v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = EMFADColors.TextTertiary
            )
        }
    }
}
