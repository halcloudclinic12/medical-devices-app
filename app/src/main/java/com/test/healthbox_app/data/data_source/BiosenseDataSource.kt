package com.test.healthbox_app.data.data_source

import android.app.Activity
import android.content.Context
import android.util.Log
import biosense.sreyasvpariyath.com.biosenselib.helper.Communicator
import biosense.sreyasvpariyath.com.biosenselib.helper.Constants
import biosense.sreyasvpariyath.com.biosenselib.helper.ControlCentre
import com.test.healthbox_app.domain.model.HbCheckMeasurement
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class BiosenseDataSource @Inject constructor(
    private val context: Context
) : Communicator {

    private var controlCentre: ControlCentre? = null
    private var resultCallback: ((HbCheckMeasurement) -> Unit)? = null
    private val _connectionState = MutableStateFlow<Boolean>(false)
    val connectionState = _connectionState.asStateFlow()


    fun connectToDevice(
        deviceAddress: String,
        deviceName: String,
        activity: Activity
    ): Flow<HbCheckMeasurement> = callbackFlow {
        try {
            controlCentre = ControlCentre(
                this@BiosenseDataSource, activity, activity, deviceAddress, Constants.devId_HB, deviceName
            )

            controlCentre?.startReceiver()

            val hbCheckMeasurement = HbCheckMeasurement()
            hbCheckMeasurement.status = "Connecting..."
            hbCheckMeasurement.isConnected = false

            send(hbCheckMeasurement)

            // Send initial connecting status
//            send(HbResult.status("Connecting..."))

            awaitClose {
                disconnectDevice()
            }
        } catch (e: Exception) {

            val hbCheckMeasurement = HbCheckMeasurement()
            hbCheckMeasurement.status = "Connection error:"
            hbCheckMeasurement.isConnected = false
            send(hbCheckMeasurement)

//            send(HbResult.Error("Connection error: ${e.message}"))
            close()
        }
    }

    /*fun connectToDevice(deviceAddress: String, deviceName: String, mActivity: Activity) {
        controlCentre = ControlCentre(
            this, context, mActivity, deviceAddress, Constants.devId_HB, deviceName
        )
        controlCentre?.startReceiver()
    }*/

    fun getResultFlow(): Flow<HbCheckMeasurement> = callbackFlow {
        resultCallback = { result ->
            trySend(result)
        }

        awaitClose {
            resultCallback = null
        }
    }

    fun startHbTest() {
        controlCentre?.startHbTest()
    }

    fun stopHbTest() {
        controlCentre?.stopHbTest()
    }

    fun disconnectDevice() {
        controlCentre?.stopReceiver()
        controlCentre = null
    }

    override fun go(p0: String?): Boolean {
        return false
    }

    override fun setGlucoseReading(p0: String?) {
    }

    override fun testStarted(p0: Boolean) {
    }

    override fun stopNotiFication() {
    }

    // Communicator interface implementations
    override fun setConnectionStatus(status: String, connectionStatus: Boolean) {

        _connectionState.value = connectionStatus
        println("setConnStatus Biosence Data Source :: Status :: $status   :: Conn Status ::  $connectionStatus")

        val hbCheckMeasurement = HbCheckMeasurement()
        hbCheckMeasurement.status = status
        hbCheckMeasurement.isConnected = connectionStatus

//        resultCallback?.invoke(HbResult.ConnectionStatus(status, connectionStatus))

        resultCallback?.invoke(hbCheckMeasurement)
    }

    override fun setSwitchActivity() {
    }

    override fun setBatteryLevel(p0: Int) {
    }

    override fun setManufacturerName(p0: String?) {
    }

    override fun setSerialNumber(p0: String?) {

    }

    override fun setModelNumber(p0: String?) {

    }

    override fun getOfflineResults(p0: ArrayList<String>?) {
    }

    override fun setHbA1cReading(p0: String?, p1: String?, p2: String?, p3: String?) {
    }

    override fun setBPReading(p0: String?, p1: String?, p2: String?) {
    }

    override fun setHB(hb: String) {

        Log.e("setHBCasesRes", "  : hb : $hb  ")

        val hbResult = HbCheckMeasurement()

        hbResult.isConnected = true
        hbResult.value = hb

        /*when (hb) {
            "2222" -> {
                hbResult.status = "TestStarted"
                hbResult.isConnected = true
            }

            "2221" -> {
                hbResult.status = "InsertStrip"
                hbResult.isConnected = true
            }

            "2227", "78" -> {
                hbResult.status = "Error2"
                hbResult.isConnected = true
            }

            "2225" -> {
                hbResult.status = "Error1"
                hbResult.isConnected = true
            }

            "2224" -> {
                hbResult.status = "WaitForResult"
                hbResult.isConnected = true
            }

            else -> {
                Log.e("hvCasesRes", "  :  $hb")

                hbResult.value = hb
//                hbResult.isConnected = true
            }
        }*/

        Log.e("hvCasesRes", "  : hb : $hb  : final : ${hbResult}")

//        resultCallback?.invoke(result)
        resultCallback?.invoke(hbResult)
    }

    override fun setLipid(p0: String?, p1: String?, p2: String?) {
        TODO("Not yet implemented")
    }

    override fun setTempreture(p0: String?) {
        TODO("Not yet implemented")
    }
}