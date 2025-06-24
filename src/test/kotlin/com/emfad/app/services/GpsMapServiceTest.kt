package com.emfad.app.services

import android.content.Context
import com.emfad.app.services.analysis.EMFReading
import com.emfad.app.services.analysis.MaterialType
import com.emfad.app.services.gps.GPSCoordinate
import com.emfad.app.services.gps.GpsMapService
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit Tests für GpsMapService
 * Testet GPS-Funktionalität und Messpunkt-Management
 */

@ExperimentalCoroutinesApi
class GpsMapServiceTest {
    
    @MockK
    private lateinit var context: Context
    
    private lateinit var gpsMapService: GpsMapService
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        // Context Mock konfigurieren
        every { context.getExternalFilesDir(any()) } returns mockk(relaxed = true)
        every { context.packageName } returns "com.emfad.app"
        
        gpsMapService = GpsMapService(context)
    }
    
    @Test
    fun `test initial state`() = runTest {
        // Initial state prüfen
        assertNull(gpsMapService.currentLocation.first())
        assertFalse(gpsMapService.isLocationEnabled.first())
        assertFalse(gpsMapService.isTracking.first())
        assertTrue(gpsMapService.measurementPoints.first().isEmpty())
        assertNull(gpsMapService.currentPath.first())
    }
    
    @Test
    fun `test addMeasurementPoint without GPS`() = runTest {
        // Test ohne GPS-Position
        val emfReading = createTestEMFReading()
        
        gpsMapService.addMeasurementPoint(emfReading)
        
        // Sollte keinen Punkt hinzufügen ohne GPS
        assertTrue(gpsMapService.measurementPoints.first().isEmpty())
    }
    
    @Test
    fun `test calculateDistance`() {
        val point1 = GPSCoordinate(
            latitude = 52.5200,
            longitude = 13.4050,
            altitude = 0.0,
            accuracy = 5.0f,
            timestamp = System.currentTimeMillis()
        )
        
        val point2 = GPSCoordinate(
            latitude = 52.5201,
            longitude = 13.4051,
            altitude = 0.0,
            accuracy = 5.0f,
            timestamp = System.currentTimeMillis()
        )
        
        val distance = gpsMapService.calculateDistance(point1, point2)
        
        // Entfernung sollte > 0 und < 100m sein (sehr nah beieinander)
        assertTrue("Distance should be > 0", distance > 0)
        assertTrue("Distance should be < 100m", distance < 100)
    }
    
    @Test
    fun `test startNewMeasurementSession`() = runTest {
        gpsMapService.startNewMeasurementSession()
        
        // Nach neuer Session sollten alle Daten zurückgesetzt sein
        assertTrue(gpsMapService.measurementPoints.first().isEmpty())
        assertNull(gpsMapService.currentPath.first())
    }
    
    @Test
    fun `test exportCurrentPath empty`() {
        val exportResult = gpsMapService.exportCurrentPath()
        
        // Ohne Pfad sollte null zurückgegeben werden
        assertNull(exportResult)
    }
    
    private fun createTestEMFReading(): EMFReading {
        return EMFReading(
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
            materialType = MaterialType.SOIL_DRY,
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
