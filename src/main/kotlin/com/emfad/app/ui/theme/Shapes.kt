package com.emfad.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * EMFAD Shapes
 * Material 3 Shapes für Samsung S21 Ultra optimiert
 */

val EMFADShapes = Shapes(
    // Extra Small - für kleine Chips und Tags
    extraSmall = RoundedCornerShape(4.dp),
    
    // Small - für kleine Buttons und Cards
    small = RoundedCornerShape(8.dp),
    
    // Medium - für Standard-Cards und Dialoge
    medium = RoundedCornerShape(12.dp),
    
    // Large - für große Cards und Bottom Sheets
    large = RoundedCornerShape(16.dp),
    
    // Extra Large - für Modals und große Container
    extraLarge = RoundedCornerShape(28.dp)
)

// EMFAD-spezifische Shapes
object EMFADCustomShapes {
    // Für Messwert-Cards
    val measurementCard = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = 8.dp,
        bottomEnd = 8.dp
    )
    
    // Für Status-Indikatoren
    val statusIndicator = RoundedCornerShape(20.dp)
    
    // Für Bluetooth-Geräte-Cards
    val deviceCard = RoundedCornerShape(12.dp)
    
    // Für AR-Overlay-Elemente
    val arOverlay = RoundedCornerShape(8.dp)
    
    // Für Diagramm-Container
    val chartContainer = RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 12.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    
    // Für Export-Buttons
    val exportButton = RoundedCornerShape(24.dp)
    
    // Für Session-Cards
    val sessionCard = RoundedCornerShape(16.dp)
    
    // Für Material-Type-Chips
    val materialChip = RoundedCornerShape(16.dp)
    
    // Für Konfidenz-Balken
    val confidenceBar = RoundedCornerShape(4.dp)
    
    // Für Navigation-Drawer
    val navigationDrawer = RoundedCornerShape(
        topEnd = 16.dp,
        bottomEnd = 16.dp
    )
    
    // Für Bottom-Navigation
    val bottomNavigation = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp
    )
    
    // Für FAB (Floating Action Button)
    val fab = RoundedCornerShape(16.dp)
    
    // Für Extended FAB
    val extendedFab = RoundedCornerShape(16.dp)
    
    // Für Snackbar
    val snackbar = RoundedCornerShape(8.dp)
    
    // Für Alert-Dialoge
    val alertDialog = RoundedCornerShape(20.dp)
    
    // Für Bottom-Sheets
    val bottomSheet = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp
    )
    
    // Für Top-App-Bar
    val topAppBar = RoundedCornerShape(
        bottomStart = 12.dp,
        bottomEnd = 12.dp
    )
    
    // Für Search-Bar
    val searchBar = RoundedCornerShape(28.dp)
    
    // Für Progress-Indikatoren
    val progressIndicator = RoundedCornerShape(8.dp)
    
    // Für Slider-Tracks
    val sliderTrack = RoundedCornerShape(4.dp)
    
    // Für Switch-Komponenten
    val switchThumb = RoundedCornerShape(10.dp)
    val switchTrack = RoundedCornerShape(14.dp)
    
    // Für Checkbox
    val checkbox = RoundedCornerShape(2.dp)
    
    // Für Radio-Buttons
    val radioButton = RoundedCornerShape(50) // Vollständig rund
    
    // Für Text-Fields
    val textField = RoundedCornerShape(
        topStart = 4.dp,
        topEnd = 4.dp
    )
    
    val outlinedTextField = RoundedCornerShape(8.dp)
    
    // Für Dropdown-Menüs
    val dropdownMenu = RoundedCornerShape(8.dp)
    
    // Für Tooltips
    val tooltip = RoundedCornerShape(4.dp)
    
    // Für Badge-Indikatoren
    val badge = RoundedCornerShape(8.dp)
    
    // Für Divider mit abgerundeten Enden
    val divider = RoundedCornerShape(1.dp)
    
    // Für Image-Container
    val imageContainer = RoundedCornerShape(12.dp)
    
    // Für Video-Player
    val videoPlayer = RoundedCornerShape(8.dp)
    
    // Für Code-Blöcke (für Export-Vorschau)
    val codeBlock = RoundedCornerShape(6.dp)
    
    // Für Kalender-Komponenten
    val calendarDay = RoundedCornerShape(8.dp)
    
    // Für Time-Picker
    val timePicker = RoundedCornerShape(12.dp)
    
    // Für Stepper-Komponenten
    val stepperButton = RoundedCornerShape(4.dp)
    
    // Für Segmented-Buttons
    val segmentedButtonStart = RoundedCornerShape(
        topStart = 20.dp,
        bottomStart = 20.dp
    )
    val segmentedButtonMiddle = RoundedCornerShape(0.dp)
    val segmentedButtonEnd = RoundedCornerShape(
        topEnd = 20.dp,
        bottomEnd = 20.dp
    )
    
    // Für Tab-Indikatoren
    val tabIndicator = RoundedCornerShape(
        topStart = 3.dp,
        topEnd = 3.dp
    )
    
    // Für Carousel-Items
    val carouselItem = RoundedCornerShape(16.dp)
    
    // Für Banner-Nachrichten
    val banner = RoundedCornerShape(0.dp)
    
    // Für Expansion-Panels
    val expansionPanel = RoundedCornerShape(8.dp)
    
    // Für List-Items
    val listItem = RoundedCornerShape(0.dp)
    val listItemWithDivider = RoundedCornerShape(4.dp)
    
    // Für Grid-Items
    val gridItem = RoundedCornerShape(8.dp)
    
    // Für Pager-Indikatoren
    val pagerIndicator = RoundedCornerShape(2.dp)
    val pagerIndicatorActive = RoundedCornerShape(8.dp)
}
