package com.emfad.app

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

/**
 * EMFAD® Original Windows UI - Android Implementation
 * Exact replica of the original EMFAD Windows software interface
 */

@Composable
fun EMFADOriginalWindowsUI(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4169E1), // Royal Blue
                        Color(0xFF00BFFF)  // Deep Sky Blue
                    )
                )
            )
            .padding(16.dp)
    ) {
        // EMFAD Header - Exact replica of Windows version
        EMFADOriginalHeader()
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Main Function Grid - 4x3 layout like original
        EMFADOriginalFunctionGrid(navController)
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Bottom Control Bar - Like original
        EMFADOriginalBottomBar()
    }
}

@Composable
fun EMFADOriginalHeader() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // EMFAD Logo - Black text
            Text(
                text = "EMFAD",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                letterSpacing = 4.sp
            )
            
            // Red accent lines and dot (simplified representation)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left lines
                repeat(8) {
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.dp)
                            .background(Color.Black)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                
                // Red dot
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color.Red, CircleShape)
                )
                
                // Right lines
                Spacer(modifier = Modifier.width(2.dp))
                repeat(8) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.dp)
                            .background(Color.Black)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // EMFAD EMUNI subtitle in blue
            Text(
                text = "EMFAD EMUNI",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )
        }
    }
}

@Composable
fun EMFADOriginalFunctionGrid(navController: NavController) {
    // Original EMFAD Windows 4x3 Function Grid
    val functions = listOf(
        // Row 1
        listOf(
            EMFADOriginalFunction("assign COM", Icons.Default.Settings, "Assign COM Port"),
            EMFADOriginalFunction("profile", Icons.Default.BarChart, "Profile Analysis"),
            EMFADOriginalFunction("scan GPS", Icons.Default.GpsFixed, "GPS Scanning"),
            EMFADOriginalFunction("connect", Icons.Default.Bluetooth, "Bluetooth Connect")
        ),
        // Row 2
        listOf(
            EMFADOriginalFunction("tools", Icons.Default.Build, "Tools & Utilities"),
            EMFADOriginalFunction("spectrum", Icons.Default.ShowChart, "Spectrum Analysis"),
            EMFADOriginalFunction("path", Icons.Default.Route, "Path Planning"),
            EMFADOriginalFunction("AR", Icons.Default.ViewInAr, "Augmented Reality") // AR Button hinzugefügt
        ),
        // Row 3
        listOf(
            EMFADOriginalFunction("setup", Icons.Default.Settings, "System Setup"),
            EMFADOriginalFunction("scan 2D/3D", Icons.Default.Scanner, "2D/3D Scanning"),
            EMFADOriginalFunction("map", Icons.Default.Map, "Map View"),
            EMFADOriginalFunction("EMTOMO", Icons.Default.ViewInAr, "EMTOMO Analysis")
        )
    )
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        functions.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { function ->
                    if (function.name.isNotEmpty()) {
                        EMFADOriginalFunctionCard(
                            function = function,
                            modifier = Modifier.weight(1f),
                            onClick = { 
                                // Navigate based on function
                                when (function.name) {
                                    "connect" -> { /* Bluetooth connection */ }
                                    "spectrum" -> { /* Spectrum analysis */ }
                                    "scan GPS" -> { /* GPS scanning */ }
                                    "map" -> { /* Map view */ }
                                    "profile" -> { /* Profile analysis */ }
                                    "scan 2D/3D" -> { /* 2D/3D scanning */ }
                                    "EMTOMO" -> { /* EMTOMO analysis */ }
                                    "assign COM" -> { /* COM port assignment */ }
                                    "tools" -> { /* Tools & utilities */ }
                                    "path" -> { /* Path planning */ }
                                    "setup" -> { /* System setup */ }
                                    "AR" -> { /* Augmented Reality mode */ }
                                }
                            }
                        )
                    } else {
                        // Empty slot
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun EMFADOriginalFunctionCard(
    function: EMFADOriginalFunction,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            function.icon?.let { icon ->
                Icon(
                    icon,
                    contentDescription = function.description,
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFF2196F3)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Function name
            Text(
                text = function.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EMFADOriginalBottomBar() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close application button
            Button(
                onClick = { /* Close application */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF4444)
                )
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close")
                Spacer(modifier = Modifier.width(4.dp))
                Text("close application", fontSize = 12.sp)
            }
            
            // Control indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("antenna A", fontSize = 12.sp, color = Color.Black)
                Text("parallel", fontSize = 12.sp, color = Color.Black)
                Text("filter 1", fontSize = 12.sp, color = Color.Black)
            }
        }
    }
}

// Data class for EMFAD functions
data class EMFADOriginalFunction(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector?,
    val description: String
)
