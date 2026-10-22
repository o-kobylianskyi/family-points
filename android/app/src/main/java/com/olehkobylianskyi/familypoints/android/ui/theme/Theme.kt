package com.olehkobylianskyi.familypoints.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Deliberate native branding shared with the web app. Never use wallpaper-derived
// dynamic palettes: they made screens visually inconsistent across devices.
private val FamilyPointsLight = lightColorScheme(
    primary = Color(0xFF7856D8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E1FF),
    onPrimaryContainer = Color(0xFF342261),
    secondary = Color(0xFF62558B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEAE5F5),
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF202536),
    surface = Color.White,
    onSurface = Color(0xFF202536),
    surfaceVariant = Color(0xFFF0F1F6),
    onSurfaceVariant = Color(0xFF697083),
    outline = Color(0xFFC9CED9),
    outlineVariant = Color(0xFFE5E7EE),
    error = Color(0xFFBA283D)
)

@Composable
fun FamilyPointsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FamilyPointsLight,
        typography = Typography,
        content = content
    )
}
