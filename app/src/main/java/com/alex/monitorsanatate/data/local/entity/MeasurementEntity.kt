package com.alex.monitorsanatate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "",
    val startTime: Long,
    val endTime: Long,
    val averageBpm: Int,
    val minBpm: Int,
    val maxBpm: Int,
    val connectionMethod: String,
    val measurementType: String = "PULS",
    val notes: String? = null,
    val aiResult: String? = null,
    val aiProbabilities: String? = null
)
