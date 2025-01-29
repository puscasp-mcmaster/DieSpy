package com.example.DieSpy.ui

import com.example.DieSpy.BoundingBox

class StatsView {
    private var sum: Int = 0
    private var count: Int = 0
    private val faces = IntArray(6)

    //Update stats based on logic
    fun updateStats(die: List<BoundingBox>) {
        sum = 0
        count = 0
        faces.fill(0)
        for (element in die){
            //Must add 1 because the dice rolls start at 0
            sum = sum + (element.cls+1)
            faces[element.cls]++
            count++
        }
    }

    fun reset() {
        sum = 0
        count = 0
    }

    //Return the current stats as a formatted string
    fun getCalcs(): String {
        return """
            Sum: $sum
            Count: $count
        """.trimIndent()
    }
    fun getFaces(): String {
        return """
            1: ${faces[0]}
            2: ${faces[1]}
            3: ${faces[2]}
            4: ${faces[3]}
            5: ${faces[4]}
            6: ${faces[5]}
        """.trimIndent()
    }
}