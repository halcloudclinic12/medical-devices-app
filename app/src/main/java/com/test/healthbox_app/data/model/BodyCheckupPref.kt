package com.test.healthbox_app.data.model

/*{
    "patient_id": "string",
    "clinic_id": "string",
    "height": "string",
    "height_result": "string",
    "weight": "string",
    "weight_result": "string",
    "bmi": "string",
    "bmi_result": "string",
    "bmr": "string",
    "bmr_result": "string",
    "bone_mass": "string",
    "bone_mass_result": "string",
    "body_fat": "string",
    "body_fat_result": "string",
    "lean_body_weight": "string",
    "lean_body_weight_result": "string",
    "muscle_mass": "string",
    "muscle_mass_result": "string",
    "muscle_rate": "string",
    "muscle_rate_result": "string",
    "subcutaneous_fat": "string",
    "subcutaneous_fat_result": "string",
    "visceral_fat": "string",
    "visceral_fat_result": "string",
    "body_water": "string",
    "body_water_result": "string",
    "meta_age": "string",
    "meta_age_result": "string",
    "protein": "string",
    "protein_result": "string",
    "fat_level": "string",
    "control_weight": "string",
    "temperature": "string",
    "temperature_result": "string",
    "blood_pressure_diastolic": "string",
    "blood_pressure_diastolic_result": "string",
    "blood_pressure_systolic": "string",
    "blood_pressure_systolic_result": "string",
    "eye_left_vision": "string",
    "eye_left_result": "string",
    "eye_right_vision": "string",
    "eye_right_result": "string",
    "oxygen": "string",
    "oxygen_result": "string",
    "pulse": "string",
    "pulse_result": "string",
    "sugar": "string",
    "sugar_result": "string",
    "hemoglobin": "string",
    "hemoglobin_result": "string"
}*/
object BodyCheckupPref {
    // Gender: "Male" or "Female" (used to apply sex-specific ranges)

    var patient_ID: String? = null
    var clinic_ID: String? = null

    var gender: String? = null
    private val isMale: Boolean get() = gender == "Male"

    //    var age: String? = DatePickerUtil.getAgeFromDob(PatientPref.patient?.dateOfBirth.toString()).toString() // required for BMR classification
    var age: String? = null

    // ----------------- Generic Helper -----------------
    private fun getResultFromRange(value: Double, ranges: Map<String, String>): String? {
        for ((label, range) in ranges) {
            val parts = range.split("-")
            val min = parts.getOrNull(0)?.toDoubleOrNull() ?: Double.NEGATIVE_INFINITY
            val max = parts.getOrNull(1)?.toDoubleOrNull() ?: Double.POSITIVE_INFINITY
            if (value in min..max) return label
        }
        return null
    }

    // ----------------- Example Parameters -----------------
    var height: String? = null
    var height_result: String? = ""
    var weight: String? = null
        set(value) {
            field = value
            weight_result = getWeightResult(value, height?.toDoubleOrNull())
        }
    var weight_result: String? = null

    var bmi: String? = null
        set(value) {
            field = value
            bmi_result = value?.toDoubleOrNull()?.let { getResultFromRange(it, ParameterRanges.BMI) }
        }
    var bmi_result: String? = null

    var bmr: String? = null
        set(value) {
            field = value
            // BMR comes from UI; we only classify it here
            bmr_result = getBmrResult(
                actualBmrStr = value, ageStr = age, weightStr = weight,   // we use current weight only to compute the "normal" baseline
                isMale = isMale
            )
        }
    var bmr_result: String? = null

    var body_fat: String? = null
        set(value) {
            field = value
            body_fat_result = value?.toDoubleOrNull()?.let {
                if (isMale) getResultFromRange(it, ParameterRanges.BODY_FAT_MALE)
                else getResultFromRange(it, ParameterRanges.BODY_FAT_FEMALE)
            }
        }
    var body_fat_result: String? = null

    // Lean body weight (kg)
    var lean_body_weight: String? = null
        set(value) {
            field = value
            lean_body_weight_result = ""
        }
    var lean_body_weight_result: String? = null


