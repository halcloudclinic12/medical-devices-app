/*
 package com.test.healthbox_app.presentation.tests.bodyAnalysis

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import cn.net.aicare.algorithmutil.AlgorithmUtil
import cn.net.aicare.modulelibrary.module.utils.AicareBleConfig
import com.test.healthbox_app.BroadcastDataParsing
import com.test.healthbox_app.R
import com.pingwang.bluetoothlib.AILinkBleManager
import com.pingwang.bluetoothlib.AILinkBleManager.onInitListener
import com.pingwang.bluetoothlib.AILinkSDK
import com.pingwang.bluetoothlib.bean.BleValueBean
import com.pingwang.bluetoothlib.listener.OnBleBroadcastDataListener
import com.pingwang.bluetoothlib.utils.BleStrUtils

 class MainActivityWeight : ComponentActivity(), BroadcastDataParsing.OnBroadcastDataParsing {

    private val REFRESH_DATA = 3
    private var mBroadcastDataParsing: BroadcastDataParsing? = null
    private var mList: MutableList<String>? = null
    private var listAdapter: ArrayAdapter<*>? = null

    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                REFRESH_DATA -> if (listAdapter != null) {
                    listAdapter!!.notifyDataSetChanged()
                }
            }
        }
    }


    fun scan(view: View?) {
        initPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContent {
//            MyApplicationTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    Greeting(
//                        name = "Android",
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }
//            }
//        }
        setContentView(R.layout.activity_main_weight)
        AILinkSDK.getInstance().init(this)

        AILinkBleManager.getInstance().init(this, object : onInitListener {
            override fun onInitSuccess() {

                //Initialization successful,
                initBleOk()
            }

            override fun onInitFailure() {
            }
        })

        mBroadcastDataParsing = BroadcastDataParsing(this)

        mList = ArrayList()
        val listView = findViewById<ListView>(R.id.listview)

        listAdapter = ArrayAdapter(
            this, R.layout.simple_list_item_1, mList as ArrayList<String>
        )
        listView.adapter = listAdapter
    }

    private fun initBleOk() {
        AILinkBleManager.getInstance().addOnBleBroadcastDataListener(mOnBleBroadcastDataListener)
    }


    private val LOCATION_PERMISSION = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private fun initPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            onPermissionsOk()
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, LOCATION_PERMISSION[0]) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, LOCATION_PERMISSION, 101)
            } else {
                onPermissionsOk()
            }
        } else {
            val allGranted = BLUETOOTH_PERMISSION.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
            if (!allGranted) {
                ActivityCompat.requestPermissions(this, BLUETOOTH_PERMISSION, 101)
            } else {
                onPermissionsOk()
            }
        }
    }


    */
/* private fun initPermissions() {
         if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
             onPermissionsOk()
             return
         }

         if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
             if (ContextCompat.checkSelfPermission(this, LOCATION_PERMISSION[0]) != PackageManager.PERMISSION_GRANTED) {
                 ActivityCompat.requestPermissions(this, LOCATION_PERMISSION, 101)
             } else {
                 onPermissionsOk()
             }
         } else {
             if (ContextCompat.checkSelfPermission(this, BLUETOOTH_PERMISSION[0]) != PackageManager.PERMISSION_GRANTED) {
                 ActivityCompat.requestPermissions(this, BLUETOOTH_PERMISSION, 101)
             } else {
                 onPermissionsOk()
             }
         }
     }*//*


    private fun onPermissionsOk() {
        mList!!.add("start scanning.")

//        mHandler.sendEmptyMessage(REFRESH_DATA)

//        CoroutineScope(Dispatchers.Main).launch {

        AILinkBleManager.getInstance().stopScan()

//            delay(200)

//            AILinkBleManager.getInstance().startScan(1000, emptyList())
        AILinkBleManager.getInstance().startScan(0)
//        }
    }

    private val mOnBleBroadcastDataListener: OnBleBroadcastDataListener = object : OnBleBroadcastDataListener {
        override fun onBleBroadcastData(bleValueBean: BleValueBean?, payload: ByteArray?) {
            Log.e("onBleScannedBrod", "  : : ")
            val manufacturerData = bleValueBean?.manufacturerData
            if (manufacturerData != null && manufacturerData.size >= 15) {
                Log.e("onBleScannedBrod", "  : manFData ${manufacturerData.size}   :: $manufacturerData")
                val product = ((manufacturerData[6].toInt() and 0xff) shl 8) or (manufacturerData[7].toInt() and 0xff)

                Log.e("onBleScannedBrod", "  : manFData Prod $product")

                if (product == WEIGHT_BODY_FAT_SCALE_BROAD_CAST_LE_ONE) {

                    Log.e("onBleScannedBrod", "  : prod WeightBody : $product ")

                    val hex = BleStrUtils.byte2HexStr(manufacturerData)
                    bleValueBean.mac?.let { onBroadCastData(it, hex, manufacturerData) }
                }
            }
        }
    }

    fun onBroadCastData(mac: String, dataHexStr: String, data: ByteArray?) {
        Log.e("MainActivity", "mac:  $mac data:  $dataHexStr")
        if (mBroadcastDataParsing != null) {
            mBroadcastDataParsing!!.dataParsing(data)
        }
    }

    private var mOldNumberId = -1

    */
