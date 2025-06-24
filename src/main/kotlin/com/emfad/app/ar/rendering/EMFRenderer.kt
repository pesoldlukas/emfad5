package com.emfad.app.ar.rendering

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.emfad.app.models.EMFReading
import com.emfad.app.models.MaterialType
import com.emfad.app.ar.core.VisualizationMode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.*

/**
 * EMFAD EMF Renderer
 * OpenGL ES Rendering für EMF-Datenvisualisierung
 * Samsung S21 Ultra optimiert
 */
class EMFRenderer(
    private val context: Context,
    private val useARCore: Boolean = true
) {
    
    companion object {
        private const val TAG = "EMFRenderer"
        
        // Shader-Code
        private const val VERTEX_SHADER_CODE = """
            uniform mat4 u_ModelViewProjection;
            uniform vec4 u_Color;
            uniform float u_PointSize;
            
            attribute vec4 a_Position;
            
            varying vec4 v_Color;
            
            void main() {
                v_Color = u_Color;
                gl_Position = u_ModelViewProjection * a_Position;
                gl_PointSize = u_PointSize;
            }
        """
        
        private const val FRAGMENT_SHADER_CODE = """
            precision mediump float;
            varying vec4 v_Color;
            
            void main() {
                float distance = length(gl_PointCoord - vec2(0.5));
                if (distance > 0.5) {
                    discard;
                }
                gl_FragColor = v_Color;
            }
        """
        
        private const val COORDS_PER_VERTEX = 3
        private const val VERTEX_STRIDE = COORDS_PER_VERTEX * 4 // 4 bytes per float
    }
    
    // OpenGL-Komponenten
    private var shaderProgram: Int = 0
    private var positionHandle: Int = 0
    private var colorHandle: Int = 0
    private var mvpMatrixHandle: Int = 0
    private var pointSizeHandle: Int = 0
    
    // Background Renderer für ARCore
    var backgroundRenderer: BackgroundRenderer? = null
        private set
    
    // Rendering-Daten
    private val emfPoints = mutableListOf<EMFPoint>()
    private var vertexBuffer: FloatBuffer? = null
    private var colorBuffer: FloatBuffer? = null
    
    // Matrizen
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    
    // Rendering-Einstellungen
    private var visualizationMode = VisualizationMode.SIGNAL_STRENGTH
    private var scaleFactor = 1.0f
    private var pointSize = 10.0f
    
    /**
     * OpenGL-Thread initialisieren
     */
    fun createOnGlThread() {
        try {
            Log.d(TAG, "Initialisiere EMF Renderer")
            
            // Shader-Programm erstellen
            shaderProgram = createShaderProgram()
            
            // Handles abrufen
            positionHandle = GLES20.glGetAttribLocation(shaderProgram, "a_Position")
            colorHandle = GLES20.glGetUniformLocation(shaderProgram, "u_Color")
            mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "u_ModelViewProjection")
            pointSizeHandle = GLES20.glGetUniformLocation(shaderProgram, "u_PointSize")
            
            // Background Renderer für ARCore
            if (useARCore) {
                backgroundRenderer = BackgroundRenderer()
                backgroundRenderer?.createOnGlThread(context)
            }
            
            // OpenGL-Einstellungen
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            
            // Matrizen initialisieren
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.setIdentityM(viewMatrix, 0)
            Matrix.setIdentityM(projectionMatrix, 0)
            
            Log.d(TAG, "EMF Renderer erfolgreich initialisiert")
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler bei Renderer-Initialisierung", e)
        }
    }
    
    /**
     * EMF-Punkt hinzufügen
     */
    fun addEMFPoint(reading: EMFReading) {
        val point = EMFPoint(
            x = reading.positionX.toFloat(),
            y = reading.positionY.toFloat(),
            z = reading.positionZ.toFloat(),
            signalStrength = reading.signalStrength.toFloat(),
            materialType = reading.materialType,
            frequency = reading.frequency.toFloat(),
            phase = reading.phase.toFloat(),
            temperature = reading.temperature.toFloat(),
            depth = reading.depth.toFloat()
        )
        
        emfPoints.add(point)
        updateBuffers()
    }
    
    /**
     * EMF-Punkte rendern
     */
    fun render(projectionMatrix: FloatArray, viewMatrix: FloatArray) {
        if (emfPoints.isEmpty()) return
        
        try {
            // Shader-Programm verwenden
            GLES20.glUseProgram(shaderProgram)
            
            // Matrizen setzen
            this.projectionMatrix.copyFrom(projectionMatrix)
            this.viewMatrix.copyFrom(viewMatrix)
            
            // Model-View-Projection Matrix berechnen
            Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
            
            // Matrix an Shader übergeben
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
            
            // Point Size setzen
            GLES20.glUniform1f(pointSizeHandle, pointSize * scaleFactor)
            
            // Vertex Buffer binden
            vertexBuffer?.let { buffer ->
                GLES20.glEnableVertexAttribArray(positionHandle)
                GLES20.glVertexAttribPointer(
                    positionHandle,
                    COORDS_PER_VERTEX,
                    GLES20.GL_FLOAT,
                    false,
                    VERTEX_STRIDE,
                    buffer
                )
            }
            
            // Punkte rendern
            renderPoints()
            
            // Vertex Array deaktivieren
            GLES20.glDisableVertexAttribArray(positionHandle)
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Rendern", e)
        }
    }
    
    /**
     * Punkte basierend auf Visualisierungsmodus rendern
     */
    private fun renderPoints() {
        emfPoints.forEachIndexed { index, point ->
            val color = getPointColor(point)
            
            // Farbe setzen
            GLES20.glUniform4fv(colorHandle, 1, color, 0)
            
            // Punkt zeichnen
            GLES20.glDrawArrays(GLES20.GL_POINTS, index, 1)
        }
    }
    
    /**
     * Punkt-Farbe basierend auf Visualisierungsmodus bestimmen
     */
    private fun getPointColor(point: EMFPoint): FloatArray {
        return when (visualizationMode) {
            VisualizationMode.SIGNAL_STRENGTH -> getSignalStrengthColor(point.signalStrength)
            VisualizationMode.MATERIAL_TYPE -> getMaterialTypeColor(point.materialType)
            VisualizationMode.DEPTH -> getDepthColor(point.depth)
            VisualizationMode.FREQUENCY -> getFrequencyColor(point.frequency)
            VisualizationMode.PHASE -> getPhaseColor(point.phase)
            VisualizationMode.TEMPERATURE -> getTemperatureColor(point.temperature)
            VisualizationMode.COMBINED -> getCombinedColor(point)
        }
    }
    
    /**
     * Signalstärke-basierte Farbe
     */
    private fun getSignalStrengthColor(strength: Float): FloatArray {
        val normalized = (strength / 1000.0f).coerceIn(0.0f, 1.0f)
        return floatArrayOf(
            normalized,           // Rot
            1.0f - normalized,    // Grün
            0.5f,                 // Blau
            0.8f                  // Alpha
        )
    }
    
    /**
     * Material-Typ-basierte Farbe
     */
    private fun getMaterialTypeColor(materialType: MaterialType): FloatArray {
        return when (materialType) {
            MaterialType.IRON -> floatArrayOf(0.7f, 0.3f, 0.3f, 0.8f)      // Rot
            MaterialType.ALUMINUM -> floatArrayOf(0.8f, 0.8f, 0.8f, 0.8f)  // Silber
            MaterialType.COPPER -> floatArrayOf(0.8f, 0.5f, 0.2f, 0.8f)    // Kupfer
            MaterialType.STEEL -> floatArrayOf(0.5f, 0.5f, 0.5f, 0.8f)     // Grau
            MaterialType.GOLD -> floatArrayOf(1.0f, 0.8f, 0.0f, 0.8f)      // Gold
            MaterialType.SILVER -> floatArrayOf(0.9f, 0.9f, 0.9f, 0.8f)    // Silber
            MaterialType.BRONZE -> floatArrayOf(0.8f, 0.5f, 0.2f, 0.8f)    // Bronze
            else -> floatArrayOf(0.5f, 0.5f, 0.5f, 0.8f)                   // Grau (Unbekannt)
        }
    }
    
    /**
     * Tiefen-basierte Farbe
     */
    private fun getDepthColor(depth: Float): FloatArray {
        val normalized = (depth / 10.0f).coerceIn(0.0f, 1.0f)
        return floatArrayOf(
            0.2f,                 // Rot
            0.5f,                 // Grün
            normalized,           // Blau (tiefer = blauer)
            0.8f                  // Alpha
        )
    }
    
    /**
     * Frequenz-basierte Farbe
     */
    private fun getFrequencyColor(frequency: Float): FloatArray {
        val normalized = (frequency / 1000.0f).coerceIn(0.0f, 1.0f)
        val hue = normalized * 360.0f // Regenbogen-Spektrum
        return hsvToRgb(hue, 1.0f, 1.0f, 0.8f)
    }
    
    /**
     * Phasen-basierte Farbe
     */
    private fun getPhaseColor(phase: Float): FloatArray {
        val normalized = ((phase + 180.0f) / 360.0f).coerceIn(0.0f, 1.0f)
        return floatArrayOf(
            sin(normalized * PI).toFloat(),
            cos(normalized * PI).toFloat(),
            0.5f,
            0.8f
        )
    }
    
    /**
     * Temperatur-basierte Farbe
     */
    private fun getTemperatureColor(temperature: Float): FloatArray {
        val normalized = ((temperature - 20.0f) / 40.0f).coerceIn(0.0f, 1.0f)
        return floatArrayOf(
            normalized,           // Rot (heißer = röter)
            0.5f,                 // Grün
            1.0f - normalized,    // Blau (kälter = blauer)
            0.8f                  // Alpha
        )
    }
    
    /**
     * Kombinierte Farbe
     */
    private fun getCombinedColor(point: EMFPoint): FloatArray {
        val signalColor = getSignalStrengthColor(point.signalStrength)
        val materialColor = getMaterialTypeColor(point.materialType)
        
        return floatArrayOf(
            (signalColor[0] + materialColor[0]) / 2.0f,
            (signalColor[1] + materialColor[1]) / 2.0f,
            (signalColor[2] + materialColor[2]) / 2.0f,
            0.8f
        )
    }
    
    /**
     * HSV zu RGB konvertieren
     */
    private fun hsvToRgb(h: Float, s: Float, v: Float, a: Float): FloatArray {
        val c = v * s
        val x = c * (1 - abs((h / 60.0f) % 2 - 1))
        val m = v - c
        
        val (r, g, b) = when {
            h < 60 -> Triple(c, x, 0.0f)
            h < 120 -> Triple(x, c, 0.0f)
            h < 180 -> Triple(0.0f, c, x)
            h < 240 -> Triple(0.0f, x, c)
            h < 300 -> Triple(x, 0.0f, c)
            else -> Triple(c, 0.0f, x)
        }
        
        return floatArrayOf(r + m, g + m, b + m, a)
    }
    
    /**
     * Vertex- und Color-Buffer aktualisieren
     */
    private fun updateBuffers() {
        if (emfPoints.isEmpty()) return
        
        // Vertex-Daten erstellen
        val vertices = FloatArray(emfPoints.size * COORDS_PER_VERTEX)
        emfPoints.forEachIndexed { index, point ->
            val offset = index * COORDS_PER_VERTEX
            vertices[offset] = point.x * scaleFactor
            vertices[offset + 1] = point.y * scaleFactor
            vertices[offset + 2] = point.z * scaleFactor
        }
        
        // Vertex Buffer erstellen
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(vertices)
                position(0)
            }
    }
    
    /**
     * Shader-Programm erstellen
     */
    private fun createShaderProgram(): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE)
        
        return GLES20.glCreateProgram().also { program ->
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)
            
            // Link-Status prüfen
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val error = GLES20.glGetProgramInfoLog(program)
                GLES20.glDeleteProgram(program)
                throw RuntimeException("Shader-Programm Link-Fehler: $error")
            }
        }
    }
    
    /**
     * Shader laden
     */
    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            
            // Compile-Status prüfen
            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                val error = GLES20.glGetShaderInfoLog(shader)
                GLES20.glDeleteShader(shader)
                throw RuntimeException("Shader Compile-Fehler: $error")
            }
        }
    }
    
    /**
     * Visualisierungsmodus setzen
     */
    fun setVisualizationMode(mode: VisualizationMode) {
        visualizationMode = mode
    }
    
    /**
     * Skalierungsfaktor setzen
     */
    fun setScaleFactor(factor: Float) {
        scaleFactor = factor
        updateBuffers()
    }
    
    /**
     * Punkt-Größe setzen
     */
    fun setPointSize(size: Float) {
        pointSize = size
    }
    
    /**
     * Alle Punkte löschen
     */
    fun clearPoints() {
        emfPoints.clear()
        updateBuffers()
    }
    
    /**
     * Ressourcen freigeben
     */
    fun cleanup() {
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram)
            shaderProgram = 0
        }
        
        backgroundRenderer?.cleanup()
        backgroundRenderer = null
        
        emfPoints.clear()
        vertexBuffer = null
        colorBuffer = null
        
        Log.d(TAG, "EMF Renderer bereinigt")
    }
    
    /**
     * Array-Kopierfunktion
     */
    private fun FloatArray.copyFrom(source: FloatArray) {
        System.arraycopy(source, 0, this, 0, minOf(this.size, source.size))
    }
}

/**
 * EMF-Punkt für Rendering
 */
data class EMFPoint(
    val x: Float,
    val y: Float,
    val z: Float,
    val signalStrength: Float,
    val materialType: MaterialType,
    val frequency: Float,
    val phase: Float,
    val temperature: Float,
    val depth: Float
)

/**
 * Background Renderer für ARCore (Placeholder)
 */
class BackgroundRenderer {
    var textureId: Int = 0
        private set
    
    fun createOnGlThread(context: Context) {
        // ARCore Background Renderer Implementierung
    }
    
    fun draw(frame: com.google.ar.core.Frame) {
        // Background Drawing Implementierung
    }
    
    fun cleanup() {
        // Cleanup Implementierung
    }
}
