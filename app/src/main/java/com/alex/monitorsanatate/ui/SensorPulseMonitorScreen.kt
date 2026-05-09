package com.alex.monitorsanatate.ui

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alex.monitorsanatate.domain.model.ConnectionState
import com.alex.monitorsanatate.ui.components.BpmGauge
import com.alex.monitorsanatate.ui.dashboard.NotConnectedCard
import com.alex.monitorsanatate.ui.theme.*

private val EcgBackground = Color(0xFF0D1B2A)
private val EcgGrid       = Color(0xFF1A3550)
private val EcgLineActive = Color(0xFF00E676)
private val EcgLineIdle   = Color(0xFF00E676).copy(alpha = 0.35f)

@Composable
fun SensorPulseMonitorScreen(onNavigateBack: () -> Unit) {
    val context   = LocalContext.current
    val viewModel: SensorPulseViewModel = hiltViewModel()

    val connectionState by viewModel.connectionState.collectAsState()
    val ecgBuffer       by viewModel.ecgBuffer.collectAsStateWithLifecycle()

    val userGender by viewModel.userGender.collectAsStateWithLifecycle()
    val userAge    by viewModel.userAge.collectAsStateWithLifecycle()
    val userWeight by viewModel.userWeight.collectAsStateWithLifecycle()

    var currentBpm    by remember { mutableStateOf(0) }
    var finalBpm      by remember { mutableStateOf(0) }
    var status        by remember { mutableStateOf("asteptare") }
    var timeRemaining by remember { mutableStateOf(0) }
    var semnalValid   by remember { mutableStateOf(false) }
    var signalRange   by remember { mutableStateOf(0) }

    var measurementStartTime by remember { mutableStateOf(0L) }
    var alreadySaved         by remember { mutableStateOf(false) }

    // 0 = tab Puls, 1 = tab ECG live
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.sensorData.collect { data ->
            currentBpm    = data.bpm
            finalBpm      = data.finalBpm
            status        = data.status
            timeRemaining = data.timeRemaining
            signalRange   = data.signalRange
            semnalValid   = data.semnalValid
        }
    }

    LaunchedEffect(status) {
        when (status) {
            "masurare" -> {
                if (measurementStartTime == 0L) measurementStartTime = System.currentTimeMillis()
                alreadySaved = false
            }
            "finalizat" -> {
                if (!alreadySaved && finalBpm > 0 && measurementStartTime > 0L) {
                    viewModel.saveMeasurement(finalBpm, measurementStartTime)
                    alreadySaved = true
                    Toast.makeText(context, "✓ Salvat în jurnal", Toast.LENGTH_SHORT).show()
                }
            }
            "asteptare" -> {
                measurementStartTime = 0L
                alreadySaved = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Înapoi", tint = TextPrimary)
                    }
                    Column {
                        Text("Senzor extern", fontSize = 20.sp,
                            fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("ESP32 · PulseSensor · BLE", fontSize = 11.sp, color = TextSecondary)
                    }
                }
                if (connectionState is ConnectionState.Connected) {
                    val (badgeTxt, badgeClr) = when {
                        semnalValid      -> "● Semnal bun"  to Ral5018Main
                        signalRange > 15 -> "● Semnal slab" to TextSecondary
                        else             -> "● Fără semnal" to TextDisabled
                    }
                    Surface(shape = RoundedCornerShape(20.dp), color = AppSurface) {
                        Text(badgeTxt, fontSize = 12.sp, color = badgeClr,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            when (val state = connectionState) {
                is ConnectionState.Disconnected ->
                    NotConnectedCard(hint = "Conectați senzorul puls urmând pașii: accesați Setări → Conexiune senzor puls, asigurați-vă că placa ESP32 este alimentată și inițiați scanarea Bluetooth.")

                is ConnectionState.Scanning,
                is ConnectionState.Connecting ->
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Ral5018Main)
                            Spacer(Modifier.height(12.dp))
                            Text("Se caută senzorul Puls...", fontSize = 14.sp, color = TextSecondary)
                        }
                    }

                is ConnectionState.Error ->
                    NotConnectedCard(message = state.message,
                        hint = "Reconectați senzorul accesând Setări → Conexiune senzor puls și inițiați o nouă scanare.")

                is ConnectionState.Connected -> {
                    // ── Badge conectat + deconectare ───────────────────────────
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Surface(shape = RoundedCornerShape(20.dp), color = AppSurface) {
                            Text("● Conectat: ${state.device.name}",
                                fontSize = 12.sp, color = Ral5018Main,
                                fontWeight = FontWeight.SemiBold,
                                modifier   = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
                        }
                        TextButton(onClick = { viewModel.disconnect() }) {
                            Text("Deconectare", fontSize = 12.sp, color = TextSecondary)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Selector tab Puls / ECG live ───────────────────────────
                    ModeTabSelector(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )

                    Spacer(Modifier.height(16.dp))

                    // ── Continut tab selectat ──────────────────────────────────
                    when (selectedTab) {
                        0 -> PulsTab(
                            currentBpm    = currentBpm,
                            finalBpm      = finalBpm,
                            status        = status,
                            timeRemaining = timeRemaining,
                            semnalValid   = semnalValid,
                            userAge       = userAge,
                            userGender    = userGender,
                            userWeight    = userWeight,
                            onStart       = { viewModel.sendStartCommand() }
                        )
                        1 -> EcgTab(
                            ecgBuffer   = ecgBuffer,
                            signalValid = semnalValid
                        )
                    }
                }
            }
        }
    }
}

// ── Selector mod ──────────────────────────────────────────────────────────────

@Composable
private fun ModeTabSelector(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppSurface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("🫀  Puls", "📈  ECG live").forEachIndexed { idx, label ->
            val isSelected = selectedTab == idx
            Surface(
                modifier  = Modifier.weight(1f),
                shape     = RoundedCornerShape(11.dp),
                color     = if (isSelected) Ral5018Main else Color.Transparent,
                onClick   = { onTabSelected(idx) }
            ) {
                Text(
                    text       = label,
                    fontSize   = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color      = if (isSelected) Color.White else TextSecondary,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.padding(vertical = 10.dp)
                )
            }
        }
    }
}

