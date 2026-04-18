package com.test.healthbox_app.data.model.mapper

import com.test.healthbox_app.data.model.ParameterRanges
import com.test.healthbox_app.data.model.Parameters
import com.test.healthbox_app.data.model.response.BasicTestData

fun BasicTestData.toParametersList(): List<Parameters> {
    val list = mutableListOf<Parameters>()

    // ✅ Helper: gender-based selection
    val isMale = patient?.gender.equals("Male", ignoreCase = true)

    // -------------- Body Measurements --------------
    list.add(
        Parameters(
            parameterName = "Height",
            value = height,
            result = heightResult,
            range = null // height range is not meaningful
        )
    )
    list.add(
        Parameters(
            parameterName = "Weight",
            value = weight,
            result = weightResult,
            range = ParameterRanges.WEIGHT.values.joinToString(", ")
        )
    )
    list.add(
        Parameters(
            parameterName = "BMI",
            value = bmi,
            result = bmiResult,
            range = ParameterRanges.BMI.values.joinToString(", ")
        )
    )
    list.add(
        Parameters(
            parameterName = "BMR",
            value = bmr,
            result = bmrResult,
            range = null // BMR range depends on age/weight
        )
    )

    // -------------- Composition Parameters --------------
    list.add(
        Parameters(
            parameterName = "Body Fat",
            value = bodyFat,
            result = bodyFatResult,
            range = if (isMale) ParameterRanges.BODY_FAT_MALE.values.joinToString(", ")
            else ParameterRanges.BODY_FAT_FEMALE.values.joinToString(", ")
        )
    )
    list.add(
        Parameters(
            parameterName = "Subcutaneous Fat",
            value = subcutaneousFat,
            result = subcutaneousFatResult,
            range = if (isMale) ParameterRanges.SUBCUTANEOUS_FAT_MALE.values.joinToString(", ")
            else ParameterRanges.SUBCUTANEOUS_FAT_FEMALE.values.joinToString(", ")
        )
    )
    list.add(
        Parameters(
            parameterName = "Visceral Fat",
            value = visceralFat,
            result = visceralFatResult,
            range = if (isMale) ParameterRanges.VISCERAL_FAT_MALE.values.joinToString(", ")
            else ParameterRanges.VISCERAL_FAT_FEMALE.values.joinToString(", ")
        )
    )
    list.add(
        Parameters(
            parameterName = "Body Water",
            value = bodyWater,
            result = bodyWaterResult,
            range = if (isMale) ParameterRanges.BODY_WATER_MALE.values.joinToString(", ")
            else ParameterRanges.BODY_WATER_FEMALE.values.joinToString(", ")
        )
    )
    list.add(
        Parameters(
            parameterName = "Protein",
            value = protein,
            result = proteinResult,
            range = ParameterRanges.PROTEIN.values.joinToString(", ")
        )
    )
    list.add(
        Parameters(
            parameterName = "Muscle Mass",
            value = muscleMass,
            result = muscleMassResult,
            range = if (isMale) ParameterRanges.MUSCLE_MASS_MALE.values.joinToString(", ")
            else ParameterRanges.MUSCLE_MASS_FEMALE.values.joinToString(", ")
        )
    )
    list.add(
        Parameters(
            parameterName = "Muscle Rate",
            value = muscleRate,
            result = muscleRateResult,
            range = ParameterRanges.MUSCLE_RATE.values.joinToString(", ")
        )
    )
    list.add(
        Parameters(
            parameterName = "Bone Mass",
            value = boneMass,
            result = boneMassResult,
            range = if (isMale) ParameterRanges.BONE_MASS_MALE.values.joinToString(", ")
            else ParameterRanges.BONE_MASS_FEMALE.values.joinToString(", ")
        )
    )

    // -------------- Vital Parameters --------------
    list.add(
        Parameters(
            parameterName = "Temperature",
            value = temperature,
            result = temperatureResult,
            range = ParameterRanges.TEMPERATURE["Normal"]
        )
    )
    list.add(
        Parameters(
            parameterName = "Pulse",
            value = pulse,
            result = pulseResult,
            range = ParameterRanges.PULSE.values.joinToString(", ")
        )
    )
    /*list.add(
        Parameters(
            parameterName = "Oxygen (SpO₂)",
            value = oxygen,
            result = oxygenResult,
            range = ParameterRanges.OXYGEN.values.joinToString(", ")
        )
    )*/
    list.add(
        Parameters(
            parameterName = "Sugar",
            value = sugar,
            result = sugarResult,
            range = ParameterRanges.SUGAR.values.joinToString(", ")
        )
    )
    list.add(
        Parameters(
            parameterName = "Hemoglobin",
            value = hemoglobin,
            result = hemoglobinResult,
            range = if (isMale) ParameterRanges.HEMOGLOBIN_MALE["Normal"]
//            range = if (isMale) ParameterRanges.HEMOGLOBIN_MALE.values.joinToString(", ")
            else ParameterRanges.HEMOGLOBIN_FEMALE.values.joinToString(", ")
        )
    )

    // -------------- Others --------------
    list.add(
        Parameters(
            parameterName = "Eye Left Vision",
            value = eyeLeftVision,
            result = eyeLeftResult,
            range = "6.6"
        )
    )
    list.add(
        Parameters(
            parameterName = "Eye Right Vision",
            value = eyeRightVision,
            result = eyeRightResult,
            range = "6.6"
        )
    )

    list.add(
        Parameters(
            parameterName = "Blood Pressure (Systolic)",
            value = bloodPressureSystolic,
            result = null,
            range = null // You can define if you want
        )
    )
    list.add(
        Parameters(
            parameterName = "Blood Pressure (Diastolic)",
            value = bloodPressureDiastolic,
            result = null,
            range = null
        )
    )

    return list.filter { !it.value.isNullOrEmpty() } // ✅ Only include those with values

}

