package com.test.healthbox_app.presentation.onboarding.mPin

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.test.healthbox_app.presentation.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MPinAuthenticateViewModel @Inject constructor() : ViewModel() {

    val mPin = MutableLiveData<String>()

    init {
        Log.e("LogsEnableLog", "  : : " + Constants.LOGS_ENABLE)
        if (Constants.LOGS_ENABLE) {
            mPin.value = "123456"
        }
    }
}