package com.emfad.app.database.dao

import androidx.room.*
import com.emfad.app.database.entities.MeasurementSessionEntity
import com.emfad.app.models.SessionStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO für Messsitzungen
 * Samsung S21 Ultra optimiert
 */
@Dao
interface MeasurementSessionDao {
    
    // Basis CRUD Operationen
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: MeasurementSessionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<MeasurementSessionEntity>): List<Long>
    
    @Update
    suspend fun update(session: MeasurementSessionEntity)
    
    @Delete
    suspend fun delete(session: MeasurementSessionEntity)
    
    @Query("DELETE FROM measurement_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM measurement_sessions")
    suspend fun deleteAll()
    
    // Abfragen für einzelne Sessions
    @Query("SELECT * FROM measurement_sessions WHERE id = :id")
    suspend fun getById(id: Long): MeasurementSessionEntity?
    
    @Query("SELECT * FROM measurement_sessions WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<MeasurementSessionEntity?>
    
    // Alle Sessions
    @Query("SELECT * FROM measurement_sessions ORDER BY startTimestamp DESC")
    suspend fun getAll(): List<MeasurementSessionEntity>
    
    @Query("SELECT * FROM measurement_sessions ORDER BY startTimestamp DESC")
    fun getAllFlow(): Flow<List<MeasurementSessionEntity>>
    
    // Status-basierte Abfragen
    @Query("SELECT * FROM measurement_sessions WHERE status = :status ORDER BY startTimestamp DESC")
    suspend fun getByStatus(status: SessionStatus): List<MeasurementSessionEntity>
    
    @Query("SELECT * FROM measurement_sessions WHERE status = :status ORDER BY startTimestamp DESC")
    fun getByStatusFlow(status: SessionStatus): Flow<List<MeasurementSessionEntity>>
    
    @Query("SELECT * FROM measurement_sessions WHERE status IN (:statuses) ORDER BY startTimestamp DESC")
    suspend fun getByStatuses(statuses: List<SessionStatus>): List<MeasurementSessionEntity>
    
    // Aktive Sessions
    @Query("SELECT * FROM measurement_sessions WHERE status = 'ACTIVE' ORDER BY startTimestamp DESC")
    suspend fun getActiveSessions(): List<MeasurementSessionEntity>
    
    @Query("SELECT * FROM measurement_sessions WHERE status = 'ACTIVE' ORDER BY startTimestamp DESC")
    fun getActiveSessionsFlow(): Flow<List<MeasurementSessionEntity>>
    
    // Zeitbasierte Abfragen
    @Query("SELECT * FROM measurement_sessions WHERE startTimestamp BETWEEN :startTime AND :endTime ORDER BY startTimestamp DESC")
    suspend fun getByTimeRange(startTime: Long, endTime: Long): List<MeasurementSessionEntity>
    
    @Query("SELECT * FROM measurement_sessions WHERE startTimestamp >= :timestamp ORDER BY startTimestamp DESC")
    suspend fun getAfterTimestamp(timestamp: Long): List<MeasurementSessionEntity>
    
    @Query("SELECT * FROM measurement_sessions ORDER BY startTimestamp DESC LIMIT :limit")
    suspend fun getLatest(limit: Int): List<MeasurementSessionEntity>
    
    @Query("SELECT * FROM measurement_sessions ORDER BY startTimestamp DESC LIMIT :limit")
    fun getLatestFlow(limit: Int): Flow<List<MeasurementSessionEntity>>
    
    // Geräte-basierte Abfragen
    @Query("SELECT * FROM measurement_sessions WHERE deviceId = :deviceId ORDER BY startTimestamp DESC")
    suspend fun getByDeviceId(deviceId: String): List<MeasurementSessionEntity>
    
    @Query("SELECT DISTINCT deviceId FROM measurement_sessions")
    suspend fun getDistinctDeviceIds(): List<String>
    
    @Query("SELECT DISTINCT deviceName FROM measurement_sessions")
    suspend fun getDistinctDeviceNames(): List<String>
    
    // Projekt-basierte Abfragen
    @Query("SELECT * FROM measurement_sessions WHERE projectName = :projectName ORDER BY startTimestamp DESC")
    suspend fun getByProjectName(projectName: String): List<MeasurementSessionEntity>
    
    @Query("SELECT DISTINCT projectName FROM measurement_sessions WHERE projectName != '' ORDER BY projectName ASC")
    suspend fun getDistinctProjectNames(): List<String>
    
    // Operator-basierte Abfragen
    @Query("SELECT * FROM measurement_sessions WHERE operatorName = :operatorName ORDER BY startTimestamp DESC")
    suspend fun getByOperatorName(operatorName: String): List<MeasurementSessionEntity>
    
    @Query("SELECT DISTINCT operatorName FROM measurement_sessions WHERE operatorName != '' ORDER BY operatorName ASC")
    suspend fun getDistinctOperatorNames(): List<String>
    
    // Standort-basierte Abfragen
    @Query("SELECT * FROM measurement_sessions WHERE location LIKE '%' || :location || '%' ORDER BY startTimestamp DESC")
    suspend fun getByLocation(location: String): List<MeasurementSessionEntity>
    
    @Query("SELECT DISTINCT location FROM measurement_sessions WHERE location != '' ORDER BY location ASC")
    suspend fun getDistinctLocations(): List<String>
    
    // Export-Status Abfragen
    @Query("SELECT * FROM measurement_sessions WHERE isExported = 1 ORDER BY exportTimestamp DESC")
    suspend fun getExportedSessions(): List<MeasurementSessionEntity>
    
    @Query("SELECT * FROM measurement_sessions WHERE isExported = 0 ORDER BY startTimestamp DESC")
    suspend fun getUnexportedSessions(): List<MeasurementSessionEntity>
    
    @Query("SELECT * FROM measurement_sessions WHERE isExported = 0 ORDER BY startTimestamp DESC")
    fun getUnexportedSessionsFlow(): Flow<List<MeasurementSessionEntity>>
    
    // Qualitäts-basierte Abfragen
    @Query("SELECT * FROM measurement_sessions WHERE qualityRating >= :minRating ORDER BY qualityRating DESC")
    suspend fun getByMinQualityRating(minRating: Int): List<MeasurementSessionEntity>
    
    @Query("SELECT * FROM measurement_sessions WHERE dataIntegrity = 1 ORDER BY startTimestamp DESC")
    suspend fun getSessionsWithDataIntegrity(): List<MeasurementSessionEntity>
    
    // Statistik-Abfragen
    @Query("SELECT COUNT(*) FROM measurement_sessions")
    suspend fun getCount(): Int
    
    @Query("SELECT COUNT(*) FROM measurement_sessions WHERE status = :status")
    suspend fun getCountByStatus(status: SessionStatus): Int
    
    @Query("SELECT COUNT(*) FROM measurement_sessions WHERE deviceId = :deviceId")
    suspend fun getCountByDeviceId(deviceId: String): Int
    
    @Query("SELECT AVG(measurementCount) FROM measurement_sessions WHERE status = 'COMPLETED'")
    suspend fun getAverageMeasurementCount(): Double?
    
    @Query("SELECT SUM(measurementCount) FROM measurement_sessions")
    suspend fun getTotalMeasurementCount(): Int?
    
    @Query("SELECT AVG(averageSignalStrength) FROM measurement_sessions WHERE status = 'COMPLETED'")
    suspend fun getOverallAverageSignalStrength(): Double?
    
    // Suche
    @Query("""
        SELECT * FROM measurement_sessions 
        WHERE name LIKE '%' || :searchTerm || '%' 
        OR description LIKE '%' || :searchTerm || '%'
        OR notes LIKE '%' || :searchTerm || '%'
        OR tags LIKE '%' || :searchTerm || '%'
        ORDER BY startTimestamp DESC
    """)
    suspend fun search(searchTerm: String): List<MeasurementSessionEntity>
    
    @Query("SELECT * FROM measurement_sessions WHERE tags LIKE '%' || :tag || '%' ORDER BY startTimestamp DESC")
    suspend fun getByTag(tag: String): List<MeasurementSessionEntity>
    
    // Session-Updates
    @Query("UPDATE measurement_sessions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: SessionStatus)
    
    @Query("UPDATE measurement_sessions SET endTimestamp = :endTime WHERE id = :id")
    suspend fun updateEndTimestamp(id: Long, endTime: Long)
    
    @Query("UPDATE measurement_sessions SET measurementCount = :count WHERE id = :id")
    suspend fun updateMeasurementCount(id: Long, count: Int)
    
    @Query("""
        UPDATE measurement_sessions 
        SET averageSignalStrength = :avgSignal,
            maxSignalStrength = :maxSignal,
            minSignalStrength = :minSignal,
            averageTemperature = :avgTemp
        WHERE id = :id
    """)
    suspend fun updateStatistics(
        id: Long,
        avgSignal: Double,
        maxSignal: Double,
        minSignal: Double,
        avgTemp: Double
    )
    
    @Query("UPDATE measurement_sessions SET isExported = 1, exportTimestamp = :timestamp, exportPath = :path WHERE id = :id")
    suspend fun markAsExported(id: Long, timestamp: Long, path: String)
    
    @Query("UPDATE measurement_sessions SET qualityRating = :rating WHERE id = :id")
    suspend fun updateQualityRating(id: Long, rating: Int)
    
    @Query("UPDATE measurement_sessions SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String)
    
    @Query("UPDATE measurement_sessions SET tags = :tags WHERE id = :id")
    suspend fun updateTags(id: Long, tags: String)
    
    // Datenbereinigung
    @Query("DELETE FROM measurement_sessions WHERE startTimestamp < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long): Int
    
    @Query("DELETE FROM measurement_sessions WHERE status = 'CANCELLED' AND startTimestamp < :cutoffTime")
    suspend fun deleteCancelledOlderThan(cutoffTime: Long): Int
    
    @Query("DELETE FROM measurement_sessions WHERE measurementCount = 0 AND status != 'ACTIVE'")
    suspend fun deleteEmptySessions(): Int
    
    // Batch-Operationen
    @Query("UPDATE measurement_sessions SET isExported = 0 WHERE id IN (:sessionIds)")
    suspend fun markAsUnexported(sessionIds: List<Long>)
    
    @Query("UPDATE measurement_sessions SET status = :status WHERE id IN (:sessionIds)")
    suspend fun updateStatusBatch(sessionIds: List<Long>, status: SessionStatus)
    
    // Paginierung
    @Query("SELECT * FROM measurement_sessions ORDER BY startTimestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getSessionsPaginated(limit: Int, offset: Int): List<MeasurementSessionEntity>
    
    // Erweiterte Abfragen
    @Query("""
        SELECT * FROM measurement_sessions 
        WHERE measurementCount >= :minCount 
        AND averageSignalStrength >= :minSignal
        AND status = 'COMPLETED'
        ORDER BY qualityRating DESC, startTimestamp DESC
    """)
    suspend fun getHighQualitySessions(minCount: Int, minSignal: Double): List<MeasurementSessionEntity>
    
    @Query("""
        SELECT * FROM measurement_sessions 
        WHERE startTimestamp BETWEEN :startTime AND :endTime
        AND deviceId = :deviceId
        AND status IN (:statuses)
        ORDER BY startTimestamp DESC
    """)
    suspend fun getFilteredSessions(
        startTime: Long,
        endTime: Long,
        deviceId: String,
        statuses: List<SessionStatus>
    ): List<MeasurementSessionEntity>
}
