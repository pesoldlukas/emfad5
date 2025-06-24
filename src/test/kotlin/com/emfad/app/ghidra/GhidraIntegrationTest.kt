package com.emfad.app.ghidra

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.emfad.app.models.EMFReading
import com.emfad.app.models.MaterialType
import com.emfad.app.models.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * GHIDRA-INTEGRATIONS-TESTS
 * 
 * Testet die vollständige Integration der rekonstruierten EMFAD-Funktionen
 * aus der Ghidra-Dekompilierung der originalen Windows-EXE-Dateien.
 */
@RunWith(AndroidJUnit4::class)
class GhidraIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var ghidraDeviceController: GhidraDeviceController
    private lateinit var ghidraExportImport: GhidraExportImportFunctions
    private lateinit var ghidraFortranProcessor: GhidraFortranProcessor
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        ghidraDeviceController = GhidraDeviceController(context)
        ghidraExportImport = GhidraExportImportFunctions
        ghidraFortranProcessor = GhidraFortranProcessor
    }
    
    @Test
    fun testEMFADTabletConfigInitialization() {
        // Test EMFAD TABLET 1.0 Konfiguration
        val config = EMFADTabletConfig()
        
        assertEquals("EMFAD TABLET 1.0", config.version)
        assertEquals(EMFADScanMode.SCAN_2D_3D, config.scanMode)
        assertFalse(config.isInitialized)
        assertTrue(config.lastConfigUpdate > 0)
    }
    
    @Test
    fun testAutobalanceConfigInitialization() {
        // Test Autobalance-Konfiguration aus EMUNIX07EXE.c
        val config = AutobalanceConfig()
        
        assertEquals("autobalance values; version 1.0", config.version)
        assertEquals(CalibrationStatus.NOT_STARTED, config.compassCalibrationStatus)
        assertEquals(CalibrationStatus.NOT_STARTED, config.horizontalCalibrationStatus)
        assertEquals(CalibrationStatus.NOT_STARTED, config.verticalCalibrationStatus)
        assertEquals(0.0f, config.horizontalOffsetX)
        assertEquals(0.0f, config.horizontalOffsetY)
        assertEquals(1.0f, config.horizontalScaleX)
        assertEquals(1.0f, config.horizontalScaleY)
    }
    
    @Test
    fun testFrequencyConfigInitialization() {
        // Test Frequenz-Konfiguration
        val config = FrequencyConfig()
        
        assertEquals(7, config.availableFrequencies.size)
        assertEquals(19000.0, config.availableFrequencies[0], 0.1) // f0 - 19.0 KHz
        assertEquals(23400.0, config.availableFrequencies[1], 0.1) // f1 - 23.4 KHz
        assertEquals(70000.0, config.availableFrequencies[2], 0.1) // f2 - 70.0 KHz
        assertEquals(77500.0, config.availableFrequencies[3], 0.1) // f3 - 77.5 KHz
        assertEquals(124000.0, config.availableFrequencies[4], 0.1) // f4 - 124.0 KHz
        assertEquals(129100.0, config.availableFrequencies[5], 0.1) // f5 - 129.1 KHz
        assertEquals(135600.0, config.availableFrequencies[6], 0.1) // f6 - 135.6 KHz
        
        assertEquals(7, config.activeFrequencies.size)
        assertTrue(config.activeFrequencies.all { it }) // Alle Frequenzen aktiv
    }
    
    @Test
    fun testDeviceControllerFormCreate() {
        // Test FormCreate-Funktion aus EMFAD3EXE.c
        ghidraDeviceController.formCreate()
        
        val deviceStatus = ghidraDeviceController.deviceStatus.value
        assertEquals("no port", deviceStatus.portStatus)
        assertEquals("EMFAD-UG", deviceStatus.deviceType)
        assertFalse(deviceStatus.isConnected)
        assertTrue(deviceStatus.lastCommunication > 0)
    }
    
    @Test
    fun testFortranReadlineUn() {
        // Test readline_un Funktion aus HzEMSoftexe.c
        val result = ghidraFortranProcessor.readlineUn(
            nunitr = 10,
            line = "Test line for readline_un",
            ios = 0,
            lineLength = 256
        )
        
        assertTrue(result.isSuccess)
        assertEquals(".\\HzHxEMSoft.f90", result.sourceFile)
        assertEquals("readline_un", result.functionName)
        assertEquals(0, result.iosStatus)
        assertTrue(result.processedData.isNotEmpty())
    }
    
    @Test
    fun testFortranReadlineF() {
        // Test readline_f Funktion aus HzEMSoftexe.c
        val result = ghidraFortranProcessor.readlineF(
            nunitr = 10,
            line = "Test line for readline_f",
            ios = 0,
            lineLength = 256
        )
        
        assertTrue(result.isSuccess)
        assertEquals(".\\HzHxEMSoft.f90", result.sourceFile)
        assertEquals("readline_f", result.functionName)
        assertEquals(0, result.iosStatus)
        assertTrue(result.processedData.isNotEmpty())
    }
    
    @Test
    fun testFortranArrayBoundsChecking() {
        // Test Array-Bounds-Checking aus HzEMSoftexe.c
        val validResult = ghidraFortranProcessor.checkArrayBounds(
            arrayName = "test_array",
            index = 5,
            lowerBound = 0,
            upperBound = 10,
            lineNumber = 1321
        )
        
        assertTrue(validResult.isSuccess)
        assertEquals(0, validResult.iosStatus)
        
        val invalidResult = ghidraFortranProcessor.checkArrayBounds(
            arrayName = "test_array",
            index = 15,
            lowerBound = 0,
            upperBound = 10,
            lineNumber = 1321
        )
        
        assertFalse(invalidResult.isSuccess)
        assertEquals(-1, invalidResult.iosStatus)
        assertTrue(invalidResult.errorMessage.contains("above upper bound"))
    }
    
    @Test
    fun testEMFDataProcessing() = runBlocking {
        // Test EMF-Datenverarbeitung mit Fortran-Algorithmen
        val testReadings = listOf(
            createTestEMFReading(19000.0, 1000.0, 45.0),
            createTestEMFReading(23400.0, 1200.0, 50.0),
            createTestEMFReading(70000.0, 800.0, 30.0)
        )
        
        val frequencyConfig = FrequencyConfig()
        val result = ghidraFortranProcessor.processEMFData(testReadings, frequencyConfig)
        
        assertTrue(result.isSuccess)
        val processedReadings = result.getOrThrow()
        assertEquals(3, processedReadings.size)
        
        // Prüfe dass Tiefenberechnung angewendet wurde
        processedReadings.forEach { reading ->
            assertTrue(reading.depth >= 0.0)
            assertTrue(reading.qualityScore >= 0.0)
            assertTrue(reading.qualityScore <= 1.0)
        }
    }
    
    @Test
    fun testExportDAT1Click() = runBlocking {
        // Test ExportDAT1Click Funktion aus EMFAD3EXE.c
        val testReadings = listOf(
            createTestEMFReading(19000.0, 1000.0, 45.0),
            createTestEMFReading(23400.0, 1200.0, 50.0)
        )
        
        val result = ghidraExportImport.exportDAT1Click(
            context = context,
            readings = testReadings,
            fileName = "test_export.dat"
        )
        
        assertTrue(result.isSuccess)
        val filePath = result.getOrThrow()
        assertTrue(filePath.contains("test_export.dat"))
        assertTrue(filePath.contains("exports"))
    }
    
    @Test
    fun testExport2D1Click() = runBlocking {
        // Test Export2D1Click Funktion aus EMFAD3EXE.c
        val testReadings = listOf(
            createTestEMFReading(19000.0, 1000.0, 45.0),
            createTestEMFReading(23400.0, 1200.0, 50.0)
        )
        
        val result = ghidraExportImport.export2D1Click(
            context = context,
            readings = testReadings,
            analysis = null,
            fileName = "test_2d_export.txt"
        )
        
        assertTrue(result.isSuccess)
        val filePath = result.getOrThrow()
        assertTrue(filePath.contains("test_2d_export.txt"))
    }
    
    @Test
    fun testEGDFormatExport() = runBlocking {
        // Test EGD-Format Export
        val testReadings = listOf(
            createTestEMFReading(19000.0, 1000.0, 45.0),
            createTestEMFReading(23400.0, 1200.0, 50.0)
        )
        
        val result = ghidraExportImport.exportEGDFormat(
            context = context,
            readings = testReadings,
            fileName = "test.egd"
        )
        
        assertTrue(result.isSuccess)
        val filePath = result.getOrThrow()
        assertTrue(filePath.endsWith("test.egd"))
    }
    
    @Test
    fun testESDFormatExport() = runBlocking {
        // Test ESD-Format Export
        val testReadings = listOf(
            createTestEMFReading(19000.0, 1000.0, 45.0),
            createTestEMFReading(23400.0, 1200.0, 50.0)
        )
        
        val result = ghidraExportImport.exportESDFormat(
            context = context,
            readings = testReadings,
            fileName = "test.esd"
        )
        
        assertTrue(result.isSuccess)
        val filePath = result.getOrThrow()
        assertTrue(filePath.endsWith("test.esd"))
    }
    
    @Test
    fun testDataValidation() {
        // Test Datenvalidierung
        val testReadings = listOf(
            createTestEMFReading(19000.0, 1000.0, 45.0),
            createTestEMFReading(0.0, -100.0, 200.0), // Ungültige Daten
            createTestEMFReading(23400.0, 1200.0, 50.0)
        )
        
        val importConfig = ImportConfig(
            validateFrequency = true,
            validateTimestamp = true,
            skipInvalidLines = true
        )
        
        val result = ghidraExportImport.validateImportedData(testReadings, importConfig)
        
        assertTrue(result.hasValidSignalData)
        assertTrue(result.validRecordCount > 0)
        assertTrue(result.invalidRecordCount > 0)
        assertTrue(result.warningMessages.isNotEmpty())
    }
    
    @Test
    fun testComplexEMFDataProcessing() = runBlocking {
        // Test komplexe EMF-Datenverarbeitung
        val realArray = doubleArrayOf(1000.0, 1200.0, 800.0)
        val imaginaryArray = doubleArrayOf(500.0, 600.0, 400.0)
        val frequencyArray = doubleArrayOf(19000.0, 23400.0, 70000.0)
        
        val result = ghidraFortranProcessor.processComplexEMFData(
            realArray, imaginaryArray, frequencyArray
        )
        
        assertTrue(result.isSuccess)
        val readings = result.getOrThrow()
        assertEquals(3, readings.size)
        
        readings.forEachIndexed { index, reading ->
            assertEquals(realArray[index], reading.realPart, 0.1)
            assertEquals(imaginaryArray[index], reading.imaginaryPart, 0.1)
            assertEquals(frequencyArray[index], reading.frequency, 0.1)
            assertTrue(reading.magnitude > 0.0)
            assertTrue(reading.depth >= 0.0)
        }
    }
    
    private fun createTestEMFReading(frequency: Double, signalStrength: Double, phase: Double): EMFReading {
        return EMFReading(
            sessionId = 1L,
            timestamp = System.currentTimeMillis(),
            frequency = frequency,
            signalStrength = signalStrength,
            phase = phase,
            amplitude = signalStrength,
            realPart = signalStrength * kotlin.math.cos(phase * kotlin.math.PI / 180.0),
            imaginaryPart = signalStrength * kotlin.math.sin(phase * kotlin.math.PI / 180.0),
            magnitude = signalStrength,
            depth = 1.5,
            temperature = 25.0,
            humidity = 50.0,
            pressure = 1013.25,
            batteryLevel = 100,
            deviceId = "TEST-DEVICE",
            materialType = MaterialType.UNKNOWN,
            confidence = 0.8,
            noiseLevel = 10.0,
            calibrationOffset = 0.0,
            gainSetting = 1.0,
            filterSetting = "default",
            measurementMode = "A",
            qualityScore = 0.9,
            xCoordinate = 0.0,
            yCoordinate = 0.0,
            zCoordinate = 0.0,
            gpsData = ""
        )
    }
}
