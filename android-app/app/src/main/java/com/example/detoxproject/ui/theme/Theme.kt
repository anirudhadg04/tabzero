package com.example.detoxproject.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

object ThemeManager {
    private const val PREFS_NAME = "tabzero_theme_preferences"
    private const val KEY_THEME = "selected_theme_mode"

    var themeMode by mutableStateOf(ThemeMode.SYSTEM)

    fun init(context: android.content.Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        themeMode = try {
            ThemeMode.valueOf(saved)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setTheme(mode: ThemeMode, context: android.content.Context) {
        themeMode = mode
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, mode.name).apply()
    }

    @Composable
    fun isDark(): Boolean {
        return when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    }
}

private val OfflineLightColorScheme = lightColorScheme(
    primary = OfflineDarkButtonLight,
    onPrimary = Color.White,
    primaryContainer = OfflineAccentPrimary,
    onPrimaryContainer = OfflineTextPrimaryLight,
    secondary = OfflineAccentPrimary,
    onSecondary = OfflineTextPrimaryLight,
    secondaryContainer = SoftBlueLight,
    onSecondaryContainer = OfflineTextPrimaryLight,
    tertiary = OfflineAccentCyan,
    onTertiary = OfflineTextPrimaryLight,
    background = OfflineBackgroundLight,
    onBackground = OfflineTextPrimaryLight,
    surface = OfflineSurfaceLight,
    onSurface = OfflineTextPrimaryLight,
    surfaceVariant = OfflineSurfaceVariantLight,
    onSurfaceVariant = OfflineTextSecondaryLight,
    outline = OfflineBorderLight
)

private val OfflineDarkColorScheme = darkColorScheme(
    primary = OfflineAccentCyan,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = OfflineSurfaceVariantDark,
    onPrimaryContainer = OfflineTextPrimaryDark,
    secondary = OfflineAccentPrimary,
    onSecondary = OfflineTextPrimaryDark,
    secondaryContainer = SoftBlueDark,
    onSecondaryContainer = OfflineTextPrimaryDark,
    tertiary = OfflineAccentCyan,
    onTertiary = Color(0xFF0F172A),
    background = OfflineBackgroundDark,
    onBackground = OfflineTextPrimaryDark,
    surface = OfflineSurfaceDark,
    onSurface = OfflineTextPrimaryDark,
    surfaceVariant = OfflineSurfaceVariantDark,
    onSurfaceVariant = OfflineTextSecondaryDark,
    outline = OfflineBorderDark
)

@Composable
fun OfflineTheme(
    darkTheme: Boolean = ThemeManager.isDark(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) OfflineDarkColorScheme else OfflineLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
