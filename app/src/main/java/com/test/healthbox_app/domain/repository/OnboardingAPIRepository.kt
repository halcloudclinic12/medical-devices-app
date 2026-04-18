package com.test.healthbox_app.domain.repository

import com.test.healthbox_app.data.model.response.ClinicLoginResponse
import com.test.healthbox_app.data.model.response.RefreshTokenResponse
import com.test.healthbox_app.data.model.response.VerifyTokenResponse
import com.test.healthbox_app.domain.model.ApiResponse
import com.test.healthbox_app.domain.model.request.ClinicLoginRequest
import com.test.healthbox_app.domain.model.request.RefreshTokenRequest
import kotlinx.coroutines.flow.Flow

interface OnboardingAPIRepository {

    suspend fun clinicLogin(clinicLoginRequest: ClinicLoginRequest): Flow<ApiResponse<ClinicLoginResponse>>

    suspend fun refreshToken(token: String, refreshTokenRequest: RefreshTokenRequest): Flow<ApiResponse<RefreshTokenResponse>>

    suspend fun verifyToken(authToken: String, token: String): Flow<ApiResponse<VerifyTokenResponse>>

}