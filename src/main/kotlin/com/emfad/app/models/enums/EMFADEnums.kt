package com.emfad.app.models.enums

/**
 * EMFAD Enums
 * Alle Enumerationen für Samsung S21 Ultra optimiert
 */

// Material Types - Erweiterte Material-Klassifikation
enum class MaterialType(val displayName: String, val density: Double, val conductivity: Double) {
    // Metalle
    IRON("Eisen", 7874.0, 1.0e7),
    STEEL("Stahl", 7850.0, 1.4e7),
    ALUMINUM("Aluminium", 2700.0, 3.5e7),
    COPPER("Kupfer", 8960.0, 5.96e7),
    BRASS("Messing", 8500.0, 1.5e7),
    BRONZE("Bronze", 8800.0, 1.0e7),
    GOLD("Gold", 19300.0, 4.1e7),
    SILVER("Silber", 10490.0, 6.1e7),
    PLATINUM("Platin", 21450.0, 9.4e6),
    TITANIUM("Titan", 4506.0, 2.4e6),
    NICKEL("Nickel", 8908.0, 1.4e7),
    ZINC("Zink", 7134.0, 1.7e7),
    TIN("Zinn", 7287.0, 9.1e6),
    LEAD("Blei", 11340.0, 4.8e6),
    
    // Legierungen
    STAINLESS_STEEL("Edelstahl", 8000.0, 1.4e6),
    CARBON_STEEL("Kohlenstoffstahl", 7850.0, 6.0e6),
    CAST_IRON("Gusseisen", 7200.0, 1.0e7),
    
    // Nicht-Metalle
    CERAMIC("Keramik", 3900.0, 1e-12),
    GLASS("Glas", 2500.0, 1e-15),
    PLASTIC("Kunststoff", 1200.0, 1e-16),
    RUBBER("Gummi", 1500.0, 1e-16),
    WOOD("Holz", 600.0, 1e-16),
    CONCRETE("Beton", 2400.0, 1e-9),
    
    // Spezielle Materialien
    GRAPHITE("Graphit", 2200.0, 1.0e5),
    CARBON_FIBER("Kohlefaser", 1600.0, 1.0e4),
    COMPOSITE("Verbundwerkstoff", 1800.0, 1e-10),
    
    // Unbekannt/Gemischt
    UNKNOWN("Unbekannt", 0.0, 0.0),
    MIXED("Gemischt", 0.0, 0.0),
    AIR("Luft", 1.225, 0.0);
    
    fun isConductive(): Boolean = conductivity > 1e3
    fun isMagnetic(): Boolean = this in listOf(IRON, STEEL, NICKEL, CAST_IRON, CARBON_STEEL)
    fun isNonMetal(): Boolean = this in listOf(CERAMIC, GLASS, PLASTIC, RUBBER, WOOD, CONCRETE, AIR)
}

// Session Status
enum class SessionStatus(val displayName: String) {
    ACTIVE("Aktiv"),
    COMPLETED("Abgeschlossen"),
    PAUSED("Pausiert"),
    ERROR("Fehler"),
    CANCELLED("Abgebrochen"),
    ARCHIVED("Archiviert");
    
    fun isActive(): Boolean = this == ACTIVE
    fun canResume(): Boolean = this == PAUSED
    fun isFinished(): Boolean = this in listOf(COMPLETED, CANCELLED, ARCHIVED)
}

// Connection State
enum class ConnectionState(val displayName: String) {
    DISCONNECTED("Getrennt"),
    CONNECTING("Verbinde..."),
    CONNECTED("Verbunden"),
    RECONNECTING("Wiederverbindung..."),
    ERROR("Verbindungsfehler"),
    TIMEOUT("Zeitüberschreitung");
    
    fun isConnected(): Boolean = this == CONNECTED
    fun isConnecting(): Boolean = this in listOf(CONNECTING, RECONNECTING)
    fun hasError(): Boolean = this in listOf(ERROR, TIMEOUT)
}

// Measurement Mode
enum class MeasurementMode(val displayName: String, val description: String) {
    SINGLE_POINT("Einzelpunkt", "Einzelne Messung an einem Punkt"),
    CONTINUOUS("Kontinuierlich", "Fortlaufende Messungen"),
    GRID_SCAN("Raster-Scan", "Systematische Raster-Abtastung"),
    LINE_SCAN("Linien-Scan", "Lineare Abtastung"),
    AREA_SCAN("Flächen-Scan", "Flächenhafte Abtastung"),
    DEPTH_PROFILE("Tiefenprofil", "Tiefenabhängige Messungen"),
    FREQUENCY_SWEEP("Frequenz-Sweep", "Frequenzabhängige Messungen"),
    CALIBRATION("Kalibrierung", "Kalibrierungsmessungen");
    
    fun requiresMovement(): Boolean = this in listOf(GRID_SCAN, LINE_SCAN, AREA_SCAN)
    fun isCalibration(): Boolean = this == CALIBRATION
}

