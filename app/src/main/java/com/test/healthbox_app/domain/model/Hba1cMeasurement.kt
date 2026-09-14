package com.test.healthbox_app.domain.model

import com.test.healthbox_app.bluetooth.DeviceType

/**
 * Unit reported by the A1cEZ 2.0 meter in `ucUnitFlag` (byte 0 of the record).
 *
 * The same meter reports both HbA1c and plain glucose, discriminated by this flag,
 * so a "glucose" value arriving on the HbA1c screen is expected and must be ignored
 * rather than treated as an error.
 */
enum class A1cUnit(val label: String) {
    NGSP_PERCENT("%"),
    IFCC_MMOL_MOL("mmol/mol"),
    GLUCOSE_MMOL_L("mmol/L"),
    GLUCOSE_MG_DL("mg/dL");

    val isGlucose: Boolean
        get() = this == GLUCOSE_MMOL_L || this == GLUCOSE_MG_DL
}

/**
 * One decoded record from the A1cEZ 2.0 HbA1c meter.
 *
 * @param value             raw `dRecordValue` in whatever [unit] the meter was set to
 * @param ngspPercent       [value] normalised to NGSP % for display and classification;
 *                          null when the record is a glucose reading
 * @param uniqueRecordNum   `ulUniqRecordNum` — global index, used to dedupe replayed history
 * @param recordNumOfDay    `ucRecordNum` — sequence within the day
 * @param deviceTimeMillis  timestamp reported by the meter itself; 0 when unparseable
 * @param isGlucoseRecord   true when `ucUnitFlag` was 2 or 3
 */
data class Hba1cMeasurement(
    override val deviceType: DeviceType = DeviceType.HBA1C_METER,
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean = false,

    val value: Double = 0.0,
    val unit: A1cUnit = A1cUnit.NGSP_PERCENT,
    val ngspPercent: Double? = null,

    val meterId: String = "",
    val uniqueRecordNum: Long = 0L,
    val recordNumOfDay: Int = 0,
    val temperatureC: Double = 0.0,
    val deviceTimeMillis: Long = 0L,
    val isGlucoseRecord: Boolean = false
) : Measurement() {

    /** Estimated average glucose in mg/dL (ADAG equation). Null for glucose records. */
    val eagMgDl: Double?
        get() = ngspPercent?.let { 28.7 * it - 46.7 }

    /** IFCC equivalent in mmol/mol. Null for glucose records. */
    val ifccMmolMol: Double?
        get() = ngspPercent?.let { (it - 2.15) * 10.929 }
}
