package com.alex.monitorsanatate.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alex.monitorsanatate.ui.theme.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alex.monitorsanatate.MainViewModel
import com.alex.monitorsanatate.ui.CameraPulseMonitorScreen
import com.alex.monitorsanatate.ui.PulseSelectionScreen
import com.alex.monitorsanatate.ui.SensorPulseMonitorScreen
import com.alex.monitorsanatate.ui.auth.ForgotPasswordScreen
import com.alex.monitorsanatate.ui.auth.LoginScreen
import com.alex.monitorsanatate.ui.auth.RegisterScreen
import com.alex.monitorsanatate.ui.auth.ResetPasswordScreen
import com.alex.monitorsanatate.ui.splash.SplashScreen
import com.alex.monitorsanatate.ui.connection.ConnectionScreen
import com.alex.monitorsanatate.ui.dashboard.DashboardScreen
import com.alex.monitorsanatate.ui.ecganalysis.EcgAnalysisScreen
import com.alex.monitorsanatate.ui.ecgdetail.EcgDetailScreen
import com.alex.monitorsanatate.ui.history.ChartsScreen
import com.alex.monitorsanatate.ui.history.HistoryDetailScreen
import com.alex.monitorsanatate.ui.history.HistoryScreen
import com.alex.monitorsanatate.ui.settings.SettingsScreen

@Composable
fun NavGraph(
    startDestination: String = Screen.Login.route,
    mainViewModel: MainViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Navigare declanșată de deep link-ul de resetare parolă
    LaunchedEffect(Unit) {
        mainViewModel.navigationEvent.collect { route ->
            navController.navigate(route) {
                popUpTo(Screen.Login.route) { inclusive = false }
            }
        }
    }

    val bottomNavRoutes = BottomNavItem.entries.map { it.screen.route }
    val authRoutes      = listOf(Screen.Login.route, Screen.Register.route,
                                  Screen.ForgotPassword.route, Screen.ResetPassword.route)
    val showBottomBar   = currentDestination?.route in bottomNavRoutes &&
                          currentDestination?.route !in authRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Surface(color = AppBackground, tonalElevation = 0.dp) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        windowInsets   = WindowInsets(0, 0, 0, 0)
                    ) {
                        BottomNavItem.entries.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == item.screen.route
                            } == true
                            NavigationBarItem(
                                icon  = {
                                    Icon(item.icon, contentDescription = item.label,
                                        modifier = Modifier.size(22.dp))
                                },
                                label = {
                                    Text(item.label, fontSize = 9.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        letterSpacing = 0.5.sp)
                                },
                                selected = selected,
                                onClick  = {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState    = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor   = Ral5018Main,
                                    selectedTextColor   = Ral5018Main,
                                    indicatorColor      = Color.Transparent,
                                    unselectedIconColor = TextDisabled,
                                    unselectedTextColor = TextDisabled
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = startDestination,
            modifier         = Modifier.padding(innerPadding)
        ) {
            // ── Splash ────────────────────────────────────────────────────
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToMain = {
                        navController.navigate(Screen.PulseMonitor.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            // ── Login ─────────────────────────────────────────────────────
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.PulseMonitor.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    },
                    onNavigateToForgotPassword = {
                        navController.navigate(Screen.ForgotPassword.route)
                    }
                )
            }

            // ── Înregistrare ──────────────────────────────────────────────
            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }

            // ── Parola uitată ─────────────────────────────────────────────
            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ── Resetare parolă (accesat via deep link) ───────────────────
            composable(Screen.ResetPassword.route) {
                ResetPasswordScreen(
                    onPasswordReset = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateBack = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // ── Tab 1: Puls ───────────────────────────────────────────────
            composable(Screen.PulseMonitor.route) {
                PulseSelectionScreen(
                    onNavigateToSensor = { navController.navigate(Screen.SensorPulse.route) },
                    onNavigateToCamera = { navController.navigate(Screen.CameraPulse.route) }
                )
            }

            composable(Screen.SensorPulse.route) {
                SensorPulseMonitorScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.CameraPulse.route) {
                CameraPulseMonitorScreen(onNavigateBack = { navController.popBackStack() })
            }

            // ── Tab 2: ECG Live ───────────────────────────────────────────
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToConnection  = { navController.navigate(Screen.Connection.route) },
                    onNavigateToEcgDetail   = { navController.navigate(Screen.EcgDetail.route) },
                    onNavigateToEcgAnalysis = {
                        navController.navigate(Screen.EcgAnalysis.route) {
                            launchSingleTop = true
                            restoreState    = false
                        }
                    }
                )
            }

            // ── Tab 3: Analiză AI ─────────────────────────────────────────
            composable(Screen.EcgAnalysis.route) {
                EcgAnalysisScreen()
            }

            // ── Tab 4: Jurnal ─────────────────────────────────────────────
            composable(Screen.History.route) {
                HistoryScreen(
                    onNavigateToDetail = { measurementId ->
                        navController.navigate(Screen.HistoryDetail.createRoute(measurementId))
                    },
                    onNavigateToCharts = { filter ->
                        navController.navigate(Screen.Charts.createRoute(filter))
                    }
                )
            }

            // ── Grafice Puls / EKG ────────────────────────────────────────
            composable(
                route = Screen.Charts.route,
                arguments = listOf(navArgument("chartFilter") { type = NavType.StringType })
            ) { backStackEntry ->
                val filter = backStackEntry.arguments?.getString("chartFilter") ?: "Puls"
                ChartsScreen(
                    chartFilter    = filter,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ── Tab 5: Setări ─────────────────────────────────────────────
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // ── Ecrane secundare ──────────────────────────────────────────
            composable(Screen.Connection.route) {
                ConnectionScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.EcgDetail.route) {
                EcgDetailScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route     = Screen.HistoryDetail.route,
                arguments = listOf(navArgument("measurementId") { type = NavType.LongType })
            ) {
                HistoryDetailScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
