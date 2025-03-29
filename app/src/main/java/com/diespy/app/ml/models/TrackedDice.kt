package com.diespy.app.ml.models

data class TrackedDice(
    var centerX: Float,
    var centerY: Float,
    val predictions: MutableList<Int> = mutableListOf(),
    var lastSeenFrame: Int = 0
) {
    fun addPrediction(classIndex: Int, maxHistory: Int = 15) {
        predictions.add(classIndex)
        if (predictions.size > maxHistory) {
            predictions.removeAt(0)
        }
    }

    fun getStablePrediction(): Int? {
        return predictions
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    }

    fun distanceTo(otherX: Float, otherY: Float): Float {
        val dx = centerX - otherX
        val dy = centerY - otherY
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}


