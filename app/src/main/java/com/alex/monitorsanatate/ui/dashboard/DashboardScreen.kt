package com.alex.monitorsanatate.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alex.monitorsanatate.domain.model.ConnectionState
import com.alex.monitorsanatate.ui.components.AppScreenHeader
import com.alex.monitorsanatate.ui.components.BpmGauge
import com.alex.monitorsanatate.ui.components.EcgWaveform
import com.alex.monitorsanatate.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToConnection: () -> Unit,
    onNavigateToEcgDetail: () -> Unit,
    onNavigateToEcgAnalysis: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val connectionState    by viewModel.connectionState.collectAsStateWithLifecycle()
    val currentBpm         by viewModel.currentBpm.collectAsStateWithLifecycle()
    val ecgPoints          by viewModel.ecgPoints.collectAsStateWithLifecycle()
    val leadOffOk          by viewModel.leadOffOk.collectAsStateWithLifecycle()
    val measurementState   by viewModel.measurementState.collectAsStateWithLifecycle()
    val timeRemaining      by viewModel.timeRemaining.collectAsStateWithLifecycle()
    val finalBpm           by viewModel.finalBpm.collectAsStateWithLifecycle()
    val ekgMetrics         by viewModel.ekgMetrics.collectAsStateWithLifecycle()
    val isConnected = connectionState is ConnectionState.Connected

    // Navigheaza la Analiza AI cand ViewModel emite evenimentul
    LaunchedEffect(Unit) {
        viewModel.navigateToEcgAnalysis.collect {
            onNavigateToEcgAnalysis()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            AppScreenHeader(
                title    = "Verificare EKG",
                subtitle = "Monitorizare în timp real"
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (connectionState) {

                    // ── Neconectat / Eroare ───────────────────────────────────
                    is ConnectionState.Disconnected,
                    is ConnectionState.Error -> {
                        NotConnectedCard(
                            message = if (connectionState is ConnectionState.Error)
                                (connectionState as ConnectionState.Error).message
                            else null,
                            hint = "Conectați senzorul EKG urmând pașii: accesați Setări → Conexiune senzor EKG, asigurați-vă că placa de dezvoltare ESP32 este alimentată și inițiați scanarea Bluetooth."
                        )
                    }

                    // ── Scanare / Conectare ───────────────────────────────────
                    is ConnectionState.Scanning,
                    is ConnectionState.Connecting -> {
                        Box(
                            modifier         = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Ral5018Main)
                                Spacer(Modifier.height(12.dp))
                                Text("Se caută senzorul EKG...",
                                    fontSize = 14.sp, color = TextSecondary)
                            }
                        }
                    }

                    // ── Conectat ─────────────────────────────────────────────
                    is ConnectionState.Connected -> {
                        val state = connectionState as ConnectionState.Connected

                        // Bara de stare: dispozitiv + badge electrozi
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Surface(shape = RoundedCornerShape(20.dp), color = AppSurface) {
                                Text(
                                    "● Conectat: ${state.device.name}",
                                    fontSize   = 12.sp,
                                    color      = Ral5018Main,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier   = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                            // Badge electrozi dezconectati
                            if (!leadOffOk) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = PulseRedMain.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier          = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector        = Icons.Filled.Warning,
                                            contentDescription = null,
                                            tint               = PulseRedMain,
                                            modifier           = Modifier.size(13.dp)
                                        )
                                        Text("Electrozi neconectați",
                                            fontSize = 11.sp, color = PulseRedMain,
                                            fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        // Card BPM — calculat din R-peaks ale semnalului EKG (AD8232)
                        Card(
                            modifier  = Modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(28.dp),
                            colors    = CardDefaults.cardColors(containerColor = AppSurfaceHigh),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Box(
                                modifier         = Modifier.fillMaxWidth().padding(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    BpmGauge(
                                        bpm         = ekgMetrics.bpm,
                                        isConnected = true,
                                        size        = 180.dp
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "din semnal EKG",
                                        fontSize  = 11.sp,
                                        color     = TextSecondary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment     = Alignment.CenterVertically
                                    ) {
                                        LiveDot()
                                        Text("LIVE", fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Ral5018Main, letterSpacing = 2.sp)
                                    }
                                }
                            }
                        }

                        // Card traseu EKG
                        Card(
                            modifier  = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onNavigateToEcgDetail),
                            shape     = RoundedCornerShape(24.dp),
                            colors    = CardDefaults.cardColors(containerColor = AppSurfaceHigh),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column {
                                Row(
                                    modifier              = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text("Semnal EKG",
                                        style      = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color      = TextPrimary)
                                    Row(
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        LiveDot()
                                        Text("TAP pentru detalii", fontSize = 11.sp,
                                            color = TextSecondary, letterSpacing = 0.5.sp)
                                    }
                                }
                                EcgWaveform(
                                    dataPoints = ecgPoints,
                                    modifier   = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                                )
                            }
                        }

                        // Buton masurare 15s
                        MeasurementButton(
                            state         = measurementState,
                            timeRemaining = timeRemaining,
                            finalBpm      = finalBpm,
                            onStart       = { viewModel.startMeasurement() }
                        )

                        // Card metrici EKG
                        if (ekgMetrics.rrIntervalMs > 0 || ekgMetrics.hrv > 0f) {
                            EkgMetricsCard(metrics = ekgMetrics)
                        }

                        // Chips status — folosesc BPM din EKG
                        if (ekgMetrics.bpm > 0) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatChip(
                                    label    = "STATUS",
                                    value    = bpmZone(ekgMetrics.bpm),
                                    color    = bpmZoneColor(ekgMetrics.bpm),
                                    modifier = Modifier.weight(1f)
                                )
                                StatChip(
                                    label    = "INTERVAL R-R",
                                    value    = "${ekgMetrics.rrIntervalMs}ms",
                                    color    = Ral5018Light,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Buton Analiza AI
                        Button(
                            onClick  = { viewModel.captureAndNavigateToEcgAnalysis() },
                            enabled  = ecgPoints.size >= 50,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape  = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Ral5018Main,
                                disabledContainerColor = AppSurface
                            )
                        ) {
                            Icon(
                                imageVector        = Icons.Filled.ImageSearch,
                                contentDescription = null,
                                modifier           = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Analizează cu AI",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 15.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ── Componente helper ─────────────────────────────────────────────────────────

@Composable
fun NotConnectedCard(message: String? = null, hint: String) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = AppSurfaceHigh),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier            = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector        = Icons.Filled.Bluetooth,
                contentDescription = null,
                tint               = TextDisabled,
                modifier           = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Senzor neconectat",
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary
            )
            if (message != null) {
                Spacer(Modifier.height(4.dp))
                Text(message, fontSize = 12.sp, color = PulseRedMain, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                hint,
                fontSize   = 13.sp,
                color      = TextSecondary,
                textAlign  = TextAlign.Start,
                lineHeight = 18.sp,
                modifier   = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun LiveDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "live")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "dot_alpha"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(50))
            .background(Ral5018Main.copy(alpha = alpha))
    )
}

@Composable
private fun StatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 10.sp, color = TextSecondary,
                letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun MeasurementButton(
    state: MeasurementState,
    timeRemaining: Int,
    finalBpm: Int,
    onStart: () -> Unit
) {
    when (state) {
        MeasurementState.IDLE -> {
            Button(
                onClick  = onStart,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Ral5018Main)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Începe Măsurare (15s)", fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, letterSpacing = 0.5.sp)
            }
        }

        MeasurementState.MEASURING -> {
            val pulseAnim by rememberInfiniteTransition(label = "pulse").animateFloat(
                initialValue = 0.6f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                label = "pulse_alpha"
            )
            Button(
                onClick  = {},
                enabled  = false,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(
                    disabledContainerColor = Color(0xFFE06C00).copy(alpha = pulseAnim)
                )
            ) {
                CircularProgressIndicator(
                    color    = Color.White,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Măsurare... ${timeRemaining}s",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                    color      = Color.White
                )
            }
        }

        MeasurementState.RESULT -> {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = Color(0xFF1B4332))
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint     = Color(0xFF52B788),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Măsurare finalizată", fontSize = 12.sp,
                            color = Color(0xFF95D5B2), fontWeight = FontWeight.Medium)
                        Text(
                            if (finalBpm > 0) "BPM final: $finalBpm bpm" else "—",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color(0xFF52B788)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onStart) {
                        Text("Repetă", fontSize = 12.sp, color = Color(0xFF95D5B2))
                    }
                }
            }
        }
    }
}

