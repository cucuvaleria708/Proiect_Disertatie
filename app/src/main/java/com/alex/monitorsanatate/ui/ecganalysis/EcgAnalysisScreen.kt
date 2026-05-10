package com.alex.monitorsanatate.ui.ecganalysis

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alex.monitorsanatate.ui.components.AppScreenHeader
import com.alex.monitorsanatate.ui.theme.*

private val ChartBg    = Color(0xFF0D1B2A)
private val ChartGrid  = Color(0xFF1A3550)
private val EcgGreen   = Color(0xFF00E676)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcgAnalysisScreen(viewModel: EcgAnalysisViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.loadImageFromUri(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            AppScreenHeader(
                title = "Analiză AI",
                subtitle = "Interpretare automată ECG"
            )

            Column(
                modifier            = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (val state = uiState) {

                    is AnalysisUiState.Idle -> {
                        UploadCard(onGallery = { galleryLauncher.launch("image/*") })
                        Spacer(Modifier.height(16.dp))
                        InfoCard()
                    }

                    is AnalysisUiState.Result -> {
                        ImagePreviewCard(bitmap = state.bitmap, compact = state.predictedIndex != -1)

                        Spacer(Modifier.height(16.dp))

                        if (state.predictedIndex == -1) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick   = { viewModel.reset() },
                                    modifier  = Modifier.weight(1f).height(54.dp),
                                    shape     = RoundedCornerShape(16.dp),
                                    colors    = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                    border    = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(AppSurfaceOverlay, Color.Transparent)))
                                ) {
                                    Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Schimbă", fontWeight = FontWeight.Medium)
                                }
                                Button(
                                    onClick  = { viewModel.analyze(state.bitmap) },
                                    modifier = Modifier.weight(1f).height(54.dp),
                                    shape    = RoundedCornerShape(16.dp),
                                    colors   = ButtonDefaults.buttonColors(containerColor = Ral5018Main),
                                    elevation = ButtonDefaults.buttonElevation(2.dp)
                                ) {
                                    Text("Analizează", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                }
                            }
                        } else {
                            PredictionResultCard(state.predictedIndex)

                            Spacer(Modifier.height(16.dp))

                            ProbabilityWaveformCard(state.predictedIndex, state.probabilities)

                            Spacer(Modifier.height(16.dp))

                            MedicalDisclaimerCard()

                            Spacer(Modifier.height(24.dp))

                            Button(
                                onClick  = { viewModel.reset() },
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(56.dp),
                                shape    = CircleShape,
                                colors   = ButtonDefaults.buttonColors(containerColor = Ral5018Main)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Refresh, null, Modifier.size(20.dp), tint = Color.White)
                                    Spacer(Modifier.width(10.dp))
                                    Text("Analiză nouă", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    is AnalysisUiState.Loading -> {
                        Spacer(Modifier.height(80.dp))
                        CircularProgressIndicator(color = Ral5018Main, modifier = Modifier.size(56.dp), strokeWidth = 4.dp)
                        Spacer(Modifier.height(20.dp))
                        Text("Se analizează ECG-ul...", fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                        Text("Modelul CNN procesează imaginea", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
                    }

                    is AnalysisUiState.Error -> ErrorCard(state.message) { viewModel.reset() }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun UploadCard(onGallery: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceHigh),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(70.dp),
                    shape = CircleShape,
                    color = AppSurfaceOverlay
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🫀", fontSize = 36.sp)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("Încarcă imagine ECG", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    "Alege o imagine ECG din galerie pentru analiza automată",
                    fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )
                Button(
                    onClick = onGallery,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ral5018Main),
                    elevation = ButtonDefaults.buttonElevation(2.dp)
                ) {
                    Icon(Icons.Filled.Image, null, Modifier.size(20.dp), tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Alege din galerie", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun ImagePreviewCard(bitmap: Bitmap, compact: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppSurfaceOverlay)
                    .padding(vertical = 8.dp, horizontal = 16.dp)
            ) {
                Text("Imagine sursă", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
            }
            Image(
                bitmap = bitmap.asImageBitmap(), contentDescription = "ECG",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 160.dp else 240.dp)
                    .background(Color.Black),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun ProbabilityChart(probabilities: List<Float>, predictedIndex: Int) {
    val anim0 by animateFloatAsState(probabilities.getOrElse(0) { 0f }, tween(900, easing = FastOutSlowInEasing), label = "p0")
    val anim1 by animateFloatAsState(probabilities.getOrElse(1) { 0f }, tween(900, easing = FastOutSlowInEasing), label = "p1")
    val anim2 by animateFloatAsState(probabilities.getOrElse(2) { 0f }, tween(900, easing = FastOutSlowInEasing), label = "p2")
    val anim3 by animateFloatAsState(probabilities.getOrElse(3) { 0f }, tween(900, easing = FastOutSlowInEasing), label = "p3")
    val animProbs = listOf(anim0, anim1, anim2, anim3)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ChartBg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w      = size.width
            val h      = size.height
            val padX   = 20f
            val padY   = 12f
            val chartW = w - 2 * padX
            val chartH = h - 2 * padY
            val n      = animProbs.size

            // Horizontal grid lines at 25 / 50 / 75 %
            listOf(0.25f, 0.50f, 0.75f).forEach { pct ->
                val y = padY + chartH * (1f - pct)
                drawLine(ChartGrid, Offset(padX, y), Offset(w - padX, y), strokeWidth = 0.8f)
            }
            // Baseline
            drawLine(ChartGrid, Offset(padX, padY + chartH), Offset(w - padX, padY + chartH), strokeWidth = 1.2f)

            // Vertical bars
            val sectionW    = chartW / n
            val barW        = sectionW * 0.50f
            val barHOffset  = (sectionW - barW) / 2f

            animProbs.forEachIndexed { i, prob ->
                val barH  = (chartH * prob).coerceAtLeast(0f)
                val left  = padX + i * sectionW + barHOffset
                val top   = padY + chartH - barH
                val color = Color(ECG_CLASSES[i].colorLong)
                    .let { c -> if (i == predictedIndex) c else c.copy(alpha = 0.35f) }
                drawRect(color, Offset(left, top), Size(barW, barH))
            }
        }
    }
}

@Composable
private fun PredictionResultCard(predictedIndex: Int) {
    val predicted = ECG_CLASSES.getOrNull(predictedIndex)
    val pColor    = predicted?.let { Color(it.colorLong) } ?: TextSecondary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceHigh),
        elevation = CardDefaults.cardElevation(10.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("DIAGNOSTIC AI", fontSize = 11.sp, color = TextSecondary, letterSpacing = 1.5.sp, fontWeight = FontWeight.Black)

                predicted?.let {
                    Surface(
                        shape  = RoundedCornerShape(20.dp),
                        color  = pColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, pColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text     = it.label.uppercase(),
                            fontSize = 11.sp,
                            color    = pColor,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            predicted?.let {
                Text(it.label, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text(it.description, fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun ProbabilityWaveformCard(predictedIndex: Int, probabilities: List<Float>) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = ChartBg),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("DISTRIBUȚIE PROBABILITĂȚI", fontSize = 11.sp, fontWeight = FontWeight.Black,
                    color = EcgGreen, letterSpacing = 1.5.sp)
                Text("CNN · 4 clase", fontSize = 10.sp, color = EcgGreen.copy(alpha = 0.5f))
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ChartBg)
            ) {
                ProbabilityChart(probabilities = probabilities, predictedIndex = predictedIndex)
            }

            Spacer(Modifier.height(12.dp))

            ECG_CLASSES.forEach { cls ->
                val prob    = probabilities.getOrElse(cls.index) { 0f }
                val clColor = Color(cls.colorLong)
                val isTop   = cls.index == predictedIndex
                val pct     = prob * 100
                val pctText = if (pct < 0.01f) "< 0.01%" else String.format("%.2f%%", pct)
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(7.dp),
                        shape    = CircleShape,
                        color    = clColor.copy(alpha = if (isTop) 1f else 0.4f)
                    ) {}
                    Spacer(Modifier.width(8.dp))
                    Text(cls.label, fontSize = 13.sp,
                        color      = if (isTop) Color.White else Color.White.copy(alpha = 0.5f),
                        fontWeight = if (isTop) FontWeight.Bold else FontWeight.Normal,
                        modifier   = Modifier.weight(1f))
                    Text(pctText, fontSize = 12.sp,
                        color      = if (isTop) clColor else Color.White.copy(alpha = 0.4f),
                        fontWeight = if (isTop) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
private fun MedicalDisclaimerCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, TextSecondary.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                "Rezultat orientativ generat de AI. Nu înlocuiește diagnosticul oficial al unui medic cardiolog.",
                fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp, fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().border(0.5.dp, TextDisabled.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceHigh)
    ) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Warning, null, tint = TextDisabled, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("Eroare Analiză", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(message, fontSize = 14.sp, color = TextSecondary,
                textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 16.dp))
            Button(
                onClick = onRetry, shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Ral5018Main),
                modifier = Modifier.height(48.dp).padding(horizontal = 16.dp)
            ) {
                Text("Încearcă din nou", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "CATEGORII DETECTATE", fontSize = 11.sp, fontWeight = FontWeight.Black,
                color = Color.White, letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(16.dp))
            ECG_CLASSES.forEach { cls ->
                Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(cls.colorLong)))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(cls.label, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(cls.description, fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}
