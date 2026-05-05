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
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val currentBpm      by viewModel.currentBpm.collectAsStateWithLifecycle()
    val ecgPoints       by viewModel.ecgPoints.collectAsStateWithLifecycle()
    val isConnected = connectionState is ConnectionState.Connected

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

                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Surface(shape = RoundedCornerShape(20.dp), color = AppSurface) {
                                Text(
                                    "● Conectat: ${state.device.name}",
                                    fontSize   = 12.sp, color = Ral5018Main,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier   = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }

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
                                        bpm         = currentBpm,
                                        isConnected = true,
                                        size        = 180.dp
                                    )
                                    Spacer(Modifier.height(16.dp))
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

                        if (currentBpm > 0) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatChip(
                                    label    = "STATUS",
                                    value    = bpmZone(currentBpm),
                                    color    = bpmZoneColor(currentBpm),
                                    modifier = Modifier.weight(1f)
                                )
                                StatChip(
                                    label    = "INTERVAL",
                                    value    = "${60000 / currentBpm}ms",
                                    color    = Ral5018Light,
                                    modifier = Modifier.weight(1f)
                                )
                            }
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
private fun LiveDot() {
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
