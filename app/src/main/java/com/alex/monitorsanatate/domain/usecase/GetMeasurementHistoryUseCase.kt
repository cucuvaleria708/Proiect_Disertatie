package com.alex.monitorsanatate.domain.usecase

import com.alex.monitorsanatate.domain.model.Measurement
import com.alex.monitorsanatate.domain.repository.MeasurementRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMeasurementHistoryUseCase @Inject constructor(
    private val measurementRepository: MeasurementRepository
) {
    operator fun invoke(): Flow<List<Measurement>> =
        measurementRepository.getAllMeasurements()

    suspend fun getById(id: Long): Measurement? =
        measurementRepository.getMeasurementById(id)

    suspend fun getEcgData(measurementId: Long): List<Float> =
        measurementRepository.getEcgDataForMeasurement(measurementId)
}
