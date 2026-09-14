package com.test.healthbox_app.presentation.reports

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.test.healthbox_app.BleConnectionViewModel
import com.test.healthbox_app.R
import com.test.healthbox_app.base.BaseFragment
import com.test.healthbox_app.bluetooth.DeviceType
import com.test.healthbox_app.data.model.BodyCheckupPref
import com.test.healthbox_app.data.model.PatientPref
import com.test.healthbox_app.data.model.ReportTestType
import com.test.healthbox_app.data.model.mapper.toParametersList
import com.test.healthbox_app.data.model.response.BasicTestData
import com.test.healthbox_app.databinding.ReportsFragmentBinding
import com.test.healthbox_app.domain.model.ApiResponse
import com.test.healthbox_app.domain.model.BleDevice
import com.test.healthbox_app.domain.model.ConnectionState
import com.test.healthbox_app.domain.model.PrintState
import com.test.healthbox_app.domain.model.ScanState
import com.test.healthbox_app.presentation.dialog.DeviceListDialog
import com.test.healthbox_app.presentation.tests.results.ParametersResultsListAdapter
import com.test.healthbox_app.presentation.util.CustomSnackBar
import com.test.healthbox_app.presentation.util.DatePickerUtil
import com.test.healthbox_app.presentation.util.PdfOpener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class ReportsFragment() : BaseFragment() {

    private lateinit var binding: ReportsFragmentBinding

    private val bleConnectionViewModel: BleConnectionViewModel by activityViewModels()

    private lateinit var reportsViewModel: ReportsViewModel

    private lateinit var deviceListDialog: DeviceListDialog

    private var selectedPrinterDevice: BleDevice? = null

    private var printReportText: String = ""

    private var reportsList: List<BasicTestData> = arrayListOf()

    private var selectedReport: BasicTestData? = BasicTestData()

    private lateinit var datesAdapter: ReportsDatesListAdapter
    private lateinit var parametersResultsListAdapter: ParametersResultsListAdapter

    private var isPrinting = false

    var selectedReportType = ReportTestType.typesList().find { it.title == "Basic Health" }?.testType

    override fun checkConnectivity() {
    }

    override val isConnected: Unit = Unit

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle Back Press
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // 👇 Your custom back press logic here
                    println("on Back Press clicked on Reports Screen  ::  ")

                    findNavController().navigate(R.id.back_button_action_reports_screen)
                }
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = ReportsFragmentBinding.inflate(inflater)

        // Get ViewModel from activity
//        bleConnectionViewModel = (activity as MainActivity).bleViewModel

        reportsViewModel = ViewModelProvider(this)[ReportsViewModel::class.java]

        // Bind user info view with ViewModel
        binding.userInfoView.loadPatientFromPref()

        mActivity?.unRegisterOnBackPress()

        binding.rvDatesList.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        binding.rvReportsList.layoutManager = LinearLayoutManager(context)

        setAndObserveDeviceAvailability()

        setupDialog()

        observeScanState()

        observeConnectionState()

        setReportsTypesList()

        showDialog()

        reportsViewModel.getBasicTest()

        observeReportsData()

        observePrintStatus()
