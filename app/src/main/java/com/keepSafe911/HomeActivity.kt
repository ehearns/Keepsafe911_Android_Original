package com.keepSafe911

import AnimationType
import addFragment
import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.telephony.TelephonyManager
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.tasks.Task
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.kotlinpermissions.KotlinPermissions
import com.keepSafe911.fragments.donation.DonationFragment
import com.keepSafe911.fragments.donation.DonationHistoryFragment
import com.keepSafe911.fragments.donation.ThankYouFragment
import com.keepSafe911.fragments.homefragment.DashBoardFragment
import com.keepSafe911.fragments.homefragment.SubDashBoardFragment
import com.keepSafe911.fragments.homefragment.boundary.AddGeofenceFragment
import com.keepSafe911.fragments.homefragment.boundary.GeofenceFragment
import com.keepSafe911.fragments.homefragment.find.AddMemberFragment
import com.keepSafe911.fragments.homefragment.find.ExploreNearByListFragment
import com.keepSafe911.fragments.homefragment.find.LiveMemberFragment
import com.keepSafe911.fragments.homefragment.find.MemberRouteFragment
import com.keepSafe911.fragments.homefragment.hibp.EmailCompromisedFragment
import com.keepSafe911.fragments.homefragment.hibp.PasswordCompromisedFragment
import com.keepSafe911.fragments.homefragment.placetovisit.PlacesToVisitFragment
import com.keepSafe911.fragments.homefragment.profile.*
import com.keepSafe911.fragments.homefragment.report.BusinessTrackFragment
import com.keepSafe911.fragments.homefragment.report.ReportFragment
import com.keepSafe911.fragments.missingchild.AcknowledgePaymentFragment
import com.keepSafe911.fragments.missingchild.MissingChildTaskFragment
import com.keepSafe911.fragments.missingchild.MissingDashBoardFragment
import com.keepSafe911.fragments.missingchild.SearchChildFragment
import com.keepSafe911.fragments.neighbour.AddNeighbourFragment
import com.keepSafe911.fragments.neighbour.NeighborCommentFragment
import com.keepSafe911.fragments.neighbour.NeighbourFragment
import com.keepSafe911.fragments.neighbour.NeighbourMapFragment
import com.keepSafe911.fragments.payment_selection.RemainPaymentMethodFragment
import com.keepSafe911.fragments.payment_selection.RemainSubscriptionFragment
import com.keepSafe911.fragments.payment_selection.UpdateSubFragment
import com.keepSafe911.geofenceservice.LocationUpdatesService
import com.keepSafe911.gps.GpsJobService
import com.keepSafe911.gps.GpsService
import com.keepSafe911.gps.GpsTracker
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.listner.PositiveButtonListener
import com.keepSafe911.model.FamilyMonitorResult
import com.keepSafe911.model.PlacesToVisitModel
import com.keepSafe911.model.SubscriptionBean
import com.keepSafe911.model.request.LoginRequest
import com.keepSafe911.model.response.*
import com.keepSafe911.model.response.voicerecognition.ManageVoiceRecognitionModel
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.offlineservice.CheckInternetAvaliability
import com.keepSafe911.openlive.Constants.KEY_CHANNEL_ID
import com.keepSafe911.openlive.Constants.KEY_CHANNEL_NAME
import com.keepSafe911.openlive.Constants.MISSING_CHILD_ID
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.switchoffaction.SwitchOffReceiver
import com.keepSafe911.utils.*
import com.keepSafe911.utils.Utils.isMyServiceRunning
import com.keepSafe911.utils.Utils.muteRecognizer
import com.keepSafe911.webservices.WebApiClient
import getScreenHeight
import getScreenWidth
import hideKeyboard
import io.agora.rtc.Constants
import isAppIsInBackground
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.app_bar_home.*
import kotlinx.android.synthetic.main.fragment_sign_up.*
import kotlinx.android.synthetic.main.menu_image.*
import kotlinx.android.synthetic.main.nav_header_home.view.*
import kotlinx.android.synthetic.main.toolbar_header.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import takeCall
import visitUrl
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule

class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    Listener, RecognitionListener {


    override fun locationCancelled() {

    }

    override fun locationOn() {
        if (easyWayLocation.getmListener != null) {
            easyWayLocation.beginUpdates()
        }
    }

    override fun onPositionChanged() {

    }

    private var bettery_level = 0
    var notificationPermission: String? = "true"

    private var location_per = false
    private lateinit var easyWayLocation: EasyWayLocation
    private lateinit var reciever: CheckInternetAvaliability
    private lateinit var switchOffReceiver: SwitchOffReceiver
    lateinit var appDatabase: OldMe911Database
    var isFromNotification: Boolean = false
    var isForPayment: Boolean = false
    var isForLogin: Boolean = false
    private var serviceTimer: TimerTask? = null
    private var timer: Timer? = Timer()
    private val MY_REQUEST_CODE = 187
    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var appUpdateInfoTask: Task<AppUpdateInfo>

    private var mService: LocationUpdatesService? = null
    private var mBound = false
    lateinit var mServiceIntent: Intent
    private var settingDialogShown: Boolean = false

