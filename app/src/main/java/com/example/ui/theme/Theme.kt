package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AuraPrimary,
    secondary = AuraSecondary,
    tertiary = AuraTertiary,
    background = AuraDarkBackground,
    surface = AuraSurface,
    surfaceVariant = AuraSurfaceVariant,
    onPrimary = AuraDarkBackground,
    onSecondary = AuraTextPrimary,
    onBackground = AuraTextPrimary,
    onSurface = AuraTextPrimary,
    onSurfaceVariant = AuraTextSecondary
)

private val LightColorScheme = DarkColorScheme // Enforce dark theme for sleek look

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Always dark for sleek AI feel
    dynamicColor: Boolean = false, // Disable dynamic colors to keep layout catchy
    content: @Composable () -> Unit,
) {
    val colorScheme = DarkColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
