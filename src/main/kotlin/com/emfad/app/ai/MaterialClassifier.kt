package com.emfad.app.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

/**
 * KI-gestützte Materialklassifizierung für EMFAD
 * Implementiert Machine Learning Algorithmen für Materialerkennung
 */
class MaterialClassifier {
    
    // Standard-Schwellwerte für verschiedene Materialtypen
    private val defaultThresholds = mapOf(
        "METALLIC_CAVITY_THRESHOLD" to 0.85,
        "CRYSTALLINE_STRUCTURE_THRESHOLD" to 0.7,
        "FERROUS_DETECTION_RATIO" to 0.65,
        "NON_FERROUS_CONDUCTIVITY" to 0.4,
        "VOID_DETECTION_THRESHOLD" to 0.3,
        "PARTICLE_DENSITY_THRESHOLD" to 0.8
    )
    
    fun getDefaultThresholds() = defaultThresholds.toMutableMap()
    
    /**
     * Hauptklassifizierungsmethode
     */
    suspend fun classify(analysis: MaterialPhysicsAnalysis): MaterialClassificationResult = withContext(Dispatchers.Default) {
        try {
            val materialType = determineMaterialType(analysis)
            val confidence = calculateConfidence(analysis, materialType)
            val properties = extractMaterialProperties(analysis, materialType)
            val recommendations = generateRecommendations(materialType, confidence, analysis)
            
            MaterialClassificationResult(
                materialType = materialType,
                confidence = confidence,
                properties = properties,
                recommendations = recommendations,
                analysisDetails = analysis
            )
        } catch (e: Exception) {
            Log.e("MaterialClassifier", "Klassifizierungsfehler", e)
            MaterialClassificationResult(
                materialType = MaterialType.UNKNOWN,
                confidence = 0.0,
                properties = emptyMap(),
                recommendations = listOf("Fehler bei der Analyse - Messung wiederholen"),
                analysisDetails = analysis
            )
        }
    }
    
    /**
     * Materialtyp bestimmen
     */
    private fun determineMaterialType(analysis: MaterialPhysicsAnalysis): MaterialType {
        // Kristallstrukturerkennung (höchste Priorität)
        if (analysis.symmetryScore > defaultThresholds["CRYSTALLINE_STRUCTURE_THRESHOLD"]!!) {
            return if (analysis.conductivity > 0.5) {
                MaterialType.CRYSTALLINE_METAL
            } else {
                MaterialType.CRYSTALLINE_NON_METAL
            }
        }
        
        // Hohlraumanalyse
        if (analysis.hollownessScore > defaultThresholds["METALLIC_CAVITY_THRESHOLD"]!!) {
            return MaterialType.CAVITY
        }
        
        // Void-Erkennung
        if (analysis.hollownessScore > defaultThresholds["VOID_DETECTION_THRESHOLD"]!! && 
            analysis.signalStrength < 0.2) {
            return MaterialType.VOID
        }
        
        // Metalltyp-Differenzierung
        if (analysis.conductivity > defaultThresholds["FERROUS_DETECTION_RATIO"]!!) {
            return if (analysis.magneticPermeability > 1.5) {
                MaterialType.FERROUS_METAL
            } else {
                MaterialType.NON_FERROUS_METAL
            }
        }
        
        // Nicht-metallische Materialien
        if (analysis.conductivity > defaultThresholds["NON_FERROUS_CONDUCTIVITY"]!!) {
            return MaterialType.CONDUCTIVE_NON_METAL
        }
        
        // Partikel-Erkennung
        if (analysis.particleDensity > defaultThresholds["PARTICLE_DENSITY_THRESHOLD"]!!) {
            return MaterialType.PARTICLE_COMPOSITE
        }
        
        // Isolatoren
        if (analysis.conductivity < 0.1) {
            return MaterialType.INSULATOR
        }
        
        return MaterialType.UNKNOWN
    }
    
