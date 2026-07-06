package com.test.healthbox_app.data.model

data class ReportsTestTypes(
    val testTypesList: List<ReportTestType>
)

data class ReportTestType(
    var title: String? = null,
    var isSelected: Boolean? = null,
    var testType: String? = null

) {
    companion object {
        fun typesList() = listOf(
            ReportTestType(title = "Basic Health", isSelected = true, testType = "BASIC"),
            ReportTestType(title = "HbA1c", isSelected = false, testType = "HBA1C"),
            ReportTestType(title = "Lipid", isSelected = false, testType = "LIPID"),
            ReportTestType(title = "Rapid", isSelected = false, testType = "RAPID"),
        )
    }
}