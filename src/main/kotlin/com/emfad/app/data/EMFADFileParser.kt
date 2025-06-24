package com.emfad.app.data

import android.util.Log
import com.emfad.app.models.EMFReading
import com.emfad.app.models.MaterialType
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

/**
 * EMFAD File Parser
 * Rekonstruiert aus echten EMFAD-Dateiformaten:
 * - .EGD (EMFAD Geophysical Data) - Hauptdatenformat
 * - .ESD (EMFAD Survey Data) - Vermessungsdaten
 * - .FADS (EMFAD Analysis Data Settings) - Analyseeinstellungen
 * - .CAL (Calibration) - Kalibrierungsdaten
 * - .INI (Configuration) - Konfigurationsdaten
 * 
 * Basiert auf reverse engineering der originalen Windows-Programme
 */
object EMFADFileParser {
    
    private const val TAG = "EMFADFileParser"
    
    // Dateiformate (aus echten Dateien extrahiert)
    data class EGDHeader(
        val version: String,
        val comment: String,
        val timestamp: String,
        val ampMode: String,
        val orientation: String,
        val aux: String,
        val offsets: List<Double>,
        val gains: List<Double>,
        val frequencyNumber: Int,
        val frequencies: List<String>
    )
    
    data class ESDHeader(
        val version: String,
        val dateTime: String,
        val frequencies: List<Double>,
        val activeFrequencies: List<Boolean>,
        val offsets: List<Double>,
        val gains: List<Double>,
        val usedFrequency: Int,
        val orientation: String,
        val ampMode: String,
        val profileMode: String,
        val profileDimensions: Pair<Double, Double>,
        val gpsCoordinates: String,
        val aux1: String,
        val aux2: String,
        val comment: String
    )
    
    data class FADSSettings(
        val threshold1: Double,
        val threshold2: Double,
        val flag1: Int,
        val minValue: Double,
        val maxValue: Double,
        val flag2: Int,
        val parameter1: Int,
        val parameter2: Int
    )
    
    data class CalibrationData(
        val timestamp: String,
        val points: List<Triple<Double, Double, Double>> // X, Y, Z coordinates
    )
    
    data class EMFADConfiguration(
        val compassSettings: Map<String, Any>,
        val offsetSettings: Map<String, Double>,
        val gainSettings: Map<String, Double>,
        val modeSettings: Map<String, Any>,
        val frequencySettings: Map<String, Any>,
        val comPortSettings: Map<String, Int>
    )
    
    /**
     * EGD-Datei parsen (Hauptdatenformat)
     */
    fun parseEGDFile(file: File): Pair<EGDHeader, List<EMFReading>>? {
        return try {
            Log.d(TAG, "Parse EGD-Datei: ${file.name}")
            
            val reader = BufferedReader(FileReader(file))
            val lines = reader.readLines()
            reader.close()
            
            // Header parsen
            val header = parseEGDHeader(lines)
            
            // Daten parsen
            val readings = parseEGDData(lines, header)
            
            Log.d(TAG, "EGD-Datei erfolgreich geparst: ${readings.size} Messungen")
            Pair(header, readings)
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Parsen der EGD-Datei", e)
            null
        }
    }
    
    /**
     * EGD-Header parsen
     */
    private fun parseEGDHeader(lines: List<String>): EGDHeader {
        val headerMap = mutableMapOf<String, String>()
        
        for (line in lines) {
            if (line.startsWith("datastart;")) break
            
            val parts = line.split(";")
            if (parts.size >= 2) {
                headerMap[parts[0]] = parts.drop(1).joinToString(";")
            }
        }
        
        return EGDHeader(
            version = headerMap["version"] ?: "V 5.0",
            comment = headerMap["comment"] ?: "",
            timestamp = lines.getOrNull(2) ?: "",
            ampMode = headerMap["ampmode"] ?: "A",
            orientation = headerMap["orientation"] ?: "vertical",
            aux = headerMap["aux"] ?: "",
            offsets = parseDoubleList(headerMap["offset"] ?: ""),
            gains = parseDoubleList(headerMap["gain"] ?: ""),
            frequencyNumber = headerMap["frq-number"]?.toIntOrNull() ?: 0,
            frequencies = parseFrequencyHeader(lines)
        )
    }
    
