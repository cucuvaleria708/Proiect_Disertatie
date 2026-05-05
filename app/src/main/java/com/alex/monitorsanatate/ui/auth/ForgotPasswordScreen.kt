package com.alex.monitorsanatate.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alex.monitorsanatate.ui.components.GlassButton
import com.alex.monitorsanatate.ui.components.GlassCard
import com.alex.monitorsanatate.ui.components.GlassInput
import com.alex.monitorsanatate.ui.theme.*

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    var email by remember { mutableStateOf("") }

    val isLoading = authState is AuthState.Loading
    val isSuccess = authState is AuthState.Success

    DisposableEffect(Unit) {
        onDispose { viewModel.resetAuthState() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // Buton înapoi
        IconButton(
            onClick   = onNavigateBack,
            modifier  = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector   = Icons.Filled.ArrowBack,
                contentDescription = "Înapoi",
                tint          = TextSecondary
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
                    imageVector   = Icons.Filled.LockReset,
                    contentDescription = null,
                    tint          = Color.White,
                    modifier      = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Resetare parolă",
                fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Introdu adresa de email asociată contului tău.\nÎți vom trimite un link de resetare.",
                fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // ── Card ──────────────────────────────────────────────────────
            GlassCard {
                if (isSuccess) {
                    // Ecran de confirmare după trimitere email
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SuccessGreen.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                               modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                Icons.Filled.Email,
                                contentDescription = null,
                                tint     = SuccessGreen,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                (authState as AuthState.Success).message,
                                color     = SuccessGreen,
                                fontSize  = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Verifică folderul Spam dacă nu găsești emailul.",
                                color    = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick   = onNavigateBack,
                        modifier  = Modifier.fillMaxWidth().height(50.dp),
                        shape     = RoundedCornerShape(12.dp),
                        colors    = ButtonDefaults.buttonColors(containerColor = Ral5018Main)
                    ) {
                        Text("Înapoi la autentificare",
                             fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                } else {
                    GlassInput(
                        value         = email,
                        onValueChange = { email = it },
                        label         = "Email",
                        placeholder   = "exemplu@email.ro",
                        leadingIcon   = {
                            Icon(Icons.Filled.Email, contentDescription = null,
                                modifier = Modifier.size(18.dp))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(Modifier.height(16.dp))

                    if (authState is AuthState.Error) {
                        Text(
                            text     = (authState as AuthState.Error).message,
                            color    = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    GlassButton(
                        title   = if (isLoading) "Se trimite..." else "Trimite link de resetare",
                        onClick = { viewModel.forgotPassword(email.trim()) },
                        loading = isLoading,
                        enabled = email.isNotBlank() && !isLoading
                    )

                    Spacer(Modifier.height(16.dp))

                    TextButton(
                        onClick  = onNavigateBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Înapoi la autentificare",
                             color = TextSecondary, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
