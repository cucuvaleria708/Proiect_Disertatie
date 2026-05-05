package com.alex.monitorsanatate.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alex.monitorsanatate.ui.components.GlassCard
import com.alex.monitorsanatate.ui.components.GlassInput
import com.alex.monitorsanatate.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ResetPasswordScreen(
    onPasswordReset: () -> Unit,
    onNavigateBack: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()

    var newPassword     by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showNew         by remember { mutableStateOf(false) }
    var showConfirm     by remember { mutableStateOf(false) }
    var localError      by remember { mutableStateOf("") }
    var successVisible  by remember { mutableStateOf(false) }

    val isLoading = authState is AuthState.Loading

    BackHandler(enabled = !isLoading) { onNavigateBack() }

    // La succes: arată animația, navighează după 1.5s
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            successVisible = true
            delay(1500)
            onPasswordReset()
        }
    }

    fun validate(): Boolean {
        return when {
            newPassword.length < 6 -> {
                localError = "Parola trebuie să aibă minim 6 caractere."
                false
            }
            newPassword != confirmPassword -> {
                localError = "Parolele nu coincid."
                false
            }
            else -> { localError = ""; true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.Center
    ) {
        // Buton back sus-stânga
        IconButton(
            onClick  = { if (!isLoading) onNavigateBack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Înapoi",
                tint               = TextSecondary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Header ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Ral5018Main, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector   = Icons.Filled.Lock,
                    contentDescription = null,
                    tint          = Color.White,
                    modifier      = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Parolă nouă",
                fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Introdu parola nouă pentru contul tău.",
                fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            GlassCard {
                // Parolă nouă
                GlassInput(
                    value         = newPassword,
                    onValueChange = { newPassword = it; localError = "" },
                    label         = "Parolă nouă",
                    placeholder   = "Minim 6 caractere",
                    leadingIcon   = {
                        Icon(Icons.Filled.Lock, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                    },
                    trailingIcon  = {
                        IconButton(onClick = { showNew = !showNew }) {
                            Icon(
                                imageVector = if (showNew) Icons.Outlined.VisibilityOff
                                              else Icons.Outlined.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    visualTransformation = if (showNew) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Spacer(Modifier.height(16.dp))

                // Confirmare parolă
                GlassInput(
                    value         = confirmPassword,
                    onValueChange = { confirmPassword = it; localError = "" },
                    label         = "Confirmă parola",
                    placeholder   = "Repetă parola nouă",
                    leadingIcon   = {
                        Icon(Icons.Filled.Lock, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                    },
                    trailingIcon  = {
                        IconButton(onClick = { showConfirm = !showConfirm }) {
                            Icon(
                                imageVector = if (showConfirm) Icons.Outlined.VisibilityOff
                                              else Icons.Outlined.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    visualTransformation = if (showConfirm) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Spacer(Modifier.height(20.dp))

                // Erori (validare locală sau de la server)
                val errorMessage = localError.ifEmpty {
                    (authState as? AuthState.Error)?.message ?: ""
                }
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text     = errorMessage,
                        color    = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Buton principal
                Button(
                    onClick   = { if (validate()) viewModel.updatePassword(newPassword) },
                    enabled   = newPassword.isNotBlank() && confirmPassword.isNotBlank() && !isLoading,
                    modifier  = Modifier.fillMaxWidth().height(50.dp),
                    shape     = RoundedCornerShape(12.dp),
                    colors    = ButtonDefaults.buttonColors(
                        containerColor = if (successVisible) SuccessGreen else Ral5018Main
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color       = Color.White
                        )
                    } else {
                        AnimatedVisibility(visible = successVisible, enter = fadeIn(), exit = fadeOut()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Check, contentDescription = null,
                                    tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Parolă schimbată!",
                                     fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        AnimatedVisibility(visible = !successVisible, enter = fadeIn(), exit = fadeOut()) {
                            Text("Setează parola nouă",
                                 fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
