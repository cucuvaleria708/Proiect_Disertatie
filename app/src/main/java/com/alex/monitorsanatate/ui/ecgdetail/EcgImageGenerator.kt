package com.alex.monitorsanatate.ui.ecgdetail

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Generează o imagine ECG de 256×256px care mimează formatul rapoartelor ECG clinice
 * din dataset-ul de antrenare (hârtie milimetrică, header alb, 4 rânduri de trace, footer).
 *
 * LAYOUT TARGET (imaginile de antrenare, după resize la 256×256):
 *   • Header alb ~20%  : "ECG REPORT" + info pacient
 *   • Border roșu      : dreptunghi 2px pe tot perimetrul
 *   • Grilă roz        : celule mici (1mm) + celule mari (5mm) — ~12 col / ~8 lin mari
 *   • 4 rânduri trace  : lead label + puls calibrare + semnal + 3 separatoare verticale
 *   • Footer alb ~5%   : text parametri calibrare
 *
 * MODEL INFERENCE:
 *   Bitmap 256×256 → createScaledBitmap(256,256) → TensorImageUtils(mean=[0.485,0.456,0.406],
 *   std=[0.229,0.224,0.225]) → ecg_model.ptl → exp(logits) softmax → 4 clase
 */
object EcgImageGenerator {

    private const val IMG_SIZE = 256

    // Layout proportions calibrated to training images after 256×256 resize
    private const val HEADER_H = 50    // ~19.5% — "ECG REPORT" white area
    private const val FOOTER_H = 14    // ~5.5%  — calibration text
    private const val SIGNAL_H = IMG_SIZE - HEADER_H - FOOTER_H  // 192 px
    private const val NUM_ROWS = 4
    private const val ROW_H    = SIGNAL_H / NUM_ROWS              // 48 px exactly

    // Grid: 12 large divisions horizontally, 8 vertically — matches training images
    private const val LARGE_DIV_X = 12
    private const val LARGE_DIV_Y = 8

    // Colors — clinical ECG paper palette
    private val GRID_MINOR = Color.argb(55,  230,  85,  85)
    private val GRID_MAJOR = Color.argb(125, 200,  35,  35)
    private val BORDER_CLR = Color.argb(215, 185,  30,  30)
    private val TRACE_CLR  = Color.argb(235,   0,   0,   0)
    private val TEXT_CLR   = Color.argb(175,  55,  55,  55)
    private val WHITE      = Color.WHITE

