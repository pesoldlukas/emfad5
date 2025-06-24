package com.emfad.app.ar

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.*

/**
 * AR-Visualisierer für EMF-Daten
 * Erweiterte 3D-Visualisierung mit Partikelsystemen und Effekten
 */
class ARVisualizer {
    private var shaderProgram: Int = 0
    private var particleShaderProgram: Int = 0
    
    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    
    // Partikel-System
    private val particles = mutableListOf<Particle>()
    private var particleBuffer: FloatBuffer? = null
    private val maxParticles = 1000
    
    // Feldlinien
    private var fieldLinesBuffer: FloatBuffer? = null
    private var fieldLinesCount = 0
    
    // Heatmap
    private var heatmapVertices: FloatBuffer? = null
    private var heatmapIndices: ByteBuffer? = null
    private var heatmapVertexCount = 0
    
    /**
     * Visualisierer initialisieren
     */
    fun initialize(): Boolean {
        return try {
            setupShaders()
            setupParticleSystem()
            setupFieldLines()
            setupHeatmap()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Shader-Programme einrichten
     */
    private fun setupShaders() {
        // Standard-Vertex-Shader
        val vertexShaderCode = """
            attribute vec4 vPosition;
            attribute vec4 vColor;
            attribute float vSize;
            uniform mat4 uMVPMatrix;
            varying vec4 fColor;
            
            void main() {
                gl_Position = uMVPMatrix * vPosition;
                fColor = vColor;
                gl_PointSize = vSize;
            }
        """.trimIndent()
        
        // Standard-Fragment-Shader
        val fragmentShaderCode = """
            precision mediump float;
            varying vec4 fColor;
            
            void main() {
                gl_FragColor = fColor;
            }
        """.trimIndent()
        
        // Partikel-Vertex-Shader
        val particleVertexShaderCode = """
            attribute vec4 vPosition;
            attribute vec4 vColor;
            attribute float vSize;
            attribute float vLife;
            uniform mat4 uMVPMatrix;
            uniform float uTime;
            varying vec4 fColor;
            varying float fLife;
            
            void main() {
                vec4 pos = vPosition;
                pos.y += sin(uTime + vPosition.x) * 0.1 * vLife;
                gl_Position = uMVPMatrix * pos;
                fColor = vColor;
                fLife = vLife;
                gl_PointSize = vSize * vLife;
            }
        """.trimIndent()
        
        // Partikel-Fragment-Shader
        val particleFragmentShaderCode = """
            precision mediump float;
            varying vec4 fColor;
            varying float fLife;
            
            void main() {
                vec2 coord = gl_PointCoord - vec2(0.5);
                float dist = length(coord);
                if (dist > 0.5) discard;
                
                float alpha = (1.0 - dist * 2.0) * fLife;
                gl_FragColor = vec4(fColor.rgb, fColor.a * alpha);
            }
        """.trimIndent()
        
        // Standard-Shader kompilieren
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        
        shaderProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(shaderProgram, vertexShader)
        GLES20.glAttachShader(shaderProgram, fragmentShader)
        GLES20.glLinkProgram(shaderProgram)
        
        // Partikel-Shader kompilieren
        val particleVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, particleVertexShaderCode)
        val particleFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, particleFragmentShaderCode)
        
        particleShaderProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(particleShaderProgram, particleVertexShader)
        GLES20.glAttachShader(particleShaderProgram, particleFragmentShader)
        GLES20.glLinkProgram(particleShaderProgram)
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
     * Partikel-System einrichten
     */
    private fun setupParticleSystem() {
        // Partikel-Buffer erstellen
        val particleData = FloatArray(maxParticles * 8) // Position(3) + Color(4) + Size(1)
        particleBuffer = ByteBuffer.allocateDirect(particleData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    }
    
    /**
     * Feldlinien einrichten
     */
    private fun setupFieldLines() {
        val lines = mutableListOf<Float>()
        val numLines = 24
        val maxDistance = 3.0f
        
        for (i in 0 until numLines) {
            val angle = 2 * PI * i / numLines
            
            // Feldlinie von Zentrum nach außen
            for (j in 0..100) {
                val t = j / 100.0f
                val distance = 0.1f + t * maxDistance
                
                val x = (distance * cos(angle)).toFloat()
                val y = calculateFieldHeight(distance, angle.toFloat(), t)
                val z = (distance * sin(angle)).toFloat()
                
                lines.addAll(listOf(x, y, z))
                
                // Farbe basierend auf Entfernung
                val intensity = 1.0f - t
                lines.addAll(listOf(0.0f, intensity, 1.0f, 0.7f))
            }
        }
        
        fieldLinesCount = lines.size / 7 // Position(3) + Color(4)
        
        val lineArray = lines.toFloatArray()
        fieldLinesBuffer = ByteBuffer.allocateDirect(lineArray.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(lineArray)
        fieldLinesBuffer?.position(0)
    }
    
    /**
     * Heatmap einrichten
     */
    private fun setupHeatmap() {
        val gridSize = 20
        val vertices = mutableListOf<Float>()
        val indices = mutableListOf<Byte>()
        
        // Grid-Vertices erstellen
        for (i in 0..gridSize) {
            for (j in 0..gridSize) {
                val x = (i - gridSize / 2) * 0.1f
                val z = (j - gridSize / 2) * 0.1f
                val y = 0f
                
                vertices.addAll(listOf(x, y, z))
                
                // Textur-Koordinaten
                vertices.addAll(listOf(i.toFloat() / gridSize, j.toFloat() / gridSize))
            }
        }
        
        // Indices für Triangles
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                val topLeft = (i * (gridSize + 1) + j).toByte()
                val topRight = (topLeft + 1).toByte()
                val bottomLeft = (topLeft + gridSize + 1).toByte()
                val bottomRight = (bottomLeft + 1).toByte()
                
                // Erstes Triangle
                indices.addAll(listOf(topLeft, bottomLeft, topRight))
                // Zweites Triangle
                indices.addAll(listOf(topRight, bottomLeft, bottomRight))
            }
        }
        
        heatmapVertexCount = indices.size
        
        // Vertex Buffer
        val vertexArray = vertices.toFloatArray()
        heatmapVertices = ByteBuffer.allocateDirect(vertexArray.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertexArray)
        heatmapVertices?.position(0)
        
        // Index Buffer
        heatmapIndices = ByteBuffer.allocateDirect(indices.size)
            .order(ByteOrder.nativeOrder())
            .put(indices.toByteArray())
        heatmapIndices?.position(0)
    }
    
    /**
     * Feldhöhe berechnen
     */
    private fun calculateFieldHeight(distance: Float, angle: Float, t: Float): Float {
        val fieldStrength = 1.0f / (distance * distance + 0.01f)
        val wave = sin(angle * 3 + t * 2 * PI).toFloat()
        return fieldStrength * 0.2f * wave
    }
    
    /**
     * EMF-Visualisierung rendern
     */
    fun renderEMFVisualization(
        visualizationData: EMFVisualizationData,
        cameraMatrix: FloatArray,
        time: Float
    ) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        
        // View Matrix setzen
        System.arraycopy(cameraMatrix, 0, viewMatrix, 0, 16)
        
        // Partikel-System aktualisieren und rendern
        updateParticles(visualizationData, time)
        renderParticles(time)
        
        // Feldlinien rendern
        renderFieldLines(visualizationData.fieldStrength)
        
        // Heatmap rendern
        renderHeatmap(visualizationData.heatmapData)
        
        // EMF-Quellen rendern
        renderEMFSources(visualizationData.sources)
        
        // Messungen rendern
        renderMeasurements(visualizationData.measurements)
        
        // Effekte rendern
        renderEffects(visualizationData, time)
    }
    
