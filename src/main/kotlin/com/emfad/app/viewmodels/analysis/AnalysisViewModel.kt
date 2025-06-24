package com.emfad.app.viewmodels.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emfad.app.ai.analyzers.ClusterAnalyzer
import com.emfad.app.ai.analyzers.InclusionDetector
import com.emfad.app.ai.classifiers.MaterialClassifier
import com.emfad.app.database.EMFADDatabase
import com.emfad.app.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Analysis ViewModel für EMFAD App
 * AI-basierte Analyse mit ursprünglichen Algorithmen
 * Samsung S21 Ultra optimiert
 */
@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val database: EMFADDatabase,
    private val materialClassifier: MaterialClassifier,
    private val clusterAnalyzer: ClusterAnalyzer,
    private val inclusionDetector: InclusionDetector
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()
    
    // Analysis State
    private val _analysisState = MutableStateFlow(AnalysisState())
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()
    
    // Current Session Analysis
    private val _currentSessionAnalysis = MutableStateFlow<SessionAnalysisResult?>(null)
    val currentSessionAnalysis: StateFlow<SessionAnalysisResult?> = _currentSessionAnalysis.asStateFlow()
    
    // Material Analysis Results
    private val _materialAnalyses = MutableStateFlow<List<MaterialAnalysis>>(emptyList())
    val materialAnalyses: StateFlow<List<MaterialAnalysis>> = _materialAnalyses.asStateFlow()
    
    // Cluster Analysis Results
    private val _clusterResults = MutableStateFlow<ClusterAnalyzer.ClusterResult?>(null)
    val clusterResults: StateFlow<ClusterAnalyzer.ClusterResult?> = _clusterResults.asStateFlow()
    
    // Inclusion Detection Results
    private val _inclusionResults = MutableStateFlow<InclusionDetector.InclusionDetectionResult?>(null)
    val inclusionResults: StateFlow<InclusionDetector.InclusionDetectionResult?> = _inclusionResults.asStateFlow()
    
    // Available Sessions
    private val _availableSessions = MutableStateFlow<List<MeasurementSession>>(emptyList())
    val availableSessions: StateFlow<List<MeasurementSession>> = _availableSessions.asStateFlow()
    
    init {
        loadAvailableSessions()
        initializeAI()
    }
    
    /**
     * AI-Komponenten initialisieren
     */
    private fun initializeAI() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isInitializing = true)
                
                // Material Classifier initialisieren
                val classifierInitialized = materialClassifier.initialize()
                
                _analysisState.value = _analysisState.value.copy(
                    isAIReady = classifierInitialized
                )
                
                _uiState.value = _uiState.value.copy(
                    isInitializing = false,
                    message = if (classifierInitialized) "AI-Komponenten bereit" else "AI-Initialisierung fehlgeschlagen"
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isInitializing = false,
                    error = "AI-Initialisierungsfehler: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Verfügbare Sessions laden
     */
    private fun loadAvailableSessions() {
        viewModelScope.launch {
            try {
                val sessions = database.measurementSessionDao()
                    .getByStatus(SessionStatus.COMPLETED)
                    .map { it.toDomainModel() }
                
                _availableSessions.value = sessions
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Fehler beim Laden der Sessions: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Session für Analyse auswählen
     */
    fun selectSessionForAnalysis(sessionId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Session-Daten laden
                val session = database.measurementSessionDao().getById(sessionId)?.toDomainModel()
                    ?: throw Exception("Session nicht gefunden")
                
                val readings = database.emfReadingDao().getBySessionId(sessionId)
                    .map { it.toDomainModel() }
                
                val existingAnalyses = database.materialAnalysisDao().getBySessionId(sessionId)
                    .map { it.toDomainModel() }
                
                _analysisState.value = _analysisState.value.copy(
                    selectedSession = session,
                    sessionReadings = readings,
                    hasData = readings.isNotEmpty()
                )
                
                _materialAnalyses.value = existingAnalyses
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Session geladen: ${readings.size} Messungen"
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Fehler beim Laden der Session: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Vollständige Session-Analyse durchführen
     */
    fun performCompleteAnalysis() {
        viewModelScope.launch {
            try {
                val readings = _analysisState.value.sessionReadings
                if (readings.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        error = "Keine Messdaten für Analyse verfügbar"
                    )
                    return@launch
                }
                
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = true,
                    analysisProgress = 0.0
                )
                
                // 1. Material-Klassifikation
                _uiState.value = _uiState.value.copy(analysisProgress = 0.2)
                val materialAnalyses = performMaterialClassification(readings)
                
                // 2. Cluster-Analyse
                _uiState.value = _uiState.value.copy(analysisProgress = 0.5)
                val clusterResult = performClusterAnalysis(readings)
                
                // 3. Einschluss-Erkennung
                _uiState.value = _uiState.value.copy(analysisProgress = 0.8)
                val inclusionResult = performInclusionDetection(readings)
                
                // 4. Gesamtauswertung
                _uiState.value = _uiState.value.copy(analysisProgress = 1.0)
                val sessionAnalysis = createSessionAnalysisResult(
                    readings, materialAnalyses, clusterResult, inclusionResult
                )
                
                // Ergebnisse speichern
                saveMaterialAnalyses(materialAnalyses)
                
                // UI aktualisieren
                _materialAnalyses.value = materialAnalyses
                _clusterResults.value = clusterResult
                _inclusionResults.value = inclusionResult
                _currentSessionAnalysis.value = sessionAnalysis
                
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    analysisProgress = 0.0,
                    message = "Analyse abgeschlossen"
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    analysisProgress = 0.0,
                    error = "Analysefehler: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Material-Klassifikation durchführen
     */
    private suspend fun performMaterialClassification(readings: List<EMFReading>): List<MaterialAnalysis> {
        val analyses = mutableListOf<MaterialAnalysis>()
        
        // Klassifikation für repräsentative Messungen
        val sampleReadings = readings.filterIndexed { index, _ -> index % 10 == 0 } // Jede 10. Messung
        
        for (reading in sampleReadings) {
            val analysis = materialClassifier.classifyMaterial(reading)
            analysis?.let { analyses.add(it) }
        }
        
        return analyses
    }
    
    /**
     * Cluster-Analyse durchführen
     */
    private suspend fun performClusterAnalysis(readings: List<EMFReading>): ClusterAnalyzer.ClusterResult {
        return clusterAnalyzer.performClustering(readings)
    }
    
    /**
     * Einschluss-Erkennung durchführen
     */
    private suspend fun performInclusionDetection(readings: List<EMFReading>): InclusionDetector.InclusionDetectionResult {
        return inclusionDetector.detectInclusions(readings)
    }
    
    /**
     * Session-Analyseergebnis erstellen
     */
    private fun createSessionAnalysisResult(
        readings: List<EMFReading>,
        materialAnalyses: List<MaterialAnalysis>,
        clusterResult: ClusterAnalyzer.ClusterResult,
        inclusionResult: InclusionDetector.InclusionDetectionResult
    ): SessionAnalysisResult {
        
        // Material-Verteilung berechnen
        val materialDistribution = materialAnalyses
            .groupBy { it.materialType }
            .mapValues { it.value.size }
        
        // Durchschnittliche Konfidenz
        val averageConfidence = materialAnalyses
            .map { it.confidence }
            .average()
        
        // Qualitätsbewertung
        val overallQuality = readings
            .map { it.qualityScore }
            .average()
        
        return SessionAnalysisResult(
            sessionId = _analysisState.value.selectedSession?.id ?: 0L,
            timestamp = System.currentTimeMillis(),
            totalMeasurements = readings.size,
            materialAnalysisCount = materialAnalyses.size,
            materialDistribution = materialDistribution,
            averageConfidence = averageConfidence,
            clusterCount = clusterResult.clusterCount,
            inclusionCount = inclusionResult.inclusionCount,
            overallQuality = overallQuality,
            analysisQuality = calculateAnalysisQuality(materialAnalyses, clusterResult, inclusionResult),
            recommendations = generateRecommendations(materialAnalyses, clusterResult, inclusionResult)
        )
    }
    
    /**
     * Analyse-Qualität berechnen
     */
    private fun calculateAnalysisQuality(
        materialAnalyses: List<MaterialAnalysis>,
        clusterResult: ClusterAnalyzer.ClusterResult,
        inclusionResult: InclusionDetector.InclusionDetectionResult
    ): Double {
        val materialQuality = if (materialAnalyses.isNotEmpty()) {
            materialAnalyses.map { it.analysisQuality }.average()
        } else 0.0
        
        val clusterQuality = clusterResult.silhouetteScore
        val inclusionQuality = inclusionResult.detectionConfidence
        
        return (materialQuality + clusterQuality + inclusionQuality) / 3.0
    }
    
    /**
     * Empfehlungen generieren
     */
    private fun generateRecommendations(
        materialAnalyses: List<MaterialAnalysis>,
        clusterResult: ClusterAnalyzer.ClusterResult,
        inclusionResult: InclusionDetector.InclusionDetectionResult
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Material-basierte Empfehlungen
        val lowConfidenceAnalyses = materialAnalyses.filter { it.confidence < 0.7 }
        if (lowConfidenceAnalyses.isNotEmpty()) {
            recommendations.add("${lowConfidenceAnalyses.size} Materialanalysen haben niedrige Konfidenz - weitere Messungen empfohlen")
        }
        
        // Cluster-basierte Empfehlungen
        if (clusterResult.clusterCount > 5) {
            recommendations.add("Hohe Anzahl von Clustern (${clusterResult.clusterCount}) - Material könnte heterogen sein")
        }
        
        // Einschluss-basierte Empfehlungen
        if (inclusionResult.inclusionCount > 0) {
            recommendations.add("${inclusionResult.inclusionCount} Einschlüsse erkannt - Qualitätsprüfung empfohlen")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Analyse zeigt gute Materialqualität - keine besonderen Maßnahmen erforderlich")
        }
        
        return recommendations
    }
    
    /**
     * Material-Analysen speichern
     */
    private suspend fun saveMaterialAnalyses(analyses: List<MaterialAnalysis>) {
        try {
            val entities = analyses.map {
                com.emfad.app.database.entities.MaterialAnalysisEntity.fromDomainModel(it)
            }
            database.materialAnalysisDao().insertAll(entities)
            
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Fehler beim Speichern der Analysen: ${e.message}"
            )
        }
    }
    
    /**
     * Analyse exportieren
     */
    fun exportAnalysis(format: ExportFormat) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isExporting = true,
                    message = "Export wird vorbereitet..."
                )
                
                // Export-Logik würde hier implementiert
                when (format) {
                    ExportFormat.PDF -> exportToPDF()
                    ExportFormat.CSV -> exportToCSV()
                    ExportFormat.MATLAB -> exportToMatlab()
                }
                
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    message = "Export erfolgreich"
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = "Export-Fehler: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun exportToPDF() {
        // PDF-Export implementieren
    }
    
    private suspend fun exportToCSV() {
        // CSV-Export implementieren
    }
    
    private suspend fun exportToMatlab() {
        // MATLAB-Export implementieren
    }
    
    /**
     * Fehler löschen
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Nachricht löschen
     */
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

/**
 * Analysis UI State
 */
data class AnalysisUiState(
    val isInitializing: Boolean = false,
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false,
    val isExporting: Boolean = false,
    val analysisProgress: Double = 0.0,
    val error: String? = null,
    val message: String? = null
)

/**
 * Analysis State
 */
data class AnalysisState(
    val isAIReady: Boolean = false,
    val selectedSession: MeasurementSession? = null,
    val sessionReadings: List<EMFReading> = emptyList(),
    val hasData: Boolean = false
)

/**
 * Session Analysis Result
 */
data class SessionAnalysisResult(
    val sessionId: Long,
    val timestamp: Long,
    val totalMeasurements: Int,
    val materialAnalysisCount: Int,
    val materialDistribution: Map<MaterialType, Int>,
    val averageConfidence: Double,
    val clusterCount: Int,
    val inclusionCount: Int,
    val overallQuality: Double,
    val analysisQuality: Double,
    val recommendations: List<String>
)

/**
 * Export-Formate
 */
enum class ExportFormat {
    PDF, CSV, MATLAB
}
