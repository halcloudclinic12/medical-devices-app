package com.test.healthbox_app.presentation.util

import android.content.Context
import androidx.core.content.ContextCompat
import com.test.healthbox_app.R

object ResultColorUtils {
    fun getColorForResult(context: Context, result: String?): Int {
        return when (result) {
            "Indicative", "Not upto Standard" -> ContextCompat.getColor(context, R.color.result_indicative)
            "Low", "Below Control Weight" -> ContextCompat.getColor(context, R.color.result_low)
            "Average", "Borderline", "Below 92% without oxygen or below 95%" -> ContextCompat.getColor(context, R.color.result_average)
            "Normal", "Good", "At Control Weight", "Standard" -> ContextCompat.getColor(context, R.color.result_normal)
            "High", "Above Control Weight" -> ContextCompat.getColor(context, R.color.result_high)
            "Very High", "Critical" -> ContextCompat.getColor(context, R.color.result_very_high)
            else -> ContextCompat.getColor(context, R.color.result_unknown)
        }
    }
}