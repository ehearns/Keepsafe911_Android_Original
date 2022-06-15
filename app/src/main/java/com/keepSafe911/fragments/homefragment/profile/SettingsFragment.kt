package com.keepSafe911.fragments.homefragment.profile


import AnimationType
import addFragment
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.keepSafe911.BuildConfig
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.gps.GpsJobService
import com.keepSafe911.gps.GpsService
import com.keepSafe911.gps.GpsTracker
import com.keepSafe911.model.response.ApiResponse
import com.keepSafe911.model.roomobj.FingerSucess_User_Object
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import com.keepSafe911.utils.Comman_Methods.Companion.isProgressHide
import com.keepSafe911.utils.Comman_Methods.Companion.isProgressShow
import com.keepSafe911.webservices.WebApiClient
import hideKeyboard
import isAppIsInBackground
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.android.synthetic.main.toolbar_header.*
import retrofit2.Call
import retrofit2.Response
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class SettingsFragment : HomeBaseFragment(), View.OnClickListener, CompoundButton.OnCheckedChangeListener{

    lateinit var appDatabase: OldMe911Database
    private var toggleFrequency: Boolean = false
    var value: Double = 0.0
    private var gpstracker: GpsTracker? = null

    private lateinit var biometricPrompt : BiometricPrompt
    private lateinit var biometricManager: BiometricManager

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mActivity.requestedOrientation =
            if (mActivity.resources.getBoolean(R.bool.isTablet)) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        LocaleUtils.setLocale(
            Locale(
                if (AppPreference
                        .getIntPreference(requireActivity()
                            , BuildConfig.languagePrefKey) == 0
                ) LocaleUtils.LAN_ENGLISH else LocaleUtils.LAN_SPANISH
            )
        )
        LocaleUtils.updateConfig(mActivity, mActivity.resources.configuration)
        /*mActivity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )*/
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity.disableDrawer()
        appDatabase = OldMe911Database.getDatabase(mActivity)
        gpstracker = GpsTracker(mActivity)

        tvAppVersion.text = BuildConfig.VERSION_NAME

        switchTravelNotification.isChecked = appDatabase.loginDao().getAll().IsNotification ?: false

        //  sdv_profile_image.loadFrescoImage(mActivity, appDatabase.loginDao().getAll().profilePath ?: "", 1)
        if (appDatabase.loginDao().getAll().isAdmin) {
//            llSummary.visibility = View.VISIBLE
            rlMessage.visibility = View.VISIBLE
//            ivSummaryInfo.visibility = View.VISIBLE
//            rlTravelNotification.visibility = View.VISIBLE
            switchMessage.isChecked = appDatabase.loginDao().getAll().IsSms


            val frequencyData = appDatabase.loginDao().getAll().frequency ?: 60
            val strFreq = mActivity.resources.getString(R.string.str_seconds)
            when {
                frequencyData > 0 -> {
                    when {
                        frequencyData < 60 -> tvTrackingStatus.text = "" + (frequencyData * 60) +" "+ strFreq
                        frequencyData > 14400 -> tvTrackingStatus.text = "14400 $strFreq"
                        else -> tvTrackingStatus.text = "$frequencyData $strFreq"
                    }
                }
                else -> tvTrackingStatus.text = "60 $strFreq"
            }
            val constraintSet = ConstraintSet()
            constraintSet.clone(settingConstraint)
            constraintSet.connect(R.id.rlLanguage,ConstraintSet.TOP,R.id.tvTrackingTime,ConstraintSet.BOTTOM,10)
            constraintSet.applyTo(settingConstraint)
        } else {
//            llSummary.visibility = View.GONE
            rlMessage.visibility = View.GONE
//            rlTravelNotification.visibility = View.GONE
//            ivSummaryInfo.visibility = View.GONE
        }
        tvTrackingTime.visibility = View.GONE
        tvTrackingStatus.visibility = View.GONE

        when {
            AppPreference
                .getIntPreference(requireActivity()
                    , BuildConfig.languagePrefKey) == 0 -> tvLanguageStatus.text =
                ArrayList(listOf(*resources.getStringArray(R.array.array_language)))[0]
            else -> tvLanguageStatus.text =
                ArrayList(listOf(*resources.getStringArray(R.array.array_language)))[1]
        }

        if (NotificationManagerCompat.from(mActivity).areNotificationsEnabled()) {
            tvNotificationStatus.text = mActivity.resources.getString(R.string.on)
        } else {
            tvNotificationStatus.text = mActivity.resources.getString(R.string.off)
        }
        if (gpstracker?.CheckForLoCation() == true) {
            tvLocationStatus.text = mActivity.resources.getString(R.string.on)
        }else{
            tvLocationStatus.text = mActivity.resources.getString(R.string.off)
        }
        val fing_obj = appDatabase.loginDao().getfingerLoginData()
        if (fing_obj != null) {
            switchTouchID.isChecked = fing_obj.touch_id
        } else {
            switchTouchID.isChecked = false
        }

        setHeader()
        listeners()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mActivity.resources.getBoolean(R.bool.isTablet)) {
                rlTouch.visibility = View.GONE
            }else{
                rlTouch.visibility = View.VISIBLE
                registerForFingerprintService()
            }
        }else{
            rlTouch.visibility = View.GONE
        }

    }

    private fun listeners() {

        switchTouchID.setOnCheckedChangeListener(this)
        llSummary.setOnClickListener(this)
        switchTravelNotification.setOnCheckedChangeListener(this)
        switchMessage.setOnCheckedChangeListener(this)
        rlLanguage.setOnClickListener(this)
        tvTrackingStatus.setOnClickListener(this)
        tvTrackingTime.setOnClickListener(this)
        btnSubmitFreq.setOnClickListener(this)
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        when (buttonView?.id) {
            R.id.switchTouchID -> {
                mActivity.hideKeyboard()
                appDatabase.loginDao().dropFinger()
                if (isChecked) {
                    val fing_obj = FingerSucess_User_Object()
                    fing_obj.user_name = appDatabase.loginDao().getAll().userName ?: ""
                    fing_obj.user_password = appDatabase.loginDao().getAll().password ?: ""
                    fing_obj.touch_id = true
                    appDatabase.loginDao().addFingerLogin(fing_obj)
                }
            }
            R.id.switchMessage -> {
                mActivity.hideKeyboard()
                if (appDatabase.loginDao().getAll().mobile!=null){
                    if (appDatabase.loginDao().getAll().mobile!=""){
                        callOnOffSMSApi(isChecked, "SMS")
                    }else{
                        switchMessage.isChecked = false
                        mActivity.showMessage(mActivity.resources.getString(R.string.str_mobile_request))
                    }
                }else{
                    switchMessage.isChecked = false
                    mActivity.showMessage(mActivity.resources.getString(R.string.str_mobile_request))
                }
            }
            R.id.switchTravelNotification -> {
                mActivity.hideKeyboard()
                callOnOffSMSApi(isChecked, "Notification")
            }
        }

    }

    private fun callOnOffSMSApi(checked: Boolean, type: String) {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            isProgressShow(mActivity)
            mActivity.isSpeedAvailable()
            val callActiveDeActiveMessage = WebApiClient.getInstance(mActivity).webApi_without?.
            activeDeActiveSMS(appDatabase.loginDao().getAll().memberID, checked, type)
            callActiveDeActiveMessage?.enqueue(object : retrofit2.Callback<ApiResponse> {
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    isProgressHide()
                }

                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            isProgressHide()
                            response.body()?.let {
                                if (it.status) {
                                    val loginupdate: LoginObject = appDatabase.loginDao().getAll()
                                    if (type == "SMS") {
                                        loginupdate.IsSms = checked
                                    } else {
                                        loginupdate.IsNotification = checked
                                    }
                                    appDatabase.loginDao().updateLogin(loginupdate)
                                }
                            }
                        }
                    } else {
                        isProgressHide()
                        Utils.showSomeThingWrongMessage(mActivity)
                    }
                }
            })
        } else {
            Utils.showNoInternetMessage(mActivity)
        }
    }


    private fun setHeader() {

        tvHeader.text = mActivity.resources.getString(R.string.app_settings)
        Utils.setTextGradientColor(tvHeader)
        tvHeader.setPadding(0, 0, 50, 0)
        iv_back.visibility = View.VISIBLE
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
        mActivity.checkUserActive()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.llSummary -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                mActivity.addFragment(BoundaryLogFragment(), true, true, animationType = AnimationType.fadeInfadeOut)
            }
            R.id.rlLanguage -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                mActivity.addFragment(LanguageFragment(), true, true, AnimationType.bottomtotop)
            }
            R.id.tvTrackingTime, R.id.tvTrackingStatus -> {
                mActivity.hideKeyboard()
                toggleView()
            }
            R.id.btnSubmitFreq -> {
                mActivity.hideKeyboard()
                when {
                    etSettingFrequency.text.toString().trim().isEmpty() -> mActivity.showMessage(mActivity.resources.getString(R.string.freq_blank))
                    etSettingFrequency.text.toString().toInt() < 60 -> mActivity.showMessage(mActivity.resources.getString(R.string.recommended_freq))
                    etSettingFrequency.text.toString().toInt() > 14400 -> mActivity.showMessage(mActivity.resources.getString(R.string.recommended_freq))
                    else -> {
                        Comman_Methods.avoidDoubleClicks(v)
                        callFrequencyChangeApi(
                            etSettingFrequency.text.toString().toInt(),
                            appDatabase.loginDao().getAll().memberID
                        )
                    }
                }
            }
        }
    }

    private fun toggleView() {
        if (toggleFrequency){
            toggleFrequency = false
            tvTrackingStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.frequency_up, 0)
            tl_ping_admin.visibility = View.GONE
            btnSubmitFreq.visibility = View.GONE
            val constraintSet = ConstraintSet()
            constraintSet.clone(settingConstraint)
            constraintSet.connect(R.id.rlLanguage,ConstraintSet.TOP,R.id.tvTrackingTime,ConstraintSet.BOTTOM,10)
            constraintSet.applyTo(settingConstraint)
        }else{
            toggleFrequency = true
            tvTrackingStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.frequency_down, 0)
            tl_ping_admin.visibility = View.VISIBLE
            btnSubmitFreq.visibility = View.VISIBLE
            val constraintSet = ConstraintSet()
            constraintSet.clone(settingConstraint)
            constraintSet.connect(R.id.rlLanguage,ConstraintSet.TOP,R.id.btnSubmitFreq,ConstraintSet.BOTTOM,10)
            constraintSet.applyTo(settingConstraint)
            if (mActivity.resources.getBoolean(R.bool.isTablet)) {
                etSettingFrequency.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            } else {
                etSettingFrequency.imeOptions = EditorInfo.IME_ACTION_DONE
            }
            val freq = appDatabase.loginDao().getAll().frequency ?: 60
            try {
                when {
                    freq > 0 -> {
                        when {
                            freq < 60 -> etSettingFrequency.setText("" + (freq * 60))
                            freq > 14400 -> etSettingFrequency.setText("14400")
                            else -> etSettingFrequency.setText("$freq")
                        }
                    } else -> etSettingFrequency.setText("60")
                }
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }


    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            setHeader()
        }
    }

    private fun callFrequencyChangeApi(frequency: Int, memberID: Int) {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            isProgressShow(mActivity)
            mActivity.isSpeedAvailable()
            val callChangeFrequency = WebApiClient.getInstance(mActivity).webApi_without?.callChangePingFrequency(memberID,frequency)
            callChangeFrequency?.enqueue(object : retrofit2.Callback<ApiResponse>{
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    isProgressHide()
                }

                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            isProgressHide()
                            response.body()?.let {
                                if (it.status) {
                                    val freq = appDatabase.loginDao().getAll().frequency ?: 0
                                    val loginupdate: LoginObject = appDatabase.loginDao().getAll()
                                    loginupdate.frequency = frequency
                                    appDatabase.loginDao().updateLogin(loginupdate)
                                    tvTrackingStatus.text =
                                        "" + freq + " " + mActivity.resources.getString(R.string.str_seconds)
                                    if (freq != frequency) {
                                        val isServiceStarted =
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                Utils.isJobServiceRunning(mActivity)
                                            } else {
                                                Utils.isMyServiceRunning(
                                                    GpsService::class.java,
                                                    mActivity
                                                )
                                            }
                                        if (isServiceStarted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                            val jobScheduler =
                                                mActivity.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                                            jobScheduler.cancel(GPSSERVICEJOBID)
                                        } else if (isServiceStarted) {
                                            mActivity.stopService(
                                                Intent(
                                                    mActivity,
                                                    GpsService::class.java
                                                )
                                            )
                                        }
                                        val isServiceStoped =
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                Utils.isJobServiceRunning(mActivity)
                                            } else {
                                                Utils.isMyServiceRunning(
                                                    GpsService::class.java,
                                                    mActivity
                                                )
                                            }
                                        try {
                                            if (isAppIsInBackground(mActivity)) {
                                                if (!isServiceStoped && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                    val jobScheduler =
                                                        mActivity.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                                                    val componentName = ComponentName(
                                                        mActivity.packageName,
                                                        GpsJobService::class.java.name
                                                    )
                                                    val jobInfo =
                                                        JobInfo.Builder(GPSSERVICEJOBID, componentName)
                                                            .setMinimumLatency(1000)
                                                            .setOverrideDeadline((241 * 60000).toLong())
                                                            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                                                            .setPersisted(true).build()
                                                    val resultCode = jobScheduler.schedule(jobInfo)
                                                    if (resultCode == JobScheduler.RESULT_SUCCESS) {
                                                        Log.d(
                                                            GpsJobService::class.java.name,
                                                            "job scheduled"
                                                        )
                                                    } else {
                                                        Log.d(
                                                            GpsJobService::class.java.name,
                                                            "job schedule failed"
                                                        )
                                                    }
                                                } else if (!isServiceStoped) {
                                                    mActivity.startService(
                                                        Intent(
                                                            mActivity,
                                                            GpsService::class.java
                                                        )
                                                    )
                                                }
                                            } else {
                                                if (!isServiceStoped && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                    val jobScheduler =
                                                        mActivity.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                                                    val componentName = ComponentName(
                                                        mActivity.packageName,
                                                        GpsJobService::class.java.name
                                                    )
                                                    val jobInfo =
                                                        JobInfo.Builder(GPSSERVICEJOBID, componentName)
                                                            .setMinimumLatency(1000)
                                                            .setOverrideDeadline((241 * 60000).toLong())
                                                            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                                                            .setPersisted(true).build()
                                                    val resultCode = jobScheduler.schedule(jobInfo)
                                                    if (resultCode == JobScheduler.RESULT_SUCCESS) {
                                                        Log.d(
                                                            GpsJobService::class.java.name,
                                                            "job scheduled"
                                                        )
                                                    } else {
                                                        Log.d(
                                                            GpsJobService::class.java.name,
                                                            "job schedule failed"
                                                        )
                                                    }
                                                } else if (!isServiceStoped) {
                                                    mActivity.startService(
                                                        Intent(
                                                            mActivity,
                                                            GpsService::class.java
                                                        )
                                                    )
                                                }
                                            }
                                        } catch (e: java.lang.Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }
                            val freq = appDatabase.loginDao().getAll().frequency ?: 60
                            val strSec = mActivity.resources.getString(R.string.str_seconds)
                            when {
                                freq > 0 -> {
                                    if (freq < 60) {
                                        tvTrackingStatus.text = "" + (freq * 60) + " " + strSec
                                    } else {
                                        tvTrackingStatus.text = "$freq $strSec"
                                    }
                                }
                                else -> tvTrackingStatus.text = "60 $strSec"
                            }
                            toggleFrequency = false
                            tvTrackingStatus.setCompoundDrawablesWithIntrinsicBounds(
                                0,
                                0,
                                R.drawable.frequency_up,
                                0
                            )
                            tl_ping_admin.visibility = View.GONE
                            btnSubmitFreq.visibility = View.GONE
                            val constraintSet = ConstraintSet()
                            constraintSet.clone(settingConstraint)
                            constraintSet.connect(
                                R.id.rlLanguage,
                                ConstraintSet.TOP,
                                R.id.tvTrackingTime,
                                ConstraintSet.BOTTOM,
                                10
                            )
                            constraintSet.applyTo(settingConstraint)
                        }
                    } else {
                        isProgressHide()
                        Utils.showSomeThingWrongMessage(mActivity)
                    }
                }
            })
        } else {
            Utils.showNoInternetMessage(mActivity)
        }
    }

    private val biometricCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)

        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
        }
    }

    private fun registerForFingerprintService() {

        biometricManager = BiometricManager.from(mActivity)
        val executor = ContextCompat.getMainExecutor(mActivity)
        biometricPrompt = BiometricPrompt(this, executor, biometricCallback)

        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> { rlTouch.visibility = View.GONE }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> { rlTouch.visibility = View.GONE }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> { rlTouch.visibility = View.GONE }
            BiometricManager.BIOMETRIC_SUCCESS -> { }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> { rlTouch.visibility = View.GONE }
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> { rlTouch.visibility = View.GONE }
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> { rlTouch.visibility = View.GONE }
        }
    }
}