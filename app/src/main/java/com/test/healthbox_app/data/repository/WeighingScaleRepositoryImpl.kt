package com.test.healthbox_app.data.repository

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.test.healthbox_app.BroadcastDataParsing
import com.test.healthbox_app.domain.model.BleDevice

import com.test.healthbox_app.domain.model.WeighingScaleResult
import com.test.healthbox_app.domain.model.WeightData
import com.test.healthbox_app.domain.repository.WeighingScaleRepository
import com.pingwang.bluetoothlib.AILinkBleManager
import com.pingwang.bluetoothlib.AILinkBleManager.onInitListener
import com.pingwang.bluetoothlib.bean.BleValueBean
import com.pingwang.bluetoothlib.listener.OnBleBroadcastDataListener
import com.pingwang.bluetoothlib.utils.BleStrUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeighingScaleRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : WeighingScaleRepository, BroadcastDataParsing.OnBroadcastDataParsing {

    private val _weightDataFlow = MutableSharedFlow<WeightData>()
    private val _scannedDevicesFlow = MutableSharedFlow<List<BleDevice>>()
    private val scannedDevices = mutableListOf<BleDevice>()
    private var mOldNumberId = -1

    private var mList: MutableList<String>? = null
    private var mBroadcastDataParsing: BroadcastDataParsing? = null

    companion object {
        private const val TAG = "WeighingScaleRepository"
        const val WEIGHT_BODY_FAT_SCALE_BROAD_CAST_LE_ONE = 0x02
    }

    private val mOnBleBroadcastDataListener: OnBleBroadcastDataListener = object : OnBleBroadcastDataListener {

        override fun onBleBroadcastData(bleValueBean: BleValueBean?, payload: ByteArray?) {
            val manufacturerData = bleValueBean?.manufacturerData

            Log.e("onBleScannedBrodIMPL", "  : : ${manufacturerData}")

            if (manufacturerData != null && manufacturerData.size >= 15) {
                Log.e("onBleScannedBrodIMPL", "  : manFData ${manufacturerData.size}   :: $manufacturerData")

                val product = ((manufacturerData[6].toInt() and 0xff) shl 8) or (manufacturerData[7].toInt() and 0xff)

                Log.e("onBleScannedBrodIMPL", "  : manFData Prod $product")

                if (product == WEIGHT_BODY_FAT_SCALE_BROAD_CAST_LE_ONE) {

                    Log.e("onBleScannedBrodIMPL", "  : prod WeightBody : $product ")

                    val hex = BleStrUtils.byte2HexStr(manufacturerData)

                    bleValueBean.mac?.let { onBroadCastData(it, hex, manufacturerData) }
                }
            }
        }
    }

    /*private val mOnBleBroadcastDataListener = object : OnBleBroadcastDataListener {

        override fun onBleBroadcastData(bleValueBean: BleValueBean?, payload: ByteArray?) {

            val manufacturerData = bleValueBean?.manufacturerData

            Log.e(TAG, "WeighingScaleLogs :: macResLogs:  OnBleBroad  ::  $manufacturerData ")

            if (manufacturerData != null && manufacturerData.size >= 15) {

                Log.e(TAG, "WeighingScaleLogs :: macResLogs:  manufacturerData  : -- :  $manufacturerData ")

                val product = ((manufacturerData[6].toInt() and 0xff) shl 8) or (manufacturerData[7].toInt() and 0xff)

                if (product == WEIGHT_BODY_FAT_SCALE_BROAD_CAST_LE_ONE) {
                    Log.e(TAG, "WeighingScaleLogs :: macResLogs:  product  ::  $product ")

                    val hex = BleStrUtils.byte2HexStr(manufacturerData)

                    Log.e(TAG, "WeighingScaleLogs :: macResLogs:  OnBleBroad  ::  $manufacturerData ")

                    bleValueBean.mac?.let {
                        onBroadCastData(it, hex, manufacturerData)

                        // Add to scanned devices
                        val device = BleDevice(
                            address = bleValueBean.address,
                            name = bleValueBean.name,
                            rssi = bleValueBean.rssi,
                            deviceType = DeviceType.WEIGHING_SCALE,
                            bondState = 12,
                            scanRecord = bleValueBean.scanRecord
                        )

                        if (!scannedDevices.any { existingDevice -> existingDevice.address == it }) {
                            Log.e(TAG, "WeighingScaleLogs :: macResLogs:  OnBleBroad  ::  $device ")

                            scannedDevices.add(device)
                            _scannedDevicesFlow.tryEmit(scannedDevices.toList())
                        }
                    }
                }
            }
        }
    }*/

    suspend fun initSDK() {
        Log.e(TAG, "WeighingScaleLogs  : initLogs  :::  Called  ::" + AILinkBleManager.getInstance().isInitOk)

        AILinkBleManager.getInstance().init(context)

        AILinkBleManager.getInstance().init(context, object : onInitListener {
            override fun onInitSuccess() {
                //Initialization successful,
//                initBleOk()
                Log.e(TAG, "WeighingScaleLogs  : initLogs  :::  Success  ::" + AILinkBleManager.getInstance().isInitOk)

                val status = AILinkBleManager.getInstance().addOnBleBroadcastDataListener(mOnBleBroadcastDataListener)

                Log.e(TAG, "WeighingScaleLogs  : initLogs  :::  Success  val $status")

            }

            override fun onInitFailure() {
                Log.e(TAG, "WeighingScaleLogs  : initLogs  :::  Failure ")
            }
        })

        mBroadcastDataParsing = BroadcastDataParsing(this)
    }

    override suspend fun initializeScale(): WeighingScaleResult<Unit> {
        return try {

            Log.e(TAG, "WeighingScaleLogs  : initLogs  :::Called::" + AILinkBleManager.getInstance().isInitOk)

            mBroadcastDataParsing = BroadcastDataParsing(this@WeighingScaleRepositoryImpl)

            suspendCancellableCoroutine { continuation ->

                Log.e(TAG, "WeighingScaleLogs  : initLogs  :::Calling::" + AILinkBleManager.getInstance().isInitOk)

//                AILinkSDK.getInstance().init(context.applicationContext)

                AILinkBleManager.getInstance().init(context.applicationContext, object : onInitListener {
                    override fun onInitSuccess() {

                        Log.e(TAG, "WeighingScaleLogs  : initLogs  :::  Success  ::" + AILinkBleManager.getInstance().isInitOk)

                        AILinkBleManager.getInstance().addOnBleBroadcastDataListener(mOnBleBroadcastDataListener)

//                        AILinkBleManager.getInstance().startScan(10000, emptyList())

                        Handler(Looper.getMainLooper()).postDelayed({
//                            AILinkBleManager.getInstance().addOnBleBroadcastDataListener(mOnBleBroadcastDataListener)

                            AILinkBleManager.getInstance().startScan(10000, emptyList())
                        }, 5000)

                        continuation.resume(WeighingScaleResult.Success(Unit), null)

                    }

                    override fun onInitFailure() {
                        Log.e(TAG, "WeighingScaleLogs  : initLogs  :::  Failed  ::" + AILinkBleManager.getInstance().isInitOk)

                        continuation.resume(WeighingScaleResult.Error(Exception("Failed to initialize BLE Manager")), null)
                    }
                })
            }


        } catch (e: Exception) {
            e.printStackTrace()
            WeighingScaleResult.Error(e)
        }
    }

    override suspend fun startScanning(): WeighingScaleResult<Unit> {

        return try {
            scannedDevices.clear()

            AILinkBleManager.getInstance().stopScan()

            AILinkBleManager.getInstance().startScan(10000, emptyList())

            WeighingScaleResult.Success(Unit)
        } catch (e: Exception) {
            WeighingScaleResult.Error(e)
        }
    }

    suspend fun stopScanning(): WeighingScaleResult<Unit> {
        return try {
            AILinkBleManager.getInstance().stopScan()
            WeighingScaleResult.Success(Unit)
        } catch (e: Exception) {
            WeighingScaleResult.Error(e)
        }
    }

    fun getWeightDataStream(): Flow<WeightData> = _weightDataFlow

    fun getScannedDevicesStream(): Flow<List<BleDevice>> = _scannedDevicesFlow

    /*suspend fun calculateBodyFatData(
        sex: Int, age: Int, height: Int, weightData: WeightData
    ): WeighingScaleResult<Pair<BodyFatData, MoreFatData>> {
        return try {
            var impedance = weightData.impedance
            while (impedance > 1000) {
                impedance /= 10
            }

            val bodyFatData = AicareBleConfig.getBodyFatData(
                AlgorithmUtil.AlgorithmType.TYPE_AICARE, sex, age, weightData.weight.toDouble() / 100, height, impedance
            )

            val moreFatData = AicareBleConfig.getMoreFatData(
                sex, height, weightData.weight.toDouble() / 100, bodyFatData.bfr, bodyFatData.rom, bodyFatData.pp
            )

            *//*val domainBodyFatData = BodyFatData(
                bmi = bodyFatData.bmi, bfr = bodyFatData.bfr, rom = bodyFatData.rom, pp = bodyFatData.pp, vwc = bodyFatData.vwc, bm = bodyFatData.bm
            )*//*

            // Map moreFatData to domain model
            val domainMoreFatData = MoreFatData(
                subcutaneousFat = 0.0,
                visceralFat = 0.0,
                bodyWater = 0.0,
                skeletalMuscle = 0.0,
                muscleMass = moreFatData.muscleMass,
                boneMass = 0.0,
                protein = moreFatData.protein,
                bmr = 0.0,
                bodyAge = 0
            )


            val domainMoreFatData = MoreFatData(
                subcutaneousFat = moreFatData.subcutaneousFat,
                visceralFat = moreFatData.visceralFat,
                bodyWater = moreFatData.bodyWater,
                skeletalMuscle = moreFatData.skeletalMuscle,
                muscleMass = moreFatData.muscleMass,
                boneMass = moreFatData.boneMass,
                protein = moreFatData.protein,
                bmr = moreFatData.bmr,
                bodyAge = moreFatData.bodyAge
            )

            WeighingScaleResult.Success(Pair(domainBodyFatData, domainMoreFatData))
        } catch (e: Exception) {
            WeighingScaleResult.Error(e)
        }
    }*/

    /*override suspend fun checkPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            true
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            LOCATION_PERMISSION.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        } else {
            BLUETOOTH_PERMISSION.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }
    }*/

    suspend fun requestPermissions(): Boolean {
        // This should be handled in the presentation layer (Activity/Fragment)
        // as we need Activity context for permission requests
        return false
    }

    private fun onBroadCastData(mac: String, dataHexStr: String, data: ByteArray?) {
        Log.e(TAG, "macResLogs: $mac data: $dataHexStr    : data : ${data} ")
        mBroadcastDataParsing?.dataParsing(data)
    }

    // Implementation of BroadcastDataParsing.OnBroadcastDataParsing
    override fun getWeightData(
        dataId: Int,
        deviceType: Int,
        weightUnit: Int,
        weightDecimal: Int,
        weightStatus: Int,
        weightNegative: Int,
        weight: Int,
        adc: Int,
        algorithmId: Int
    ) {
        if (mOldNumberId == dataId) {
            return // Same ID, don't process
        }
        mOldNumberId = dataId

        Log.e(TAG, "weight data: $weight")

        /*val weightData = WeightData(
            dataId = dataId,
            deviceType = deviceType,
            weightUnit = weightUnit,
            weightDecimal = weightDecimal,
            weightStatus = weightStatus,
            weightNegative = weightNegative,
            weight = weight,
            impedance = adc,
            algorithmId = algorithmId,
            mac = "" // You might need to store current MAC somewhere
        )*/

//        _weightDataFlow.tryEmit(weightData)
    }


}