    /**
     * Konfidenz berechnen
     */
    private fun calculateConfidence(analysis: MaterialPhysicsAnalysis, materialType: MaterialType): Double {
        var confidence = 0.0
        var factors = 0
        
        // Signalstärke-Faktor
        if (analysis.signalStrength > 0.1) {
            confidence += (analysis.signalStrength * 0.3)
            factors++
        }
        
        // Konsistenz-Faktor
        val consistencyScore = calculateConsistency(analysis)
        confidence += (consistencyScore * 0.25)
        factors++
        
        // Materialtyp-spezifische Konfidenz
        when (materialType) {
            MaterialType.FERROUS_METAL -> {
                confidence += if (analysis.magneticPermeability > 1.5) 0.2 else 0.1
                confidence += if (analysis.conductivity > 0.6) 0.15 else 0.05
            }
            MaterialType.NON_FERROUS_METAL -> {
                confidence += if (analysis.conductivity > 0.4 && analysis.magneticPermeability < 1.2) 0.2 else 0.1
            }
            MaterialType.CRYSTALLINE_METAL, MaterialType.CRYSTALLINE_NON_METAL -> {
                confidence += if (analysis.symmetryScore > 0.7) 0.25 else 0.1
            }
            MaterialType.CAVITY, MaterialType.VOID -> {
                confidence += if (analysis.hollownessScore > 0.5) 0.2 else 0.1
            }
            else -> {
                confidence += 0.1
            }
        }
        factors++
        
        // Tiefenanalyse-Faktor
        if (analysis.depth > 0) {
            val depthFactor = min(analysis.depth / 10.0, 1.0) * 0.1
            confidence += depthFactor
            factors++
        }
        
        return (confidence / factors).coerceIn(0.0, 1.0)
    }
    
    /**
     * Konsistenz der Analyse berechnen
     */
    private fun calculateConsistency(analysis: MaterialPhysicsAnalysis): Double {
        val measurements = listOf(
            analysis.signalStrength,
            analysis.conductivity,
            analysis.symmetryScore,
            analysis.hollownessScore
        )
        
        val mean = measurements.average()
        val variance = measurements.map { (it - mean).pow(2) }.average()
        val standardDeviation = sqrt(variance)
        
        // Niedrige Standardabweichung = hohe Konsistenz
        return (1.0 - standardDeviation).coerceIn(0.0, 1.0)
    }
    
    /**
     * Materialeigenschaften extrahieren
     */
    private fun extractMaterialProperties(analysis: MaterialPhysicsAnalysis, materialType: MaterialType): Map<String, Any> {
        val properties = mutableMapOf<String, Any>()
        
        // Grundlegende Eigenschaften
        properties["conductivity"] = analysis.conductivity
        properties["magnetic_permeability"] = analysis.magneticPermeability
        properties["signal_strength"] = analysis.signalStrength
        properties["depth"] = analysis.depth
        properties["size"] = analysis.size
        
        // Materialtyp-spezifische Eigenschaften
        when (materialType) {
            MaterialType.FERROUS_METAL -> {
                properties["iron_content"] = estimateIronContent(analysis)
                properties["hardness"] = estimateHardness(analysis)
                properties["corrosion_resistance"] = estimateCorrosionResistance(analysis)
            }
            MaterialType.NON_FERROUS_METAL -> {
                properties["metal_type"] = identifyNonFerrousType(analysis)
                properties["purity"] = estimatePurity(analysis)
                properties["oxidation_level"] = estimateOxidation(analysis)
            }
            MaterialType.CRYSTALLINE_METAL, MaterialType.CRYSTALLINE_NON_METAL -> {
                properties["crystal_structure"] = analyzeCrystalStructure(analysis)
                properties["grain_size"] = estimateGrainSize(analysis)
                properties["lattice_defects"] = detectLatticeDefects(analysis)
            }
            MaterialType.CAVITY, MaterialType.VOID -> {
                properties["void_volume"] = estimateVoidVolume(analysis)
                properties["void_shape"] = analyzeVoidShape(analysis)
                properties["surrounding_material"] = analyzeSurroundingMaterial(analysis)
            }
            MaterialType.PARTICLE_COMPOSITE -> {
                properties["particle_size"] = estimateParticleSize(analysis)
                properties["particle_distribution"] = analyzeParticleDistribution(analysis)
                properties["matrix_material"] = identifyMatrixMaterial(analysis)
            }
            else -> {
                properties["density"] = estimateDensity(analysis)
                properties["porosity"] = estimatePorosity(analysis)
            }
        }
        
        return properties
    }
    
    /**
     * Empfehlungen generieren
     */
    private fun generateRecommendations(materialType: MaterialType, confidence: Double, analysis: MaterialPhysicsAnalysis): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Konfidenz-basierte Empfehlungen
        when {
            confidence < 0.3 -> {
                recommendations.add("Niedrige Analysegenauigkeit - Messung wiederholen")
                recommendations.add("Gerät kalibrieren und Messposition überprüfen")
            }
            confidence < 0.6 -> {
                recommendations.add("Mittlere Analysegenauigkeit - zusätzliche Messungen empfohlen")
            }
            else -> {
                recommendations.add("Hohe Analysegenauigkeit - Ergebnis zuverlässig")
            }
        }
        
