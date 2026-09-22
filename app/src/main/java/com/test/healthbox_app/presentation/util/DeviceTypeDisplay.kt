package com.test.healthbox_app.presentation.util

import com.test.healthbox_app.R
import com.test.healthbox_app.bluetooth.DeviceType

/** Real per-device icons (already used elsewhere in the app, e.g. the Results screen's
 *  parameter rows) - shared here so the Connected Devices row list and its detail pane
 *  don't each duplicate the mapping. No dedicated printer icon exists, so BT_PRINTER
 *  falls back to the generic Bluetooth glyph. */
fun DeviceType.iconRes(): Int = when (this) {
    DeviceType.HEIGHT -> R.drawable.ic_height
    DeviceType.THERMOMETER -> R.drawable.ic_temperature
    DeviceType.PULSE -> R.drawable.ic_pulse
    DeviceType.WEIGHING_SCALE -> R.drawable.ic_body_weight
    DeviceType.BLOOD_PRESSURE_MONITOR -> R.drawable.ic_blood_pressure
    DeviceType.HB_CHECK -> R.drawable.ic_hemoglobin
    DeviceType.GLUCOSE_METER -> R.drawable.ic_glucose_test
    DeviceType.HBA1C_METER -> R.drawable.ic_hba1c
    DeviceType.BT_PRINTER -> R.drawable.ic_bluetooth
}

/** The real device icons above are full-color artwork and render best untinted (see
 *  the Results screen's parameter rows for the same convention). ic_bluetooth is a
 *  plain vector glyph, not colored artwork, and needs an explicit tint or it renders
 *  as a solid black square. */
fun DeviceType.needsIconTint(): Boolean = this == DeviceType.BT_PRINTER
