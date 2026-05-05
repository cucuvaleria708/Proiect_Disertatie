package com.alex.monitorsanatate.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MedicalDarkScheme = darkColorScheme(
    primary              = Ral5018Main,
    onPrimary            = Color.White,
    primaryContainer     = Ral5018Container,
    onPrimaryContainer   = TextPrimary,

    secondary            = Ral5018Light,
    onSecondary          = Color.Black,
    secondaryContainer   = AppSurface,
    onSecondaryContainer = TextSecondary,

    tertiary             = Ral5018Main,
    onTertiary           = Color.White,
    tertiaryContainer    = Ral5018Container,

    background           = AppBackground,
    onBackground         = TextPrimary,

    surface              = AppSurface,
    onSurface            = TextPrimary,
    surfaceVariant       = AppSurfaceHigh,
    onSurfaceVariant     = TextSecondary,

    error                = Color(0xFFCF6679),
    outline              = TextDisabled,
    inversePrimary       = Ral5018Main,
)

@Composable
fun MonitorSanatateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MedicalDarkScheme,
        typography  = Typography,
        content     = content
    )
}
