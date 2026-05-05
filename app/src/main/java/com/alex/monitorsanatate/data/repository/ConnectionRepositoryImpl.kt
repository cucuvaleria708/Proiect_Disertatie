package com.alex.monitorsanatate.data.repository

import com.alex.monitorsanatate.data.remote.ble.BleConnectionManager
import com.alex.monitorsanatate.data.remote.wifi.WifiConnectionManager
import com.alex.monitorsanatate.domain.model.ConnectionMethod
import com.alex.monitorsanatate.domain.model.ConnectionState
import com.alex.monitorsanatate.domain.model.DeviceInfo
import com.alex.monitorsanatate.domain.repository.ConnectionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionRepositoryImpl @Inject constructor(
    private val wifiConnectionManager: WifiConnectionManager,
    private val bleConnectionManager: BleConnectionManager,
    private val sensorRepositoryImpl: SensorRepositoryImpl
) : ConnectionRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _discoveredDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    override val discoveredDevices: StateFlow<List<DeviceInfo>> = _discoveredDevices

    private var activeMethod: ConnectionMethod = ConnectionMethod.BLE

    init {
        // Forward WiFi state and data
        scope.launch {
            wifiConnectionManager.connectionState.collect { state ->
                if (activeMethod == ConnectionMethod.WIFI) _connectionState.value = state
            }
        }
        scope.launch {
            wifiConnectionManager.sensorData.collect { data ->
                if (activeMethod == ConnectionMethod.WIFI) sensorRepositoryImpl.emitSensorData(data)
            }
        }
        scope.launch {
            wifiConnectionManager.discoveredDevices.collect { devices ->
                if (activeMethod == ConnectionMethod.WIFI) _discoveredDevices.value = devices
            }
        }

        // Forward BLE state and data
        scope.launch {
            bleConnectionManager.connectionState.collect { state ->
                if (activeMethod == ConnectionMethod.BLE) _connectionState.value = state
            }
        }
        scope.launch {
            bleConnectionManager.sensorData.collect { data ->
                if (activeMethod == ConnectionMethod.BLE) sensorRepositoryImpl.emitSensorData(data)
            }
        }
        scope.launch {
            bleConnectionManager.discoveredDevices.collect { devices ->
                if (activeMethod == ConnectionMethod.BLE) _discoveredDevices.value = devices
            }
        }
    }

    override suspend fun startScan(method: ConnectionMethod) {
        activeMethod = method
        when (method) {
            ConnectionMethod.WIFI    -> wifiConnectionManager.startScan()
            ConnectionMethod.BLE     -> bleConnectionManager.startScan()
            ConnectionMethod.CAMERA,
            ConnectionMethod.GALLERY -> Unit
        }
    }

    override suspend fun stopScan() {
        when (activeMethod) {
            ConnectionMethod.WIFI    -> wifiConnectionManager.stopScan()
            ConnectionMethod.BLE     -> bleConnectionManager.stopScan()
            ConnectionMethod.CAMERA,
            ConnectionMethod.GALLERY -> Unit
        }
    }

    override suspend fun connect(device: DeviceInfo) {
        activeMethod = device.method
        when (device.method) {
            ConnectionMethod.WIFI    -> wifiConnectionManager.connect(device)
            ConnectionMethod.BLE     -> bleConnectionManager.connect(device)
            ConnectionMethod.CAMERA,
            ConnectionMethod.GALLERY -> Unit
        }
    }

    override suspend fun disconnect() {
        when (activeMethod) {
            ConnectionMethod.WIFI    -> wifiConnectionManager.disconnect()
            ConnectionMethod.BLE     -> bleConnectionManager.disconnect()
            ConnectionMethod.CAMERA,
            ConnectionMethod.GALLERY -> Unit
        }
    }

    override fun sendCommand(command: String) {
        when (activeMethod) {
            ConnectionMethod.WIFI    -> wifiConnectionManager.sendCommand(command)
            ConnectionMethod.BLE     -> bleConnectionManager.sendCommand(command)
            ConnectionMethod.CAMERA,
            ConnectionMethod.GALLERY -> Unit
        }
    }

    override fun cleanup() {
        scope.cancel()
        wifiConnectionManager.cleanup()
        bleConnectionManager.cleanup()
    }
}
