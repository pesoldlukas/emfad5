package com.emfad.app.ar.core

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import com.emfad.app.models.EMFReading
import com.emfad.app.models.MaterialAnalysis
import com.emfad.app.ar.rendering.EMFRenderer
import com.emfad.app.ar.visualization.EMFVisualization
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * EMFAD AR Manager
 * ARCore Integration für 3D-EMF-Visualisierung
 * Samsung S21 Ultra optimiert mit Fallback-Funktionalität
 */
class ARManager(private val context: Context) : GLSurfaceView.Renderer {
    
    companion object {
        private const val TAG = "ARManager"
        private const val NEAR_CLIP = 0.1f
        private const val FAR_CLIP = 100.0f
    }
    
    // ARCore Components
    private var session: Session? = null
    private var displayRotationHelper: DisplayRotationHelper? = null
    private var trackingStateHelper: TrackingStateHelper? = null
    
    // Rendering Components
    private var emfRenderer: EMFRenderer? = null
    private var emfVisualization: EMFVisualization? = null
    
    // State Management
    private val _arState = MutableStateFlow(ARState())
    val arState: StateFlow<ARState> = _arState.asStateFlow()
    
    private val _isARSupported = MutableStateFlow(false)
    val isARSupported: StateFlow<Boolean> = _isARSupported.asStateFlow()
    
    private val _trackingState = MutableStateFlow(TrackingState.STOPPED)
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()
    
    // EMF Data
    private val emfReadings = mutableListOf<EMFReading>()
    private val materialAnalyses = mutableListOf<MaterialAnalysis>()
    private val arAnchors = mutableListOf<Anchor>()
    
    // Visualization Settings
    private var visualizationMode = VisualizationMode.SIGNAL_STRENGTH
    private var showMaterialTypes = true
    private var showInclusions = true
    private var scaleFactor = 1.0f
    
    /**
     * AR-Unterstützung prüfen und initialisieren
     */
    fun initializeAR(): Boolean {
        return try {
            Log.d(TAG, "Initialisiere ARCore")
            
            // ARCore-Verfügbarkeit prüfen
            val availability = ArCoreApk.getInstance().checkAvailability(context)
            
            when (availability) {
                ArCoreApk.Availability.SUPPORTED_INSTALLED -> {
                    Log.d(TAG, "ARCore ist verfügbar und installiert")
                    _isARSupported.value = true
                    createARSession()
                    true
                }
                ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
                ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                    Log.w(TAG, "ARCore muss aktualisiert/installiert werden")
                    _isARSupported.value = false
                    false
                }
                ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
                    Log.w(TAG, "Gerät unterstützt ARCore nicht - Fallback-Modus aktiviert")
                    _isARSupported.value = false
                    initializeFallbackMode()
                    true // Fallback ist verfügbar
                }
                else -> {
                    Log.e(TAG, "ARCore-Status unbekannt")
                    _isARSupported.value = false
                    false
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei ARCore-Initialisierung", e)
            _isARSupported.value = false
            initializeFallbackMode()
            true // Fallback ist verfügbar
        }
    }
    
