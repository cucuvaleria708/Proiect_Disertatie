package com.alex.monitorsanatate.ui.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alex.monitorsanatate.domain.model.ConnectionMethod
import com.alex.monitorsanatate.domain.model.ConnectionState
import com.alex.monitorsanatate.domain.model.DeviceInfo
import com.alex.monitorsanatate.domain.repository.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val connectionRepository: ConnectionRepository
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = connectionRepository.connectionState
    val discoveredDevices: StateFlow<List<DeviceInfo>> = connectionRepository.discoveredDevices

    private val _selectedMethod = MutableStateFlow(ConnectionMethod.WIFI)
    val selectedMethod: StateFlow<ConnectionMethod> = _selectedMethod

    private val _selectedTab = MutableStateFlow("test")
    val selectedTab: StateFlow<String> = _selectedTab

    private val _manualIp = MutableStateFlow("192.168.1.100")
    val manualIp: StateFlow<String> = _manualIp

    private val _manualPort = MutableStateFlow("81")
    val manualPort: StateFlow<String> = _manualPort

    // LED test state
    private val _ledState = MutableStateFlow(false)
    val ledState: StateFlow<Boolean> = _ledState

    private val _ledRequestStatus = MutableStateFlow("")
    val ledRequestStatus: StateFlow<String> = _ledRequestStatus

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    fun selectTab(tab: String) {
        _selectedTab.value = tab
    }

    fun selectMethod(method: ConnectionMethod) {
        _selectedMethod.value = method
    }

    fun updateManualIp(ip: String) {
        _manualIp.value = ip
    }

    fun updateManualPort(port: String) {
        _manualPort.value = port
    }

    fun startScan() {
        viewModelScope.launch {
            connectionRepository.startScan(_selectedMethod.value)
        }
    }

    fun stopScan() {
        viewModelScope.launch {
            connectionRepository.stopScan()
        }
    }

    fun connectToDevice(device: DeviceInfo) {
        viewModelScope.launch {
            connectionRepository.connect(device)
        }
    }

    fun connectManualWifi() {
        val ip = _manualIp.value.trim()
        val port = _manualPort.value.trim()
        if (ip.isNotEmpty()) {
            val device = DeviceInfo(
                name = "ESP32 ($ip)",
                address = "$ip:$port",
                method = ConnectionMethod.WIFI
            )
            viewModelScope.launch {
                connectionRepository.connect(device)
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            connectionRepository.disconnect()
        }
    }

    override fun onCleared() {
        super.onCleared()
        connectionRepository.cleanup()
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
    }

    fun sendLedCommand(turnOn: Boolean) {
        val ip = _manualIp.value.trim()
        val port = _manualPort.value.trim()
        if (ip.isEmpty()) {
            _ledRequestStatus.value = "Introdu IP-ul ESP32"
            return
        }
        val path = if (turnOn) "/H" else "/L"
        val url = "http://$ip:$port$path"

        _ledRequestStatus.value = "Se trimite..."

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        _ledState.value = turnOn
                        _ledRequestStatus.value = if (turnOn) "LED pornit" else "LED oprit"
                    } else {
                        _ledRequestStatus.value = "Eroare: ${response.code}"
                    }
                }
            } catch (e: Exception) {
                _ledRequestStatus.value = "Eroare: ${e.message}"
            }
        }
    }
}
