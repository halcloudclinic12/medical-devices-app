package com.test.healthbox_app.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.findViewTreeLifecycleOwner
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
    fun setOnScanClickListener(listener: OnClickListener) {
        binding.buttonScan.setOnClickListener(listener)
    }
}