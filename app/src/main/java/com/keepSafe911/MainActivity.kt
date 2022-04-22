package com.keepSafe911

import addFragment
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.keepSafe911.fragments.LandingOneFragment
import com.keepSafe911.fragments.LoginFragment
import com.keepSafe911.fragments.SplashFragment
import com.keepSafe911.utils.AppPreference
import com.keepSafe911.utils.LocaleUtils
import com.keepSafe911.utils.Utils
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    var isLoggedOut = false
    var channelName = ""
    var liveStreamId = ""
    var missingChildId = ""

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation =
            if (resources.getBoolean(R.bool.isTablet))
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // mFingerPrintAuthHelper = FingerPrintAuthHelper.getHelper(this, this)
        LocaleUtils.setLocale(
            Locale(
                if (AppPreference
                        .getIntPreference(this@MainActivity
                            , BuildConfig.languagePrefKey) == 0
                ) LocaleUtils.LAN_ENGLISH else LocaleUtils.LAN_SPANISH
            )
        )
        LocaleUtils.updateConfig(this, resources.configuration)
        /*window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )*/
        setContentView(R.layout.activity_main)
        getBundleData()

        if (isLoggedOut) {
            addFragment(LoginFragment(), false, true, animationType = AnimationType.fadeInfadeOut)
        } else {
            addFragment(SplashFragment.newInstance(channelName, liveStreamId, missingChildId), false, true, animationType = AnimationType.fadeInfadeOut)
        }
    }

    private fun getBundleData() {
        val intent = intent
        if (intent != null) {
            isLoggedOut = intent.getBooleanExtra("IsLoggedOut", false)
        }

        if (intent.extras != null){
            for (key in intent.extras!!.keySet()){
                if (key.equals("channelName")){
                    channelName = intent.extras?.getString("channelName") ?: ""
                }
                if (key.equals("liveStreamId")) {
                    liveStreamId = intent.extras?.getString("liveStreamId") ?: ""
                }
                if (key.equals("MissingChildId")){
                    missingChildId = intent.extras?.getString("MissingChildId") ?: ""
                }
            }
        }
    }

    fun showMessage(message: String) {
        Utils.showToastMessage(this@MainActivity, message)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            getVisibleFragment()?.onActivityResult(requestCode, resultCode, data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getVisibleFragment(): Fragment? {
        try {
            val fragmentManager = supportFragmentManager
            @SuppressLint("RestrictedApi") val fragments = fragmentManager.fragments
            if (fragments != null) {
                for (fragment in fragments) {
                    if (fragment != null && fragment.isVisible)
                        return fragment
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun onBackPressed() {
        val currentFragment: Fragment? = supportFragmentManager.findFragmentById(R.id.frame_container)
        if (currentFragment != null) {
            if (currentFragment is LoginFragment || currentFragment is LandingOneFragment) {
                if (currentFragment is LoginFragment) {
                    if (currentFragment.isFrom) {
                        super.onBackPressed()
                    } else {
                        finish()
                    }
                } else {
                    finish()
                }
            } else {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppPreference.clearPrivacyTerms(this@MainActivity)
    }

    //For check Internet speed for showing error if poor internet connections.
    fun isSpeedAvailable(): Boolean {
        val connectivity = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (connectivity != null) {
            val info = connectivity.allNetworkInfo

            if (info != null) {
                for (i in info.indices) {
                    if (info[i].state == NetworkInfo.State.CONNECTED) {
                        if (!isConnectionSpeed(info[i].type,info[i].subtype)){
                            showMessage(resources.getString(R.string.bad_connection))
                        }
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun isConnectionSpeed(type: Int, subType: Int): Boolean {
        return when (type) {
            ConnectivityManager.TYPE_WIFI -> true
            ConnectivityManager.TYPE_MOBILE -> when (subType) {
                TelephonyManager.NETWORK_TYPE_1xRTT -> false // ~ 50-100 kbps
                TelephonyManager.NETWORK_TYPE_CDMA -> false // ~ 14-64 kbps
                TelephonyManager.NETWORK_TYPE_EDGE -> false // ~ 50-100 kbps
                TelephonyManager.NETWORK_TYPE_EVDO_0 -> true // ~ 400-1000 kbps
                TelephonyManager.NETWORK_TYPE_EVDO_A -> true // ~ 600-1400 kbps
                TelephonyManager.NETWORK_TYPE_GPRS -> false // ~ 100 kbps
                TelephonyManager.NETWORK_TYPE_HSDPA -> true // ~ 2-14 Mbps
                TelephonyManager.NETWORK_TYPE_HSPA -> true // ~ 700-1700 kbps
                TelephonyManager.NETWORK_TYPE_HSUPA -> true // ~ 1-23 Mbps
                TelephonyManager.NETWORK_TYPE_UMTS -> true // ~ 400-7000 kbps
                /*
                  * Above API level 7, make sure to set android:targetSdkVersion
                  * to appropriate level to use these
                  */
                TelephonyManager.NETWORK_TYPE_EHRPD // API level 11
                -> true // ~ 1-2 Mbps
                TelephonyManager.NETWORK_TYPE_EVDO_B // API level 9
                -> true // ~ 5 Mbps
                TelephonyManager.NETWORK_TYPE_HSPAP // API level 13
                -> true // ~ 10-20 Mbps
                TelephonyManager.NETWORK_TYPE_IDEN // API level 8
                -> false // ~25 kbps
                TelephonyManager.NETWORK_TYPE_LTE // API level 11
                -> true // ~ 10+ Mbps
                // Unknown
                TelephonyManager.NETWORK_TYPE_UNKNOWN -> false
                else -> false
            }
            else -> false
        }
    }
}
