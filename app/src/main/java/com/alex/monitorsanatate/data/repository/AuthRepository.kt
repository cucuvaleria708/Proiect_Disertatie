package com.alex.monitorsanatate.data.repository

import com.alex.monitorsanatate.data.local.datastore.SettingsDataStore
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

sealed class DeepLinkResult {
    object EmailConfirmation : DeepLinkResult()
    object PasswordRecovery  : DeepLinkResult()
    object Unknown           : DeepLinkResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val supabaseAuth: Auth,
    private val settingsDataStore: SettingsDataStore,
    private val medicalProfileRepository: MedicalProfileRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val authToken: Flow<String?> = settingsDataStore.authToken
    val userId: Flow<String?>    = settingsDataStore.userId

    init {
        observeSessionStatus()
    }

    private fun observeSessionStatus() {
        supabaseAuth.sessionStatus
            .onEach { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val session = status.session
                        val user    = session.user
                        if (user != null) {
                            val name = user.userMetadata?.get("username")?.jsonPrimitive?.contentOrNull
                                ?: user.email?.substringBefore("@")
                                ?: "Utilizator"
                            settingsDataStore.setAuthToken(session.accessToken)
                            settingsDataStore.setUserId(user.id)
                            settingsDataStore.setUserName(name)
                            settingsDataStore.setUserEmail(user.email ?: "")
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {}
                    else -> {}
                }
            }
            .launchIn(scope)
    }

    // ── Deep link ─────────────────────────────────────────────────────────────
    // Returnează tipul de deep link pentru ca MainActivity să știe cum să navigheze.

    suspend fun handleDeepLink(uri: String): DeepLinkResult {
        return try {
            when {
                "reset-password" in uri && "code=" in uri -> {
                    val code = uri.substringAfter("code=").substringBefore("&")
                    supabaseAuth.exchangeCodeForSession(code)
                    DeepLinkResult.PasswordRecovery
                }
                "code=" in uri -> {
                    val code = uri.substringAfter("code=").substringBefore("&")
                    supabaseAuth.exchangeCodeForSession(code)
                    DeepLinkResult.EmailConfirmation
                }
                else -> DeepLinkResult.Unknown
            }
        } catch (e: Exception) {
            e.printStackTrace()
            DeepLinkResult.Unknown
        }
    }

    // ── Autentificare ─────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            supabaseAuth.signInWith(Email) {
                this.email    = email
                this.password = password
            }
            val session = supabaseAuth.currentSessionOrNull()
            val user    = supabaseAuth.currentUserOrNull()

            if (session == null || user == null) {
                return Result.failure(Exception("Sesiunea nu a putut fi creată."))
            }
            if (user.emailConfirmedAt == null) {
                supabaseAuth.signOut()
                return Result.failure(
                    Exception("Te rugăm să confirmi adresa de email înainte de a te autentifica.")
                )
            }

            val name = user.userMetadata
                ?.get("username")?.jsonPrimitive?.contentOrNull
                ?: user.email?.substringBefore("@")
                ?: "Utilizator"
            settingsDataStore.setAuthToken(session.accessToken)
            settingsDataStore.setUserId(user.id)
            settingsDataStore.setUserName(name)
            settingsDataStore.setUserEmail(user.email)

            syncPendingProfileIfNeeded(userId = user.id, userEmail = user.email ?: "")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String, username: String): Result<Unit> {
        return try {
            supabaseAuth.signUpWith(Email) {
                this.email    = email
                this.password = password
                data = buildJsonObject { put("username", username) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Parola uitată ─────────────────────────────────────────────────────────

    suspend fun resetPasswordForEmail(email: String): Result<Unit> {
        return try {
            supabaseAuth.resetPasswordForEmail(email, redirectUrl = "https://cucuvaleria708.github.io/vital-signs-reset-password/reset-password.html")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            supabaseAuth.updateUser { password = newPassword }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    suspend fun logout() {
        try { supabaseAuth.signOut() } catch (_: Exception) {}
        finally {
            settingsDataStore.setAuthToken(null)
            settingsDataStore.setUserId(null)
            settingsDataStore.setUserName(null)
            settingsDataStore.setUserEmail(null)
        }
    }

    suspend fun isLoggedIn(): Boolean = supabaseAuth.currentSessionOrNull() != null

    // ── Sync profil medical din înregistrare ──────────────────────────────────

    private suspend fun syncPendingProfileIfNeeded(userId: String, userEmail: String) {
        val pendingEmail = settingsDataStore.getPendingProfileEmail() ?: return
        if (pendingEmail.equals(userEmail, ignoreCase = true)) {
            if (medicalProfileRepository.getProfileOnce(userId) == null) {
                medicalProfileRepository.saveProfile(
                    userId = userId,
                    gender = settingsDataStore.getPendingProfileGender(),
                    age    = settingsDataStore.getPendingProfileAge(),
                    weight = settingsDataStore.getPendingProfileWeight()
                )
            }
            settingsDataStore.clearPendingProfile()
        }
    }
}
