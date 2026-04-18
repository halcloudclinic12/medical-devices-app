package com.test.healthbox_app.presentation.util

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

class CustomSnackBar {

    companion object {

        // SnackBar Types
        enum class SnackBarType {
            SUCCESS, ERROR, WARNING, INFO, CUSTOM
        }

        // Create a beautiful custom SnackBar
        fun make(
            view: View, message: String, duration: Int = Snackbar.LENGTH_SHORT, type: SnackBarType = SnackBarType.INFO
        ): Snackbar {

            val snackbar = Snackbar.make(view, message, duration)

            // Get the SnackBar view
            val snackbarView = snackbar.view


            // Apply styling based on type
            when (type) {
                SnackBarType.SUCCESS -> styleSuccess(snackbarView)
                SnackBarType.ERROR -> styleError(snackbarView)
                SnackBarType.WARNING -> styleWarning(snackbarView)
                SnackBarType.INFO -> styleInfo(snackbarView)
                SnackBarType.CUSTOM -> styleCustom(snackbarView)
            }

            // Apply common styling
            applyCommonStyling(snackbarView)

            return snackbar
        }

        // Success SnackBar (Green theme)
        private fun styleSuccess(view: View) {
            val backgroundColor = Color.parseColor("#4CAF50")

            applyRoundedCorners(view, backgroundColor)

            val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            textView.setTextColor(Color.WHITE)

            textView.setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.ic_menu_info_details, 0, 0, 0
            )
            textView.compoundDrawablePadding = 16
            textView.gravity = Gravity.CENTER_VERTICAL
        }

        // Error SnackBar (Red theme)
        private fun styleError(view: View) {
//            view.setBackgroundColor(Color.parseColor("#F44336"))
            val backgroundColor = Color.parseColor("#F44336")

            applyRoundedCorners(view, backgroundColor)

            val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            textView.setTextColor(Color.WHITE)
            textView.setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.ic_dialog_alert, 0, 0, 0
            )
            textView.compoundDrawablePadding = 16
            textView.gravity = Gravity.CENTER_VERTICAL
        }

        // Warning SnackBar (Orange theme)
        private fun styleWarning(view: View) {
//            view.setBackgroundColor(Color.parseColor("#FF9800"))
            val backgroundColor = Color.parseColor("#FF9800")

            applyRoundedCorners(view, backgroundColor)

            val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            textView.setTextColor(Color.WHITE)
            textView.setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.ic_dialog_info, 0, 0, 0
            )
            textView.compoundDrawablePadding = 16
            textView.gravity = Gravity.CENTER_VERTICAL
        }

        // Info SnackBar (Blue theme)
        private fun styleInfo(view: View) {
//            view.setBackgroundColor(Color.parseColor("#2196F3"))
            val backgroundColor = Color.parseColor("#2196F3")

            applyRoundedCorners(view, backgroundColor)
            val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            textView.setTextColor(Color.WHITE)
            textView.setCompoundDrawablesWithIntrinsicBounds(
                android.R.drawable.ic_dialog_info, 0, 0, 0
            )
            textView.compoundDrawablePadding = 16
            textView.gravity = Gravity.CENTER_VERTICAL
        }

        // Custom gradient SnackBar
        private fun styleCustom(view: View) {
            // Create gradient background
            val gradientDrawable = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(
                    Color.parseColor("#667eea"), Color.parseColor("#764ba2")
                )
            )
            gradientDrawable.cornerRadius = 24f
            view.background = gradientDrawable

            val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            textView.setTextColor(Color.WHITE)
            textView.gravity = Gravity.CENTER_VERTICAL
        }

        // Apply common styling to all SnackBars
        private fun applyCommonStyling(view: View) {
            // Round corners
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.cornerRadius = 16f

            // Add elevation/shadow
            ViewCompat.setElevation(view, 12f)

            // Adjust margins
            val params = view.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(24, 0, 24, 24)
            view.layoutParams = params

            // Customize text appearance
            val textView = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            textView.textSize = 14f
            textView.typeface = Typeface.DEFAULT_BOLD
            textView.maxLines = 2
        }

        // Create SnackBar with action button
        fun makeWithAction(
            view: View,
            message: String,
            actionText: String,
            actionColor: Int = Color.WHITE,
            action: () -> Unit,
            type: SnackBarType = SnackBarType.INFO,
            duration: Int = Snackbar.LENGTH_LONG
        ): Snackbar {

            val snackbar = make(view, message, duration, type)

            snackbar.setAction(actionText) { action() }
            snackbar.setActionTextColor(actionColor)

            // Style action button
            val actionButton = snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_action)
            actionButton.typeface = Typeface.DEFAULT_BOLD
            actionButton.textSize = 14f

            return snackbar
        }

        // Animated SnackBar with slide-in effect
        fun makeAnimated(
            view: View, message: String, type: SnackBarType = SnackBarType.INFO, duration: Int = Snackbar.LENGTH_SHORT
        ): Snackbar {

            val snackbar = make(view, message, duration, type)

            // Add custom animation
            snackbar.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                override fun onShown(transientBottomBar: Snackbar?) {
                    super.onShown(transientBottomBar)
                    transientBottomBar?.view?.let { snackbarView ->
                        snackbarView.alpha = 0f
                        snackbarView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(300).start()
                    }
                }
            })

            return snackbar
        }

        // Apply rounded corners to SnackBar
        private fun applyRoundedCorners(view: View, backgroundColor: Int) {
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.setColor(backgroundColor)
            drawable.cornerRadius = 24f // Increased corner radius for more rounded appearance
            view.background = drawable
        }
    }
}


/*
// Extension functions for easier usage
fun View.showSuccessSnackBar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    CustomSnackBar.make(this, message, duration, CustomSnackBar.SnackBarType.SUCCESS).show()
}

fun View.showErrorSnackBar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    CustomSnackBar.make(this, message, duration, CustomSnackBar.SnackBarType.ERROR).show()
}

fun View.showWarningSnackBar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    CustomSnackBar.make(this, message, duration, CustomSnackBar.SnackBarType.WARNING).show()
}

fun View.showInfoSnackBar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    CustomSnackBar.make(this, message, duration, CustomSnackBar.SnackBarType.INFO).show()
}

fun View.showCustomSnackBar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    CustomSnackBar.make(this, message, duration, CustomSnackBar.SnackBarType.CUSTOM).show()
}*/
