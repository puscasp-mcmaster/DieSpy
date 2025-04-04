package com.diespy.app.ml.models

data class MatchCandidate(
    val tracked: TrackedDice,
    val box: DiceBoundingBox,
    val distance: Float
)
