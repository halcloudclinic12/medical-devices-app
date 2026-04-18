package com.test.healthbox_app.presentation.onboarding.mPin

import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.test.healthbox_app.R
import com.test.healthbox_app.base.BaseFragment
import com.test.healthbox_app.databinding.MPinAuthenticateFragmentBinding
import com.test.healthbox_app.presentation.util.Constants
import com.test.healthbox_app.presentation.util.CustomSnackBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MPinAuthenticateFragment() : BaseFragment() {

    lateinit var viewModel: MPinAuthenticateViewModel

    private lateinit var binding: MPinAuthenticateFragmentBinding

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
        binding = MPinAuthenticateFragmentBinding.inflate(inflater)

        viewModel = ViewModelProvider(this)[MPinAuthenticateViewModel::class.java]

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val displayMetrics = DisplayMetrics()
        mActivity?.windowManager!!.defaultDisplay.getMetrics(displayMetrics)

        if (Constants.LOGS_ENABLE)
            mActivity?.navController?.navigate(R.id.patient_login_m_pin_success_action)

        binding.buttonSubmit.setOnClickListener {
            viewModel.mPin.let { it ->

                Log.e("enteredMPinLogs", " : " + it.value.toString())

                if (it.value.isNullOrBlank()) {
                    showSnackBar(binding.root, "Enter M Pin", CustomSnackBar.Companion.SnackBarType.ERROR)
                    return@setOnClickListener
                }

                if (it.value.toString().length < 6) {
                    showSnackBar(binding.root, "Enter valid M Pin", CustomSnackBar.Companion.SnackBarType.ERROR)
                    return@setOnClickListener
                }

                if (it.value.toString() == "123456") {
                    mActivity?.navController?.navigate(R.id.patient_login_m_pin_success_action)
                }
            }
        }
    }
}