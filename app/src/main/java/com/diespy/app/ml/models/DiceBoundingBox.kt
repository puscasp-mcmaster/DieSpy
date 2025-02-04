package com.diespy.app.ml.models

/**
 * Represents a detected bounding box in the image.
 * This stores the coordinates, confidence score, and class details.
 */
data class DiceBoundingBox(
    val x1: Float, // Top-left X coordinate
    val y1: Float, // Top-left Y coordinate
    val x2: Float, // Bottom-right X coordinate
    val y2: Float, // Bottom-right Y coordinate
    val confidence: Float, // Confidence score
    val classIndex: Int, // Class index (e.g., 0 = die face "1", 5 = die face "6")
    val className: String // Name of detected class
) {
    /**
     * Converts bounding box coordinates from normalized values (0 to 1)
     * into absolute pixel coordinates based on the given image dimensions.
     */
    fun toPixelCoordinates(imageWidth: Int, imageHeight: Int): DiceBoundingBox {
        return DiceBoundingBox(
            x1 * imageWidth, y1 * imageHeight,  // Scale top-left corner
            x2 * imageWidth, y2 * imageHeight,  // Scale bottom-right corner
            confidence, classIndex, className
        )
    }
    /**
     * Formats confidence score as a percentage (e.g., 95% instead of 0.95).
     */
    fun formattedConfidence(): String {
        return "${(confidence * 100).toInt()}%"
    }

}
