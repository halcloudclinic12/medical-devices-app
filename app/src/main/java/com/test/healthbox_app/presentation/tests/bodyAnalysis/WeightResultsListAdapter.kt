package com.test.healthbox_app.presentation.tests.bodyAnalysis

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.test.healthbox_app.R
import com.test.healthbox_app.domain.model.WeightMeasurement
import javax.inject.Inject

class WeightResultsListAdapter @Inject constructor(
    private val weightMeasurementList: List<WeightMeasurement>
) : RecyclerView.Adapter<WeightResultsListAdapter.DeviceViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.weight_results_list_item, parent, false)
        return DeviceViewHolder(view)
    }

    override fun getItemCount(): Int {
        return weightMeasurementList.size
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val weightMeasurement = weightMeasurementList[position]
        holder.bind(weightMeasurement)
    }

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvParameterName: TextView = itemView.findViewById(R.id.tv_parameter_name)
        private val tvValue: TextView = itemView.findViewById(R.id.tv_value)

        fun bind(weightMeasurement: WeightMeasurement) {
            tvParameterName.text = weightMeasurement.label
            tvValue.text = weightMeasurement.value + weightMeasurement.unit

            /*tvDeviceName.setOnClickListener { view ->
                print("onItemClick  : $device")
                onItemClick(device)
            }*/
        }
    }
}