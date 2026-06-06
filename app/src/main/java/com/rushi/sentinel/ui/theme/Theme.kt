package com.rushi.sentinel.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable

private val DarkColorPalette = darkColors(
    primary = PrimaryTeal,
    primaryVariant = PrimaryVariantTeal,
    secondary = AccentCyan,
    background = DeepBackground,
    surface = SlateSurface,
    error = ErrorRed,
    onPrimary = DeepBackground,
    onSecondary = DeepBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onError = TextPrimary
)

@Composable
fun SentinelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = DarkColorPalette,
        typography = Typography,
        content = content
    )
}