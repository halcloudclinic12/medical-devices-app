package com.test.healthbox_app.data.model.mapper

import com.test.healthbox_app.data.model.BodyCheckupPref
import com.test.healthbox_app.domain.model.request.BasicTestRequest
import com.test.healthbox_app.domain.model.request.Hba1cTestRequest

fun BodyCheckupPref.toBasicTestRequestDto(): BasicTestRequest {
    return BasicTestRequest(
        patientId = patient_ID ?: "",
        clinicId = clinic_ID ?: "",

        height = height,
        heightResult = height_result,

        weight = weight,
        weightResult = weight_result,

        bmi = bmi,
        bmiResult = bmi_result,

        bmr = bmr,
        bmrResult = bmr_result,

        boneMass = bone_mass,
        boneMassResult = bone_mass_result,

        bodyFat = body_fat,
        bodyFatResult = body_fat_result,

        leanBodyWeight = lean_body_weight,
        leanBodyWeightResult = lean_body_weight_result,

        muscleMass = muscle_mass,
        muscleMassResult = muscle_mass_result,

        muscleRate = muscle_rate,
        muscleRateResult = muscle_rate_result,

        subcutaneousFat = subcutaneous_fat,
        subcutaneousFatResult = subcutaneous_fat_result,

        visceralFat = visceral_fat,
        visceralFatResult = visceral_fat_result,

        bodyWater = body_water,
        bodyWaterResult = body_water_result,

        metaAge = meta_age,
        metaAgeResult = meta_age_result,

        protein = protein,
        proteinResult = protein_result,

        fatLevel = fat_level,
        controlWeight = control_weight,

        temperature = temperature,
        temperatureResult = temperature_result,

        bloodPressureDiastolic = blood_pressure_diastolic,
        bloodPressureDiastolicResult = blood_pressure_diastolic_result,

        bloodPressureSystolic = blood_pressure_systolic,
        bloodPressureSystolicResult = blood_pressure_systolic_result,

        eyeLeftVision = eye_left_vision,
        eyeLeftResult = eye_left_result,

        eyeRightVision = eye_right_vision,
        eyeRightResult = eye_right_result,

        oxygen = oxygen,
        oxygenResult = oxygen_result,

        pulse = pulse,
        pulseResult = pulse_result,

        sugar = sugar,
        sugarResult = sugar_result,

        hemoglobin = hemoglobin,
        hemoglobinResult = hemoglobin_result
    )
}

fun BodyCheckupPref.toHba1cTestRequestDto(): Hba1cTestRequest {
    return Hba1cTestRequest(
        patientId = patient_ID ?: "",
        clinicId = clinic_ID ?: "",

        hba1c = hba1c,
        hba1cResult = hba1c_result
    )
}