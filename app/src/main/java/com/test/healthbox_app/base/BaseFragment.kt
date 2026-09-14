package com.test.healthbox_app.base

import android.app.ProgressDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.test.healthbox_app.MainActivity
import com.test.healthbox_app.R
import com.test.healthbox_app.data.model.PatientPref
import com.test.healthbox_app.presentation.util.CustomSnackBar
import com.test.healthbox_app.presentation.util.DatePickerUtil
import com.test.healthbox_app.presentation.util.NetConnection

abstract class BaseFragment : Fragment() {
    private var progressDialog: ProgressDialog? = null

    abstract fun checkConnectivity()

    abstract val isConnected: Unit

    @JvmField
    var mActivity: MainActivity? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = context as MainActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initDependencyInjection()
    }

    protected open fun initDependencyInjection() {
        // Initialize Dagger/Hilt components
    }

    private fun makeApiCall() {
        if (NetConnection.checkConnection(activity)) {
            Log.d("Checking_internet", "devic123")
            isConnected
        } else {
            showNoInternetConnectionDialog()
            //Toast.makeText(this, "Connect Your Network", Toast.LENGTH_SHORT).show();
        }
    }

    fun showSnackBar(view: View, message: String, snackBarType: CustomSnackBar.Companion.SnackBarType) {
        CustomSnackBar.make(
            view, message, Snackbar.LENGTH_SHORT, snackBarType
        ).show()
    }

    /**
     * Universal guard for any user-initiated Bluetooth action (scan, connect, print, ...).
     * If Bluetooth is already on, [onEnabled] runs immediately. If it's off, the user is shown
     * the system "Turn on Bluetooth?" prompt; [onEnabled] runs only if they accept, otherwise
     * [rootView] gets a snackbar explaining why nothing happened.
     *
     * Call this at the entry point of the action (a button click), not from a passive observer
     * (e.g. a saved-device auto-connect on screen entry) — that would pop the system dialog
     * unprompted just from navigating to the screen.
     */
    fun ensureBluetoothEnabled(rootView: View, onEnabled: () -> Unit) {
        val activity = mActivity ?: return
        activity.ensureBluetoothEnabled { enabled ->
            if (enabled) {
                onEnabled()
            } else {
                showSnackBar(
                    rootView,
                    "Bluetooth is turned off. Please turn it on to continue.",
                    CustomSnackBar.Companion.SnackBarType.ERROR
                )
            }
        }
    }

    /**
     * Call this inside onViewCreated of child fragments
     * and pass the included user info view root.
     */
    protected fun setupUserInfo(userInfoView: View) {
        val tvName = userInfoView.findViewById<TextView>(R.id.tv_patient_name_age)
        val tvDetails = userInfoView.findViewById<TextView>(R.id.tv_patient_gender)


        val patient = PatientPref.patient
        patient?.let {

            tvName.text = patient.name
            val gender = patient.gender ?: ""
            val age = DatePickerUtil.getAgeFromDob(patient.dateOfBirth.toString())

            tvDetails.text = "$age | $gender"
        }

//        userViewModel.userDob.observe(viewLifecycleOwner) { dob ->

//            val gender = userViewModel.userGender.value ?: ""
//            tvDetails.text = "$dob | $gender"
//        }
    }

    fun showDialog() {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(requireContext())
            progressDialog!!.show()

            if (progressDialog!!.window != null) {
                progressDialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }

            progressDialog!!.setContentView(R.layout.progress_dialog)
            progressDialog!!.isIndeterminate = true
            progressDialog!!.setCancelable(false)
            progressDialog!!.setCanceledOnTouchOutside(false)

        } else {
            progressDialog!!.show()
        }
    }

    fun hideDialog() {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog!!.dismiss()
        }
    }

    fun showToastMessage(message: String?) {
        Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show()
    }

    fun showNoInternetConnectionDialog() {
        Log.e("Testing net Connection", "Entering showNoInternetConnectionDialog Method")
        val builder = AlertDialog.Builder(requireActivity())
        builder.setMessage("Whoops! Its seems you don't have internet connection, please try again later!").setTitle("No Internet Connection")
            .setCancelable(false).setNeutralButton("Retry") { dialog, id -> makeApiCall() }
        val alert = builder.create()
        alert.show()
        //  Log.e("Testing net Connection", "Showed NoIntenetConnectionDialog");
    }

}
