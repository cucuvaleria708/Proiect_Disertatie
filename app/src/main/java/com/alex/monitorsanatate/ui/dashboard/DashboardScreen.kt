package com.alex.monitorsanatate.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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

    val snackbarHostState = remember { SnackbarHostState() }

    // Navigare la Analiza AI
    LaunchedEffect(Unit) {
        viewModel.navigateToEcgAnalysis.collect {
            onNavigateToEcgAnalysis()
        }
    }
    // Mesaje export CSV
    LaunchedEffect(Unit) {
        viewModel.exportMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            AppScreenHeader(
                title    = "Verificare ECG",
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
                            hint = "Conectați modulul ESP32 urmând pașii: accesați Setări → Conexiune modul ESP32, asigurați-vă că placa de dezvoltare ESP32 este alimentată și inițiați scanarea Bluetooth."
                        )
                        ElectrodeGuideCard()
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
                                Text("Se caută modulul ESP32...",
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

                        // Ghid plasare electrozi — mereu vizibil
                        ElectrodeGuideCard()

                        // Card BPM — calculat din R-peaks ale semnalului ECG (AD8232)
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
                                        "din semnal ECG",
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

                        // Card traseu ECG
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
                                        .padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text("Semnal ECG",
                                        style      = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color      = TextPrimary)
                                    Row(
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        // Buton export CSV
                                        IconButton(
                                            onClick  = { viewModel.exportEcgToCsv() },
                                            enabled  = ecgPoints.size >= 50,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Download,
                                                contentDescription = "Exportă ECG CSV",
                                                modifier = Modifier.size(18.dp),
                                                tint = if (ecgPoints.size >= 50) Ral5018Main
                                                       else MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        LiveDot()
                                        Text(
                                            "TAP pentru detalii", fontSize = 11.sp,
                                            color = TextSecondary, letterSpacing = 0.5.sp,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
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
                            leadOffOk     = leadOffOk,
                            onStart       = { viewModel.startMeasurement() }
                        )

                        // Card metrici ECG
                        if (ekgMetrics.rrIntervalMs > 0 || ekgMetrics.hrv > 0f) {
                            EkgMetricsCard(metrics = ekgMetrics)
                        }

                        // Chips status — folosesc BPM din ECG
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

        // Snackbar flotant (pentru confirmarea exportului CSV)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
        ) { data ->
            Snackbar(
                snackbarData   = data,
                shape          = RoundedCornerShape(12.dp),
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor   = MaterialTheme.colorScheme.inverseOnSurface
            )
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
    leadOffOk: Boolean,
    onStart: () -> Unit
) {
    when (state) {
        MeasurementState.IDLE -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick  = onStart,
                    enabled  = leadOffOk,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = Ral5018Main,
                        disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        disabledContentColor   = Color.White.copy(alpha = 0.6f)
                    )
                ) {
                    Icon(
                        if (leadOffOk) Icons.Filled.PlayArrow else Icons.Filled.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (leadOffOk) "Începe Măsurare (15s)" else "Pune electrozii pe piele",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    )
                }
                if (!leadOffOk) {
                    Text(
                        "Electrozii nu sunt detectați pe piele. Asigurați contactul și așteptați semnalul ECG.",
                        fontSize  = 12.sp,
                        color     = PulseRedMain,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth()
                    )
                }
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

// ── Ghid plasare electrozi ────────────────────────────────────────────────────

private data class ElectrodeInfo(
    val label: String,
    val color: Color,
    val limbDesc: String,
    val chestDesc: String
)

@Composable
private fun ElectrodeGuideCard() {
    val guideElectrodes = listOf(
        ElectrodeInfo("RA", Color(0xFFE53935), "Brațul drept",   "Sub claviculă dreaptă"),
        ElectrodeInfo("LA", Color(0xFFFFD600), "Brațul stâng",   "Sub claviculă stângă"),
        ElectrodeInfo("RL", Color(0xFF43A047), "Piciorul drept", "Abdomen inferior drept")
    )

    val infiniteTransition = rememberInfiniteTransition(label = "electrode_guide")

    val stepFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 3f,
        animationSpec = infiniteRepeatable(
            animation  = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "electrode_step"
    )
    val activeStep = stepFloat.toInt() % 3

    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 2.8f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue  = 0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                "PLASARE ELECTROZI",
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Black,
                color         = Color(0xFF00E676),
                letterSpacing = 1.5.sp
            )

            Spacer(Modifier.height(10.dp))

            // Column labels aligned over the two figures
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "Brațe / Picioare",
                    modifier      = Modifier.weight(1f),
                    textAlign     = TextAlign.Center,
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.SemiBold,
                    color         = Color(0xFF4A7A9B),
                    letterSpacing = 0.5.sp
                )
                Text(
                    "Piept",
                    modifier      = Modifier.weight(1f),
                    textAlign     = TextAlign.Center,
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.SemiBold,
                    color         = Color(0xFF4A7A9B),
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(Modifier.height(4.dp))

            // Two human figures side-by-side with electrode dots
            Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                val W  = size.width
                val H  = size.height
                val bg = Color(0xFF1A3550)
                val ol = Color(0xFF2E5F8A)
                val sw = 2.dp.toPx()
                val hw = W / 2f          // half canvas width
                val tw = hw * 0.17f      // torso half-width (shared scale)

                val cx1 = hw * 0.5f      // left figure centre
                val cx2 = hw * 1.5f      // right figure centre

                // Vertical separator
                drawLine(
                    color       = Color(0xFF1E4060),
                    start       = Offset(W * 0.5f, H * 0.02f),
                    end         = Offset(W * 0.5f, H * 0.98f),
                    strokeWidth = 1.dp.toPx()
                )

                // Both silhouettes share identical geometry; only electrode positions differ
                for (cx in listOf(cx1, cx2)) {
                    // Head
                    val headR = hw * 0.11f
                    drawCircle(bg, headR, Offset(cx, H * 0.08f))
                    drawCircle(ol, headR, Offset(cx, H * 0.08f), style = Stroke(sw))

                    // Torso
                    val tT = H * 0.18f; val tB = H * 0.57f
                    drawRoundRect(bg, Offset(cx - tw, tT), Size(tw * 2f, tB - tT), CornerRadius(6.dp.toPx()))
                    drawRoundRect(ol, Offset(cx - tw, tT), Size(tw * 2f, tB - tT), CornerRadius(6.dp.toPx()), style = Stroke(sw))

                    // Right arm (person's right = viewer's left)
                    val rArm = Path().apply {
                        moveTo(cx - tw, H * 0.22f)
                        cubicTo(cx - tw * 1.9f, H * 0.26f, cx - tw * 2.1f, H * 0.40f, cx - tw * 1.9f, H * 0.55f)
                    }
                    drawPath(rArm, ol, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))

                    // Left arm (person's left = viewer's right)
                    val lArm = Path().apply {
                        moveTo(cx + tw, H * 0.22f)
                        cubicTo(cx + tw * 1.9f, H * 0.26f, cx + tw * 2.1f, H * 0.40f, cx + tw * 1.9f, H * 0.55f)
                    }
                    drawPath(lArm, ol, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))

                    // Right leg (viewer's left)
                    drawLine(ol, Offset(cx - tw * 0.43f, tB), Offset(cx - tw * 0.55f, H * 0.88f), strokeWidth = sw)
                    // Left leg (viewer's right)
                    drawLine(ol, Offset(cx + tw * 0.43f, tB), Offset(cx + tw * 0.55f, H * 0.88f), strokeWidth = sw)
                }

                // Left figure: limb placement — electrodes on arms and leg
                //   RA (red)    → right arm (person's right = viewer's left arm)
                //   LA (yellow) → left arm  (person's left  = viewer's right arm)
                //   RL (green)  → right leg (person's right = viewer's left leg)
                val limbDots = listOf(
                    Offset(cx1 - tw * 1.90f, H * 0.39f),
                    Offset(cx1 + tw * 1.90f, H * 0.38f),
                    Offset(cx1 - tw * 0.52f, H * 0.73f)
                )

                // Right figure: chest placement — electrodes on torso
                //   RA (red)    → right upper chest (viewer's left)
                //   LA (yellow) → left upper chest  (viewer's right)
                //   RL (green)  → lower abdomen
                val chestDots = listOf(
                    Offset(cx2 - tw * 0.65f, H * 0.27f),
                    Offset(cx2 + tw * 0.65f, H * 0.26f),
                    Offset(cx2 - tw * 0.32f, H * 0.47f)
                )

                val dotColors = listOf(Color(0xFFE53935), Color(0xFFFFD600), Color(0xFF43A047))
                val dotR = 7.5.dp.toPx()

                listOf(limbDots, chestDots).forEach { dots ->
                    dots.forEachIndexed { i, pos ->
                        val c = dotColors[i]
                        val isActive = i == activeStep
                        if (isActive) drawCircle(c.copy(alpha = pulseAlpha), dotR * pulseRadius, pos)
                        drawCircle(c.copy(alpha = if (isActive) 0.25f else 0.10f), dotR * 1.7f, pos)
                        drawCircle(c.copy(alpha = if (isActive) 1f    else 0.40f), dotR, pos)
                        drawCircle(Color.White.copy(alpha = if (isActive) 0.90f else 0.28f), dotR * 0.38f, pos)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF1A3550), thickness = 1.dp)
            Spacer(Modifier.height(10.dp))

            // Two-column legend mirrors the two figures
            Row(Modifier.fillMaxWidth()) {
                // Left column: limb descriptions
                Column(Modifier.weight(1f).padding(end = 10.dp)) {
                    guideElectrodes.forEachIndexed { i, el ->
                        val isActive = i == activeStep
                        Row(
                            modifier              = Modifier.padding(vertical = 3.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(if (isActive) 10.dp else 8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(el.color.copy(alpha = if (isActive) 1f else 0.38f))
                            )
                            Text(
                                el.label,
                                fontSize   = 10.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color      = el.color.copy(alpha = if (isActive) 1f else 0.5f),
                                modifier   = Modifier.width(20.dp)
                            )
                            Text(
                                el.limbDesc,
                                fontSize   = 10.sp,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                color      = if (isActive) Color.White else Color(0xFF5E849E)
                            )
                        }
                    }
                }

                // Vertical rule
                Box(
                    Modifier
                        .width(1.dp)
                        .height(76.dp)
                        .background(Color(0xFF1A3550))
                )

                // Right column: chest descriptions
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    guideElectrodes.forEachIndexed { i, el ->
                        val isActive = i == activeStep
                        Row(
                            modifier              = Modifier.padding(vertical = 3.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(if (isActive) 10.dp else 8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(el.color.copy(alpha = if (isActive) 1f else 0.38f))
                            )
                            Text(
                                el.chestDesc,
                                fontSize   = 10.sp,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                color      = if (isActive) Color.White else Color(0xFF5E849E)
                            )
                        }
                    }
                }
            }
        }
    }
}