    /**
     * Partikel aktualisieren
     */
    private fun updateParticles(data: EMFVisualizationData, time: Float) {
        // Alte Partikel entfernen
        particles.removeAll { it.life <= 0f }
        
        // Neue Partikel für EMF-Quellen erstellen
        for (source in data.sources) {
            if (particles.size < maxParticles && Math.random() < 0.1) {
                val particle = Particle(
                    x = source.x + (Math.random() - 0.5).toFloat() * 0.2f,
                    y = source.y + (Math.random() - 0.5).toFloat() * 0.2f,
                    z = source.z + (Math.random() - 0.5).toFloat() * 0.2f,
                    vx = (Math.random() - 0.5).toFloat() * 0.01f,
                    vy = Math.random().toFloat() * 0.02f,
                    vz = (Math.random() - 0.5).toFloat() * 0.01f,
                    life = 1.0f,
                    size = source.intensity * 10f + 5f,
                    color = getIntensityColor(source.intensity)
                )
                particles.add(particle)
            }
        }
        
        // Partikel aktualisieren
        for (particle in particles) {
            particle.x += particle.vx
            particle.y += particle.vy
            particle.z += particle.vz
            particle.life -= 0.01f
            
            // Schwerkraft simulieren
            particle.vy -= 0.001f
        }
    }
    
