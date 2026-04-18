package com.test.healthbox_app.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.test.healthbox_app.R
import com.test.healthbox_app.domain.model.StepItem
import com.test.healthbox_app.domain.model.StepStatus

class StepsLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val recyclerView: RecyclerView
    private lateinit var adapter: StepsLayoutAdapter

    private var stepClickListener: ((StepItem) -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_steps, this, true)
        recyclerView = findViewById(R.id.recyclerViewSteps)
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    }

    fun setSteps(steps: List<StepItem>) {
        adapter = StepsLayoutAdapter(context = context, steps = steps, onStepClick = { step ->
            stepClickListener?.invoke(step)
        })
        recyclerView.adapter = adapter
    }

    fun setOnStepClickListener(listener: (StepItem) -> Unit) {
        stepClickListener = listener
    }

    fun updateStepStatus(stepId: Int, status: StepStatus) {
        if (::adapter.isInitialized) {
            adapter.updateStepStatus(stepId, status)
        }
    }

    fun markStepCompleted(stepId: Int) {
        updateStepStatus(stepId, StepStatus.COMPLETED)
    }

    fun markStepCurrent(stepId: Int) {
        updateStepStatus(stepId, StepStatus.CURRENT)
    }

    fun markStepPending(stepId: Int) {
        updateStepStatus(stepId, StepStatus.PENDING)
    }
}