    /**
     * AR-Session erstellen
     */
    private fun createARSession(): Boolean {
        return try {
            session = Session(context)
            
            // Session-Konfiguration
            val config = Config(session).apply {
                // Plane Detection aktivieren
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                
                // Light Estimation aktivieren
                lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                
                // Depth API aktivieren (falls verfügbar)
                if (session?.isDepthModeSupported(Config.DepthMode.AUTOMATIC) == true) {
                    depthMode = Config.DepthMode.AUTOMATIC
                }
                
                // Instant Placement aktivieren
                instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
            }
            
            session?.configure(config)
            
            // Helper-Klassen initialisieren
            displayRotationHelper = DisplayRotationHelper(context)
            trackingStateHelper = TrackingStateHelper(context)
            
            _arState.value = _arState.value.copy(
                isInitialized = true,
                hasSession = true
            )
            
            Log.d(TAG, "AR-Session erfolgreich erstellt")
            true
            
        } catch (e: UnavailableArcoreNotInstalledException) {
            Log.e(TAG, "ARCore nicht installiert", e)
            false
        } catch (e: UnavailableApkTooOldException) {
            Log.e(TAG, "ARCore APK zu alt", e)
            false
        } catch (e: UnavailableSdkTooOldException) {
            Log.e(TAG, "SDK zu alt für ARCore", e)
            false
        } catch (e: UnavailableDeviceNotCompatibleException) {
            Log.e(TAG, "Gerät nicht kompatibel mit ARCore", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unbekannter Fehler bei Session-Erstellung", e)
            false
        }
    }
    
    /**
     * Fallback-Modus für Geräte ohne ARCore-Unterstützung
     */
    private fun initializeFallbackMode() {
        Log.d(TAG, "Initialisiere Fallback-Modus für 3D-Visualisierung")
        
        _arState.value = _arState.value.copy(
            isInitialized = true,
            hasSession = false,
            isFallbackMode = true
        )
        
        // 3D-Renderer ohne ARCore initialisieren
        emfRenderer = EMFRenderer(context, false) // false = kein ARCore
        emfVisualization = EMFVisualization(context, false)
    }
    
    /**
     * AR-Session starten
     */
    fun startARSession() {
        try {
            session?.resume()
            _arState.value = _arState.value.copy(isRunning = true)
            Log.d(TAG, "AR-Session gestartet")
            
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Kamera nicht verfügbar", e)
            _arState.value = _arState.value.copy(
                error = "Kamera nicht verfügbar für AR"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Starten der AR-Session", e)
            _arState.value = _arState.value.copy(
                error = "AR-Session konnte nicht gestartet werden"
            )
        }
    }
    
    /**
     * AR-Session pausieren
     */
    fun pauseARSession() {
        session?.pause()
        _arState.value = _arState.value.copy(isRunning = false)
        Log.d(TAG, "AR-Session pausiert")
    }
    
    /**
     * EMF-Daten zur Visualisierung hinzufügen
     */
    fun addEMFReading(reading: EMFReading) {
        emfReadings.add(reading)
        
        // AR-Anchor erstellen (falls AR verfügbar)
        if (_isARSupported.value && session != null) {
            createARVisualization(reading)
        } else {
            // Fallback: 3D-Koordinaten direkt verwenden
            emfVisualization?.addEMFPoint(reading)
        }
        
        Log.d(TAG, "EMF-Messung zur AR-Visualisierung hinzugefügt")
    }
    
    /**
     * Material-Analyse zur Visualisierung hinzufügen
     */
    fun addMaterialAnalysis(analysis: MaterialAnalysis) {
        materialAnalyses.add(analysis)
        emfVisualization?.addMaterialAnalysis(analysis)
        Log.d(TAG, "Material-Analyse zur AR-Visualisierung hinzugefügt")
    }
    
    /**
     * AR-Visualisierung für EMF-Messung erstellen
     */
    private fun createARVisualization(reading: EMFReading) {
        try {
            val session = this.session ?: return
            
            // Pose für EMF-Punkt erstellen
            val pose = Pose.makeTranslation(
                reading.positionX.toFloat(),
                reading.positionY.toFloat(),
                reading.positionZ.toFloat()
            )
            
            // Anchor erstellen
            val anchor = session.createAnchor(pose)
            arAnchors.add(anchor)
            
            // Visualisierung mit Anchor verknüpfen
            emfVisualization?.addEMFPointWithAnchor(reading, anchor)
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Erstellen der AR-Visualisierung", e)
        }
    }
    
    /**
     * Visualisierungsmodus ändern
     */
    fun setVisualizationMode(mode: VisualizationMode) {
        visualizationMode = mode
        emfVisualization?.setVisualizationMode(mode)
        Log.d(TAG, "Visualisierungsmodus geändert: $mode")
    }
    
    /**
     * Skalierungsfaktor setzen
     */
    fun setScaleFactor(factor: Float) {
        scaleFactor = factor
        emfVisualization?.setScaleFactor(factor)
        Log.d(TAG, "Skalierungsfaktor geändert: $factor")
    }
    
    /**
     * Material-Typen anzeigen/verstecken
     */
    fun setShowMaterialTypes(show: Boolean) {
        showMaterialTypes = show
        emfVisualization?.setShowMaterialTypes(show)
    }
    
    /**
     * Einschlüsse anzeigen/verstecken
     */
    fun setShowInclusions(show: Boolean) {
        showInclusions = show
        emfVisualization?.setShowInclusions(show)
    }
    
    /**
     * Alle Visualisierungen löschen
     */
    fun clearVisualizations() {
        emfReadings.clear()
        materialAnalyses.clear()
        
        // AR-Anchors entfernen
        arAnchors.forEach { it.detach() }
        arAnchors.clear()
        
        emfVisualization?.clearAll()
        Log.d(TAG, "Alle Visualisierungen gelöscht")
    }
    
    // GLSurfaceView.Renderer Implementation
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        
        try {
            // Renderer initialisieren
            emfRenderer = EMFRenderer(context, _isARSupported.value)
            emfVisualization = EMFVisualization(context, _isARSupported.value)
            
            emfRenderer?.createOnGlThread()
            emfVisualization?.initialize()
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei OpenGL-Initialisierung", e)
        }
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        displayRotationHelper?.onSurfaceChanged(width, height)
        GLES20.glViewport(0, 0, width, height)
    }
    
    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        
        if (_arState.value.isFallbackMode) {
            // Fallback-Rendering ohne ARCore
            renderFallbackMode()
        } else {
            // AR-Rendering mit ARCore
            renderARMode()
        }
    }
    
