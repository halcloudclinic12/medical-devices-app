package com.test.healthbox_app

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
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
        super.onBackPressed()
        try {
            if (monBackPressListener != null) {
                monBackPressListener!!.onBackPress()
            } else {
                super.onBackPressed()
            }
        } catch (e: Exception) {
            e.printStackTrace()
//            super.onBackPressed();
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
