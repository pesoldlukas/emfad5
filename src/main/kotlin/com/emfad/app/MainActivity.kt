package com.emfad.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * EMFAD® MainActivity
 * Hauptaktivität der EMFAD Android-App
 * Implementiert Jetpack Compose mit Navigation
 * Optimiert für Samsung S21 Ultra
 */

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Timber Logging initialisieren
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Edge-to-Edge aktivieren für Samsung S21 Ultra
        enableEdgeToEdge()

        setContent {
            // Verwende die ECHTE EMFAD Navigation mit echten Backend-Services
            com.emfad.app.navigation.EMFADNavigation()
        }

        Timber.d("EMFAD MainActivity created on Samsung S21 Ultra")
    }

    override fun onResume() {
        super.onResume()
        Timber.d("EMFAD MainActivity resumed")
    }

    override fun onPause() {
        super.onPause()
        Timber.d("EMFAD MainActivity paused")
    }
}

// Alte Navigation entfernt - verwende jetzt echte EMFADNavigation

@Composable
fun EMFADOriginalLayout(navController: androidx.navigation.NavController) {
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
        EMFADHeader()

        Spacer(modifier = Modifier.height(24.dp))

        // Main Function Grid - 4x3 layout like original
        EMFADFunctionGrid(navController)

        Spacer(modifier = Modifier.weight(1f))

        // Bottom Control Bar - Like original
        EMFADBottomControlBar()
    }
}

@Composable
fun EMFADHeader() {
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
            // EMFAD Logo - Black text with red accent
            Text(
                text = "EMFAD",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                letterSpacing = 4.sp
            )

            // Red accent lines and dot (simplified)
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
fun EMFADDashboardScreen(navController: androidx.navigation.NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🔧 EMFAD®",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Elektromagnetische Feldanalyse",
            fontSize = 20.sp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "✅ Samsung Galaxy S21 Ultra optimiert",
            fontSize = 16.sp,
            color = Color(0xFF4CAF50)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Gerätestatus
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Gerätestatus",
                    fontSize = 18.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFFF9800)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Kein Gerät verbunden",
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { navController.navigate("measurement") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    )
                ) {
                    Text("Verbinden", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Schnellaktionen
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { navController.navigate("measurement") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Science, contentDescription = "Messung")
                    Text("Messung", fontSize = 12.sp)
                }
            }

            Button(
                onClick = { navController.navigate("analysis") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFEB3B),
                    contentColor = Color.Black
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Analytics, contentDescription = "Analyse", tint = Color.Black)
                    Text("Analyse", fontSize = 12.sp, color = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // System-Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "System-Information",
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Version: 1.0.0", color = Color(0xB3FFFFFF), fontSize = 14.sp)
                Text("Build: Samsung S21 Ultra", color = Color(0xB3FFFFFF), fontSize = 14.sp)
                Text("Kotlin: 1.9.20", color = Color(0xB3FFFFFF), fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun EMFADMeasurementScreen(navController: androidx.navigation.NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "EMFAD Messung",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Messwert-Anzeige
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Aktueller Messwert",
                    fontSize = 16.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "0.00 µT",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Frequenz: 50 Hz",
                    fontSize = 14.sp,
                    color = Color(0xB3FFFFFF)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Steuerung
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { /* Start Messung */ },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start")
            }

            Button(
                onClick = { /* Stop Messung */ },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = "Stop")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Kalibrierung
        Button(
            onClick = { /* Kalibrierung */ },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFEB3B),
                contentColor = Color.Black
            )
        ) {
            Icon(Icons.Default.Tune, contentDescription = "Kalibrierung", tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Kalibrierung", color = Color.Black)
        }
    }
}

@Composable
fun EMFADAnalysisScreen(navController: androidx.navigation.NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "EMFAD Analyse",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Spektrum-Anzeige (Platzhalter)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Spektrum-Analyzer\n(Implementierung folgt)",
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Analyse-Optionen
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { /* FFT Analyse */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Text("FFT Analyse")
            }

            Button(
                onClick = { /* Harmonische Analyse */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9C27B0)
                )
            ) {
                Text("Harmonische Analyse")
            }

            Button(
                onClick = { /* Profil erstellen */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800)
                )
            ) {
                Text("Profil erstellen")
            }
        }
    }
}

@Composable
fun EMFADARScreen(navController: androidx.navigation.NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ViewInAr,
            contentDescription = "AR",
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF2196F3)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "AR-Visualisierung",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "ARCore-Integration\n(Implementierung folgt)",
            color = Color.White,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { /* AR starten */ },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3)
            )
        ) {
            Text("AR-Modus starten")
        }
    }
}

@Composable
fun EMFADExportScreen(navController: androidx.navigation.NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "EMFAD Export",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Export-Optionen
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Datenexport",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { /* CSV Export */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "CSV")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CSV Export")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { /* PDF Export */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336)
                        )
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PDF Report")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { /* EGD Export */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFEB3B),
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "EGD", tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EGD Format", color = Color.Black)
                    }
                }
            }
        }
    }
}
