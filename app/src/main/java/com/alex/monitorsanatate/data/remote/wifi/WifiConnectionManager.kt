package com.alex.monitorsanatate.data.remote.wifi

import com.alex.monitorsanatate.data.remote.ConnectionManager
import com.alex.monitorsanatate.domain.model.ConnectionMethod
import com.alex.monitorsanatate.domain.model.ConnectionState
import com.alex.monitorsanatate.domain.model.DeviceInfo
import com.alex.monitorsanatate.domain.model.SensorData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiConnectionManager @Inject constructor(
    private val webSocketClient: WebSocketClient
) : ConnectionManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val sensorData: SharedFlow<SensorData> = webSocketClient.sensorData

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _discoveredDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    override val discoveredDevices: StateFlow<List<DeviceInfo>> = _discoveredDevices

    init {
        scope.launch {
            webSocketClient.isConnected.collect { connected ->
                if (connected) {
                    val currentDevice = (_connectionState.value as? ConnectionState.Connecting)
                    _connectionState.value = if (currentDevice != null) {
                        ConnectionState.Connected(
                            DeviceInfo(
                                name = "ESP32",
                                address = "WiFi",
                                method = ConnectionMethod.WIFI
                            )
                        )
                    } else {
                        ConnectionState.Disconnected
                    }
                } else if (_connectionState.value is ConnectionState.Connected ||
                           _connectionState.value is ConnectionState.Connecting) {
                    // Also handle failure during connection attempt (Connecting → Disconnected)
                    _connectionState.value = ConnectionState.Disconnected
                }
            }
        }
    }

    override suspend fun startScan() {
        _connectionState.value = ConnectionState.Scanning
        // For WiFi, we don't "scan" - user enters IP manually
        // But we can provide a default entry
        _discoveredDevices.value = listOf(
            DeviceInfo(
                name = "ESP32 (Manual IP)",
                address = "192.168.1.100:81",
                method = ConnectionMethod.WIFI
            )
        )
    }

    override suspend fun stopScan() {
        if (_connectionState.value is ConnectionState.Scanning) {
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    override suspend fun connect(device: DeviceInfo) {
        _connectionState.value = ConnectionState.Connecting
        val parts = device.address.split(":")
        val ip = parts[0]
        val port = parts.getOrNull(1)?.toIntOrNull() ?: 81
        webSocketClient.connect(ip, port)
    }

    override suspend fun disconnect() {
        webSocketClient.disconnect()
        _connectionState.value = ConnectionState.Disconnected
    }

    override fun sendCommand(command: String) {
        webSocketClient.sendCommand(command)
    }

    fun cleanup() {
        scope.cancel()
        webSocketClient.cleanup()
    }
}
