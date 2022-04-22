package com.keepSafe911.gps

import android.Manifest
import android.app.*
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.*
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.keepSafe911.model.GeoFenceResult
import com.keepSafe911.model.request.LoginRequest
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.keepSafe911.BuildConfig
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import com.keepSafe911.R
import com.keepSafe911.listner.CommonApiListener
import isAppIsInBackground
import java.text.DecimalFormatSymbols
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)

class GpsJobService : JobService(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
    LocationListener {
    override fun onConnected(p0: Bundle?) {
        Log.d("", "Location update started ..............: ")
        startLocationUpdates()
    }

    override fun onConnectionSuspended(p0: Int) {
        Log.i("suspended---", "GoogleApiClient connection has been suspend")
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.e("connectionfail----", "onConnectionFailed:------------ ")
    }

    override fun onLocationChanged(location: Location) {
        Log.e("LocationChange", "Firing onLocationChanged..............................................")

        mCurrentLocation = location
        lat = location.latitude
        lng = location.longitude
        sendMessageToActivity(context)
    }

    private var locationPermission = "false"
    private var onlineCount = 0
    private lateinit var context: Context
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mCurrentLocation: Location
    private var mGoogleApiClient: GoogleApiClient? = null
    private var gpsLatitude: Double = 0.toDouble()
    private var gpsLongitude: Double = 0.toDouble()
    private lateinit var geofencingClient: GeofencingClient
    lateinit var appDatabase: OldMe911Database
    var geoFenceCurrentList: ArrayList<GeoFenceResult> = ArrayList()
    var duplicateGeoFenceCurrentList: ArrayList<GeoFenceResult> = ArrayList()
    var mGeofences: ArrayList<Geofence> = ArrayList()


    companion object{
        private val TWO_MINUTES = 1000 * 60 * 2
        var mTimerTask: MyJobTimerTask? = null
        var mTimer: Timer? = null
        var lat: Double = 0.toDouble()
        var lng: Double = 0.toDouble()
        private val REQUEST_CODE_RECOVER_PLAY_SERVICES = 200

        private fun sendMessageToFragment(count: Int, context: Context) {
            val intent = Intent("LiveMemberCount")
            // You can also include some extra data.
            val b = Bundle()
            b.putInt("count", count)
            intent.putExtra("LiveCount", b)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }

        private fun sendMessageToActivity(context: Context) {
            val intent = Intent("SubscribeTrial")
            // You can also include some extra data.
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }
    override fun onStopJob(params: JobParameters?): Boolean {
        if (mTimer != null && mTimerTask != null) {
            mTimer?.cancel()
            mTimerTask?.cancel()
        }
        try {
            stopLocationUpdates()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onlineCount = 0
        return true
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d("onStartCommand", "onStartCommand called...")
        var freq = 0
        if (appDatabase.loginDao().getAll()!=null) {
            freq = appDatabase.loginDao().getAll().frequency ?: 0
        }
        if (appDatabase.loginDao().getAll()!=null) {
            if (appDatabase.loginDao().getAll().isReport != null) {
                if (appDatabase.loginDao().getAll().isReport) {
                    when {
                        freq != null -> if (freq != 0) {
                            when {
                                freq < 60 -> {
                                    val loginupdate: LoginObject = appDatabase.loginDao().getAll()
                                    if (loginupdate != null) {
                                        loginupdate.frequency = freq * 60
                                        appDatabase.loginDao().updateLogin(loginupdate)
                                        mTimer?.scheduleAtFixedRate(
                                            mTimerTask,
                                            0,
                                            (freq * 60000).toLong()
                                        )
                                    }
                                }
                                freq > 14400 -> {
                                    val loginupdate: LoginObject = appDatabase.loginDao().getAll()
                                    if (loginupdate != null) {
                                        loginupdate.frequency = 14400
                                        appDatabase.loginDao().updateLogin(loginupdate)
                                        mTimer?.scheduleAtFixedRate(
                                            mTimerTask,
                                            0,
                                            (14400 * 1000).toLong()
                                        )
                                    }
                                }
                                else -> mTimer?.scheduleAtFixedRate(
                                    mTimerTask,
                                    0,
                                    (freq * 1000).toLong()
                                )
                            }
                        } else {
                            val loginupdate: LoginObject = appDatabase.loginDao().getAll()
                            if (loginupdate != null) {
                                loginupdate.frequency = 60
                                appDatabase.loginDao().updateLogin(loginupdate)
                                mTimer?.scheduleAtFixedRate(mTimerTask, 0, 60000)
                            }
                        }
                        else -> {
                            val loginupdate: LoginObject = appDatabase.loginDao().getAll()
                            if (loginupdate != null) {
                                loginupdate.frequency = 60
                                appDatabase.loginDao().updateLogin(loginupdate)
                                mTimer?.scheduleAtFixedRate(mTimerTask, 0, 60000)
                            }
                        }
                    }
                }
            }
        }
        return true
    }

    override fun onCreate() {
        mTimer = Timer()
        context = this
        appDatabase = OldMe911Database.getDatabase(applicationContext)
        mTimerTask = MyJobTimerTask()
        //		if (checkGooglePlayServices())
        //		{
        buildGoogleApiClient()
        //prepare connection request
        createLocationRequest()
        //}
        if ((mGoogleApiClient?.isConnecting == false) && (mGoogleApiClient?.isConnected == false)) {
            mGoogleApiClient?.connect()
        }
        geofencingClient = LocationServices.getGeofencingClient(context)
    }

    /*LocationRequest method*/
    protected fun createLocationRequest() {
        mLocationRequest = LocationRequest.create()
        var freq = 0
        if (appDatabase.loginDao().getAll()!=null) {
            freq = appDatabase.loginDao().getAll().frequency ?: 0
        }
        if (appDatabase.loginDao().getAll()!=null) {
            if (appDatabase.loginDao().getAll().isReport != null) {
                if (appDatabase.loginDao().getAll().isReport) {
                    when {
                        freq != null -> if (freq != 0) {
                            when {
                                freq < 60 -> {
                                    mLocationRequest.interval = 50000
                                    mLocationRequest.fastestInterval = 40000
                                }
                                freq > 14400 -> {
                                    mLocationRequest.interval = (14000 * 1000).toLong()
                                    mLocationRequest.fastestInterval = (13000 * 1000).toLong()
                                }
                                else -> {
                                    mLocationRequest.interval = ((freq * 1000) - 10000).toLong()
                                    mLocationRequest.fastestInterval =
                                        ((freq * 1000) - 20000).toLong()
                                }
                            }
                        } else {
                            mLocationRequest.interval = 50000
                            mLocationRequest.fastestInterval = 40000
                        }
                        else -> {
                            mLocationRequest.interval = 50000
                            mLocationRequest.fastestInterval = 40000
                        }
                    }
                }
            }
        }
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    @Synchronized
    protected fun buildGoogleApiClient() {
        mGoogleApiClient = GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build()
    }

    override fun onStart(intent: Intent?, startId: Int) {
        super.onStart(intent, startId)
        if (mGoogleApiClient != null) {
            mGoogleApiClient?.connect()
        }
    }

    inner class MyJobTimerTask : TimerTask() {
        private val toastHandler = Handler(Looper.getMainLooper())
        override fun run() {
            Thread(Runnable {
                toastHandler.post {

                    try {
                        startLocationUpdates()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    //  Appconstants.WorkerName = prefs.getString("WORKERNAME","NONAME");

                    val gpsTracker = GpsTracker(context)
                    gpsLatitude = if (gpsTracker.getLatitude() != null) if (gpsTracker.getLatitude() != 0.0) gpsTracker.getLatitude() else lat else lat
                    gpsLongitude = if (gpsTracker.getLongitude() != null) if (gpsTracker.getLongitude() != 0.0) gpsTracker.getLongitude() else lng else lng

                    if (gpsTracker.isGPSEnable() && gpsTracker.isNetWorkProviderEnable()) {
                        if (appDatabase.loginDao().getAll()!=null) {
                            if (appDatabase.loginDao().getAll().isReport != null) {
                                if (appDatabase.loginDao().getAll().isReport) {

                                    if (isAppIsInBackground(context)) {
                                        val connectivityManager =
                                            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            val connectivityCallback: ConnectivityManager.NetworkCallback =
                                                object : ConnectivityManager.NetworkCallback() {
                                                    override fun onAvailable(network: Network) {
                                                        Log.d("!@@@Internet", "onAvailable: ")
                                                        try {
                                                            if (gpsLatitude != 0.0 && gpsLongitude != 0.0) {
                                                                backGroundPing()
                                                            }
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                    }

                                                    override fun onLost(network: Network) {
                                                        storePingDataInDatabase(gpsLatitude, gpsLongitude)
                                                    }
                                                }
                                            connectivityManager.registerNetworkCallback(
                                                NetworkRequest.Builder()
                                                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                                                    .build(), connectivityCallback
                                            )
                                        } else {
                                            if (ConnectionUtil.isInternetAvailable(applicationContext)) {
                                                try {
                                                    if (gpsLatitude != 0.0 && gpsLongitude != 0.0) {
                                                        backGroundPing()
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            } else {
                                                storePingDataInDatabase(gpsLatitude, gpsLongitude)
                                            }
                                        }
                                    } else {
                                        if (ConnectionUtil.isInternetAvailable(applicationContext)) {
                                            try {
                                                if (gpsLatitude != 0.0 && gpsLongitude != 0.0) {
                                                    backGroundPing()
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        } else {
                                            storePingDataInDatabase(gpsLatitude, gpsLongitude)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val mBuilder = NotificationCompat.Builder(applicationContext)
                            .setSmallIcon(Utils.getNotificationIcon())
                            .setStyle(NotificationCompat.BigTextStyle().bigText(context.resources.getString(R.string.locationMessage)))
                            .setContentTitle(context.resources.getString(R.string.locationError))
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setContentText(context.resources.getString(R.string.locationMessage))
                        val ntfIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        ntfIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        /*val stackBuilder = TaskStackBuilder.create(applicationContext)
                        stackBuilder.addParentStack(NotificationActivity::class.java)
                        stackBuilder.addNextIntent(ntfIntent)*/

                        val resultPendingIntent1 = PendingIntent.getActivity(context, 0, ntfIntent, PendingIntent.FLAG_ONE_SHOT)
                        mBuilder.setContentIntent(resultPendingIntent1)
                        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val channelId = context.getString(R.string.default_notification_channel_id)
                            val channel = NotificationChannel(channelId, context.resources.getString(R.string.locationError), NotificationManager.IMPORTANCE_HIGH)
                            channel.description = context.resources.getString(R.string.locationMessage)
                            channel.enableLights(true)
                            channel.lightColor = Color.GREEN
                            channel.setShowBadge(true)
                            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                            nm.createNotificationChannel(channel)
                            mBuilder.setChannelId(channelId)
                        }
                        nm.notify(1235, mBuilder.notification)
                        Log.e("Warning", "GPS NOT ENABLED!!!")
                    }
                }
            }).start()
        }
    }

    private fun storePingDataInDatabase(gpsLatitude: Double, gpsLongitude: Double) {
        val dbhelper =
            OldMe911Database.getDatabase(applicationContext)
        val c = Calendar.getInstance()
        val df = SimpleDateFormat("MM-dd-yyyy HH:mm:ss")
        val formattedDate = df.format(c.time)

        try {
            //Log.e("latlong0ffline---------", "" + mCurrentLocation.getLatitude() + " " + mCurrentLocation.getLongitude());

            Log.e(
                "latlong0ffline---------",
                "$gpsLatitude $gpsLongitude"
            )
            if (gpsLatitude != 0.0 && gpsLongitude != 0.0) {

                val androidId: String =
                    Settings.Secure.getString(
                        context.contentResolver,
                        Settings.Secure.ANDROID_ID
                    )

                //byte[] encodeValue = Base64.encode(getCompleteAddressString(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()).getBytes(), Base64.DEFAULT);
                val locationObject = dbhelper.loginDao().getAll()

                val decimalSymbols = DecimalFormatSymbols.getInstance(Locale.US)

                val oldLatitudeString: String =
                    if (locationObject.latitude != null) DecimalFormat(
                        "#.####", decimalSymbols
                    ).format(
                        locationObject.latitude
                    ) else "0.0"
                val oldLongitudeString: String =
                    if (locationObject.longitude != null) DecimalFormat(
                        "#.####", decimalSymbols
                    ).format(locationObject.longitude) else "0.0"

                val oldLatitude: Double =
                    if (locationObject.latitude != null) locationObject.latitude else 0.0
                val oldLongitude: Double =
                    if (locationObject.longitude != null) locationObject.longitude else 0.0

                val newLatitude =
                    DecimalFormat("#.####", decimalSymbols).format(gpsLatitude)
                val newLongitude =
                    DecimalFormat("#.####", decimalSymbols).format(gpsLongitude)

                val distance = FloatArray(2)

                Location.distanceBetween(
                    oldLatitude, oldLongitude,
                    gpsLatitude, gpsLongitude, distance
                )

                val distanceInKilometer = distance[0] / 1000

                println("!@@@@@distance = ${distance[0]}")
                println("!@@@@@distanceInKilometer = ${distanceInKilometer}")

                val address = if (distance[0] > 50f) {
                    /*Utils.encodeString(
                    Utils.getCompleteAddressString(
                        context,
                        gpsLatitude,
                        gpsLongitude
                    )
                )*/
                    ""
                } else {
                    locationObject.locationAddress
                }
                /*val address = if (oldLatitudeString!=newLatitude && oldLongitudeString!=newLongitude)
        Utils.encodeString(Utils.getCompleteAddressString(
            context,
            gpsLatitude,
            gpsLongitude
        ))
    else locationObject.locationAddress*/
                //Log.e("address", "handleMessage: "+new String(encodeValue) );
                val loginRequestObject = LoginRequest()
                loginRequestObject.id = locationObject.memberID
                loginRequestObject.recordStatus =
                    PIN_RECORD_STATUS.toString()
                loginRequestObject.email = locationObject.email
                loginRequestObject.firstName =
                    locationObject.firstName
                loginRequestObject.lastName =
                    locationObject.lastName
                loginRequestObject.latitude = gpsLatitude.toString()
                loginRequestObject.locationAddress = address
                loginRequestObject.longitude =
                    gpsLongitude.toString()
                loginRequestObject.password =
                    locationObject.password
                loginRequestObject.profilePath =
                    locationObject.profilePath
                loginRequestObject.startDate =
                    Comman_Methods.getcurrentDate()
                loginRequestObject.uuid = androidId
                loginRequestObject.userName =
                    locationObject.userName
                loginRequestObject.devicetypeid =
                    DEVICE_TYPE_ID.toString()
                loginRequestObject.createdby = "0"
                loginRequestObject.mobile = ""
                loginRequestObject.deviceToken = ""
                loginRequestObject.batteryLevel =
                    Utils.GetBatterylevel(context).toString()
                loginRequestObject.deviceCompanyName =
                    Comman_Methods.getdevicename()
                loginRequestObject.deviceModel =
                    Comman_Methods.getdeviceModel()
                loginRequestObject.deviceOS =
                    Comman_Methods.getdeviceVersion()
                loginRequestObject.deviceTokenId =
                    AppPreference.getStringPreference(context, BuildConfig.firebasePrefKey)
                loginRequestObject.deviceType = DEVICE_TYPE
                loginRequestObject.frequency =
                    locationObject.frequency
                loginRequestObject.loginByApp =
                    if (locationObject.loginByApp != null) locationObject.loginByApp else 2

                loginRequestObject.locationPermission = "true"
                if (NotificationManagerCompat.from(context)
                        .areNotificationsEnabled()
                ) {
                    loginRequestObject.notificationPermission =
                        "true"
                } else {
                    loginRequestObject.notificationPermission =
                        "false"
                }

                dbhelper.loginRequestDao()
                    .addLoginRequest(loginRequestObject)

                duplicateGeoFenceCurrentList = ArrayList()
                duplicateGeoFenceCurrentList =
                    appDatabase.geoFenceDao()
                        .getAllGeoFenceDetail() as ArrayList<GeoFenceResult>
                setGeofenceData()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun backGroundPing() {
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                callPingApi()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun callPingApi() {
        val androidId: String = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        appDatabase = OldMe911Database.getDatabase(applicationContext)

        /*val loginAllData: ArrayList<LoginObject> = if (appDatabase.loginDao().getAllLoginDetail()!=null) {
            if (appDatabase.loginDao().getAllLoginDetail().isNotEmpty()) {
                if (appDatabase.loginDao().countUser() > 10){
                    appDatabase.loginDao().lastSomeRecords() as ArrayList<LoginObject>
                }else {
                    appDatabase.loginDao().getAllLoginDetail() as ArrayList<LoginObject>
                }
            }else{
                ArrayList()
            }
        } else{
            ArrayList()
        }*/

        val loginParamter: LoginObject = appDatabase.loginDao().getAll()
        val login_json = LoginRequest()

        /*var address: String = ""
        for (i in 0 until loginAllData.size){
            val singleData = loginAllData[i]
            val oldLatitude: String = if (singleData.latitude!=null) DecimalFormat("#.####").format(singleData.latitude) else "0.0"
            val oldLongitude: String = if (singleData.longitude!=null) DecimalFormat("#.####").format(singleData.longitude) else "0.0"

            val newLatitude = DecimalFormat("#.####").format(gpsLatitude)
            val newLongitude = DecimalFormat("#.####").format(gpsLongitude)
            if (oldLatitude==newLatitude && oldLongitude==newLongitude) {
                address = if (singleData.locationAddress!=null) singleData.locationAddress!! else ""
            }
        }*/
        val decimalSymbols = DecimalFormatSymbols.getInstance(Locale.US)
        val oldLatitudeString: String = if (loginParamter.latitude!=null) DecimalFormat("#.####", decimalSymbols).format(loginParamter.latitude) else "0.0"
        val oldLongitudeString: String = if (loginParamter.longitude!=null) DecimalFormat("#.####", decimalSymbols).format(loginParamter.longitude) else "0.0"

        val oldLatitude: Double = if (loginParamter.latitude!=null) loginParamter.latitude else 0.0
        val oldLongitude: Double = if (loginParamter.longitude!=null) loginParamter.longitude else 0.0

        val newLatitude = DecimalFormat("#.####", decimalSymbols).format(gpsLatitude)
        val newLongitude = DecimalFormat("#.####", decimalSymbols).format(gpsLongitude)

        val distance = FloatArray(2)

        Location.distanceBetween(
            oldLatitude, oldLongitude,
            gpsLatitude, gpsLongitude, distance
        )

        val distanceInKilometer = distance[0]/1000

        println("!@@@@@distance = ${distance[0]}")
        println("!@@@@@distanceInKilometer = ${distanceInKilometer}")

        val address = if (distance[0] > 50f){
            ""
        }else{
            loginParamter.locationAddress
        }

//            val distanceInMeter = Comman_Methods.distance(gpsLatitude, gpsLongitude, oldLatitude, oldLongitude)

        /*val address = if (oldLatitudeString!=newLatitude && oldLongitudeString!=newLongitude)
        *//*Utils.encodeString(Utils.getCompleteAddressString(
                context,
                gpsLatitude,
                gpsLongitude
            ))*//* ""
            else loginParamter.locationAddress*/

        login_json.batteryLevel = Utils.GetBatterylevel(applicationContext).toString()
        login_json.deviceCompanyName = Comman_Methods.getdevicename()
        login_json.deviceModel = Comman_Methods.getdeviceModel()
        login_json.deviceOS = Comman_Methods.getdeviceVersion()
        login_json.uuid = androidId
        login_json.email = loginParamter.email
        login_json.latitude = DecimalFormat("#.######", decimalSymbols).format(gpsLatitude).toString()
        login_json.longitude = DecimalFormat("#.######", decimalSymbols).format(gpsLongitude).toString()
        login_json.recordStatus = PIN_RECORD_STATUS.toString()
        login_json.locationPermission = locationPermission
        login_json.locationAddress = address
        login_json.mobile = ""
        login_json.createdby = loginParamter.memberID.toString()
        login_json.userName = loginParamter.userName
        login_json.id = loginParamter.memberID
        login_json.firstName = loginParamter.firstName
        login_json.lastName = loginParamter.lastName
        login_json.profilePath = loginParamter.profilePath
        login_json.devicetypeid = "1"
        login_json.frequency = loginParamter.frequency
        login_json.loginByApp = loginParamter.loginByApp ?: 2
        if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            login_json.notificationPermission = "true"
        } else {
            login_json.notificationPermission = "false"
        }

        login_json.deviceTokenId = AppPreference.getStringPreference(context, BuildConfig.firebasePrefKey)
        login_json.deviceType = "Android"
        login_json.startDate = Comman_Methods.getcurrentDate()
        login_json.password = loginParamter.password

        Utils.callLoginPingLogoutApi(this, login_json, object : CommonApiListener {
            override fun loginResponse(
                status: Boolean,
                loginData: LoginObject?,
                message: String,
                responseMessage: String
            ) {
                if (status) {


                    duplicateGeoFenceCurrentList = ArrayList()
                    duplicateGeoFenceCurrentList.addAll(appDatabase.geoFenceDao()
                        .getAllGeoFenceDetail() as ArrayList<GeoFenceResult>)

                    try {
                        if (loginData != null) {
                            val oldLoginData = appDatabase.loginDao().getAll()
                            val freq = appDatabase.loginDao().getAll().frequency

                            val inputFormat = SimpleDateFormat(INPUT_DATE_FORMAT)
                            val outputFormat = SimpleDateFormat(OUTPUT_DATE_FORMAT)
                            val startDateFormat = SimpleDateFormat(DELIVER_DATE_FORMAT)


                            /**
                             * Set manually data to Object From Response.
                             */
                            val login_obj: LoginObject = LoginObject()

                            /**
                             * Add Login for get Days from
                             * SubscriptionEndDate & SubscriptionStartDate
                             * for set Call api after this time.
                             */
                            if (loginData.SubscriptionStartDate != null) {

                                login_obj.SubscriptionEndDate = loginData.SubscriptionEndDate
                                login_obj.SubscriptionStartDate = loginData.SubscriptionStartDate

                                val startdate = inputFormat.parse(loginData.startDate)
                                val start_date_change = outputFormat.format(startdate)
                                val startDateValue = outputFormat.parse(start_date_change)

                                val enddate = inputFormat.parse(loginData.SubscriptionEndDate)
                                val end_date_change = outputFormat.format(enddate)
                                val endDateValue = outputFormat.parse(end_date_change)


                                val diffrence: Long = endDateValue.time - startDateValue.time
                                val days = TimeUnit.DAYS.convert(diffrence, TimeUnit.MILLISECONDS)
                                login_obj.time_interval_days = days.toInt()
                            }



                            login_obj.IsNotification = if (loginData.IsNotification!=null) loginData.IsNotification else if (oldLoginData.IsNotification!=null) oldLoginData.IsNotification else false
                            login_obj.IsSms = if (loginData.IsSms!=null) loginData.IsSms else if (oldLoginData.IsSms!=null) oldLoginData.IsSms else false
                            login_obj.IsSubscription = if (loginData.IsSubscription!=null) loginData.IsSubscription else if (oldLoginData.IsSubscription!=null) oldLoginData.IsSubscription else false

                            login_obj.Package = if (loginData.Package!=null) if (loginData.Package != "") if (loginData.Package.toInt() > 0) loginData.Package else oldLoginData.Package else oldLoginData.Package else oldLoginData.Package
                            login_obj.count = if (loginData.count!=null) loginData.count!! else if (oldLoginData.count!=null) oldLoginData.count else 0
                            login_obj.deviceDetails = if (loginData.deviceDetails!=null) loginData.deviceDetails else if (oldLoginData.deviceDetails!=null) oldLoginData.deviceDetails else ""
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
                            login_obj.isAdmin = loginData.isAdmin
                            login_obj.userName = if (loginData.userName!=null) loginData.userName else if (oldLoginData.userName!=null) oldLoginData.userName else ""
                            login_obj.profilePath = if (loginData.profilePath!=null) loginData.profilePath else if (oldLoginData.profilePath!=null) oldLoginData.profilePath else ""
                            login_obj.firstName = if (loginData.firstName!=null) loginData.firstName else if (oldLoginData.firstName!=null) oldLoginData.firstName else ""
                            login_obj.lastName = if (loginData.lastName != null) loginData.lastName else if (oldLoginData.lastName!=null) oldLoginData.lastName else ""
                            login_obj.sequirityQuestionID = if (loginData.sequirityQuestionID!=null) loginData.sequirityQuestionID else if (oldLoginData.sequirityQuestionID!=null) oldLoginData.sequirityQuestionID else 0
                            login_obj.sequirityAnswer = if (loginData.sequirityAnswer!=null) loginData.sequirityAnswer else if (oldLoginData.sequirityAnswer!=null) oldLoginData.sequirityAnswer else ""
                            login_obj.domainName = if (loginData.domainName!=null) loginData.domainName else ""
                            login_obj.subscriptionExpireDate = if (loginData.subscriptionExpireDate!=null) loginData.subscriptionExpireDate else if (oldLoginData.subscriptionExpireDate!=null) oldLoginData.subscriptionExpireDate else ""
                            login_obj.freeTrail = loginData.freeTrail
                            login_obj.memberUtcDateTime = loginData.memberUtcDateTime
                            login_obj.mobile = if (loginData.mobile != null) loginData.mobile else if (oldLoginData.mobile!=null) oldLoginData.mobile else ""
                            login_obj.eventGeoFanceListing = loginData.eventGeoFanceListing
                            if (loginData.lstFamilyMonitoringGeoFence!=null) {
                                login_obj.lstFamilyMonitoringGeoFence = loginData.lstFamilyMonitoringGeoFence
                            }else{
                                login_obj.lstFamilyMonitoringGeoFence = if (oldLoginData.lstFamilyMonitoringGeoFence!=null) oldLoginData.lstFamilyMonitoringGeoFence else ArrayList()
                            }
                            login_obj.frequency = if (loginData.frequency!=null) loginData.frequency else if (oldLoginData.frequency!=null) oldLoginData.frequency else 0
                            login_obj.totalMembers = if (loginData.totalMembers!=null) loginData.totalMembers else if (oldLoginData.totalMembers!=null) oldLoginData.totalMembers else 0
                            login_obj.adminID = if (loginData.adminID!=null) loginData.adminID else if (oldLoginData.adminID!=null) oldLoginData.adminID else 0
                            login_obj.isFromIos = if (loginData.isFromIos!=null) loginData.isFromIos else if (oldLoginData.isFromIos!=null) oldLoginData.isFromIos else false
                            login_obj.isReport = if (loginData.isReport!=null) loginData.isReport else if (oldLoginData.isReport!=null) oldLoginData.isReport else false
                            login_obj.loginByApp = if (loginData.loginByApp != null) loginData.loginByApp else 2
                            login_obj.IsAdditionalMember = if (loginData.IsAdditionalMember != null) loginData.IsAdditionalMember else if (oldLoginData.IsAdditionalMember != null) oldLoginData.IsAdditionalMember else false
                            login_obj.ReferralCode = if (loginData.ReferralCode != null) loginData.ReferralCode else if (oldLoginData.ReferralCode != null) oldLoginData.ReferralCode else ""
                            login_obj.ReferralName = if (loginData.ReferralName != null) loginData.ReferralName else if (oldLoginData.ReferralName != null) oldLoginData.ReferralName else ""
                            login_obj.PromocodeUrl = if (loginData.PromocodeUrl != null) loginData.PromocodeUrl else if (oldLoginData.PromocodeUrl != null) oldLoginData.PromocodeUrl else ""
                            login_obj.Promocode = if (loginData.Promocode != null) loginData.Promocode else if (oldLoginData.Promocode != null) oldLoginData.Promocode else ""
                            login_obj.isChildMissing = if (loginData.isChildMissing != null) loginData.isChildMissing else if (oldLoginData.isChildMissing != null) oldLoginData.isChildMissing else false
                            login_obj.clientMobileNumber = if (loginData.clientMobileNumber != null) loginData.clientMobileNumber else if (oldLoginData.clientMobileNumber != null) oldLoginData.clientMobileNumber else ""
                            login_obj.clientImageUrl = if (loginData.clientImageUrl != null) loginData.clientImageUrl else if (oldLoginData.clientImageUrl != null) oldLoginData.clientImageUrl else ""
                            login_obj.currentSubscriptionEndDate =
                                if (loginData.currentSubscriptionEndDate != null) loginData.currentSubscriptionEndDate else if (oldLoginData.currentSubscriptionEndDate != null) oldLoginData.currentSubscriptionEndDate else ""
                            login_obj.liveStreamDuration =
                                if (loginData.liveStreamDuration != null) loginData.liveStreamDuration else if (oldLoginData.liveStreamDuration != null) oldLoginData.liveStreamDuration else 15
                            login_obj.adminName =
                                if (loginData.adminName != null) loginData.adminName else if (oldLoginData.adminName != null) oldLoginData.adminName else ""
                            login_obj.isAdminLoggedIn =
                                if (loginData.isAdminLoggedIn != null) loginData.isAdminLoggedIn else if (oldLoginData.isAdminLoggedIn != null) oldLoginData.isAdminLoggedIn else false

                            appDatabase.loginDao().addLogin(login_obj)
                            /*if (loginData.SubscriptionStartDate != null && loginData.count != null) {
                                appDatabase.loginDao().addLogin(loginData)
                            }*/
                            if (loginData.count != null) {
                                sendMessageToFragment(
                                    loginData.count ?: 0,
                                    applicationContext
                                )
                            }else{
                                sendMessageToFragment(
                                    oldLoginData.count ?: 0,
                                    applicationContext
                                )
                            }
                            setGeofenceData()
                            if (freq!=loginData.frequency){
                                if (loginData.frequency!!<60){
                                    if (freq!=(loginData.frequency!!*60)){
                                        val isServiceStarted =
                                            Utils.isJobServiceRunning(context)
                                        if (isServiceStarted) {
                                            val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                                            jobScheduler.cancel(GPSSERVICEJOBID)
                                        }
                                        val isServiceStopped =
                                            Utils.isJobServiceRunning(context)
                                        try {
                                            if (isAppIsInBackground(context)) {
                                                if (!isServiceStopped) {
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
                                                }
                                            } else {
                                                if (!isServiceStopped) {
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
                                                }
                                            }
                                        } catch (e: java.lang.Exception){
                                            e.printStackTrace()
                                        }
                                    }
                                }else {
                                    val isServiceStarted =
                                        Utils.isJobServiceRunning(context)
                                    if (isServiceStarted) {
                                        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                                        jobScheduler.cancel(GPSSERVICEJOBID)
                                    }
                                    val isServiceStoped =
                                        Utils.isJobServiceRunning(context)
                                    try {
                                        if (isAppIsInBackground(context)) {
                                            if (!isServiceStoped) {
                                                val jobScheduler =
                                                    getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                                                val componentName =
                                                    ComponentName(packageName, GpsJobService::class.java.name)
                                                val jobInfo = JobInfo.Builder(
                                                    GPSSERVICEJOBID, componentName)
                                                    .setMinimumLatency(1000)
                                                    .setOverrideDeadline((241 * 60000).toLong())
                                                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                                                    .setPersisted(true).build()
                                                val resultCode = jobScheduler.schedule(jobInfo)
                                                if (resultCode == JobScheduler.RESULT_SUCCESS) {
                                                    Log.d(GpsJobService::class.java.name, "job scheduled")
                                                } else {
                                                    Log.d(GpsJobService::class.java.name, "job schedule failed")
                                                }
                                            }
                                        } else {
                                            if (!isServiceStoped) {
                                                val jobScheduler =
                                                    getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                                                val componentName =
                                                    ComponentName(packageName, GpsJobService::class.java.name)
                                                val jobInfo = JobInfo.Builder(
                                                    GPSSERVICEJOBID, componentName)
                                                    .setMinimumLatency(1000)
                                                    .setOverrideDeadline((241 * 60000).toLong())
                                                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                                                    .setPersisted(true).build()
                                                val resultCode = jobScheduler.schedule(jobInfo)
                                                if (resultCode == JobScheduler.RESULT_SUCCESS) {
                                                    Log.d(GpsJobService::class.java.name, "job scheduled")
                                                } else {
                                                    Log.d(GpsJobService::class.java.name, "job schedule failed")
                                                }
                                            }
                                        }
                                    } catch (e: java.lang.Exception){
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    }catch (e: Exception){
                        e.printStackTrace()
                    }
                }
            }
        }, isLoader = false)
    }

    private fun setGeofenceData() {
//        appDatabase.geoFenceDao().dropGeoFence()
        geoFenceCurrentList = ArrayList()
        geoFenceCurrentList.addAll(appDatabase.loginDao().getAll().lstFamilyMonitoringGeoFence)
        if (geoFenceCurrentList.size > 0) {
            if (duplicateGeoFenceCurrentList.size > 0) {
                for (i in 0 until geoFenceCurrentList.size) {
                    for (j in 0 until duplicateGeoFenceCurrentList.size) {
                        if (geoFenceCurrentList[i].iD == duplicateGeoFenceCurrentList[j].iD) {
                            geoFenceCurrentList[i].geoID = duplicateGeoFenceCurrentList[j].geoID
                            geoFenceCurrentList[i].ex =
                                if (duplicateGeoFenceCurrentList[j].ex != null) duplicateGeoFenceCurrentList[j].ex else "Enter"
                            appDatabase.geoFenceDao().updateGeoFence(geoFenceCurrentList[i])
                        }
                    }
                }
            }
            appDatabase.geoFenceDao().dropGeoFence()
            appDatabase.geoFenceDao().addAllGeoFence(geoFenceCurrentList)
        }
    }

    protected fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        locationPermission = "true"
        LocationServices.FusedLocationApi.requestLocationUpdates(
            mGoogleApiClient!!, mLocationRequest, this
        )
    }

    protected fun stopLocationUpdates() {
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient!!, this
            )
        }
    }
}