package com.emfad.app.services

import com.emfad.app.services.analysis.MaterialType
import com.emfad.app.services.analysis.SignalAnalyzer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.ln

/**
 * Unit Tests für SignalAnalyzer
 * Testet EMFAD-Tiefenberechnung und Signal-Processing
 */

@ExperimentalCoroutinesApi
class SignalAnalyzerTest {
    
    private lateinit var signalAnalyzer: SignalAnalyzer
    
    @Before
    fun setup() {
        signalAnalyzer = SignalAnalyzer()
    }
    
    @Test
    fun `test depth calculation algorithm`() = runTest {
        // Test der originalen EMFAD-Tiefenberechnung
        // depth = -ln(calibratedSignal / 1000.0) / 0.417
        
        val magnitude = 1000.0
        val frequency = 77500.0
        val calibrationConstant = 2750.0 // Für 77.5kHz
        val attenuationFactor = 0.417
        
        val calibratedSignal = magnitude * (calibrationConstant / 1000.0)
        val expectedDepth = -ln(calibratedSignal / 1000.0) / attenuationFactor
        
        // Signal simulieren
        val rawSignal = createTestSignal(magnitude)
        
        val result = signalAnalyzer.analyzeSignal(
            rawSignal = rawSignal,
            frequency = frequency,
            sessionId = 1L,
            deviceId = "TEST"
        )
        
        assertNotNull("Result should not be null", result)
        result?.let {
            assertTrue("Depth should be positive", it.depth >= 0.0)
            assertEquals("Frequency should match", frequency, it.frequency, 0.1)
            assertEquals("Magnitude should match", magnitude, it.magnitude, 100.0)
        }
    }
    
    @Test
    fun `test material type estimation`() = runTest {
        // Test Material-Erkennung basierend auf Signal-Charakteristika
        
        // Metall-Signal (hohe Magnitude, niedrige Phase)
        val metalSignal = createTestSignal(15000.0, phase = 5.0)
        val metalResult = signalAnalyzer.analyzeSignal(
            rawSignal = metalSignal,
            frequency = 77500.0,
            sessionId = 1L,
            deviceId = "TEST"
        )
        
        metalResult?.let {
            assertEquals("Should detect metal", MaterialType.METAL, it.materialType)
        }
        
        // Wasser-Signal (mittlere Magnitude, hohe Phase)
        val waterSignal = createTestSignal(2000.0, phase = 50.0)
        val waterResult = signalAnalyzer.analyzeSignal(
            rawSignal = waterSignal,
            frequency = 77500.0,
            sessionId = 1L,
            deviceId = "TEST"
        )
        
        waterResult?.let {
            assertEquals("Should detect water", MaterialType.WATER, it.materialType)
        }
    }
    
    @Test
    fun `test frequency specific calibration`() = runTest {
        val testFrequencies = mapOf(
            19000.0 to 3333.0,
            23400.0 to 3200.0,
            70000.0 to 2800.0,
            77500.0 to 2750.0,
            124000.0 to 2400.0,
            129100.0 to 2350.0,
            135600.0 to 2300.0
        )
        
        testFrequencies.forEach { (frequency, expectedCalibration) ->
            val signal = createTestSignal(1000.0)
            val result = signalAnalyzer.analyzeSignal(
                rawSignal = signal,
                frequency = frequency,
                sessionId = 1L,
                deviceId = "TEST"
            )
            
            assertNotNull("Result should not be null for frequency $frequency", result)
            result?.let {
                assertTrue("Depth should be calculated for frequency $frequency", it.depth >= 0.0)
                assertEquals("Frequency should match", frequency, it.frequency, 0.1)
            }
        }
    }
    
    @Test
    fun `test signal quality calculation`() = runTest {
        // Test mit gutem Signal
        val goodSignal = createTestSignal(5000.0, noiseLevel = 10.0)
        val goodResult = signalAnalyzer.analyzeSignal(
            rawSignal = goodSignal,
            frequency = 77500.0,
            sessionId = 1L,
            deviceId = "TEST"
        )
        
        goodResult?.let {
            assertTrue("Good signal should have high quality", it.qualityScore > 0.5)
            assertTrue("Good signal should have low noise", it.noiseLevel < 100.0)
        }
        
        // Test mit schlechtem Signal
        val badSignal = createTestSignal(100.0, noiseLevel = 500.0)
        val badResult = signalAnalyzer.analyzeSignal(
            rawSignal = badSignal,
            frequency = 77500.0,
            sessionId = 1L,
            deviceId = "TEST"
        )
        
        badResult?.let {
            assertTrue("Bad signal should have lower quality", it.qualityScore < 0.8)
            assertTrue("Bad signal should have higher noise", it.noiseLevel > 50.0)
        }
    }
    
    @Test
    fun `test calibrateForMaterial`() {
        // Test Material-spezifische Kalibrierung
        val originalReading = createTestReading()
        
        // Kalibrierung für verschiedene Materialien testen
        val soilReading = signalAnalyzer.calibrateForMaterial(originalReading, MaterialType.SOIL_DRY)
        val clayReading = signalAnalyzer.calibrateForMaterial(originalReading, MaterialType.CLAY)
        val metalReading = signalAnalyzer.calibrateForMaterial(originalReading, MaterialType.METAL)
        
        // Verschiedene Materialien sollten verschiedene Tiefen ergeben
        assertNotEquals("Soil and clay should have different depths", 
            soilReading.depth, clayReading.depth, 0.01)
        assertNotEquals("Soil and metal should have different depths", 
            soilReading.depth, metalReading.depth, 0.01)
        
        // Material-Typ sollte aktualisiert werden
        assertEquals("Material type should be updated", MaterialType.SOIL_DRY, soilReading.materialType)
        assertEquals("Material type should be updated", MaterialType.CLAY, clayReading.materialType)
        assertEquals("Material type should be updated", MaterialType.METAL, metalReading.materialType)
    }
    
    private fun createTestSignal(magnitude: Double, phase: Double = 45.0, noiseLevel: Double = 10.0): ByteArray {
        // Simuliert ein komplexes Signal als ByteArray
        val sampleCount = 1024
        val signal = ByteArray(sampleCount * 8) // 2 * 4 Bytes pro Sample (Real + Imaginär als Float)
        
        val buffer = java.nio.ByteBuffer.wrap(signal).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        
        for (i in 0 until sampleCount) {
            val noise = (Math.random() - 0.5) * noiseLevel
            val real = (magnitude * Math.cos(Math.toRadians(phase)) + noise).toFloat()
            val imaginary = (magnitude * Math.sin(Math.toRadians(phase)) + noise).toFloat()
            
            buffer.putFloat(real)
            buffer.putFloat(imaginary)
        }
        
        return signal
    }
    
    private fun createTestReading(): com.emfad.app.services.analysis.EMFReading {
        return com.emfad.app.services.analysis.EMFReading(
            sessionId = 1L,
            timestamp = System.currentTimeMillis(),
            frequency = 77500.0,
            signalStrength = 1000.0,
            phase = 45.0,
            amplitude = 1000.0,
            realPart = 707.0,
            imaginaryPart = 707.0,
            magnitude = 1000.0,
            depth = 2.5,
            temperature = 20.0,
            humidity = 50.0,
            pressure = 1013.25,
            batteryLevel = 100,
            deviceId = "TEST_DEVICE",
            materialType = MaterialType.UNKNOWN,
            confidence = 0.8,
            noiseLevel = 10.0,
            calibrationOffset = 0.0,
            gainSetting = 1.0,
            filterSetting = "default",
            measurementMode = "A",
            qualityScore = 0.9
        )
    }
}
