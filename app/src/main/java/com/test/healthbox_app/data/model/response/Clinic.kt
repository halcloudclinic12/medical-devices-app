package com.test.healthbox_app.data.model.response

import com.google.gson.annotations.SerializedName

data class Clinic(
    @SerializedName("_id") var Id: String? = null,
    @SerializedName("name") var name: String? = null,
    @SerializedName("email") var email: String? = null,
    @SerializedName("address") var address: String? = null,
    @SerializedName("city_id") var cityId: String? = null,
    @SerializedName("state_id") var stateId: String? = null,
    @SerializedName("is_active") var isActive: Boolean? = null,
    @SerializedName("clinic_id") var clinicId: String? = null,
    @SerializedName("country_id") var countryId: String? = null,
    @SerializedName("customer_id") var customerId: String? = null,
    @SerializedName("is_verified") var isVerified: Boolean? = null,
    @SerializedName("phone_number") var phoneNumber: String? = null,
    @SerializedName("is_test_account") var isTestAccount: Boolean? = null,
    @SerializedName("date_of_establishment") var dateOfEstablishment: String? = null,
    @SerializedName("city") var city: City? = City(),
    @SerializedName("state") var state: State? = State()
)

