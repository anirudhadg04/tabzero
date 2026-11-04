package com.example.detoxproject

import com.example.detoxproject.domain.engine.LevelCalculator
import org.junit.Assert.*
import org.junit.Test

class LevelCalculatorTest {

    @Test
    fun testLevel1_InitialZeroXP() {
        val info = LevelCalculator.calculateLevel(0)
        assertEquals(1, info.level)
        assertEquals(0, info.currentXPInLevel)
        assertEquals(100, info.xpNeededForNextLevel)
        assertEquals(0, info.totalCumulativeXP)
        assertEquals(0.0f, info.progress, 0.001f)
    }

    @Test
    fun testLevel1_MidProgress() {
        val info = LevelCalculator.calculateLevel(60)
        assertEquals(1, info.level)
        assertEquals(60, info.currentXPInLevel)
        assertEquals(100, info.xpNeededForNextLevel)
        assertEquals(60, info.totalCumulativeXP)
        assertEquals(0.6f, info.progress, 0.001f)
    }

    @Test
    fun testLevel2_ExactThreshold() {
        val info = LevelCalculator.calculateLevel(100)
        assertEquals(2, info.level)
        assertEquals(0, info.currentXPInLevel)
        assertEquals(150, info.xpNeededForNextLevel)
        assertEquals(100, info.totalCumulativeXP)
        assertEquals(0.0f, info.progress, 0.001f)
    }

    @Test
    fun testLevel3_ExactThreshold() {
        // Level 1->2 costs 100. Level 2->3 costs 150. Cumulative = 250
        val info = LevelCalculator.calculateLevel(250)
        assertEquals(3, info.level)
        assertEquals(0, info.currentXPInLevel)
        assertEquals(200, info.xpNeededForNextLevel)
        assertEquals(250, info.totalCumulativeXP)
        assertEquals(0.0f, info.progress, 0.001f)
    }

    @Test
    fun testLevel4_ExactThreshold() {
        // Level 1: 0..99, Level 2: 100..249, Level 3: 250..449, Level 4: 450
        val info = LevelCalculator.calculateLevel(450)
        assertEquals(4, info.level)
        assertEquals(0, info.currentXPInLevel)
        assertEquals(250, info.xpNeededForNextLevel)
        assertEquals(450, info.totalCumulativeXP)
        assertEquals(0.0f, info.progress, 0.001f)
    }

    @Test
    fun testLevel5_ExactThreshold() {
        // 450 + 250 = 700
        val info = LevelCalculator.calculateLevel(700)
        assertEquals(5, info.level)
        assertEquals(0, info.currentXPInLevel)
        assertEquals(300, info.xpNeededForNextLevel)
        assertEquals(700, info.totalCumulativeXP)
        assertEquals(0.0f, info.progress, 0.001f)
    }

    @Test
    fun testLevel6_ExactThreshold() {
        // 700 + 300 = 1000
        val info = LevelCalculator.calculateLevel(1000)
        assertEquals(6, info.level)
        assertEquals(0, info.currentXPInLevel)
        assertEquals(350, info.xpNeededForNextLevel)
        assertEquals(1000, info.totalCumulativeXP)
        assertEquals(0.0f, info.progress, 0.001f)
    }

    @Test
    fun testLevelAdvancementFormulaConsistency() {
        for (level in 1..25) {
            val expectedCost = 100 + 50 * (level - 1)
            assertEquals(expectedCost, LevelCalculator.xpRequiredForNextLevel(level))
        }
    }

    @Test
    fun testCumulativeThresholdMatchesStepByStepSum() {
        var runningSum = 0
        for (level in 1..25) {
            assertEquals(runningSum, LevelCalculator.cumulativeXpForLevel(level))
            runningSum += LevelCalculator.xpRequiredForNextLevel(level)
        }
    }

    @Test
    fun testNegativeXPClamping() {
        val info = LevelCalculator.calculateLevel(-50)
        assertEquals(1, info.level)
        assertEquals(0, info.currentXPInLevel)
        assertEquals(100, info.xpNeededForNextLevel)
        assertEquals(0, info.totalCumulativeXP)
        assertEquals(0.0f, info.progress, 0.001f)
    }
}
