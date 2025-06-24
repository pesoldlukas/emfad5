package com.emfad.app.ar

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.*

/**
 * AR-Renderer für EMF-Visualisierung
 * Implementiert OpenGL ES Rendering für Augmented Reality
 */
class ARRenderer {
    private var shaderProgram: Int = 0
    private var positionHandle: Int = 0
    private var colorHandle: Int = 0
    private var mvpMatrixHandle: Int = 0
    
    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    
    private var sphereVertexBuffer: FloatBuffer? = null
    private var sphereIndexBuffer: ByteBuffer? = null
    private var sphereVertexCount = 0
    
    private var fieldLinesBuffer: FloatBuffer? = null
    private var fieldLinesCount = 0
    
    /**
     * Renderer initialisieren
     */
    fun initialize(): Boolean {
        return try {
            setupShaders()
            setupGeometry()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Shader-Programme einrichten
     */
    private fun setupShaders() {
        val vertexShaderCode = """
            attribute vec4 vPosition;
            attribute vec4 vColor;
            uniform mat4 uMVPMatrix;
            varying vec4 fColor;
            
            void main() {
                gl_Position = uMVPMatrix * vPosition;
                fColor = vColor;
                gl_PointSize = 8.0;
            }
        """.trimIndent()
        
        val fragmentShaderCode = """
            precision mediump float;
            varying vec4 fColor;
            
            void main() {
                gl_FragColor = fColor;
            }
        """.trimIndent()
        
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        
        shaderProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(shaderProgram, vertexShader)
        GLES20.glAttachShader(shaderProgram, fragmentShader)
        GLES20.glLinkProgram(shaderProgram)
        
        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition")
        colorHandle = GLES20.glGetAttribLocation(shaderProgram, "vColor")
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix")
    }
    
    /**
     * Shader laden
     */
    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
    
    /**
     * Geometrie einrichten
     */
    private fun setupGeometry() {
        createSphere()
        createFieldLines()
    }
    
    /**
     * Kugel für EMF-Quellen erstellen
     */
    private fun createSphere() {
        val radius = 0.05f
        val stacks = 16
        val slices = 16
        
        val vertices = mutableListOf<Float>()
        val indices = mutableListOf<Byte>()
        
        // Vertices generieren
        for (i in 0..stacks) {
            val phi = PI * i / stacks
            for (j in 0..slices) {
                val theta = 2 * PI * j / slices
                
                val x = (radius * sin(phi) * cos(theta)).toFloat()
                val y = (radius * cos(phi)).toFloat()
                val z = (radius * sin(phi) * sin(theta)).toFloat()
                
                vertices.addAll(listOf(x, y, z))
            }
        }
        
        // Indices generieren
        for (i in 0 until stacks) {
            for (j in 0 until slices) {
                val first = (i * (slices + 1) + j).toByte()
                val second = (first + slices + 1).toByte()
                
                indices.addAll(listOf(first, second, (first + 1).toByte()))
                indices.addAll(listOf(second, (second + 1).toByte(), (first + 1).toByte()))
            }
        }
        
        sphereVertexCount = indices.size
        
        // Vertex Buffer erstellen
        val vertexArray = vertices.toFloatArray()
        sphereVertexBuffer = ByteBuffer.allocateDirect(vertexArray.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertexArray)
        sphereVertexBuffer?.position(0)
        
        // Index Buffer erstellen
        sphereIndexBuffer = ByteBuffer.allocateDirect(indices.size)
            .order(ByteOrder.nativeOrder())
            .put(indices.toByteArray())
        sphereIndexBuffer?.position(0)
    }
    
    /**
     * Feldlinien erstellen
     */
    private fun createFieldLines() {
        val lines = mutableListOf<Float>()
        val numLines = 20
        val maxDistance = 2.0f
        
        for (i in 0 until numLines) {
            val angle = 2 * PI * i / numLines
            val startX = (0.1f * cos(angle)).toFloat()
            val startZ = (0.1f * sin(angle)).toFloat()
            
            // Feldlinie von Quelle nach außen
            for (j in 0..50) {
                val t = j / 50.0f
                val distance = 0.1f + t * maxDistance
                
                val x = (distance * cos(angle)).toFloat()
                val y = calculateFieldHeight(distance, angle.toFloat())
                val z = (distance * sin(angle)).toFloat()
                
                lines.addAll(listOf(x, y, z))
            }
        }
        
        fieldLinesCount = lines.size / 3
        
        val lineArray = lines.toFloatArray()
        fieldLinesBuffer = ByteBuffer.allocateDirect(lineArray.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(lineArray)
        fieldLinesBuffer?.position(0)
    }
    
    /**
     * Feldhöhe berechnen (vereinfachtes EMF-Feld)
     */
    private fun calculateFieldHeight(distance: Float, angle: Float): Float {
        val fieldStrength = 1.0f / (distance * distance + 0.01f)
        return fieldStrength * 0.1f * sin(angle * 3)
    }
    
    /**
     * EMF-Daten rendern
     */
    fun renderEMFData(emfData: EMFVisualizationData, cameraMatrix: FloatArray) {
        GLES20.glUseProgram(shaderProgram)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        
        // View Matrix setzen
        System.arraycopy(cameraMatrix, 0, viewMatrix, 0, 16)
        
        // EMF-Quellen rendern
        renderEMFSources(emfData.sources)
        
        // Feldlinien rendern
        renderFieldLines(emfData.fieldStrength)
        
        // Heatmap rendern
        renderHeatmap(emfData.heatmapData)
        
        // Messungen rendern
        renderMeasurements(emfData.measurements)
    }
    
    /**
     * EMF-Quellen rendern
     */
    private fun renderEMFSources(sources: List<EMFSource>) {
        for (source in sources) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, source.x, source.y, source.z)
            Matrix.scaleM(modelMatrix, 0, source.intensity, source.intensity, source.intensity)
            
            Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
            
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
            
            // Farbe basierend auf Intensität
            val color = getIntensityColor(source.intensity)
            GLES20.glVertexAttrib4f(colorHandle, color[0], color[1], color[2], color[3])
            
            // Kugel zeichnen
            sphereVertexBuffer?.position(0)
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, sphereVertexBuffer)
            GLES20.glEnableVertexAttribArray(positionHandle)
            
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, sphereVertexCount, GLES20.GL_UNSIGNED_BYTE, sphereIndexBuffer)
        }
    }
    
    /**
     * Feldlinien rendern
     */
    private fun renderFieldLines(fieldStrength: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        
        // Feldlinien-Farbe
        val alpha = (fieldStrength * 0.5f).coerceIn(0.1f, 0.8f)
        GLES20.glVertexAttrib4f(colorHandle, 0.0f, 1.0f, 1.0f, alpha)
        
        fieldLinesBuffer?.position(0)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, fieldLinesBuffer)
        GLES20.glEnableVertexAttribArray(positionHandle)
        
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, fieldLinesCount)
    }
    
    /**
     * Heatmap rendern
     */
    private fun renderHeatmap(heatmapData: Array<Array<Float>>) {
        val gridSize = heatmapData.size
        val cellSize = 0.1f
        
        for (i in heatmapData.indices) {
            for (j in heatmapData[i].indices) {
                val intensity = heatmapData[i][j]
                if (intensity > 0.1f) {
                    val x = (i - gridSize / 2) * cellSize
                    val z = (j - gridSize / 2) * cellSize
                    
                    renderHeatmapCell(x, 0f, z, intensity)
                }
            }
        }
    }
    
    /**
     * Einzelne Heatmap-Zelle rendern
     */
    private fun renderHeatmapCell(x: Float, y: Float, z: Float, intensity: Float) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.scaleM(modelMatrix, 0, 0.05f, intensity * 0.2f, 0.05f)
        
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        
        val color = getIntensityColor(intensity)
        GLES20.glVertexAttrib4f(colorHandle, color[0], color[1], color[2], 0.6f)
        
        // Würfel zeichnen (vereinfacht als Kugel)
        sphereVertexBuffer?.position(0)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, sphereVertexBuffer)
        GLES20.glEnableVertexAttribArray(positionHandle)
        
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, sphereVertexCount, GLES20.GL_UNSIGNED_BYTE, sphereIndexBuffer)
    }
    
    /**
     * Messungen rendern
     */
    private fun renderMeasurements(measurements: List<EMFMeasurement>) {
        for (measurement in measurements) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, measurement.x, measurement.y + 0.1f, measurement.z)
            Matrix.scaleM(modelMatrix, 0, 0.02f, 0.02f, 0.02f)
            
            Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
            
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
            
            val color = getIntensityColor(measurement.value)
            GLES20.glVertexAttrib4f(colorHandle, color[0], color[1], color[2], 1.0f)
            
            // Punkt zeichnen
            GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1)
        }
    }
    
    /**
     * Farbe basierend auf Intensität
     */
    private fun getIntensityColor(intensity: Float): FloatArray {
        val normalizedIntensity = intensity.coerceIn(0f, 1f)
        
        return when {
            normalizedIntensity < 0.3f -> floatArrayOf(0f, 1f, 0f, 1f) // Grün
            normalizedIntensity < 0.6f -> floatArrayOf(1f, 1f, 0f, 1f) // Gelb
            normalizedIntensity < 0.8f -> floatArrayOf(1f, 0.5f, 0f, 1f) // Orange
            else -> floatArrayOf(1f, 0f, 0f, 1f) // Rot
        }
    }
    
    /**
     * Projektionsmatrix setzen
     */
    fun setProjectionMatrix(projectionMatrix: FloatArray) {
        System.arraycopy(projectionMatrix, 0, this.projectionMatrix, 0, 16)
    }
    
    /**
     * Viewport setzen
     */
    fun setViewport(width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        
        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 10f)
    }
    
    /**
     * Ressourcen freigeben
     */
    fun cleanup() {
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram)
            shaderProgram = 0
        }
    }
}

/**
 * Datenklassen für AR-Visualisierung
 */
data class EMFVisualizationData(
    val sources: List<EMFSource>,
    val fieldStrength: Float,
    val heatmapData: Array<Array<Float>>,
    val measurements: List<EMFMeasurement>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as EMFVisualizationData
        
        if (sources != other.sources) return false
        if (fieldStrength != other.fieldStrength) return false
        if (!heatmapData.contentDeepEquals(other.heatmapData)) return false
        if (measurements != other.measurements) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = sources.hashCode()
        result = 31 * result + fieldStrength.hashCode()
        result = 31 * result + heatmapData.contentDeepHashCode()
        result = 31 * result + measurements.hashCode()
        return result
    }
}

data class EMFSource(
    val x: Float,
    val y: Float,
    val z: Float,
    val intensity: Float,
    val type: EMFSourceType
)

data class EMFMeasurement(
    val x: Float,
    val y: Float,
    val z: Float,
    val value: Float,
    val timestamp: Long
)

enum class EMFSourceType {
    ELECTRICAL_DEVICE,
    WIRELESS_TRANSMITTER,
    POWER_LINE,
    UNKNOWN
}
