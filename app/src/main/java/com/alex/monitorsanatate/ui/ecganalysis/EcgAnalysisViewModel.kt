package com.alex.monitorsanatate.ui.ecganalysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import javax.inject.Inject
import kotlin.math.exp
import com.alex.monitorsanatate.di.EcgSnapshotHolder
import com.alex.monitorsanatate.domain.model.ConnectionMethod
import com.alex.monitorsanatate.domain.model.Measurement
import com.alex.monitorsanatate.domain.usecase.SaveMeasurementUseCase

// ── Clase ECG ────────────────────────────────────────────────────────────────
val ECG_CLASSES = listOf(
    EcgClass(0, "Infarct miocardic acut",          "Semne ECG compatibile cu infarct miocardic acut.",              0xFF9B2226),
    EcgClass(1, "Infarct miocardic în antecedente","Modificări ECG sugestive pentru infarct miocardic în antecedente.", 0xFFCA6702),
    EcgClass(2, "Ritm cardiac anormal",            "Aritmie sau anomalie de conducere detectată.",                  0xFFBC6C25),
    EcgClass(3, "Ritm cardiac normal",             "Ritm cardiac normal, fără anomalii detectate.",                 0xFF2D6A4F),
)

data class EcgClass(
    val index: Int,
    val label: String,
    val description: String,
    val colorLong: Long
)

sealed class AnalysisUiState {
    object Idle       : AnalysisUiState()
    object Loading    : AnalysisUiState()
    data class Result(
        val predictedIndex: Int,
        val probabilities: List<Float>,
        val bitmap: Bitmap
    ) : AnalysisUiState()
    data class Error(val message: String) : AnalysisUiState()
}

@HiltViewModel
class EcgAnalysisViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val saveMeasurementUseCase: SaveMeasurementUseCase,
    private val ecgSnapshotHolder: EcgSnapshotHolder
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val uiState: StateFlow<AnalysisUiState> = _uiState

    // true dacă imaginea curentă a fost generată din semnal live (nu din galerie)
    private val _fromSensor = MutableStateFlow(false)
    val fromSensor: StateFlow<Boolean> = _fromSensor

    /**
     * Dacă există un bitmap generat din semnal live, îl afișează fără a porni analiza.
     * Utilizatorul vede imaginea ECG generată și apasă manual "Analizează".
     * Trebuie apelat din Screen via LaunchedEffect (funcționează și cu ViewModel refolosit).
     */
    fun checkAndAnalyzePendingSnapshot() {
        val snapshot = ecgSnapshotHolder.pendingBitmap ?: return
        ecgSnapshotHolder.pendingBitmap = null
        _fromSensor.value = true
        _uiState.value = AnalysisUiState.Result(
            predictedIndex = -1,
            probabilities  = emptyList(),
            bitmap         = snapshot
        )
    }

    private var module: Module? = null

    private suspend fun getModule(): Module = withContext(Dispatchers.IO) {
        if (module == null) {
            try {
                module = LiteModuleLoader.loadModuleFromAsset(context.assets, "ecg_model.ptl")
            } catch (e: Exception) {
                e.printStackTrace()
                throw Exception("Nu s-a putut încărca modelul AI din assets: ${e.message}")
            }
        }
        module ?: throw Exception("Modelul AI nu este disponibil.")
    }

    fun loadImageFromUri(uri: Uri) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream, null, bounds)
                    }
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
                    val maxDim = 1024
                    var sampleSize = 1
                    var w = bounds.outWidth
                    var h = bounds.outHeight
                    while (w > maxDim || h > maxDim) { sampleSize *= 2; w /= 2; h /= 2 }
                    val options = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                        inSampleSize = sampleSize
                    }
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream, null, options)
                    }
                } catch (e: Exception) { e.printStackTrace(); null }
            }
            if (bitmap != null) {
                _uiState.value = AnalysisUiState.Result(predictedIndex = -1, probabilities = emptyList(), bitmap = bitmap)
            } else {
                _uiState.value = AnalysisUiState.Error("Nu s-a putut încărca imaginea.")
            }
        }
    }

    fun analyze(bitmap: Bitmap) {
        val fromSensor = _fromSensor.value
        _fromSensor.value = false
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            _uiState.value = AnalysisUiState.Loading
            try {
                val result = withContext(Dispatchers.Default) { runInference(bitmap) }
                val endTime = System.currentTimeMillis()

                saveMeasurementUseCase(
                    Measurement(
                        startTime        = startTime,
                        endTime          = endTime,
                        averageBpm       = 0,
                        minBpm           = 0,
                        maxBpm           = 0,
                        measurementType  = "AI_ECG",
                        connectionMethod = if (fromSensor) ConnectionMethod.BLE else ConnectionMethod.GALLERY,
                        aiResult         = ECG_CLASSES.getOrNull(result.predictedIndex)?.label,
                        aiProbabilities  = result.probabilities.joinToString(",")
                    )
                )

                _uiState.value = result
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = AnalysisUiState.Error(
                    "Eroare la analiză: ${e.message}\n\nVerifică dacă modelul ecg_model.ptl este corect adăugat în assets."
                )
            }
        }
    }

    private suspend fun runInference(originalBitmap: Bitmap): AnalysisUiState.Result {
        val mod = getModule()
        val scaled = Bitmap.createScaledBitmap(originalBitmap, 256, 256, true)
        val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
        val std  = floatArrayOf(0.229f, 0.224f, 0.225f)
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(scaled, mean, std)
        val outputTensor = mod.forward(IValue.from(inputTensor)).toTensor()
        val logProbs = outputTensor.dataAsFloatArray
        val probs = logProbs.map { exp(it.toDouble()).toFloat() }
        val sum = probs.sum().coerceAtLeast(1e-6f)
        val tempProbs = probs.map { it / sum }.toMutableList()
        val maxIdx = tempProbs.indices.maxByOrNull { tempProbs[it] } ?: 0
        tempProbs[maxIdx] += 1.0f - tempProbs.sum()
        return AnalysisUiState.Result(
            predictedIndex = maxIdx,
            probabilities  = tempProbs.toList(),
            bitmap         = originalBitmap
        )
    }

    fun reset() {
        _uiState.value = AnalysisUiState.Idle
        _fromSensor.value = false
    }

    override fun onCleared() {
        super.onCleared()
        module = null
    }
}
