package com.example.detoxproject.domain.engine

import com.example.detoxproject.data.local.entity.DetoxSessionEntity
import com.example.detoxproject.data.local.entity.HabitCompletionEntity
import com.example.detoxproject.data.local.entity.HabitEntity
import com.example.detoxproject.data.local.entity.PlannedActivityEntity
import com.example.detoxproject.data.local.entity.PledgeEntity
import com.example.detoxproject.data.local.entity.UserGoalsEntity
import org.json.JSONObject

object UndoRedoSerializer {

    fun serializeSession(s: DetoxSessionEntity): String {
        return JSONObject().apply {
            put("id", s.id)
            put("userId", s.userId)
            put("pledgeId", s.pledgeId ?: JSONObject.NULL)
            put("title", s.title)
            put("startTime", s.startTime)
            put("endTime", s.endTime ?: JSONObject.NULL)
            put("targetMinutes", s.targetMinutes)
            put("targetSeconds", s.targetSeconds)
            put("actualMinutes", s.actualMinutes)
            put("status", s.status)
            put("pausedAt", s.pausedAt ?: JSONObject.NULL)
            put("accumulatedPauseMs", s.accumulatedPauseMs)
            put("category", s.category)
            put("restrictedPackages", s.restrictedPackages)
            put("allowAudio", s.allowAudio)
            put("sessionDate", s.sessionDate)
        }.toString()
    }

    fun deserializeSession(jsonStr: String): DetoxSessionEntity {
        val o = JSONObject(jsonStr)
        return DetoxSessionEntity(
            id = o.getLong("id"),
            userId = o.optLong("userId", 1L),
            pledgeId = if (o.isNull("pledgeId")) null else o.getLong("pledgeId"),
            title = o.getString("title"),
            startTime = o.getLong("startTime"),
            endTime = if (o.isNull("endTime")) null else o.getLong("endTime"),
            targetMinutes = o.getInt("targetMinutes"),
            targetSeconds = o.optInt("targetSeconds", o.getInt("targetMinutes") * 60),
            actualMinutes = o.getInt("actualMinutes"),
            status = o.getString("status"),
            pausedAt = if (o.isNull("pausedAt")) null else o.getLong("pausedAt"),
            accumulatedPauseMs = o.getLong("accumulatedPauseMs"),
            category = o.getString("category"),
            restrictedPackages = o.getString("restrictedPackages"),
            allowAudio = o.getBoolean("allowAudio"),
            sessionDate = o.getString("sessionDate")
        )
    }

    fun serializePledge(p: PledgeEntity): String {
        return JSONObject().apply {
            put("id", p.id)
            put("userId", p.userId)
            put("title", p.title)
            put("targetMinutes", p.targetMinutes)
            put("category", p.category)
            put("restrictedPackages", p.restrictedPackages)
            put("allowAudio", p.allowAudio)
            put("createdAt", p.createdAt)
            put("isCompleted", p.isCompleted)
        }.toString()
    }

    fun deserializePledge(jsonStr: String): PledgeEntity {
        val o = JSONObject(jsonStr)
        return PledgeEntity(
            id = o.getLong("id"),
            userId = o.optLong("userId", 1L),
            title = o.getString("title"),
            targetMinutes = o.getInt("targetMinutes"),
            category = o.getString("category"),
            restrictedPackages = o.getString("restrictedPackages"),
            allowAudio = o.getBoolean("allowAudio"),
            createdAt = o.getLong("createdAt"),
            isCompleted = o.getBoolean("isCompleted")
        )
    }

    fun serializeGoals(g: UserGoalsEntity): String {
        return JSONObject().apply {
            put("userId", g.userId)
            put("userName", g.userName)
            put("dailySteps", g.dailySteps)
            put("dailyOfflineMinutes", g.dailyOfflineMinutes)
            put("strideLengthMeters", g.strideLengthMeters.toDouble())
            put("targetCalories", g.targetCalories)
            put("maxScreenTimeMinutes", g.maxScreenTimeMinutes)
            put("avatar", g.avatar)
            put("updatedAt", g.updatedAt)
        }.toString()
    }

