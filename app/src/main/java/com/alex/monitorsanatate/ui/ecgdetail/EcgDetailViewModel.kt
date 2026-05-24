package com.alex.monitorsanatate.ui.ecgdetail

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alex.monitorsanatate.di.EcgSnapshotHolder
import com.alex.monitorsanatate.domain.model.ConnectionState
import com.alex.monitorsanatate.domain.repository.ConnectionRepository
import com.alex.monitorsanatate.domain.repository.SensorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
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

// Semnal plat → electrozi neconectați: range < 30 ADC pe ultimele 50 eșantioane
private const val LEAD_OFF_RANGE_THRESHOLD = 30f

@HiltViewModel
class EcgDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sensorRepository: SensorRepository,
    private val connectionRepository: ConnectionRepository,
    private val ecgSnapshotHolder: EcgSnapshotHolder
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = connectionRepository.connectionState

    private val _ecgPoints    = MutableStateFlow<List<Float>>(emptyList())
    val ecgPoints: StateFlow<List<Float>> = _ecgPoints

    private val _currentBpm   = MutableStateFlow(0)
    val currentBpm: StateFlow<Int> = _currentBpm

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _isLeadOff    = MutableStateFlow(true)
    val isLeadOff: StateFlow<Boolean> = _isLeadOff

    // ── Clasificare bătăi ─────────────────────────────────────────────────────
    private val _latestBeat   = MutableStateFlow<BeatClassification?>(null)
    val latestBeat: StateFlow<BeatClassification?> = _latestBeat

    // Ultimele 5 bătăi — afișate în cardul live
    private val _beatHistory  = MutableStateFlow<List<BeatClassification>>(emptyList())
    val beatHistory: StateFlow<List<BeatClassification>> = _beatHistory

    // Toate bătăile din sesiunea curentă — folosite pentru raportul complet
    private val _allBeats = MutableStateFlow<List<BeatClassification>>(emptyList())
    val allBeats: StateFlow<List<BeatClassification>> = _allBeats

    private val _exportMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val exportMessage: SharedFlow<String> = _exportMessage

    private val ecgBuffer     = mutableListOf<Float>()
    private val maxBufferSize = 2500  // ~10 s la 250 Hz

    private val beatClassifier = EcgBeatClassifier(context)

    // Contor absolut de eșantioane primite — folosit pentru a evita
    // re-procesarea aceluiași interval de buffer după trim
    private var totalSamplesAdded = 0L
    private var lastSearchEndAbs  = 0L  // limita superioară a ultimei căutări de R-peaks

    init {
        // Preîncarcă modelul 1D în fundal
        viewModelScope.launch(Dispatchers.IO) {
            try { beatClassifier.preloadModule() } catch (_: Exception) {}
        }

        // Resetare la deconectare
        viewModelScope.launch {
            connectionRepository.connectionState.collect { state ->
                if (state !is ConnectionState.Connected) {
                    ecgBuffer.clear()
                    _ecgPoints.value    = emptyList()
                    _currentBpm.value   = 0
                    _isLeadOff.value    = true
                    _latestBeat.value   = null
                    _beatHistory.value  = emptyList()
                    _allBeats.value     = emptyList()
                    totalSamplesAdded   = 0L
                    lastSearchEndAbs    = 0L
                }
            }
        }

        viewModelScope.launch {
            sensorRepository.sensorData.collect { data ->
                _currentBpm.value = data.bpm

                if (data.ecgPoints.isNotEmpty()) {
                    val newCount = data.ecgPoints.size
                    totalSamplesAdded += newCount

                    ecgBuffer.addAll(data.ecgPoints)
                    if (ecgBuffer.size > maxBufferSize) {
                        ecgBuffer.subList(0, ecgBuffer.size - maxBufferSize).clear()
                    }
                    _ecgPoints.value = ecgBuffer.toList()
                    _isLeadOff.value = detectLeadOff(ecgBuffer, data.semnalValid)

                    // Clasificare bătăi — cauta R-peaks în intervalul nou
                    classifyNewBeats()
                }
            }
        }
    }

    private fun classifyNewBeats() {
        // Buffer start în termeni absoluți (după trim)
        val bufferStartAbs = totalSamplesAdded - ecgBuffer.size

        // Lasăm 100 de eșantioane marjă la sfârșit (fereastră post-peak)
        val searchEndAbs   = totalSamplesAdded - 100L
        // Pornim căutarea de unde ne-am oprit data trecută
        val searchStartAbs = lastSearchEndAbs.coerceAtLeast(bufferStartAbs)

        val searchFrom = (searchStartAbs - bufferStartAbs).toInt()
        val searchTo   = (searchEndAbs   - bufferStartAbs).toInt().coerceAtMost(ecgBuffer.size)

        if (searchTo <= searchFrom + 10 || ecgBuffer.size < 250) return

        val snapshot  = ecgBuffer.toList()
        val slice     = snapshot.subList(searchFrom, searchTo)
        val relPeaks  = beatClassifier.detectRPeaks(slice)

        relPeaks.forEach { relIdx ->
            val bufIdx   = searchFrom + relIdx
            val beatData = beatClassifier.extractBeat(snapshot, bufIdx) ?: return@forEach
            val beatCopy = beatData.copyOf()

            viewModelScope.launch(Dispatchers.Default) {
                try {
                    val result = beatClassifier.classify(beatCopy)
                    _latestBeat.value  = result
                    val h = _beatHistory.value.takeLast(4) + result
                    _beatHistory.value = h
                    _allBeats.value    = _allBeats.value + result
                } catch (_: Exception) {}
            }
        }

        lastSearchEndAbs = searchEndAbs
    }

    private fun detectLeadOff(buffer: List<Float>, firmwareSemnalValid: Boolean): Boolean {
        val recent = if (buffer.size >= 50) buffer.takeLast(50) else buffer
        if (recent.size < 20) return true
        val range = recent.max() - recent.min()
        // Range check primar — firmware-ul poate trimite semnalValid=true chiar fără electrozi
        if (range < LEAD_OFF_RANGE_THRESHOLD) return true
        // Dacă firmware-ul explicit invalidează semnalul, respectăm indicația
        return !firmwareSemnalValid
    }

    fun generateAndStoreSnapshot(onReady: () -> Unit) {
        if (ecgBuffer.size < 50) return
        viewModelScope.launch {
            _isGenerating.value = true
            withContext(Dispatchers.Default) {
                ecgSnapshotHolder.pendingBitmap = EcgImageGenerator.generate(ecgBuffer.toList())
            }
            _isGenerating.value = false
            onReady()
        }
    }

    // ── Export CSV ────────────────────────────────────────────────────────────

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

                // Construim conținutul CSV
                val csv = buildString {
                    appendLine("# ECG Export — MonitorSanatate")
                    appendLine("# Data: $displayDate")
                    appendLine("# Esantioane: ${snapshot.size}")
                    appendLine("# Frecventa de esantionare: ~250 Hz  |  Gama ADC: 0-1023 (10-bit)")
                    appendLine("index,timestamp_ms,adc_value")
                    snapshot.forEachIndexed { i, v ->
                        // Fiecare eșantion ≙ 4 ms la 250 Hz
                        appendLine("$i,${i * 4},${v.toInt()}")
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ — MediaStore Downloads (nu necesită permisiune suplimentară)
                    val cv = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE,    "text/csv")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = context.contentResolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv
                    ) ?: throw Exception("Nu s-a putut crea fișierul în Downloads.")
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(csv) }
                        ?: throw Exception("Nu s-a putut scrie fișierul.")
                } else {
                    // Android < 10 — folderul public Downloads (necesită WRITE_EXTERNAL_STORAGE)
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

    override fun onCleared() {
        super.onCleared()
        beatClassifier.release()
    }
}
