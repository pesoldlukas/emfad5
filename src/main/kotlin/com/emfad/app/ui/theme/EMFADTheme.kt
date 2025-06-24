package com.emfad.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * EMFAD Material 3 Theme
 * Samsung S21 Ultra optimiert mit Dynamic Colors
 */

// EMFAD Brand Colors
private val EMFADPrimary = Color(0xFF1976D2)      // Blau
private val EMFADSecondary = Color(0xFF03DAC6)    // Türkis
private val EMFADTertiary = Color(0xFFFF6F00)     // Orange
private val EMFADError = Color(0xFFB00020)        // Rot
private val EMFADSurface = Color(0xFFF5F5F5)      // Hellgrau
private val EMFADBackground = Color(0xFFFFFFFF)   // Weiß

// Dark Theme Colors
private val EMFADPrimaryDark = Color(0xFF90CAF9)
private val EMFADSecondaryDark = Color(0xFF03DAC6)
private val EMFADTertiaryDark = Color(0xFFFFB74D)
private val EMFADSurfaceDark = Color(0xFF121212)
private val EMFADBackgroundDark = Color(0xFF000000)

private val LightColorScheme = lightColorScheme(
    primary = EMFADPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF0D47A1),
    
    secondary = EMFADSecondary,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFE0F2F1),
    onSecondaryContainer = Color(0xFF004D40),
    
    tertiary = EMFADTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0B2),
    onTertiaryContainer = Color(0xFFE65100),
    
    error = EMFADError,
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFF7F0000),
    
    background = EMFADBackground,
    onBackground = Color(0xFF1C1B1F),
    surface = EMFADSurface,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    scrim = Color(0xFF000000),
    
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFFD0BCFF)
)

private val DarkColorScheme = darkColorScheme(
    primary = EMFADPrimaryDark,
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004881),
    onPrimaryContainer = Color(0xFFD1E4FF),
    
    secondary = EMFADSecondaryDark,
    onSecondary = Color(0xFF003A32),
    secondaryContainer = Color(0xFF005048),
    onSecondaryContainer = Color(0xFF7FF6E8),
    
    tertiary = EMFADTertiaryDark,
    onTertiary = Color(0xFF8A2E00),
    tertiaryContainer = Color(0xFFC44200),
    onTertiaryContainer = Color(0xFFFFDCC6),
    
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    background = EMFADBackgroundDark,
    onBackground = Color(0xFFE6E1E5),
    surface = EMFADSurfaceDark,
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    scrim = Color(0xFF000000),
    
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = Color(0xFF1976D2)
)

@Composable
fun EMFADTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EMFADTypography,
        shapes = EMFADShapes,
        content = content
    )
}

// Custom Colors für EMFAD-spezifische UI-Elemente
object EMFADColors {
    val SignalStrong = Color(0xFF4CAF50)      // Grün für starke Signale
    val SignalMedium = Color(0xFFFF9800)      // Orange für mittlere Signale
    val SignalWeak = Color(0xFFF44336)        // Rot für schwache Signale
    val MaterialIron = Color(0xFF795548)      // Braun für Eisen
    val MaterialAluminum = Color(0xFF9E9E9E)  // Grau für Aluminium
    val MaterialCopper = Color(0xFFFF5722)    // Orange-Rot für Kupfer
    val MaterialGold = Color(0xFFFFD700)      // Gold
    val MaterialSilver = Color(0xFFC0C0C0)    // Silber
    val AROverlay = Color(0x80FFFFFF)         // Transparentes Weiß für AR
    val ChartGrid = Color(0x40000000)         // Transparentes Schwarz für Diagramm-Gitter
}

// Erweiterte Farb-Utilities
@Composable
fun getSignalStrengthColor(strength: Double): Color {
    return when {
        strength >= 700.0 -> EMFADColors.SignalStrong
        strength >= 400.0 -> EMFADColors.SignalMedium
        else -> EMFADColors.SignalWeak
    }
}

@Composable
fun getMaterialTypeColor(materialType: String): Color {
    return when (materialType.lowercase()) {
        "iron", "eisen" -> EMFADColors.MaterialIron
        "aluminum", "aluminium" -> EMFADColors.MaterialAluminum
        "copper", "kupfer" -> EMFADColors.MaterialCopper
        "gold" -> EMFADColors.MaterialGold
        "silver", "silber" -> EMFADColors.MaterialSilver
        else -> MaterialTheme.colorScheme.onSurface
    }
}

// Theme-Utilities
@Composable
fun isLightTheme(): Boolean = !isSystemInDarkTheme()

@Composable
fun getContentColor(backgroundColor: Color): Color {
    return if (backgroundColor.luminance() > 0.5) {
        Color.Black
    } else {
        Color.White
    }
}

// Luminance-Berechnung für bessere Kontraste
private fun Color.luminance(): Float {
    val r = if (red <= 0.03928f) red / 12.92f else kotlin.math.pow((red + 0.055f) / 1.055f, 2.4f).toFloat()
    val g = if (green <= 0.03928f) green / 12.92f else kotlin.math.pow((green + 0.055f) / 1.055f, 2.4f).toFloat()
    val b = if (blue <= 0.03928f) blue / 12.92f else kotlin.math.pow((blue + 0.055f) / 1.055f, 2.4f).toFloat()
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}
