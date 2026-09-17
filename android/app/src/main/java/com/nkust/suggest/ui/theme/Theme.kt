package com.nkust.suggest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = AccentCyan,
    background = BgDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceElevatedDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextMainDark,
    onSurface = TextMainDark,
    onSurfaceVariant = TextMutedDark,
    outline = BorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = AccentCyan,
    background = BgLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceElevatedLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextMainLight,
    onSurface = TextMainLight,
    onSurfaceVariant = TextMutedLight,
    outline = BorderLight
)

@Composable
fun NKUSTSuggestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
