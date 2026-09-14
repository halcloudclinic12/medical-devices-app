package com.test.healthbox_app.data.model

/**
 * Which test flow is currently in progress / just completed, tracked for the
 * duration of the session on [com.test.healthbox_app.BleConnectionViewModel]
 * (activity-scoped, shared with [com.test.healthbox_app.presentation.tests.results.ResultsFragment]
 * the same way [com.test.healthbox_app.bluetooth.DeviceType] selection already is).
 *
 * Deliberately separate from [ReportTestType] — that one drives the Reports-screen
 * filter list and its instances carry mutable-via-`copy()` UI selection state
 * (`isSelected`), which is a UI-list concern, not session state. This enum exists
 * only to answer "which flow produced the data now in BodyCheckupPref", so
 * ResultsFragment knows which API to call and which parameter list to display.
 *
 * [apiCode] matches the values [ReportTestType.testType] already uses ("BASIC",
 * "HBA1C"), so it can be passed straight into PdfOpener.buildUrl(testType = ...)
 * without a lookup.
 */
enum class TestFlowType(val apiCode: String) {
    BASIC("BASIC"),
    HBA1C("HBA1C")
}