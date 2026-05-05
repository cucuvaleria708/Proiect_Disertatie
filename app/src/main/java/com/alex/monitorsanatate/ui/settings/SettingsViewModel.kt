package com.alex.monitorsanatate.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alex.monitorsanatate.data.local.datastore.SettingsDataStore
import com.alex.monitorsanatate.data.remote.ble.BleConnectionManager
import com.alex.monitorsanatate.data.repository.AuthRepository
import com.alex.monitorsanatate.data.repository.MedicalProfileRepository
import com.alex.monitorsanatate.domain.model.ConnectionMethod
import com.alex.monitorsanatate.domain.model.ConnectionState
import com.alex.monitorsanatate.domain.model.DeviceInfo
import com.alex.monitorsanatate.domain.repository.ConnectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val authRepository: AuthRepository,
    private val bleConnectionManager: BleConnectionManager,
    private val connectionRepository: ConnectionRepository,
    private val medicalProfileRepository: MedicalProfileRepository
) : ViewModel() {

    val userName: StateFlow<String> = settingsDataStore.userName
        .map { it ?: "Utilizator" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Utilizator")

    val userEmail: StateFlow<String> = settingsDataStore.userEmail
        .map { it ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    private val currentUserId: StateFlow<String?> = settingsDataStore.userId
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ── Profil medical — citit din Room DB ────────────────────────────────────

    val userGender: StateFlow<String> = currentUserId.flatMapLatest { uid ->
        if (uid.isNullOrEmpty()) flowOf("M")
        else medicalProfileRepository.getProfile(uid).map { it?.gender ?: "M" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "M")

    val userAge: StateFlow<Int> = currentUserId.flatMapLatest { uid ->
        if (uid.isNullOrEmpty()) flowOf(0)
        else medicalProfileRepository.getProfile(uid).map { it?.age ?: 0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val userWeight: StateFlow<Float> = currentUserId.flatMapLatest { uid ->
        if (uid.isNullOrEmpty()) flowOf(0f)
        else medicalProfileRepository.getProfile(uid).map { it?.weight ?: 0f }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    // Praguri calculate din profilul din DB (folosite de alerte)
    val computedBpmHigh: StateFlow<Int> = userAge.map { age ->
        bpmHighThreshold(age.takeIf { it > 0 } ?: 30)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)

    val computedBpmLow: StateFlow<Int> = userAge.map { age ->
        bpmLowThreshold(age.takeIf { it > 0 } ?: 30)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 60)

    // ── Scriere profil în Room DB ─────────────────────────────────────────────

    fun saveProfile(gender: String, age: Int, weight: Float) {
        val uid = currentUserId.value ?: return
        viewModelScope.launch {
            medicalProfileRepository.saveProfile(
                userId = uid,
                gender = gender,
                age    = age,
                weight = weight
            )
        }
    }

    // Păstrate pentru compatibilitate cu SettingsScreen (apelate din butonul Salvează)
    fun setGender(gender: String) = saveProfile(gender, userAge.value, userWeight.value)
    fun setAge(age: Int)          = saveProfile(userGender.value, age, userWeight.value)
    fun setWeight(weight: Float)  = saveProfile(userGender.value, userAge.value, weight)

    // ── Conexiune senzor Puls (BLE direct) ───────────────────────────────────
    val pulseConnectionState: StateFlow<ConnectionState> = bleConnectionManager.connectionState
    val pulseDevices: StateFlow<List<DeviceInfo>>        = bleConnectionManager.discoveredDevices

    fun startPulseScan()                       { viewModelScope.launch { bleConnectionManager.startScan() } }
    fun stopPulseScan()                        { viewModelScope.launch { bleConnectionManager.stopScan() } }
    fun connectPulseDevice(device: DeviceInfo) { viewModelScope.launch { bleConnectionManager.connect(device) } }
    fun disconnectPulse()                      { viewModelScope.launch { bleConnectionManager.disconnect() } }

    // ── Conexiune senzor EKG (BLE via ConnectionRepository) ──────────────────
    val ekgConnectionState: StateFlow<ConnectionState> = connectionRepository.connectionState
    val ekgDevices: StateFlow<List<DeviceInfo>>        = connectionRepository.discoveredDevices

    fun startEkgScan()                        { viewModelScope.launch { connectionRepository.startScan(ConnectionMethod.BLE) } }
    fun stopEkgScan()                         { viewModelScope.launch { connectionRepository.stopScan() } }
    fun connectEkgDevice(device: DeviceInfo)  { viewModelScope.launch { connectionRepository.connect(device) } }
    fun disconnectEkg()                       { viewModelScope.launch { connectionRepository.disconnect() } }

    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent: SharedFlow<Unit> = _logoutEvent

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _logoutEvent.emit(Unit)
        }
    }
}

// ── Formule medicale BPM (ESC / AHA guidelines) ───────────────────────────────

fun bpmLowThreshold(age: Int): Int = when {
    age < 1  -> 100
    age < 3  ->  90
    age < 6  ->  80
    age < 12 ->  70
    else     ->  60
}

fun bpmHighThreshold(age: Int): Int = when {
    age < 1  -> 160
    age < 3  -> 150
    age < 6  -> 140
    age < 12 -> 120
    age < 18 -> 110
    else     -> 100
}
