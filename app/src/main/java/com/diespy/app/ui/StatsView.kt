package com.diespy.app.ui

import com.diespy.app.BoundingBox

class StatsView {
    private var rollSum = 0              // Total sum of dice rolls
    private var numDice = 0              // Number of detected dice
    private val faceCounts = IntArray(6) // Count of each face (1-6) appearing

    /**
     * Updates the dice roll statistics based on detected dice.
     */
    fun updateStats(detectedDice: List<BoundingBox>) {
        rollSum = 0
        faceCounts.fill(0)

        detectedDice.forEach { dice ->
            val diceValue = dice.classIndex + 1  // Convert from 0-based index to 1-6
            rollSum += diceValue
            faceCounts[dice.classIndex]++
        }
        numDice = detectedDice.size
    }

    /**
     * Resets all stored statistics.
     */
    fun reset() {
        rollSum = 0
        numDice = 0
        faceCounts.fill(0)  // Also reset face counts
    }

    /**
     * Returns a summary of dice statistics.
     */
    fun getStatSummary(): String {
        return """
            Sum: $rollSum
            Count: $numDice
        """.trimIndent()
    }

    /**
     * Returns a breakdown of detected dice face occurrences.
     */
    fun getFaceCounts(): String {
        return """
            1: ${faceCounts[0]}
            2: ${faceCounts[1]}
            3: ${faceCounts[2]}
            4: ${faceCounts[3]}
            5: ${faceCounts[4]}
            6: ${faceCounts[5]}
        """.trimIndent()
    }
}
