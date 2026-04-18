package com.test.healthbox_app.data.model.response

import com.google.gson.annotations.SerializedName

data class ClinicData(
    @SerializedName("valid") var valid: Boolean? = null,
    @SerializedName("token") var token: String? = null,
    @SerializedName("clinic") var clinic: Clinic? = Clinic(),
    @SerializedName("refresh_token") var refreshToken: String? = null,
    @SerializedName("message") var message: String? = null,
)
