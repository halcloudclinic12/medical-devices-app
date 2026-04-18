package com.test.healthbox_app.domain.model.request

import com.google.gson.annotations.SerializedName

data class RefreshTokenRequest(
    @SerializedName("refresh_token") var refreshToken: String? = null
)
