package com.emfad.app.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ForeignKey
import com.emfad.app.models.EMFReading
import com.emfad.app.models.MaterialType

/**
 * Room Entity für EMF-Messungen
 * Samsung S21 Ultra optimiert mit allen ursprünglichen Algorithmen
 */
@Entity(
    tableName = "emf_readings",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["timestamp"]),
        Index(value = ["materialType"]),
        Index(value = ["frequency"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = MeasurementSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EMFReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val sessionId: Long,
    val timestamp: Long,
    val frequency: Double,
    val signalStrength: Double,
    val phase: Double,
    val amplitude: Double,
    val realPart: Double,
    val imaginaryPart: Double,
    val magnitude: Double,
    val depth: Double,
    val temperature: Double,
    val humidity: Double,
    val pressure: Double,
    val batteryLevel: Int,
    val deviceId: String,
    val materialType: MaterialType,
    val confidence: Double,
    val noiseLevel: Double,
    val calibrationOffset: Double,
    val gainSetting: Double,
    val filterSetting: String,
    val measurementMode: String,
    val positionX: Double = 0.0,
    val positionY: Double = 0.0,
    val positionZ: Double = 0.0,
    val orientationX: Double = 0.0,
    val orientationY: Double = 0.0,
    val orientationZ: Double = 0.0,
    val qualityScore: Double = 1.0,
    val isValidated: Boolean = false,
    val notes: String = "",
    val rawData: String = "" // JSON für zusätzliche Rohdaten
) {
    /**
     * Konvertierung zu Domain Model
     */
    fun toDomainModel(): EMFReading {
        return EMFReading(
            id = id,
            sessionId = sessionId,
            timestamp = timestamp,
            frequency = frequency,
            signalStrength = signalStrength,
            phase = phase,
            amplitude = amplitude,
            realPart = realPart,
            imaginaryPart = imaginaryPart,
            magnitude = magnitude,
            depth = depth,
            temperature = temperature,
            humidity = humidity,
            pressure = pressure,
            batteryLevel = batteryLevel,
            deviceId = deviceId,
            materialType = materialType,
            confidence = confidence,
            noiseLevel = noiseLevel,
            calibrationOffset = calibrationOffset,
            gainSetting = gainSetting,
            filterSetting = filterSetting,
            measurementMode = measurementMode,
            positionX = positionX,
            positionY = positionY,
            positionZ = positionZ,
            orientationX = orientationX,
            orientationY = orientationY,
            orientationZ = orientationZ,
            qualityScore = qualityScore,
            isValidated = isValidated,
            notes = notes,
            rawData = rawData
        )
    }
    
    companion object {
        /**
         * Konvertierung von Domain Model
         */
        fun fromDomainModel(reading: EMFReading): EMFReadingEntity {
            return EMFReadingEntity(
                id = reading.id,
                sessionId = reading.sessionId,
                timestamp = reading.timestamp,
                frequency = reading.frequency,
                signalStrength = reading.signalStrength,
                phase = reading.phase,
                amplitude = reading.amplitude,
                realPart = reading.realPart,
                imaginaryPart = reading.imaginaryPart,
                magnitude = reading.magnitude,
                depth = reading.depth,
                temperature = reading.temperature,
                humidity = reading.humidity,
                pressure = reading.pressure,
                batteryLevel = reading.batteryLevel,
                deviceId = reading.deviceId,
                materialType = reading.materialType,
                confidence = reading.confidence,
                noiseLevel = reading.noiseLevel,
                calibrationOffset = reading.calibrationOffset,
                gainSetting = reading.gainSetting,
                filterSetting = reading.filterSetting,
                measurementMode = reading.measurementMode,
                positionX = reading.positionX,
                positionY = reading.positionY,
                positionZ = reading.positionZ,
                orientationX = reading.orientationX,
                orientationY = reading.orientationY,
                orientationZ = reading.orientationZ,
                qualityScore = reading.qualityScore,
                isValidated = reading.isValidated,
                notes = reading.notes,
                rawData = reading.rawData
            )
        }
    }
}