//        printReportText = getPrintText()

        nextTestCall()

        return binding.root
    }

    private fun setAndObserveDeviceAvailability() {
        bleConnectionViewModel.setDeviceType(DeviceType.BT_PRINTER)

        bleConnectionViewModel.getDevice()

        if (bleConnectionViewModel.selectedDevice.value == null) {
            binding.buttonConnectPrinterLayout.visibility = View.VISIBLE
            binding.buttonPrintLayout.visibility = View.GONE
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

    fun observeReportsData() {

        viewLifecycleOwner.lifecycleScope.launch {
            reportsViewModel.getBasicTestState.collect { state ->
                when (state) {
                    is ApiResponse.ApiLoading -> {
                        println("Get BasicTestState Logs  :  Loading")
                        // show loading
                    }

                    is ApiResponse.ApiSuccess -> {
                        val apiData = state.data
                        println("Get BasicTestState Logs Success:: ${Gson().toJson(apiData.data)}")

                        if (apiData.data != null) {

                            reportsList = apiData.data.records

                            reportsList?.let {
                                selectedReport = it[0]
                                it[0].isSelected = true
                            }

                            setDatesList()
                            setResultsList()
                            delay(100)
                            hideDialog()

                        } else {
                            hideDialog()
                            CustomSnackBar.make(
                                binding.root, "Failed to get data.", Snackbar.LENGTH_SHORT, CustomSnackBar.Companion.SnackBarType.ERROR
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
        }
    }

    private fun setReportsTypesList() {
        viewLifecycleOwner.lifecycleScope.launch {
            reportsViewModel.reportTypes.collect { state ->

                binding.rvTestTypeList.layoutManager = LinearLayoutManager(context)

                binding.rvTestTypeList.adapter = state.testTypesList.let {
                    ReportsTypesListAdapter(
                        reportsTypesList = it, context = context, onItemClick = { selectedReportTestType ->
                            println("\nselectedReportTestType Logs print :: ${selectedReportTestType}")
//                            selectedReportType = selectedReportTestType.testType
                        })
                }
            }
        }
    }

    private fun setDatesList() {

        selectedReport?.let {
            datesAdapter = ReportsDatesListAdapter(
                reportsResultList = reportsList, selectedReport = it, onItemClick = { report ->

                    println(" selected Report logs :: ${Gson().toJson(selectedReport)}")
                    println(" selected Report logs :: ${Gson().toJson(report)}")

                    // ✅ 1. Update selected report
                    selectedReport = report

                    // ✅ 2. Notify both adapters
//                    datesAdapter.notifyDataSetChanged()
//                    parametersResultsListAdapter.notifyDataSetChanged()

                    setDatesList()
                    setResultsList()

                })
            binding.rvDatesList.adapter = datesAdapter
        }
    }

    private fun setResultsList() {

        selectedReport?.let {

            println("\nBodyCheckupPref Logs print :: ${Gson().toJson(selectedReport)}")

            val parametersList = selectedReport!!.toParametersList()

            /* binding.rvReportsList.adapter = parametersList.let {
                 ParametersResultsListAdapter(parametersResultList = it, context = context)
             }*/

            parametersList.let {
                parametersResultsListAdapter = ParametersResultsListAdapter(parametersResultList = it, context = context)
            }

            binding.rvReportsList.adapter = parametersResultsListAdapter
        }
    }

    private fun nextTestCall() {

        binding.ivBack.setOnClickListener {
//            back_button_action
            findNavController().navigate(R.id.back_button_action_reports_screen)
        }

        binding.buttonPrintLayout.setOnClickListener {

            if (!isPrinting) {
                ensureBluetoothEnabled(binding.root) {
                    bleConnectionViewModel.printText(getPrintText())

                    binding.lottieAnimationView.playAnimation()
                    binding.lottieAnimationView.visibility = View.VISIBLE
                }

            } else {

                CustomSnackBar.make(
                    binding.root, "Printing in progress.", Snackbar.LENGTH_SHORT, CustomSnackBar.Companion.SnackBarType.ERROR
                ).show()
            }

        }

        binding.buttonViewReport.setOnClickListener {
            println("selectedReportType Logs on click :: ${selectedReport?.Id} :: $selectedReportType")
            val url = PdfOpener.buildUrl(testId = selectedReport?.Id.toString(), testType = selectedReportType)
            PdfOpener.openUrl(context = requireContext(), url = url)

            /*CustomSnackBar.make(
                binding.root, "Report not available.", Snackbar.LENGTH_SHORT, CustomSnackBar.Companion.SnackBarType.ERROR
            ).show()*/
        }

        binding.buttonConnectPrinterLayout.setOnClickListener {

            binding.lottieAnimationViewConnectPrinter.playAnimation()
            binding.lottieAnimationViewConnectPrinter.visibility = View.VISIBLE

            if (bleConnectionViewModel.selectedDevice.value == null) {
                bleConnectionViewModel.startBTDeviceScanning()
            } else {
                selectedPrinterDevice?.let { printerDevice ->
                    bleConnectionViewModel.connectBTDevice(bleDevice = printerDevice)
                }
            }
        }


    }

    private fun observePrintStatus() {

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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

    private fun observeConnectionState() {
        // Collect the connection state flow
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                bleConnectionViewModel.btConnectionState.collect { connectionStateMap ->
                    println("Connection State Printer Device Status Result :: Printer ::  " + connectionStateMap)

                    if (connectionStateMap.isEmpty()) {
                        selectedPrinterDevice?.let { printerDevice ->
                            bleConnectionViewModel.connectBTDevice(bleDevice = printerDevice)
                        }
                    }

                    connectionStateMap.forEach { (deviceType, state) ->
                        if (deviceType == DeviceType.BT_PRINTER) {
                            when (state) {

                                is ConnectionState.Connected -> {
                                    Log.e("scanStatePrinter :  conn", "  :  ${ScanState.Connected}")

                                    //Close BLE device dialog
                                    deviceListDialog.dismissDialog()

                                    //Close loader after device connected
                                    hideDialog()

                                    // Save the connected device in shared pref
                                    selectedPrinterDevice?.let {
                                        bleConnectionViewModel.saveDevice(it)
                                    }

                                    withContext(Dispatchers.Main) {
                                        binding.buttonPrintLayout.visibility = View.VISIBLE
                                        binding.buttonConnectPrinterLayout.visibility = View.GONE

                                        binding.lottieAnimationViewConnectPrinter.cancelAnimation()
                                        binding.lottieAnimationViewConnectPrinter.visibility = View.GONE
                                    }
                                }

                                is ConnectionState.Connecting -> {

                                }

                                is ConnectionState.Paired -> {
                                    selectedPrinterDevice?.let { printerDevice ->
                                        bleConnectionViewModel.connectBTDevice(bleDevice = printerDevice)
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

                                        binding.buttonPrintLayout.visibility = View.GONE
                                        binding.buttonConnectPrinterLayout.visibility = View.VISIBLE

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

        val dateOfBirthTimeStamp = DatePickerUtil.isoStringToTimestamp(selectedReport?.createdAt)

        val createdDate = DatePickerUtil.formatDate(dateOfBirthTimeStamp, pattern = DatePickerUtil.prettyFormat)

        // ----- Header -----
        sb.appendLine("     Health Report")
        sb.appendLine()
        sb.appendLine("Name:${PatientPref.patient?.name ?: "-"}")
        sb.appendLine("Age :${BodyCheckupPref.age ?: "-"} Yr   Gender:${PatientPref.patient?.gender ?: "-"}")
        sb.appendLine("${createdDate}  ")
        sb.appendLine("------------------------")

        // ----- Dynamically build parameters -----
        selectedReport!!.toParametersList().filter { !it.value.isNullOrBlank() } // ✅ Only print non-null & non-empty
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