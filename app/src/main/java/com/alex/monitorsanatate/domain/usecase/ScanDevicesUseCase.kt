package com.alex.monitorsanatate.domain.usecase

import com.alex.monitorsanatate.domain.model.ConnectionMethod
import com.alex.monitorsanatate.domain.model.DeviceInfo
import com.alex.monitorsanatate.domain.repository.ConnectionRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ScanDevicesUseCase @Inject constructor(
    private val connectionRepository: ConnectionRepository
) {
    val discoveredDevices: StateFlow<List<DeviceInfo>>
        get() = connectionRepository.discoveredDevices

    suspend fun startScan(method: ConnectionMethod) {
        connectionRepository.startScan(method)
    }

    suspend fun stopScan() {
        connectionRepository.stopScan()
    }
}