    fun generate(rawSamples: List<Float>): Bitmap {
        val bitmap = Bitmap.createBitmap(IMG_SIZE, IMG_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(WHITE)

        // Order matters: grid first, then annotations on top
        drawGrid(canvas)
        drawHeader(canvas)
        drawFooter(canvas)
        drawBorder(canvas)
        drawRowSeparators(canvas)
        drawLeadAnnotations(canvas)
        drawSignal(canvas, rawSamples)

        return bitmap
    }

    // ── Grilă ECG milimetrică ────────────────────────────────────────────────

    private fun drawGrid(canvas: Canvas) {
        val left   = 2f
        val right  = (IMG_SIZE - 2).toFloat()
        val top    = HEADER_H.toFloat()
        val bottom = (IMG_SIZE - FOOTER_H).toFloat()

        val w  = right - left
        val h  = bottom - top
        val dx1 = w / (LARGE_DIV_X * 5)
        val dy1 = h / (LARGE_DIV_Y * 5)

        val minorP = Paint().apply { color = GRID_MINOR; strokeWidth = 0.5f }
        val majorP = Paint().apply { color = GRID_MAJOR; strokeWidth = 0.9f }

        var x = left
        while (x <= right + 0.3f) {
            canvas.drawLine(x, top, x, bottom, minorP); x += dx1
        }
        var y = top
        while (y <= bottom + 0.3f) {
            canvas.drawLine(left, y, right, y, minorP); y += dy1
        }
        x = left
        while (x <= right + 0.3f) {
            canvas.drawLine(x, top, x, bottom, majorP); x += dx1 * 5
        }
        y = top
        while (y <= bottom + 0.3f) {
            canvas.drawLine(left, y, right, y, majorP); y += dy1 * 5
        }
    }

    // ── Border roșu ──────────────────────────────────────────────────────────

    private fun drawBorder(canvas: Canvas) {
        canvas.drawRect(1.2f, 1.2f, (IMG_SIZE - 1.2f), (IMG_SIZE - 1.2f), Paint().apply {
            color = BORDER_CLR; strokeWidth = 2.4f; style = Paint.Style.STROKE
        })
    }

    // ── Header: "ECG REPORT" + info pacient ──────────────────────────────────

    private fun drawHeader(canvas: Canvas) {
        // Fundal alb peste grilă
        canvas.drawRect(0f, 0f, IMG_SIZE.toFloat(), HEADER_H.toFloat(), Paint().apply {
            color = WHITE
        })

        val titleP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color     = TEXT_CLR
            textSize  = 9.5f
            typeface  = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ECG REPORT", (IMG_SIZE / 2f), 12f, titleP)

        val infoP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEXT_CLR; textSize = 5.5f
        }
        canvas.drawText("ID : 000000   Years   Male   cm   kg   /   mmHg   Race:Unknown   Room No.:   Department:", 4f, 20f, infoP)
        canvas.drawText("Exam Room:   Medication:", 4f, 27f, infoP)
        canvas.drawText("Diagnosis Information:", 4f, 34f, infoP)

        val rightP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEXT_CLR; textSize = 5.5f; textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Technician :", (IMG_SIZE - 4).toFloat(), 20f, rightP)
        canvas.drawText("Ref. Phys. :", (IMG_SIZE - 4).toFloat(), 27f, rightP)
        canvas.drawText("Report Confirmed by:", (IMG_SIZE - 4).toFloat(), 34f, rightP)

        // Linie despărțitoare header / zonă semnal
        canvas.drawLine(2f, HEADER_H.toFloat(), (IMG_SIZE - 2f), HEADER_H.toFloat(), Paint().apply {
            color = BORDER_CLR; strokeWidth = 0.8f
        })
    }

    // ── Footer: parametri calibrare ──────────────────────────────────────────