/**
     * 获取重量数据
     * 体重数据(Stabilize weight)
     *
     * @param weightUnit     weight unit
     * @param weightDecimal  weight decimal point
     * @param weightStatus   0: real-time weight, 1: stable weight
     * @param weightNegative 0: positive weight; 1: negative weight
     * @param weight         raw data (Raw data)
     * @param adc            impedance 65535 indicates failure to measure impedance
     * @param algorithmId    algorithm id
     * @param deviceType     0x00 : weighing scale
     * 0x01: body fat scale
     * @param dataId         dataId
     *//*

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
        var adc = adc
        if (mOldNumberId == dataId) {
            //id相同,不处理
            return
        }
        mOldNumberId = dataId
        Log.e("MainActivity", "weight data:$weight")
        var showData = ""
        when (weightStatus) {
            0x00 -> showData += "Real-time data"
            0x01 -> showData += "Stable data"
            else -> {}
        }

        var unitStr = ""
        when (weightUnit) {
            0 -> unitStr = "kg"
            1 -> unitStr = "斤"
            6 -> unitStr = "lb"
            4 -> unitStr = "st:lb"
            else -> {}
        }

        while (adc > 1000) {
            adc = adc / 10
        }

        showData += "\nweight:$weight"
        showData += "\nweightDecimal:$weightDecimal"
        showData += "\nunit type:$weightUnit->$unitStr"
        showData += "\nImpedance:$adc"
        showData += "\nAlgorithm ID:$algorithmId"
        mList!!.add(showData)
        if (weightStatus == 0x01) {
            initBodyFatDataCalculation(1, 25, 170, weight, adc)
            return
        }
        mHandler.sendEmptyMessage(REFRESH_DATA)
    }

    */
/**
     * initBodyFatDataCalculation
     *
     * @param sex    sex Female=2; Male=1;
     * @param age    age (0~120)
     * @param height height (0-269)
     * @param weight weight (0~220)
     * @param adc    adc (0~1000)
     *//*

    private fun initBodyFatDataCalculation(sex: Int, age: Int, height: Int, weight: Int, adc: Int) {
        val bodyFatData = AicareBleConfig.getBodyFatData(
            AlgorithmUtil.AlgorithmType.TYPE_AICARE, sex, age, weight.toDouble() / 100, height, adc
        )
        val moreFatData = AicareBleConfig.getMoreFatData(
            sex, height, weight.toDouble() / 100, bodyFatData.bfr, bodyFatData.rom, bodyFatData.pp
        )
        //http://doc.elinkthings.com/web/#/12?page_id=50  -> Part of the class description -> cn.net.aicare.algorithmutil.BodyFatData and MoreFatData doc
        mList!!.add("bodyFatData1:bmi=" + bodyFatData.bmi + "  bfr=" + bodyFatData.bfr + "  rom=" + bodyFatData.rom + "  pp=" + bodyFatData.pp + "  vwc=" + bodyFatData.vwc + "  bm=" + bodyFatData.bm)
        mList!!.add("bodyFatData2:$moreFatData")
        mHandler.sendEmptyMessage(REFRESH_DATA)
    }


    companion object {
        private val TAG: String = MainActivityWeight::class.java.getName()
        const val WEIGHT_BODY_FAT_SCALE_BROAD_CAST_LE_ONE: Int = 0x02
        private val BLUETOOTH_PERMISSION = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!", modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {

}*/
