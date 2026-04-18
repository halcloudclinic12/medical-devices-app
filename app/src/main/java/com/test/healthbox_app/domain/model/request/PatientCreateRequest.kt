package com.test.healthbox_app.domain.model.request

import com.google.gson.annotations.SerializedName

data class PatientCreateRequest(
    @SerializedName("clinic_id")
    var clinic_id: String? = null,

    @SerializedName("email")
    var email: String? = null,

    @SerializedName("first_name")
    var first_name: String? = null,

    @SerializedName("gender")
    var gender: String? = null,

    @SerializedName("last_name")
    var last_name: String? = null,

    @SerializedName("mobile")
    var mobile: String? = null
)