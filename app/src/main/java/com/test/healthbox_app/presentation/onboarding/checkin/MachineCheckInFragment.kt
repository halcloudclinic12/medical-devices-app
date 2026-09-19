package com.test.healthbox_app.presentation.onboarding.checkin

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.test.healthbox_app.R
import com.test.healthbox_app.base.BaseFragment
import com.test.healthbox_app.data.model.response.ClinicLoginResponse
import com.test.healthbox_app.databinding.MachineCheckInFragmentBinding
import com.test.healthbox_app.domain.model.ApiResponse
import com.test.healthbox_app.presentation.util.CustomSnackBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.logging.Logger

@AndroidEntryPoint
class MachineCheckInFragment() : BaseFragment() {

    private lateinit var viewModel: MachineCheckInViewModel

    private lateinit var binding: MachineCheckInFragmentBinding

    private var clinicLoginResponse: ClinicLoginResponse? = ClinicLoginResponse()

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = MachineCheckInFragmentBinding.inflate(inflater)

        viewModel = ViewModelProvider(this)[MachineCheckInViewModel::class.java]

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

//        viewModel = ViewModelProvider(this, factory)[MachineCheckInViewModel::class.java]

        viewModel.formError.observe(viewLifecycleOwner) { it ->

            hideDialog()

            CustomSnackBar.make(
                binding.root, it.toString(), Snackbar.LENGTH_SHORT, CustomSnackBar.Companion.SnackBarType.WARNING
            ).show()
        }

        viewModel.machineId.observe(viewLifecycleOwner) { it ->
            resetClinic()
        }

        viewModel.machinePassword.observe(viewLifecycleOwner) { it ->
            resetClinic()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /*viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            viewModel.loginState.collect { state ->
                when (state) {
                    is ApiResponse.ApiLoading -> {
                        Logger.getLogger("LoginClinicLogs").info("  :  Loading : ${state.apiData?.data?.token}")
                        // show loading
                    }

                    is ApiResponse.ApiSuccess -> {
                        Logger.getLogger("LoginClinicLogs").info("  :  Token : ${state.apiData?.data?.token}")

                        hideDialog()

                        state.apiData?.let { apiData ->
                            if (apiData.data?.valid == true) {

                                clinicLoginResponse = apiData

                                withContext(Dispatchers.Main) {
                                    binding.buttonCheckIn.visibility = View.VISIBLE

                                    showClinicName(apiData)
                                }

                            } else {

//                                Toast.makeText(requireContext(), apiData.data?.message, Toast.LENGTH_SHORT).show()
                                withContext(Dispatchers.Main) {
                                    binding.buttonCheckIn.visibility = View.GONE
                                }

                                apiData.data?.message?.let {
                                    showSnackBar(binding.root, it, CustomSnackBar.Companion.SnackBarType.ERROR)
                                }
                            }
                        }
                    }

                    is ApiResponse.ApiError -> {
                        hideDialog()


                        withContext(Dispatchers.Main) {
                            binding.buttonCheckIn.visibility = View.GONE
                        }
                        state.message?.let {
                            println("Machine Logoin API Logs :: ${state.message}")
                            println("Machine Logoin API Logs :: ${state.apiData}")

                            showSnackBar(binding.root, it, CustomSnackBar.Companion.SnackBarType.ERROR)
                        }
//                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }*/

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            viewModel.loginState.collect { state ->
                when (state) {
                    is ApiResponse.ApiLoading -> {
                        Logger.getLogger("LoginClinicLogs").info("  :  Loading")
                        // show loading
                    }

                    is ApiResponse.ApiSuccess -> {
                        val apiData = state.data
                        Logger.getLogger("LoginClinicLogs").info("  :  Token : ${apiData.data?.token}")

                        hideDialog()

                        if (apiData.data?.valid == true) {
                            clinicLoginResponse = apiData

                            withContext(Dispatchers.Main) {
                                binding.buttonCheckIn.visibility = View.VISIBLE
                                showClinicName(apiData)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                binding.buttonCheckIn.visibility = View.GONE
                            }

                            apiData.data?.message?.let {
                                showSnackBar(binding.root, it, CustomSnackBar.Companion.SnackBarType.ERROR)
                            }
                        }
                    }

                    is ApiResponse.ApiError -> {
                        hideDialog()

                        withContext(Dispatchers.Main) {
                            binding.buttonCheckIn.visibility = View.GONE
                        }

                        println("Machine Login API Logs :: ${state.message}")
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

            viewModel.clinicLogin()

        }

        binding.buttonCheckIn.setOnClickListener {

            Logger.getLogger("LoginClinicLogs").info(" :: clinic Response :: ${clinicLoginResponse} ")

            clinicLoginResponse?.let { it1 ->
                viewModel.saveClinic(it1)
            }

            showSnackBar(binding.root, "Clinic Logged-in Successfully..", CustomSnackBar.Companion.SnackBarType.SUCCESS)

            mActivity?.navController?.navigate(R.id.enter_m_pin_after_check_in_action)
        }

    }


    fun showClinicName(apiData: ClinicLoginResponse) {

        val clinicName = apiData.data?.clinic?.name ?: ""

        binding.clinicConfirmationLayout.visibility = View.VISIBLE
        binding.machineNameText.text = clinicName
    }

    fun resetClinic() {
        binding.buttonCheckIn.visibility = View.GONE
        binding.clinicConfirmationLayout.visibility = View.GONE
    }
}