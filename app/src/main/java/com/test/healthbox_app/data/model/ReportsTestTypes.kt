package com.test.healthbox_app.data.model

data class ReportsTestTypes(
    val testTypesList: List<ReportTestType>
)

data class ReportTestType(
    var title: String? = null,
    var isSelected: Boolean? = null
) {
    companion object {
        fun typesList() = listOf(
            ReportTestType(title = "Basic Health", isSelected = true),
            ReportTestType(title = "HbA1c", isSelected = false),
            ReportTestType(title = "Lipid", isSelected = false),
            ReportTestType(title = "Rapid", isSelected = false),
        )
    }
}