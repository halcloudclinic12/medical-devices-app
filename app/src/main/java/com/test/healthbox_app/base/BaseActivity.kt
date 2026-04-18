package com.test.healthbox_app.base

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.test.healthbox_app.presentation.util.NetConnection

abstract class BaseActivity : AppCompatActivity() {
    protected abstract fun networkIsAvailable()

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun attachBaseContext(newBase: Context) {
        // Apply the updated configuration with the new locale
        super.attachBaseContext(newBase)
    }

    protected fun checkConnectivity() {
        if (NetConnection.checkConnection(this)) {
            Log.d("Checking internet", "devic123")
            networkIsAvailable()
        } else {
            showNoInternetConnectionDialog()
            //Toast.makeText(this, "Connect Your Network", Toast.LENGTH_SHORT).show();
        }
    }

    fun showNoInternetConnectionDialog() {
        Log.e("Testing net Connection", "Entering showNoInternetConnectionDialog Method")
        val builder = AlertDialog.Builder(this)
        builder.setMessage("getString(R.string.wh)")
            .setTitle("")
            .setCancelable(false)
            .setNeutralButton("Retry") { dialog, id -> checkConnectivity() }
        val alert = builder.create()
        alert.show()
        //  Log.e("Testing net Connection", "Showed NoIntenetConnectionDialog");
    }

    protected fun replaceFragment(fragment: Fragment, isAddToBackStack: Boolean): Fragment {
        /*  FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.viewHolder, fragment);
        if (isAddToBackStack)
            fragmentTransaction.addToBackStack(fragment.toString());
        fragmentTransaction.commit();*/
        return fragment
    }

    private fun buildAlertMessageNoGps() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id -> startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 1001) }
            .setNegativeButton("No") { dialog, id -> dialog.cancel() }
        val alert = builder.create()
        alert.show()
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("TAG", "onRequestPermissionsResult:    given")
            } else {
                checkRuntimePermission()
            }
        }
    }


    override fun startActivityForResult(intent: Intent, requestCode: Int) {
        super.startActivityForResult(intent, requestCode)
        when (requestCode) {
            1001 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d("TAG", "permission granded ...........................: ")
                }
            }

            else -> {}
        }
    }

    protected fun replaceFragment(fragment: Fragment, isAddToBackStack: Boolean, bundle: Bundle?): Fragment {
        /* FragmentManager fragmentManager = getSupportFragmentManager();
        fragment.setArguments(bundle);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.viewHolder, fragment);
        if (isAddToBackStack)
            fragmentTransaction.addToBackStack(fragment.toString());
        fragmentTransaction.commit();*/
        return fragment
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun checkRuntimePermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                10
            )
        }
    }
}
