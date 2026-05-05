package com.alex.monitorsanatate.ui.ecgdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alex.monitorsanatate.domain.model.ConnectionState
import com.alex.monitorsanatate.domain.repository.ConnectionRepository
import com.alex.monitorsanatate.domain.repository.SensorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EcgDetailViewModel @Inject constructor(
    private val sensorRepository: SensorRepository,
    private val connectionRepository: ConnectionRepository
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = connectionRepository.connectionState

    private val _ecgPoints = MutableStateFlow<List<Float>>(emptyList())
    val ecgPoints: StateFlow<List<Float>> = _ecgPoints

    private val _currentBpm = MutableStateFlow(0)
    val currentBpm: StateFlow<Int> = _currentBpm

    private val ecgBuffer = mutableListOf<Float>()
    private val maxBufferSize = 2500 // ~10 seconds at 250Hz

    init {
        viewModelScope.launch {
            sensorRepository.sensorData.collect { data ->
                _currentBpm.value = data.bpm

                ecgBuffer.addAll(data.ecgPoints)
                if (ecgBuffer.size > maxBufferSize) {
                    val excess = ecgBuffer.size - maxBufferSize
                    ecgBuffer.subList(0, excess).clear()
                }
                _ecgPoints.value = ecgBuffer.toList()
            }
        }
    }
}
