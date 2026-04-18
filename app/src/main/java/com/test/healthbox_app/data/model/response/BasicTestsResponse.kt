package com.test.healthbox_app.data.model.response

import com.google.gson.annotations.SerializedName

data class BasicTestsResponse(
    @SerializedName("data") var data: BasicTestResData
)

data class BasicTestResData(
    @SerializedName("total") var total: Int? = null,
    @SerializedName("records") var records: ArrayList<BasicTestData> = arrayListOf()
)
