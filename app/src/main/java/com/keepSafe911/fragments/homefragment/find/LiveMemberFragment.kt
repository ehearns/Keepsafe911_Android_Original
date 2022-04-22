package com.keepSafe911.fragments.homefragment.find


import addFragment
import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.net.*
import android.os.*
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.gps.GpsTracker
import com.keepSafe911.model.GeoFenceResult
import com.keepSafe911.model.LiveMemberResult
import com.keepSafe911.model.MapFilter
import com.keepSafe911.model.MapFilter.Companion.getMapFilter
import com.keepSafe911.model.request.LoginRequest
import com.keepSafe911.model.response.LiveMemberResponse
import com.keepSafe911.model.response.yelp.Business
import com.keepSafe911.model.response.yelp.Region
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import com.keepSafe911.webservices.WebApiClient
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.keepSafe911.BuildConfig
import com.kotlinpermissions.KotlinPermissions
import kotlinx.android.synthetic.main.popup_livemember_marker.view.*
import kotlinx.android.synthetic.main.raw_live_member_filter.view.*
import kotlinx.android.synthetic.main.raw_new_memberlist.view.*
import retrofit2.Call
import retrofit2.Response
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import com.keepSafe911.R
import com.keepSafe911.listner.CommonApiListener
import hideKeyboard
import isAppIsInBackground
import kotlinx.android.synthetic.main.bottom_live_member_sheet.*
import kotlinx.android.synthetic.main.fragment_live_member.*
import kotlinx.android.synthetic.main.raw_call_message.view.*
import kotlinx.android.synthetic.main.toolbar_header.*
import sendSMS
import takeCall
import visitUrl
import java.text.DecimalFormatSymbols

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class LiveMemberFragment : HomeBaseFragment(), OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, LocationListener , View.OnClickListener{

    private var locationPermission = "false"
    private var mGoogleApiClient: GoogleApiClient? = null
    private var mLocationRequest: LocationRequest? = null
    private lateinit var map: GoogleMap
    private var updateMembers: TimerTask? = null
    private var timer: Timer? = Timer()
    private var isFromHandler = false
    internal lateinit var marker1: Marker
    internal lateinit var marker2: Marker
    lateinit var appDatabase: OldMe911Database
    private var sheetBehavior: BottomSheetBehavior<*>? = null
    private var mapFilterLatitude: Double = 0.0
    private var mapFilterLongitude: Double = 0.0
    private var mapFilterTermName: String = ""
    private var mapFilterCategoryName: String = ""
    private var mapFilterID: Int = 0
    var yelpBusinessList: ArrayList<Business> = ArrayList()
    var yelpTotal: Int = 0
    var yelpRegion: Region = Region()
    var hashMapMarkerList: ArrayList<HashMap<String,Marker>> = ArrayList()
    lateinit var mapPinAdapter: MapPinAdapter
    lateinit var bounds: LatLngBounds
    lateinit var liveMemberListAdapter: LiveMemberListAdapter
    private var gpstracker: GpsTracker? = null
    lateinit var callMessageDialog: Dialog
    private var gpsLatitude: Double = 0.toDouble()
    private var gpsLongitude: Double = 0.toDouble()
    private var geoFenceList: ArrayList<GeoFenceResult> = ArrayList()


    var geoFenceCurrentList: ArrayList<GeoFenceResult> = ArrayList()
    var duplicateGeoFenceCurrentList: ArrayList<GeoFenceResult> = ArrayList()
    var mGeofences: ArrayList<Geofence> = ArrayList()
    private var settingDialogShown: Boolean = false
    var isFrom: Boolean = false


    override fun onMapReady(p0: GoogleMap) {
        map = p0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    mActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                buildGoogleApiClient()
//                 map.setMyLocationEnabled(true);
            }
        } else {
            buildGoogleApiClient()
//             map.setMyLocationEnabled(true);
        }
        map.uiSettings.isMapToolbarEnabled = false
        map.uiSettings.isZoomControlsEnabled = false
    }

    override fun onConnected(p0: Bundle?) {
        try {
            mLocationRequest = LocationRequest.create()
            mLocationRequest?.interval = 50000
            mLocationRequest?.fastestInterval = 40000
            mLocationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

            if ((mGoogleApiClient?.isConnecting == false) && (mGoogleApiClient?.isConnected == false)) {
                buildGoogleApiClient()
                mGoogleApiClient?.connect()
            }
            if (ActivityCompat.checkSelfPermission(
                    mActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    mActivity, Manifest.permission.ACCESS_COARSE_LOCATION
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
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient!!, mLocationRequest!!, this)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onConnectionSuspended(p0: Int) {

    }

    override fun onConnectionFailed(p0: ConnectionResult) {

    }

    override fun onLocationChanged(p0: Location) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isFrom = it.getBoolean(ARG_PARAM1, false)
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
        return inflater.inflate(R.layout.fragment_live_member, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity.disableDrawer()
        mActivity.checkNavigationItem(1)
        mActivity.hideKeyboard()
        appDatabase = OldMe911Database.getDatabase(mActivity)
        gpstracker = GpsTracker(mActivity)
        val loginObject = appDatabase.loginDao().getAll()

        setHeader()


        rvLiveImageList.visibility = View.VISIBLE
        rlLiveAddUser.setOnClickListener(this)
        ivLiveUserPlus.visibility =View.GONE


        if (checkGooglePlayServices()) {
            buildGoogleApiClient()
        }
        map_liveMember.onCreate(savedInstanceState)
        map_liveMember.onResume()
        MapsInitializer.initialize(mActivity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            settingDialogShown = false
            checkLocationPermission(true)
        }else{
            if (gpstracker?.CheckForLoCation() == false) {
                Utils.showLocationSettingsAlert(mActivity)
            }else{
                callPingLiveMemberApi(true)
            }
        }
        if ((mGoogleApiClient?.isConnecting == false) && (mGoogleApiClient?.isConnected == false)) {
            mGoogleApiClient?.connect()
        }
        map_liveMember.getMapAsync { googleMap ->
            map = googleMap
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(
                        mActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    buildGoogleApiClient()
                    // mMap.setMyLocationEnabled(true);

                    if (map_liveMember != null && map_liveMember.findViewById<View>(Integer.parseInt("1")) != null) {
                        // Get the button view
                        val locationButton =
                            (map_liveMember.findViewById<View>(Integer.parseInt("1")).parent as View).findViewById<View>(
                                Integer.parseInt("2")
                            )
                        // and next place it, on bottom right (as Google Maps app)
                        val layoutParams = locationButton.layoutParams as RelativeLayout.LayoutParams
                        // position on right bottom
                        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
                        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                        layoutParams.setMargins(0, 0, 30, 30)

                        map.setInfoWindowAdapter(CustomInfoWindowAdapter())

                        map.setOnInfoWindowClickListener {
                            if (it.snippet=="own"){
                                if (liveMemberList.size > 0){
                                    for (i in 0 until liveMemberList.size){
                                        if (it.title == liveMemberList[i].memberID.toString()) {
                                            showCallMessageToUserDialog(liveMemberList[i])
                                        }
                                    }
                                }
                            } else {
                                if (yelpBusinessList.size > 0) {
                                    for (i in 0 until yelpBusinessList.size) {
                                        if (it.title == yelpBusinessList[i].id) {
                                            showCallMessageDialog(yelpBusinessList[i])
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                buildGoogleApiClient()
                // mMap.setMyLocationEnabled(true);

                if (map_liveMember != null && map_liveMember.findViewById<View>(Integer.parseInt("1")) != null) {
                    // Get the button view
                    val locationButton =
                        (map_liveMember.findViewById<View>(Integer.parseInt("1")).parent as View).findViewById<View>(
                            Integer.parseInt("2")
                        )
                    // and next place it, on bottom right (as Google Maps app)
                    val layoutParams = locationButton.layoutParams as RelativeLayout.LayoutParams
                    // position on right bottom
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                    layoutParams.setMargins(0, 0, 30, 30)

                    map.setInfoWindowAdapter(CustomInfoWindowAdapter())
                    map.setOnInfoWindowClickListener {
                        if (it.snippet=="own"){
                            if (liveMemberList.size > 0){
                                for (i in 0 until liveMemberList.size){
                                    if (it.title == liveMemberList[i].memberID.toString()) {
                                        showCallMessageToUserDialog(liveMemberList[i])
                                    }
                                }
                            }
                        } else {
                            if (yelpBusinessList.size > 0) {
                                for (i in 0 until yelpBusinessList.size) {
                                    if (it.title == yelpBusinessList[i].id) {
                                        showCallMessageDialog(yelpBusinessList[i])
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        sheetBehavior = BottomSheetBehavior.from(bottom_live_member_filter)
        sheetBehavior?.isHideable = false
        sheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(@NonNull bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> { }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        rlFilterModule.visibility = View.GONE
                        tvCloseMapFilter.visibility = View.VISIBLE
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        rlFilterModule.visibility = View.VISIBLE
                        tvCloseMapFilter.visibility = View.GONE
                    }
                    BottomSheetBehavior.STATE_DRAGGING -> { }
                    BottomSheetBehavior.STATE_SETTLING -> { }
                    else -> { }
                }
            }
            override fun onSlide(@NonNull bottomSheet: View, slideOffset: Float) { }
        })
        tvMapPinFilterText.setOnClickListener(this)
        tvCloseMapFilter.setOnClickListener(this)
        ivCancelFilter.setOnClickListener(this)
        ivCancelFilter.visibility = View.GONE
        tvMapPinFilter.text = mActivity.resources.getString(R.string.str_explore_by)
        rvMapPinFilter.layoutManager = GridLayoutManager(mActivity, 4, RecyclerView.VERTICAL, false)
        mapPinAdapter = MapPinAdapter(mActivity, getMapFilter)
        rvMapPinFilter.adapter = mapPinAdapter
        /*when {
            loginObject.isAdmin -> ivRefreshLiveUser.visibility = View.GONE
            else -> ivRefreshLiveUser.visibility = View.VISIBLE
        }*/
        ivRefreshLiveUser.visibility = View.VISIBLE
        ivRefreshLiveUser.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id){
            R.id.tvMapPinFilterText -> {
                mActivity.hideKeyboard()
                if (sheetBehavior?.state != BottomSheetBehavior.STATE_EXPANDED) {
                    sheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                    rlFilterModule.visibility = View.GONE
                    tvCloseMapFilter.visibility = View.VISIBLE
                }
                hideInfoWindow()
            }
            R.id.tvCloseMapFilter -> {
                mActivity.hideKeyboard()
                if (sheetBehavior?.state != BottomSheetBehavior.STATE_COLLAPSED) {
                    sheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
                    rlFilterModule.visibility = View.VISIBLE
                    tvCloseMapFilter.visibility = View.GONE
                }
                hideInfoWindow()
            }
            R.id.ivCancelFilter -> {
                mActivity.hideKeyboard()
                if(yelpBusinessList.size > 0){
                    try {
                        for (i in 0 until yelpBusinessList.size) {
                            if (hashMapMarkerList.size > 0) {
                                if (hashMapMarkerList[i][yelpBusinessList[i].id] != null) {
                                    val marker = hashMapMarkerList[i][yelpBusinessList[i].id]
                                    marker?.remove()
                                }
                            }
                        }
                    }catch (e: java.lang.Exception){
                        e.printStackTrace()
                    }
                }
                yelpBusinessList = ArrayList()
                hideInfoWindow()
                changeFilterDesign()
                try {
                    if (this::bounds.isInitialized) {
                        if (bounds != null) {
                            setBounds()
//                            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
//                            map.moveCamera(CameraUpdateFactory.zoomTo(ZOOM_VALUE))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                toggleListOption()
                if (sheetBehavior?.state != BottomSheetBehavior.STATE_EXPANDED) {
                    sheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                    rlFilterModule.visibility = View.GONE
                    tvCloseMapFilter.visibility = View.VISIBLE
                }
            }
            R.id.ivRefreshLiveUser -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    settingDialogShown = false
                    checkLocationPermission(true)
                }else{
                    gpstracker = GpsTracker(mActivity)
                    if (gpstracker?.CheckForLoCation() == false) {
                        Utils.showLocationSettingsAlert(mActivity)
                    }else{
                        callPingLiveMemberApi(true)
                    }
                }
            }
        }
    }

    private fun callPingLiveMemberApi(timeEnable: Boolean){
        gpstracker = GpsTracker(mActivity)
        gpsLatitude = gpstracker?.getLatitude() ?: 0.0
        gpsLongitude = gpstracker?.getLongitude() ?: 0.0

        if ((gpstracker?.isGPSEnable() == true) && (gpstracker?.isNetWorkProviderEnable() == true)) {
            if (appDatabase.loginDao().getAll().isReport!=null) {
                if (!appDatabase.loginDao().getAll().isReport) {
                    if (ConnectionUtil.isInternetAvailable(mActivity)) {
                        try {
                            if (gpsLatitude != 0.0 && gpsLongitude != 0.0) {
                                mActivity.runOnUiThread {
                                    callPingApi(timeEnable)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        val dbhelper = OldMe911Database.getDatabase(mActivity)
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
                                        mActivity.contentResolver,
                                        Settings.Secure.ANDROID_ID
                                    )

                                //byte[] encodeValue = Base64.encode(getCompleteAddressString(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()).getBytes(), Base64.DEFAULT);
                                val locationObject = dbhelper.loginDao().getAll()
                                val decimalSymbols = DecimalFormatSymbols.getInstance(Locale.US)
                                val oldLatitudeString: String =
                                    if (locationObject.latitude != null) DecimalFormat("#.####", decimalSymbols).format(
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

                                val distanceInKilometer = distance[0] * 1000

                                println("!@@@@@distance = ${distance[0]}")
                                println("!@@@@@distanceInKilometer = ${distanceInKilometer}")

                                val address = if (distance[0] > 50f) {
                                    Utils.encodeString(
                                        Utils.getCompleteAddressString(
                                            mActivity,
                                            gpsLatitude,
                                            gpsLongitude
                                        )
                                    )
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
                                loginRequestObject.firstName = locationObject.firstName
                                loginRequestObject.lastName = locationObject.lastName
                                loginRequestObject.latitude = gpsLatitude.toString()
                                loginRequestObject.locationAddress = address
                                loginRequestObject.longitude = gpsLongitude.toString()
                                loginRequestObject.password = locationObject.password
                                loginRequestObject.profilePath =
                                    locationObject.profilePath
                                loginRequestObject.startDate =
                                    Comman_Methods.getcurrentDate()
                                loginRequestObject.uuid = androidId
                                loginRequestObject.userName = locationObject.userName
                                loginRequestObject.devicetypeid =
                                    DEVICE_TYPE_ID.toString()
                                loginRequestObject.createdby = "0"
                                loginRequestObject.mobile = ""
                                loginRequestObject.deviceToken = ""
                                loginRequestObject.batteryLevel =
                                    Utils.GetBatterylevel(mActivity).toString()
                                loginRequestObject.deviceCompanyName =
                                    Comman_Methods.getdevicename()
                                loginRequestObject.deviceModel =
                                    Comman_Methods.getdeviceModel()
                                loginRequestObject.deviceOS =
                                    Comman_Methods.getdeviceVersion()
                                loginRequestObject.deviceTokenId =
                                    AppPreference.getStringPreference(mActivity, BuildConfig.firebasePrefKey)
                                loginRequestObject.deviceType = DEVICE_TYPE
                                loginRequestObject.frequency = locationObject.frequency
                                loginRequestObject.loginByApp = locationObject.loginByApp ?: 2

                                loginRequestObject.locationPermission = "true"
                                if (NotificationManagerCompat.from(mActivity).areNotificationsEnabled()) {
                                    loginRequestObject.notificationPermission = "true"
                                } else {
                                    loginRequestObject.notificationPermission = "false"
                                }

                                dbhelper.loginRequestDao()
                                    .addLoginRequest(loginRequestObject)

                                duplicateGeoFenceCurrentList = ArrayList()
                                duplicateGeoFenceCurrentList =
                                    appDatabase.geoFenceDao().getAllGeoFenceDetail() as ArrayList<GeoFenceResult>
                                setGeofenceData(gpsLatitude, gpsLongitude)
                                callApi(timeEnable)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }else{
                    callApi(timeEnable)
                }
            } else{
                callApi(timeEnable)
            }
        } else{
            val mBuilder = NotificationCompat.Builder(mActivity)
                .setSmallIcon(Utils.getNotificationIcon())
                .setStyle(NotificationCompat.BigTextStyle().bigText(mActivity.resources.getString(R.string.locationMessage)))
                .setContentTitle(mActivity.resources.getString(R.string.locationError))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentText(mActivity.resources.getString(R.string.locationMessage))
            val ntfIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            ntfIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            /*val stackBuilder = TaskStackBuilder.create(applicationContext)
            stackBuilder.addParentStack(NotificationActivity::class.java)
            stackBuilder.addNextIntent(ntfIntent)*/


            val resultPendingIntent1 = PendingIntent.getActivity(context, 0, ntfIntent, PendingIntent.FLAG_ONE_SHOT)
            mBuilder.setContentIntent(resultPendingIntent1)
            val nm = mActivity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelId = mActivity.getString(R.string.default_notification_channel_id)
                val channel = NotificationChannel(channelId, resources.getString(R.string.locationError), NotificationManager.IMPORTANCE_HIGH)
                channel.description = resources.getString(R.string.locationMessage)
                channel.enableLights(true)
                channel.lightColor = Color.GREEN
                channel.setShowBadge(true)
                channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                nm.createNotificationChannel(channel)
                mBuilder.setChannelId(channelId)
            }
            nm.notify(1235, mBuilder.notification)
            Log.e("Warning", "GPS NOT ENABLED")

            callApi(true)
        }
    }

    private fun hideInfoWindow() {
        if (this::marker1.isInitialized) {
            if (marker1 != null) {
                if (marker1.isInfoWindowShown) {
                    marker1.hideInfoWindow()
                }
            }
        }
        if (this::marker2.isInitialized) {
            if (marker2 != null) {
                if (marker2.isInfoWindowShown) {
                    marker2.hideInfoWindow()
                }
            }
        }
    }

    inner class MapPinAdapter(
        val context: Context,
        val mapFilter: ArrayList<MapFilter>
    ) : RecyclerView.Adapter<MapPinAdapter.MapPinHolder>() {

        var currentSelectedPosition: Int = -1
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): MapPinHolder {
            return MapPinHolder(LayoutInflater.from(context).inflate(R.layout.raw_live_member_filter,p0,false))
        }

        override fun getItemCount(): Int {
            return mapFilter.size
        }

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun onBindViewHolder(holder: MapPinHolder, position: Int) {
            val weight: Int = Utils.calculateNoOfColumns(context, 4.4)
//            val height: Int = Utils.calculateNoOfRows(context, 7.0)
            val layoutParams = holder.parentLiveMemberFilter.layoutParams
            layoutParams.width = weight
//            layoutParams.height = height
            holder.parentLiveMemberFilter.layoutParams = layoutParams

            val mapObject = mapFilter[position]

            if (currentSelectedPosition == position){
                ivCancelFilter.visibility = View.VISIBLE
                mapObject.isSelected = true
                holder.parentLiveMemberFilter.background = ContextCompat.getDrawable(mActivity,R.drawable.map_filter_border)
            }else{
                mapObject.isSelected = false
                holder.parentLiveMemberFilter.background = ContextCompat.getDrawable(mActivity,R.color.caldroid_white)
            }

            holder.tvLiveMemberFilterName.text = context.resources.getString(mapObject.filterName)
            holder.cvLiveMemberFilter.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(mActivity, mapObject.fabBackground)))
            holder.ivLiveMemberFilter.setImageResource(mapObject.fabIcon)
            holder.parentLiveMemberFilter.setOnClickListener {
                mActivity.hideKeyboard()
                currentSelectedPosition = holder.bindingAdapterPosition
                notifyDataSetChanged()
                toggleBottomSheet()
                mapFilterTermName = mapObject.termsName
                mapFilterID = mapObject.mapFilterId
                mapFilterCategoryName = mActivity.resources.getString(mapObject.filterName)
                if (mapFilterLatitude!=0.0 && mapFilterLongitude!=0.0) {
                    bottom_live_member_filter.visibility = View.VISIBLE
                    if (!mapObject.isSelected) {
                        if(yelpBusinessList.size > 0){

                            for (i in 0 until yelpBusinessList.size){
                                if (hashMapMarkerList.size > 0){
                                    if (hashMapMarkerList[i][yelpBusinessList[i].id]!=null) {
                                        val marker = hashMapMarkerList[i][yelpBusinessList[i].id]
                                        marker?.remove()
                                    }
                                }
                            }
                        }
                        callYelpFilterApi(mapObject.termsName, mapFilterCategoryName, mapFilterID, mapObject.category)
                    }
                }else{
                    changeFilterDesign()
                    bottom_live_member_filter.visibility = View.GONE
                }
            }
        }

        inner class MapPinHolder(view: View): RecyclerView.ViewHolder(view){
            var cvLiveMemberFilter: CardView = view.cvLiveMemberFilter
            var tvLiveMemberFilterName: TextView = view.tvLiveMemberFilterName
            var parentLiveMemberFilter: RelativeLayout = view.parentLiveMemberFilter
            var ivLiveMemberFilter: ImageView = view.ivLiveMemberFilter
        }
    }


    private fun toggleBottomSheet() {
        if (sheetBehavior?.state != BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            sheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun callYelpFilterApi(
        termsName: String,
        filterName: String,
        filterId: Int,
        category: String
    ) {
        mActivity.isSpeedAvailable()
        Utils.yelpTermBasedFilterApi(mActivity, termsName, mapFilterLatitude, mapFilterLongitude, category, object : CommonApiListener {
            override fun yelpDataResponse(
                businesses: ArrayList<Business>,
                total: Int,
                region: Region
            ) {
                yelpBusinessList = ArrayList()
                hashMapMarkerList = ArrayList()
                for (yelp in 0 until businesses.size) {
                    if (businesses[yelp].isClosed == false) {
                        yelpBusinessList.add(businesses[yelp])
                    }
                }
                if (yelpBusinessList.size > 0) {
                    val builder = LatLngBounds.Builder()
                    for (i in 0 until yelpBusinessList.size) {
                        if (yelpBusinessList[i].isClosed == false) {
                            var yelpLatLng: LatLng? = null
                            yelpBusinessList[i].coordinates?.let { coordinates ->
                                val yelpLatitude = coordinates.latitude ?: 0.0
                                val yelpLongitude = coordinates.longitude ?: 0.0

                                if (yelpLatitude != 0.0 && yelpLongitude != 0.0) {
                                    yelpLatLng = LatLng(yelpLatitude, yelpLongitude)
                                }else{
                                    yelpBusinessList[i].location?.let { location ->
                                        location.displayAddress?.let { addresses ->
                                            var address: String = ""
                                            for (k in 0 until addresses.size) {
                                                val displayAddress = addresses[k]
                                                address = if (k != 0) {
                                                    "$address, $displayAddress"
                                                } else {
                                                    displayAddress
                                                }
                                            }
                                            yelpLatLng = Utils.getLocationFromAddress(mActivity,address)
                                        }
                                    }
                                }
                            }
                            var filterIcon: BitmapDescriptor
                            when (filterId) {
                                1 -> {
                                    filterIcon =
                                        BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_hotel)
                                }
                                2 -> {
                                    filterIcon =
                                        BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_restaurant)
                                }
                                3 -> {
                                    filterIcon =
                                        BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_theater)
                                }
                                4 -> {
                                    filterIcon =
                                        BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_gas)
                                }
                                5 -> {
                                    filterIcon =
                                        BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_bank)
                                }
                                6 -> {
                                    filterIcon =
                                        BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_atm)
                                }
                                7 -> {
                                    filterIcon =
                                        BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_hospital)
                                }
                                8 -> {
                                    filterIcon =
                                        BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_mechanic)
                                }
                                9 -> {
                                    filterIcon =
                                        BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_airport)
                                }
                                10 -> {
                                    filterIcon =
                                        BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_museum)
                                }
                                11 -> {
                                    filterIcon =
                                        BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_urgent)
                                }
                                12 -> {
                                    filterIcon =
                                        BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_office)
                                }
                                13 -> {
                                    filterIcon =
                                        BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_pet)
                                }
                                14 -> {
                                    filterIcon =
                                        BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_police)
                                }
                                15 -> {
                                    filterIcon =
                                        BitmapDescriptorFactory.fromResource(R.drawable.ic_pin_landmark)
                                }

                                else -> {
                                    filterIcon =
                                        BitmapDescriptorFactory.fromResource(R.drawable.ic_location_setting)
                                }
                            }

                            yelpLatLng?.let { latlng ->
                                try {
                                    val markerOptions4 = MarkerOptions().position(latlng)
                                        .title(yelpBusinessList[i].id ?: "")
                                        .snippet("filter")
                                        .icon(filterIcon)
                                    val hashMapMarker = HashMap<String, Marker>()
                                    marker2 = map.addMarker(markerOptions4)!!
                                    hashMapMarker[yelpBusinessList[i].id ?: ""] = marker2
                                    hashMapMarkerList.add(hashMapMarker)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                builder.include(latlng)
                                try {
                                    val filterBounds = builder.build()
                                    map.moveCamera(
                                        CameraUpdateFactory.newLatLngBounds(
                                            filterBounds,
                                            250
                                        )
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }else{
                    changeFilterDesign()
                    mActivity.showMessage(mActivity.resources.getString(R.string.str_no_map_filter,filterName))
                }
                toggleListOption()
                yelpTotal = total
                yelpRegion = region
            }
        })
    }

    private fun setHeader() {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        tvHeader.text = ""
        ivMenuLogo.visibility = View.VISIBLE
        iv_menu.visibility = View.VISIBLE
        iv_menu.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.openDrawer()
        }
        toggleListOption()
        // mActivity.checkUserActive()
    }

    private fun toggleListOption(){
        if (tvList!=null) {
            if (yelpBusinessList.size > 0) {
                tvList.visibility = View.VISIBLE
            } else {
                tvList.visibility = View.GONE
            }
            tvList.setOnClickListener {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
                mActivity.addFragment(
                    ExploreNearByListFragment.newInstance(
                        false,
                        yelpBusinessList,
                        mapFilterCategoryName,
                        ArrayList(),
                        mapFilterLatitude,
                        mapFilterLongitude
                    ),
                    true,
                    true,
                    animationType = AnimationType.fadeInfadeOut
                )
            }
        }
    }


    private fun checkGooglePlayServices(): Boolean {
        val checkGooglePlayServices = GooglePlayServicesUtil
            .isGooglePlayServicesAvailable(mActivity)
        if (checkGooglePlayServices != ConnectionResult.SUCCESS) {

            GooglePlayServicesUtil.getErrorDialog(
                checkGooglePlayServices,
                mActivity, 200
            )?.show()

            return false
        }
        return true
    }

    @Synchronized
    private fun buildGoogleApiClient() {
        mGoogleApiClient = GoogleApiClient.Builder(mActivity)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build()
        mGoogleApiClient?.connect()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
            setHeader()
            val database = OldMe911Database.getDatabase(mActivity)
            val loginObject = database.loginDao().getAll()
            mActivity.hideKeyboard()
            /*if (loginObject.isAdmin) {
                if (timer == null) {
                    timer = Timer()
                    updateMembers = CustomTimerTask()
                    timer?.scheduleAtFixedRate(updateMembers, 60000, 60000)
                } else {
                    timer = Timer()
                    updateMembers = CustomTimerTask()
                    timer?.scheduleAtFixedRate(updateMembers, 60000, 60000)
                }
            }*/
        } else {
            timerClose()
        }
    }

    private fun checkLocationPermission(timeEnable: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
                .permissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
                .onAccepted { permissions ->
                    if (permissions.size == 3) {
                        if (ContextCompat.checkSelfPermission(
                                mActivity,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {

                            if (mGoogleApiClient == null) {
                                buildGoogleApiClient()
                            }
                            locationPermission = "true"
//                    callApi(timeEnable)
                            gpstracker = GpsTracker(mActivity)
                            if (gpstracker?.CheckForLoCation() == false) {
                                Utils.showLocationSettingsAlert(mActivity)
                            }else{
                                callPingLiveMemberApi(timeEnable)
                            }
                        }
                    }
                }
                .onDenied {
                    if (!settingDialogShown) {
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                            settingDialogShown = true
                        }
                        checkLocationPermission(timeEnable)
                    }
                }
                .onForeverDenied {
                    if (!settingDialogShown) {
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                            settingDialogShown = true
                        }
                        locationPermission = "false"
                    }
                }
                .ask()
        } else {
            KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
                .permissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .onAccepted { permissions ->
                    if (permissions.size == 2) {
                        if (ContextCompat.checkSelfPermission(
                                mActivity,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {

                            if (mGoogleApiClient == null) {
                                buildGoogleApiClient()
                            }
                            locationPermission = "true"
//                    callApi(timeEnable)
                            gpstracker = GpsTracker(mActivity)
                            if (gpstracker?.CheckForLoCation() == false) {
                                Utils.showLocationSettingsAlert(mActivity)
                            }else{
                                callPingLiveMemberApi(timeEnable)
                            }
                        }
                    }
                }
                .onDenied {
                    checkLocationPermission(timeEnable)
                }
                .onForeverDenied {
                    locationPermission = "false"
                }
                .ask()
        }
    }

    private fun callApi(timeEnable: Boolean) {
        val currentFragment: Fragment? = mActivity.supportFragmentManager.findFragmentById(R.id.frame_container)!!
        if (currentFragment is LiveMemberFragment) {
            if (ConnectionUtil.isInternetAvailable(mActivity)) {
                val database = OldMe911Database.getDatabase(mActivity)
                val loginObject = database.loginDao().getAll()
                if (yelpBusinessList.size > 0) {
                    for (i in 0 until yelpBusinessList.size) {
                        if (hashMapMarkerList.size > 0) {
                            if (hashMapMarkerList[i][yelpBusinessList[i].id] != null) {
                                val marker = hashMapMarkerList[i][yelpBusinessList[i].id]
                                marker?.remove()
                            }
                        }
                    }
                }
                yelpBusinessList = ArrayList()
                toggleListOption()
                if (loginObject != null) {
                    if (loginObject.memberID != null) {
                        callLiveMemberApi(loginObject.memberID)
                    } else {
                        timerClose()
                    }
                } else {
                    timerClose()
                }
                /*if (timeEnable) {
                    if (loginObject.isAdmin) {
                        updateMembers = CustomTimerTask()
                        timer?.scheduleAtFixedRate(updateMembers, 60000, 60000)
                    }else{
                        if (this::map.isInitialized) {
                            map.clear()
                        }
                        isFromHandler = !isAppIsInBackground(mActivity)
                    }
                }else{
                    if (!loginObject.isAdmin) {
                        if (this::map.isInitialized) {
                            map.clear()
                        }
                        isFromHandler = !isAppIsInBackground(mActivity)
                    }
                }*/
                if (this::map.isInitialized) {
                    map.clear()
                }
                isFromHandler = !isAppIsInBackground(mActivity)
            } else {
                Utils.showNoInternetMessage(mActivity)
                bottom_live_member_filter.visibility = View.GONE
            }
        }else{
            timerClose()
        }
    }

    private fun timerClose() {
        if (timer != null && updateMembers != null) {
            timer?.cancel()
            updateMembers?.cancel()
        }
    }

    inner class CustomTimerTask : java.util.TimerTask() {
        private val handler = Handler(Looper.getMainLooper())

        // int longitude,latitude;
        override fun run() {
            Thread {
                handler.post {
                    map.clear()
                    isFromHandler = !isAppIsInBackground(mActivity)
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            settingDialogShown = false
                            checkLocationPermission(false)
                        } else {
                            if (gpstracker?.CheckForLoCation() == false) {
                                Utils.showLocationSettingsAlert(mActivity)
                            } else {
                                callApi(false)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }.start()
        }
    }


    fun setAdapter() {
        if (rvLiveImageList!=null) {
            rvLiveImageList.layoutManager = LinearLayoutManager(mActivity, RecyclerView.HORIZONTAL,false)
            liveMemberListAdapter = LiveMemberListAdapter(mActivity, liveMemberList)
            liveMemberListAdapter.notifyDataSetChanged()
            rvLiveImageList.adapter = liveMemberListAdapter
        }
    }


    inner class LiveMemberListAdapter(val context: Context, val memberList: ArrayList<LiveMemberResult>): RecyclerView.Adapter<LiveMemberListAdapter.LiveMemberListHolder>(){
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): LiveMemberListHolder {
            return LiveMemberListHolder(LayoutInflater.from(activity).inflate(R.layout.raw_new_memberlist, p0, false))
        }

        override fun getItemCount(): Int {
            return memberList.size
        }

        override fun onBindViewHolder(p0: LiveMemberListHolder, p1: Int) {
            try {
                if (memberList[p1].profilePath != null) {
                    p0.sdvMemberImage.loadFrescoImage(mActivity, memberList[p1].profilePath ?: "", 1)
                }
                p0.tvMemberListName.text = memberList[p1].memberName ?: ""
                p0.ivLiveMemberInitialize.visibility = View.VISIBLE
                /*if (memberList[p1].isMemberLogin == true){
            }else{
                p0.ivLiveMemberInitialize.visibility = View.GONE
            }*/
                p0.rvMemberDetail.setOnClickListener {
                    mActivity.hideKeyboard()
                    if (sheetBehavior != null) {
                        if (sheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
                            sheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
                        }
                    }
                    hideInfoWindow()
                    map.setPadding(0, 0, 0, 0)
                    val latlng = LatLng(memberList[p1].latitude, memberList[p1].longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, NEAR_ZOOM_VALUE))
                    if (mapFilterLongitude != memberList[p1].latitude && mapFilterLongitude != memberList[p1].longitude) {
                        if (yelpBusinessList.size > 0) {
                            for (i in 0 until yelpBusinessList.size) {
                                if (hashMapMarkerList.size > 0) {
                                    if (hashMapMarkerList[i][yelpBusinessList[i].id] != null) {
                                        val marker = hashMapMarkerList[i][yelpBusinessList[i].id]
                                        marker?.remove()
                                    }
                                }
                            }
                        }
                        yelpBusinessList = ArrayList()
                        toggleListOption()
                        changeFilterDesign()
                    }
                    mapFilterLatitude = memberList[p1].latitude
                    mapFilterLongitude = memberList[p1].longitude
                    bottom_live_member_filter.visibility = View.VISIBLE
                    /*if (liveMemberList.size > 0){
                    for (i in 0 until liveMemberList.size){
                        if (memberList[p1].memberID == liveMemberList[i].memberID){
                            map.moveCamera(CameraUpdateFactory.newLatLng(LatLng(liveMemberList[i].latitude,liveMemberList[i].longitude)))
                            map.moveCamera(CameraUpdateFactory.zoomTo(NEAR_ZOOM_VALUE))
                        }
                    }
                }*/
                }
            } catch (e: Exception){
                e.printStackTrace()
            }
        }

        inner class LiveMemberListHolder(view: View): RecyclerView.ViewHolder(view){
            var rvMemberDetail: RelativeLayout = view.rvMemberDetail
            var sdvMemberImage: SimpleDraweeView = view.sdvMemberImage
            var tvMemberListName: TextView = view.tvMemberListName
            var ivLiveMemberInitialize: ImageView = view.ivLiveMemberInitialize
        }
    }


    fun changeFilterDesign(){
        if (rvMapPinFilter!=null) {
            mapPinAdapter = MapPinAdapter(mActivity, getMapFilter)
            rvMapPinFilter.adapter = mapPinAdapter
            mapPinAdapter.notifyDataSetChanged()
            ivCancelFilter.visibility=View.GONE
        }
    }
    private fun callLiveMemberApi(liveMemberID: Int) {
        Comman_Methods.isProgressShow(mActivity)
        mActivity.closeDrawer()
        mActivity.onLiveMemberPingCalling(1)
        mActivity.isSpeedAvailable()
        val callLiveMember = WebApiClient.getInstance(mActivity)
            .webApi_without?.getLiveMembers(liveMemberID)
        callLiveMember?.enqueue(object : retrofit2.Callback<LiveMemberResponse> {
            override fun onFailure(call: Call<LiveMemberResponse>, t: Throwable) {
                Comman_Methods.isProgressHide()
                if (bottom_live_member_filter!=null) {
                    bottom_live_member_filter.visibility = View.GONE
                }
            }

            override fun onResponse(call: Call<LiveMemberResponse>, response: Response<LiveMemberResponse>) {
                val statusCode: Int = response.code()
                if (statusCode == 200) {
                    if (response.isSuccessful) {
                        Comman_Methods.isProgressHide()
                        response.body()?.let {
                            if (it.status == true) {
                                map.setPadding(0, 0, 0, 0)
                                val liveResultList = it.result ?: ArrayList()
                                if (liveResultList.size > 0) {
                                    if (bottom_live_member_filter != null) {
                                        bottom_live_member_filter.visibility = View.VISIBLE
                                    }
                                    if (this@LiveMemberFragment::callMessageDialog.isInitialized) {
                                        if (callMessageDialog.isShowing) {
                                            callMessageDialog.dismiss()
                                        }
                                    }
                                    liveMemberList = liveResultList
                                    val builder = LatLngBounds.Builder()
                                    if (liveMemberList.size > 0) {
                                        if (tvVisibleLiveCount != null) {
                                            tvVisibleLiveCount.visibility = View.VISIBLE
                                            tvVisibleLiveCount.text = liveMemberList.size.toString()
                                        }
                                        appDatabase.loginDao().getAll().count =
                                            if (liveMemberList.size > 0) {
                                                liveMemberList.size
                                            } else {
                                                appDatabase.loginDao().getAll().count ?: 0
                                            }
                                        val loginObject = appDatabase.loginDao().getAll()
                                        loginObject.count = liveMemberList.size
                                        appDatabase.loginDao().updateLogin(loginObject)
                                        sendMessageToFragment(
                                            appDatabase.loginDao().getAll().count ?: 0,
                                            mActivity
                                        )
                                    } else {
                                        if (tvVisibleLiveCount != null) {
                                            tvVisibleLiveCount.visibility = View.GONE
                                        }
                                        appDatabase.loginDao().getAll().count = 0
                                        val loginObject = appDatabase.loginDao().getAll()
                                        loginObject.count = liveMemberList.size
                                        appDatabase.loginDao().updateLogin(loginObject)
                                        sendMessageToFragment(
                                            appDatabase.loginDao().getAll().count ?: 0,
                                            mActivity
                                        )
                                    }
                                    for (i in 0 until liveMemberList.size) {
                                        val liveMemberBean = liveMemberList[i]
                                        var latLng: LatLng? = null
                                        try {
                                            latLng = LatLng(
                                                liveMemberBean.latitude,
                                                liveMemberBean.longitude
                                            )
                                            //BitmapDescriptor icon4 = BitmapDescriptorFactory.fromResource(R.drawable.ic_location_marker);
                                            /*val markerOptions4 = MarkerOptions().position(latLng)
                                        .title(liveMemberBean.memberID.toString())
                                        .snippet("own")
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_marker))*/

                                            val markerOptions4 =
                                                MarkerOptions().position(latLng)
                                                    .title(liveMemberBean.memberID.toString())
                                                    .snippet("own")
                                                    .icon(
                                                        BitmapDescriptorFactory.fromBitmap(
                                                            createCustomMarker(liveMemberBean.profilePath ?: "")
                                                        )
                                                    )

                                            marker1 = map.addMarker(markerOptions4)!!
                                            if (liveMemberBean.memberID == appDatabase.loginDao().getAll().memberID) {
                                                mapFilterLatitude = liveMemberBean.latitude
                                                mapFilterLongitude = liveMemberBean.longitude
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        builder.include(latLng!!)
                                    }
                                    try {
                                        bounds = builder.build()
                                        setBounds()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    /*if (!isFromHandler) {
                                    try {
                                        bounds = builder.build()
                                        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }*/
                                    if (mapFilterLatitude != 0.0 && mapFilterLongitude != 0.0) {
                                        bottom_live_member_filter.visibility = View.VISIBLE
                                        changeFilterDesign()
                                    } else {
                                        bottom_live_member_filter.visibility = View.GONE
                                    }
                                    if (liveMemberList.size > 0) {
                                        if (rlLiveAddUser != null) {
                                            rlLiveAddUser.visibility = View.VISIBLE
                                        }
                                        setAdapter()
                                    } else {
                                        if (rlLiveAddUser != null) {
                                            rlLiveAddUser.visibility = View.GONE
                                        }
                                    }
                                } else {
                                    mActivity.showMessage(mActivity.resources.getString(R.string.no_data))
                                    bottom_live_member_filter.visibility = View.GONE
                                }
                            }
                        }
                    }
                } else {
                    Comman_Methods.isProgressHide()
                    mActivity.showMessage(mActivity.resources.getString(R.string.error_message))
                    if (bottom_live_member_filter!=null) {
                        bottom_live_member_filter.visibility = View.GONE
                    }
                }
            }
        })
    }

    private fun setBounds() {
        map.setPadding(0,0,0,0)
        var count = 0
        val loginData = appDatabase.loginDao().getAll()
        if (liveMemberList.size > 0) {
            for (i in 0 until liveMemberList.size) {
                if (loginData.memberID == liveMemberList[i].memberID) {
                    map.moveCamera(
                        CameraUpdateFactory.newLatLng(
                            LatLng(
                                liveMemberList[i].latitude,
                                liveMemberList[i].longitude
                            )
                        )
                    )
                }
                if (liveMemberList.size > 1){
                    val distance: Double = distance(liveMemberList[0].latitude,liveMemberList[0].longitude,liveMemberList[i].latitude,liveMemberList[i].longitude)
                    if (distance <= distanceLocationMeter){
                        count += 1
                    }
                }
            }
            when {
                liveMemberList.size == 1 -> {
                    val latlng = LatLng(liveMemberList[0].latitude, liveMemberList[0].longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, NEAR_ZOOM_VALUE))
                }
                count == liveMemberList.size -> {
                    val latlng = LatLng(liveMemberList[0].latitude, liveMemberList[0].longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, ZOOM_VALUE))
                }
                else -> map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 250))
            }
        }
    }


    private var liveMemberList: ArrayList<LiveMemberResult> = ArrayList()

    private inner class CustomInfoWindowAdapter : GoogleMap.InfoWindowAdapter {

        private var infoView: View = mActivity.layoutInflater.inflate(R.layout.popup_livemember_marker, null)

        val tv_LiveMemName: TextView = infoView.tv_liveMemName
        val tv_LiveMemDate: TextView = infoView.tv_liveMemDate
        val tv_LiveMemAddr: TextView= infoView.tv_liveMemAddress
        val tv_LiveMemDeviceDetail: TextView= infoView.tv_liveMemDeviceDetail
        val tvLiveMemberNumber: TextView= infoView.tvLiveMemberNumber
        val tvLiveMemberMobileNumber: TextView= infoView.tvLiveMemberMobileNumber
        val tvYelpPlaceName: TextView= infoView.tvYelpPlaceName
        val tvYelpPlaceRating: TextView= infoView.tvYelpPlaceRating
        val tvYelpPlaceReview: TextView= infoView.tvYelpPlaceReview
        val tvYelpPlaceDistance: TextView= infoView.tvYelpPlaceDistance
        val tvPlaceDistance: TextView= infoView.tvPlaceDistance
        val tvYelpPlacePhone: TextView= infoView.tvYelpPlacePhone
        val tvPlacePhone: TextView= infoView.tvPlacePhone
        val tvYelpPlacePrice: TextView= infoView.tvYelpPlacePrice
        val tvPlacePrice: TextView= infoView.tvPlacePrice
        val tvYelpPlaceCategory: TextView= infoView.tvYelpPlaceCategory
        val tvYelpPlaceAddress: TextView= infoView.tvYelpPlaceAddress
        val tvPlaceCategory: TextView= infoView.tvPlaceCategory
        val rbYelpPlaceRating: RatingBar= infoView.rbYelpPlaceRating
        val llLiveMemberPopUp: LinearLayout= infoView.llLiveMemberPopUp
        val rlNearBy: RelativeLayout= infoView.rlNearBy

        override fun getInfoContents(marker: Marker): View? {

            if (marker != null && marker.isInfoWindowShown) {
                marker.hideInfoWindow()
                marker.showInfoWindow()
            }
            return null
        }

        override fun getInfoWindow(marker: Marker): View {
            map.uiSettings.isMapToolbarEnabled = false
            map.uiSettings.isZoomControlsEnabled = false
            map.setPadding(0, Comman_Methods.convertDpToPixel(150F, mActivity).toInt(),0,0)
            if (sheetBehavior!=null) {
                if (sheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
                    sheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
            if (marker.snippet=="own"){
                for (i in 0 until liveMemberList.size) {
                    rlNearBy.visibility = View.GONE
                    llLiveMemberPopUp.visibility = View.VISIBLE
                    if (marker.title == liveMemberList[i].memberID.toString()) {
                        tv_LiveMemName.text = if (liveMemberList[i].memberName != null) {
                            liveMemberList[i].memberName
                        } else {
                            ""
                        }
                        if (liveMemberList[i].mobile!=null){
                            if (liveMemberList[i].mobile!=""){
                                tvLiveMemberNumber.visibility = View.VISIBLE
                                tvLiveMemberMobileNumber.visibility = View.VISIBLE
                                tvLiveMemberMobileNumber.text = liveMemberList[i].mobile
                            }else{
                                tvLiveMemberNumber.visibility = View.GONE
                                tvLiveMemberMobileNumber.visibility = View.GONE
                            }
                        }else{
                            tvLiveMemberNumber.visibility = View.GONE
                            tvLiveMemberMobileNumber.visibility = View.GONE
                        }
                        val formatter = SimpleDateFormat(DELIVER_DATE_FORMAT)
                        var diagStartDate = ""
                        try {
                            var date1: Date? = null
                            if (liveMemberList[i].startDate != null) {
                                date1 = formatter.parse(liveMemberList[i].startDate ?: "")
                            }
                            val target = SimpleDateFormat(SHOW_DATE_TIME)
                            if (date1 != null) {
                                diagStartDate = target.format(date1)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        tv_LiveMemDate.text = diagStartDate
                        tv_LiveMemAddr.text = liveMemberList[i].locationAddress
                        if (liveMemberList[i].deviceCompanyName.equals("Apple")) {
                            tv_LiveMemDeviceDetail.text =
                                "" + liveMemberList[i].deviceCompanyName + " | " + liveMemberList[i].deviceOS
                        } else {
                            tv_LiveMemDeviceDetail.text =
                                "" + liveMemberList[i].deviceCompanyName + " | " + liveMemberList[i].deviceOS + " | Android"
                        }
                        if (mapFilterLatitude != liveMemberList[i].latitude && mapFilterLongitude != liveMemberList[i].longitude) {
                            if (yelpBusinessList.size > 0) {
                                for (j in 0 until yelpBusinessList.size) {
                                    if (hashMapMarkerList.size > 0) {
                                        if (hashMapMarkerList[j][yelpBusinessList[j].id] != null) {
                                            val marker1 = hashMapMarkerList[j][yelpBusinessList[j].id]
                                            marker1?.remove()
                                        }
                                    }
                                }
                            }
                            changeFilterDesign()
                        }
                        mapFilterLatitude = liveMemberList[i].latitude
                        mapFilterLongitude = liveMemberList[i].longitude
                        bottom_live_member_filter.visibility = View.VISIBLE
                    }
                }
            }else {
                for (j in 0 until yelpBusinessList.size) {
                    rlNearBy.visibility = View.VISIBLE
                    llLiveMemberPopUp.visibility = View.GONE
                    val yelpBusinessObject = yelpBusinessList[j]
                    if (marker.title == (yelpBusinessObject.id ?: "")) {
                        tvYelpPlaceName.text = yelpBusinessObject.name ?: ""
                        val decimalSymbols = DecimalFormatSymbols.getInstance(Locale.US)
                        tvYelpPlaceRating.text = DecimalFormat("##.##", decimalSymbols).format(yelpBusinessObject.rating).toString()
                        rbYelpPlaceRating.rating = yelpBusinessObject.rating?.toFloat() ?: 0F
                        rbYelpPlaceRating.isEnabled = false
                        val distance = yelpBusinessObject.distance ?: 0.0
                        tvYelpPlaceDistance.text =
                            if (distance > 0) DecimalFormat("##.##", decimalSymbols).format(distance / meterToMiles).toString() + " mi" else "- mi"
                        if (yelpBusinessObject.phone != "") {
                            tvYelpPlacePhone.visibility = View.VISIBLE
                            tvPlacePhone.visibility = View.VISIBLE
                            tvYelpPlacePhone.text = yelpBusinessObject.phone ?: ""
                        } else {
                            tvYelpPlacePhone.visibility = View.GONE
                            tvPlacePhone.visibility = View.GONE
                        }
                        if (yelpBusinessObject.price != "") {
                            tvYelpPlacePrice.visibility = View.VISIBLE
                            tvPlacePrice.visibility = View.VISIBLE
                            tvYelpPlacePrice.text = yelpBusinessObject.price ?: ""
                        } else {
                            tvYelpPlacePrice.visibility = View.GONE
                            tvPlacePrice.visibility = View.GONE
                        }
                        tvYelpPlaceReview.text = "(" + yelpBusinessObject.reviewCount?.toString() + ")"
                        tvPlaceCategory.text = mActivity.resources.getString(R.string.str_category) + ":- "
                        yelpBusinessObject.location?.let { location ->
                            location.displayAddress?.let { address ->
                                for (k in 0 until address.size) {
                                    val displayAddress = address[k]
                                    if (k != 0) {
                                        tvYelpPlaceAddress.text = tvYelpPlaceAddress.text.toString() + ", " + displayAddress
                                    } else {
                                        tvYelpPlaceAddress.text = displayAddress
                                    }
                                }

                            }
                        }
                        yelpBusinessObject.categories?.let { category ->
                            for (k in 0 until category.size) {
                                val displayAddress = category[k]
                                if (k != 0) {
                                    tvYelpPlaceCategory.text =
                                        tvYelpPlaceCategory.text.toString() + ", " + displayAddress.title
                                } else {
                                    tvYelpPlaceCategory.text = displayAddress.title
                                }
                            }
                        }
                    }
                }
            }
            return infoView
        }
    }


    companion object {
        fun newInstance(isFrom: Boolean): LiveMemberFragment {
            val args = Bundle()
            args.putBoolean(ARG_PARAM1, isFrom)
            val fragment = LiveMemberFragment()
            fragment.arguments = args
            return fragment
        }

        private fun sendMessageToFragment(count: Int, context: Context) {
            val intent = Intent("LiveMemberCount")
            // You can also include some extra data.
            val b = Bundle()
            b.putInt("count", count)
            intent.putExtra("LiveCount", b)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }


    fun createCustomMarker(profile: String): Bitmap {

        val markerView = (mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.livemember_custom_marker, null)
        val rlLiveMember = markerView.findViewById<RelativeLayout>(R.id.rlLiveMember)
        val sdvMarkerMemberImage = markerView.findViewById<SimpleDraweeView>(R.id.sdvMarkerMemberImage)
        sdvMarkerMemberImage.loadFrescoImage(mActivity,profile,1)
        val displayMetrics = DisplayMetrics()
        mActivity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        markerView.layoutParams = ViewGroup.LayoutParams(20, ViewGroup.LayoutParams.WRAP_CONTENT)
        markerView.measure(displayMetrics.widthPixels, displayMetrics.heightPixels)
        markerView.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)
        markerView.buildDrawingCache()
        val bitmap = Bitmap.createBitmap(markerView.measuredWidth, markerView.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        markerView.draw(canvas)

        return bitmap
    }

    private fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double) : Double{
        val theta: Double = lon1 - lon2
        var dist: Double = sin(deg2rad(lat1)) * sin(deg2rad(lat2)) + cos(deg2rad(lat1)) * cos(deg2rad(lat2)) * Math.cos(deg2rad(theta))
        dist = acos(dist)
        dist = rad2deg(dist)
        dist *= 60 * 1.1515
        return (dist)
    }

    private fun deg2rad(deg: Double): Double {
        return (deg * Math.PI / 180.0)
    }

    private fun rad2deg(rad: Double) : Double{
        return (rad * 180.0 / Math.PI)
    }

    private fun showCallMessageDialog(business: Business){
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.raw_call_message, null)
        val mDialog = AlertDialog.Builder(mActivity)
        mDialog.setView(dialogLayout)
        callMessageDialog = mDialog.create()
        callMessageDialog.window?.attributes?.windowAnimations = R.style.animationForDialog
        callMessageDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val businessPhone = business.phone ?: ""
        if (businessPhone != "") {
            dialogLayout.tvCall.visibility = View.VISIBLE
            dialogLayout.tvSms.visibility = View.VISIBLE
        }else{
            dialogLayout.tvCall.visibility = View.GONE
            dialogLayout.tvSms.visibility = View.GONE
        }
        dialogLayout.tvDirection.visibility = View.GONE
        val businessLat = business.coordinates?.latitude ?: 0.0
        val businessLng = business.coordinates?.longitude ?: 0.0
        if (businessLat != 0.0 && businessLng != 0.0) {
            dialogLayout.tvDirection.visibility = View.VISIBLE
        } else {
            dialogLayout.tvDirection.visibility = View.GONE
        }
        dialogLayout.tvCall.setOnClickListener {
            mActivity.hideKeyboard()
            if (Comman_Methods.isSimExists(mActivity)) {
                Comman_Methods.avoidDoubleClicks(it)
                mActivity.takeCall(business.phone ?: "")
            }else{
                mActivity.showMessage(mActivity.resources.getString(R.string.str_sim_prob))
            }
            callMessageDialog.dismiss()
        }
        dialogLayout.tvSms.setOnClickListener {
            mActivity.hideKeyboard()
            if (Comman_Methods.isSimExists(mActivity)) {
                Comman_Methods.avoidDoubleClicks(it)
                mActivity.sendSMS(business.phone ?: "")
            }else{
                mActivity.showMessage(mActivity.resources.getString(R.string.str_sim_prob))
            }
            callMessageDialog.dismiss()
        }
        dialogLayout.tvDirection.setOnClickListener {
            mActivity.hideKeyboard()
            var lat = 0.0
            var lng = 0.0
            business.coordinates?.let { cor ->
                lat = cor.latitude ?: 0.0
                lng = cor.longitude ?: 0.0
            }
            if (lat != 0.0 && lng != 0.0) {
                Comman_Methods.avoidDoubleClicks(it)
                mActivity.visitUrl("google.navigation:q=$lat,$lng")
            }
            callMessageDialog.dismiss()
        }
        callMessageDialog.setCancelable(true)
        callMessageDialog.show()
    }

    fun showCallMessageToUserDialog(liveUser: LiveMemberResult){
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.raw_call_message, null)
        val mDialog = AlertDialog.Builder(mActivity)
        mDialog.setView(dialogLayout)
        callMessageDialog = mDialog.create()
        callMessageDialog.window?.attributes?.windowAnimations = R.style.animationForDialog
        callMessageDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val liveUserMobile = liveUser.mobile ?: ""
        if (liveUserMobile != "") {
            dialogLayout.tvCall.visibility = View.VISIBLE
            dialogLayout.tvSms.visibility = View.VISIBLE
        }else{
            dialogLayout.tvCall.visibility = View.GONE
            dialogLayout.tvSms.visibility = View.GONE
        }
        if (liveUser.latitude != 0.0 && liveUser.longitude != 0.0) {
            dialogLayout.tvDirection.visibility = View.VISIBLE
        } else {
            dialogLayout.tvDirection.visibility = View.GONE
        }
        dialogLayout.tvCall.setOnClickListener {
            mActivity.hideKeyboard()
            if (Comman_Methods.isSimExists(mActivity)) {
                Comman_Methods.avoidDoubleClicks(it)
                mActivity.takeCall(liveUser.mobile ?: "")
            }else{
                mActivity.showMessage(mActivity.resources.getString(R.string.str_sim_prob))
            }
            callMessageDialog.dismiss()
        }
        dialogLayout.tvSms.setOnClickListener {
            mActivity.hideKeyboard()
            if (Comman_Methods.isSimExists(mActivity)) {
                Comman_Methods.avoidDoubleClicks(it)
                mActivity.sendSMS(liveUser.mobile ?: "")
            }else{
                mActivity.showMessage(mActivity.resources.getString(R.string.str_sim_prob))
            }
            callMessageDialog.dismiss()
        }
        dialogLayout.tvDirection.setOnClickListener {
            mActivity.hideKeyboard()
            if (liveUser.latitude != 0.0 && liveUser.longitude != 0.0) {
                Comman_Methods.avoidDoubleClicks(it)
                mActivity.visitUrl("google.navigation:q=" + liveUser.latitude + "," + liveUser.longitude + "")
            }
            callMessageDialog.dismiss()
        }
        callMessageDialog.setCancelable(true)
        callMessageDialog.show()
    }


    private fun callPingApi(timeEnable: Boolean) {
        mActivity.closeDrawer()
        mActivity.onLiveMemberPingCalling(1)
        mActivity.isSpeedAvailable()

        val androidId: String = Settings.Secure.getString(mActivity.contentResolver, Settings.Secure.ANDROID_ID)
        appDatabase = OldMe911Database.getDatabase(mActivity)

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
                address = singleData.locationAddress ?: ""
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

        val distanceInKilometer = distance[0]*1000

        println("!@@@@@distance = ${distance[0]}")
        println("!@@@@@distanceInKilometer = ${distanceInKilometer}")

        val address = if (distance[0] > 50f){
            ""
        }else{
            loginParamter.locationAddress
        }

//            val distanceInMeter = Comman_Methods.distance(oldLatitude, oldLongitude, gpsLatitude, gpsLongitude)

        /*val address = if (oldLatitudeString!=newLatitude && oldLongitudeString!=newLongitude)
        *//*Utils.encodeString(Utils.getCompleteAddressString(
                context,
                gpsLatitude,
                gpsLongitude
            ))*//* ""
            else loginParamter.locationAddress*/

        login_json.batteryLevel = Utils.GetBatterylevel(mActivity).toString()
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

        if (NotificationManagerCompat.from(mActivity).areNotificationsEnabled()) {
            login_json.notificationPermission = "true"
        } else {
            login_json.notificationPermission = "false"
        }

        login_json.deviceTokenId = AppPreference.getStringPreference(mActivity, BuildConfig.firebasePrefKey)
        login_json.deviceType = "Android"
        login_json.startDate = Comman_Methods.getcurrentDate()
        login_json.password = loginParamter.password

        Utils.callLoginPingLogoutApi(mActivity, login_json, object : CommonApiListener {
            override fun loginResponse(
                status: Boolean,
                loginData: LoginObject?,
                message: String,
                responseMessage: String
            ) {
                if (status) {


                    duplicateGeoFenceCurrentList = ArrayList()
                    duplicateGeoFenceCurrentList = appDatabase.geoFenceDao()
                        .getAllGeoFenceDetail() as ArrayList<GeoFenceResult>

                    try {
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
                        if (loginData != null) {
                            if (loginData.SubscriptionStartDate != null) {

                                login_obj.SubscriptionEndDate = loginData.SubscriptionEndDate
                                login_obj.SubscriptionStartDate = loginData.SubscriptionStartDate

                                val startdate =
                                    inputFormat.parse(loginData.startDate)
                                val start_date_change = outputFormat.format(startdate)
                                val startDateValue =
                                    outputFormat.parse(start_date_change)

                                val enddate =
                                    inputFormat.parse(loginData.SubscriptionEndDate)
                                val end_date_change = outputFormat.format(enddate)
                                val endDateValue = outputFormat.parse(end_date_change)


                                val diffrence: Long =
                                    endDateValue.time - startDateValue.time
                                val days = TimeUnit.DAYS.convert(
                                    diffrence,
                                    TimeUnit.MILLISECONDS
                                )
                                login_obj.time_interval_days = days.toInt()
                            }



                            login_obj.IsNotification =
                                if (loginData.IsNotification != null) loginData.IsNotification else if (oldLoginData.IsNotification != null) oldLoginData.IsNotification else false
                            login_obj.IsSms =
                                if (loginData.IsSms != null) loginData.IsSms else if (oldLoginData.IsSms != null) oldLoginData.IsSms else false
                            login_obj.IsSubscription =
                                if (loginData.IsSubscription != null) loginData.IsSubscription else if (oldLoginData.IsSubscription != null) oldLoginData.IsSubscription else false

                            login_obj.Package =
                                if (loginData.Package != null) if (loginData.Package != "") if (loginData.Package.toInt() > 0) loginData.Package else oldLoginData.Package else oldLoginData.Package else oldLoginData.Package
                            login_obj.count =
                                if (loginData.count != null) (loginData.count ?: 0) else if (oldLoginData.count != null) oldLoginData.count else 0
                            login_obj.deviceDetails =
                                if (loginData.deviceDetails != null) loginData.deviceDetails else if (oldLoginData.deviceDetails != null) oldLoginData.deviceDetails else ""
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
                            login_obj.userName =
                                if (loginData.userName != null) loginData.userName else if (oldLoginData.userName != null) oldLoginData.userName else ""
                            login_obj.profilePath =
                                if (loginData.profilePath != null) loginData.profilePath else if (oldLoginData.profilePath != null) oldLoginData.profilePath else ""
                            login_obj.firstName =
                                if (loginData.firstName != null) loginData.firstName else if (oldLoginData.firstName != null) oldLoginData.firstName else ""
                            login_obj.lastName =
                                if (loginData.lastName != null) loginData.lastName else if (oldLoginData.lastName != null) oldLoginData.lastName else ""
                            login_obj.sequirityQuestionID =
                                if (loginData.sequirityQuestionID != null) loginData.sequirityQuestionID else if (oldLoginData.sequirityQuestionID != null) oldLoginData.sequirityQuestionID else 0
                            login_obj.sequirityAnswer =
                                if (loginData.sequirityAnswer != null) loginData.sequirityAnswer else if (oldLoginData.sequirityAnswer != null) oldLoginData.sequirityAnswer else ""
                            login_obj.domainName = loginData.domainName ?: ""
                            login_obj.subscriptionExpireDate =
                                if (loginData.subscriptionExpireDate != null) loginData.subscriptionExpireDate else if (oldLoginData.subscriptionExpireDate != null) oldLoginData.subscriptionExpireDate else ""
                            login_obj.freeTrail = loginData.freeTrail
                            login_obj.memberUtcDateTime = loginData.memberUtcDateTime
                            login_obj.mobile =
                                if (loginData.mobile != null) loginData.mobile else if (oldLoginData.mobile != null) oldLoginData.mobile else ""
                            login_obj.eventGeoFanceListing =
                                loginData.eventGeoFanceListing
                            if (loginData.lstFamilyMonitoringGeoFence != null) {
                                login_obj.lstFamilyMonitoringGeoFence =
                                    loginData.lstFamilyMonitoringGeoFence
                            } else {
                                login_obj.lstFamilyMonitoringGeoFence =
                                    if (oldLoginData.lstFamilyMonitoringGeoFence != null) oldLoginData.lstFamilyMonitoringGeoFence else ArrayList()
                            }
                            login_obj.frequency =
                                if (loginData.frequency != null) loginData.frequency else if (oldLoginData.frequency != null) oldLoginData.frequency else 0
                            login_obj.totalMembers =
                                if (loginData.totalMembers != null) loginData.totalMembers else if (oldLoginData.totalMembers != null) oldLoginData.totalMembers else 0
                            login_obj.adminID =
                                if (loginData.adminID != null) loginData.adminID else if (oldLoginData.adminID != null) oldLoginData.adminID else 0
                            login_obj.isFromIos =
                                if (loginData.isFromIos != null) loginData.isFromIos else if (oldLoginData.isFromIos != null) oldLoginData.isFromIos else false
                            login_obj.isReport =
                                if (loginData.isReport != null) loginData.isReport else if (oldLoginData.isReport != null) oldLoginData.isReport else false
                            login_obj.loginByApp = loginData.loginByApp ?: 2
                            login_obj.IsAdditionalMember =
                                if (loginData.IsAdditionalMember != null) loginData.IsAdditionalMember else if (oldLoginData.IsAdditionalMember != null) oldLoginData.IsAdditionalMember else false
                            login_obj.ReferralCode =
                                if (loginData.ReferralCode != null) loginData.ReferralCode else if (oldLoginData.ReferralCode != null) oldLoginData.ReferralCode else ""
                            login_obj.ReferralName =
                                if (loginData.ReferralName != null) loginData.ReferralName else if (oldLoginData.ReferralName != null) oldLoginData.ReferralName else ""
                            login_obj.PromocodeUrl =
                                if (loginData.PromocodeUrl != null) loginData.PromocodeUrl else if (oldLoginData.PromocodeUrl != null) oldLoginData.PromocodeUrl else ""
                            login_obj.Promocode =
                                if (loginData.Promocode != null) loginData.Promocode else if (oldLoginData.Promocode != null) oldLoginData.Promocode else ""
                            login_obj.isChildMissing =
                                if (loginData.isChildMissing != null) loginData.isChildMissing else if (oldLoginData.isChildMissing != null) oldLoginData.isChildMissing else false
                            login_obj.clientMobileNumber =
                                if (loginData.clientMobileNumber != null) loginData.clientMobileNumber else if (oldLoginData.clientMobileNumber != null) oldLoginData.clientMobileNumber else ""
                            login_obj.clientImageUrl =
                                if (loginData.clientImageUrl != null) loginData.clientImageUrl else if (oldLoginData.clientImageUrl != null) oldLoginData.clientImageUrl else ""
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
                                    mActivity
                                )
                            }else{
                                sendMessageToFragment(
                                    oldLoginData.count ?: 0,
                                    mActivity
                                )
                            }
                            setGeofenceData(gpsLatitude, gpsLongitude)
                            callApi(timeEnable)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    if (message == "Invalid email or password.") {

                        //   callPingApi("3")
                    }
                }
            }

            override fun onFailureResult() {
                mActivity.showMessage(mActivity.resources.getString(R.string.error_message))
            }
        })
    }

    private fun setGeofenceData(userLatitude: Double, userLongitude: Double) {
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
}