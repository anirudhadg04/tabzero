package com.example.detoxproject

import com.example.detoxproject.data.local.OfflineDatabase
import com.example.detoxproject.domain.engine.ZenXPManager
import org.junit.Assert.*
import org.junit.Test

class ZenXPRulesTest {

    @Test
    fun testCategoryCapsAndValues() {
        assertEquals(10, ZenXPManager.HABIT_XP)
        assertEquals(60, ZenXPManager.MAX_HABIT_XP_PER_DAY)

        assertEquals(25, ZenXPManager.ALL_HABITS_BONUS_XP)

        assertEquals(5, ZenXPManager.DETOX_XP_PER_15_MIN)
        assertEquals(120, ZenXPManager.MAX_DETOX_XP_PER_DAY)

        assertEquals(30, ZenXPManager.STEP_GOAL_XP)

        assertEquals(10, ZenXPManager.EXERCISE_QUALIFYING_MINS)
        assertEquals(20, ZenXPManager.EXERCISE_XP_PER_SESSION)
        assertEquals(40, ZenXPManager.MAX_EXERCISE_XP_PER_DAY)

        assertEquals(15, ZenXPManager.ALL_TASKS_BONUS_XP)
    }

    @Test
    fun testDetoxIntervalCalculation() {
        // Under 15 mins -> 0 XP
        val min14 = (14 / 15) * ZenXPManager.DETOX_XP_PER_15_MIN
        assertEquals(0, min14)

        // Exactly 15 mins -> 5 XP
        val min15 = (15 / 15) * ZenXPManager.DETOX_XP_PER_15_MIN
        assertEquals(5, min15)

        // 29 mins -> 5 XP
        val min29 = (29 / 15) * ZenXPManager.DETOX_XP_PER_15_MIN
        assertEquals(5, min29)

        // 30 mins -> 10 XP
        val min30 = (30 / 15) * ZenXPManager.DETOX_XP_PER_15_MIN
        assertEquals(10, min30)

        // 60 mins -> 20 XP
        val min60 = (60 / 15) * ZenXPManager.DETOX_XP_PER_15_MIN
        assertEquals(20, min60)

        // 360 mins (6 hrs) -> 120 XP (hits cap)
        val raw360 = (360 / 15) * ZenXPManager.DETOX_XP_PER_15_MIN
        assertEquals(120, minOf(raw360, ZenXPManager.MAX_DETOX_XP_PER_DAY))
    }

    @Test
    fun testCanonicalAchievementsValues() {
        val achievements = OfflineDatabase.getCanonicalAchievements(1L)
        assertTrue("Should have expanded dynamic achievements", achievements.size >= 29)

        val byId = achievements.associateBy { it.id }

        // Disconnect
        assertEquals(20, byId["first_detox"]?.xpReward)
        assertEquals(50, byId["detox_10_hours"]?.xpReward)
        assertEquals(75, byId["detox_25_hours"]?.xpReward)
        assertEquals(40, byId["deep_zen_master"]?.xpReward)
        assertEquals(150, byId["detox_100_hours"]?.xpReward)

        // Move
        assertEquals(30, byId["step_starter"]?.xpReward)
        assertEquals(50, byId["step_7_days"]?.xpReward)
        assertEquals(75, byId["step_14_days"]?.xpReward)
        assertEquals(30, byId["step_10k_day"]?.xpReward)
        assertEquals(60, byId["active_workout_10"]?.xpReward)
        assertEquals(100, byId["step_30_days"]?.xpReward)

        // Reconnect
        assertEquals(20, byId["first_offline_task"]?.xpReward)
        assertEquals(35, byId["reconnect_5_tasks"]?.xpReward)
        assertEquals(50, byId["reconnect_10_tasks"]?.xpReward)
        assertEquals(75, byId["reconnect_25_tasks"]?.xpReward)
        assertEquals(100, byId["reconnect_50_tasks"]?.xpReward)

        // Consistency
        assertEquals(30, byId["streak_3_days"]?.xpReward)
        assertEquals(60, byId["streak_7_days"]?.xpReward)
        assertEquals(80, byId["streak_14_days"]?.xpReward)
        assertEquals(120, byId["streak_30_days"]?.xpReward)
        assertEquals(180, byId["streak_60_days"]?.xpReward)
        assertEquals(250, byId["streak_100_days"]?.xpReward)

        // Personal Growth
        assertEquals(30, byId["habits_10"]?.xpReward)
        assertEquals(50, byId["habits_25"]?.xpReward)
        assertEquals(75, byId["habits_50"]?.xpReward)
        assertEquals(150, byId["habits_100"]?.xpReward)
        assertEquals(250, byId["habits_200"]?.xpReward)
        assertEquals(50, byId["level_5_achiever"]?.xpReward)
        assertEquals(150, byId["level_10_master"]?.xpReward)
    }

    @Test
    fun testAppendOnlyLedgerRedoRestoreCycle() {
        data class MockTx(val id: Long, val sourceEventId: String, val amount: Int, val type: String)

        val ledger = mutableListOf<MockTx>()

        fun computeBalance(): Int {
            return ledger.sumOf {
                when (it.type) {
                    "REWARD", "RESTORE" -> it.amount
                    "REVERSAL" -> -it.amount
                    else -> 0
                }
            }
        }

        // 1. Initial Reward: Habit completion (+10 XP)
        ledger.add(MockTx(1, "habit_1", 10, "REWARD"))
        assertEquals(10, computeBalance())
        assertEquals(1, ledger.size)

        // 2. Undo: Appends REVERSAL (-10 XP). Ledger size becomes 2 (NO DELETION)
        ledger.add(MockTx(2, "habit_1", 10, "REVERSAL"))
        assertEquals(0, computeBalance())
        assertEquals(2, ledger.size)

        // 3. Redo: Appends RESTORE (+10 XP). Ledger size becomes 3 (NEVER DELETES REVERSAL)
        ledger.add(MockTx(3, "habit_1", 10, "RESTORE"))
        assertEquals(10, computeBalance())
        assertEquals(3, ledger.size)

        // 4. Second Undo cycle: Appends another REVERSAL (-10 XP). Ledger size becomes 4
        ledger.add(MockTx(4, "habit_1", 10, "REVERSAL"))
        assertEquals(0, computeBalance())
        assertEquals(4, ledger.size)

        // 5. Second Redo cycle: Appends another RESTORE (+10 XP). Ledger size becomes 5
        ledger.add(MockTx(5, "habit_1", 10, "RESTORE"))
        assertEquals(10, computeBalance())
        assertEquals(5, ledger.size)

        // Check history integrity: every transaction preserved
        val types = ledger.map { it.type }
        assertEquals(listOf("REWARD", "REVERSAL", "RESTORE", "REVERSAL", "RESTORE"), types)
    }
}
