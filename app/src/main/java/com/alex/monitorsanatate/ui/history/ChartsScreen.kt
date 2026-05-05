package com.alex.monitorsanatate.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alex.monitorsanatate.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    chartFilter: String,        // "Puls" sau "EKG"
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    // Setează filtrul pe instanța locală a ViewModel-ului
    LaunchedEffect(chartFilter) {
        viewModel.setFilter(chartFilter)
    }

    val measurements   by viewModel.measurements.collectAsStateWithLifecycle()
    val latestEcgData  by viewModel.latestEcgData.collectAsStateWithLifecycle()

    val isEkg   = chartFilter == "EKG"
    val isAi    = chartFilter == "AI"
    val accentColor = when {
        isEkg -> Ral5018Main
        isAi  -> Color(0xFF6750A4)
        else  -> PulseRedMain
    }
    val icon = when {
        isEkg -> Icons.Filled.GraphicEq
        isAi  -> Icons.Filled.AutoGraph
        else  -> Icons.Filled.Favorite
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = accentColor.copy(alpha = 0.15f),
                            modifier = Modifier.padding(end = 10.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.padding(6.dp).size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Grafice $chartFilter",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${measurements.size} înregistrări",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Înapoi",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            if (isAi) {
                // ── Grafic distribuție rezultate AI ──────────────────────────
                SectionHeader(
                    title    = "Distribuție rezultate AI",
                    subtitle = "Frecvența fiecărui diagnostic detectat de rețeaua neurală"
                )
                AiResultsChart(measurements = measurements)
            } else {
                // ── Grafic BPM Tendință ───────────────────────────────────────
                SectionHeader(
                    title    = "Tendință BPM în timp",
                    subtitle = if (isEkg) "BPM mediu per sesiune EKG" else "BPM mediu per sesiune de puls"
                )
                BpmTrendChart(
                    measurements = measurements,
                    chartType    = chartFilter
                )

                // ── Statistici rezumat ────────────────────────────────────────
                if (measurements.isNotEmpty()) {
                    val avgBpm  = measurements.filter { it.averageBpm > 0 }
                        .map { it.averageBpm }.average().toInt()
                    val minBpm  = measurements.minOf { it.minBpm }
                    val maxBpm  = measurements.maxOf { it.maxBpm }
                    val normal  = measurements.count { it.averageBpm in 60..100 }
                    val brady   = measurements.count { it.averageBpm in 1..59 }
                    val tachy   = measurements.count { it.averageBpm > 100 }

                    SectionHeader(
                        title    = "Rezumat clinic",
                        subtitle = "Pe baza tuturor înregistrărilor afișate"
                    )
                    MedicalStatsCard(
                        avgBpm  = avgBpm,
                        minBpm  = minBpm,
                        maxBpm  = maxBpm,
                        normal  = normal,
                        brady   = brady,
                        tachy   = tachy,
                        accentColor = accentColor
                    )
                }

                // ── Formă de undă EKG (doar pentru EKG) ──────────────────────
                if (isEkg) {
                    val latestEkg = measurements.maxByOrNull { it.startTime }
                    SectionHeader(
                        title    = "Formă de undă ECG",
                        subtitle = "Ultima înregistrare EKG salvată"
                    )
                    EcgWaveformChart(
                        ecgData       = latestEcgData,
                        recordingDate = latestEkg?.startTime
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  COMPONENTE INTERNE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(horizontal = 2.dp)) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(subtitle, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun MedicalStatsCard(
    avgBpm: Int,
    minBpm: Int,
    maxBpm: Int,
    normal: Int,
    brady: Int,
    tachy: Int,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Rând BPM ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("MIN", "$minBpm", "BPM", Color(0xFF4DB6B4))
                StatDivider()
                StatItem("MEDIU", "$avgBpm", "BPM", accentColor)
                StatDivider()
                StatItem("MAX", "$maxBpm", "BPM", Color(0xFFFF5252))
            }

            Divider(
                color = AppSurfaceHigh,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // ── Rând zone clinice ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("NORMAL", "$normal", "sesiuni", Color(0xFF4CAF50))
                StatDivider()
                StatItem("BRADICARDIE", "$brady", "sesiuni", Color(0xFFEF5350))
                StatDivider()
                StatItem("TAHICARDIE", "$tachy", "sesiuni", Color(0xFFFF9800))
            }

            // ── Interpretare automată ─────────────────────────────────────────
            val total = (normal + brady + tachy).coerceAtLeast(1)
            val pctNormal = normal * 100 / total
            Spacer(Modifier.height(12.dp))
            val (interpText, interpColor) = when {
                pctNormal >= 80 -> "Profil cardiac predominant normal (${pctNormal}% sesiuni în zonă normală)" to Color(0xFF4CAF50)
                brady > tachy   -> "Tendință bradicardică — consultați medicul dacă simptomele persistă" to Color(0xFFEF5350)
                else            -> "Tendință tahicardică — monitorizare continuă recomandată" to Color(0xFFFF9800)
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = interpColor.copy(alpha = 0.10f)
            ) {
                Text(
                    text = interpText,
                    fontSize = 12.sp,
                    color = interpColor,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.sp, color = TextDisabled, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = color)
        Text(unit, fontSize = 10.sp, color = TextSecondary)
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .height(44.dp)
            .width(1.dp)
            .background(AppSurfaceHigh)
    )
}