// ── Tab 0: Puls ───────────────────────────────────────────────────────────────

@Composable
private fun PulsTab(
    currentBpm: Int,
    finalBpm: Int,
    status: String,
    timeRemaining: Int,
    semnalValid: Boolean,
    userAge: Int,
    userGender: String,
    userWeight: Float,
    onStart: () -> Unit
) {
    // Card BPM hero cu BpmGauge (semiluna)
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(28.dp),
        colors    = CardDefaults.cardColors(containerColor = AppSurfaceHigh),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val gaugeBpm = when (status) {
                "finalizat" -> finalBpm
                "masurare"  -> currentBpm
                else        -> if (currentBpm > 40) currentBpm else 0
            }
            val gaugeConnected = when (status) {
                "finalizat" -> finalBpm > 0
                "masurare"  -> true
                else        -> currentBpm > 40
            }

            BpmGauge(
                bpm         = gaugeBpm,
                isConnected = gaugeConnected,
                size        = 200.dp
            )

            when (status) {
                "masurare" -> {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Măsurare în curs • ${timeRemaining}s",
                        fontSize   = 14.sp,
                        color      = Ral5018Light,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress   = { (15 - timeRemaining) / 15f },
                        modifier   = Modifier.fillMaxWidth().height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color      = Ral5018Main,
                        trackColor = AppSurfaceOverlay
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Nu mișca degetul", fontSize = 12.sp, color = TextSecondary)
                }
                "finalizat" -> {
                    Spacer(Modifier.height(14.dp))
                    Surface(shape = RoundedCornerShape(20.dp), color = AppSurface) {
                        Text(
                            "✓ Măsurare finalizată",
                            fontSize   = 14.sp,
                            color      = Ral5018Main,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Surface(shape = RoundedCornerShape(20.dp), color = AppSurface) {
                        Text(
                            bpmCategory(finalBpm),
                            fontSize   = 13.sp,
                            color      = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            modifier   = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        )
                    }
                }
                else -> {
                    if (currentBpm <= 40) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Plasează degetul pe senzor",
                            fontSize  = 13.sp,
                            color     = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (status == "finalizat" && finalBpm > 0) {
        Spacer(Modifier.height(16.dp))
        MedicalResultCard(bpm = finalBpm, age = userAge, gender = userGender, weight = userWeight)
    }

    Spacer(Modifier.height(16.dp))

    Button(
        onClick  = onStart,
        enabled  = status == "asteptare",
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape    = RoundedCornerShape(18.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = if (semnalValid) Ral5018Main else Ral5018Main.copy(alpha = 0.7f),
            disabledContainerColor = AppSurfaceHigh
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = when {
                status == "masurare"  -> "Măsurare în curs..."
                status == "finalizat" -> "Se resetează..."
                !semnalValid          -> "Începe (Semnal Slab) · 15s"
                else                  -> "Începe Măsurarea  ·  15s"
            },
            fontSize   = 15.sp,
            fontWeight = FontWeight.Bold,
            color      = if (status == "asteptare") TextPrimary else TextSecondary
        )
    }

    Spacer(Modifier.height(10.dp))
    Text(
        if (semnalValid) "Semnal stabil. Poți începe."
        else "Plasează degetul ferm pe senzor pentru rezultate precise.",
        fontSize  = 12.sp,
        color     = if (semnalValid) Ral5018Main else TextSecondary,
        textAlign = TextAlign.Center
    )
}

// ── Tab 1: ECG live ───────────────────────────────────────────────────────────

@Composable
private fun EcgTab(ecgBuffer: FloatArray, signalValid: Boolean) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = EcgBackground),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("ECG LIVE", fontSize = 11.sp, fontWeight = FontWeight.Black,
                    color = EcgLineActive, letterSpacing = 1.5.sp)
                Text(
                    if (ecgBuffer.isNotEmpty()) "${ecgBuffer.size} esc · 500 Hz"
                    else "aștept semnal...",
                    fontSize = 10.sp, color = EcgLineActive.copy(alpha = 0.5f)
                )
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(EcgBackground),
                contentAlignment = Alignment.Center
            ) {
                if (ecgBuffer.size < 4) {
                    Text(
                        "Plasează degetul pe senzor\npentru a vizualiza ECG-ul",
                        fontSize  = 13.sp,
                        color     = EcgLineActive.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.padding(horizontal = 24.dp)
                    )
                } else {
                    EcgWaveformCanvas(
                        samples     = ecgBuffer,
                        signalValid = signalValid,
                        modifier    = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Legenda semnal
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                val (dot, txt) = if (signalValid)
                    EcgLineActive to "Semnal valid — deget detectat"
                else
                    EcgLineIdle   to "Fără semnal — plasează degetul pe senzor"
                Surface(
                    modifier = Modifier.size(7.dp),
                    shape    = RoundedCornerShape(50),
                    color    = dot
                ) {}
                Spacer(Modifier.width(8.dp))
                Text(txt, fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

// ── Canvas ECG ────────────────────────────────────────────────────────────────

@Composable
private fun EcgWaveformCanvas(
    samples: FloatArray,
    signalValid: Boolean,
    modifier: Modifier = Modifier
) {
    val displayCount = minOf(samples.size, 500)
    val startIdx     = samples.size - displayCount

    var lo = Float.MAX_VALUE
    var hi = Float.MIN_VALUE
    for (i in startIdx until samples.size) {
        val v = samples[i]; if (v < lo) lo = v; if (v > hi) hi = v
    }
    val range   = (hi - lo).coerceAtLeast(30f)
    val padding = range * 0.08f
    val yLo     = lo - padding
    val yRange  = range + 2f * padding

    val lineColor = if (signalValid) EcgLineActive else EcgLineIdle

    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height

        for (row in 1..3) drawLine(EcgGrid, Offset(0f, h * row / 4f), Offset(w, h * row / 4f), 0.6f)
        for (col in 1..5) drawLine(EcgGrid, Offset(w * col / 6f, 0f), Offset(w * col / 6f, h), 0.6f)

        if (displayCount < 2) return@Canvas
        val xStep = w / (displayCount - 1).toFloat()
        val path  = Path()
        for (i in 0 until displayCount) {
            val norm = ((samples[startIdx + i] - yLo) / yRange).coerceIn(0f, 1f)
            val x = i * xStep; val y = h * (1f - norm)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// ── Util ──────────────────────────────────────────────────────────────────────

private fun bpmCategory(bpm: Int): String = when {
    bpm < 50   -> "Bradicardie severă"
    bpm < 60   -> "Bradicardie"
    bpm <= 100 -> "Normal"
    bpm <= 120 -> "Tahicardie ușoară"
    else       -> "Tahicardie"
}
