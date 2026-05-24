package com.alex.monitorsanatate.ui.ecgdetail

import android.content.Context
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

// ── Date despre clasele MIT-BIH ───────────────────────────────────────────────
data class BeatClass(
    val index: Int,
    val label: String,
    val shortCode: String,
    val colorLong: Long
)

val BEAT_CLASSES = listOf(
    BeatClass(0, "Bătaie normală",                "N", 0xFF2D6A4F),
    BeatClass(1, "Bătaie supraventriculară",       "S", 0xFFCA6702),
    BeatClass(2, "Extrasistolă ventriculară",      "V", 0xFF9B2226),
    BeatClass(3, "Bătaie de fuziune",              "F", 0xFFBC6C25),
    BeatClass(4, "Necalsificat / Pacemaker",       "Q", 0xFF4A7A9B),
)

data class BeatClassification(
    val classIndex: Int,
    val confidence: Float,
    val probabilities: List<Float>
)

// ── Clasificatorul ────────────────────────────────────────────────────────────
class EcgBeatClassifier(private val context: Context) {

    private var module: Module? = null

    companion object {
        // Durată fereastră: 130 eșantioane la 250 Hz ≈ 0.52 s
        // Identică cu durata bătăii în MIT-BIH (187 eș. la 360 Hz ≈ 0.52 s)
        const val WIN_TOTAL    = 130
        const val PRE_PEAK     = 35   // eșantioane înainte de vârful R în fereastră
        const val BEAT_SAMPLES = 187  // dimensiunea așteptată de model
        const val REFRACTORY   = 125  // perioadă refractară: 0.5 s la 250 Hz
    }

    // Încarcă modelul din assets (blocat, apelați pe Dispatchers.IO)
    fun preloadModule() {
        if (module == null) {
            module = LiteModuleLoader.loadModuleFromAsset(context.assets, "ecg_beat_model.ptl")
        }
    }

    private fun getModule(): Module = module
        ?: LiteModuleLoader.loadModuleFromAsset(context.assets, "ecg_beat_model.ptl")
            .also { module = it }

    // Detecție R-peaks pe buffer de valori ADC brute (0–1023)
    fun detectRPeaks(buffer: List<Float>): List<Int> {
        if (buffer.size < 10) return emptyList()
        val mean     = buffer.average().toFloat()
        val variance = buffer.map { (it - mean).pow(2) }.average()
        val std      = sqrt(variance).toFloat()
        val thresh   = mean + 1.2f * std

        val peaks    = mutableListOf<Int>()
        var lastPeak = -REFRACTORY
        for (i in 1 until buffer.size - 1) {
            if (buffer[i] > thresh &&
                buffer[i] >= buffer[i - 1] &&
                buffer[i] >= buffer[i + 1] &&
                i - lastPeak > REFRACTORY
            ) {
                peaks.add(i)
                lastPeak = i
            }
        }
        return peaks
    }

    // Extrage fereastră în jurul R-peak, resamplează la 187 eș., normalizează [0,1]
    fun extractBeat(buffer: List<Float>, rPeakIdx: Int): FloatArray? {
        val start = rPeakIdx - PRE_PEAK
        val end   = start + WIN_TOTAL
        if (start < 0 || end > buffer.size) return null

        val raw       = FloatArray(WIN_TOTAL) { buffer[start + it] }
        val resampled = resampleLinear(raw, BEAT_SAMPLES)

        val min   = resampled.min()
        val max   = resampled.max()
        val range = (max - min).coerceAtLeast(1e-6f)
        return FloatArray(BEAT_SAMPLES) { (resampled[it] - min) / range }
    }

    // Interpolare liniară: redimensionare input.size → targetSize
    private fun resampleLinear(input: FloatArray, targetSize: Int): FloatArray {
        val out   = FloatArray(targetSize)
        val ratio = (input.size - 1).toFloat() / (targetSize - 1).toFloat()
        for (i in 0 until targetSize) {
            val pos = i * ratio
            val lo  = pos.toInt()
            val hi  = (lo + 1).coerceAtMost(input.size - 1)
            val f   = pos - lo
            out[i]  = input[lo] * (1f - f) + input[hi] * f
        }
        return out
    }

    // Rulează inferența pe un beat preprocesate (187 eșantioane, [0,1])
    fun classify(beatArray: FloatArray): BeatClassification {
        require(beatArray.size == BEAT_SAMPLES)
        val mod     = getModule()
        val tensor  = Tensor.fromBlob(beatArray, longArrayOf(1L, 1L, BEAT_SAMPLES.toLong()))
        val logits  = mod.forward(IValue.from(tensor)).toTensor().dataAsFloatArray

        // Softmax numerically stable
        val maxL = logits.max()
        val exps = logits.map { exp((it - maxL).toDouble()).toFloat() }
        val sum  = exps.sum().coerceAtLeast(1e-9f)
        val probs = exps.map { it / sum }

        val idx = probs.indices.maxByOrNull { probs[it] } ?: 0
        return BeatClassification(
            classIndex    = idx,
            confidence    = probs[idx],
            probabilities = probs
        )
    }

    fun release() { module = null }
}
