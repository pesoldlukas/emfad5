package com.emfad.app.calibration

import android.util.Log
import com.emfad.app.models.EMFReading
import com.emfad.app.data.EMFADFileParser
import java.io.File
import kotlin.math.*

/**
 * EMFAD Calibration System
 * Rekonstruiert aus echten Kalibrierungsdateien:
 * - CalXY_14102024_193844.cal
 * - CalXZ_14102024_193926.cal
 * 
 * Implementiert die Kalibrierungsalgorithmen aus den originalen Windows-Programmen:
 * - Kalibrierungskonstante 3333 (aus EMFAD3.exe)
 * - Autobalance-Algorithmus (aus EMUNI-X-07.exe)
 * - Temperaturkompensation
 * - Multi-Frequenz Kalibrierung
 */
class EMFADCalibration {
    
    companion object {
        private const val TAG = "EMFADCalibration"
        
        // EMFAD-Konstanten (aus EXE-Analyse)
        private const val CALIBRATION_CONSTANT = 3333.0
        private const val ATTENUATION_FACTOR = 0.417
        private const val REFERENCE_TEMPERATURE = 25.0
        private const val TEMP_COEFFICIENT = 0.002 // 0.2% pro Grad
        
        // Kalibrierungstypen
        enum class CalibrationType {
            XY_PLANE,    // CalXY - Horizontale Ebene
            XZ_PLANE,    // CalXZ - Vertikale Ebene
            AUTOBALANCE, // Autobalance aus EMUNI-X-07.exe
            FREQUENCY,   // Frequenz-spezifische Kalibrierung
            TEMPERATURE  // Temperaturkompensation
        }
    }
    
    // Kalibrierungsdaten
    private var xyCalibrationPoints = mutableListOf<Triple<Double, Double, Double>>()
    private var xzCalibrationPoints = mutableListOf<Triple<Double, Double, Double>>()
    private var autobalanceBaseline = mutableListOf<Double>()
    private var frequencyCalibration = mutableMapOf<Double, Double>()
    private var temperatureCalibration = mutableMapOf<Double, Double>()
    
    // Kalibrierungsstatus
    private var isXYCalibrated = false
    private var isXZCalibrated = false
    private var isAutobalanceCalibrated = false
    private var isFrequencyCalibrated = false
    private var isTemperatureCalibrated = false
    
