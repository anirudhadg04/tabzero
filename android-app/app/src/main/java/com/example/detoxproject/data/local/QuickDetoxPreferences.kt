package com.example.detoxproject.data.local

import android.content.Context

/**
 * Manages persisted user preferences for Fast / Quick Detox mode.
 * Stores the specific list of restricted packages to block during quick detox.
 */
object QuickDetoxPreferences {
    private const val PREFS_NAME = "quick_detox_preferences"
    private const val KEY_RESTRICTED_PACKAGES = "quick_detox_restricted_packages"
    private const val KEY_DURATION_SECONDS = "quick_detox_duration_seconds"

    const val DEFAULT_DURATION_SECONDS = 30 * 60 // 30 minutes default (1800s)

    val DEFAULT_PACKAGES = listOf(
        "com.instagram.android",
        "com.whatsapp",
        "com.google.android.youtube",
        "com.facebook.katana",
        "com.zhiliaoapp.musically",
        "com.twitter.android",
        "com.snapchat.android"
    )

    fun getRestrictedPackages(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_RESTRICTED_PACKAGES, null)
        return if (!raw.isNullOrBlank()) {
            raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
        } else {
            DEFAULT_PACKAGES
        }
    }

    fun setRestrictedPackages(context: Context, packages: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_RESTRICTED_PACKAGES, packages.joinToString(",")).apply()
    }

    fun addRestrictedPackage(context: Context, packageName: String) {
        val current = getRestrictedPackages(context).toMutableList()
        if (!current.contains(packageName)) {
            current.add(packageName)
            setRestrictedPackages(context, current)
        }
    }

    fun removeRestrictedPackage(context: Context, packageName: String) {
        val current = getRestrictedPackages(context).toMutableList()
        if (current.remove(packageName)) {
            setRestrictedPackages(context, current)
        }
    }

    fun getDurationSeconds(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_DURATION_SECONDS, DEFAULT_DURATION_SECONDS).coerceAtLeast(5)
    }

    fun setDurationSeconds(context: Context, seconds: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_DURATION_SECONDS, seconds.coerceAtLeast(5)).apply()
    }
}
