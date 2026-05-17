package com.alex.monitorsanatate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alex.monitorsanatate.data.local.datastore.SettingsDataStore
import com.alex.monitorsanatate.domain.model.ConnectionMethod
import com.alex.monitorsanatate.domain.model.ConnectionState
import com.alex.monitorsanatate.domain.model.DeviceInfo
import com.alex.monitorsanatate.domain.model.Measurement
import com.alex.monitorsanatate.domain.model.SensorData
import com.alex.monitorsanatate.domain.repository.ConnectionRepository
import com.alex.monitorsanatate.domain.repository.SensorRepository
import com.alex.monitorsanatate.domain.usecase.SaveMeasurementUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SensorPulseViewModel @Inject constructor(
    private val connectionRepository: ConnectionRepository,
    private val sensorRepository: SensorRepository,
    private val saveMeasurementUseCase: SaveMeasurementUseCase,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = connectionRepository.connectionState
    val discoveredDevices: StateFlow<List<DeviceInfo>> = connectionRepository.discoveredDevices
    val sensorData: SharedFlow<SensorData> = sensorRepository.sensorData

    // ── Buffer ECG live ───────────────────────────────────────────────────────
    // Fereastra: 750 esantioane = 1.5 secunde la 500Hz (1-2 batai cardiace vizibile).
    private val ECG_WINDOW = 750
    private val ecgAccumulator = ArrayDeque<Float>(ECG_WINDOW + 50)
    private val _ecgBuffer = MutableStateFlow(FloatArray(0))
    val ecgBuffer: StateFlow<FloatArray> = _ecgBuffer

    // ── Profil medical al utilizatorului curent ──────────────────────────────
    private val currentUserId: StateFlow<String?> = settingsDataStore.userId
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val userGender: StateFlow<String> = currentUserId.flatMapLatest { uid ->
        if (uid.isNullOrEmpty()) flowOf("M") else settingsDataStore.getUserGender(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "M")

    val userAge: StateFlow<Int> = currentUserId.flatMapLatest { uid ->
        if (uid.isNullOrEmpty()) flowOf(0) else settingsDataStore.getUserAge(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val userWeight: StateFlow<Float> = currentUserId.flatMapLatest { uid ->
        if (uid.isNullOrEmpty()) flowOf(0f) else settingsDataStore.getUserWeight(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    init {
        // Acumuleaza esantioanele ECG din fiecare notificare BLE
        viewModelScope.launch {
            sensorRepository.sensorData.collect { data ->
                if (data.ecgPoints.isNotEmpty()) {
                    for (pt in data.ecgPoints) {
                        if (ecgAccumulator.size >= ECG_WINDOW) ecgAccumulator.removeFirst()
                        ecgAccumulator.addLast(pt)
                    }
                    _ecgBuffer.value = ecgAccumulator.toFloatArray()
                }
            }
        }
    }

    // ── Conexiune BLE ────────────────────────────────────────────────────────
    fun startScan() { viewModelScope.launch { connectionRepository.startScan(ConnectionMethod.BLE) } }
    fun stopScan()  { viewModelScope.launch { connectionRepository.stopScan() } }
    fun connect(device: DeviceInfo) { viewModelScope.launch { connectionRepository.connect(device) } }

    fun disconnect() {
        clearEcgBuffer()
        viewModelScope.launch { connectionRepository.disconnect() }
    }

    fun sendStartCommand() { connectionRepository.sendCommand("START") }
    fun sendResetCommand() { connectionRepository.sendCommand("RESET") }

    private fun clearEcgBuffer() {
        ecgAccumulator.clear()
        _ecgBuffer.value = FloatArray(0)
    }

    override fun onCleared() {
        super.onCleared()
        // ConnectionRepository e @Singleton — conexiunea persista dupa navigare
    }

    fun saveMeasurement(finalBpm: Int, startTime: Long) {
        if (finalBpm <= 0 || startTime == 0L) return
        viewModelScope.launch {
            saveMeasurementUseCase(
                Measurement(
                    startTime        = startTime,
                    endTime          = System.currentTimeMillis(),
                    averageBpm       = finalBpm,
                    minBpm           = finalBpm,
                    maxBpm           = finalBpm,
                    measurementType  = "PULS",
                    connectionMethod = ConnectionMethod.BLE
                )
            )
        }
    }
}
