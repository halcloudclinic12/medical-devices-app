package com.test.healthbox_app.domain.repository

import com.test.healthbox_app.domain.model.StepFlow
import kotlinx.coroutines.flow.Flow

interface StepFlowRepository {
    fun getStepFlow(flowId: Int): Flow<StepFlow>
    suspend fun updateStepFlow(stepFlow: StepFlow)
}