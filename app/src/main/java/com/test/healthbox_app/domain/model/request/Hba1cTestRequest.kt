package com.test.healthbox_app.domain.model.request

import com.google.gson.annotations.SerializedName

data class Hba1cTestRequest(
    @SerializedName("patient_id") val patientId: String,
    @SerializedName("clinic_id") val clinicId: String,
    @SerializedName("test_type") val testType: String = "HBA1C",

    @SerializedName("hba1c") val hba1c: String? = null,
    @SerializedName("hba1c_result") val hba1cResult: String? = null
)