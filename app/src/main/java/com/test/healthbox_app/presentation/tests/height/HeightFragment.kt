package com.test.healthbox_app.presentation.tests.height

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
import com.google.android.material.snackbar.Snackbar
import com.test.healthbox_app.BleConnectionViewModel
import com.test.healthbox_app.MainActivity
import com.test.healthbox_app.R
import com.test.healthbox_app.base.BaseFragment
import com.test.healthbox_app.bluetooth.DeviceType
import com.test.healthbox_app.data.model.BodyCheckupPref
import com.test.healthbox_app.databinding.HeightFragmentBinding
import com.test.healthbox_app.domain.model.BleDevice
import com.test.healthbox_app.domain.model.ConnectionState
import com.test.healthbox_app.domain.model.HeightMeasurement
import com.test.healthbox_app.domain.model.ScanState
import com.test.healthbox_app.domain.model.StepItem
import com.test.healthbox_app.domain.model.StepStatus
import com.test.healthbox_app.presentation.dialog.DeviceListDialog
import com.test.healthbox_app.presentation.util.BasicHealthTestsType
import com.test.healthbox_app.presentation.util.Constants
import com.test.healthbox_app.presentation.util.CustomSnackBar
import com.test.healthbox_app.presentation.view.StepsViewModel
import com.test.healthbox_app.presentation.view.deviceStatus.DeviceStatusViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val s = "updatingUIState"

@AndroidEntryPoint
class HeightFragment() : BaseFragment() {

    private lateinit var binding: HeightFragmentBinding

    private val stepsViewModel: StepsViewModel by activityViewModels()

    private val deviceStatusViewModel: DeviceStatusViewModel by activityViewModels()

    private lateinit var bleConnectionViewModel: BleConnectionViewModel

