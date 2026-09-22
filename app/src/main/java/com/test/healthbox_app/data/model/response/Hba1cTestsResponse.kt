package com.test.healthbox_app.data.model.response

import com.google.gson.annotations.SerializedName

data class Hba1cTestsResponse(
    @SerializedName("data") var data: Hba1cTestResData
)

data class Hba1cTestResData(
    @SerializedName("total") var total: Int? = null,
    @SerializedName("records") var records: ArrayList<Hba1cTestData> = arrayListOf()
)
