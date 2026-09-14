package com.test.healthbox_app.presentation.tests.hba1c

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.test.healthbox_app.BleConnectionViewModel
import com.test.healthbox_app.MainActivity
import com.test.healthbox_app.R
import com.test.healthbox_app.base.BaseFragment
import com.test.healthbox_app.bluetooth.DeviceType
import com.test.healthbox_app.data.model.BodyCheckupPref
import com.test.healthbox_app.data.model.TestFlowType
import com.test.healthbox_app.databinding.Hba1cTestFragmentBinding
import com.test.healthbox_app.domain.model.BleDevice
import com.test.healthbox_app.domain.model.ConnectionState
import com.test.healthbox_app.domain.model.Hba1cMeasurement
import com.test.healthbox_app.domain.model.ScanState
import com.test.healthbox_app.presentation.dialog.DeviceListDialog
import com.test.healthbox_app.presentation.util.CustomSnackBar
import com.test.healthbox_app.presentation.view.deviceStatus.DeviceStatusViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * HbA1c test screen (A1cEZ 2.0 meter).
 *
 * Flow: scan or auto-connect a saved meter -> enable notifications -> the operator runs
 * the test physically on the meter -> the meter pushes a 49-byte record -> display.
 *
 * The app cannot start the measurement: the vendor manual documents no start command,
 * so "Start" here means "connect and begin listening". The cycle takes several minutes,
 * which is why this screen shows an explicit status line and holds the screen awake
 * instead of sitting behind a modal spinner.
 *
 * Deliberate differences from BloodSugarTestFragment, which this is otherwise modelled on:
 *  - connection state is filtered to HBA1C_METER instead of iterating the whole map
 *  - no TODO() branches (they throw NotImplementedError)
 *  - no !! on selectedDevice / selectedDeviceType
 *  - listening is started once per connection, never stacked
 *  - the GATT connection is always torn down in onDestroyView
 */
@AndroidEntryPoint
class Hba1cTestFragment : BaseFragment() {

    private lateinit var binding: Hba1cTestFragmentBinding
    private lateinit var bleConnectionViewModel: BleConnectionViewModel

    private val deviceStatusViewModel: DeviceStatusViewModel by activityViewModels()

    private var deviceListDialog: DeviceListDialog? = null

    /** Guards against stacking notification collectors on repeated Connected emissions. */
    private var isListening = false

    /** Last accepted record index, so a replayed history dump is not shown as fresh. */
    private var lastRecordNum: Long = -1L

    companion object {
        const val TAG = "Hba1cTestFragment"

        /** Records older than this are treated as history, not as this session's result. */
        private const val MAX_RECORD_AGE_MS = 15 * 60 * 1000L

        /**
         * Verbatim from the BioHermes A1C EZ 2.0 User's Manual, section VI "Trouble
         * shooting" (LCD-only — never sent over BLE, see [BleConnectionViewModel.hba1cError]).
         * Shown in-app so the operator doesn't need the printed manual to decode what the
         * meter's screen is telling them.
         */
        private val ERROR_CODE_REFERENCE = listOf(
            "Can not turn on" to "Battery damaged or too low — replace the batteries.",
            "E-0" to "Software problem — contact local distributor.",
            "E-1" to "Hardware problem — contact local distributor.",
            "E-2" to "High temperature — change the testing environment.",
            "E-3" to "Used strip inserted — change to a new strip.",
            "E-4" to "Testing overtime — follow the operating steps in order.",
            "E-5" to "Strip removed during testing — retest.",
            "E-6" to "Operational error — follow the operating steps in order.",
            "E-7" to "Delay in adding the blood sample — retest, add blood promptly after the prompt.",
            "E-8" to "Delay in adding buffer B — retest, add buffer B promptly after the prompt.",
            "Low battery symbol" to "Replace the batteries.",
            "Blinking CODE" to "No code chip inserted — insert the chip matching the strip's code.",
            "HI" to "Result above the high limit — retest with a new strip; if it persists, consult a doctor.",
            "LO" to "Result below the low limit — retest with a new strip; if it persists, consult a doctor."
        )
    }

    override fun checkConnectivity() {}