    /**
     * AR-Modus rendern
     */
    private fun renderARMode() {
        val session = this.session ?: return
        
        try {
            session.setCameraTextureName(emfRenderer?.backgroundRenderer?.textureId ?: 0)
            
            val frame = session.update()
            val camera = frame.camera
            
            // Tracking-Status aktualisieren
            val trackingState = camera.trackingState
            _trackingState.value = trackingState
            
            if (trackingState == TrackingState.TRACKING) {
                // Kamera-Hintergrund rendern
                emfRenderer?.backgroundRenderer?.draw(frame)
                
                // Projektions- und View-Matrizen abrufen
                val projectionMatrix = FloatArray(16)
                camera.getProjectionMatrix(projectionMatrix, 0, NEAR_CLIP, FAR_CLIP)
                
                val viewMatrix = FloatArray(16)
                camera.getViewMatrix(viewMatrix, 0)
                
                // EMF-Visualisierungen rendern
                emfVisualization?.render(projectionMatrix, viewMatrix, frame)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim AR-Rendering", e)
        }
    }
    
    /**
     * Fallback-Modus rendern
     */
    private fun renderFallbackMode() {
        try {
            // Einfache 3D-Visualisierung ohne ARCore
            emfVisualization?.renderFallback()
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Fallback-Rendering", e)
        }
    }
    
    /**
     * Ressourcen freigeben
     */
    fun cleanup() {
        session?.close()
        session = null
        
        arAnchors.forEach { it.detach() }
        arAnchors.clear()
        
        emfRenderer?.cleanup()
        emfVisualization?.cleanup()
        
        _arState.value = ARState()
        Log.d(TAG, "AR-Manager bereinigt")
    }
}

/**
 * AR-Zustand
 */
data class ARState(
    val isInitialized: Boolean = false,
    val hasSession: Boolean = false,
    val isRunning: Boolean = false,
    val isFallbackMode: Boolean = false,
    val error: String? = null
)

/**
 * Visualisierungsmodi
 */
enum class VisualizationMode {
    SIGNAL_STRENGTH,
    MATERIAL_TYPE,
    DEPTH,
    FREQUENCY,
    PHASE,
    TEMPERATURE,
    COMBINED
}
