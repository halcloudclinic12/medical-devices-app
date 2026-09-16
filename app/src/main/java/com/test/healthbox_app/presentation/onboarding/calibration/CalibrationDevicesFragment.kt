package com.test.healthbox_app.presentation.onboarding.calibration

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.test.healthbox_app.base.BaseFragment
import com.test.healthbox_app.data.data_source.CalibrationOperators
import com.test.healthbox_app.data.data_source.CalibrationType
import com.test.healthbox_app.databinding.CalibrationFragmentBinding
import com.test.healthbox_app.domain.model.StringValues
import com.test.healthbox_app.presentation.dialog.ItemListDialog
import com.test.healthbox_app.presentation.util.CustomSnackBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CalibrationDevicesFragment() : BaseFragment() {

    private lateinit var binding: CalibrationFragmentBinding

    private lateinit var viewModel: CalibrationDeviceViewModel

    private lateinit var deviceTypesDialog: ItemListDialog
    private lateinit var calibrationOperatorTypesDialog: ItemListDialog


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
                    println("on Back Press clicked on Calibration Devices Screen  ::  ")

                    findNavController().popBackStack()
                }
            })


        viewModel.formError.observe(viewLifecycleOwner) { it ->

            hideDialog()

            CustomSnackBar.make(
                binding.root, it.toString(), Snackbar.LENGTH_SHORT, CustomSnackBar.Companion.SnackBarType.WARNING
            ).show()
        }

        viewModel.calibrationSave.observe(viewLifecycleOwner) { it ->

            hideDialog()

            if (it) {

                CustomSnackBar.make(
                    binding.root,
                    "Calibration data saved for ${viewModel.selectedCalibrationType.value?.name}",
                    Snackbar.LENGTH_SHORT,
                    CustomSnackBar.Companion.SnackBarType.SUCCESS
                ).show()

                viewModel.clearSelectedData()
            }
        }

        viewModel.calibrationState.observe(viewLifecycleOwner) { calibration ->
            calibration?.let { (operator, value) ->

                println("Calibration Operator Logs :: $operator")
                println("Calibration Value Logs :: $value")

                viewModel.selectedCalibrationType.value = viewModel.selectedCalibrationType.value
                viewModel.selectedCalibrationOperators.value = operator
                viewModel.calibrationValue.value = value.toString()

                binding.deviceTypeInput.setText(viewModel.selectedCalibrationType.value?.name)
                binding.operationTypeInput.setText(viewModel.selectedCalibrationOperators.value?.name)

                binding.calibrationValueInput.setText("$value")
            } ?: run {
                Log.d("Calibration", "No calibration found")
            }
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = CalibrationFragmentBinding.inflate(inflater)

        viewModel = ViewModelProvider(this)[CalibrationDeviceViewModel::class.java]

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        setupDeviceTypeDialog()
        setupOperatorTypeDialog()
        nextTestCall()

        return binding.root
    }


    private fun nextTestCall() {

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.buttonSubmit.setOnClickListener {
            viewModel.saveCalibration()
        }


        binding.deviceTypeInput.setOnClickListener {

            var genderList: List<StringValues> = mutableListOf(
                StringValues(value = CalibrationType.HEIGHT.name, isSelected = false),
                StringValues(value = CalibrationType.TEMPERATURE.name, isSelected = false)
            )
            deviceTypesDialog.showDialog("Select Device Type", genderList)
        }

        binding.operationTypeInput.setOnClickListener {

            var genderList: List<StringValues> = mutableListOf(
                StringValues(value = CalibrationOperators.PLUS.name, isSelected = false),
                StringValues(value = CalibrationOperators.MINUS.name, isSelected = false)
            )
            calibrationOperatorTypesDialog.showDialog("Select Operator", genderList)
        }
    }

    private fun setupDeviceTypeDialog() {
        deviceTypesDialog = mActivity?.let { ItemListDialog(it) }!!

        deviceTypesDialog.setOnItemSelectedListener(object : ItemListDialog.OnItemClickListener {
            override fun onItemSelect(deviceType: StringValues?) {

                Log.e("onClickDevice", "  : Device Type :  $deviceType")

                deviceType?.let {
                    viewModel.loadCalibration(calibrationType = CalibrationType.valueOf("${it.value}"))

                    binding.deviceTypeInput.setText(it.value)
                    viewModel.selectedCalibrationType.value = CalibrationType.valueOf("${it.value}")
                }

            }
        })
    }

    private fun setupOperatorTypeDialog() {
        calibrationOperatorTypesDialog = mActivity?.let { ItemListDialog(it) }!!

        calibrationOperatorTypesDialog.setOnItemSelectedListener(object : ItemListDialog.OnItemClickListener {
            override fun onItemSelect(operatorType: StringValues?) {

                Log.e("onClickDevice", "  : Operator Type :  $operatorType")

                operatorType?.let {

                    binding.operationTypeInput.setText(it.value)

                    viewModel.selectedCalibrationOperators.value = CalibrationOperators.valueOf("${it.value}")
                }
            }
        })
    }

}