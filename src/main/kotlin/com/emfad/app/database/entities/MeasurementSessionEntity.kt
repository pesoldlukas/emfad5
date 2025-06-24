package com.emfad.app.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import com.emfad.app.models.MeasurementSession
import com.emfad.app.models.SessionStatus

/**
 * Room Entity für Messsitzungen
 * Samsung S21 Ultra optimiert
 */
@Entity(
    tableName = "measurement_sessions",
    indices = [
        Index(value = ["startTimestamp"]),
        Index(value = ["endTimestamp"]),
        Index(value = ["status"]),
        Index(value = ["deviceId"])
    ]
)
data class MeasurementSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,
    val description: String,
    val startTimestamp: Long,
    val endTimestamp: Long?,
    val status: SessionStatus,
    val deviceId: String,
    val deviceName: String,
    val operatorName: String,
    val location: String,
    val projectName: String,
    val sampleId: String,
    val measurementCount: Int = 0,
    val averageSignalStrength: Double = 0.0,
    val maxSignalStrength: Double = 0.0,
    val minSignalStrength: Double = 0.0,
    val averageTemperature: Double = 0.0,
    val calibrationData: String = "", // JSON
    val settings: String = "", // JSON für Session-Einstellungen
    val notes: String = "",
    val tags: String = "", // Komma-getrennte Tags
    val exportPath: String = "",
    val isExported: Boolean = false,
    val exportTimestamp: Long? = null,
    val qualityRating: Int = 0, // 1-5 Sterne
    val dataIntegrity: Boolean = true,
    val compressionRatio: Double = 1.0,
    val fileSize: Long = 0,
    val checksum: String = "",
    val version: Int = 1
) {
    /**
     * Konvertierung zu Domain Model
     */
    fun toDomainModel(): MeasurementSession {
        return MeasurementSession(
            id = id,
            name = name,
            description = description,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            status = status,
            deviceId = deviceId,
            deviceName = deviceName,
            operatorName = operatorName,
            location = location,
            projectName = projectName,
            sampleId = sampleId,
            measurementCount = measurementCount,
            averageSignalStrength = averageSignalStrength,
            maxSignalStrength = maxSignalStrength,
            minSignalStrength = minSignalStrength,
            averageTemperature = averageTemperature,
            calibrationData = calibrationData,
            settings = settings,
            notes = notes,
            tags = tags,
            exportPath = exportPath,
            isExported = isExported,
            exportTimestamp = exportTimestamp,
            qualityRating = qualityRating,
            dataIntegrity = dataIntegrity,
            compressionRatio = compressionRatio,
            fileSize = fileSize,
            checksum = checksum,
            version = version
        )
    }
    
    companion object {
        /**
         * Konvertierung von Domain Model
         */
        fun fromDomainModel(session: MeasurementSession): MeasurementSessionEntity {
            return MeasurementSessionEntity(
                id = session.id,
                name = session.name,
                description = session.description,
                startTimestamp = session.startTimestamp,
                endTimestamp = session.endTimestamp,
                status = session.status,
                deviceId = session.deviceId,
                deviceName = session.deviceName,
                operatorName = session.operatorName,
                location = session.location,
                projectName = session.projectName,
                sampleId = session.sampleId,
                measurementCount = session.measurementCount,
                averageSignalStrength = session.averageSignalStrength,
                maxSignalStrength = session.maxSignalStrength,
                minSignalStrength = session.minSignalStrength,
                averageTemperature = session.averageTemperature,
                calibrationData = session.calibrationData,
                settings = session.settings,
                notes = session.notes,
                tags = session.tags,
                exportPath = session.exportPath,
                isExported = session.isExported,
                exportTimestamp = session.exportTimestamp,
                qualityRating = session.qualityRating,
                dataIntegrity = session.dataIntegrity,
                compressionRatio = session.compressionRatio,
                fileSize = session.fileSize,
                checksum = session.checksum,
                version = session.version
            )
        }
    }
}
