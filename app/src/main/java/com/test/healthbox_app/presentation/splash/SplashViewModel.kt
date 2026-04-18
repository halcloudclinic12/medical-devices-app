package com.test.healthbox_app.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.test.healthbox_app.data.model.response.ClinicLoginResponse
import com.test.healthbox_app.data.model.response.RefreshTokenResponse
import com.test.healthbox_app.data.model.response.VerifyTokenResponse
import com.test.healthbox_app.domain.model.ApiResponse
import com.test.healthbox_app.domain.model.request.RefreshTokenRequest
import com.test.healthbox_app.domain.use_cases.OnBoardingAPIUseCases
import com.test.healthbox_app.domain.use_cases.SharedPreferenceUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    val sharedPreferenceUseCases: SharedPreferenceUseCases, val onBoardingAPIUseCases: OnBoardingAPIUseCases
) : ViewModel() {


    private val _refreshTokenState = MutableStateFlow<ApiResponse<RefreshTokenResponse>>(ApiResponse.ApiLoading())
    val refreshTokenState: StateFlow<ApiResponse<RefreshTokenResponse>> get() = _refreshTokenState

    private val _verifyTokenState = MutableStateFlow<ApiResponse<VerifyTokenResponse>>(ApiResponse.ApiLoading())
    val verifyTokenState: StateFlow<ApiResponse<VerifyTokenResponse>> get() = _verifyTokenState

    init {
        println("clinicDataLogs Splash  :: refresh Token : model : ${sharedPreferenceUseCases.getRefreshToken()}")
    }

    fun getClinic(): ClinicLoginResponse? {
        return sharedPreferenceUseCases.getClinic()
    }

    fun refreshToken() {

        viewModelScope.launch(Dispatchers.IO) {

            val refreshTokenRequest = RefreshTokenRequest()

            println("clinicDataLogs Splash  : Token : ${sharedPreferenceUseCases.getToken()}")
            println("clinicDataLogs Splash  : refresh Token : ${sharedPreferenceUseCases.getRefreshToken()}")

            refreshTokenRequest.refreshToken = sharedPreferenceUseCases.getRefreshToken()

            onBoardingAPIUseCases.refreshToken(
                refreshTokenRequest = refreshTokenRequest, authToken = "Bearer ${sharedPreferenceUseCases.getToken().toString()}"
            ).collect { it ->
                println("\ncreateBasicTestLog   :: Res Logs :: ${Gson().toJson(it)}")
                _refreshTokenState.value = it

                sharedPreferenceUseCases.saveVerificationTime()

            }
        }
    }

    fun verifyToken(token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            onBoardingAPIUseCases.verifyToken(
                token = token, authToken = "Bearer ${sharedPreferenceUseCases.getToken().toString()}"
            ).collect { it ->
                println("\ncreateBasicTestLog   :: Res Logs :: ${Gson().toJson(it)}")
                _verifyTokenState.value = it

                sharedPreferenceUseCases.saveVerificationTime()

            }
        }
    }

    fun saveToken(token: String, refreshToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            sharedPreferenceUseCases.saveTokens(token = token, refreshToken = refreshToken)
        }
    }

    fun isVerificationExpired(): Boolean? {
        return sharedPreferenceUseCases.isVerificationExpired()
    }

}