package com.test.healthbox_app.presentation.util

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.test.healthbox_app.R
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object DatePickerUtil {

    // Custom: "02-10-2025"
    val dateOnly = "dd-MM-yyyy"
    val dateReportFormatOnly = "dd-MMM\n yyyy"

    // Custom: "Thursday, 02 Oct 2025, 02:45 PM"
    val prettyFormat = "EEEE, dd MMM yyyy, hh:mm a"

    // Custom: ISO-like: "2025-10-02T14:45:12Z"
    val isoFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'"

    /**
     * Show a date picker that allows only past dates.
     *
     * @param context The context (Activity) to attach the picker.
     * @param onDateSelected Callback returning the selected date in milliseconds.
     */
    fun showPastDatePicker(context: Context, onDateSelected: (Long) -> Unit) {
        val constraintsBuilder = CalendarConstraints.Builder().setValidator(DateValidatorPointBackward.now()).build()

        val datePicker = MaterialDatePicker.Builder.datePicker().setTitleText("Select Date").setTheme(R.style.CustomDatePickerTheme)
            .setCalendarConstraints(constraintsBuilder).build()

        datePicker.addOnPositiveButtonClickListener { selection ->

            onDateSelected(selection)

        }

        (context as? AppCompatActivity)?.supportFragmentManager?.let {
            datePicker.show(it, "DATE_PICKER")
        }
    }

    /**
     * Utility to format the timestamp into a readable date.
     */
    fun formatDate(timestamp: Long?, pattern: String = "dd-MM-yyyy"): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp!!))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun isoStringToTimestamp(dateString: String?): Long? {
        return try {
            if (dateString.isNullOrBlank()) return null

            // Parse ISO 8601 with Zulu UTC (Z)
            val instant = Instant.parse(dateString)
            instant.toEpochMilli() // return milliseconds since epoch
        } catch (e: Exception) {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun formatDateToMM_DD_YYYY(dateString: String?): String? {
        return try {
            if (dateString.isNullOrBlank()) return null

            val inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.ENGLISH)
            val outputFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy", Locale.ENGLISH)

            val date = LocalDate.parse(dateString, inputFormatter)
            date.format(outputFormatter)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getAgeFromDob(dobString: String): Int {
        // Parse the ISO 8601 date string
        val dob = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ZonedDateTime.parse(dobString, DateTimeFormatter.ISO_DATE_TIME)
                .withZoneSameInstant(ZoneOffset.UTC) // normalize timezone if needed
                .toLocalDate()
        } else {
            TODO("VERSION.SDK_INT < O")
        }

        // Get today's date
        val today = LocalDate.now(ZoneOffset.UTC)

        // Calculate period between dob and today
        return Period.between(dob, today).years
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getCurrentDateTimeFormatted(pattern: String = "yyyy-MM-dd HH:mm:a"): String {
        return try {
            val currentDateTime = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            currentDateTime.format(formatter)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}