package com.test.healthbox_app.presentation.view

import android.content.Context
import android.content.ContextWrapper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.test.healthbox_app.MainActivity
import com.test.healthbox_app.R
import com.test.healthbox_app.databinding.DeviceStatusLayoutBinding
import com.test.healthbox_app.presentation.view.deviceStatus.DeviceStatusViewModel

/*class DeviceStatusLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {*/

class DeviceStatusLayout : ConstraintLayout {

    private lateinit var binding: DeviceStatusLayoutBinding
    private var deviceStatusViewModel: DeviceStatusViewModel? = null

//    private val binding: DeviceStatusLayoutBinding
//    private lateinit var deviceStatusViewModel: DeviceStatusViewModel

    /*init {
//        LayoutInflater.from(context).inflate(R.layout.device_status_layout, this, true)
//        binding = DeviceStatusLayoutBinding.inflate(LayoutInflater.from(context), this, true)

        binding = DataBindingUtil.inflate(
            LayoutInflater.from(context),
            R.layout.device_status_layout,
            this,
            true
        )

        binding.viewModel = deviceStatusViewModel

    }*/

    // Constructor with three parameters (most comprehensive)
    constructor(context: Context) : super(context) {
        initializeLayout(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initializeLayout(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initializeLayout(context)
    }

    private fun initializeLayout(context: Context) {
        binding = DataBindingUtil.inflate(
            LayoutInflater.from(context), R.layout.device_status_layout, this, true
        )
    }


    // Add this function to bind the view model from the fragment
    fun bindViewModel(viewModel: DeviceStatusViewModel) {

        this.deviceStatusViewModel = viewModel

        binding.viewModel = viewModel
        binding.lifecycleOwner = findViewTreeLifecycleOwner() // Important for LiveData observation
        binding.executePendingBindings()
    }

    fun setUpDeviceAvailability(isDeviceAvailable: Boolean) {

        Log.e("setUpdevAvaiLog", " : $isDeviceAvailable")

        if (isDeviceAvailable) {
            binding.buttonScan.visibility = View.GONE
            binding.tvDeviceAvailability.visibility = View.VISIBLE
        } else {
            binding.buttonScan.visibility = View.VISIBLE
            binding.tvDeviceAvailability.visibility = View.GONE
        }
    }

    fun setupDeviceStatus(context: Context, isDeviceConnected: Boolean) {
        if (isDeviceConnected) {
            binding.tvDeviceAvailability.visibility = View.VISIBLE
            binding.tvDeviceAvailability.text = "Connected"
            binding.tvDeviceAvailability.setTextColor(context.resources!!.getColor(R.color.green))
        } else {
            binding.tvDeviceAvailability.visibility = View.VISIBLE
            binding.tvDeviceAvailability.text = "Disconnected"
            binding.tvDeviceAvailability.setTextColor(context.resources!!.getColor(R.color.red))
        }
    }

    // Add a method to set up click listeners that can be configured from the fragment
    //
    // This is the single choke point every screen's Scan button routes through, so it also
    // doubles as the universal "Bluetooth is off" gate: before the fragment's own listener
    // runs, Bluetooth is confirmed on (or the user is prompted to turn it on via the system
    // dialog). Without this, screens see an empty scan result and misreport it as
    // "No devices found" instead of asking the user to enable Bluetooth.
    fun setOnScanClickListener(listener: OnClickListener) {
        binding.buttonScan.setOnClickListener { view ->
            val activity = findHostActivity()
            if (activity == null) {
                listener.onClick(view)
                return@setOnClickListener
            }
            activity.ensureBluetoothEnabled { enabled ->
                if (enabled) {
                    listener.onClick(view)
                } else {
                    Snackbar.make(this, "Bluetooth is turned off. Please turn it on to scan for devices.", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    /** A custom view's `context` is usually the hosting Activity, but data-binding inflation
     *  can hand back a themed ContextWrapper around it, so unwrap defensively. */
    private fun findHostActivity(): MainActivity? {
        var ctx: Context? = context
        while (ctx != null) {
            if (ctx is MainActivity) return ctx
            ctx = (ctx as? ContextWrapper)?.baseContext
        }
        return null
    }
}