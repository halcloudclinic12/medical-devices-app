package com.test.healthbox_app.presentation.tests.temperature

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
import com.test.healthbox_app.databinding.TemperatureFragmentBinding
import com.test.healthbox_app.domain.model.BleDevice
import com.test.healthbox_app.domain.model.ConnectionState
import com.test.healthbox_app.domain.model.ScanState
import com.test.healthbox_app.domain.model.StepStatus
import com.test.healthbox_app.domain.model.TemperatureMeasurement
import com.test.healthbox_app.presentation.dialog.DeviceListDialog
import com.test.healthbox_app.presentation.util.BasicHealthTestsType
import com.test.healthbox_app.presentation.util.Constants
import com.test.healthbox_app.presentation.util.CustomSnackBar
import com.test.healthbox_app.presentation.view.StepsViewModel
import com.test.healthbox_app.presentation.view.deviceStatus.DeviceStatusViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TemperatureFragment() : BaseFragment() {

    private lateinit var binding: TemperatureFragmentBinding

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
                        binding.root,
                        "Are you sure to go back, It will clear all the test results?",
                        "Yes",
                        Color.WHITE,
                        action = {
                            findNavController().navigate(R.id.close_button_action_temperature_screen)
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
        binding = TemperatureFragmentBinding.inflate(inflater)

        // Get ViewModel from activity
        bleConnectionViewModel = (activity as MainActivity).bleViewModel

        binding.viewModel = bleConnectionViewModel

        // Bind user info view with ViewModel
        binding.userInfoView.loadPatientFromPref()

        mActivity?.unRegisterOnBackPress()

        initializeGroupASteps()

        initializeDeviceStatus()

        setAndObserveDeviceAvailability()

        setupDialog()

        observeScanState()

        observeConnectionState()

        observeDataState()

        nextTestCall()

        if (Constants.LOGS_ENABLE) {
            binding.editTemperature.setText("98")
        }

        return binding.root
    }

    private fun setAndObserveDeviceAvailability() {
        bleConnectionViewModel.setDeviceType(DeviceType.THERMOMETER)

        // selectedDevice is a single, activity-scoped StateFlow shared by every test
        // screen. A StateFlow replays its current value to a new collector immediately,
        // so without this clear, the collect{} below fires first with whatever device the
        // PREVIOUS screen left behind and connects to it under this device type — then
        // fires again once getDevice() below resolves the real one. That double/wrong
        // connect is what made reconnecting on return visits unreliable.
        bleConnectionViewModel.setSelectedDevice(null)

        bleConnectionViewModel.getDevice()

        if (bleConnectionViewModel.selectedDevice.value == null) {
            binding.deviceStatusLayout.setUpDeviceAvailability(false)
        } else {
            binding.deviceStatusLayout.setUpDeviceAvailability(true)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            bleConnectionViewModel.selectedDevice.collect { device ->

                print("selectedDeviceLogsObs :  $device")

                if (device != null) {
                    binding.deviceStatusLayout.setUpDeviceAvailability(true)

//                    bleConnectionViewModel.connectToDevice(device, bleConnectionViewModel.selectedDeviceType.value!!)
                }
            }
        }
    }

    private fun nextTestCall() {
        binding.buttonNextLayout.setOnClickListener { view ->
            mActivity?.let {

                if (binding.editTemperature.text.toString().isEmpty()) {
                    CustomSnackBar.make(
                        binding.root, "Perform Temperature test first", Snackbar.LENGTH_SHORT, CustomSnackBar.Companion.SnackBarType.WARNING
                    ).show()
                    return@setOnClickListener
                }

                disconnectDevice()

                // Save before advancing the shared step list — see the matching comment in
                // HeightFragment.nextTestCall() for why the order matters here.
                saveTemperatureData()

                stepsViewModel.goToNextStep()

                println("Body checkup response : Temp : -> ${BodyCheckupPref.temperature} ")

                it.navController?.navigate(R.id.next_button_pulse_checkup_action)
            }
        }
    }

    fun saveTemperatureData() {
        binding.editTemperature.text.toString().let {
            BodyCheckupPref.temperature = binding.editTemperature.text.toString()
        }
    }

    fun disconnectDevice() {
        bleConnectionViewModel.selectedDevice.value?.let {
            bleConnectionViewModel.disconnect(
                bleConnectionViewModel.selectedDevice.value!!, bleConnectionViewModel.selectedDeviceType.value!!
            )
        }

        bleConnectionViewModel.setSelectedDevice(null)

        bleConnectionViewModel.setDeviceType(null)

        // Stop promptly rather than leaving it running until the next screen's
        // bindViewModel() cleans it up (see DeviceStatusLayout.bindViewModel()).
        deviceStatusViewModel.stopBleScan()
    }

    private fun setupDialog() {
        deviceListDialog = mActivity?.let {
            DeviceListDialog(it, onDeviceClose = {
                deviceListDialog.dismissDialog()

                // Closed without picking a device - stop the scan and clear its result
                // instead of leaving it to keep running/sitting in the shared state.
                deviceStatusViewModel.stopBleScan()
            })
        }!!

        deviceListDialog.setOnItemSelectedListener(object : DeviceListDialog.OnItemClickListener {
            override fun onItemSelect(bleDevice: BleDevice?) {
//                binding.buttonScan.visibility = View.GONE

                showDialog()

                bleDevice?.let {
                    //update selected device when selected from the list
                    bleConnectionViewModel.updateSelectedDevice(it)

                    //connect to selected device when selected from the list
                    bleConnectionViewModel.connectToDevice(it, bleConnectionViewModel.selectedDeviceType.value)
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

                    // connectionStateMap is shared/activity-scoped and accumulates one entry
                    // per device type ever visited (HEIGHT, THERMOMETER, PULSE, ...). This used
                    // to iterate every entry: the Disconnected branch checked the CURRENT
                    // SCREEN's selectedDeviceType instead of THIS entry's own deviceType (so a
                    // stale non-THERMOMETER Disconnected entry could flip the UI back to
                    // "Disconnected" right after a real THERMOMETER=Connected had just set it to
                    // Connected), and the Connected branch called getTemperature() once per
                    // Connected entry in the whole map — not just for THERMOMETER — risking
                    // duplicate reads if another device type was also Connected. Only react to
                    // this screen's own entry.
                    val state = connectionStateMap[DeviceType.THERMOMETER] ?: return@collect

                    when (state) {

                        is ConnectionState.Connected -> {
                            Log.e("scanStateTemperature :  conn", "  :  ${ScanState.Connected}")

                            //Close BLE device dialog
                            deviceListDialog.dismissDialog()

                            //Close loader after device connected
                            hideDialog()

                            //To clear the scanned devices list
                            deviceStatusViewModel.updateScannedDevicesList()

                            // Save the connected device in shared pref
                            bleConnectionViewModel.selectedDevice.value?.let {

                                bleConnectionViewModel.saveDevice(it)
                            }

                            bleConnectionViewModel.getTemperature()

                            binding.deviceStatusLayout.setupDeviceStatus(mActivity!!, true)
//                                binding.tvDeviceAvailability.text = "Connected"
//                                binding.tvDeviceAvailability.setTextColor(mActivity?.resources!!.getColor(R.color.green))
                        }

                        is ConnectionState.Connecting -> {

                        }

                        is ConnectionState.Disconnected -> {
//                                binding.tvDeviceAvailability.text = "Disconnected"

                            binding.deviceStatusLayout.setupDeviceStatus(mActivity!!, false)

                            bleConnectionViewModel.selectedDevice.value?.let {
                                /*bleConnectionViewModel.connectToDevice(
                                    bleConnectionViewModel.selectedDevice.value!!,
                                    bleConnectionViewModel.selectedDeviceType.value!!
                                )*/
                            }
                        }

                        is ConnectionState.Error -> {

                        }

                        // BT_PRINTER can reach these states from the Results screen and the
                        // map is shared/activity-scoped, so entries for OTHER device types
                        // keep arriving here forever. TODO() used to crash this collector
                        // permanently the first time that happened, which looked like
                        // "device never reconnects" on every screen after Results.
                        is ConnectionState.Paired -> Unit

                        is ConnectionState.PairedFailed -> Unit
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

                    bleConnectionViewModel.getResponseAsString(DeviceType.THERMOMETER)?.let {
                        Log.e("deviceResponseLogs", "   :   " + bleConnectionViewModel.getResponseAsString(DeviceType.THERMOMETER))

                        val temperatureMeasurement: TemperatureMeasurement =
                            bleConnectionViewModel.getResponseAsString(DeviceType.THERMOMETER)!! as TemperatureMeasurement


                        binding.editTemperature.setText("${temperatureMeasurement.temperatureFahrenheit}")

                        // Real device-reported value, not a computed conversion - the device
                        // already sends both units, so no client-side math to get wrong.
                        binding.tvTemperatureEquivalent.text =
                            "Equivalent to %.1f°C".format(temperatureMeasurement.temperatureCelsius)

                    }


                    /*if (bleConnectionViewModel.getResponseAsString(DeviceType.THERMOMETER) == "F") {
                        binding.editTemperature.setText("Err")
                    } else {

                        val data = bleConnectionViewModel.getResponseAsString(DeviceType.THERMOMETER)

                        data?.let {
                            val a = java.lang.Byte.toUnsignedInt(data[2].code.toByte())
                            val b = java.lang.Byte.toUnsignedInt(data[3].code.toByte())
                            Log.e("temperatureDataLogs", " : a :$a  : b :$b")

                            val tem = ((a shl 8) + b) * 1.0 / 100

                            Log.e("temperatureDataLogs", " : Centigrade 1 :$tem")

//                            tem = Math.round(tem * 10) * 1.0 / 10
//                            Log.e("temperatureDataLogs", " : Centigrade :$tem")

                            val fahrenheit = tem * 9 / 5 + 32
                            Log.e("temperatureDataLogs", " : Frag_fahrenheit :$fahrenheit    :  :  ${StringUtils.singleDecimalValue(fahrenheit)}")

                            binding.editTemperature.setText(StringUtils.singleDecimalValue(fahrenheit))

                        }
                    }*/
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

                    // Consumed - without this, the non-empty result sits in the shared
                    // StateFlow and replays to the next screen that subscribes, exactly
                    // like the empty case below already guards against.
                    deviceStatusViewModel.updateScannedDevicesList()
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
        // Observe steps changes
        stepsViewModel.steps.observe(viewLifecycleOwner) { steps ->
            binding.stepsLayout.setSteps(steps)
        }

        // ✅ Click listener for navigation
        binding.stepsLayout.setOnStepClickListener { step ->
            println("Step clicked temperature : : ${step}")

            disconnectDevice()
            stepsViewModel.updateStepStatus(stepId = step.id, status = StepStatus.CURRENT)

            when (step.id) {
                BasicHealthTestsType.HEIGHT.stepNumber -> {
                    println("Step 1 clicked ${step}")
                    mActivity?.let {
                        it.navController?.navigate(R.id.temperature_to_height_screen_nav_action)
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