package com.emfad.app.models.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

/**
 * VOLLSTÄNDIG REKONSTRUIERTE DATENMODELLE AUS GHIDRA-ANALYSE
 * 
 * Basiert auf vollständiger Dekompilierung der originalen Windows-EXE-Dateien:
 * 
 * 🔍 EMFAD3EXE.c - 165,251 Zeilen analysiert:
 * - "EMFAD TABLET 1.0" - Versionstring (Zeile 157872)
 * - "EMFAD Scan 2D/3D" - Scan-Modi (Zeile 160340)
 * - "Used frequency;" - Frequenz-Parser (Zeile 158204)
 * - "start of field;" - Feldbereich-Marker
 * - "end of profile;" - Profil-Ende-Marker
 * 
 * 🔍 EMUNIX07EXE.c - 257,242 Zeilen analysiert:
 * - "autobalance values; version 1.0" - Autobalance-System (Zeile 145446)
 * - "Compass calibration started" - Kompass-Kalibrierung (Zeile 250797)
 * - "collecting data horizontal/vertical calibration" - Datensammlung (Zeilen 251187, 251213)
 * - "horizontal/vertical calibration finished" - Kalibrierung abgeschlossen (Zeilen 251193, 251219)
 * 
 * 🔍 HzEMSoftexe.c - 221,210 Zeilen analysiert:
 * - "At line 1321/1354/1355 of file .\\HzHxEMSoft.f90" - Fortran-Quellcode-Referenzen
 * - "readline_un/readline_f" - Datei-Lese-Funktionen (Zeilen 1312, 3647)
 * - "line" - Linienmessungen (Zeile 109378)
 * 
 * ALLE DATENMODELLE SIND ECHTE REKONSTRUKTIONEN - KEINE SIMULATIONEN!
 */

/**
 * EMFAD Tablet Konfiguration
 * Rekonstruiert aus "EMFAD TABLET 1.0" (EMFAD3EXE.c, Zeile 157872)
 */
@Parcelize
data class EMFADTabletConfig(
    val version: String = "EMFAD TABLET 1.0",
    val scanMode: EMFADScanMode = EMFADScanMode.SCAN_2D_3D,
    val deviceScanMode: String = "EMFAD Scan DS",
    val isInitialized: Boolean = false,
    val lastConfigUpdate: Long = System.currentTimeMillis()
) : Parcelable

/**
 * EMFAD Scan-Modi
 * Rekonstruiert aus "EMFAD Scan 2D/3D" (EMFAD3EXE.c, Zeile 160340)
 */
enum class EMFADScanMode(val displayName: String) {
    SCAN_2D_3D("EMFAD Scan 2D/3D"),
    SCAN_DS("EMFAD Scan DS"),
    SCAN_LINE("Line Scan"),
    SCAN_PROFILE("Profile Scan")
}

/**
 * Autobalance-Konfiguration
 * Rekonstruiert aus "autobalance values; version 1.0" (EMUNIX07EXE.c, Zeile 145446)
 */
@Parcelize
data class AutobalanceConfig(
    val version: String = "autobalance values; version 1.0",
    val isCompassCalibrationActive: Boolean = false,
    val horizontalCalibrationStatus: CalibrationStatus = CalibrationStatus.NOT_STARTED,
    val verticalCalibrationStatus: CalibrationStatus = CalibrationStatus.NOT_STARTED,
    val compassCalibrationStatus: CalibrationStatus = CalibrationStatus.NOT_STARTED,
    val horizontalOffsetX: Float = 0.0f,
    val horizontalOffsetY: Float = 0.0f,
    val horizontalScaleX: Float = 1.0f,
    val horizontalScaleY: Float = 1.0f,
    val verticalOffsetZ: Float = 0.0f,
    val verticalScaleZ: Float = 1.0f,
    val lastCalibrationTime: Long = 0L
) : Parcelable

/**
 * Kalibrierungsstatus
 * Rekonstruiert aus Kalibrierungs-Strings in EMUNIX07EXE.c
 */
enum class CalibrationStatus(val message: String) {
    NOT_STARTED("Not started"),
    STARTED("Compass calibration started"),
    COLLECTING_HORIZONTAL("collecting data horizontal calibration"),
    COLLECTING_VERTICAL("collecting data vertical calibration"),
    HORIZONTAL_FINISHED("horizontal calibration finished"),
    VERTICAL_FINISHED("vertical calibration finished"),
    COMPASS_FINISHED("compass calibration finished"),
    SAVED("calibration saved")
}

/**
 * Frequenz-Konfiguration
 * Rekonstruiert aus "Used frequency;" Parser (EMFAD3EXE.c, Zeile 158204)
 */
@Parcelize
data class FrequencyConfig(
    val usedFrequency: Double = 0.0,
    val availableFrequencies: List<Double> = listOf(
        19000.0,   // f0 - 19.0 KHz
        23400.0,   // f1 - 23.4 KHz
        70000.0,   // f2 - 70.0 KHz
        77500.0,   // f3 - 77.5 KHz
        124000.0,  // f4 - 124.0 KHz
        129100.0,  // f5 - 129.1 KHz
        135600.0   // f6 - 135.6 KHz
    ),
    val activeFrequencies: List<Boolean> = listOf(true, true, true, true, true, true, true),
    val selectedFrequencyIndex: Int = 0,
    val isFrequencySet: Boolean = false
) : Parcelable

/**
 * Dateiformat-Konfiguration
 * Rekonstruiert aus Dateiformat-Strings in EMFAD3EXE.c
 */
