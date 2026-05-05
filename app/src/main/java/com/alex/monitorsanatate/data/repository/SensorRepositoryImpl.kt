package com.alex.monitorsanatate.data.repository

import com.alex.monitorsanatate.domain.model.SensorData
import com.alex.monitorsanatate.domain.repository.SensorRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorRepositoryImpl @Inject constructor() : SensorRepository {

    private val _sensorData = MutableSharedFlow<SensorData>(replay = 1, extraBufferCapacity = 64)
    override val sensorData: SharedFlow<SensorData> = _sensorData

    fun emitSensorData(data: SensorData) {
        _sensorData.tryEmit(data)
    }
}
