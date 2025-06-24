# 📚 EMFAD APP API DOKUMENTATION

## 🎯 API-ÜBERSICHT

Die EMFAD Android App bietet eine umfassende API für elektromagnetische Feldanalyse und Detektion. Alle APIs basieren auf der **vollständigen Ghidra-Rekonstruktion** der originalen Windows-EMFAD-Software.

---

## 🔍 GHIDRA-REKONSTRUIERTE APIS

### 📊 **GhidraDeviceController API**

Hauptklasse für EMFAD-Geräte-Kommunikation, rekonstruiert aus EMFAD3EXE.c.

```kotlin
class GhidraDeviceController(private val context: Context) {
    
    /**
     * FormCreate - Initialisierung aus EMFAD3EXE.c
     * Rekonstruiert aus Zeile 157872: "EMFAD TABLET 1.0"
     */
    fun formCreate()
    
    /**
     * Verbindung zu EMFAD-Gerät herstellen
     * Unterstützt USB-Serial (FTDI, Prolific, Silicon Labs) und Bluetooth BLE
     * 
     * @return true wenn Verbindung erfolgreich
     */
    suspend fun connectToDevice(): Boolean
    
    /**
     * EMFAD-Messung starten
     * Implementiert echtes EMFAD-Protokoll mit Sync-Byte 0xAA
     * 
     * @param frequencyConfig Frequenz-Konfiguration (7 EMFAD-Frequenzen)
     * @return true wenn Messung erfolgreich gestartet
     */
    suspend fun startMeasurement(frequencyConfig: FrequencyConfig): Boolean
    
    /**
     * EMFAD-Messung stoppen
     * 
     * @return true wenn Messung erfolgreich gestoppt
     */
    suspend fun stopMeasurement(): Boolean
    
    /**
     * Autobalance-Kalibrierung starten
     * Rekonstruiert aus "autobalance values; version 1.0" (EMUNIX07EXE.c)
     * 
     * @return true wenn Kalibrierung erfolgreich gestartet
     */
    suspend fun startAutobalanceCalibration(): Boolean
    
    /**
     * Gerätestatus abrufen
     * 
     * @return StateFlow<DeviceStatus> für kontinuierliche Überwachung
     */
    val deviceStatus: StateFlow<DeviceStatus>
    
    /**
     * Messdaten abrufen
     * 
     * @return StateFlow<EMFReading?> für Echtzeit-Messdaten
     */
    val measurementData: StateFlow<EMFReading?>
    
    /**
     * Autobalance-Konfiguration abrufen
     * 
     * @return StateFlow<AutobalanceConfig> für Kalibrierungs-Status
     */
    val autobalanceConfig: StateFlow<AutobalanceConfig>
}
```

### 📁 **GhidraExportImportFunctions API**

Export/Import-Funktionen rekonstruiert aus EMFAD3EXE.c.

