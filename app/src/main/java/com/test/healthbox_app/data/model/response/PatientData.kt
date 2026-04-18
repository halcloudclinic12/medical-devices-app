package com.test.healthbox_app.data.model.response

data class PatientData(
    val message: String,
    val success: Boolean,
    val patient: Patient
)