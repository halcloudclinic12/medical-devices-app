package com.test.healthbox_app.presentation.reports

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val viewHighlight: View = itemView.findViewById(R.id.view_highlight)

        fun bind(reportTestType: ReportTestType, context: Context, onItemClick: (ReportTestType) -> Unit) {
            tvTitle.text = reportTestType.title

            if (reportTestType.isSelected == true) {
                viewHighlight.visibility = View.VISIBLE
            } else {
                viewHighlight.visibility = View.INVISIBLE
            }

            itemView.setOnClickListener { view ->
                print("onItemClick  : $reportTestType")
                onItemClick(reportTestType)
            }

        }
    }
}