package com.test.healthbox_app.presentation.tests.glucose

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
import com.test.healthbox_app.databinding.BloodSugarTestFragmentBinding
import com.test.healthbox_app.domain.model.BleDevice
import com.test.healthbox_app.domain.model.ConnectionState
import com.test.healthbox_app.domain.model.GlucoseMeasurement
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BloodSugarTestFragment() : BaseFragment() {

    private lateinit var binding: BloodSugarTestFragmentBinding

    private val stepsViewModel: StepsViewModel by activityViewModels()

    private val deviceStatusViewModel: DeviceStatusViewModel by activityViewModels()

    private lateinit var bleConnectionViewModel: BleConnectionViewModel

    private lateinit var deviceListDialog: DeviceListDialog

    companion object {
        const val TAG = "GlucoseTestFragment"
    }

    override fun checkConnectivity() {
    }

    override val isConnected: Unit = Unit

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle Back Press
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    CustomSnackBar.makeWithAction(
                        binding.root, "Are you sure to go back, It will clear all the test results?", "Yes", Color.WHITE, action = {
                            findNavController().navigate(R.id.close_button_action_glucose_screen)
                        }, duration = 5000, type = CustomSnackBar.Companion.SnackBarType.CUSTOM
                    ).show()
                }
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = BloodSugarTestFragmentBinding.inflate(inflater)

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
        bleConnectionViewModel.setDeviceType(DeviceType.GLUCOSE_METER)

        bleConnectionViewModel.getDevice()

        if (bleConnectionViewModel.selectedDevice.value == null) {
            binding.deviceStatusLayout.setUpDeviceAvailability(false)
        } else {
            binding.deviceStatusLayout.setUpDeviceAvailability(true)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            bleConnectionViewModel.selectedDevice.collect { device ->
                Log.i(TAG, "selectedGluDeviceLogsObs : Device ::  $device")

                if (device != null) {
                    binding.deviceStatusLayout.setUpDeviceAvailability(true)

                    bleConnectionViewModel.connectToDevice(device, bleConnectionViewModel.selectedDeviceType.value!!)
                }
            }
        }
    }

    private fun nextTestCall() {

        binding.ivSugarIcon.setOnClickListener { view ->

            binding.layoutBloodGlucoseTestSteps.visibility = View.GONE
            binding.layoutBloodSugarReadings.visibility = View.VISIBLE

        }

        binding.buttonStart.setOnClickListener {
            ensureBluetoothEnabled(binding.root) {
                showDialog()

                val device = bleConnectionViewModel.selectedDevice.value

                if (device != null) {
                    binding.deviceStatusLayout.setUpDeviceAvailability(true)

                    bleConnectionViewModel.connectToDevice(device, bleConnectionViewModel.selectedDeviceType.value!!)
                }
            }
        }

        binding.buttonRetest.setOnClickListener {
            ensureBluetoothEnabled(binding.root) {
                showDialog()

                val device = bleConnectionViewModel.selectedDevice.value

                if (device != null) {
                    binding.deviceStatusLayout.setUpDeviceAvailability(true)

                    bleConnectionViewModel.connectToDevice(device, bleConnectionViewModel.selectedDeviceType.value!!)
                }
            }
        }

        binding.buttonNextLayout.setOnClickListener { view ->
            mActivity?.let {
                CoroutineScope(Dispatchers.Main).launch {

                    saveBloodSugarData()

                    bleConnectionViewModel.selectedDevice.value?.let {
                        bleConnectionViewModel.disconnect(
                            bleConnectionViewModel.selectedDevice.value!!, bleConnectionViewModel.selectedDeviceType.value!!
                        )
                    }

                    stepsViewModel.goToNextStep()

                    bleConnectionViewModel.setSelectedDevice(null)

                    bleConnectionViewModel.setDeviceType(null)

                    it.navController?.navigate(R.id.next_button_hemoglobin_checkup_action)
                }
            }
        }
    }

    fun saveBloodSugarData() {
        binding.editBloodSugar.text.toString().let {
            BodyCheckupPref.sugar = binding.editBloodSugar.text.toString()
        }
    }

    fun disconnectDevice() {
        bleConnectionViewModel.setSelectedDevice(null)

        bleConnectionViewModel.setDeviceType(null)
    }


    private fun setupDialog() {
        deviceListDialog = mActivity?.let {
            DeviceListDialog(it, onDeviceClose = {
                deviceListDialog.dismissDialog()
            })
        }!!

        deviceListDialog.setOnItemSelectedListener(object : DeviceListDialog.OnItemClickListener {
            override fun onItemSelect(bleDevice: BleDevice?) {
//                binding.buttonScan.visibility = View.GONE

                Log.i(TAG, "selectedGluDeviceLogsObs :  $bleDevice")

                showDialog()

                bleDevice?.let {
                    //update selected device when selected from the list
                    bleConnectionViewModel.updateSelectedDevice(it)

                    CoroutineScope(Dispatchers.IO).launch {
                        deviceStatusViewModel.stopBleScan()

//                        deviceListDialog.dismissDialog()

                        delay(100)

                        Log.i(TAG, "selectedGluDeviceLogsObs :  $it")

                        bleConnectionViewModel.connectToDevice(it, bleConnectionViewModel.selectedDeviceType.value!!)
                    }

                    /*if (it.bondState == BluetoothDevice.BOND_BONDED) {
                        bleConnectionViewModel.connectToDevice(it, bleConnectionViewModel.selectedDeviceType.value!!)
                    } else {
                        //connect to selected device when selected from the list
                        bleConnectionViewModel.pairBTDevice(it)
                    }*/
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
                    Log.i(TAG, "observeScanStateLogs  ${state}")
                    updateUIForScanState(state)
                }
            }
        }
    }

    private fun observeConnectionState() {
        // Collect the connection state flow
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                bleConnectionViewModel.connectionState.collect { connectionStateMap ->
                    Log.i("connectionState Glucose Logs :", "   :   " + connectionStateMap)

                    connectionStateMap.forEach { (deviceType, state) ->
                        when (state) {

                            is ConnectionState.Connected -> {
                                Log.i("scanStateGlucose :  conn", "  :  ${ScanState.Connected}")

                                hideDialog()

                                //To clear the scanned devices list
                                deviceStatusViewModel.updateScannedDevicesList()

                                //Close BLE device dialog
                                deviceListDialog.dismissDialog()

                                //Close loader after device connected
//                                hideDialog()

                                // Save the connected device in shared pref
                                bleConnectionViewModel.selectedDevice.value?.let { bleConnectionViewModel.saveDevice(it) }

                                binding.deviceStatusLayout.setupDeviceStatus(mActivity!!, true)
//                                binding.tvDeviceAvailability.text = "Connected"
//                                binding.tvDeviceAvailability.setTextColor(mActivity?.resources!!.getColor(R.color.green))

                                bleConnectionViewModel.getGlucoseData()
                            }

                            is ConnectionState.Connecting -> {

                            }

                            is ConnectionState.Disconnected -> {

                                hideDialog()
//                                binding.tvDeviceAvailability.text = "Disconnected"
                                if (bleConnectionViewModel.selectedDeviceType.value!!.equals(DeviceType.GLUCOSE_METER)) {

//                                    binding.buttonStart.visibility = View.GONE

                                    binding.deviceStatusLayout.setupDeviceStatus(mActivity!!, false)

                                    if (binding.editBloodSugar.text.toString().isNotEmpty()) {
                                        binding.buttonRetest.visibility = View.VISIBLE
                                    } else {
                                        binding.buttonStart.visibility = View.VISIBLE
                                    }


                                    /*bleConnectionViewModel.connectToDevice(
                                        bleConnectionViewModel.selectedDevice.value!!, bleConnectionViewModel.selectedDeviceType.value!!
                                    )*/
                                }
                            }

                            is ConnectionState.Error -> {
                                println("device connection error :: ${state.message}")

                                Toast.makeText(mActivity, state.message, Toast.LENGTH_LONG).show()

                                hideDialog()

                                if (binding.editBloodSugar.text.toString().isNotEmpty()) {
                                    binding.buttonRetest.visibility = View.VISIBLE
                                } else {
                                    binding.buttonStart.visibility = View.VISIBLE
                                }
                            }

                            is ConnectionState.Paired -> TODO()

                            is ConnectionState.PairedFailed -> TODO()
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
                    Log.e(TAG, "deviceResponseLogs   :   " + deviceResponse)

                    bleConnectionViewModel.getResponseAsString(DeviceType.GLUCOSE_METER)?.let {

                        Log.e(TAG, "deviceResponseLogs   :   " + bleConnectionViewModel.getResponseAsString(DeviceType.GLUCOSE_METER))

                        val glucoseMeasurement: GlucoseMeasurement =
                            bleConnectionViewModel.getResponseAsString(DeviceType.GLUCOSE_METER)!! as GlucoseMeasurement

                        Log.e(TAG, "deviceResponseLogs   :  glucoseMeasurement  :  " + glucoseMeasurement)

                        glucoseMeasurement.let {
                            if (glucoseMeasurement.sugar > 0) {

                                hideDialog()

                                bleConnectionViewModel.setDeviceResponseStatus(false)

                                binding.layoutBloodGlucoseTestSteps.visibility = View.GONE
                                binding.layoutBloodSugarReadings.visibility = View.VISIBLE

                                binding.editBloodSugar.setText("${glucoseMeasurement.sugar}")
                            }

                        }


                        /*bloodPressureMeasurement.let {

                            if (bloodPressureMeasurement.isTesting) {

                                bleConnectionViewModel.setDeviceResponseStatus(false)

                                binding.editTestingValue.setText("${bloodPressureMeasurement.systolic}")
                            }

                            if (!bloodPressureMeasurement.isTesting && bloodPressureMeasurement.isValid) {

                                Log.e(
                                    "deviceResponseLogs",
                                    "   :  bloodPressureMeasurement result  :  " + bleConnectionViewModel.deviceResponsesReceived.value
                                )

                                bleConnectionViewModel.setDeviceResponseStatus(true)

                                binding.editSystolic.setText("${bloodPressureMeasurement.systolic}")
                                binding.editDiastolic.setText("${bloodPressureMeasurement.diastolic}")
                                binding.editHeartRate.setText("${bloodPressureMeasurement.pulseRate}")
    //                            binding.editPulse.setText(pulseMeasurement.oxygenSaturation)


                                Log.e(
                                    "deviceResponseLogs",
                                    "   :  bloodPressureMeasurement result  :  " + bleConnectionViewModel.deviceResponsesReceived.value
                                )
                            }
                        }*/
                    }
                }
            }
        }
    }

    private fun updateUIForScanState(state: ScanState) {
        Log.e(TAG, "updatingUIState : " + state.toString())
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
                Log.e(TAG, "scanStateHeight :  in_conn  :  ${ScanState.Connected}")
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
            stepsViewModel.updateStepStatus(stepId = step.id, status = StepStatus.CURRENT)

            when (step.id) {
                BasicHealthTestsType.HEIGHT.stepNumber -> {
                    println("Step 1 clicked ${step}")

                    mActivity?.let {
                        it.navController?.navigate(R.id.glucose_to_height_screen_nav_action)
                    }
                }

                BasicHealthTestsType.TEMPERATURE.stepNumber -> {
                    println("Step 2 clicked ${step}")

                    mActivity?.let {
                        it.navController?.navigate(R.id.glucose_to_temperature_screen_nav_action)
                    }
                }

                BasicHealthTestsType.SPO2.stepNumber -> {
                    println("Step 3 clicked ${step}")

                    mActivity?.let {
                        it.navController?.navigate(R.id.glucose_to_pulse_screen_nav_action)
                    }
                }

                BasicHealthTestsType.WEIGHT.stepNumber -> {
                    println("Step 4 clicked ${step}")

                    mActivity?.let {
                        it.navController?.navigate(R.id.glucose_to_weight_screen_nav_action)
                    }
                }

                BasicHealthTestsType.VISION.stepNumber -> {
                    println("Step 5 clicked ${step}")

                    mActivity?.let {
                        it.navController?.navigate(R.id.glucose_to_vision_screen_nav_action)
                    }
                }

                BasicHealthTestsType.BLOOD_PRESSURE.stepNumber -> {
                    println("Step 5 clicked ${step}")

                    mActivity?.let {
                        it.navController?.navigate(R.id.glucose_to_blood_pressure_screen_nav_action)
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