package com.emfad.app.database.dao

import androidx.room.*
import com.emfad.app.database.entities.EMFReadingEntity
import com.emfad.app.models.MaterialType
import kotlinx.coroutines.flow.Flow

/**
 * DAO für EMF-Messungen
 * Samsung S21 Ultra optimiert mit allen ursprünglichen Algorithmen
 */
@Dao
interface EMFReadingDao {
    
    // Basis CRUD Operationen
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: EMFReadingEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readings: List<EMFReadingEntity>): List<Long>
    
    @Update
    suspend fun update(reading: EMFReadingEntity)
    
    @Delete
    suspend fun delete(reading: EMFReadingEntity)
    
    @Query("DELETE FROM emf_readings WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM emf_readings WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: Long)
    
    @Query("DELETE FROM emf_readings")
    suspend fun deleteAll()
    
    // Abfragen für einzelne Messungen
    @Query("SELECT * FROM emf_readings WHERE id = :id")
    suspend fun getById(id: Long): EMFReadingEntity?
    
    @Query("SELECT * FROM emf_readings WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<EMFReadingEntity?>
    
    // Abfragen für Session-basierte Messungen
    @Query("SELECT * FROM emf_readings WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getBySessionId(sessionId: Long): List<EMFReadingEntity>
    
    @Query("SELECT * FROM emf_readings WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getBySessionIdFlow(sessionId: Long): Flow<List<EMFReadingEntity>>
    
    @Query("SELECT * FROM emf_readings WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLatestBySessionId(sessionId: Long, limit: Int): List<EMFReadingEntity>
    
    // Zeitbasierte Abfragen
    @Query("SELECT * FROM emf_readings WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    suspend fun getByTimeRange(startTime: Long, endTime: Long): List<EMFReadingEntity>
    
    @Query("SELECT * FROM emf_readings WHERE timestamp >= :timestamp ORDER BY timestamp ASC")
    suspend fun getAfterTimestamp(timestamp: Long): List<EMFReadingEntity>
    
    @Query("SELECT * FROM emf_readings ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getLatest(limit: Int): List<EMFReadingEntity>
    
    @Query("SELECT * FROM emf_readings ORDER BY timestamp DESC LIMIT :limit")
    fun getLatestFlow(limit: Int): Flow<List<EMFReadingEntity>>
    
    // Material-basierte Abfragen
    @Query("SELECT * FROM emf_readings WHERE materialType = :materialType ORDER BY timestamp DESC")
    suspend fun getByMaterialType(materialType: MaterialType): List<EMFReadingEntity>
    
    @Query("SELECT * FROM emf_readings WHERE materialType = :materialType AND confidence >= :minConfidence ORDER BY confidence DESC")
    suspend fun getByMaterialTypeWithConfidence(materialType: MaterialType, minConfidence: Double): List<EMFReadingEntity>
    
    // Qualitäts-basierte Abfragen
    @Query("SELECT * FROM emf_readings WHERE qualityScore >= :minQuality ORDER BY qualityScore DESC")
    suspend fun getHighQualityReadings(minQuality: Double): List<EMFReadingEntity>
    
    @Query("SELECT * FROM emf_readings WHERE isValidated = 1 ORDER BY timestamp DESC")
    suspend fun getValidatedReadings(): List<EMFReadingEntity>
    
    @Query("SELECT * FROM emf_readings WHERE isValidated = 0 ORDER BY timestamp DESC")
    suspend fun getUnvalidatedReadings(): List<EMFReadingEntity>
    
    // Frequenz-basierte Abfragen
    @Query("SELECT * FROM emf_readings WHERE frequency BETWEEN :minFreq AND :maxFreq ORDER BY frequency ASC")
    suspend fun getByFrequencyRange(minFreq: Double, maxFreq: Double): List<EMFReadingEntity>
    
    @Query("SELECT DISTINCT frequency FROM emf_readings ORDER BY frequency ASC")
    suspend fun getDistinctFrequencies(): List<Double>
    
    // Signal-Stärke basierte Abfragen
    @Query("SELECT * FROM emf_readings WHERE signalStrength >= :minStrength ORDER BY signalStrength DESC")
    suspend fun getByMinSignalStrength(minStrength: Double): List<EMFReadingEntity>
    
    @Query("SELECT * FROM emf_readings WHERE signalStrength BETWEEN :minStrength AND :maxStrength ORDER BY signalStrength DESC")
    suspend fun getBySignalStrengthRange(minStrength: Double, maxStrength: Double): List<EMFReadingEntity>
    
    // Geräte-basierte Abfragen
    @Query("SELECT * FROM emf_readings WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    suspend fun getByDeviceId(deviceId: String): List<EMFReadingEntity>
    
    @Query("SELECT DISTINCT deviceId FROM emf_readings")
    suspend fun getDistinctDeviceIds(): List<String>
    
    // Statistik-Abfragen
    @Query("SELECT COUNT(*) FROM emf_readings")
    suspend fun getCount(): Int
    
    @Query("SELECT COUNT(*) FROM emf_readings WHERE sessionId = :sessionId")
    suspend fun getCountBySessionId(sessionId: Long): Int
    
    @Query("SELECT AVG(signalStrength) FROM emf_readings WHERE sessionId = :sessionId")
    suspend fun getAverageSignalStrength(sessionId: Long): Double?
    
    @Query("SELECT MAX(signalStrength) FROM emf_readings WHERE sessionId = :sessionId")
    suspend fun getMaxSignalStrength(sessionId: Long): Double?
    
    @Query("SELECT MIN(signalStrength) FROM emf_readings WHERE sessionId = :sessionId")
    suspend fun getMinSignalStrength(sessionId: Long): Double?
    
    @Query("SELECT AVG(temperature) FROM emf_readings WHERE sessionId = :sessionId")
    suspend fun getAverageTemperature(sessionId: Long): Double?
    
    @Query("SELECT COUNT(*) FROM emf_readings WHERE materialType = :materialType")
    suspend fun getCountByMaterialType(materialType: MaterialType): Int
    
    // Erweiterte Analyse-Abfragen
    @Query("""
        SELECT * FROM emf_readings 
        WHERE sessionId = :sessionId 
        AND signalStrength BETWEEN :minStrength AND :maxStrength
        AND frequency BETWEEN :minFreq AND :maxFreq
        ORDER BY timestamp ASC
    """)
    suspend fun getFilteredReadings(
        sessionId: Long,
        minStrength: Double,
        maxStrength: Double,
        minFreq: Double,
        maxFreq: Double
    ): List<EMFReadingEntity>
    
    @Query("""
        SELECT * FROM emf_readings 
        WHERE depth BETWEEN :minDepth AND :maxDepth
        AND materialType = :materialType
        ORDER BY depth ASC
    """)
    suspend fun getByDepthAndMaterial(
        minDepth: Double,
        maxDepth: Double,
        materialType: MaterialType
    ): List<EMFReadingEntity>
    
    // Position-basierte Abfragen (für AR)
    @Query("""
        SELECT * FROM emf_readings 
        WHERE positionX BETWEEN :minX AND :maxX
        AND positionY BETWEEN :minY AND :maxY
        AND positionZ BETWEEN :minZ AND :maxZ
        ORDER BY timestamp ASC
    """)
    suspend fun getByPositionRange(
        minX: Double, maxX: Double,
        minY: Double, maxY: Double,
        minZ: Double, maxZ: Double
    ): List<EMFReadingEntity>
    
    // Datenbereinigung
    @Query("DELETE FROM emf_readings WHERE timestamp < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long): Int
    
    @Query("DELETE FROM emf_readings WHERE qualityScore < :minQuality")
    suspend fun deleteLowQualityReadings(minQuality: Double): Int
    
    // Batch-Operationen
    @Query("UPDATE emf_readings SET isValidated = 1 WHERE sessionId = :sessionId")
    suspend fun markSessionAsValidated(sessionId: Long)
    
    @Query("UPDATE emf_readings SET qualityScore = :quality WHERE sessionId = :sessionId")
    suspend fun updateSessionQuality(sessionId: Long, quality: Double)
    
    // Suche in Notizen
    @Query("SELECT * FROM emf_readings WHERE notes LIKE '%' || :searchTerm || '%' ORDER BY timestamp DESC")
    suspend fun searchInNotes(searchTerm: String): List<EMFReadingEntity>
    
    // Paginierung
    @Query("SELECT * FROM emf_readings ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getReadingsPaginated(limit: Int, offset: Int): List<EMFReadingEntity>
}
