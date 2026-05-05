package com.alex.monitorsanatate.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.monitorsanatate.ui.theme.Ral5018Container
import com.alex.monitorsanatate.ui.theme.Ral5018Light
import com.alex.monitorsanatate.ui.theme.Ral5018Main

@Composable
fun BpmGauge(
    bpm: Int,
    isConnected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")

    val heartScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = if (isConnected && bpm > 0) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation   = tween(
                durationMillis = if (bpm > 0) 60_000 / bpm.coerceIn(30, 200) else 800,
                easing         = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartbeat_scale"
    )

    // Arc sweep animation: vai fill based on BPM (30–200 range)
    val arcSweep by animateFloatAsState(
        targetValue = if (isConnected && bpm > 0)
            ((bpm.coerceIn(30, 200) - 30f) / 170f) * 240f else 0f,
        animationSpec = tween(600),
        label = "arc_sweep"
    )

    val activeColor  = if (isConnected) Ral5018Light else Color(0xFF2C2C2C)
    val trackColor   = Color(0xFF1E1E1E)

    Column(
        modifier           = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Arc progress ring
            Canvas(modifier = Modifier.size(size)) {
                val strokeWidth = 10.dp.toPx()
                val inset       = strokeWidth / 2f
                val arcSize     = Size(this.size.width - inset * 2, this.size.height - inset * 2)
                val startAngle  = 150f
                val sweepTotal  = 240f

                // Track (background arc)
                drawArc(
                    color       = trackColor,
                    startAngle  = startAngle,
                    sweepAngle  = sweepTotal,
                    useCenter   = false,
                    topLeft     = Offset(inset, inset),
                    size        = arcSize,
                    style       = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Active arc with gradient
                if (arcSweep > 0f) {
                    drawArc(
                        brush      = Brush.sweepGradient(
                            colors = listOf(Ral5018Container, Ral5018Main, Ral5018Light),
                            center = Offset(this.size.width / 2f, this.size.height / 2f)
                        ),
                        startAngle = startAngle,
                        sweepAngle = arcSweep,
                        useCenter  = false,
                        topLeft    = Offset(inset, inset),
                        size       = arcSize,
                        style      = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // Heart + BPM number centrat în arc
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector     = Icons.Filled.Favorite,
                    contentDescription = "Heart",
                    tint            = if (isConnected) Ral5018Main else Color(0xFF454545),
                    modifier        = Modifier
                        .size(32.dp)
                        .scale(if (isConnected) heartScale else 1f)
                )
                Text(
                    text       = if (isConnected && bpm > 0) "$bpm" else "--",
                    fontSize   = 46.sp,
                    fontWeight = FontWeight.Black,
                    color      = if (isConnected) Ral5018Main else Color(0xFF454545),
                    lineHeight = 48.sp
                )
                Text(
                    text  = "BPM",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isConnected) Ral5018Light.copy(alpha = 0.8f)
                            else Color(0xFF454545)
                )
            }
        }
    }
}
