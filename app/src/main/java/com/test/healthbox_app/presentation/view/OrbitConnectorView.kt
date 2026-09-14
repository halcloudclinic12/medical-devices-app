package com.test.healthbox_app.presentation.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.hypot

/**
 * Draws the radial connector lines in the steps orbit dialog (see StepsOrbitDialog): one
 * line per checkup step, from the hub circle's outer edge to that step's node circle's outer
 * edge.
 *
 * Endpoints are computed from the hub and node circle Views' *actual* on-screen positions at
 * draw time (getLocationOnScreen), not from an assumed radius/angle. An earlier version
 * assumed every node's circle sat exactly at a fixed (angle, 190dp) point from the hub — true
 * for the *slot* ConstraintLayout positions via circular constraints, but each slot wraps the
 * circle plus a title and status label stacked below it, so the circle itself sits off-center
 * within that slot, consistently offset toward the top of the screen. That made the endpoint
 * math correct for some directions and wrong (by a constant, direction-dependent amount) for
 * others — lines to the top nodes fell short of the circle, lines to the bottom nodes ran
 * past it. Measuring the real views removes the assumption entirely: whatever position
 * layout actually gives a circle, that's what the line aims at.
 *
 * This view must be laid out so it fills the same region as the hub and node circles, since
 * their measured positions are converted into *this* view's local coordinate space.
 */
class OrbitConnectorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** [nodeCircle] must be the circular node view itself, not the slot that wraps its label. */
    data class Connection(val hubCircle: View, val nodeCircle: View, val color: Int, val dashed: Boolean)

    private val density = context.resources.displayMetrics.density
    private val strokeWidthPx = 1.5f * density
    private val dashOnPx = 3f * density
    private val dashOffPx = 6f * density
    // Deliberate breathing room between a line's endpoint and the circle it connects to —
    // added equally to the hub side and the node side, on top of each circle's own actual
    // (correctly-measured) radius, so it's identical for all 8 lines regardless of angle.
    private val endGapPx = 6f * density

    private var connections: List<Connection> = emptyList()

    init {
        // DashPathEffect silently doesn't draw under hardware acceleration.
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setConnections(connections: List<Connection>) {
        this.connections = connections
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (connections.isEmpty()) return

        val selfOnScreen = IntArray(2).also { getLocationOnScreen(it) }

        for (conn in connections) {
            val (hubCx, hubCy) = centerRelativeToSelf(conn.hubCircle, selfOnScreen) ?: continue
            val (nodeCx, nodeCy) = centerRelativeToSelf(conn.nodeCircle, selfOnScreen) ?: continue

            val dx = nodeCx - hubCx
            val dy = nodeCy - hubCy
            val dist = hypot(dx, dy)
            if (dist <= 0f) continue
            val ux = dx / dist
            val uy = dy / dist

            val hubRadius = conn.hubCircle.width / 2f + endGapPx
            val nodeRadius = conn.nodeCircle.width / 2f + endGapPx

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = conn.color
                strokeWidth = strokeWidthPx
                strokeCap = Paint.Cap.ROUND
                if (conn.dashed) {
                    pathEffect = DashPathEffect(floatArrayOf(dashOnPx, dashOffPx), 0f)
                }
            }

            canvas.drawLine(
                hubCx + hubRadius * ux, hubCy + hubRadius * uy,
                nodeCx - nodeRadius * ux, nodeCy - nodeRadius * uy,
                paint
            )
        }
    }

    /** The given view's center, in this view's local coordinate space — null if not laid out. */
    private fun centerRelativeToSelf(view: View, selfOnScreen: IntArray): Pair<Float, Float>? {
        if (view.width == 0 || view.height == 0) return null
        val viewOnScreen = IntArray(2).also { view.getLocationOnScreen(it) }
        val cx = (viewOnScreen[0] - selfOnScreen[0]) + view.width / 2f
        val cy = (viewOnScreen[1] - selfOnScreen[1]) + view.height / 2f
        return cx to cy
    }
}