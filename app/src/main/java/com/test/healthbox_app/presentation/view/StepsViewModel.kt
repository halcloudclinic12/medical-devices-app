package com.test.healthbox_app.presentation.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.test.healthbox_app.domain.model.StepItem
import com.test.healthbox_app.domain.model.StepStatus
import androidx.lifecycle.map

class StepsViewModel : ViewModel() {

    private val _steps = MutableLiveData<List<StepItem>>()
    val steps: LiveData<List<StepItem>> = _steps

    fun initializeSteps(steps: List<StepItem>) {
        _steps.value = steps
    }

    fun updateStepStatus(stepId: Int, status: StepStatus) {
        val currentSteps = _steps.value?.toMutableList() ?: return
        val index = currentSteps.indexOfFirst { it.id == stepId }

        /*if (index != -1) {
            val updatedStep = currentSteps[index].copy(status = status)
            currentSteps[index] = updatedStep
            _steps.value = currentSteps
        }*/

        // If setting a step to CURRENT → make sure others are not CURRENT
        if (status == StepStatus.CURRENT) {
            currentSteps.replaceAll { step ->
                if (step.id == stepId) step.copy(status = StepStatus.CURRENT)
                else if (step.status == StepStatus.CURRENT) step.copy(status = StepStatus.COMPLETED)
                else step
            }
        } else {
            // Just update the status normally
            currentSteps[index] = currentSteps[index].copy(status = status)
        }

        _steps.value = currentSteps

    }

    val currentStep: LiveData<StepItem?> = steps.map { stepList ->
        stepList.firstOrNull {
            it.status == StepStatus.CURRENT
        }
    }

    fun goToNextStep() {
        val currentSteps = _steps.value?.toMutableList() ?: return
        val currentStepIndex = currentSteps.indexOfFirst { it.status == StepStatus.CURRENT }

        if (currentStepIndex != -1 && currentStepIndex < currentSteps.size - 1) {
            // Mark current step as completed
            currentSteps[currentStepIndex] = currentSteps[currentStepIndex].copy(status = StepStatus.COMPLETED)

            // Mark next step as current
            currentSteps[currentStepIndex + 1] = currentSteps[currentStepIndex + 1].copy(status = StepStatus.CURRENT)

            _steps.value = currentSteps
        }
    }

    fun resetSteps() {
        val currentSteps = _steps.value?.toMutableList() ?: return

        currentSteps.forEachIndexed { index, step ->
            currentSteps[index] = step.copy(
                status = if (index == 0) StepStatus.CURRENT else StepStatus.PENDING
            )
        }

        _steps.value = currentSteps
    }
}