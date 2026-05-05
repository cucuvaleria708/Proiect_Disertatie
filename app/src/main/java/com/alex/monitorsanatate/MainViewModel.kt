package com.alex.monitorsanatate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alex.monitorsanatate.data.repository.AuthRepository
import com.alex.monitorsanatate.data.repository.DeepLinkResult
import com.alex.monitorsanatate.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val startDestination: StateFlow<String?> = MutableStateFlow(Screen.Splash.route)

    // Emite ruta la care NavGraph trebuie să navigheze după un deep link
    private val _navigationEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()

    fun processDeepLink(uri: String) {
        viewModelScope.launch {
            when (authRepository.handleDeepLink(uri)) {
                is DeepLinkResult.PasswordRecovery -> {
                    _navigationEvent.emit(Screen.ResetPassword.route)
                }
                else -> { /* confirmare email — sesiunea se actualizează prin observeSessionStatus */ }
            }
        }
    }
}