    private fun drawFooter(canvas: Canvas) {
        canvas.drawRect(0f, (IMG_SIZE - FOOTER_H).toFloat(), IMG_SIZE.toFloat(), IMG_SIZE.toFloat(), Paint().apply {
            color = WHITE
        })
        canvas.drawText(
            "0.67-25Hz  AC50  25mm/s  10mm/mV  +12.5+inf  SD=3 V1.0  SIMP V1.7        2024-06-05  10:30:24 AM",
            4f, (IMG_SIZE - FOOTER_H + 9).toFloat(),
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TEXT_CLR; textSize = 4.8f }
        )
    }

    // ── Linii despărțitoare între rânduri ────────────────────────────────────

    private fun drawRowSeparators(canvas: Canvas) {
        val p = Paint().apply { color = GRID_MAJOR; strokeWidth = 0.9f }
        for (row in 1 until NUM_ROWS) {
            val y = (HEADER_H + row * ROW_H).toFloat()
            canvas.drawLine(2f, y, (IMG_SIZE - 2f), y, p)
        }
    }

    // ── Lead labels, separatoare secțiuni, puls calibrare ────────────────────

    private fun drawLeadAnnotations(canvas: Canvas) {
        val rowLeads    = listOf("I", "II", "III", "aVF")
        val segLabels   = listOf(
            listOf("aVR", "V1", "V4"),
            listOf("aVL", "V2", "V5"),
            listOf("aVF", "V3", "V6"),
            listOf<String>()     // rândul 4 = rhythm strip lung, fără separatoare
        )

        val labelP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TEXT_CLR; textSize = 5.8f }
        val divP   = Paint().apply { color = TEXT_CLR; strokeWidth = 0.6f }
        val calP   = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TRACE_CLR; strokeWidth = 1.0f; style = Paint.Style.STROKE
            strokeCap = Paint.Cap.SQUARE
        }

        val sLeft   = 2f
        val sRight  = (IMG_SIZE - 2).toFloat()
        val sWidth  = sRight - sLeft

        // Puls calibrare: dreptunghi 5px lat × ~10px înalt la centrul fiecărui rând
        val xCal  = sLeft + 12f
        val calW  = 5f
        val calH  = (ROW_H * 0.21f)   // ~10px = 1 mV la 10 mm/mV standard

        for (row in 0 until NUM_ROWS) {
            val rowTop = (HEADER_H + row * ROW_H).toFloat()
            val cy     = rowTop + ROW_H / 2f

            // Label lead (I, II, III, aVF)
            canvas.drawText(rowLeads[row], sLeft + 2f, rowTop + 9f, labelP)

            // Puls calibrare
            val calPath = Path().apply {
                moveTo(xCal,        cy)
                lineTo(xCal,        cy - calH)
                lineTo(xCal + calW, cy - calH)
                lineTo(xCal + calW, cy)
            }
            canvas.drawPath(calPath, calP)

            // Separatoare secțiuni și labels (aVR, V1, V4 etc.)
            val segs = segLabels[row]
            for (s in segs.indices) {
                val xDiv = sLeft + (s + 1) * sWidth / 4f
                canvas.drawLine(xDiv, rowTop, xDiv, rowTop + ROW_H, divP)
                canvas.drawText(segs[s], xDiv + 2f, rowTop + 9f, labelP)
            }
        }
    }

    // ── Traseu ECG pe 4 rânduri ───────────────────────────────────────────────

    private fun drawSignal(canvas: Canvas, samples: List<Float>) {
        if (samples.size < NUM_ROWS * 2) return

        // Ultimele ~10 s de semnal
        val pts = if (samples.size > 2500) samples.takeLast(2500) else samples

        // Amplitudine globală — consistentă pe toate rândurile
        val mean = pts.average().toFloat()
        val std  = sqrt(pts.map { (it - mean).pow(2) }.average()).toFloat().coerceAtLeast(5f)
        // 2σ → 35% din jumătatea rândului, lăsând marjă pentru vârfuri R de 3-4σ
        val gain = (ROW_H * 0.35f) / (2.0f * std)

        val tracePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color       = TRACE_CLR
            strokeWidth = 1.1f
            style       = Paint.Style.STROKE
            strokeCap   = Paint.Cap.ROUND
            strokeJoin  = Paint.Join.ROUND
        }

        // Semnalul începe după zona de calibrare (~20px de la marginea stângă)
        val xStart    = 2f + 20f
        val xEnd      = (IMG_SIZE - 2).toFloat()
        val drawWidth = xEnd - xStart
        val segSize   = pts.size / NUM_ROWS

        for (row in 0 until NUM_ROWS) {
            val i0 = row * segSize
            val i1 = if (row == NUM_ROWS - 1) pts.size else (row + 1) * segSize
            if (i1 <= i0 + 1) continue

            val seg     = pts.subList(i0, i1)
            val rowTop  = (HEADER_H + row * ROW_H).toFloat()
            val rowBot  = rowTop + ROW_H
            val centerY = rowTop + ROW_H / 2f
            val stepX   = drawWidth / (seg.size - 1).coerceAtLeast(1)

            val path = Path()
            seg.forEachIndexed { idx, v ->
                val px = xStart + idx * stepX
                val py = (centerY - (v - mean) * gain).coerceIn(rowTop + 1f, rowBot - 1f)
                if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            canvas.drawPath(path, tracePaint)
        }
    }
}
