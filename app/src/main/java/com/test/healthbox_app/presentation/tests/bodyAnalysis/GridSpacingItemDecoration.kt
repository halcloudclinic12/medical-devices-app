package com.test.healthbox_app.presentation.tests.bodyAnalysis

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Gives a GridLayoutManager grid mathematically equal gaps everywhere - between tiles
 * AND at the grid's outer edges. A plain per-item padding (the previous approach) gives
 * double spacing between tiles (each neighbor contributes its own padding) but only half
 * that at the outer edges, which is what made the Body Analysis results grid look uneven.
 */
class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacingPx: Int
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return
        val column = position % spanCount

        outRect.left = spacingPx - column * spacingPx / spanCount
        outRect.right = (column + 1) * spacingPx / spanCount
        if (position < spanCount) {
            outRect.top = spacingPx
        }
        outRect.bottom = spacingPx
    }
}
