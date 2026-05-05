package com.alex.monitorsanatate.ui.theme

import androidx.compose.ui.graphics.Color

// ── RAL 5018 (Turquoise Blue) ──────────────────────────────────────────────
val Ral5018Main      = Color(0xFF008F8C)
val Ral5018Light     = Color(0xFF4DB6B4)
val Ral5018Container = Color(0xFF005E5C)

// ── Grey Scale ──────────────────────────────────────────────────────────────
val AppBackground     = Color(0xFF121212)   // Fundal principal
val AppSurface        = Color(0xFF252525)   // Carduri standard
val AppSurfaceHigh    = Color(0xFF333333)   // Carduri elevate / hover
val AppSurfaceOverlay = Color(0xFF424242)

// ── Text ──────────────────────────────────────────────────────────────────────
val TextPrimary       = Color(0xFFE0E0E0)   // Gri foarte deschis (aproape alb)
val TextSecondary     = Color(0xFF9E9E9E)   // Gri mediu
val TextDisabled      = Color(0xFF616161)

// ── Semantic (Mapping to RAL 5018 and Grey as requested) ─────────────────────
val LiveRed           = Ral5018Main         // Inlocuim roșu cu RAL 5018
val BpmRed            = Ral5018Main
val WarningAmber      = Color(0xFF757575)   // Gri pentru alerte (neutru)
val SuccessGreen      = Ral5018Light
val EcgLine           = Ral5018Main
val EcgGrid           = Color(0xFF2A2A2A)
val EcgGridMajor      = Color(0xFF3A3A3A)

// ── Actual Red (Vibrant Pulse) ──────────────────────────────────────────
val PulseRedMain      = Color(0xFFFF1744)
val PulseRedVibrant   = Color(0xFFFF5252)
val PulseRedDark      = Color(0xFFB91D1D)
val PulseRedGlow      = Color(0xFFFF1744).copy(alpha = 0.4f)

// ── Actual Burgundy (Deep Red) ──────────────────────────────────────────
val BurgundyMain      = Color(0xFF800020)
val BurgundyLight     = Color(0xFFAA334E)
val BurgundyDark      = Color(0xFF4D0012)
val BurgundyContainer = Color(0xFF2E000B)
val BurgundyVibrant   = Color(0xFF990026)

// Legacy compatibility (all mapped to the new theme to prevent crashes)
val MedicalBlueLight     = Ral5018Light
val MedicalBluePrimary   = Ral5018Main
val MedicalBlueContainer = Ral5018Container
val MedicalBlueDeep      = Color(0xFF003D3B)

val BurgundyPrimary   = BurgundyMain
val MedicalBurgundy   = BurgundyMain

val RoseLight         = Color(0xFF757575)
val RoseContainer     = Color(0xFF424242)
val TealLight         = Ral5018Light
val TealContainer     = Ral5018Container
