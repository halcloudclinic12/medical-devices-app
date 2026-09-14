package com.test.healthbox_app.presentation.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.test.healthbox_app.R
import com.test.healthbox_app.data.model.BodyCheckupPref
import com.test.healthbox_app.databinding.DialogStepsOrbitBinding
import com.test.healthbox_app.domain.model.StepItem
import com.test.healthbox_app.domain.model.StepStatus
import com.test.healthbox_app.presentation.view.OrbitConnectorView

/**
 * Full-screen radial view of every step in the Basic Health Checkup, opened from
 * StepsLayout's "N Steps" toggle (replaces the old inline expanded chip strip). Purely a
 * navigation surface: tapping a node invokes the same callback each test fragment already
 * supplies via StepsLayout.setOnStepClickListener, so no fragment's click-handling changes.
 *
 * All 8 node/line positions are fixed in dialog_steps_orbit.xml via ConstraintLayout circular
 * constraints — this class only sets color/text/visibility on top of them, keyed by
 * StepItem.id rather than list order.
 */
class StepsOrbitDialog(private val context: Context) {

    private val dialog = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
    private lateinit var binding: DialogStepsOrbitBinding

    fun show(steps: List<StepItem>, onStepClick: (StepItem) -> Unit) {
        binding = DialogStepsOrbitBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)
        dialog.setCancelable(true)

        dialog.window?.let { window ->
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            window.setGravity(Gravity.CENTER)
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(0.45f)
        }

        bind(steps, onStepClick)

        // orbitCanvas and the pill bars' interactive children consume their own taps; a tap
        // that falls through all of them (blank scrim, or blank space in a bar) lands here.
        binding.orbitRoot.setOnClickListener { dismiss() }
        binding.btnCloseOrbit.setOnClickListener { dismiss() }

