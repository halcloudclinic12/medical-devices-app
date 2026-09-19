package com.test.healthbox_app.presentation.tests.height

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

        setupHeightEquivalent()

        if (Constants.LOGS_ENABLE) {
            binding.editHeight.setText("172")
        }

        return binding.root
    }

    // The cm reading is the real, device-reported value. The ft/in line under it is a
    // plain unit conversion computed from that same value — not a second, independent
    // measurement — so it's recomputed on every change instead of coming from the device.
    private fun setupHeightEquivalent() {
        binding.editHeight.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val cm = s?.toString()?.toFloatOrNull()

                binding.tvHeightEquivalent.text = if (cm != null && cm > 0) {
                    val totalInches = cm / 2.54f
                    val feet = (totalInches / 12).toInt()
                    val inches = totalInches - (feet * 12)
                    "Equivalent to %d ft %.1f inches".format(feet, inches)
                } else {
                    ""
                }
            }
        })
    }

    private fun setAndObserveDeviceAvailability() {
        bleConnectionViewModel.setDeviceType(DeviceType.HEIGHT)

        // selectedDevice is a single, activity-scoped StateFlow shared by every test
        // screen. A StateFlow replays its current value to a new collector immediately,
        // so without this clear, the collect{} below fires first with whatever device the
        // PREVIOUS screen left behind and connects to it under the HEIGHT device type —
        // then fires again once getDevice() below resolves the real one. That double/wrong
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

                // editHeight can also hold "Err" (device reported a failed reading — see
                // observeDataState()) or a stale value left over from before a reconnect.
                // Both used to pass this check since it only tested for empty, and would get
                // written verbatim into BodyCheckupPref.height — which later crashed the
                // Weight screen's height.toInt() and silently dropped its results.
                if (binding.editHeight.text.toString().isEmpty() || binding.editHeight.text.toString().toIntOrNull() == null) {
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

                    // Stop promptly rather than leaving it running until the next screen's
                    // bindViewModel() cleans it up (see DeviceStatusLayout.bindViewModel()).
                    deviceStatusViewModel.stopBleScan()

                    delay(100)

                    // Save before advancing the shared step list — goToNextStep() fires an
                    // immediate LiveData update that the collapsed pill (still on-screen for
                    // this fragment until navigate() below actually completes) reacts to. In
                    // the other order, that pill would briefly count Height as "skipped"
                    // rather than completed, for the instant between these two calls where
                    // the step list already says Temperature is current but
                    // BodyCheckupPref.height hasn't been written yet.
                    saveHeightData()

                    stepsViewModel.goToNextStep()

                    bleConnectionViewModel.setSelectedDevice(null)

                    bleConnectionViewModel.setDeviceType(null)

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

                    // connectionStateMap is shared/activity-scoped and accumulates one entry
                    // per device type ever visited (HEIGHT, THERMOMETER, PULSE, ...). This used
                    // to iterate every entry and, for Disconnected, check the CURRENT SCREEN's
                    // selectedDeviceType instead of THIS entry's own deviceType — so a stale
                    // THERMOMETER=Disconnected (or any other non-HEIGHT) entry from a previous
                    // run would still pass that check while on the Height screen and immediately
                    // flip the UI back to "Disconnected" right after the real HEIGHT=Connected
                    // entry had just set it to "Connected" moments earlier in the same forEach.
                    // Net effect: the device would genuinely connect (proven by getHeight()
                    // successfully sending/receiving) but the status label stayed stuck on
                    // Disconnected. Only react to the entry that's actually this screen's device.
                    val state = connectionStateMap[DeviceType.HEIGHT] ?: return@collect

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

                            binding.deviceStatusLayout.setupDeviceStatus(
                                mActivity!!, true, bleConnectionViewModel.selectedDevice.value?.name
                            )
//                                binding.tvDeviceAvailability.text = "Connected"
//                                binding.tvDeviceAvailability.setTextColor(mActivity?.resources!!.getColor(R.color.green))
                        }

                        is ConnectionState.Connecting -> {

                        }

                        is ConnectionState.Disconnected -> {
//                                binding.tvDeviceAvailability.text = "Disconnected"
                            binding.deviceStatusLayout.setupDeviceStatus(mActivity!!, false)

                            /*bleConnectionViewModel.connectToDevice(
                                bleConnectionViewModel.selectedDevice.value!!, bleConnectionViewModel.selectedDeviceType.value!!
                            )*/
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

                // A connection attempt (ScanState.Connecting) may have failed here - restore
                // whatever the card was really showing before it, rather than guessing
                // "Disconnected" (which would be wrong if this was a re-scan-to-switch
                // attempt on an already-connected device).
                mActivity?.let { binding.deviceStatusLayout.clearConnecting(it) }
            }

            is ScanState.Connecting -> {
                Log.e("scanStateHeight :  in_conn", "  :  ${ScanState.Connected}")

                // The device list dialog dismisses the instant a device row is tapped, but
                // the actual BLE connection can take a couple seconds - previously the card
                // just sat on its old state through this whole gap.
                mActivity?.let { binding.deviceStatusLayout.setConnecting(it) }
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