// Data Quality Level
enum class DataQuality(val displayName: String, val threshold: Double) {
    EXCELLENT("Ausgezeichnet", 0.95),
    GOOD("Gut", 0.80),
    FAIR("Ausreichend", 0.60),
    POOR("Schlecht", 0.40),
    INVALID("Ungültig", 0.0);
    
    companion object {
        fun fromScore(score: Double): DataQuality {
            return values().first { score >= it.threshold }
        }
    }
}

// Export Format
enum class ExportFormat(val displayName: String, val fileExtension: String, val mimeType: String) {
    CSV("CSV (Comma Separated Values)", "csv", "text/csv"),
    JSON("JSON Data", "json", "application/json"),
    XML("XML Data", "xml", "application/xml"),
    PDF("PDF Report", "pdf", "application/pdf"),
    MATLAB("MATLAB Script", "m", "text/plain"),
    EXCEL("Excel Spreadsheet", "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    HDF5("HDF5 Scientific Data", "h5", "application/x-hdf"),
    BINARY("Binary Data", "bin", "application/octet-stream");
    
    fun isTextFormat(): Boolean = this in listOf(CSV, JSON, XML, MATLAB)
    fun isBinaryFormat(): Boolean = this in listOf(PDF, EXCEL, HDF5, BINARY)
    fun supportsCompression(): Boolean = this in listOf(JSON, XML, HDF5, BINARY)
}

// Visualization Mode für AR
enum class VisualizationMode(val displayName: String, val description: String) {
    SIGNAL_STRENGTH("Signalstärke", "Farbkodierung nach Signalstärke"),
    MATERIAL_TYPE("Material-Typ", "Farbkodierung nach Material"),
    DEPTH("Tiefe", "Farbkodierung nach Tiefe"),
    FREQUENCY("Frequenz", "Farbkodierung nach Frequenz"),
    PHASE("Phase", "Farbkodierung nach Phase"),
    TEMPERATURE("Temperatur", "Farbkodierung nach Temperatur"),
    QUALITY("Qualität", "Farbkodierung nach Datenqualität"),
    CONFIDENCE("Konfidenz", "Farbkodierung nach AI-Konfidenz"),
    COMBINED("Kombiniert", "Mehrere Parameter kombiniert"),
    HEATMAP("Heatmap", "Kontinuierliche Heatmap-Darstellung"),
    CONTOUR("Konturlinien", "Isolinien-Darstellung"),
    VOLUME("Volumen", "3D-Volumen-Rendering");
    
    fun is3D(): Boolean = this in listOf(VOLUME, COMBINED)
    fun supportsAnimation(): Boolean = this in listOf(HEATMAP, VOLUME, COMBINED)
}

// Filter Type
enum class FilterType(val displayName: String, val description: String) {
    NONE("Kein Filter", "Keine Filterung"),
    LOW_PASS("Tiefpass", "Entfernt hohe Frequenzen"),
    HIGH_PASS("Hochpass", "Entfernt niedrige Frequenzen"),
    BAND_PASS("Bandpass", "Lässt nur bestimmten Frequenzbereich durch"),
    BAND_STOP("Bandsperre", "Blockiert bestimmten Frequenzbereich"),
    NOTCH("Notch", "Entfernt spezifische Frequenz"),
    ADAPTIVE("Adaptiv", "Automatische Anpassung"),
    KALMAN("Kalman", "Kalman-Filter für Rauschunterdrückung"),
    MEDIAN("Median", "Median-Filter für Ausreißer"),
    GAUSSIAN("Gauss", "Gauss-Filter für Glättung");
    
    fun isFrequencyFilter(): Boolean = this in listOf(LOW_PASS, HIGH_PASS, BAND_PASS, BAND_STOP, NOTCH)
    fun isStatisticalFilter(): Boolean = this in listOf(MEDIAN, GAUSSIAN, KALMAN)
}

// Calibration Type
enum class CalibrationType(val displayName: String, val description: String) {
    ZERO_OFFSET("Nullpunkt", "Nullpunkt-Kalibrierung"),
    GAIN("Verstärkung", "Verstärkungs-Kalibrierung"),
    PHASE("Phase", "Phasen-Kalibrierung"),
    FREQUENCY("Frequenz", "Frequenz-Kalibrierung"),
    TEMPERATURE("Temperatur", "Temperatur-Kompensation"),
    FULL_SYSTEM("Vollsystem", "Komplette System-Kalibrierung"),
    REFERENCE_MATERIAL("Referenzmaterial", "Kalibrierung mit bekanntem Material"),
    AIR_CALIBRATION("Luft-Kalibrierung", "Kalibrierung in Luft"),
    BACKGROUND("Hintergrund", "Hintergrund-Kalibrierung");
    
