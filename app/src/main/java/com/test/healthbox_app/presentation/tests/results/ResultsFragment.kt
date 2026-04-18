package com.test.healthbox_app.presentation.tests.results

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.test.healthbox_app.BleConnectionViewModel
import com.test.healthbox_app.R
import com.test.healthbox_app.base.BaseFragment
import com.test.healthbox_app.bluetooth.DeviceType
import com.test.healthbox_app.data.model.BodyCheckupPref
import com.test.healthbox_app.data.model.PatientPref
import com.test.healthbox_app.databinding.ResultsFragmentBinding
import com.test.healthbox_app.domain.model.ApiResponse
import com.test.healthbox_app.domain.model.BleDevice
import com.test.healthbox_app.domain.model.ConnectionState
import com.test.healthbox_app.domain.model.PrintState
import com.test.healthbox_app.domain.model.ScanState
import com.test.healthbox_app.domain.model.StepStatus
import com.test.healthbox_app.presentation.dialog.DeviceListDialog
import com.test.healthbox_app.presentation.util.CustomSnackBar
import com.test.healthbox_app.presentation.util.DatePickerUtil
import com.test.healthbox_app.presentation.view.StepsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class ResultsFragment() : BaseFragment() {

    private lateinit var binding: ResultsFragmentBinding
    private val bleConnectionViewModel: BleConnectionViewModel by activityViewModels()
    private val resultsViewModel: ResultsViewModel by activityViewModels()

    private val stepsViewModel: StepsViewModel by activityViewModels()

    private lateinit var deviceListDialog: DeviceListDialog

    private var selectedPrinterDevice: BleDevice? = null
    private var printReportText: String = ""

    override fun checkConnectivity() {
    }

    override val isConnected: Unit = Unit

    private var isPrinting = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = ResultsFragmentBinding.inflate(inflater)

        // Get ViewModel from activity
//        bleConnectionViewModel = (activity as MainActivity).bleViewModel
//        resultsViewModel = ViewModelProvider(this)[ResultsViewModel::class.java]

        binding.viewModel = bleConnectionViewModel

        // Bind user info view with ViewModel
        binding.userInfoView.loadPatientFromPref()

        mActivity?.unRegisterOnBackPress()

        setAndObserveDeviceAvailability()

        setupDialog()

        observeScanState()

        observeConnectionState()

        setResultsList()

        showDialog()

        resultsViewModel.createBasicTest()

        printReportText = getPrintText()

        observePrintStatus()

        nextTestCall()

        viewLifecycleOwner.lifecycleScope.launch {

            resultsViewModel.createBasicTestState.collect { state ->
                when (state) {
                    is ApiResponse.ApiLoading -> {
                        println("CreateBasicTestState Logs  :  Loading")
                        // show loading
                    }

                    is ApiResponse.ApiSuccess -> {
                        val apiData = state.data
                        println("CreateBasicTestState Logs Success:: ${apiData.data}")
                        hideDialog()

                        if (apiData.data != null) {
                            CustomSnackBar.make(
                                binding.root, "Data saved successfully.", Snackbar.LENGTH_SHORT, CustomSnackBar.Companion.SnackBarType.SUCCESS
                            ).show()
                        } else {
                            CustomSnackBar.make(
                                binding.root, "Failed to save data.", Snackbar.LENGTH_SHORT, CustomSnackBar.Companion.SnackBarType.ERROR
                            ).show()
                        }
                    }

                    is ApiResponse.ApiError -> {
                        hideDialog()

                        CustomSnackBar.make(
                            binding.root, state.message, Snackbar.LENGTH_SHORT, CustomSnackBar.Companion.SnackBarType.ERROR
                        ).show()
                    }
                }
            }

            /*resultsViewModel.createBasicTestState.collect { state ->
                when (state) {
                    is ApiResponse.ApiLoading -> {
                        println("CreateBasicTestState Logs  :  Loading : ${state.apiData?.data}")
                        // show loading
                    }

                    is ApiResponse.ApiSuccess -> {
                        println("CreateBasicTestState Logs Success:: ${state.apiData?.data}")
                        hideDialog()

                        state.apiData?.let { apiData ->
                            if (apiData.data != null) {

                                hideDialog()

                                CustomSnackBar.make(
                                    binding.root, "Data saved successfully.", Snackbar.LENGTH_SHORT, CustomSnackBar.Companion.SnackBarType.SUCCESS
                                ).show()

                            } else {
                                hideDialog()
                                CustomSnackBar.make(
                                    binding.root, "Failed to save data.", Snackbar.LENGTH_SHORT, CustomSnackBar.Companion.SnackBarType.ERROR
                                ).show()

                            }
                        }
                    }

                    is ApiResponse.ApiError -> {
                        hideDialog()

                        state.message?.let {
                            CustomSnackBar.make(
                                binding.root, it, Snackbar.LENGTH_SHORT, CustomSnackBar.Companion.SnackBarType.ERROR
                            ).show()

                        }

                    }
                }
            }*/
        }

        return binding.root
    }

    private fun setAndObserveDeviceAvailability() {
        bleConnectionViewModel.setDeviceType(DeviceType.BT_PRINTER)

        bleConnectionViewModel.getDevice()

        if (bleConnectionViewModel.selectedDevice.value == null) {
            binding.buttonConnectPrinterLayout.visibility = View.VISIBLE
            binding.buttonPrintLayout.visibility = View.INVISIBLE
        } else {
            binding.buttonPrintLayout.visibility = View.VISIBLE
            binding.buttonConnectPrinterLayout.visibility = View.GONE
        }

        viewLifecycleOwner.lifecycleScope.launch {
            bleConnectionViewModel.selectedDevice.collect { device ->
                print("selectedDeviceLogsObs :  $device")
                if (device != null) {
                    selectedPrinterDevice = device
                }
            }
        }
    }

    private fun setResultsList() {

        println("\nBodyCheckupPref Logs print :: ${BodyCheckupPref.toParameterList()}")
        val parametersResultList = BodyCheckupPref.toParameterList()

        binding.rvResults.layoutManager = LinearLayoutManager(context)
        binding.rvResults.adapter = parametersResultList.let {
            ParametersResultsListAdapter(parametersResultList = it, context = context)
        }

    }

    private fun nextTestCall() {

        binding.buttonPrintLayout.setOnClickListener {

            if (!isPrinting) {
                bleConnectionViewModel.printText(printReportText)

                binding.lottieAnimationView.playAnimation()
                binding.lottieAnimationView.visibility = View.VISIBLE
            } else {

                CustomSnackBar.make(
                    binding.root, "Printing in progress.", Snackbar.LENGTH_SHORT, CustomSnackBar.Companion.SnackBarType.ERROR
                ).show()
            }
        }

        binding.buttonViewReport.setOnClickListener {
            CustomSnackBar.make(
                binding.root, "Report not available.", Snackbar.LENGTH_SHORT, CustomSnackBar.Companion.SnackBarType.ERROR
            ).show()
        }

        binding.buttonConnectPrinterLayout.setOnClickListener {
            binding.lottieAnimationViewConnectPrinter.playAnimation()
            binding.lottieAnimationViewConnectPrinter.visibility = View.VISIBLE

            if (bleConnectionViewModel.selectedDevice.value == null) {
                bleConnectionViewModel.startBTDeviceScanning()
            } else {
                selectedPrinterDevice?.let {
                    bleConnectionViewModel.connectBTDevice(bleDevice = it)
                }
            }
        }

        binding.buttonHome.setOnClickListener {

            stepsViewModel.resetSteps()

            BodyCheckupPref.clearAll()

            findNavController().navigate(R.id.home_button_action_results_screen)
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

                CoroutineScope(Dispatchers.IO).launch {
                    bleConnectionViewModel.getBTDeviceConnectionState()
                }

                Log.e("onClickDevice", "  :  $bleDevice")

                bleDevice?.let {

                    showDialog()

                    selectedPrinterDevice = bleDevice

                    if (bleDevice.bondState == BluetoothDevice.BOND_BONDED) {
                        bleConnectionViewModel.connectBTDevice(bleDevice)
                    } else {
                        bleConnectionViewModel.pairBTDevice(bleDevice)
                    }
                }
            }
        })
    }

    private fun observeScanState() {

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe the scan state from ViewModel
                bleConnectionViewModel.scanState.collect { state ->

                    println("observePrinterBTScanStateLogs  ${state}")

                    updateUIForScanState(state)

                }
            }
        }
    }

    private fun updateUIForScanState(state: ScanState) {
        Log.e("updatingUIState", " : " + state.toString())
        when (state) {
            is ScanState.Idle -> {
                // Handle idle state (initial state)
                hideDialog()
            }

            is ScanState.Scanning -> {
                // Handle scanning state (show progress)
                showDialog()
            }

            is ScanState.DevicesFound -> {

                hideDialog()

                // Show dialog with the devices list
                if (state.devices.isNotEmpty()) {
                    deviceListDialog.showDialog(state.devices)
                } else {
                    // Show empty state or message
                    if (bleConnectionViewModel.selectedDevice.value == null) Toast.makeText(requireContext(), "No devices found", Toast.LENGTH_SHORT)
                        .show()
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

    private fun observePrintStatus() {

        viewLifecycleOwner.lifecycleScope.launch {
            bleConnectionViewModel.printState.collect { state ->
                when (state) {
                    is PrintState.Error -> {
                        isPrinting = false

                        binding.lottieAnimationView.cancelAnimation()
                        binding.lottieAnimationView.visibility = View.GONE

                        CustomSnackBar.make(
                            binding.root, state.message, Snackbar.LENGTH_SHORT, CustomSnackBar.Companion.SnackBarType.ERROR
                        ).show()

                        if (state.message == "Device not connected") {
                            binding.buttonPrintLayout.visibility = View.GONE
                            binding.buttonConnectPrinterLayout.visibility = View.VISIBLE
                        }

                    }

                    PrintState.Idle -> {
                        isPrinting = false

                        binding.lottieAnimationView.cancelAnimation()
                        binding.lottieAnimationView.visibility = View.GONE
                    }

                    PrintState.Loading -> {

                    }

                    is PrintState.Success -> {
                        isPrinting = false

                        binding.lottieAnimationView.cancelAnimation()
                        binding.lottieAnimationView.visibility = View.GONE

                        CustomSnackBar.make(
                            binding.root, state.message, Snackbar.LENGTH_SHORT, CustomSnackBar.Companion.SnackBarType.SUCCESS
                        ).show()
                    }

                }
            }
        }
    }

    private fun observeConnectionState() {
        // Collect the connection state flow
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                bleConnectionViewModel.btConnectionState.collect { connectionStateMap ->

                    println("Connection State Printer Device Status Result :: Printer ::  " + connectionStateMap)

                    if (connectionStateMap.isEmpty()) {
                        selectedPrinterDevice?.let {
                            bleConnectionViewModel.connectBTDevice(bleDevice = it)
                        }
                    }

                    connectionStateMap.forEach { (deviceType, state) ->
                        if (deviceType == DeviceType.BT_PRINTER) {
                            when (state) {

                                is ConnectionState.Connected -> {
                                    Log.e("scanStatePrinter :  conn", "  :  ${ScanState.Connected}")


                                    withContext(Dispatchers.Main) {

                                        //Close BLE device dialog
                                        deviceListDialog.dismissDialog()

                                        //Close loader after device connected
                                        hideDialog()

                                        // Save the connected device in shared pref
                                        selectedPrinterDevice?.let {
                                            bleConnectionViewModel.saveDevice(it)
                                        }

                                        binding.buttonPrintLayout.visibility = View.VISIBLE
                                        binding.buttonConnectPrinterLayout.visibility = View.GONE

                                        binding.lottieAnimationViewConnectPrinter.cancelAnimation()
                                        binding.lottieAnimationViewConnectPrinter.visibility = View.GONE
                                    }
                                }

                                is ConnectionState.Connecting -> {

                                }

                                is ConnectionState.Paired -> {

                                    selectedPrinterDevice?.let {
                                        bleConnectionViewModel.connectBTDevice(bleDevice = it)
                                    }

                                }

                                is ConnectionState.PairedFailed -> {

                                }

                                is ConnectionState.Disconnected -> {
                                    //Close loader after device connected
                                    hideDialog()

                                    selectedPrinterDevice?.let { printerDevice ->
                                        bleConnectionViewModel.connectBTDevice(bleDevice = printerDevice)
                                    }
                                }

                                is ConnectionState.Error -> {
                                    withContext(Dispatchers.Main) {
                                        //Close loader after device connected
                                        hideDialog()

                                        CustomSnackBar.make(
                                            binding.root, state.message, Snackbar.LENGTH_SHORT, CustomSnackBar.Companion.SnackBarType.ERROR
                                        ).show()

                                        binding.lottieAnimationViewConnectPrinter.cancelAnimation()
                                        binding.lottieAnimationViewConnectPrinter.visibility = View.GONE
                                    }
                                }


                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("NewApi")
    private fun getPrintText(): String {
        val sb = StringBuilder()

        // ----- Header -----
        sb.appendLine("     Health Report")
        sb.appendLine()
        sb.appendLine("Name:${PatientPref.patient?.name ?: "-"}")
        sb.appendLine("Age :${BodyCheckupPref.age ?: "-"} Yr   Gender:${PatientPref.patient?.gender ?: "-"}")
        sb.appendLine("${DatePickerUtil.getCurrentDateTimeFormatted()}  ")
        sb.appendLine("------------------------")

        // ----- Dynamically build parameters -----
        BodyCheckupPref.toParameterList().filter { !it.value.isNullOrBlank() } // ✅ Only print non-null & non-empty
            .forEach { param ->
                sb.appendLine("${param.parameterName}: ${param.value}")

                // Normal range line — only if present
                if (!param.range.isNullOrBlank()) {
                    sb.appendLine("[Normal Range]: ${param.range}")
                }

                // Result line — only if present
                if (!param.result.isNullOrBlank()) {
                    sb.appendLine("Result: ${param.result}")
                }

                sb.appendLine() // blank line between parameters
            }

        // ----- Footer -----
        sb.appendLine("------------------------")
        sb.appendLine("       Thank You")
        sb.appendLine("Above results are indicative figure, don't follow without consulting a doctor")
//        sb.appendLine(" figure, don't follow without consulting a doctor")
        sb.appendLine("\n\n\n\n\n")

        return sb.toString()
    }


}