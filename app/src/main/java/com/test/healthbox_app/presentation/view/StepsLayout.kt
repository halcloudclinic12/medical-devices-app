package com.test.healthbox_app.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.test.healthbox_app.R
import com.test.healthbox_app.data.model.BodyCheckupPref
import com.test.healthbox_app.domain.model.StepItem
import com.test.healthbox_app.domain.model.StepStatus
import com.test.healthbox_app.presentation.dialog.StepsOrbitDialog

/**
 * Collapsed steps summary pill shown at the top of every Basic Health Checkup screen (see
 * layout_steps.xml). Tapping "N Steps" opens [StepsOrbitDialog] — a full-screen radial view
 * of all 8 steps — rather than expanding anything inline; this view only ever renders the
 * collapsed summary.
 */
class StepsLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val tvStepBadge: TextView
    private val tvCollapsedStepTitle: TextView
    private val tvNextStep: TextView
    private val tvCompletedCount: TextView
    private val tvStepsCount: TextView
    private val expandToggle: LinearLayout

    private var stepClickListener: ((StepItem) -> Unit)? = null
    private var lastSteps: List<StepItem> = emptyList()

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_steps, this, true)

        tvStepBadge = findViewById(R.id.tvStepBadge)
        tvCollapsedStepTitle = findViewById(R.id.tvCollapsedStepTitle)
        tvNextStep = findViewById(R.id.tvNextStep)
        tvCompletedCount = findViewById(R.id.tvCompletedCount)
        tvStepsCount = findViewById(R.id.tvStepsCount)
        expandToggle = findViewById(R.id.expandToggle)

        expandToggle.setOnClickListener {
            if (lastSteps.isNotEmpty()) {
                StepsOrbitDialog(context).show(lastSteps) { step ->
                    stepClickListener?.invoke(step)
                }
            }
        }
    }

    fun setSteps(steps: List<StepItem>) {
        lastSteps = steps
        bindCollapsedSummary(steps)
    }

    fun setOnStepClickListener(listener: (StepItem) -> Unit) {
        stepClickListener = listener
    }

    /** Derives the "2/8 · Body Temperature · Next: SpO2" summary from the step list. */
    private fun bindCollapsedSummary(steps: List<StepItem>) {
        if (steps.isEmpty()) return

        val currentIndex = steps.indexOfFirst { it.status == StepStatus.CURRENT }
        val current = if (currentIndex != -1) steps[currentIndex] else steps.first()
        val position = if (currentIndex != -1) currentIndex + 1 else 1
        val next = steps.getOrNull(position)

        tvStepBadge.text = "$position/${steps.size}"
        tvCollapsedStepTitle.text = current.title
        tvStepsCount.text = "${steps.size} Steps"

        if (next != null) {
            tvNextStep.visibility = View.VISIBLE
            tvNextStep.text = "Next: ${next.title}"
        } else {
            tvNextStep.visibility = View.GONE
        }

        // Real captured values, not a StepStatus tally — see BodyCheckupPref.isStepComplete.
        val completed = BodyCheckupPref.completedStepCount(steps.map { it.id })

        // Steps before the current one have necessarily been visited (the checkup only ever
        // advances one at a time) — if one of those has no captured value, it was skipped
        // rather than completed. A step at or after the current one hasn't been reached yet,
        // so it's excluded here rather than counted as skipped.
        val visitedIds = steps.take(currentIndex.coerceAtLeast(0)).map { it.id }
        val skipped = BodyCheckupPref.skippedStepCount(visitedIds)

        tvCompletedCount.text = if (skipped > 0) {
            "· $completed Completed · $skipped Skipped"
        } else {
            "· $completed Completed"
        }
    }
}