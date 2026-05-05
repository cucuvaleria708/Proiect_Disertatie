package com.alex.monitorsanatate.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.alex.monitorsanatate.ui.theme.EcgGrid
import com.alex.monitorsanatate.ui.theme.EcgGridMajor
import com.alex.monitorsanatate.ui.theme.EcgLine

@Composable
fun EcgWaveform(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = EcgLine,
    strokeWidth: Float = 2.5f,
    visiblePoints: Int = 500
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(Color(0xFF121212))
    ) {
        drawEcgGrid()
        drawEcgTrace(dataPoints, lineColor, strokeWidth, visiblePoints)
    }
}

private fun DrawScope.drawEcgGrid() {
    val small = size.width / 50f
    val large = small * 5f

    var x = 0f
    while (x <= size.width) {
        drawLine(EcgGrid.copy(alpha = 0.25f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.8f)
        x += small
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(EcgGrid.copy(alpha = 0.25f), Offset(0f, y), Offset(size.width, y), strokeWidth = 0.8f)
        y += small
    }
    x = 0f
    while (x <= size.width) {
        drawLine(EcgGridMajor.copy(alpha = 0.5f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.2f)
        x += large
    }
    y = 0f
    while (y <= size.height) {
        drawLine(EcgGridMajor.copy(alpha = 0.5f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.2f)
        y += large
    }
    val centerY = size.height / 2f
    drawLine(EcgGridMajor.copy(alpha = 0.8f), Offset(0f, centerY), Offset(size.width, centerY), strokeWidth = 1.5f)
}

private fun DrawScope.drawEcgTrace(
    dataPoints: List<Float>,
    lineColor: Color,
    strokeWidth: Float,
    visiblePoints: Int
) {
    if (dataPoints.size < 2) return
    val points = if (dataPoints.size > visiblePoints) dataPoints.takeLast(visiblePoints) else dataPoints
    val path = Path()
    val centerY = size.height / 2f
    val amplitude = size.height * 0.38f
    val stepX = size.width / (visiblePoints - 1).toFloat()

    points.forEachIndexed { index, value ->
        val x = index * stepX
        val y = centerY - (value * amplitude)
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }

    // Glow layer
    drawPath(path, lineColor.copy(alpha = 0.18f),
        style = Stroke(width = strokeWidth * 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    // Soft outer
    drawPath(path, lineColor.copy(alpha = 0.35f),
        style = Stroke(width = strokeWidth * 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    // Sharp line
    drawPath(path, lineColor,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
