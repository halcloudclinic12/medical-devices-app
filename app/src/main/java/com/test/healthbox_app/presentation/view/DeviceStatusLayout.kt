package com.test.healthbox_app.presentation.view

import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.test.healthbox_app.MainActivity
import com.test.healthbox_app.R
import com.test.healthbox_app.databinding.DeviceStatusDialogBinding
import com.test.healthbox_app.databinding.DeviceStatusLayoutBinding
import com.test.healthbox_app.presentation.view.deviceStatus.DeviceStatusViewModel

class DeviceStatusLayout : ConstraintLayout {

    private lateinit var binding: DeviceStatusLayoutBinding
    private var deviceStatusViewModel: DeviceStatusViewModel? = null

    private var scanListener: OnClickListener? = null

    // Gates the scan tap while a connection attempt is in flight, so a second tap during
    // "Connecting…" can't fire a fresh scan on top of it.
    private var isConnecting = false

    // Last real connection state (null = no device known yet), so a failed re-scan-to-switch
    // attempt (clearConnecting) can restore exactly what was on screen before it, instead of
    // guessing "Disconnected" and overwriting an already-connected device's status. Also what
    // the status dialog reads to render its content when opened.
    private var lastKnownConnected: Boolean? = null
    private var lastKnownDeviceName: String? = null

