package com.emfad.app.services.export

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.util.Log
import com.emfad.app.models.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

/**
 * EMFAD PDF Generator
 * Erstellt detaillierte PDF-Berichte für EMF-Messungen
 * Samsung S21 Ultra optimiert
 */
class PDFGenerator(private val context: Context) {
    
    companion object {
        private const val TAG = "PDFGenerator"
        private const val PAGE_WIDTH = 595 // A4 width in points
        private const val PAGE_HEIGHT = 842 // A4 height in points
        private const val MARGIN = 50
        private const val LINE_HEIGHT = 20
        private val DATE_FORMAT = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
    }
    
    private val paint = Paint().apply {
        isAntiAlias = true
        textSize = 12f
        color = Color.BLACK
    }
    
    private val titlePaint = Paint().apply {
        isAntiAlias = true
        textSize = 18f
        color = Color.BLACK
        typeface = Typeface.DEFAULT_BOLD
    }
    
    private val headerPaint = Paint().apply {
        isAntiAlias = true
        textSize = 14f
        color = Color.BLACK
        typeface = Typeface.DEFAULT_BOLD
    }
    
    /**
     * Session-Bericht generieren
     */
    fun generateSessionReport(
        session: MeasurementSession,
        readings: List<EMFReading>,
        analyses: List<MaterialAnalysis>,
        outputFile: File
    ) {
        try {
            Log.d(TAG, "Generiere PDF-Bericht für Session: ${session.name}")
            
            val document = PdfDocument()
            var currentPage = 1
            var yPosition = MARGIN + 50
            
            // Seite 1: Übersicht und Session-Info
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPage).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            
            yPosition = drawTitle(canvas, "EMFAD Messbericht", yPosition)
            yPosition = drawSessionInfo(canvas, session, yPosition)
            yPosition = drawSessionStatistics(canvas, session, readings, yPosition)
            
            document.finishPage(page)
            currentPage++
            
            // Seite 2: Messdaten-Übersicht
            if (readings.isNotEmpty()) {
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPage).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPosition = MARGIN + 50
                
                yPosition = drawHeader(canvas, "Messdaten-Übersicht", yPosition)
                yPosition = drawMeasurementSummary(canvas, readings, yPosition)
                yPosition = drawSignalStrengthChart(canvas, readings, yPosition)
                
                document.finishPage(page)
                currentPage++
            }
            
            // Seite 3: Material-Analysen
            if (analyses.isNotEmpty()) {
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPage).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPosition = MARGIN + 50
                
                yPosition = drawHeader(canvas, "Material-Analysen", yPosition)
                yPosition = drawMaterialAnalyses(canvas, analyses, yPosition)
                yPosition = drawMaterialDistribution(canvas, analyses, yPosition)
                