    /**
     * Partikel rendern
     */
    private fun renderParticles(time: Float) {
        if (particles.isEmpty()) return
        
        GLES20.glUseProgram(particleShaderProgram)
        
        // Partikel-Daten in Buffer laden
        val particleData = mutableListOf<Float>()
        for (particle in particles) {
            // Position
            particleData.addAll(listOf(particle.x, particle.y, particle.z))
            // Farbe
            particleData.addAll(particle.color.toList())
            // Größe
            particleData.add(particle.size)
            // Leben
            particleData.add(particle.life)
        }
        
        particleBuffer?.clear()
        particleBuffer?.put(particleData.toFloatArray())
        particleBuffer?.position(0)
        
        // Matrix setzen
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        
        val mvpMatrixHandle = GLES20.glGetUniformLocation(particleShaderProgram, "uMVPMatrix")
        val timeHandle = GLES20.glGetUniformLocation(particleShaderProgram, "uTime")
        
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniform1f(timeHandle, time)
        
        // Partikel zeichnen
        val positionHandle = GLES20.glGetAttribLocation(particleShaderProgram, "vPosition")
        val colorHandle = GLES20.glGetAttribLocation(particleShaderProgram, "vColor")
        val sizeHandle = GLES20.glGetAttribLocation(particleShaderProgram, "vSize")
        val lifeHandle = GLES20.glGetAttribLocation(particleShaderProgram, "vLife")
        
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 36, particleBuffer)
        GLES20.glEnableVertexAttribArray(positionHandle)
        
        particleBuffer?.position(3)
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 36, particleBuffer)
        GLES20.glEnableVertexAttribArray(colorHandle)
        
        particleBuffer?.position(7)
        GLES20.glVertexAttribPointer(sizeHandle, 1, GLES20.GL_FLOAT, false, 36, particleBuffer)
        GLES20.glEnableVertexAttribArray(sizeHandle)
        
        particleBuffer?.position(8)
        GLES20.glVertexAttribPointer(lifeHandle, 1, GLES20.GL_FLOAT, false, 36, particleBuffer)
        GLES20.glEnableVertexAttribArray(lifeHandle)
        
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, particles.size)
    }
    
    /**
     * Feldlinien rendern
     */
    private fun renderFieldLines(fieldStrength: Float) {
        GLES20.glUseProgram(shaderProgram)
        
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        
        val mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        
        val positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition")
        val colorHandle = GLES20.glGetAttribLocation(shaderProgram, "vColor")
        
        fieldLinesBuffer?.position(0)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 28, fieldLinesBuffer)
        GLES20.glEnableVertexAttribArray(positionHandle)
        
        fieldLinesBuffer?.position(3)
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 28, fieldLinesBuffer)
        GLES20.glEnableVertexAttribArray(colorHandle)
        
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, fieldLinesCount)
    }
    
    /**
     * Heatmap rendern
     */
    private fun renderHeatmap(heatmapData: Array<Array<Float>>) {
        // Heatmap-Rendering-Implementierung
        // Vereinfacht für diese Demo
    }
    
    /**
     * EMF-Quellen rendern
     */
    private fun renderEMFSources(sources: List<EMFSource>) {
        // EMF-Quellen-Rendering-Implementierung
        // Vereinfacht für diese Demo
    }
    
    /**
     * Messungen rendern
     */
    private fun renderMeasurements(measurements: List<EMFMeasurement>) {
        // Messungen-Rendering-Implementierung
        // Vereinfacht für diese Demo
    }
    
    /**
     * Effekte rendern
     */
    private fun renderEffects(data: EMFVisualizationData, time: Float) {
        // Zusätzliche visuelle Effekte
        // Pulsing, Glowing, etc.
    }
    
    /**
     * Farbe basierend auf Intensität
     */
    private fun getIntensityColor(intensity: Float): FloatArray {
        val normalizedIntensity = intensity.coerceIn(0f, 1f)
        
        return when {
            normalizedIntensity < 0.3f -> floatArrayOf(0f, 1f, 0f, 0.8f) // Grün
            normalizedIntensity < 0.6f -> floatArrayOf(1f, 1f, 0f, 0.8f) // Gelb
            normalizedIntensity < 0.8f -> floatArrayOf(1f, 0.5f, 0f, 0.8f) // Orange
            else -> floatArrayOf(1f, 0f, 0f, 0.8f) // Rot
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
        if (particleShaderProgram != 0) {
            GLES20.glDeleteProgram(particleShaderProgram)
            particleShaderProgram = 0
        }
        particles.clear()
    }
}

/**
 * Partikel-Klasse
 */
data class Particle(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    var life: Float,
    var size: Float,
    val color: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as Particle
        
        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false
        if (vx != other.vx) return false
        if (vy != other.vy) return false
        if (vz != other.vz) return false
        if (life != other.life) return false
        if (size != other.size) return false
        if (!color.contentEquals(other.color)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        result = 31 * result + vx.hashCode()
        result = 31 * result + vy.hashCode()
        result = 31 * result + vz.hashCode()
        result = 31 * result + life.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + color.contentHashCode()
        return result
    }
}
