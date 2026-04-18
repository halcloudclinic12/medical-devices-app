package com.test.healthbox_app.presentation.splash

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.test.healthbox_app.R
import com.test.healthbox_app.base.BaseFragment
import com.test.healthbox_app.databinding.SplashFragmentBinding
import com.test.healthbox_app.domain.model.ApiResponse
import com.test.healthbox_app.presentation.util.CustomSnackBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.logging.Logger

@AndroidEntryPoint
class SplashFragment() : BaseFragment() {

    private lateinit var viewModel: SplashViewModel

    private lateinit var splashFragmentBinding: SplashFragmentBinding

    private var token: String = ""
    private var refreshToken: String = ""

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
        splashFragmentBinding = SplashFragmentBinding.inflate(inflater)

        viewModel = ViewModelProvider(this)[SplashViewModel::class.java]

        splashFragmentBinding.lifecycleOwner = this

        return splashFragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        Handler(Looper.getMainLooper()).postDelayed({


        if (viewModel.getClinic() == null) {
            println("clinicDataLogs Splash  :  :   ${Gson().toJson(viewModel.getClinic())}")
            mActivity?.navController?.navigate(R.id.machine_check_in_action)

        } else {
            println("clinicDataLogs Splash  : isVerificationExpired : ${viewModel.isVerificationExpired()}   : -- :    ${Gson().toJson(viewModel.getClinic())}")

            if (viewModel.isVerificationExpired() == true) {
                viewModel.refreshToken()
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    mActivity?.navController?.navigate(R.id.action_splash_fragment_to_login_patient)
                }, 500)
            }
        }
        observeTokenState()

//        }, 500)
    }

    private fun observeTokenState() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            viewModel.refreshTokenState.collect { state ->
                when (state) {
                    is ApiResponse.ApiLoading -> {
                        Log.d("LoginClinicLogs", "  :  Loading")
                        // show loading
                    }

                    is ApiResponse.ApiSuccess -> {
                        val apiData = state.data
                        Logger.getLogger("LoginClinicLogs").info(" refresh api success Token : ${apiData.data?.token}")
                        hideDialog()

                        Logger.getLogger("LoginClinicLogs").info(" refresh api Data :: 1 :: ${apiData.data}")

                        if (apiData.data?.token.isNullOrEmpty()) {
                            CustomSnackBar.make(
                                splashFragmentBinding.root,
                                apiData.data?.error?.message.toString(),
                                Snackbar.LENGTH_SHORT,
                                CustomSnackBar.Companion.SnackBarType.WARNING
                            ).show()
                        } else {
                            Logger.getLogger("LoginClinicLogs").info(" refresh api Data :: 2 :: ${apiData.data}")

                            token = apiData.data?.token.toString()
                            refreshToken = apiData.data?.refreshToken.toString()

                            Logger.getLogger("LoginClinicLogs").info(" refresh api Data else ${apiData.data}")

                            viewModel.saveToken(token = token, refreshToken = refreshToken)

                            withContext(Dispatchers.Main) {
                                mActivity?.navController?.navigate(R.id.action_splash_fragment_to_MPinAuthenticateFragment)
                            }
                        }
                    }

                    is ApiResponse.ApiError -> {
                        hideDialog()
                    }
                }
            }
        }
        /*viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            viewModel.refreshTokenState.collect { state ->
                when (state) {
                    is ApiResponse.ApiLoading -> {
                        Log.d("LoginClinicLogs", "  :  Loading : ${state.apiData?.data?.token}")
                        // show loading
                    }

                    is ApiResponse.ApiSuccess -> {
                        Logger.getLogger("LoginClinicLogs").info(" refresh api success Token : ${state.apiData?.data?.token}")
                        hideDialog()

                        state.apiData?.let { apiData ->
                            Logger.getLogger("LoginClinicLogs").info(" refresh api Data :: 1 ::${apiData.data} ")

                            if (apiData.data?.token.isNullOrEmpty()) {
                                CustomSnackBar.make(
                                    splashFragmentBinding.root,
                                    apiData.data?.error?.message.toString(),
                                    Snackbar.LENGTH_SHORT,
                                    CustomSnackBar.Companion.SnackBarType.WARNING
                                ).show()

                            } else {

                                Logger.getLogger("LoginClinicLogs").info(" refresh api Data :: 2 ::${apiData.data} ")

                                token = apiData.data?.token.toString()

                                refreshToken = apiData.data?.refreshToken.toString()

                                Logger.getLogger("LoginClinicLogs").info(" refresh api Data else ${apiData.data}")

//                                viewModel.verifyToken(token = apiData.data?.token.toString())

                                viewModel.saveToken(token = token, refreshToken = refreshToken)

                                withContext(Dispatchers.Main) {
                                    mActivity?.navController?.navigate(R.id.action_splash_fragment_to_MPinAuthenticateFragment)
                                }

                            }
                        }
                    }

                    is ApiResponse.ApiError -> {
                        hideDialog()
                    }
                }
            }
        }*/

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            viewModel.verifyTokenState.collect { state ->

                when (state) {
                    is ApiResponse.ApiLoading -> {
                        println("VerifyTokenLogs  :  Loading")
                        // show loading
                    }

                    is ApiResponse.ApiSuccess -> {
                        val apiData = state.data
                        println("VerifyTokenLogs :  Token : ${apiData.data}")
                        hideDialog()

                        if (apiData.data?.valid == true) {

                            println("VerifyTokenLogs :  Saving Token : $token")
                            println("VerifyTokenLogs :  Saving Refresh Token : $refreshToken")

                            viewModel.saveToken(token = token, refreshToken = refreshToken)

                            withContext(Dispatchers.Main) {
                                mActivity?.navController?.navigate(R.id.action_splash_fragment_to_MPinAuthenticateFragment)
                            }

                        } else {
                            withContext(Dispatchers.Main) {
                                CustomSnackBar.make(
                                    splashFragmentBinding.root,
                                    "",
                                    Snackbar.LENGTH_SHORT,
                                    CustomSnackBar.Companion.SnackBarType.WARNING
                                ).show()
                            }
                        }
                    }

                    is ApiResponse.ApiError -> {
                        hideDialog()
                    }
                }
            }
        }

        /*viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            viewModel.verifyTokenState.collect { state ->

                when (state) {
                    is ApiResponse.ApiLoading -> {
                        println("VerifyTokenLogs  :  Loading : ")
                        // show loading
                    }

                    is ApiResponse.ApiSuccess -> {
                        println("VerifyTokenLogs :  Token : ${state.apiData?.data}")
                        hideDialog()

                        state.apiData?.let { apiData ->
                            if (apiData.data?.valid == true) {

                                println("VerifyTokenLogs :  Saving Token : ${token}")
                                println("VerifyTokenLogs :  Saving Refresh Token : ${refreshToken}")

                                viewModel.saveToken(token = token, refreshToken = refreshToken)

                                withContext(Dispatchers.Main) {
                                    mActivity?.navController?.navigate(R.id.action_splash_fragment_to_MPinAuthenticateFragment)
                                }

                            } else {
                                withContext(Dispatchers.Main) {
                                    CustomSnackBar.make(
                                        splashFragmentBinding.root, "", Snackbar.LENGTH_SHORT, CustomSnackBar.Companion.SnackBarType.WARNING
                                    ).show()
                                }
                            }
                        }
                    }

                    is ApiResponse.ApiError -> {
                        hideDialog()

                    }
                }
            }
        }*/
    }

}