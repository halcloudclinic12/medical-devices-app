/*
package com.kiosk.healthbox_app.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.kiosk.healthbox_app.R
import com.kiosk.healthbox_app.domain.model.StepItem

class StepIndicatorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var steps: List<StepItem> = emptyList()
    private var currentStep = 0

    init {
        orientation = HORIZONTAL
    }

    fun setSteps(steps: List<StepItem>) {
        this.steps = steps
        removeAllViews()

        steps.forEachIndexed { index, step ->
            // Inflate step item view
            val stepView = LayoutInflater.from(context).inflate(R.layout.item_step, this, false)

            // Set step icon and title
            stepView.findViewById<ImageView>(R.id.ivStepIcon).setImageResource(step.icon)
            stepView.findViewById<TextView>(R.id.tvStepTitle).text = step.title

            // Apply styles based on step state
            updateStepViewState(stepView, index == currentStep, index < currentStep)

            addView(stepView)

            // Add connector line if not the last item
            if (index < steps.size - 1) {
                val connector = View(context)
                val params = LayoutParams(0, dpToPx(2)).apply {
                    weight = 1f
                    gravity = Gravity.CENTER_VERTICAL
                }
                connector.layoutParams = params
                connector.setBackgroundResource(R.drawable.dotted_line)
                addView(connector)
            }
        }
    }

    fun setCurrentStep(position: Int) {
        if (position < 0 || position >= steps.size) return

        currentStep = position
        updateStepsState()
    }

    private fun updateStepsState() {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (i % 2 == 0) { // Step views are at even positions
                val stepIndex = i / 2
                updateStepViewState(child, stepIndex == currentStep, stepIndex < currentStep)
            } else { // Connector views are at odd positions
                val beforeStepIndex = i / 2
                child.setBackgroundResource(
                    if (beforeStepIndex < currentStep) R.drawable.solid_line
                    else R.drawable.dotted_line
                )
            }
        }
    }

    private fun updateStepViewState(view: View, isActive: Boolean, isCompleted: Boolean) {
        val container = view.findViewById<CardView>(R.id.cardContainer)
        val icon = view.findViewById<ImageView>(R.id.ivStepIcon)
        val title = view.findViewById<TextView>(R.id.tvStepTitle)

        when {
            isActive -> {
                container.setCardBackgroundColor(ContextCompat.getColor(context, R.color.app_color))
                title.setTextColor(ContextCompat.getColor(context, R.color.purple_700))
            }

            isCompleted -> {
                container.setCardBackgroundColor(ContextCompat.getColor(context, R.color.green))
                title.setTextColor(ContextCompat.getColor(context, R.color.green))
            }

            else -> {
                container.setCardBackgroundColor(ContextCompat.getColor(context, R.color.gray_600))
                title.setTextColor(ContextCompat.getColor(context, R.color.gray_600))
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}*/
