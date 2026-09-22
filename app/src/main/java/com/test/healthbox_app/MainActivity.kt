package com.test.healthbox_app

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.test.healthbox_app.base.BaseActivity
import com.test.healthbox_app.databinding.MainActivityBinding
import com.test.healthbox_app.domain.model.StepItem
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private lateinit var binding: MainActivityBinding

    val bleViewModel: BleConnectionViewModel by viewModels()

    var navController: NavController? = null

    // ── Universal "Bluetooth is off" gate ──────────────────────────────────────
    //
    // Every Bluetooth operation anywhere in the app (BLE scan, BLE connect, classic
    // Bluetooth printer, ...) should call ensureBluetoothEnabled() first instead of
    // assuming the adapter is on. This is the single place that can show the system
    // "Turn on Bluetooth?" dialog, since only an Activity can register an
    // ActivityResultLauncher, and MainActivity is the only Activity in this app.

    private var pendingBluetoothEnableCallback: ((Boolean) -> Unit)? = null

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val adapter = bluetoothAdapter()
        // Some OEM builds report RESULT_CANCELED from this dialog even though the user
        // accepted and the adapter did turn on, so trust the adapter's own state too.
        val enabled = result.resultCode == Activity.RESULT_OK || adapter?.isEnabled == true
        pendingBluetoothEnableCallback?.invoke(enabled)
        pendingBluetoothEnableCallback = null
    }

    private val bluetoothConnectPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchEnableBluetoothDialog()
        } else {
            pendingBluetoothEnableCallback?.invoke(false)
            pendingBluetoothEnableCallback = null
        }
    }

    private fun bluetoothAdapter(): BluetoothAdapter? =
        (getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    /**
     * Ensures Bluetooth is on before a Bluetooth operation runs.
     *  - Already on: [onResult] fires immediately with `true`.
     *  - Off: shows the system "Turn on Bluetooth?" prompt (requesting the BLUETOOTH_CONNECT
     *    runtime permission first on Android 12+ if it isn't granted yet) and [onResult] fires
     *    once with the outcome — `true` only if the user accepted.
     *  - No adapter on this device: fires `false` immediately.
     *
     * Only one request is tracked at a time; a second call before the first resolves replaces
     * the pending callback, which is fine since the system dialog is modal.
     */
    fun ensureBluetoothEnabled(onResult: (Boolean) -> Unit) {
        val adapter = bluetoothAdapter()
        if (adapter == null) {
            onResult(false)
            return
        }
        if (adapter.isEnabled) {
            onResult(true)
            return
        }

        pendingBluetoothEnableCallback = onResult

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            bluetoothConnectPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            launchEnableBluetoothDialog()
        }
    }

    private fun launchEnableBluetoothDialog() {
        try {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } catch (e: SecurityException) {
            pendingBluetoothEnableCallback?.invoke(false)
            pendingBluetoothEnableCallback = null
        }
    }

    interface onBackPressListener {
        fun onBackPress()
    }

    private var monBackPressListener: onBackPressListener? = null

    fun registerOnBackPress(monBackPressListener: onBackPressListener?) {
        this.monBackPressListener = monBackPressListener
    }

    fun unRegisterOnBackPress() {
        this.monBackPressListener = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔒 Enforce landscape for this activity
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        // Or: ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Optional: Setup NavController
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.my_nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        hideKeyboard()

        // Initialize steps
        val steps = listOf(
            StepItem(1, "Height", R.drawable.ic_height),
            StepItem(2, "Analysis", R.drawable.ic_height),
            StepItem(3, "Temperature", R.drawable.ic_height),
            StepItem(4, "SPO2", R.drawable.ic_height),
            StepItem(5, "Pressure", R.drawable.ic_height),
            StepItem(6, "Blood Sugar", R.drawable.ic_height)
        )

//        stepNavigationViewModel.initializeSteps(steps)

        /*AILinkSDK.getInstance().init(this)

        AILinkBleManager.getInstance().init(this, object : onInitListener {
            override fun onInitSuccess() {

                Log.e("AILinkLogsMain","   ::  Success  :")
                //Initialization successful,
//                initBleOk()
            }

            override fun onInitFailure() {
                Log.e("AILinkLogsMain","   ::  failed  :")

            }
        })*/


    }

    /*override fun onResume() {
        super.onResume()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }*/

    override fun networkIsAvailable() {
    }

    fun hideKeyboard() {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }

    override fun onBackPressed() {
        // Was calling super.onBackPressed() unconditionally above AND again in the `else`
        // branch below (monBackPressListener is never set - nothing in the codebase calls
        // registerOnBackPress()/unRegisterOnBackPress() - so that branch always ran). Each
        // call dispatches through onBackPressedDispatcher, which pops/invokes the current
        // NavHostFragment back-stack entry or fragment callback - so one physical back press
        // was popping the nav back stack TWICE. That's what made the previous screen (e.g. a
        // stale Dashboard instance still sitting underneath) visibly flash on screen before
        // the second pop/finish took over.
        try {
            if (monBackPressListener != null) {
                monBackPressListener!!.onBackPress()
            } else {
                super.onBackPressed()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


/*

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    HealthboxappTheme {
        Greeting("Android")
    }
}*/
