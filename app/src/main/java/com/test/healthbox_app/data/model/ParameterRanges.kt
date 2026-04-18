package com.test.healthbox_app.data.model

object ParameterRanges {
    // ---------------- BMI ----------------
    val BMI = mapOf(
        "Underweight" to "0-18.4",
        "Normal" to "18.5-24.9",
        "Overweight" to "25-29.9",
        "Obese" to "30-1000"
    )

    // ---------------- Weight (as ratio vs standard weight) ----------------
    val WEIGHT = mapOf(
        "Low" to "0-0.89",
        "Standard" to "0.90-1.09",
        "High" to "1.10-100"
    )

    // ---------------- Body Fat % ----------------
    val BODY_FAT_MALE = mapOf(
        "Low" to "0-5.9",
        "Normal" to "6-24.9",
        "High" to "25-100"
    )
    val BODY_FAT_FEMALE = mapOf(
        "Low" to "0-13.9",
        "Normal" to "14-31.9",
        "High" to "32-100"
    )

    // BMR multipliers (age → multiplier)
    val BMR_MALE = listOf(
        16..29 to 24.0,
        30..49 to 22.3,
        50..69 to 21.5,
        70..150 to 21.5
    )

    val BMR_FEMALE = listOf(
        18..29 to 23.6,
        30..49 to 21.7,
        50..69 to 20.7,
        70..150 to 20.7
    )

    // ---------------- Subcutaneous Fat % ----------------
    val SUBCUTANEOUS_FAT_MALE = mapOf(
        "Low" to "0-7.9",
        "Normal" to "8-25",
        "High" to "25.1-100"
    )
    val SUBCUTANEOUS_FAT_FEMALE = mapOf(
        "Low" to "0-17.9",
        "Normal" to "18-30",
        "High" to "30.1-100"
    )

    // ---------------- Visceral Fat Index ----------------
    val VISCERAL_FAT_MALE = mapOf(
        "Normal" to "0-9",
        "High" to "10-14",
        "Very High" to "15-100"
    )
    val VISCERAL_FAT_FEMALE = mapOf(
        "Normal" to "0-12",
        "High" to "13-16",
        "Very High" to "17-100"
    )

    // ---------------- Body Water % ----------------
    val BODY_WATER_MALE = mapOf(
        "Low" to "0-49.9",
        "Normal" to "50-65",
        "High" to "65.1-100"
    )
    val BODY_WATER_FEMALE = mapOf(
        "Low" to "0-44.9",
        "Normal" to "45-60",
        "High" to "60.1-100"
    )

    // ---------------- Protein % ----------------
    val PROTEIN = mapOf(
        "Low" to "0-15.9",
        "Normal" to "16-20",
        "High" to "20.1-100"
    )

    // ---------------- Muscle Mass (kg) ----------------
    val MUSCLE_MASS_MALE = mapOf(
        "Low" to "0-39.9",
        "Normal" to "40-50",
        "High" to "50.1-100"
    )
    val MUSCLE_MASS_FEMALE = mapOf(
        "Low" to "0-29.9",
        "Normal" to "30-40",
        "High" to "40.1-100"
    )

    // ---------------- Muscle Rate % ----------------
    val MUSCLE_RATE = mapOf(
        "Low" to "0-32.9",
        "Normal" to "33-39",
        "High" to "39.1-100"
    )

    // ---------------- Bone Mass (kg) ----------------
    val BONE_MASS_MALE = mapOf(
        "Low" to "0-2.49",
        "Normal" to "2.5-3.2",
        "High" to "3.21-100"
    )
    val BONE_MASS_FEMALE = mapOf(
        "Low" to "0-1.79",
        "Normal" to "1.8-2.5",
        "High" to "2.51-100"
    )

    // ---------------- Temperature (°F) ----------------
    val TEMPERATURE = mapOf(
        "Low" to "0-96.9",
        "Normal" to "97-99",
        "High" to "99.1-110"
    )

    // ---------------- Pulse (bpm) ----------------
    val PULSE = mapOf(
        "Low" to "0-59",
        "Normal" to "60-100",
        "High" to "101-300"
    )

    // ---------------- Oxygen (SpO2 %) ----------------
    val OXYGEN = mapOf(
        "Critical" to "0-89",
        "Low" to "90-94",
        "Normal" to "95-100"
    )

    // ---------------- Sugar (mg/dL) ----------------
    val SUGAR = mapOf(
        "Normal" to "0-99",
        "Prediabetes" to "100-125",
        "Diabetes" to "126-500"
    )

    // ---------------- Hemoglobin (g/dL) ----------------
    val HEMOGLOBIN_MALE = mapOf(
        "Low" to "0-13.7",
        "Normal" to "13.8-17.2",
        "High" to "17.3-30"
    )
    val HEMOGLOBIN_FEMALE = mapOf(
        "Low" to "0-12",
        "Normal" to "12.1-15.1",
        "High" to "15.2-30"
    )
}
