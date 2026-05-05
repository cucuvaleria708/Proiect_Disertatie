package com.alex.monitorsanatate.domain.model

data class SensorData(
    val bpm: Int,
    val ecgPoints: List<Float>,
    val finalBpm: Int = 0,
    val status: String = "asteptare",
    val timeRemaining: Int = 0,
    val semnalValid: Boolean = false,
    val signalRange: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