        // Materialtyp-spezifische Empfehlungen
        when (materialType) {
            MaterialType.FERROUS_METAL -> {
                recommendations.add("Eisenhaltiges Metall erkannt")
                if (analysis.conductivity > 0.8) {
                    recommendations.add("Hohe Leitfähigkeit - möglicherweise Stahl oder Eisen")
                }
                recommendations.add("Korrosionsschutz überprüfen")
            }
            MaterialType.NON_FERROUS_METAL -> {
                recommendations.add("Nicht-eisenhaltiges Metall erkannt")
                recommendations.add("Mögliche Materialien: Aluminium, Kupfer, Messing")
                recommendations.add("Weitere Analyse für genaue Identifikation empfohlen")
            }
            MaterialType.CRYSTALLINE_METAL, MaterialType.CRYSTALLINE_NON_METAL -> {
                recommendations.add("Kristalline Struktur erkannt")
                recommendations.add("Materialqualität durch Kristallstruktur bestimmt")
                if (analysis.symmetryScore > 0.8) {
                    recommendations.add("Hohe Kristallqualität - gute Materialeigenschaften")
                }
            }
            MaterialType.CAVITY -> {
                recommendations.add("Hohlraum erkannt")
                recommendations.add("Strukturelle Integrität überprüfen")
                recommendations.add("Mögliche Korrosion oder Materialermüdung")
            }
            MaterialType.VOID -> {
                recommendations.add("Leerstelle oder Defekt erkannt")
                recommendations.add("Qualitätskontrolle erforderlich")
                recommendations.add("Produktionsprozess überprüfen")
            }
            MaterialType.PARTICLE_COMPOSITE -> {
                recommendations.add("Verbundmaterial mit Partikeln erkannt")
                recommendations.add("Partikelverteilung analysieren")
                recommendations.add("Bindungsqualität überprüfen")
            }
            MaterialType.INSULATOR -> {
                recommendations.add("Isoliermaterial erkannt")
                recommendations.add("Elektrische Eigenschaften überprüfen")
                recommendations.add("Alterung und Degradation überwachen")
            }
            MaterialType.UNKNOWN -> {
                recommendations.add("Material nicht eindeutig identifiziert")
                recommendations.add("Erweiterte Analyse oder andere Messmethoden verwenden")
                recommendations.add("Referenzmaterial für Vergleich verwenden")
            }
            else -> {
                recommendations.add("Weitere Analyse empfohlen")
            }
        }
        
        // Signalqualität-basierte Empfehlungen
        if (analysis.signalStrength < 0.2) {
            recommendations.add("Schwaches Signal - Messabstand verringern")
        }
        
        if (analysis.depth < 1.0) {
            recommendations.add("Oberflächennahe Messung - tiefere Analyse möglich")
        }
        