```kotlin
object GhidraExportImportFunctions {
    
    /**
     * ExportDAT1Click - DAT-Export-Funktion
     * Rekonstruiert aus "ExportDAT1Click" String in EMFAD3EXE.c
     * 
     * @param context Android Context
     * @param readings Liste der EMF-Messungen
     * @param fileName Dateiname für Export
     * @param exportConfig Export-Konfiguration
     * @return Result<String> mit Dateipfad bei Erfolg
     */
    fun exportDAT1Click(
        context: Context,
        readings: List<EMFReading>,
        fileName: String = "emfad_export.dat",
        exportConfig: ExportConfig = ExportConfig()
    ): Result<String>
    
    /**
     * Export2D1Click - 2D-Export-Funktion
     * Rekonstruiert aus "Export2D1Click" String in EMFAD3EXE.c
     * 
     * @param context Android Context
     * @param readings Liste der EMF-Messungen
     * @param analysis Material-Analyse (optional)
     * @param fileName Dateiname für Export
     * @param exportConfig Export-Konfiguration
     * @return Result<String> mit Dateipfad bei Erfolg
     */
    fun export2D1Click(
        context: Context,
        readings: List<EMFReading>,
        analysis: MaterialAnalysis?,
        fileName: String = "emfad_2d_export.txt",
        exportConfig: ExportConfig = ExportConfig()
    ): Result<String>
    
    /**
     * importTabletFile1Click - Tablet-Import-Funktion
     * Rekonstruiert aus "importTabletFile1Click" String in EMFAD3EXE.c
     * 
     * @param context Android Context
     * @param filePath Pfad zur Import-Datei
     * @param importConfig Import-Konfiguration
     * @return Result<Pair<List<EMFReading>, DataValidationResult>>
     */
    fun importTabletFile1Click(
        context: Context,
        filePath: String,
        importConfig: ImportConfig = ImportConfig()
    ): Result<Pair<List<EMFReading>, DataValidationResult>>
    
    /**
     * EGD-Format Export
     * EMFAD Grid Data Format
     * 
     * @param context Android Context
     * @param readings Liste der EMF-Messungen
     * @param fileName Dateiname
     * @param fileFormatConfig Dateiformat-Konfiguration
     * @return Result<String> mit Dateipfad bei Erfolg
     */
    fun exportEGDFormat(
        context: Context,
        readings: List<EMFReading>,
        fileName: String = "export.egd",
        fileFormatConfig: FileFormatConfig = FileFormatConfig()
    ): Result<String>
    
    /**
     * ESD-Format Export
     * EMFAD Survey Data Format
     * 
     * @param context Android Context
     * @param readings Liste der EMF-Messungen
     * @param fileName Dateiname
     * @param fileFormatConfig Dateiformat-Konfiguration
     * @return Result<String> mit Dateipfad bei Erfolg
     */
    fun exportESDFormat(
        context: Context,
        readings: List<EMFReading>,
        fileName: String = "export.esd",
        fileFormatConfig: FileFormatConfig = FileFormatConfig()
    ): Result<String>
    
    /**
     * Importierte Daten validieren
     * Implementiert echte EMFAD-Validierung
     * 
     * @param readings Liste der EMF-Messungen
     * @param importConfig Import-Konfiguration
     * @return DataValidationResult mit Validierungs-Ergebnissen
     */
    fun validateImportedData(
        readings: List<EMFReading>, 
        importConfig: ImportConfig
    ): DataValidationResult
}
```

### 🧮 **GhidraFortranProcessor API**

Fortran-Verarbeitung rekonstruiert aus HzEMSoftexe.c.

```kotlin
object GhidraFortranProcessor {
    
    /**
     * readline_un - Unformatierte Zeilen-Lese-Funktion
     * Rekonstruiert aus "readline_un" (HzEMSoftexe.c, Zeile 1312)
     * 
     * @param nunitr Unit-Nummer
     * @param line Eingabe-Zeile
     * @param ios I/O-Status
     * @param lineLength Zeilen-Länge (max 256)
     * @return FortranProcessingResult mit Verarbeitungs-Ergebnis
     */
    fun readlineUn(
        nunitr: Int,
        line: String,
        ios: Int,
        lineLength: Int = 256
    ): FortranProcessingResult
    
    /**
     * readline_f - Formatierte Zeilen-Lese-Funktion
     * Rekonstruiert aus "readline_f" (HzEMSoftexe.c, Zeile 3647)
     * 
     * @param nunitr Unit-Nummer
     * @param line Eingabe-Zeile
     * @param ios I/O-Status
     * @param lineLength Zeilen-Länge (max 256)
     * @return FortranProcessingResult mit Verarbeitungs-Ergebnis
     */
    fun readlineF(
        nunitr: Int,
        line: String,
        ios: Int,
        lineLength: Int = 256
    ): FortranProcessingResult
    
    /**
     * EMF-Daten mit Fortran-Algorithmen verarbeiten
     * Implementiert mathematische Präzision aus HzEMSoftexe.c
     * 
     * @param readings Liste der EMF-Messungen
     * @param frequencyConfig Frequenz-Konfiguration
     * @return Result<List<EMFReading>> mit verarbeiteten Messungen
     */
    suspend fun processEMFData(
        readings: List<EMFReading>,
        frequencyConfig: FrequencyConfig
    ): Result<List<EMFReading>>
    
    /**
     * Array-Bounds-Checking aus Fortran
     * Rekonstruiert aus Array-Bounds-Fehlern in HzEMSoftexe.c
     * 
     * @param arrayName Array-Name
     * @param index Array-Index
     * @param lowerBound Untere Grenze
     * @param upperBound Obere Grenze
     * @param lineNumber Zeilen-Nummer (für Debugging)
     * @return FortranProcessingResult mit Prüf-Ergebnis
     */
    fun checkArrayBounds(
        arrayName: String,
        index: Int,
        lowerBound: Int,
        upperBound: Int,
        lineNumber: Int = 0
    ): FortranProcessingResult
    
    /**
     * Komplexe EMF-Daten verarbeiten
     * Verarbeitet Real/Imaginär-Teile mit Fortran-Mathematik
     * 
     * @param realArray Array der Real-Teile
     * @param imaginaryArray Array der Imaginär-Teile
     * @param frequencyArray Array der Frequenzen
     * @return Result<List<EMFReading>> mit verarbeiteten Messungen
     */
    fun processComplexEMFData(
        realArray: DoubleArray,
        imaginaryArray: DoubleArray,
        frequencyArray: DoubleArray
    ): Result<List<EMFReading>>
    
    /**
     * Linienmessungen verarbeiten
     * Rekonstruiert aus "line" Referenzen (HzEMSoftexe.c, Zeile 109378)
     * 
     * @param readings Liste der EMF-Messungen
     * @param lineConfig Linienmessungs-Konfiguration
     * @return Result<List<EMFReading>> mit Positions-Koordinaten
     */
    fun processLineMeasurement(
        readings: List<EMFReading>,
        lineConfig: LineMeasurementConfig
    ): Result<List<EMFReading>>
}
```

