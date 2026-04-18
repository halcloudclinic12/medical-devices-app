package com.test.healthbox_app.domain.model.request

import com.google.gson.annotations.SerializedName

data class PatientUpdateRequest(
    @SerializedName("name")
    var name: String? = null,
    @SerializedName("gender")
    var gender: String? = null,
    @SerializedName("email")
    var email: String? = null,
    @SerializedName("blood_group")
    var bloodGroup: String? = null,
    @SerializedName("mobile")
    var mobile: String? = null,
    @SerializedName("date_of_birth")
    var dateOfBirth: String? = null
)
