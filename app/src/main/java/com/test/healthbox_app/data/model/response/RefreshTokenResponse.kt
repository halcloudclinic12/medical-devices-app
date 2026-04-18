package com.test.healthbox_app.data.model.response

import com.google.gson.annotations.SerializedName

data class RefreshTokenResponse(
    @SerializedName("data") var data: RefreshTokenData?,
)

data class RefreshTokenData(
    @SerializedName("token") var token: String? = null,
    @SerializedName("refreshToken") var refreshToken: String? = null,
    @SerializedName("success") var success: Boolean?,
    @SerializedName("message") var message: String?,
    @SerializedName("error") var error: RefreshTokenError?

)

data class RefreshTokenError(
    @SerializedName("message") var message: String?,
)
