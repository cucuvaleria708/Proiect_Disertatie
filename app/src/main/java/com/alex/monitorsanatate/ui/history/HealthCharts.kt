package com.alex.monitorsanatate.ui.history

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.alex.monitorsanatate.domain.model.Measurement
import com.alex.monitorsanatate.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
//  BPM TREND CHART  — simplu și clar
//  O singură linie BPM în timp, gradient jos, stats sub grafic
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BpmTrendChart(
    measurements: List<Measurement>,
    chartType: String,
    modifier: Modifier = Modifier
) {
    val sorted = remember(measurements) {
        measurements.filter { it.averageBpm > 0 }.sortedBy { it.startTime }.takeLast(30)
    }
    val lineColor = if (chartType == "EKG") Ral5018Main else PulseRedMain
    val dateFmt   = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }
    val textMeasurer = rememberTextMeasurer()

    val animProgress by animateFloatAsState(
        targetValue = if (sorted.size >= 2) 1f else 0f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "bpm_anim"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                "BPM în timp",
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (sorted.size < 2) "Adaugă cel puțin 2 înregistrări"
                else "Ultimele ${sorted.size} sesiuni · de la ${dateFmt.format(Date(sorted.first().startTime))} la ${dateFmt.format(Date(sorted.last().startTime))}",
                fontSize = 11.sp, color = TextSecondary
            )

            Spacer(Modifier.height(12.dp))

            if (sorted.size < 2) {
                Box(Modifier.fillMaxWidth().height(140.dp), Alignment.Center) {
                    Text(
                        "Nu sunt suficiente date pentru grafic.",
                        color = TextDisabled, fontSize = 13.sp, textAlign = TextAlign.Center
                    )
                }
                return@Column
            }

            // ── Canvas ────────────────────────────────────────────────────────
            Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                val lM = 42.dp.toPx(); val rM = 8.dp.toPx()
                val tM = 8.dp.toPx();  val bM = 28.dp.toPx()
                val cL = lM; val cT = tM; val cR = size.width - rM; val cB = size.height - bM
                val cW = cR - cL; val cH = cB - cT

                val allBpm = sorted.map { it.averageBpm.toFloat() }
                val raw    = allBpm.min(); val rawMax = allBpm.max()
                val pad    = ((rawMax - raw) * 0.20f).coerceAtLeast(10f)
                val yMin   = (raw - pad).coerceAtLeast(20f)
                val yMax   = (rawMax + pad).coerceAtMost(230f)

                fun bY(b: Float) = cT + cH * (1f - (b.coerceIn(yMin, yMax) - yMin) / (yMax - yMin))
                fun iX(i: Int)  = cL + cW * i / (sorted.size - 1).toFloat()

                // ── 4 linii orizontale de referință ──────────────────────────
                val steps = 4
                for (s in 0..steps) {
                    val bpm = yMin + (yMax - yMin) * s / steps
                    val gy  = bY(bpm)
                    drawLine(Color(0xFF2E2E2E), Offset(cL, gy), Offset(cR, gy), 0.8f)
                    drawText(
                        textMeasurer, bpm.toInt().toString(),
                        Offset(0f, gy - 7.dp.toPx()),
                        TextStyle(fontSize = 9.sp, color = Color(0xFF777777))
                    )
                }

                // ── Etichete X: prima și ultima dată ─────────────────────────
                drawText(
                    textMeasurer, dateFmt.format(Date(sorted.first().startTime)),
                    Offset(cL, cB + 5.dp.toPx()),
                    TextStyle(fontSize = 9.sp, color = Color(0xFF777777))
                )
                val lastLabel = dateFmt.format(Date(sorted.last().startTime))
                drawText(
                    textMeasurer, lastLabel,
                    Offset(cR - 20.dp.toPx(), cB + 5.dp.toPx()),
                    TextStyle(fontSize = 9.sp, color = Color(0xFF777777))
                )

                // ── Fill gradient ─────────────────────────────────────────────
                val animN = (2 + (sorted.size - 2) * animProgress).toInt().coerceIn(2, sorted.size)
                val fill  = Path()
                sorted.take(animN).forEachIndexed { i, m ->
                    val x = iX(i); val y = bY(m.averageBpm.toFloat())
                    if (i == 0) { fill.moveTo(x, cB); fill.lineTo(x, y) } else fill.lineTo(x, y)
                }
                fill.lineTo(iX(animN - 1), cB); fill.close()
                drawPath(
                    fill,
                    Brush.verticalGradient(
                        listOf(lineColor.copy(alpha = 0.30f), Color.Transparent),
                        startY = cT, endY = cB
                    )
                )

                // ── Linie principală ──────────────────────────────────────────
                val line = Path()
                sorted.take(animN).forEachIndexed { i, m ->
                    val x = iX(i); val y = bY(m.averageBpm.toFloat())
                    if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
                }
                drawPath(line, lineColor, style = Stroke(2.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // ── Punct final ───────────────────────────────────────────────
                if (animN == sorted.size) {
                    val lx = iX(sorted.size - 1)
                    val ly = bY(sorted.last().averageBpm.toFloat())
                    drawCircle(AppSurface, 6f, Offset(lx, ly))
                    drawCircle(lineColor,  4f, Offset(lx, ly))
                }
            }

            // ── Stats MIN / MED / MAX ─────────────────────────────────────────
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                val minV = sorted.minOf { it.minBpm }
                val maxV = sorted.maxOf { it.maxBpm }
                val avgV = sorted.map { it.averageBpm }.average().toInt()
                SimpleStatLabel("MIN", "$minV", "BPM", Color(0xFF4DB6B4))
                SimpleStatLabel("MEDIU", "$avgV", "BPM", lineColor)
                SimpleStatLabel("MAX", "$maxV", "BPM", Color(0xFFEF5350))
            }
        }
    }
}

