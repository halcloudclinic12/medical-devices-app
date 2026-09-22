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

    // Sanity ceiling for a final hemoglobin reading (raw device value / 10, in g/dL) -
    // generously above any physiologically plausible human reading, so an unmapped status
    // code that happens to be numeric (like "2241" -> 224.1) can't masquerade as a result.
    // See observeDataState()'s `else` branch.
    private val MAX_PLAUSIBLE_HB_GDL = 30.0

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

        // selectedDevice is a single, activity-scoped StateFlow shared by every test
        // screen. A StateFlow replays its current value to a new collector immediately,
        // so without this clear, the collect{} below fires first with whatever device the
        // PREVIOUS screen left behind and calls connectHBDevice() on it — then fires again
        // once getDevice() below resolves the real HB_CHECK device. Each call spins up a
        // brand-new ControlCentre/BluetoothLeService from the vendor SDK without tearing
        // down the previous one, so two GATT clients end up racing to connect to the same
        // meter address — which is exactly what produced the endless
        // "onClientConnectionState() status=133 / Disconnected" loop in logcat.
        bleConnectionViewModel.setSelectedDevice(null)

        bleConnectionViewModel.getDevice()

        if (bleConnectionViewModel.selectedDevice.value == null) {
            binding.deviceStatusLayout.setUpDeviceAvailability(false)
        } else {
            binding.deviceStatusLayout.setUpDeviceAvailability(true)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            bleConnectionViewModel.selectedDevice.collect { device ->
                print("selectedHBDeviceLogsObs : Device ::  $device")

                // selectedDevice is shared across every screen. Even with the clear above,
                // a later screen (e.g. Results, setting BT_PRINTER) can push a new value into
                // this same StateFlow while this collector is still alive, and it would be
                // wrongly treated as "the HB_CHECK device is now available" — spinning up a
                // second, wrong-protocol ControlCentre against that device's address (this is
                // what produced the endless status=133 reconnect loop seen in logcat, where
                // connectHBDevice() was called with the BT_PRINTER's BleDevice). Guard on the
                // device's own deviceType rather than the shared selectedDeviceType field,
                // since that field can be raced the same way.
                if (device != null && device.deviceType == DeviceType.HB_CHECK) {
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

        binding.buttonStartTest.setOnClickListener {
            startHbStartLoader()
            bleConnectionViewModel.startHbTest()
        }
    }

    private var hbStartLoaderTimeoutJob: Job? = null

    /** Spinner shown from the moment Start Test is tapped until the device's first real
     *  response comes back through observeDataState() (stopHbStartLoader()), so the button
     *  reflects "waiting on the device" instead of looking like a dead tap. Guarded by a
     *  failsafe timeout in case the device never responds, so the button can't get stuck
     *  disabled forever. */
    private fun startHbStartLoader() {
        binding.buttonStartTest.isEnabled = false
        binding.buttonStartTest.alpha = 0.6f
        binding.lottieAnimationViewStartTest.visibility = View.VISIBLE
        binding.lottieAnimationViewStartTest.playAnimation()

        hbStartLoaderTimeoutJob?.cancel()
        hbStartLoaderTimeoutJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(8000)
            stopHbStartLoader()
        }
    }

    private fun stopHbStartLoader() {
        hbStartLoaderTimeoutJob?.cancel()
        hbStartLoaderTimeoutJob = null

        binding.lottieAnimationViewStartTest.cancelAnimation()
        binding.lottieAnimationViewStartTest.visibility = View.GONE
        binding.buttonStartTest.isEnabled = true
        binding.buttonStartTest.alpha = 1f
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
                                println("DEBUG-DEVICESAVE HemoglobinTestFragment: connected, about to save device: name=${it.name} address=${it.address} deviceType=${it.deviceType}")
                                Log.e("DEBUG-DEVICESAVE", "HemoglobinTestFragment: connected, about to save device: name=${it.name} address=${it.address} deviceType=${it.deviceType}")
                                bleConnectionViewModel.saveDevice(it)
                            } ?: run {
                                println("DEBUG-DEVICESAVE HemoglobinTestFragment: connectionState=true but selectedDevice.value is NULL - nothing to save")
                                Log.e("DEBUG-DEVICESAVE", "HemoglobinTestFragment: connectionState=true but selectedDevice.value is NULL - nothing to save")
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

                        // Any real response from the device is proof the test actually
                        // started - stop the Start Test spinner here rather than waiting on
                        // a specific status code. Idempotent (no-op once already stopped), so
                        // it's safe to call on every response, not just the first.
                        stopHbStartLoader()

                        // DEBUG-HB: trace the exact status-code sequence and timing coming
                        // off the real device, plus hbCounter's state at each point - to
                        // diagnose why nothing shows during tray-open/calibration and why
                        // the on-device countdown isn't reflected in the UI after the strip
                        // is inserted. Remove once diagnosed.
                        println("DEBUG-HB raw code=$hbValue hbCounter=$hbCounter hbTimerJob active=${hbTimerJob?.isActive}")
                        Log.e("DEBUG-HB", "raw code=$hbValue hbCounter=$hbCounter hbTimerJob active=${hbTimerJob?.isActive}")

                        when (hbValue) {
                            "2222" -> {
                                println("DEBUG-HB branch=2222 (Checking process started) - starting 5s counter")
                                showHbMessage("Checking process started")
                                startHbCheckCounter(seconds = 5)
                            }

                            "2221" -> {
                                println("DEBUG-HB branch=2221 (Please collect blood and insert strip)")
                                showHbMessage("Please collect blood and insert strip")
                            }

                            "2223" -> {
                                println("DEBUG-HB branch=2223 (clear message)")
                                clearHbMessage()
                            }

                            "2227" -> {
                                println("DEBUG-HB branch=2227 (Error 2)")
                                showHbMessage("Error 2 - Please clean and close the tray")
                            }

                            "2235" -> {
                                println("DEBUG-HB branch=2235 (clear message)")
                                clearHbMessage()
                            }

                            "2225" -> {
                                println("DEBUG-HB branch=2225 (Error 1)")
                                showHbMessage("Error 1 - Please clean and close the tray")
                            }

                            "78" -> {
                                println("DEBUG-HB branch=78 (Error 2)")
                                showHbMessage("Error 2 - Please clean and close the tray")
                            }

                            "2224" -> {
                                println("DEBUG-HB branch=2224 (Please wait for Result) - hbCounter before decrement=$hbCounter")
                                showHbMessage("Please wait for Result")
                                decreaseHbCounterByOne()
                            }

                            else -> {
                                println("deviceResponseLogs HB Result value :: $hbValue")
                                println("DEBUG-HB branch=else raw=$hbValue")
                                hbValue?.toDoubleOrNull()?.let { value ->

                                    val res = value / 10

                                    // Confirmed via logcat: code "2241" (not in the mapped
                                    // list above) fell through here and got treated as a
                                    // literal result of 224.1 g/dL - physiologically
                                    // impossible (real hemoglobin readings are roughly
                                    // 0-25 g/dL even at extremes), and the device's own
                                    // screen was showing "0C2", not a finished reading, at
                                    // that same moment. So "2241" is almost certainly another
                                    // in-progress/status code we don't have mapped yet, not a
                                    // result - only accept values inside a plausible range as
                                    // a genuine final reading; anything else is logged instead
                                    // of being written to the result field, so it doesn't
                                    // silently masquerade as real data.
                                    if (res > 0 && res <= MAX_PLAUSIBLE_HB_GDL) {
                                        println("DEBUG-HB branch=else ACCEPTED as final result raw=$hbValue res=$res")
                                        binding.tvMessage.visibility = View.GONE
                                        binding.editHemoglobinValue.setText(res.toString())

                                        bleConnectionViewModel.stopHbTest()
                                    } else {
                                        println("DEBUG-HB branch=else REJECTED - raw=$hbValue res=$res is outside plausible range (0, $MAX_PLAUSIBLE_HB_GDL] - likely an unmapped status code, not a result")
                                        Log.e("DEBUG-HB", "Unmapped/implausible HB code rejected: raw=$hbValue res=$res")
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

        // Deliberately not touching editHemoglobinValue here. "2222" (which calls this
        // function) fires as soon as the device is connected/ready - before the patient has
        // actually inserted blood and the strip - so displaying hbCounter immediately showed
        // "5" prematurely, well before the device's own on-screen countdown had started.
        // The visible countdown should be driven ONLY by decreaseHbCounterByOne(), which
        // fires once per real "2224" response - i.e. only once the device's own countdown is
        // genuinely running - so the result field now stays exactly as it already was
        // (typically empty) until that actually happens.
        hbTimerJob = viewLifecycleOwner.lifecycleScope.launch {
            // Pure safety bound, no UI side effects: if the device never sends enough "2224"
            // responses to bring hbCounter to 0 on its own, this just lets the job end after
            // `seconds` ticks rather than leaving hbCounter in a stale non-zero state forever.
            repeat(seconds) {
                delay(1000)
                println("HB_COUNTER Counter  : : $hbCounter")
                if (hbCounter <= 0) return@launch
            }

            println("HB_COUNTER Countdown finished :: ")
        }
    }

    private fun decreaseHbCounterByOne() {
        // DEBUG-HB: if hbCounter is already 0 here, this is a no-op - meaning "2224" arrived
        // without "2222" ever starting the counter (or it already ran out), so the on-device
        // countdown has nothing to decrement and the UI number won't move. Remove once
        // diagnosed.
        println("DEBUG-HB decreaseHbCounterByOne() called, hbCounter=$hbCounter")
        if (hbCounter > 0) {
            // Display the current value BEFORE decrementing, not after - the first real
            // "2224" tick should show "5" (the genuine start of the on-device countdown),
            // not skip straight to "4". This is still only ever reached from a real "2224"
            // event, never at connect time, so it doesn't reintroduce the premature-"5"
            // bug startHbCheckCounter()'s own comment describes above.
            binding.editHemoglobinValue.setText(hbCounter.toString())

            hbCounter--
            Log.e("HB_COUNTER", "Decreased counter by 1 → $hbCounter")
        } else {
            println("DEBUG-HB decreaseHbCounterByOne() NO-OP - hbCounter already <= 0")
        }
    }

    /** Clears any status message on screen - used for the device's "idle"/no-message codes
     *  ("2223", "2235"), which previously called showHbMessage("") and silently did nothing
     *  (that function's own isNotEmpty() guard skipped it), leaving tv_message stuck showing
     *  whatever it last displayed - or, on the very first such code, stuck in its initial
     *  `visibility="gone"` state with nothing ever shown at all. */
    private fun clearHbMessage() {
        println("DEBUG-HB clearHbMessage() called")
        binding.tvMessage.text = ""
        binding.tvMessage.visibility = View.GONE
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