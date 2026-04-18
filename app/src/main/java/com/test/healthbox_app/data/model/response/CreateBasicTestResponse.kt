package com.test.healthbox_app.data.model.response

import com.google.gson.annotations.SerializedName

data class CreateBasicTestResponse(
    @SerializedName("data") val data: BasicTestData

)
