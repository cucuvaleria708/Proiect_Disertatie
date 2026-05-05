package com.alex.monitorsanatate.domain.repository

import com.alex.monitorsanatate.domain.model.Measurement
import kotlinx.coroutines.flow.Flow

interface MeasurementRepository {
    fun getAllMeasurements(): Flow<List<Measurement>>
    suspend fun getMeasurementById(id: Long): Measurement?
    suspend fun getEcgDataForMeasurement(id: Long): List<Float>
    suspend fun saveMeasurement(measurement: Measurement)
    suspend fun deleteMeasurement(id: Long)
}
