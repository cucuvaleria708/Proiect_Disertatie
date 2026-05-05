package com.alex.monitorsanatate.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.alex.monitorsanatate.ui.theme.PulseRedDark
import com.alex.monitorsanatate.ui.theme.PulseRedMain
import com.alex.monitorsanatate.ui.theme.PulseRedVibrant

@Composable
fun IPhoneHeart(modifier: Modifier = Modifier, scale: Float = 1f) {
    Box(
        modifier = modifier
            .size(60.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Inima redesenata pentru eleganta maxima (rosu vibrant)
            val path = Path().apply {
                moveTo(width * 0.5f, height * 0.85f)  // varf jos

                // Stanga jos -> stanga sus
                cubicTo(
                    width * 0.1f,  height * 0.65f,
                    width * 0.0f,  height * 0.38f,
                    width * 0.22f, height * 0.22f
                )
                // Bump stanga sus
                cubicTo(
                    width * 0.35f, height * 0.06f,
                    width * 0.47f, height * 0.1f,
                    width * 0.5f,  height * 0.28f
                )
                // Bump dreapta sus
                cubicTo(
                    width * 0.53f, height * 0.1f,
                    width * 0.65f, height * 0.06f,
                    width * 0.78f, height * 0.22f
                )
                // Dreapta sus -> dreapta jos
                cubicTo(
                    width * 1.0f,  height * 0.38f,
                    width * 0.9f,  height * 0.65f,
                    width * 0.5f,  height * 0.85f
                )
                close()
            }

            // 1. Glow rosu subtil sub inima
            drawPath(
                path = path,
                brush = Brush.radialGradient(
                    colors = listOf(PulseRedMain.copy(alpha = 0.3f), Color.Transparent),
                    center = center,
                    radius = width * 0.9f
                )
            )

            // 2. Corp inima cu gradient rosu intens
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(PulseRedVibrant, PulseRedMain, PulseRedDark),
                    start = Offset(width * 0.3f, 0f),
                    end = Offset(width * 0.7f, height)
                )
            )

            // 3. Reflexie lucioasa (gloss) tip sticla
            val glossPath = Path().apply {
                moveTo(width * 0.32f, height * 0.3f)
                cubicTo(width * 0.36f, height * 0.18f, width * 0.48f, height * 0.18f, width * 0.5f, height * 0.28f)
                cubicTo(width * 0.52f, height * 0.18f, width * 0.64f, height * 0.18f, width * 0.68f, height * 0.3f)
                cubicTo(width * 0.65f, height * 0.42f, width * 0.35f, height * 0.42f, width * 0.32f, height * 0.3f)
            }
            drawPath(
                path = glossPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.05f))
                )
            )
            
            // 4. Contur fin pentru definitie
            drawPath(
                path = path,
                color = PulseRedDark.copy(alpha = 0.2f),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}
