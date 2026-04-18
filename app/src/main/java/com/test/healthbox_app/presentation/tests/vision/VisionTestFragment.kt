package com.test.healthbox_app.presentation.tests.vision

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
import com.test.healthbox_app.databinding.VisionTestFragmentBinding
import com.test.healthbox_app.domain.model.BleDevice
import com.test.healthbox_app.domain.model.HbCheckMeasurement
import com.test.healthbox_app.domain.model.ScanState
import com.test.healthbox_app.domain.model.StepStatus
import com.test.healthbox_app.presentation.dialog.DeviceListDialog
import com.test.healthbox_app.presentation.util.BasicHealthTestsType
import com.test.healthbox_app.presentation.util.CustomSnackBar
import com.test.healthbox_app.presentation.view.StepsViewModel
import com.test.healthbox_app.presentation.view.VisionsTestAlphabetsView
import com.test.healthbox_app.presentation.view.deviceStatus.DeviceStatusViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VisionTestFragment() : BaseFragment() {

    private lateinit var binding: VisionTestFragmentBinding

    private val stepsViewModel: StepsViewModel by activityViewModels()

    private val deviceStatusViewModel: DeviceStatusViewModel by activityViewModels()

    private lateinit var bleConnectionViewModel: BleConnectionViewModel

    private lateinit var deviceListDialog: DeviceListDialog

    override fun checkConnectivity() {}

    override val isConnected: Unit = Unit

    private val TAG = "VisionTestFragment"

    var eyeLeftVision = ""
    var eyeRightVision = ""

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
                            findNavController().navigate(R.id.close_button_action_vision_screen)
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
        binding = VisionTestFragmentBinding.inflate(inflater)

        // Get ViewModel from activity
        bleConnectionViewModel = (activity as MainActivity).bleViewModel

        binding.viewModel = bleConnectionViewModel

        binding.lifecycleOwner = viewLifecycleOwner

        // Bind user info view with ViewModel
        binding.userInfoView.loadPatientFromPref()

//        binding.lifecycleOwner = this

        initializeGroupASteps()

        initializeDeviceStatus()

        setupDialog()

//        bleConnectionViewModel.startScanning()

//        startWeighingProcess()

//        observeScanState()

//        observeConnectionState()

        observeDataState()

        nextTestCall()

        return binding.root
    }


    private fun nextTestCall() {
        binding.buttonStartVision.setOnClickListener { view ->

            binding.layoutVisionImageStart.visibility = View.GONE
            binding.visionSlidesView.visibility = View.VISIBLE

            binding.visionSlidesView.bind(bleConnectionViewModel)

            binding.visionSlidesView.onSelectionChanged = { side, id, selectedTag ->

                println("onSelection changed Vision ${side}  : selected Tag : ${selectedTag} ")

                when (side) {
                    VisionsTestAlphabetsView.Side.LEFT -> {
                        eyeLeftVision = selectedTag.toString()
                    }

                    VisionsTestAlphabetsView.Side.RIGHT -> {
                        eyeRightVision = selectedTag.toString()
                    }
                }
            }
        }

        binding.buttonNextLayout.setOnClickListener { view ->
            mActivity?.let {

                CoroutineScope(Dispatchers.Main).launch {

                    saveVisionData()

                    delay(200)

                    stepsViewModel.goToNextStep()

                    bleConnectionViewModel.setSelectedDevice(null)

                    bleConnectionViewModel.setDeviceType(null)

                    it.navController?.navigate(R.id.next_button_blood_pressure_action)
                }
            }
        }
    }

    private fun saveVisionData() {
        eyeLeftVision.let {
            eyeRightVision.let {
                BodyCheckupPref.eye_left_vision = eyeLeftVision
                BodyCheckupPref.eye_right_vision = eyeRightVision
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

            println("Step clicked vision : : ${step}")

            stepsViewModel.updateStepStatus(stepId = step.id, status = StepStatus.CURRENT)

            when (step.id) {
                BasicHealthTestsType.HEIGHT.stepNumber -> {
                    println("Step 1 clicked ${step}")

                    mActivity?.let {
                        it.navController?.navigate(R.id.vision_to_height_screen_nav_action)
                    }
                }

                BasicHealthTestsType.TEMPERATURE.stepNumber -> {
                    println("Step 2 clicked ${step}")

                    mActivity?.let {
                        it.navController?.navigate(R.id.vision_to_temperature_screen_nav_action)
                    }
                }

                BasicHealthTestsType.SPO2.stepNumber -> {
                    println("Step 3 clicked ${step}")

                    mActivity?.let {
                        it.navController?.navigate(R.id.vision_to_pulse_screen_nav_action)
                    }
                }

                BasicHealthTestsType.WEIGHT.stepNumber -> {
                    println("Step 4 clicked ${step}")

                    mActivity?.let {
                        it.navController?.navigate(R.id.vision_to_weight_screen_nav_action)
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

        }

    }

}