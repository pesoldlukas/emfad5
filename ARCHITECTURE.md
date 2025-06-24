# 🏗️ EMFAD APP ARCHITEKTUR

## 📐 SYSTEM-ARCHITEKTUR ÜBERSICHT

Die EMFAD Android App folgt einer **Clean Architecture** mit **MVVM-Pattern** und ist speziell für **Samsung S21 Ultra** optimiert. Die Architektur basiert auf der vollständigen **Ghidra-Rekonstruktion** der originalen Windows-EMFAD-Software.

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                       │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   UI Screens    │  │  ViewModels     │  │  Compose UI  │ │
│  │  (Jetpack       │  │   (MVVM)        │  │ (Material 3) │ │
│  │   Compose)      │  │                 │  │              │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────┐
│                    DOMAIN LAYER                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   Use Cases     │  │   Repositories  │  │    Models    │ │
│  │  (Business      │  │   (Interfaces)  │  │ (Domain      │ │
│  │    Logic)       │  │                 │  │  Entities)   │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────┐
│                     DATA LAYER                              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │ Ghidra-Recon-   │  │   Services      │  │   Database   │ │
│  │ structed Core   │  │ (Background)    │  │    (Room)    │ │
│  │ (EMFAD Logic)   │  │                 │  │              │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────┐
│                  HARDWARE LAYER                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   USB-Serial    │  │   Bluetooth     │  │   ARCore     │ │
│  │   (FTDI, etc.)  │  │     (BLE)       │  │ (Optional)   │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## 🔍 GHIDRA-REKONSTRUIERTE KERN-ARCHITEKTUR

### 📊 **Ghidra-Komponenten-Hierarchie**

```
com.emfad.app.ghidra/
├── 🎯 GhidraDeviceController.kt
│   ├── FormCreate() ← EMFAD3EXE.c:157872
│   ├── connectToDevice() ← COM-Port Management
│   ├── startMeasurement() ← EMFAD-Protokoll
│   └── USB-Serial Support (FTDI, Prolific, Silicon Labs)
│
├── 📁 GhidraExportImportFunctions.kt
│   ├── exportDAT1Click() ← EMFAD3EXE.c:158304
│   ├── export2D1Click() ← 2D-Export-Funktion
│   ├── importTabletFile1Click() ← Import-Funktion
│   └── EGD/ESD/FADS Format Support
│
├── 🧮 GhidraFortranProcessor.kt
│   ├── readlineUn() ← HzEMSoftexe.c:1312
│   ├── readlineF() ← HzEMSoftexe.c:3647
│   ├── processEMFData() ← Mathematische Algorithmen
│   └── Array-Bounds-Checking ← Fortran-Kompatibilität
│
├── 📋 GhidraReconstructedDataModels.kt
│   ├── EMFADTabletConfig ← "EMFAD TABLET 1.0"
│   ├── AutobalanceConfig ← "autobalance values; version 1.0"
│   ├── FrequencyConfig ← 7 EMFAD-Frequenzen
│   └── CalibrationStatus ← EMUNIX07EXE.c Kalibrierung
│
└── 🎨 GhidraReconstructedUIComponents.kt
    ├── TfrmFrequencyModeSelect ← Frequenzauswahl-Dialog
    ├── TfrmAutoBalance ← Autobalance-Formular
    ├── ExportDialog ← Export-Funktionen
    └── ImportDialog ← Import-Funktionen
```

### 🔄 **Datenfluss-Architektur**

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   EMFAD-Gerät   │───▶│ GhidraDevice    │───▶│ MeasurementData │
│  (USB/Bluetooth)│    │   Controller    │    │   Processing    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │                       │
                                ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Fortran-        │◀───│ Raw EMF Data    │───▶│ Database        │
│ Processor       │    │ (Real/Imaginary)│    │ (Room)          │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Processed       │───▶│ UI Components   │◀───│ Export/Import   │
│ EMF Readings    │    │ (Compose)       │    │ Functions       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 🎨 PRESENTATION LAYER

### 📱 **UI-Architektur (Jetpack Compose)**

