package com.test.healthbox_app.presentation.tests.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.test.healthbox_app.data.model.BodyCheckupPref
import com.test.healthbox_app.data.model.mapper.toBasicTestRequestDto
import com.test.healthbox_app.data.model.response.CreateBasicTestResponse
import com.test.healthbox_app.data.model.response.Patient
import com.test.healthbox_app.domain.model.ApiResponse
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
class ResultsViewModel @Inject constructor(
    private val patientsAPIUseCases: PatientsAPIUseCases, private val sharedPreferenceUseCases: SharedPreferenceUseCases
) : ViewModel() {

    private val _createBasicTestState = MutableStateFlow<ApiResponse<CreateBasicTestResponse>>(ApiResponse.ApiLoading())
    val createBasicTestState: StateFlow<ApiResponse<CreateBasicTestResponse>> get() = _createBasicTestState

    init {
        if (Constants.LOGS_ENABLE) {

        }
    }

    fun createBasicTest() {
        viewModelScope.launch(Dispatchers.IO) {

            val basicRequest = BodyCheckupPref.toBasicTestRequestDto()
            println("\ncreateBasicTestLog   :: Calling:: ${basicRequest}")

            patientsAPIUseCases.createBasicTest(
                basicTestRequest = basicRequest, authToken = "Bearer ${sharedPreferenceUseCases.getToken().toString()}"
            ).collect { it ->
                println("\ncreateBasicTestLog   :: Res Logs :: ${Gson().toJson(it)}")
                _createBasicTestState.value = it
            }

        }
    }

    fun savePatient(patient: Patient) {
        val status = sharedPreferenceUseCases.savePatient(patient = patient)

        Logger.getLogger("PatientLoginAPILog")
            .info("PatientLoginAPILog : savePatient : $status   Patient :: ${Gson().toJson(sharedPreferenceUseCases.getPatient())}")


    }

}