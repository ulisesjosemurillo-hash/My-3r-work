package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = lightColorScheme(
    primary = JtGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = JtGreenSecondary,
    onPrimaryContainer = Color.White,
    secondary = JtGold,
    onSecondary = Color.White,
    background = JtBackground,
    onBackground = JtPrimaryText,
    surface = JtSurface,
    onSurface = JtPrimaryText,
    surfaceVariant = JtBackground,
    onSurfaceVariant = JtSecondaryText,
    outline = JtBorder,
    error = JtError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}