    fun isRequired(): Boolean = this in listOf(ZERO_OFFSET, GAIN, FULL_SYSTEM)
    fun isOptional(): Boolean = this in listOf(TEMPERATURE, BACKGROUND)
}

// Error Type
enum class ErrorType(val displayName: String, val severity: ErrorSeverity) {
    // Bluetooth Errors
    BLUETOOTH_DISABLED("Bluetooth deaktiviert", ErrorSeverity.WARNING),
    BLUETOOTH_PERMISSION_DENIED("Bluetooth-Berechtigung verweigert", ErrorSeverity.ERROR),
    DEVICE_NOT_FOUND("Gerät nicht gefunden", ErrorSeverity.ERROR),
    CONNECTION_FAILED("Verbindung fehlgeschlagen", ErrorSeverity.ERROR),
    CONNECTION_LOST("Verbindung verloren", ErrorSeverity.WARNING),
    
    // Measurement Errors
    CALIBRATION_REQUIRED("Kalibrierung erforderlich", ErrorSeverity.WARNING),
    INVALID_DATA("Ungültige Daten", ErrorSeverity.ERROR),
    SENSOR_ERROR("Sensor-Fehler", ErrorSeverity.ERROR),
    TEMPERATURE_OUT_OF_RANGE("Temperatur außerhalb des Bereichs", ErrorSeverity.WARNING),
    BATTERY_LOW("Batterie schwach", ErrorSeverity.WARNING),
    
    // System Errors
    MEMORY_LOW("Speicher knapp", ErrorSeverity.WARNING),
    STORAGE_FULL("Speicher voll", ErrorSeverity.ERROR),
    PERMISSION_DENIED("Berechtigung verweigert", ErrorSeverity.ERROR),
    NETWORK_ERROR("Netzwerk-Fehler", ErrorSeverity.WARNING),
    
    // AI Errors
    MODEL_NOT_LOADED("AI-Modell nicht geladen", ErrorSeverity.ERROR),
    CLASSIFICATION_FAILED("Klassifikation fehlgeschlagen", ErrorSeverity.WARNING),
    INSUFFICIENT_DATA("Unzureichende Daten", ErrorSeverity.WARNING),
    
    // Export Errors
    EXPORT_FAILED("Export fehlgeschlagen", ErrorSeverity.ERROR),
    FILE_ACCESS_DENIED("Dateizugriff verweigert", ErrorSeverity.ERROR),
    DISK_FULL("Festplatte voll", ErrorSeverity.ERROR);
    
    fun isCritical(): Boolean = severity == ErrorSeverity.CRITICAL
    fun requiresUserAction(): Boolean = severity in listOf(ErrorSeverity.ERROR, ErrorSeverity.CRITICAL)
}

// Error Severity
enum class ErrorSeverity(val displayName: String, val level: Int) {
    INFO("Information", 0),
    WARNING("Warnung", 1),
    ERROR("Fehler", 2),
    CRITICAL("Kritisch", 3);
    
    fun isHigherThan(other: ErrorSeverity): Boolean = level > other.level
}

// Log Level
enum class LogLevel(val displayName: String, val level: Int) {
    VERBOSE("Ausführlich", 0),
    DEBUG("Debug", 1),
    INFO("Information", 2),
    WARNING("Warnung", 3),
    ERROR("Fehler", 4),
    CRITICAL("Kritisch", 5);
    
    fun shouldLog(targetLevel: LogLevel): Boolean = level >= targetLevel.level
}

// Theme Mode
enum class Theme(val displayName: String) {
    LIGHT("Hell"),
    DARK("Dunkel"),
    SYSTEM("System");
    
    fun isSystemControlled(): Boolean = this == SYSTEM
}

// Language
enum class Language(val displayName: String, val code: String) {
    GERMAN("Deutsch", "de"),
    ENGLISH("English", "en"),
    FRENCH("Français", "fr"),
    SPANISH("Español", "es"),
    ITALIAN("Italiano", "it");
    
    companion object {
        fun fromCode(code: String): Language? = values().find { it.code == code }
    }
}

// Unit System
enum class UnitSystem(val displayName: String) {
    METRIC("Metrisch"),
    IMPERIAL("Imperial"),
    SCIENTIFIC("Wissenschaftlich");
    
    fun isMetric(): Boolean = this == METRIC
}

// Notification Priority
enum class NotificationPriority(val displayName: String, val level: Int) {
    LOW("Niedrig", 0),
    NORMAL("Normal", 1),
    HIGH("Hoch", 2),
    URGENT("Dringend", 3);
    
    fun isUrgent(): Boolean = this == URGENT
}

// Device Type
enum class DeviceType(val displayName: String, val modelPrefix: String) {
    EMFAD_UG12_DS_WL("EMFAD UG12 DS WL", "UG12"),
    EMFAD_UG12_DS("EMFAD UG12 DS", "UG12"),
    EMFAD_PROTOTYPE("EMFAD Prototyp", "PROTO"),
    UNKNOWN("Unbekannt", "UNK");
    
    companion object {
        fun fromModelName(modelName: String): DeviceType {
            return values().find { modelName.startsWith(it.modelPrefix) } ?: UNKNOWN
        }
    }
    
    fun isWireless(): Boolean = this == EMFAD_UG12_DS_WL
    fun isProduction(): Boolean = this in listOf(EMFAD_UG12_DS_WL, EMFAD_UG12_DS)
}