    var subcutaneous_fat: String? = null
        set(value) {
            field = value
            subcutaneous_fat_result = value?.toDoubleOrNull()?.let {
                if (isMale) getResultFromRange(it, ParameterRanges.SUBCUTANEOUS_FAT_MALE)
                else getResultFromRange(it, ParameterRanges.SUBCUTANEOUS_FAT_FEMALE)
            }
        }
    var subcutaneous_fat_result: String? = null

    var visceral_fat: String? = null
        set(value) {
            field = value
            visceral_fat_result = value?.toDoubleOrNull()?.let {
                if (isMale) getResultFromRange(it, ParameterRanges.VISCERAL_FAT_MALE)
                else getResultFromRange(it, ParameterRanges.VISCERAL_FAT_FEMALE)
            }
        }
    var visceral_fat_result: String? = null

    var body_water: String? = null
        set(value) {
            field = value
            body_water_result = value?.toDoubleOrNull()?.let {
                if (isMale) getResultFromRange(it, ParameterRanges.BODY_WATER_MALE)
                else getResultFromRange(it, ParameterRanges.BODY_WATER_FEMALE)
            }
        }
    var body_water_result: String? = null

    // Metabolic age (years)
    var meta_age: String? = null
        set(value) {
            field = value
            meta_age_result = value?.toIntOrNull()?.let { getMetaAgeResult(it) }
        }
    var meta_age_result: String? = null

    var protein: String? = null
        set(value) {
            field = value
            protein_result = value?.toDoubleOrNull()?.let { getResultFromRange(it, ParameterRanges.PROTEIN) }
        }
    var protein_result: String? = null

    var fat_level: String? = null
        set(value) {
            field = value
            fat_level_result = ""
        }
    var fat_level_result: String? = null

    var control_weight_result: String? = null

    var control_weight: String? = null
        set(value) {
            field = value
            control_weight_result = getControlWeightResult(value, weight)
        }

    var muscle_mass: String? = null
        set(value) {
            field = value
            muscle_mass_result = value?.toDoubleOrNull()?.let {
                if (isMale) getResultFromRange(it, ParameterRanges.MUSCLE_MASS_MALE)
                else getResultFromRange(it, ParameterRanges.MUSCLE_MASS_FEMALE)
            }
        }
    var muscle_mass_result: String? = null

    var muscle_rate: String? = null
        set(value) {
            field = value
            muscle_rate_result = value?.toDoubleOrNull()?.let { getResultFromRange(it, ParameterRanges.MUSCLE_RATE) }
        }
    var muscle_rate_result: String? = null

    var bone_mass: String? = null
        set(value) {
            field = value
            bone_mass_result = value?.toDoubleOrNull()?.let {
                if (isMale) getResultFromRange(it, ParameterRanges.BONE_MASS_MALE)
                else getResultFromRange(it, ParameterRanges.BONE_MASS_FEMALE)
            }
        }
    var bone_mass_result: String? = null

    var temperature: String? = null
        set(value) {
            field = value
            temperature_result = value?.toDoubleOrNull()?.let { getResultFromRange(it, ParameterRanges.TEMPERATURE) }
        }
    var temperature_result: String? = null

    // Blood pressure - diastolic and systolic (mmHg)
    var blood_pressure_diastolic: String? = null
        set(value) {
            field = value
            blood_pressure_diastolic_result = getDiastolicResult(value)
            blood_pressure_overall_result = getBPOverallResult(blood_pressure_systolic, value)
        }
    var blood_pressure_diastolic_result: String? = null

    var blood_pressure_systolic: String? = null
        set(value) {
            field = value
            blood_pressure_systolic_result = getSystolicResult(value)
            blood_pressure_overall_result = getBPOverallResult(value, blood_pressure_diastolic)
        }
    var blood_pressure_systolic_result: String? = null

    // Vision (units assumed same as existing; if using decimal Snellen convert accordingly)
    var eye_left_vision: String? = null
        set(value) {
            field = value
            eye_left_result = getVisionResult(value)
        }
    var eye_left_result: String? = null

    var eye_right_vision: String? = null
        set(value) {
            field = value
            eye_right_result = getVisionResult(value)
        }
    var eye_right_result: String? = null

    // Combined BP assessment (Normal / Elevated / Stage 1 / Stage 2 / Hypertensive Crisis)
    var blood_pressure_overall_result: String? = null

