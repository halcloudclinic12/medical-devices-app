package com.test.healthbox_app.presentation.util

enum class BasicHealthTestsType(val stepNumber: Int, val stepName: String) {
    HEIGHT(1, "Height"),
    TEMPERATURE(2, "Temperature"),
    SPO2(3, "SPO2"),
    WEIGHT(4, "Weight"),
    VISION(5, "Vision"),
    BLOOD_PRESSURE(6, "Blood Pressure"),
    BLOOD_SUGAR(7, "Blood Sugar"),
    HEMOGLOBIN(8, "Hemoglobin");

    companion object {
        fun fromStepNumber(stepNumber: Int): BasicHealthTestsType? {
            return entries.find { it.stepNumber == stepNumber }
        }
    }
}