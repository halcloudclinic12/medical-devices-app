package com.test.healthbox_app.presentation.tests.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.test.healthbox_app.data.model.BodyCheckupPref
import com.test.healthbox_app.data.model.mapper.toBasicTestRequestDto
import com.test.healthbox_app.data.model.mapper.toHba1cTestRequestDto
import com.test.healthbox_app.data.model.response.CreateBasicTestResponse
import com.test.healthbox_app.data.model.response.CreateHba1cTestResponse
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
        // resultsViewModel is activity-scoped, so _createBasicTestState still holds the
        // terminal ApiSuccess/ApiError from the PREVIOUS patient's submission when this screen
        // is revisited. ResultsFragment calls createBasicTest() then immediately
        // viewLifecycleOwner.lifecycleScope.launch { createBasicTestState.collect { ... } } —
        // on lifecycleScope's Main.immediate dispatcher that launch runs synchronously (no
        // dispatch) since we're already on the main thread, so StateFlow's replay of that
        // stale terminal value fires the ApiSuccess/ApiError branch, and its
        // CustomSnackBar.make(binding.root, ...) call, before onCreateView has returned and
        // the fragment's view is attached to a window — crashing with "No suitable parent
        // found from the given view." Resetting to ApiLoading here, synchronously and before
        // the fragment's collector is even launched, ensures that replay is always safe.
        _createBasicTestState.value = ApiResponse.ApiLoading()

        viewModelScope.launch(Dispatchers.IO) {

            val basicRequest = BodyCheckupPref.toBasicTestRequestDto()
            println("\ncreateBasicTestLog   :: Request Body:: ${Gson().toJson(basicRequest)}")

            patientsAPIUseCases.createBasicTest(
                basicTestRequest = basicRequest, authToken = "Bearer ${sharedPreferenceUseCases.getToken().toString()}"
            ).collect { it ->
                println("\ncreateBasicTestLog   :: Res Logs :: ${Gson().toJson(it)}")
                _createBasicTestState.value = it

            }

        }
    }

    private val _createHba1cTestState = MutableStateFlow<ApiResponse<CreateHba1cTestResponse>>(ApiResponse.ApiLoading())
    val createHba1cTestState: StateFlow<ApiResponse<CreateHba1cTestResponse>> get() = _createHba1cTestState

    fun createHba1cTest() {
        // Same replay hazard as createBasicTest() above — reset before the fragment's
        // collector can observe a stale terminal state from a previous visit.
        _createHba1cTestState.value = ApiResponse.ApiLoading()

        viewModelScope.launch(Dispatchers.IO) {

            val hba1cRequest = BodyCheckupPref.toHba1cTestRequestDto()
            println("\ncreateHba1cTestLog   :: Request Body:: ${Gson().toJson(hba1cRequest)}")

            patientsAPIUseCases.createHba1cTest(
                hba1cTestRequest = hba1cRequest, authToken = "Bearer ${sharedPreferenceUseCases.getToken().toString()}"
            ).collect { it ->
                println("\ncreateHba1cTestLog   :: Res Logs :: ${Gson().toJson(it)}")
                _createHba1cTestState.value = it
            }

        }
    }

    /**
     * Called from the Home button (activity-scoped ResultsViewModel survives that
     * navigation) so the next patient's Results visit doesn't start out holding this
     * patient's ApiSuccess/ApiError. createBasicTest()/createHba1cTest() already guard
     * against this at their own call sites, so this is belt-and-suspenders — it just
     * avoids leaving stale data sitting in these flows between patients.
     */
    fun resetTestState() {
        _createBasicTestState.value = ApiResponse.ApiLoading()
        _createHba1cTestState.value = ApiResponse.ApiLoading()
    }

    fun savePatient(patient: Patient) {
        val status = sharedPreferenceUseCases.savePatient(patient = patient)

        Logger.getLogger("PatientLoginAPILog")
            .info("PatientLoginAPILog : savePatient : $status   Patient :: ${Gson().toJson(sharedPreferenceUseCases.getPatient())}")


    }

}