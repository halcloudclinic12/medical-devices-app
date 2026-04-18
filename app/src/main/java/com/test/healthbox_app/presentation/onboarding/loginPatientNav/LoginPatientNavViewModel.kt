package com.test.healthbox_app.presentation.onboarding.loginPatientNav

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.test.healthbox_app.data.model.PatientPref
import com.test.healthbox_app.data.model.response.ClinicLoginResponse
import com.test.healthbox_app.data.model.response.Patient
import com.test.healthbox_app.data.model.response.PatientLoginResponse
import com.test.healthbox_app.data.permission.PermissionHandler
import com.test.healthbox_app.domain.model.ApiResponse
import com.test.healthbox_app.domain.model.request.PatientLoginRequest
import com.test.healthbox_app.domain.repository.SetPermissionHandler
import com.test.healthbox_app.domain.use_cases.CheckAndRequestPermissionsUseCase
import com.test.healthbox_app.domain.use_cases.PatientsAPIUseCases
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
class LoginPatientNavViewModel @Inject constructor(
    private val checkAndRequestPermissionsUseCase: CheckAndRequestPermissionsUseCase,
    private val patientsAPIUseCases: PatientsAPIUseCases,
    private val sharedPreferenceUseCases: SharedPreferenceUseCases
) : ViewModel() {

    private val _permissionsGranted = MutableLiveData<Boolean>()
    val permissionsGranted: LiveData<Boolean> = _permissionsGranted

    val mobileNumber = MutableLiveData<String>()

    // Error observables
    private val _formError = MutableLiveData<String?>()
    val formError: LiveData<String?> = _formError


    private val _patientLoginState = MutableStateFlow<ApiResponse<PatientLoginResponse>>(ApiResponse.ApiLoading())
    val patientLoginState: StateFlow<ApiResponse<PatientLoginResponse>> get() = _patientLoginState

    init {
        if (Constants.LOGS_ENABLE) {
            mobileNumber.value = "8484844053"
            mobileNumber.value = "9876543211"
        }

        println("All Connected Devices Logs :: ${Gson().toJson(sharedPreferenceUseCases.getAllDevices())}")
    }

    // We need this to set the PermissionHandler from the Fragment
    fun setPermissionHandler(permissionHandler: PermissionHandler) {
        // You'll need to add a setter in your UseCase and Repository
        (checkAndRequestPermissionsUseCase as? SetPermissionHandler)?.setPermissionHandler(
            permissionHandler
        )
    }

    fun checkPermissions() {
        Log.e("checkPermissionsLogs", "  :  Called  :  ")

        viewModelScope.launch {
            try {
                val granted = checkAndRequestPermissionsUseCase.checkPermissions()
                Log.e("checkPermissionsLogs", "  :  Called  :  " + granted)
                _permissionsGranted.value = granted
            } catch (e: Exception) {
                // Handle error
                _permissionsGranted.value = false
            }
        }
    }

    fun requestPermissions() {
        checkAndRequestPermissionsUseCase.requestPermissions()
    }

    fun getClinic(): ClinicLoginResponse? {

        return sharedPreferenceUseCases.getClinic()!!
    }

    fun clearPreferenceData(): Boolean {
        PatientPref.patient = null
//        BodyCheckupPref.clearAll()

        return sharedPreferenceUseCases.clearAllExceptDevices()
    }

    fun loginPatient() {
        viewModelScope.launch(Dispatchers.IO) {

            if (mobileNumber.value.isNullOrEmpty()) {
                _formError.postValue("Please Enter the mobile number")
                return@launch
            } else if (mobileNumber.value?.length != 10) {
                _formError.postValue("Please Enter the valid mobile number")
                return@launch
            }

            val patientLoginRequest = PatientLoginRequest()
            patientLoginRequest.mobile = mobileNumber.value
            patientLoginRequest.clinicId = sharedPreferenceUseCases.getClinic()!!.data!!.clinic!!.Id

            patientsAPIUseCases.patientLogin(patientLoginRequest).collect { it ->
                Log.e("patientLoginAPILog", "  : Res 11 : ${Gson().toJson(it)}")
                _patientLoginState.value = it
            }
        }
    }

    fun resetPatientLoginState() {
        _patientLoginState.value = ApiResponse.ApiLoading()
    }

    fun savePatient(patient: Patient) {
        try {

            val status = sharedPreferenceUseCases.savePatient(patient = patient)

            Logger.getLogger("PatientLoginAPILog")
                .info("PatientLoginAPILog : savePatient : $status   Patient :: ${Gson().toJson(sharedPreferenceUseCases.getPatient())}")


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}