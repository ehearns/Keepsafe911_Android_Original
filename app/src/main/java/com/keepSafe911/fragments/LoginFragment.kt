package com.keepSafe911.fragments

import AnimationType
import ValidationUtil.Companion.isRequiredField
import ValidationUtil.Companion.isValidEmail
import addFragment
import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.*
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import checkLocationPermission
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.EasyWayLocation.LOCATION_SETTING_REQUEST_CODE
import com.example.easywaylocation.Listener
import com.google.firebase.messaging.FirebaseMessaging
import com.keepSafe911.BuildConfig
import com.keepSafe911.HomeActivity
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.MainBaseFragment
import com.keepSafe911.gps.GpsTracker
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.listner.PositiveButtonListener
import com.keepSafe911.model.request.LoginRequest
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.model.roomobj.Remember_User_Object
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.room.OldMe911Database.Companion.getDatabase
import com.keepSafe911.utils.*
import com.keepSafe911.utils.Comman_Methods.Companion.avoidDoubleClicks
import com.keepSafe911.utils.Comman_Methods.Companion.getcurrentDate
import com.keepSafe911.utils.Comman_Methods.Companion.getdeviceModel
import com.keepSafe911.utils.Comman_Methods.Companion.getdeviceVersion
import com.keepSafe911.utils.Comman_Methods.Companion.getdevicename
import com.kotlinpermissions.KotlinPermissions
import hideKeyboard
import kotlinx.android.synthetic.main.background_layout.*
import kotlinx.android.synthetic.main.fragment_login.*
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class LoginFragment : MainBaseFragment(), View.OnClickListener, Listener {


    private var batteryLevel = 0
    private var locationPermission = false
    private lateinit var easyWayLocation: EasyWayLocation

    lateinit var appDatabase: OldMe911Database
    private var gpstracker: GpsTracker? = null

    private var fireBaseTokenValue: String = ""
    private lateinit var biometricPrompt : BiometricPrompt
    private lateinit var biometricManager: BiometricManager
    private var settingDialogShown: Boolean = false
    var isFrom: Boolean = false

    private val mBatInfoReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isFrom = it.getBoolean(ARG_PARAM1, false)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: Boolean) =
            LoginFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_PARAM1, param1)
                }
            }
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
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("MissingPermission", "SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            if (it != null) {
                fireBaseTokenValue = it
            }
        }
        appDatabase = getDatabase(mActivity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mActivity.resources.getBoolean(R.bool.isTablet)){
                tv_finger.visibility = View.GONE
            }else{
                tv_finger.visibility = View.VISIBLE
                registerForFingerprintService()
            }
        }else{
            tv_finger.visibility = View.GONE
        }