---

## 📊 DATENMODELL-APIS

### 🔧 **EMFADTabletConfig**

EMFAD Tablet Konfiguration aus "EMFAD TABLET 1.0".

```kotlin
@Parcelize
data class EMFADTabletConfig(
    val version: String = "EMFAD TABLET 1.0",
    val scanMode: EMFADScanMode = EMFADScanMode.SCAN_2D_3D,
    val deviceScanMode: String = "EMFAD Scan DS",
    val isInitialized: Boolean = false,
    val lastConfigUpdate: Long = System.currentTimeMillis()
) : Parcelable

enum class EMFADScanMode(val displayName: String) {
    SCAN_2D_3D("EMFAD Scan 2D/3D"),
    SCAN_DS("EMFAD Scan DS"),
    SCAN_LINE("Line Scan"),
    SCAN_PROFILE("Profile Scan")
}
```

### ⚖️ **AutobalanceConfig**

Autobalance-Konfiguration aus "autobalance values; version 1.0".

```kotlin
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
```

### 📡 **FrequencyConfig**

Frequenz-Konfiguration mit allen 7 EMFAD-Frequenzen.

```kotlin
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
```

### 📱 **DeviceStatus**

Gerätestatus für EMFAD-Hardware.

```kotlin
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
```

---

## 🎨 UI-KOMPONENTEN-APIS

### 🔧 **TfrmFrequencyModeSelect**

Frequenzauswahl-Dialog rekonstruiert aus EMFAD3EXE.c.

```kotlin
@Composable
fun TfrmFrequencyModeSelect(
    frequencyConfig: FrequencyConfig,
    onFrequencySelected: (Int) -> Unit,
    onDismiss: () -> Unit
)
```

### ⚖️ **TfrmAutoBalance**

Autobalance-Formular rekonstruiert aus EMUNIX07EXE.c.

```kotlin
@Composable
fun TfrmAutoBalance(
    autobalanceConfig: AutobalanceConfig,
    onStartCompassCalibration: () -> Unit,
    onStartHorizontalCalibration: () -> Unit,
    onStartVerticalCalibration: () -> Unit,
    onSaveCalibration: () -> Unit,
    onDeleteAllCalibration: () -> Unit
)
```

### 📤 **ExportDialog**

Export-Dialog für alle EMFAD-Dateiformate.

```kotlin
@Composable
fun ExportDialog(
    exportConfig: ExportConfig,
    onExportDAT: (String) -> Unit,
    onExport2D: (String) -> Unit,
    onDismiss: () -> Unit
)
```

### 📥 **ImportDialog**

Import-Dialog für Tablet-Dateien.

```kotlin
@Composable
fun ImportDialog(
    importConfig: ImportConfig,
    onImportTabletFile: (String) -> Unit,
    onDismiss: () -> Unit
)
```

---

## ⚙️ SERVICE-APIS

### 📊 **MeasurementService**

Kern-Service mit vollständiger Ghidra-Integration.

