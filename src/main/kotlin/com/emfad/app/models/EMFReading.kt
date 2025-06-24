package com.emfad.app.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * EMF-Messung (rekonstruiert aus echten EMFAD-Dateiformaten)
 * Basiert auf .EGD und .ESD Dateianalyse der originalen Windows-Programme
 * 
 * Datenstruktur extrahiert aus:
 * - DefaultData.EGD: Hauptdatenformat mit GPS
 * - DefaultData.ESD: Survey-Daten mit Profilen
 * - EMUNI-X-07.ini: Konfiguration und Frequenzen
 * - CalXY/CalXZ.cal: Kalibrierungsdaten
 */
@Parcelize
data class EMFReading(
    val sessionId: Long,
    val timestamp: Long,
    
    // Frequenzdaten (aus EMFAD-INI: f0-f6)
    val frequency: Double, // Hauptfrequenz in Hz (19000, 23400, 70000, 77500, 124000, 129100, 135600)
    val signalStrength: Double, // A-Kanal Wert (Real-Teil)
    val phase: Double, // Berechnet aus A/B Kanälen: atan2(B, A)
    val amplitude: Double, // Magnitude: sqrt(A² + B²)
    val realPart: Double, // A-Kanal (Real-Teil)
    val imaginaryPart: Double, // B-Kanal (Imaginär-Teil)
    val magnitude: Double, // sqrt(A² + B²)
    
    // EMFAD-spezifische Berechnungen (aus EXE-Analyse)
    val depth: Double, // Tiefe basierend auf Kalibrierungskonstante 3333 und Dämpfungsfaktor 0.417
    val temperature: Double, // Temperatur für Kompensation
    val humidity: Double, // Luftfeuchtigkeit
    val pressure: Double, // Luftdruck
    val batteryLevel: Int, // Batteriestand
    val deviceId: String, // "EMFAD-UG", "EMFAD-UGH", "EMFAD-TABLET"
    
    // Material-Klassifikation (aus AI-Analyse)
    val materialType: MaterialType,
    val confidence: Double,
    val noiseLevel: Double,
    
    // Konfiguration (aus .EGD/.ESD Header)
    val calibrationOffset: Double, // offset aus Header (7 Werte für 7 Frequenzen)
    val gainSetting: Double, // gain aus Header (7 Werte für 7 Frequenzen)
    val filterSetting: String, // Filter-Einstellung
    val measurementMode: String, // ampmode: "A" oder "B"
    val qualityScore: Double = 1.0, // Qualitätsbewertung der Messung
    
    // Koordinaten und GPS (aus echten .EGD Dateien)
    val xCoordinate: Double = 0.0, // X-Koordinate
    val yCoordinate: Double = 0.0, // Y-Koordinate  
    val zCoordinate: Double = 0.0, // Z-Koordinate
    val gpsData: String = "", // NMEA GPGGA Format: $GPGGA,141446.00,5157.41889,N,01028.55510,E,1,06,1.46,180.5,M,45.9,M,,*5A
    
    // Erweiterte EMFAD-Felder (aus Dateianalyse)
    val orientation: String = "vertical", // aus Header: "vertical" oder "horizontal"
    val profileNumber: Int = 0, // für ESD-Dateien: Profilnummer zwischen "start of field" und "end of profile"
    val frequencyIndex: Int = 0, // Index in EMFAD_FREQUENCIES (0-6)
    val aux1: String = "", // AUX1 aus ESD-Header
    val aux2: String = "", // AUX2 aus ESD-Header
    val comment: String = "", // Kommentar aus Header
    
    // Multi-Frequenz Daten (alle 7 Frequenzen aus .EGD/.ESD)
    val allFrequencyData: List<Pair<Double, Double>> = emptyList(), // A/B Werte für alle 7 Frequenzen
    val activeFrequencies: List<Boolean> = emptyList(), // Welche Frequenzen aktiv sind (aus ESD: "active Frequencies")
    
    // Zusätzliche Metadaten
    val version: String = "EMFAD TABLET 1.0", // Version aus ESD-Header
    val profileMode: String = "parallel", // Profile-Mode aus ESD
    val profileDimensions: Pair<Double, Double> = Pair(0.0, 0.0), // Profil-Länge und -Abstand
    val usedFrequency: Int = 2, // "Used frequency" aus ESD-Header
    
    // Rohdaten für Debugging
    val rawDataLine: String = "" // Original-Datenzeile aus .EGD/.ESD für Debugging
) : Parcelable {
    
    /**
     * Berechnet die kalibrierte Signalstärke mit EMFAD-Algorithmus
     * Basiert auf Kalibrierungskonstante 3333 aus EMFAD3.exe
     */
    fun getCalibratedSignalStrength(): Double {
        val calibrationConstant = 3333.0
        val tempCoeff = 0.002 // 0.2% pro Grad (aus EMUNI-X-07.exe)
        val referenceTemp = 25.0
        
        val calibrationFactor = calibrationConstant / 1000.0
        val tempCompensation = 1.0 + (temperature - referenceTemp) * tempCoeff
        
        return signalStrength * calibrationFactor * tempCompensation
    }
    
    /**
     * Berechnet die Tiefe mit EMFAD-Algorithmus
     * Basiert auf logarithmischer Funktion mit Dämpfungsfaktor 0.417
     */
    fun calculateEMFADDepth(): Double {
        val calibratedSignal = getCalibratedSignalStrength()
        val attenuationFactor = 0.417 // aus EMFAD3.exe
        
        return if (calibratedSignal > 0) {
            -kotlin.math.ln(calibratedSignal / 1000.0) / attenuationFactor
        } else {
            0.0
        }
    }
    
    /**
     * Konvertiert GPS-Daten zu Dezimalgrad
     * Parst NMEA GPGGA Format aus echten .EGD Dateien
     */
    fun getGPSCoordinates(): Pair<Double, Double> {
        if (!gpsData.startsWith("\$GPGGA")) return Pair(0.0, 0.0)
        
        return try {
            val parts = gpsData.split(",")
            if (parts.size >= 6) {
                val latStr = parts[2]
                val latDir = parts[3]
                val lonStr = parts[4]
                val lonDir = parts[5]
                
                val lat = convertNMEAToDecimal(latStr, latDir)
                val lon = convertNMEAToDecimal(lonStr, lonDir)
                
                Pair(lat, lon)
            } else {
                Pair(0.0, 0.0)
            }
        } catch (e: Exception) {
            Pair(0.0, 0.0)
        }
    }
    
    /**
     * Konvertiert NMEA-Koordinaten zu Dezimalgrad
     */
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
    
    /**
     * Gibt die Frequenz als formatierten String zurück
     */
    fun getFrequencyString(): String {
        return when {
            frequency >= 1000000 -> "${frequency / 1000000} MHz"
            frequency >= 1000 -> "${frequency / 1000} KHz"
            else -> "${frequency} Hz"
        }
    }
    
    /**
     * Prüft ob die Messung gültig ist
     */
    fun isValid(): Boolean {
        return signalStrength >= 0.0 &&
               signalStrength <= 100000.0 && // Maximaler Signalwert
               frequency > 0.0 &&
               temperature >= -40.0 &&
               temperature <= 85.0 &&
               qualityScore >= 0.0 &&
               qualityScore <= 1.0
    }
    
    /**
     * Gibt eine Zusammenfassung der Messung zurück
     */
    fun getSummary(): String {
        return "Freq: ${getFrequencyString()}, Signal: ${String.format("%.1f", signalStrength)}, " +
               "Tiefe: ${String.format("%.2f", depth)}m, Material: $materialType"
    }
    
    /**
     * Erstellt eine Kopie mit aktualisierten Werten
     */
    fun withUpdatedAnalysis(materialType: MaterialType, confidence: Double, depth: Double): EMFReading {
        return copy(
            materialType = materialType,
            confidence = confidence,
            depth = depth
        )
    }
    
    companion object {
        /**
         * EMFAD-Frequenzen aus EMUNI-X-07.ini
         */
        val EMFAD_FREQUENCIES = doubleArrayOf(
            19000.0,  // f0
            23400.0,  // f1
            70000.0,  // f2
            77500.0,  // f3
            124000.0, // f4
            129100.0, // f5
            135600.0  // f6
        )
        
        /**
         * Standard-Verstärkungen aus EMUNI-X-07.ini
         */
        val DEFAULT_GAINS = doubleArrayOf(
            1.0,   // gain0
            1.0,   // gain1
            26.2,  // gain2 (aus echten Daten)
            1.0,   // gain3
            1.0,   // gain4
            1.0,   // gain5
            1.0    // gain6
        )
        
        /**
         * Standard-Offsets aus EMUNI-X-07.ini
         */
        val DEFAULT_OFFSETS = doubleArrayOf(
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        )
        
        /**
         * Erstellt eine Test-Messung
         */
        fun createTestReading(sessionId: Long = System.currentTimeMillis()): EMFReading {
            return EMFReading(
                sessionId = sessionId,
                timestamp = System.currentTimeMillis(),
                frequency = EMFAD_FREQUENCIES[0], // 19 KHz
                signalStrength = 2500.0,
                phase = 45.0,
                amplitude = 2500.0,
                realPart = 2500.0,
                imaginaryPart = 1500.0,
                magnitude = 2915.0,
                depth = 2.5,
                temperature = 25.0,
                humidity = 50.0,
                pressure = 1013.25,
                batteryLevel = 85,
                deviceId = "EMFAD-TABLET",
                materialType = MaterialType.IRON,
                confidence = 0.85,
                noiseLevel = 50.0,
                calibrationOffset = 0.0,
                gainSetting = 1.0,
                filterSetting = "default",
                measurementMode = "A",
                qualityScore = 0.9,
                orientation = "vertical",
                version = "EMFAD TABLET 1.0"
            )
        }
    }
}
