package com.alex.monitorsanatate.ui.ecgdetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alex.monitorsanatate.domain.model.ConnectionState
import com.alex.monitorsanatate.ui.theme.AppBackground
import com.alex.monitorsanatate.ui.theme.AppSurface
import com.alex.monitorsanatate.ui.theme.AppSurfaceHigh
import com.alex.monitorsanatate.ui.theme.EcgGrid
import com.alex.monitorsanatate.ui.theme.EcgGridMajor
import com.alex.monitorsanatate.ui.theme.EcgLine
import com.alex.monitorsanatate.ui.theme.Ral5018Main
import com.alex.monitorsanatate.ui.theme.TextPrimary
import com.alex.monitorsanatate.ui.theme.TextSecondary

private const val ADC_CENTER     = 512f
private const val ADC_HALF_RANGE = 512f

// ── Card clasificare bătaie curentă ──────────────────────────────────────────
@Composable
private fun BeatClassificationCard(
    latest: BeatClassification,
    history: List<BeatClassification>,
    modifier: Modifier = Modifier
) {
    val beatClass  = BEAT_CLASSES.getOrNull(latest.classIndex) ?: return
    val classColor = Color(beatClass.colorLong)

    Card(
        modifier  = modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = AppSurfaceHigh),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Clasificare bătaie (MIT-BIH)",
                    style         = MaterialTheme.typography.labelMedium,
                    color         = TextSecondary,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(classColor.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        beatClass.shortCode,
                        color      = classColor,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    beatClass.label,
                    color      = classColor,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp
                )
                Text(
                    "${(latest.confidence * 100).toInt()}%",
                    color      = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp
                )
            }

            Spacer(Modifier.height(6.dp))

            LinearProgressIndicator(
                progress          = { latest.confidence },
                modifier          = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color             = classColor,
                trackColor        = AppSurface
            )

            if (history.size > 1) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = AppSurface, thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("Ultimele bătăi:", fontSize = 11.sp, color = TextSecondary)
                    history.forEach { beat ->
                        val bc = BEAT_CLASSES.getOrNull(beat.classIndex)
                        if (bc != null) {
                            Box(
                                modifier         = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(bc.colorLong).copy(alpha = 0.85f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    bc.shortCode,
                                    color      = Color.White,
                                    fontSize   = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Modal: Raport complet al sesiunii ────────────────────────────────────────
@Composable
private fun BeatReportSheet(allBeats: List<BeatClassification>) {
    val total     = allBeats.size
    val countMap  = allBeats.groupingBy { it.classIndex }.eachCount()
    val dominant  = countMap.maxByOrNull { it.value }
    val dominantClass = dominant?.key?.let { BEAT_CLASSES.getOrNull(it) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            "Raport clasificare EKG",
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "$total bătăi analizate în sesiunea curentă",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(Modifier.height(20.dp))

        if (dominantClass != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                colors   = CardDefaults.cardColors(
                    containerColor = Color(dominantClass.colorLong).copy(alpha = 0.12f)
                )
            ) {
                Row(
                    modifier          = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier         = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(dominantClass.colorLong).copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            dominantClass.shortCode,
                            color      = Color(dominantClass.colorLong),
                            fontWeight = FontWeight.Bold,
                            fontSize   = 18.sp
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "Tip predominant",
                            fontSize = 11.sp,
                            color    = TextSecondary
                        )
                        Text(
                            dominantClass.label,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 16.sp,
                            color      = Color(dominantClass.colorLong)
                        )
                        val pct = if (total > 0) dominant!!.value * 100 / total else 0
                        Text(
                            "${dominant!!.value} bătăi ($pct%)",
                            fontSize = 12.sp,
                            color    = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = AppSurface)
        Spacer(Modifier.height(16.dp))

        Text(
            "Distribuție per clasă",
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color      = TextPrimary
        )
        Spacer(Modifier.height(12.dp))

        BEAT_CLASSES.forEach { beatClass ->
            val count = countMap[beatClass.index] ?: 0
            val frac  = if (total > 0) count.toFloat() / total else 0f
            val color = Color(beatClass.colorLong)

            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier         = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        beatClass.shortCode,
                        color      = color,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 12.sp
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            beatClass.label,
                            fontSize = 13.sp,
                            color    = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "$count bătăi · ${(frac * 100).toInt()}%",
                            fontSize = 12.sp,
                            color    = TextSecondary
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress   = { frac },
                        modifier   = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color      = color,
                        trackColor = AppSurface
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── Ecranul principal ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcgDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: EcgDetailViewModel = hiltViewModel()
) {
    val ecgPoints       by viewModel.ecgPoints.collectAsStateWithLifecycle()
    val currentBpm      by viewModel.currentBpm.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val isLeadOff       by viewModel.isLeadOff.collectAsStateWithLifecycle()
    val latestBeat      by viewModel.latestBeat.collectAsStateWithLifecycle()
    val beatHistory     by viewModel.beatHistory.collectAsStateWithLifecycle()
    val allBeats        by viewModel.allBeats.collectAsStateWithLifecycle()

    val isConnected  = connectionState is ConnectionState.Connected
    val hasData      = ecgPoints.size >= 50
    val hasBeats     = allBeats.isNotEmpty()

    var zoomLevel     by remember { mutableFloatStateOf(1f) }
    var showReport    by remember { mutableStateOf(false) }
    val sheetState    = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.exportMessage.collect { msg -> snackbarState.showSnackbar(msg) }
    }

    if (showReport && hasBeats) {
        ModalBottomSheet(
            onDismissRequest  = { showReport = false },
            sheetState        = sheetState,
            containerColor    = AppBackground,
            dragHandle        = {
                Box(
                    modifier         = Modifier
                        .padding(vertical = 12.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AppSurface)
                )
            }
        ) {
            BeatReportSheet(allBeats = allBeats)
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Verificare EKG",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = TextPrimary
                        )
                        Text(
                            when {
                                !isConnected -> "Senzor deconectat"
                                isLeadOff    -> "Electrozi neataşaţi"
                                hasBeats     -> "${allBeats.size} bătăi clasificate"
                                else         -> "Înregistrare activă..."
                            },
                            fontSize = 11.sp,
                            color    = when {
                                !isConnected -> TextSecondary
                                isLeadOff    -> Color(0xFFCA6702)
                                else         -> Color(0xFF2D6A4F)
                            }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Înapoi",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarState) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor   = MaterialTheme.colorScheme.onErrorContainer,
                    shape          = RoundedCornerShape(12.dp),
                    modifier       = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // ── Bara stare: BPM + calitate semnal ───────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape     = RoundedCornerShape(14.dp),
                colors    = CardDefaults.cardColors(containerColor = AppSurfaceHigh),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // BPM
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null,
                            tint     = if (isConnected && !isLeadOff) Ral5018Main else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text       = if (isConnected && currentBpm > 0) "$currentBpm BPM" else "-- BPM",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 18.sp,
                            color      = if (isConnected && currentBpm > 0) Ral5018Main else TextSecondary
                        )
                    }

                    // Stare conexiune
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        !isConnected -> Color(0xFF9E9E9E)
                                        isLeadOff    -> Color(0xFFCA6702)
                                        else         -> Color(0xFF2D6A4F)
                                    }
                                )
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = when {
                                !isConnected -> "Deconectat"
                                isLeadOff    -> "Fără electrozi"
                                else         -> "Semnal activ"
                            },
                            fontSize = 13.sp,
                            color    = when {
                                !isConnected -> TextSecondary
                                isLeadOff    -> Color(0xFFCA6702)
                                else         -> Color(0xFF2D6A4F)
                            },
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Eșantioane colectate
                    Text(
                        text     = if (hasData) "${ecgPoints.size} eș." else "—",
                        fontSize = 12.sp,
                        color    = TextSecondary
                    )
                }
            }

            // ── Traseu ECG live ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppSurfaceHigh)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                zoomLevel = (zoomLevel * zoom).coerceIn(0.5f, 5f)
                            }
                        }
                ) {
                    val w       = size.width
                    val h       = size.height
                    val centerY = h / 2f

                    val smallStep = w / (50f * zoomLevel)
                    var x = 0f
                    while (x <= w) {
                        drawLine(EcgGrid, Offset(x, 0f), Offset(x, h), strokeWidth = 0.5f)
                        x += smallStep
                    }
                    var y = 0f
                    while (y <= h) {
                        drawLine(EcgGrid, Offset(0f, y), Offset(w, y), strokeWidth = 0.5f)
                        y += smallStep
                    }
                    val largeStep = smallStep * 5f
                    x = 0f
                    while (x <= w) {
                        drawLine(EcgGridMajor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                        x += largeStep
                    }
                    y = 0f
                    while (y <= h) {
                        drawLine(EcgGridMajor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                        y += largeStep
                    }
                    drawLine(EcgGridMajor, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 1.5f)

                    if (ecgPoints.size >= 2) {
                        val visible = (500 / zoomLevel).toInt().coerceAtLeast(50)
                        val pts     = if (ecgPoints.size > visible) ecgPoints.takeLast(visible) else ecgPoints
                        val path    = Path()
                        val ampScale = h * 0.35f
                        val stepX   = w / (visible - 1).toFloat()

                        pts.forEachIndexed { idx, v ->
                            val px   = idx * stepX
                            val norm = (v - ADC_CENTER) / ADC_HALF_RANGE
                            val py   = centerY - norm * ampScale
                            if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        drawPath(path, EcgLine, style = Stroke(width = 2.5f))
                    }
                }

                // Overlay când nu există semnal
                if (ecgPoints.size < 2 || (isConnected && isLeadOff)) {
                    Column(
                        modifier             = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment  = Alignment.CenterHorizontally,
                        verticalArrangement  = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (!isConnected) Icons.Filled.Bluetooth
                                          else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            modifier   = Modifier.size(44.dp),
                            tint       = TextSecondary.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = when {
                                !isConnected -> "Senzorul nu este conectat\nMergi la Setări → Conexiune senzor"
                                isLeadOff    -> "Aplică electrozii ECG pe piele\npentru a înregistra semnalul"
                                else         -> "Aşteaptă date ECG..."
                            },
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ── Card clasificare bătaie curentă ─────────────────────────────
            if (latestBeat != null && isConnected && !isLeadOff) {
                BeatClassificationCard(
                    latest   = latestBeat!!,
                    history  = beatHistory,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── Butoane de jos ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppBackground)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Buton Descarcă CSV
                OutlinedButton(
                    onClick  = { viewModel.exportEcgToCsv() },
                    enabled  = hasData,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape  = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor         = Ral5018Main,
                        disabledContentColor = TextSecondary
                    )
                ) {
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Descarcă CSV",
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 14.sp
                    )
                }

                // Buton Raport clasificare
                Button(
                    onClick  = { showReport = true },
                    enabled  = hasBeats,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape  = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor         = Ral5018Main,
                        disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(3.dp)
                ) {
                    Icon(
                        Icons.Filled.Assessment,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint     = Color.White
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Raport EKG",
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 14.sp,
                        color      = Color.White
                    )
                }
            }
        }
    }
}
