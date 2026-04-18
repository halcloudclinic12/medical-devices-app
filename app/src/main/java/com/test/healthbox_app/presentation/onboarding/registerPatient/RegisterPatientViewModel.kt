package com.test.healthbox_app.presentation.onboarding.registerPatient

import android.util.Log
import androidx.compose.ui.text.toLowerCase
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.test.healthbox_app.data.model.response.Patient
import com.test.healthbox_app.data.model.response.PatientLoginResponse
import com.test.healthbox_app.domain.model.ApiResponse
import com.test.healthbox_app.domain.model.StringValues
import com.test.healthbox_app.domain.model.request.PatientUpdateRequest
import com.test.healthbox_app.domain.use_cases.PatientsAPIUseCases
import com.test.healthbox_app.domain.use_cases.SharedPreferenceUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class RegisterPatientViewModel @Inject constructor(
    private val patientsAPIUseCases: PatientsAPIUseCases,
    private val sharedPreferenceUseCases: SharedPreferenceUseCases
) : ViewModel() {

    val isMachineCheckInSuccess = MutableLiveData<Boolean>()

    private val _patient = MutableLiveData<Patient>()
    val patient: LiveData<Patient> = _patient

    private val _patientUpdateResponse = MutableStateFlow<ApiResponse<PatientLoginResponse>>(ApiResponse.ApiLoading())
    val patientUpdateResponse: StateFlow<ApiResponse<PatientLoginResponse>> get() = _patientUpdateResponse

    private val _formError = MutableLiveData<String?>()
    val formError: LiveData<String?> = _formError

    init {
        isMachineCheckInSuccess.value = false

        _patient.value = sharedPreferenceUseCases.getPatient()
    }

    fun updateGender(selectedGender: StringValues) {
        var patient = patient.value
        patient?.gender = selectedGender.value

        _patient.value = patient!!

    }

    fun updateDateOfBirth(dateOfBirth: String) {
        var patientObj = patient.value
        patientObj?.dateOfBirth = dateOfBirth

        _patient.value = patientObj!!

        Log.e("patientLoginAPILog", "  : Res 11 : ${Gson().toJson(patient.value)}")
    }

    fun updateBloodGroup(selectedGender: StringValues) {
        var patient = patient.value
        patient?.bloodGroup = selectedGender.value

        _patient.value = patient!!
    }

    fun updatePatient() {
        viewModelScope.launch(Dispatchers.IO) {

            Log.e("patientLoginAPILog", "  : Res 11 : ${Gson().toJson(patient.value)}")

            patient.value?.let {

                if (it.name.isNullOrEmpty()) {
                    _formError.postValue("Please Enter the name")
                    return@launch
                } else if (it.dateOfBirth.isNullOrEmpty()) {
                    _formError.postValue("Please select the date of birth")
                    return@launch
                } else if (it.mobile.isNullOrEmpty()) {
                    _formError.postValue("Please enter the mobile number")
                    return@launch
                } else if (it.mobile?.length != 10) {
                    _formError.postValue("Please enter the valid mobile number")
                    return@launch
                } else if (it.email.isNullOrEmpty()) {
                    _formError.postValue("Please enter the email")
                    return@launch
                } else if (it.gender.isNullOrEmpty()) {
                    _formError.postValue("Please select the gender")
                    return@launch
                }

                val patientUpdateRequest = PatientUpdateRequest().apply {
                    name = it.name
                    dateOfBirth = it.dateOfBirth
                    mobile = it.mobile
                    email = it.email
                    gender = it.gender?.lowercase(Locale.ROOT)
                    bloodGroup = it.bloodGroup
                }

                patientsAPIUseCases.patientUpdate(
                    patientId = it.id.toString(),
                    patientUpdateRequest = patientUpdateRequest
                ).collect { patientResponse ->
                    println("patientUpdateAPILog  : Res   : ${Gson().toJson(patientResponse)}")

                    _patientUpdateResponse.value = patientResponse

                    if (patientResponse is ApiResponse.ApiSuccess) {
                        patientResponse.data.data?.patient?.let { updatedPatient ->
                            _patient.postValue(updatedPatient)
                            sharedPreferenceUseCases.savePatient(updatedPatient)
                        }
                    }
                }
            }
        }
    }

    /*fun updatePatient() {
        viewModelScope.launch(Dispatchers.IO) {

            Log.e("patientLoginAPILog", "  : Res 11 : ${Gson().toJson(patient.value)}")
            patient.value?.let {
                if (patient.value?.name.isNullOrEmpty()) {
                    _formError.postValue("Please Enter the name")
                    return@launch
                } else if (patient.value?.dateOfBirth.isNullOrEmpty()) {
                    _formError.postValue("Please select the date of birth")
                    return@launch
                } else if (patient.value?.mobile.isNullOrEmpty()) {
                    _formError.postValue("Please enter the mobile number")
                    return@launch
                } else if (patient.value?.mobile?.length != 10) {
                    _formError.postValue("Please enter the valid mobile number")
                    return@launch
                } else if (patient.value?.email.isNullOrEmpty()) {
                    _formError.postValue("Please enter the email")
                    return@launch
                } else if (patient.value?.gender.isNullOrEmpty()) {
                    _formError.postValue("Please select the gender")
                    return@launch
                }

                val patientUpdateRequest = PatientUpdateRequest()
                patientUpdateRequest.name = patient.value?.name
                patientUpdateRequest.dateOfBirth = patient.value?.dateOfBirth
                patientUpdateRequest.mobile = patient.value?.mobile
                patientUpdateRequest.email = patient.value?.email
                patientUpdateRequest.gender = patient.value?.gender?.lowercase(Locale.ROOT)
                patientUpdateRequest.bloodGroup = patient.value?.bloodGroup

                patientsAPIUseCases.patientUpdate(
                    patientId = patient.value?.id.toString(),
                    patientUpdateRequest = patientUpdateRequest
                )
                    .collect { patientResponse ->
                        println("patientUpdateAPILog  : Res   : ${Gson().toJson(it)}")

                        _patientUpdateResponse.value = patientResponse

                        patientResponse.data?.data?.patient?.let { patient ->
                            _patient.postValue(patient)
                            sharedPreferenceUseCases.savePatient(patient)
                        }
                    }
            }
        }
    }*/


    fun resetPatientUpdateState() {
        _patientUpdateResponse.value = ApiResponse.ApiLoading()
    }
}