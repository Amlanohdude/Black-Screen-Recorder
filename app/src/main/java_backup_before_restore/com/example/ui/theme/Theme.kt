package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentRed,
    onPrimary = Color.White,
    primaryContainer = SurfaceSecondary,
    onPrimaryContainer = TextPrimary,
    secondary = TextSecondary,
    onSecondary = BgDark,
    secondaryContainer = SurfaceTertiary,
    onSecondaryContainer = TextPrimary,
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfacePrimary,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceSecondary,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = SurfaceSecondary,
    error = AccentRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
