package com.test.healthbox_app.presentation.dialog

import android.R
import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import androidx.recyclerview.widget.LinearLayoutManager
import com.test.healthbox_app.databinding.DeviceListDialogBinding
import com.test.healthbox_app.domain.model.BleDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DeviceListDialog @Inject constructor(
    @ApplicationContext private val context: Context, val onDeviceClose: () -> Unit
) {

    private var dialog: Dialog = Dialog(context, R.style.Theme_Translucent_NoTitleBar_Fullscreen)

    private lateinit var binding: DeviceListDialogBinding

    private var deviceList: MutableList<BleDevice>? = ArrayList<BleDevice>()

    private var mOnItemClickListener: OnItemClickListener? = null

    fun setOnItemSelectedListener(listener: OnItemClickListener) {
        this.mOnItemClickListener = listener
    }

    fun showDialog(deviceList: List<BleDevice>) {

        dialog.setCancelable(false)

        binding = DeviceListDialogBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        // Width is fixed to a fraction of the screen; height wraps the actual device
        // count instead of a forced 60% of screen height — with only 2-3 nearby devices
        // typically found, that used to leave a large empty gap below the list.
        dialog.window?.let { window ->
            val width = (context.resources.displayMetrics.widthPixels * 0.34).toInt()

            window.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)

            // Optional: Set dialog position to center
            window.setGravity(android.view.Gravity.CENTER)

            // Optional: Add animations
            // window.setWindowAnimations(R.style.DialogAnimation)


            // ✅ Apply background dim
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(0.5f) // 0 = no dim, 1 = full black background

            // ✅ Apply blur (Android 12+ only)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                window.attributes = window.attributes.apply {
                    blurBehindRadius = 60 // tweak blur intensity
                }
            } else {
                // fallback for lower versions: dim only
                window.setDimAmount(0.6f)
            }

            // Optional: make background transparent
            window.setBackgroundDrawableResource(android.R.color.transparent)


        }


        if (this.deviceList!!.isNotEmpty()) {
            this.deviceList!!.clear()
        }

        this.deviceList!!.addAll(deviceList)

        setupRecyclerView()

        binding.ivCloseDialog.setOnClickListener {

            println("onScan List Device Dialog close click ::  DeviceListDialog  :: ")

            onDeviceClose.invoke()
        }

        Log.e("dialogVisLog", "  :   ${dialog.isShowing}")

        if (!dialog.isShowing) dialog.show()
    }

    private fun setupRecyclerView() {
        binding.rvDeviceList.layoutManager = LinearLayoutManager(context)
        binding.rvDeviceList.adapter = deviceList?.let {
            DeviceListAdapter(it) { device ->
                print("\ndeviceAdpter:  $device")
                mOnItemClickListener?.onItemSelect(device)

                dialog.dismiss()
            }
        }
    }

    fun dismissDialog() {
        println("onScan List Device Dialog close click HB")
        dialog.dismiss()
    }


    interface OnItemClickListener {
        fun onItemSelect(unit: BleDevice?)
    }
}