    lateinit var tvCancelSearch: TextView
    lateinit var etSearchTravel: EditText
    lateinit var ivSearchTravel: ImageView
    lateinit var ivMenu: ImageView
    var channelName = ""
    var id = ""
    var missingChildId = ""
    var speech: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    var updateLiveStream: TimerTask? = null
    var timerLiveStream: Timer? = Timer()
    var updateMissingTask: TimerTask? = null
    var timerMissingTask: Timer? = Timer()
    private var selectedSubscription: SubscriptionTypeResult? = null
    private lateinit var clientCallDialog: Dialog
    private var toast: Toast? = null

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder: LocationUpdatesService.LocalBinder =
                service as LocationUpdatesService.LocalBinder
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mService = null
            mBound = false
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            channelName = it.getStringExtra(KEY_CHANNEL_NAME) ?: ""
            id = it.getStringExtra(KEY_CHANNEL_ID) ?: ""
            missingChildId = it.getStringExtra(MISSING_CHILD_ID) ?: ""
        }
        if (missingChildId.isNotEmpty()){
            addFragment(SearchChildFragment.newInstance(missingChildId),
                true,
                true,
                animationType = AnimationType.fadeInfadeOut  )
            missingChildId = ""
        }
    }

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation =
            if (resources.getBoolean(R.bool.isTablet)) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        LocaleUtils.setLocale(
            Locale(
                if (AppPreference
                        .getIntPreference(this@HomeActivity
                            , BuildConfig.languagePrefKey) == 0
                ) LocaleUtils.LAN_ENGLISH else LocaleUtils.LAN_SPANISH
            )
        )
        LocaleUtils.updateConfig(this, resources.configuration)
        /*window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )*/
        setContentView(R.layout.activity_home)
        settingDialogShown = false
        setPermission()
        getBundleData()
        appDatabase = OldMe911Database.getDatabase(this)
        easyWayLocation = EasyWayLocation(this)
        easyWayLocation.setListener(this)
        val loginData = appDatabase.loginDao().getAll()

        registerReceiver(this.mBatInfoReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        nav_view.setNavigationItemSelectedListener(this)

        prepareNavigationData(loginData)

        if (loginData.isChildMissing == true) {
            if (loginData.Package == "6") {
                callSubscriptionTypeList()
            }
        }

//        disableDrawer()
        val isServiceStarted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Utils.isJobServiceRunning(this)
        } else {
            isMyServiceRunning(GpsService::class.java, this)
        }
        if (isAppIsInBackground(this)) {
            val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            reciever = CheckInternetAvaliability()
            try {
                registerReceiver(reciever, filter)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            reciever = CheckInternetAvaliability()
            try {
                registerReceiver(reciever, filter)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (isAppIsInBackground(this)) {
            val filter = IntentFilter(Intent.ACTION_SHUTDOWN)
            switchOffReceiver = SwitchOffReceiver()
            try {
                registerReceiver(switchOffReceiver, filter)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            val filter = IntentFilter(Intent.ACTION_SHUTDOWN)
            switchOffReceiver = SwitchOffReceiver()
            try {
                registerReceiver(switchOffReceiver, filter)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        try {
            isFromNotification = intent.getBooleanExtra("isFromNotification", false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        when {
            isFromNotification -> {
                if (loginData.isAdmin) {
                    addFragment(
                        BoundaryLogFragment(),
                        true,
                        true,
                        animationType = AnimationType.fadeInfadeOut
                    )
                }else {
                    gotoDashBoard()
                }
            }
            else ->{
                if (missingChildId.isNotEmpty()){
                    addFragment(SearchChildFragment.newInstance(missingChildId),
                        true,
                        true,
                        animationType = AnimationType.fadeInfadeOut  )
                    missingChildId = ""
                } else {
                    gotoDashBoard()
                }
                /*if (loginData.isAdmin) {
                    if (loginData.totalMembers!! > 0) {
                        addFragment(
                            LiveMemberFragment.newInstance(true),
                            true,
                            true,
                            animationType = AnimationType.fadeInfadeOut
                        )
                    } else {
                        addFragment(
                            AddMemberFragment.newInstance(
                                false,
                                FamilyMonitorResult(),
                                SubscriptionBean(),
                                true,
                                false,
                                true
                            ),
                            true,
                            true,
                            AnimationType.fadeInfadeOut
                        )
                    }
                }else{
                    addFragment(
                        LiveMemberFragment.newInstance(true),
                        true,
                        true,
                        animationType = AnimationType.fadeInfadeOut
                    )
                }*/
            }
        }
        /**
         * Schedule Timer to call API after given time difference.
         */
        /*if (loginData.Package == "0") {
            if (isAppIsInBackground(this@HomeActivity)) {
                LocalBroadcastManager.getInstance(this@HomeActivity).registerReceiver(
                    mMessageReceiver, IntentFilter("LiveMemberCount")
                )
                if (isServiceStarted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                    jobScheduler.cancel(GPSSERVICEJOBID)
                }else if (isServiceStarted){
                    stopService(Intent(this@HomeActivity, GpsService::class.java))
                }
            } else {
                LocalBroadcastManager.getInstance(this@HomeActivity).registerReceiver(
                    mMessageReceiver, IntentFilter("LiveMemberCount")
                )
                if (isServiceStarted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                    jobScheduler.cancel(GPSSERVICEJOBID)
                }else if (isServiceStarted){
                    stopService(Intent(this@HomeActivity, GpsService::class.java))
                }
            }
            addFragment(
                UpdateSubFragment.newInstance(
                    true,
                    false,
                    false,
                    false,
                    FamilyMonitorResult(),
                    ArrayList<SubscriptionTypeResult>(),
                    false,
                    false, true
                ), false, true, animationType = AnimationType.fadeInfadeOut
            )
        } else*/ if (loginData.isAdmin && loginData.SubscriptionStartDate != null) {

            val days = loginData.time_interval_days
            val mills = TimeUnit.DAYS.toMillis(days.toLong())

            checkDeviceSubscriptionApi(true)
//            callCheckSubscriptionApi(true)


            /**
             * Set a timer for checking Admin User's Subscriptions.
             */
            if (mills > 0){
                val timer = Timer("schedule", true)
                // schedule a single event
                timer.schedule(mills) {
                    checkDeviceSubscriptionApi(true)
//                    callCheckSubscriptionApi(true)
                }
            }
        }else if (!loginData.isAdmin){

            checkDeviceSubscriptionApi(true)
//            checkMemberSubscriptionApi(true)
            /*if (isAppIsInBackground(this)) {
                LocalBroadcastManager.getInstance(this).registerReceiver(
                    mMessageReceiver, IntentFilter("LiveMemberCount")
                )
                if (!isServiceStarted) {
                        startService(Intent(this@HomeActivity, GpsService::class.java))
                    }

            } else {
                LocalBroadcastManager.getInstance(this).registerReceiver(
                    mMessageReceiver, IntentFilter("LiveMemberCount")
                )
                if (!isServiceStarted) {
                        startService(Intent(this@HomeActivity, GpsService::class.java))
                    }

            }
            checkVersionCode()*/
        }

        if (isForLogin) {
            if (!loginData.isAdmin) {
                if (loginData.isAdminLoggedIn == true) {
                    showLocationTrackingByAdmin(
                        loginData.adminName ?: ""
                    )
                }
            }
        }

        Utils.voiceRecognitionListing(this@HomeActivity, object : CommonApiListener {
            override fun voiceRecognitionResponse(
                status: Boolean,
                voiceList: ArrayList<ManageVoiceRecognitionModel>,
                message: String,
                responseMessage: String
            ) {
            }
        }, false)
    }

    private fun callSubscriptionTypeList() {
        Utils.subscriptionTypeListApi(this@HomeActivity, object : CommonApiListener {
            override fun subscriptionTypeResponse(
                status: Boolean,
                subscriptionTypeResult: ArrayList<SubscriptionTypeResult>,
                message: String,
                responseMessage: String
            ) {
                if (status) {
                    for (i in 0 until subscriptionTypeResult.size) {
                        if (subscriptionTypeResult[i].id == 6) {
                            selectedSubscription = subscriptionTypeResult[i]
                        }
                    }
                }
            }
        }, false)
    }

    private fun checkMissingSubscription(): Boolean {
        val loginData = appDatabase.loginDao().getAll()
        return if (loginData != null) {
            loginData.isChildMissing == true
        } else {
            false
        }
    }

    fun purchaseSubscription(): Boolean {
        val loginData = appDatabase.loginDao().getAll()
        if (loginData != null) {
            if (loginData.isChildMissing == true) {
                if (loginData.Package == "6") {
                    addFragment(
                        RemainPaymentMethodFragment.newInstance(false,
                            SubscriptionBean(
                                selectedSubscription?.id ?: 0,
                                selectedSubscription?.days ?: 0,
                                selectedSubscription?.totalCost ?: 0.0
                            ), false, FamilyMonitorResult()
                        ), true, true, AnimationType.fadeInfadeOut
                    )
                } else {
                    addFragment(
                        RemainSubscriptionFragment.newInstance(
                            false, false,
                            FamilyMonitorResult(), false, true
                        ), true, true, AnimationType.fadeInfadeOut
                    )
                }
                return true
            } else {
                return false
            }
        } else {
            return false
        }
    }

    private fun requiredChanges(versionCheck: Boolean) {
        try {
            LocalBroadcastManager.getInstance(this@HomeActivity).registerReceiver(
                mMessageReceiver, IntentFilter("LiveMemberCount")
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        callCheckFrequencyPaymentReport()
        checkPermissions()
        if (!versionCheck) {
            val currentFragment: Fragment? = supportFragmentManager.findFragmentById(R.id.frame_container)
            if (currentFragment != null) {
                if (currentFragment is DashBoardFragment) {
                    startLiveStreamTime(currentFragment)
                }
                if (currentFragment is MissingChildTaskFragment) {
                    startMissingChildTaskTime(currentFragment)
                }
            }
        }
        checkUserActive(versionCheck)
    }

    fun showLocationTrackingByAdmin(adminName: String) {
        Comman_Methods.isCustomTrackingPopUpShow(this@HomeActivity,
            message = resources.getString(R.string.your_phone_track, adminName),
            positiveButtonText = resources.getString(R.string.str_ok),
            singlePositive = true,
            positiveButtonListener = object : PositiveButtonListener {})
    }

    private fun checkDeviceSubscriptionApi(versionCheck: Boolean) {
        val loginData = appDatabase.loginDao().getAll()
        val isAdditionalMember = loginData.IsAdditionalMember ?: false
        val userId = when {
            loginData.isAdmin -> {
                loginData.memberID
            }
            isAdditionalMember -> {
                loginData.memberID
            }
            else -> {
                loginData.adminID
            }
        }

        Utils.userDeviceSubscription(this@HomeActivity, userId ?: 0, object : CommonApiListener {
            override fun deviceCheckResponse(
                status: Boolean,
                message: String,
                responseMessage: String,
                result: DeviceSubscriptionResult
            ) {
                if (result.subscriptionTookFrom == 1) {
                    checkAllSubscription(versionCheck)
                } else {
                    if (result.status) {
                        when (result.subscriptionTookFrom) {
                            2 -> {
                                requiredChanges(versionCheck)
                            }
                        }
                    } else {
                        Comman_Methods.isCustomTrackingPopUpHide()
                        when (result.subscriptionTookFrom) {
                            0, 2 -> {
                                if (loginData.isAdmin) {
                                    LocalBroadcastManager.getInstance(this@HomeActivity).registerReceiver(
                                        mMessageReceiver, IntentFilter("LiveMemberCount")
                                    )
                                    if (!checkMissingSubscription()) {
                                        addFragment(
                                            RemainSubscriptionFragment.newInstance(
                                                result.subscriptionTookFrom == 0,
                                                false, FamilyMonitorResult(), true
                                            ),
                                            false,
                                            true,
                                            animationType = AnimationType.fadeInfadeOut
                                        )
                                    }
                                } else {
                                    val isServiceStarted =
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                            Utils.isJobServiceRunning(this@HomeActivity)
                                        } else {
                                            isMyServiceRunning(GpsService::class.java, this@HomeActivity)
                                        }
                                    LocalBroadcastManager.getInstance(this@HomeActivity).registerReceiver(
                                        mMessageReceiver, IntentFilter("LiveMemberCount")
                                    )
                                    stopGpsServices(isServiceStarted)
                                    if (!checkMissingSubscription()) {
                                        addFragment(
                                            UpdateSubFragment.newInstance(
                                                true, false,
                                                false, false,
                                                FamilyMonitorResult(), ArrayList(),
                                                false, false,
                                                true
                                            ),
                                            false, true,
                                            animationType = AnimationType.fadeInfadeOut
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }, false)
    }

    private fun checkMemberSubscriptionApi(versionCheck: Boolean) {
        if (ConnectionUtil.isInternetAvailable(this@HomeActivity)) {
            isSpeedAvailable()
            val subResponseCall = WebApiClient.getInstance(this).webApi_without?.checkMemberSubscription(appDatabase.loginDao().getAll().memberID)

            subResponseCall?.enqueue(object : Callback<MemberSubscription> {
                override fun onFailure(call: Call<MemberSubscription>, t: Throwable) {}

                override fun onResponse(
                    call: Call<MemberSubscription>,
                    response: Response<MemberSubscription>
                ) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {

                            val isServiceStarted =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    Utils.isJobServiceRunning(this@HomeActivity)
                                } else {
                                    isMyServiceRunning(GpsService::class.java, this@HomeActivity)
                                }
                            LocalBroadcastManager.getInstance(this@HomeActivity).registerReceiver(
                                mMessageReceiver, IntentFilter("LiveMemberCount")
                            )
                            response.body()?.let {
                                if (it.status == true) {
                                    /*try {
                                        if (isAppIsInBackground(this@HomeActivity)) {
                                            if (!isServiceStarted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                                                val jobScheduler =
                                                    getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                                                val componentName = ComponentName(
                                                    packageName,
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
                                                    Log.d(GpsJobService::class.java.name, "job scheduled")
                                                } else {
                                                    Log.d(
                                                        GpsJobService::class.java.name,
                                                        "job schedule failed"
                                                    )
                                                }
                                            }else if (!isServiceStarted) {
                                                startService(Intent(this@HomeActivity, GpsService::class.java))
                                            }
                                        } else {
                                            if (!isServiceStarted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                                                val jobScheduler =
                                                    getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                                                val componentName = ComponentName(
                                                    packageName,
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
                                                    Log.d(GpsJobService::class.java.name, "job scheduled")
                                                } else {
                                                    Log.d(
                                                        GpsJobService::class.java.name,
                                                        "job schedule failed"
                                                    )
                                                }
                                            }else if (!isServiceStarted) {
                                                startService(Intent(this@HomeActivity, GpsService::class.java))
                                            }
                                        }
                                    }catch (e: Exception){
                                        e.printStackTrace()
                                    }*/
                                    requiredChanges(versionCheck)
                                } else {
                                    stopGpsServices(isServiceStarted)
                                    if (!checkMissingSubscription()) {
                                        Comman_Methods.isCustomTrackingPopUpHide()
                                        addFragment(
                                            UpdateSubFragment.newInstance(
                                                true, false,
                                                false, false,
                                                FamilyMonitorResult(), ArrayList(),
                                                false, false,
                                                true
                                            ),
                                            false,
                                            true,
                                            animationType = AnimationType.fadeInfadeOut
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Utils.showSomeThingWrongMessage(this@HomeActivity)
                    }
                }
            })
        } else {
            Utils.showNoInternetMessage(this@HomeActivity)
        }
    }

    private fun callCheckFrequencyPaymentReport() {
        if (ConnectionUtil.isInternetAvailable(this)){
            if (appDatabase.loginDao().getAll() != null) {
                if (appDatabase.loginDao().getAll().memberID != null) {
                    val callVersionUpdate =
                        WebApiClient.getInstance(this).webApi_without?.checkFrequencyPaymentReport(
                            appDatabase.loginDao().getAll().memberID
                        )
                    callVersionUpdate?.enqueue(object : Callback<CheckFrequencyResponse> {
                        override fun onFailure(call: Call<CheckFrequencyResponse>, t: Throwable) {}

                        override fun onResponse(
                            call: Call<CheckFrequencyResponse>,
                            response: Response<CheckFrequencyResponse>
                        ) {
                            if (response.isSuccessful) {
                                response.body()?.let {
                                    if (it.status == true) {
                                        val isServiceStarted =
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                Utils.isJobServiceRunning(this@HomeActivity)
                                            } else {
                                                isMyServiceRunning(
                                                    GpsService::class.java,
                                                    this@HomeActivity
                                                )
                                            }
                                        LocalBroadcastManager.getInstance(this@HomeActivity).registerReceiver(
                                            mMessageReceiver, IntentFilter("LiveMemberCount")
                                        )
                                        val checkFrequencyResult = response.body()?.result
                                        if (checkFrequencyResult != null) {
                                            val loginUpdate: LoginObject = appDatabase.loginDao().getAll()
                                            val frequency = checkFrequencyResult.frequency ?: 0
                                            if (loginUpdate != null) {
                                                loginUpdate.frequency = if (frequency >= 60) {
                                                    frequency
                                                } else {
                                                    loginUpdate.frequency
                                                }
                                                loginUpdate.isReport = checkFrequencyResult.isReport ?: false
                                                appDatabase.loginDao().updateLogin(loginUpdate)
                                            }
                                            if (checkFrequencyResult.isReport == true) {
                                                try {
                                                    if (isAppIsInBackground(this@HomeActivity)) {
                                                        if (!isServiceStarted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                            val jobScheduler =
                                                                getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                                                            val componentName = ComponentName(
                                                                packageName,
                                                                GpsJobService::class.java.name
                                                            )
                                                            val jobInfo =
                                                                JobInfo.Builder(
                                                                    GPSSERVICEJOBID,
                                                                    componentName
                                                                )
                                                                    .setMinimumLatency(1000)
                                                                    .setOverrideDeadline((241 * 60000).toLong())
                                                                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                                                                    .setPersisted(true).build()
                                                            val resultCode =
                                                                jobScheduler.schedule(jobInfo)
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
                                                        } else if (!isServiceStarted) {
                                                            startService(
                                                                Intent(
                                                                    this@HomeActivity,
                                                                    GpsService::class.java
                                                                )
                                                            )
                                                        }
                                                    } else {
                                                        if (!isServiceStarted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                            val jobScheduler =
                                                                getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                                                            val componentName = ComponentName(
                                                                packageName,
                                                                GpsJobService::class.java.name
                                                            )
                                                            val jobInfo =
                                                                JobInfo.Builder(
                                                                    GPSSERVICEJOBID,
                                                                    componentName
                                                                )
                                                                    .setMinimumLatency(1000)
                                                                    .setOverrideDeadline((241 * 60000).toLong())
                                                                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                                                                    .setPersisted(true).build()
                                                            val resultCode =
                                                                jobScheduler.schedule(jobInfo)
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
                                                        } else if (!isServiceStarted) {
                                                            startService(
                                                                Intent(
                                                                    this@HomeActivity,
                                                                    GpsService::class.java
                                                                )
                                                            )
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            } else {
                                                stopGpsServices(isServiceStarted)
                                            }
                                        } else {
                                            stopGpsServices(isServiceStarted)
                                        }
                                    }
                                }
                            }
                        }
                    })
                }
            }
        }
    }

    private fun prepareNavigationData(loginData: LoginObject) {
        setProfile(loginData)
        nav_view.menu.findItem(R.id.nav_member_list).isVisible = loginData.isAdmin
        //nav_view.menu.findItem(R.id.nav_fencing).isVisible = loginData.isAdmin
        nav_view.menu.getItem(1).setActionView(R.layout.menu_image)
        val tv_count: TextView = nav_view.menu.getItem(1).actionView.findViewById(R.id.tvLiveMemberCount)
        val loginCount = loginData.count ?: 0
        if (loginCount > 0) {
            tv_count.visibility = View.VISIBLE
            tv_count.text = loginCount.toString()
        }else{
            tv_count.visibility = View.GONE
        }
    }

    fun setProfile(loginData: LoginObject) {
        if (loginData!=null) {
            val profilePath: String = loginData.profilePath ?: ""
            // nav_view.getHeaderView(0).sdv_profile_image.loadFrescoImage(this, profilePath, 1)

            try {
                val file_path =
                    "" + cacheDir + CACHE_FOLDER_NAME + if (profilePath != "") profilePath.split("Family/")[1] else ""
                val file = File(file_path)

                if (file.exists()) {
                    nav_view.getHeaderView(0).sdv_profile_image.setImageURI(
                        Uri.fromFile(file).toString()
                    )
                } else {
                    Comman_Methods.createDir(this)
                    downLoadTask(Comman_Methods.stringToURL(profilePath))
                }
            }catch (e: Exception){
                e.printStackTrace()
            }
            val firstName = if (loginData.firstName != null) loginData.firstName else ""
            val lastName = if (loginData.lastName != null) loginData.lastName else ""

            nav_view.getHeaderView(0).tvUserName.text = "$firstName $lastName"
            tvAppVersion.text = resources.getString(R.string.str_version) + " " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")"
        }
    }

    private fun getBundleData() {
        intent?.let {
            isForLogin = it.getBooleanExtra(IS_FROM_LOGIN_KEY, false)
            isForPayment = it.getBooleanExtra(IS_FOR_PAYMENT_KEY, false)
            channelName = it.getStringExtra(KEY_CHANNEL_NAME) ?: ""
            id = it.getStringExtra(KEY_CHANNEL_ID) ?: ""
            missingChildId = it.getStringExtra(MISSING_CHILD_ID) ?: ""
        }
    }

    private val mBatInfoReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            bettery_level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        }
    }

    fun closeDrawer(){
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        }
    }

    private fun hideView(): Boolean {
        try {
            return if (this@HomeActivity::etSearchTravel.isInitialized) {
                if (etSearchTravel.visibility == View.VISIBLE) {
                    tvCancelSearch.visibility = View.GONE
                    etSearchTravel.setText("")
                    etSearchTravel.visibility = View.GONE
                    ivSearchTravel.visibility = View.VISIBLE
                    ivMenu.visibility = View.VISIBLE
                    false
                } else {
                    true
                }
            } else {
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return true
        }
    }

    fun gotoDashBoard() {
        addFragment(DashBoardFragment(), true, true, animationType = AnimationType.fadeInfadeOut)
    }

    override fun onBackPressed() {
        val loginData = appDatabase.loginDao().getAll()
        val currentFragment: Fragment? = supportFragmentManager.findFragmentById(R.id.frame_container)
        if (currentFragment != null) {
            if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
                drawer_layout.closeDrawer(GravityCompat.START)
            } else if (currentFragment is SubDashBoardFragment || currentFragment is NeighbourFragment ||
                currentFragment is ReportPaymentHistoryFragment || currentFragment is ReportFragment ||
                currentFragment is GeofenceFragment || currentFragment is MemberRouteFragment ||
                currentFragment is LiveMemberFragment || currentFragment is PlacesToVisitFragment ||
                currentFragment is SearchChildFragment || currentFragment is DonationHistoryFragment ||
                currentFragment is MissingDashBoardFragment) {

                if (currentFragment is GeofenceFragment) {
                    if (hideView()) {
                        gotoDashBoard()
                    }
                } else if (currentFragment is ReportFragment){
                    if (loginData.isAdmin){
                        super.onBackPressed()
                    }else{
                        gotoDashBoard()
                    }
                } else {
                    gotoDashBoard()
                }
            } else if (currentFragment is AddMemberFragment) {
                if (currentFragment.isFrom){
                    addFragment(
                        LiveMemberFragment(),
                        true,
                        true,
                        animationType = AnimationType.fadeInfadeOut
                    )
                }else{
                    gotoDashBoard()
                }
            } else if (currentFragment is MissingChildTaskFragment || currentFragment is AcknowledgePaymentFragment) {
                if (currentFragment is MissingChildTaskFragment) {
                    if (currentFragment.isFrom) {
                        addFragment(
                            MissingDashBoardFragment(),
                            true,
                            true,
                            animationType = AnimationType.fadeInfadeOut
                        )
                    } else {
                        super.onBackPressed()
                    }
                } else {
                    addFragment(
                        MissingDashBoardFragment(),
                        true,
                        true,
                        animationType = AnimationType.fadeInfadeOut
                    )
                }
            } else if (currentFragment is DonationFragment || currentFragment is ThankYouFragment) {
                addFragment(DonationHistoryFragment(), true, true, animationType = AnimationType.fadeInfadeOut)
            } else if (currentFragment is BoundaryLogFragment || currentFragment is LanguageFragment) {
                addFragment(SettingsFragment(), true, true, animationType = AnimationType.fadeInfadeOut)
            } else if (currentFragment is ChangePasswordFragment || currentFragment is SettingsFragment || currentFragment is EditProfileFragment || currentFragment is PaymentFragment || currentFragment is SupportFragment || currentFragment is VoiceRecognitionFragment) {
                addFragment(
                    SubDashBoardFragment.newInstance(4),
                    true,
                    true,
                    animationType = AnimationType.fadeInfadeOut
                )
            } else if (currentFragment is EmailCompromisedFragment || currentFragment is PasswordCompromisedFragment) {
                addFragment(
                    SubDashBoardFragment.newInstance(5),
                    true,
                    true,
                    animationType = AnimationType.fadeInfadeOut
                )
            } else if (currentFragment is AddGeofenceFragment) {
                addFragment(GeofenceFragment(), true, true, animationType = AnimationType.fadeInfadeOut)
            } else if (currentFragment is ExploreNearByListFragment) {
                if (currentFragment.isFrom) {
                    addFragment(
                        PlacesToVisitFragment(),
                        true,
                        true,
                        animationType = AnimationType.bottomtotop
                    )
                }else {
                    super.onBackPressed()
                }
            }else if (currentFragment is BusinessTrackFragment) {
                if (currentFragment.isFromHistory){
                    addFragment(
                        ReportPaymentHistoryFragment(),
                        true,
                        true,
                        animationType = AnimationType.fadeInfadeOut
                    )
                }else{
                    gotoDashBoard()
                }
            } /*else if (currentFragment is BusinessTrackFragment || currentFragment is ReportFragment) {
            addFragment(SubDashBoardFragment.newInstance(2), false, true, animationType = AnimationType.fadeInfadeOut)
        } else if (currentFragment is BenefitBankFragment || currentFragment is BankFragment) {
            addFragment(SubDashBoardFragment.newInstance(3), false, true, animationType = AnimationType.fadeInfadeOut)
        } else if (currentFragment is MemberListFragment || currentFragment is GeofenceFragment || currentFragment is MemberRouteFragment || currentFragment is LiveMemberFragment) {
            /*if (currentFragment is LiveMemberFragment){
                if (currentFragment.isFrom){
                    gotoDashBoard()
                }else{
                    addFragment(SubDashBoardFragment.newInstance(1), false, true, animationType = AnimationType.fadeInfadeOut)
                }
            }else{
                addFragment(SubDashBoardFragment.newInstance(1), false, true, animationType = AnimationType.fadeInfadeOut)
            }*/
            addFragment(DashBoardFragment(), true, true, animationType = AnimationType.fadeInfadeOut)
        }
        else if (currentFragment is ComparePhotoFragment) {
            addFragment(
                SubDashBoardFragment.newInstance(6),
                true,
                true,
                animationType = AnimationType.fadeInfadeOut
            )
        } */
            else if (currentFragment is SubscriptionHistoryFragment) {
                addFragment(PaymentFragment(), true, true, animationType = AnimationType.fadeInfadeOut)
            } else if (currentFragment is AddNeighbourFragment || currentFragment is NeighbourMapFragment) {
                if (currentFragment is AddNeighbourFragment){
                    currentFragment.videoFileDelete?.let {
                        if (it.exists()) {
                            it.delete()
                        }
                    }
                    when {
                        currentFragment.isFromPlace -> {
                            addFragment(
                                PlacesToVisitFragment(),
                                true,
                                true,
                                animationType = AnimationType.bottomtotop
                            )
                        }
                        currentFragment.isFromActivity -> {
                            gotoDashBoard()
                        }
                        else -> {
                            addFragment(
                                NeighbourFragment(),
                                true,
                                true,
                                animationType = AnimationType.bottomtotop
                            )
                        }
                    }
                }else {
                    addFragment(
                        NeighbourFragment(),
                        true,
                        true,
                        animationType = AnimationType.bottomtotop
                    )
                }
            } else if (currentFragment is DashBoardFragment) {
                finish()
            } else if (currentFragment is NeighborCommentFragment) {
                if (currentFragment.isFrom){
                    addFragment(
                        NeighbourFragment(),
                        true,
                        true,
                        animationType = AnimationType.bottomtotop
                    )
                }else{
                    super.onBackPressed()
                }
            } else if (currentFragment is UpdateSubFragment){
                if (currentFragment.isFromCancelledAllData){
                    finish()
                }else{
                    super.onBackPressed()
                }
            } else {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    // checking permission on runtime above Marshmallow
    @SuppressLint("MissingPermission", "HardwareIds")
    private fun setPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            KotlinPermissions.with(this) // where this is an FragmentActivity instance
                .permissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
                .onAccepted { permissions ->
                    if (permissions.size == 5) {
                        easyWayLocation = EasyWayLocation(this)
                        easyWayLocation.setListener(this)
                        location_per = true
                    }

                }
                .onDenied {
                    if (!settingDialogShown) {
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                            settingDialogShown = true
                        }
                        setPermission()
                    }
                }
                .onForeverDenied {
                    if (!settingDialogShown) {
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                            settingDialogShown = true
                        }
                        easyWayLocation = EasyWayLocation(this)
                        easyWayLocation.setListener(this)
                    }
                }
                .ask()
        } else {
            KotlinPermissions.with(this) // where this is an FragmentActivity instance
                .permissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
                .onAccepted { permissions ->
                    if (permissions.size == 4) {
                        easyWayLocation = EasyWayLocation(this)
                        easyWayLocation.setListener(this)
                        location_per = true
                    }

                }
                .onDenied {
                    setPermission()
                }
                .onForeverDenied {
                    easyWayLocation = EasyWayLocation(this)
                    easyWayLocation.setListener(this)
                }
                .ask()
        }
    }

    // navigation menu item click
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val loginData = appDatabase.loginDao().getAll()
        val previousSelected = currentlySelected()
        when (item.itemId) {
            R.id.nav_dash -> {
                gotoDashBoard()
            }
            R.id.nav_live_member -> {
                if (!purchaseSubscription()) {
                    if (ConnectionUtil.isInternetAvailable(this)) {
                        addFragment(
                            LiveMemberFragment(),
                            true,
                            true,
                            animationType = AnimationType.fadeInfadeOut
                        )
                    } else {
                        Utils.showNoInternetMessage(this@HomeActivity)
                    }
                }
            }
            R.id.nav_news -> {
                if (!purchaseSubscription()) {
                    addFragment(
                        NeighbourFragment(),
                        true,
                        true,
                        animationType = AnimationType.fadeInfadeOut
                    )
                }
            }
            R.id.nav_settings -> {
                addFragment(
                    SubDashBoardFragment.newInstance(SETTING_KEY),
                    true,
                    false,
                    animationType = AnimationType.fadeInfadeOut
                )
            }
            R.id.nav_place_visit -> {
                if (!purchaseSubscription()) {
                    addFragment(
                        PlacesToVisitFragment(),
                        true,
                        true,
                        animationType = AnimationType.fadeInfadeOut
                    )
                }
            }
            R.id.nav_hibp -> {
                if (!purchaseSubscription()) {
                    addFragment(
                        SubDashBoardFragment.newInstance(PWNED),
                        true,
                        false,
                        animationType = AnimationType.fadeInfadeOut
                    )
                }
            }
            R.id.nav_logout -> {
                if (ConnectionUtil.isInternetAvailable(this)) {
                    logoutConfirmDialog(previousSelected)
                } else {
                    Utils.showNoInternetMessage(this@HomeActivity)
                }
            }
            R.id.nav_noUse -> {

            }
            R.id.nav_member_list -> {
                if (!purchaseSubscription()) {
                    if (appDatabase.memberDao().countMember() >= FIXED_USER_COUNT) {
                        showMessage(resources.getString(R.string.member_limit))
                    } else {
                        addFragment(
                            AddMemberFragment.newInstance(
                                false,
                                FamilyMonitorResult(),
                                SubscriptionBean(),
                                true,
                                false,
                                false
                            ), true, true, animationType = AnimationType.fadeInfadeOut
                        )
                    }
                }
            }
            R.id.nav_fencing -> {
                if (!purchaseSubscription()) {
                    addFragment(
                        GeofenceFragment(),
                        true,
                        true,
                        animationType = AnimationType.fadeInfadeOut
                    )
                }
            }
            R.id.nav_child -> {
                if (!purchaseSubscription()) {
                    if (loginData.isAdmin) {
                        if (loginData.isReport) {
                            addFragment(
                                ReportPaymentHistoryFragment(),
                                true,
                                true,
                                animationType = AnimationType.fadeInfadeOut
                            )
                        } else {
                            addFragment(
                                BusinessTrackFragment(),
                                true,
                                true,
                                animationType = AnimationType.fadeInfadeOut
                            )
                        }
                    } else {
                        addFragment(
                            ReportFragment(),
                            true,
                            true,
                            animationType = AnimationType.fadeInfadeOut
                        )
                    }
                }
            }
            R.id.nav_missing_child -> {
                addFragment(MissingDashBoardFragment(), true, true, AnimationType.fadeInfadeOut)
                /*try {
                    visitUrl("market://details?id=com.oldmemissingchild")
                } catch (e: Exception) {
                    e.printStackTrace()
                    visitUrl("http://play.google.com/store/apps/details?id=com.oldmemissingchild")
                }*/
            }
            R.id.nav_injury_referral -> {
                try {
                    visitUrl("market://details?id=com.oldmereferral")
                } catch (e: Exception) {
                    e.printStackTrace()
                    visitUrl("http://play.google.com/store/apps/details?id=com.oldmereferral")
                }
            }
            R.id.nav_search_child -> {
                addFragment(SearchChildFragment(), true, true, AnimationType.fadeInfadeOut)
            }
            R.id.nav_donation -> {
                addFragment(DonationHistoryFragment(), true, true, AnimationType.fadeInfadeOut)
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    fun openLiveStream(isFromNotification: Boolean, channelNameData: String, liveStreamId: String, liveStreamResult: LiveStreamResult = LiveStreamResult(), role: Int = Constants.CLIENT_ROLE_AUDIENCE) {
        if (channelNameData.isNotEmpty()) {
            stopSpeech()
            /*val role = if (isFromNotification) {
                Constants.CLIENT_ROLE_AUDIENCE
            } else {
                Constants.CLIENT_ROLE_BROADCASTER
            }*/
            val intent = Utils.moveToLiveStream(this@HomeActivity, false, channelNameData, liveStreamId, role, liveStreamResult)
            /*intent.putExtra(KEY_CLIENT_ROLE, role)
            intent.putExtra(KEY_CHANNEL_NAME, channelNameData)
            intent.putExtra(KEY_CHANNEL_ID, liveStreamId)
            intent.putExtra("liveStreamData", liveStreamResult)
            intent.putExtra("isFromNotification", false)*/
            liveStreamResultLauncher.launch(intent)
//            startActivity(intent)
            channelName = ""
            id = ""
        }
    }

    private val liveStreamResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let {
                val videoFile = it.getStringExtra("videoFile") ?: ""
                val liveStreamId = it.getStringExtra(KEY_CHANNEL_ID) ?: ""
                if (liveStreamId.isNotEmpty()) {
                    callDeleteLiveStreamApi(liveStreamId.toInt())
                }
                Handler(Looper.getMainLooper()).post {
                    if (videoFile.isNotEmpty()) {
                        moveToAddNewsScreen(videoFile)
                    }
                }
            }
        }
    }

    private fun callDeleteLiveStreamApi(liveStreamId: Int) {
        Utils.deleteLiveStreamApi(this@HomeActivity, liveStreamId, object : CommonApiListener {
            override fun commonResponse(
                status: Boolean,
                message: String,
                responseMessage: String,
                result: String
            ) {}
        }, false)
    }

    private fun moveToAddNewsScreen(videoFile: String) {
        addFragment(
            AddNeighbourFragment.newInstance(false, PlacesResult(), videoFile, true),
            true,
            true,
            animationType = AnimationType.bottomtotop
        )
    }

    fun show911CallPopUp() {
        try {
            val currentFragment: Fragment? = supportFragmentManager.findFragmentById(R.id.frame_container)!!
            if (currentFragment is DashBoardFragment) {
                Comman_Methods.isCustomPopUpShow(this@HomeActivity,
                    message = resources.getString(R.string.conf_call_911),
                    positiveButtonListener = object : PositiveButtonListener {
                        override fun okClickListener() {
                            takeCall("911")
                        }
                    })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun callLiveStreamApi() {
        if (ConnectionUtil.isInternetAvailable(this@HomeActivity)) {
            val gpsTracker = GpsTracker(this@HomeActivity)
            Comman_Methods.isProgressShow(this@HomeActivity)
            isSpeedAvailable()
            val loginObject: LoginObject = appDatabase.loginDao().getAll()
            val jsonObject = JsonObject()

            val emergencyLat = if (gpsTracker != null) {
                if (gpsTracker.getLatitude() != 0.0) {
                    gpsTracker.getLatitude()
                } else {
                    0.0
                }
            } else {
                0.0
            }

            val emergencyLng = if (gpsTracker != null) {
                if (gpsTracker.getLongitude() != 0.0) {
                    gpsTracker.getLongitude()
                } else {
                    0.0
                }
            } else {
                0.0
            }
            if (loginObject.isAdmin) {
                jsonObject.addProperty("AdminId",loginObject.memberID)
                jsonObject.addProperty("MemberID",0)
                jsonObject.addProperty("IsAdmin",true)
            }else{
                jsonObject.addProperty("AdminId",loginObject.adminID)
                jsonObject.addProperty("MemberID",loginObject.memberID)
                jsonObject.addProperty("IsAdmin",false)
            }
            jsonObject.addProperty("Id", 0)
            jsonObject.addProperty("ChannelName", loginObject.memberID.toString())
            jsonObject.addProperty("Lat", emergencyLat)
            jsonObject.addProperty("Lng", emergencyLng)
            jsonObject.addProperty("NotificationBy", 2)
            jsonObject.addProperty("CreatedOn", Utils.getCurrentTimeStamp())

            val memberListCall = WebApiClient.getInstance(this@HomeActivity)
                .webApi_without?.liveStreamNotification(jsonObject)
            memberListCall?.enqueue(object : Callback<CommonValidationResponse> {
                override fun onResponse(
                    call: Call<CommonValidationResponse>,
                    response: Response<CommonValidationResponse>
                ) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {

                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                if (it.status == true) {
                                    val gson: Gson = GsonBuilder().create()
                                    val responseTypeToken: TypeToken<LiveStreamResult> =
                                        object : TypeToken<LiveStreamResult>() {}
                                    val responseData: LiveStreamResult? =
                                        gson.fromJson(
                                            gson.toJson(it.result),
                                            responseTypeToken.type
                                        )
                                    val data = responseData ?: LiveStreamResult()
                                    openLiveStream(false, loginObject.memberID.toString(), (data.id ?: 0).toString(), data, Constants.CLIENT_ROLE_BROADCASTER)
                                } else {
                                    showMessage(it.message ?: "")
                                }
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        Utils.showSomeThingWrongMessage(this@HomeActivity)
                    }
                }

                override fun onFailure(call: Call<CommonValidationResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }
            })
        } else {
            Utils.showNoInternetMessage(this@HomeActivity)
        }
    }
    // logout confirmation dialog
    private fun logoutConfirmDialog(previousPosition: Int) {
        Comman_Methods.isCustomPopUpShow(this@HomeActivity,
            title = resources.getString(R.string.logout_conf),
            message = resources.getString(R.string.wish_logout),
            positiveButtonListener = object : PositiveButtonListener {
                override fun okClickListener() {
                    notificationPermission = if (NotificationManagerCompat.from(this@HomeActivity).areNotificationsEnabled()) {
                        "true"
                    } else {
                        "false"
                    }
                    callLogOutPingApi(LOGOUT_RECORD_STATUS.toString(), true)
                }

                override fun cancelClickLister() {
                    if (previousPosition >= 0){
                        nav_view.menu.getItem(previousPosition).isChecked = true
                    }else{
                        deCheckAllNavigationItem()
                    }
                }
            })
    }

    private fun dropAllTables() {
        appDatabase.loginDao().dropLogin()
        appDatabase.loginRequestDao().dropTable()
        appDatabase.geoFenceDao().dropGeoFence()
        appDatabase.memberDao().dropTable()
        appDatabase.geoFenceDao().dropGeoNotify()
        appDatabase.loginDao().dropPhrases()
    }

    // logout api
    fun callLogOutPingApi(recordStatus: String, isLogout: Boolean) {
        val androidId: String = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        )

        isSpeedAvailable()
        val loginParameter: LoginObject = appDatabase.loginDao().getAll()

        val decimalSymbols = DecimalFormatSymbols.getInstance(Locale.US)

        val oldLatitudeString: String = if (loginParameter.latitude!=null) DecimalFormat(
            "#.####",
            decimalSymbols
        ).format(loginParameter.latitude) else "0.0"
        val oldLongitudeString: String = if (loginParameter.longitude!=null) DecimalFormat(
            "#.####",
            decimalSymbols
        ).format(loginParameter.longitude) else "0.0"

        val oldLatitude: Double = if (loginParameter.latitude!=null) loginParameter.latitude else 0.0
        val oldLongitude: Double = if (loginParameter.longitude!=null) loginParameter.longitude else 0.0

        val newLatitude = DecimalFormat("#.####", decimalSymbols).format(easyWayLocation.latitude)
        val newLongitude = DecimalFormat("#.####", decimalSymbols).format(easyWayLocation.longitude)

        val distance = FloatArray(2)

        Location.distanceBetween(
            oldLatitude, oldLongitude,
            easyWayLocation.latitude, easyWayLocation.longitude, distance
        )

        val distanceInKilometer = distance[0]/1000

        println("!@@@@@distance = ${distance[0]}")
        println("!@@@@@distanceInKilometer = ${distanceInKilometer}")

        val address = if (distance[0] > 50f){
            ""
        }else{
            loginParameter.locationAddress
        }
        /*val address = if (oldLatitudeString!=newLatitude && oldLongitudeString!=newLongitude)
            ""
        else loginParamter.locationAddress*/


        val login_json = LoginRequest()

        login_json.batteryLevel = bettery_level.toString()
        login_json.deviceCompanyName = Comman_Methods.getdevicename()
        login_json.deviceModel = Comman_Methods.getdeviceModel()
        login_json.deviceOS = Comman_Methods.getdeviceVersion()
        login_json.uuid = androidId
        login_json.email = loginParameter.email
        login_json.latitude = DecimalFormat("#.######", decimalSymbols).format(easyWayLocation.latitude)
        login_json.longitude = DecimalFormat("#.######", decimalSymbols).format(easyWayLocation.longitude)
        login_json.recordStatus = recordStatus
        login_json.locationPermission = location_per.toString()
        login_json.locationAddress = address
//                Utils.getCompleteAddressString(this, easyWayLocation.latitude, easyWayLocation.longitude)
        login_json.mobile = ""
        login_json.createdby = loginParameter.memberID.toString()
        login_json.userName = loginParameter.userName
        login_json.id = loginParameter.memberID
        login_json.firstName = loginParameter.firstName
        login_json.lastName = loginParameter.lastName
        login_json.profilePath = loginParameter.profilePath
        login_json.frequency = loginParameter.frequency
        login_json.loginByApp = loginParameter.loginByApp ?: 2
        login_json.devicetypeid = DEVICE_TYPE_ID.toString()
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            login_json.notificationPermission = "true"
        } else {
            login_json.notificationPermission = "false"
        }
        if (isLogout){
            login_json.deviceTokenId = ""
        }else {
            login_json.deviceTokenId = AppPreference.getStringPreference(this@HomeActivity, BuildConfig.firebasePrefKey)
        }
        login_json.deviceType = DEVICE_TYPE
        login_json.startDate = Comman_Methods.getcurrentDate()
        login_json.password = loginParameter.password

        Utils.callLoginPingLogoutApi(this@HomeActivity, login_json, object : CommonApiListener {
            override fun loginResponse(
                status: Boolean,
                loginData: LoginObject?,
                message: String,
                responseMessage: String
            ) {
                if (status) {
                    if (isLogout) {

                        // stop all services
                        val isServiceStarted = isMyServiceRunning(
                            GpsService::class.java,
                            this@HomeActivity
                        )
                        if (isServiceStarted) {
                            stopService(
                                Intent(
                                    this@HomeActivity,
                                    GpsService::class.java
                                )
                            )
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            val jobScheduler =
                                getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                            jobScheduler.cancelAll()
                        }

                        Comman_Methods.delSavedImages(this@HomeActivity)
                        try {
                            unregisterReceiver(reciever)
                            unregisterReceiver(switchOffReceiver)
                            LocalBroadcastManager.getInstance(this@HomeActivity).unregisterReceiver(mMessageReceiver)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        //delete all tables from database
                        dropAllTables()
                        stopSpeech()
                        muteRecognizer(this@HomeActivity, true)
                        if (this@HomeActivity::mServiceIntent.isInitialized) {
                            stopService(mServiceIntent)
                        }
                        mService?.removeLocationUpdates()
                        val notificationManager =
                            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancelAll()
                        Utils.notificationCountID = 0
                        finish()
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                        val intent = Intent(this@HomeActivity, MainActivity::class.java)
                        intent.putExtra("IsLoggedOut", true)
                        startActivity(intent)
                        val file = File(cacheDir.toString() + CACHE_FOLDER_NAME)
                        if (file.exists()) {
                            Comman_Methods.deleteDir(file)
                        }
                    } else {
                        //appDatabase.loginDao().addLogin(login_response.result!!)
                        //tvLiveMemberCount.text = login_response.result!!.count!!.toString()
                    }
                } else {
                    if (message == "Invalid email or password.") {
                        if (recordStatus == "2") {
                            callLogOutPingApi("3", true)
                        } else if (recordStatus == "3") {

                            // stop all services
                            val isServiceStarted = isMyServiceRunning(
                                GpsService::class.java,
                                this@HomeActivity
                            )
                            if (isServiceStarted) {
                                stopService(
                                    Intent(
                                        this@HomeActivity,
                                        GpsService::class.java
                                    )
                                )
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                val jobScheduler =
                                    getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                                jobScheduler.cancelAll()
                            }

                            Comman_Methods.delSavedImages(this@HomeActivity)
                            try {
                                unregisterReceiver(reciever)
                                unregisterReceiver(switchOffReceiver)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            //delete all tables from database
                            appDatabase.loginDao().dropLogin()
                            appDatabase.loginRequestDao().dropTable()
                            appDatabase.geoFenceDao().dropGeoFence()
                            appDatabase.memberDao().dropTable()
                            appDatabase.geoFenceDao().dropGeoNotify()

                            finish()
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                            val intent =
                                Intent(this@HomeActivity, MainActivity::class.java)
                            intent.putExtra("IsLoggedOut", true)
                            startActivity(intent)
                            val file = File(cacheDir.toString() + CACHE_FOLDER_NAME)
                            if (file.exists()) {
                                Comman_Methods.deleteDir(file)
                            }

                        }

                    } else if (currentlySelected() >= 0) {
                        nav_view.menu.getItem(currentlySelected()).isChecked = true
                    } else {
                        deCheckAllNavigationItem()
                    }
                    showMessage(message)
                }
            }

            override fun onFailureResult() {
                if (currentlySelected() >= 0) {
                    nav_view.menu.getItem(currentlySelected()).isChecked = true
                } else {
                    deCheckAllNavigationItem()
                }
                showMessage(resources.getString(R.string.error_message))
            }

        }, isLogout)
    }


    fun showMessage(message: String) {
        Utils.showToastMessage(this@HomeActivity, message)
    }

    fun enableDrawer() {
        drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    fun openDrawer() {
        drawer_layout.openDrawer(GravityCompat.START)
    }

    fun disableDrawer() {
        closeDrawer()
        drawer_layout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    fun checkNavigationItem(position: Int) {
        nav_view.menu.getItem(position).isChecked = true
    }

    fun deCheckAllNavigationItem(){
        for (i: Int in 0 until nav_view.menu.size()){
            nav_view.menu.getItem(i).isChecked = false
        }
    }

    fun currentlySelected(): Int{
        var k = -1
        for (i: Int in 0 until nav_view.menu.size()){
            if (nav_view.menu.getItem(i).isChecked) {
                k=i
            }
        }
        return k
    }

    override fun onDestroy() {
        super.onDestroy()
        muteRecognizer(this@HomeActivity)
        unregisterReceiver(mBatInfoReceiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EasyWayLocation.LOCATION_SETTING_REQUEST_CODE) {
            easyWayLocation.onActivityResult(resultCode)
        } else if (requestCode == MY_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                println("Update flow failed! Result code: $resultCode")
                // If the update is cancelled or fails,
                // you can request to start the update again.
                visitUrl("http://play.google.com/store/apps/details?id=$packageName")
            } else if (resultCode == RESULT_OK){
                finish()
            }
        } else {
            try {
                getVisibleFragment()?.onActivityResult(requestCode, resultCode, data)
            }catch (e: Exception) {
                e.printStackTrace()
            }
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

    val mMessageReceiver = object : BroadcastReceiver() {
        override fun onReceive(app_context: Context, intent: Intent) {
            // Get extra data included in the Intent
            val b = intent.getBundleExtra("LiveCount")
            val count = b?.getInt("count") ?: 0
            if (tvLiveMemberCount!=null) {
                if (nav_view.menu!=null) {
                    nav_view.menu.getItem(1).setActionView(R.layout.menu_image)
                    val tv_count: TextView = nav_view.menu.getItem(1).actionView.findViewById(R.id.tvLiveMemberCount)
                    tv_count.visibility = if (count == 0) View.GONE else View.VISIBLE
                    tv_count.text = count.toString()
                }
            }
        }
    }

    fun changeNavigationLanguage(){
        val previousPosition = currentlySelected()
        requestedOrientation =
            if (resources.getBoolean(R.bool.isTablet)) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        LocaleUtils.setLocale(
            Locale(
                if (AppPreference
                        .getIntPreference(this@HomeActivity
                            , BuildConfig.languagePrefKey) == 0
                ) LocaleUtils.LAN_ENGLISH else LocaleUtils.LAN_SPANISH
            )
        )
        LocaleUtils.updateConfig(this, resources.configuration)
        if (nav_view!=null) {
            if (nav_view.menu!=null) {
                nav_view.menu.clear()
                nav_view.inflateMenu(R.menu.activity_home_drawer)
                if (previousPosition >= 0) {
                    nav_view.menu.getItem(previousPosition).isChecked = true
                }
                val loginData = appDatabase.loginDao().getAll()
                prepareNavigationData(loginData)
            }
        }
    }

    private fun callCheckSubscriptionApi(versionCheck: Boolean) {
        isSpeedAvailable()
        Utils.userSubscriptionCheck(this@HomeActivity, object : CommonApiListener{
            override fun commonResponse(
                status: Boolean,
                message: String,
                responseMessage: String,
                result: String
            ) {
                val isServiceStarted =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Utils.isJobServiceRunning(this@HomeActivity)
                    } else {
                        isMyServiceRunning(GpsService::class.java, this@HomeActivity)
                    }
                LocalBroadcastManager.getInstance(this@HomeActivity).registerReceiver(
                    mMessageReceiver, IntentFilter("LiveMemberCount")
                )
                if (status) {

                    if (result != "active") {
                        stopGpsServices(isServiceStarted)
                        if (!checkMissingSubscription()) {
                            Comman_Methods.isCustomTrackingPopUpHide()
                            addFragment(
                                UpdateSubFragment.newInstance(
                                    true, false,
                                    false, false,
                                    FamilyMonitorResult(), ArrayList(), false, false, true
                                ), false, true, animationType = AnimationType.fadeInfadeOut
                            )
                        }
                    } else if (result == "active") {
                        /*try {
                        if (isAppIsInBackground(this@HomeActivity)) {
                            if (!isServiceStarted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                                val jobScheduler =
                                    getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                                val componentName = ComponentName(
                                    packageName,
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
                                    Log.d(GpsJobService::class.java.name, "job scheduled")
                                } else {
                                    Log.d(
                                        GpsJobService::class.java.name,
                                        "job schedule failed"
                                    )
                                }
                            }else if (!isServiceStarted) {
                                startService(Intent(this@HomeActivity, GpsService::class.java))
                            }
                        } else {
                            if (!isServiceStarted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                                val jobScheduler =
                                    getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                                val componentName = ComponentName(
                                    packageName,
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
                                    Log.d(GpsJobService::class.java.name, "job scheduled")
                                } else {
                                    Log.d(
                                        GpsJobService::class.java.name,
                                        "job schedule failed"
                                    )
                                }
                            }else if (!isServiceStarted) {
                                startService(Intent(this@HomeActivity, GpsService::class.java))
                            }
                        }
                    } catch (e: Exception){
                        e.printStackTrace()
                    }*/
                        requiredChanges(versionCheck)
                    }
                } else {
                    stopGpsServices(isServiceStarted)
                    if (!checkMissingSubscription()) {
                        Comman_Methods.isCustomTrackingPopUpHide()
                        addFragment(
                            UpdateSubFragment.newInstance(
                                true, false,
                                false, false,
                                FamilyMonitorResult(), ArrayList(), false, true, true
                            ), false, true, animationType = AnimationType.fadeInfadeOut
                        )
                    }
//                            }
                }
            }
        })
    }

    fun stopGpsServices(isServiceStarted: Boolean) {
        if (isAppIsInBackground(this@HomeActivity)) {
            if (isServiceStarted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val jobScheduler =
                    getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                jobScheduler.cancel(GPSSERVICEJOBID)
            } else if (isServiceStarted) {
                stopService(
                    Intent(
                        this@HomeActivity,
                        GpsService::class.java
                    )
                )
            }
        } else {
            if (isServiceStarted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val jobScheduler =
                    getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                jobScheduler.cancel(GPSSERVICEJOBID)
            } else if (isServiceStarted) {
                stopService(
                    Intent(
                        this@HomeActivity,
                        GpsService::class.java
                    )
                )
            }
        }
    }

    fun isSpeedAvailable(): Boolean {
        val connectivity = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (connectivity != null) {
            val info = connectivity.allNetworkInfo

            if (info != null) {
                for (i in info.indices) {
                    if (info[i].state == NetworkInfo.State.CONNECTED) {
                        if (!isConnectionSpeed(info[i].type, info[i].subtype)){
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

    private fun checkVersionCodeWithGoogle(){
        if (ConnectionUtil.isInternetAvailable(this)){
            if (Build.VERSION.SDK_INT >= 21){
                // Creates instance of the manager.
                appUpdateManager = AppUpdateManagerFactory.create(this@HomeActivity)
                // Returns an intent object that you use to check for an update.
                appUpdateInfoTask = appUpdateManager.appUpdateInfo

                // Checks that the platform will allow the specified type of update.
                appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        // For a flexible update, use AppUpdateType.FLEXIBLE
                        && appUpdateInfo.isUpdateTypeAllowed(
                            AppUpdateType.IMMEDIATE
                        )
                    ) {
                        // Request the update.
                        appUpdateManager.startUpdateFlowForResult(
                            // Pass the intent that is returned by 'getAppUpdateInfo()'.
                            appUpdateInfo,
                            // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
                            AppUpdateType.IMMEDIATE,
                            // The current activity making the update request.
                            this@HomeActivity,
                            // Include a request code to later monitor this update request.
                            MY_REQUEST_CODE
                        )
                    }
                }
            }else{
                checkVersionCode()
            }
        }
    }

    private fun checkVersionCode(){
        if (ConnectionUtil.isInternetAvailable(this)){
            val callVersionUpdate = WebApiClient.getInstance(this).webApi_without?.getAppVersion(3)
            callVersionUpdate?.enqueue(object : Callback<ApiResponse> {
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}

                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            if (it.status) {
                                if (compareVersionNames(
                                        BuildConfig.VERSION_NAME, it.result
                                    )) {
                                    try {
                                        Comman_Methods.isCustomPopUpShow(this@HomeActivity,
                                            title = resources.getString(R.string.str_update),
                                            message = resources.getString(
                                                R.string.update_message, BuildConfig.VERSION_NAME,
                                                it.result),
                                            positiveButtonText = resources.getString(R.string.str_update),
                                            singlePositive = true,
                                            positiveButtonListener = object : PositiveButtonListener {
                                                override fun okClickListener() {
                                                    visitUrl("http://play.google.com/store/apps/details?id=$packageName")
                                                }
                                            })
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    }
                }
            })
        }
    }

    fun compareVersionNames(oldVersionName: String, newVersionName: String): Boolean {
        try {
            var existingVersion = oldVersionName
            var newVersion = newVersionName
            if (existingVersion.isEmpty() || newVersion.isEmpty()) {
                return false
            }

            existingVersion = existingVersion.replace(".", "")
            newVersion = newVersion.replace(".", "")

            val existingVersionLength: Int = existingVersion.length
            val newVersionLength: Int = newVersion.length

            val versionBuilder = StringBuilder()
            if (newVersionLength > existingVersionLength) {
                versionBuilder.append(existingVersion)
                for (i in existingVersionLength until newVersionLength) {
                    versionBuilder.append("0")
                }
                existingVersion = versionBuilder.toString()
            } else if (existingVersionLength > newVersionLength) {
                versionBuilder.append(newVersion)
                for (i in newVersionLength until existingVersionLength) {
                    versionBuilder.append("0")
                }
                newVersion = versionBuilder.toString()
            }

            return newVersion.toInt() > existingVersion.toInt()
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            if (mBound) {
                unbindService(mServiceConnection)
                mBound = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            bindService(
                Intent(this, LocationUpdatesService::class.java),
                mServiceConnection,
                Context.BIND_AUTO_CREATE
            )
            Handler(Looper.getMainLooper()).postDelayed({
                mService?.requestLocationUpdates()
            }, 3000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        stopSpeech()
    }

    override fun onStop() {
        if (mBound) {
            unbindService(mServiceConnection)
            mBound = false
        }
        stopSpeech()
        super.onStop()
    }

    fun stopSpeech() {
        if (speech != null) {
            speech?.stopListening()
            speech?.cancel()
            speech?.destroy()
        }
        stopLiveStreamTimer()
        stopMissingChildTask()
    }

    fun stopLiveStreamTimer() {
        try {
            timerLiveStream?.cancel()
            updateLiveStream?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopMissingChildTask() {
        try {
            timerMissingTask?.cancel()
            updateMissingTask?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkAllSubscription(versionCheck: Boolean) {
        val loginData = appDatabase.loginDao().getAll()
        if (loginData != null) {
            if (loginData.isAdmin!=null) {
                if (loginData.isAdmin) {
                    callCheckSubscriptionApi(versionCheck)
                } else {
                    checkMemberSubscriptionApi(versionCheck)
                }
            }
//            callLogOutPingApi("2",false)
        }
    }

    override fun onResume() {
        super.onResume()
        /*checkPermissions()
        val currentFragment: Fragment? = supportFragmentManager.findFragmentById(R.id.frame_container)!!
        if (currentFragment is DashBoardFragment) {
            startLiveStreamTime(currentFragment)
        }
        if (currentFragment is MissingChildTaskFragment) {
            startMissingChildTaskTime(currentFragment)
        }*/
        checkDeviceSubscriptionApi(false)
        /*if (Build.VERSION.SDK_INT >= 21) {
            appUpdateManager = AppUpdateManagerFactory.create(this@HomeActivity)
            appUpdateManager
                .appUpdateInfo
                .addOnSuccessListener { appUpdateInfo ->
                    // If the update is downloaded but not installed,
                    // notify the user to complete the update.
                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        if (appDatabase.loginDao().getAll() != null) {
                            popupSnackBarForCompleteUpdate()
                        }
                    } else if (appUpdateInfo.updateAvailability()
                        == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                    ) {
                        // If an in-app update is already running, resume the update.
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            IMMEDIATE,
                            this@HomeActivity,
                            MY_REQUEST_CODE
                        )
                    }
                }
        }*/
    }

    fun startLiveStreamTime(currentFragment: DashBoardFragment) {
        stopLiveStreamTimer()
        try {
            timerLiveStream = Timer()
            updateLiveStream = currentFragment.CustomLiveStreamTimerTask()
            timerLiveStream?.scheduleAtFixedRate(updateLiveStream, 0, 15000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startMissingChildTaskTime(currentFragment: MissingChildTaskFragment) {
        stopMissingChildTask()
        try {
            timerMissingTask = Timer()
            updateMissingTask = currentFragment.CustomMissingChildTask()
            timerMissingTask?.scheduleAtFixedRate(updateMissingTask, 0, 15000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun popupSnackBarForCompleteUpdate() {
        Snackbar.make(
            coordinator,
            resources.getString(R.string.str_update_download),
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction(resources.getString(R.string.str_restart)) { appUpdateManager.completeUpdate() }
            setActionTextColor(ContextCompat.getColor(this@HomeActivity, R.color.special_green))
            show()
        }
    }

    private fun downLoadTask(url: URL?) {
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            var downLoadBitmap: Bitmap? = null
            var connection: HttpURLConnection? = null
            try {
                connection = url?.openConnection() as HttpURLConnection
                connection.connect()
                val inputStream = connection.inputStream
                val bufferedInputStream = BufferedInputStream(inputStream)
                downLoadBitmap = BitmapFactory.decodeStream(bufferedInputStream)
            } catch (e: Exception) {
                e.printStackTrace()
                println("!@@@e.message = ${e.message}")
            } finally {
                // Disconnect the http url connection
                connection?.disconnect()
            }
            handler.post {

                if (downLoadBitmap != null) {
                    // Display the downloaded image into ImageView
                    // sdv_profile_image.setImageBitmap(result);

                    println("!@@@DownloadTask.onPostExecute")
                    // Save bitmap to internal storage
                    try {

                        val degrees :Float= 0f
                        val matrix: Matrix =  Matrix()
                        matrix.setRotate(degrees)
                        val bOutput = Bitmap.createBitmap(
                            downLoadBitmap,
                            0,
                            0,
                            downLoadBitmap.width,
                            downLoadBitmap.height,
                            matrix,
                            true
                        )

                        val imageInternalUri = saveImageToInternalStorage(bOutput)

                        val file = File(imageInternalUri.toString())
                        if (file != null) {
                            val uri: Uri = Uri.fromFile(file)

                            if (uri != null) {
                                nav_view.getHeaderView(0).sdv_profile_image.setImageURI(uri.toString())
                                nav_view.getHeaderView(0).sdv_profile_image.loadImage(
                                    this@HomeActivity,
                                    uri.toString()
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                } else {
                    // Notify user that an error occurred while downloading image
                }
            }
        }
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri {
        var fileName = "ProfileImage.jpg"
        var file: File? = null
        if (appDatabase.loginDao().getAll() != null) {
            val profilePath = appDatabase.loginDao().getAll().profilePath ?: ""
            if (profilePath.split("Family/")[1] != "") {
                fileName = profilePath.split("Family/")[1]
            }
            file = Comman_Methods.createSignatureDir(this, fileName)
            try {
                // Initialize a new OutputStream
                var stream: OutputStream? = null

                // If the output file exists, it can be replaced or appended to it
                stream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 60, stream)

                // Flushes the stream
                stream.flush()

                // Closes the stream
                stream.close()


                Comman_Methods.isProgressHide()
//            mActivity.setUpdatedImage()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Parse the gallery image url to uri

        // Return the saved image Uri
        return Uri.parse(if (file != null) file.absolutePath else "")
    }

    fun onLiveMemberPingCalling(previousPosition: Int){
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        }
        if (previousPosition >= 0){
            nav_view.menu.getItem(previousPosition).isChecked = true
        }else{
            deCheckAllNavigationItem()
        }
        Comman_Methods.isCustomPopUpHide()
    }

    fun checkUserActive(versionCheck: Boolean = false){
        val appDatabase = OldMe911Database.getDatabase(this)
        val loginObject = appDatabase.loginDao().getAll()

        val memberId = loginObject.memberID
        if (ConnectionUtil.isInternetAvailable(this@HomeActivity)) {
            isSpeedAvailable()

            val callIsUserActive = WebApiClient.getInstance(this).webApi_without?.callCheckActiveStatus(memberId)

            callIsUserActive?.enqueue(object : Callback<ApiResponse> {
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {

                }

                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            response.body()?.let {
                                if (!it.status) {
                                    alertActiveUser(it.result)
                                } else {
                                    if (versionCheck){
                                        checkVersionCode()
                                    }
                                }
                            }
                        }
                    } else {
                        Utils.showSomeThingWrongMessage(this@HomeActivity)
                    }
                }
            })
        } else {
            Utils.showNoInternetMessage(this@HomeActivity)
        }
    }

    private fun alertActiveUser(message: String) {
        Comman_Methods.isCustomPopUpHide()
        Comman_Methods.isCustomPopUpShow(this@HomeActivity,
            title = resources.getString(R.string.str_alert), message = message,
            negativeButtonText = resources.getString(R.string.logout), singleNegative = true,
            positiveButtonListener = object : PositiveButtonListener {
                override fun cancelClickLister() {
                    callLogOutPingApi(LOGOUT_RECORD_STATUS.toString(), true)
                }
            })
    }



    private fun startSpeechRecognition(){

        if (ConnectionUtil.isInternetAvailable(this@HomeActivity)) {
            Log.i(
                "LOG_TAG",
                "isRecognitionAvailable: " + SpeechRecognizer.isRecognitionAvailable(this)
            )
            if (SpeechRecognizer.isRecognitionAvailable(this)) {
                speech = SpeechRecognizer.createSpeechRecognizer(this)
                Utils.muteRecognizer(this@HomeActivity)
                speech?.setRecognitionListener(this)
                recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                recognizerIntent?.apply {
//                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "US-en")
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
//                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                speech?.startListening(recognizerIntent)
            } else {
                showMessage("Speech Recognition service not available")
            }
        } else {
            Utils.showNoInternetMessage(this@HomeActivity)
        }
    }

    fun checkPermissions(onStart: Boolean = true, isFromApi: Boolean = false) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            KotlinPermissions.with(this@HomeActivity) // where this is an FragmentActivity instance
                .permissions(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO
                )

                .onAccepted { permissions ->
                    if (permissions.size == 3) {
                        if (isFromApi) {
                            callLiveStreamApi()
                        } else {
                            openLiveStream(true, channelName, id)
                            if (onStart) {
                                startSpeechRecognition()
                            } else {
                                googleCheck()
                            }
                        }
                    }
                }
                .onDenied {
                    checkPermissions(onStart, isFromApi)
                }
                .onForeverDenied {
                    showMessage(resources.getString(R.string.permission_app_set))
                }
                .ask()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            KotlinPermissions.with(this@HomeActivity) // where this is an FragmentActivity instance
                .permissions(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO
                )

                .onAccepted { permissions ->
                    if (permissions.size == 4) {
                        if (isFromApi) {
                            callLiveStreamApi()
                        } else {
                            openLiveStream(true, channelName, id)
                            if (onStart) {
                                startSpeechRecognition()
                            } else {
                                googleCheck()
                            }
                        }
                    }
                }
                .onDenied {
                    checkPermissions(onStart, isFromApi)
                }
                .onForeverDenied {
                    showMessage(resources.getString(R.string.permission_app_set))
                }
                .ask()
        } else {
            if (isFromApi) {
                callLiveStreamApi()
            } else {
                openLiveStream(true, channelName, id)
                if (onStart) {
                    startSpeechRecognition()
                } else {
                    googleCheck()
                }
            }
        }
    }

    private fun appInstalledOrNot(uri: String): Boolean {
        return try {
            packageManager.getPackageInfo(uri, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: java.lang.Exception){
            false
        }
    }

    private fun googleCheck() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS

        Comman_Methods.isCustomPopUpShow(this@HomeActivity,
            message = resources.getString(R.string.str_allow_mic_permission),
            positiveButtonText = resources.getString(R.string.action_settings),
            singlePositive = true,
            positiveButtonListener = object : PositiveButtonListener {
                override fun okClickListener() {
                    when {
                        appInstalledOrNot("com.google.android.apps.searchlite") -> {
                            val uri = Uri.fromParts("package", "com.google.android.apps.searchlite", null)
                            intent.data = uri
                            googleLauncher.launch(intent)
                        }
                        appInstalledOrNot("com.google.android.googlequicksearchbox") -> {
                            val uri = Uri.fromParts("package", "com.google.android.googlequicksearchbox", null)
                            intent.data = uri
                            googleLauncher.launch(intent)
                        }
                        else -> {
                            showMessage("Speech Recognition service not available")
                        }
                    }
                }
            })
    }

    var googleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        startSpeechRecognition()
    }

    override fun onReadyForSpeech(params: Bundle?) {  }

    override fun onBeginningOfSpeech() {  }

    override fun onRmsChanged(rmsdB: Float) {  }

    override fun onBufferReceived(buffer: ByteArray?) {  }

    override fun onEndOfSpeech() {  }

    override fun onError(error: Int) {
        val errorMessage: String = getErrorText(error)
    }

    override fun onResults(results: Bundle?) {

        val arrPhrases = appDatabase.loginDao().getAllPhrases()
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: ArrayList()

        var text = ""
        for (result in matches) text = result.trimIndent()
        var isMatched = false
        for (i in arrPhrases.indices){
            if (text.equals(arrPhrases[i].voiceText ,true)){
                isMatched = true
            }
        }

        if (isMatched){
            checkPermissions(false, true)
        }else{
            if (ConnectionUtil.isInternetAvailable(this@HomeActivity)) {
                muteRecognizer(this@HomeActivity)
                speech?.startListening(recognizerIntent)
            } else {
                Utils.showNoInternetMessage(this@HomeActivity)
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {  }

    override fun onEvent(eventType: Int, params: Bundle?) {  }

    private fun getErrorText(errorCode: Int): String {
        when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> {
                return "Audio recording error"
            }
            SpeechRecognizer.ERROR_CLIENT -> {
                return "Client side error"
            }
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                checkPermissions(false)
                return "Insufficient permissions"
            }
            SpeechRecognizer.ERROR_NETWORK -> {
                return "Network error"
            }
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                return "Network timeout"
            }
            SpeechRecognizer.ERROR_NO_MATCH -> {
                if (ConnectionUtil.isInternetAvailable(this@HomeActivity)) {
                    muteRecognizer(this@HomeActivity)
                    speech?.startListening(recognizerIntent)
                } else {
                    Utils.showNoInternetMessage(this@HomeActivity)
                }
                return "No match"
            }
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                return "RecognitionService busy"
            }
            SpeechRecognizer.ERROR_SERVER -> {
                return "error from server"
            }
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                if (ConnectionUtil.isInternetAvailable(this@HomeActivity)) {
                    muteRecognizer(this@HomeActivity)
                    speech?.startListening(recognizerIntent)
                } else {
                    Utils.showNoInternetMessage(this@HomeActivity)
                }
                return "No speech input"
            }
            else -> {
                return "Didn't understand, please try again"
            }
        }
    }

    fun openClientCallDialog(){
        val loginData = appDatabase.loginDao().getAll()
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialogLayout = inflater.inflate(R.layout.popup_client_call, null)
        val mDialog = AlertDialog.Builder(this@HomeActivity)
        mDialog.setView(dialogLayout)
        if (this::clientCallDialog.isInitialized){
            if (clientCallDialog.isShowing){
                clientCallDialog.dismiss()
            }
        }
        clientCallDialog = mDialog.create()
        clientCallDialog.window?.attributes?.windowAnimations = R.style.animationForDialog
        clientCallDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val sdvClientImage: SimpleDraweeView = dialogLayout.findViewById(R.id.sdvClientImage)
        val tvClientNumber: TextView = dialogLayout.findViewById(R.id.tvClientNumber)
        val btnClientCancel: Button = dialogLayout.findViewById(R.id.btnClientCancel)
        val btnClientCall: Button = dialogLayout.findViewById(R.id.btnClientCall)
        val flClientCall: FrameLayout = dialogLayout.findViewById(R.id.flClientCall)

        val clientNumber = loginData.clientMobileNumber ?: ""
        val clientImageUrl = loginData.clientImageUrl ?: ""

        sdvClientImage.loadFrescoImage(this@HomeActivity, clientImageUrl, 1)
        tvClientNumber.text = clientNumber
        btnClientCall.setOnClickListener {
            hideKeyboard()
            if (Comman_Methods.isSimExists(this@HomeActivity)) {
                Comman_Methods.avoidDoubleClicks(it)
                takeCall(clientNumber)
            } else {
                showMessage(resources.getString(R.string.str_sim_prob))
            }
            clientCallDialog.dismiss()
        }
        flClientCall.setOnClickListener {
            hideKeyboard()
            if (Comman_Methods.isSimExists(this@HomeActivity)) {
                Comman_Methods.avoidDoubleClicks(it)
                takeCall(clientNumber)
            } else {
                showMessage(resources.getString(R.string.str_sim_prob))
            }
            clientCallDialog.dismiss()
        }
        btnClientCancel.setOnClickListener {
            hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            clientCallDialog.dismiss()
        }
        clientCallDialog.setCancelable(false)
        clientCallDialog.show()
    }
}