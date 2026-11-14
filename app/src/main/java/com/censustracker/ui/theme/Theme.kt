package com.censustracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Modern palette: indigo primary, sky secondary, teal tertiary
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF0284C7),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFF0D9488),
    surface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFFE2E8F0),
    background = Color(0xFFF8FAFC),
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEE2E2),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF7DD3FC),
    onSecondary = Color(0xFF0C4A6E),
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFF5EEAD4),
    surface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFF1E293B),
    background = Color(0xFF0F172A),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF7F1D1D),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF64748B)
)

val AppTypography = Typography()

@Composable
fun CensusTrackerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
