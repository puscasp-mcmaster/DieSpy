package com.diespy.app.managers.game

import com.diespy.app.ml.models.DiceBoundingBox

class DiceStatsManager {
    private var rollSum = 0              // Total sum of dice rolls
    private var numDice = 0              // Number of detected dice
    private val faceCounts = IntArray(12) // Count of each face (1-6) appearing

    /**
     * Resets all stored statistics.
     */
    fun reset() {
        rollSum = 0
        numDice = 0
        faceCounts.fill(0)  // Also reset face counts
    }
}