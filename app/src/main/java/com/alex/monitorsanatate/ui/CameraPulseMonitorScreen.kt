package com.alex.monitorsanatate.ui

import android.Manifest
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alex.monitorsanatate.ui.components.BpmGauge
import com.alex.monitorsanatate.ui.settings.bpmHighThreshold
import com.alex.monitorsanatate.ui.settings.bpmLowThreshold
import com.alex.monitorsanatate.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.*
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPulseMonitorScreen(onNavigateBack: () -> Unit) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (cameraPermissionState.status.isGranted) {
        CameraPulseContent(onNavigateBack)
    } else {
        Box(
            modifier = Modifier.fillMaxSize().background(AppBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null,
                    tint = Ral5018Main, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    "Avem nevoie de permisiunea pentru cameră pentru a măsura pulsul.",
                    textAlign = TextAlign.Center, color = TextPrimary
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Acordă permisiunea")
                }
                TextButton(onClick = onNavigateBack) {
                    Text("Înapoi", color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun CameraPulseContent(onNavigateBack: () -> Unit) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: CameraPulseViewModel = hiltViewModel()

    val userGender by viewModel.userGender.collectAsStateWithLifecycle()
    val userAge    by viewModel.userAge.collectAsStateWithLifecycle()
    val userWeight by viewModel.userWeight.collectAsStateWithLifecycle()

    var bpm                  by remember { mutableStateOf(0) }
    var isFingerDetected     by remember { mutableStateOf(false) }
    var isCalibrating        by remember { mutableStateOf(false) }
    var measurementProgress  by remember { mutableStateOf(0f) }
    var isMeasuring          by remember { mutableStateOf(false) }
    var finalBpm             by remember { mutableStateOf(0) }
    var status               by remember { mutableStateOf("asteptare") }
    var measurementStartTime by remember { mutableStateOf(0L) }

    val measuredBpms = remember { mutableStateListOf<Int>() }

    val analyzer = remember {
        HeartRateAnalyzer { detectedBpm, fingerPresent, calibrating ->
            bpm              = detectedBpm
            isFingerDetected = fingerPresent
            isCalibrating    = calibrating
            if (fingerPresent && isMeasuring && detectedBpm > 0) measuredBpms.add(detectedBpm)
            if (!fingerPresent) measuredBpms.clear()
        }
    }

    val cameraExecutor    = remember { Executors.newSingleThreadExecutor() }
    val cameraControl     = remember { mutableStateOf<CameraControl?>(null) }
    val cameraProviderRef = remember { mutableStateOf<ProcessCameraProvider?>(null) }

    LaunchedEffect(isMeasuring, isFingerDetected) {
        if (isMeasuring && isFingerDetected) {
            status = "masurare"
            measuredBpms.clear()
            measurementStartTime = System.currentTimeMillis()
            val steps = 150
            for (i in 1..steps) {
                if (!isMeasuring || !isFingerDetected) break
                delay(100)
                measurementProgress = i.toFloat() / steps
            }
            if (isMeasuring && isFingerDetected) {
                val valid = if (measuredBpms.size > 20) measuredBpms.drop(20) else measuredBpms
                finalBpm = when {
                    valid.isNotEmpty() -> valid.sorted().let { s -> s[s.size / 2] }
                    bpm > 0            -> bpm
                    else               -> 0
                }
                status    = if (finalBpm > 0) "finalizat" else "asteptare"
                isMeasuring = false
                if (finalBpm > 0 && measuredBpms.isNotEmpty()) {
                    viewModel.saveMeasurement(
                        bpms      = measuredBpms.toList(),
                        startTime = measurementStartTime
                    )
                    Toast.makeText(context, "✓ Salvat în jurnal", Toast.LENGTH_SHORT).show()
                }
            } else {
                measurementProgress = 0f
                status      = "asteptare"
                isMeasuring = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Înapoi", tint = TextPrimary)
                }
                Column {
                    Text(
                        "Cameră telefon",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary
                    )
                    Text(
                        "Măsurare puls prin optică PPG",
                        fontSize = 11.sp,
                        color    = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Card detecție ─────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(24.dp),
                colors   = CardDefaults.cardColors(containerColor = AppSurfaceHigh)
            ) {
                Column(
                    modifier            = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            "DETECȚIE DEGET",
                            fontSize      = 11.sp,
                            color         = TextSecondary,
                            fontWeight    = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                        // Indicator stare live
                        val (dotColor, stateLabel) = when {
                            !isFingerDetected -> Color(0xFF616161) to "Inactiv"
                            isCalibrating     -> Color(0xFFFFCC00) to "Calibrare..."
                            else              -> Color(0xFF4CAF50) to "Detectat"
                        }
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(dotColor, androidx.compose.foundation.shape.CircleShape)
                            )
                            Text(stateLabel, fontSize = 11.sp, color = dotColor,
                                fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Preview cameră
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(AppBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val future = ProcessCameraProvider.getInstance(ctx)
                                future.addListener({
                                    val provider = future.get()
                                    cameraProviderRef.value = provider
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    val analysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()
                                        .also { it.setAnalyzer(cameraExecutor, analyzer) }
                                    try {
                                        provider.unbindAll()
                                        val cam = provider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview, analysis
                                        )
                                        cameraControl.value = cam.cameraControl
                                        cameraControl.value?.enableTorch(true)
                                    } catch (e: Exception) {
                                        Log.e("CameraPulse", "Binding failed", e)
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Overlay stare
                        when {
                            !isFingerDetected -> Box(
                                modifier         = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.70f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Acoperiți complet obiectivul\ncamerei și blițul cu degetul.",
                                    color      = Color.White,
                                    fontSize   = 13.sp,
                                    textAlign  = TextAlign.Center,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 19.sp
                                )
                            }
                            isCalibrating -> Box(
                                modifier         = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.60f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        "⏳ Se calibrează...",
                                        color    = Color(0xFFFFCC00),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            else -> Box(
                                modifier         = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        "● Deget detectat",
                                        color    = Color(0xFF4CAF50),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Hint contextual
                    val hint = when {
                        !isFingerDetected     -> "Nu apăsați prea tare. Folosiți degetul arătător."
                        isCalibrating         -> "Mențineți degetul nemișcat. Calibrare în curs (~3 sec)."
                        isMeasuring           -> "Vă rugăm să rămâneți nemișcat în timpul procesului."
                        status == "finalizat" -> "Puteți ridica degetul. Rezultatul este afișat mai jos."
                        else                  -> "Calibrat. Apăsați butonul pentru a porni măsurarea."
                    }
                    Text(
                        hint,
                        fontSize   = 11.sp,
                        color      = TextSecondary,
                        textAlign  = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Card BPM cu BpmGauge (ascuns după finalizare) ─────────────────
            if (status != "finalizat") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(24.dp),
                    colors   = CardDefaults.cardColors(containerColor = AppSurfaceHigh)
                ) {
                    Column(
                        modifier            = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val displayBpm = if (isFingerDetected && bpm > 0) bpm else 0

                        BpmGauge(
                            bpm         = displayBpm,
                            isConnected = isFingerDetected,
                            size        = 200.dp
                        )

                        when {
                            isCalibrating -> {
                                Spacer(Modifier.height(14.dp))
                                LinearProgressIndicator(
                                    modifier   = Modifier
                                        .fillMaxWidth(0.55f)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color      = Ral5018Light,
                                    trackColor = AppSurfaceOverlay
                                )
                                Spacer(Modifier.height(6.dp))
                                Text("Calibrare în curs...", fontSize = 12.sp, color = TextSecondary)
                            }
                            status == "masurare" -> {
                                Spacer(Modifier.height(16.dp))
                                LinearProgressIndicator(
                                    progress   = { measurementProgress },
                                    modifier   = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color      = Ral5018Main,
                                    trackColor = AppSurfaceOverlay
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "${(measurementProgress * 15).toInt()} / 15 sec",
                                    fontSize = 11.sp,
                                    color    = TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
            }

            // ── Card rezultat medical (după finalizare) ───────────────────────
            if (status == "finalizat" && finalBpm > 0) {
                MedicalResultCard(
                    bpm    = finalBpm,
                    age    = userAge,
                    gender = userGender,
                    weight = userWeight
                )
                Spacer(Modifier.height(14.dp))
            }

            // ── Buton principal ───────────────────────────────────────────────
            Button(
                onClick = {
                    if (status == "finalizat") {
                        status              = "asteptare"
                        measurementProgress = 0f
                        finalBpm            = 0
                    } else {
                        isMeasuring = true
                    }
                },
                enabled  = (status == "finalizat") || (isFingerDetected && !isMeasuring && bpm > 0),
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape    = RoundedCornerShape(18.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Ral5018Main)
            ) {
                Text(
                    when {
                        status == "finalizat" -> "Măsoară din nou"
                        isMeasuring          -> "În curs..."
                        else                 -> "Începe măsurarea"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraControl.value?.enableTorch(false)
            cameraProviderRef.value?.unbindAll()
            cameraExecutor.shutdown()
        }
    }
}

// ── Card rezultat medical ─────────────────────────────────────────────────────

@Composable
fun MedicalResultCard(bpm: Int, age: Int, gender: String, weight: Float) {
    val safeAge  = age.takeIf  { it > 0 } ?: 30
    val lowThr   = bpmLowThreshold(safeAge)
    val highThr  = bpmHighThreshold(safeAge)
    val hasProfile = age > 0

    data class Zone(val label: String, val color: Color, val text: String)
    val zone = when {
        bpm < lowThr  -> Zone(
            "BRADICARDIE",
            Color(0xFFFF9800),
            "Ritmul cardiac este mai lent decât intervalul normal de repaus. " +
            "Dacă simptomele persistă (amețeli, oboseală), consultați un medic."
        )
        bpm > highThr -> Zone(
            "TAHICARDIE",
            Color(0xFFFF1744),
            "Ritmul cardiac este mai rapid decât intervalul normal de repaus. " +
            "Dacă simptomele persistă (palpitații, dificultăți de respirație), consultați un medic."
        )
        else          -> Zone(
            "NORMAL",
            Ral5018Light,
            "Ritmul cardiac se încadrează în intervalul normal de repaus pentru profilul dvs."
        )
    }

    val profileParts = buildList {
        add(if (gender == "F") "Feminin" else "Masculin")
        if (age > 0)    add("$age ani")
        if (weight > 0) add("${weight.toInt()} kg")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(24.dp),
        colors   = CardDefaults.cardColors(containerColor = AppSurfaceHigh)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // Header
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "REZULTAT MEDICAL",
                    fontSize      = 11.sp,
                    color         = TextSecondary,
                    fontWeight    = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Ral5018Main.copy(alpha = 0.12f)
                ) {
                    Text(
                        "ESC · AHA",
                        fontSize   = 10.sp,
                        color      = Ral5018Main,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = AppSurface, thickness = 0.8.dp)
            Spacer(Modifier.height(16.dp))

            // BPM + badge categorie
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "$bpm",
                        fontSize   = 56.sp,
                        fontWeight = FontWeight.Black,
                        color      = TextPrimary,
                        lineHeight = 56.sp
                    )
                    Text(
                        "BPM",
                        fontSize      = 13.sp,
                        color         = TextSecondary,
                        letterSpacing = 2.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = zone.color.copy(alpha = 0.14f)
                ) {
                    Text(
                        zone.label,
                        fontSize   = 17.sp,
                        color      = zone.color,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = AppSurface, thickness = 0.8.dp)
            Spacer(Modifier.height(14.dp))

            // Profil
            if (hasProfile) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Profil:", fontSize = 12.sp, color = TextSecondary)
                    Text(
                        profileParts.joinToString(" · "),
                        fontSize   = 12.sp,
                        color      = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(6.dp))
            }

            // Interval normal
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Interval normal:", fontSize = 12.sp, color = TextSecondary)
                Text(
                    "$lowThr – $highThr BPM",
                    fontSize   = 12.sp,
                    color      = Ral5018Light,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(14.dp))

            // Interpretare
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = zone.color.copy(alpha = 0.08f)
            ) {
                Text(
                    zone.text,
                    fontSize   = 13.sp,
                    color      = TextPrimary,
                    lineHeight = 20.sp,
                    modifier   = Modifier.padding(14.dp)
                )
            }

            if (!hasProfile) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "* Configurați profilul în Setări pentru o evaluare mai precisă.",
                    fontSize = 10.sp,
                    color    = TextSecondary.copy(alpha = 0.65f)
                )
            }
        }
    }
}

// ── Analizor cadre cameră ─────────────────────────────────────────────────────

class HeartRateAnalyzer(
    private val onResult: (bpm: Int, fingerPresent: Boolean, isCalibrating: Boolean) -> Unit
) : ImageAnalysis.Analyzer {

    private val frameValues = mutableListOf<Float>()
    private val timestamps  = mutableListOf<Long>()
    private val minFrames   = 30
    private val windowSize  = 150
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun analyze(image: ImageProxy) {
        try {
            val yPlane = image.planes[0]
            val vPlane = image.planes[2]

            val yBuffer      = yPlane.buffer
            val vBuffer      = vPlane.buffer
            val yRowStride   = yPlane.rowStride
            val yPixelStride = yPlane.pixelStride
            val vRowStride   = vPlane.rowStride
            val vPixelStride = vPlane.pixelStride
            val imgWidth     = image.width
            val imgHeight    = image.height

            val yData = ByteArray(yBuffer.remaining())
            val vData = ByteArray(vBuffer.remaining())
            yBuffer.get(yData)
            vBuffer.get(vData)

            // Eșantionăm centrul imaginii (degetul acoperă centrul)
            val margin   = 0.25f
            val rowStart = (imgHeight * margin).toInt()
            val rowEnd   = (imgHeight * (1f - margin)).toInt()
            val colStart = (imgWidth  * margin).toInt()
            val colEnd   = (imgWidth  * (1f - margin)).toInt()

            var redSum = 0.0
            var ySum   = 0.0
            var vSum   = 0.0
            val step   = 16
            var count  = 0

            for (row in rowStart until rowEnd step step) {
                for (col in colStart until colEnd step step) {
                    val yIdx = row * yRowStride + col * yPixelStride
                    val vIdx = (row / 2) * vRowStride + (col / 2) * vPixelStride
                    if (yIdx >= yData.size || vIdx >= vData.size) continue
                    val y   = yData[yIdx].toInt() and 0xFF
                    val v   = vData[vIdx].toInt() and 0xFF
                    redSum += y + 1.402 * (v - 128)
                    ySum   += y
                    vSum   += v
                    count++
                }
            }

            if (count == 0) { image.close(); return }

            val avgRed = (redSum / count).toFloat()
            val avgY   = (ySum   / count).toFloat()
            val avgV   = (vSum   / count).toFloat()

            val fingerPresent = avgY > 60 && avgV > 135 && avgRed > 140

            val now = System.currentTimeMillis()

            if (fingerPresent) {
                val smoothed = if (frameValues.isEmpty()) avgRed
                               else frameValues.last() * 0.45f + avgRed * 0.55f

                frameValues.add(smoothed)
                timestamps.add(now)

                if (frameValues.size > windowSize) {
                    frameValues.removeAt(0)
                    timestamps.removeAt(0)
                }

                if (frameValues.size < minFrames) {
                    mainHandler.post { onResult(0, true, true) }
                } else {
                    val bpm = calculateBpm()
                    mainHandler.post { onResult(bpm, true, bpm == 0) }
                }
            } else {
                frameValues.clear()
                timestamps.clear()
                mainHandler.post { onResult(0, false, false) }
            }
        } finally {
            image.close()
        }
    }

    private fun calculateBpm(): Int {
        if (frameValues.size < minFrames) return 0

        val mean     = frameValues.average().toFloat()
        val variance = frameValues.map { (it - mean) * (it - mean) }.average().toFloat()
        val std      = kotlin.math.sqrt(variance.toDouble()).toFloat()

        if (std < 0.3f) return 0

        val threshold   = mean + 0.25f * std
        val peaks       = mutableListOf<Int>()
        var lastPeakIdx = -20

        for (i in 2 until frameValues.size - 2) {
            val v = frameValues[i]
            if (v > frameValues[i - 1] && v > frameValues[i - 2] &&
                v > frameValues[i + 1] && v > frameValues[i + 2] &&
                v > threshold && (i - lastPeakIdx) >= 15
            ) {
                peaks.add(i)
                lastPeakIdx = i
            }
        }

        if (peaks.size < 2) return 0

        val intervals = mutableListOf<Long>()
        for (j in 1 until peaks.size) {
            val dt = timestamps[peaks[j]] - timestamps[peaks[j - 1]]
            if (dt in 300L..1500L) intervals.add(dt)
        }

        if (intervals.isEmpty()) return 0

        val sorted   = intervals.sorted()
        val medianMs = sorted[sorted.size / 2].toDouble()

        return (60000.0 / medianMs).toInt().coerceIn(40, 200)
    }
}