//        ivTopBubble.visibility = View.VISIBLE
//        ivBottomBubble.visibility = View.VISIBLE

        gpstracker = GpsTracker(mActivity)
        mActivity.registerReceiver(this.mBatInfoReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            et_email.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_NEXT
            et_password.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_DONE
        } else {
            et_email.imeOptions = EditorInfo.IME_ACTION_NEXT
            et_password.imeOptions = EditorInfo.IME_ACTION_DONE
        }


        val remData = appDatabase.loginDao().getRemember()
        if (remData != null) {
            et_password.setText(remData.user_password)
            et_email.setText(remData.user_name)
        }

        val fingerObj = appDatabase.loginDao().getfingerLoginData()
        if (fingerObj != null) {
            tv_finger.visibility = View.VISIBLE
        } else {
            tv_finger.visibility = View.GONE
        }

        val content = SpannableString(mActivity.resources.getString(R.string.sign_up_text))
        content.setSpan(StyleSpan(Typeface.BOLD), mActivity.resources.getString(R.string.sign_up_text).indexOf(mActivity.resources.getString(R.string.sign_up)), mActivity.resources.getString(R.string.sign_up_text).length, 0)
        content.setSpan(
            ForegroundColorSpan(Color.parseColor("#881A46")),
            mActivity.resources.getString(R.string.sign_up_text).indexOf(
                mActivity.resources.getString(
                    R.string.sign_up
                )
            ),
            mActivity.resources.getString(R.string.sign_up_text).length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        content.setSpan(
            UnderlineSpan(),
            mActivity.resources.getString(R.string.sign_up_text).indexOf(
                mActivity.resources.getString(
                    R.string.sign_up
                )
            ),
            mActivity.resources.getString(R.string.sign_up_text).length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tv_signup.text = content

        val signInContent = mActivity.resources.getString(R.string.sign_in).uppercase()
        val signContent = mActivity.resources.getString(R.string.str_sign).uppercase()
        val contentTitle = SpannableString(signInContent)
        contentTitle.setSpan(StyleSpan(Typeface.BOLD), 0, signContent.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvSignInTitle.text = contentTitle

        /*val rowValue = if (tv_finger.visibility == View.VISIBLE) 4.5 else 4.0
        val height = Utils.calculateNoOfRows(mActivity, rowValue)
        clLogin.setPadding(0, 0,0,0)
        clLogin.setPadding(40, height,40,20)*/


        tvForgotPassword.setOnClickListener(this)
        tv_signup.setOnClickListener(this)
        btn_login.setOnClickListener(this)
        tv_finger.setOnClickListener(this)

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        /*val rowValue = if (tv_finger.visibility == View.VISIBLE) 4.5 else 4.0
        val height = Utils.calculateNoOfRows(mActivity, rowValue)
        clLogin.setPadding(0, 0,0,0)
        clLogin.setPadding(40, height,40,20)*/
    }

    private val biometricCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            val fingerObj = appDatabase.loginDao().getfingerLoginData()
            if (fingerObj != null) {
                if (fingerObj.touch_id) {
                    tv_finger.visibility = View.VISIBLE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        settingDialogShown = false
                        if (checkLocationPermission(mActivity)) {
                            setPermission(fingerObj.user_name, fingerObj.user_password, true)
                        } else {
                            disclosurePopUp(fingerObj.user_name, fingerObj.user_password, true)
                        }
                    } else {
                        callApi(fingerObj.user_name, fingerObj.user_password,true)
                        locationPermission = true
                    }
                } else {
                    mActivity.showMessage(mActivity.resources.getString(R.string.enable_auth))
                }
            } else {
                tv_finger.visibility = View.GONE
                mActivity.showMessage(mActivity.resources.getString(R.string.enable_auth))
            }
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
        }
    }

    private fun isBiometricFeatureAvailable(): Boolean {
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun buildBiometricPrompt(): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(mActivity.resources.getString(R.string.app_name))
            .setDescription("Confirm your identity so we can verify it's you")
            .setNegativeButtonText(mActivity.resources.getString(R.string.cancel))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setConfirmationRequired(false) //Allows user to authenticate without performing an action, such as pressing a button, after their biometric credential is accepted.
            .build()
    }

    private fun registerForFingerprintService() {

        biometricManager = BiometricManager.from(mActivity)
        val executor = ContextCompat.getMainExecutor(mActivity)
        biometricPrompt = BiometricPrompt(this, executor, biometricCallback)

        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> { tv_finger.visibility = View.GONE }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> { tv_finger.visibility = View.GONE }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> { tv_finger.visibility = View.GONE }
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val fingerObj = appDatabase.loginDao().getfingerLoginData()
                if (fingerObj != null) {
                    if (fingerObj.touch_id) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            openBioMetric()
                        }, 1000)
                    }
                }
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> { tv_finger.visibility = View.GONE }
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> { tv_finger.visibility = View.GONE }
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> { tv_finger.visibility = View.GONE }
        }
    }

    private fun openBioMetric() {
        if (isBiometricFeatureAvailable()) {
            biometricPrompt.authenticate(buildBiometricPrompt())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mActivity.unregisterReceiver(mBatInfoReceiver)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_login -> {
                mActivity.hideKeyboard()
                if (checkforValidations()) {
                    avoidDoubleClicks(v)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        settingDialogShown = false
                        if (checkLocationPermission(mActivity)) {
                            setPermission(
                                et_email.text.toString(),
                                et_password.text.toString(),
                                false
                            )
                        } else {
                            disclosurePopUp(et_email.text.toString(),
                                et_password.text.toString(),
                                false)
                        }
                    } else {
                        callApi(et_email.text.toString(), et_password.text.toString(),false)
                        locationPermission = true
                    }
                }
            }
            R.id.tv_finger -> {
                mActivity.hideKeyboard()
                avoidDoubleClicks(v)
                openBioMetric()
            }
            R.id.tv_signup -> {
                mActivity.hideKeyboard()
                avoidDoubleClicks(v)
                mActivity.addFragment(LandingTwoFragment(), true, true, animationType = AnimationType.fadeInfadeOut)
            }
            R.id.tvForgotPassword -> {
                mActivity.hideKeyboard()
                avoidDoubleClicks(v)
                mActivity.addFragment(ForgotFragment(), true, true, animationType = AnimationType.fadeInfadeOut)
            }

        }
    }

    private fun disclosurePopUp(userName: String, password: String, isAuthenticate: Boolean) {
        Comman_Methods.isCustomPopUpShow(mActivity,
            message = mActivity.resources.getString(R.string.str_location_permission_disclosure),
            positiveButtonText = mActivity.resources.getString(R.string.str_allow),
            negativeButtonText = mActivity.resources.getString(R.string.str_decline),
            positiveButtonListener = object : PositiveButtonListener{
                override fun okClickListener() {
                    setPermission(
                        userName,
                        password,
                        isAuthenticate
                    )
                }
            })
    }

    private fun checkforValidations(): Boolean {

        return when {
            !isRequiredField(et_email.text.toString().trim()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_user))
                false
            }
            !isRequiredField(et_password.text.toString()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_pass))
                false
            }
            else -> true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            LOCATION_SETTING_REQUEST_CODE -> easyWayLocation.onActivityResult(resultCode)
        }
    }

    private fun callApi(user_name: String, user_password: String, isFromAuth:Boolean) {
        easyWayLocation = EasyWayLocation(mActivity)
        easyWayLocation.setListener(this)

        if (gpstracker?.CheckForLoCation() == true) {
            callLoginApi(user_name, user_password,isFromAuth)
        } else {
//            mActivity.enableGpsData()
            Utils.showLocationSettingsAlert(mActivity)
        }
    }

    @SuppressLint("MissingPermission", "HardwareIds")
    private fun setPermission(userName: String, password: String, isAuthenticate: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
                    .permissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                    )
                    .onAccepted { permissions ->
                        if (permissions.size == 3) {
                            if (checkLocationPermission(mActivity)) {
                                easyWayLocation = EasyWayLocation(mActivity)
                                easyWayLocation.setListener(this)
                                locationPermission = true
                                callApi(userName, password, isAuthenticate)
                            } else {
                                Utils.showSettingsAlert(mActivity)
                            }
                        }
                    }
                    .onDenied {
                        setPermission(userName, password, isAuthenticate)
                    }
                    .onForeverDenied {
                        if (!settingDialogShown) {
                            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                                settingDialogShown = true
                            }
                            easyWayLocation = EasyWayLocation(mActivity)
                            easyWayLocation.setListener(this)
                        }
                        Utils.showSettingsAlert(mActivity)
                    }
                    .ask()
            } else {
                KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
                    .permissions(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                    .onAccepted { permissions ->
                        if (permissions.size == 7) {
                            locationPermission = true
                            callApi(userName, password, isAuthenticate)
                        }
                    }
                    .onDenied {
                        setPermission(userName, password, isAuthenticate)
                    }
                    .onForeverDenied {
                        easyWayLocation = EasyWayLocation(mActivity)
                        easyWayLocation.setListener(this)
                        Utils.showSettingsAlert(mActivity)
                    }
                    .ask()
            }
        } else {
            KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
                .permissions(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                .onAccepted { permissions ->
                    if (permissions.size == 6) {
                        locationPermission = true
                        callApi(userName, password, isAuthenticate)
                    }
                }
                .onDenied {
                    setPermission(userName, password, isAuthenticate)
                }
                .onForeverDenied {
                    easyWayLocation = EasyWayLocation(mActivity)
                    easyWayLocation.setListener(this)
                    Utils.showSettingsAlert(mActivity)
                }
                .ask()
        }
    }

    @SuppressLint("HardwareIds")
    private fun callLoginApi(user_name: String, user_password: String, fromAuth: Boolean) {

        val androidId: String = Settings.Secure.getString(mActivity.contentResolver, Settings.Secure.ANDROID_ID)
        mActivity.isSpeedAvailable()

        val tokenId: String = if (fireBaseTokenValue.trim() != "") fireBaseTokenValue else AppPreference.getStringPreference(mActivity, BuildConfig.firebasePrefKey)
        val login_json = LoginRequest()

        login_json.batteryLevel = batteryLevel.toString()
        login_json.deviceCompanyName = getdevicename()
        login_json.deviceModel = getdeviceModel()
        login_json.deviceOS = getdeviceVersion()
        // login_json.uuid =device_uuid
        login_json.uuid = androidId
        if (fromAuth){
            login_json.userName = user_name.trim()
        }else{
            if (isValidEmail(et_email.text.toString())) {
                login_json.email = user_name.trim()
            } else {
                login_json.userName = user_name.trim()
            }
        }
        val decimalSymbols = DecimalFormatSymbols.getInstance(Locale.US)
        login_json.latitude = DecimalFormat("#.######", decimalSymbols).format(easyWayLocation.latitude)
        login_json.longitude = DecimalFormat("#.######", decimalSymbols).format(easyWayLocation.longitude)
        login_json.recordStatus = LOGIN_RECORD_STATUS.toString()
        login_json.locationPermission = locationPermission.toString()
        login_json.locationAddress = ""
//                Utils.getCompleteAddressString(mActivity, easyWayLocation.latitude, easyWayLocation.longitude)
        login_json.mobile = ""
        login_json.createdby = ""
        login_json.id = 0
        login_json.firstName = ""
        login_json.lastName = ""
        login_json.profilePath = ""
        login_json.frequency = 60
        login_json.loginByApp = 2
        login_json.devicetypeid = DEVICE_TYPE_ID.toString()

        if (NotificationManagerCompat.from(mActivity).areNotificationsEnabled()) {
            login_json.notificationPermission = "true"
        } else {
            login_json.notificationPermission = "false"
        }

        login_json.deviceTokenId = tokenId
        login_json.deviceType = DEVICE_TYPE
        login_json.startDate = getcurrentDate()
        login_json.password = user_password

        Utils.callLoginPingLogoutApi(mActivity, login_json, object : CommonApiListener {
            override fun loginResponse(
                status: Boolean,
                loginData: LoginObject?,
                message: String,
                responseMessage: String
            ) {
                if (status) {

                    val inputFormat = SimpleDateFormat(INPUT_DATE_FORMAT)
                    val outputFormat = SimpleDateFormat(OUTPUT_DATE_FORMAT)
                    val startDateFormat = SimpleDateFormat(DELIVER_DATE_FORMAT)


                    /**
                     * Set manually data to Object From Response.
                     */
                    val login_obj = LoginObject()

                    /**
                     * Add Login for get Days from
                     * SubscriptionEndDate & SubscriptionStartDate
                     * for set Call api after this time.
                     */
                    if (loginData != null) {
                        if (loginData.SubscriptionStartDate != null) {

                            login_obj.SubscriptionEndDate = loginData.SubscriptionEndDate
                            login_obj.SubscriptionStartDate =
                                loginData.SubscriptionStartDate

                            val startDate =
                                inputFormat.parse(loginData.startDate ?: "")
                            val startDateChange = outputFormat.format(startDate)
                            val startDateValue = outputFormat.parse(startDateChange)

                            val endDate =
                                inputFormat.parse(loginData.SubscriptionEndDate)
                            val endDateChange = outputFormat.format(endDate)
                            val endDateValue = outputFormat.parse(endDateChange)


                            val diffrence: Long = endDateValue.time - startDateValue.time
                            val days =
                                TimeUnit.DAYS.convert(diffrence, TimeUnit.MILLISECONDS)
                            login_obj.time_interval_days = days.toInt()
                        }



                        login_obj.IsNotification = loginData.IsNotification
                        login_obj.IsSms = loginData.IsSms
                        login_obj.IsSubscription = loginData.IsSubscription

                        login_obj.Package = loginData.Package
                        login_obj.count = loginData.count ?: 1
                        login_obj.deviceDetails = loginData.deviceDetails
                        login_obj.memberID = loginData.memberID
                        login_obj.familyID = loginData.familyID
                        login_obj.email = loginData.email
                        login_obj.password = loginData.password
                        login_obj.recordStatus = loginData.recordStatus
                        login_obj.uUID = loginData.uUID
                        login_obj.locationAddress = loginData.locationAddress
                        login_obj.latitude = loginData.latitude
                        login_obj.longitude = loginData.longitude
                        login_obj.startDate = loginData.startDate
                        login_obj.deviceDetails = loginData.deviceDetails
                        login_obj.isAdmin = loginData.isAdmin
                        login_obj.userName = loginData.userName
                        login_obj.profilePath = loginData.profilePath
                        login_obj.firstName = loginData.firstName
                        login_obj.lastName = loginData.lastName ?: ""
                        login_obj.sequirityQuestionID = loginData.sequirityQuestionID
                        login_obj.sequirityAnswer = loginData.sequirityAnswer
                        login_obj.domainName = loginData.domainName
                        login_obj.subscriptionExpireDate = loginData.subscriptionExpireDate
                        login_obj.freeTrail = loginData.freeTrail
                        login_obj.memberUtcDateTime = loginData.memberUtcDateTime
                        login_obj.mobile = loginData.mobile ?: ""
                        login_obj.eventGeoFanceListing = loginData.eventGeoFanceListing
                        login_obj.lstFamilyMonitoringGeoFence = loginData.lstFamilyMonitoringGeoFence
                        login_obj.frequency = loginData.frequency
                        login_obj.totalMembers = loginData.totalMembers
                        login_obj.adminID = loginData.adminID
                        login_obj.isFromIos = loginData.isFromIos
                        login_obj.isReport = loginData.isReport
                        login_obj.loginByApp = loginData.loginByApp ?: 2
                        login_obj.IsAdditionalMember = loginData.IsAdditionalMember ?: false
                        login_obj.ReferralCode = loginData.ReferralCode ?: ""
                        login_obj.ReferralName = loginData.ReferralName ?: ""
                        login_obj.PromocodeUrl = loginData.PromocodeUrl ?: ""
                        login_obj.Promocode = loginData.Promocode ?: ""
                        login_obj.isChildMissing = loginData.isChildMissing ?: false
                        login_obj.clientMobileNumber = loginData.clientMobileNumber ?: ""
                        login_obj.clientImageUrl = loginData.clientImageUrl ?: ""
                        login_obj.currentSubscriptionEndDate = loginData.currentSubscriptionEndDate ?: ""
                        login_obj.liveStreamDuration = loginData.liveStreamDuration ?: 15
                        login_obj.adminName = loginData.adminName ?: ""
                        login_obj.isAdminLoggedIn = loginData.isAdminLoggedIn ?: false
                        login_obj.payId = loginData.payId ?: ""
                        login_obj.productId = loginData.productId ?: ""
                        login_obj.paymentType = loginData.paymentType ?: 0

                        appDatabase.loginDao().addLogin(login_obj)


                        if (appDatabase.loginDao().getRemember() != null) {
                            if (!fromAuth) {
                                appDatabase.loginDao()
                                    .deleteRemember(appDatabase.loginDao().getRemember())
                            }
                        }

                        val intent = Intent(mActivity, HomeActivity::class.java)

                        if (!loginData.IsSubscription) {
                            mActivity.finish()
                            mActivity.overridePendingTransition(
                                R.anim.fade_in,
                                R.anim.fade_out
                            )
                            intent.putExtra(IS_FROM_LOGIN_KEY, true)
                            intent.putExtra(IS_FOR_PAYMENT_KEY, true)
                            mActivity.startActivity(intent)
                        } else {
                            mActivity.finish()
                            mActivity.overridePendingTransition(
                                R.anim.fade_in,
                                R.anim.fade_out
                            )
                            intent.putExtra(IS_FROM_LOGIN_KEY, true)
                            intent.putExtra(IS_FOR_PAYMENT_KEY, false)
                            mActivity.startActivity(intent)
                        }


                        /**
                         * Save email and Passsword into Db for Remember .
                         */
                        if (cb_remember != null) {
                            if (cb_remember.isChecked) {
                                val remObj = Remember_User_Object()

                                remObj.user_name = et_email.text.toString()
                                remObj.user_password = et_password.text.toString()
                                remObj.user_id = 0
                                appDatabase.loginDao().addRemember(remObj)
                            }
                        }
                        val fingerObj = appDatabase.loginDao().getfingerLoginData()
                        if (fingerObj != null) {
                            if (fingerObj.user_name != user_name) {
                                appDatabase.loginDao().dropFinger()
                            }
                        }
                    }
                } else {
                    if (responseMessage != "") {
                        mActivity.showMessage(responseMessage)
                    } else {
                        mActivity.showMessage(message)
                    }
                }
            }

            override fun onFailureResult() {
                mActivity.showMessage(mActivity.resources.getString(R.string.error_message))
            }
        })
    }

    override fun locationCancelled() {
    }

    override fun locationOn() {
        if (easyWayLocation.getmListener != null) {
            easyWayLocation.beginUpdates()
        }
    }

    override fun onPositionChanged() {
        // easyWayLocation.showAlertDialog("", "", null)
    }
}
