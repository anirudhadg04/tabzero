package com.example.detoxproject.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Accents
val OfflineAccentPrimary = Color(0xFFA5B4FC) // Soft Indigo
val OfflineAccentCyan = Color(0xFF67E8F9)
val OfflineAccentViolet = Color(0xFFC4B5FD)

// Light Palette (Reference 1 Aesthetic)
val OfflineBackgroundLight = Color(0xFFF8FAFC)
val OfflineSurfaceLight = Color(0xFFFFFFFF)
val OfflineSurfaceVariantLight = Color(0xFFF1F5F9)
val OfflineBorderLight = Color(0xFFE2E8F0)
val OfflineTextPrimaryLight = Color(0xFF111827)
val OfflineTextSecondaryLight = Color(0xFF6B7280)
val OfflineTextMutedLight = Color(0xFF9CA3AF)
val OfflineDarkButtonLight = Color(0xFF18181B)

val SoftGreenLight = Color(0xFFECF5E4)
val SoftBlueLight = Color(0xFFE8EBFA)
val SoftBeigeLight = Color(0xFFF0E9DF)
val SoftTealLight = Color(0xFFE1F0EF)
val SoftRoseLight = Color(0xFFFCE7F3)
val SoftAmberLight = Color(0xFFFEF3C7)

// Dark Palette (Night Owl & Zen Landscape Aesthetic)
val OfflineBackgroundDark = Color(0xFF0D1117)
val OfflineSurfaceDark = Color(0xFF161B22)
val OfflineSurfaceVariantDark = Color(0xFF21262D)
val OfflineBorderDark = Color(0xFF30363D)
val OfflineTextPrimaryDark = Color(0xFFF0F6FC)
val OfflineTextSecondaryDark = Color(0xFF8B949E)
val OfflineTextMutedDark = Color(0xFF6E7681)
val OfflineDarkButtonDark = Color(0xFFF0F6FC)

val SoftGreenDark = Color(0xFF1C2E22)
val SoftBlueDark = Color(0xFF1E2540)
val SoftBeigeDark = Color(0xFF2E261E)
val SoftTealDark = Color(0xFF182E30)
val SoftRoseDark = Color(0xFF381C28)
val SoftAmberDark = Color(0xFF382E18)

// Dynamic Composable Getters respecting ThemeManager
val OfflineBackground: Color
    @Composable get() = if (ThemeManager.isDark()) OfflineBackgroundDark else OfflineBackgroundLight

val OfflineSurface: Color
    @Composable get() = if (ThemeManager.isDark()) OfflineSurfaceDark else OfflineSurfaceLight

val OfflineSurfaceVariant: Color
    @Composable get() = if (ThemeManager.isDark()) OfflineSurfaceVariantDark else OfflineSurfaceVariantLight

val OfflineBorder: Color
    @Composable get() = if (ThemeManager.isDark()) OfflineBorderDark else OfflineBorderLight

val OfflineTextPrimary: Color
    @Composable get() = if (ThemeManager.isDark()) OfflineTextPrimaryDark else OfflineTextPrimaryLight

val OfflineTextSecondary: Color
    @Composable get() = if (ThemeManager.isDark()) OfflineTextSecondaryDark else OfflineTextSecondaryLight

val OfflineTextMuted: Color
    @Composable get() = if (ThemeManager.isDark()) OfflineTextMutedDark else OfflineTextMutedLight

val OfflineDarkButton: Color
    @Composable get() = if (ThemeManager.isDark()) OfflineDarkButtonDark else OfflineDarkButtonLight

val OfflineOnDarkButton: Color
    @Composable get() = if (ThemeManager.isDark()) Color(0xFF0F172A) else Color.White

val SoftGreen: Color
    @Composable get() = if (ThemeManager.isDark()) SoftGreenDark else SoftGreenLight

val SoftBlue: Color
    @Composable get() = if (ThemeManager.isDark()) SoftBlueDark else SoftBlueLight

val SoftBeige: Color
    @Composable get() = if (ThemeManager.isDark()) SoftBeigeDark else SoftBeigeLight

val SoftTeal: Color
    @Composable get() = if (ThemeManager.isDark()) SoftTealDark else SoftTealLight

val SoftRose: Color
    @Composable get() = if (ThemeManager.isDark()) SoftRoseDark else SoftRoseLight

val SoftAmber: Color
    @Composable get() = if (ThemeManager.isDark()) SoftAmberDark else SoftAmberLight
