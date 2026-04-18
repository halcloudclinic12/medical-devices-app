package com.test.healthbox_app.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RadioButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.test.healthbox_app.BleConnectionViewModel
import com.test.healthbox_app.databinding.VisionsTestsAlphabetsViewBinding

class VisionsTestAlphabetsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: VisionsTestsAlphabetsViewBinding = VisionsTestsAlphabetsViewBinding.inflate(LayoutInflater.from(context), this, true)

    // Fragment/Activity sets this
    var onSelectionChanged: ((side: Side, selectedId: Int, selectedText: String?) -> Unit)? = null

    enum class Side { LEFT, RIGHT }

    var selectedSlide = 0

    fun bind(viewModel: BleConnectionViewModel) {
//        binding.viewModel = viewModel
        binding.executePendingBindings()

        // Apply grouping logic
        setExclusiveSelection(leftRadios, Side.LEFT)
        setExclusiveSelection(rightRadios, Side.RIGHT)

        binding.ivBackStep.setOnClickListener {
            println("selectedSlide value on Back CLick : : ${selectedSlide}")

            if (selectedSlide > 0) {
                selectedSlide -= 1

                updateViewsStatus()
            }
        }

        binding.ivNextStep.setOnClickListener {
            println("selectedSlide value on Next CLick : : ${selectedSlide}")
            if (selectedSlide < 3) {
                selectedSlide += 1

                updateViewsStatus()
            }
        }
    }

    private fun updateViewsStatus() {
        when (selectedSlide) {
            0 -> {
                binding.ivBackStep.visibility = INVISIBLE
                binding.ivNextStep.visibility = VISIBLE

                binding.layoutAlphabetsSlide1.visibility = VISIBLE
                binding.layoutAlphabetsSlide2.visibility = GONE
                binding.layoutAlphabetsSlide3.visibility = GONE

            }

            1 -> {
                binding.ivBackStep.visibility = VISIBLE
                binding.ivNextStep.visibility = VISIBLE

                binding.layoutAlphabetsSlide1.visibility = GONE
                binding.layoutAlphabetsSlide2.visibility = VISIBLE
                binding.layoutAlphabetsSlide3.visibility = GONE

            }

            2 -> {
                binding.ivBackStep.visibility = VISIBLE
                binding.ivNextStep.visibility = VISIBLE

                binding.layoutAlphabetsSlide1.visibility = GONE
                binding.layoutAlphabetsSlide2.visibility = GONE
                binding.layoutAlphabetsSlide3.visibility = VISIBLE
            }
        }
    }

    private val leftRadios by lazy {
        listOf(
            binding.leftRb20200,
            binding.leftRb20100,
            binding.leftRb2070,
            binding.leftRb2050,
            binding.leftRb2040,
            binding.leftRb2030,
            binding.leftRb2025,
            binding.leftRb2020
        )
    }
    private val rightRadios by lazy {
        listOf(
            binding.rightRb20200,
            binding.rightRb20100,
            binding.rightRb2070,
            binding.rightRb2050,
            binding.rightRb2040,
            binding.rightRb2030,
            binding.rightRb2025,
            binding.rightRb2020
        )
    }

    /**
     * Ensures only one RadioButton in the given list is checked at a time,
     * and triggers callback on selection
     */
    private fun setExclusiveSelection(radioButtons: List<RadioButton>, side: Side) {
        radioButtons.forEach { rb ->
            rb.setOnClickListener {

                println("on Radio Button Clicked : : ${rb.isChecked}  : text : ${rb.text}  : tag : ${rb.tag} ")
                // Uncheck all others
                radioButtons.filter { it != rb }.forEach { it.isChecked = false }
                rb.isChecked = true

                // Callback only here
                onSelectionChanged?.invoke(side, rb.id, rb.tag?.toString())

            }
        }
    }

    /** Helpers */
    fun getSelectedLeftRadio(): RadioButton? = leftRadios.firstOrNull { it.isChecked }
    fun getSelectedRightRadio(): RadioButton? = rightRadios.firstOrNull { it.isChecked }

    fun isValidSelection(): Boolean {
        return getSelectedLeftRadio() != null && getSelectedRightRadio() != null
    }

}