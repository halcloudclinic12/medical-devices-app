package com.test.healthbox_app.domain.model.mapper

import cn.net.aicare.MoreFatData
import cn.net.aicare.algorithmutil.BodyFatData
import com.test.healthbox_app.domain.model.WeightMeasurement

fun mapToBodyParameters(
    weight: Double,
    bodyFatData: BodyFatData, moreFatData: MoreFatData
): List<WeightMeasurement> {
    return listOf(
        WeightMeasurement("Standard Weight", "${weight}", "kg", isValid = true),
        WeightMeasurement("Visceral Fat", "${bodyFatData.uvi}", "%", isValid = true),
        WeightMeasurement("Subcutaneous Fat", "${bodyFatData.sfr}", "%", isValid = true),
        WeightMeasurement("Protein", "${bodyFatData.pp}", "%", isValid = true),
        WeightMeasurement("Muscle Rate", "${bodyFatData.rom}", "%", isValid = true),
        WeightMeasurement("Muscle Mass", "${moreFatData.muscleMass}", "kg", isValid = true),
        WeightMeasurement("Metabolic Age", "${bodyFatData.bodyAge}", "years"),
        WeightMeasurement("Lean Body Weight", "${moreFatData.removeFatWeight}", "kg", isValid = true),
        WeightMeasurement("Bone Mass", "${bodyFatData.bm}", "kg", isValid = true),
        WeightMeasurement("Body Water Rate", "${bodyFatData.vwc}", "kg", isValid = true),
        WeightMeasurement("BMI", "${bodyFatData.bmi}", "Kg/m2", isValid = true),
        WeightMeasurement("Body Fat Rate", "${bodyFatData.bfr}", "%", isValid = true),
        WeightMeasurement("Basal Metabolic Rate", "${bodyFatData.bmr}", "kcal", isValid = true),
        WeightMeasurement("Fat Level", "${moreFatData.fatLevel}", ""),
        WeightMeasurement("Control Weight", "${moreFatData.controlWeight}", "kg"),
    )
}