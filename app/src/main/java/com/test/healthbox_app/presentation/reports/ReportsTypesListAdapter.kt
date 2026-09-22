package com.test.healthbox_app.presentation.reports

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.test.healthbox_app.R
import com.test.healthbox_app.data.model.ReportTestType
import javax.inject.Inject

class ReportsTypesListAdapter @Inject constructor(
    private val reportsTypesList: List<ReportTestType>, private val context: Context?, private val onItemClick: (ReportTestType) -> Unit
) : RecyclerView.Adapter<ReportsTypesListAdapter.DeviceViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.reports_types_llayout_row, parent, false)
        return DeviceViewHolder(view)
    }

    override fun getItemCount(): Int {
        return reportsTypesList.size
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val reportTestType = reportsTypesList[position]
        holder.bind(reportTestType, context!!, onItemClick)
    }

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layoutChip: ConstraintLayout = itemView.findViewById(R.id.layout_chip)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)

        fun bind(reportTestType: ReportTestType, context: Context, onItemClick: (ReportTestType) -> Unit) {
            tvTitle.text = reportTestType.title

            if (reportTestType.isSelected == true) {
                layoutChip.setBackgroundResource(R.drawable.bg_btn_teal)
                tvTitle.setTextColor(ContextCompat.getColor(context, R.color.white))
            } else {
                layoutChip.setBackgroundResource(R.drawable.bg_btn_outline)
                tvTitle.setTextColor(ContextCompat.getColor(context, R.color.app_color))
            }

            itemView.setOnClickListener { view ->
                print("onItemClick  : $reportTestType")
                onItemClick(reportTestType)
            }

        }
    }
}