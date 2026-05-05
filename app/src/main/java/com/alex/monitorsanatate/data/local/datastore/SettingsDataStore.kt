package com.alex.monitorsanatate.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    private val context: Context
) {
    companion object {
        val PREFERRED_CONNECTION   = stringPreferencesKey("preferred_connection")
        val BPM_ALERT_HIGH         = intPreferencesKey("bpm_alert_high")
        val BPM_ALERT_LOW          = intPreferencesKey("bpm_alert_low")
        val ALERTS_ENABLED         = booleanPreferencesKey("alerts_enabled")
        val ESP32_IP               = stringPreferencesKey("esp32_ip")
        val ESP32_PORT             = intPreferencesKey("esp32_port")
        val AUTH_TOKEN             = stringPreferencesKey("auth_token")
        val USER_ID                = stringPreferencesKey("user_id")
        val USER_NAME              = stringPreferencesKey("user_name")
        val USER_EMAIL             = stringPreferencesKey("user_email")
        // Profil medical în așteptare (salvat la înregistrare, aplicat la primul login)
        val PENDING_PROFILE_EMAIL  = stringPreferencesKey("pending_profile_email")
        val PENDING_PROFILE_GENDER = stringPreferencesKey("pending_profile_gender")
        val PENDING_PROFILE_AGE    = intPreferencesKey("pending_profile_age")
        val PENDING_PROFILE_WEIGHT = floatPreferencesKey("pending_profile_weight")
    }

    val authToken: Flow<String?> = context.dataStore.data.map { it[AUTH_TOKEN] }
    val userId: Flow<String?>    = context.dataStore.data.map { it[USER_ID] }
    val userName: Flow<String?>  = context.dataStore.data.map { it[USER_NAME] }
    val userEmail: Flow<String?> = context.dataStore.data.map { it[USER_EMAIL] }

    val preferredConnection: Flow<String> = context.dataStore.data.map { it[PREFERRED_CONNECTION] ?: "WIFI" }
    val bpmAlertHigh: Flow<Int>           = context.dataStore.data.map { it[BPM_ALERT_HIGH] ?: 120 }
    val bpmAlertLow: Flow<Int>            = context.dataStore.data.map { it[BPM_ALERT_LOW] ?: 50 }
    val alertsEnabled: Flow<Boolean>      = context.dataStore.data.map { it[ALERTS_ENABLED] ?: true }
    val esp32Ip: Flow<String>             = context.dataStore.data.map { it[ESP32_IP] ?: "192.168.1.100" }
    val esp32Port: Flow<Int>              = context.dataStore.data.map { it[ESP32_PORT] ?: 81 }

    // ── Profil medical — chei dinamice per utilizator ─────────────────────────
    // Fiecare utilizator are propriile date stocate cu chei unice bazate pe userId.

    fun getUserGender(userId: String): Flow<String> = context.dataStore.data.map {
        it[stringPreferencesKey("profile_gender_$userId")] ?: "M"
    }

    fun getUserAge(userId: String): Flow<Int> = context.dataStore.data.map {
        it[intPreferencesKey("profile_age_$userId")] ?: 0
    }

    fun getUserWeight(userId: String): Flow<Float> = context.dataStore.data.map {
        it[floatPreferencesKey("profile_weight_$userId")] ?: 0f
    }

    suspend fun setUserGender(userId: String, gender: String) {
        context.dataStore.edit { it[stringPreferencesKey("profile_gender_$userId")] = gender }
    }

    suspend fun setUserAge(userId: String, age: Int) {
        context.dataStore.edit { it[intPreferencesKey("profile_age_$userId")] = age }
    }

    suspend fun setUserWeight(userId: String, weight: Float) {
        context.dataStore.edit { it[floatPreferencesKey("profile_weight_$userId")] = weight }
    }

    // ── Metode generale ───────────────────────────────────────────────────────

    suspend fun setAuthToken(token: String?) {
        context.dataStore.edit { prefs ->
            if (token == null) prefs.remove(AUTH_TOKEN) else prefs[AUTH_TOKEN] = token
        }
    }

    suspend fun setUserId(id: String?) {
        context.dataStore.edit { prefs ->
            if (id == null) prefs.remove(USER_ID) else prefs[USER_ID] = id
        }
    }

    suspend fun setUserName(name: String?) {
        context.dataStore.edit { prefs ->
            if (name == null) prefs.remove(USER_NAME) else prefs[USER_NAME] = name
        }
    }

    suspend fun setUserEmail(email: String?) {
        context.dataStore.edit { prefs ->
            if (email == null) prefs.remove(USER_EMAIL) else prefs[USER_EMAIL] = email
        }
    }

    suspend fun setPreferredConnection(method: String) {
        context.dataStore.edit { it[PREFERRED_CONNECTION] = method }
    }

    suspend fun setEsp32Ip(ip: String) { context.dataStore.edit { it[ESP32_IP] = ip } }
    suspend fun setEsp32Port(port: Int) { context.dataStore.edit { it[ESP32_PORT] = port } }
    suspend fun setBpmAlertHigh(value: Int) { context.dataStore.edit { it[BPM_ALERT_HIGH] = value } }
    suspend fun setBpmAlertLow(value: Int) { context.dataStore.edit { it[BPM_ALERT_LOW] = value } }
    suspend fun setAlertsEnabled(enabled: Boolean) { context.dataStore.edit { it[ALERTS_ENABLED] = enabled } }

    // ── Profil medical în așteptare ───────────────────────────────────────────
    // Salvat la pasul 2 de înregistrare. AuthRepository îl aplică la primul login.

    suspend fun savePendingProfile(email: String, gender: String, age: Int, weight: Float) {
        context.dataStore.edit { prefs ->
            prefs[PENDING_PROFILE_EMAIL]  = email
            prefs[PENDING_PROFILE_GENDER] = gender
            if (age > 0)    prefs[PENDING_PROFILE_AGE]    = age
            if (weight > 0f) prefs[PENDING_PROFILE_WEIGHT] = weight
        }
    }

    suspend fun getPendingProfileEmail(): String? =
        context.dataStore.data.map { it[PENDING_PROFILE_EMAIL] }.first()

    suspend fun getPendingProfileGender(): String =
        context.dataStore.data.map { it[PENDING_PROFILE_GENDER] ?: "M" }.first()

    suspend fun getPendingProfileAge(): Int =
        context.dataStore.data.map { it[PENDING_PROFILE_AGE] ?: 0 }.first()

    suspend fun getPendingProfileWeight(): Float =
        context.dataStore.data.map { it[PENDING_PROFILE_WEIGHT] ?: 0f }.first()

    suspend fun clearPendingProfile() {
        context.dataStore.edit { prefs ->
            prefs.remove(PENDING_PROFILE_EMAIL)
            prefs.remove(PENDING_PROFILE_GENDER)
            prefs.remove(PENDING_PROFILE_AGE)
            prefs.remove(PENDING_PROFILE_WEIGHT)
        }
    }
}
