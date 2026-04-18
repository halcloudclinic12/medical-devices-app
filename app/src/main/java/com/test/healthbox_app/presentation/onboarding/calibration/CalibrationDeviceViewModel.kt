package com.test.healthbox_app.presentation.onboarding.calibration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.test.healthbox_app.data.data_source.CalibrationOperators
import com.test.healthbox_app.data.data_source.CalibrationType
import com.test.healthbox_app.domain.use_cases.SharedPreferenceUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalibrationDeviceViewModel @Inject constructor(
    val sharedPreferenceUseCases: SharedPreferenceUseCases
) : ViewModel() {

    // Error observables
    private val _formError = MutableLiveData<String?>()
    val formError: LiveData<String?> = _formError

    private val _calibrationSave = MutableLiveData<Boolean>()
    val calibrationSave: LiveData<Boolean> = _calibrationSave

    val selectedCalibrationType = MutableLiveData<CalibrationType?>()
    val selectedCalibrationOperators = MutableLiveData<CalibrationOperators?>()

    val calibrationValue = MutableLiveData<String?>()

    private val _calibrationState = MutableLiveData<Pair<CalibrationOperators, Float>?>()

    val calibrationState: LiveData<Pair<CalibrationOperators, Float>?>
        get() = _calibrationState

    init {
        loadCalibration(CalibrationType.HEIGHT)
    }

    fun saveCalibration() {
        viewModelScope.launch {
            if (selectedCalibrationType.value == null) {
                _formError.postValue("Please select the device type.")
                return@launch
            } else if (selectedCalibrationOperators.value == null) {
                _formError.postValue("Please select the operator type.")
                return@launch
            } else if (calibrationValue.value.isNullOrEmpty()) {
                _formError.postValue("Please enter calibration value.")
                return@launch
            }

            selectedCalibrationType.value?.let {
                selectedCalibrationOperators.value?.let { operator ->
                    calibrationValue.value?.let { value ->

                        _calibrationSave.value = sharedPreferenceUseCases.saveCalibration(
                            calibrationType = it, operator = operator, value = value.toFloat()
                        )

                    }
                }
            }
        }

    }

    fun loadCalibration(calibrationType: CalibrationType) {
        val res = sharedPreferenceUseCases.getCalibration(calibrationType = calibrationType)

        if (res != null) {
            _calibrationState.value = res
        } else {
            selectedCalibrationOperators.value = null
            calibrationValue.value = null
        }

        calibrationState.value?.let { (operator, value) ->
            selectedCalibrationType.value = CalibrationType.HEIGHT
            calibrationState.value

        }
    }

    fun clearSelectedData() {
        selectedCalibrationType.value = null
        selectedCalibrationOperators.value = null
        calibrationValue.value = null
    }


}