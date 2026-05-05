package com.alex.monitorsanatate.domain.usecase

import com.alex.monitorsanatate.domain.repository.ConnectionRepository
import javax.inject.Inject

class DisconnectUseCase @Inject constructor(
    private val connectionRepository: ConnectionRepository
) {
    suspend operator fun invoke() {
        connectionRepository.disconnect()
    }
}
