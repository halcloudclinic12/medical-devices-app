package com.test.healthbox_app.domain.model.request

import com.google.gson.annotations.SerializedName

data class ClinicLoginRequest(
    @SerializedName("clinic_id")
    var clinicId: String? = null,
    @SerializedName("password")
    var password: String? = null
)
