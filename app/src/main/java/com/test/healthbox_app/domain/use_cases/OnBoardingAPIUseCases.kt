package com.test.healthbox_app.domain.use_cases

import com.test.healthbox_app.domain.model.request.ClinicLoginRequest
import com.test.healthbox_app.domain.model.request.RefreshTokenRequest
import com.test.healthbox_app.domain.repository.OnboardingAPIRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class OnBoardingAPIUseCases @Inject constructor(
    private val onboardingAPIRepository: OnboardingAPIRepository
) {

    suspend fun clinicLogin(clinicLoginRequest: ClinicLoginRequest) =
        onboardingAPIRepository.clinicLogin(clinicLoginRequest).flowOn(Dispatchers.IO)

    suspend fun refreshToken(authToken: String, refreshTokenRequest: RefreshTokenRequest) =
        onboardingAPIRepository.refreshToken(token = authToken, refreshTokenRequest = refreshTokenRequest).flowOn(Dispatchers.IO)

    suspend fun verifyToken(authToken: String, token: String) =
        onboardingAPIRepository.verifyToken(authToken = authToken, token = token).flowOn(Dispatchers.IO)
}