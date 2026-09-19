package com.test.healthbox_app.presentation.tests.bodyAnalysis

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
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

            val value = weightMeasurement.value.orEmpty()
            val unit = weightMeasurement.unit.orEmpty()

            // A real space between value and unit (e.g. "79.75 kg", not "79.75kg") - the
            // previous fused string with only a size/color change still read as touching.
            // Some rows have no unit at all (Fat Level's value is a bare word like "OVER").
            if (unit.isNotEmpty()) {
                val combined = "$value $unit"
                val spannable = SpannableString(combined)
                val unitStart = value.length
                spannable.setSpan(RelativeSizeSpan(0.55f), unitStart, combined.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(itemView.context, R.color.patient_login_muted_text)),
                    unitStart, combined.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                tvValue.text = spannable
            } else {
                tvValue.text = value
            }

            /*tvDeviceName.setOnClickListener { view ->
                print("onItemClick  : $device")
                onItemClick(device)
            }*/
        }
    }
}