package com.test.healthbox_app.presentation.dashboard

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
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
import com.test.healthbox_app.data.model.PatientPref
import com.test.healthbox_app.databinding.DashboardFragmentBinding
import com.test.healthbox_app.di.factory.PermissionHandlerFactory
import com.test.healthbox_app.domain.model.BleDevice
import com.test.healthbox_app.domain.model.ConnectionState
import com.test.healthbox_app.domain.model.ScanState
import com.test.healthbox_app.domain.model.StepStatus
import com.test.healthbox_app.presentation.dialog.DeviceListDialog
import com.test.healthbox_app.presentation.util.BluetoothPermissionHandler
import com.test.healthbox_app.presentation.util.CustomSnackBar
import com.test.healthbox_app.presentation.view.StepsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class DashboardFragment() : BaseFragment() {

    private lateinit var viewModel: DashboardViewModel
    private lateinit var bleConnectionViewModel: BleConnectionViewModel

    private lateinit var binding: DashboardFragmentBinding

    private lateinit var permissionHandler: BluetoothPermissionHandler

    private lateinit var deviceListDialog: DeviceListDialog

    private var selectedPrinterDevice: BleDevice? = null

    private val stepsViewModel: StepsViewModel by activityViewModels()

    // Inject the factory
    @Inject
    lateinit var permissionHandlerFactory: PermissionHandlerFactory

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
//            onPermissionsGranted()

            Log.i("dashboardPermissionAll", "  :  permissionGranted  : ")
        } else {
//            onPermissionsDenied()
            Log.i("dashboardPermissionAll", "  :  permissionDenied  : ")
        }
    }

    override fun checkConnectivity() {
    }

    override val isConnected: Unit = Unit


    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle Back Press
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // 👇 Your custom back press logic here
                    println("on Back Press clicked on Dashboard  ::  ")

                    // Leaving Dashboard for the login screen means switching away from
                    // the current patient — clear identity (kept alive across tests
                    // within a session by clearAll()) so the next patient can't inherit
                    // a leftover patient_id/clinic_id/age or see the previous name.
                    BodyCheckupPref.clearPatientIdentity()
                    PatientPref.patient = null

                    findNavController().navigate(R.id.dash_back_button_action)
                }
            })

        viewModel.checkPermissions()

        viewModel.permissionsGranted.observe(viewLifecycleOwner) { granted ->
            if (granted) {
                Log.i("dashboardPermission", "  :  permissionGranted  : ")
//                onPermissionsGranted()
            } else {
                Log.i("dashboardPermission", "  :  permissionDenied  : ")
//                showPermissionsNeeded()
                viewModel.requestPermissions()
            }
        }

        binding.healthCheckupLayout.setOnClickListener {

            //Setting Height Screen as current Step
            stepsViewModel.updateStepStatus(stepId = 0, status = StepStatus.CURRENT)

            mActivity?.navController?.navigate(R.id.start_health_checkup_height_action)
        }

        binding.reportLayout.setOnClickListener {
            mActivity?.navController?.navigate(R.id.dash_to_reports_screen_action)
        }

        binding.hba1cLayout.setOnClickListener {
            mActivity?.navController?.navigate(R.id.dash_to_hba1c_screen_action)
        }

        val patient = PatientPref.patient

        println("patientSavedLogs   ::  ${patient}")

        setAndObserveDeviceAvailability()

        setupDialog()

        observeScanState()

        observeConnectionState()

        binding.buttonSubmit.setOnClickListener { view ->
            bleConnectionViewModel.startBTDeviceScanning()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = DashboardFragmentBinding.inflate(inflater)
        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        // Bind user info view with ViewModel
        binding.userInfoView.loadPatientFromPref()

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        // Create the PermissionHandler and set it in the ViewModel
        val permissionHandler = permissionHandlerFactory.create(this, permissionLauncher)
        viewModel.setPermissionHandler(permissionHandler)

        // Get ViewModel from activity
        bleConnectionViewModel = (activity as MainActivity).bleViewModel

        // Collect the connection state flow
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                bleConnectionViewModel.connectionState.collect { connectionStateMap ->

                    Log.e("connectionState", "   :   " + connectionStateMap)

                }
            }
        }


        /*binding.reportLayout.setOnClickListener { view ->
            bleConnectionViewModel.printText("Test Print \n\n\n\n")
        }*/

        return binding.root
    }

    private fun setAndObserveDeviceAvailability() {
        bleConnectionViewModel.setDeviceType(DeviceType.BT_PRINTER)

        bleConnectionViewModel.getDevice()

        viewLifecycleOwner.lifecycleScope.launch {
            bleConnectionViewModel.selectedDevice.collect { device ->
                print("selectedDeviceLogsObs :  $device")
                if (device != null) {
                    selectedPrinterDevice = device
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

                Log.e("onClickDevice", "  :  $bleDevice")

                CoroutineScope(Dispatchers.IO).launch {
                    bleConnectionViewModel.getBTDeviceConnectionState()
                }

                bleDevice?.let {

                    showDialog()

                    selectedPrinterDevice = bleDevice
                    CoroutineScope(Dispatchers.IO).launch {
                        if (bleDevice.bondState == BluetoothDevice.BOND_BONDED) {
                            bleConnectionViewModel.connectBTDevice(bleDevice)
                        } else {
                            bleConnectionViewModel.pairBTDevice(bleDevice)
                        }
                    }
                }
                //                showDialog()

                /*bleDevice?.let {
                    //update selected device when selected from the list
                    bleConnectionViewModel.updateSelectedDevice(it)

                    //connect to selected device when selected from the list
                    bleConnectionViewModel.connectToDevice(it, bleConnectionViewModel.selectedDeviceType.value)
                }*/
            }
        })
    }

    private fun observeScanState() {

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe the scan state from ViewModel
                bleConnectionViewModel.scanState.collect { state ->

                    println("observeBTScanStateLogs  ${state}")

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

    private fun observeConnectionState() {
        // Collect the connection state flow
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                bleConnectionViewModel.btConnectionState.collect { connectionStateMap ->

                    println("Connection State Printer Device Status Dash :: Printer ::  " + connectionStateMap)

                    withContext(Dispatchers.IO) {
//                        delay(1000)
                        if (connectionStateMap.isEmpty() && selectedPrinterDevice != null) {
                            selectedPrinterDevice?.let { printerDevice ->
                                bleConnectionViewModel.connectBTDevice(bleDevice = printerDevice)
                            }
                        }
                    }
                    connectionStateMap.forEach { (deviceType, state) ->
                        if (deviceType == DeviceType.BT_PRINTER) when (state) {
                            is ConnectionState.Connected -> {
                                withContext(Dispatchers.Main) {
                                    Log.e("scanStatePrinter :  conn", "  :  ${ScanState.Connected}")

                                    //Close BLE device dialog
                                    deviceListDialog.dismissDialog()

                                    //Close loader after device connected
                                    hideDialog()
                                }
                                // Save the connected device in shared pref
                                selectedPrinterDevice?.let {
                                    bleConnectionViewModel.saveDevice(it)
                                }

                            }

                            is ConnectionState.Connecting -> {

                            }

                            is ConnectionState.Paired -> {

                                withContext(Dispatchers.IO) {
//                                    delay(1000)
                                    selectedPrinterDevice?.let { printerDevice ->
                                        bleConnectionViewModel.connectBTDevice(bleDevice = printerDevice)
                                    }
                                }

                            }

                            is ConnectionState.PairedFailed -> {

                            }

                            is ConnectionState.Disconnected -> {

                                println("Connection State Printer Device Status Dash :: Printer Disconnected ::  " + connectionStateMap)

                                withContext(Dispatchers.Main) {
                                    selectedPrinterDevice?.let { printerDevice ->
                                        bleConnectionViewModel.connectBTDevice(bleDevice = printerDevice)
                                    }
                                }
                            }

                            is ConnectionState.Error -> {

                                hideDialog()

                                CustomSnackBar.make(
                                    binding.root, state.message, Snackbar.LENGTH_SHORT, CustomSnackBar.Companion.SnackBarType.ERROR
                                ).show()
                            }

                        }
                    }
                }
            }
        }
    }
}