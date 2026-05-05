package com.alex.monitorsanatate.domain.repository

import com.alex.monitorsanatate.domain.model.ConnectionMethod
import com.alex.monitorsanatate.domain.model.ConnectionState
import com.alex.monitorsanatate.domain.model.DeviceInfo
import kotlinx.coroutines.flow.StateFlow

interface ConnectionRepository {
    val connectionState: StateFlow<ConnectionState>
    val discoveredDevices: StateFlow<List<DeviceInfo>>

    suspend fun startScan(method: ConnectionMethod)
    suspend fun stopScan()
    suspend fun connect(device: DeviceInfo)
    suspend fun disconnect()
    fun sendCommand(command: String)
    fun cleanup()
}
