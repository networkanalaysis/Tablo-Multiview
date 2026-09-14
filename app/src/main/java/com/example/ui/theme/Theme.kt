package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AerioDarkColorScheme = darkColorScheme(
    primary = AerioCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00363A),
    onPrimaryContainer = AerioCyan,
    secondary = AerioNeonGreen,
    onSecondary = Color.Black,
    tertiary = AerioOrange,
    background = AerioNavyBg,
    onBackground = AerioTextPrimary,
    surface = AerioCardDark,
    onSurface = AerioTextPrimary,
    surfaceVariant = AerioCardBorder,
    onSurfaceVariant = AerioTextSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to dark broadcast theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AerioDarkColorScheme,
        typography = Typography,
        content = content
    )
}
