package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ARMeasureColorScheme = darkColorScheme(
    primary = ARCyan,
    onPrimary = Color(0xFF070B19),
    primaryContainer = ARCardSlate,
    onPrimaryContainer = Color.White,
    secondary = ARLaserGreen,
    onSecondary = Color(0xFF070B19),
    secondaryContainer = ARBorderSlate,
    onSecondaryContainer = Color.White,
    tertiary = ARAmber,
    onTertiary = Color(0xFF070B19),
    background = ARDarkNavy,
    onBackground = Color.White,
    surface = ARSurfaceNavy,
    onSurface = Color.White,
    surfaceVariant = ARCardSlate,
    onSurfaceVariant = Color.White.copy(alpha = 0.8f),
    error = ARRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent high-tech AR styling
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ARMeasureColorScheme,
        typography = Typography,
        content = content
    )
}
