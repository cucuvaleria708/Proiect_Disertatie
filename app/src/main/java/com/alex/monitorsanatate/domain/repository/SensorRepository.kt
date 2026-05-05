package com.alex.monitorsanatate.domain.repository

import com.alex.monitorsanatate.domain.model.SensorData
import kotlinx.coroutines.flow.SharedFlow

interface SensorRepository {
    val sensorData: SharedFlow<SensorData>
}
