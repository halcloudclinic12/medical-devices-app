package com.test.healthbox_app.data.model.response

import com.google.gson.annotations.SerializedName

data class Patient(
    @SerializedName("_id") var id: String? = null,
    @SerializedName("email") var email: String? = null,
    @SerializedName("name") var name: String? = null,
    @SerializedName("bloodGroup") var bloodGroup: String? = null,
    @SerializedName("mobile") var mobile: String? = null,
    @SerializedName("gender") var gender: String? = null,
//    @SerializedName("last_name") var lastName: String? = null,
    @SerializedName("created_at") var createdAt: String? = null,
//    @SerializedName("first_name") var firstName: String? = null,
    @SerializedName("app_version") var appVersion: String? = null,
    @SerializedName("date_of_birth") var dateOfBirth: String? = null,
    @SerializedName("patient_token") var patientToken: String? = null,
)

