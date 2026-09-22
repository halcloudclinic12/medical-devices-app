package com.test.healthbox_app.domain.repository

import com.test.healthbox_app.data.model.response.BasicTestsResponse
import com.test.healthbox_app.data.model.response.CreateBasicTestResponse
import com.test.healthbox_app.data.model.response.CreateHba1cTestResponse
import com.test.healthbox_app.data.model.response.Hba1cTestsResponse
import com.test.healthbox_app.data.model.response.PatientLoginResponse
import com.test.healthbox_app.domain.model.ApiResponse
import com.test.healthbox_app.domain.model.request.BasicTestRequest
import com.test.healthbox_app.domain.model.request.Hba1cTestRequest
import com.test.healthbox_app.domain.model.request.PatientCreateRequest
import com.test.healthbox_app.domain.model.request.PatientLoginRequest
import com.test.healthbox_app.domain.model.request.PatientUpdateRequest
import kotlinx.coroutines.flow.Flow

interface PatientsAPIRepository {

    suspend fun patientLogin(patientLoginRequest: PatientLoginRequest): Flow<ApiResponse<PatientLoginResponse>>

    suspend fun patientUpdate(patientId: String, patientUpdateRequest: PatientUpdateRequest): Flow<ApiResponse<PatientLoginResponse>>

    suspend fun patientCreate(patientCreateRequest: PatientCreateRequest): Flow<ApiResponse<PatientLoginResponse>>

    suspend fun createBasicTest(basicTestRequest: BasicTestRequest, token: String): Flow<ApiResponse<CreateBasicTestResponse>>

    suspend fun getBasicTests(patientId: String, token: String): Flow<ApiResponse<BasicTestsResponse>>

    suspend fun createHba1cTest(hba1cTestRequest: Hba1cTestRequest, token: String): Flow<ApiResponse<CreateHba1cTestResponse>>

    suspend fun getHba1cTests(patientId: String, token: String): Flow<ApiResponse<Hba1cTestsResponse>>
}