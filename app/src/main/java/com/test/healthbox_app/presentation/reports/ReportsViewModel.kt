package com.test.healthbox_app.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.test.healthbox_app.data.model.PatientPref
import com.test.healthbox_app.data.model.ReportTestType
import com.test.healthbox_app.data.model.ReportsTestTypes
import com.test.healthbox_app.data.model.response.BasicTestsResponse
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
class ReportsViewModel @Inject constructor(
    private val patientsAPIUseCases: PatientsAPIUseCases, private val sharedPreferenceUseCases: SharedPreferenceUseCases
) : ViewModel() {

    private val _getBasicTestState = MutableStateFlow<ApiResponse<BasicTestsResponse>>(ApiResponse.ApiLoading())
    val getBasicTestState: StateFlow<ApiResponse<BasicTestsResponse>> get() = _getBasicTestState

    private val _reportTypes = MutableStateFlow(
        ReportsTestTypes(testTypesList = ReportTestType.typesList())
    )
    val reportTypes: StateFlow<ReportsTestTypes> = _reportTypes

    init {
        if (Constants.LOGS_ENABLE) {

        }
    }

    // 2️⃣ Update selection when user taps a type
    fun onReportTypeSelected(selectedTitle: String) {
        val updatedList = _reportTypes.value.testTypesList.map { type ->
            type.copy(isSelected = type.title == selectedTitle)
        }
        _reportTypes.value = ReportsTestTypes(testTypesList = updatedList)
    }

    // Optional: clear all selections
    fun clearSelection() {
        _reportTypes.value = ReportsTestTypes(
            testTypesList = _reportTypes.value.testTypesList.map { it.copy(isSelected = false) }
        )
    }


    fun getBasicTest() {
        viewModelScope.launch(Dispatchers.IO) {

            PatientPref.patient?.let {
                patientsAPIUseCases.getBasicTest(
                    patientId = it.id.toString(), authToken = "Bearer ${sharedPreferenceUseCases.getToken().toString()}"
                ).collect { it ->
                    println("\ncreateBasicTestLog   :: Res Logs :: ${Gson().toJson(it)}")
                    _getBasicTestState.value = it
                }
            }


        }
    }

    fun savePatient(patient: Patient) {
        val status = sharedPreferenceUseCases.savePatient(patient = patient)

        Logger.getLogger("PatientLoginAPILog")
            .info("PatientLoginAPILog : savePatient : $status   Patient :: ${Gson().toJson(sharedPreferenceUseCases.getPatient())}")


    }

}