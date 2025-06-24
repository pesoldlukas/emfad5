package com.emfad.app.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.emfad.app.models.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

/**
 * Repository für Datenbankoperationen
 * Abstraktionsschicht zwischen ViewModels und Database
 */
class DatabaseRepository(private val database: EMFADDatabase) {
    
    private val gson = Gson()
    
    // DAOs
    private val measurementDao = database.measurementDao()
    private val sessionDao = database.sessionDao()
    private val calibrationDao = database.calibrationDao()
    private val deviceDao = database.deviceDao()
    private val analysisDao = database.analysisDao()
    
    /**
     * Messdaten-Operationen
     */
    suspend fun saveMeasurement(measurement: MeasurementResult, sessionId: String): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val entity = MeasurementEntity(
            id = id,
            sessionId = sessionId,
            timestamp = measurement.timestamp,
            frequency = measurement.frequency,
            signalStrength = measurement.signalStrength,
            depth = measurement.depth,
            temperature = measurement.temperature,
            conductivity = 0.0, // Wird später durch Analyse gesetzt
            magneticPermeability = 1.0
        )
        measurementDao.insertMeasurement(entity)
        id
    }
    
    suspend fun getMeasurementsBySession(sessionId: String): Flow<List<MeasurementResult>> {
        return measurementDao.getMeasurementsBySession(sessionId).map { entities ->
            entities.map { entity ->
                MeasurementResult(
                    timestamp = entity.timestamp,
                    frequency = entity.frequency,
                    signalStrength = entity.signalStrength,
                    depth = entity.depth,
                    temperature = entity.temperature
                )
            }
        }
    }
    
    suspend fun getRecentMeasurements(limit: Int = 100): List<MeasurementResult> = withContext(Dispatchers.IO) {
        measurementDao.getRecentMeasurements(limit).map { entity ->
            MeasurementResult(
                timestamp = entity.timestamp,
                frequency = entity.frequency,
                signalStrength = entity.signalStrength,
                depth = entity.depth,
                temperature = entity.temperature
            )
        }
    }
    
    suspend fun getMeasurementStatistics(sessionId: String): MeasurementStatistics = withContext(Dispatchers.IO) {
        val count = measurementDao.getMeasurementCount(sessionId)
        val average = measurementDao.getAverageSignalStrength(sessionId) ?: 0.0
        val max = measurementDao.getMaxSignalStrength(sessionId) ?: 0.0
        val min = measurementDao.getMinSignalStrength(sessionId) ?: 0.0
        
        MeasurementStatistics(
            count = count,
            averageSignalStrength = average,
            maxSignalStrength = max,
            minSignalStrength = min
        )
    }
    
    suspend fun deleteMeasurementsBySession(sessionId: String) = withContext(Dispatchers.IO) {
        measurementDao.deleteMeasurementsBySession(sessionId)
    }
    
    suspend fun deleteOldMeasurements(daysOld: Int = 30) = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        measurementDao.deleteOldMeasurements(cutoffTime)
    }
    
    /**
     * Sitzungs-Operationen
     */
    suspend fun createSession(name: String, deviceId: String, location: String = ""): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val session = SessionEntity(
            id = id,
            name = name,
            startTimestamp = System.currentTimeMillis(),
            endTimestamp = null,
            deviceId = deviceId,
            location = location
        )
        sessionDao.insertSession(session)
        id
    }
    
    suspend fun endSession(sessionId: String) = withContext(Dispatchers.IO) {
        val session = sessionDao.getSessionById(sessionId)
        session?.let {
            val measurementCount = measurementDao.getMeasurementCount(sessionId)
            val updatedSession = it.copy(
                endTimestamp = System.currentTimeMillis(),
                measurementCount = measurementCount
            )
            sessionDao.updateSession(updatedSession)
        }
    }
    
    fun getAllSessions(): Flow<List<MeasurementSession>> {
        return sessionDao.getAllSessions().map { entities ->
            entities.map { entity ->
                MeasurementSession(
                    id = entity.id,
                    name = entity.name,
                    startTimestamp = entity.startTimestamp,
                    endTimestamp = entity.endTimestamp,
                    deviceId = entity.deviceId,
                    measurementCount = entity.measurementCount,
                    notes = entity.notes,
                    location = entity.location
                )
            }
        }
    }
    
    suspend fun getSessionById(sessionId: String): MeasurementSession? = withContext(Dispatchers.IO) {
        sessionDao.getSessionById(sessionId)?.let { entity ->
            MeasurementSession(
                id = entity.id,
                name = entity.name,
                startTimestamp = entity.startTimestamp,
                endTimestamp = entity.endTimestamp,
                deviceId = entity.deviceId,
                measurementCount = entity.measurementCount,
                notes = entity.notes,
                location = entity.location
            )
        }
    }
    
    suspend fun updateSessionNotes(sessionId: String, notes: String) = withContext(Dispatchers.IO) {
        val session = sessionDao.getSessionById(sessionId)
        session?.let {
            sessionDao.updateSession(it.copy(notes = notes))
        }
    }
    
    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        // Erst alle Messungen löschen
        measurementDao.deleteMeasurementsBySession(sessionId)
        // Dann die Sitzung
        sessionDao.deleteSessionById(sessionId)
    }
    
    suspend fun getActiveSessions(): List<MeasurementSession> = withContext(Dispatchers.IO) {
        sessionDao.getActiveSessions().map { entity ->
            MeasurementSession(
                id = entity.id,
                name = entity.name,
                startTimestamp = entity.startTimestamp,
                endTimestamp = entity.endTimestamp,
                deviceId = entity.deviceId,
                measurementCount = entity.measurementCount,
                notes = entity.notes,
                location = entity.location
            )
        }
    }
    
    /**
     * Kalibrierungs-Operationen
     */
    suspend fun saveCalibration(calibration: CalibrationData, deviceId: String): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val entity = CalibrationEntity(
            id = id,
            deviceId = deviceId,
            timestamp = System.currentTimeMillis(),
            frequency = calibration.frequency,
            offsetX = calibration.offsetX,
            offsetY = calibration.offsetY,
            offsetZ = calibration.offsetZ,
            gainX = calibration.gainX,
            gainY = calibration.gainY,
            gainZ = calibration.gainZ,
            temperature = calibration.temperature,
            isValid = true
        )
        
        // Alte Kalibrierungen invalidieren
        calibrationDao.invalidateCalibrations(deviceId)
        
        // Neue Kalibrierung speichern
        calibrationDao.insertCalibration(entity)
        id
    }
    
    suspend fun getLatestCalibration(deviceId: String): CalibrationData? = withContext(Dispatchers.IO) {
        calibrationDao.getLatestValidCalibration(deviceId)?.let { entity ->
            CalibrationData(
                frequency = entity.frequency,
                offsetX = entity.offsetX,
                offsetY = entity.offsetY,
                offsetZ = entity.offsetZ,
                gainX = entity.gainX,
                gainY = entity.gainY,
                gainZ = entity.gainZ,
                temperature = entity.temperature,
                timestamp = entity.timestamp
            )
        }
    }
    
    suspend fun getCalibrationHistory(deviceId: String): List<CalibrationData> = withContext(Dispatchers.IO) {
        calibrationDao.getCalibrationsByDevice(deviceId).map { entity ->
            CalibrationData(
                frequency = entity.frequency,
                offsetX = entity.offsetX,
                offsetY = entity.offsetY,
                offsetZ = entity.offsetZ,
                gainX = entity.gainX,
                gainY = entity.gainY,
                gainZ = entity.gainZ,
                temperature = entity.temperature,
                timestamp = entity.timestamp
            )
        }
    }
    
    suspend fun deleteOldCalibrations(daysOld: Int = 90) = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        calibrationDao.deleteOldCalibrations(cutoffTime)
    }
    
    /**
     * Geräte-Operationen
     */
    suspend fun saveDevice(device: DeviceInfo): String = withContext(Dispatchers.IO) {
        val entity = DeviceEntity(
            id = device.id,
            name = device.name,
            macAddress = device.macAddress,
            firmwareVersion = device.firmwareVersion,
            lastConnected = System.currentTimeMillis(),
            batteryLevel = device.batteryLevel,
            isActive = false
        )
        deviceDao.insertDevice(entity)
        device.id
    }
    
    suspend fun updateDeviceStatus(deviceId: String, batteryLevel: Int, isActive: Boolean) = withContext(Dispatchers.IO) {
        val device = deviceDao.getDeviceById(deviceId)
        device?.let {
            if (isActive) {
                deviceDao.deactivateAllDevices()
            }
            deviceDao.updateDevice(
                it.copy(
                    batteryLevel = batteryLevel,
                    isActive = isActive,
                    lastConnected = if (isActive) System.currentTimeMillis() else it.lastConnected
                )
            )
        }
    }
    
    fun getAllDevices(): Flow<List<DeviceInfo>> {
        return deviceDao.getAllDevices().map { entities ->
            entities.map { entity ->
                DeviceInfo(
                    id = entity.id,
                    name = entity.name,
                    macAddress = entity.macAddress,
                    firmwareVersion = entity.firmwareVersion,
                    batteryLevel = entity.batteryLevel,
                    isConnected = entity.isActive,
                    lastConnected = entity.lastConnected
                )
            }
        }
    }
    
    suspend fun getActiveDevice(): DeviceInfo? = withContext(Dispatchers.IO) {
        deviceDao.getActiveDevice()?.let { entity ->
            DeviceInfo(
                id = entity.id,
                name = entity.name,
                macAddress = entity.macAddress,
                firmwareVersion = entity.firmwareVersion,
                batteryLevel = entity.batteryLevel,
                isConnected = entity.isActive,
                lastConnected = entity.lastConnected
            )
        }
    }
    
    suspend fun getDeviceByMacAddress(macAddress: String): DeviceInfo? = withContext(Dispatchers.IO) {
        deviceDao.getDeviceByMacAddress(macAddress)?.let { entity ->
            DeviceInfo(
                id = entity.id,
                name = entity.name,
                macAddress = entity.macAddress,
                firmwareVersion = entity.firmwareVersion,
                batteryLevel = entity.batteryLevel,
                isConnected = entity.isActive,
                lastConnected = entity.lastConnected
            )
        }
    }
    
    /**
     * Analyse-Operationen
     */
    suspend fun saveAnalysisResult(
        measurementId: String,
        result: MaterialClassificationResult
    ): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        
        val propertiesJson = gson.toJson(result.properties)
        val recommendationsJson = gson.toJson(result.recommendations)
        
        val entity = AnalysisResultEntity(
            id = id,
            measurementId = measurementId,
            materialType = result.materialType.name,
            confidence = result.confidence,
            symmetryScore = 0.0, // Vereinfacht für Build-Kompatibilität
            hollownessScore = 0.0, // Vereinfacht für Build-Kompatibilität
            conductivity = 0.0, // Vereinfacht für Build-Kompatibilität
            particleDensity = 0.0, // Vereinfacht für Build-Kompatibilität
            properties = propertiesJson,
            recommendations = recommendationsJson,
            timestamp = System.currentTimeMillis()
        )
        
        analysisDao.insertAnalysis(entity)
        id
    }
    
    suspend fun getAnalysisByMeasurement(measurementId: String): MaterialClassificationResult? = withContext(Dispatchers.IO) {
        analysisDao.getAnalysisByMeasurement(measurementId)?.let { entity ->
            // Vereinfachte Deserialisierung für Build-Kompatibilität
            val properties: Map<String, String> = mapOf("material" to entity.materialType)
            val recommendations: List<String> = listOf("Weitere Analyse empfohlen")
            
            MaterialClassificationResult(
                materialType = MaterialType.valueOf(entity.materialType),
                confidence = entity.confidence,
                properties = properties,
                recommendations = recommendations
            )
        }
    }
    
    suspend fun getRecentAnalyses(limit: Int = 50): List<AnalysisResultEntity> = withContext(Dispatchers.IO) {
        analysisDao.getRecentAnalyses(limit)
    }
    
    suspend fun getMaterialTypeStatistics(): Map<String, Int> = withContext(Dispatchers.IO) {
        analysisDao.getMaterialTypeStatistics().associate { it.materialType to it.count }
    }
    
    suspend fun getAverageConfidenceByMaterial(materialType: String): Double = withContext(Dispatchers.IO) {
        analysisDao.getAverageConfidence(materialType) ?: 0.0
    }
    
    suspend fun deleteOldAnalyses(daysOld: Int = 60) = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        analysisDao.deleteOldAnalyses(cutoffTime)
    }
    
    /**
     * Datenbank-Wartung
     */
    suspend fun performMaintenance() = withContext(Dispatchers.IO) {
        // Alte Daten löschen
        deleteOldMeasurements(30)
        deleteOldCalibrations(90)
        deleteOldAnalyses(60)
        
        // Datenbankgröße optimieren
        database.query("VACUUM", null)
    }
    
    suspend fun getDatabaseSize(): Long = withContext(Dispatchers.IO) {
        // Datenbankgröße in Bytes
        val dbFile = database.openHelper.writableDatabase.path
        java.io.File(dbFile).length()
    }
    
    suspend fun exportData(sessionId: String): ExportData = withContext(Dispatchers.IO) {
        val session = getSessionById(sessionId)
        val measurements = measurementDao.getMeasurementsBySession(sessionId)
        
        // Vereinfachte Export-Datenstruktur
        ExportData(
            session = session,
            measurementCount = measurementDao.getMeasurementCount(sessionId),
            exportTimestamp = System.currentTimeMillis()
        )
    }
}

// Datenmodelle wurden nach com.emfad.app.models verschoben
