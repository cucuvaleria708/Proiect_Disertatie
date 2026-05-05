package com.alex.monitorsanatate.domain.usecase

import com.alex.monitorsanatate.domain.model.Measurement
import com.alex.monitorsanatate.domain.repository.MeasurementRepository
import javax.inject.Inject

class SaveMeasurementUseCase @Inject constructor(
    private val measurementRepository: MeasurementRepository
) {
    suspend operator fun invoke(measurement: Measurement) {
        measurementRepository.saveMeasurement(measurement)
    }
}
