package com.alex.monitorsanatate.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alex.monitorsanatate.data.local.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashDestination {
    object Loading : SplashDestination()
    object Main : SplashDestination()
    object Login : SplashDestination()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    init {
        viewModelScope.launch {
            val token = settingsDataStore.authToken.first()
            val name = settingsDataStore.userName.first()
            _userName.value = name ?: ""
            _isLoggedIn.value = token != null

            delay(2500)

            _destination.value = if (token != null) {
                SplashDestination.Main
            } else {
                SplashDestination.Login
            }
        }
    }
}
