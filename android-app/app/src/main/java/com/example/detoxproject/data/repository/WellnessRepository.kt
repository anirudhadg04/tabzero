package com.example.detoxproject.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.detoxproject.data.local.OfflineDatabase
import com.example.detoxproject.data.local.entity.*
import com.example.detoxproject.data.network.OpenMeteoClient
import com.example.detoxproject.data.network.OpenMeteoWeather
import com.example.detoxproject.data.security.AuthSessionManager
import com.example.detoxproject.data.security.PasswordHasher
import com.example.detoxproject.domain.engine.DetoxTimerEngine
import com.example.detoxproject.domain.engine.DateManager
import com.example.detoxproject.domain.engine.HabitStreakCalculator
import com.example.detoxproject.domain.engine.HabitWithStatus
import com.example.detoxproject.domain.engine.UndoRedoManager
import com.example.detoxproject.domain.engine.UndoRedoSerializer
import com.example.detoxproject.domain.engine.ZenXPManager
import com.example.detoxproject.domain.engine.GraceDayManager
import com.example.detoxproject.domain.engine.WeeklyGraceStatus
import com.example.detoxproject.domain.engine.StreakEngine
import com.example.detoxproject.domain.engine.AllStreaksInfo
import com.example.detoxproject.domain.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class WellnessRepository(
    val database: OfflineDatabase,
    private val authSession: AuthSessionManager
) {
    private val openMeteoClient = OpenMeteoClient()

    // Active User State
    private val _activeUserId = MutableStateFlow<Long?>(authSession.getActiveUserId())
    val activeUserId: StateFlow<Long?> = _activeUserId.asStateFlow()

    val undoRedoManager = UndoRedoManager(database, _activeUserId.value ?: 1L)
    val zenXpManager: ZenXPManager get() = undoRedoManager.zenXpManager
    val graceDayManager: GraceDayManager get() = GraceDayManager(database.graceDayDao(), getCurrentUserId())

    init {
        val initialId = _activeUserId.value ?: authSession.getActiveUserId()
        if (initialId != null) {
            undoRedoManager.updateUserId(initialId)
            CoroutineScope(Dispatchers.IO).launch {
                syncCanonicalAchievements(initialId)
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val totalXp: Flow<Int> = _activeUserId.flatMapLatest { id ->
        if (id != null) database.xpTransactionDao().getTotalBalance(id) else flowOf(0)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getRecentXPTransactions(limit: Int = 50): Flow<List<XPTransactionEntity>> = _activeUserId.flatMapLatest { id ->
        if (id != null) database.xpTransactionDao().getRecentTransactions(id, limit) else flowOf(emptyList())
    }

    // Current User Flow
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentUser: Flow<UserEntity?> = _activeUserId.flatMapLatest { id ->
        if (id != null) database.userDao().getUserByIdFlow(id) else flowOf(null)
    }

    // Local DB Observables Scoped to Active User
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userGoals: Flow<UserGoalsEntity?> = _activeUserId.flatMapLatest { id ->
        if (id != null) database.userGoalsDao().getUserGoals(id) else flowOf(null)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeSession: Flow<DetoxSessionEntity?> = _activeUserId.flatMapLatest { id ->
        if (id != null) database.detoxDao().getActiveSession(id) else flowOf(null)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allSessions: Flow<List<DetoxSessionEntity>> = _activeUserId.flatMapLatest { id ->
        if (id != null) database.detoxDao().getAllSessions(id) else flowOf(emptyList())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allPledges: Flow<List<PledgeEntity>> = _activeUserId.flatMapLatest { id ->
        if (id != null) database.pledgeDao().getAllPledges(id) else flowOf(emptyList())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allActivities: Flow<List<PlannedActivityEntity>> = _activeUserId.flatMapLatest { id ->
        if (id != null) database.plannedActivityDao().getAllActivities(id) else flowOf(emptyList())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getActivitiesForDate(dateIso: String): Flow<List<PlannedActivityEntity>> = _activeUserId.flatMapLatest { id ->
        if (id != null) database.plannedActivityDao().getActivitiesForDate(dateIso, id) else flowOf(emptyList())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allAchievements: Flow<List<AchievementEntity>> = _activeUserId.flatMapLatest { id ->
        if (id != null) database.achievementDao().getAllAchievements(id) else flowOf(emptyList())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allActiveHabits: Flow<List<HabitEntity>> = _activeUserId.flatMapLatest { id ->
        if (id != null) database.habitDao().getAllActiveHabits(id) else flowOf(emptyList())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allHabits: Flow<List<HabitEntity>> = _activeUserId.flatMapLatest { id ->
        if (id != null) database.habitDao().getAllHabits(id) else flowOf(emptyList())
    }

    val weatherCache: Flow<WeatherCacheEntity?> = database.weatherCacheDao().getWeather()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val pastActionsCount: Flow<Int> = _activeUserId.flatMapLatest { id ->
        if (id != null) database.actionHistoryDao().getPastCount(id) else flowOf(0)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val futureActionsCount: Flow<Int> = _activeUserId.flatMapLatest { id ->
        if (id != null) database.actionHistoryDao().getFutureCount(id) else flowOf(0)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val recentActions: Flow<List<ActionHistoryEntity>> = _activeUserId.flatMapLatest { id ->
        if (id != null) database.actionHistoryDao().getRecentActions(id, 5) else flowOf(emptyList())
    }

    suspend fun getRecentActionsDirect(limit: Int = 5): List<ActionHistoryEntity> {
        return database.actionHistoryDao().getRecentActionsDirect(getCurrentUserId(), limit)
    }

    private fun getCurrentUserId(): Long = _activeUserId.value ?: 1L

    suspend fun syncCanonicalAchievements(userId: Long) {
        val existingList = database.achievementDao().getAllAchievementsDirect(userId)
        val existingMap = existingList.associateBy { it.id }.toMutableMap()
        val canonicalList = OfflineDatabase.getCanonicalAchievements(userId)

        val legacyAliases = mapOf(
            "hour_free" to "detox_1_hour_master",
            "one_hour_free" to "detox_1_hour_master",
            "deep_focus" to "deep_zen_master",
            "consistency" to "streak_3_days",
            "consistency_3" to "streak_3_days",
            "reconnect_nature" to "first_offline_task",
            "mindful_reconnect" to "first_offline_task",
            "habit_champion" to "streak_7_days"
        )

        val toUpsert = mutableListOf<AchievementEntity>()

        for (canonical in canonicalList) {
            val directExisting = existingMap[canonical.id]
            val legacyAliasId = legacyAliases.entries.firstOrNull { it.value == canonical.id }?.key
            val legacyExisting = legacyAliasId?.let { existingMap[it] }

            val isUnlocked = (directExisting?.isUnlocked == true) || (legacyExisting?.isUnlocked == true)
            val unlockedAt = directExisting?.unlockedAt ?: legacyExisting?.unlockedAt
            val progress = maxOf(directExisting?.progress ?: 0f, legacyExisting?.progress ?: 0f)

            toUpsert.add(
                canonical.copy(
                    userId = userId,
                    isUnlocked = isUnlocked,
                    unlockedAt = unlockedAt,
                    progress = progress
                )
            )
        }

        // Clean up legacy alias rows
        for (legacyId in legacyAliases.keys) {
            if (existingMap.containsKey(legacyId)) {
                database.achievementDao().deleteAchievementById(userId, legacyId)
            }
        }

        // Upsert all canonical achievements
        database.achievementDao().upsertAchievements(toUpsert)

        // Trigger immediate aggregate recalculation to evaluate progress & unlocks
        undoRedoManager.recalculateAggregates()
    }

    // --- AUTHENTICATION FLOW ---

    suspend fun registerUser(
        name: String,
        email: String,
        password: String,
        confirmPass: String
    ): Result<UserEntity> = database.withTransaction {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim().lowercase()

        if (trimmedName.length < 2) {
            return@withTransaction Result.failure(IllegalArgumentException("Name must be at least 2 characters"))
        }
        if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            return@withTransaction Result.failure(IllegalArgumentException("Please enter a valid email address"))
        }
        if (password.length < 6) {
            return@withTransaction Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }
        if (password != confirmPass) {
            return@withTransaction Result.failure(IllegalArgumentException("Passwords do not match"))
        }

        val existing = database.userDao().getUserByEmail(trimmedEmail)
        if (existing != null) {
            return@withTransaction Result.failure(IllegalArgumentException("An account with this email already exists"))
        }

        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword(password, salt)

        val newUser = UserEntity(
            name = trimmedName,
            email = trimmedEmail,
            passwordHash = hash,
            salt = salt
        )
        val newId = database.userDao().insertUser(newUser)
        val created = newUser.copy(id = newId)

        // Seed default goals for the new user
        database.userGoalsDao().insertOrUpdate(
            UserGoalsEntity(
                userId = newId,
                userName = trimmedName,
                dailySteps = 8000,
                dailyOfflineMinutes = 120
            )
        )

        // Seed initial achievements for the new user
        syncCanonicalAchievements(newId)

        // Seed starter habits & pledges for the new user
        OfflineDatabase.seedStarterHabits(database, newId)
        OfflineDatabase.seedStarterPledges(database, newId)

        // Switch active user
        authSession.setActiveUserId(newId)
        _activeUserId.value = newId
        undoRedoManager.updateUserId(newId)

        Result.success(created)
    }

    suspend fun loginUser(email: String, password: String): Result<UserEntity> {
        val trimmedEmail = email.trim().lowercase()
        val user = database.userDao().getUserByEmail(trimmedEmail)
            ?: return Result.failure(IllegalArgumentException("No account found with this email"))

        val isValid = PasswordHasher.verifyPassword(password, user.salt, user.passwordHash)
        if (!isValid) {
            return Result.failure(IllegalArgumentException("Invalid password"))
        }

        // Transparent migration: if account used legacy plain SHA-256 hash, upgrade to PBKDF2 with fresh salt
        if (PasswordHasher.isLegacyHash(user.passwordHash)) {
            val newSalt = PasswordHasher.generateSalt()
            val newHash = PasswordHasher.hashPassword(password, newSalt)
            database.userDao().updatePasswordByEmail(trimmedEmail, newHash, newSalt)
        }

        authSession.setActiveUserId(user.id)
        _activeUserId.value = user.id
        undoRedoManager.updateUserId(user.id)
        syncCanonicalAchievements(user.id)

        return Result.success(user)
    }

    suspend fun resetPassword(email: String, newPassword: String): Result<Unit> {
        val trimmed = email.trim().lowercase()
        val user = database.userDao().getUserByEmail(trimmed)
            ?: return Result.failure(IllegalArgumentException("No account found for $email"))

        if (newPassword.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }

        val newSalt = PasswordHasher.generateSalt()
        val newHash = PasswordHasher.hashPassword(newPassword, newSalt)
        database.userDao().updatePasswordByEmail(trimmed, newHash, newSalt)
        return Result.success(Unit)
    }

    suspend fun getSecurityQuestionForEmail(email: String): Result<Pair<String, String>> {
        val trimmed = email.trim().lowercase()
        val user = database.userDao().getUserByEmail(trimmed)
            ?: return Result.failure(IllegalArgumentException("No account registered with $email"))
        val goals = database.userGoalsDao().getUserGoalsDirect(user.id)
            ?: UserGoalsEntity(userId = user.id)
        return Result.success(Pair("DAILY_STEPS", "What is your Daily Step Goal?"))
    }

    suspend fun verifySecurityAnswer(email: String, questionKey: String, answer: String): Result<Boolean> {
        val trimmed = email.trim().lowercase()
        val user = database.userDao().getUserByEmail(trimmed)
            ?: return Result.failure(IllegalArgumentException("No account registered with $email"))
        val goals = database.userGoalsDao().getUserGoalsDirect(user.id)
            ?: UserGoalsEntity(userId = user.id)
        val ansInt = answer.trim().toIntOrNull()
            ?: return Result.failure(IllegalArgumentException("Please enter a valid numeric goal value"))

        val isCorrect = when (questionKey) {
            "DAILY_STEPS" -> ansInt == goals.dailySteps
            "DAILY_OFFLINE" -> ansInt == goals.dailyOfflineMinutes
            else -> ansInt == goals.dailySteps || ansInt == goals.dailyOfflineMinutes
        }

        return if (isCorrect) {
            Result.success(true)
        } else {
            Result.failure(IllegalArgumentException("Incorrect answer. That does not match your registered goal."))
        }
    }

    fun logout() {
        authSession.clearSession()
        _activeUserId.value = null
    }

    // --- SESSION MANAGEMENT ENGINE ---

    suspend fun createAndStartSession(
        title: String,
        targetMinutes: Int,
        category: String = "focus",
        restrictedPackages: String = "",
        allowAudio: Boolean = false,
        pledgeId: Long? = null,
        targetSeconds: Int = targetMinutes * 60
    ): Result<DetoxSessionEntity> = database.withTransaction {
        val uid = getCurrentUserId()
        val active = database.detoxDao().getActiveSessionDirect(uid)
        if (active != null) {
            return@withTransaction Result.failure(
                IllegalStateException("An active detox session is already in progress: ${active.title}")
            )
        }

        val effectiveSeconds = if (targetSeconds > 0) targetSeconds else targetMinutes * 60
        if (effectiveSeconds <= 0 || effectiveSeconds > 720 * 60) {
            return@withTransaction Result.failure(
                IllegalArgumentException("Invalid target duration: $effectiveSeconds seconds. Must be between 5 seconds and 12 hours.")
            )
        }

        val cleanedTitle = title.trim().ifBlank { "Mindful Focus" }
        val localDateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        val newSession = DetoxSessionEntity(
            userId = uid,
            pledgeId = pledgeId,
            title = cleanedTitle,
            startTime = System.currentTimeMillis(),
            endTime = null,
            targetMinutes = (effectiveSeconds / 60).coerceAtLeast(1),
            targetSeconds = effectiveSeconds,
            actualMinutes = 0,
            status = "ACTIVE",
            pausedAt = null,
            accumulatedPauseMs = 0L,
            category = category,
            restrictedPackages = restrictedPackages,
            allowAudio = allowAudio,
            sessionDate = localDateStr
        )

        val insertedId = database.detoxDao().insertSession(newSession)
        val created = newSession.copy(id = insertedId)

        undoRedoManager.recordOperation(
            actionType = "CREATE_SESSION",
            description = "Started session: $cleanedTitle ($targetMinutes min)",
            entityType = "SESSION",
            entityId = insertedId,
            beforeJson = null,
            afterJson = UndoRedoSerializer.serializeSession(created)
        )

        Result.success(created)
    }

    suspend fun pauseSession(sessionId: Long): Result<DetoxSessionEntity> = database.withTransaction {
        val session = database.detoxDao().getSessionById(sessionId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Session not found with ID $sessionId"))

        if (session.status != "ACTIVE") {
            return@withTransaction Result.failure(
                IllegalStateException("Cannot pause session in status '${session.status}'. Must be 'ACTIVE'.")
            )
        }

        val updated = session.copy(
            status = "PAUSED",
            pausedAt = System.currentTimeMillis()
        )
        database.detoxDao().updateSession(updated)

        undoRedoManager.recordOperation(
            actionType = "UPDATE_SESSION",
            description = "Paused session: ${session.title}",
            entityType = "SESSION",
            entityId = sessionId,
            beforeJson = UndoRedoSerializer.serializeSession(session),
            afterJson = UndoRedoSerializer.serializeSession(updated)
        )

        Result.success(updated)
    }

    suspend fun resumeSession(sessionId: Long): Result<DetoxSessionEntity> = database.withTransaction {
        val session = database.detoxDao().getSessionById(sessionId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Session not found with ID $sessionId"))

        if (session.status != "PAUSED") {
            return@withTransaction Result.failure(
                IllegalStateException("Cannot resume session in status '${session.status}'. Must be 'PAUSED'.")
            )
        }

        val now = System.currentTimeMillis()
        val currentPauseDuration = if (session.pausedAt != null) (now - session.pausedAt) else 0L
        val totalPausedMs = session.accumulatedPauseMs + currentPauseDuration

        val updated = session.copy(
            status = "ACTIVE",
            pausedAt = null,
            accumulatedPauseMs = totalPausedMs
        )
        database.detoxDao().updateSession(updated)

        undoRedoManager.recordOperation(
            actionType = "UPDATE_SESSION",
            description = "Resumed session: ${session.title}",
            entityType = "SESSION",
            entityId = sessionId,
            beforeJson = UndoRedoSerializer.serializeSession(session),
            afterJson = UndoRedoSerializer.serializeSession(updated)
        )

        Result.success(updated)
    }

    suspend fun completeSession(sessionId: Long): Result<DetoxSessionEntity> = database.withTransaction {
        val session = database.detoxDao().getSessionById(sessionId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Session not found with ID $sessionId"))

        if (session.status != "ACTIVE" && session.status != "PAUSED") {
            return@withTransaction Result.failure(
                IllegalStateException("Cannot complete session in status '${session.status}'.")
            )
        }

        val now = System.currentTimeMillis()
        val elapsedMs = DetoxTimerEngine.calculateElapsedMs(session, now)
        val actualMinutes = (elapsedMs / (1000 * 60)).toInt().coerceAtLeast(1)

        val updated = session.copy(
            status = "COMPLETED",
            endTime = now,
            actualMinutes = actualMinutes,
            pausedAt = null
        )
        database.detoxDao().updateSession(updated)

        // Mark associated pledge as completed if linked
        if (session.pledgeId != null) {
            val pledge = database.pledgeDao().getPledgeById(session.pledgeId)
            if (pledge != null) {
                database.pledgeDao().updatePledge(pledge.copy(isCompleted = true))
            }
        }

        undoRedoManager.recordOperation(
            actionType = "COMPLETE_SESSION",
            description = "Completed detox: ${session.title} ($actualMinutes min)",
            entityType = "SESSION",
            entityId = sessionId,
            beforeJson = UndoRedoSerializer.serializeSession(session),
            afterJson = UndoRedoSerializer.serializeSession(updated)
        )

        // Award Zen XP (5 XP per 15 min, max 120 XP/day)
        zenXpManager.awardDetoxCompletion(
            sessionId = sessionId,
            actualMinutes = actualMinutes,
            date = updated.sessionDate,
            sessionTitle = updated.title.ifBlank { "Detox Session" }
        )

        undoRedoManager.recalculateAggregates()
        Result.success(updated)
    }

    suspend fun cancelSession(sessionId: Long): Result<DetoxSessionEntity> = database.withTransaction {
        val session = database.detoxDao().getSessionById(sessionId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Session not found with ID $sessionId"))

        val now = System.currentTimeMillis()
        val updated = session.copy(
            status = "CANCELLED",
            endTime = now,
            pausedAt = null
        )
        database.detoxDao().updateSession(updated)

        undoRedoManager.recordOperation(
            actionType = "UPDATE_SESSION",
            description = "Cancelled session: ${session.title}",
            entityType = "SESSION",
            entityId = sessionId,
            beforeJson = UndoRedoSerializer.serializeSession(session),
            afterJson = UndoRedoSerializer.serializeSession(updated)
        )

        Result.success(updated)
    }

    suspend fun deleteSession(sessionId: Long): Result<Boolean> = database.withTransaction {
        val session = database.detoxDao().getSessionById(sessionId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Session not found with ID $sessionId"))

        database.detoxDao().deleteSession(session)

        undoRedoManager.recordOperation(
            actionType = "DELETE_SESSION",
            description = "Deleted session: ${session.title}",
            entityType = "SESSION",
            entityId = sessionId,
            beforeJson = UndoRedoSerializer.serializeSession(session),
            afterJson = null
        )

        undoRedoManager.recalculateAggregates()
        Result.success(true)
    }

    // --- PLEDGES ---

    suspend fun createPledge(
        title: String,
        targetMinutes: Int,
        category: String,
        restrictedPackages: String = "",
        allowAudio: Boolean = false
    ): Result<PledgeEntity> = database.withTransaction {
        val uid = getCurrentUserId()
        val cleaned = title.trim().ifBlank { "Daily Detox Pledge" }
        val pledge = PledgeEntity(
            userId = uid,
            title = cleaned,
            targetMinutes = targetMinutes.coerceIn(5, 720),
            category = category,
            restrictedPackages = restrictedPackages,
            allowAudio = allowAudio
        )
        val id = database.pledgeDao().insertPledge(pledge)
        val created = pledge.copy(id = id)

        undoRedoManager.recordOperation(
            actionType = "CREATE_PLEDGE",
            description = "Created pledge: $cleaned ($targetMinutes min)",
            entityType = "PLEDGE",
            entityId = id,
            beforeJson = null,
            afterJson = UndoRedoSerializer.serializePledge(created)
        )

        Result.success(created)
    }

    // --- GOALS ---

    suspend fun updateGoals(
        dailySteps: Int,
        dailyOfflineMinutes: Int,
        targetCalories: Int = 400,
        userName: String = "User",
        maxScreenTimeMinutes: Int = 240,
        avatar: String? = null
    ): Result<UserGoalsEntity> = database.withTransaction {
        val uid = getCurrentUserId()
        val current = database.userGoalsDao().getUserGoalsDirect(uid) ?: UserGoalsEntity(userId = uid)
        val updated = current.copy(
            userId = uid,
            userName = userName.trim().ifBlank { current.userName },
            dailySteps = dailySteps.coerceIn(1000, 50000),
            dailyOfflineMinutes = dailyOfflineMinutes.coerceIn(15, 720),
            targetCalories = targetCalories.coerceIn(50, 5000),
            maxScreenTimeMinutes = maxScreenTimeMinutes.coerceIn(30, 1440),
            avatar = avatar ?: current.avatar,
            updatedAt = System.currentTimeMillis()
        )
        database.userGoalsDao().insertOrUpdate(updated)

        undoRedoManager.recordOperation(
            actionType = "UPDATE_GOALS",
            description = "Updated goals: $dailySteps steps, $dailyOfflineMinutes min offline, $targetCalories kcal",
            entityType = "GOALS",
            entityId = uid,
            beforeJson = UndoRedoSerializer.serializeGoals(current),
            afterJson = UndoRedoSerializer.serializeGoals(updated)
        )

        undoRedoManager.recalculateAggregates()
        Result.success(updated)
    }

    suspend fun updateAvatar(avatar: String): Result<Unit> {
        val uid = getCurrentUserId()
        val current = database.userGoalsDao().getUserGoalsDirect(uid) ?: UserGoalsEntity(userId = uid)
        val updated = current.copy(avatar = avatar, updatedAt = System.currentTimeMillis())
        database.userGoalsDao().insertOrUpdate(updated)
        return Result.success(Unit)
    }

    // --- PLANNED ACTIVITIES ---

    suspend fun addPlannedActivity(
        title: String,
        durationMinutes: Int,
        category: String,
        description: String = ""
    ): Result<PlannedActivityEntity> = database.withTransaction {
        val uid = getCurrentUserId()
        val localDateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val activity = PlannedActivityEntity(
            userId = uid,
            title = title,
            description = description.ifBlank { "Unplugged offline activity" },
            category = category,
            durationMinutes = durationMinutes,
            scheduledDate = localDateStr,
            status = "PLANNED"
        )
        val id = database.plannedActivityDao().insertActivity(activity)
        val created = activity.copy(id = id)

        undoRedoManager.recordOperation(
            actionType = "CREATE_ACTIVITY",
            description = "Added to planner: $title ($durationMinutes min)",
            entityType = "ACTIVITY",
            entityId = id,
            beforeJson = null,
            afterJson = UndoRedoSerializer.serializeActivity(created)
        )

        Result.success(created)
    }

    suspend fun togglePlannedActivity(activityId: Long): Result<PlannedActivityEntity> = database.withTransaction {
        val existing = database.plannedActivityDao().getActivityById(activityId)
            ?: return@withTransaction Result.failure(Exception("Activity not found"))

        val newStatus = if (existing.status == "COMPLETED") "PLANNED" else "COMPLETED"
        val updated = existing.copy(
            status = newStatus,
            completedAt = if (newStatus == "COMPLETED") System.currentTimeMillis() else null
        )
        database.plannedActivityDao().updateActivity(updated)

        undoRedoManager.recordOperation(
            actionType = if (newStatus == "COMPLETED") "COMPLETE_ACTIVITY" else "UNCOMPLETE_ACTIVITY",
            description = "${if (newStatus == "COMPLETED") "Completed" else "Reopened"}: ${existing.title}",
            entityType = "ACTIVITY",
            entityId = activityId,
            beforeJson = UndoRedoSerializer.serializeActivity(existing),
            afterJson = UndoRedoSerializer.serializeActivity(updated)
        )

        if (newStatus == "COMPLETED") {
            val dateTasks = database.plannedActivityDao().getActivitiesForDateDirect(existing.scheduledDate, getCurrentUserId())
            if (dateTasks.isNotEmpty() && dateTasks.all { it.status == "COMPLETED" }) {
                zenXpManager.awardAllTasksBonus(existing.scheduledDate)
            }
        } else {
            zenXpManager.reverseReward("all_tasks_${existing.scheduledDate}", "Task reopened")
        }

        undoRedoManager.recalculateAggregates()
        Result.success(updated)
    }

    suspend fun addTaskForDate(
        title: String,
        durationMinutes: Int,
        category: String,
        description: String = "",
        scheduledDate: String
    ): Result<PlannedActivityEntity> = database.withTransaction {
        val uid = getCurrentUserId()
        val activity = PlannedActivityEntity(
            userId = uid,
            title = title.trim().ifBlank { "Daily Focus Task" },
            description = description.ifBlank { "Scheduled wellness task" },
            category = category,
            durationMinutes = durationMinutes.coerceIn(5, 720),
            scheduledDate = scheduledDate,
            status = "PLANNED"
        )
        val id = database.plannedActivityDao().insertActivity(activity)
        val created = activity.copy(id = id)

        undoRedoManager.recordOperation(
            actionType = "CREATE_ACTIVITY",
            description = "Added task: ${created.title} ($durationMinutes min)",
            entityType = "ACTIVITY",
            entityId = id,
            beforeJson = null,
            afterJson = UndoRedoSerializer.serializeActivity(created)
        )

        Result.success(created)
    }

    suspend fun updatePlannedActivity(activity: PlannedActivityEntity): Result<PlannedActivityEntity> = database.withTransaction {
        val existing = database.plannedActivityDao().getActivityById(activity.id)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Task not found with ID ${activity.id}"))

        database.plannedActivityDao().updateActivity(activity)

        undoRedoManager.recordOperation(
            actionType = "UPDATE_ACTIVITY",
            description = "Updated task: ${activity.title}",
            entityType = "ACTIVITY",
            entityId = activity.id,
            beforeJson = UndoRedoSerializer.serializeActivity(existing),
            afterJson = UndoRedoSerializer.serializeActivity(activity)
        )

        Result.success(activity)
    }

    suspend fun deletePlannedActivity(activityId: Long): Result<Boolean> = database.withTransaction {
        val existing = database.plannedActivityDao().getActivityById(activityId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Task not found with ID $activityId"))

        database.plannedActivityDao().deleteActivityById(activityId)

        undoRedoManager.recordOperation(
            actionType = "DELETE_ACTIVITY",
            description = "Deleted task: ${existing.title}",
            entityType = "ACTIVITY",
            entityId = activityId,
            beforeJson = UndoRedoSerializer.serializeActivity(existing),
            afterJson = null
        )

        Result.success(true)
    }

    // --- RECURRING HABITS ---

    suspend fun createHabit(
        title: String,
        description: String = "",
        category: String = "mindfulness",
        icon: String = "spa",
        frequency: String = "DAILY",
        targetDays: String = "1,2,3,4,5,6,7",
        createdDate: String = ""
    ): Result<HabitEntity> = database.withTransaction {
        val uid = getCurrentUserId()
        val cDate = createdDate.ifBlank { LocalDate.now().format(DateManager.ISO_FORMATTER) }
        val habit = HabitEntity(
            userId = uid,
            title = title.trim().ifBlank { "Daily Habit" },
            description = description.trim(),
            category = category,
            icon = icon,
            frequency = frequency,
            targetDays = targetDays,
            isArchived = false,
            createdDate = cDate
        )
        val id = database.habitDao().insertHabit(habit)
        val created = habit.copy(id = id)

        undoRedoManager.recordOperation(
            actionType = "CREATE_HABIT",
            description = "Created habit: ${created.title}",
            entityType = "HABIT",
            entityId = id,
            beforeJson = null,
            afterJson = UndoRedoSerializer.serializeHabit(created)
        )

        undoRedoManager.recalculateAggregates()
        Result.success(created)
    }

    suspend fun updateHabit(habit: HabitEntity): Result<HabitEntity> = database.withTransaction {
        val existing = database.habitDao().getHabitById(habit.id)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Habit not found with ID ${habit.id}"))

        database.habitDao().updateHabit(habit)

        undoRedoManager.recordOperation(
            actionType = "UPDATE_HABIT",
            description = "Updated habit: ${habit.title}",
            entityType = "HABIT",
            entityId = habit.id,
            beforeJson = UndoRedoSerializer.serializeHabit(existing),
            afterJson = UndoRedoSerializer.serializeHabit(habit)
        )

        undoRedoManager.recalculateAggregates()
        Result.success(habit)
    }

    suspend fun archiveHabit(habitId: Long, isArchived: Boolean): Result<Boolean> = database.withTransaction {
        val existing = database.habitDao().getHabitById(habitId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Habit not found with ID $habitId"))

        val updated = existing.copy(isArchived = isArchived)
        database.habitDao().updateHabit(updated)

        undoRedoManager.recordOperation(
            actionType = if (isArchived) "ARCHIVE_HABIT" else "UNARCHIVE_HABIT",
            description = "${if (isArchived) "Archived" else "Restored"} habit: ${existing.title}",
            entityType = "HABIT",
            entityId = habitId,
            beforeJson = UndoRedoSerializer.serializeHabit(existing),
            afterJson = UndoRedoSerializer.serializeHabit(updated)
        )

        undoRedoManager.recalculateAggregates()
        Result.success(true)
    }

    suspend fun deleteHabit(habitId: Long): Result<Boolean> = database.withTransaction {
        val existing = database.habitDao().getHabitById(habitId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Habit not found with ID $habitId"))

        database.habitDao().deleteHabit(habitId)
        database.habitDao().deleteAllCompletionsForHabit(habitId)

        undoRedoManager.recordOperation(
            actionType = "DELETE_HABIT",
            description = "Deleted habit: ${existing.title}",
            entityType = "HABIT",
            entityId = habitId,
            beforeJson = UndoRedoSerializer.serializeHabit(existing),
            afterJson = null
        )

        undoRedoManager.recalculateAggregates()
        Result.success(true)
    }

    suspend fun toggleHabitCompletion(habitId: Long, date: String): Result<Boolean> = database.withTransaction {
        val habit = database.habitDao().getHabitById(habitId)
            ?: return@withTransaction Result.failure(IllegalArgumentException("Habit not found with ID $habitId"))

        val isCompleted = database.habitDao().isHabitCompleted(habitId, date)
        if (isCompleted) {
            // Once ticked, habits cannot be unticked for the day directly (preserves streak momentum)
            return@withTransaction Result.success(true)
        } else {
            val comp = HabitCompletionEntity(habitId = habitId, date = date, completedAt = System.currentTimeMillis())
            val compId = database.habitDao().insertCompletion(comp)
            val createdComp = comp.copy(id = compId)
            undoRedoManager.recordOperation(
                actionType = "COMPLETE_HABIT",
                description = "Completed habit: ${habit.title}",
                entityType = "HABIT_COMPLETION",
                entityId = habitId,
                beforeJson = null,
                afterJson = UndoRedoSerializer.serializeHabitCompletion(createdComp)
            )

            // Award 10 XP for habit completion (max 60 XP/day)
            zenXpManager.awardHabitCompletion(habitId, date, habit.title)

            // Check if all active scheduled habits for date are completed (award 25 bonus XP)
            val activeHabits = database.habitDao().getAllActiveHabitsDirect(getCurrentUserId())
            val parsedDate = try { LocalDate.parse(date) } catch (_: Exception) { LocalDate.now() }
            val scheduledForDate = activeHabits.filter { HabitStreakCalculator.isScheduledOn(it, parsedDate) }
            val completionsForDate = database.habitDao().getCompletionsForDateDirect(date)
            if (scheduledForDate.isNotEmpty() && scheduledForDate.all { h ->
                    completionsForDate.any { c -> c.habitId == h.id } || h.id == habitId
                }) {
                zenXpManager.awardAllHabitsBonus(date)
            }
        }

        undoRedoManager.recalculateAggregates()
        Result.success(!isCompleted)
    }

    // --- UNIFIED DAILY SUMMARY ---

    fun getUnifiedDailySummary(date: LocalDate): Flow<UnifiedDailySummary> {
        val uid = getCurrentUserId()
        val dateIso = date.format(DateManager.ISO_FORMATTER)
        val today = LocalDate.now()

        val habitsFlow = combine(
            database.habitDao().getAllActiveHabits(uid),
            database.habitDao().getCompletionsForDate(dateIso),
            database.plannedActivityDao().getActivitiesForDate(dateIso, uid)
        ) { habits, completions, tasks ->
            Triple(habits, completions, tasks)
        }

        val activityFlow = combine(
            database.dailyActivityDao().getDailyActivity(dateIso),
            database.detoxDao().getAllSessionsForDate(dateIso, uid),
            database.exerciseDao().getExercisesForDate(dateIso)
        ) { dailyAct, allDetoxSessions, exercises ->
            Triple(dailyAct, allDetoxSessions, exercises)
        }

        val xpAndGoalsFlow = combine(
            database.xpTransactionDao().getTransactionsForDateFlow(uid, dateIso),
            database.userGoalsDao().getUserGoals(uid)
        ) { transactions, goals ->
            Pair(transactions, goals)
        }

        return combine(habitsFlow, activityFlow, xpAndGoalsFlow) { (habits, dayCompletions, tasks), (dailyAct, allDetoxSessions, exercises), (transactions, goals) ->
            val completedHabitIds = dayCompletions.map { it.habitId }.toSet()
            val allCompletions = database.habitDao().getAllCompletionsDirect()

            val scheduledHabits = habits.filter { habit ->
                HabitStreakCalculator.isScheduledOn(habit, date)
            }

            val habitsWithStatus = scheduledHabits.map { habit ->
                val completionsForThisHabit = allCompletions.filter { it.habitId == habit.id }
                val stats = HabitStreakCalculator.calculateStats(habit, completionsForThisHabit, today)
                HabitWithStatus(
                    habit = habit,
                    stats = stats,
                    isCompletedForDate = habit.id in completedHabitIds
                )
            }

            val completedDetox = allDetoxSessions.filter { it.status == "COMPLETED" }
            val totalDetoxMins = completedDetox.sumOf { it.actualMinutes }
            val totalExerciseMins = exercises.sumOf { (it.durationSeconds / 60).toInt() }
            val netXp = transactions.sumOf {
                when (it.transactionType) {
                    "REWARD", "RESTORE" -> it.xpAmount
                    "REVERSAL" -> -it.xpAmount
                    else -> 0
                }
            }

            UnifiedDailySummary(
                date = date,
                dateIso = dateIso,
                displayDate = DateManager.formatFriendly(date, today),
                habits = habitsWithStatus,
                habitsCompletedCount = habitsWithStatus.count { it.isCompletedForDate },
                habitsTotalCount = habitsWithStatus.size,
                tasks = tasks,
                tasksCompletedCount = tasks.count { it.status == "COMPLETED" },
                tasksTotalCount = tasks.size,
                dailyActivity = dailyAct,
                detoxSessions = completedDetox,
                allDetoxSessions = allDetoxSessions,
                totalDetoxMinutes = totalDetoxMins,
                exerciseSessions = exercises,
                totalExerciseMinutes = totalExerciseMins,
                xpTransactions = transactions,
                netXpForDate = netXp,
                stepGoalTarget = goals?.dailySteps ?: 8000,
                isToday = date == today,
                isPast = date.isBefore(today),
                isFuture = date.isAfter(today)
            )
        }
    }

    suspend fun getTrendsAnalyticsDataDirect(
        timeRange: TrendTimeRange,
        endDate: LocalDate = LocalDate.now()
    ): TrendsAnalyticsData {
        val uid = getCurrentUserId()
        val daysCount = timeRange.days
        val startDate = endDate.minusDays((daysCount - 1).toLong())
        val startIso = startDate.format(DateManager.ISO_FORMATTER)
        val endIso = endDate.format(DateManager.ISO_FORMATTER)

        // Previous period of equal duration
        val prevEndDate = startDate.minusDays(1)
        val prevStartDate = prevEndDate.minusDays((daysCount - 1).toLong())
        val prevStartIso = prevStartDate.format(DateManager.ISO_FORMATTER)
        val prevEndIso = prevEndDate.format(DateManager.ISO_FORMATTER)

        val userGoals = database.userGoalsDao().getUserGoalsDirect(uid)
        val stepGoal = userGoals?.dailySteps ?: 8000
        val dailyOfflineTarget = userGoals?.dailyOfflineMinutes ?: 120

        // 1. Movement Trends
        val activities = database.dailyActivityDao().getActivitiesBetweenDirect(startIso, endIso)
        val activitiesByDate = activities.associateBy { it.date }
        val dailySteps = mutableListOf<Pair<LocalDate, Int>>()
        for (i in 0 until daysCount) {
            val d = startDate.plusDays(i.toLong())
            val dIso = d.format(DateManager.ISO_FORMATTER)
            val st = activitiesByDate[dIso]?.steps ?: 0
            dailySteps.add(Pair(d, st))
        }
        val totalSteps = dailySteps.sumOf { it.second }
        val avgDailySteps = if (daysCount > 0) totalSteps / daysCount else 0
        val daysGoalReached = dailySteps.count { it.second >= stepGoal }
        val goalConsistency = if (daysCount > 0) ((daysGoalReached.toFloat() / daysCount) * 100).toInt() else 0

        val prevActivities = database.dailyActivityDao().getActivitiesBetweenDirect(prevStartIso, prevEndIso)
        val prevTotalSteps = prevActivities.sumOf { it.steps }
        val stepsChangePercent = if (prevTotalSteps > 0) {
            ((totalSteps - prevTotalSteps).toFloat() / prevTotalSteps) * 100f
        } else null

        val movementAnalytics = MovementTrendAnalytics(
            dailySteps = dailySteps,
            averageDailySteps = avgDailySteps,
            totalSteps = totalSteps,
            daysGoalReached = daysGoalReached,
            totalDays = daysCount,
            goalConsistencyPercent = goalConsistency,
            prevTotalSteps = prevTotalSteps,
            stepsChangePercent = stepsChangePercent
        )

        // 2. Detox Trends
        val detoxSessions = database.detoxDao().getSessionsBetweenDatesDirect(startIso, endIso, uid)
        val completedDetox = detoxSessions.filter { it.status == "COMPLETED" }
        val detoxByDate = completedDetox.groupBy { it.sessionDate }
        val dailyDetox = mutableListOf<Pair<LocalDate, Int>>()
        for (i in 0 until daysCount) {
            val d = startDate.plusDays(i.toLong())
            val dIso = d.format(DateManager.ISO_FORMATTER)
            val mins = detoxByDate[dIso]?.sumOf { it.actualMinutes } ?: 0
            dailyDetox.add(Pair(d, mins))
        }
        val totalDetoxMins = dailyDetox.sumOf { it.second }
        val avgDailyDetox = if (daysCount > 0) totalDetoxMins / daysCount else 0
        val completedCount = completedDetox.size
        val longestSession = completedDetox.maxOfOrNull { it.actualMinutes } ?: 0
        val daysMeetingDetoxGoal = dailyDetox.count { it.second >= dailyOfflineTarget }
        val detoxGoalConsistency = if (daysCount > 0) ((daysMeetingDetoxGoal.toFloat() / daysCount) * 100).toInt() else 0

        val prevDetoxSessions = database.detoxDao().getSessionsBetweenDatesDirect(prevStartIso, prevEndIso, uid)
        val prevTotalDetox = prevDetoxSessions.filter { it.status == "COMPLETED" }.sumOf { it.actualMinutes }
        val detoxChangePercent = if (prevTotalDetox > 0) {
            ((totalDetoxMins - prevTotalDetox).toFloat() / prevTotalDetox) * 100f
        } else null

        val detoxAnalytics = DetoxTrendAnalytics(
            dailyMinutes = dailyDetox,
            totalDetoxMinutes = totalDetoxMins,
            averageDailyMinutes = avgDailyDetox,
            completedSessionCount = completedCount,
            longestSessionMinutes = longestSession,
            goalConsistencyPercent = detoxGoalConsistency,
            prevTotalDetoxMinutes = prevTotalDetox,
            detoxChangePercent = detoxChangePercent
        )

        // 3. Habit Trends
        val completions = database.habitDao().getCompletionsBetweenDatesDirect(startIso, endIso)
        val completionsByDate = completions.groupBy { it.date }
        val activeHabits = database.habitDao().getAllActiveHabitsDirect(uid)
        var totalScheduledOpps = 0
        var totalCompletedOpps = 0
        val dailyCompletionTrend = mutableListOf<Pair<LocalDate, Float>>()

        for (i in 0 until daysCount) {
            val d = startDate.plusDays(i.toLong())
            val dIso = d.format(DateManager.ISO_FORMATTER)
            val scheduledForDay = activeHabits.count { HabitStreakCalculator.isScheduledOn(it, d) }
            val completedForDay = completionsByDate[dIso]?.size ?: 0
            totalScheduledOpps += scheduledForDay
            totalCompletedOpps += completedForDay.coerceAtMost(scheduledForDay)
            val ratio = if (scheduledForDay > 0) (completedForDay.toFloat() / scheduledForDay).coerceIn(0f, 1f) else 1f
            dailyCompletionTrend.add(Pair(d, ratio))
        }

        val habitCompletionRate = if (totalScheduledOpps > 0) {
            ((totalCompletedOpps.toFloat() / totalScheduledOpps) * 100).toInt().coerceIn(0, 100)
        } else 100

        val habitCompletionCounts = completions.groupBy { it.habitId }
        val topConsistentHabits = activeHabits.map { habit ->
            val count = habitCompletionCounts[habit.id]?.size ?: 0
            Pair(habit.title, count)
        }.sortedByDescending { it.second }.take(5)

        val prevCompletions = database.habitDao().getCompletionsBetweenDatesDirect(prevStartIso, prevEndIso)
        val prevCompletionsByDate = prevCompletions.groupBy { it.date }
        var prevScheduledOpps = 0
        var prevCompletedOpps = 0
        for (i in 0 until daysCount) {
            val d = prevStartDate.plusDays(i.toLong())
            val dIso = d.format(DateManager.ISO_FORMATTER)
            val sched = activeHabits.count { HabitStreakCalculator.isScheduledOn(it, d) }
            val comp = prevCompletionsByDate[dIso]?.size ?: 0
            prevScheduledOpps += sched
            prevCompletedOpps += comp.coerceAtMost(sched)
        }
        val prevRate = if (prevScheduledOpps > 0) ((prevCompletedOpps.toFloat() / prevScheduledOpps) * 100).toInt() else 0
        val habitChangePercent = if (prevRate > 0) {
            ((habitCompletionRate - prevRate).toFloat() / prevRate) * 100f
        } else null

        val habitAnalytics = HabitTrendAnalytics(
            totalScheduledOpportunities = totalScheduledOpps,
            completedOpportunities = totalCompletedOpps,
            completionPercentage = habitCompletionRate,
            dailyCompletionTrend = dailyCompletionTrend,
            topConsistentHabits = topConsistentHabits,
            prevCompletionPercentage = prevRate,
            completionChangePercent = habitChangePercent
        )

        // 4. Exercise Trends
        val exercises = database.exerciseDao().getExercisesBetweenDatesDirect(startIso, endIso)
        val exerciseCount = exercises.size
        val totalExerciseDuration = exercises.sumOf { (it.durationSeconds / 60).toInt() }
        val totalCalories = exercises.sumOf { (it.calories ?: 0f).toInt() }
        val categoryBreakdown = exercises.groupBy { it.activityType }.mapValues { it.value.size }

        val prevExercises = database.exerciseDao().getExercisesBetweenDatesDirect(prevStartIso, prevEndIso)
        val prevExerciseCount = prevExercises.size
        val exerciseChangePercent = if (prevExerciseCount > 0) {
            ((exerciseCount - prevExerciseCount).toFloat() / prevExerciseCount) * 100f
        } else null

        val exerciseAnalytics = ExerciseTrendAnalytics(
            sessionCount = exerciseCount,
            totalDurationMinutes = totalExerciseDuration,
            activeCaloriesBurned = totalCalories,
            categoryBreakdown = categoryBreakdown,
            prevSessionCount = prevExerciseCount,
            sessionChangePercent = exerciseChangePercent
        )

        // 5. Zen XP Trends
        val transactions = database.xpTransactionDao().getTransactionsBetweenDatesDirect(startIso, endIso, uid)
        val totalEarned = transactions.filter { it.transactionType == "REWARD" }.sumOf { it.xpAmount }
        val totalReversed = transactions.filter { it.transactionType == "REVERSAL" }.sumOf { it.xpAmount }
        val totalRestored = transactions.filter { it.transactionType == "RESTORE" }.sumOf { it.xpAmount }
        val netChange = totalEarned + totalRestored - totalReversed

        val sourceBreakdown = transactions.groupBy { it.sourceEventType }.mapValues { (_, txs) ->
            txs.sumOf {
                when (it.transactionType) {
                    "REWARD", "RESTORE" -> it.xpAmount
                    "REVERSAL" -> -it.xpAmount
                    else -> 0
                }
            }
        }

        val txByDate = transactions.groupBy { it.date }
        val dailyNetXP = mutableListOf<Pair<LocalDate, Int>>()
        for (i in 0 until daysCount) {
            val d = startDate.plusDays(i.toLong())
            val dIso = d.format(DateManager.ISO_FORMATTER)
            val dayTxs = txByDate[dIso] ?: emptyList()
            val dayNet = dayTxs.sumOf {
                when (it.transactionType) {
                    "REWARD", "RESTORE" -> it.xpAmount
                    "REVERSAL" -> -it.xpAmount
                    else -> 0
                }
            }
            dailyNetXP.add(Pair(d, dayNet))
        }

        val prevTransactions = database.xpTransactionDao().getTransactionsBetweenDatesDirect(prevStartIso, prevEndIso, uid)
        val prevNet = prevTransactions.sumOf {
            when (it.transactionType) {
                "REWARD", "RESTORE" -> it.xpAmount
                "REVERSAL" -> -it.xpAmount
                else -> 0
            }
        }
        val xpChangePercent = if (prevNet != 0) {
            ((netChange - prevNet).toFloat() / kotlin.math.abs(prevNet)) * 100f
        } else null

        val xpAnalytics = XPTrendAnalytics(
            totalEarned = totalEarned,
            totalReversed = totalReversed,
            totalRestored = totalRestored,
            netChange = netChange,
            sourceBreakdown = sourceBreakdown,
            dailyNetXP = dailyNetXP,
            prevNetChange = prevNet,
            netXPChangePercent = xpChangePercent
        )

        return TrendsAnalyticsData(
            timeRange = timeRange,
            startDate = startDate,
            endDate = endDate,
            movement = movementAnalytics,
            detox = detoxAnalytics,
            habits = habitAnalytics,
            exercise = exerciseAnalytics,
            xp = xpAnalytics
        )
    }

    // --- CALENDAR & HISTORY AGGREGATES ---

    suspend fun getMonthCalendarDataDirect(yearMonth: YearMonth, selectedDate: LocalDate): CalendarMonthData {
        val today = LocalDate.now()
        val startOfMonth = yearMonth.atDay(1)
        val endOfMonth = yearMonth.atEndOfMonth()
        val startIso = startOfMonth.format(DateManager.ISO_FORMATTER)
        val endIso = endOfMonth.format(DateManager.ISO_FORMATTER)

        val completions = database.habitDao().getCompletionsBetweenDatesDirect(startIso, endIso)
        val completionsByDate = completions.groupBy { it.date }

        val detoxSessions = database.detoxDao().getAllCompletedSessionsDirect(getCurrentUserId())
        val detoxByDate = detoxSessions.groupBy { it.sessionDate }

        val activeHabits = database.habitDao().getAllActiveHabitsDirect(getCurrentUserId())

        val daysList = mutableListOf<CalendarDayInfo>()

        // Leading blank padding days to align Monday as first column (1 = Monday, 7 = Sunday)
        val leadingBlanks = startOfMonth.dayOfWeek.value - 1
        for (i in leadingBlanks downTo 1) {
            val prevDate = startOfMonth.minusDays(i.toLong())
            daysList.add(
                CalendarDayInfo(
                    date = prevDate,
                    dayOfMonth = prevDate.dayOfMonth,
                    isCurrentMonth = false,
                    isToday = prevDate == today,
                    isSelected = prevDate == selectedDate,
                    isFuture = prevDate.isAfter(today)
                )
            )
        }

        var totalActiveDays = 0
        var totalMonthlyFocusMinutes = 0
        var totalMonthlySteps = 0
        var totalScheduledHabits = 0
        var totalCompletedHabits = 0

        for (day in 1..yearMonth.lengthOfMonth()) {
            val date = yearMonth.atDay(day)
            val dateIso = date.format(DateManager.ISO_FORMATTER)
            val isFuture = date.isAfter(today)

            val dayComps = completionsByDate[dateIso].orEmpty()
            val dayDetox = detoxByDate[dateIso].orEmpty()
            val dayDetoxMins = dayDetox.sumOf { it.actualMinutes }
            val dailyAct = database.dailyActivityDao().getDailyActivityDirect(dateIso)
            val steps = dailyAct?.steps ?: 0

            val hasHabits = dayComps.isNotEmpty()
            val hasDetox = dayDetoxMins > 0
            val hasFitness = steps > 0

            val scheduledForDay = activeHabits.count { HabitStreakCalculator.isScheduledOn(it, date) }
            val score = if (scheduledForDay > 0) (dayComps.size.toFloat() / scheduledForDay).coerceIn(0f, 1f) else if (hasDetox || hasFitness) 1f else 0f

            if (!isFuture && (hasHabits || hasDetox || hasFitness)) {
                totalActiveDays++
            }

            if (!isFuture) {
                totalMonthlyFocusMinutes += dayDetoxMins
                totalMonthlySteps += steps
                totalScheduledHabits += scheduledForDay
                totalCompletedHabits += dayComps.size
            }

            daysList.add(
                CalendarDayInfo(
                    date = date,
                    dayOfMonth = day,
                    isCurrentMonth = true,
                    isToday = date == today,
                    isSelected = date == selectedDate,
                    isFuture = isFuture,
                    hasHabitActivity = hasHabits,
                    hasDetoxActivity = hasDetox,
                    hasFitnessActivity = hasFitness,
                    completionScore = score
                )
            )
        }

        // Trailing padding to fill complete 7-day row
        val trailingBlanks = (7 - (daysList.size % 7)) % 7
        for (i in 1..trailingBlanks) {
            val nextDate = endOfMonth.plusDays(i.toLong())
            daysList.add(
                CalendarDayInfo(
                    date = nextDate,
                    dayOfMonth = nextDate.dayOfMonth,
                    isCurrentMonth = false,
                    isToday = nextDate == today,
                    isSelected = nextDate == selectedDate,
                    isFuture = nextDate.isAfter(today)
                )
            )
        }

        val consistencyPercent = if (totalScheduledHabits > 0) {
            ((totalCompletedHabits.toFloat() / totalScheduledHabits) * 100).toInt().coerceIn(0, 100)
        } else if (totalActiveDays > 0) 100 else 0

        return CalendarMonthData(
            yearMonth = yearMonth,
            days = daysList,
            totalActiveDays = totalActiveDays,
            monthlyConsistencyPercent = consistencyPercent,
            totalMonthlyFocusMinutes = totalMonthlyFocusMinutes,
            totalMonthlySteps = totalMonthlySteps
        )
    }

    suspend fun getWeeklyAnalyticsDataDirect(endDate: LocalDate): WeeklyAnalyticsData {
        val today = LocalDate.now()
        val days = mutableListOf<WeeklyTrendDay>()
        var totalDetox = 0
        var totalSteps = 0
        var totalScheduled = 0
        var totalCompleted = 0

        val activeHabits = database.habitDao().getAllActiveHabitsDirect(getCurrentUserId())

        for (i in 6 downTo 0) {
            val date = endDate.minusDays(i.toLong())
            val dateIso = date.format(DateManager.ISO_FORMATTER)
            val dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

            val dayDetoxMins = database.detoxDao().getOfflineMinutesForDateDirect(dateIso, getCurrentUserId()) ?: 0
            val dailyAct = database.dailyActivityDao().getDailyActivityDirect(dateIso)
            val steps = dailyAct?.steps ?: 0

            val scheduledCount = activeHabits.count { HabitStreakCalculator.isScheduledOn(it, date) }
            val completedCount = database.habitDao().getCompletionsForDateDirect(dateIso).size

            totalDetox += dayDetoxMins
            totalSteps += steps
            totalScheduled += scheduledCount
            totalCompleted += completedCount

            days.add(
                WeeklyTrendDay(
                    date = date,
                    dayLabel = dayLabel,
                    detoxMinutes = dayDetoxMins,
                    steps = steps,
                    habitsCompleted = completedCount,
                    habitsTotal = scheduledCount,
                    isToday = date == today
                )
            )
        }

        val rate = if (totalScheduled > 0) ((totalCompleted.toFloat() / totalScheduled) * 100).toInt().coerceIn(0, 100) else 100

        return WeeklyAnalyticsData(
            days = days,
            averageFocusMinutesPerDay = totalDetox / 7,
            averageStepsPerDay = totalSteps / 7,
            weeklyHabitCompletionRate = rate
        )
    }

    // --- UNDO / REDO ---

    suspend fun undo(): Boolean = undoRedoManager.undo()

    suspend fun redo(): Boolean = undoRedoManager.redo()

    // --- DIRECT OPEN-METEO WEATHER INTEGRATION ---

    suspend fun syncWeather(): Boolean {
        val result = openMeteoClient.fetchCurrentWeather()
        if (result.isSuccess) {
            val weather = result.getOrNull()!!
            database.weatherCacheDao().saveWeather(
                WeatherCacheEntity(
                    locationKey = "default",
                    locationName = weather.locationName,
                    temperature = weather.temperature,
                    weatherCode = weather.weatherCode,
                    humidity = weather.humidity,
                    windSpeed = weather.windSpeed,
                    precipitationProbability = 0,
                    conditionText = weather.conditionText,
                    isCached = false,
                    fetchedAt = System.currentTimeMillis()
                )
            )
            return true
        }
        return false
    }

    // --- PHASE 5: ZEN XP, STREAKS & GRACE DAYS ---

    suspend fun getGraceStatus(referenceDate: LocalDate = LocalDate.now()): WeeklyGraceStatus {
        return graceDayManager.getGraceStatus(referenceDate)
    }

    suspend fun consumeGraceDay(date: LocalDate, reason: String = "Protected via Weekly Grace Day"): Boolean {
        val result = graceDayManager.consumeGraceDay(date, reason)
        if (result) {
            undoRedoManager.recalculateAggregates()
        }
        return result
    }

    suspend fun calculateAllStreaks(referenceDate: LocalDate = LocalDate.now()): AllStreaksInfo {
        val uid = getCurrentUserId()
        val habits = database.habitDao().getAllActiveHabitsDirect(uid)
        val habitCompletions = database.habitDao().getAllCompletionsDirect()
        val detoxSessions = database.detoxDao().getAllCompletedSessionsDirect(uid)
        val goals = database.userGoalsDao().getUserGoalsDirect(uid)
        val dailyGoalMinutes = goals?.dailyOfflineMinutes ?: 120
        val dailyActivities = database.dailyActivityDao().getAllActivitiesDirect()
        val exerciseSessions = database.exerciseDao().getAllCompletedExercisesDirect()
        val protectedRecords = database.graceDayDao().getAllGraceDaysDirect(uid)
        val protectedDates = protectedRecords.mapNotNull {
            try { LocalDate.parse(it.protectedDate) } catch (_: Exception) { null }
        }.toSet()

        return StreakEngine.calculateAllStreaks(
            habits = habits,
            completions = habitCompletions,
            completedSessions = detoxSessions,
            dailyActivities = dailyActivities,
            completedExercises = exerciseSessions,
            dailyOfflineGoalMinutes = dailyGoalMinutes,
            protectedDates = protectedDates,
            referenceDate = referenceDate
        )
    }

    // --- MINDFUL NOTES ---

    fun getMindfulNotes(): Flow<List<com.example.detoxproject.data.local.entity.MindfulNoteEntity>> =
        database.mindfulNoteDao().getAllNotes()

    suspend fun saveMindfulNote(title: String, content: String, colorHex: String = "#E8F5E9"): Long {
        val note = com.example.detoxproject.data.local.entity.MindfulNoteEntity(
            title = title.trim(),
            content = content.trim(),
            colorHex = colorHex,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val id = database.mindfulNoteDao().insertNote(note)
        undoRedoManager.recalculateAggregates()
        return id
    }

    suspend fun updateMindfulNote(id: Long, title: String, content: String, colorHex: String) {
        val existing = database.mindfulNoteDao().getNoteById(id) ?: return
        val updated = existing.copy(
            title = title.trim(),
            content = content.trim(),
            colorHex = colorHex,
            updatedAt = System.currentTimeMillis()
        )
        database.mindfulNoteDao().updateNote(updated)
    }

    suspend fun deleteMindfulNote(id: Long) {
        database.mindfulNoteDao().deleteNoteById(id)
        undoRedoManager.recalculateAggregates()
    }

    companion object {
        @Volatile
        private var INSTANCE: WellnessRepository? = null

        fun getInstance(context: Context): WellnessRepository {
            return INSTANCE ?: synchronized(this) {
                val db = OfflineDatabase.getInstance(context)
                val auth = AuthSessionManager(context)
                val repo = WellnessRepository(db, auth)
                INSTANCE = repo
                repo
            }
        }
    }
}