```kotlin
class MeasurementService : Service() {
    
    /**
     * Messung starten
     * Integriert alle Ghidra-Komponenten
     * 
     * @return Result<Unit> bei Erfolg
     */
    suspend fun startMeasurement(): Result<Unit>
    
    /**
     * Messung stoppen
     * 
     * @return Result<Unit> bei Erfolg
     */
    suspend fun stopMeasurement(): Result<Unit>
    
    /**
     * Autobalance-Kalibrierung starten
     * 
     * @return Result<Unit> bei Erfolg
     */
    suspend fun startAutobalanceCalibration(): Result<Unit>
    
    /**
     * Daten im DAT-Format exportieren
     * 
     * @param fileName Dateiname
     * @return Result<String> mit Dateipfad bei Erfolg
     */
    suspend fun exportDAT(fileName: String): Result<String>
    
    /**
     * Daten im 2D-Format exportieren
     * 
     * @param fileName Dateiname
     * @return Result<String> mit Dateipfad bei Erfolg
     */
    suspend fun export2D(fileName: String): Result<String>
    
    /**
     * Tablet-Datei importieren
     * 
     * @param filePath Dateipfad
     * @return Result<Pair<List<EMFReading>, DataValidationResult>>
     */
    suspend fun importTabletFile(filePath: String): Result<Pair<List<EMFReading>, DataValidationResult>>
    
    /**
     * Service-Status abrufen
     * Erweitert mit Ghidra-Status
     * 
     * @return MeasurementServiceStatus mit vollständigem Status
     */
    fun getServiceStatus(): MeasurementServiceStatus
    
    /**
     * Ghidra-Frequenzanalyse durchführen
     * 
     * @return Result<List<EMFReading>> mit analysierten Daten
     */
    suspend fun performGhidraFrequencyAnalysis(): Result<List<EMFReading>>
    
    /**
     * Mit Ghidra-Gerät verbinden
     * 
     * @return Result<Unit> bei Erfolg
     */
    suspend fun connectToGhidraDevice(): Result<Unit>
}
```

---

## 🔌 HARDWARE-APIS

### 📡 **BluetoothManager**

Nordic BLE Integration für EMFAD-Geräte.

```kotlin
class EMFADBluetoothManager(private val context: Context) {

    /**
     * BLE-Scan für EMFAD-Geräte starten
     *
     * @return Flow<EMFADDevice> mit gefundenen Geräten
     */
    fun scanForEMFADDevices(): Flow<EMFADDevice>

    /**
     * Mit EMFAD-Gerät verbinden
     *
     * @param device EMFAD-Gerät
     * @return EMFADConnection bei Erfolg, null bei Fehler
     */
    suspend fun connectToDevice(device: EMFADDevice): EMFADConnection?

    /**
     * Verbindungsstatus überwachen
     *
     * @return StateFlow<ConnectionState>
     */
    val connectionState: StateFlow<ConnectionState>

    /**
     * Messdaten-Stream
     *
     * @return SharedFlow<EMFReading>
     */
    val measurementData: SharedFlow<EMFReading>

    /**
     * EMFAD-Kommando senden
     *
     * @param command EMFAD-Kommando
     * @return EMFADResponse bei Erfolg, null bei Fehler
     */
    suspend fun sendCommand(command: EMFADCommand): EMFADResponse?
}
```

### 🔌 **USBSerialManager**

USB-Serial Kommunikation für EMFAD-Adapter.

```kotlin
class USBSerialManager(private val context: Context) {

    /**
     * USB-Serial Geräte scannen
     * Unterstützt FTDI, Prolific, Silicon Labs
     *
     * @return List<EMFADDevice> mit gefundenen Adaptern
     */
    fun scanForEMFADDevices(): List<EMFADDevice>

    /**
     * Mit USB-Serial Adapter verbinden
     *
     * @param device USB-Serial Adapter
     * @return EMFADConnection bei Erfolg, null bei Fehler
     */
    fun connectToDevice(device: EMFADDevice): EMFADConnection?
}
```

---

## 🤖 KI UND ANALYSE-APIS

### 🧠 **MaterialClassifier**

TensorFlow Lite Integration für Material-Erkennung.

```kotlin
class MaterialClassifier {

    /**
     * Material-Klassifikation basierend auf EMF-Signaturen
     *
     * @param emfReading EMF-Messung
     * @return MaterialType erkanntes Material
     */
    fun classifyMaterial(emfReading: EMFReading): MaterialType

    /**
     * Konfidenz-Bewertung berechnen
     *
     * @param signals Signal-Array
     * @return Double Konfidenz-Wert (0.0 - 1.0)
     */
    fun calculateConfidence(signals: List<Double>): Double

    /**
     * TensorFlow Lite Modell aktualisieren
     *
     * @param newModelPath Pfad zum neuen Modell
     */
    fun updateModel(newModelPath: String)

    /**
     * Modell initialisieren
     * Lädt TensorFlow Lite Modell aus Assets
     */
    suspend fun initialize()
}
```

