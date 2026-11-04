package com.example.detoxproject.data.security

import android.content.Context
import android.content.SharedPreferences

class AuthSessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tabzero_auth", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACTIVE_USER_ID = "active_user_id"
    }

    fun getActiveUserId(): Long? {
        val id = prefs.getLong(KEY_ACTIVE_USER_ID, -1L)
        return if (id != -1L) id else null
    }

    fun setActiveUserId(userId: Long) {
        prefs.edit().putLong(KEY_ACTIVE_USER_ID, userId).apply()
    }

    fun clearSession() {
        prefs.edit().remove(KEY_ACTIVE_USER_ID).apply()
    }
}
