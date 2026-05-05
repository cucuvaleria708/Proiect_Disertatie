package com.alex.monitorsanatate.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alex.monitorsanatate.domain.model.ConnectionMethod
import com.alex.monitorsanatate.domain.model.ConnectionState
import com.alex.monitorsanatate.domain.model.Measurement
import com.alex.monitorsanatate.domain.repository.ConnectionRepository
import com.alex.monitorsanatate.domain.repository.SensorRepository
import com.alex.monitorsanatate.domain.usecase.SaveMeasurementUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sensorRepository: SensorRepository,
    private val connectionRepository: ConnectionRepository,
    private val saveMeasurementUseCase: SaveMeasurementUseCase
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = connectionRepository.connectionState

    private val _currentBpm = MutableStateFlow(0)
    val currentBpm: StateFlow<Int> = _currentBpm

    private val _ecgPoints = MutableStateFlow<List<Float>>(emptyList())
    val ecgPoints: StateFlow<List<Float>> = _ecgPoints

    private val ecgBuffer = mutableListOf<Float>()
    private val maxBufferSize = 1000

    private var sessionStartTime: Long = 0
    private val sessionBpmValues = mutableListOf<Int>()
    private var wasConnected = false

    init {
        viewModelScope.launch {
            sensorRepository.sensorData.collect { data ->
                _currentBpm.value = data.bpm
                if (data.bpm > 0 && wasConnected) {
                    sessionBpmValues.add(data.bpm)
                }
                ecgBuffer.addAll(data.ecgPoints)
                if (ecgBuffer.size > maxBufferSize) {
                    ecgBuffer.subList(0, ecgBuffer.size - maxBufferSize).clear()
                }
                _ecgPoints.value = ecgBuffer.toList()
            }
        }

        viewModelScope.launch {
            connectionRepository.connectionState.collect { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        if (!wasConnected) {
                            sessionStartTime = System.currentTimeMillis()
                            sessionBpmValues.clear()
                            wasConnected = true
                        }
                    }
                    else -> {
                        if (wasConnected) {
                            saveSessionIfNeeded()
                            wasConnected = false
                        }
                    }
                }
            }
        }
    }

    private fun saveSessionIfNeeded() {
        val validBpms = sessionBpmValues.filter { it > 0 }
        if (sessionStartTime > 0 && validBpms.size >= 5) {
            val avg = validBpms.average().toInt()
            viewModelScope.launch {
                saveMeasurementUseCase(
                    Measurement(
                        startTime = sessionStartTime,
                        endTime = System.currentTimeMillis(),
                        averageBpm = avg,
                        minBpm = validBpms.min(),
                        maxBpm = validBpms.max(),
                        measurementType = "EKG",
                        connectionMethod = ConnectionMethod.WIFI
                    )
                )
            }
        }
        sessionStartTime = 0
        sessionBpmValues.clear()
    }

    override fun onCleared() {
        super.onCleared()
        if (wasConnected) saveSessionIfNeeded()
    }
}