@Composable
private fun SimpleStatLabel(label: String, value: String, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.sp, color = TextDisabled, letterSpacing = 0.5.sp, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = color)
        Text(unit,  fontSize = 10.sp, color = TextSecondary)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  ECG WAVEFORM CHART
//  Aspect clasic hârtie medicală — grid verde, linie teal cu glow
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EcgWaveformChart(
    ecgData: List<Float>,
    recordingDate: Long? = null,
    modifier: Modifier = Modifier
) {
    val display = remember(ecgData) {
        if (ecgData.size <= 600) ecgData
        else {
            val step = ecgData.size / 600
            ecgData.filterIndexed { i, _ -> i % step == 0 }.take(600)
        }
    }

    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.getDefault()) }

    val animProgress by animateFloatAsState(
        targetValue = if (display.isNotEmpty()) 1f else 0f,
        animationSpec = tween(2200, easing = LinearEasing),
        label = "ecg_anim"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1A0C)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Formă de undă ECG",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ral5018Light
                    )
                    if (recordingDate != null) {
                        Text(
                            dateFmt.format(Date(recordingDate)),
                            fontSize = 10.sp,
                            color = Ral5018Light.copy(alpha = 0.55f)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Ral5018Main.copy(alpha = 0.15f)
                ) {
                    Text(
                        "${ecgData.size} pts",
                        fontSize = 10.sp,
                        color = Ral5018Light,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (display.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Nu există date ECG pentru această înregistrare.",
                        color = Ral5018Light.copy(alpha = 0.45f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
                return@Column
            }

            // ── Canvas ECG ────────────────────────────────────────────────────
            Canvas(modifier = Modifier.fillMaxWidth().height(170.dp)) {
                val lM = 6.dp.toPx(); val rM = 6.dp.toPx()
                val tM = 6.dp.toPx(); val bM = 6.dp.toPx()
                val cL = lM; val cT = tM
                val cR = size.width - rM; val cB = size.height - bM
                val cW = cR - cL; val cH = cB - cT

                // ── Hârtie medicală ───────────────────────────────────────────
                val smallDiv = cW / 25f     // 25 diviziuni mici pe orizontală
                val smallDivV = cH / 10f   // 10 pe verticală

                // Grilă mică
                var gx = cL
                while (gx <= cR + 0.5f) {
                    drawLine(Color(0xFF162616), Offset(gx, cT), Offset(gx, cB), 0.5f)
                    gx += smallDiv
                }
                var gy = cT
                while (gy <= cB + 0.5f) {
                    drawLine(Color(0xFF162616), Offset(cL, gy), Offset(cR, gy), 0.5f)
                    gy += smallDivV
                }

                // Grilă majoră (5×)
                gx = cL
                while (gx <= cR + 0.5f) {
                    drawLine(Color(0xFF1E4020), Offset(gx, cT), Offset(gx, cB), 0.9f)
                    gx += smallDiv * 5f
                }
                gy = cT
                while (gy <= cB + 0.5f) {
                    drawLine(Color(0xFF1E4020), Offset(cL, gy), Offset(cR, gy), 0.9f)
                    gy += smallDivV * 5f
                }

                // ── Linie ECG ─────────────────────────────────────────────────
                val vMin = display.min(); val vMax = display.max()
                val vRange = (vMax - vMin).coerceAtLeast(0.001f)
                val vPad = vRange * 0.12f

                fun vToY(v: Float) =
                    cT + cH * (1f - (v - vMin + vPad) / (vRange + 2 * vPad))
                fun iToX(i: Int) =
                    cL + cW * i / (display.size - 1).toFloat()

                val animN = (display.size * animProgress).toInt().coerceAtLeast(2)
                val path = Path()
                display.take(animN).forEachIndexed { i, v ->
                    val x = iToX(i); val y = vToY(v)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                // Glow
                drawPath(
                    path, Color(0xFF008F8C).copy(alpha = 0.28f),
                    style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Linie principală
                drawPath(
                    path, Color(0xFF4DB6B4),
                    style = Stroke(width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // ── Etichete standard EKG ─────────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("25 mm/s", "10 mm/mV", "0.05–150 Hz").forEach { label ->
                    Text(
                        label,
                        fontSize = 9.sp,
                        color = Ral5018Light.copy(alpha = 0.45f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  SECȚIUNE GRAFICE — container folosit în HistoryScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ChartsSection(
    measurements: List<Measurement>,
    currentFilter: String,
    latestEcgData: List<Float>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        when (currentFilter) {
            "AI" -> {
                AiResultsChart(measurements = measurements)
            }
            else -> {
                BpmTrendChart(measurements = measurements, chartType = currentFilter)
                if (currentFilter == "EKG") {
                    Spacer(Modifier.height(12.dp))
                    EcgWaveformChart(
                        ecgData = latestEcgData,
                        recordingDate = measurements.maxByOrNull { it.startTime }?.startTime
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  AI RESULTS CHART
//  Distribuție rezultate + linie cronologică
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AiResultsChart(
    measurements: List<Measurement>,
    modifier: Modifier = Modifier
) {
    val sorted  = remember(measurements) { measurements.sortedBy { it.startTime } }
    val dateFmt = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }

    // Grupare după aiResult
    val groups = remember(sorted) {
        sorted
            .groupBy { it.aiResult?.trim()?.ifEmpty { "Necunoscut" } ?: "Necunoscut" }
            .entries
            .sortedByDescending { it.value.size }
    }
    val total = sorted.size.coerceAtLeast(1)

    // Culoare per rezultat
    fun resultColor(result: String): Color = when {
        result.contains("Normal", ignoreCase = true)  ||
        result.contains("Sinus",  ignoreCase = true)  -> Color(0xFF43A047)   // verde
        result.contains("Fibril", ignoreCase = true)  ||
        result.contains("AFib",   ignoreCase = true)  -> Color(0xFFEF5350)   // roșu
        result.contains("Block",  ignoreCase = true)  ||
        result.contains("Bloc",   ignoreCase = true)  -> Color(0xFFFF9800)   // portocaliu
        result.contains("Tachy",  ignoreCase = true)  ||
        result.contains("Brady",  ignoreCase = true)  -> Color(0xFFFF7043)   // portocaliu închis
        else                                          -> Color(0xFF9E9E9E)   // gri
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header ────────────────────────────────────────────────────────
            Text(
                "Distribuție rezultate AI",
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (sorted.isEmpty()) "Nu există sesiuni AI înregistrate"
                else "${sorted.size} sesiuni · ${dateFmt.format(Date(sorted.first().startTime))} – ${dateFmt.format(Date(sorted.last().startTime))}",
                fontSize = 11.sp, color = TextSecondary
            )

            if (sorted.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(100.dp), Alignment.Center) {
                    Text("Nu sunt date AI disponibile.", color = TextDisabled, fontSize = 13.sp)
                }
                return@Column
            }

            Spacer(Modifier.height(16.dp))

            // ── Bare orizontale per rezultat ──────────────────────────────────
            groups.forEach { (result, items) ->
                val pct   = items.size.toFloat() / total
                val color = resultColor(result)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bulă colorată
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color, RoundedCornerShape(50))
                    )
                    Spacer(Modifier.width(8.dp))

                    // Nume rezultat
                    Text(
                        result,
                        fontSize = 12.sp, color = TextPrimary,
                        modifier = Modifier.width(100.dp),
                        maxLines = 1
                    )
                    Spacer(Modifier.width(6.dp))

                    // Bara de progres
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(18.dp)
                            .background(AppSurfaceHigh, RoundedCornerShape(50))
                    ) {
                        val animPct by animateFloatAsState(
                            targetValue = pct,
                            animationSpec = tween(900, easing = FastOutSlowInEasing),
                            label = "bar_$result"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animPct)
                                .fillMaxHeight()
                                .background(color.copy(alpha = 0.85f), RoundedCornerShape(50))
                        )
                    }
                    Spacer(Modifier.width(8.dp))

                    // Număr + procent
                    Text(
                        "${items.size}  (${(pct * 100).toInt()}%)",
                        fontSize = 11.sp, color = TextSecondary,
                        modifier = Modifier.width(56.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Divider(color = AppSurfaceHigh, thickness = 1.dp)
            Spacer(Modifier.height(12.dp))

            // ── Linie cronologică ─────────────────────────────────────────────
            Text(
                "Cronologie sesiuni",
                fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))

            // Linie cu puncte colorate
            Box(modifier = Modifier.fillMaxWidth().height(36.dp)) {
                // Linie de bază
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.Center)
                        .background(AppSurfaceHigh, RoundedCornerShape(1.dp))
                )
                // Puncte
                if (sorted.size == 1) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.Center)
                            .background(resultColor(sorted[0].aiResult ?: ""), RoundedCornerShape(50))
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        sorted.forEach { m ->
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        resultColor(m.aiResult?.trim() ?: ""),
                                        RoundedCornerShape(50)
                                    )
                            )
                        }
                    }
                }
            }

            // Prima și ultima dată sub linie
            if (sorted.size >= 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(dateFmt.format(Date(sorted.first().startTime)), fontSize = 9.sp, color = TextDisabled)
                    Text(dateFmt.format(Date(sorted.last().startTime)),  fontSize = 9.sp, color = TextDisabled)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Legendă ───────────────────────────────────────────────────────
            val mostCommon = groups.firstOrNull()
            if (mostCommon != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = resultColor(mostCommon.key).copy(alpha = 0.10f)
                ) {
                    Text(
                        "Cel mai frecvent rezultat: ${mostCommon.key} — ${mostCommon.value.size} din $total sesiuni",
                        fontSize = 12.sp,
                        color = resultColor(mostCommon.key),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