---

## 🗄️ DATABASE-APIS

### 💾 **EMFADDatabase**

Room Database für EMFAD-Daten.

```kotlin
@Database(
    entities = [
        EMFReadingEntity::class,
        MeasurementSessionEntity::class,
        MaterialAnalysisEntity::class,
        CalibrationDataEntity::class
    ],
    version = 1
)
abstract class EMFADDatabase : RoomDatabase() {
    abstract fun emfReadingDao(): EMFReadingDao
    abstract fun measurementSessionDao(): MeasurementSessionDao
    abstract fun materialAnalysisDao(): MaterialAnalysisDao
    abstract fun calibrationDataDao(): CalibrationDataDao
}
```

---

## 🔧 UTILITY-APIS

### 📐 **EMFCalculations**

EMFAD-spezifische Berechnungen.

```kotlin
object EMFCalculations {

    /**
     * Tiefe aus EMF-Signal berechnen
     * Verwendet echte EMFAD-Kalibrierungskonstante 3333
     *
     * @param magnitude Signal-Magnitude
     * @param frequency Frequenz
     * @return Double berechnete Tiefe in Metern
     */
    fun calculateDepth(magnitude: Double, frequency: Double): Double {
        val calibrationConstant = when {
            frequency < 25000 -> 3333.0
            frequency < 80000 -> 2800.0
            else -> 2300.0
        }

        val attenuationFactor = 0.417
        val calibratedSignal = magnitude * (calibrationConstant / 1000.0)

        return if (calibratedSignal > 0) {
            -kotlin.math.ln(calibratedSignal / 1000.0) / attenuationFactor
        } else {
            0.0
        }
    }

    /**
     * Komplexe Zahlen aus Real/Imaginär-Teil berechnen
     *
     * @param realPart Real-Teil
     * @param imaginaryPart Imaginär-Teil
     * @return Pair<Double, Double> Magnitude und Phase
     */
    fun calculateComplexValues(realPart: Double, imaginaryPart: Double): Pair<Double, Double> {
        val magnitude = kotlin.math.sqrt(realPart * realPart + imaginaryPart * imaginaryPart)
        val phase = kotlin.math.atan2(imaginaryPart, realPart) * 180.0 / kotlin.math.PI
        return Pair(magnitude, phase)
    }
}
```

---

## 📚 VERWENDUNGSBEISPIELE

### 🚀 **Grundlegende Nutzung**

```kotlin
// 1. Service initialisieren
val measurementService = MeasurementService()

// 2. Ghidra-Gerät verbinden
val connectionResult = measurementService.connectToGhidraDevice()
if (connectionResult.isSuccess) {
    println("EMFAD-Gerät erfolgreich verbunden")

    // 3. Messung starten
    val measurementResult = measurementService.startMeasurement()
    if (measurementResult.isSuccess) {
        println("Messung gestartet")

        // 4. Messdaten überwachen
        measurementService.measurementData.collect { reading ->
            reading?.let {
                println("Neue Messung: Frequenz=${it.frequency}Hz, Tiefe=${it.depth}m")
            }
        }
    }
}
```

### 📁 **Export/Import Beispiel**

```kotlin
// Export
val readings = measurementService.getAllReadings()
val exportResult = GhidraExportImportFunctions.exportDAT1Click(
    context = context,
    readings = readings,
    fileName = "messung_$(System.currentTimeMillis()).dat"
)

if (exportResult.isSuccess) {
    println("Export erfolgreich: ${exportResult.getOrNull()}")
}

// Import
val importResult = GhidraExportImportFunctions.importTabletFile1Click(
    context = context,
    filePath = "/path/to/import/file.dat"
)

if (importResult.isSuccess) {
    val (importedReadings, validationResult) = importResult.getOrThrow()
    println("Import erfolgreich: ${importedReadings.size} Messungen")
    println("Validierung: ${validationResult.isValid}")
}
```

---

**Die EMFAD Android App API bietet vollständige Funktionalität basierend auf der echten Ghidra-Rekonstruktion der originalen Windows-EMFAD-Software!** 🚀📱⚡
