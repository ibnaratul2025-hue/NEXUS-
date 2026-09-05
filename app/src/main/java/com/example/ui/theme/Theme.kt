package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NexusColorScheme = darkColorScheme(
    primary = NexusCyan,
    onPrimary = Color(0xFF00382F),
    primaryContainer = NexusCyanDark,
    onPrimaryContainer = NexusCyan,
    secondary = NexusViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A154B),
    onSecondaryContainer = Color(0xFFF3E8FF),
    tertiary = NexusBlue,
    onTertiary = Color(0xFF003355),
    background = NexusBackground,
    onBackground = NexusTextPrimary,
    surface = NexusSurface,
    onSurface = NexusTextPrimary,
    surfaceVariant = NexusSurfaceElevated,
    onSurfaceVariant = NexusTextSecondary,
    outline = NexusBorder,
    outlineVariant = NexusBorderActive,
    error = NexusRuby,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve NEXUS cyberpunk identity
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = NexusColorScheme,
        typography = Typography,
        content = content
    )
}
