package com.alex.monitorsanatate.domain.usecase

import com.alex.monitorsanatate.domain.model.SensorData
import com.alex.monitorsanatate.domain.repository.SensorRepository
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

class ObserveSensorDataUseCase @Inject constructor(
    private val sensorRepository: SensorRepository
) {
    operator fun invoke(): SharedFlow<SensorData> = sensorRepository.sensorData
}
