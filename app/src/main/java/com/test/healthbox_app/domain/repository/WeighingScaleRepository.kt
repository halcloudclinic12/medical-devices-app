package com.test.healthbox_app.domain.repository

import com.test.healthbox_app.domain.model.WeighingScaleResult

interface WeighingScaleRepository {

    //    suspend fun initSDK()
    suspend fun initializeScale(): WeighingScaleResult<Unit>

    suspend fun startScanning(): WeighingScaleResult<Unit>

    /*fun startScan()
    fun stopScan()
    fun setOnDataReceivedListener(listener: (mac: String, rawData: ByteArray) -> Unit)*/
}