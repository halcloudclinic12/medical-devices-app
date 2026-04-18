package com.test.healthbox_app.presentation.dialog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.test.healthbox_app.R
import com.test.healthbox_app.domain.model.BleDevice
import javax.inject.Inject

class DeviceListAdapter @Inject constructor(
    private val devices: List<BleDevice>,
    private val onItemClick: (BleDevice) -> Unit
) : RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.device_list_item, parent, false)
        return DeviceViewHolder(view)
    }

    override fun getItemCount(): Int {
        return devices.size
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.bind(device, onItemClick)
    }

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDeviceName: TextView = itemView.findViewById(R.id.tv_device_name)

        fun bind(device: BleDevice, onItemClick: (BleDevice) -> Unit) {
            tvDeviceName.text = device.name

            tvDeviceName.setOnClickListener { view ->
                print("onItemClick  : $device")
                onItemClick(device)
            }
        }
    }
}