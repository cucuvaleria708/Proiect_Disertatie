package com.alex.monitorsanatate.data.remote

import com.alex.monitorsanatate.domain.model.ConnectionState
import com.alex.monitorsanatate.domain.model.DeviceInfo
import com.alex.monitorsanatate.domain.model.SensorData
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ConnectionManager {
    val sensorData: SharedFlow<SensorData>
    val connectionState: StateFlow<ConnectionState>
    val discoveredDevices: StateFlow<List<DeviceInfo>>

    suspend fun startScan()
    suspend fun stopScan()
    suspend fun connect(device: DeviceInfo)
    suspend fun disconnect()
    fun sendCommand(command: String)
}
