package com.test.healthbox_app.presentation.tests.results

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.test.healthbox_app.R
import com.test.healthbox_app.data.model.Parameters
import com.test.healthbox_app.presentation.util.ResultColorUtils
import javax.inject.Inject

class ParametersResultsListAdapter @Inject constructor(
    private val parametersResultList: List<Parameters>, private val context: Context?
) : RecyclerView.Adapter<ParametersResultsListAdapter.DeviceViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.parameters_result_row_layout, parent, false)
        return DeviceViewHolder(view)
    }

    override fun getItemCount(): Int {
        return parametersResultList.size
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val parameterResult = parametersResultList[position]
        holder.bind(parameterResult, context!!)
    }

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvParameter: TextView = itemView.findViewById(R.id.tv_parameter)
        private val tvResult: TextView = itemView.findViewById(R.id.tv_result)
        private val tvValue: TextView = itemView.findViewById(R.id.tv_value)
        private val tvRange: TextView = itemView.findViewById(R.id.tv_range)

        fun bind(parameter: Parameters, context: Context) {
            tvParameter.text = parameter.parameterName
            tvResult.text = parameter.result
            tvValue.text = parameter.value
            tvRange.text = parameter.range

            tvResult.setTextColor(ResultColorUtils.getColorForResult(context = context, parameter.result.toString()))


            /*tvDeviceName.setOnClickListener { view ->
                print("onItemClick  : $device")
                onItemClick(device)
            }*/
        }
    }
}