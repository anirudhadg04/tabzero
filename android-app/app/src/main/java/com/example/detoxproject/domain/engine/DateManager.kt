package com.example.detoxproject.domain.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Manages the reactive local calendar date.
 * Observes system date, timezone, and clock tick broadcasts to trigger
 * automatic midnight rollover without requiring an application restart.
 */
class DateManager(private val context: Context) {

    private val _currentDate = MutableStateFlow(LocalDate.now())
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val now = LocalDate.now()
            if (now != _currentDate.value) {
                _currentDate.value = now
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_TIME_TICK)
        }
        try {
            context.registerReceiver(receiver, filter)
        } catch (_: Exception) {}
    }

    fun checkDateRollover() {
        val now = LocalDate.now()
        if (now != _currentDate.value) {
            _currentDate.value = now
        }
    }

    fun unregister() {
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {}
    }

    fun getTodayIso(): String = _currentDate.value.format(ISO_FORMATTER)

    companion object {
        val ISO_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        fun toIso(date: LocalDate): String = date.format(ISO_FORMATTER)

        fun fromIso(iso: String): LocalDate = try {
            LocalDate.parse(iso, ISO_FORMATTER)
        } catch (_: Exception) {
            LocalDate.now()
        }

        fun formatFriendly(date: LocalDate, today: LocalDate = LocalDate.now()): String {
            return when (date) {
                today -> "Today, ${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${date.dayOfMonth}"
                today.minusDays(1) -> "Yesterday, ${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${date.dayOfMonth}"
                today.plusDays(1) -> "Tomorrow, ${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${date.dayOfMonth}"
                else -> "${date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())}, ${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${date.dayOfMonth}"
            }
        }
    }
}