    /**
     * Kalibrierungsdatei laden
     */
    fun loadCalibrationFile(file: File, type: CalibrationType): Boolean {
        return try {
            Log.d(TAG, "Lade Kalibrierungsdatei: ${file.name}, Typ: $type")
            
            when (type) {
                CalibrationType.XY_PLANE -> loadXYCalibration(file)
                CalibrationType.XZ_PLANE -> loadXZCalibration(file)
                else -> {
                    Log.w(TAG, "Kalibrierungstyp $type wird noch nicht unterstützt")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Laden der Kalibrierungsdatei", e)
            false
        }
    }
    
    /**
     * XY-Kalibrierung laden (CalXY_*.cal)
     */
    private fun loadXYCalibration(file: File): Boolean {
        val calibrationData = EMFADFileParser.parseCALFile(file) ?: return false
        
        xyCalibrationPoints.clear()
        xyCalibrationPoints.addAll(calibrationData.points)
        
        isXYCalibrated = xyCalibrationPoints.isNotEmpty()
        
        Log.d(TAG, "XY-Kalibrierung geladen: ${xyCalibrationPoints.size} Punkte")
        return isXYCalibrated
    }
    
    /**
     * XZ-Kalibrierung laden (CalXZ_*.cal)
     */
    private fun loadXZCalibration(file: File): Boolean {
        val calibrationData = EMFADFileParser.parseCALFile(file) ?: return false
        
        xzCalibrationPoints.clear()
        xzCalibrationPoints.addAll(calibrationData.points)
        
        isXZCalibrated = xzCalibrationPoints.isNotEmpty()
        
        Log.d(TAG, "XZ-Kalibrierung geladen: ${xzCalibrationPoints.size} Punkte")
        return isXZCalibrated
    }
    
    /**
     * Autobalance-Kalibrierung durchführen
     * Implementiert "autobalance values; version 1.0" aus EMUNI-X-07.exe
     */
    fun performAutobalanceCalibration(readings: List<EMFReading>): Boolean {
        return try {
            if (readings.size < 10) {
                Log.w(TAG, "Nicht genügend Messungen für Autobalance-Kalibrierung")
                return false
            }
            
            Log.d(TAG, "Führe Autobalance-Kalibrierung durch mit ${readings.size} Messungen")
            
            // Baseline aus ersten 10 Messungen berechnen
            autobalanceBaseline.clear()
            readings.take(10).forEach { reading ->
                autobalanceBaseline.add(reading.signalStrength)
            }
            
            isAutobalanceCalibrated = autobalanceBaseline.isNotEmpty()
            
            Log.d(TAG, "Autobalance-Kalibrierung abgeschlossen: Baseline = ${autobalanceBaseline.average()}")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei Autobalance-Kalibrierung", e)
            false
        }
    }
    
    /**
     * Frequenz-spezifische Kalibrierung
     */
    fun performFrequencyCalibration(frequencyReadings: Map<Double, List<EMFReading>>): Boolean {
        return try {
            Log.d(TAG, "Führe Frequenz-Kalibrierung durch für ${frequencyReadings.size} Frequenzen")
            
            frequencyCalibration.clear()
            
            for ((frequency, readings) in frequencyReadings) {
                if (readings.isNotEmpty()) {
                    val averageSignal = readings.map { it.signalStrength }.average()
                    val calibrationFactor = CALIBRATION_CONSTANT / averageSignal
                    frequencyCalibration[frequency] = calibrationFactor
                    
                    Log.d(TAG, "Frequenz ${frequency}Hz: Kalibrierungsfaktor = $calibrationFactor")
                }
            }
            
            isFrequencyCalibrated = frequencyCalibration.isNotEmpty()
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei Frequenz-Kalibrierung", e)
            false
        }
    }
    
    /**
     * Temperaturkalibrierung durchführen
     */
    fun performTemperatureCalibration(temperatureReadings: Map<Double, List<EMFReading>>): Boolean {
        return try {
            Log.d(TAG, "Führe Temperatur-Kalibrierung durch für ${temperatureReadings.size} Temperaturen")
            
            temperatureCalibration.clear()
            
            for ((temperature, readings) in temperatureReadings) {
                if (readings.isNotEmpty()) {
                    val averageSignal = readings.map { it.signalStrength }.average()
                    val tempCompensation = 1.0 + (temperature - REFERENCE_TEMPERATURE) * TEMP_COEFFICIENT
                    temperatureCalibration[temperature] = tempCompensation
                    
                    Log.d(TAG, "Temperatur ${temperature}°C: Kompensationsfaktor = $tempCompensation")
                }
            }
            
            isTemperatureCalibrated = temperatureCalibration.isNotEmpty()
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei Temperatur-Kalibrierung", e)
            false
        }
    }
    
    /**
     * Messung kalibrieren (Hauptfunktion)
     */
    fun calibrateReading(reading: EMFReading): EMFReading {
        var calibratedReading = reading
        
        // 1. Autobalance anwenden
        if (isAutobalanceCalibrated) {
            calibratedReading = applyAutobalance(calibratedReading)
        }
        
        // 2. Frequenz-Kalibrierung anwenden
        if (isFrequencyCalibrated) {
            calibratedReading = applyFrequencyCalibration(calibratedReading)
        }
        
        // 3. Temperaturkompensation anwenden
        if (isTemperatureCalibrated) {
            calibratedReading = applyTemperatureCompensation(calibratedReading)
        }
        
        // 4. Koordinaten-Kalibrierung anwenden
        if (isXYCalibrated || isXZCalibrated) {
            calibratedReading = applyCoordinateCalibration(calibratedReading)
        }
        
        // 5. EMFAD-spezifische Kalibrierung anwenden
        calibratedReading = applyEMFADCalibration(calibratedReading)
        
        return calibratedReading
    }
    
    /**
     * Autobalance anwenden
     */
    private fun applyAutobalance(reading: EMFReading): EMFReading {
        if (autobalanceBaseline.isEmpty()) return reading
        
        val baseline = autobalanceBaseline.average()
        val correctedSignal = reading.signalStrength - baseline
        
        return reading.copy(
            signalStrength = maxOf(0.0, correctedSignal),
            calibrationOffset = baseline
        )
    }
    
    /**
     * Frequenz-Kalibrierung anwenden
     */
    private fun applyFrequencyCalibration(reading: EMFReading): EMFReading {
        val calibrationFactor = frequencyCalibration[reading.frequency] ?: 1.0
        
        return reading.copy(
            signalStrength = reading.signalStrength * calibrationFactor,
            gainSetting = calibrationFactor
        )
    }
    
    /**
     * Temperaturkompensation anwenden
     */
    private fun applyTemperatureCompensation(reading: EMFReading): EMFReading {
        val tempCompensation = calculateTemperatureCompensation(reading.temperature)
        
        return reading.copy(
            signalStrength = reading.signalStrength * tempCompensation
        )
    }
    
    /**
     * Koordinaten-Kalibrierung anwenden
     */
    private fun applyCoordinateCalibration(reading: EMFReading): EMFReading {
        var calibratedX = reading.xCoordinate
        var calibratedY = reading.yCoordinate
        var calibratedZ = reading.zCoordinate
        
        // XY-Kalibrierung anwenden
        if (isXYCalibrated && xyCalibrationPoints.isNotEmpty()) {
            val xyCorrection = interpolateCalibration(
                reading.xCoordinate, reading.yCoordinate, 
                xyCalibrationPoints, isXYPlane = true
            )
            calibratedX += xyCorrection.first
            calibratedY += xyCorrection.second
        }
        
        // XZ-Kalibrierung anwenden
        if (isXZCalibrated && xzCalibrationPoints.isNotEmpty()) {
            val xzCorrection = interpolateCalibration(
                reading.xCoordinate, reading.zCoordinate,
                xzCalibrationPoints, isXYPlane = false
            )
            calibratedX += xzCorrection.first
            calibratedZ += xzCorrection.second
        }
        
        return reading.copy(
            xCoordinate = calibratedX,
            yCoordinate = calibratedY,
            zCoordinate = calibratedZ
        )
    }
    
    /**
     * EMFAD-spezifische Kalibrierung anwenden
     */
    private fun applyEMFADCalibration(reading: EMFReading): EMFReading {
        // Kalibrierungskonstante 3333 anwenden
        val calibrationFactor = CALIBRATION_CONSTANT / 1000.0
        val tempCompensation = calculateTemperatureCompensation(reading.temperature)
        
        val calibratedSignal = reading.signalStrength * calibrationFactor * tempCompensation
        
        // Tiefe neu berechnen
        val calibratedDepth = if (calibratedSignal > 0) {
            -ln(calibratedSignal / 1000.0) / ATTENUATION_FACTOR
        } else {
            0.0
        }
        
        return reading.copy(
            signalStrength = calibratedSignal,
            depth = calibratedDepth
        )
    }
    
    /**
     * Temperaturkompensation berechnen
     */
    private fun calculateTemperatureCompensation(temperature: Double): Double {
        return 1.0 + (temperature - REFERENCE_TEMPERATURE) * TEMP_COEFFICIENT
    }
    
    /**
     * Kalibrierungs-Interpolation
     */
    private fun interpolateCalibration(
        x: Double, y: Double,
        calibrationPoints: List<Triple<Double, Double, Double>>,
        isXYPlane: Boolean
    ): Pair<Double, Double> {
        
        if (calibrationPoints.size < 3) return Pair(0.0, 0.0)
        
        // Nächste Kalibrierungspunkte finden
        val nearestPoints = calibrationPoints
            .map { point ->
                val distance = if (isXYPlane) {
                    sqrt((x - point.first).pow(2) + (y - point.second).pow(2))
                } else {
                    sqrt((x - point.first).pow(2) + (y - point.third).pow(2))
                }
                Pair(point, distance)
            }
            .sortedBy { it.second }
            .take(3)
        
        // Gewichtete Interpolation
        var totalWeight = 0.0
        var weightedCorrectionX = 0.0
        var weightedCorrectionY = 0.0
        
        for ((point, distance) in nearestPoints) {
            val weight = if (distance > 0) 1.0 / distance else 1.0
            totalWeight += weight
            
            if (isXYPlane) {
                weightedCorrectionX += weight * (point.first - x)
                weightedCorrectionY += weight * (point.second - y)
            } else {
                weightedCorrectionX += weight * (point.first - x)
                weightedCorrectionY += weight * (point.third - y)
            }
        }
        
        return if (totalWeight > 0) {
            Pair(weightedCorrectionX / totalWeight, weightedCorrectionY / totalWeight)
        } else {
            Pair(0.0, 0.0)
        }
    }
    
    /**
     * Kalibrierungsstatus abrufen
     */
    fun getCalibrationStatus(): Map<String, Boolean> {
        return mapOf(
            "xy_calibrated" to isXYCalibrated,
            "xz_calibrated" to isXZCalibrated,
            "autobalance_calibrated" to isAutobalanceCalibrated,
            "frequency_calibrated" to isFrequencyCalibrated,
            "temperature_calibrated" to isTemperatureCalibrated
        )
    }
    
    /**
     * Kalibrierung zurücksetzen
     */
    fun resetCalibration(type: CalibrationType? = null) {
        when (type) {
            CalibrationType.XY_PLANE -> {
                xyCalibrationPoints.clear()
                isXYCalibrated = false
            }
            CalibrationType.XZ_PLANE -> {
                xzCalibrationPoints.clear()
                isXZCalibrated = false
            }
            CalibrationType.AUTOBALANCE -> {
                autobalanceBaseline.clear()
                isAutobalanceCalibrated = false
            }
            CalibrationType.FREQUENCY -> {
                frequencyCalibration.clear()
                isFrequencyCalibrated = false
            }
            CalibrationType.TEMPERATURE -> {
                temperatureCalibration.clear()
                isTemperatureCalibrated = false
            }
            null -> {
                // Alle Kalibrierungen zurücksetzen
                xyCalibrationPoints.clear()
                xzCalibrationPoints.clear()
                autobalanceBaseline.clear()
                frequencyCalibration.clear()
                temperatureCalibration.clear()
                
                isXYCalibrated = false
                isXZCalibrated = false
                isAutobalanceCalibrated = false
                isFrequencyCalibrated = false
                isTemperatureCalibrated = false
            }
        }
        
        Log.d(TAG, "Kalibrierung zurückgesetzt: $type")
    }
    
    /**
     * Kalibrierungsdaten exportieren
     */
    fun exportCalibrationData(): Map<String, Any> {
        return mapOf(
            "xy_points" to xyCalibrationPoints,
            "xz_points" to xzCalibrationPoints,
            "autobalance_baseline" to autobalanceBaseline,
            "frequency_calibration" to frequencyCalibration,
            "temperature_calibration" to temperatureCalibration,
            "calibration_status" to getCalibrationStatus(),
            "calibration_constant" to CALIBRATION_CONSTANT,
            "attenuation_factor" to ATTENUATION_FACTOR,
            "reference_temperature" to REFERENCE_TEMPERATURE,
            "temp_coefficient" to TEMP_COEFFICIENT
        )
    }
}
