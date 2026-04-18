package com.test.healthbox_app.domain.model

data class StepFlow(
    val id: Int,
    val name: String,
    val steps: List<StepItem>,
    val currentStepIndex: Int = 0
) {
    val totalSteps: Int
        get() = steps.size

    val currentStep: StepItem?
        get() = if (currentStepIndex < steps.size) steps[currentStepIndex] else null

    val isCompleted: Boolean
        get() = currentStepIndex >= steps.size

    fun moveToNextStep(): StepFlow {
        return if (currentStepIndex < steps.size - 1) {
            this.copy(currentStepIndex = currentStepIndex + 1)
        } else {
            this
        }
    }

    fun moveToPreviousStep(): StepFlow {
        return if (currentStepIndex > 0) {
            this.copy(currentStepIndex = currentStepIndex - 1)
        } else {
            this
        }
    }

    fun moveToStep(stepId: Int): StepFlow {
        val stepIndex = steps.indexOfFirst { it.id == stepId }
        return if (stepIndex != -1) {
            this.copy(currentStepIndex = stepIndex)
        } else {
            this
        }
    }
}