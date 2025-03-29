package com.diespy.app.ml.models

import com.diespy.app.ml.models.TrackedDice
import com.diespy.app.ml.models.DiceBoundingBox

data class MatchCandidate(
    val tracked: TrackedDice,
    val box: DiceBoundingBox,
    val distance: Float
)
