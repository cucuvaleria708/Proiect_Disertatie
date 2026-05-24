package com.alex.monitorsanatate.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alex.monitorsanatate.data.local.datastore.SettingsDataStore
import com.alex.monitorsanatate.domain.model.Measurement
import com.alex.monitorsanatate.domain.repository.MeasurementRepository
import com.alex.monitorsanatate.domain.usecase.GetMeasurementHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getMeasurementHistoryUseCase: GetMeasurementHistoryUseCase,
    private val measurementRepository: MeasurementRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val userName: StateFlow<String> = settingsDataStore.userName
        .map { it ?: "Utilizator" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Utilizator")

    val userEmail: StateFlow<String> = settingsDataStore.userEmail
        .map { it ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val currentUserId: StateFlow<String?> = settingsDataStore.userId
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val userGender: StateFlow<String> = currentUserId.flatMapLatest { uid ->
        if (uid.isNullOrEmpty()) flowOf("M") else settingsDataStore.getUserGender(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "M")

    val userAge: StateFlow<Int> = currentUserId.flatMapLatest { uid ->
        if (uid.isNullOrEmpty()) flowOf(0) else settingsDataStore.getUserAge(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val userWeight: StateFlow<Float> = currentUserId.flatMapLatest { uid ->
        if (uid.isNullOrEmpty()) flowOf(0f) else settingsDataStore.getUserWeight(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    private val _currentFilter = MutableStateFlow("Toate")
    val currentFilter: StateFlow<String> = _currentFilter

    val measurements: StateFlow<List<Measurement>> = getMeasurementHistoryUseCase()
        .combine(_currentFilter) { list, filter ->
            when (filter) {
                "Ritm Cardiac" -> list.filter { it.measurementType != "ECG" && it.measurementType != "EKG" && it.measurementType != "AI_ECG" }
                "Traseu ECG"  -> list.filter { it.measurementType == "ECG" || it.measurementType == "EKG" }
                "Predicție"   -> list.filter { it.measurementType == "AI_ECG" }
                else   -> list
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ECG waveform al celei mai recente înregistrări ECG (încărcat când filtrul este "ECG")
    val latestEcgData: StateFlow<List<Float>> = measurements
        .combine(_currentFilter) { list, filter ->
            if (filter == "Traseu ECG") list.maxByOrNull { it.startTime }?.id else null
        }
        .distinctUntilChanged()
        .flatMapLatest { id ->
            if (id != null) {
                flow { emit(getMeasurementHistoryUseCase.getEcgData(id)) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(filter: String) {
        _currentFilter.value = filter
    }

    /** Șterge o singură înregistrare după ID. */
    fun deleteMeasurement(id: Long) {
        viewModelScope.launch {
            measurementRepository.deleteMeasurement(id)
        }
    }

    /**
     * Șterge toate înregistrările vizibile curent (conform filtrului activ).
     * Dacă filtrul e "Toate" → șterge tot; dacă e "Puls" → șterge doar Puls; etc.
     */
    fun deleteAllVisible() {
        viewModelScope.launch {
            measurements.value.forEach { m ->
                measurementRepository.deleteMeasurement(m.id)
            }
        }
    }
}

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMeasurementHistoryUseCase: GetMeasurementHistoryUseCase
) : ViewModel() {

    private val measurementId: Long = savedStateHandle["measurementId"] ?: 0L

    private val _measurement = MutableStateFlow<Measurement?>(null)
    val measurement: StateFlow<Measurement?> = _measurement

    private val _ecgData = MutableStateFlow<List<Float>>(emptyList())
    val ecgData: StateFlow<List<Float>> = _ecgData

    init {
        viewModelScope.launch {
            _measurement.value = getMeasurementHistoryUseCase.getById(measurementId)
            _ecgData.value = getMeasurementHistoryUseCase.getEcgData(measurementId)
        }
    }
}
