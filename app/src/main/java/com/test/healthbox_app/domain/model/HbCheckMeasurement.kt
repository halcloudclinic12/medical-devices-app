package com.test.healthbox_app.domain.model

import com.test.healthbox_app.bluetooth.DeviceType

data class HbCheckMeasurement(
    override var deviceType: DeviceType = DeviceType.HB_CHECK,
    override var timestamp: Long = System.currentTimeMillis(),
    override var isValid: Boolean = false,
    val result: HbCheckMeasurement? = null,
    var value: String? = null,
    var status: String? = null,
    var isConnected: Boolean = false

    /*object TestStarted : HbResult()
    object InsertStrip : HbResult()
    object WaitForResult : HbResult()
    object Error1 : HbResult()
    object Error2 : HbResult()
    data class Value(val value: String) : HbResult()
    data class Error(val message: String) : HbResult()
    data class ConnectionStatus(val status: String, val isConnected: Boolean) : HbResult()*/
) : Measurement()