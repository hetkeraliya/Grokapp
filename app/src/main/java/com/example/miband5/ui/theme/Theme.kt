package com.example.miband5.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = MagentaPurple,
    secondary = CyanLight,
    background = DashboardBackgroundDark,
    surface = NavyMid,
    onBackground = Color.White,
    onSurface = Color.White,
    onPrimary = Color.White
)

private val LightColors = lightColorScheme(
    primary = MagentaPurple,
    secondary = CyanLight,
    background = DashboardBackgroundLight,
    surface = Color.White,
    onBackground = Color(0xFF14151F),
    onSurface = Color(0xFF14151F),
    onPrimary = Color.White
)

@Composable
fun MiBand5Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
