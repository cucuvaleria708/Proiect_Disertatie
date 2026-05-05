package com.alex.monitorsanatate.domain.model

data class Measurement(
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val averageBpm: Int,
    val minBpm: Int,
    val maxBpm: Int,
    val measurementType: String = "PULS",  // "PULS", "EKG", "AI_ECG"
    val ecgData: List<Float> = emptyList(),
    val connectionMethod: ConnectionMethod,
    val notes: String? = null,
    val aiResult: String? = null,
    val aiProbabilities: String? = null
)
