package com.test.healthbox_app.presentation.onboarding.registerPatient

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.test.healthbox_app.R
import com.test.healthbox_app.base.BaseFragment
import com.test.healthbox_app.data.model.BodyCheckupPref
import com.test.healthbox_app.data.model.PatientPref
import com.test.healthbox_app.databinding.RegisterPatientFragmentBinding
import com.test.healthbox_app.domain.model.ApiResponse
import com.test.healthbox_app.domain.model.StringValues
import com.test.healthbox_app.presentation.dialog.ItemListDialog
import com.test.healthbox_app.presentation.util.CustomSnackBar
import com.test.healthbox_app.presentation.util.DatePickerUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterPatientFragment() : BaseFragment() {

    private lateinit var binding: RegisterPatientFragmentBinding

    lateinit var viewModel: RegisterPatientViewModel

    private lateinit var genderListDialog: ItemListDialog

    private lateinit var bloodGroupListDialog: ItemListDialog

    override val isConnected: Unit = Unit

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = RegisterPatientFragmentBinding.inflate(inflater)

        viewModel = ViewModelProvider(this)[RegisterPatientViewModel::class.java]

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        return binding.root
    }

    @SuppressLint("NewApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.genderInputLayout.isHintAnimationEnabled = false

        // Handle Back Press
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // 👇 Your custom back press logic here
                    println("on Back Press clicked on Dashboard  ::  ")

                    findNavController().navigate(R.id.register_patient_back_button_action)
                }
            })

        setupGenderDialog()

        setupBloodGroupDialog()

        viewModel.patient.value?.dateOfBirth?.let {
            val dateOfBirthTimeStamp = DatePickerUtil.isoStringToTimestamp(viewModel.patient.value?.dateOfBirth!!)
            binding.dateOfBirthInput.setText(DatePickerUtil.formatDate(dateOfBirthTimeStamp))
        }
        viewModel.formError.observe(viewLifecycleOwner) { it ->

            hideDialog()

            CustomSnackBar.make(
                binding.root, it.toString(), Snackbar.LENGTH_SHORT, CustomSnackBar.Companion.SnackBarType.WARNING
            ).show()
        }

        /*lifecycleScope.launchWhenStarted {
            viewModel.patientUpdateResponse.collect { state ->

                println("Patient Login Response :: $state")

                when (state) {
                    is ApiResponse.ApiLoading -> {
//                        showDialog()
                    }

                    is ApiResponse.ApiSuccess -> {
                        hideDialog()

                        state.apiData?.let { apiData ->

                            if (apiData.data.success == true) {

                                CoroutineScope(Dispatchers.IO).launch {
                                    PatientPref.patient = apiData.data.patient

                                    BodyCheckupPref.age = DatePickerUtil.getAgeFromDob(apiData.data.patient.dateOfBirth.toString()).toString()

                                    delay(100)

                                    viewModel.resetPatientUpdateState()
                                }

                                findNavController().navigate(R.id.go_to_dashboard_register_action)

                                apiData.data.message.let {
                                    showSnackBar(binding.root, "Patient saved successfully.", CustomSnackBar.Companion.SnackBarType.SUCCESS)
                                }

                            } else {
                                if (apiData.data.patient != null) {
                                    PatientPref.patient = apiData.data.patient

                                    findNavController().navigate(R.id.go_to_dashboard_register_action)

                                    apiData.data.message.let {
                                        showSnackBar(binding.root, "Patient saved successfully.", CustomSnackBar.Companion.SnackBarType.SUCCESS)
                                    }
                                } else {
                                    apiData.data.message.let {
                                        showSnackBar(binding.root, apiData.data.message, CustomSnackBar.Companion.SnackBarType.ERROR)
                                    }
                                }
                            }
                        }
                    }

                    is ApiResponse.ApiError -> {
                        hideDialog()

                        state.message?.let {
                            showSnackBar(binding.root, state.message, CustomSnackBar.Companion.SnackBarType.ERROR)
                        }

                    }
                }
            }
        }*/

        lifecycleScope.launchWhenStarted {
            viewModel.patientUpdateResponse.collect { state ->

                println("Patient Login Response :: $state")

                when (state) {
                    is ApiResponse.ApiLoading -> {
                        // show loading
                    }

                    is ApiResponse.ApiSuccess -> {
                        val apiData = state.data
                        hideDialog()

                        if (apiData.data.success == true) {

                            CoroutineScope(Dispatchers.IO).launch {
                                PatientPref.patient = apiData.data.patient

                                BodyCheckupPref.age = DatePickerUtil.getAgeFromDob(
                                    apiData.data.patient.dateOfBirth.toString()
                                ).toString()

                                delay(100)

                                viewModel.resetPatientUpdateState()
                            }

                            findNavController().navigate(R.id.go_to_dashboard_register_action)

                            showSnackBar(
                                binding.root,
                                "Patient saved successfully.",
                                CustomSnackBar.Companion.SnackBarType.SUCCESS
                            )

                        } else {
                            if (apiData.data.patient != null) {
                                PatientPref.patient = apiData.data.patient

                                findNavController().navigate(R.id.go_to_dashboard_register_action)

                                showSnackBar(
                                    binding.root,
                                    "Patient saved successfully.",
                                    CustomSnackBar.Companion.SnackBarType.SUCCESS
                                )
                            } else {
                                showSnackBar(
                                    binding.root,
                                    apiData.data.message,
                                    CustomSnackBar.Companion.SnackBarType.ERROR
                                )
                            }
                        }
                    }

                    is ApiResponse.ApiError -> {
                        hideDialog()

                        showSnackBar(
                            binding.root,
                            state.message,
                            CustomSnackBar.Companion.SnackBarType.ERROR
                        )
                    }
                }
            }
        }

        binding.buttonSubmit.setOnClickListener {

            showDialog()

            viewModel.updatePatient()
        }

        binding.dateOfBirthInput.setOnClickListener {

            mActivity?.let { context ->
                DatePickerUtil.showPastDatePicker(context = context) { selectedDate ->

                    val date = DatePickerUtil.formatDate(selectedDate)
                    println("Selected Date of Birth ::  $selectedDate   : Converted:   $date")

                    binding.dateOfBirthInput.setText(date)
//                    viewModel.patient.value?.dateOfBirth = DatePickerUtil.formatDateToMM_DD_YYYY(date)
                    viewModel.updateDateOfBirth(DatePickerUtil.formatDateToMM_DD_YYYY(date).toString())

                }
            }
        }

        binding.genderInput.setOnClickListener {

            var genderList: List<StringValues> = mutableListOf(
                StringValues(value = "Male", isSelected = viewModel.patient.value?.gender.toString().equals("Male", ignoreCase = true)),
                StringValues(value = "Female", isSelected = viewModel.patient.value?.gender.toString().equals("Female", ignoreCase = true))
            )

            genderListDialog.showDialog("Select Gender", genderList)
        }

        binding.bloodGroupInput.setOnClickListener {

            var bloodGroupList: MutableList<StringValues> = mutableListOf(
                StringValues(value = "A+", isSelected = viewModel.patient.value?.bloodGroup == "A+"),
                StringValues(value = "A-", isSelected = viewModel.patient.value?.bloodGroup == "A-"),
                StringValues(value = "B+", isSelected = viewModel.patient.value?.bloodGroup == "B+"),
                StringValues(value = "B-", isSelected = viewModel.patient.value?.bloodGroup == "B-"),
                StringValues(value = "O+", isSelected = viewModel.patient.value?.bloodGroup == "O+"),
                StringValues(value = "O-", isSelected = viewModel.patient.value?.bloodGroup == "O-"),
                StringValues(value = "AB+", isSelected = viewModel.patient.value?.bloodGroup == "AB+"),
                StringValues(value = "AB-", isSelected = viewModel.patient.value?.bloodGroup == "AB-")
            )

            bloodGroupListDialog.showDialog("Select Blood Group", bloodGroupList)
        }

    }

    private fun setupGenderDialog() {
        genderListDialog = mActivity?.let { ItemListDialog(it) }!!

        genderListDialog.setOnItemSelectedListener(object : ItemListDialog.OnItemClickListener {
            override fun onItemSelect(gender: StringValues?) {

                Log.e("onClickDevice", "  : Gender :  $gender")

                gender?.let {
                    viewModel.updateGender(gender)
                }
            }
        })
    }

    private fun setupBloodGroupDialog() {
        bloodGroupListDialog = mActivity?.let { ItemListDialog(it) }!!

        bloodGroupListDialog.setOnItemSelectedListener(object : ItemListDialog.OnItemClickListener {
            override fun onItemSelect(bloodGroup: StringValues?) {

                Log.e("onClickDevice", "  :  Blood Group : $bloodGroup")

                bloodGroup?.let {
                    viewModel.updateBloodGroup(bloodGroup)
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
    }

    override fun checkConnectivity() {
    }
}