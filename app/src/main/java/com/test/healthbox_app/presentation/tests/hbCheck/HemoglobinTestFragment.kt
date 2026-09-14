package com.test.healthbox_app.presentation.tests.hbCheck

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.test.healthbox_app.BleConnectionViewModel
import com.test.healthbox_app.MainActivity
import com.test.healthbox_app.R
import com.test.healthbox_app.base.BaseFragment
import com.test.healthbox_app.bluetooth.DeviceType
import com.test.healthbox_app.data.model.BodyCheckupPref
import com.test.healthbox_app.databinding.HemoglobinTestFragmentBinding
import com.test.healthbox_app.domain.model.BleDevice
import com.test.healthbox_app.domain.model.HbCheckMeasurement
import com.test.healthbox_app.domain.model.ScanState
import com.test.healthbox_app.domain.model.StepStatus
import com.test.healthbox_app.presentation.dialog.DeviceListDialog
import com.test.healthbox_app.presentation.util.BasicHealthTestsType
import com.test.healthbox_app.presentation.util.CustomSnackBar
import com.test.healthbox_app.presentation.view.StepsViewModel
import com.test.healthbox_app.presentation.view.deviceStatus.DeviceStatusViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class HemoglobinTestFragment() : BaseFragment() {

    private lateinit var binding: HemoglobinTestFragmentBinding

    private val stepsViewModel: StepsViewModel by activityViewModels()

    private val deviceStatusViewModel: DeviceStatusViewModel by activityViewModels()

    private lateinit var bleConnectionViewModel: BleConnectionViewModel

    private lateinit var deviceListDialog: DeviceListDialog

    override fun checkConnectivity() {
    }

    override val isConnected: Unit = Unit

    private var hbCounter = 0
    private var hbTimerJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle Back Press
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {

                    // Handle Back Press
                    requireActivity().onBackPressedDispatcher.addCallback(
                        viewLifecycleOwner, object : OnBackPressedCallback(true) {
                            override fun handleOnBackPressed() {

                                CustomSnackBar.makeWithAction(
                                    binding.root, "Are you sure to go back, It will clear all the test results?", "Yes", Color.WHITE, action = {
                                        findNavController().navigate(R.id.close_button_action_hb_screen)
                                    }, duration = 5000, type = CustomSnackBar.Companion.SnackBarType.CUSTOM
                                ).show()
                            }
                        })
                }
            })

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = HemoglobinTestFragmentBinding.inflate(inflater)

        // Get ViewModel from activity
        bleConnectionViewModel = (activity as MainActivity).bleViewModel

        binding.viewModel = bleConnectionViewModel

        binding.lifecycleOwner = this

        // Bind user info view with ViewModel
        binding.userInfoView.loadPatientFromPref()

        initializeGroupASteps()

        initializeDeviceStatus()

        setAndObserveDeviceAvailability()

        setupDialog()

        observeScanState()

        observeConnectionState()

        observeDataState()

        nextTestCall()

        return binding.root
    }

    private fun setAndObserveDeviceAvailability() {
        bleConnectionViewModel.setDeviceType(DeviceType.HB_CHECK)

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

                    bleConnectionViewModel.connectHBDevice(device, mActivity!!)
//                    bleConnectionViewModel.connectToDevice(device, bleConnectionViewModel.selectedDeviceType.value!!)
                }
            }
        }
    }

    private fun nextTestCall() {
        binding.buttonNextLayout.setOnClickListener { view ->
            mActivity?.let {
                CoroutineScope(Dispatchers.Main).launch {
                    saveHemoglobinData()

                    disconnectDevice()

                    delay(300)

                    it.navController?.navigate(R.id.next_button_results_action)
                }
            }
        }
    }

    fun saveHemoglobinData() {
        binding.editHemoglobinValue.text.toString().let {
            BodyCheckupPref.hemoglobin = binding.editHemoglobinValue.text.toString()
        }
    }

    fun disconnectDevice() {

        bleConnectionViewModel.disconnectToHbDevice()

        bleConnectionViewModel.setSelectedDevice(null)

        bleConnectionViewModel.setDeviceType(null)
    }

    private fun setupDialog() {
        deviceListDialog = mActivity?.let {
            DeviceListDialog(it, onDeviceClose = {
                lifecycleScope.launch {
                    println("onScan List Device Dialog close click HB")

                    bleConnectionViewModel.stopScanning()

                    delay(400)

                    deviceListDialog.dismissDialog()
                }
            })
        }!!

        deviceListDialog.setOnItemSelectedListener(object : DeviceListDialog.OnItemClickListener {
            override fun onItemSelect(bleDevice: BleDevice?) {
//                binding.buttonScan.visibility = View.GONE

                print("selectedHBDeviceLogsObs :  $bleDevice")

                showDialog()

                bleDevice?.let {
                    //update selected device when selected from the list
                    bleConnectionViewModel.updateSelectedDevice(it)

                    print("selectedHBDeviceLogsObs :  $it")

                    //connect to selected device when selected from the list
                    bleConnectionViewModel.connectHBDevice(it, mActivity!!)

//                    bleConnectionViewModel.connectToDevice(it, bleConnectionViewModel.selectedDeviceType.value)
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
                    println("connectionState Hb Check Device ::  :   " + connectionState)

                    if (connectionState) {
                        Log.e(
                            "scanStateHb :  conn", "  :  ${ScanState.Connected}  :  Selected Device:  ${bleConnectionViewModel.selectedDevice.value}"
                        )

                        withContext(Dispatchers.Main) {

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
                        }
                    } else {

                        Log.e("scanStateHb :  conn", "  :  ${ScanState.Idle}")

                        if (bleConnectionViewModel.selectedDeviceType.value!!.equals(DeviceType.HB_CHECK)) {
                            binding.deviceStatusLayout.setupDeviceStatus(mActivity!!, false)

                            bleConnectionViewModel.selectedDevice.value?.let {

                            }
                        }
                    }


                    /*connectionStateMap.forEach { (deviceType, state) ->
                        when (state) {

                            is ConnectionState.Connected -> {
                                Log.e("scanStateHeight :  conn", "  :  ${ScanState.Connected}")

                                //Close BLE device dialog
                                deviceListDialog.dismissDialog()

                                //Close loader after device connected
                                hideDialog()

                                // Save the connected device in shared pref
                                bleConnectionViewModel.selectedDevice.value?.let {
                                    bleConnectionViewModel.saveDevice(it)
                                }

                                bleConnectionViewModel.getBloodPressure()

                                binding.deviceStatusLayout.setupDeviceStatus(mActivity!!, true)
//                                binding.tvDeviceAvailability.text = "Connected"
//                                binding.tvDeviceAvailability.setTextColor(mActivity?.resources!!.getColor(R.color.green))
                            }

                            is ConnectionState.Connecting -> {

                            }

                            is ConnectionState.Disconnected -> {
//                                binding.tvDeviceAvailability.text = "Disconnected"

                                if (bleConnectionViewModel.selectedDeviceType.value!!.equals(DeviceType.HB_CHECK)) {
                                    binding.deviceStatusLayout.setupDeviceStatus(mActivity!!, false)

                                    bleConnectionViewModel.selectedDevice.value?.let {
                                        *//*bleConnectionViewModel.connectToDevice(
                                            bleConnectionViewModel.selectedDevice.value!!,
                                            bleConnectionViewModel.selectedDeviceType.value!!
                                        )*//*
                                    }
                                }
                            }
                        }
                    }*/

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

                    bleConnectionViewModel.getResponseAsString(DeviceType.HB_CHECK)?.let {

                        println("deviceResponseLogs :   " + bleConnectionViewModel.getResponseAsString(DeviceType.HB_CHECK))

                        val hbCheckMeasurement: HbCheckMeasurement =
                            bleConnectionViewModel.getResponseAsString(DeviceType.HB_CHECK)!! as HbCheckMeasurement

                        println("deviceResponseLogs   :  hbCheckMeasurement  :  " + hbCheckMeasurement)

                        val hbValue = hbCheckMeasurement.value // adjust field name if needed

                        println("deviceResponseLogs hbCheckMeasurement value :: $hbValue")

                        when (hbValue) {
                            "2222" -> {
                                showHbMessage("Checking process started")
                                startHbCheckCounter(seconds = 5)
                            }

                            "2221" -> {
                                showHbMessage("Please collect blood and insert strip")
                            }

                            "2223" -> {
                                showHbMessage("") // blank message
                            }

                            "2227" -> {
                                showHbMessage("Error 2 - Please clean and close the tray")
                            }

                            "2235" -> {
                                showHbMessage("") // blank message
                            }

                            "2225" -> {
                                showHbMessage("Error 1 - Please clean and close the tray")
                            }

                            "78" -> {
                                showHbMessage("Error 2 - Please clean and close the tray")
                            }

                            "2224" -> {
                                showHbMessage("Please wait for Result")
                                decreaseHbCounterByOne()
                            }

                            else -> {
                                println("deviceResponseLogs HB Result value :: $hbValue")
                                hbValue?.toDouble()?.let { value ->

                                    val res = value / 10

                                    if (res > 0) {
                                        binding.tvMessage.visibility = View.GONE
                                        binding.editHemoglobinValue.setText(res.toString())

                                        bleConnectionViewModel.stopHbTest()
                                    }


                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startHbCheckCounter(seconds: Int) {
        hbTimerJob?.cancel()
        hbCounter = seconds

        hbTimerJob = viewLifecycleOwner.lifecycleScope.launch {
            while (hbCounter > 0) {

                println("HB_COUNTER Counter  : : $hbCounter")

                delay(1000)

                showHbMessage("HB checking process completed")
                if (hbCounter != 5) binding.editHemoglobinValue.setText(hbCounter.toString())
//                hbCounter--
            }

            println("HB_COUNTER Countdown finished :: ")
        }
    }

    private fun decreaseHbCounterByOne() {
        if (hbCounter > 0) {
            hbCounter--
            Log.e("HB_COUNTER", "Decreased counter by 1 → $hbCounter")

            binding.editHemoglobinValue.setText(hbCounter.toString())
        }
    }

    private fun showHbMessage(message: String) {
        if (message.isNotEmpty()) {
            println("HB_MESSAGE  ::  $message")
            // Optionally show a SnackBar or Toast here
            // CustomSnackBar.show(view, message, CustomSnackBar.SnackBarType.INFO)
            binding.tvMessage.visibility = View.VISIBLE
            binding.tvMessage.setText(message.toString())
        }
    }

    private fun updateUIForScanState(state: ScanState) {
        Log.e("updatingUIState", " : " + state.toString())
        when (state) {
            is ScanState.Idle -> {
                // Handle idle state (initial state)
//                binding.scanDeviceProgressBar.visibility = View.GONE
                hideDialog()
//                binding.buttonScan.isEnabled = true
//                binding.tvDeviceAvailability.text = "Ready to scan"
            }

            is ScanState.Scanning -> {
                // Handle scanning state (show progress)
                showDialog()
//                binding.scanDeviceProgressBar.visibility = View.VISIBLE
//                binding.buttonScan.isEnabled = false
//                binding.tvDeviceAvailability.text = "Scanning for devices..."
            }

            is ScanState.DevicesFound -> {
                // Handle the found devices
//                binding.scanDeviceProgressBar.visibility = View.GONE
//                binding.buttonScan.isEnabled = true
//                binding.tvDeviceAvailability.text = "Found ${state.devices.size} devices"

                hideDialog()

                // Show dialog with the devices list
                if (state.devices.isNotEmpty()) {
                    deviceListDialog.showDialog(state.devices)
                } else {
                    // Show empty state or message
                    Toast.makeText(requireContext(), "No devices found", Toast.LENGTH_SHORT).show()
                    // deviceStatusViewModel is activity-scoped (shared across every test screen),
                    // so this terminal empty result must be consumed or it replays on the next
                    // screen/subscribe.
                    deviceStatusViewModel.clearScanState()
                }
            }

            is ScanState.Error -> {
                // Handle error state
                hideDialog()
//                binding.scanDeviceProgressBar.visibility = View.GONE
//                binding.buttonScan.isEnabled = true
//                binding.tvDeviceAvailability.text = "Error: ${state.message}"
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

            disconnectDevice()
            stepsViewModel.updateStepStatus(step.id, status = StepStatus.CURRENT)

            when (step.id) {
                BasicHealthTestsType.HEIGHT.stepNumber -> {
                    println("Step 1 clicked ${step}")

                    mActivity?.let {
                        it.navController?.navigate(R.id.hb_check_to_height_screen_nav_action)
                    }
                }

                BasicHealthTestsType.TEMPERATURE.stepNumber -> {
                    println("Step 2 clicked ${step}")

                    mActivity?.let {
                        it.navController?.navigate(R.id.hb_check_to_temperature_screen_nav_action)
                    }
                }

                BasicHealthTestsType.SPO2.stepNumber -> {
                    println("Step 3 clicked ${step}")

                    mActivity?.let {
                        it.navController?.navigate(R.id.hb_check_to_pulse_screen_nav_action)
                    }
                }

                BasicHealthTestsType.WEIGHT.stepNumber -> {
                    println("Step 4 clicked ${step}")

                    mActivity?.let {
                        it.navController?.navigate(R.id.hb_check_to_weight_screen_nav_action)
                    }
                }

                BasicHealthTestsType.VISION.stepNumber -> {
                    println("Step 5 clicked ${step}")

                    mActivity?.let {
                        it.navController?.navigate(R.id.hb_check_to_vision_screen_nav_action)
                    }
                }

                BasicHealthTestsType.BLOOD_PRESSURE.stepNumber -> {
                    println("Step 6 clicked ${step}")

                    mActivity?.let {
                        it.navController?.navigate(R.id.hb_check_to_blood_pressure_screen_nav_action)
                    }
                }

                BasicHealthTestsType.BLOOD_SUGAR.stepNumber -> {
                    println("Step 7 clicked ${step}")

                    mActivity?.let {
                        it.navController?.navigate(R.id.hb_check_to_glucose_test_screen_nav_action)
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

            deviceStatusViewModel.startScan()
        }

    }

}