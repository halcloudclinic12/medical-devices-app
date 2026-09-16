package com.test.healthbox_app.presentation.onboarding.connectedDevices

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.test.healthbox_app.R
import com.test.healthbox_app.bluetooth.DeviceType
import javax.inject.Inject

class ConnectedDevicesListAdapter @Inject constructor(
    private val devicesTypesList: List<DeviceType>,
    private val selectedDeviceTypes: DeviceType,
    private val onItemClick: (DeviceType) -> Unit
) : RecyclerView.Adapter<ConnectedDevicesListAdapter.DeviceViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_connected_device_type, parent, false)
        return DeviceViewHolder(view)
    }

    override fun getItemCount(): Int {
        return devicesTypesList.size
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val deviceType = devicesTypesList[position]
        holder.bind(deviceType, selectedDeviceTypes, onItemClick)
    }

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val viewHighlight: View = itemView.findViewById(R.id.view_highlight)

        fun bind(deviceType: DeviceType, selectedDeviceType: DeviceType, onItemClick: (DeviceType) -> Unit) {

            var title = ""

            when (deviceType) {
                DeviceType.HEIGHT -> {
                    title = "Height"
                }

                DeviceType.THERMOMETER -> {
                    title = "Thermometer"
                }

                DeviceType.PULSE -> {
                    title = "Pulse"
                }

                DeviceType.WEIGHING_SCALE -> {
                    title = "Weighing Scale"
                }

                DeviceType.BLOOD_PRESSURE_MONITOR -> {
                    title = "Blood Pressure Monitor"
                }

                DeviceType.HB_CHECK -> {
                    title = "HB Check"
                }

                DeviceType.GLUCOSE_METER -> {
                    title = "Glucose Meter"
                }

                DeviceType.HBA1C_METER -> {
                    title = "HbA1c Meter"
                }

                DeviceType.BT_PRINTER -> {
                    title = "Printer"
                }
            }

            tvTitle.text = title


            if (deviceType.name == selectedDeviceType.name) {
                viewHighlight.visibility = View.VISIBLE
            } else {
                viewHighlight.visibility = View.INVISIBLE
            }

            itemView.setOnClickListener { view ->
                print("onItemClick  : $deviceType")
                onItemClick(deviceType)

            }

        }
    }
}