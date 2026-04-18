package com.test.healthbox_app.domain.model

import com.prowess.sdk.Printer

data class PrintJob(
    val message: String,
    val fontType: Byte = Printer.PR_FONTLARGENORMAL
)
