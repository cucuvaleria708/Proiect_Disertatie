package com.alex.monitorsanatate.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alex.monitorsanatate.data.local.datastore.SettingsDataStore
import com.alex.monitorsanatate.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle    : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String)   : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val serverIp: StateFlow<String> = settingsDataStore.esp32Ip
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "192.168.1.100")

    // ── Login ─────────────────────────────────────────────────────────────────

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.login(email, password)
            _authState.value = if (result.isSuccess) {
                AuthState.Success("Autentificare reusita!")
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Eroare necunoscuta")
            }
        }
    }

    // ── Înregistrare ──────────────────────────────────────────────────────────

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.register(email, password, username)
            _authState.value = if (result.isSuccess) {
                AuthState.Success("Cont creat cu succes! Verifica email-ul pentru confirmare.")
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Eroare necunoscuta")
            }
        }
    }

    // ── Parola uitată: pasul 1 — trimite email ────────────────────────────────

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.resetPasswordForEmail(email)
            _authState.value = if (result.isSuccess) {
                AuthState.Success("Email trimis! Verifică inbox-ul pentru link-ul de resetare.")
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Eroare la trimiterea emailului")
            }
        }
    }

    // ── Parola uitată: pasul 2 — setează parola nouă ──────────────────────────

    fun updatePassword(newPassword: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.updatePassword(newPassword)
            _authState.value = if (result.isSuccess) {
                AuthState.Success("Parola a fost schimbată cu succes!")
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Eroare la schimbarea parolei")
            }
        }
    }

    // ── Profil medical în așteptare (înregistrare pasul 2) ────────────────────

    fun savePendingProfile(email: String, gender: String, age: Int, weight: Float) {
        viewModelScope.launch {
            settingsDataStore.savePendingProfile(email, gender, age, weight)
        }
    }

    fun saveServerIp(ip: String) {
        viewModelScope.launch { settingsDataStore.setEsp32Ip(ip.trim()) }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}