```kotlin
// Screen-Hierarchie
sealed class EMFADScreen(val route: String) {
    object Home : EMFADScreen("home")
    object Measurement : EMFADScreen("measurement")
    object Analysis : EMFADScreen("analysis")
    object Settings : EMFADScreen("settings")
    object Calibration : EMFADScreen("calibration")
}

// Navigation-Struktur
@Composable
fun EMFADNavigation() {
    NavHost(
        navController = navController,
        startDestination = EMFADScreen.Home.route
    ) {
        composable(EMFADScreen.Home.route) { HomeScreen() }
        composable(EMFADScreen.Measurement.route) { MeasurementScreen() }
        composable(EMFADScreen.Analysis.route) { AnalysisScreen() }
        composable(EMFADScreen.Settings.route) { SettingsScreen() }
        composable(EMFADScreen.Calibration.route) { CalibrationScreen() }
    }
}
```

### 🎯 **ViewModel-Architektur (MVVM)**

```kotlin
// Basis-ViewModel für alle Screens
abstract class BaseEMFADViewModel : ViewModel() {
    protected val _uiState = MutableStateFlow(UIState.Loading)
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()

    protected val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()
}

// Measurement-spezifisches ViewModel
class MeasurementViewModel(
    private val measurementService: MeasurementService,
    private val ghidraDeviceController: GhidraDeviceController
) : BaseEMFADViewModel() {

    private val _measurementData = MutableStateFlow<EMFReading?>(null)
    val measurementData: StateFlow<EMFReading?> = _measurementData.asStateFlow()

    private val _deviceStatus = MutableStateFlow(DeviceStatus())
    val deviceStatus: StateFlow<DeviceStatus> = _deviceStatus.asStateFlow()

    fun startMeasurement() {
        viewModelScope.launch {
            try {
                _uiState.value = UIState.Loading
                val result = measurementService.startMeasurement()
                if (result.isSuccess) {
                    _uiState.value = UIState.Success
                    monitorMeasurementData()
                } else {
                    _uiState.value = UIState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                _uiState.value = UIState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun monitorMeasurementData() {
        viewModelScope.launch {
            measurementService.measurementData.collect { reading ->
                _measurementData.value = reading
            }
        }
    }
}
```

## 🏢 DOMAIN LAYER

### 📋 **Use Cases**

```kotlin
// Measurement Use Cases
class StartMeasurementUseCase(
    private val measurementRepository: MeasurementRepository,
    private val ghidraDeviceController: GhidraDeviceController
) {
    suspend operator fun invoke(config: MeasurementConfig): Result<Unit> {
        return try {
            // 1. Ghidra-Device initialisieren
            ghidraDeviceController.formCreate()

            // 2. Verbindung herstellen
            val connected = ghidraDeviceController.connectToDevice()
            if (!connected) {
                return Result.failure(Exception("Device connection failed"))
            }

            // 3. Messung starten
            val started = ghidraDeviceController.startMeasurement(config.frequencyConfig)
            if (started) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Measurement start failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Export Use Cases
class ExportDataUseCase(
    private val ghidraExportImport: GhidraExportImportFunctions,
    private val measurementRepository: MeasurementRepository
) {
    suspend operator fun invoke(
        format: ExportFormat,
        fileName: String,
        sessionId: Long
    ): Result<String> {
        return try {
            val readings = measurementRepository.getReadingsBySession(sessionId)

            when (format) {
                ExportFormat.DAT -> ghidraExportImport.exportDAT1Click(context, readings, fileName)
                ExportFormat.EGD -> ghidraExportImport.exportEGDFormat(context, readings, fileName)
                ExportFormat.ESD -> ghidraExportImport.exportESDFormat(context, readings, fileName)
                ExportFormat.TWO_D -> ghidraExportImport.export2D1Click(context, readings, null, fileName)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### 🔄 **Repository Pattern**

```kotlin
// Measurement Repository Interface
interface MeasurementRepository {
    suspend fun startSession(session: MeasurementSession): Result<Long>
    suspend fun saveReading(reading: EMFReading): Result<Unit>
    suspend fun getReadingsBySession(sessionId: Long): List<EMFReading>
    suspend fun getAllSessions(): List<MeasurementSession>
    suspend fun deleteSession(sessionId: Long): Result<Unit>
}