    private lateinit var deviceListDialog: DeviceListDialog

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
                            findNavController().navigate(R.id.close_button_action_height_screen)
                        }, duration = 5000, type = CustomSnackBar.Companion.SnackBarType.CUSTOM
                    ).show()
                }
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = HeightFragmentBinding.inflate(inflater)

        // Get ViewModel from activity
        bleConnectionViewModel = (activity as MainActivity).bleViewModel

        binding.viewModel = bleConnectionViewModel

        // Bind user info view with ViewModel
        binding.userInfoView.loadPatientFromPref()

        initializeGroupASteps()

        initializeDeviceStatus()

        setupDialog()

        setAndObserveDeviceAvailability()

        observeScanState()

        observeConnectionState()

        observeDataState()

        nextTestCall()

        if (Constants.LOGS_ENABLE) {
            binding.editHeight.setText("172")
        }

        return binding.root
    }

    private fun setAndObserveDeviceAvailability() {
        bleConnectionViewModel.setDeviceType(DeviceType.HEIGHT)

        bleConnectionViewModel.getDevice()

        if (bleConnectionViewModel.selectedDevice.value == null) {
            binding.deviceStatusLayout.setUpDeviceAvailability(false)
        } else {
            binding.deviceStatusLayout.setUpDeviceAvailability(true)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            bleConnectionViewModel.selectedDevice.collect { device ->

                println("selectedDeviceLogsObs Height :  $device")

                if (device != null) {
                    binding.deviceStatusLayout.setUpDeviceAvailability(true)

                    bleConnectionViewModel.connectToDevice(device, bleConnectionViewModel.selectedDeviceType.value!!)
                }
            }
        }
    }

    private fun nextTestCall() {
        binding.buttonNextLayout.setOnClickListener { view ->
            mActivity?.let {

                if (binding.editHeight.text.toString().isEmpty()) {
                    CustomSnackBar.make(
                        binding.root, "Perform Height test first", Snackbar.LENGTH_SHORT, CustomSnackBar.Companion.SnackBarType.WARNING
                    ).show()
                    return@setOnClickListener
                }

                CoroutineScope(Dispatchers.Main).launch {
                    println("Body checkup response :: -> ${BodyCheckupPref.height} ")

//                if (binding.editHeight.text.toString().toInt() < 100) {

                    println("Body checkup response :: Selected Device ${bleConnectionViewModel.selectedDevice.value}  :: Device Type :: ${bleConnectionViewModel.selectedDeviceType.value} ")

                    bleConnectionViewModel.selectedDevice.value?.let {
                        bleConnectionViewModel.disconnect(
                            bleConnectionViewModel.selectedDevice.value!!, bleConnectionViewModel.selectedDeviceType.value!!
                        )
                    }

                    delay(100)

                    stepsViewModel.goToNextStep()

                    bleConnectionViewModel.setSelectedDevice(null)

                    bleConnectionViewModel.setDeviceType(null)

                    saveHeightData()

                    it.navController?.navigate(R.id.next_button_temperature_checkup_action)
                }
            }
        }
    }

    fun saveHeightData() {
        binding.editHeight.text.toString().let {
            BodyCheckupPref.height = binding.editHeight.text.toString()
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
//                binding.buttonScan.visibility = View.GONE

                showDialog()

                bleDevice?.let {
                    //update selected device when selected from the list
                    bleConnectionViewModel.updateSelectedDevice(it)

                    CoroutineScope(Dispatchers.IO).launch {
                        deviceStatusViewModel.stopBleScan()

                        delay(100)

                        withContext(Dispatchers.Main) {
                            //connect to selected device when selected from the list
                            bleConnectionViewModel.connectToDevice(it, bleConnectionViewModel.selectedDeviceType.value)
                        }
                    }
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
                bleConnectionViewModel.connectionState.collect { connectionStateMap ->
                    Log.e("connectionState", "   :   " + connectionStateMap)

                    connectionStateMap.forEach { (deviceType, state) ->
                        when (state) {

                            is ConnectionState.Connected -> {
                                Log.e("scanStateHeight :  conn", "  :  ${ScanState.Connected}")

                                //To clear the scanned devices list
                                deviceStatusViewModel.updateScannedDevicesList()

                                //Close BLE device dialog
                                deviceListDialog.dismissDialog()

                                //Close loader after device connected
                                hideDialog()

                                // Save the connected device in shared pref
                                bleConnectionViewModel.selectedDevice.value?.let {

                                    bleConnectionViewModel.saveDevice(it)
                                }

                                binding.deviceStatusLayout.setupDeviceStatus(mActivity!!, true)
//                                binding.tvDeviceAvailability.text = "Connected"
//                                binding.tvDeviceAvailability.setTextColor(mActivity?.resources!!.getColor(R.color.green))
                            }

                            is ConnectionState.Connecting -> {

                            }

                            is ConnectionState.Disconnected -> {
//                                binding.tvDeviceAvailability.text = "Disconnected"
                                if (bleConnectionViewModel.selectedDeviceType.value!!.equals(DeviceType.HEIGHT)) {

                                    binding.deviceStatusLayout.setupDeviceStatus(mActivity!!, false)

                                    /*bleConnectionViewModel.connectToDevice(
                                        bleConnectionViewModel.selectedDevice.value!!, bleConnectionViewModel.selectedDeviceType.value!!
                                    )*/
                                }

                            }

                            is ConnectionState.Error -> {
                            }

                            is ConnectionState.Paired -> TODO()

                            is ConnectionState.PairedFailed -> TODO()
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                bleConnectionViewModel.btConnectionState.collect { connectionStateMap ->
                    println("Connection State Printer Device Status    :: Printer ::  " + connectionStateMap)
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

                    bleConnectionViewModel.getResponseAsString(DeviceType.HEIGHT)?.let {
                        val heightMeasurement: HeightMeasurement =
                            bleConnectionViewModel.getResponseAsString(DeviceType.HEIGHT)!! as HeightMeasurement

                        heightMeasurement.let {

                            if (heightMeasurement.isValid) {
                                binding.editHeight.setText(heightMeasurement.heightCm.toString())
                            } else {
                                if (heightMeasurement.error == "F") binding.editHeight.setText("Err")
                            }
                        }

                        Log.e("deviceResponseLogs", "   :   " + bleConnectionViewModel.getResponseAsString(DeviceType.HEIGHT))

                        /*if (bleConnectionViewModel.getResponseAsString(DeviceType.HEIGHT) == "F") {
                            binding.editHeight.setText("Err")
                        } else {
                            binding.editHeight.setText(bleConnectionViewModel.getResponseAsString(DeviceType.HEIGHT))
                        }*/
                    }

                }
            }
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
                    if (bleConnectionViewModel.selectedDevice.value == null) Toast.makeText(requireContext(), "No devices found", Toast.LENGTH_SHORT)
                        .show()
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
        // Initialize steps if needed (typically done in the activity or first fragment)
        if (stepsViewModel.steps.value == null) {
            val healthSteps = listOf(
                StepItem(
                    BasicHealthTestsType.HEIGHT.stepNumber,
                    BasicHealthTestsType.HEIGHT.stepName,
                    R.drawable.ic_height,
                    false,
                    StepStatus.CURRENT
                ), StepItem(
                    BasicHealthTestsType.TEMPERATURE.stepNumber,
                    BasicHealthTestsType.TEMPERATURE.stepName,
                    R.drawable.ic_temperature,
                    false,
                    StepStatus.PENDING
                ), StepItem(
                    BasicHealthTestsType.SPO2.stepNumber, BasicHealthTestsType.SPO2.stepName, R.drawable.ic_pulse, false, StepStatus.PENDING
                ), StepItem(
                    BasicHealthTestsType.WEIGHT.stepNumber, BasicHealthTestsType.WEIGHT.stepName, R.drawable.ic_body_weight, false, StepStatus.PENDING
                ), StepItem(
                    BasicHealthTestsType.VISION.stepNumber, BasicHealthTestsType.VISION.stepName, R.drawable.ic_vision, false, StepStatus.PENDING
                ), StepItem(
                    BasicHealthTestsType.BLOOD_PRESSURE.stepNumber,
                    BasicHealthTestsType.BLOOD_PRESSURE.stepName,
                    R.drawable.ic_blood_pressure,
                    false,
                    StepStatus.PENDING
                ), StepItem(
                    BasicHealthTestsType.BLOOD_SUGAR.stepNumber,
                    BasicHealthTestsType.BLOOD_SUGAR.stepName,
                    R.drawable.ic_height,
                    false,
                    StepStatus.PENDING
                ), StepItem(
                    BasicHealthTestsType.HEMOGLOBIN.stepNumber,
                    BasicHealthTestsType.HEMOGLOBIN.stepName,
                    R.drawable.ic_hemoglobin,
                    false,
                    StepStatus.PENDING
                )
            )
            stepsViewModel.initializeSteps(healthSteps)
        }

        // Observe steps changes
        stepsViewModel.steps.observe(viewLifecycleOwner) { steps ->
            binding.stepsLayout.setSteps(steps)
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