    override val isConnected: Unit = Unit

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = Hba1cTestFragmentBinding.inflate(inflater)

        bleConnectionViewModel = (activity as MainActivity).bleViewModel
        binding.viewModel = bleConnectionViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.userInfoView.loadPatientFromPref()

        initializeDeviceStatus()
        setupDialog()
        setupButtons()
        observeScanState()
        observeConnectionState()
        observeMeasurement()
        observeHba1cError()
        setAndObserveDeviceAvailability()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // The HbA1c cycle runs for minutes; let the operator watch it without the
        // tablet sleeping mid-test. Cleared again in onDestroyView.
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    CustomSnackBar.makeWithAction(
                        binding.root,
                        "Are you sure to go back, It will clear the HbA1c result?",
                        "Yes",
                        Color.WHITE,
                        action = {
                            // Abandoning the attempt — going straight to Dashboard, not
                            // Results, so nothing will ever consume/clear this otherwise.
                            bleConnectionViewModel.setTestFlow(null)
                            findNavController().navigate(R.id.close_button_action_hba1c_screen)
                        },
                        duration = 5000,
                        type = CustomSnackBar.Companion.SnackBarType.CUSTOM
                    ).show()
                }
            })
    }

    // ── Saved device / auto-connect ───────────────────────────────────────────

    private fun setAndObserveDeviceAvailability() {
        // BodyCheckupPref is a process-wide singleton and clearAll() only runs at the end
        // of the basic health checkup, never in this standalone flow. Clear the slot on
        // entry so a value left over from an earlier session — possibly a different
        // patient — can never be re-saved as this patient's result.
        BodyCheckupPref.hba1c = null

        // Tells ResultsFragment (same activity-scoped ViewModel) which flow produced
        // the data it should show and which API to call. Cleared on the cancel path
        // below (no Results screen will read it in that case) and on Results' own
        // Home button once it's been consumed — never cleared here on success, or
        // Results would read null before it gets a chance to.
        bleConnectionViewModel.setTestFlow(TestFlowType.HBA1C)

        bleConnectionViewModel.setDeviceType(DeviceType.HBA1C_METER)
        bleConnectionViewModel.getDevice()

        binding.deviceStatusLayout.setUpDeviceAvailability(
            bleConnectionViewModel.selectedDevice.value != null
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                bleConnectionViewModel.selectedDevice.collect { device ->
                    if (device == null) return@collect
                    binding.deviceStatusLayout.setUpDeviceAvailability(true)
                    connectTo(device)
                }
            }
        }
    }

    /**
     * Deliberately does not call showDialog() itself — whether a loader shows depends
     * on *why* this is being called (silent launch-time auto-connect vs. a user tapping
     * Start or picking a device from the scan dialog), which only the caller knows.
     * Each caller shows its own loader before calling this, where one is wanted.
     */
    private fun connectTo(device: BleDevice) {
        if (bleConnectionViewModel.selectedDeviceType.value != DeviceType.HBA1C_METER) {
            bleConnectionViewModel.setDeviceType(DeviceType.HBA1C_METER)
        }

        // repeatOnLifecycle re-collects the selectedDevice StateFlow every time the screen
        // returns to STARTED, so without this guard backgrounding the app mid-test would
        // tear down a healthy GATT connection and reconnect for no reason.
        if (bleConnectionViewModel.connectionState.value[DeviceType.HBA1C_METER]
            == ConnectionState.Connected
        ) {
            // The GATT link never dropped (a timed-out/errored test doesn't disconnect the
            // meter), so onMeterConnected() won't fire again on its own to restart listening.
            // Retest relies on this path to actually do something in that case.
            Log.d(TAG, "Meter already connected — restarting listener directly")
            isListening = false
            onMeterConnected()
            return
        }

        bleConnectionViewModel.connectToDevice(device, DeviceType.HBA1C_METER)
    }

    // ── Scanning + device selection ───────────────────────────────────────────

    private fun initializeDeviceStatus() {
        binding.deviceStatusLayout.bindViewModel(deviceStatusViewModel)
        binding.deviceStatusLayout.setOnScanClickListener { deviceStatusViewModel.startScan() }
    }

    private fun setupDialog() {
        val activity = mActivity ?: return

        deviceListDialog = DeviceListDialog(activity, onDeviceClose = {
            deviceListDialog?.dismissDialog()
        }).apply {
            setOnItemSelectedListener(object : DeviceListDialog.OnItemClickListener {
                override fun onItemSelect(unit: BleDevice?) {
                    val device = unit ?: return
                    Log.i(TAG, "HbA1c device selected: ${device.name} ${device.address}")

                    deviceStatusViewModel.stopBleScan()

                    // User-initiated connect (picked from the scan dialog) — show the
                    // loader, same as the Start-button-with-known-device path.
                    showDialog()

                    // Pin the type: SharedPreferencesManager keys off BleDevice.deviceType,
                    // so a meter the scanner failed to classify would otherwise be saved
                    // under a null key and never found again.
                    bleConnectionViewModel.updateSelectedDevice(
                        device.copy(deviceType = DeviceType.HBA1C_METER)
                    )
                }
            })
        }
    }

    private fun observeScanState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                deviceStatusViewModel.scanState.collect { state ->
                    when (state) {
                        is ScanState.Idle -> hideDialog()

                        is ScanState.Scanning -> showDialog()

                        is ScanState.DevicesFound -> {
                            hideDialog()
                            if (state.devices.isNotEmpty()) {
                                deviceListDialog?.showDialog(state.devices)
                            } else {
                                showSnackBar(
                                    binding.root,
                                    "No devices found",
                                    CustomSnackBar.Companion.SnackBarType.ERROR
                                )
                                // deviceStatusViewModel is activity-scoped (shared across every
                                // test screen), so this terminal empty result must be consumed
                                // or it replays the same message on the next screen/subscribe.
                                deviceStatusViewModel.clearScanState()
                            }
                        }

                        is ScanState.Error -> {
                            hideDialog()
                            showSnackBar(
                                binding.root,
                                "Scan error: ${state.message}",
                                CustomSnackBar.Companion.SnackBarType.ERROR
                            )
                        }

                        is ScanState.Connecting, is ScanState.Connected -> Unit
                    }
                }
            }
        }
    }

    // ── Connection ────────────────────────────────────────────────────────────

    private fun observeConnectionState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                bleConnectionViewModel.connectionState.collect { stateMap ->
                    // Only react to our own meter — the printer or another test's device
                    // changing state must not touch this screen.
                    val state = stateMap[DeviceType.HBA1C_METER] ?: return@collect

                    when (state) {
                        is ConnectionState.Connected -> onMeterConnected()

                        is ConnectionState.Disconnected -> onMeterDisconnected(null)

                        is ConnectionState.Error -> onMeterDisconnected(state.message)

                        // Never TODO() here: Paired/PairedFailed belong to the classic
                        // Bluetooth printer flow and are unreachable for a BLE meter,
                        // but a thrown NotImplementedError would crash the screen.
                        is ConnectionState.Connecting,
                        is ConnectionState.Paired,
                        is ConnectionState.PairedFailed -> Unit
                    }
                }
            }
        }
    }

    private fun onMeterConnected() {
        hideDialog()
        deviceStatusViewModel.updateScannedDevicesList()
        deviceListDialog?.dismissDialog()

        bleConnectionViewModel.selectedDevice.value?.let { device ->
            bleConnectionViewModel.saveDevice(device.copy(deviceType = DeviceType.HBA1C_METER))
        }

        mActivity?.let { binding.deviceStatusLayout.setupDeviceStatus(it, true) }

        // Start the notification collector exactly once per connection. Calling
        // getHba1cData() on every emission of the state map would stack collectors and
        // feed each fragment to the stateful parser more than once.
        if (!isListening) {
            isListening = true
            bleConnectionViewModel.getHba1cData()
            binding.tvHba1cStatus.text = getString(R.string.hba1c_status_connected)
        }

        binding.buttonStart.visibility = View.GONE
    }

    private fun onMeterDisconnected(errorMessage: String?) {
        isListening = false
        hideDialog()

        mActivity?.let { binding.deviceStatusLayout.setupDeviceStatus(it, false) }
        binding.tvHba1cStatus.text = getString(R.string.hba1c_status_disconnected)

        showStartOrRetest()

        if (errorMessage != null) {
            CustomSnackBar.make(
                binding.root, errorMessage, Snackbar.LENGTH_SHORT,
                CustomSnackBar.Companion.SnackBarType.ERROR
            ).show()
        }
    }

    private fun showStartOrRetest() {
        val hasResult = binding.editHba1c.text?.isNotBlank() == true
        binding.buttonStart.visibility = if (hasResult) View.GONE else View.VISIBLE
        binding.buttonRetest.visibility = if (hasResult) View.VISIBLE else View.GONE
    }

    // ── Result ────────────────────────────────────────────────────────────────

    private fun observeMeasurement() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                bleConnectionViewModel.hba1cMeasurement.collect { measurement ->
                    val m = measurement ?: return@collect

                    // The A1cEZ reports glucose from the same characteristic. A glucose
                    // record here is expected, not an error — drop it silently.
                    if (m.isGlucoseRecord) {
                        Log.d(TAG, "Ignoring glucose record from HbA1c meter")
                        return@collect
                    }

                    if (!m.isValid) {
                        Log.w(TAG, "Discarding implausible HbA1c record: ${m.value} ${m.unit}")
                        return@collect
                    }

                    if (!isFreshRecord(m)) {
                        Log.d(TAG, "Ignoring replayed record #${m.uniqueRecordNum}")
                        return@collect
                    }

                    lastRecordNum = m.uniqueRecordNum
                    showResult(m)
                }
            }
        }
    }

    /**
     * The meter never reports *why* a test failed over BLE — no error code, no status
     * byte, nothing (confirmed against both the BLE dev manual and the meter's own user
     * manual: E-codes/HI/LO exist only on its LCD). So this only ever fires from
     * [BleConnectionViewModel]'s timeout — "no result showed up in time" — not from any
     * value the device sent. Point the operator at the meter's screen instead of
     * pretending the app knows what went wrong.
     */
    private fun observeHba1cError() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                bleConnectionViewModel.hba1cError.collect { message ->
                    val error = message ?: return@collect

                    // One-shot: consume it immediately so backgrounding/resuming the
                    // screen doesn't replay the same message again.
                    bleConnectionViewModel.clearHba1cError()

                    hideDialog()
                    isListening = false  // let a later connectTo() restart the listener
                    showStartOrRetest()

                    CustomSnackBar.makeWithAction(
                        binding.root, error, "View Codes", Color.WHITE,
                        action = { showErrorCodeReference() },
                        type = CustomSnackBar.Companion.SnackBarType.ERROR,
                        duration = Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /** Rejects duplicates and history the meter may replay when it connects. */
    private fun isFreshRecord(m: Hba1cMeasurement): Boolean {
        if (m.uniqueRecordNum == lastRecordNum) return false
        if (m.deviceTimeMillis <= 0L) return true  // meter clock unreadable — accept
        val age = System.currentTimeMillis() - m.deviceTimeMillis
        return age <= MAX_RECORD_AGE_MS && age >= -MAX_RECORD_AGE_MS
    }

    private fun showResult(m: Hba1cMeasurement) {
        hideDialog()

        val ngsp = m.ngspPercent ?: m.value

        binding.layoutHba1cInstructions.visibility = View.GONE
        binding.layoutHba1cResult.visibility = View.VISIBLE

        binding.editHba1c.setText(String.format(Locale.US, "%.1f", ngsp))
        binding.tvHba1cUnit.text = getString(R.string.hba1c_unit_ngsp)
        binding.tvHba1cResult.text = BodyCheckupPref.classifyHba1c(ngsp).orEmpty()
        binding.tvHba1cStatus.text = getString(R.string.hba1c_status_connected)

        binding.buttonStart.visibility = View.GONE
        binding.buttonRetest.visibility = View.VISIBLE

        Log.d(TAG, "HbA1c result ${"%.1f".format(ngsp)}% (record #${m.uniqueRecordNum}, meter ${m.meterId})")
    }

    // ── Buttons ───────────────────────────────────────────────────────────────

    private fun setupButtons() {
        binding.buttonStart.setOnClickListener { startOrScan() }

        binding.buttonRetest.setOnClickListener {
            // Clear the previous reading so a stale value cannot be saved by mistake.
            binding.editHba1c.setText("")
            binding.tvHba1cResult.text = ""
            binding.layoutHba1cResult.visibility = View.INVISIBLE
            binding.layoutHba1cInstructions.visibility = View.VISIBLE
            startOrScan()
        }

        binding.buttonNextLayout.setOnClickListener {
            saveHba1cData()
            teardown()
            // testFlow stays HBA1C across this navigation — ResultsFragment (same
            // activity-scoped ViewModel) reads it, and clears it once consumed.
            mActivity?.navController?.navigate(R.id.next_button_action_hba1c_screen)
        }

        binding.buttonHba1cHelp.setOnClickListener { showErrorCodeReference() }

        // Manual toggle to the result panel — same visibility swap showResult() does,
        // without needing a real decoded measurement.
        binding.ivHba1cIcon.setOnClickListener {
            binding.layoutHba1cInstructions.visibility = View.GONE
            binding.layoutHba1cResult.visibility = View.VISIBLE
        }
    }

    /**
     * Reference dialog for the meter's own LCD-only error codes. See
     * [ERROR_CODE_REFERENCE] for the source and why the app can't read these itself.
     */
    private fun showErrorCodeReference() {
        val activity = mActivity ?: return

        // Code + meaning on one line (not "$code\n$meaning") so each entry reads as a
        // single row rather than wrapping mid-sentence; the wider dialog below is what
        // actually keeps it from wrapping on a tablet-sized screen.
        val message = ERROR_CODE_REFERENCE.joinToString("\n\n") { (code, meaning) ->
            "$code — $meaning"
        }

        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.hba1c_help_title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()

        // A plain AlertDialog's themed width was too narrow for the longest line (it
        // wrapped); a fixed 90%-of-screen width fixed that but left dead space on every
        // shorter line. What's actually wanted is "as wide as the longest line needs" —
        // so measure the dialog UNSPECIFIED (its natural, unwrapped width) and use that,
        // falling back to a screen-width cap only if it doesn't fit (e.g. narrow/portrait).
        dialog.window?.let { window ->
            val decorView = window.decorView
            decorView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val maxWidth = (resources.displayMetrics.widthPixels * 0.95f).toInt()
            val naturalWidth = decorView.measuredWidth
            window.setLayout(minOf(naturalWidth, maxWidth), WindowManager.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun startOrScan() {
        ensureBluetoothEnabled(binding.root) {
            val device = bleConnectionViewModel.selectedDevice.value
            if (device == null) {
                deviceStatusViewModel.startScan()
                return@ensureBluetoothEnabled
            }
            binding.tvHba1cStatus.text = getString(R.string.hba1c_status_waiting)
            showDialog()
            connectTo(device)
        }
    }

    private fun saveHba1cData() {
        val value = binding.editHba1c.text?.toString()?.trim()
        BodyCheckupPref.hba1c = if (value.isNullOrEmpty()) null else value
        Log.d(TAG, "Saved HbA1c=${BodyCheckupPref.hba1c} result=${BodyCheckupPref.hba1c_result}")
    }

    // ── Teardown ──────────────────────────────────────────────────────────────

    /**
     * Releases the GATT connection and the notification collector. Android allows only
     * a handful of concurrent GATT clients, so leaking one here would eventually make
     * *other* tests fail to connect.
     */
    private fun teardown() {
        isListening = false

        bleConnectionViewModel.stopHba1cListening()

        bleConnectionViewModel.selectedDevice.value?.let { device ->
            bleConnectionViewModel.disconnect(device, DeviceType.HBA1C_METER)
        }

        deviceStatusViewModel.stopBleScan()
        deviceListDialog?.dismissDialog()

        bleConnectionViewModel.setSelectedDevice(null)
        bleConnectionViewModel.setDeviceType(null)
    }

    override fun onDestroyView() {
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        teardown()
        deviceListDialog = null
        super.onDestroyView()
    }
}