// Implementation mit Room Database
class MeasurementRepositoryImpl(
    private val database: EMFADDatabase,
    private val ghidraFortranProcessor: GhidraFortranProcessor
) : MeasurementRepository {

    override suspend fun saveReading(reading: EMFReading): Result<Unit> {
        return try {
            // 1. Fortran-Verarbeitung anwenden
            val processedReadings = ghidraFortranProcessor.processEMFData(
                listOf(reading),
                FrequencyConfig()
            )

            val processedReading = processedReadings.getOrThrow().first()

            // 2. In Datenbank speichern
            val entity = EMFReadingEntity.fromDomainModel(processedReading)
            database.emfReadingDao().insert(entity)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

## 💾 DATA LAYER

### 🗄️ **Database-Architektur (Room)**

```kotlin
// Database-Schema
@Database(
    entities = [
        EMFReadingEntity::class,
        MeasurementSessionEntity::class,
        MaterialAnalysisEntity::class,
        CalibrationDataEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class EMFADDatabase : RoomDatabase() {
    abstract fun emfReadingDao(): EMFReadingDao
    abstract fun measurementSessionDao(): MeasurementSessionDao
    abstract fun materialAnalysisDao(): MaterialAnalysisDao
    abstract fun calibrationDataDao(): CalibrationDataDao

    companion object {
        @Volatile
        private var INSTANCE: EMFADDatabase? = null

        fun getDatabase(context: Context): EMFADDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EMFADDatabase::class.java,
                    "emfad_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// Entity-Definitionen
@Entity(tableName = "emf_readings")
data class EMFReadingEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: Long,
    val timestamp: Long,
    val frequency: Double,
    val signalStrength: Double,
    val phase: Double,
    val amplitude: Double,
    val realPart: Double,
    val imaginaryPart: Double,
    val magnitude: Double,
    val depth: Double,
    val temperature: Double,
    val humidity: Double,
    val pressure: Double,
    val batteryLevel: Int,
    val deviceId: String,
    val materialType: String,
    val confidence: Double,
    val noiseLevel: Double,
    val calibrationOffset: Double,
    val gainSetting: Double,
    val filterSetting: String,
    val measurementMode: String,
    val qualityScore: Double,
    val xCoordinate: Double,
    val yCoordinate: Double,
    val zCoordinate: Double,
    val gpsData: String
) {
    fun toDomainModel(): EMFReading {
        return EMFReading(
            sessionId = sessionId,
            timestamp = timestamp,
            frequency = frequency,
            signalStrength = signalStrength,
            phase = phase,
            amplitude = amplitude,
            realPart = realPart,
            imaginaryPart = imaginaryPart,
            magnitude = magnitude,
            depth = depth,
            temperature = temperature,
            humidity = humidity,
            pressure = pressure,
            batteryLevel = batteryLevel,
            deviceId = deviceId,
            materialType = MaterialType.valueOf(materialType),
            confidence = confidence,
            noiseLevel = noiseLevel,
            calibrationOffset = calibrationOffset,
            gainSetting = gainSetting,
            filterSetting = filterSetting,
            measurementMode = measurementMode,
            qualityScore = qualityScore,
            xCoordinate = xCoordinate,
            yCoordinate = yCoordinate,
            zCoordinate = zCoordinate,
            gpsData = gpsData
        )
    }

    companion object {
        fun fromDomainModel(reading: EMFReading): EMFReadingEntity {
            return EMFReadingEntity(
                sessionId = reading.sessionId,
                timestamp = reading.timestamp,
                frequency = reading.frequency,
                signalStrength = reading.signalStrength,
                phase = reading.phase,
                amplitude = reading.amplitude,
                realPart = reading.realPart,
                imaginaryPart = reading.imaginaryPart,
                magnitude = reading.magnitude,
                depth = reading.depth,
                temperature = reading.temperature,
                humidity = reading.humidity,
                pressure = reading.pressure,
                batteryLevel = reading.batteryLevel,
                deviceId = reading.deviceId,
                materialType = reading.materialType.name,
                confidence = reading.confidence,
                noiseLevel = reading.noiseLevel,
                calibrationOffset = reading.calibrationOffset,
                gainSetting = reading.gainSetting,
                filterSetting = reading.filterSetting,
                measurementMode = reading.measurementMode,
                qualityScore = reading.qualityScore,
                xCoordinate = reading.xCoordinate,
                yCoordinate = reading.yCoordinate,
                zCoordinate = reading.zCoordinate,
                gpsData = reading.gpsData
            )
        }
    }
}
```

### 🔧 **Service-Architektur**

```kotlin
// MeasurementService - Kern-Service mit Ghidra-Integration
class MeasurementService : Service() {

    // Ghidra-Komponenten
    private lateinit var ghidraDeviceController: GhidraDeviceController
    private lateinit var ghidraExportImport: GhidraExportImportFunctions
    private lateinit var ghidraFortranProcessor: GhidraFortranProcessor

    // EMFAD-Konfigurationen
    private var emfadTabletConfig = EMFADTabletConfig()
    private var autobalanceConfig = AutobalanceConfig()
    private var frequencyConfig = FrequencyConfig()

    // State Management
    private val _serviceState = MutableStateFlow(MeasurementServiceState.IDLE)
    val serviceState: StateFlow<MeasurementServiceState> = _serviceState.asStateFlow()

    private val _measurementData = MutableStateFlow<EMFReading?>(null)
    val measurementData: StateFlow<EMFReading?> = _measurementData.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        initializeGhidraComponents()
        startBackgroundProcessing()
    }

    private fun initializeGhidraComponents() {
        // Ghidra-Komponenten initialisieren
        ghidraDeviceController = GhidraDeviceController(this)
        ghidraExportImport = GhidraExportImportFunctions
        ghidraFortranProcessor = GhidraFortranProcessor

        // FormCreate - Initialisierung aus EMFAD3EXE.c
        ghidraDeviceController.formCreate()

        // EMFAD TABLET 1.0 Konfiguration
        emfadTabletConfig = EMFADTabletConfig(
            version = "EMFAD TABLET 1.0",
            scanMode = EMFADScanMode.SCAN_2D_3D,
            isInitialized = true
        )

        // Autobalance-System aus EMUNIX07EXE.c
        autobalanceConfig = AutobalanceConfig(
            version = "autobalance values; version 1.0"
        )

        // Frequenz-Konfiguration
        frequencyConfig = FrequencyConfig()

        Log.d(TAG, "Ghidra-Komponenten initialisiert")
    }

    suspend fun startMeasurement(): Result<Unit> {
        return try {
            // Ghidra-Gerät für Messung vorbereiten
            val measurementStarted = ghidraDeviceController.startMeasurement(frequencyConfig)

            if (measurementStarted) {
                _serviceState.value = MeasurementServiceState.MEASURING

                // Überwache Messdaten
                serviceScope.launch {
                    ghidraDeviceController.measurementData.collect { reading ->
                        reading?.let {
                            // Fortran-Verarbeitung anwenden
                            val processedReading = applyFortranProcessing(it)
                            _measurementData.value = processedReading
                        }
                    }
                }

                Result.success(Unit)
            } else {
                Result.failure(Exception("Measurement start failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun applyFortranProcessing(reading: EMFReading): EMFReading {
        return try {
            val processedReadings = ghidraFortranProcessor.processEMFData(
                listOf(reading),
                frequencyConfig
            )
            processedReadings.getOrNull()?.firstOrNull() ?: reading
        } catch (e: Exception) {
            Log.w(TAG, "Fehler bei Fortran-Verarbeitung", e)
            reading
        }
    }
}
```

## 🔌 HARDWARE LAYER

### 📡 **USB-Serial Architektur**

```kotlin
// USB-Serial Device Management
class USBSerialManager(private val context: Context) {

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    // Unterstützte USB-Serial Adapter
    private val supportedDevices = mapOf(
        // FTDI Devices
        Pair(0x0403, 0x6001) to "FTDI FT232R",
        Pair(0x0403, 0x6010) to "FTDI FT2232H",
        Pair(0x0403, 0x6011) to "FTDI FT4232H",

        // Prolific Devices
        Pair(0x067B, 0x2303) to "Prolific PL2303",
        Pair(0x067B, 0x04BB) to "Prolific PL2303HX",

        // Silicon Labs Devices
        Pair(0x10C4, 0xEA60) to "Silicon Labs CP210x",
        Pair(0x10C4, 0xEA70) to "Silicon Labs CP210x",

        // Direkte EMFAD-Geräte (falls verfügbar)
        Pair(0x1234, 0x5678) to "EMFAD-UG Direct"
    )

    fun scanForEMFADDevices(): List<EMFADDevice> {
        val devices = mutableListOf<EMFADDevice>()

        usbManager.deviceList.values.forEach { usbDevice ->
            val key = Pair(usbDevice.vendorId, usbDevice.productId)
            supportedDevices[key]?.let { deviceName ->
                devices.add(
                    EMFADDevice(
                        usbDevice = usbDevice,
                        name = deviceName,
                        type = EMFADDeviceType.USB_SERIAL,
                        isSupported = true
                    )
                )
            }
        }

        return devices
    }

    fun connectToDevice(device: EMFADDevice): EMFADConnection? {
        return try {
            val connection = usbManager.openDevice(device.usbDevice)
            connection?.let {
                EMFADUSBConnection(
                    usbConnection = it,
                    device = device,
                    baudRate = 115200,
                    dataBits = 8,
                    stopBits = 1,
                    parity = UsbSerialInterface.PARITY_NONE
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "USB connection failed", e)
            null
        }
    }
}

// EMFAD-spezifische USB-Verbindung
class EMFADUSBConnection(
    private val usbConnection: UsbDeviceConnection,
    private val device: EMFADDevice,
    private val baudRate: Int,
    private val dataBits: Int,
    private val stopBits: Int,
    private val parity: Int
) : EMFADConnection {

    private var isConnected = false

    override suspend fun initialize(): Boolean {
        return try {
            // USB-Interface beanspruchen
            val intf = device.usbDevice.getInterface(0)
            if (usbConnection.claimInterface(intf, true)) {

                // Serial-Parameter konfigurieren
                configureSerialParameters()

                // EMFAD-Protokoll initialisieren
                initializeEMFADProtocol()

                isConnected = true
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "USB initialization failed", e)
            false
        }
    }

    private fun configureSerialParameters() {
        when (device.usbDevice.vendorId) {
            0x0403 -> configureFTDI()      // FTDI
            0x067B -> configureProlific()  // Prolific
            0x10C4 -> configureSiliconLabs() // Silicon Labs
        }
    }

    private fun configureFTDI() {
        // FTDI FT232 Konfiguration für 115200 8N1
        usbConnection.controlTransfer(0x40, 0x00, 0x0000, 0, null, 0, 1000) // Reset
        usbConnection.controlTransfer(0x40, 0x03, 0x4138, 0, null, 0, 1000) // Set baud rate
        usbConnection.controlTransfer(0x40, 0x04, 0x0008, 0, null, 0, 1000) // Set data bits
    }

    override suspend fun sendCommand(command: EMFADCommand): EMFADResponse? {
        return try {
            val commandBytes = command.toByteArray()
            val outEndpoint = device.usbDevice.getInterface(0).getEndpoint(0)

            val bytesSent = usbConnection.bulkTransfer(
                outEndpoint,
                commandBytes,
                commandBytes.size,
                1000
            )

            if (bytesSent > 0) {
                receiveResponse()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Command send failed", e)
            null
        }
    }

    private suspend fun receiveResponse(): EMFADResponse? {
        return try {
            val inEndpoint = device.usbDevice.getInterface(0).getEndpoint(1)
            val buffer = ByteArray(1024)

            val bytesReceived = usbConnection.bulkTransfer(
                inEndpoint,
                buffer,
                buffer.size,
                1000
            )

            if (bytesReceived > 0) {
                EMFADResponse.fromByteArray(buffer.copyOf(bytesReceived))
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Response receive failed", e)
            null
        }
    }
}
```

### 📶 **Bluetooth BLE Architektur**

```kotlin
// Nordic BLE Manager Integration
class EMFADBLEManager(private val context: Context) {

    private val bleManager = BleManager(context)
    private var currentDevice: BluetoothDevice? = null
    private var bleConnection: EMFADBLEConnection? = null

    // EMFAD-spezifische Service UUIDs
    companion object {
        val EMFAD_SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-123456789abc")
        val EMFAD_DATA_CHARACTERISTIC = UUID.fromString("12345678-1234-1234-1234-123456789abd")
        val EMFAD_COMMAND_CHARACTERISTIC = UUID.fromString("12345678-1234-1234-1234-123456789abe")
        val EMFAD_STATUS_CHARACTERISTIC = UUID.fromString("12345678-1234-1234-1234-123456789abf")
    }

    fun scanForEMFADDevices(): Flow<EMFADDevice> = flow {
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        val scanFilters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(EMFAD_SERVICE_UUID))
                .build()
        )

        val scanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = EMFADDevice(
                    bluetoothDevice = result.device,
                    name = result.device.name ?: "Unknown EMFAD Device",
                    type = EMFADDeviceType.BLUETOOTH_LE,
                    rssi = result.rssi,
                    isSupported = true
                )

                // Emit device über Flow
                runBlocking { emit(device) }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "BLE scan failed with error: $errorCode")
            }
        }

        scanner.startScan(scanFilters, scanSettings, scanCallback)

        // Scan für 10 Sekunden
        delay(10000)
        scanner.stopScan(scanCallback)
    }

    suspend fun connectToDevice(device: EMFADDevice): EMFADConnection? {
        return try {
            currentDevice = device.bluetoothDevice

            val connection = EMFADBLEConnection(
                context = context,
                device = device.bluetoothDevice!!
            )

            val connected = connection.connect()
            if (connected) {
                bleConnection = connection
                connection
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "BLE connection failed", e)
            null
        }
    }
}

// EMFAD-spezifische BLE-Verbindung
class EMFADBLEConnection(
    private val context: Context,
    private val device: BluetoothDevice
) : BleManagerCallbacks, EMFADConnection {

    private val bleManager = object : BleManager(context) {
        override fun getGattCallback(): BleManagerGattCallback = EMFADGattCallback()
    }

    private var dataCharacteristic: BluetoothGattCharacteristic? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    private var statusCharacteristic: BluetoothGattCharacteristic? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _measurementData = MutableSharedFlow<EMFReading>()
    val measurementData: SharedFlow<EMFReading> = _measurementData.asSharedFlow()

    suspend fun connect(): Boolean = suspendCoroutine { continuation ->
        bleManager.setGattCallbacks(this)

        bleManager.connect(device)
            .timeout(10000)
            .enqueue()

        // Warte auf Verbindungsresultat
        CoroutineScope(Dispatchers.IO).launch {
            connectionState.first { it == ConnectionState.CONNECTED || it == ConnectionState.DISCONNECTED }
            continuation.resume(connectionState.value == ConnectionState.CONNECTED)
        }
    }

    override fun onDeviceConnected(device: BluetoothDevice) {
        _connectionState.value = ConnectionState.CONNECTED
        Log.d(TAG, "EMFAD device connected: ${device.address}")
    }

    override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
        _connectionState.value = ConnectionState.DISCONNECTED
        Log.d(TAG, "EMFAD device disconnected: ${device.address}, reason: $reason")
    }

    override suspend fun sendCommand(command: EMFADCommand): EMFADResponse? {
        return try {
            commandCharacteristic?.let { characteristic ->
                val commandBytes = command.toByteArray()

                bleManager.writeCharacteristic(characteristic, commandBytes)
                    .timeout(5000)
                    .await()

                // Warte auf Antwort über Status-Characteristic
                receiveResponse()
            }
        } catch (e: Exception) {
            Log.e(TAG, "BLE command send failed", e)
            null
        }
    }

    private suspend fun receiveResponse(): EMFADResponse? = suspendCoroutine { continuation ->
        // Implementation für Response-Handling über BLE
        // Nutzt Status-Characteristic für Antworten
        continuation.resume(null) // Placeholder
    }

    inner class EMFADGattCallback : BleManagerGattCallback() {

        override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            val service = gatt.getService(EMFADBLEManager.EMFAD_SERVICE_UUID)

            if (service != null) {
                dataCharacteristic = service.getCharacteristic(EMFADBLEManager.EMFAD_DATA_CHARACTERISTIC)
                commandCharacteristic = service.getCharacteristic(EMFADBLEManager.EMFAD_COMMAND_CHARACTERISTIC)
                statusCharacteristic = service.getCharacteristic(EMFADBLEManager.EMFAD_STATUS_CHARACTERISTIC)

                return dataCharacteristic != null &&
                       commandCharacteristic != null &&
                       statusCharacteristic != null
            }

            return false
        }

        override fun initialize() {
            // Enable notifications für Data-Characteristic
            dataCharacteristic?.let { characteristic ->
                enableNotifications(characteristic).enqueue()
            }

            // Enable notifications für Status-Characteristic
            statusCharacteristic?.let { characteristic ->
                enableNotifications(characteristic).enqueue()
            }
        }

        override fun onCharacteristicNotification(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            data: ByteArray
        ) {
            when (characteristic.uuid) {
                EMFADBLEManager.EMFAD_DATA_CHARACTERISTIC -> {
                    // Parse EMF-Daten
                    val emfReading = parseEMFData(data)
                    emfReading?.let {
                        CoroutineScope(Dispatchers.IO).launch {
                            _measurementData.emit(it)
                        }
                    }
                }

                EMFADBLEManager.EMFAD_STATUS_CHARACTERISTIC -> {
                    // Parse Status-Updates
                    val statusUpdate = parseStatusUpdate(data)
                    Log.d(TAG, "Status update: $statusUpdate")
                }
            }
        }

        private fun parseEMFData(data: ByteArray): EMFReading? {
            return try {
                // Parse EMFAD-Datenpaket
                val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

                val timestamp = System.currentTimeMillis()
                val frequency = buffer.double
                val realPart = buffer.double
                val imaginaryPart = buffer.double
                val temperature = buffer.float.toDouble()
                val batteryLevel = buffer.get().toInt() and 0xFF

                val magnitude = sqrt(realPart * realPart + imaginaryPart * imaginaryPart)
                val phase = atan2(imaginaryPart, realPart) * 180.0 / PI

                // EMFAD-spezifische Tiefenberechnung
                val calibrationConstant = 3333.0
                val attenuationFactor = 0.417
                val calibratedSignal = magnitude * (calibrationConstant / 1000.0)
                val depth = if (calibratedSignal > 0) {
                    -ln(calibratedSignal / 1000.0) / attenuationFactor
                } else {
                    0.0
                }

                EMFReading(
                    sessionId = timestamp / 1000,
                    timestamp = timestamp,
                    frequency = frequency,
                    signalStrength = magnitude,
                    phase = phase,
                    amplitude = magnitude,
                    realPart = realPart,
                    imaginaryPart = imaginaryPart,
                    magnitude = magnitude,
                    depth = depth,
                    temperature = temperature,
                    humidity = 50.0,
                    pressure = 1013.25,
                    batteryLevel = batteryLevel,
                    deviceId = device.address,
                    materialType = MaterialType.UNKNOWN,
                    confidence = 0.0,
                    noiseLevel = 10.0,
                    calibrationOffset = 0.0,
                    gainSetting = 1.0,
                    filterSetting = "default",
                    measurementMode = "A",
                    qualityScore = min(1.0, magnitude / 1000.0),
                    xCoordinate = 0.0,
                    yCoordinate = 0.0,
                    zCoordinate = 0.0,
                    gpsData = ""
                )

            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse EMF data", e)
                null
            }
        }
    }
}
```