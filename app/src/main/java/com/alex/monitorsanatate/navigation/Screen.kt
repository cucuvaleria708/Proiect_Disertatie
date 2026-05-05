package com.alex.monitorsanatate.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Splash         : Screen("splash")
    data object Login          : Screen("login")
    data object Register       : Screen("register")
    data object ForgotPassword : Screen("forgot_password")
    data object ResetPassword  : Screen("reset_password")
    data object PulseMonitor  : Screen("pulse_monitor")
    data object SensorPulse   : Screen("sensor_pulse")
    data object CameraPulse   : Screen("camera_pulse")
    data object Dashboard     : Screen("dashboard")
    data object EcgAnalysis   : Screen("ecg_analysis")
    data object History       : Screen("history")
    data object Settings      : Screen("settings")

    // Ecrane secundare (fara tab in bara de jos)
    data object Connection    : Screen("connection")
    data object EcgDetail     : Screen("ecg_detail")
    data object HistoryDetail : Screen("history_detail/{measurementId}") {
        fun createRoute(measurementId: Long) = "history_detail/$measurementId"
    }
    data object Charts : Screen("charts/{chartFilter}") {
        fun createRoute(filter: String) = "charts/$filter"
    }
}

enum class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
) {
    PULSE(Screen.PulseMonitor, "Verificare Puls", Icons.Filled.Favorite),
    LIVE(Screen.Dashboard,    "Verificare EKG",   Icons.Filled.GraphicEq),
    ANALYSIS(Screen.EcgAnalysis, "Analiză AI", Icons.Filled.ImageSearch),
    HISTORY(Screen.History,   "Jurnal",    Icons.Filled.History),
    SETTINGS(Screen.Settings, "Setări",     Icons.Filled.Settings),
}
