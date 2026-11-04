package com.example.detoxproject.domain.engine

import com.example.detoxproject.data.local.dao.GraceDayDao
import com.example.detoxproject.data.local.entity.GraceDayEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields

data class WeeklyGraceStatus(
    val year: Int,
    val weekNumber: Int,
    val isGraceUsedThisWeek: Boolean,
    val protectedDate: String? = null,
    val remainingGraceDays: Int // 1 if unused, 0 if used
)

class GraceDayManager(
    private val graceDayDao: GraceDayDao,
    private val userId: Long = 1L
) {
    companion object {
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }

    /**
     * Retrieves the current week's grace status (Monday through Sunday).
     */
    suspend fun getGraceStatus(referenceDate: LocalDate = LocalDate.now()): WeeklyGraceStatus {
        val weekFields = WeekFields.ISO
        val year = referenceDate.get(weekFields.weekBasedYear())
        val weekNum = referenceDate.get(weekFields.weekOfWeekBasedYear())

        val record = graceDayDao.getGraceDayForWeekDirect(userId, year, weekNum)
        return WeeklyGraceStatus(
            year = year,
            weekNumber = weekNum,
            isGraceUsedThisWeek = record != null,
            protectedDate = record?.protectedDate,
            remainingGraceDays = if (record != null) 0 else 1
        )
    }

    /**
     * Checks whether [date] is protected by grace.
     */
    suspend fun isDateProtected(date: LocalDate): Boolean {
        val dateStr = date.format(DATE_FORMATTER)
        val record = graceDayDao.getGraceDayForDateDirect(userId, dateStr)
        return record != null
    }

    suspend fun consumeGraceDay(date: LocalDate, reason: String = "Manual Grace Day Protection"): Boolean {
        return tryConsumeGrace(date, reason)
    }

    /**
     * Attempts to consume the shared weekly grace day to protect [missedDate].
     *
     * Rules:
     * - Only past or present dates (never future).
     * - Only one date per calendar week (Monday to Sunday).
     * - Returns true if grace was granted or was already granted for this date.
     * - Returns false if the week's grace was already consumed by a different date.
     */
    suspend fun tryConsumeGrace(missedDate: LocalDate, reason: String = "Automatic streak grace"): Boolean {
        val today = LocalDate.now()
        if (missedDate.isAfter(today)) return false

        val weekFields = WeekFields.ISO
        val year = missedDate.get(weekFields.weekBasedYear())
        val weekNum = missedDate.get(weekFields.weekOfWeekBasedYear())
        val dateStr = missedDate.format(DATE_FORMATTER)

        val existingForWeek = graceDayDao.getGraceDayForWeekDirect(userId, year, weekNum)
        if (existingForWeek != null) {
            // If already protected for this exact date, success
            return existingForWeek.protectedDate == dateStr
        }

        val entity = GraceDayEntity(
            userId = userId,
            year = year,
            weekNumber = weekNum,
            protectedDate = dateStr,
            consumedAt = System.currentTimeMillis(),
            reason = reason
        )
        val insertedId = graceDayDao.insertGraceDay(entity)
        return insertedId > 0L
    }
}
