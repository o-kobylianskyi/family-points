package com.olehkobylianskyi.familypoints.android.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Saved on-device; does not depend on a family, JWT or backend availability. */
enum class FamilyThemeMode { SYSTEM, LIGHT, DARK }

class FamilyThemePreference(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("fp_ui_theme", Context.MODE_PRIVATE)
    fun get(): FamilyThemeMode =
        runCatching { FamilyThemeMode.valueOf(prefs.getString("mode", "SYSTEM") ?: "SYSTEM") }
            .getOrDefault(FamilyThemeMode.SYSTEM)
    fun set(value: FamilyThemeMode) { prefs.edit().putString("mode", value.name).apply() }
}

private val SoftFamilyLight = lightColorScheme(
    primary = Color(0xFF518FB8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9EDFB),
    onPrimaryContainer = Color(0xFF18374D),
    secondary = Color(0xFF5B9974),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCECE2),
    background = Color(0xFFF5FAFF),
    onBackground = Color(0xFF121B30),
    surface = Color.White,
    onSurface = Color(0xFF121B30),
    surfaceVariant = Color(0xFFEDF5FB),
    onSurfaceVariant = Color(0xFF5D6C80),
    outline = Color(0xFFC9DCEA),
    outlineVariant = Color(0xFFE0EAF0),
    error = Color(0xFFB93448)
)

private val SoftFamilyDark = darkColorScheme(
    primary = Color(0xFF91D4FF),
    onPrimary = Color(0xFF102638),
    primaryContainer = Color(0xFF24465C),
    onPrimaryContainer = Color(0xFFE0F3FF),
    secondary = Color(0xFF76DED4),
    onSecondary = Color(0xFF102E33),
    secondaryContainer = Color(0xFF1E4948),
    background = Color(0xFF10171E),
    onBackground = Color(0xFFF4F7FC),
    surface = Color(0xFF19232C),
    onSurface = Color(0xFFF4F7FC),
    surfaceVariant = Color(0xFF24323D),
    onSurfaceVariant = Color(0xFFB8C9D9),
    outline = Color(0xFF405363),
    outlineVariant = Color(0xFF2D3B45),
    error = Color(0xFFFF879A)
)

private val SoftFamilyShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(21.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun FamilyPointsTheme(
    mode: FamilyThemeMode = FamilyThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (mode) {
        FamilyThemeMode.DARK -> true
        FamilyThemeMode.LIGHT -> false
        FamilyThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) SoftFamilyDark else SoftFamilyLight,
        shapes = SoftFamilyShapes,
        typography = Typography,
        content = content
    )
}
