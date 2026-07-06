package com.test.healthbox_app.presentation.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.test.healthbox_app.BuildConfig

object PdfOpener {

    fun openUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(url), "application/pdf")
            flags = Intent.FLAG_ACTIVITY_NO_HISTORY
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Fallback: open in browser
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(browserIntent)
        }
    }


    fun buildUrl(testId: String, testType: String?): String {
        return "${BuildConfig.BASE_API}api/v1/tests/download-pdf?test_id=$testId&test_type=$testType"
    }

}