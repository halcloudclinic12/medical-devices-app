package com.test.healthbox_app.data.model.response

import com.google.gson.annotations.SerializedName

data class State(
    @SerializedName("_id") var Id: String? = null,
    @SerializedName("code") var code: String? = null,
    @SerializedName("name") var name: String? = null,
    @SerializedName("country_id") var countryId: String? = null,
    @SerializedName("created_at") var createdAt: String? = null,
    @SerializedName("is_active") var isActive: Boolean? = null,
    @SerializedName("unique_id") var uniqueId: String? = null,
    @SerializedName("__v") var _v: Int? = null
)
