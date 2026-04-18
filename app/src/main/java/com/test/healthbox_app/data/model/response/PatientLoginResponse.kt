package com.test.healthbox_app.data.model.response

import com.google.gson.annotations.SerializedName

data class PatientLoginResponse(
    @SerializedName("data") val data: PatientData
)