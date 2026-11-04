package com.example.detoxproject.domain.engine

import com.example.detoxproject.data.local.entity.DetoxSessionEntity

/**
 * Authoritative single source of truth for timer calculations.
 * Relies strictly on wall-clock timestamps and accumulated pause duration,
 * making it 100% resilient to application kills, backgrounding, and device reboots.
 */
object DetoxTimerEngine {

    fun calculateElapsedMs(
        session: DetoxSessionEntity,
        currentTimeMs: Long = System.currentTimeMillis()
    ): Long {
        if (session.status == "COMPLETED" || session.status == "CANCELLED") {
            return session.actualMinutes * 60_000L
        }

        val effectiveEnd = if (session.status == "PAUSED") {
            session.pausedAt ?: currentTimeMs
        } else {
            currentTimeMs
        }

        val elapsed = effectiveEnd - session.startTime - session.accumulatedPauseMs
        return elapsed.coerceAtLeast(0L)
    }

    fun calculateRemainingMs(
        session: DetoxSessionEntity,
        currentTimeMs: Long = System.currentTimeMillis()
    ): Long {
        val targetMs = if (session.targetSeconds > 0) session.targetSeconds * 1000L else session.targetMinutes * 60_000L
        val elapsed = calculateElapsedMs(session, currentTimeMs)
        return (targetMs - elapsed).coerceAtLeast(0L)
    }

    fun calculateProgress(
        session: DetoxSessionEntity,
        currentTimeMs: Long = System.currentTimeMillis()
    ): Float {
        val targetMs = if (session.targetSeconds > 0) session.targetSeconds * 1000L else session.targetMinutes * 60_000L
        if (targetMs <= 0L) return 1f
        val elapsed = calculateElapsedMs(session, currentTimeMs)
        return (elapsed.toFloat() / targetMs.toFloat()).coerceIn(0f, 1f)
    }

    fun isTargetReached(
        session: DetoxSessionEntity,
        currentTimeMs: Long = System.currentTimeMillis()
    ): Boolean {
        return calculateRemainingMs(session, currentTimeMs) <= 0L
    }

    /**
     * Calculates the exact wall-clock timestamp when the active session will complete.
     * Takes accumulated pause duration and second precision into account.
     */
    fun calculateExpectedEndTimeMs(session: DetoxSessionEntity): Long {
        val targetMs = if (session.targetSeconds > 0) session.targetSeconds * 1000L else session.targetMinutes * 60_000L
        return session.startTime + session.accumulatedPauseMs + targetMs
    }

    fun formatMinutesSeconds(remainingMs: Long): String {
        val totalSecs = (remainingMs / 1000L).coerceAtLeast(0L)
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return "%02d:%02d".format(mins, secs)
    }
}
