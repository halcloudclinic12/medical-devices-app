package com.test.healthbox_app.domain.use_cases

import com.test.healthbox_app.domain.model.StepFlow
import com.test.healthbox_app.domain.repository.StepFlowRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStepFlowUseCase @Inject constructor(
    private val stepFlowRepository: StepFlowRepository
) {

    /**
     * Use case for getting step flow
     * @param flowId flow Id of steps
     * @return Flow of discovered stepFlow
     */

    fun getStepFlow(flowId: Int): Flow<StepFlow> {
        return stepFlowRepository.getStepFlow(flowId = flowId)
    }

    /**
     * Use case for updating step flow
     * @param stepFlow The step Flow to update
     */
    suspend fun updateStepFlow(
        stepFlow: StepFlow
    ) {
        return stepFlowRepository.updateStepFlow(stepFlow = stepFlow)
    }

}