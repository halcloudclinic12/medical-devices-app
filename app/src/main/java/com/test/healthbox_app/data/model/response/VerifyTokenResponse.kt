package com.test.healthbox_app.data.model.response

import com.google.gson.annotations.SerializedName

data class VerifyTokenResponse(
    @SerializedName("data") var data: VerifyTokenData?
)

data class VerifyTokenData(
    @SerializedName("valid") var valid: Boolean? = null,

    )