    var pulse: String? = null
        set(value) {
            field = value
            pulse_result = value?.toDoubleOrNull()?.let { getResultFromRange(it, ParameterRanges.PULSE) }
        }
    var pulse_result: String? = null

    var oxygen: String? = null
        set(value) {
            field = value
            oxygen_result = value?.toDoubleOrNull()?.let { getResultFromRange(it, ParameterRanges.OXYGEN) }
        }
    var oxygen_result: String? = null

    var sugar: String? = null
        set(value) {
            field = value
            sugar_result = value?.toDoubleOrNull()?.let { getResultFromRange(it, ParameterRanges.SUGAR) }
        }
    var sugar_result: String? = null

    var hemoglobin: String? = null
        set(value) {
            field = value
            hemoglobin_result = value?.toDoubleOrNull()?.let {
                if (isMale) getResultFromRange(it, ParameterRanges.HEMOGLOBIN_MALE)
                else getResultFromRange(it, ParameterRanges.HEMOGLOBIN_FEMALE)
            }
        }
    var hemoglobin_result: String? = null

    // HbA1c — always stored normalised to NGSP %; the parser converts mmol/mol for us
    var hba1c: String? = null
        set(value) {
            field = value
            hba1c_result = value?.toDoubleOrNull()?.let { getResultFromRange(it, ParameterRanges.HBA1C) }
        }
    var hba1c_result: String? = null

    /** Classifies an NGSP % value without mutating stored state — for live UI display. */
    fun classifyHba1c(ngspPercent: Double?): String? =
        ngspPercent?.let { getResultFromRange(it, ParameterRanges.HBA1C) }

    // ----------------- Special Case: Weight -----------------
    private fun getWeightResult(value: String?, height: Double?): String? {
        val w = value?.toDoubleOrNull() ?: return null
        val h = height ?: return null
        val std = if (isMale) ((h - 80) * 0.7) else (((h * 1.37) - 110) * 0.45)
        val ratio = w / std
        return getResultFromRange(ratio, ParameterRanges.WEIGHT)
    }

    private fun getBmrResult(
        actualBmrStr: String?, ageStr: String?, weightStr: String?, isMale: Boolean
    ): String? {
        val actualBmr = actualBmrStr?.toDoubleOrNull() ?: return null
        val a = ageStr?.toIntOrNull() ?: return null
        val w = weightStr?.toDoubleOrNull() ?: return null

        val multiplier = getBmrMultiplier(a, isMale) ?: return null
        val baseline = w * multiplier

        // Classification per your rule:
        // Normal  : BMR ≥ baseline
        // Not up to Normal : BMR < baseline
        return if (actualBmr >= baseline) "Normal metabolism" else "Below normal metabolism"
    }

    private fun getBmrMultiplier(age: Int, isMale: Boolean): Double? {
        val table = if (isMale) ParameterRanges.BMR_MALE else ParameterRanges.BMR_FEMALE
        // exact bucket
        table.firstOrNull { age in it.first }?.second?.let { return it }
        // if out of bounds, fall back to nearest bracket
        val minRange = table.minByOrNull { it.first.first }!!
        val maxRange = table.maxByOrNull { it.first.last }!!
        return when {
            age < minRange.first.first -> minRange.second
            age > maxRange.first.last -> maxRange.second
            else -> null // shouldn't happen
        }
    }

    private fun getControlWeightResult(control: String?, actual: String?): String? {
        val controlVal = control?.toDoubleOrNull() ?: return "Not Available"
        val actualVal = actual?.toDoubleOrNull() ?: return "Not Available"

        val lower = controlVal * 0.9
        val upper = controlVal * 1.1

        return when {
            actualVal < lower -> "Below Control Weight"
            actualVal > upper -> "Above Control Weight"
            else -> "Normal / At Control Weight"
        }
    }

    private fun getMetaAgeResult(metaAge: Int): String? {
        var metaAgeResult: String? = null

        val actualAge = age?.toIntOrNull() ?: return null

        if (metaAge <= actualAge) metaAgeResult = "Standard"
        else metaAgeResult = "Not up to Standard"

        return metaAgeResult
    }

