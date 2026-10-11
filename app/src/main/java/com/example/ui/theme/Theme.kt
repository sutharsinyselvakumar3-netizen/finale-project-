package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AgroGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = AgroGreenContainer,
    onPrimaryContainer = AgroOnGreenContainer,
    secondary = AgroGreenLight,
    onSecondary = Color.Black,
    secondaryContainer = AgroSurfaceVariant,
    onSecondaryContainer = AgroTextPrimary,
    tertiary = StatusAutoBlue,
    onTertiary = Color.White,
    background = AgroCharcoalDark,
    onBackground = AgroTextPrimary,
    surface = AgroSurfaceDark,
    onSurface = AgroTextPrimary,
    surfaceVariant = AgroSurfaceVariant,
    onSurfaceVariant = AgroTextSecondary,
    outline = AgroOutline,
    error = StatusEmergencyRed,
    onError = Color.White
)

@Composable
fun OnionRobotTheme(
    content: @Composable () -> Unit
) {
    // Agricultural robot UI is outdoor-optimized with deep charcoal surfaces and high-contrast green accents
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
