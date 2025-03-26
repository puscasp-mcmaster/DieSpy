package com.diespy.app.ui.dice_simulation

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridSpacingDecoration(
    private val spanCount: Int,
    private val horizontalSpacing: Int,
    private val verticalSpacing: Int
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount

        // Distribute horizontal spacing evenly
        outRect.left = horizontalSpacing / 2
        outRect.right = horizontalSpacing / 2

        // Set vertical spacing explicitly
        outRect.top = verticalSpacing / 2
        outRect.bottom = verticalSpacing / 2
    }
}
