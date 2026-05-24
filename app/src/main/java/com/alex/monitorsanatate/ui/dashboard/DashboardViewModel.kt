package com.alex.monitorsanatate.ui.dashboard

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alex.monitorsanatate.di.EcgSnapshotHolder
import com.alex.monitorsanatate.ui.ecgdetail.EcgImageGenerator
import com.alex.monitorsanatate.domain.model.ConnectionMethod
import com.alex.monitorsanatate.domain.model.ConnectionState
import com.alex.monitorsanatate.domain.model.Measurement
import com.alex.monitorsanatate.domain.repository.ConnectionRepository
import com.alex.monitorsanatate.domain.repository.SensorRepository
import com.alex.monitorsanatate.domain.usecase.SaveMeasurementUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt


private const val LEAD_OFF_THRESHOLD_NORM = 30f / 1023f  // ecgBuffer e normalizat 0–1

// ─────────────────────────────────────────────────────────────────────────────
//  State-ul panoului de metrici ECG
// ─────────────────────────────────────────────────────────────────────────────
data class EkgMetrics(
    val bpm: Int            = 0,    // BPM calculat din interval R-R al semnalului ECG
    val rrIntervalMs: Int   = 0,    // interval intre batai (ms)
    val hrv: Float          = 0f,   // RMSSD (ms) — variabilitate ritm cardiac
    val pnn50: Int          = 0,    // % intervale R-R cu diferenta >50ms
    val interpretation: String = "—",
    val interpretationColor: InterpretationColor = InterpretationColor.NEUTRAL
)

enum class InterpretationColor { GREEN, YELLOW, RED, NEUTRAL }

