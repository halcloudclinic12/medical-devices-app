package com.test.healthbox_app.presentation.onboarding.loginPatientNav

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
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
import com.test.healthbox_app.databinding.LoginPatientNavFragmentBinding
import com.test.healthbox_app.di.factory.PermissionHandlerFactory
import com.test.healthbox_app.domain.model.ApiResponse
import com.test.healthbox_app.domain.model.BleDevice
import com.test.healthbox_app.domain.model.ConnectionState
import com.test.healthbox_app.domain.model.ScanState
import com.test.healthbox_app.presentation.dialog.DeviceListDialog
import com.test.healthbox_app.presentation.util.BluetoothPermissionHandler
import com.test.healthbox_app.presentation.util.CustomSnackBar
import com.test.healthbox_app.presentation.util.DatePickerUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class LoginPatientNavFragment() : BaseFragment() {

    private lateinit var binding: LoginPatientNavFragmentBinding

    lateinit var viewModel: LoginPatientNavViewModel

    private lateinit var bleConnectionViewModel: BleConnectionViewModel

    private lateinit var deviceListDialog: DeviceListDialog

    private lateinit var selectedPrinterDevice: BleDevice

    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle

    private lateinit var permissionHandler: BluetoothPermissionHandler

    override val isConnected: Unit = Unit

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = LoginPatientNavFragmentBinding.inflate(inflater)

        viewModel = ViewModelProvider(this)[LoginPatientNavViewModel::class.java]

        // Get ViewModel from activity
        bleConnectionViewModel = (activity as MainActivity).bleViewModel

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        // Create the PermissionHandler and set it in the ViewModel
        val permissionHandler = permissionHandlerFactory.create(this, permissionLauncher)
        viewModel.setPermissionHandler(permissionHandler)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()

        setupDrawer()

        setupNavHeader()

        checkPermissions()

        /*setupDialog()

        observeScanState()

        observeConnectionState()

        printerConnectionState()*/

        viewModel.formError.observe(viewLifecycleOwner) { it ->

            hideDialog()

            CustomSnackBar.make(
                binding.root, it.toString(), Snackbar.LENGTH_SHORT, CustomSnackBar.Companion.SnackBarType.WARNING
            ).show()
        }

        binding.buttonSubmit.setOnClickListener {
            mActivity?.let {
                val url = "https://dev-api.halcloudclinic.com/api/v1/tests/download-pdf?test_id=69d3e15e9b3ed26210da827a&test_type=BASIC"

                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)

//                showDialog()
//                viewModel.loginPatient()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.patientLoginState.collect { state ->

                println("PatientClinicLogs view Data  :  get : $state")

                when (state) {
                    is ApiResponse.ApiLoading -> {
                        Log.d("PatientClinicLogs", "  :  Loading")
                        // show loading
                    }

                    is ApiResponse.ApiSuccess -> {
                        val apiData = state.data
                        Log.d("PatientClinicLogs", "  :  Data : ${apiData.data}")
                        hideDialog()

                        if (apiData.data.success == true) {

                            CoroutineScope(Dispatchers.IO).launch {

                                viewModel.savePatient(apiData.data.patient)

                                apiData.data.patient.let { patient ->
                                    BodyCheckupPref.patient_ID = patient.id.toString()
                                    patient.dateOfBirth?.let { dateOfBirth ->
                                        BodyCheckupPref.age = DatePickerUtil.getAgeFromDob(dateOfBirth.toString()).toString()
                                    }
                                }

                                viewModel.getClinic()?.let { clinicLoginRes ->
                                    clinicLoginRes.data?.let { data ->
                                        data.clinic?.let { clinic ->
                                            BodyCheckupPref.clinic_ID = clinic.Id
                                        }
                                    }
                                }

                                showSnackBar(
                                    binding.root, "Patient logged in successfully.", CustomSnackBar.Companion.SnackBarType.SUCCESS
                                )

                                delay(100)

                                viewModel.resetPatientLoginState()
                            }

                            findNavController().navigate(R.id.register_patient_login_action)

                        } else {
                            showSnackBar(
                                binding.root, apiData.data.message, CustomSnackBar.Companion.SnackBarType.ERROR
                            )
                        }
                    }

                    is ApiResponse.ApiError -> {
                        hideDialog()

                        showSnackBar(
                            binding.root, state.message, CustomSnackBar.Companion.SnackBarType.ERROR
                        )
                    }
                }
            }
        }

        /*viewLifecycleOwner.lifecycleScope.launch {
            viewModel.patientLoginState.collect { state ->

                println("PatientClinicLogs view Data  :  get : ${state}")

                when (state) {
                    is ApiResponse.ApiLoading -> {
                        Log.d("PatientClinicLogs", "  :  Loading : ${state.apiData?.data}")
                        // show loading
                    }

                    is ApiResponse.ApiSuccess -> {
                        Log.d("PatientClinicLogs", "  :  Token : ${state.apiData?.data}")
                        hideDialog()

                        state.apiData?.let { apiData ->
                            if (apiData.data.success == true) {

                                CoroutineScope(Dispatchers.IO).launch {

                                    viewModel.savePatient(apiData.data.patient)

                                    apiData.data.patient.let { it ->

                                        BodyCheckupPref.patient_ID = it.id.toString()
                                        it.dateOfBirth?.let { dateOfBirth ->
                                            BodyCheckupPref.age = DatePickerUtil.getAgeFromDob(dateOfBirth.toString()).toString()
                                        }
                                    }

                                    viewModel.getClinic()?.let { clinicLoginRes ->
                                        clinicLoginRes.data?.let { data ->
                                            data.clinic?.let { clinic ->
                                                BodyCheckupPref.clinic_ID = clinic.Id
                                            }
                                        }
                                    }

                                    apiData.data.message.let {
                                        showSnackBar(binding.root, "Patient logged in successfully.", CustomSnackBar.Companion.SnackBarType.SUCCESS)
                                    }

                                    delay(100)

                                    viewModel.resetPatientLoginState()

                                }
                                findNavController().navigate(R.id.register_patient_login_action)

                            } else {
//                                Toast.makeText(requireContext(), apiData.data?.message, Toast.LENGTH_SHORT).show()
                                apiData.data.message.let {
                                    showSnackBar(binding.root, apiData.data.message, CustomSnackBar.Companion.SnackBarType.ERROR)
                                }
                            }
                        }
                    }

                    is ApiResponse.ApiError -> {
                        hideDialog()

                        state.message?.let {
                            showSnackBar(binding.root, it, CustomSnackBar.Companion.SnackBarType.ERROR)
                        }

//                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }*/

    }

    private fun printerConnectionState() {

        setAndObserveDeviceAvailability()

        // Collect the connection state flow
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                bleConnectionViewModel.connectionState.collect { connectionStateMap ->

                    Log.e("connectionState", "   :   " + connectionStateMap)

                }
            }
        }
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
                        delay(1000)
                        if (connectionStateMap.isEmpty()) {
                            bleConnectionViewModel.connectBTDevice(bleDevice = selectedPrinterDevice)
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
                                selectedPrinterDevice.let {
                                    bleConnectionViewModel.saveDevice(it)
                                }

                            }

                            is ConnectionState.Connecting -> {

                            }

                            is ConnectionState.Paired -> {

                                withContext(Dispatchers.IO) {
                                    delay(1000)
                                    bleConnectionViewModel.connectBTDevice(bleDevice = selectedPrinterDevice)
                                }

                            }

                            is ConnectionState.PairedFailed -> {

                            }

                            is ConnectionState.Disconnected -> {

                                println("Connection State Printer Device Status Dash :: Printer Disconnected ::  " + connectionStateMap)

                                withContext(Dispatchers.Main) {
                                    bleConnectionViewModel.connectBTDevice(bleDevice = selectedPrinterDevice)
                                }
                            }

                            is ConnectionState.Error -> {
                            }

                        }
                    }


                }
            }
        }
    }

    private fun checkPermissions() {

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
    }

    override fun onResume() {
        super.onResume()
        actionBarDrawerToggle.syncState() // Sync on resume
    }

    override fun checkConnectivity() {
    }

    private fun setupToolbar() {

        (requireActivity() as AppCompatActivity).apply {
            setSupportActionBar(binding.toolbar)
            binding.toolbar.setTitle("")

            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                setHomeButtonEnabled(true)
            }
        }
    }


    private fun setupDrawer() {

        actionBarDrawerToggle = ActionBarDrawerToggle(
            activity, binding.drawerLayout, binding.toolbar, R.string.drawer_open, R.string.drawer_close
        )

        // Add toggle to DrawerLayout
        binding.drawerLayout.addDrawerListener(actionBarDrawerToggle)

        actionBarDrawerToggle.syncState()  // This is crucial!

        // Override the back arrow with hamburger
        (requireActivity() as AppCompatActivity).supportActionBar?.setHomeAsUpIndicator(null)

        // Setup the NavigationView
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_connected_device -> {
                    // Handle home navigation
                    findNavController().navigate(R.id.connected_devices_action)
                    true
                }

                R.id.nav_clinic_analytics -> {
                    // Handle profile navigation
                    true
                }

                R.id.nav_clinic_profile -> {
                    // Handle profile navigation
                    true
                }

                R.id.nav_calibration -> {
                    // Handle Calibration Device
                    findNavController().navigate(R.id.calibration_devices_action)

                    true
                }

                R.id.nav_logout -> {
                    // Handle profile navigation
                    viewModel.clearPreferenceData()

                    findNavController().navigate(R.id.clinic_logout_action)
                    true
                }
                // Add more menu items as needed
                else -> false
            }
        }

        // Enable the toggle button in the ActionBar
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
    }

    fun setupNavHeader() {
        val headerView = binding.navigationView.getHeaderView(0)

        val tvClinicName = headerView.findViewById<TextView>(R.id.tv_clinic_name)
        val tvClinicId = headerView.findViewById<TextView>(R.id.tv_clinic_id)

        viewModel.getClinic()?.let { clinicLoginRes ->
            clinicLoginRes.data?.let { data ->
                data.clinic?.let { clinic ->
                    tvClinicName.text = "Welcome to ${clinic.name}"
                    tvClinicName.isSelected = true
                    tvClinicId.text = "Clinic Id: ${clinic.clinicId}"
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        actionBarDrawerToggle.onConfigurationChanged(newConfig)
    }

}