    fun deserializeGoals(jsonStr: String): UserGoalsEntity {
        val o = JSONObject(jsonStr)
        return UserGoalsEntity(
            userId = o.optLong("userId", 1L),
            userName = o.optString("userName", "User"),
            dailySteps = o.getInt("dailySteps"),
            dailyOfflineMinutes = o.getInt("dailyOfflineMinutes"),
            strideLengthMeters = o.optDouble("strideLengthMeters", 0.75).toFloat(),
            targetCalories = o.optInt("targetCalories", 400),
            maxScreenTimeMinutes = o.optInt("maxScreenTimeMinutes", 240),
            avatar = o.optString("avatar", "🦉"),
            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
        )
    }

    fun serializeActivity(a: PlannedActivityEntity): String {
        return JSONObject().apply {
            put("id", a.id)
            put("userId", a.userId)
            put("title", a.title)
            put("description", a.description)
            put("category", a.category)
            put("durationMinutes", a.durationMinutes)
            put("scheduledDate", a.scheduledDate)
            put("status", a.status)
            put("weatherCondition", a.weatherCondition ?: JSONObject.NULL)
            put("completedAt", a.completedAt ?: JSONObject.NULL)
        }.toString()
    }

    fun deserializeActivity(jsonStr: String): PlannedActivityEntity {
        val o = JSONObject(jsonStr)
        return PlannedActivityEntity(
            id = o.getLong("id"),
            userId = o.optLong("userId", 1L),
            title = o.getString("title"),
            description = o.getString("description"),
            category = o.getString("category"),
            durationMinutes = o.getInt("durationMinutes"),
            scheduledDate = o.getString("scheduledDate"),
            status = o.getString("status"),
            weatherCondition = if (o.isNull("weatherCondition")) null else o.getString("weatherCondition"),
            completedAt = if (o.isNull("completedAt")) null else o.getLong("completedAt")
        )
    }

    fun serializeHabit(h: HabitEntity): String {
        return JSONObject().apply {
            put("id", h.id)
            put("userId", h.userId)
            put("title", h.title)
            put("description", h.description)
            put("category", h.category)
            put("icon", h.icon)
            put("frequency", h.frequency)
            put("targetDays", h.targetDays)
            put("isArchived", h.isArchived)
            put("createdDate", h.createdDate)
            put("createdAt", h.createdAt)
        }.toString()
    }

    fun deserializeHabit(jsonStr: String): HabitEntity {
        val o = JSONObject(jsonStr)
        return HabitEntity(
            id = o.getLong("id"),
            userId = o.optLong("userId", 1L),
            title = o.getString("title"),
            description = o.optString("description", ""),
            category = o.optString("category", "mindfulness"),
            icon = o.optString("icon", "spa"),
            frequency = o.optString("frequency", "DAILY"),
            targetDays = o.optString("targetDays", "1,2,3,4,5,6,7"),
            isArchived = o.optBoolean("isArchived", false),
            createdDate = o.getString("createdDate"),
            createdAt = o.optLong("createdAt", System.currentTimeMillis())
        )
    }

    fun serializeHabitCompletion(c: HabitCompletionEntity): String {
        return JSONObject().apply {
            put("id", c.id)
            put("habitId", c.habitId)
            put("date", c.date)
            put("completedAt", c.completedAt)
        }.toString()
    }

    fun deserializeHabitCompletion(jsonStr: String): HabitCompletionEntity {
        val o = JSONObject(jsonStr)
        return HabitCompletionEntity(
            id = o.optLong("id", 0L),
            habitId = o.getLong("habitId"),
            date = o.getString("date"),
            completedAt = o.optLong("completedAt", System.currentTimeMillis())
        )
    }
}
