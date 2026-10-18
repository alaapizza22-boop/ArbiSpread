package com.arbispread.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ArbiDarkScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Color.Black,
    secondary = AccentCyanDim,
    background = BgDeep,
    onBackground = TextPrimary,
    surface = BgSurface,
    onSurface = TextPrimary,
    surfaceVariant = BgCard,
    error = LossRed,
)

@Composable
fun ArbiSpreadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // التطبيق مصمم أساساً على الوضع الداكن بطابع منصات التداول
    MaterialTheme(
        colorScheme = ArbiDarkScheme,
        typography = ArbiTypography,
        content = content
    )
}
