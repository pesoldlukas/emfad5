package com.emfad.app.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.emfad.app.database.converters.EMFADTypeConverters
import com.emfad.app.database.dao.EMFReadingDao
import com.emfad.app.database.dao.MaterialAnalysisDao
import com.emfad.app.database.dao.MeasurementSessionDao
import com.emfad.app.database.entities.EMFReadingEntity
import com.emfad.app.database.entities.MaterialAnalysisEntity
import com.emfad.app.database.entities.MeasurementSessionEntity

/**
 * EMFAD Room Database
 * Samsung S21 Ultra optimiert mit allen ursprünglichen Algorithmen
 */
@Database(
    entities = [
        MeasurementSessionEntity::class,
        EMFReadingEntity::class,
        MaterialAnalysisEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(EMFADTypeConverters::class)
abstract class EMFADDatabase : RoomDatabase() {
    
    // DAOs
    abstract fun measurementSessionDao(): MeasurementSessionDao
    abstract fun emfReadingDao(): EMFReadingDao
    abstract fun materialAnalysisDao(): MaterialAnalysisDao
    
    companion object {
        private const val DATABASE_NAME = "emfad_database"
        
        @Volatile
        private var INSTANCE: EMFADDatabase? = null
        
        /**
         * Singleton Database Instance
         */
        fun getDatabase(context: Context): EMFADDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EMFADDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_1_2)
                .addCallback(DatabaseCallback())
                .fallbackToDestructiveMigration() // Nur für Development
                .build()
                
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Database Callback für Initialisierung
         */
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                
                // Indizes für Performance-Optimierung
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS idx_emf_readings_session_timestamp 
                    ON emf_readings(sessionId, timestamp)
                """)
                
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS idx_emf_readings_material_confidence 
                    ON emf_readings(materialType, confidence)
                """)
                
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS idx_emf_readings_frequency_signal 
                    ON emf_readings(frequency, signalStrength)
                """)
                
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS idx_material_analyses_session_material 
                    ON material_analyses(sessionId, materialType)
                """)
                
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS idx_material_analyses_confidence_quality 
                    ON material_analyses(confidence, analysisQuality)
                """)
                
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS idx_sessions_status_timestamp 
                    ON measurement_sessions(status, startTimestamp)
                """)
                
                // Trigger für automatische Statistik-Updates
                db.execSQL("""
                    CREATE TRIGGER update_session_stats_insert
                    AFTER INSERT ON emf_readings
                    BEGIN
                        UPDATE measurement_sessions 
                        SET measurementCount = (
                            SELECT COUNT(*) FROM emf_readings 
                            WHERE sessionId = NEW.sessionId
                        ),
                        averageSignalStrength = (
                            SELECT AVG(signalStrength) FROM emf_readings 
                            WHERE sessionId = NEW.sessionId
                        ),
                        maxSignalStrength = (
                            SELECT MAX(signalStrength) FROM emf_readings 
                            WHERE sessionId = NEW.sessionId
                        ),
                        minSignalStrength = (
                            SELECT MIN(signalStrength) FROM emf_readings 
                            WHERE sessionId = NEW.sessionId
                        ),
                        averageTemperature = (
                            SELECT AVG(temperature) FROM emf_readings 
                            WHERE sessionId = NEW.sessionId
                        )
                        WHERE id = NEW.sessionId;
                    END
                """)
                
                db.execSQL("""
                    CREATE TRIGGER update_session_stats_delete
                    AFTER DELETE ON emf_readings
                    BEGIN
                        UPDATE measurement_sessions 
                        SET measurementCount = (
                            SELECT COUNT(*) FROM emf_readings 
                            WHERE sessionId = OLD.sessionId
                        ),
                        averageSignalStrength = (
                            SELECT COALESCE(AVG(signalStrength), 0) FROM emf_readings 
                            WHERE sessionId = OLD.sessionId
                        ),
                        maxSignalStrength = (
                            SELECT COALESCE(MAX(signalStrength), 0) FROM emf_readings 
                            WHERE sessionId = OLD.sessionId
                        ),
                        minSignalStrength = (
                            SELECT COALESCE(MIN(signalStrength), 0) FROM emf_readings 
                            WHERE sessionId = OLD.sessionId
                        ),
                        averageTemperature = (
                            SELECT COALESCE(AVG(temperature), 0) FROM emf_readings 
                            WHERE sessionId = OLD.sessionId
                        )
                        WHERE id = OLD.sessionId;
                    END
                """)
            }
            
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                
                // Pragma-Einstellungen für Performance
                db.execSQL("PRAGMA foreign_keys=ON")
                db.execSQL("PRAGMA journal_mode=WAL")
                db.execSQL("PRAGMA synchronous=NORMAL")
                db.execSQL("PRAGMA cache_size=10000")
                db.execSQL("PRAGMA temp_store=MEMORY")
            }
        }
        
        /**
         * Migration von Version 1 zu 2 (Beispiel für zukünftige Updates)
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Beispiel-Migration - wird bei Bedarf implementiert
                // database.execSQL("ALTER TABLE emf_readings ADD COLUMN newColumn TEXT")
            }
        }
        
        /**
         * Database für Tests erstellen
         */
        fun createInMemoryDatabase(context: Context): EMFADDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                EMFADDatabase::class.java
            )
            .allowMainThreadQueries()
            .build()
        }
        
        /**
         * Database-Instanz schließen
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
        
        /**
         * Database-Größe abrufen
         */
        fun getDatabaseSize(context: Context): Long {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            return if (dbFile.exists()) dbFile.length() else 0L
        }
        
        /**
         * Database-Pfad abrufen
         */
        fun getDatabasePath(context: Context): String {
            return context.getDatabasePath(DATABASE_NAME).absolutePath
        }
        
        /**
         * Database-Backup erstellen
         */
        suspend fun createBackup(context: Context, backupPath: String): Boolean {
            return try {
                val dbFile = context.getDatabasePath(DATABASE_NAME)
                val backupFile = java.io.File(backupPath)
                
                if (dbFile.exists()) {
                    dbFile.copyTo(backupFile, overwrite = true)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * Database aus Backup wiederherstellen
         */
        suspend fun restoreFromBackup(context: Context, backupPath: String): Boolean {
            return try {
                closeDatabase()
                
                val dbFile = context.getDatabasePath(DATABASE_NAME)
                val backupFile = java.io.File(backupPath)
                
                if (backupFile.exists()) {
                    backupFile.copyTo(dbFile, overwrite = true)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * Database-Statistiken abrufen
         */
        suspend fun getDatabaseStats(context: Context): Map<String, Any> {
            val database = getDatabase(context)
            
            return try {
                mapOf(
                    "totalSessions" to database.measurementSessionDao().getCount(),
                    "totalReadings" to database.emfReadingDao().getCount(),
                    "totalAnalyses" to database.materialAnalysisDao().getCount(),
                    "databaseSize" to getDatabaseSize(context),
                    "databasePath" to getDatabasePath(context)
                )
            } catch (e: Exception) {
                emptyMap()
            }
        }
        
        /**
         * Database-Wartung durchführen
         */
        suspend fun performMaintenance(context: Context): Boolean {
            return try {
                val database = getDatabase(context)
                
                // Vacuum für Speicheroptimierung
                database.openHelper.writableDatabase.execSQL("VACUUM")
                
                // Analyze für Query-Optimierung
                database.openHelper.writableDatabase.execSQL("ANALYZE")
                
                true
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * Alte Daten bereinigen
         */
        suspend fun cleanupOldData(context: Context, cutoffDays: Int): Map<String, Int> {
            val database = getDatabase(context)
            val cutoffTime = System.currentTimeMillis() - (cutoffDays * 24 * 60 * 60 * 1000L)
            
            return try {
                val deletedReadings = database.emfReadingDao().deleteOlderThan(cutoffTime)
                val deletedAnalyses = database.materialAnalysisDao().deleteOlderThan(cutoffTime)
                val deletedSessions = database.measurementSessionDao().deleteOlderThan(cutoffTime)
                
                mapOf(
                    "deletedReadings" to deletedReadings,
                    "deletedAnalyses" to deletedAnalyses,
                    "deletedSessions" to deletedSessions
                )
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }
}
