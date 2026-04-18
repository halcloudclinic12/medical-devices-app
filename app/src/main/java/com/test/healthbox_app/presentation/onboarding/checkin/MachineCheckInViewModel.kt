package com.test.healthbox_app.presentation.onboarding.checkin

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.test.healthbox_app.data.model.response.ClinicLoginResponse
import com.test.healthbox_app.domain.model.ApiResponse
import com.test.healthbox_app.domain.model.request.ClinicLoginRequest
import com.test.healthbox_app.domain.use_cases.OnBoardingAPIUseCases
import com.test.healthbox_app.domain.use_cases.SharedPreferenceUseCases
import com.test.healthbox_app.presentation.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.logging.Logger
import javax.inject.Inject

@HiltViewModel
class MachineCheckInViewModel @Inject constructor(
    val onBoardingAPIUseCases: OnBoardingAPIUseCases, val sharedPreferenceUseCases: SharedPreferenceUseCases
) : ViewModel() {

    val machineId = MutableLiveData<String>()
    val machinePassword = MutableLiveData<String>()


    // Error observables
    private val _formError = MutableLiveData<String?>()
    val formError: LiveData<String?> = _formError

    private val _loginState = MutableStateFlow<ApiResponse<ClinicLoginResponse>>(ApiResponse.ApiLoading())
    val loginState: StateFlow<ApiResponse<ClinicLoginResponse>> get() = _loginState

    init {
        if (Constants.LOGS_ENABLE) {
            machineId.value = "2"
            machinePassword.value = "12345"
        }
    }

    fun clinicLogin() {
        viewModelScope.launch(Dispatchers.IO) {

            if (machineId.value.isNullOrEmpty()) {
                _formError.postValue("Please Enter the Machine ID")
                return@launch
            } else if (machinePassword.value.isNullOrEmpty()) {
                _formError.postValue("Please Enter the Machine Password")
                return@launch
            }
            try {
                val clinicLoginRequest = ClinicLoginRequest()
                clinicLoginRequest.clinicId = machineId.value
                clinicLoginRequest.password = machinePassword.value

                onBoardingAPIUseCases.clinicLogin(clinicLoginRequest).collect { it ->
                    Log.e("clinicLoginAPILog", "  : Res 11 : $it")
                    _loginState.value = it

                    sharedPreferenceUseCases.saveVerificationTime()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveClinic(clinicLoginResponse: ClinicLoginResponse) {
        val status = sharedPreferenceUseCases.saveClinic(clinicLoginResponse)

        Logger.getLogger("ClinicLoginAPILog")
            .info("ClinicLoginAPILog : saveClinic : $status   clinic :: ${Gson().toJson(sharedPreferenceUseCases.getClinic())}")

        Logger.getLogger("ClinicLoginAPILog").info("ClinicLoginAPILog : Token : ${sharedPreferenceUseCases.getToken()}")

        Logger.getLogger("ClinicLoginAPILog").info("ClinicLoginAPILog : Refresh Token : ${sharedPreferenceUseCases.getRefreshToken()}")
    }
}