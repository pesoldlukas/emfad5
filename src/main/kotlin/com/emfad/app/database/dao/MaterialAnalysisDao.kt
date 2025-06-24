package com.emfad.app.database.dao

import androidx.room.*
import com.emfad.app.database.entities.MaterialAnalysisEntity
import com.emfad.app.models.MaterialType
import kotlinx.coroutines.flow.Flow

/**
 * DAO für Material-Analysen
 * Basiert auf ursprünglichen Algorithmen aus Chat-File
 */
@Dao
interface MaterialAnalysisDao {
    
    // Basis CRUD Operationen
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(analysis: MaterialAnalysisEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(analyses: List<MaterialAnalysisEntity>): List<Long>
    
    @Update
    suspend fun update(analysis: MaterialAnalysisEntity)
    
    @Delete
    suspend fun delete(analysis: MaterialAnalysisEntity)
    
    @Query("DELETE FROM material_analyses WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM material_analyses WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: Long)
    
    @Query("DELETE FROM material_analyses")
    suspend fun deleteAll()
    
    // Abfragen für einzelne Analysen
    @Query("SELECT * FROM material_analyses WHERE id = :id")
    suspend fun getById(id: Long): MaterialAnalysisEntity?
    
    @Query("SELECT * FROM material_analyses WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<MaterialAnalysisEntity?>
    
    // Session-basierte Abfragen
    @Query("SELECT * FROM material_analyses WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    suspend fun getBySessionId(sessionId: Long): List<MaterialAnalysisEntity>
    
    @Query("SELECT * FROM material_analyses WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getBySessionIdFlow(sessionId: Long): Flow<List<MaterialAnalysisEntity>>
    
    @Query("SELECT * FROM material_analyses WHERE sessionId = :sessionId ORDER BY confidence DESC LIMIT 1")
    suspend fun getBestAnalysisForSession(sessionId: Long): MaterialAnalysisEntity?
    
    // Material-basierte Abfragen
    @Query("SELECT * FROM material_analyses WHERE materialType = :materialType ORDER BY confidence DESC")
    suspend fun getByMaterialType(materialType: MaterialType): List<MaterialAnalysisEntity>
    
    @Query("SELECT * FROM material_analyses WHERE materialType = :materialType AND confidence >= :minConfidence ORDER BY confidence DESC")
    suspend fun getByMaterialTypeWithConfidence(materialType: MaterialType, minConfidence: Double): List<MaterialAnalysisEntity>
    
    @Query("SELECT DISTINCT materialType FROM material_analyses")
    suspend fun getDistinctMaterialTypes(): List<MaterialType>
    
    // Konfidenz-basierte Abfragen
    @Query("SELECT * FROM material_analyses WHERE confidence >= :minConfidence ORDER BY confidence DESC")
    suspend fun getHighConfidenceAnalyses(minConfidence: Double): List<MaterialAnalysisEntity>
    
    @Query("SELECT * FROM material_analyses WHERE confidence < :maxConfidence ORDER BY confidence ASC")
    suspend fun getLowConfidenceAnalyses(maxConfidence: Double): List<MaterialAnalysisEntity>
    
    // Kristallstruktur-Analysen (ursprünglicher Algorithmus)
    @Query("SELECT * FROM material_analyses WHERE crystallineStructure = 1 ORDER BY confidence DESC")
    suspend fun getCrystallineAnalyses(): List<MaterialAnalysisEntity>
    
    @Query("SELECT * FROM material_analyses WHERE symmetryScore >= :minSymmetry ORDER BY symmetryScore DESC")
    suspend fun getBySymmetryScore(minSymmetry: Double): List<MaterialAnalysisEntity>
    
    @Query("SELECT * FROM material_analyses WHERE crystalSymmetry = :symmetryType ORDER BY confidence DESC")
    suspend fun getByCrystalSymmetry(symmetryType: String): List<MaterialAnalysisEntity>
    
    // Hohlraum-Analysen
    @Query("SELECT * FROM material_analyses WHERE cavityDetected = 1 ORDER BY cavityVolume DESC")
    suspend fun getCavityAnalyses(): List<MaterialAnalysisEntity>
    
    @Query("SELECT * FROM material_analyses WHERE hollownessScore >= :minHollowness ORDER BY hollownessScore DESC")
    suspend fun getByHollownessScore(minHollowness: Double): List<MaterialAnalysisEntity>
    
    @Query("SELECT * FROM material_analyses WHERE cavityVolume BETWEEN :minVolume AND :maxVolume ORDER BY cavityVolume DESC")
    suspend fun getByCavityVolumeRange(minVolume: Double, maxVolume: Double): List<MaterialAnalysisEntity>
    
    // Leitfähigkeits-Analysen
    @Query("SELECT * FROM material_analyses WHERE conductivity >= :minConductivity ORDER BY conductivity DESC")
    suspend fun getByConductivity(minConductivity: Double): List<MaterialAnalysisEntity>
    
    @Query("SELECT * FROM material_analyses WHERE conductivity BETWEEN :minCond AND :maxCond ORDER BY conductivity DESC")
    suspend fun getByConductivityRange(minCond: Double, maxCond: Double): List<MaterialAnalysisEntity>
    
    // Cluster-Analysen (DBSCAN Algorithmus)
    @Query("SELECT * FROM material_analyses WHERE clusterCount >= :minClusters ORDER BY clusterCount DESC")
    suspend fun getByClusterCount(minClusters: Int): List<MaterialAnalysisEntity>
    
    @Query("SELECT * FROM material_analyses WHERE clusterDensity >= :minDensity ORDER BY clusterDensity DESC")
    suspend fun getByClusterDensity(minDensity: Double): List<MaterialAnalysisEntity>
    
    @Query("SELECT * FROM material_analyses WHERE clusterCoherence >= :minCoherence ORDER BY clusterCoherence DESC")
    suspend fun getByClusterCoherence(minCoherence: Double): List<MaterialAnalysisEntity>
    
    // Skin-Effekt Analysen
    @Query("SELECT * FROM material_analyses WHERE skinDepth BETWEEN :minDepth AND :maxDepth ORDER BY skinDepth ASC")
    suspend fun getBySkinDepthRange(minDepth: Double, maxDepth: Double): List<MaterialAnalysisEntity>
    
    @Query("SELECT * FROM material_analyses WHERE impedanceReal != 0 OR impedanceImaginary != 0 ORDER BY timestamp DESC")
    suspend fun getWithImpedanceData(): List<MaterialAnalysisEntity>
    
    // Schicht-Analysen
    @Query("SELECT * FROM material_analyses WHERE layerCount >= :minLayers ORDER BY layerCount DESC")
    suspend fun getByLayerCount(minLayers: Int): List<MaterialAnalysisEntity>
    
    @Query("SELECT * FROM material_analyses WHERE layerCount > 1 ORDER BY layerCount DESC")
    suspend fun getMultiLayerAnalyses(): List<MaterialAnalysisEntity>
    
    // Einschluss-Analysen
    @Query("SELECT * FROM material_analyses WHERE inclusionCount > 0 ORDER BY inclusionCount DESC")
    suspend fun getWithInclusions(): List<MaterialAnalysisEntity>
    
    @Query("SELECT * FROM material_analyses WHERE inclusionCount >= :minInclusions ORDER BY inclusionCount DESC")
    suspend fun getByInclusionCount(minInclusions: Int): List<MaterialAnalysisEntity>
    
    // Qualitäts-basierte Abfragen
    @Query("SELECT * FROM material_analyses WHERE analysisQuality >= :minQuality ORDER BY analysisQuality DESC")
    suspend fun getHighQualityAnalyses(minQuality: Double): List<MaterialAnalysisEntity>
    
    @Query("SELECT * FROM material_analyses WHERE dataCompleteness >= :minCompleteness ORDER BY dataCompleteness DESC")
    suspend fun getCompleteAnalyses(minCompleteness: Double): List<MaterialAnalysisEntity>
    
    @Query("SELECT * FROM material_analyses WHERE measurementStability >= :minStability ORDER BY measurementStability DESC")
    suspend fun getStableAnalyses(minStability: Double): List<MaterialAnalysisEntity>
    
    @Query("SELECT * FROM material_analyses WHERE noiseLevel <= :maxNoise ORDER BY noiseLevel ASC")
    suspend fun getLowNoiseAnalyses(maxNoise: Double): List<MaterialAnalysisEntity>
    
    // Validierungs-Abfragen
    @Query("SELECT * FROM material_analyses WHERE isValidated = 1 ORDER BY validationTimestamp DESC")
    suspend fun getValidatedAnalyses(): List<MaterialAnalysisEntity>
    
    @Query("SELECT * FROM material_analyses WHERE isValidated = 0 ORDER BY timestamp DESC")
    suspend fun getUnvalidatedAnalyses(): List<MaterialAnalysisEntity>
    
    @Query("SELECT * FROM material_analyses WHERE validatedBy = :validator ORDER BY validationTimestamp DESC")
    suspend fun getByValidator(validator: String): List<MaterialAnalysisEntity>
    
    // Zeitbasierte Abfragen
    @Query("SELECT * FROM material_analyses WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getByTimeRange(startTime: Long, endTime: Long): List<MaterialAnalysisEntity>
    
    @Query("SELECT * FROM material_analyses ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLatest(limit: Int): List<MaterialAnalysisEntity>
    
    @Query("SELECT * FROM material_analyses ORDER BY timestamp DESC LIMIT :limit")
    fun getLatestFlow(limit: Int): Flow<List<MaterialAnalysisEntity>>
    
    // Statistik-Abfragen
    @Query("SELECT COUNT(*) FROM material_analyses")
    suspend fun getCount(): Int
    
    @Query("SELECT COUNT(*) FROM material_analyses WHERE materialType = :materialType")
    suspend fun getCountByMaterialType(materialType: MaterialType): Int
    
    @Query("SELECT AVG(confidence) FROM material_analyses WHERE materialType = :materialType")
    suspend fun getAverageConfidence(materialType: MaterialType): Double?
    
    @Query("SELECT AVG(symmetryScore) FROM material_analyses WHERE crystallineStructure = 1")
    suspend fun getAverageSymmetryScore(): Double?
    
    @Query("SELECT AVG(conductivity) FROM material_analyses")
    suspend fun getAverageConductivity(): Double?
    
    @Query("SELECT COUNT(*) FROM material_analyses WHERE cavityDetected = 1")
    suspend fun getCavityDetectionCount(): Int
    
    @Query("SELECT AVG(clusterCount) FROM material_analyses WHERE clusterCount > 0")
    suspend fun getAverageClusterCount(): Double?
    
    // Erweiterte Analysen
    @Query("""
        SELECT * FROM material_analyses 
        WHERE materialType = :materialType 
        AND confidence >= :minConfidence
        AND analysisQuality >= :minQuality
        ORDER BY confidence DESC, analysisQuality DESC
    """)
    suspend fun getHighQualityByMaterial(
        materialType: MaterialType,
        minConfidence: Double,
        minQuality: Double
    ): List<MaterialAnalysisEntity>
    
    @Query("""
        SELECT * FROM material_analyses 
        WHERE symmetryScore >= :minSymmetry
        AND hollownessScore <= :maxHollowness
        AND conductivity BETWEEN :minCond AND :maxCond
        ORDER BY confidence DESC
    """)
    suspend fun getByPhysicalProperties(
        minSymmetry: Double,
        maxHollowness: Double,
        minCond: Double,
        maxCond: Double
    ): List<MaterialAnalysisEntity>
    
    // Updates
    @Query("UPDATE material_analyses SET isValidated = 1, validatedBy = :validator, validationTimestamp = :timestamp WHERE id = :id")
    suspend fun markAsValidated(id: Long, validator: String, timestamp: Long)
    
    @Query("UPDATE material_analyses SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String)
    
    @Query("UPDATE material_analyses SET confidence = :confidence WHERE id = :id")
    suspend fun updateConfidence(id: Long, confidence: Double)
    
    // Datenbereinigung
    @Query("DELETE FROM material_analyses WHERE timestamp < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long): Int
    
    @Query("DELETE FROM material_analyses WHERE confidence < :minConfidence")
    suspend fun deleteLowConfidenceAnalyses(minConfidence: Double): Int
    
    @Query("DELETE FROM material_analyses WHERE analysisQuality < :minQuality")
    suspend fun deleteLowQualityAnalyses(minQuality: Double): Int
    
    // Suche
    @Query("SELECT * FROM material_analyses WHERE notes LIKE '%' || :searchTerm || '%' ORDER BY timestamp DESC")
    suspend fun searchInNotes(searchTerm: String): List<MaterialAnalysisEntity>
    
    @Query("SELECT * FROM material_analyses WHERE algorithmVersion = :version ORDER BY timestamp DESC")
    suspend fun getByAlgorithmVersion(version: String): List<MaterialAnalysisEntity>
    
    // Paginierung
    @Query("SELECT * FROM material_analyses ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getAnalysesPaginated(limit: Int, offset: Int): List<MaterialAnalysisEntity>
}
