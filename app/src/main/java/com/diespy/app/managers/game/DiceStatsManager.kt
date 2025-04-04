package com.diespy.app.managers.game

class DiceStatsManager {
    private var rollSum = 0
    private var numDice = 0
    private val faceCounts = IntArray(12)

    fun reset() {
        rollSum = 0
        numDice = 0
        faceCounts.fill(0)
    }
}