package com.test.healthbox_app.data.repository

import com.test.healthbox_app.data.model.response.BasicTestsResponse
import com.test.healthbox_app.data.model.response.CreateBasicTestResponse
import com.test.healthbox_app.data.model.response.CreateHba1cTestResponse
import com.test.healthbox_app.data.model.response.PatientLoginResponse
import com.test.healthbox_app.data.network.ApiService
import com.test.healthbox_app.domain.model.ApiResponse
import com.test.healthbox_app.domain.model.request.BasicTestRequest
import com.test.healthbox_app.domain.model.request.Hba1cTestRequest
import com.test.healthbox_app.domain.model.request.PatientCreateRequest
import com.test.healthbox_app.domain.model.request.PatientLoginRequest
import com.test.healthbox_app.domain.model.request.PatientUpdateRequest
import com.test.healthbox_app.domain.repository.PatientsAPIRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatientsAPIRepositoryImpl @Inject constructor(private val apiService: ApiService) : PatientsAPIRepository {

    override suspend fun patientLogin(request: PatientLoginRequest): Flow<ApiResponse<PatientLoginResponse>> = flow {
        emit(ApiResponse.ApiLoading())
        try {
            val response = apiService.patientLogin(request)
            emit(ApiResponse.ApiSuccess(response))
        } catch (e: Exception) {
            emit(ApiResponse.ApiError<PatientLoginResponse>(message = e.localizedMessage ?: "Unknown error"))
        }
    }


    override suspend fun patientUpdate(patientId: String, request: PatientUpdateRequest): Flow<ApiResponse<PatientLoginResponse>> = flow {
        emit(ApiResponse.ApiLoading())
        try {
            val response = apiService.patientUpdate(patientId = patientId, request = request)

            emit(ApiResponse.ApiSuccess(response))
        } catch (e: Exception) {
            emit(ApiResponse.ApiError<PatientLoginResponse>(message = e.localizedMessage ?: "Unknown error"))
        }
    }

    override suspend fun patientCreate(patientCreateRequest: PatientCreateRequest): Flow<ApiResponse<PatientLoginResponse>> = flow {
        emit(ApiResponse.ApiLoading())
        try {
            val response = apiService.patientCreate(createPatientRequest = patientCreateRequest)
            emit(ApiResponse.ApiSuccess(response))
        } catch (e: Exception) {
            emit(ApiResponse.ApiError<PatientLoginResponse>(message = e.localizedMessage ?: "Unknown error"))
        }
    }

    override suspend fun createBasicTest(basicTestRequest: BasicTestRequest, token: String): Flow<ApiResponse<CreateBasicTestResponse>> = flow {
        emit(ApiResponse.ApiLoading())
        try {
            val response = apiService.createBasicTest(request = basicTestRequest/*, token = token*/)
            emit(ApiResponse.ApiSuccess(response))
        } catch (e: Exception) {
            emit(ApiResponse.ApiError<CreateBasicTestResponse>(message = e.localizedMessage ?: "Unknown error"))
        }
    }

    override suspend fun getBasicTests(
        patientId: String,
        token: String
    ): Flow<ApiResponse<BasicTestsResponse>> = flow {
        emit(ApiResponse.ApiLoading())
        try {
            val response = apiService.getBasicTests(patientId = patientId/*, token = token*/)
            emit(ApiResponse.ApiSuccess(response))
        } catch (e: Exception) {
            emit(ApiResponse.ApiError<BasicTestsResponse>(message = e.localizedMessage ?: "Unknown error"))
        }
    }

    override suspend fun createHba1cTest(hba1cTestRequest: Hba1cTestRequest, token: String): Flow<ApiResponse<CreateHba1cTestResponse>> = flow {
        emit(ApiResponse.ApiLoading())
        try {
            val response = apiService.createHba1cTest(request = hba1cTestRequest/*, token = token*/)
            emit(ApiResponse.ApiSuccess(response))
        } catch (e: Exception) {
            emit(ApiResponse.ApiError<CreateHba1cTestResponse>(message = e.localizedMessage ?: "Unknown error"))
        }
    }

}
