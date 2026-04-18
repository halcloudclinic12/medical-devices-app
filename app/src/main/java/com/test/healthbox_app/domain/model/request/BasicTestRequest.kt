package com.test.healthbox_app.domain.model.request

import com.google.gson.annotations.SerializedName

data class BasicTestRequest(
    @SerializedName("patient_id") val patientId: String,
    @SerializedName("clinic_id") val clinicId: String,

    @SerializedName("height") val height: String? = null,
    @SerializedName("height_result") val heightResult: String? = null,

    @SerializedName("weight") val weight: String? = null,
    @SerializedName("weight_result") val weightResult: String? = null,

    @SerializedName("bmi") val bmi: String? = null,
    @SerializedName("bmi_result") val bmiResult: String? = null,

    @SerializedName("bmr") val bmr: String? = null,
    @SerializedName("bmr_result") val bmrResult: String? = null,

    @SerializedName("bone_mass") val boneMass: String? = null,
    @SerializedName("bone_mass_result") val boneMassResult: String? = null,

    @SerializedName("body_fat") val bodyFat: String? = null,
    @SerializedName("body_fat_result") val bodyFatResult: String? = null,

    @SerializedName("lean_body_weight") val leanBodyWeight: String? = null,
    @SerializedName("lean_body_weight_result") val leanBodyWeightResult: String? = null,

    @SerializedName("muscle_mass") val muscleMass: String? = null,
    @SerializedName("muscle_mass_result") val muscleMassResult: String? = null,

    @SerializedName("muscle_rate") val muscleRate: String? = null,
    @SerializedName("muscle_rate_result") val muscleRateResult: String? = null,

    @SerializedName("subcutaneous_fat") val subcutaneousFat: String? = null,
    @SerializedName("subcutaneous_fat_result") val subcutaneousFatResult: String? = null,

    @SerializedName("visceral_fat") val visceralFat: String? = null,
    @SerializedName("visceral_fat_result") val visceralFatResult: String? = null,

    @SerializedName("body_water") val bodyWater: String? = null,
    @SerializedName("body_water_result") val bodyWaterResult: String? = null,

    @SerializedName("meta_age") val metaAge: String? = null,
    @SerializedName("meta_age_result") val metaAgeResult: String? = null,

    @SerializedName("protein") val protein: String? = null,
    @SerializedName("protein_result") val proteinResult: String? = null,

    @SerializedName("fat_level") val fatLevel: String? = null,
    @SerializedName("control_weight") val controlWeight: String? = null,

    @SerializedName("temperature") val temperature: String? = null,
    @SerializedName("temperature_result") val temperatureResult: String? = null,

    @SerializedName("blood_pressure_diastolic") val bloodPressureDiastolic: String? = null,
    @SerializedName("blood_pressure_diastolic_result") val bloodPressureDiastolicResult: String? = null,

    @SerializedName("blood_pressure_systolic") val bloodPressureSystolic: String? = null,
    @SerializedName("blood_pressure_systolic_result") val bloodPressureSystolicResult: String? = null,

    @SerializedName("eye_left_vision") val eyeLeftVision: String? = null,
    @SerializedName("eye_left_result") val eyeLeftResult: String? = null,

    @SerializedName("eye_right_vision") val eyeRightVision: String? = null,
    @SerializedName("eye_right_result") val eyeRightResult: String? = null,

    @SerializedName("oxygen") val oxygen: String? = null,
    @SerializedName("oxygen_result") val oxygenResult: String? = null,

    @SerializedName("pulse") val pulse: String? = null,
    @SerializedName("pulse_result") val pulseResult: String? = null,

    @SerializedName("sugar") val sugar: String? = null,
    @SerializedName("sugar_result") val sugarResult: String? = null,

    @SerializedName("hemoglobin") val hemoglobin: String? = null,
    @SerializedName("hemoglobin_result") val hemoglobinResult: String? = null
)