package com.onlylipu.cloud.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Graphite950,
    primaryContainer = AccentCyanDim,
    background = Graphite950,
    onBackground = TextPrimary,
    surface = Graphite900,
    onSurface = TextPrimary,
    surfaceVariant = Graphite800,
    onSurfaceVariant = TextSecondary,
    outline = Graphite700,
    error = ErrorRed
)

private val LightScheme = lightColorScheme(
    primary = AccentCyanDim,
    onPrimary = LightSurface,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightBackground,
    onSurfaceVariant = LightTextSecondary,
    error = ErrorRed
)

@Composable
fun OnlyLipuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        content = content
    )
}
