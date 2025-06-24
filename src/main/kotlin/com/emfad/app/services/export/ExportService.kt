package com.emfad.app.services.export

import android.content.Context
import android.os.Environment
import android.util.Log
import com.emfad.app.database.EMFADDatabase
import com.emfad.app.models.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * EMFAD Export Service
 * CSV, PDF, MATLAB Export mit ursprünglichen Algorithmen
 * Samsung S21 Ultra optimiert
 */
class ExportService(
    private val context: Context,
    private val database: EMFADDatabase
) {
    
    companion object {
        private const val TAG = "ExportService"
        private const val EXPORT_FOLDER = "EMFAD_Exports"
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
    }
    
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd HH:mm:ss")
        .create()
    
    /**
     * Session als CSV exportieren
     */
    suspend fun exportSessionToCSV(sessionId: Long): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Exportiere Session $sessionId als CSV")
            
            // Session-Daten laden
            val session = database.measurementSessionDao().getById(sessionId)?.toDomainModel()
                ?: return@withContext Result.failure(Exception("Session nicht gefunden"))
            
            val readings = database.emfReadingDao().getBySessionId(sessionId)
                .map { it.toDomainModel() }
            
            val analyses = database.materialAnalysisDao().getBySessionId(sessionId)
                .map { it.toDomainModel() }
            
            // Export-Datei erstellen
            val exportDir = getExportDirectory()
            val fileName = "EMFAD_Session_${session.name}_${DATE_FORMAT.format(Date())}.csv"
            val file = File(exportDir, fileName)
            
            FileWriter(file).use { writer ->
                // CSV-Header schreiben
                writeCSVHeader(writer)
                
                // Messdaten schreiben
                readings.forEach { reading ->
                    writeCSVReading(writer, reading, session, analyses)
                }
            }
            
            // Session als exportiert markieren
            database.measurementSessionDao().markAsExported(
                sessionId, 
                System.currentTimeMillis(), 
                file.absolutePath
            )
            
            Log.d(TAG, "CSV-Export erfolgreich: ${file.absolutePath}")
            Result.success(file.absolutePath)
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim CSV-Export", e)
            Result.failure(e)
        }
    }
    
    /**
     * Session als PDF exportieren
     */
    suspend fun exportSessionToPDF(sessionId: Long): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Exportiere Session $sessionId als PDF")
            
            // Session-Daten laden
            val session = database.measurementSessionDao().getById(sessionId)?.toDomainModel()
                ?: return@withContext Result.failure(Exception("Session nicht gefunden"))
            
            val readings = database.emfReadingDao().getBySessionId(sessionId)
                .map { it.toDomainModel() }
            
            val analyses = database.materialAnalysisDao().getBySessionId(sessionId)
                .map { it.toDomainModel() }
            
            // PDF erstellen
            val exportDir = getExportDirectory()
            val fileName = "EMFAD_Report_${session.name}_${DATE_FORMAT.format(Date())}.pdf"
            val file = File(exportDir, fileName)
            
            val pdfGenerator = PDFGenerator(context)
            pdfGenerator.generateSessionReport(session, readings, analyses, file)
            
            // Session als exportiert markieren
            database.measurementSessionDao().markAsExported(
                sessionId, 
                System.currentTimeMillis(), 
                file.absolutePath
            )
            
            Log.d(TAG, "PDF-Export erfolgreich: ${file.absolutePath}")
            Result.success(file.absolutePath)
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim PDF-Export", e)
            Result.failure(e)
        }
    }
    
    /**
     * Session als MATLAB-Datei exportieren
     */
    suspend fun exportSessionToMatlab(sessionId: Long): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Exportiere Session $sessionId als MATLAB")
            
            // Session-Daten laden
            val session = database.measurementSessionDao().getById(sessionId)?.toDomainModel()
                ?: return@withContext Result.failure(Exception("Session nicht gefunden"))
            
            val readings = database.emfReadingDao().getBySessionId(sessionId)
                .map { it.toDomainModel() }
            
            val analyses = database.materialAnalysisDao().getBySessionId(sessionId)
                .map { it.toDomainModel() }
            
            // MATLAB-Datei erstellen
            val exportDir = getExportDirectory()
            val fileName = "EMFAD_Data_${session.name}_${DATE_FORMAT.format(Date())}.m"
            val file = File(exportDir, fileName)
            
            FileWriter(file).use { writer ->
                writeMatlabHeader(writer, session)
                writeMatlabData(writer, readings, analyses)
                writeMatlabAnalysisFunctions(writer)
            }
            
            // Session als exportiert markieren
            database.measurementSessionDao().markAsExported(
                sessionId, 
                System.currentTimeMillis(), 
                file.absolutePath
            )
            
            Log.d(TAG, "MATLAB-Export erfolgreich: ${file.absolutePath}")
            Result.success(file.absolutePath)
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim MATLAB-Export", e)
            Result.failure(e)
        }
    }
    
    /**
     * Session als JSON exportieren
     */
    suspend fun exportSessionToJSON(sessionId: Long): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Exportiere Session $sessionId als JSON")
            
            // Session-Daten laden
            val session = database.measurementSessionDao().getById(sessionId)?.toDomainModel()
                ?: return@withContext Result.failure(Exception("Session nicht gefunden"))
            
            val readings = database.emfReadingDao().getBySessionId(sessionId)
                .map { it.toDomainModel() }
            
            val analyses = database.materialAnalysisDao().getBySessionId(sessionId)
                .map { it.toDomainModel() }
            
            // JSON-Export-Objekt erstellen
            val exportData = SessionExportData(
                session = session,
                readings = readings,
                analyses = analyses,
                exportTimestamp = System.currentTimeMillis(),
                exportVersion = "1.0.0"
            )
            
            // JSON-Datei erstellen
            val exportDir = getExportDirectory()
            val fileName = "EMFAD_Data_${session.name}_${DATE_FORMAT.format(Date())}.json"
            val file = File(exportDir, fileName)
            
            FileWriter(file).use { writer ->
                gson.toJson(exportData, writer)
            }
            
            Log.d(TAG, "JSON-Export erfolgreich: ${file.absolutePath}")
            Result.success(file.absolutePath)
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim JSON-Export", e)
            Result.failure(e)
        }
    }
    
    /**
     * CSV-Header schreiben
     */
    private fun writeCSVHeader(writer: FileWriter) {
        val header = listOf(
            "Timestamp", "Session_ID", "Session_Name", "Device_ID",
            "Position_X", "Position_Y", "Position_Z",
            "Frequency", "Signal_Strength", "Phase", "Amplitude",
            "Real_Part", "Imaginary_Part", "Magnitude", "Depth",
            "Temperature", "Humidity", "Pressure", "Battery_Level",
            "Material_Type", "Confidence", "Noise_Level",
            "Calibration_Offset", "Gain_Setting", "Filter_Setting",
            "Measurement_Mode", "Quality_Score", "Is_Validated"
        ).joinToString(",")
        
        writer.write(header + "\n")
    }
    
    /**
     * CSV-Messung schreiben
     */
    private fun writeCSVReading(
        writer: FileWriter, 
        reading: EMFReading, 
        session: MeasurementSession,
        analyses: List<MaterialAnalysis>
    ) {
        val analysis = analyses.find { it.timestamp <= reading.timestamp }
        
        val row = listOf(
            reading.timestamp.toString(),
            session.id.toString(),
            "\"${session.name}\"",
            "\"${reading.deviceId}\"",
            reading.positionX.toString(),
            reading.positionY.toString(),
            reading.positionZ.toString(),
            reading.frequency.toString(),
            reading.signalStrength.toString(),
            reading.phase.toString(),
            reading.amplitude.toString(),
            reading.realPart.toString(),
            reading.imaginaryPart.toString(),
            reading.magnitude.toString(),
            reading.depth.toString(),
            reading.temperature.toString(),
            reading.humidity.toString(),
            reading.pressure.toString(),
            reading.batteryLevel.toString(),
            reading.materialType.toString(),
            reading.confidence.toString(),
            reading.noiseLevel.toString(),
            reading.calibrationOffset.toString(),
            reading.gainSetting.toString(),
            "\"${reading.filterSetting}\"",
            "\"${reading.measurementMode}\"",
            reading.qualityScore.toString(),
            reading.isValidated.toString()
        ).joinToString(",")
        
        writer.write(row + "\n")
    }
    
    /**
     * MATLAB-Header schreiben
     */
    private fun writeMatlabHeader(writer: FileWriter, session: MeasurementSession) {
        writer.write("""
            %% EMFAD Measurement Data Export
            %% Session: ${session.name}
            %% Generated: ${Date()}
            %% Device: ${session.deviceName}
            %% Operator: ${session.operatorName}
            %% Location: ${session.location}
            
            clear all;
            close all;
            clc;
            
            %% Session Information
            session_info.name = '${session.name}';
            session_info.description = '${session.description}';
            session_info.start_time = ${session.startTimestamp};
            session_info.end_time = ${session.endTimestamp ?: "NaN"};
            session_info.device_id = '${session.deviceId}';
            session_info.operator = '${session.operatorName}';
            session_info.location = '${session.location}';
            session_info.project = '${session.projectName}';
            
        """.trimIndent())
    }
    
    /**
     * MATLAB-Daten schreiben
     */
    private fun writeMatlabData(
        writer: FileWriter, 
        readings: List<EMFReading>, 
        analyses: List<MaterialAnalysis>
    ) {
        if (readings.isEmpty()) return
        
        writer.write("\n%% Measurement Data\n")
        
        // Arrays für alle Messwerte
        val fields = listOf(
            "timestamp", "frequency", "signal_strength", "phase", "amplitude",
            "real_part", "imaginary_part", "magnitude", "depth", "temperature",
            "position_x", "position_y", "position_z", "quality_score"
        )
        
        fields.forEach { field ->
            writer.write("data.$field = [")
            readings.forEachIndexed { index, reading ->
                val value = when (field) {
                    "timestamp" -> reading.timestamp
                    "frequency" -> reading.frequency
                    "signal_strength" -> reading.signalStrength
                    "phase" -> reading.phase
                    "amplitude" -> reading.amplitude
                    "real_part" -> reading.realPart
                    "imaginary_part" -> reading.imaginaryPart
                    "magnitude" -> reading.magnitude
                    "depth" -> reading.depth
                    "temperature" -> reading.temperature
                    "position_x" -> reading.positionX
                    "position_y" -> reading.positionY
                    "position_z" -> reading.positionZ
                    "quality_score" -> reading.qualityScore
                    else -> 0.0
                }
                writer.write(value.toString())
                if (index < readings.size - 1) writer.write(", ")
            }
            writer.write("];\n")
        }
        
        // Material-Analysen
        if (analyses.isNotEmpty()) {
            writer.write("\n%% Material Analysis Data\n")
            writer.write("analysis.material_types = {")
            analyses.forEachIndexed { index, analysis ->
                writer.write("'${analysis.materialType}'")
                if (index < analyses.size - 1) writer.write(", ")
            }
            writer.write("};\n")
            
            writer.write("analysis.confidence = [")
            analyses.forEachIndexed { index, analysis ->
                writer.write(analysis.confidence.toString())
                if (index < analyses.size - 1) writer.write(", ")
            }
            writer.write("];\n")
        }
    }
    
    /**
     * MATLAB-Analysefunktionen schreiben
     */
    private fun writeMatlabAnalysisFunctions(writer: FileWriter) {
        writer.write("""
            
            %% Analysis Functions
            
            % Plot signal strength over time
            function plot_signal_strength(data)
                figure;
                plot(data.timestamp, data.signal_strength);
                xlabel('Time (ms)');
                ylabel('Signal Strength');
                title('EMF Signal Strength over Time');
                grid on;
            end
            
            % Plot frequency response
            function plot_frequency_response(data)
                figure;
                scatter(data.frequency, data.signal_strength, 'filled');
                xlabel('Frequency (Hz)');
                ylabel('Signal Strength');
                title('EMF Frequency Response');
                grid on;
            end
            
            % Calculate conductivity (original algorithm)
            function conductivity = calculate_conductivity(frequency, signal_strength, depth)
                omega = 2 * pi * frequency;
                mu0 = 4 * pi * 1e-7;
                skin_depth = depth ./ signal_strength * 1000;
                conductivity = 2 ./ (omega * mu0 * skin_depth.^2);
            end
            
            % Plot 3D EMF field
            function plot_3d_field(data)
                figure;
                scatter3(data.position_x, data.position_y, data.position_z, ...
                        50, data.signal_strength, 'filled');
                xlabel('X Position (m)');
                ylabel('Y Position (m)');
                zlabel('Z Position (m)');
                colorbar;
                title('3D EMF Field Visualization');
            end
            
            %% Auto-generate plots
            if exist('data', 'var')
                plot_signal_strength(data);
                plot_frequency_response(data);
                plot_3d_field(data);
            end
            
        """.trimIndent())
    }
    
    /**
     * Export-Verzeichnis erstellen/abrufen
     */
    private fun getExportDirectory(): File {
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val exportDir = File(externalDir, EXPORT_FOLDER)
        
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        
        return exportDir
    }
    
    /**
     * Verfügbare Export-Formate abrufen
     */
    fun getAvailableFormats(): List<ExportFormat> {
        return listOf(
            ExportFormat.CSV,
            ExportFormat.PDF,
            ExportFormat.MATLAB,
            ExportFormat.JSON
        )
    }
    
    /**
     * Export-Statistiken abrufen
     */
    suspend fun getExportStatistics(): ExportStatistics = withContext(Dispatchers.IO) {
        try {
            val exportedSessions = database.measurementSessionDao().getExportedSessions()
            val totalExports = exportedSessions.size
            val exportDir = getExportDirectory()
            val exportFiles = exportDir.listFiles()?.size ?: 0
            val totalSize = exportDir.walkTopDown()
                .filter { it.isFile }
                .map { it.length() }
                .sum()
            
            ExportStatistics(
                totalExports = totalExports,
                totalFiles = exportFiles,
                totalSizeBytes = totalSize,
                lastExportTime = exportedSessions.maxOfOrNull { it.exportTimestamp ?: 0L } ?: 0L
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Abrufen der Export-Statistiken", e)
            ExportStatistics()
        }
    }
}

/**
 * Session Export Data
 */
data class SessionExportData(
    val session: MeasurementSession,
    val readings: List<EMFReading>,
    val analyses: List<MaterialAnalysis>,
    val exportTimestamp: Long,
    val exportVersion: String
)

/**
 * Export-Formate
 */
enum class ExportFormat(val displayName: String, val fileExtension: String) {
    CSV("CSV (Comma Separated Values)", "csv"),
    PDF("PDF Report", "pdf"),
    MATLAB("MATLAB Script", "m"),
    JSON("JSON Data", "json")
}

/**
 * Export-Statistiken
 */
data class ExportStatistics(
    val totalExports: Int = 0,
    val totalFiles: Int = 0,
    val totalSizeBytes: Long = 0L,
    val lastExportTime: Long = 0L
)
