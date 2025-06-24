package com.emfad.app.di

import android.content.Context
import com.emfad.app.ai.MaterialClassifier
import com.emfad.app.models.ClusterAnalyzer
import com.emfad.app.models.SymmetryAnalyzer
import com.emfad.app.services.AnalysisService
import com.emfad.app.services.BluetoothService
import com.emfad.app.services.DatabaseService
import com.emfad.app.services.HeatmapGenerator
import com.emfad.app.services.MeasurementService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBluetoothService(
        @ApplicationContext context: Context
    ): BluetoothService {
        return BluetoothService(context)
    }

    @Provides
    @Singleton
    fun provideMeasurementService(
        bluetoothService: BluetoothService,
        databaseService: DatabaseService
    ): MeasurementService {
        return MeasurementService(bluetoothService, databaseService)
    }

    @Provides
    @Singleton
    fun provideDatabaseService(
        measurementDao: com.emfad.app.database.dao.MeasurementDao
    ): DatabaseService {
        return DatabaseService(measurementDao)
    }

    @Provides
    @Singleton
    fun provideMaterialClassifier(
        @ApplicationContext context: Context
    ): MaterialClassifier {
        return MaterialClassifier(context)
    }

    @Provides
    @Singleton
    fun provideClusterAnalyzer(): ClusterAnalyzer {
        return ClusterAnalyzer()
    }

    @Provides
    @Singleton
    fun provideSymmetryAnalyzer(): SymmetryAnalyzer {
        return SymmetryAnalyzer()
    }

    @Provides
    @Singleton
    fun provideAnalysisService(
        materialClassifier: MaterialClassifier,
        clusterAnalyzer: ClusterAnalyzer,
        symmetryAnalyzer: SymmetryAnalyzer
    ): AnalysisService {
        return AnalysisService(materialClassifier, clusterAnalyzer, symmetryAnalyzer)
    }

    @Provides
    @Singleton
    fun provideHeatmapGenerator(): HeatmapGenerator {
        return HeatmapGenerator()
    }
}