@Composable
private fun EkgMetricsCard(metrics: EkgMetrics) {
    val interpretColor = when (metrics.interpretationColor) {
        InterpretationColor.GREEN   -> Color(0xFF2D6A4F)
        InterpretationColor.YELLOW  -> Color(0xFFCA6702)
        InterpretationColor.RED     -> Color(0xFF9B2226)
        InterpretationColor.NEUTRAL -> TextSecondary
    }
    val interpretBg = interpretColor.copy(alpha = 0.15f)

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = AppSurfaceHigh),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.FavoriteBorder, contentDescription = null,
                        tint = Ral5018Main, modifier = Modifier.size(18.dp))
                    Text("Analiză Ritm Cardiac", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = interpretBg
                ) {
                    Text(
                        metrics.interpretation,
                        fontSize   = 11.sp,
                        color      = interpretColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = AppSurface, thickness = 1.dp)
            Spacer(Modifier.height(14.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCell(
                    label    = "INTERVAL R-R",
                    value    = if (metrics.rrIntervalMs > 0) "${metrics.rrIntervalMs} ms" else "—",
                    modifier = Modifier.weight(1f)
                )
                MetricCell(
                    label    = "HRV (RMSSD)",
                    value    = if (metrics.hrv > 0f) "${"%.1f".format(metrics.hrv)} ms" else "—",
                    modifier = Modifier.weight(1f)
                )
                MetricCell(
                    label    = "pNN50",
                    value    = if (metrics.pnn50 > 0 || metrics.hrv > 0f) "${metrics.pnn50}%" else "—",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 9.sp, color = TextSecondary,
            letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold,
            color = TextPrimary, textAlign = TextAlign.Center)
    }
}

private fun bpmZone(bpm: Int) = when {
    bpm < 60   -> "Repaus"
    bpm <= 100 -> "Normal"
    bpm <= 140 -> "Activ"
    else       -> "Intens"
}

private fun bpmZoneColor(bpm: Int) = when {
    bpm < 60   -> TextSecondary
    bpm <= 100 -> Ral5018Light
    else       -> Ral5018Main
}
