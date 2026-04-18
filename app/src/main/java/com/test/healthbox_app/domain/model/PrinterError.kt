package com.test.healthbox_app.domain.model

import java.lang.Exception

sealed class PrinterError(message: String) : Exception(message) {
    object DeviceNotConnected : PrinterError("Device not connected")
    object PlatenOpen : PrinterError("Platen open")
    object PaperOut : PrinterError("Paper out")
    object ImproperVoltage : PrinterError("Printer at improper voltage")
    object PrintFailure : PrinterError("Print failed")
    object ParameterError : PrinterError("Parameter error")
    object NoResponse : PrinterError("No response from Pride device")
    object DemoVersion : PrinterError("Library in demo version")
    object InvalidDeviceId : PrinterError("Connected device is not authenticated")
    object NotActivated : PrinterError("Library not activated")
    object NotSupported : PrinterError("Not Supported")
    data class UnknownError(val code: Int) : PrinterError("Unknown Response from Device: $code")
}