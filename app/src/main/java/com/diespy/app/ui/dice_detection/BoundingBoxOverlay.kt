package com.diespy.app.ui.dice_detection

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.diespy.app.ml.models.DiceBoundingBox

class BoundingBoxOverlay(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    // List to store detected bounding boxes
    private var results = listOf<DiceBoundingBox>()

    // Paint objects for drawing bounding boxes and labels
    private val boxPaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 8F }
    private val textPaint = Paint().apply { color = Color.WHITE; textSize = 42f; style = Paint.Style.FILL }
    private val textBackgroundPaint = Paint().apply { style = Paint.Style.FILL }

    private val bounds = Rect() // Text bounds for label placement
    private val colorMap = mutableMapOf<String, Int>() // Stores unique colors for each detected label

    /**
     * Clears overlay by resetting detected results and invalidating view.
     */
    fun clear() {
        results = emptyList()
        invalidate()
    }

    /**
     * Sets new bounding boxes and refreshes the overlay.
     */
    fun setResults(diceBoundingBoxes: List<DiceBoundingBox>) {
        results = diceBoundingBoxes
        invalidate()
    }

    /**
     * Overrides the default Android `draw()` method to render bounding boxes.
     */
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        results.forEach { boundingBox -> drawBoundingBox(canvas, boundingBox.toPixelCoordinates(width, height)) }
    }

    /**
     * Draws a bounding box and label text on the canvas.
     */
    private fun drawBoundingBox(canvas: Canvas, diceBoundingBox: DiceBoundingBox) {
        val color = getColorForLabel(diceBoundingBox.className)
        boxPaint.color = color

        val left = diceBoundingBox.x1
        val top = diceBoundingBox.y1
        val right = diceBoundingBox.x2
        val bottom = diceBoundingBox.y2

        // Draw bounding box
        canvas.drawRoundRect(left, top, right, bottom, 16f, 16f, boxPaint)

        // Format label text
        var className = diceBoundingBox.className
        if (className.endsWith("n")) {
            className = className.dropLast(1)
        }
        val drawableText = "$className - ${Math.round(diceBoundingBox.confidence * 100.0)}%"

        textPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)

        // Draw background for label
        val textBackgroundRect = RectF(
            left,
            top,
            left + bounds.width() + TEXT_PADDING,
            top + bounds.height() + TEXT_PADDING
        )
        textBackgroundPaint.color = color
        canvas.drawRoundRect(textBackgroundRect, 8f, 8f, textBackgroundPaint)

        // Draw label text
        canvas.drawText(drawableText, left, top + bounds.height(), textPaint)
    }

    /**
     * Generates or retrieves a unique color for each detected label.
     */
    private fun getColorForLabel(label: String): Int {
        return colorMap.getOrPut(label) {
            when (label) {
                "1" -> Color.RED
                "2" -> Color.rgb(102, 0, 204)
                "3" -> Color.rgb(0, 204, 0)
                "4" -> Color.rgb(102, 178, 255)
                "5" -> Color.BLUE
                "6" -> Color.rgb(255, 0, 127)
                else -> Color.rgb((0..255).random(), (0..255).random(), (0..255).random())
            }
        }
    }

    companion object {
        private const val TEXT_PADDING = 8
    }
}
