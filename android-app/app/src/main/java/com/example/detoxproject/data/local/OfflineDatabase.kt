package com.example.detoxproject.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.detoxproject.data.local.dao.*
import com.example.detoxproject.data.local.entity.*
import com.example.detoxproject.data.security.PasswordHasher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Database(
    entities = [
        UserEntity::class,
        UserGoalsEntity::class,
        DetoxSessionEntity::class,
        PledgeEntity::class,
        PlannedActivityEntity::class,
        AchievementEntity::class,
        ActionHistoryEntity::class,
        WeatherCacheEntity::class,
        DailyActivityEntity::class,
        ExerciseSessionEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class,
        XPTransactionEntity::class,
        GraceDayEntity::class,
        MindfulNoteEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class OfflineDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun userGoalsDao(): UserGoalsDao
    abstract fun detoxDao(): DetoxDao
    abstract fun pledgeDao(): PledgeDao
    abstract fun plannedActivityDao(): PlannedActivityDao
    abstract fun achievementDao(): AchievementDao
    abstract fun actionHistoryDao(): ActionHistoryDao
    abstract fun weatherCacheDao(): WeatherCacheDao
    abstract fun dailyActivityDao(): DailyActivityDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun habitDao(): HabitDao
    abstract fun xpTransactionDao(): XPTransactionDao
    abstract fun graceDayDao(): GraceDayDao
    abstract fun mindfulNoteDao(): MindfulNoteDao

    companion object {
        @Volatile
        private var INSTANCE: OfflineDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE detox_sessions ADD COLUMN targetSeconds INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_activities (
                        date TEXT PRIMARY KEY NOT NULL,
                        steps INTEGER NOT NULL DEFAULT 0,
                        goalSteps INTEGER NOT NULL DEFAULT 8000,
                        distanceMeters REAL NOT NULL DEFAULT 0.0,
                        activeCalories REAL NOT NULL DEFAULT 0.0,
                        activeMinutes INTEGER NOT NULL DEFAULT 0,
                        source TEXT NOT NULL DEFAULT 'STEP_COUNTER',
                        lastUpdated INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS exercise_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        activityType TEXT NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER,
                        durationSeconds INTEGER NOT NULL DEFAULT 0,
                        distanceMeters REAL,
                        steps INTEGER,
                        calories REAL,
                        notes TEXT,
                        status TEXT NOT NULL DEFAULT 'ACTIVE',
                        sessionDate TEXT NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_goals ADD COLUMN targetCalories INTEGER NOT NULL DEFAULT 400")
                db.execSQL("ALTER TABLE user_goals ADD COLUMN maxScreenTimeMinutes INTEGER NOT NULL DEFAULT 240")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS habits (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL DEFAULT 1,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        category TEXT NOT NULL DEFAULT 'mindfulness',
                        icon TEXT NOT NULL DEFAULT 'spa',
                        frequency TEXT NOT NULL DEFAULT 'DAILY',
                        targetDays TEXT NOT NULL DEFAULT '1,2,3,4,5,6,7',
                        isArchived INTEGER NOT NULL DEFAULT 0,
                        createdDate TEXT NOT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS habit_completions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        habitId INTEGER NOT NULL,
                        date TEXT NOT NULL,
                        completedAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_habit_completions_habitId_date 
                    ON habit_completions (habitId, date)
                """)
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_habit_completions_date 
                    ON habit_completions (date)
                """)
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_habit_completions_habitId 
                    ON habit_completions (habitId)
                """)
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_goals ADD COLUMN avatar TEXT NOT NULL DEFAULT '🦉'")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create xp_transactions table & indexes
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS xp_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL DEFAULT 1,
                        sourceEventId TEXT NOT NULL,
                        sourceEventType TEXT NOT NULL,
                        xpAmount INTEGER NOT NULL,
                        transactionType TEXT NOT NULL,
                        linkedTransactionId INTEGER,
                        date TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        description TEXT NOT NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_xp_transactions_userId ON xp_transactions (userId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_xp_transactions_date ON xp_transactions (date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_xp_transactions_sourceEventId ON xp_transactions (sourceEventId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_xp_transactions_sourceEventType ON xp_transactions (sourceEventType)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_xp_transactions_unique_event ON xp_transactions (userId, sourceEventId, transactionType)")

                // 2. Create grace_days table & indexes
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS grace_days (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId INTEGER NOT NULL DEFAULT 1,
                        year INTEGER NOT NULL,
                        weekNumber INTEGER NOT NULL,
                        protectedDate TEXT NOT NULL,
                        consumedAt INTEGER NOT NULL,
                        reason TEXT NOT NULL DEFAULT ''
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_grace_days_week_user ON grace_days (userId, year, weekNumber)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_grace_days_date_user ON grace_days (userId, protectedDate)")

                // 3. Add category and xpReward to achievements
                db.execSQL("ALTER TABLE achievements ADD COLUMN category TEXT NOT NULL DEFAULT 'DISCONNECT'")
                db.execSQL("ALTER TABLE achievements ADD COLUMN xpReward INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop old composite unique index that prevented repeated undo/redo append cycles
                db.execSQL("DROP INDEX IF EXISTS index_xp_transactions_unique_event")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_xp_transactions_user_source ON xp_transactions (userId, sourceEventId)")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS mindful_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        colorHex TEXT NOT NULL DEFAULT '#E8F5E9',
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }

        fun getInstance(context: Context): OfflineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OfflineDatabase::class.java,
                    "offline_wellness.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getInstance(context)
                                seedDefaultData(database)
                            }
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getInstance(context)
                                seedHabitsIfEmpty(database)
                                seedAchievementsIfEmpty(database)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedStarterHabits(database: OfflineDatabase, userId: Long) {
            val habitCount = database.habitDao().getHabitCount(userId)
            if (habitCount == 0) {
                val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val starterHabits = listOf(
                    HabitEntity(
                        userId = userId,
                        title = "Walk 8,000 steps",
                        description = "Hit your daily step goal for cardio and stamina",
                        category = "fitness",
                        icon = "directions_walk",
                        frequency = "DAILY",
                        targetDays = "1,2,3,4,5,6,7",
                        createdDate = todayStr
                    ),
                    HabitEntity(
                        userId = userId,
                        title = "Unplugged Reading",
                        description = "30 minutes of physical book or offline article reading",
                        category = "reading",
                        icon = "menu_book",
                        frequency = "DAILY",
                        targetDays = "1,2,3,4,5,6,7",
                        createdDate = todayStr
                    ),
                    HabitEntity(
                        userId = userId,
                        title = "Complete Detox Session",
                        description = "At least 30 minutes of phone-free focused quiet time",
                        category = "focus",
                        icon = "timer",
                        frequency = "DAILY",
                        targetDays = "1,2,3,4,5,6,7",
                        createdDate = todayStr
                    ),
                    HabitEntity(
                        userId = userId,
                        title = "Evening Wind-Down",
                        description = "Disconnect from digital screens 45 minutes before sleep",
                        category = "sleep",
                        icon = "bedtime",
                        frequency = "DAILY",
                        targetDays = "1,2,3,4,5,6,7",
                        createdDate = todayStr
                    ),
                    HabitEntity(
                        userId = userId,
                        title = "Mindful Meditation",
                        description = "10 minutes of breathwork and mindful presence",
                        category = "mindfulness",
                        icon = "spa",
                        frequency = "DAILY",
                        targetDays = "1,2,3,4,5,6,7",
                        createdDate = todayStr
                    )
                )
                database.habitDao().insertHabits(starterHabits)
            }
        }

        suspend fun seedStarterPledges(database: OfflineDatabase, userId: Long) {
            database.pledgeDao().insertPledge(
                PledgeEntity(
                    userId = userId,
                    title = "Focused Study",
                    targetMinutes = 45,
                    category = "study",
                    restrictedPackages = "com.instagram.android,com.zhiliaoapp.musically"
                )
            )
            database.pledgeDao().insertPledge(
                PledgeEntity(
                    userId = userId,
                    title = "Phone-Free Evening",
                    targetMinutes = 60,
                    category = "sleep",
                    restrictedPackages = "com.google.android.youtube,com.twitter.android"
                )
            )
        }

        suspend fun seedHabitsIfEmpty(database: OfflineDatabase, userId: Long = 1L) {
            val userCount = database.userDao().getUserCount()
            if (userCount > 0) {
                seedStarterHabits(database, userId)
            }
        }

        suspend fun seedDefaultData(database: OfflineDatabase) {
            // Production clean database: users must explicitly register or authenticate.
            // Starter data (goals, habits, pledges, achievements) is seeded dynamically upon registration.
        }

        fun getCanonicalAchievements(userId: Long = 1L): List<AchievementEntity> = listOf(
            // 1. Disconnect
            AchievementEntity(userId, "first_detox", "First Detox", "Completed your very first detox focus session.", "spa", category = "DISCONNECT", xpReward = 20),
            AchievementEntity(userId, "detox_1_hour_master", "One-Hour Focus Master", "Completed a full 60-minute digital detox focus session in one sitting.", "spa", category = "DISCONNECT", xpReward = 35),
            AchievementEntity(userId, "detox_10_hours", "10 Hours of Stillness", "Accumulated 10 total hours of digital detox.", "timer", category = "DISCONNECT", xpReward = 50),
            AchievementEntity(userId, "detox_25_hours", "Quarter Century", "Accumulated 25 total hours of digital detox.", "hourglass_bottom", category = "DISCONNECT", xpReward = 75),
            AchievementEntity(userId, "deep_zen_master", "Deep Zen Master", "Completed a continuous 2-hour detox session in one sitting.", "self_improvement", category = "DISCONNECT", xpReward = 40),
            AchievementEntity(userId, "detox_100_hours", "Centurion of Zen", "Accumulated 100 total hours of digital detox.", "psychology", category = "DISCONNECT", xpReward = 150),

            // 2. Move
            AchievementEntity(userId, "step_starter", "First Step Goal", "Reached your daily step milestone for the first time.", "directions_walk", category = "MOVE", xpReward = 30),
            AchievementEntity(userId, "workout_starter", "First Workout", "Recorded your very first physical workout or exercise session.", "fitness_center", category = "MOVE", xpReward = 25),
            AchievementEntity(userId, "workout_5_club", "Five-Workout Club", "Recorded 5 physical exercise workouts.", "sports_gymnastics", category = "MOVE", xpReward = 45),
            AchievementEntity(userId, "step_7_days", "Seven Days on Your Feet", "Reached your daily step goal on 7 different days.", "directions_run", category = "MOVE", xpReward = 50),
            AchievementEntity(userId, "step_14_days", "Two-Week Stride", "Reached your daily step goal on 14 different days.", "hiking", category = "MOVE", xpReward = 75),
            AchievementEntity(userId, "step_10k_day", "Ten Thousand Peak", "Reached 10,000 steps in a single day.", "altitude", category = "MOVE", xpReward = 30),
            AchievementEntity(userId, "active_workout_10", "Ten Active Workouts", "Completed 10 recorded physical exercise workouts.", "fitness_center", category = "MOVE", xpReward = 60),
            AchievementEntity(userId, "step_30_days", "Monthly Mover", "Reached your daily step goal on 30 different days.", "sports_gymnastics", category = "MOVE", xpReward = 100),

            // 3. Reconnect
            AchievementEntity(userId, "first_offline_task", "Back to Reality", "Completed your first planned real-world activity.", "nature_people", category = "RECONNECT", xpReward = 20),
            AchievementEntity(userId, "reconnect_5_tasks", "Real-Life Momentum", "Completed 5 planned real-world activities.", "local_florist", category = "RECONNECT", xpReward = 35),
            AchievementEntity(userId, "reconnect_10_tasks", "Living Beyond Screens", "Completed 10 planned real-world activities.", "park", category = "RECONNECT", xpReward = 50),
            AchievementEntity(userId, "reconnect_25_tasks", "Unplugged Explorer", "Completed 25 planned real-world activities.", "explore", category = "RECONNECT", xpReward = 75),
            AchievementEntity(userId, "reconnect_50_tasks", "World Conqueror", "Completed 50 planned real-world activities.", "terrain", category = "RECONNECT", xpReward = 100),

            // 4. Consistency
            AchievementEntity(userId, "streak_3_days", "Three-Day Momentum", "Achieved a 3-day streak across your wellness journeys.", "local_fire_department", category = "CONSISTENCY", xpReward = 30),
            AchievementEntity(userId, "grace_guardian", "Grace Guardian", "Used a weekly grace day to protect and preserve your active wellness streaks.", "shield", category = "CONSISTENCY", xpReward = 25),
            AchievementEntity(userId, "streak_7_days", "Seven-Day Rhythm", "Maintained a solid 7-day wellness streak.", "bolt", category = "CONSISTENCY", xpReward = 60),
            AchievementEntity(userId, "streak_14_days", "Fortnight of Fortitude", "Maintained an unbroken 14-day wellness streak.", "whatshot", category = "CONSISTENCY", xpReward = 80),
            AchievementEntity(userId, "streak_30_days", "Unstoppable Flow", "Built an extraordinary 30-day wellness streak.", "auto_awesome", category = "CONSISTENCY", xpReward = 120),
            AchievementEntity(userId, "streak_60_days", "Two-Month Anchor", "Built a dedicated 60-day wellness streak.", "diamond", category = "CONSISTENCY", xpReward = 180),
            AchievementEntity(userId, "streak_100_days", "Century of Mastery", "Achieved a monumental 100-day wellness streak.", "military_tech", category = "CONSISTENCY", xpReward = 250),

            // 5. Personal Growth
            AchievementEntity(userId, "mindful_note_scribe", "Mindful Scribe", "Wrote your first personal reflection note in More Mindful.", "edit_note", category = "PERSONAL_GROWTH", xpReward = 20),
            AchievementEntity(userId, "mindful_reflector", "Quiet Reflection", "Authored 5 mindful notes and thoughts.", "menu_book", category = "PERSONAL_GROWTH", xpReward = 40),
            AchievementEntity(userId, "habits_10", "Ten Habits Formed", "Completed habits 10 times.", "check_circle", category = "PERSONAL_GROWTH", xpReward = 30),
            AchievementEntity(userId, "habits_25", "Quarter Habit Milestone", "Completed habits 25 times.", "task_alt", category = "PERSONAL_GROWTH", xpReward = 50),
            AchievementEntity(userId, "habits_50", "Fifty Steps Forward", "Completed habits 50 times.", "emoji_events", category = "PERSONAL_GROWTH", xpReward = 75),
            AchievementEntity(userId, "habits_100", "Centennial of Habits", "Completed habits 100 times.", "workspace_premium", category = "PERSONAL_GROWTH", xpReward = 150),
            AchievementEntity(userId, "habits_200", "Master of Daily Habits", "Completed habits 200 times.", "stars", category = "PERSONAL_GROWTH", xpReward = 250),
            AchievementEntity(userId, "level_5_achiever", "Zen Level 5", "Attained Zen Progression Level 5.", "verified", category = "PERSONAL_GROWTH", xpReward = 50),
            AchievementEntity(userId, "level_10_master", "Zen Grandmaster (Level 10)", "Attained Zen Progression Level 10.", "military_tech", category = "PERSONAL_GROWTH", xpReward = 150)
        )

        suspend fun seedAchievementsIfEmpty(database: OfflineDatabase, userId: Long = 1L) {
            val userCount = database.userDao().getUserCount()
            if (userCount > 0) {
                val achievements = getCanonicalAchievements(userId)
                database.achievementDao().insertAll(achievements)
            }
        }
    }
}
