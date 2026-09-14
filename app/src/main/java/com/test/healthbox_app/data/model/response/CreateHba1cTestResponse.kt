package com.test.healthbox_app.data.model.response

import com.google.gson.annotations.SerializedName

data class CreateHba1cTestResponse(
    @SerializedName("data") val data: Hba1cTestData
)