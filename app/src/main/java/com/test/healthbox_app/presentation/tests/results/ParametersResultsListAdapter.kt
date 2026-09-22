package com.test.healthbox_app.presentation.tests.results

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.test.healthbox_app.R
import com.test.healthbox_app.data.model.Parameters
import com.test.healthbox_app.presentation.util.ResultColorUtils
import javax.inject.Inject

/**
 * Renders the results list as a hero summary card + grouped, categorized parameter rows
 * (flagged results pinned first under their own section) instead of one flat list - the
 * summary/grouping is computed here from the same flat [parametersResultList] every caller
 * already passes in, so ResultsFragment's construction site never needed to change.
 */
class ParametersResultsListAdapter @Inject constructor(
    private val parametersResultList: List<Parameters>, private val context: Context?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed class ListItem {
        data class Summary(val okCount: Int, val totalCount: Int, val flaggedCount: Int) : ListItem()
        data class SectionHeader(val title: String, val isAttention: Boolean) : ListItem()
        data class Row(val parameter: Parameters) : ListItem()
    }

    companion object {
        private const val TYPE_SUMMARY = 0
        private const val TYPE_SECTION_HEADER = 1
        private const val TYPE_ROW = 2

        private const val SECTION_ATTENTION = "NEEDS ATTENTION"
        private const val SECTION_VITALS = "VITALS"
        private const val SECTION_BODY_COMPOSITION = "BODY COMPOSITION"
        private const val SECTION_VISION = "VISION"

        // This adapter is also reused by ReportsFragment (past-reports browsing), which maps
        // Parameters through BasicTestDataMapper instead of BodyCheckupPref.toParameterList()
        // and spells a couple of names differently ("Eye Left Vision", "Oxygen (SpO₂)") -
        // both spellings are listed so categorization/icons work correctly on either screen.
        private val VITALS_PARAMS = setOf(
            "Temperature", "Blood Pressure (Diastolic)", "Blood Pressure (Systolic)",
            "Oxygen Saturation", "Oxygen (SpO₂)", "Pulse", "Sugar", "Hemoglobin", "HbA1c"
        )
        private val VISION_PARAMS = setOf(
            "Left Eye Vision", "Right Eye Vision", "Eye Left Vision", "Eye Right Vision"
        )

        // Every "this is fine" label actually produced across BodyCheckupPref's classifiers
        // (getResultFromRange tables, getBmrResult, getControlWeightResult, getVisionResult,
        // getSystolicResult/getDiastolicResult all use different vocabularies - this is an
        // allowlist of the "OK" outcomes rather than a keyword guess, since e.g. "Below normal
        // metabolism" and "Not up to Standard" both contain an OK-sounding word but mean the
        // opposite).
        private val OK_RESULTS = setOf(
            "normal", "standard", "good", "normal metabolism", "normal / at control weight", "at control weight"
        )

        // Not a real result either way - excluded from the flagged/normal tally entirely.
        private val UNMEASURED_RESULTS = setOf("not available")

        private fun categoryFor(parameterName: String?): String = when {
            parameterName in VITALS_PARAMS -> SECTION_VITALS
            parameterName in VISION_PARAMS -> SECTION_VISION
            else -> SECTION_BODY_COMPOSITION
        }

        /** null = unmeasured (excluded from the summary tally), true = flagged, false = OK. */
        private fun isFlagged(result: String?): Boolean? {
            if (result.isNullOrBlank()) return null
            val normalized = result.trim().lowercase()
            if (normalized in UNMEASURED_RESULTS) return null
            return normalized !in OK_RESULTS
        }

        private fun iconFor(parameterName: String?): Int = when (parameterName) {
            "Height" -> R.drawable.ic_height
            "Weight" -> R.drawable.ic_body_weight
            "Temperature" -> R.drawable.ic_temperature
            "Blood Pressure (Diastolic)", "Blood Pressure (Systolic)" -> R.drawable.ic_blood_pressure
            "Left Eye Vision", "Right Eye Vision", "Eye Left Vision", "Eye Right Vision" -> R.drawable.ic_vision
            "Pulse" -> R.drawable.ic_pulse
            "Sugar" -> R.drawable.ic_glucose_test
            "Hemoglobin" -> R.drawable.ic_hemoglobin
            "HbA1c" -> R.drawable.ic_hba1c
            "Oxygen Saturation", "Oxygen (SpO₂)" -> R.drawable.ic_pulse
            else -> R.drawable.ic_health_checkup
        }
    }

    private val items: List<ListItem> = buildList {
        val flaggedParams = parametersResultList.filter { isFlagged(it.result) == true }
        val okCount = parametersResultList.count { isFlagged(it.result) == false }
        val totalCounted = okCount + flaggedParams.size

        if (totalCounted > 0) {
            add(ListItem.Summary(okCount = okCount, totalCount = totalCounted, flaggedCount = flaggedParams.size))
        }

        val flaggedNames = flaggedParams.mapNotNull { it.parameterName }.toSet()
        val remaining = parametersResultList.filter { it.parameterName !in flaggedNames }
        val grouped = remaining.groupBy { categoryFor(it.parameterName) }

        // Collect the non-empty groups first rather than adding headers inline. A single
        // parameter (the HbA1c flow's lone row, or a Basic report where only one step was
        // actually completed) only ever produces one group - its own header would just
        // repeat what the hero card already says, so it's suppressed when there's nothing
        // else on screen to distinguish it from.
        val sections = buildList {
            if (flaggedParams.isNotEmpty()) add(Triple(SECTION_ATTENTION, true, flaggedParams))
            listOf(SECTION_VITALS, SECTION_BODY_COMPOSITION, SECTION_VISION).forEach { category ->
                val rows = grouped[category].orEmpty()
                if (rows.isNotEmpty()) add(Triple(category, false, rows))
            }
        }

        val showSectionHeaders = sections.size > 1
        sections.forEach { (title, isAttention, rows) ->
            if (showSectionHeaders) add(ListItem.SectionHeader(title, isAttention))
            rows.forEach { add(ListItem.Row(it)) }
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is ListItem.Summary -> TYPE_SUMMARY
        is ListItem.SectionHeader -> TYPE_SECTION_HEADER
        is ListItem.Row -> TYPE_ROW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SUMMARY -> SummaryViewHolder(inflater.inflate(R.layout.item_result_summary, parent, false))
            TYPE_SECTION_HEADER -> SectionHeaderViewHolder(inflater.inflate(R.layout.item_result_section_header, parent, false))
            else -> DeviceViewHolder(inflater.inflate(R.layout.parameters_result_row_layout, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.Summary -> (holder as SummaryViewHolder).bind(item, context!!)
            is ListItem.SectionHeader -> (holder as SectionHeaderViewHolder).bind(item, context!!)
            is ListItem.Row -> (holder as DeviceViewHolder).bind(item.parameter, context!!)
        }
    }

    private class SummaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPercent: TextView = itemView.findViewById(R.id.tv_summary_percent)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_summary_title)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tv_summary_subtitle)

        fun bind(summary: ListItem.Summary, context: Context) {
            val percent = if (summary.totalCount > 0) (summary.okCount * 100) / summary.totalCount else 0
            tvPercent.text = "$percent%"

            // A single-parameter report (the HbA1c flow, or a Basic report where only one
            // step was completed) reads oddly as "1 of 1 parameters normal" - phrase it as
            // a single result instead.
            tvTitle.text = if (summary.totalCount == 1) {
                if (summary.okCount == 1) "Your result is normal" else "Your result needs review"
            } else {
                "${summary.okCount} of ${summary.totalCount} parameters normal"
            }

            if (summary.flaggedCount > 0) {
                tvSubtitle.text = when {
                    summary.totalCount == 1 -> "Outside the normal range"
                    summary.flaggedCount == 1 -> "1 result needs attention"
                    else -> "${summary.flaggedCount} results need attention"
                }
                tvSubtitle.setTextColor(ResultColorUtils.getColorForResult(context, "Low"))
            } else {
                tvSubtitle.text = if (summary.totalCount == 1) "Within the normal range" else "All parameters in normal range"
                tvSubtitle.setTextColor(ResultColorUtils.getColorForResult(context, "Normal"))
            }
        }
    }

    private class SectionHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSectionTitle: TextView = itemView.findViewById(R.id.tv_section_title)

        fun bind(section: ListItem.SectionHeader, context: Context) {
            tvSectionTitle.text = section.title
            tvSectionTitle.setTextColor(
                if (section.isAttention) {
                    ResultColorUtils.getColorForResult(context, "Low")
                } else {
                    ResultColorUtils.getColorForResult(context, null)
                }
            )
        }
    }

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_parameter_icon)
        private val tvParameter: TextView = itemView.findViewById(R.id.tv_parameter)
        private val tvResult: TextView = itemView.findViewById(R.id.tv_result)
        private val tvValue: TextView = itemView.findViewById(R.id.tv_value)
        private val tvRange: TextView = itemView.findViewById(R.id.tv_range)

        fun bind(parameter: Parameters, context: Context) {
            ivIcon.setImageResource(iconFor(parameter.parameterName))
            tvParameter.text = parameter.parameterName
            tvValue.text = parameter.value
            // Some parameters (Fat Level, Control Weight) intentionally have no range/result -
            // show a placeholder dash instead of an empty chip/label, which otherwise looks
            // like a rendering bug in the card layout.
            tvRange.text = if (parameter.range.isNullOrBlank()) "—" else parameter.range
            tvResult.text = if (parameter.result.isNullOrBlank()) "—" else parameter.result

            tvResult.setTextColor(ResultColorUtils.getColorForResult(context = context, parameter.result))
        }
    }
}
