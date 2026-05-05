package com.alex.monitorsanatate.domain.model

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Scanning : ConnectionState()
    data object Connecting : ConnectionState()
    data class Connected(val device: DeviceInfo) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
