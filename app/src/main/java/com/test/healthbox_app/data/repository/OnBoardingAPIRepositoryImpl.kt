package com.test.healthbox_app.data.repository

import com.test.healthbox_app.data.model.response.ClinicLoginResponse
import com.test.healthbox_app.data.model.response.RefreshTokenResponse
import com.test.healthbox_app.data.model.response.VerifyTokenResponse
import com.test.healthbox_app.data.network.ApiService
import com.test.healthbox_app.domain.model.ApiResponse
import com.test.healthbox_app.domain.model.request.ClinicLoginRequest
import com.test.healthbox_app.domain.model.request.RefreshTokenRequest
import com.test.healthbox_app.domain.repository.OnboardingAPIRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnBoardingAPIRepositoryImpl @Inject constructor(private val apiService: ApiService) : OnboardingAPIRepository {

    override suspend fun clinicLogin(request: ClinicLoginRequest): Flow<ApiResponse<ClinicLoginResponse>> = flow {
        emit(ApiResponse.ApiLoading())
        try {
            val response = apiService.clinicLogin(request)
            emit(ApiResponse.ApiSuccess(response))
        } catch (e: Exception) {
            emit(ApiResponse.ApiError<ClinicLoginResponse>(message = e.localizedMessage ?: "Unknown error"))
        }
    }

    override suspend fun refreshToken(
        token: String, refreshTokenRequest: RefreshTokenRequest
    ): Flow<ApiResponse<RefreshTokenResponse>> = flow {
        emit(ApiResponse.ApiLoading())
        try {
            val response = apiService.refreshToken(refreshTokenRequest = refreshTokenRequest)
            emit(ApiResponse.ApiSuccess(response))
        } catch (e: Exception) {
            emit(ApiResponse.ApiError<RefreshTokenResponse>(message = e.localizedMessage ?: "Unknown error"))
        }
    }

    override suspend fun verifyToken(
        authToken: String,
        token: String
    ): Flow<ApiResponse<VerifyTokenResponse>> = flow {
        emit(ApiResponse.ApiLoading())
        try {
            val response = apiService.verifyToken(token = token)
            emit(ApiResponse.ApiSuccess(response))
        } catch (e: Exception) {
            emit(ApiResponse.ApiError<VerifyTokenResponse>(message = e.localizedMessage ?: "Unknown error"))
        }
    }
}
