package com.example.detoxproject.domain.engine

/**
 * Progression state calculated deterministically from the user's cumulative Zen XP balance.
 *
 * Formula:
 * XP_next = 100 + 50 * (L - 1)
 *
 * Cost table:
 * Level 1 -> 2: 100 XP (Cumulative: 100)
 * Level 2 -> 3: 150 XP (Cumulative: 250)
 * Level 3 -> 4: 200 XP (Cumulative: 450)
 * Level 4 -> 5: 250 XP (Cumulative: 700)
 * Level 5 -> 6: 300 XP (Cumulative: 1000)
 * Level 10 -> 11: 550 XP
 * Level 20 -> 21: 1050 XP
 */
data class UserLevelInfo(
    val level: Int,
    val currentXPInLevel: Int,
    val xpNeededForNextLevel: Int,
    val totalCumulativeXP: Int,
    val progress: Float
)

object LevelCalculator {

    /**
     * XP required to advance from [level] to [level + 1].
     */
    fun xpRequiredForNextLevel(level: Int): Int {
        val safeLevel = level.coerceAtLeast(1)
        return 100 + 50 * (safeLevel - 1)
    }

    /**
     * Total cumulative XP required from Level 1 to initially reach [level].
     * Level 1 starts at 0 XP.
     */
    fun cumulativeXpForLevel(level: Int): Int {
        if (level <= 1) return 0
        val n = level - 1
        // Sum_{k=1}^{n} (100 + 50 * (k - 1)) = 100*n + 50*(n*(n-1)/2) = 100*n + 25*n*(n-1)
        return 100 * n + 25 * n * (n - 1)
    }

    /**
     * Resolves level, progress, and thresholds from total cumulative XP balance.
     * Guaranteed deterministic, never resets, never yields negative XP.
     */
    fun calculateLevel(totalXP: Int): UserLevelInfo {
        val safeTotal = totalXP.coerceAtLeast(0)

        var currentLevel = 1
        while (cumulativeXpForLevel(currentLevel + 1) <= safeTotal) {
            currentLevel++
        }

        val baseXPForCurrentLevel = cumulativeXpForLevel(currentLevel)
        val currentXPInLevel = safeTotal - baseXPForCurrentLevel
        val neededForNext = xpRequiredForNextLevel(currentLevel)
        val progress = if (neededForNext > 0) {
            (currentXPInLevel.toFloat() / neededForNext.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        return UserLevelInfo(
            level = currentLevel,
            currentXPInLevel = currentXPInLevel,
            xpNeededForNextLevel = neededForNext,
            totalCumulativeXP = safeTotal,
            progress = progress
        )
    }
}
