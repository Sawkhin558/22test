package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Emerald500,
    onPrimary = Color.White,
    secondary = Indigo700,
    onSecondary = Color.White,
    tertiary = Amber500,
    background = Slate900,
    surface = Slate800,
    onBackground = Slate100,
    onSurface = Slate100,
    error = Crimson500
)

private val LightColorScheme = lightColorScheme(
    primary = Emerald600,
    onPrimary = Color.White,
    secondary = Indigo700,
    onSecondary = Color.White,
    tertiary = Amber500,
    background = Color(0xFFF8FAFC), // Slate 50
    surface = Color.White,
    onBackground = Slate900,
    onSurface = Slate900,
    error = Crimson700
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic colors so our unique Slate-Emerald branding is always persistent
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