                document.finishPage(page)
                currentPage++
            }
            
            // Seite 4: Detaillierte Daten (falls nötig)
            if (readings.size > 20) {
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPage).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPosition = MARGIN + 50
                
                yPosition = drawHeader(canvas, "Detaillierte Messdaten", yPosition)
                yPosition = drawDetailedMeasurements(canvas, readings.take(50), yPosition)
                
                document.finishPage(page)
            }
            
            // PDF speichern
            FileOutputStream(outputFile).use { outputStream ->
                document.writeTo(outputStream)
            }
            
            document.close()
            Log.d(TAG, "PDF-Bericht erfolgreich erstellt: ${outputFile.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Generieren des PDF-Berichts", e)
            throw e
        }
    }
    
    /**
     * Titel zeichnen
     */
    private fun drawTitle(canvas: Canvas, title: String, yPos: Int): Int {
        canvas.drawText(title, MARGIN.toFloat(), yPos.toFloat(), titlePaint)
        
        // Linie unter Titel
        canvas.drawLine(
            MARGIN.toFloat(),
            (yPos + 10).toFloat(),
            (PAGE_WIDTH - MARGIN).toFloat(),
            (yPos + 10).toFloat(),
            paint
        )
        
        return yPos + 40
    }
    
    /**
     * Header zeichnen
     */
    private fun drawHeader(canvas: Canvas, header: String, yPos: Int): Int {
        canvas.drawText(header, MARGIN.toFloat(), yPos.toFloat(), headerPaint)
        return yPos + 30
    }
    
    /**
     * Session-Informationen zeichnen
     */
    private fun drawSessionInfo(canvas: Canvas, session: MeasurementSession, yPos: Int): Int {
        var currentY = drawHeader(canvas, "Session-Informationen", yPos)
        
        val info = listOf(
            "Name: ${session.name}",
            "Beschreibung: ${session.description}",
            "Operator: ${session.operatorName}",
            "Standort: ${session.location}",
            "Projekt: ${session.projectName}",
            "Probe-ID: ${session.sampleId}",
            "Gerät: ${session.deviceName} (${session.deviceId})",
            "Start: ${DATE_FORMAT.format(Date(session.startTimestamp))}",
            "Ende: ${session.endTimestamp?.let { DATE_FORMAT.format(Date(it)) } ?: "Laufend"}",
            "Status: ${session.status}"
        )
        
        info.forEach { line ->
            canvas.drawText(line, MARGIN.toFloat(), currentY.toFloat(), paint)
            currentY += LINE_HEIGHT
        }
        
        return currentY + 20
    }
    
    /**
     * Session-Statistiken zeichnen
     */
    private fun drawSessionStatistics(
        canvas: Canvas, 
        session: MeasurementSession, 
        readings: List<EMFReading>, 
        yPos: Int
    ): Int {
        var currentY = drawHeader(canvas, "Statistiken", yPos)
        
        val avgSignal = readings.map { it.signalStrength }.average()
        val maxSignal = readings.maxOfOrNull { it.signalStrength } ?: 0.0
        val minSignal = readings.minOfOrNull { it.signalStrength } ?: 0.0
        val avgTemp = readings.map { it.temperature }.average()
        val avgQuality = readings.map { it.qualityScore }.average()
        
        val stats = listOf(
            "Anzahl Messungen: ${readings.size}",
            "Durchschnittliche Signalstärke: ${"%.2f".format(avgSignal)}",
            "Maximale Signalstärke: ${"%.2f".format(maxSignal)}",
            "Minimale Signalstärke: ${"%.2f".format(minSignal)}",
            "Durchschnittstemperatur: ${"%.1f".format(avgTemp)}°C",
            "Durchschnittliche Qualität: ${"%.2f".format(avgQuality)}"
        )
        
        stats.forEach { line ->
            canvas.drawText(line, MARGIN.toFloat(), currentY.toFloat(), paint)
            currentY += LINE_HEIGHT
        }
        
        return currentY + 20
    }
    
    /**
     * Messdaten-Zusammenfassung zeichnen
     */
    private fun drawMeasurementSummary(canvas: Canvas, readings: List<EMFReading>, yPos: Int): Int {
        var currentY = yPos
        
        val frequencyRange = readings.map { it.frequency }
        val minFreq = frequencyRange.minOrNull() ?: 0.0
        val maxFreq = frequencyRange.maxOrNull() ?: 0.0
        
        val depthRange = readings.map { it.depth }
        val minDepth = depthRange.minOrNull() ?: 0.0
        val maxDepth = depthRange.maxOrNull() ?: 0.0
        
        val summary = listOf(
            "Frequenzbereich: ${"%.1f".format(minFreq)} - ${"%.1f".format(maxFreq)} Hz",
            "Tiefenbereich: ${"%.2f".format(minDepth)} - ${"%.2f".format(maxDepth)} m",
            "Messdauer: ${calculateMeasurementDuration(readings)} Minuten",
            "Validierte Messungen: ${readings.count { it.isValidated }}"
        )
        
        summary.forEach { line ->
            canvas.drawText(line, MARGIN.toFloat(), currentY.toFloat(), paint)
            currentY += LINE_HEIGHT
        }
        
        return currentY + 20
    }
    
    /**
     * Signalstärke-Diagramm zeichnen
     */
    private fun drawSignalStrengthChart(canvas: Canvas, readings: List<EMFReading>, yPos: Int): Int {
        if (readings.isEmpty()) return yPos
        
        var currentY = drawHeader(canvas, "Signalstärke-Verlauf", yPos)
        
        val chartWidth = PAGE_WIDTH - 2 * MARGIN
        val chartHeight = 150
        val chartLeft = MARGIN
        val chartTop = currentY
        val chartRight = chartLeft + chartWidth
        val chartBottom = chartTop + chartHeight
        
        // Diagramm-Rahmen
        val framePaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        
        canvas.drawRect(
            chartLeft.toFloat(),
            chartTop.toFloat(),
            chartRight.toFloat(),
            chartBottom.toFloat(),
            framePaint
        )
        
        // Daten normalisieren
        val maxSignal = readings.maxOfOrNull { it.signalStrength } ?: 1.0
        val minSignal = readings.minOfOrNull { it.signalStrength } ?: 0.0
        val signalRange = maxSignal - minSignal
        
        // Datenpunkte zeichnen
        val linePaint = Paint().apply {
            color = Color.BLUE
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        
        val path = Path()
        readings.forEachIndexed { index, reading ->
            val x = chartLeft + (index.toFloat() / (readings.size - 1)) * chartWidth
            val normalizedSignal = if (signalRange > 0) {
                (reading.signalStrength - minSignal) / signalRange
            } else 0.5
            val y = chartBottom - normalizedSignal * chartHeight
            
            if (index == 0) {
                path.moveTo(x, y.toFloat())
            } else {
                path.lineTo(x, y.toFloat())
            }
        }
        
        canvas.drawPath(path, linePaint)
        
        // Achsenbeschriftungen
        canvas.drawText("Zeit", (chartLeft + chartWidth / 2).toFloat(), (chartBottom + 20).toFloat(), paint)
        
        // Y-Achse beschriftung (rotiert)
        canvas.save()
        canvas.rotate(-90f, (chartLeft - 30).toFloat(), (chartTop + chartHeight / 2).toFloat())
        canvas.drawText("Signalstärke", (chartLeft - 30).toFloat(), (chartTop + chartHeight / 2).toFloat(), paint)
        canvas.restore()
        
        return chartBottom + 40
    }
    
    /**
     * Material-Analysen zeichnen
     */
    private fun drawMaterialAnalyses(canvas: Canvas, analyses: List<MaterialAnalysis>, yPos: Int): Int {
        var currentY = yPos
        
        val materialCounts = analyses.groupBy { it.materialType }.mapValues { it.value.size }
        val avgConfidence = analyses.map { it.confidence }.average()
        
        val analysisInfo = listOf(
            "Anzahl Analysen: ${analyses.size}",
            "Durchschnittliche Konfidenz: ${"%.2f".format(avgConfidence)}",
            "Erkannte Materialien: ${materialCounts.size}"
        )
        
        analysisInfo.forEach { line ->
            canvas.drawText(line, MARGIN.toFloat(), currentY.toFloat(), paint)
            currentY += LINE_HEIGHT
        }
        
        currentY += 10
        
        // Material-Verteilung
        materialCounts.forEach { (material, count) ->
            val percentage = (count.toDouble() / analyses.size * 100)
            canvas.drawText(
                "- $material: $count (${"%.1f".format(percentage)}%)",
                (MARGIN + 20).toFloat(),
                currentY.toFloat(),
                paint
            )
            currentY += LINE_HEIGHT
        }
        
        return currentY + 20
    }
    
    /**
     * Material-Verteilungsdiagramm zeichnen
     */
    private fun drawMaterialDistribution(canvas: Canvas, analyses: List<MaterialAnalysis>, yPos: Int): Int {
        if (analyses.isEmpty()) return yPos
        
        var currentY = drawHeader(canvas, "Material-Verteilung", yPos)
        
        val materialCounts = analyses.groupBy { it.materialType }.mapValues { it.value.size }
        val total = analyses.size.toFloat()
        
        val chartCenterX = PAGE_WIDTH / 2
        val chartCenterY = currentY + 100
        val chartRadius = 80
        
        var startAngle = 0f
        val colors = listOf(
            Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
            Color.MAGENTA, Color.CYAN, Color.GRAY
        )
        
        materialCounts.entries.forEachIndexed { index, (material, count) ->
            val sweepAngle = (count / total) * 360f
            val color = colors[index % colors.size]
            
            val arcPaint = Paint().apply {
                this.color = color
                style = Paint.Style.FILL
            }
            
            val rect = RectF(
                (chartCenterX - chartRadius).toFloat(),
                (chartCenterY - chartRadius).toFloat(),
                (chartCenterX + chartRadius).toFloat(),
                (chartCenterY + chartRadius).toFloat()
            )
            
            canvas.drawArc(rect, startAngle, sweepAngle, true, arcPaint)
            
            // Legende
            val legendY = currentY + 200 + index * 20
            canvas.drawRect(
                (MARGIN + 20).toFloat(),
                (legendY - 10).toFloat(),
                (MARGIN + 35).toFloat(),
                (legendY + 5).toFloat(),
                arcPaint
            )
            canvas.drawText(
                "$material ($count)",
                (MARGIN + 45).toFloat(),
                legendY.toFloat(),
                paint
            )
            
            startAngle += sweepAngle
        }
        
        return currentY + 250 + materialCounts.size * 20
    }
    
    /**
     * Detaillierte Messungen zeichnen
     */
    private fun drawDetailedMeasurements(canvas: Canvas, readings: List<EMFReading>, yPos: Int): Int {
        var currentY = yPos
        
        // Tabellen-Header
        val headers = listOf("Zeit", "Frequenz", "Signal", "Material", "Qualität")
        val columnWidths = listOf(120, 80, 80, 100, 80)
        var xPos = MARGIN
        
        headers.forEachIndexed { index, header ->
            canvas.drawText(header, xPos.toFloat(), currentY.toFloat(), headerPaint)
            xPos += columnWidths[index]
        }
        
        currentY += 25
        
        // Tabellen-Daten
        readings.take(30).forEach { reading ->
            xPos = MARGIN
            val rowData = listOf(
                DATE_FORMAT.format(Date(reading.timestamp)).substring(11), // Nur Zeit
                "${"%.1f".format(reading.frequency)}",
                "${"%.1f".format(reading.signalStrength)}",
                reading.materialType.toString().take(8),
                "${"%.2f".format(reading.qualityScore)}"
            )
            
            rowData.forEachIndexed { index, data ->
                canvas.drawText(data, xPos.toFloat(), currentY.toFloat(), paint)
                xPos += columnWidths[index]
            }
            
            currentY += LINE_HEIGHT
            
            if (currentY > PAGE_HEIGHT - MARGIN - 50) break
        }
        
        return currentY + 20
    }
    
    /**
     * Messdauer berechnen
     */
    private fun calculateMeasurementDuration(readings: List<EMFReading>): Long {
        if (readings.size < 2) return 0
        
        val startTime = readings.minOfOrNull { it.timestamp } ?: 0
        val endTime = readings.maxOfOrNull { it.timestamp } ?: 0
        
        return (endTime - startTime) / (1000 * 60) // Minuten
    }
}
