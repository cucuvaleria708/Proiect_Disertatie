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
import androidx.compose.material.icons.filled.*
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
import com.alex.monitorsanatate.ui.components.GlassButton
import com.alex.monitorsanatate.ui.components.GlassCard
import com.alex.monitorsanatate.ui.components.GlassInput
import com.alex.monitorsanatate.ui.theme.*
import kotlinx.coroutines.delay

private enum class RegisterStep { ACCOUNT, MEDICAL_PROFILE, EMAIL_CONFIRM }

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()

    // ── Pas 1: date cont ──────────────────────────────────────────────────────
    var username    by remember { mutableStateOf("") }
    var email       by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // ── Pas 2: profil medical ─────────────────────────────────────────────────
    var gender      by remember { mutableStateOf("M") }
    var ageText     by remember { mutableStateOf("") }
    var weightText  by remember { mutableStateOf("") }

    // ── Flow de pași ─────────────────────────────────────────────────────────
    var currentStep by remember { mutableStateOf(RegisterStep.ACCOUNT) }

    // Când contul e creat cu succes → treci la pasul 2
    LaunchedEffect(authState) {
        if (authState is AuthState.Success && currentStep == RegisterStep.ACCOUNT) {
            currentStep = RegisterStep.MEDICAL_PROFILE
        }
    }

    // Animații de intrare (pas 1)
    var headerVisible by remember { mutableStateOf(false) }
    var formVisible   by remember { mutableStateOf(false) }
    var footerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        headerVisible = true; delay(100)
        formVisible   = true; delay(100)
        footerVisible = true
    }

    val isLoading = authState is AuthState.Loading

    // ── PASUL 3: confirmare email ─────────────────────────────────────────────
    if (currentStep == RegisterStep.EMAIL_CONFIRM) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .windowInsetsPadding(WindowInsets.systemBars),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Ral5018Main.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = null,
                        tint = Ral5018Main,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "Bun venit, $username!",
                    fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    color = TextPrimary, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Am trimis un email de confirmare la:",
                    fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    email,
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = Ral5018Main, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Profilul tău medical a fost salvat și va fi activat după confirmare.",
                    fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(32.dp))
                GlassButton(
                    title = "Mergi la autentificare",
                    onClick = onNavigateToLogin
                )
            }
        }
        return
    }

    // ── PASUL 2: profil medical ───────────────────────────────────────────────
    if (currentStep == RegisterStep.MEDICAL_PROFILE) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Ral5018Main, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Profil Medical",
                    fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Completează datele pentru interpretări personalizate.\nPoți modifica oricând din Setări.",
                    fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(28.dp))

                GlassCard {
                    // Gen
                    Text(
                        "Gen",
                        fontSize = 12.sp, color = TextSecondary,
                        fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = gender == "M",
                            onClick  = { gender = "M" },
                            label    = { Text("Masculin", fontSize = 13.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Ral5018Main,
                                selectedLabelColor     = Color.White
                            )
                        )
                        FilterChip(
                            selected = gender == "F",
                            onClick  = { gender = "F" },
                            label    = { Text("Feminin", fontSize = 13.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Ral5018Main,
                                selectedLabelColor     = Color.White
                            )
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Vârstă + Greutate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value         = ageText,
                            onValueChange = { ageText = it.filter { c -> c.isDigit() }.take(3) },
                            label         = { Text("Vârstă (ani)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine    = true,
                            modifier      = Modifier.weight(1f),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = Ral5018Main,
                                unfocusedBorderColor = AppSurface,
                                focusedLabelColor    = Ral5018Main,
                                unfocusedTextColor   = TextPrimary,
                                focusedTextColor     = TextPrimary,
                                cursorColor          = Ral5018Main
                            )
                        )
                        OutlinedTextField(
                            value         = weightText,
                            onValueChange = {
                                weightText = it.filter { c -> c.isDigit() || c == '.' }.take(5)
                            },
                            label         = { Text("Greutate (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine    = true,
                            modifier      = Modifier.weight(1f),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = Ral5018Main,
                                unfocusedBorderColor = AppSurface,
                                focusedLabelColor    = Ral5018Main,
                                unfocusedTextColor   = TextPrimary,
                                focusedTextColor     = TextPrimary,
                                cursorColor          = Ral5018Main
                            )
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Buton Salvează
                    Button(
                        onClick = {
                            val age    = ageText.toIntOrNull()?.coerceIn(1, 120) ?: 0
                            val weight = weightText.toFloatOrNull()?.coerceIn(30f, 300f) ?: 0f
                            viewModel.savePendingProfile(email, gender, age, weight)
                            currentStep = RegisterStep.EMAIL_CONFIRM
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Ral5018Main)
                    ) {
                        Text("Salvează și continuă", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }

                    Spacer(Modifier.height(10.dp))

                    // Buton Omite
                    TextButton(
                        onClick = { currentStep = RegisterStep.EMAIL_CONFIRM },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Omite, completez mai târziu",
                            color = TextSecondary, fontSize = 14.sp
                        )
                    }
                }
            }
        }
        return
    }

    // ── PASUL 1: creare cont ──────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = headerVisible,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { -40 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Ral5018Main, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PersonAdd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Cont Nou",
                        fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Creează-ți contul pentru a începe",
                        fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            AnimatedVisibility(
                visible = formVisible,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { 40 }
            ) {
                GlassCard {
                    Text(
                        text = "Înregistrare",
                        fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary
                    )
                    Spacer(Modifier.height(20.dp))

                    GlassInput(
                        value = username,
                        onValueChange = { username = it },
                        label = "Nume utilizator",
                        placeholder = "ion.popescu",
                        leadingIcon = {
                            Icon(Icons.Filled.Person, contentDescription = null,
                                modifier = Modifier.size(18.dp))
                        }
                    )
                    Spacer(Modifier.height(16.dp))

                    GlassInput(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        placeholder = "exemplu@email.ro",
                        leadingIcon = {
                            Icon(Icons.Filled.Email, contentDescription = null,
                                modifier = Modifier.size(18.dp))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    Spacer(Modifier.height(16.dp))

                    GlassInput(
                        value = password,
                        onValueChange = { password = it },
                        label = "Parola",
                        placeholder = "••••••••",
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, contentDescription = null,
                                modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Outlined.VisibilityOff
                                                  else Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    Spacer(Modifier.height(24.dp))

                    if (authState is AuthState.Error) {
                        Text(
                            text = (authState as AuthState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    GlassButton(
                        title   = if (isLoading) "Se creează contul..." else "Creează cont",
                        onClick = { viewModel.register(username, email, password) },
                        loading = isLoading,
                        enabled = username.isNotBlank() && email.isNotBlank() && password.isNotBlank()
                    )

                    Spacer(Modifier.height(20.dp))

                    TextButton(onClick = onNavigateToLogin) {
                        Text(
                            text = "Ai deja cont? ",
                            color = TextSecondary, fontSize = 14.sp
                        )
                        Text(
                            text = "Conectează-te",
                            color = Ral5018Main, fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            AnimatedVisibility(
                visible = footerVisible,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { 40 }
            ) {
                Text(
                    text = "Date protejate conform GDPR. Comunicații criptate.",
                    fontSize = 12.sp, color = TextDisabled,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
