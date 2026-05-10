package com.alex.monitorsanatate.ui.ecgdetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alex.monitorsanatate.domain.model.ConnectionState
import com.alex.monitorsanatate.ui.theme.AppBackground
import com.alex.monitorsanatate.ui.theme.EcgGrid
import com.alex.monitorsanatate.ui.theme.EcgGridMajor
import com.alex.monitorsanatate.ui.theme.EcgLine
import com.alex.monitorsanatate.ui.theme.Ral5018Main
import com.alex.monitorsanatate.ui.theme.TextSecondary

// Valoare centrală ADC: senzorul PulseSensor trimite valori 10-bit (0-1023)
// cu centrul la 512 (tensiune medie ~1.65V la ADC 3.3V).
private const val ADC_CENTER = 512f
private const val ADC_HALF_RANGE = 512f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcgDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: EcgDetailViewModel = hiltViewModel()
) {
    val ecgPoints by viewModel.ecgPoints.collectAsStateWithLifecycle()
    val currentBpm by viewModel.currentBpm.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val isConnected = connectionState is ConnectionState.Connected

    var zoomLevel by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }

    Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        TopAppBar(
            title = { Text("Verificare ECG") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Înapoi")
                }
            },
            actions = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = if (isConnected) Ral5018Main else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isConnected && currentBpm > 0) "$currentBpm BPM" else "-- BPM",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isConnected) Ral5018Main else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            zoomLevel = (zoomLevel * zoom).coerceIn(0.5f, 5f)
                            offsetX += pan.x
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val centerY = height / 2f

                // Grila mica
                val smallGridSize = width / (50f * zoomLevel)
                var x = offsetX % smallGridSize
                while (x <= width) {
                    drawLine(EcgGrid, Offset(x, 0f), Offset(x, height), strokeWidth = 0.5f)
                    x += smallGridSize
                }
                var y = 0f
                while (y <= height) {
                    drawLine(EcgGrid, Offset(0f, y), Offset(width, y), strokeWidth = 0.5f)
                    y += smallGridSize
                }

                // Grila mare (5 celule mici)
                val largeGridSize = smallGridSize * 5f
                x = offsetX % largeGridSize
                while (x <= width) {
                    drawLine(EcgGridMajor, Offset(x, 0f), Offset(x, height), strokeWidth = 1f)
                    x += largeGridSize
                }
                y = 0f
                while (y <= height) {
                    drawLine(EcgGridMajor, Offset(0f, y), Offset(width, y), strokeWidth = 1f)
                    y += largeGridSize
                }

                // Linie centrala
                drawLine(EcgGridMajor, Offset(0f, centerY), Offset(width, centerY), strokeWidth = 1.5f)

                if (ecgPoints.size >= 2) {
                    val visiblePoints = (500 / zoomLevel).toInt().coerceAtLeast(50)
                    val points = if (ecgPoints.size > visiblePoints) {
                        ecgPoints.takeLast(visiblePoints)
                    } else {
                        ecgPoints
                    }

                    val path = Path()
                    // Amplitudinea: 35% din inaltimea ecranului pentru o deviere de 512 unitati ADC
                    val amplitudeScale = height * 0.35f
                    val stepX = width / (visiblePoints - 1).toFloat()

                    points.forEachIndexed { index, value ->
                        val px = index * stepX
                        // Normalizare ADC 0-1023 → [-1, 1] cu centrul la 512
                        val normalizedValue = (value - ADC_CENTER) / ADC_HALF_RANGE
                        val py = centerY - (normalizedValue * amplitudeScale)
                        if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }

                    drawPath(
                        path = path,
                        color = EcgLine,
                        style = Stroke(width = 2.5f)
                    )
                }
            }

            // Overlay "fara date" — afisat cand nu exista suficiente esantioane
            if (ecgPoints.size < 2) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isConnected)
                            "Plasează degetul pe senzor\npentru a vizualiza ECG-ul"
                        else
                            "Senzorul nu este conectat\nMergi la Setări → Conexiune senzor puls",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