        return recommendations
    }
    
    // Hilfsmethoden für Eigenschaftsschätzung
    private fun estimateIronContent(analysis: MaterialPhysicsAnalysis): Double {
        return (analysis.magneticPermeability - 1.0) * 0.5
    }
    
    private fun estimateHardness(analysis: MaterialPhysicsAnalysis): String {
        return when {
            analysis.signalStrength > 0.8 -> "Hart"
            analysis.signalStrength > 0.5 -> "Mittel"
            else -> "Weich"
        }
    }
    
    private fun estimateCorrosionResistance(analysis: MaterialPhysicsAnalysis): String {
        return when {
            analysis.conductivity > 0.9 -> "Hoch"
            analysis.conductivity > 0.6 -> "Mittel"
            else -> "Niedrig"
        }
    }
    
    private fun identifyNonFerrousType(analysis: MaterialPhysicsAnalysis): String {
        return when {
            analysis.conductivity > 0.9 -> "Kupfer/Silber"
            analysis.conductivity > 0.6 -> "Aluminium"
            analysis.conductivity > 0.4 -> "Messing/Bronze"
            else -> "Unbekannt"
        }
    }
    
    private fun estimatePurity(analysis: MaterialPhysicsAnalysis): Double {
        return analysis.conductivity * 0.9 + analysis.symmetryScore * 0.1
    }
    
    private fun estimateOxidation(analysis: MaterialPhysicsAnalysis): String {
        return when {
            analysis.signalStrength < 0.3 -> "Hoch"
            analysis.signalStrength < 0.6 -> "Mittel"
            else -> "Niedrig"
        }
    }
    
    private fun analyzeCrystalStructure(analysis: MaterialPhysicsAnalysis): String {
        return when {
            analysis.symmetryScore > 0.9 -> "Kubisch"
            analysis.symmetryScore > 0.7 -> "Hexagonal"
            analysis.symmetryScore > 0.5 -> "Tetragonal"
            else -> "Amorph"
        }
    }
    
    private fun estimateGrainSize(analysis: MaterialPhysicsAnalysis): String {
        return when {
            analysis.symmetryScore > 0.8 -> "Fein"
            analysis.symmetryScore > 0.5 -> "Mittel"
            else -> "Grob"
        }
    }
    
    private fun detectLatticeDefects(analysis: MaterialPhysicsAnalysis): String {
        return when {
            analysis.symmetryScore < 0.3 -> "Viele"
            analysis.symmetryScore < 0.6 -> "Einige"
            else -> "Wenige"
        }
    }
    
    private fun estimateVoidVolume(analysis: MaterialPhysicsAnalysis): Double {
        return analysis.hollownessScore * analysis.size
    }
    
    private fun analyzeVoidShape(analysis: MaterialPhysicsAnalysis): String {
        return when {
            analysis.symmetryScore > 0.7 -> "Sphärisch"
            analysis.symmetryScore > 0.4 -> "Elliptisch"
            else -> "Unregelmäßig"
        }
    }
    
    private fun analyzeSurroundingMaterial(analysis: MaterialPhysicsAnalysis): String {
        return when {
            analysis.conductivity > 0.5 -> "Metallisch"
            analysis.conductivity > 0.2 -> "Halbleiter"
            else -> "Isolierend"
        }
    }
    
    private fun estimateParticleSize(analysis: MaterialPhysicsAnalysis): String {
        return when {
            analysis.particleDensity > 0.8 -> "Fein"
            analysis.particleDensity > 0.5 -> "Mittel"
            else -> "Grob"
        }
    }
    
    private fun analyzeParticleDistribution(analysis: MaterialPhysicsAnalysis): String {
        return when {
            analysis.symmetryScore > 0.7 -> "Homogen"
            analysis.symmetryScore > 0.4 -> "Teilweise homogen"
            else -> "Heterogen"
        }
    }
    
    private fun identifyMatrixMaterial(analysis: MaterialPhysicsAnalysis): String {
        return when {
            analysis.conductivity > 0.5 -> "Metallische Matrix"
            analysis.conductivity > 0.2 -> "Keramische Matrix"
            else -> "Polymer-Matrix"
        }
    }
    
    private fun estimateDensity(analysis: MaterialPhysicsAnalysis): String {
        return when {
            analysis.signalStrength > 0.7 -> "Hoch"
            analysis.signalStrength > 0.4 -> "Mittel"
            else -> "Niedrig"
        }
    }
    
    private fun estimatePorosity(analysis: MaterialPhysicsAnalysis): String {
        return when {
            analysis.hollownessScore > 0.6 -> "Hoch"
            analysis.hollownessScore > 0.3 -> "Mittel"
            else -> "Niedrig"
        }
    }
}

/**
 * Datenklassen für Materialklassifizierung
 */
data class MaterialClassificationResult(
    val materialType: MaterialType,
    val confidence: Double,
    val properties: Map<String, Any>,
    val recommendations: List<String>,
    val analysisDetails: MaterialPhysicsAnalysis
)

data class MaterialPhysicsAnalysis(
    val symmetryScore: Double,
    val hollownessScore: Double,
    val conductivity: Double,
    val magneticPermeability: Double = 1.0,
    val signalStrength: Double,
    val depth: Double,
    val size: Double,
    val particleDensity: Double = 0.0,
    val confidence: Double
)

enum class MaterialType {
    FERROUS_METAL,
    NON_FERROUS_METAL,
    CRYSTALLINE_METAL,
    CRYSTALLINE_NON_METAL,
    CAVITY,
    VOID,
    PARTICLE_COMPOSITE,
    CONDUCTIVE_NON_METAL,
    INSULATOR,
    UNKNOWN
}