    private fun getDiastolicResult(value: String?): String? {
        val v = value?.toIntOrNull() ?: return null
        // Simple categorization; overall result uses systolic+diastolic per AHA/ACC. :contentReference[oaicite:3]{index=3}
        return when {
            v < 60 -> "Low"
            v in 60..79 -> "Normal"
            v in 80..89 -> "High (elevated diastolic)"
            v >= 90 -> "Hypertensive range"
            else -> null
        }
    }

    private fun getSystolicResult(value: String?): String? {
        val v = value?.toIntOrNull() ?: return null
        return when {
            v < 90 -> "Low"
            v in 90..119 -> "Normal"
            v in 120..129 -> "Elevated"
            v in 130..139 -> "Hypertension Stage 1"
            v >= 140 -> "Hypertension Stage 2"
            else -> null
        }
    }

    private fun getBPOverallResult(systolicRaw: String?, diastolicRaw: String?): String? {
        val s = systolicRaw?.toIntOrNull()
        val d = diastolicRaw?.toIntOrNull()
        if (s == null && d == null) return null

        // Follow AHA/ACC: highest category of either reading determines classification. :contentReference[oaicite:4]{index=4}
        val sCategory = when {
            s == null -> 0
            s < 120 -> 1 // normal
            s in 120..129 -> 2 // elevated
            s in 130..139 -> 3 // stage1
            s >= 140 -> 4 // stage2
            else -> 0
        }
        val dCategory = when {
            d == null -> 0
            d < 80 -> 1
            d in 80..89 -> 3
            d >= 90 -> 4
            else -> 0
        }
        val category = maxOf(sCategory, dCategory)
        return when (category) {
            1 -> "Normal"
            2 -> "Elevated"
            3 -> "Hypertension Stage 1"
            4 -> "Hypertension Stage 2"
            else -> null
        }
    }


    private fun getVisionResult(value: String?): String? {

        var visionResult: String? = null
        if (value == "6/6") {
            visionResult = "Standard"
        } else {
            visionResult = "Not upto Standard"
        }

        return visionResult
    }


    // 👉 Convert all parameters into a list
    fun toParameterList(): List<Parameters> {

        val list = mutableListOf<Parameters>()

        fun addParam(name: String, value: String?, result: String?, unit: String?, range: String?) {
            if (!value.isNullOrEmpty()) {
                val displayValue = if (!unit.isNullOrEmpty()) "$value $unit" else value
                list.add(Parameters(parameterName = name, value = displayValue, result = result, range = range))
            }
        }

        return listOf(
            Parameters("Height", "", height, "cm"),
            Parameters("Weight", weight_result, weight, "kg"),
            Parameters("BMI", bmi_result, bmi, "18.5 - 24.9"),
            Parameters("BMR", bmr_result, bmr, "Normal based on age/sex"),
            Parameters("Bone Mass", bone_mass_result, bone_mass, "2.5 - 4.0 kg"),
            Parameters("Body Fat", body_fat_result, body_fat, if (gender == "M") "10-20%" else "18-30%"),
            Parameters("Lean Body Weight", lean_body_weight_result, lean_body_weight, "Healthy Range"),
            Parameters("Muscle Mass", muscle_mass_result, muscle_mass, "Varies by sex/age"),
            Parameters("Muscle Rate", muscle_rate_result, muscle_rate, "Normal: 33-39%"),
            Parameters("Subcutaneous Fat", subcutaneous_fat_result, subcutaneous_fat, "10-20%"),
            Parameters("Visceral Fat", visceral_fat_result, visceral_fat, "1-12"),
            Parameters("Body Water", body_water_result, body_water, "50-65%"),
            Parameters("Metabolic Age", meta_age_result, meta_age, "Should ≈ Actual Age"),
            Parameters("Protein", protein_result, protein, "16-20%"),
            Parameters("Fat Level", null, fat_level, "Normal <20%"),
            Parameters("Control Weight", null, control_weight, "-"),
            Parameters("Temperature", temperature_result, temperature, "97°F - 99°F"),
            Parameters("Blood Pressure (Diastolic)", blood_pressure_diastolic_result, blood_pressure_diastolic, "60-80 mmHg"),
            Parameters("Blood Pressure (Systolic)", blood_pressure_systolic_result, blood_pressure_systolic, "90-120 mmHg"),
            Parameters("Left Eye Vision", eye_left_result, eye_left_vision, "6/6"),
            Parameters("Right Eye Vision", eye_right_result, eye_right_vision, "6/6"),
            Parameters("Oxygen Saturation", oxygen_result, oxygen, "95-100%"),
            Parameters("Pulse", pulse_result, pulse, "60-100 bpm"),
            Parameters("Sugar", sugar_result, sugar, "70-110 mg/dL (fasting)"),
            Parameters("Hemoglobin", hemoglobin_result, hemoglobin, if (gender == "M") "13.5-17.5 g/dL" else "12-16 g/dL")
            // NOTE: HbA1c is deliberately NOT listed here. This list feeds the *basic
            // health checkup* results screen and printout, and clearAll() only runs at
            // the end of that flow (ResultsFragment "Home" button). The HbA1c test is a
            // separate standalone flow, so including it would let a value captured in an
            // earlier session — possibly for a different patient — appear in the next
            // patient's basic report. HbA1c belongs to the HBA1C report type instead.
        ).filter { it.value != null } // optional: hide null values
    }

