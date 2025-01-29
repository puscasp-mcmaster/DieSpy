package com.diespy.app

data class BoundingBox(
    val x1: Float, //top left
    val y1: Float, //top left
    val x2: Float, //bottom right
    val y2: Float, //bottom right
    val cnf: Float, //confidence
    val cls: Int, //class index
    val clsName: String //name of detected class
)

