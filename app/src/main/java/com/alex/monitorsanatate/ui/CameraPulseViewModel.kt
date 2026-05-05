package com.alex.monitorsanatate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alex.monitorsanatate.data.local.datastore.SettingsDataStore
import com.alex.monitorsanatate.domain.model.ConnectionMethod
import com.alex.monitorsanatate.domain.model.Measurement
import com.alex.monitorsanatate.domain.usecase.SaveMeasurementUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraPulseViewModel @Inject constructor(
    private val saveMeasurementUseCase: SaveMeasurementUseCase,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

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

    fun saveMeasurement(bpms: List<Int>, startTime: Long) {
        if (bpms.isEmpty() || startTime == 0L) return
        viewModelScope.launch {
            saveMeasurementUseCase(
                Measurement(
                    startTime        = startTime,
                    endTime          = System.currentTimeMillis(),
                    averageBpm       = bpms.average().toInt(),
                    minBpm           = bpms.min(),
                    maxBpm           = bpms.max(),
                    measurementType  = "PULS",
                    connectionMethod = ConnectionMethod.CAMERA
                )
            )
        }
    }
}
