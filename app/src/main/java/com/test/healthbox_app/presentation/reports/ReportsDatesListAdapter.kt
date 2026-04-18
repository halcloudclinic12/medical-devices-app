package com.test.healthbox_app.presentation.reports

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.test.healthbox_app.R
import com.test.healthbox_app.data.model.response.BasicTestData
import com.test.healthbox_app.presentation.util.DatePickerUtil
import javax.inject.Inject

class ReportsDatesListAdapter @Inject constructor(
    private val reportsResultList: List<BasicTestData>,
    private val selectedReport: BasicTestData,
    private val onItemClick: (BasicTestData) -> Unit
) : RecyclerView.Adapter<ReportsDatesListAdapter.DeviceViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.reports_dates_list_row_layout, parent, false)
        return DeviceViewHolder(view)
    }

    override fun getItemCount(): Int {
        return reportsResultList.size
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val reportsResult = reportsResultList[position]
        holder.bind(selectedReport = selectedReport, basicTestData = reportsResult, onItemClick = onItemClick)
    }

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layoutDate: ConstraintLayout = itemView.findViewById(R.id.layout_date)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)

        @SuppressLint("NewApi")
        fun bind(selectedReport: BasicTestData, basicTestData: BasicTestData, onItemClick: (BasicTestData) -> Unit) {
            if (selectedReport.Id == basicTestData.Id) {
                layoutDate.setBackgroundResource(R.drawable.bg_step_completed)
            } else {
                layoutDate.setBackgroundResource(R.drawable.bg_step_pending)
            }

            val dateOfBirthTimeStamp = DatePickerUtil.isoStringToTimestamp(basicTestData.createdAt)

            tvDate.text = DatePickerUtil.formatDate(dateOfBirthTimeStamp, pattern = DatePickerUtil.dateReportFormatOnly)

            itemView.setOnClickListener {
                onItemClick(basicTestData)
            }

        }
    }
}