    /**
     * EGD-Daten parsen
     */
    private fun parseEGDData(lines: List<String>, header: EGDHeader): List<EMFReading> {
        val readings = mutableListOf<EMFReading>()
        var dataStarted = false
        var sessionId = System.currentTimeMillis()
        
        for (line in lines) {
            if (line.startsWith("datastart;")) {
                dataStarted = true
                continue
            }
            
            if (!dataStarted || line.trim().isEmpty()) continue
            
            val reading = parseEGDDataLine(line, header, sessionId)
            reading?.let { readings.add(it) }
        }
        
        return readings
    }
    
    /**
     * EGD-Datenzeile parsen
     */
    private fun parseEGDDataLine(line: String, header: EGDHeader, sessionId: Long): EMFReading? {
        return try {
            val parts = line.split(";")
            if (parts.size < 3) return null
            
            // Datum und Zeit parsen
            val dateStr = parts[0]
            val timeStr = parts[1]
            val timestamp = parseDateTime(dateStr, timeStr)
            
            // Frequenzdaten extrahieren (A/B Kanäle für jede Frequenz)
            val frequencyData = mutableListOf<Pair<Double, Double>>()
            var index = 2
            
            // Basierend auf dem echten Format: A 19,0 KHz;B 19,0 KHz;A 23,4 KHz;B 23,4 KHz;...
            while (index < parts.size - 2) { // -2 für GPS und Koordinaten
                if (index + 1 < parts.size) {
                    val aValue = parts[index].toDoubleOrNull() ?: 0.0
                    val bValue = parts[index + 1].toDoubleOrNull() ?: 0.0
                    frequencyData.add(Pair(aValue, bValue))
                    index += 2
                }
            }
            
            // GPS-Daten extrahieren
            val gpsData = if (parts.size > index) parts[index] else ""
            val coordinates = parseGPSCoordinates(gpsData)
            
            // Hauptfrequenz bestimmen (basierend auf frq-number)
            val mainFrequencyIndex = header.frequencyNumber
            val mainFrequencyData = frequencyData.getOrNull(mainFrequencyIndex) ?: Pair(0.0, 0.0)
            
            // EMFReading erstellen
            EMFReading(
                sessionId = sessionId,
                timestamp = timestamp,
                frequency = extractFrequencyValue(header.frequencies, mainFrequencyIndex),
                signalStrength = mainFrequencyData.first,
                phase = calculatePhase(mainFrequencyData.first, mainFrequencyData.second),
                amplitude = sqrt(mainFrequencyData.first * mainFrequencyData.first + mainFrequencyData.second * mainFrequencyData.second),
                realPart = mainFrequencyData.first,
                imaginaryPart = mainFrequencyData.second,
                magnitude = sqrt(mainFrequencyData.first * mainFrequencyData.first + mainFrequencyData.second * mainFrequencyData.second),
                depth = calculateDepthFromSignal(mainFrequencyData.first),
                temperature = 25.0, // Default, wird später aus anderen Quellen ergänzt
                humidity = 50.0,
                pressure = 1013.25,
                batteryLevel = 100,
                deviceId = "EMFAD-UG",
                materialType = MaterialType.UNKNOWN,
                confidence = 0.0,
                noiseLevel = calculateNoiseLevel(frequencyData),
                calibrationOffset = header.offsets.getOrNull(mainFrequencyIndex) ?: 0.0,
                gainSetting = header.gains.getOrNull(mainFrequencyIndex) ?: 1.0,
                filterSetting = "default",
                measurementMode = header.ampMode,
                qualityScore = calculateQualityScore(frequencyData),
                xCoordinate = coordinates.first,
                yCoordinate = coordinates.second,
                zCoordinate = 0.0,
                gpsData = gpsData
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Parsen der EGD-Datenzeile: $line", e)
            null
        }
    }
    
    /**
     * ESD-Datei parsen (Survey Data)
     */
    fun parseESDFile(file: File): Pair<ESDHeader, List<EMFReading>>? {
        return try {
            Log.d(TAG, "Parse ESD-Datei: ${file.name}")
            
            val reader = BufferedReader(FileReader(file))
            val lines = reader.readLines()
            reader.close()
            
            // Header parsen
            val header = parseESDHeader(lines)
            
            // Daten parsen
            val readings = parseESDData(lines, header)
            
            Log.d(TAG, "ESD-Datei erfolgreich geparst: ${readings.size} Messungen")
            Pair(header, readings)
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Parsen der ESD-Datei", e)
            null
        }
    }
    
    /**
     * ESD-Header parsen
     */
    private fun parseESDHeader(lines: List<String>): ESDHeader {
        val headerMap = mutableMapOf<String, String>()
        
        for (line in lines) {
            if (line.startsWith("start of field;")) break
            
            val parts = line.split(";", limit = 2)
            if (parts.size >= 2) {
                headerMap[parts[0].trim()] = parts[1].trim()
            }
        }
        
        return ESDHeader(
            version = headerMap["Version"] ?: "EMFAD TABLET 1.0",
            dateTime = headerMap["Date Time"] ?: "",
            frequencies = parseFrequencyList(headerMap["Frequencies/KHz"] ?: ""),
            activeFrequencies = parseBooleanList(headerMap["active Frequencies"] ?: ""),
            offsets = parseDoubleList(headerMap["Offset"] ?: ""),
            gains = parseDoubleList(headerMap["Gain"] ?: ""),
            usedFrequency = headerMap["Used frequency"]?.toIntOrNull() ?: 0,
            orientation = headerMap["Orientaion"] ?: "vertical", // Typo im Original
            ampMode = headerMap["Amp-Mode"] ?: "A",
            profileMode = headerMap["Profile-Mode"] ?: "parallel",
            profileDimensions = parseProfileDimensions(headerMap["Dimension profile length, profile distance"] ?: ""),
            gpsCoordinates = headerMap["GPS coordinates"] ?: "",
            aux1 = headerMap["AUX1"] ?: "",
            aux2 = headerMap["AUX2"] ?: "",
            comment = headerMap["Comment"] ?: ""
        )
    }
    
    /**
     * ESD-Daten parsen
     */
    private fun parseESDData(lines: List<String>, header: ESDHeader): List<EMFReading> {
        val readings = mutableListOf<EMFReading>()
        var dataStarted = false
        var sessionId = System.currentTimeMillis()
        var profileNumber = 0
        
        for (line in lines) {
            when {
                line.startsWith("start of field;") -> {
                    dataStarted = true
                    continue
                }
                line.startsWith("end of profile;") -> {
                    profileNumber++
                    continue
                }
                !dataStarted || line.trim().isEmpty() -> continue
            }
            
            val reading = parseESDDataLine(line, header, sessionId, profileNumber)
            reading?.let { readings.add(it) }
        }
        
        return readings
    }
    
    /**
     * ESD-Datenzeile parsen
     */
    private fun parseESDDataLine(line: String, header: ESDHeader, sessionId: Long, profileNumber: Int): EMFReading? {
        return try {
            val parts = line.split(";")
            if (parts.size < 3) return null
            
            // Zeit parsen
            val timeStr = parts[0].trim()
            val timestamp = parseTime(timeStr)
            
            // Frequenzdaten extrahieren
            val frequencyData = mutableListOf<Pair<Double, Double>>()
            var index = 1
            
            while (index < parts.size - 1) {
                if (index + 1 < parts.size) {
                    val aValue = parts[index].trim().toDoubleOrNull() ?: 0.0
                    val bValue = parts[index + 1].trim().toDoubleOrNull() ?: 0.0
                    frequencyData.add(Pair(aValue, bValue))
                    index += 2
                }
            }
            
            // Hauptfrequenz bestimmen
            val mainFrequencyIndex = header.usedFrequency
            val mainFrequencyData = frequencyData.getOrNull(mainFrequencyIndex) ?: Pair(0.0, 0.0)
            val frequency = header.frequencies.getOrNull(mainFrequencyIndex) ?: 19.0
            
            EMFReading(
                sessionId = sessionId,
                timestamp = timestamp,
                frequency = frequency * 1000.0, // KHz zu Hz
                signalStrength = mainFrequencyData.first,
                phase = calculatePhase(mainFrequencyData.first, mainFrequencyData.second),
                amplitude = sqrt(mainFrequencyData.first * mainFrequencyData.first + mainFrequencyData.second * mainFrequencyData.second),
                realPart = mainFrequencyData.first,
                imaginaryPart = mainFrequencyData.second,
                magnitude = sqrt(mainFrequencyData.first * mainFrequencyData.first + mainFrequencyData.second * mainFrequencyData.second),
                depth = calculateDepthFromSignal(mainFrequencyData.first),
                temperature = 25.0,
                humidity = 50.0,
                pressure = 1013.25,
                batteryLevel = 100,
                deviceId = "EMFAD-TABLET",
                materialType = MaterialType.UNKNOWN,
                confidence = 0.0,
                noiseLevel = calculateNoiseLevel(frequencyData),
                calibrationOffset = header.offsets.getOrNull(mainFrequencyIndex) ?: 0.0,
                gainSetting = header.gains.getOrNull(mainFrequencyIndex) ?: 1.0,
                filterSetting = "default",
                measurementMode = header.ampMode,
                qualityScore = calculateQualityScore(frequencyData),
                xCoordinate = 0.0,
                yCoordinate = 0.0,
                zCoordinate = 0.0,
                gpsData = header.gpsCoordinates,
                profileNumber = profileNumber
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Parsen der ESD-Datenzeile: $line", e)
            null
        }
    }
    
    /**
     * FADS-Datei parsen (Analysis Settings)
     */
    fun parseFADSFile(file: File): FADSSettings? {
        return try {
            Log.d(TAG, "Parse FADS-Datei: ${file.name}")
            
            val reader = BufferedReader(FileReader(file))
            val lines = reader.readLines()
            reader.close()
            
            if (lines.size < 4) return null
            
            FADSSettings(
                threshold1 = lines[0].trim().split("\\s+".toRegex())[0].toDouble(),
                threshold2 = lines[0].trim().split("\\s+".toRegex())[1].toDouble(),
                flag1 = lines[0].trim().split("\\s+".toRegex())[2].toInt(),
                minValue = lines[1].trim().split("\\s+".toRegex())[0].toDouble(),
                maxValue = lines[1].trim().split("\\s+".toRegex())[1].toDouble(),
                flag2 = lines[1].trim().split("\\s+".toRegex())[2].toInt(),
                parameter1 = lines[2].trim().toInt(),
                parameter2 = lines[3].trim().toInt()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Parsen der FADS-Datei", e)
            null
        }
    }
    
    /**
     * CAL-Datei parsen (Calibration Data)
     */
    fun parseCALFile(file: File): CalibrationData? {
        return try {
            Log.d(TAG, "Parse CAL-Datei: ${file.name}")
            
            val reader = BufferedReader(FileReader(file))
            val lines = reader.readLines()
            reader.close()
            
            if (lines.isEmpty()) return null
            
            val timestamp = lines[0]
            val points = mutableListOf<Triple<Double, Double, Double>>()
            
            for (i in 1 until lines.size) {
                val line = lines[i].trim()
                if (line.isEmpty()) continue
                
                val parts = line.split(";")
                if (parts.size >= 3) {
                    val x = parts[0].toDoubleOrNull() ?: 0.0
                    val y = parts[1].toDoubleOrNull() ?: 0.0
                    val z = parts[2].toDoubleOrNull() ?: 0.0
                    points.add(Triple(x, y, z))
                }
            }
            
            CalibrationData(timestamp, points)
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Parsen der CAL-Datei", e)
            null
        }
    }
    
    // Hilfsfunktionen
    
    private fun parseDoubleList(str: String): List<Double> {
        return str.split(";").mapNotNull { 
            it.replace(",", ".").trim().toDoubleOrNull() 
        }
    }
    
    private fun parseFrequencyList(str: String): List<Double> {
        return str.split(";").mapNotNull { 
            it.replace(",", ".").trim().toDoubleOrNull() 
        }
    }
    
    private fun parseBooleanList(str: String): List<Boolean> {
        return str.split(";").map { 
            it.trim().uppercase() == "TRUE" 
        }
    }
    
    private fun parseFrequencyHeader(lines: List<String>): List<String> {
        for (line in lines) {
            if (line.startsWith("date;time;")) {
                return line.split(";").drop(2).filter { it.contains("KHz") }
            }
        }
        return emptyList()
    }
    
    private fun parseDateTime(dateStr: String, timeStr: String): Long {
        return try {
            val format = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
            format.parse("$dateStr $timeStr")?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    
    private fun parseTime(timeStr: String): Long {
        return try {
            val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val today = Calendar.getInstance()
            val time = format.parse(timeStr)
            if (time != null) {
                val cal = Calendar.getInstance()
                cal.time = time
                today.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY))
                today.set(Calendar.MINUTE, cal.get(Calendar.MINUTE))
                today.set(Calendar.SECOND, cal.get(Calendar.SECOND))
                today.timeInMillis
            } else {
                System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    
    private fun parseGPSCoordinates(gpsData: String): Pair<Double, Double> {
        return try {
            // NMEA GPGGA Format parsen
            if (gpsData.startsWith("\$GPGGA")) {
                val parts = gpsData.split(",")
                if (parts.size >= 6) {
                    val latStr = parts[2]
                    val latDir = parts[3]
                    val lonStr = parts[4]
                    val lonDir = parts[5]
                    
                    val lat = convertNMEAToDecimal(latStr, latDir)
                    val lon = convertNMEAToDecimal(lonStr, lonDir)
                    
                    return Pair(lat, lon)
                }
            }
            Pair(0.0, 0.0)
        } catch (e: Exception) {
            Pair(0.0, 0.0)
        }
    }
    
    private fun convertNMEAToDecimal(coord: String, direction: String): Double {
        if (coord.length < 4) return 0.0
        
        val degrees = coord.substring(0, coord.length - 7).toDoubleOrNull() ?: 0.0
        val minutes = coord.substring(coord.length - 7).toDoubleOrNull() ?: 0.0
        
        var decimal = degrees + minutes / 60.0
        
        if (direction == "S" || direction == "W") {
            decimal = -decimal
        }
        
        return decimal
    }
    
    private fun parseProfileDimensions(str: String): Pair<Double, Double> {
        val parts = str.split(";")
        return if (parts.size >= 2) {
            val length = parts[0].replace(",", ".").trim().toDoubleOrNull() ?: 0.0
            val distance = parts[1].replace(",", ".").trim().toDoubleOrNull() ?: 0.0
            Pair(length, distance)
        } else {
            Pair(0.0, 0.0)
        }
    }
    
    private fun extractFrequencyValue(frequencies: List<String>, index: Int): Double {
        return try {
            val freqStr = frequencies.getOrNull(index) ?: "19,0 KHz"
            val numStr = freqStr.replace("KHz", "").replace("A", "").replace("B", "").trim()
            numStr.replace(",", ".").toDouble() * 1000.0 // KHz zu Hz
        } catch (e: Exception) {
            19000.0 // Default 19 KHz
        }
    }
    
    private fun calculatePhase(real: Double, imaginary: Double): Double {
        return atan2(imaginary, real) * 180.0 / PI
    }
    
    private fun calculateDepthFromSignal(signal: Double): Double {
        // EMFAD-spezifische Tiefenberechnung
        val calibrationConstant = 3333.0
        val attenuationFactor = 0.417
        val calibratedSignal = signal * (calibrationConstant / 1000.0)
        
        return if (calibratedSignal > 0) {
            -ln(calibratedSignal / 1000.0) / attenuationFactor
        } else {
            0.0
        }
    }
    
    private fun calculateNoiseLevel(frequencyData: List<Pair<Double, Double>>): Double {
        if (frequencyData.isEmpty()) return 0.0
        
        val signals = frequencyData.map { sqrt(it.first * it.first + it.second * it.second) }
        val mean = signals.average()
        val variance = signals.map { (it - mean) * (it - mean) }.average()
        
        return sqrt(variance)
    }
    
    private fun calculateQualityScore(frequencyData: List<Pair<Double, Double>>): Double {
        if (frequencyData.isEmpty()) return 0.0
        
        val signals = frequencyData.map { sqrt(it.first * it.first + it.second * it.second) }
        val maxSignal = signals.maxOrNull() ?: 0.0
        val noiseLevel = calculateNoiseLevel(frequencyData)
        
        return if (noiseLevel > 0) {
            min(1.0, maxSignal / noiseLevel / 100.0)
        } else {
            1.0
        }
    }
}
