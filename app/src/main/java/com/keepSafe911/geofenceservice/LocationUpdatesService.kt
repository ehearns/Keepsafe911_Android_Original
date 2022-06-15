package com.keepSafe911.geofenceservice

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import callSendNotificationApi
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.keepSafe911.model.GeoFenceResult
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.R
import com.keepSafe911.utils.ConnectionUtil
import com.keepSafe911.utils.INPUT_CHECK_DATE_FORMAT
import com.keepSafe911.utils.LocationUtil
import com.keepSafe911.utils.Utils
import isAppIsInBackground
import storeGeofenceData
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class LocationUpdatesService : Service(), IGeoListener {

    companion object {
        const val TAG = "LocationUpdatesService"
        private const val PACKAGE_NAME =
            "com.keepSafe911.geofenceservice.LocationUpdatesService"
        const val ACTION_BROADCAST = "$PACKAGE_NAME.broadcast"
        const val EXTRA_LOCATION = "$PACKAGE_NAME.location"
        const val EXTRA_STARTED_FROM_NOTIFICATION = "$PACKAGE_NAME.started_from_notification"
        const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 5000
        const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2
        const val NOTIFICATION_ID = Int.MAX_VALUE
        const val NOTIFICATION_TEXT = "Application is using your location."
    }

    private val mBinder: IBinder = LocalBinder()

    private var mChangingConfiguration = false

    private var mNotificationManager: NotificationManager? = null

    private lateinit var mLocationRequest: LocationRequest

    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private lateinit var mLocationCallback: LocationCallback

    private lateinit var mServiceHandler: Handler

    private lateinit var mLocation: Location

    lateinit var context: Context

    private var geoFenceList: ArrayList<GeoFenceResult> = ArrayList()

    private lateinit var appDataBase: OldMe911Database

    inner class LocalBinder : Binder() {
        fun getService(): LocationUpdatesService {
            return this@LocationUpdatesService
        }
    }

    override fun onCreate() {
        context = this
        appDataBase = OldMe911Database.getDatabase(context)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let {
                    onNewLocation(it)
                }
            }
        }
        createLocationRequest()
        getLastLocation()
        val handlerThread = HandlerThread(TAG)
        handlerThread.start()
        mServiceHandler = Handler(handlerThread.looper)
        mNotificationManager =
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = applicationContext.resources.getString(R.string.default_notification_channel_id)
            val contentTitle = applicationContext.resources.getString(R.string.app_name)
            val mChannel =
                NotificationChannel(channelId, contentTitle, NotificationManager.IMPORTANCE_LOW)
            mChannel.description = NOTIFICATION_TEXT
            mNotificationManager?.createNotificationChannel(mChannel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service started")
        if (intent != null){
            val startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION, false)
            if (startedFromNotification) {
                removeLocationUpdates()
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mChangingConfiguration = true
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.i(TAG, "in onBind()")
        stopForeground(true)
        mChangingConfiguration = false
        return mBinder
    }

    override fun onRebind(intent: Intent?) {
        Log.i(TAG, "in onRebind()")
        stopForeground(true)
        mChangingConfiguration = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "Last client unbound from service")
        val appDataBase = OldMe911Database.getDatabase(context)
        val loginData = appDataBase.loginDao().getAll()
        if (loginData != null) {
            if (!mChangingConfiguration && LocationUtil.requestingLocationUpdates(this)) {
                Log.i(TAG, "Starting foreground service")
                startForeground(NOTIFICATION_ID, getNotification())
            }
        }
        return true
    }

    override fun onDestroy() {
        mServiceHandler.removeCallbacksAndMessages(null)
    }

    fun requestLocationUpdates() {
        Log.i(TAG, "Requesting location updates")
        try {
            startService(Intent(applicationContext, LocationUpdatesService::class.java))
            LocationUtil.setRequestingLocationUpdates(this, true)
            try {
                mFusedLocationClient.requestLocationUpdates(
                    mLocationRequest,
                    mLocationCallback, Looper.myLooper()!!
                )
            } catch (unlikely: SecurityException) {
                LocationUtil.setRequestingLocationUpdates(this, false)
                Log.e(
                    TAG, "Lost location permission. Could not request updates. $unlikely"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeLocationUpdates() {
        Log.i(TAG, "Removing location updates")
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback)
            LocationUtil.setRequestingLocationUpdates(this, false)
            stopSelf()
        } catch (unlikely: SecurityException) {
            LocationUtil.setRequestingLocationUpdates(this, true)
            Log.e(
                TAG,
                "Lost location permission. Could not remove updates. $unlikely"
            )
        }
    }

    /**
     * create notification for foreground service
     */
    private fun getNotification(): Notification? {
        val contentTitle = applicationContext.resources.getString(R.string.app_name)
        val channelId = applicationContext.resources.getString(R.string.default_notification_channel_id)

        val builder =
            NotificationCompat.Builder(this)
                .setContentText(NOTIFICATION_TEXT)
                .setSmallIcon(Utils.getNotificationIcon())
                .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
                .setContentTitle(contentTitle)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setTicker(NOTIFICATION_TEXT)
                .setWhen(System.currentTimeMillis())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(channelId)
        }
        return builder.build()
    }

    /**
     * get last location of user
     */
    private fun getLastLocation() {
        try {
            mFusedLocationClient.lastLocation
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        mLocation = task.result
                    } else {
                        Log.w(TAG, "Failed to get location.")
                    }
                }
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission.$unlikely")
        }
    }

    /**
     * get new location of user
     */
    fun onNewLocation(location: Location) {
        workWithGeoFenceList(location)
        mLocation = location
        val intent = Intent(ACTION_BROADCAST)
        intent.putExtra(EXTRA_LOCATION, location)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    /**
     * work on geofence list for trigger enter/exit on it
     */
    private fun workWithGeoFenceList(location: Location) {
        val userLatLong = LatLng(location.latitude, location.longitude)
        appDataBase = OldMe911Database.getDatabase(context)
        geoFenceList = ArrayList()
        geoFenceList = appDataBase.geoFenceDao().getAllGeoFenceDetail() as ArrayList<GeoFenceResult>
        val geoFenceUtil = GeoFenceUtil(this, geoFenceList)
        val distanceList = geoFenceUtil.filterLatLong(userLatLong)
        distanceList.sortBy { it.distance }
        if(distanceList.size>0) {
            val nearestDistance = distanceList[0]
            geoFenceUtil.checkForGeoEntryExit(location, nearestDistance)
        }
    }

    private fun createLocationRequest() {
        mLocationRequest = LocationRequest.create()
        mLocationRequest.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    /**
     * function called when geofence enter trigger
     */
    override fun showNotificationEntryEvent(distance: Distance, userLocation: Location) {
        val appDataBase = OldMe911Database.getDatabase(context)
        val loginData = appDataBase.loginDao().getAll()
        val geofenceBean = distance.geofenceModel
        if (geofenceBean.isActive) {
            if (checkDateAndTime(geofenceBean)) {
                if (geofenceBean.ex == "Enter") {
                    if (isAppIsInBackground(context)) {
                        val connectivityManager =
                            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            val connectivityCallback: ConnectivityManager.NetworkCallback =
                                object :
                                    ConnectivityManager.NetworkCallback() {
                                    override fun onAvailable(network: Network) {
                                        Log.d(
                                            "!@@@Internet",
                                            "onAvailable: "
                                        )
                                        if (loginData != null) {
                                            callSendNotificationApi(
                                                "Entered",
                                                true,
                                                appDataBase.loginDao().getAll().memberID,
                                                geofenceBean.iD,
                                                userLocation.latitude.toFloat(),
                                                userLocation.longitude.toFloat(),
                                                context
                                            )
                                            appDataBase.geoFenceDao()
                                                .updateGeoFenceData("Exit", geofenceBean.geoID)
                                        }
                                    }

                                    override fun onLost(network: Network) {
                                        Log.d(
                                            "!@@@Internet",
                                            "onLost: "
                                        )
                                        storeGeofenceData("Entered",
                                            true,
                                            appDataBase.loginDao().getAll().memberID,
                                            geofenceBean.iD,
                                            userLocation.latitude.toFloat(),
                                            userLocation.longitude.toFloat(),
                                            context
                                        )
                                    }
                                }
                            connectivityManager.registerNetworkCallback(
                                NetworkRequest.Builder()
                                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                                    .build(), connectivityCallback
                            )
                        } else {
                            if (ConnectionUtil.isInternetAvailable(context)) {
                                callSendNotificationApi(
                                    "Entered",
                                    true,
                                    appDataBase.loginDao().getAll().memberID,
                                    geofenceBean.iD,
                                    userLocation.latitude.toFloat(),
                                    userLocation.longitude.toFloat(),
                                    context
                                )
                                appDataBase.geoFenceDao()
                                    .updateGeoFenceData("Exit", geofenceBean.geoID)
                            } else {
                                storeGeofenceData("Entered",
                                    true,
                                    appDataBase.loginDao().getAll().memberID,
                                    geofenceBean.iD,
                                    userLocation.latitude.toFloat(),
                                    userLocation.longitude.toFloat(),
                                    context
                                )
                            }
                        }
                    } else {
                        if (ConnectionUtil.isInternetAvailable(context)) {
                            callSendNotificationApi(
                                "Entered",
                                true,
                                appDataBase.loginDao().getAll().memberID,
                                geofenceBean.iD,
                                userLocation.latitude.toFloat(),
                                userLocation.longitude.toFloat(),
                                context
                            )
                            appDataBase.geoFenceDao()
                                .updateGeoFenceData("Exit", geofenceBean.geoID)
                        } else {
                            storeGeofenceData("Entered",
                                true,
                                appDataBase.loginDao().getAll().memberID,
                                geofenceBean.iD,
                                userLocation.latitude.toFloat(),
                                userLocation.longitude.toFloat(),
                                context
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * function called when geofence exit trigger
     */
    override fun showNotificationExitEvent(distance: Distance, userLocation: Location) {
        val appDataBase = OldMe911Database.getDatabase(context)
        val loginData = appDataBase.loginDao().getAll()
        val geofenceBean = distance.geofenceModel
        if (geofenceBean.isActive) {
            if (checkDateAndTime(geofenceBean)) {
                if (geofenceBean.ex == "Exit") {
                    if (isAppIsInBackground(context)) {
                        val connectivityManager =
                            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            val connectivityCallback: ConnectivityManager.NetworkCallback =
                                object :
                                    ConnectivityManager.NetworkCallback() {
                                    override fun onAvailable(network: Network) {
                                        Log.d(
                                            "!@@@Internet",
                                            "onAvailable: "
                                        )
                                        if (loginData != null) {
                                            callSendNotificationApi(
                                                "Exited",
                                                false,
                                                appDataBase.loginDao().getAll().memberID,
                                                geofenceBean.iD,
                                                userLocation.latitude.toFloat(),
                                                userLocation.longitude.toFloat(),
                                                context
                                            )
                                            appDataBase.geoFenceDao()
                                                .updateGeoFenceData("Enter", geofenceBean.geoID)
                                        }
                                    }

                                    override fun onLost(network: Network) {
                                        Log.d(
                                            "!@@@Internet",
                                            "onLost: "
                                        )
                                        storeGeofenceData("Exited",
                                            false,
                                            appDataBase.loginDao().getAll().memberID,
                                            geofenceBean.iD,
                                            userLocation.latitude.toFloat(),
                                            userLocation.longitude.toFloat(),
                                            context
                                        )
                                    }
                                }
                            connectivityManager.registerNetworkCallback(
                                NetworkRequest.Builder()
                                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                                    .build(), connectivityCallback
                            )
                        } else {
                            if (ConnectionUtil.isInternetAvailable(context)) {
                                if (loginData != null) {
                                    callSendNotificationApi(
                                        "Exited",
                                        false,
                                        appDataBase.loginDao().getAll().memberID,
                                        geofenceBean.iD,
                                        userLocation.latitude.toFloat(),
                                        userLocation.longitude.toFloat(),
                                        context
                                    )
                                    appDataBase.geoFenceDao()
                                        .updateGeoFenceData("Enter", geofenceBean.geoID)
                                }
                            } else {
                                storeGeofenceData("Exited",
                                    false,
                                    appDataBase.loginDao().getAll().memberID,
                                    geofenceBean.iD,
                                    userLocation.latitude.toFloat(),
                                    userLocation.longitude.toFloat(),
                                    context
                                )
                            }
                        }
                    } else {
                        if (ConnectionUtil.isInternetAvailable(context)) {
                            if (loginData != null) {
                                callSendNotificationApi(
                                    "Exited",
                                    false,
                                    appDataBase.loginDao().getAll().memberID,
                                    geofenceBean.iD,
                                    userLocation.latitude.toFloat(),
                                    userLocation.longitude.toFloat(),
                                    context
                                )
                                appDataBase.geoFenceDao()
                                    .updateGeoFenceData("Enter", geofenceBean.geoID)
                            }
                        } else {
                            storeGeofenceData("Exited",
                                false,
                                appDataBase.loginDao().getAll().memberID,
                                geofenceBean.iD,
                                userLocation.latitude.toFloat(),
                                userLocation.longitude.toFloat(),
                                context
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkDateAndTime(geofenceBean: GeoFenceResult): Boolean {
        val c = Calendar.getInstance()
        val df = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)

        val currentDateTime = df.format(c.time)
        var currentTime: Date? = null
        try {
            currentTime = df.parse(currentDateTime)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        var startDateTime: Date? = null
        try {
            startDateTime = df.parse(geofenceBean.startDate ?: "")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val cale = Calendar.getInstance()
        if (startDateTime != null) {
            cale.time = startDateTime
            val geofenceStartEventTime = df.format(cale.time)
            try {
                startDateTime = df.parse(geofenceStartEventTime)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }



        var eventDateTime: Date? = null
        try {
            eventDateTime = df.parse(geofenceBean.endDate ?: "")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val cal = Calendar.getInstance()
        if (eventDateTime != null) {
            cal.time = eventDateTime
            val geofenceEventTime = df.format(cal.time)
            try {
                eventDateTime = df.parse(geofenceEventTime)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val startTime = if (startDateTime != null) {
            startDateTime <= currentTime
        } else {
            false
        }
        val eventTime = if (eventDateTime != null) {
            eventDateTime >= currentTime
        } else {
            false
        }
        return (startTime && eventTime)
    }
}