package com.test.healthbox_app.data.model.response

import com.google.gson.annotations.SerializedName

data class ClinicLoginResponse(
    @SerializedName("data") var data: ClinicData? = ClinicData()
)
