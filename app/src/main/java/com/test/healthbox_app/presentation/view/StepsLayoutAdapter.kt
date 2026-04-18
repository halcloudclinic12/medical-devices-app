package com.test.healthbox_app.presentation.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.test.healthbox_app.R
import com.test.healthbox_app.domain.model.StepItem
import com.test.healthbox_app.domain.model.StepStatus
import javax.inject.Inject

class StepsLayoutAdapter @Inject constructor(
    private val context: Context,
    private val steps: List<StepItem>,
    private val onStepClick: (StepItem) -> Unit
) : RecyclerView.Adapter<StepsLayoutAdapter.StepViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_step, parent, false)
        return StepViewHolder(view)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        val step = steps[position]
        holder.bind(step, position == steps.size - 1)
    }

    override fun getItemCount(): Int = steps.size

    fun updateStepStatus(stepId: Int, newStatus: StepStatus) {
        val position = steps.indexOfFirst { it.id == stepId }
        if (position != -1) {
            (steps[position] as? StepItem)?.let {
                val updatedStep = it.copy(status = newStatus)
                (steps as MutableList<StepItem>)[position] = updatedStep
                notifyItemChanged(position)
            }
        }
    }

    inner class StepViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivStepIcon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvStepTitle)
        private val viewConnector: View = itemView.findViewById(R.id.viewConnector)

        fun bind(step: StepItem, isLastItem: Boolean) {
            tvTitle.text = step.title
            ivIcon.setImageResource(step.icon)

            // Handle click
            itemView.setOnClickListener {
                onStepClick(step)
            }

            // Handle connector visibility
            viewConnector.visibility = if (isLastItem) View.GONE else View.VISIBLE

            // Apply styling based on status
            when (step.status) {
                StepStatus.COMPLETED -> {
                    ivIcon.setBackgroundResource(R.drawable.bg_step_completed)
                    tvTitle.setTextColor(ContextCompat.getColor(context, R.color.step_completed))
                    viewConnector.setBackgroundColor(ContextCompat.getColor(context, R.color.step_completed))
                }

                StepStatus.CURRENT -> {
                    ivIcon.setBackgroundResource(R.drawable.bg_step_current)
                    tvTitle.setTextColor(ContextCompat.getColor(context, R.color.step_current))
                    viewConnector.setBackgroundColor(ContextCompat.getColor(context, R.color.step_pending))
                }

                StepStatus.PENDING -> {
                    ivIcon.setBackgroundResource(R.drawable.bg_step_pending)
                    tvTitle.setTextColor(ContextCompat.getColor(context, R.color.step_pending))
                    viewConnector.setBackgroundColor(ContextCompat.getColor(context, R.color.step_pending))
                }
            }
        }
    }
}