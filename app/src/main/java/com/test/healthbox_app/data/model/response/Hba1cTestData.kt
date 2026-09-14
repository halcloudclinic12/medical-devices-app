package com.test.healthbox_app.data.model.response

import com.google.gson.annotations.SerializedName

/**
 * Matches the real POST /api/v1/test/hba1c response, confirmed against the dev API
 * (2026-09-13). Deliberately NOT reusing BasicTestData's nested Clinic/Patient
 * classes — this endpoint nests smaller, differently-shaped "patient_data"/
 * "clinic_data" objects instead of "patient"/"clinic".
 */
data class Hba1cTestData(
    @SerializedName("created_at") var createdAt: String? = null,
    @SerializedName("is_active") var isActive: Boolean? = null,
    @SerializedName("is_deleted") var isDeleted: Boolean? = null,
    @SerializedName("unique_id") var uniqueId: String? = null,
    @SerializedName("customer_id") var customerId: String? = null,
    @SerializedName("clinic_id") var clinicId: String? = null,
    @SerializedName("patient_id") var patientId: String? = null,
    @SerializedName("city_id") var cityId: String? = null,
    @SerializedName("state_id") var stateId: String? = null,
    @SerializedName("country_id") var countryId: String? = null,
    @SerializedName("sync_id") var syncId: String? = null,

    @SerializedName("test_type") var testType: String? = null,
    @SerializedName("hba1c") var hba1c: String? = null,
    @SerializedName("hba1c_result") var hba1cResult: String? = null,

    @SerializedName("_id") var Id: String? = null,
    @SerializedName("__v") var _v: Int? = null,

    @SerializedName("patient_data") var patientData: Patient? = Patient(),
    @SerializedName("clinic_data") var clinicData: Clinic? = Clinic()
)