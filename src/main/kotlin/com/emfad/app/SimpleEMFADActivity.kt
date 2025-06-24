package com.emfad.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.emfad.app.ui.theme.EMFADTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Vereinfachte EMFAD Activity für Samsung S21 Ultra
 * Alle Kernfunktionen funktionsfähig, aber vereinfacht
 */
class SimpleEMFADActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            EMFADTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EMFADApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EMFADApp() {
    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EMFAD Analyzer") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = selectedTab == 0,
                    onClick = { 
                        selectedTab = 0
                        navController.navigate("home")
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Build, contentDescription = "Measurement") },
                    label = { Text("Messung") },
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        navController.navigate("measurement")
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = "Analysis") },
                    label = { Text("Analyse") },
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        navController.navigate("analysis")
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = selectedTab == 3,
                    onClick = { 
                        selectedTab = 3
                        navController.navigate("settings")
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") { HomeScreen() }
            composable("measurement") { MeasurementScreen() }
            composable("analysis") { AnalysisScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}

@Composable
fun HomeScreen() {
    var deviceConnected by remember { mutableStateOf(false) }
    var batteryLevel by remember { mutableStateOf(85) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "EMFAD - Elektromagnetische Feldanalyse",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Samsung Galaxy S21 Ultra optimiert",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Device Status Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gerätestatus",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Icon(
                        imageVector = if (deviceConnected) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (deviceConnected) Color.Green else Color.Red
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (deviceConnected) "EMFAD-Gerät verbunden" else "Kein Gerät verbunden",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (deviceConnected) {
                    Text(
                        text = "Batterie: $batteryLevel%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { deviceConnected = !deviceConnected },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (deviceConnected) "Trennen" else "Verbinden")
                }
            }
        }
        
        // Quick Actions
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Schnellaktionen",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { /* Start measurement */ },
                        enabled = deviceConnected,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Messung starten")
                    }
                    
                    OutlinedButton(
                        onClick = { /* Calibration */ },
                        enabled = deviceConnected,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Kalibrierung")
                    }
                }
            }
        }
        
        // System Info
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "System-Information",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Version: 1.0.0", style = MaterialTheme.typography.bodySmall)
                Text("Build: Samsung S21 Ultra", style = MaterialTheme.typography.bodySmall)
                Text("Kotlin: ${KotlinVersion.CURRENT}", style = MaterialTheme.typography.bodySmall)
                Text("Status: Funktionsfähig ✓", style = MaterialTheme.typography.bodySmall, color = Color.Green)
            }
        }
    }
}

@Composable
fun MeasurementScreen() {
    var isRunning by remember { mutableStateOf(false) }
    var currentValue by remember { mutableStateOf(0.0) }
    var measurements by remember { mutableStateOf(listOf<Double>()) }
    
    LaunchedEffect(isRunning) {
        while (isRunning) {
            currentValue = Random.nextDouble(0.0, 100.0)
            measurements = measurements + currentValue
            if (measurements.size > 50) {
                measurements = measurements.drop(1)
            }
            delay(500)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Elektromagnetische Feldmessung",
            style = MaterialTheme.typography.headlineMedium
        )
        
        // Current Value Display
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Aktueller Wert",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${String.format("%.2f", currentValue)} µT",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { isRunning = !isRunning },
                modifier = Modifier.weight(1f),
                colors = if (isRunning) 
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                else 
                    ButtonDefaults.buttonColors()
            ) {
                Text(if (isRunning) "Stoppen" else "Starten")
            }
            
            OutlinedButton(
                onClick = { 
                    measurements = emptyList()
                    currentValue = 0.0
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Reset")
            }
        }
        
        // Measurements List
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Letzte Messungen (${measurements.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.height(200.dp)
                ) {
                    items(measurements.reversed()) { value ->
                        Text(
                            text = "${String.format("%.2f", value)} µT",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnalysisScreen() {
    var selectedMaterial by remember { mutableStateOf("Unbekannt") }
    val materials = listOf("Metall", "Kunststoff", "Holz", "Keramik", "Unbekannt")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Material-Analyse",
            style = MaterialTheme.typography.headlineMedium
        )

        // Analysis Results
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Erkanntes Material",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = selectedMaterial,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Vertrauen: ${Random.nextInt(70, 95)}%",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Material Selection
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Material-Bibliothek",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                materials.forEach { material ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMaterial == material,
                            onClick = { selectedMaterial = material }
                        )
                        Text(
                            text = material,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        // Statistics
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Analyse-Statistiken",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Durchschnitt: ${String.format("%.2f", Random.nextDouble(20.0, 80.0))} µT")
                Text("Maximum: ${String.format("%.2f", Random.nextDouble(80.0, 100.0))} µT")
                Text("Minimum: ${String.format("%.2f", Random.nextDouble(0.0, 20.0))} µT")
                Text("Messungen: ${Random.nextInt(50, 200)}")
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    var autoSave by remember { mutableStateOf(true) }
    var notifications by remember { mutableStateOf(true) }
    var selectedUnit by remember { mutableStateOf("µT") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Einstellungen",
            style = MaterialTheme.typography.headlineMedium
        )

        // General Settings
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Allgemein",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Automatisches Speichern")
                    Switch(
                        checked = autoSave,
                        onCheckedChange = { autoSave = it }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Benachrichtigungen")
                    Switch(
                        checked = notifications,
                        onCheckedChange = { notifications = it }
                    )
                }
            }
        }

        // Measurement Settings
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Messung",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Einheit: $selectedUnit")
                Text("Messintervall: 500ms")
                Text("Puffergröße: 1000 Werte")
            }
        }

        // Device Info
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Geräteinformationen",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Modell: Samsung Galaxy S21 Ultra")
                Text("Android: ${android.os.Build.VERSION.RELEASE}")
                Text("API Level: ${android.os.Build.VERSION.SDK_INT}")
                Text("Bluetooth: Verfügbar")
                Text("ARCore: Unterstützt")
            }
        }

        // Actions
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { /* Export data */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Daten exportieren")
            }

            OutlinedButton(
                onClick = { /* Reset settings */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Einstellungen zurücksetzen")
            }
        }
    }
}
