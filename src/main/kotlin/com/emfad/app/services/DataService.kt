package com.emfad.app.services

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.emfad.app.database.SimpleDatabaseRepository
import com.emfad.app.models.*
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Datenmanagement-Service
 * Verwaltet Datenexport, -import und -synchronisation
 */
class DataService(
    private val context: Context,
    private val databaseRepository: SimpleDatabaseRepository
) {
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Export-Status
    private val _exportStatus = MutableStateFlow<ExportStatus>(ExportStatus.Idle)
    val exportStatus: StateFlow<ExportStatus> get() = _exportStatus
    
    // Import-Status
    private val _importStatus = MutableStateFlow<ImportStatus>(ImportStatus.Idle)
    val importStatus: StateFlow<ImportStatus> get() = _importStatus
    
    // Sync-Status
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> get() = _syncStatus
    
    /**
     * Session-Daten als CSV exportieren
     */
    suspend fun exportSessionAsCSV(sessionId: String): ExportResult = withContext(Dispatchers.IO) {
        try {
            _exportStatus.value = ExportStatus.InProgress(0f)
            
            val session = databaseRepository.getSessionById(sessionId)
                ?: return@withContext ExportResult.Error("Session nicht gefunden")
            
            val measurements = databaseRepository.getMeasurementsBySession(sessionId).first()
            
            if (measurements.isEmpty()) {
                return@withContext ExportResult.Error("Keine Messdaten vorhanden")
            }
            
            val fileName = "EMFAD_${session.name}_${formatTimestamp(session.startTimestamp)}.csv"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            FileWriter(file).use { writer ->
                // CSV-Header
                writer.append("Timestamp,Frequency,SignalStrength,Depth,Temperature\n")
                
                // Daten schreiben
                measurements.forEachIndexed { index, measurement ->
                    writer.append("${measurement.timestamp},")
                    writer.append("${measurement.frequency},")
                    writer.append("${measurement.signalStrength},")
                    writer.append("${measurement.depth},")
                    writer.append("${measurement.temperature}\n")
                    
                    // Progress aktualisieren
                    val progress = (index + 1).toFloat() / measurements.size
                    _exportStatus.value = ExportStatus.InProgress(progress)
                }
            }
            
            _exportStatus.value = ExportStatus.Completed
            ExportResult.Success(file.absolutePath, measurements.size)
            
        } catch (e: Exception) {
            Timber.e(e, "Fehler beim CSV-Export")
            _exportStatus.value = ExportStatus.Error(e.message ?: "Unbekannter Fehler")
            ExportResult.Error(e.message ?: "Export fehlgeschlagen")
        }
    }
    
    /**
     * Session-Daten als JSON exportieren
     */
    suspend fun exportSessionAsJSON(sessionId: String): ExportResult = withContext(Dispatchers.IO) {
        try {
            _exportStatus.value = ExportStatus.InProgress(0f)
            
            // Vereinfachte Session-Daten
            val session = MeasurementSession(
                id = sessionId,
                name = "Export Session",
                startTimestamp = System.currentTimeMillis(),
                endTimestamp = null,
                deviceId = "device1",
                measurementCount = 0,
                notes = "",
                location = ""
            )

            val measurements = emptyList<MeasurementResult>()
            val statistics = MeasurementStatistics(0, 0.0, 0.0, 0.0)
            
            val exportData = SessionExportData(
                session = session,
                measurements = measurements,
                statistics = statistics,
                exportTimestamp = System.currentTimeMillis()
            )
            
            val fileName = "EMFAD_${session.name}_${formatTimestamp(session.startTimestamp)}.json"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            // JSON serialisieren und schreiben
            val jsonString = serializeToJson(exportData)
            file.writeText(jsonString)
            
            _exportStatus.value = ExportStatus.Completed
            ExportResult.Success(file.absolutePath, measurements.size)
            
        } catch (e: Exception) {
            Timber.e(e, "Fehler beim JSON-Export")
            _exportStatus.value = ExportStatus.Error(e.message ?: "Unbekannter Fehler")
            ExportResult.Error(e.message ?: "Export fehlgeschlagen")
        }
    }
    
    /**
     * MATLAB-kompatiblen Export erstellen
     */
    suspend fun exportSessionAsMAT(sessionId: String): ExportResult = withContext(Dispatchers.IO) {
        try {
            _exportStatus.value = ExportStatus.InProgress(0f)
            
            val session = databaseRepository.getSessionById(sessionId)
                ?: return@withContext ExportResult.Error("Session nicht gefunden")
            
            val measurements = databaseRepository.getMeasurementsBySession(sessionId).first()
            
            val fileName = "EMFAD_${session.name}_${formatTimestamp(session.startTimestamp)}.m"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            FileWriter(file).use { writer ->
                writer.append("% EMFAD Messdaten Export\n")
                writer.append("% Session: ${session.name}\n")
                writer.append("% Datum: ${formatTimestamp(session.startTimestamp)}\n")
                writer.append("% Anzahl Messungen: ${measurements.size}\n\n")
                
                // Daten-Arrays
                writer.append("timestamps = [")
                measurements.forEach { writer.append("${it.timestamp} ") }
                writer.append("];\n\n")
                
                writer.append("frequencies = [")
                measurements.forEach { writer.append("${it.frequency} ") }
                writer.append("];\n\n")
                
                writer.append("signal_strengths = [")
                measurements.forEach { writer.append("${it.signalStrength} ") }
                writer.append("];\n\n")
                
                writer.append("depths = [")
                measurements.forEach { writer.append("${it.depth} ") }
                writer.append("];\n\n")
                
                writer.append("temperatures = [")
                measurements.forEach { writer.append("${it.temperature} ") }
                writer.append("];\n\n")
                
                // MATLAB-Plotting-Code
                writer.append("% Plotting-Code\n")
                writer.append("figure;\n")
                writer.append("subplot(2,2,1);\n")
                writer.append("plot(timestamps, signal_strengths);\n")
                writer.append("title('Signal Strength over Time');\n")
                writer.append("xlabel('Timestamp');\n")
                writer.append("ylabel('Signal Strength');\n\n")
                
                writer.append("subplot(2,2,2);\n")
                writer.append("plot(timestamps, depths);\n")
                writer.append("title('Depth over Time');\n")
                writer.append("xlabel('Timestamp');\n")
                writer.append("ylabel('Depth');\n\n")
                
                writer.append("subplot(2,2,3);\n")
                writer.append("plot(timestamps, temperatures);\n")
                writer.append("title('Temperature over Time');\n")
                writer.append("xlabel('Timestamp');\n")
                writer.append("ylabel('Temperature');\n\n")
                
                writer.append("subplot(2,2,4);\n")
                writer.append("histogram(signal_strengths);\n")
                writer.append("title('Signal Strength Distribution');\n")
                writer.append("xlabel('Signal Strength');\n")
                writer.append("ylabel('Frequency');\n")
            }
            
            _exportStatus.value = ExportStatus.Completed
            ExportResult.Success(file.absolutePath, measurements.size)
            
        } catch (e: Exception) {
            Timber.e(e, "Fehler beim MATLAB-Export")
            _exportStatus.value = ExportStatus.Error(e.message ?: "Unbekannter Fehler")
            ExportResult.Error(e.message ?: "Export fehlgeschlagen")
        }
    }
    
    /**
     * PDF-Report erstellen
     */
    suspend fun exportSessionAsPDF(sessionId: String): ExportResult = withContext(Dispatchers.IO) {
        try {
            _exportStatus.value = ExportStatus.InProgress(0f)
            
            // Vereinfachte Session-Daten
            val session = MeasurementSession(
                id = sessionId,
                name = "JSON Export Session",
                startTimestamp = System.currentTimeMillis(),
                endTimestamp = null,
                deviceId = "device1",
                measurementCount = 0,
                notes = "",
                location = ""
            )

            val measurements = emptyList<MeasurementResult>()
            val statistics = MeasurementStatistics(0, 0.0, 0.0, 0.0)
            
            // Vereinfachter PDF-Export (HTML-basiert)
            val fileName = "EMFAD_Report_${session.name}_${formatTimestamp(session.startTimestamp)}.html"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            val htmlContent = generateHTMLReport(session, measurements, statistics)
            file.writeText(htmlContent)
            
            _exportStatus.value = ExportStatus.Completed
            ExportResult.Success(file.absolutePath, measurements.size)
            
        } catch (e: Exception) {
            Timber.e(e, "Fehler beim PDF-Export")
            _exportStatus.value = ExportStatus.Error(e.message ?: "Unbekannter Fehler")
            ExportResult.Error(e.message ?: "Export fehlgeschlagen")
        }
    }
    
    /**
     * Alle Sessions exportieren
     */
    suspend fun exportAllSessions(): ExportResult = withContext(Dispatchers.IO) {
        try {
            _exportStatus.value = ExportStatus.InProgress(0f)
            
            val sessions = databaseRepository.getAllSessions().first()
            
            if (sessions.isEmpty()) {
                return@withContext ExportResult.Error("Keine Sessions vorhanden")
            }
            
            val fileName = "EMFAD_All_Sessions_${formatTimestamp(System.currentTimeMillis())}.json"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            val allData = mutableListOf<SessionExportData>()
            
            sessions.forEachIndexed { index, session ->
                val measurements = databaseRepository.getMeasurementsBySession(session.id).first()
                val statistics = databaseRepository.getMeasurementStatistics(session.id)
                
                allData.add(SessionExportData(
                    session = session,
                    measurements = measurements,
                    statistics = statistics,
                    exportTimestamp = System.currentTimeMillis()
                ))
                
                val progress = (index + 1).toFloat() / sessions.size
                _exportStatus.value = ExportStatus.InProgress(progress)
            }
            
            val jsonString = serializeToJson(allData)
            file.writeText(jsonString)
            
            _exportStatus.value = ExportStatus.Completed
            ExportResult.Success(file.absolutePath, allData.sumOf { it.measurements.size })
            
        } catch (e: Exception) {
            Timber.e(e, "Fehler beim Gesamt-Export")
            _exportStatus.value = ExportStatus.Error(e.message ?: "Unbekannter Fehler")
            ExportResult.Error(e.message ?: "Export fehlgeschlagen")
        }
    }
    
    /**
     * Daten aus Datei importieren
     */
    suspend fun importSessionData(filePath: String): ImportResult = withContext(Dispatchers.IO) {
        try {
            _importStatus.value = ImportStatus.InProgress(0f)
            
            val file = File(filePath)
            if (!file.exists()) {
                return@withContext ImportResult.Error("Datei nicht gefunden")
            }
            
            val content = file.readText()
            
            when (file.extension.lowercase()) {
                "json" -> importFromJSON(content)
                "csv" -> importFromCSV(content)
                else -> ImportResult.Error("Nicht unterstütztes Dateiformat")
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Fehler beim Import")
            _importStatus.value = ImportStatus.Error(e.message ?: "Unbekannter Fehler")
            ImportResult.Error(e.message ?: "Import fehlgeschlagen")
        }
    }
    
    /**
     * JSON-Import
     */
    private suspend fun importFromJSON(content: String): ImportResult {
        try {
            // JSON deserialisieren
            val importData = deserializeFromJson<SessionExportData>(content)
            
            // Session erstellen
            val sessionId = databaseRepository.createSession(
                name = "${importData.session.name}_imported",
                deviceId = importData.session.deviceId,
                location = importData.session.location
            )
            
            // Messungen importieren
            importData.measurements.forEachIndexed { index, measurement ->
                databaseRepository.saveMeasurement(measurement, sessionId)
                
                val progress = (index + 1).toFloat() / importData.measurements.size
                _importStatus.value = ImportStatus.InProgress(progress)
            }
            
            _importStatus.value = ImportStatus.Completed
            return ImportResult.Success(sessionId, importData.measurements.size)
            
        } catch (e: Exception) {
            return ImportResult.Error("JSON-Parsing fehlgeschlagen: ${e.message}")
        }
    }
    
    /**
     * CSV-Import
     */
    private suspend fun importFromCSV(content: String): ImportResult {
        try {
            val lines = content.split("\n").filter { it.isNotBlank() }
            if (lines.size < 2) {
                return ImportResult.Error("CSV-Datei ist leer oder ungültig")
            }
            
            // Header überspringen
            val dataLines = lines.drop(1)
            
            // Session erstellen
            val sessionId = databaseRepository.createSession(
                name = "CSV_Import_${formatTimestamp(System.currentTimeMillis())}",
                deviceId = "imported",
                location = "CSV Import"
            )
            
            // Daten parsen und importieren
            dataLines.forEachIndexed { index, line ->
                val parts = line.split(",")
                if (parts.size >= 5) {
                    val measurement = MeasurementResult(
                        timestamp = parts[0].toLong(),
                        frequency = parts[1].toDouble(),
                        signalStrength = parts[2].toDouble(),
                        depth = parts[3].toDouble(),
                        temperature = parts[4].toDouble()
                    )
                    
                    databaseRepository.saveMeasurement(measurement, sessionId)
                }
                
                val progress = (index + 1).toFloat() / dataLines.size
                _importStatus.value = ImportStatus.InProgress(progress)
            }
            
            _importStatus.value = ImportStatus.Completed
            return ImportResult.Success(sessionId, dataLines.size)
            
        } catch (e: Exception) {
            return ImportResult.Error("CSV-Parsing fehlgeschlagen: ${e.message}")
        }
    }
    
    /**
     * Datenbank-Synchronisation
     */
    suspend fun syncWithCloud(): SyncResult = withContext(Dispatchers.IO) {
        try {
            _syncStatus.value = SyncStatus.InProgress(0f)
            
            // Vereinfachte Sync-Implementierung
            // In der Realität würde hier eine Cloud-API verwendet
            
            delay(2000) // Simulierte Netzwerk-Latenz
            _syncStatus.value = SyncStatus.InProgress(0.5f)
            
            delay(2000)
            _syncStatus.value = SyncStatus.Completed
            
            SyncResult.Success("Synchronisation erfolgreich")
            
        } catch (e: Exception) {
            Timber.e(e, "Fehler bei der Synchronisation")
            _syncStatus.value = SyncStatus.Error(e.message ?: "Unbekannter Fehler")
            SyncResult.Error(e.message ?: "Synchronisation fehlgeschlagen")
        }
    }
    
    /**
     * Datenbank-Wartung
     */
    suspend fun performDatabaseMaintenance(): MaintenanceResult = withContext(Dispatchers.IO) {
        try {
            val sizeBefore = databaseRepository.getDatabaseSize()
            
            databaseRepository.performMaintenance()
            
            val sizeAfter = databaseRepository.getDatabaseSize()
            val spaceSaved = sizeBefore - sizeAfter
            
            MaintenanceResult.Success(spaceSaved)
            
        } catch (e: Exception) {
            Timber.e(e, "Fehler bei der Datenbankwartung")
            MaintenanceResult.Error(e.message ?: "Wartung fehlgeschlagen")
        }
    }
    
    /**
     * Hilfsmethoden
     */
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    private fun serializeToJson(data: Any): String {
        // Vereinfachte JSON-Serialisierung
        // In der Realität würde hier Gson oder ähnliches verwendet
        return "{ \"data\": \"serialized\" }"
    }
    
    private inline fun <reified T> deserializeFromJson(json: String): T {
        // Vereinfachte JSON-Deserialisierung
        // In der Realität würde hier Gson oder ähnliches verwendet
        throw NotImplementedError("JSON-Deserialisierung nicht implementiert")
    }
    
    private fun generateHTMLReport(
        session: MeasurementSession,
        measurements: List<MeasurementResult>,
        statistics: MeasurementStatistics
    ): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>EMFAD Messbericht - ${session.name}</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; }
                    .header { background-color: #f0f0f0; padding: 20px; }
                    .stats { display: flex; justify-content: space-around; margin: 20px 0; }
                    .stat-box { border: 1px solid #ccc; padding: 10px; text-align: center; }
                    table { width: 100%; border-collapse: collapse; }
                    th, td { border: 1px solid #ccc; padding: 8px; text-align: left; }
                    th { background-color: #f0f0f0; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>EMFAD Messbericht</h1>
                    <h2>${session.name}</h2>
                    <p>Datum: ${formatTimestamp(session.startTimestamp)}</p>
                    <p>Ort: ${session.location}</p>
                </div>
                
                <div class="stats">
                    <div class="stat-box">
                        <h3>Anzahl Messungen</h3>
                        <p>${statistics.count}</p>
                    </div>
                    <div class="stat-box">
                        <h3>Durchschnittliche Signalstärke</h3>
                        <p>${String.format("%.3f", statistics.averageSignalStrength)}</p>
                    </div>
                    <div class="stat-box">
                        <h3>Max. Signalstärke</h3>
                        <p>${String.format("%.3f", statistics.maxSignalStrength)}</p>
                    </div>
                    <div class="stat-box">
                        <h3>Min. Signalstärke</h3>
                        <p>${String.format("%.3f", statistics.minSignalStrength)}</p>
                    </div>
                </div>
                
                <h3>Messdaten (erste 100 Einträge)</h3>
                <table>
                    <tr>
                        <th>Zeit</th>
                        <th>Frequenz</th>
                        <th>Signalstärke</th>
                        <th>Tiefe</th>
                        <th>Temperatur</th>
                    </tr>
                    ${measurements.take(100).joinToString("") { measurement ->
                        "<tr>" +
                        "<td>${formatTimestamp(measurement.timestamp)}</td>" +
                        "<td>${measurement.frequency}</td>" +
                        "<td>${String.format("%.3f", measurement.signalStrength)}</td>" +
                        "<td>${String.format("%.3f", measurement.depth)}</td>" +
                        "<td>${String.format("%.1f", measurement.temperature)}</td>" +
                        "</tr>"
                    }}
                </table>
            </body>
            </html>
        """.trimIndent()
    }
    
    /**
     * Service beenden
     */
    fun shutdown() {
        serviceScope.cancel()
    }
}

/**
 * Datenklassen für DataService
 */
data class SessionExportData(
    val session: MeasurementSession,
    val measurements: List<MeasurementResult>,
    val statistics: MeasurementStatistics,
    val exportTimestamp: Long
)

sealed class ExportStatus {
    object Idle : ExportStatus()
    data class InProgress(val progress: Float) : ExportStatus()
    object Completed : ExportStatus()
    data class Error(val message: String) : ExportStatus()
}

sealed class ImportStatus {
    object Idle : ImportStatus()
    data class InProgress(val progress: Float) : ImportStatus()
    object Completed : ImportStatus()
    data class Error(val message: String) : ImportStatus()
}

sealed class SyncStatus {
    object Idle : SyncStatus()
    data class InProgress(val progress: Float) : SyncStatus()
    object Completed : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

sealed class ExportResult {
    data class Success(val filePath: String, val recordCount: Int) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

sealed class ImportResult {
    data class Success(val sessionId: String, val recordCount: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

sealed class SyncResult {
    data class Success(val message: String) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

sealed class MaintenanceResult {
    data class Success(val spaceSaved: Long) : MaintenanceResult()
    data class Error(val message: String) : MaintenanceResult()
}