    /**
     * Parameter row(s) for the standalone HbA1c results screen. Deliberately separate
     * from [toParameterList] — see the NOTE above explaining why HbA1c is excluded
     * from that one (avoids a value from an earlier, possibly different patient's
     * HbA1c session leaking into the next patient's basic-checkup report).
     */
    fun toHba1cParameterList(): List<Parameters> = listOfNotNull(
        hba1c?.let { Parameters("HbA1c", hba1c_result, it, ParameterRanges.HBA1C["Normal"]) }
    )

    /**
     * Clears the fields identifying the *currently active patient* — patient_ID,
     * clinic_ID, age (set once at login/registration, not by any test) — plus (via the
     * caller) [PatientPref.patient], the other half of "who's active" state. Call this
     * when actually switching away from the current patient (e.g. Dashboard's back
     * button returning to the login screen), not between tests for the same patient —
     * that's what [clearAll] is for.
     */
    fun clearPatientIdentity() {
        patient_ID = null
        clinic_ID = null
        age = null
    }

    /**
     * Clears the *test values* from a completed checkup, ready for the next test.
     * Deliberately does NOT clear patient_ID / clinic_ID / age — those identify the
     * currently active patient (set once at login/registration), not a test result,
     * and the patient stays active across multiple tests in the same session (e.g.
     * Basic checkup, then HbA1c, without re-logging in). Clearing them here used to
     * mean any test performed after a Results "Home" tap silently submitted a blank
     * patient_id/clinic_id — see the HbA1c integration session, 2026-09-14.
     * Use [clearPatientIdentity] instead when actually switching patients.
     */
    fun clearAll() {
        gender = null
        meta_age = null
        meta_age_result = null

        height = null
        height_result = null
        weight = null
        weight_result = null
        bmi = null
        bmi_result = null
        bmr = null
        bmr_result = null
        body_fat = null
        body_fat_result = null
        lean_body_weight = null
        lean_body_weight_result = null
        subcutaneous_fat = null
        subcutaneous_fat_result = null
        visceral_fat = null
        visceral_fat_result = null
        body_water = null
        body_water_result = null
        protein = null
        protein_result = null
        fat_level = null
        fat_level_result = null
        control_weight = null
        control_weight_result = null
        muscle_mass = null
        muscle_mass_result = null
        muscle_rate = null
        muscle_rate_result = null
        bone_mass = null
        bone_mass_result = null
        temperature = null
        temperature_result = null
        blood_pressure_diastolic = null
        blood_pressure_diastolic_result = null
        blood_pressure_systolic = null
        blood_pressure_systolic_result = null
        blood_pressure_overall_result = null
        eye_left_vision = null
        eye_left_result = null
        eye_right_vision = null
        eye_right_result = null
        pulse = null
        pulse_result = null
        oxygen = null
        oxygen_result = null
        sugar = null
        sugar_result = null
        hemoglobin = null
        hemoglobin_result = null
        hba1c = null
        hba1c_result = null
    }

}