        dialog.show()
    }

    private fun bind(steps: List<StepItem>, onStepClick: (StepItem) -> Unit) {
        val currentIndex = steps.indexOfFirst { it.status == StepStatus.CURRENT }
        val current = if (currentIndex != -1) steps[currentIndex] else steps.firstOrNull()
        val position = if (currentIndex != -1) currentIndex + 1 else 1
        // The checkup is strictly linear — goToNextStep() only ever advances by one — so any
        // step at or before this id has necessarily already been visited (completed, or
        // skipped past), while anything after it hasn't been reached yet. Gates which nodes
        // are tappable, further down.
        val currentStepId = current?.id ?: 1

        binding.tvOrbitSubtitle.text = "Step $position of ${steps.size} Active"
        binding.tvHubStepTitle.text = current?.title.orEmpty()
        binding.tvHubStepLabel.text = "Step $position/${steps.size}"

        val slots = listOf(
            binding.nodeSlot1, binding.nodeSlot2, binding.nodeSlot3, binding.nodeSlot4,
            binding.nodeSlot5, binding.nodeSlot6, binding.nodeSlot7, binding.nodeSlot8
        )

        // step_pending (medium gray), not the paler step_pending_border — the border tone
        // was legible on a cardinal (horizontal/vertical) line but washed out on a diagonal
        // one at this stroke width, which read as "some steps aren't connected."
        val pendingColor = ContextCompat.getColor(context, R.color.step_pending)
        val activeColor = ContextCompat.getColor(context, R.color.green)
        val nextColor = ContextCompat.getColor(context, R.color.orbit_next)

        val connections = mutableListOf<OrbitConnectorView.Connection>()

        // Slot n (1-based) is wired in XML to a fixed angle; match it to the step with that
        // id rather than assuming `steps` is in id order.
        for (slotIndex in 0 until 8) {
            val stepId = slotIndex + 1
            val slot = slots[slotIndex]
            val step = steps.firstOrNull { it.id == stepId }

            slot.removeAllViews()

            if (step == null) {
                slot.visibility = View.GONE
                continue
            }

            slot.visibility = View.VISIBLE

            val isNext = currentIndex != -1 && slotIndex == currentIndex + 1
            val isCurrent = step.status == StepStatus.CURRENT
            // "Done" is a real saved value (BodyCheckupPref), not StepStatus.COMPLETED — that
            // status flag also gets set on a step a chip-tap merely navigated past, which
            // previously made this dialog's checkmarks disagree with the pill's completed
            // count (see StepsLayout.bindCollapsedSummary). Both now read the same source.
            val isDone = BodyCheckupPref.isStepComplete(step.id)
            val lineColor = when {
                isCurrent || isDone -> activeColor
                isNext -> nextColor
                else -> pendingColor
            }

            val nodeView = LayoutInflater.from(context).inflate(R.layout.item_orbit_node, slot, false)
            val nodeCircle = bindNode(nodeView, step, isDone, isNext, step.id <= currentStepId, onStepClick)
            slot.addView(nodeView)

            // Solid to the step actually happening right now; dotted to every other step
            // (done, next, or pending) — the connection you're not currently on. nodeCircle,
            // not `slot`, is the endpoint — see OrbitConnectorView for why that distinction
            // is the whole fix here.
            connections += OrbitConnectorView.Connection(binding.hubCenter, nodeCircle, lineColor, dashed = !isCurrent)
        }

        binding.orbitConnector.setConnections(connections)
    }

    /** Returns the node's circle view, for OrbitConnectorView to aim a line at. */
    private fun bindNode(
        nodeView: View, step: StepItem, isDone: Boolean, isNext: Boolean, isNavigable: Boolean, onStepClick: (StepItem) -> Unit
    ): FrameLayout {
        val nodeCircle = nodeView.findViewById<FrameLayout>(R.id.nodeCircle)
        val tvNumber = nodeView.findViewById<TextView>(R.id.tvNodeNumber)
        val tvCheck = nodeView.findViewById<TextView>(R.id.tvNodeCheck)
        val tvTitle = nodeView.findViewById<TextView>(R.id.tvNodeTitle)
        val tvStatus = nodeView.findViewById<TextView>(R.id.tvNodeStatus)

        tvNumber.text = String.format("%02d", step.id)
        tvTitle.text = step.title

        when {
            // CURRENT wins even if this step happens to already have a saved value (e.g. a
            // retest) — being the active step is a status, not a completeness fact.
            step.status == StepStatus.CURRENT -> {
                nodeCircle.setBackgroundResource(R.drawable.bg_orbit_node_current)
                tvCheck.visibility = View.GONE
                tvNumber.visibility = View.VISIBLE
                tvNumber.setTextColor(Color.WHITE)
                tvStatus.text = "Active"
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.green))
            }

            isDone -> {
                nodeCircle.setBackgroundResource(R.drawable.bg_orbit_node_completed)
                tvCheck.visibility = View.VISIBLE
                tvNumber.visibility = View.GONE
                tvStatus.text = "Done"
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.green))
            }

            isNext -> {
                nodeCircle.setBackgroundResource(R.drawable.bg_orbit_node_next)
                tvCheck.visibility = View.GONE
                tvNumber.visibility = View.VISIBLE
                tvStatus.text = "Next"
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.orbit_next))
            }

            else -> {
                nodeCircle.setBackgroundResource(R.drawable.bg_orbit_node_pending)
                tvCheck.visibility = View.GONE
                tvNumber.visibility = View.VISIBLE
                tvStatus.text = "Pending"
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.step_pending))
            }
        }

        // Tapping a step not yet reached only closes the dialog — it never navigates or
        // changes which step is current. Tapping anything at or before the current step
        // (completed or merely skipped past) navigates there, same as before.
        nodeView.setOnClickListener {
            if (isNavigable) {
                onStepClick(step)
            }
            dismiss()
        }

        return nodeCircle
    }

    fun dismiss() {
        if (dialog.isShowing) dialog.dismiss()
    }
}