enum class MeasurementState { IDLE, MEASURING, RESULT }

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sensorRepository: SensorRepository,
    private val connectionRepository: ConnectionRepository,
    private val saveMeasurementUseCase: SaveMeasurementUseCase,
    private val ecgSnapshotHolder: EcgSnapshotHolder
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = connectionRepository.connectionState

    private val _currentBpm = MutableStateFlow(0)
    val currentBpm: StateFlow<Int> = _currentBpm

    private val _ecgPoints = MutableStateFlow<List<Float>>(emptyList())
    val ecgPoints: StateFlow<List<Float>> = _ecgPoints

    private val _leadOffOk = MutableStateFlow(false)
    val leadOffOk: StateFlow<Boolean> = _leadOffOk

    // ── Stare masurare 15s ────────────────────────────────────────────────────
    private val _measurementState = MutableStateFlow(MeasurementState.IDLE)
    val measurementState: StateFlow<MeasurementState> = _measurementState

    private val _timeRemaining = MutableStateFlow(0)
    val timeRemaining: StateFlow<Int> = _timeRemaining

    private val _finalBpm = MutableStateFlow(0)
    val finalBpm: StateFlow<Int> = _finalBpm

    // ── Metrici ECG calculate pe Android ─────────────────────────────────────
    private val _ekgMetrics = MutableStateFlow(EkgMetrics())
    val ekgMetrics: StateFlow<EkgMetrics> = _ekgMetrics

    // ── Navigare la Analiza AI ────────────────────────────────────────────────
    private val _navigateToEcgAnalysis = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToEcgAnalysis: SharedFlow<Unit> = _navigateToEcgAnalysis

    // ── Export CSV ────────────────────────────────────────────────────────────
    private val _exportMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val exportMessage: SharedFlow<String> = _exportMessage

    private val ecgBuffer = mutableListOf<Float>()
    private val maxBufferSize = 2500

    private var sessionStartTime: Long = 0
    private val sessionBpmValues = mutableListOf<Int>()
    private var wasConnected = false

    init {
        // Colecteaza date de la senzor
        viewModelScope.launch {
            sensorRepository.sensorData.collect { data ->
                _currentBpm.value    = data.bpm
                _timeRemaining.value = data.timeRemaining
                _finalBpm.value      = data.finalBpm

                // Actualizeaza starea masuratoare pe baza status-ului din firmware
                _measurementState.value = when (data.status) {
                    "masurare"  -> MeasurementState.MEASURING
                    "finalizat" -> MeasurementState.RESULT
                    else        -> if (_measurementState.value == MeasurementState.RESULT)
                                       MeasurementState.RESULT  // pastreaza rezultatul pana la reset
                                   else MeasurementState.IDLE
                }

                if (data.bpm > 0 && wasConnected) sessionBpmValues.add(data.bpm)

                // Normalizeaza ADC 10-bit (0–1023) → [0, 1] pentru EcgWaveform
                val normalized = data.ecgPoints.map { it / 1023f }
                ecgBuffer.addAll(normalized)
                if (ecgBuffer.size > maxBufferSize) {
                    ecgBuffer.subList(0, ecgBuffer.size - maxBufferSize).clear()
                }
                _ecgPoints.value = ecgBuffer.toList()

                // Detecție lead-off: range local pe ultimele 50 eșantioane normalizate
                // Nu folosim data.semnalValid ca override — firmware-ul poate trimite true chiar fără electrozi
                val recent = if (ecgBuffer.size >= 50) ecgBuffer.takeLast(50) else ecgBuffer
                _leadOffOk.value = if (recent.size >= 20)
                    (recent.max() - recent.min()) >= LEAD_OFF_THRESHOLD_NORM
                else false
            }
        }

        // Urmareste conexiunea pentru salvare sesiune
        viewModelScope.launch {
            connectionRepository.connectionState.collect { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        if (!wasConnected) {
                            sessionStartTime = System.currentTimeMillis()
                            sessionBpmValues.clear()
                            wasConnected = true
                        }
                    }
                    else -> {
                        if (wasConnected) { saveSessionIfNeeded(); wasConnected = false }
                        _leadOffOk.value     = false
                        _measurementState.value = MeasurementState.IDLE
                    }
                }
            }
        }

        // Calculeaza metrici ECG la fiecare 3 secunde
        viewModelScope.launch {
            while (true) {
                delay(3000)
                val snapshot = _ecgPoints.value
                val bpm      = _currentBpm.value
                if (snapshot.size >= 300 && bpm > 0) {
                    val metrics = withContext(Dispatchers.Default) {
                        computeEkgMetrics(snapshot.takeLast(1500), bpm)
                    }
                    _ekgMetrics.value = metrics
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Porneste masurarea de 15s (trimite comanda "START" la ESP32)
    // ─────────────────────────────────────────────────────────────────────────
    fun startMeasurement() {
        if (_measurementState.value == MeasurementState.MEASURING) return
        if (!_leadOffOk.value) return  // electrozi nedetectați pe piele
        _measurementState.value = MeasurementState.IDLE  // reset vizual imediat
        _finalBpm.value = 0
        connectionRepository.sendCommand("START")
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Captureza ECG → Bitmap 256×256 (format clinic 4 rânduri) → Analiza AI
    // ─────────────────────────────────────────────────────────────────────────
    fun captureAndNavigateToEcgAnalysis() {
        val points = _ecgPoints.value
        if (points.size < 50) return
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.Default) {
                // Buffer-ul stochează valori normalizate [0,1]; EcgImageGenerator
                // așteaptă valori ADC brute (0–1023) pentru scalarea corectă
                EcgImageGenerator.generate(points.map { it * 1023f })
            }
            ecgSnapshotHolder.pendingBitmap = bmp
            _navigateToEcgAnalysis.tryEmit(Unit)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Calcul metrici ECG pe Android (R-peaks, HRV, interpretare)
    // ─────────────────────────────────────────────────────────────────────────
    private fun computeEkgMetrics(points: List<Float>, pulseSensorBpm: Int): EkgMetrics {
        val peaks = detectRPeaks(points)
        if (peaks.size < 3) {
            // Nu avem suficiente R-peaks din ECG — nu afisam BPM
            return EkgMetrics(
                bpm             = 0,
                rrIntervalMs    = 0,
                interpretation  = "Date insuficiente",
                interpretationColor = InterpretationColor.NEUTRAL
            )
        }

        // Intervale R-R in ms (la 500Hz: 1 sample = 2ms)
        val rrMs = peaks.zipWithNext { a, b -> (b - a) * 2 }

        // RMSSD
        val squaredDiffs = rrMs.zipWithNext { a, b -> (b - a).toDouble().pow(2) }
        val rmssd = if (squaredDiffs.isEmpty()) 0f
                    else sqrt(squaredDiffs.average()).toFloat()

        // pNN50: % intervale cu diferenta > 50ms
        val nn50 = rrMs.zipWithNext { a, b -> abs(b - a) > 50 }.count { it }
        val pnn50 = if (rrMs.size > 1) (nn50 * 100) / (rrMs.size - 1) else 0

        // BPM si interval R-R calculate exclusiv din semnalul ECG (AD8232)
        val rrMean = rrMs.average().toInt()
        val ecgBpm = if (rrMean > 0) 60000 / rrMean else 0

        return EkgMetrics(
            bpm                 = ecgBpm,
            rrIntervalMs        = rrMean,
            hrv                 = rmssd,
            pnn50               = pnn50,
            interpretation      = interpretRhythm(ecgBpm, rmssd),
            interpretationColor = rhythmColor(ecgBpm, rmssd)
        )
    }

    // Detectare R-peaks: maxime locale deasupra pragului adaptiv
    private fun detectRPeaks(points: List<Float>): List<Int> {
        if (points.size < 10) return emptyList()

        val mean      = points.average().toFloat()
        val variance  = points.map { (it - mean).pow(2) }.average()
        val std       = sqrt(variance).toFloat()
        val threshold = mean + 1.2f * std

        // Perioada refractara: 300ms = 150 esantioane la 500Hz
        val refractory = 150
        val peaks      = mutableListOf<Int>()
        var lastPeak   = -refractory

        for (i in 1 until points.size - 1) {
            if (points[i] > threshold &&
                points[i] >= points[i - 1] &&
                points[i] >= points[i + 1] &&
                i - lastPeak > refractory
            ) {
                peaks.add(i)
                lastPeak = i
            }
        }
        return peaks
    }

    private fun interpretRhythm(bpm: Int, rmssd: Float): String = when {
        bpm <= 0              -> "Fără semnal"
        bpm > 150             -> "Tahicardie severă (>150 bpm)"
        bpm > 100             -> "Tahicardie (>100 bpm)"
        bpm < 40              -> "Bradicardie severă (<40 bpm)"
        bpm < 60              -> "Bradicardie (<60 bpm)"
        rmssd > 120f          -> "Posibil ritm neregulat"
        rmssd in 20f..120f    -> "Ritm sinusal normal"
        rmssd > 0f && rmssd < 20f -> "HRV scăzut"
        else                  -> "Ritm sinusal normal"
    }

    private fun rhythmColor(bpm: Int, rmssd: Float): InterpretationColor = when {
        bpm <= 0                           -> InterpretationColor.NEUTRAL
        bpm > 100 || bpm < 60             -> InterpretationColor.RED
        rmssd > 120f || (rmssd > 0f && rmssd < 20f) -> InterpretationColor.YELLOW
        else                               -> InterpretationColor.GREEN
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Export ECG → CSV în folderul Downloads
    // ─────────────────────────────────────────────────────────────────────────
    fun exportEcgToCsv() {
        val snapshot = ecgBuffer.toList()
        if (snapshot.isEmpty()) {
            _exportMessage.tryEmit("Nu există date ECG de exportat.")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ts          = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val displayDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val fileName    = "ecg_$ts.csv"

                // Buffer normalizat [0,1] → ADC brut 0-1023
                val csv = buildString {
                    appendLine("# ECG Export — MonitorSanatate")
                    appendLine("# Data: $displayDate")
                    appendLine("# Esantioane: ${snapshot.size}")
                    appendLine("# Frecventa de esantionare: ~250 Hz  |  Gama ADC: 0-1023 (10-bit)")
                    appendLine("index,timestamp_ms,adc_value")
                    snapshot.forEachIndexed { i, v ->
                        appendLine("$i,${i * 4},${(v * 1023f).toInt()}")
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val cv = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE,    "text/csv")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = context.contentResolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv
                    ) ?: throw Exception("Nu s-a putut crea fișierul.")
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(csv) }
                        ?: throw Exception("Nu s-a putut scrie fișierul.")
                } else {
                    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    dir.mkdirs()
                    File(dir, fileName).writeText(csv)
                }

                _exportMessage.emit("Salvat în Downloads: $fileName")
            } catch (e: Exception) {
                _exportMessage.emit("Eroare export: ${e.message}")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Salveaza sesiunea la deconectare
    // ─────────────────────────────────────────────────────────────────────────
    private fun saveSessionIfNeeded() {
        val validBpms = sessionBpmValues.filter { it > 0 }
        if (sessionStartTime > 0 && validBpms.size >= 5) {
            val ecgSnapshot = ecgBuffer.toList().takeLast(2500)
            viewModelScope.launch {
                saveMeasurementUseCase(
                    Measurement(
                        startTime        = sessionStartTime,
                        endTime          = System.currentTimeMillis(),
                        averageBpm       = validBpms.average().toInt(),
                        minBpm           = validBpms.min(),
                        maxBpm           = validBpms.max(),
                        measurementType  = "ECG",
                        connectionMethod = ConnectionMethod.BLE,
                        ecgData          = ecgSnapshot
                    )
                )
            }
        }
        sessionStartTime = 0
        sessionBpmValues.clear()
    }

    override fun onCleared() {
        super.onCleared()
        if (wasConnected) saveSessionIfNeeded()
    }
}
