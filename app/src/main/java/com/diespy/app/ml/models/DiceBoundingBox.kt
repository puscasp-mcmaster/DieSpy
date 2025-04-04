package com.diespy.app.ml.models

data class DiceBoundingBox(
    val x1: Float, //Top-left X coordinate
    val y1: Float, //Top-left Y coordinate
    val x2: Float, //Bottom-right X coordinate
    val y2: Float, //Bottom-right Y coordinate
    val confidence: Float, //Confidence score
    val classIndex: Int, //Class index (e.g., 0 = die face "1", 5 = die face "6")
    val className: String //Name of detected class
) {

    fun toPixelCoordinates(imageWidth: Int, imageHeight: Int): DiceBoundingBox {
        return DiceBoundingBox(
            x1 * imageWidth, y1 * imageHeight,  //Scale top-left corner
            x2 * imageWidth, y2 * imageHeight,  //Scale bottom-right corner
            confidence, classIndex, className
        )
    }
}
