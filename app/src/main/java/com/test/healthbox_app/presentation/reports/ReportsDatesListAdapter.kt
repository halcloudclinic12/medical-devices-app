package com.test.healthbox_app.presentation.reports

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.test.healthbox_app.R
import com.test.healthbox_app.presentation.util.DatePickerUtil

/**
 * Generic over the report record type ([T]) - BasicTestData and Hba1cTestData are two
 * unrelated Gson models (deliberately not sharing a base type, see Hba1cTestData's own
 * doc comment), so [getId]/[getCreatedAt] extract what this adapter needs instead of
 * requiring a shared interface. Always constructed manually per report type (Basic vs
 * HbA1c), never through Hilt, despite living in the same file structure as other
 * @Inject adapters.
 */
class ReportsDatesListAdapter<T>(
    private val reportsResultList: List<T>,
    private val selectedReport: T,
    private val getId: (T) -> String?,
    private val getCreatedAt: (T) -> String?,
    private val onItemClick: (T) -> Unit
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
        holder.bind(
            isSelected = getId(selectedReport) == getId(reportsResult),
            createdAt = getCreatedAt(reportsResult),
            onItemClick = { onItemClick(reportsResult) }
        )
    }

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layoutDate: ConstraintLayout = itemView.findViewById(R.id.layout_date)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)

        @SuppressLint("NewApi")
        fun bind(isSelected: Boolean, createdAt: String?, onItemClick: () -> Unit) {
            val context = itemView.context

            if (isSelected) {
                layoutDate.setBackgroundResource(R.drawable.bg_btn_teal)
                tvDate.setTextColor(ContextCompat.getColor(context, R.color.white))
            } else {
                layoutDate.setBackgroundResource(R.drawable.bg_btn_outline)
                tvDate.setTextColor(ContextCompat.getColor(context, R.color.app_color))
            }

            val dateOfBirthTimeStamp = DatePickerUtil.isoStringToTimestamp(createdAt)

            // Single-line "dd MMM" - the chip is only 40dp tall (matches the
            // report-type chips it shares a row with), too short for a multi-line stack.
            tvDate.text = DatePickerUtil.formatDate(dateOfBirthTimeStamp, pattern = "dd MMM")

            itemView.setOnClickListener {
                onItemClick()
            }

        }
    }
}
