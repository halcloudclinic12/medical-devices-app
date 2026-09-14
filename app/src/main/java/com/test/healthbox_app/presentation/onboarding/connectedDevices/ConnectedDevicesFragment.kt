package com.test.healthbox_app.presentation.onboarding.connectedDevices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.test.healthbox_app.BleConnectionViewModel
import com.test.healthbox_app.MainActivity
import com.test.healthbox_app.R
import com.test.healthbox_app.base.BaseFragment
import com.test.healthbox_app.bluetooth.DeviceType
import com.test.healthbox_app.databinding.ConnectedDevicesFragmentBinding
import com.test.healthbox_app.domain.model.BleDevice
import com.test.healthbox_app.presentation.util.CustomSnackBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConnectedDevicesFragment() : BaseFragment() {

    private lateinit var binding: ConnectedDevicesFragmentBinding

    private lateinit var bleConnectionViewModel: BleConnectionViewModel

    private var selectedDeviceType: DeviceType = DeviceType.HEIGHT

    private var selectedDevice: BleDevice? = null

    // Directly expose all enum values as a List
    val deviceTypesList: List<DeviceType> = DeviceType.entries

    private lateinit var connectedDevicesListAdapter: ConnectedDevicesListAdapter

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
                    println("on Back Press clicked on Connected Devices Screen  ::  ")

                    findNavController().navigate(R.id.back_button_action_connected_devices_screen)
                }
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = ConnectedDevicesFragmentBinding.inflate(inflater)

        // Get ViewModel from activity
        bleConnectionViewModel = (activity as MainActivity).bleViewModel

        binding.rvDevicesList.layoutManager = LinearLayoutManager(requireContext())

        getDevice()

        setAvailableDevicesList()

        nextTestCall()

        return binding.root
    }

    private fun setAvailableDevicesList() {
        connectedDevicesListAdapter = ConnectedDevicesListAdapter(
            devicesTypesList = deviceTypesList, selectedDeviceTypes = selectedDeviceType, onItemClick = { devicetype ->
                selectedDeviceType = devicetype

                setAvailableDevicesList()

                getDevice()
            })
        binding.rvDevicesList.adapter = connectedDevicesListAdapter
    }

    fun getDevice() {
        selectedDevice = bleConnectionViewModel.getDeviceByType(selectedDeviceType)

        println("selected device available :: ${selectedDevice}")

        if (selectedDevice == null) {
            binding.tvNoDeviceFound.visibility = View.VISIBLE
            binding.layoutDeviceDetails.visibility = View.GONE

        } else {
            binding.tvNoDeviceFound.visibility = View.GONE
            binding.layoutDeviceDetails.visibility = View.VISIBLE

        }


        selectedDevice?.let {
            var deviceTypeName = ""

            when (selectedDeviceType) {
                DeviceType.HEIGHT -> {
                    deviceTypeName = "Height"
                }

                DeviceType.THERMOMETER -> {
                    deviceTypeName = "Thermometer"
                }

                DeviceType.PULSE -> {
                    deviceTypeName = "Pulse"
                }

                DeviceType.WEIGHING_SCALE -> {
                    deviceTypeName = "Weighing Scale"
                }

                DeviceType.BLOOD_PRESSURE_MONITOR -> {
                    deviceTypeName = "Blood Pressure Monitor"
                }

                DeviceType.HB_CHECK -> {
                    deviceTypeName = "HB Check"
                }

                DeviceType.GLUCOSE_METER -> {
                    deviceTypeName = "Glucose Meter"
                }

                DeviceType.HBA1C_METER -> {
                    deviceTypeName = "HbA1c Meter"
                }

                DeviceType.BT_PRINTER -> {
                    deviceTypeName = "Printer"
                }
            }

            binding.tvDeviceTypeName.text = deviceTypeName

            binding.tvDeviceName.text = it.name
            binding.tvDeviceAddress.text = it.address
        }
    }

    private fun nextTestCall() {

        binding.ivBack.setOnClickListener {
            findNavController().navigate(R.id.back_button_action_connected_devices_screen)
        }

        binding.buttonDisconnectDeviceLayout.setOnClickListener {

            bleConnectionViewModel.removeDevice(selectedDeviceType)

            CustomSnackBar.make(
                binding.root, "Device removed successfully.", Snackbar.LENGTH_SHORT, CustomSnackBar.Companion.SnackBarType.ERROR
            ).show()


            getDevice()

        }
    }
}