package com.alex.monitorsanatate.data.remote.ble

import android.annotation.SuppressLint
import kotlinx.coroutines.cancel
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.content.Context
import com.alex.monitorsanatate.data.remote.ConnectionManager
import com.alex.monitorsanatate.domain.model.ConnectionMethod
import com.alex.monitorsanatate.domain.model.ConnectionState
import com.alex.monitorsanatate.domain.model.DeviceInfo
import com.alex.monitorsanatate.domain.model.SensorData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context
) : ConnectionManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private var bluetoothGatt: BluetoothGatt? = null
    private val gattOperationQueue = Channel<GattOperation>(Channel.UNLIMITED)

    private val _sensorData = MutableSharedFlow<SensorData>(replay = 1, extraBufferCapacity = 64)
    override val sensorData: SharedFlow<SensorData> = _sensorData

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _discoveredDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    override val discoveredDevices: StateFlow<List<DeviceInfo>> = _discoveredDevices

    private val foundDevices = mutableMapOf<String, DeviceInfo>()
    private var currentBpm = 0
    private var lastFinalBpm = 0
    private var lastStatus = "asteptare"
    private var lastTimeRemaining = 0
    private var lastSemnalValid = false
    private var lastSignalRange = 0

    init {
        // Process GATT operations sequentially with delay so each write
        // completes before the next one starts (BT stack requires this)
        scope.launch {
            for (operation in gattOperationQueue) {
                operation.execute()
                delay(300)
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: "Unknown Device"
            val address = device.address
            val rssi = result.rssi

            val deviceInfo = DeviceInfo(
                name = name,
                address = address,
                method = ConnectionMethod.BLE,
                rssi = rssi
            )
            foundDevices[address] = deviceInfo
            _discoveredDevices.value = foundDevices.values.toList()
        }

        override fun onScanFailed(errorCode: Int) {
            _connectionState.value = ConnectionState.Error("Scanare BLE eșuată: $errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = ConnectionState.Connected(
                        DeviceInfo(
                            name = gatt.device.name ?: "ESP32",
                            address = gatt.device.address,
                            method = ConnectionMethod.BLE
                        )
                    )
                    // Negociem MTU inainte de service discovery:
                    // pachetul ECG = 20 samples x 2 bytes = 40 bytes payload
                    // MTU implicit (23) permite doar 20 bytes → truncheaza silentios.
                    // Cu MTU=100: payload=97 bytes → suficient pentru 40 bytes ECG.
                    gatt.requestMtu(100)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = ConnectionState.Disconnected
                    // Only close if we haven't already closed in disconnect()
                    if (bluetoothGatt != null) {
                        gatt.close()
                        bluetoothGatt = null
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            // MTU negociat — acum porneste service discovery cu payload-ul corect.
            gatt.discoverServices()
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return

            val service = gatt.getService(BleConstants.HEALTH_SERVICE_UUID)

            if (service != null) {
                // Expected service found — subscribe to known characteristics
                val charsToNotify = listOfNotNull(
                    service.getCharacteristic(BleConstants.BPM_CHARACTERISTIC_UUID),
                    service.getCharacteristic(BleConstants.ECG_CHARACTERISTIC_UUID)
                )
                charsToNotify.forEach { char ->
                    scope.launch {
                        gattOperationQueue.send(GattOperation {
                            enableNotification(gatt, char)
                        })
                    }
                }
            } else {
                // Firmware doesn't advertise our UUID — subscribe to ALL notifiable
                // characteristics found so data still flows (UUID mismatch in firmware)
                gatt.services.flatMap { it.characteristics }
                    .filter { char ->
                        (char.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                    }
                    .forEach { char ->
                        scope.launch {
                            gattOperationQueue.send(GattOperation {
                                enableNotification(gatt, char)
                            })
                        }
                    }
            }
        }

        @SuppressLint("MissingPermission")
        private fun enableNotification(gatt: BluetoothGatt, char: BluetoothGattCharacteristic) {
            gatt.setCharacteristicNotification(char, true)
            val descriptor = char.getDescriptor(BleConstants.CCCD_UUID) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
        }

        // Android < 13 calls the deprecated 2-param version; forward to the new one
        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            onCharacteristicChanged(gatt, characteristic, characteristic.value ?: return)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            when (characteristic.uuid) {
                BleConstants.ECG_CHARACTERISTIC_UUID -> parseEcg(value)
                // BPM char or any unknown char from fallback subscription
                else -> parseBpm(value)
            }
        }

        private fun parseBpm(value: ByteArray) {
            if (value.size >= 9) {
                // Format extins: currentBpm(2) + finalBpm(2) + status(1) +
                //                timeRemaining(1) + semnalValid(1) + signalRange(2)
                val buf = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)
                currentBpm        = buf.short.toInt() and 0xFFFF
                lastFinalBpm      = buf.short.toInt() and 0xFFFF
                val statusCode    = buf.get().toInt() and 0xFF
                lastTimeRemaining = buf.get().toInt() and 0xFF
                lastSemnalValid   = buf.get().toInt() != 0
                lastSignalRange   = buf.short.toInt() and 0xFFFF
                lastStatus = when (statusCode) {
                    1    -> "masurare"
                    2    -> "finalizat"
                    else -> "asteptare"
                }
                _sensorData.tryEmit(SensorData(
                    bpm           = currentBpm,
                    ecgPoints     = emptyList(),
                    finalBpm      = lastFinalBpm,
                    status        = lastStatus,
                    timeRemaining = lastTimeRemaining,
                    semnalValid   = lastSemnalValid,
                    signalRange   = lastSignalRange
                ))
            } else if (value.size >= 2) {
                currentBpm = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)
                    .short.toInt() and 0xFFFF
                _sensorData.tryEmit(SensorData(bpm = currentBpm, ecgPoints = emptyList()))
            } else if (value.size == 1) {
                // Single-byte BPM (some simple firmware variants)
                currentBpm = value[0].toInt() and 0xFF
                _sensorData.tryEmit(SensorData(bpm = currentBpm, ecgPoints = emptyList()))
            }
        }

        private fun parseEcg(value: ByteArray) {
            val ecgPoints = mutableListOf<Float>()
            val buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)
            while (buffer.remaining() >= 2) {
                ecgPoints.add(buffer.short.toFloat())
            }
            if (ecgPoints.isNotEmpty()) {
                _sensorData.tryEmit(SensorData(
                    bpm           = currentBpm,
                    ecgPoints     = ecgPoints,
                    finalBpm      = lastFinalBpm,
                    status        = lastStatus,
                    timeRemaining = lastTimeRemaining,
                    semnalValid   = lastSemnalValid,
                    signalRange   = lastSignalRange
                ))
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun startScan() {
        _connectionState.value = ConnectionState.Scanning
        foundDevices.clear()
        _discoveredDevices.value = emptyList()

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _connectionState.value = ConnectionState.Error("Activează Bluetooth și încearcă din nou")
            return
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner ?: run {
            _connectionState.value = ConnectionState.Error("BLE nu este disponibil")
            return
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            // No service-UUID filter: ESP32 firmware may not advertise the UUID
            // in the advertisement packet even though the service exists after connect.
            scanner.startScan(null, settings, scanCallback)
        } catch (e: SecurityException) {
            _connectionState.value = ConnectionState.Error("Permisiuni BLE refuzate")
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error("Eroare scanare: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun stopScan() {
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        if (_connectionState.value is ConnectionState.Scanning) {
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(device: DeviceInfo) {
        stopScan()
        _connectionState.value = ConnectionState.Connecting

        val bluetoothDevice: BluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
            ?: run {
                _connectionState.value = ConnectionState.Error("Dispozitiv negăsit")
                return
            }

        bluetoothGatt = bluetoothDevice.connectGatt(
            context,
            false,
            gattCallback,
            BluetoothDevice.TRANSPORT_LE
        )
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect() {
        val gatt = bluetoothGatt
        bluetoothGatt = null  // clear first so onConnectionStateChange won't close again
        gatt?.disconnect()
        gatt?.close()
        _connectionState.value = ConnectionState.Disconnected
    }

    @SuppressLint("MissingPermission")
    fun cleanup() {
        try { bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback) } catch (_: Exception) {}
        val gatt = bluetoothGatt
        bluetoothGatt = null
        try { gatt?.disconnect(); gatt?.close() } catch (_: Exception) {}
        _connectionState.value = ConnectionState.Disconnected
        gattOperationQueue.close()
        scope.cancel()
    }

    @SuppressLint("MissingPermission")
    override fun sendCommand(command: String) {
        val gatt = bluetoothGatt ?: return
        val service = gatt.getService(BleConstants.HEALTH_SERVICE_UUID) ?: return
        val char = service.getCharacteristic(BleConstants.COMMAND_CHARACTERISTIC_UUID) ?: return
        val bytes = command.toByteArray()

        scope.launch {
            gattOperationQueue.send(GattOperation {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeCharacteristic(char, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                } else {
                    @Suppress("DEPRECATION")
                    char.value = bytes
                    @Suppress("DEPRECATION")
                    gatt.writeCharacteristic(char)
                }
            })
        }
    }

    private class GattOperation(private val action: () -> Unit) {
        fun execute() = action()
    }
}
