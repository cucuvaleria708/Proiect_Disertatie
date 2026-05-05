package com.alex.monitorsanatate.domain.usecase

import com.alex.monitorsanatate.domain.model.DeviceInfo
import com.alex.monitorsanatate.domain.repository.ConnectionRepository
import javax.inject.Inject

class ConnectToDeviceUseCase @Inject constructor(
    private val connectionRepository: ConnectionRepository
) {
    suspend operator fun invoke(device: DeviceInfo) {
        connectionRepository.connect(device)
    }
}