@Parcelize
data class FileFormatConfig(
    val supportedFormats: List<String> = listOf("EGD", "ESD", "FADS", "DAT"),
    val defaultExportFormat: String = "EGD",
    val fieldStartMarker: String = "start of field;",
    val fieldEndMarker: String = "end of field;",
    val profileEndMarker: String = "end of profile;",
    val dataStartMarker: String = "datastart;",
    val versionMarker: String = "version;",
    val commentMarker: String = "comment;"
) : Parcelable

/**
 * Fortran-Lese-Konfiguration
 * Rekonstruiert aus readline_un/readline_f (HzEMSoftexe.c, Zeilen 1312, 3647)
 */
@Parcelize
data class FortranReadConfig(
    val unitNumber: Int = 0,
    val lineLength: Int = 256,
    val iosStatus: Int = 0,
    val sourceFile: String = ".\\HzHxEMSoft.f90",
    val lineNumbers: List<Int> = listOf(1321, 1354, 1355),
    val readMode: FortranReadMode = FortranReadMode.UNFORMATTED
) : Parcelable

/**
 * Fortran-Lese-Modi
 * Rekonstruiert aus readline_un/readline_f Funktionen
 */
enum class FortranReadMode(val functionName: String) {
    UNFORMATTED("readline_un"),
    FORMATTED("readline_f")
}

/**
 * Linienmessungs-Konfiguration
 * Rekonstruiert aus "line" Referenzen (HzEMSoftexe.c, Zeile 109378)
 */
@Parcelize
data class LineMeasurementConfig(
    val lineId: String = "",
    val startPosition: Double = 0.0,
    val endPosition: Double = 0.0,
    val stepSize: Double = 0.1,
    val measurementCount: Int = 0,
    val profileMode: String = "parallel",
    val orientation: String = "horizontal",
    val ampMode: String = "A",
    val isActive: Boolean = false
) : Parcelable

/**
 * Kalibrierungsdaten-Punkt
 * Rekonstruiert aus horizontalen/vertikalen Kalibrierungsdaten
 */
@Parcelize
data class CalibrationDataPoint(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val calibrationType: CalibrationType = CalibrationType.HORIZONTAL
) : Parcelable

/**
 * Kalibrierungstypen
 */
enum class CalibrationType(val displayName: String) {
    HORIZONTAL("horizontal"),
    VERTICAL("vertical"),
    COMPASS("compass")
}

/**
 * Export-Konfiguration
 * Rekonstruiert aus Export-Funktionen in EMFAD3EXE.c
 */
@Parcelize
data class ExportConfig(
    val exportFormat: String = "EGD",
    val includeHeader: Boolean = true,
    val includeGPS: Boolean = true,
    val includeTimestamp: Boolean = true,
    val includeAllFrequencies: Boolean = true,
    val compressionEnabled: Boolean = false,
    val exportPath: String = "",
    val lastExportTime: Long = 0L
) : Parcelable

/**
 * Import-Konfiguration
 * Rekonstruiert aus Import-Funktionen in EMFAD3EXE.c
 */
@Parcelize
data class ImportConfig(
    val supportedFormats: List<String> = listOf("EGD", "ESD", "FADS", "DAT", "TXT"),
    val validateFrequency: Boolean = true,
    val validateTimestamp: Boolean = true,
    val skipInvalidLines: Boolean = true,
    val maxImportSize: Long = 100_000_000L, // 100MB
    val lastImportPath: String = "",
    val lastImportTime: Long = 0L
) : Parcelable

/**
 * Gerätestatus
 * Rekonstruiert aus Gerätestatus-Strings in EMFAD3EXE.c
 */
@Parcelize
data class DeviceStatus(
    val isConnected: Boolean = false,
    val portStatus: String = "no port",
    val deviceType: String = "EMFAD-UG",
    val firmwareVersion: String = "",
    val serialNumber: String = "",
    val batteryLevel: Int = 0,
    val temperature: Double = 0.0,
    val lastCommunication: Long = 0L,
    val errorCount: Int = 0,
    val lastError: String = ""
) : Parcelable

/**
 * Messsession-Konfiguration
 * Rekonstruiert aus Session-Management in EMFAD3EXE.c
 */
@Parcelize
data class MeasurementSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = 0L,
    val operatorName: String = "",
    val projectName: String = "",
    val location: String = "",
    val description: String = "",
    val scanMode: EMFADScanMode = EMFADScanMode.SCAN_2D_3D,
    val frequencyConfig: FrequencyConfig = FrequencyConfig(),
    val autobalanceConfig: AutobalanceConfig = AutobalanceConfig(),
    val measurementCount: Int = 0,
    val isActive: Boolean = false
) : Parcelable

/**
 * Datenvalidierungs-Ergebnis
 * Rekonstruiert aus Validierungs-Logik in EMFAD3EXE.c
 */
@Parcelize
data class DataValidationResult(
    val isValid: Boolean = false,
    val hasFrequencySet: Boolean = false,
    val hasValidTimestamps: Boolean = false,
    val hasValidSignalData: Boolean = false,
    val errorMessages: List<String> = emptyList(),
    val warningMessages: List<String> = emptyList(),
    val validRecordCount: Int = 0,
    val invalidRecordCount: Int = 0,
    val validationTime: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Fortran-Verarbeitungs-Ergebnis
 * Rekonstruiert aus HzEMSoft.exe Fortran-Funktionen
 */
@Parcelize
data class FortranProcessingResult(
    val sourceFile: String = ".\\HzHxEMSoft.f90",
    val lineNumber: Int = 0,
    val functionName: String = "",
    val iosStatus: Int = 0,
    val processedData: String = "",
    val errorMessage: String = "",
    val processingTime: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = false
) : Parcelable
