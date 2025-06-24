package com.emfad.app.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ForeignKey
import com.emfad.app.models.MaterialAnalysis
import com.emfad.app.models.MaterialType

/**
 * Room Entity für Material-Analysen
 * Basiert auf ursprünglichen Algorithmen aus Chat-File
 */
@Entity(
    tableName = "material_analyses",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["timestamp"]),
        Index(value = ["materialType"]),
        Index(value = ["confidence"])
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
data class MaterialAnalysisEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val sessionId: Long,
    val timestamp: Long,
    val materialType: MaterialType,
    val confidence: Double,
    
    // Physikalische Eigenschaften (ursprüngliche Algorithmen)
    val symmetryScore: Double,
    val hollownessScore: Double,
    val conductivity: Double,
    val magneticPermeability: Double,
    val signalStrength: Double,
    val depth: Double,
    val size: Double,
    val particleDensity: Double,
    
    // Kristallstruktur-Analyse
    val crystallineStructure: Boolean,
    val crystalSymmetry: String,
    val latticeParameter: Double,
    val grainSize: Double,
    val crystallineOrientation: String,
    
    // Cluster-Analyse (DBSCAN Algorithmus)
    val clusterCount: Int,
    val clusterDensity: Double,
    val clusterSeparation: Double,
    val clusterCoherence: Double,
    val clusterData: String, // JSON für Cluster-Details
    
    // Skin-Effekt Berechnung
    val skinDepth: Double,
    val impedanceReal: Double,
    val impedanceImaginary: Double,
    val frequencyResponse: String, // JSON für Frequenz-Antwort
    
    // Hohlraum-Erkennung
    val cavityDetected: Boolean,
    val cavityVolume: Double,
    val cavityDepth: Double,
    val cavityShape: String,
    val cavityOrientation: String,
    
    // Schichtanalyse
    val layerCount: Int,
    val layerThickness: String, // JSON Array
    val layerMaterials: String, // JSON Array
    val layerInterfaces: String, // JSON Array
    
    // Einschluss-Erkennung
    val inclusionCount: Int,
    val inclusionTypes: String, // JSON Array
    val inclusionSizes: String, // JSON Array
    val inclusionPositions: String, // JSON Array
    
    // Qualitätsbewertung
    val analysisQuality: Double,
    val dataCompleteness: Double,
    val measurementStability: Double,
    val noiseLevel: Double,
    val calibrationAccuracy: Double,
    
    // Metadaten
    val algorithmVersion: String,
    val processingTime: Long,
    val rawAnalysisData: String, // JSON für vollständige Rohdaten
    val notes: String = "",
    val isValidated: Boolean = false,
    val validatedBy: String = "",
    val validationTimestamp: Long? = null
) {
    /**
     * Konvertierung zu Domain Model
     */
    fun toDomainModel(): MaterialAnalysis {
        return MaterialAnalysis(
            id = id,
            sessionId = sessionId,
            timestamp = timestamp,
            materialType = materialType,
            confidence = confidence,
            symmetryScore = symmetryScore,
            hollownessScore = hollownessScore,
            conductivity = conductivity,
            magneticPermeability = magneticPermeability,
            signalStrength = signalStrength,
            depth = depth,
            size = size,
            particleDensity = particleDensity,
            crystallineStructure = crystallineStructure,
            crystalSymmetry = crystalSymmetry,
            latticeParameter = latticeParameter,
            grainSize = grainSize,
            crystallineOrientation = crystallineOrientation,
            clusterCount = clusterCount,
            clusterDensity = clusterDensity,
            clusterSeparation = clusterSeparation,
            clusterCoherence = clusterCoherence,
            clusterData = clusterData,
            skinDepth = skinDepth,
            impedanceReal = impedanceReal,
            impedanceImaginary = impedanceImaginary,
            frequencyResponse = frequencyResponse,
            cavityDetected = cavityDetected,
            cavityVolume = cavityVolume,
            cavityDepth = cavityDepth,
            cavityShape = cavityShape,
            cavityOrientation = cavityOrientation,
            layerCount = layerCount,
            layerThickness = layerThickness,
            layerMaterials = layerMaterials,
            layerInterfaces = layerInterfaces,
            inclusionCount = inclusionCount,
            inclusionTypes = inclusionTypes,
            inclusionSizes = inclusionSizes,
            inclusionPositions = inclusionPositions,
            analysisQuality = analysisQuality,
            dataCompleteness = dataCompleteness,
            measurementStability = measurementStability,
            noiseLevel = noiseLevel,
            calibrationAccuracy = calibrationAccuracy,
            algorithmVersion = algorithmVersion,
            processingTime = processingTime,
            rawAnalysisData = rawAnalysisData,
            notes = notes,
            isValidated = isValidated,
            validatedBy = validatedBy,
            validationTimestamp = validationTimestamp
        )
    }
    
    companion object {
        /**
         * Konvertierung von Domain Model
         */
        fun fromDomainModel(analysis: MaterialAnalysis): MaterialAnalysisEntity {
            return MaterialAnalysisEntity(
                id = analysis.id,
                sessionId = analysis.sessionId,
                timestamp = analysis.timestamp,
                materialType = analysis.materialType,
                confidence = analysis.confidence,
                symmetryScore = analysis.symmetryScore,
                hollownessScore = analysis.hollownessScore,
                conductivity = analysis.conductivity,
                magneticPermeability = analysis.magneticPermeability,
                signalStrength = analysis.signalStrength,
                depth = analysis.depth,
                size = analysis.size,
                particleDensity = analysis.particleDensity,
                crystallineStructure = analysis.crystallineStructure,
                crystalSymmetry = analysis.crystalSymmetry,
                latticeParameter = analysis.latticeParameter,
                grainSize = analysis.grainSize,
                crystallineOrientation = analysis.crystallineOrientation,
                clusterCount = analysis.clusterCount,
                clusterDensity = analysis.clusterDensity,
                clusterSeparation = analysis.clusterSeparation,
                clusterCoherence = analysis.clusterCoherence,
                clusterData = analysis.clusterData,
                skinDepth = analysis.skinDepth,
                impedanceReal = analysis.impedanceReal,
                impedanceImaginary = analysis.impedanceImaginary,
                frequencyResponse = analysis.frequencyResponse,
                cavityDetected = analysis.cavityDetected,
                cavityVolume = analysis.cavityVolume,
                cavityDepth = analysis.cavityDepth,
                cavityShape = analysis.cavityShape,
                cavityOrientation = analysis.cavityOrientation,
                layerCount = analysis.layerCount,
                layerThickness = analysis.layerThickness,
                layerMaterials = analysis.layerMaterials,
                layerInterfaces = analysis.layerInterfaces,
                inclusionCount = analysis.inclusionCount,
                inclusionTypes = analysis.inclusionTypes,
                inclusionSizes = analysis.inclusionSizes,
                inclusionPositions = analysis.inclusionPositions,
                analysisQuality = analysis.analysisQuality,
                dataCompleteness = analysis.dataCompleteness,
                measurementStability = analysis.measurementStability,
                noiseLevel = analysis.noiseLevel,
                calibrationAccuracy = analysis.calibrationAccuracy,
                algorithmVersion = analysis.algorithmVersion,
                processingTime = analysis.processingTime,
                rawAnalysisData = analysis.rawAnalysisData,
                notes = analysis.notes,
                isValidated = analysis.isValidated,
                validatedBy = analysis.validatedBy,
                validationTimestamp = analysis.validationTimestamp
            )
        }
    }
}