    // Constructor with three parameters (most comprehensive)
    constructor(context: Context) : super(context) {
        initializeLayout(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initializeLayout(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initializeLayout(context)
    }

    private fun initializeLayout(context: Context) {
        binding = DataBindingUtil.inflate(
            LayoutInflater.from(context), R.layout.device_status_layout, this, true
        )

        // Just an icon + status dot in the header - tap opens the status dialog with the
        // actual detail and the scan action. Not gated on Bluetooth here: looking at status
        // shouldn't prompt a Bluetooth-enable dialog, only actually starting a scan should
        // (see the dialog's own scan button, wired in showStatusDialog()).
        binding.deviceStatusLayout.setOnClickListener {
            showStatusDialog()
        }
    }

    // Add this function to bind the view model from the fragment
    fun bindViewModel(viewModel: DeviceStatusViewModel) {

        this.deviceStatusViewModel = viewModel

        // DeviceStatusViewModel is activity-scoped/shared across every test screen. A scan
        // left running by whichever screen the patient was on before this one (dismissed
        // dialog, navigated away mid-scan) would otherwise keep going and could still
        // deliver a result here that this screen never asked for. Every screen calls
        // bindViewModel() once, on entry, before it starts observing scan state - so
        // stopping any stale scan right here is the one place this needs to happen, instead
        // of every screen having to track its own "did I ask for this" flag.
        viewModel.stopBleScan()

        binding.viewModel = viewModel
        binding.lifecycleOwner = findViewTreeLifecycleOwner() // Important for LiveData observation
        binding.executePendingBindings()
    }

    fun setUpDeviceAvailability(isDeviceAvailable: Boolean) {

        Log.e("setUpdevAvaiLog", " : $isDeviceAvailable")

        isConnecting = false

        if (!isDeviceAvailable) {
            lastKnownConnected = null
            recolorDot(R.color.red)
        }
        // isDeviceAvailable == true carries no color info by itself; the ConnectionState
        // observer's setupDeviceStatus() call that follows sets the real connected/red color.
    }

    /** A connection attempt is in flight — the gap between picking a device from the scan
     *  list (which dismisses immediately) and ScanState.Connected/Error landing. Previously
     *  invisible: the badge just sat frozen on its old state through this whole window. */
    fun setConnecting(context: Context) {
        isConnecting = true
        recolorDot(R.color.orbit_next)
    }

    /** Reverts a "Connecting…" badge after a failed attempt (ScanState.Error) back to
     *  whatever was really on screen before it — a re-scan-to-switch attempt that errors
     *  out must not report an already-connected device as disconnected. No-ops if the
     *  widget wasn't actually in the connecting state. */
    fun clearConnecting(context: Context) {
        if (!isConnecting) return

        val connected = lastKnownConnected
        if (connected != null) {
            setupDeviceStatus(context, connected, lastKnownDeviceName)
        } else {
            setUpDeviceAvailability(false)
        }
    }

    fun setupDeviceStatus(context: Context, isDeviceConnected: Boolean, deviceName: String? = null) {
        // A transient "disconnected" blip from the BLE stack while a connection is still
        // settling (some adapters report this before the GATT connection completes) must
        // not stomp the "Connecting…" badge back to "Disconnected" mid-attempt. A real
        // failure arrives as ScanState.Error and routes through clearConnecting instead.
        if (isConnecting && !isDeviceConnected) return

        isConnecting = false
        lastKnownConnected = isDeviceConnected
        lastKnownDeviceName = deviceName

        recolorDot(if (isDeviceConnected) R.color.green else R.color.red)
    }

    // The status dot is the only thing on the tiny badge that carries state color - same
    // "online dot" language as UserInfoView's avatar. Solid shape compiles to a
    // GradientDrawable so it can be recolored directly.
    //
    // mutate() matters here: Android caches one Drawable instance per resource id and
    // shares it across every view that references it, so calling setColor() on a shared
    // drawable recolors it everywhere it's used, not just this one dot. bg_status_dot is
    // only ever used by this single view today, but mutate() is what makes that a real
    // guarantee instead of an accident of nobody else having reused the drawable yet (see
    // bg_circle_status_dialog's comment for what happens when that assumption breaks).
    private fun recolorDot(colorRes: Int) {
        val color = resources.getColor(colorRes)
        val drawable = binding.statusDot.background as? GradientDrawable ?: return
        (drawable.mutate() as GradientDrawable).setColor(color)
    }

    /** Centered status dialog, built fresh on each tap so its content always reflects the
     *  current isConnecting/lastKnownConnected/lastKnownDeviceName. Reuses the exact window
     *  setup already proven in ItemListDialog (transparent window background + wrap-content
     *  height + screen dim) rather than a PopupWindow, which would clip against this icon's
     *  position in the far corner and wouldn't dim the screen like every other dialog here. */
    private fun showStatusDialog() {
        val dialog = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
        val dialogBinding = DeviceStatusDialogBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(dialogBinding.root)

        dialog.window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setGravity(Gravity.CENTER)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(0.45f)
        }

        val connected = lastKnownConnected
        val statusColorRes = when {
            isConnecting -> R.color.orbit_next
            connected == true -> R.color.green
            else -> R.color.red
        }
        val statusColor = resources.getColor(statusColorRes)
        (dialogBinding.dialogIconFrame.background as? GradientDrawable)?.let {
            (it.mutate() as GradientDrawable).setColor(statusColor)
        }

        dialogBinding.tvDialogStatus.text = when {
            isConnecting -> "Connecting…"
            connected == true -> "Connected"
            else -> "Disconnected"
        }

        // "Scan for Device" reads as "nothing is connected yet" - misleading when a device
        // already is connected and this button is really offering to switch to a different
        // one, so the label reflects which case it actually is.
        dialogBinding.btnDialogScan.text = context.getString(
            if (connected == true) R.string.device_status_change_cta else R.string.device_status_scan_cta
        )

        if (connected == true && !lastKnownDeviceName.isNullOrBlank()) {
            dialogBinding.tvDialogDeviceName.text = lastKnownDeviceName
            dialogBinding.tvDialogDeviceName.visibility = View.VISIBLE
        } else {
            dialogBinding.tvDialogDeviceName.visibility = View.GONE
        }

        // Can't start a second scan while one attempt is already in flight.
        dialogBinding.btnDialogScan.isEnabled = !isConnecting
        dialogBinding.btnDialogScan.alpha = if (isConnecting) 0.5f else 1f

        dialogBinding.btnDialogScan.setOnClickListener {
            dialog.dismiss()

            // Same Bluetooth-off gate every screen's scan action routes through (see the
            // previous inline version of this listener) — moved here from the icon tap so
            // just opening the dialog to look at status never prompts a Bluetooth dialog.
            val activity = findHostActivity()
            if (activity == null) {
                scanListener?.onClick(it)
                return@setOnClickListener
            }
            activity.ensureBluetoothEnabled { enabled ->
                if (enabled) {
                    scanListener?.onClick(it)
                } else {
                    Snackbar.make(this, "Bluetooth is turned off. Please turn it on to scan for devices.", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        dialogBinding.ivCloseStatusDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // Add a method to set up click listeners that can be configured from the fragment.
    // The listener now fires from the status dialog's "Scan for Device" button, not the
    // icon tap directly (see showStatusDialog()) — the icon tap only opens the dialog.
    fun setOnScanClickListener(listener: OnClickListener) {
        this.scanListener = listener
    }

    /** A custom view's `context` is usually the hosting Activity, but data-binding inflation
     *  can hand back a themed ContextWrapper around it, so unwrap defensively. */
    private fun findHostActivity(): MainActivity? {
        var ctx: Context? = context
        while (ctx != null) {
            if (ctx is MainActivity) return ctx
            ctx = (ctx as? ContextWrapper)?.baseContext
        }
        return null
    }
}
