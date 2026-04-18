package com.test.healthbox_app.presentation.tests.bodyAnalysis

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import cn.net.aicare.algorithmutil.AlgorithmUtil
import cn.net.aicare.modulelibrary.module.utils.AicareBleConfig
import com.google.gson.Gson
import com.test.healthbox_app.BleConnectionViewModel
import com.test.healthbox_app.BroadcastDataParsing
import com.test.healthbox_app.MainActivity
import com.test.healthbox_app.R
import com.test.healthbox_app.base.BaseFragment
import com.test.healthbox_app.bluetooth.DeviceType
import com.test.healthbox_app.data.model.BodyCheckupPref
import com.test.healthbox_app.data.model.PatientPref
import com.test.healthbox_app.databinding.BodyAnalysisFragmentBinding
import com.test.healthbox_app.domain.model.BleDevice
import com.test.healthbox_app.domain.model.HbCheckMeasurement
import com.test.healthbox_app.domain.model.ScanState
import com.test.healthbox_app.domain.model.StepStatus
import com.test.healthbox_app.domain.model.WeightData
import com.test.healthbox_app.domain.model.WeightMeasurement
import com.test.healthbox_app.domain.model.mapper.mapToBodyParameters
import com.test.healthbox_app.presentation.dialog.DeviceListDialog
import com.test.healthbox_app.presentation.util.BasicHealthTestsType
import com.test.healthbox_app.presentation.util.CustomSnackBar
import com.test.healthbox_app.presentation.util.DatePickerUtil
import com.test.healthbox_app.presentation.view.StepsViewModel
import com.test.healthbox_app.presentation.view.deviceStatus.DeviceStatusViewModel
import com.pingwang.bluetoothlib.AILinkBleManager
import com.pingwang.bluetoothlib.AILinkBleManager.onInitListener
import com.pingwang.bluetoothlib.AILinkSDK
import com.pingwang.bluetoothlib.bean.BleValueBean
import com.pingwang.bluetoothlib.listener.OnBleBroadcastDataListener
import com.pingwang.bluetoothlib.utils.BleStrUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BodyAnalysisFragment() : BaseFragment(), BroadcastDataParsing.OnBroadcastDataParsing/*, OnBleBroadcastDataListener */ {

    private lateinit var binding: BodyAnalysisFragmentBinding

    private val stepsViewModel: StepsViewModel by activityViewModels()

    private val deviceStatusViewModel: DeviceStatusViewModel by activityViewModels()

    private lateinit var bleConnectionViewModel: BleConnectionViewModel

    private lateinit var deviceListDialog: DeviceListDialog

    private var weightMeasurementsList: List<WeightMeasurement> = listOf()

    override fun checkConnectivity() {}

    override val isConnected: Unit = Unit

    private val TAG = "BodyAnalysisFragment"

    private val mOnBleBroadcastDataListener: OnBleBroadcastDataListener = object : OnBleBroadcastDataListener {
        override fun onBleBroadcastData(bleValueBean: BleValueBean?, payload: ByteArray?) {
            val manufacturerData = bleValueBean?.manufacturerData

            Log.e("onBleScannedBrodBAF", "  : : ${manufacturerData}")

            if (manufacturerData != null && manufacturerData.size >= 15) {
                Log.e("onBleScannedBrodBAF", "  : manFData ${manufacturerData.size}   :: $manufacturerData")

                val product = ((manufacturerData[6].toInt() and 0xff) shl 8) or (manufacturerData[7].toInt() and 0xff)

                Log.e("onBleScannedBrodBAF", "  : manFData Prod $product")

                if (product == WEIGHT_BODY_FAT_SCALE_BROAD_CAST_LE_ONE) {
                    Log.e("onBleScannedBrodBAF", "  : prod WeightBody : $product ")

                    val hex = BleStrUtils.byte2HexStr(manufacturerData)
                    bleValueBean.mac?.let { onBroadCastData(it, hex, manufacturerData) }
                }
            }
        }
    }

    private var mBroadcastDataParsing: BroadcastDataParsing? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle Back Press
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {


                    CustomSnackBar.makeWithAction(
                        binding.root,
                        "Are you sure to go back, It will clear all the test results?",
                        "Yes",
                        Color.WHITE,
                        action = {
                            findNavController().navigate(R.id.close_button_action_body_analysis_screen)
                        },
                        duration = 5000,
                        type = CustomSnackBar.Companion.SnackBarType.CUSTOM
                    ).show()
                }
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = BodyAnalysisFragmentBinding.inflate(inflater)

        // Get ViewModel from activity
        bleConnectionViewModel = (activity as MainActivity).bleViewModel

        binding.viewModel = bleConnectionViewModel

        binding.lifecycleOwner = viewLifecycleOwner

        // Bind user info view with ViewModel
        binding.userInfoView.loadPatientFromPref()

        initializeGroupASteps()

        initializeDeviceStatus()

        setAndObserveDeviceAvailability()

        setupDialog()

        observeDataState()

        nextTestCall()

        Log.e(TAG, "WeighingScaleLogs  : initLogs  ::11::  Called  ::" + AILinkBleManager.getInstance().isInitOk)

        AILinkSDK.getInstance().init(mActivity)

        AILinkBleManager.getInstance().init(mActivity, object : onInitListener {
            override fun onInitSuccess() {
                Log.e(TAG, "WeighingScaleLogs  : initLogs  :::  Success  ::" + AILinkBleManager.getInstance().isInitOk)
                //Initialization successful,
                initBleOk()
            }

            override fun onInitFailure() {
                Log.e(TAG, "WeighingScaleLogs  : initLogs  :::  Failure ")
            }
        })

        mBroadcastDataParsing = BroadcastDataParsing(this)

        return binding.root
    }

    private fun initBleOk() {
        AILinkBleManager.getInstance().addOnBleBroadcastDataListener(mOnBleBroadcastDataListener)
    }

    fun scan() {
//        initPermissions()
        onPermissionsOk()
    }

    companion object {
        private val TAG: String = BodyAnalysisFragment::class.java.getName()
        const val WEIGHT_BODY_FAT_SCALE_BROAD_CAST_LE_ONE: Int = 0x02
        private val BLUETOOTH_PERMISSION = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT
        )
        private val LOCATION_PERMISSION = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private fun initPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            onPermissionsOk()
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), LOCATION_PERMISSION[0]) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), LOCATION_PERMISSION, 101)
            } else {
                onPermissionsOk()
            }
        } else {
            val allGranted = BLUETOOTH_PERMISSION.all { ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED }
            if (!allGranted) {
                ActivityCompat.requestPermissions(requireActivity(), BLUETOOTH_PERMISSION, 101)
            } else {
                onPermissionsOk()
            }
        }
    }

    private fun stopScan() {
        AILinkBleManager.getInstance().stopScan()
    }

    private fun onPermissionsOk() {
//        mList!!.add("start scanning.")
//        mHandler.sendEmptyMessage(REFRESH_DATA)
//        AILinkBleManager.getInstance().stopScan()

        stopScan()

        AILinkBleManager.getInstance().startScan(1000, emptyList())
//        AILinkBleManager.getInstance().startScan(0)
    }

    private fun startWeighingProcess() {
        // Example user data - in a real app, get this from user input
        val sex = 1 // Male
        val age = 25
        val height = 170

        bleConnectionViewModel.startWeighing(sex, age, height)
    }

    private fun setAndObserveDeviceAvailability() {
        bleConnectionViewModel.setDeviceType(DeviceType.WEIGHING_SCALE)

        bleConnectionViewModel.getDevice()

        if (bleConnectionViewModel.selectedDevice.value == null) {
            binding.deviceStatusLayout.setUpDeviceAvailability(false)
        } else {
            binding.deviceStatusLayout.setUpDeviceAvailability(true)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            bleConnectionViewModel.selectedDevice.collect { device ->
                print("selectedHBDeviceLogsObs : Device ::  $device")

                if (device != null) {
                    binding.deviceStatusLayout.setUpDeviceAvailability(true)

//                    bleConnectionViewModel.connectHBDevice(device, mActivity!!)

//                    bleConnectionViewModel.connectToDevice(device, bleConnectionViewModel.selectedDeviceType.value!!)

                }
            }
        }
    }

    private fun nextTestCall() {
        binding.buttonStartWeight.setOnClickListener { view ->
//            showDialog()

            binding.rvWeightResults.visibility = View.GONE

            weightMeasurementsList = emptyList()

            scan()
        }

        binding.buttonNextLayout.setOnClickListener { view ->
            mActivity?.let {
//                scan()

//                bleConnectionViewModel.startScanning()

                CoroutineScope(Dispatchers.Main).launch {

                    saveWeightData()

                    stopScan()

                    disconnectDevice()

                    stepsViewModel.goToNextStep()

                    it.navController?.navigate(R.id.next_button_vision_action)


                }
            }
        }
    }

    fun disconnectDevice() {

        bleConnectionViewModel.setSelectedDevice(null)

        bleConnectionViewModel.setDeviceType(null)
    }


    private fun saveWeightData() {
        weightMeasurementsList.let {
            weightMeasurementsList.forEach { weightMeasurement ->

                when (weightMeasurement.label) {

                    "Standard Weight" -> {
                        BodyCheckupPref.weight = weightMeasurement.value
                    }

                    "Visceral Fat" -> {
                        BodyCheckupPref.visceral_fat = weightMeasurement.value
                    }

                    "Subcutaneous Fat" -> {
                        BodyCheckupPref.subcutaneous_fat = weightMeasurement.value
                    }

                    "Protein" -> {
                        BodyCheckupPref.protein = weightMeasurement.value
                    }

                    "Muscle Rate" -> {
                        BodyCheckupPref.muscle_rate = weightMeasurement.value
                    }

                    "Muscle Mass" -> {
                        BodyCheckupPref.muscle_mass = weightMeasurement.value
                    }

                    "Metabolic Age" -> {
                        BodyCheckupPref.meta_age = weightMeasurement.value
                    }

                    "Lean Body Weight" -> {
                        BodyCheckupPref.lean_body_weight = weightMeasurement.value
                    }

                    "Bone Mass" -> {
                        BodyCheckupPref.bone_mass = weightMeasurement.value
                    }

                    "Body Water Rate" -> {
                        BodyCheckupPref.body_water = weightMeasurement.value
                    }

                    "BMI" -> {
                        BodyCheckupPref.bmi = weightMeasurement.value
                    }

                    "Body Fat Rate" -> {
                        BodyCheckupPref.body_fat = weightMeasurement.value
                    }

                    "Basal Metabolic Rate" -> {
//                    BodyCheckupPref. = weightMeasurement.value
                    }

                    "Fat Level" -> {
                        BodyCheckupPref.fat_level = weightMeasurement.value
                    }

                    "Control Weight" -> {
                        BodyCheckupPref.control_weight = weightMeasurement.value
                    }
                }
            }
        }
    }

    private fun setupDialog() {
        deviceListDialog = mActivity?.let {
            DeviceListDialog(it, onDeviceClose = {
                deviceListDialog.dismissDialog()
            })
        }!!

        deviceListDialog.setOnItemSelectedListener(object : DeviceListDialog.OnItemClickListener {
            override fun onItemSelect(bleDevice: BleDevice?) {

                print("selectedHBDeviceLogsObs :  $bleDevice")

                showDialog()

                bleDevice?.let {
                    //update selected device when selected from the list
                    bleConnectionViewModel.updateSelectedDevice(it)

                    print("selectedHBDeviceLogsObs :  $it")
                    //connect to selected device when selected from the list
                    bleConnectionViewModel.connectHBDevice(it, mActivity!!)

                }
            }
        })
    }

    private fun observeScanState() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe the scan state from ViewModel
                deviceStatusViewModel.scanState.collect { state ->
                    print("observeScanStateLogs  ${state}")
                    updateUIForScanState(state)
                }
            }
        }
    }

    private fun observeConnectionState() {
        // Collect the connection state flow
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                bleConnectionViewModel.hbCheckConnectionState.collect { connectionState ->
                    Log.e("connectionState", "   :   " + connectionState)

                    if (connectionState) {
                        Log.e(
                            "scanStateHb :  conn", "  :  ${ScanState.Connected}  :  Selected Device:  ${bleConnectionViewModel.selectedDevice.value}"
                        )

                        //Close BLE device dialog
                        deviceListDialog.dismissDialog()

                        //Close loader after device connected
                        hideDialog()

                        // Save the connected device in shared pref
                        bleConnectionViewModel.selectedDevice.value?.let {
                            bleConnectionViewModel.saveDevice(it)
                        }

//                        bleConnectionViewModel.getBloodPressure()

                        binding.deviceStatusLayout.setupDeviceStatus(mActivity!!, true)

                    } else {

                        Log.e("scanStateHb :  conn", "  :  ${ScanState.Idle}")

                        if (bleConnectionViewModel.selectedDeviceType.value!!.equals(DeviceType.WEIGHING_SCALE)) {
                            binding.deviceStatusLayout.setupDeviceStatus(mActivity!!, false)

                            bleConnectionViewModel.selectedDevice.value?.let {

                            }
                        }
                    }
                }
            }
        }

    }

    private fun observeDataState() {
        // Collect the connection state flow
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                bleConnectionViewModel.deviceResponses.collect { deviceResponse ->
                    Log.e("deviceResponseLogs", "   :   " + deviceResponse)

                    bleConnectionViewModel.getResponseAsString(DeviceType.WEIGHING_SCALE)?.let {

                        Log.e("deviceResponseLogs", "   :   " + bleConnectionViewModel.getResponseAsString(DeviceType.WEIGHING_SCALE))

                        val hbCheckMeasurement: HbCheckMeasurement =
                            bleConnectionViewModel.getResponseAsString(DeviceType.WEIGHING_SCALE)!! as HbCheckMeasurement

                        Log.e("deviceResponseLogs", "   :  hbCheckMeasurement  :  " + hbCheckMeasurement)

                    }
                }
            }
        }
    }

    private fun updateUIForScanState(state: ScanState) {
        Log.e("updatingUIState", " : " + state.toString())
        when (state) {
            is ScanState.Idle -> {
                hideDialog()
            }

            is ScanState.Scanning -> {
                showDialog()
            }

            is ScanState.DevicesFound -> {
                hideDialog()

                // Show dialog with the devices list
                if (state.devices.isNotEmpty()) {
                    deviceListDialog.showDialog(state.devices)
                } else {
                    // Show empty state or message
                    Toast.makeText(requireContext(), "No devices found", Toast.LENGTH_SHORT).show()
                }
            }

            is ScanState.Error -> {
                // Handle error state
                hideDialog()
                Toast.makeText(requireContext(), "Error: ${state.message}", Toast.LENGTH_SHORT).show()
            }

            is ScanState.Connecting -> {
                Log.e("scanStateHeight :  in_conn", "  :  ${ScanState.Connected}")
            }

            is ScanState.Connected -> {

            }
        }
    }

    private fun initializeGroupASteps() {
        // Observe steps changes
        stepsViewModel.steps.observe(viewLifecycleOwner) { steps ->
            binding.stepsLayout.setSteps(steps)
        }

        // ✅ Click listener for navigation
        binding.stepsLayout.setOnStepClickListener { step ->

            println("Step clicked weight : : ${step}")

//            disconnectDevice()

            stepsViewModel.updateStepStatus(stepId = step.id, status = StepStatus.CURRENT)

            when (step.id) {
                BasicHealthTestsType.HEIGHT.stepNumber -> {
                    println("Step 1 clicked ${step}")

                    mActivity?.let {
                        it.navController?.navigate(R.id.weight_to_height_screen_nav_action)
                    }
                }

                BasicHealthTestsType.TEMPERATURE.stepNumber -> {
                    println("Step 2 clicked ${step}")

                    mActivity?.let {
                        it.navController?.navigate(R.id.weight_to_temperature_screen_nav_action)
                    }
                }

                BasicHealthTestsType.SPO2.stepNumber -> {
                    println("Step 3 clicked ${step}")

                    mActivity?.let {
                        it.navController?.navigate(R.id.weight_to_pulse_screen_nav_action)
                    }
                }
            }
        }
    }

    private fun initializeDeviceStatus() {
        // Bind the view model to the custom view
        binding.deviceStatusLayout.bindViewModel(deviceStatusViewModel)

        // Set up click listeners
        binding.deviceStatusLayout.setOnScanClickListener {
            Log.e(TAG, " : onScan ButtonClicked :")

            scan()
        }
    }

    fun onBroadCastData(mac: String, dataHexStr: String, data: ByteArray?) {
        Log.e("MainActivity", "mac:  $mac dataHexStr:  $dataHexStr  : data : $data")

        binding.deviceStatusLayout.setupDeviceStatus(mActivity!!, true)

        if (bleConnectionViewModel.selectedDevice.value == null) {
            bleConnectionViewModel.saveDevice(
                BleDevice(
                    address = mac,
                    name = "Weighing Scale" ?: "Unknown Device",
                    rssi = 0,
                    deviceType = DeviceType.WEIGHING_SCALE,
                    bondState = BluetoothDevice.BOND_BONDED,
                    scanRecord = data ?: byteArrayOf()
                )
            )
        }

//        if (bleConnectionViewModel.selectedDevice.value!!.address == mac) {
        if (mBroadcastDataParsing != null) {

            binding.ivWeighingScale.visibility = View.GONE
            binding.layoutWeightField.visibility = View.VISIBLE
            binding.tvStartWeight.text = "Retest"

            mBroadcastDataParsing!!.dataParsing(data)
        }
//        }
    }

    private var mOldNumberId = -1

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
     */
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

        Log.e("MainActivity", "weight data: $weight : Weight Decimal :-> $weightDecimal : Weight Status :-> $weightStatus")

        var showData = ""

        when (weightStatus) {
            0x00 -> showData += "Real-time data"
            0x01 -> {

                showData += "Stable data"

                /*if (weight > 0) {
                    stopScan()
                }*/
            }

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

        val weightData: WeightData = WeightData(
            weight = formatWeight(
                rawWeight = weight, decimal = weightDecimal
            ), unit = unitStr, impedance = adc, algorithmId = algorithmId
        )


        binding.editWeight.setText(weightData.weight)

//        hideDialog()
//        mList!!.add(showData)
        println("\nweightStatus get Weight Data :: weightStatus :: $weightStatus  :: weight :: $weight")

        if (weightStatus == 0x01 && weight > 0) {

            initBodyFatDataCalculation(
                sex = if (PatientPref.patient?.gender == "Male") 1 else 2,
                age = DatePickerUtil.getAgeFromDob(PatientPref.patient?.dateOfBirth.toString()),
                height = BodyCheckupPref.height.toString().toInt(),
                weight = weight,
                adc = adc
            )
            return
        }
    }

    /**
     * initBodyFatDataCalculation
     *
     * @param sex    sex Female=2; Male=1;
     * @param age    age (0~120)
     * @param height height (0-269)
     * @param weight weight (0~220)
     * @param adc    adc (0~1000)
     */
    private fun initBodyFatDataCalculation(sex: Int, age: Int, height: Int, weight: Int, adc: Int) {

        println("\nweightStatus get Weight Data :: sex :: $sex  :: age :: $age :: height:: $height  :: weight :: $weight  :: adc ::$adc ")

        val bodyFatData = AicareBleConfig.getBodyFatData(
            AlgorithmUtil.AlgorithmType.TYPE_AICARE, sex, age, weight.toDouble() / 100, height, adc
        )
        val moreFatData = AicareBleConfig.getMoreFatData(
            sex, height, weight.toDouble() / 100, bodyFatData.bfr, bodyFatData.rom, bodyFatData.pp
        )
        //http://doc.elinkthings.com/web/#/12?page_id=50  -> Part of the class description -> cn.net.aicare.algorithmutil.BodyFatData and MoreFatData doc

        Log.e(TAG, "WeighingScaleLogs  : BodyFat  ::::  Called  :$weight  : :  ${weight.toDouble() / 100}    :: " + Gson().toJson(bodyFatData))
        Log.e(TAG, "WeighingScaleLogs  : MoreFat  ::::  Called  ::" + Gson().toJson(moreFatData))

        val result = mapToBodyParameters(weight.toDouble() / 100, bodyFatData, moreFatData)

        Log.e(TAG, "WeighingScaleLogs  : result  ::::  final  ::" + Gson().toJson(result))

        if (result.isNotEmpty()) AILinkBleManager.getInstance().stopScan()

        setWeightResults(result)

//        mList!!.add("bodyFatData1:bmi=" + bodyFatData.bmi + "  bfr=" + bodyFatData.bfr + "  rom=" + bodyFatData.rom + "  pp=" + bodyFatData.pp + "  vwc=" + bodyFatData.vwc + "  bm=" + bodyFatData.bm)
//        mList!!.add("bodyFatData2:$moreFatData")
//        mHandler.sendEmptyMessage(REFRESH_DATA)
    }

    private fun setWeightResults(weightMeasurements: List<WeightMeasurement>) {

        weightMeasurementsList = weightMeasurements

        binding.rvWeightResults.visibility = View.VISIBLE

//        binding.editWeight.visibility = View.GONE
        binding.layoutWeightField.visibility = View.GONE
        binding.buttonStartWeight.visibility = View.VISIBLE

        binding.rvWeightResults.layoutManager = GridLayoutManager(context, 3)
        binding.rvWeightResults.adapter = weightMeasurements.let {
            WeightResultsListAdapter(it)
        }

    }

    fun formatWeight(rawWeight: Int, decimal: Int): String {
        val factor = Math.pow(10.0, decimal.toDouble())
        val weight = rawWeight / factor
        return String.format("%.${decimal}f", weight)
//        return String.format("%.${decimal}f", weight)
    }


}