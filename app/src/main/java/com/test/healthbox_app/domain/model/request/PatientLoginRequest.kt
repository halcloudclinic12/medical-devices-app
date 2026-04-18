package com.test.healthbox_app.domain.model.request

import com.google.gson.annotations.SerializedName

data class PatientLoginRequest(
    @SerializedName("mobile")
    var mobile: String? = null,
    @SerializedName("clinic_id")
    var clinicId: String? = null,
)
