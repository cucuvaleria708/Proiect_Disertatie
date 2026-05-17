package com.alex.monitorsanatate.ui

import android.widget.Toast
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Color
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

@Composable
fun SensorPulseMonitorScreen(onNavigateBack: () -> Unit) {
    val context   = LocalContext.current
    val viewModel: SensorPulseViewModel = hiltViewModel()

    val connectionState by viewModel.connectionState.collectAsState()
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

                    PulsTab(
                        currentBpm    = currentBpm,
                        finalBpm      = finalBpm,
                        status        = status,
                        timeRemaining = timeRemaining,
                        semnalValid   = semnalValid,
                        userAge       = userAge,
                        userGender    = userGender,
                        userWeight    = userWeight,
                        onStart       = { viewModel.sendStartCommand() },
                        onReset       = { viewModel.sendResetCommand() }
                    )
                }
            }
        }
    }
}

// ── Tab Puls ──────────────────────────────────────────────────────────────────

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
    onStart: () -> Unit,
    onReset: () -> Unit
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
                else        -> if (currentBpm > 40 && semnalValid) currentBpm else 0
            }
            val gaugeConnected = when (status) {
                "finalizat" -> finalBpm > 0
                "masurare"  -> true
                else        -> currentBpm > 40 && semnalValid
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
        onClick  = { if (status == "finalizat") onReset() else onStart() },
        enabled  = status == "asteptare" || status == "finalizat",
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
                status == "finalizat" -> "Măsoară din nou"
                !semnalValid          -> "Începe (Semnal Slab) · 15s"
                else                  -> "Începe Măsurarea  ·  15s"
            },
            fontSize   = 15.sp,
            fontWeight = FontWeight.Bold,
            color      = if (status == "masurare") TextSecondary else TextPrimary
        )
    }

    Spacer(Modifier.height(10.dp))
    Text(
        "Plasează degetul ferm pe senzor pentru rezultate precise.",
        fontSize  = 12.sp,
        color     = TextSecondary,
        textAlign = TextAlign.Center
    )
}

// ── Util ──────────────────────────────────────────────────────────────────────

private fun bpmCategory(bpm: Int): String = when {
    bpm < 50   -> "Bradicardie severă"
    bpm < 60   -> "Bradicardie"
    bpm <= 100 -> "Normal"
    bpm <= 120 -> "Tahicardie ușoară"
    else       -> "Tahicardie"
}
