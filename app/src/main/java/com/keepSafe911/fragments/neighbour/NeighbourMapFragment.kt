package com.keepSafe911.fragments.neighbour


import AnimationType
import addFragment
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.location.Location
import android.net.Uri
import android.os.*
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.JsonObject
import com.google.maps.android.SphericalUtil
import com.kotlinpermissions.KotlinPermissions
import com.like.LikeButton
import com.like.OnLikeListener
import com.keepSafe911.BuildConfig
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.gps.GpsTracker
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.model.*
import com.keepSafe911.model.response.*
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import com.keepSafe911.utils.Comman_Methods.Companion.avoidDoubleClicks
import hideKeyboard
import kotlinx.android.synthetic.main.bottom_map_sheet.*
import kotlinx.android.synthetic.main.fragment_neighbour_map.*
import kotlinx.android.synthetic.main.raw_neighbour_image.view.*
import kotlinx.android.synthetic.main.raw_neighbour_vedio.view.*
import kotlinx.android.synthetic.main.raw_neighbour_vedio.view.tvPostComment
import kotlinx.android.synthetic.main.raw_neighbour_vedio.view.tvPostCommentCount
import kotlinx.android.synthetic.main.raw_neighbour_vedio.view.tvPostDate
import kotlinx.android.synthetic.main.raw_neighbour_vedio.view.tvPostDescription
import kotlinx.android.synthetic.main.raw_neighbour_vedio.view.tvPostLike
import kotlinx.android.synthetic.main.raw_neighbour_vedio.view.tvPostLikeCount
import kotlinx.android.synthetic.main.raw_neighbour_vedio.view.tvPostSeparator
import kotlinx.android.synthetic.main.raw_neighbour_vedio.view.tvPostShare
import kotlinx.android.synthetic.main.raw_neighbour_vedio.view.tvPostTimeDuration
import kotlinx.android.synthetic.main.raw_neighbour_vedio.view.tvPostTitle
import kotlinx.android.synthetic.main.toolbar_header.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

//• = \u2022, ● = \u25CF, ○ = \u25CB, ▪ = \u25AA, ■ = \u25A0, □ = \u25A1, ► = \u25BA
class NeighbourMapFragment : HomeBaseFragment(), View.OnClickListener, OnMapReadyCallback,
    GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
    LocationListener {

    companion object {
        fun newInstance(
            isFromUser: ArrayList<FeedResponseResult>
        ): NeighbourMapFragment {
            val args = Bundle()
            args.putParcelableArrayList(ARG_PARAM1, isFromUser)
            val fragment = NeighbourMapFragment()
            fragment.arguments = args
            return fragment
        }
    }

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    private lateinit var oldColors: ColorStateList
    private var mGoogleApiClient: GoogleApiClient? = null
    private var mLocationRequest: LocationRequest? = null
    private lateinit var map: GoogleMap
    internal lateinit var marker1: Marker
    lateinit var appDatabase: OldMe911Database
    private var sheetBehavior: BottomSheetBehavior<*>? = null
    var param1: String = ""
    var param2: String = ""
    var days: Long = 0L
    var incidentTypeID: Int = 0
    private var gpstracker: GpsTracker? = null
    var incidentTypeList: ArrayList<IncidentType> = ArrayList()
    private val TYPE_IMAGE = 2
    private val TYPE_VIDEO = 1
    private var uploadObjectList = ArrayList<FeedResponseResult>()
    lateinit var neighbourAdapter: NeighbourMapAdapter
    lateinit var bottomSheetDialog: BottomSheetDialog
    var value: Double = 0.0
    var areaRadiusList: ArrayList<AreaRadius> = ArrayList()
    var fullName: String = ""
    var loginObject: LoginObject = LoginObject()
    lateinit var containerMapPopUp: ExoPlayerMapRecyclerView
    private var settingDialogShown: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            uploadObjectList = it.getParcelableArrayList(ARG_PARAM1) ?: ArrayList()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        return inflater.inflate(R.layout.fragment_neighbour_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity.hideKeyboard()
        mActivity.disableDrawer()
        appDatabase = OldMe911Database.getDatabase(mActivity)
        loginObject = appDatabase.loginDao().getAll()
        fullName = loginObject.firstName+" "+loginObject.lastName
        gpstracker = GpsTracker(mActivity)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        setHeader()
        if (checkGooglePlayServices()) {
            buildGoogleApiClient()
        }
        mvNeighbourMap.onCreate(savedInstanceState)
        mvNeighbourMap.onResume()

        MapsInitializer.initialize(mActivity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            settingDialogShown = false
            checkLocationPermission()
        }else{
            if (gpstracker?.CheckForLoCation() == false) {
                Utils.showLocationSettingsAlert(mActivity)
            } else {
                if (mGoogleApiClient == null) {
                    buildGoogleApiClient()
                }
            }
        }
        if ((mGoogleApiClient?.isConnecting == false) && (mGoogleApiClient?.isConnected == false)) {
            mGoogleApiClient?.connect()
        }
        mvNeighbourMap.getMapAsync (this)
        sheetBehavior = BottomSheetBehavior.from(bottom_map_option)
        sheetBehavior?.isHideable = false
        sheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(@NonNull bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> { }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        tvNeighbourFilterText.text = mActivity.resources.getString(R.string.str_hide_filter)
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        tvNeighbourFilterText.text = mActivity.resources.getString(R.string.str_show_filter)
                    }
                    BottomSheetBehavior.STATE_DRAGGING -> { }
                    BottomSheetBehavior.STATE_SETTLING -> { }
                    else -> {}
                }
            }
            override fun onSlide(@NonNull bottomSheet: View, slideOffset: Float) { }
        })
        callCategoryApi()
        tvNeighbourFilterText.setOnClickListener(this)
        tv24Hours.setOnClickListener(this)
        tv7days.setOnClickListener(this)
        tv14Days.setOnClickListener(this)
        tv30Days.setOnClickListener(this)
    }

    private fun callCategoryApi(){
        mActivity.isSpeedAvailable()
        Utils.categoryList(mActivity, object : CommonApiListener {
            override fun categoryList(
                status: Boolean,
                categoryResult: ArrayList<CategoryResult>,
                message: String
            ) {
                if (status) {
                    if (categoryResult.size > 0) {
                        incidentTypeList = ArrayList()
                        incidentTypeList.add(
                            IncidentType(
                                0,
                                0,
                                mActivity.resources.getString(R.string.all),
                                true
                            )
                        )
                        for (i in 0 until categoryResult.size) {
                            val category = categoryResult[i]
                            var color = 0
                            when (category.id ?: 0) {
                                1 -> {
                                    color = R.color.caldroid_yellow
                                }
                                2 -> {
                                    color = R.color.color_red
                                }
                                3 -> {
                                    color = R.color.special_green
                                }
                                4 -> {
                                    color = android.R.color.holo_purple
                                }
                                5 -> {
                                    color = R.color.event_color_04
                                }
                                6 -> {
                                    color = R.color.color_purple
                                }
                                else -> {
                                    color = R.color.Date_bg
                                }
                            }
                            incidentTypeList.add(
                                IncidentType(
                                    category.id ?: 0,
                                    color,
                                    category.name ?: "",
                                    true
                                )
                            )
                            textInitialize()
                        }
                    } else {
                        addIncidentTypes()
                        textInitialize()
                    }
                }
            }

            override fun onFailureResult() {
                addIncidentTypes()
                textInitialize()
            }
        })
    }
    private fun addIncidentTypes() {
        incidentTypeList = ArrayList()
        incidentTypeList.add(IncidentType(0,0,mActivity.resources.getString(R.string.all),true))
        incidentTypeList.add(IncidentType(1,R.color.caldroid_yellow,mActivity.resources.getString(R.string.str_news),true))
        incidentTypeList.add(IncidentType(2,R.color.caldroid_light_red,mActivity.resources.getString(R.string.str_crime),true))
        incidentTypeList.add(IncidentType(3,R.color.special_green,mActivity.resources.getString(R.string.str_safety),true))
        incidentTypeList.add(IncidentType(4,android.R.color.holo_purple,mActivity.resources.getString(R.string.str_suspicious),true))
        incidentTypeList.add(IncidentType(5,R.color.event_color_04,mActivity.resources.getString(R.string.str_stranger),true))
        incidentTypeList.add(IncidentType(6,R.color.color_purple,mActivity.resources.getString(R.string.str_lost_pet),true))
    }

    private fun textInitialize(){
        oldColors = tv24Hours.textColors
        tvNeighbourFilterText.text = mActivity.resources.getString(R.string.str_show_filter)
        if (rvIncidentType!=null) {
            rvIncidentType.layoutManager = LinearLayoutManager(mActivity, RecyclerView.HORIZONTAL, false)
            rvIncidentType.adapter = IncidentTypeAdapter(mActivity, incidentTypeList)
        }
        param1 = mActivity.resources.getString(R.string.all)
        param2 = mActivity.resources.getString(R.string.str_30_days)
        setFilterText(param1,param2)
        days = (30*24)
        incidentTypeID = 0
    }
    private fun setHeader() {
        val policy =
            StrictMode.ThreadPolicy.Builder()
                .permitAll().build()
        StrictMode.setThreadPolicy(policy)
        tvHeader.text = mActivity.resources.getString(R.string.neighbour_title)
        Utils.setTextGradientColor(tvHeader)
        iv_back.visibility = View.VISIBLE
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            avoidDoubleClicks(it)
            if (this::bottomSheetDialog.isInitialized){
                if (bottomSheetDialog.isShowing) {
                    containerMapPopUp.onRelease()
                }
            }
            mActivity.onBackPressed()
        }
        mActivity.checkUserActive()
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.tvNeighbourFilterText -> {
                mActivity.hideKeyboard()
                toggleBottomSheet()
            }
            R.id.tv24Hours -> {
                mActivity.hideKeyboard()
                tv24Hours.setTextColor(ContextCompat.getColor(mActivity, R.color.Date_bg))
                tv24Hours.background = ContextCompat.getDrawable(mActivity,R.drawable.green_border)

                tv7days.background = ContextCompat.getDrawable(mActivity,R.color.caldroid_white)
                tv7days.setTextColor(oldColors)
                tv14Days.background = ContextCompat.getDrawable(mActivity,R.color.caldroid_white)
                tv14Days.setTextColor(oldColors)
                tv30Days.background = ContextCompat.getDrawable(mActivity,R.color.caldroid_white)
                tv30Days.setTextColor(oldColors)
                param2 = mActivity.resources.getString(R.string.str_24_hours)
                setFilterText(param1,param2)
                days = 24L
                daysWiseFilter(days,incidentTypeID)
            }
            R.id.tv7days -> {
                mActivity.hideKeyboard()
                tv7days.setTextColor(ContextCompat.getColor(mActivity, R.color.Date_bg))
                tv7days.background = ContextCompat.getDrawable(mActivity,R.drawable.green_border)

                tv24Hours.background = ContextCompat.getDrawable(mActivity,R.color.caldroid_white)
                tv24Hours.setTextColor(oldColors)
                tv14Days.background = ContextCompat.getDrawable(mActivity,R.color.caldroid_white)
                tv14Days.setTextColor(oldColors)
                tv30Days.background = ContextCompat.getDrawable(mActivity,R.color.caldroid_white)
                tv30Days.setTextColor(oldColors)
                param2 = mActivity.resources.getString(R.string.str_7_days)
                setFilterText(param1,param2)
                days = (7*24)
                daysWiseFilter(days,incidentTypeID)
            }
            R.id.tv14Days -> {
                mActivity.hideKeyboard()
                tv14Days.setTextColor(ContextCompat.getColor(mActivity, R.color.Date_bg))
                tv14Days.background = ContextCompat.getDrawable(mActivity,R.drawable.green_border)

                tv7days.background = ContextCompat.getDrawable(mActivity,R.color.caldroid_white)
                tv7days.setTextColor(oldColors)
                tv24Hours.background = ContextCompat.getDrawable(mActivity,R.color.caldroid_white)
                tv24Hours.setTextColor(oldColors)
                tv30Days.background = ContextCompat.getDrawable(mActivity,R.color.caldroid_white)
                tv30Days.setTextColor(oldColors)
                param2 = mActivity.resources.getString(R.string.str_14_days)
                setFilterText(param1,param2)
                days = (14*24)
                daysWiseFilter(days,incidentTypeID)
            }
            R.id.tv30Days -> {
                mActivity.hideKeyboard()
                tv30Days.setTextColor(ContextCompat.getColor(mActivity, R.color.Date_bg))
                tv30Days.background = ContextCompat.getDrawable(mActivity,R.drawable.green_border)

                tv7days.background = ContextCompat.getDrawable(mActivity,R.color.caldroid_white)
                tv7days.setTextColor(oldColors)
                tv14Days.background = ContextCompat.getDrawable(mActivity,R.color.caldroid_white)
                tv14Days.setTextColor(oldColors)
                tv24Hours.background = ContextCompat.getDrawable(mActivity,R.color.caldroid_white)
                tv24Hours.setTextColor(oldColors)
                param2 = mActivity.resources.getString(R.string.str_30_days)
                setFilterText(param1,param2)
                days = (30*24)
                daysWiseFilter(days,incidentTypeID)
            }
        }
    }

    private fun setFilterText(param1: String,param2: String) {
        if (incidentTypeList.size > 0) {
            if (incidentTypeList[0].incidentSelected) {
                tvNeighbourFilter.text = mActivity.resources.getString(R.string.str_past, param2, param1)
            } else {
                var count = 0
                for (i in 0 until incidentTypeList.size) {
                    if (incidentTypeList[i].incidentSelected) {
                        count += 1
                    }
                }
                tvNeighbourFilter.text = when {
                    count == incidentTypeList.size - 1 -> mActivity.resources.getString(
                        R.string.str_past,
                        param2,
                        mActivity.resources.getString(R.string.all)
                    )
                    count == 1 -> mActivity.resources.getString(R.string.str_past_int_single, param2, count)
                    count > 1 -> mActivity.resources.getString(R.string.str_past_int, param2, count)
                    else -> mActivity.resources.getString(
                        R.string.str_past_no,
                        param2,
                        mActivity.resources.getString(R.string.no)
                    )
                }
            }
        }
    }

    private fun toggleBottomSheet() {
        if (sheetBehavior?.state != BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
            tvNeighbourFilterText.text = mActivity.resources.getString(R.string.str_hide_filter)
        } else {
            sheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            tvNeighbourFilterText.text = mActivity.resources.getString(R.string.str_show_filter)
        }
    }

    override fun onMapReady(p0: GoogleMap) {
        map = p0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    mActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                buildGoogleApiClient()
                if (mvNeighbourMap != null && mvNeighbourMap.findViewById<View>(Integer.parseInt("1")) != null) {
                    // Get the button view
                    val locationButton =
                        (mvNeighbourMap.findViewById<View>(Integer.parseInt("1")).parent as View).findViewById<View>(
                            Integer.parseInt("2")
                        )
                    // and next place it, on bottom right (as Google Maps app)
                    val layoutParams = locationButton.layoutParams as RelativeLayout.LayoutParams
                    // position on right bottom
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                    layoutParams.setMargins(0, 0, 30, 30)

//                        map.setInfoWindowAdapter(CustomInfoWindowAdapter())

                }
//                 map.setMyLocationEnabled(true);
            }
        } else {
            buildGoogleApiClient()
            if (mvNeighbourMap != null && mvNeighbourMap.findViewById<View>(Integer.parseInt("1")) != null) {
                // Get the button view
                val locationButton =
                    (mvNeighbourMap.findViewById<View>(Integer.parseInt("1")).parent as View).findViewById<View>(
                        Integer.parseInt("2")
                    )
                // and next place it, on bottom right (as Google Maps app)
                val layoutParams = locationButton.layoutParams as RelativeLayout.LayoutParams
                // position on right bottom
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                layoutParams.setMargins(0, 0, 30, 30)

//                    map.setInfoWindowAdapter(CustomInfoWindowAdapter())

            }
//             map.setMyLocationEnabled(true);
        }

        map.uiSettings.isMapToolbarEnabled = false
        map.uiSettings.isZoomControlsEnabled = false

        /*if (Utils.getAreaRadiusList(mActivity)!=null){
            if (Utils.getAreaRadiusList(mActivity).size > 0) {
                areaRadiusList = ArrayList()
                areaRadiusList.addAll(Utils.getAreaRadiusList(mActivity))
                for (i: Int in 0 until areaRadiusList.size) {
                    if (areaRadiusList[i].memberId == appDatabase.loginDao().getAll().memberID) {
                        value = areaRadiusList[i].memberRadius
                    }
                }
            }else{
                value = 1/10.0
                value *= meterToMiles
            }
        }else{
            value = 1/10.0
            value *= meterToMiles
        }*/
        gpstracker = GpsTracker(mActivity)
        val latitude = gpstracker?.getLatitude() ?: 0.0
        val longitude = gpstracker?.getLongitude() ?: 0.0
        if (latitude != 0.0 && longitude != 0.0) {
            try {
                val builder = LatLngBounds.Builder()
                val pos = LatLng(latitude, longitude)
                val radius = value //radius in meters
                val zoomLevel = ((radius / meterToMiles) * multiplierForZoomLevel) + radius
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, calculateZoomLevel(zoomLevel.toInt()).toFloat()))
                val markerOptions = MarkerOptions().position(pos)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.home_marker))
                    .snippet("0")

                marker1 = map.addMarker(markerOptions)!!
                builder.include(pos)
//                drawOctagonGrid(pos, radius, 1)
                if (uploadObjectList.size > 0) {
                    val duplicateUploadObjectList: ArrayList<FeedResponseResult> = ArrayList()
                    for (i in 0 until uploadObjectList.size) {
                        val formatter = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
                        val formatter2 = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
//                formatter.timeZone = TimeZone.getTimeZone("UTC")
                        try {
                            val date1 = formatter.parse(uploadObjectList[i].createdOn ?: "")
                            val date2 = formatter2.parse(Utils.getCurrentTimeStamp())

                            val differenceTime = daysDifference(date1, date2)

                            if (differenceTime <= (30 * 24)) {
                                duplicateUploadObjectList.add(uploadObjectList[i])
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    setMarkerInMap(duplicateUploadObjectList, builder)
                }
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
        map.setOnMarkerClickListener(GoogleMap.OnMarkerClickListener { marker ->
            try {
                if (this::bottomSheetDialog.isInitialized){
                    if (bottomSheetDialog.isShowing) {
                        if (this::containerMapPopUp.isInitialized) {
                            containerMapPopUp.onRelease()
                        }
                        bottomSheetDialog.dismiss()
                    }
                }
                if (Integer.parseInt(marker.snippet) > 0) {
                    if (uploadObjectList.size > 0) {
                        marker.alpha = 0.6f
                        if (sheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED){
                            sheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
                            tvNeighbourFilterText.text = mActivity.resources.getString(R.string.str_show_filter)
                        }
                        val dialogLayout = layoutInflater.inflate(R.layout.post_popup_layout, null)
                        containerMapPopUp = dialogLayout.findViewById<ExoPlayerMapRecyclerView>(R.id.containerMapPopUp)
                        bottomSheetDialog = BottomSheetDialog(mActivity)
                        bottomSheetDialog.setContentView(dialogLayout)
                        val mBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(dialogLayout.parent as View)
                        mBehavior.isHideable = false
                        bottomSheetDialog.setOnShowListener {
                            mBehavior.peekHeight = dialogLayout.height
                        }
                        bottomSheetDialog.setOnDismissListener {
                            if (this@NeighbourMapFragment::neighbourAdapter.isInitialized) {
                                if (this::containerMapPopUp.isInitialized) {
                                    containerMapPopUp.onRelease()
                                }
                            }
                        }
                        bottomSheetDialog.setOnCancelListener {
                            if (this@NeighbourMapFragment::neighbourAdapter.isInitialized) {
                                if (this::containerMapPopUp.isInitialized) {
                                    containerMapPopUp.onRelease()
                                }
                            }
                        }
                        val userId: Int = appDatabase.loginDao().getAll().memberID
                        var feedResponseResult = FeedResponseResult()
                        for (i in 0 until uploadObjectList.size){
                            if (Integer.parseInt(marker.snippet) == uploadObjectList[i].iD){
                                feedResponseResult = uploadObjectList[i]
                            }
                        }

                        mBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                            override fun onStateChanged(@NonNull bottomSheet: View, newState: Int) {
                                when (newState) {
                                    BottomSheetBehavior.STATE_HIDDEN -> {
                                        if (this@NeighbourMapFragment::neighbourAdapter.isInitialized) {
                                            if (this@NeighbourMapFragment::containerMapPopUp.isInitialized) {
                                                containerMapPopUp.onRelease()
                                            }
                                        }
                                    }
                                    BottomSheetBehavior.STATE_EXPANDED -> { }
                                    BottomSheetBehavior.STATE_COLLAPSED -> {
                                        if (this@NeighbourMapFragment::neighbourAdapter.isInitialized) {
                                            if (this@NeighbourMapFragment::containerMapPopUp.isInitialized) {
                                                containerMapPopUp.onRelease()
                                            }
                                        }
                                    }
                                    BottomSheetBehavior.STATE_DRAGGING -> { }
                                    BottomSheetBehavior.STATE_SETTLING -> { }
                                }
                            }
                            override fun onSlide(@NonNull bottomSheet: View, slideOffset: Float) { }
                        })
                        bottomSheetDialog.show()
                        containerMapPopUp.setVideoInfoList(feedResponseResult)
                        containerMapPopUp.layoutManager = LinearLayoutManager(mActivity, RecyclerView.VERTICAL, false)
                        neighbourAdapter = NeighbourMapAdapter(mActivity, feedResponseResult,userId)
                        containerMapPopUp.adapter = neighbourAdapter
                        /*var firstTime = true
                        if (firstTime) {
                            Handler(Looper.getMainLooper()).post { containerMapPopUp.playVideo() }
                            firstTime = false
                        }
                        containerMapPopUp.scrollToPosition(0)*/
                    }
                }
                return@OnMarkerClickListener true
            } catch (e: Exception) {
                return@OnMarkerClickListener false
            }
        })
        map.setOnMapClickListener {
            if (this::bottomSheetDialog.isInitialized){
                if (bottomSheetDialog.isShowing) {
                    if (this::containerMapPopUp.isInitialized) {
                        containerMapPopUp.onRelease()
                    }
                    bottomSheetDialog.dismiss()
                }
            }
        }
    }


    fun setMarkerInMap(
        uploadList: ArrayList<FeedResponseResult>,
        builder: LatLngBounds.Builder
    ){
        if (uploadList.size > 0) {
            try {
                for (i in 0 until uploadList.size) {
                    /*val distance = FloatArray(2)

                    Location.distanceBetween(
                        uploadList[i]._lat ?: 0.0, uploadList[i]._long ?: 0.0,
                        gpstracker?.getLatitude() ?: 0.0, gpstracker?.getLongitude() ?: 0.0, distance
                    )
                    if (distance[0] > 10000) {

                    } else {*/
                    var color = 0
                    val feedResult = uploadList[i]
                    when (feedResult.categoryID) {
                        1 -> {
                            color = R.color.caldroid_yellow
                        }
                        2 -> {
                            color = R.color.color_red
                        }
                        3 -> {
                            color = R.color.special_green
                        }
                        4 -> {
                            color = android.R.color.holo_purple
                        }
                        5 -> {
                            color = R.color.event_color_04
                        }
                        6 -> {
                            color = R.color.color_purple
                        }
                        else -> {
                            color = R.color.Date_bg
                        }
                    }
                    val uploadPosition = LatLng(feedResult._lat ?: 0.0, feedResult._long ?: 0.0)
                    val markerOptions1 = MarkerOptions().position(uploadPosition)
                        .icon(
                            BitmapDescriptorFactory.fromBitmap(
                                createCustomMarker(
                                    (feedResult.categoryName ?: "").trim(),
                                    color
                                )
                            )
                        )
                        .snippet(feedResult.iD.toString())
                    marker1 = map.addMarker(markerOptions1)!!
                    builder.include(uploadPosition)
                    val bounds: LatLngBounds = builder.build()
                    var count = 0
                    if (uploadList.size > 0) {
                        for (j in 0 until uploadList.size) {
                            val distance: Double = distance(gpstracker?.getLatitude() ?: 0.0, gpstracker?.getLongitude() ?: 0.0,feedResult._lat ?: 0.0, feedResult._long ?: 0.0)
                            if (distance <= distanceLocationMeter){
                                count += 1
                            }
                        }
                        when (count) {
                            uploadList.size -> map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(gpstracker?.getLatitude() ?: 0.0, gpstracker?.getLongitude() ?:0.0),
                                ZOOM_VALUE
                            ))
                            else -> map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 250))
                        }
                    }else{
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(gpstracker?.getLatitude() ?: 0.0, gpstracker?.getLongitude() ?: 0.0),
                            NEAR_ZOOM_VALUE
                        ))
                    }
//                    }
                }
            }catch (e: java.lang.Exception){
                e.printStackTrace()
            }
        }
    }

    private fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double) : Double{
        val theta: Double = lon1 - lon2
        var dist: Double = sin(deg2rad(lat1)) * sin(deg2rad(lat2)) + cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * cos(deg2rad(theta))
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

    private fun drawOctagonGrid(startPosition: LatLng, radius:Double, count:Int){
        var curPos = startPosition
        val width = radius * 2 * Math.sqrt(3.toDouble())/2
        for(i in 0 until count) {
            drawHorizontalHexagon(curPos, radius)
            curPos = SphericalUtil.computeOffset(curPos, width,90.toDouble())
        }
    }

    private fun drawHorizontalHexagon(position:LatLng,radius:Double){
        val coordinates: ArrayList<LatLng> = ArrayList()
        for(angle in 0 until 360 step 45) {
            coordinates.add(SphericalUtil.computeOffset(position, radius, angle.toDouble()))
        }

        val opts = PolygonOptions().addAll(coordinates)
            .strokeColor(ContextCompat.getColor(mActivity, R.color.Date_bg)).strokeWidth(3f)

        map.addPolygon(opts)
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
                    mActivity, Manifest.permission.ACCESS_FINE_LOCATION
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

    private fun checkLocationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
                .permissions(Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                .onAccepted { permissions ->
                    if (permissions.size == 3) {
                        if (ContextCompat.checkSelfPermission(
                                mActivity, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                                mActivity, Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            if (mGoogleApiClient == null) {
                                buildGoogleApiClient()
                            }
                        }
                    }
                }
                .onDenied {
                    if (!settingDialogShown) {
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                            settingDialogShown = true
                        }
                        checkLocationPermission()
                    }
                }
                .onForeverDenied {

                }
                .ask()
        } else {
            KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
                .permissions(Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                .onAccepted { permissions ->
                    if (permissions.size == 2) {
                        if (ContextCompat.checkSelfPermission(
                                mActivity, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                                mActivity, Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            if (mGoogleApiClient == null) {
                                buildGoogleApiClient()
                            }
                        }
                    }
                }
                .onDenied {
                    checkLocationPermission()
                }
                .onForeverDenied {

                }
                .ask()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
            setHeader()
            mActivity.hideKeyboard()
        }
    }

    inner class IncidentTypeAdapter(val context: Context,val incidentTypeList: ArrayList<IncidentType>): RecyclerView.Adapter<IncidentTypeAdapter.IncidentTypeHolder>(){

        var currentPostion = -1
        var isFirst = true
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): IncidentTypeHolder {
            return IncidentTypeHolder(LayoutInflater.from(context).inflate(R.layout.raw_incident,p0,false))
        }

        override fun getItemCount(): Int {
            return incidentTypeList.size
        }

        override fun onBindViewHolder(p0: IncidentTypeHolder, p1: Int) {
            p0.tvIncidentTitle.setOnClickListener {
                if (incidentTypeList[p1].incidentID > 0) {
                    incidentTypeList[p1].incidentSelected = !incidentTypeList[p1].incidentSelected
                    incidentTypeList[0].incidentSelected = false
                    incidentTypeID = incidentTypeList[p1].incidentID
                    daysWiseFilter(days, incidentTypeID)
                } else {
                    isFirst = false
                    incidentTypeID = incidentTypeList[p1].incidentID
                    daysWiseFilter(days, incidentTypeID)
                }
                notifyDataSetChanged()
                /*currentPostion = p0.adapterPosition
                isFirst = false
                notifyDataSetChanged()*/
            }

            if (!isFirst){
//                incidentTypeList[p1].incidentSelected = currentPostion != p1
                for (i in 0 until incidentTypeList.size){
                    incidentTypeList[i].incidentSelected = true
                }
                isFirst = true
            }

            val scale = resources.displayMetrics.density
            val dpAsPixels = (10*scale + 0.5f).toInt()
            p0.tvIncidentTitle.setPadding(dpAsPixels,dpAsPixels,dpAsPixels,dpAsPixels)

            if (incidentTypeList[p1].incidentID == 0){
                p0.tvIncidentTitle.background = ContextCompat.getDrawable(context,R.color.caldroid_white)
                p0.tvIncidentTitle.setTextColor(ContextCompat.getColor(context, R.color.Date_bg))
                p0.tvIncidentTitle.text = incidentTypeList[p1].incidentText
                param1 = mActivity.resources.getString(R.string.all)
                setFilterText(param1,param2)
            }else {
                if (!incidentTypeList[p1].incidentSelected) {
                    p0.tvIncidentTitle.background = ContextCompat.getDrawable(context,R.drawable.green_border)
                    val wordSpan = SpannableString("\u25CF " + incidentTypeList[p1].incidentText)
                    wordSpan.setSpan(
                        ForegroundColorSpan(ContextCompat.getColor(mActivity, incidentTypeList[p1].incidentColor)),
                        0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    wordSpan.setSpan(
                        ForegroundColorSpan(ContextCompat.getColor(context, R.color.Date_bg)),
                        2, incidentTypeList[p1].incidentText.length + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    p0.tvIncidentTitle.text = wordSpan
                    param1 = incidentTypeList[p1].incidentText
                    setFilterText(param1,param2)
                } else {
                    p0.tvIncidentTitle.background = ContextCompat.getDrawable(mActivity,R.drawable.neighbour_back)
                    val wordSpan = SpannableString("\u25CF " + incidentTypeList[p1].incidentText)
                    wordSpan.setSpan(
                        ForegroundColorSpan(ContextCompat.getColor(mActivity, incidentTypeList[p1].incidentColor)),
                        0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    wordSpan.setSpan(
                        ForegroundColorSpan(ContextCompat.getColor(context, R.color.caldroid_white)),
                        2, incidentTypeList[p1].incidentText.length + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    p0.tvIncidentTitle.text = wordSpan
                }
            }
        }

        inner class IncidentTypeHolder(itemView: View): RecyclerView.ViewHolder(itemView){
            var tvIncidentTitle: TextView = itemView.findViewById(R.id.tvIncidentTitle)
        }
    }


    fun createCustomMarker(_name: String, incidentColor: Int): Bitmap {

        val marker = (mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.custom_marker, null)
        val txt_name = marker.findViewById<TextView>(R.id.tvNeighborCustomMarker)
        val wordSpan = SpannableString("\u25CF $_name")
        wordSpan.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(mActivity, incidentColor)),
            0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        txt_name.text = wordSpan

        val displayMetrics = DisplayMetrics()
        mActivity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        marker.layoutParams = ViewGroup.LayoutParams(52, ViewGroup.LayoutParams.WRAP_CONTENT)
        marker.measure(displayMetrics.widthPixels, displayMetrics.heightPixels)
        marker.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)
        marker.buildDrawingCache()
        val bitmap = Bitmap.createBitmap(marker.measuredWidth, marker.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        marker.draw(canvas)

        return bitmap
    }



    inner class NeighbourMapAdapter(
        val context: Context,
        private var uploadList: FeedResponseResult,
        private val userId: Int
    ): RecyclerView.Adapter<BaseViewHolder>() {

        var currentPositionLike:Int = -1
        var currentPositionComment:Int = -1
        var isValueChanged = false
        var isFromButton: Boolean = false
        var isFromApiFalse: Boolean = false

        override fun onCreateViewHolder(viewGroup:  ViewGroup, viewType: Int): BaseViewHolder {
            var viewHolder: BaseViewHolder?  = null
            val inflater :LayoutInflater  = LayoutInflater.from(viewGroup.context)

            when (viewType){
                TYPE_VIDEO ->{
                    val v1: View  = inflater.inflate(R.layout.raw_neighbour_vedio, viewGroup, false)
                    viewHolder =  NeighbourHolder(v1)

                }
                TYPE_IMAGE -> {
                    val v2: View  = inflater.inflate(R.layout.raw_neighbour_image, viewGroup, false)
                    viewHolder =  ImageHolder(v2)
                }
            }
            return  viewHolder!!

        }

        override fun getItemViewType(position: Int): Int {
            return if (uploadList.fileType==1) TYPE_VIDEO else TYPE_IMAGE
        }
        override fun getItemCount(): Int {
            return 1
        }

        override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
            holder.onBind(position)
            if (currentPositionLike == position){
                if (isFromButton) {
                    if (!isValueChanged) {
//                    uploadList.isLiked = uploadList.isLiked == false
                        if (uploadList.isLiked == false) {
                            val commentFeed = LstOfFeedLikeOrComment()
                            uploadList.likeCount = uploadList.likeCount?.plus(1)
                            uploadList.isLiked = true
                            commentFeed.comments = ""
                            commentFeed.date = Utils.getCurrentTimeStamp()
                            commentFeed.feedID = uploadList.iD
                            commentFeed.feedType = 1
                            commentFeed.responseBy = loginObject.memberID
                            commentFeed.name = fullName
                            commentFeed.profileUrl = loginObject.profilePath ?: ""
                            commentFeed.isDeleted = true
                            callLikeCommentShareApi(uploadList, commentFeed)
                        } else {
                            if (uploadList.likeCount!! > 0) {
                                for (i in 0 until uploadList.lstOfFeedLikeOrComments!!.size) {
                                    if (uploadList.lstOfFeedLikeOrComments!![i].feedType!! == 1 && uploadList.lstOfFeedLikeOrComments!![i].responseBy == userId) {
                                        if (uploadList.likeCount!! > 0) {
                                            uploadList.likeCount = uploadList.likeCount?.minus(1)
                                        }
                                        uploadList.isLiked = false
                                        callLikeCommentShareApi(uploadList, uploadList.lstOfFeedLikeOrComments!![i])
                                    }
                                }
                            }
                        }
                        isValueChanged = true
                    }
                } else if (isFromApiFalse){
                    uploadList.isLiked = !(uploadList.isLiked ?: false)
                }
            }else{
                uploadList.isLiked = uploadList.isLiked
            }

            /*if (currentPositionComment == position){
                if (isFromButton) {
                    uploadList.isCommented = !(uploadList.isCommented ?: false)
                    if (uploadList.isCommented == true) {
                        callLikeCommentShareApi(COMMENT, uploadList)
                    } else {
                        if (uploadList.commentCount!! > 0) {
                            uploadList.commentCount = uploadList.commentCount?.minus(1)
                        }
                    }
                } else if (isFromApiFalse){
                    uploadList.isCommented = !(uploadList.isCommented ?: false)
                }
            }else{
                uploadList.isCommented = uploadList.isCommented
            }*/
            var color = 0
            when (uploadList.categoryID){
                1 -> {
                    color = R.color.caldroid_yellow
                }
                2 -> {
                    color = R.color.color_red
                }
                3 -> {
                    color = R.color.special_green
                }
                4 -> {
                    color = android.R.color.holo_purple
                }
                5 -> {
                    color = R.color.event_color_04
                }
                6 -> {
                    color = R.color.color_purple
                }
                else -> {
                    color = R.color.caldroid_black
                }
            }
            when(holder.itemViewType){
                TYPE_VIDEO ->{
                    val nhholder: NeighbourHolder =  holder as NeighbourHolder

                    if (uploadList.userImage!=null) {
                        nhholder.sdvNewsUserVideo.loadFrescoImage(mActivity, uploadList.userImage ?: "", 1)
                    }
                    nhholder.lbHelpful.isLiked = (uploadList.isLiked ?: false)
                    nhholder.rlVideoParent.background = ContextCompat.getDrawable(mActivity,R.color.caldroid_white)
                    val userName = uploadList.addedBy ?: mActivity.resources.getString(R.string.str_neighbor)
                    if (userName!="") {
                        nhholder.tvPostSeparator.text = "\u0009 $userName"
                    }else{
                        nhholder.tvPostSeparator.text = "\u0009 "+mActivity.resources.getString(R.string.str_neighbor)
                    }
                    var differenceTime = ""
                    val formatter = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
                    val formatter2 = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
//                    formatter.timeZone = TimeZone.getTimeZone("UTC")
                    try {
                        val date1 = formatter.parse(uploadList.createdOn ?: "")
                        val date2 = formatter2.parse(Utils.getCurrentTimeStamp())

                        differenceTime = printDifference(date1!!, date2!!)
                    }catch (e: Exception){
                        e.printStackTrace()
                    }
                    nhholder.tvPostTimeDuration.visibility = View.GONE
                    nhholder.tvPostMapTimeDuration.visibility = View.VISIBLE
                    nhholder.tvPostMapTimeDuration.text = differenceTime
                    nhholder.tvPostViewed.visibility = View.GONE
                    nhholder.tvPostTypeVideo.visibility = View.VISIBLE
                    val wordSpan = SpannableString("\u25CF "+uploadList.categoryName)
                    wordSpan.setSpan(
                        ForegroundColorSpan(ContextCompat.getColor(mActivity, color)),
                        0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    nhholder.tvPostTypeVideo.text = wordSpan

                    val tvCommentName = nhholder.tvPostMapTimeDuration.layoutParams as RelativeLayout.LayoutParams
                    if (nhholder.tvPostTypeVideo.text!=""){
                        tvCommentName.addRule(RelativeLayout.START_OF,R.id.flPostDateOptionVideo)
                        nhholder.tvPostMapTimeDuration.layoutParams = tvCommentName
                    }else{
                        tvCommentName.addRule(RelativeLayout.ALIGN_PARENT_END)
                        nhholder.tvPostMapTimeDuration.layoutParams = tvCommentName
                    }

                    var diagStartDate = ""

                    try {
                        val date1 = formatter.parse(uploadList.createdOn ?: "")
                        val target = SimpleDateFormat(OUTPUT_DATE_FORMAT2)
                        if (date1 != null) {
                            diagStartDate = target.format(date1)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }


                    nhholder.tvPostDate.text = diagStartDate
                    nhholder.tvPostTitle.text = uploadList.title
                    nhholder.tvPostDescription.text = uploadList.feeds
                    nhholder.tvPostLikeCount.visibility = View.GONE
                    nhholder.tvPostCommentCount.visibility = View.GONE
                    nhholder.tvPostLikeVideo.text = uploadList.likeCount.toString()
                    nhholder.lbHelpful.setOnLikeListener(object: OnLikeListener {
                        override fun liked(p0: LikeButton?) {
                            avoidDoubleClicks(p0!!)
                            isFromButton = true
                            currentPositionLike = nhholder.bindingAdapterPosition
                            notifyDataSetChanged()
                        }

                        override fun unLiked(p0: LikeButton?) {
                            avoidDoubleClicks(p0!!)
                            isFromButton = true
                            currentPositionLike = nhholder.bindingAdapterPosition
                            notifyDataSetChanged()
                        }
                    })
                    nhholder.tvPostComment.setOnClickListener {
                        mActivity.hideKeyboard()
                        avoidDoubleClicks(it)
                        bottomSheetDialog.dismiss()
                        mActivity.addFragment(
                            NeighborCommentFragment.newInstance(uploadObjectList, uploadList,false),
                            true,
                            true,
                            animationType = AnimationType.bottomtotop
                        )
//                        Toast.makeText(mActivity,mActivity.resources.getString(R.string.under_dev),Toast.LENGTH_SHORT).show()
                        /*isFromButton = true
                        currentPositionComment = nhholder.adapterPosition
                        notifyDataSetChanged()*/
                    }
                    nhholder.ivPlayVideo.setOnClickListener {
                        mActivity.hideKeyboard()
                        avoidDoubleClicks(it)
                        var firstTime = true
                        if (firstTime) {
                            Handler(Looper.getMainLooper()).post {
                                if (this@NeighbourMapFragment::containerMapPopUp.isInitialized) {
                                    containerMapPopUp.playVideo()
                                }
                            }
                            firstTime = false
                        }
                    }
                    nhholder.tvPostShare.setOnClickListener {
                        mActivity.hideKeyboard()
                        avoidDoubleClicks(it)
                        bottomSheetDialog.dismiss()
                        val fileType = uploadList.fileType ?: 0
                        val description = "Hello this news posted by "+userName+"\n\n"+
                                "News Title:- "+(uploadList.title ?: "")+"\n"+
                                "News Type:- "+(uploadList.categoryName ?: "")+"\n"+
                                "News Description:- "+(uploadList.feeds ?: "")

                        if (fileType > 0){
                            if (fileType == 2){
                                val shareIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "*/*"
                                    putExtra(Intent.EXTRA_SUBJECT, uploadList.title ?: "")
                                    putExtra(Intent.EXTRA_TEXT, description)
                                }
                                startActivity(Intent.createChooser(shareIntent, resources.getText(R.string.send_to)))
                            }else{
                                setPermission(fileType, uploadList.title ?: "",description,uploadList.file ?: "")
//                                DownloadTask(uploadList.title,description).execute(uploadList.file ?: "")
                            }
                        } else if (fileType == 0) {
                            setPermission(fileType, uploadList.title ?: "",description,uploadList.file ?: "")
/*
                            Comman_Methods.shareImages(
                                mActivity,
                                uploadList.file ?: "",
                                description,
                                uploadList.title
                            )
*/
                        }
                    }
                }
                TYPE_IMAGE ->{
                    val ivholder: ImageHolder =  holder as ImageHolder
                    ivholder.sdvNewsUserImage.loadFrescoImage(mActivity, uploadList.userImage ?: "", 1)
                    ivholder.lbHelpful.isLiked = uploadList.isLiked ?: false
                    ivholder.rlImageParent.background = ContextCompat.getDrawable(mActivity,R.color.caldroid_white)
                    val userName = uploadList.addedBy ?: mActivity.resources.getString(R.string.str_neighbor)
                    val fileType = uploadList.fileType ?: 0
                    if (userName!="") {
                        ivholder.tvPostSeparator.text = "\u0009 $userName"
                    }else{
                        ivholder.tvPostSeparator.text = "\u0009 "+mActivity.resources.getString(R.string.str_neighbor)
                    }
//                    ivholder.tvPostSeparator.text = "\u25CF News"
                    var differenceTime = ""
                    val formatter = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
                    val formatter2 = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
//                    formatter.timeZone = TimeZone.getTimeZone("UTC")
                    try {
                        val date1 = formatter.parse(uploadList.createdOn ?: "")
                        val date2 = formatter2.parse(Utils.getCurrentTimeStamp())

                        differenceTime = printDifference(date1!!, date2!!)
                    }catch (e: Exception){
                        e.printStackTrace()
                    }
                    ivholder.tvPostTimeDuration.visibility = View.GONE
                    ivholder.tvPostMapTimeDurationImage.visibility = View.VISIBLE
                    ivholder.tvPostMapTimeDurationImage.text = differenceTime
                    ivholder.tvPostViewed.visibility = View.GONE
                    ivholder.tvPostTypeImage.visibility = View.VISIBLE
                    val wordSpan = SpannableString("\u25CF "+uploadList.categoryName)
                    wordSpan.setSpan(
                        ForegroundColorSpan(ContextCompat.getColor(mActivity, color)),
                        0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    ivholder.tvPostTypeImage.text = wordSpan
                    val tvCommentName = ivholder.tvPostMapTimeDurationImage.layoutParams as RelativeLayout.LayoutParams
                    if (ivholder.tvPostTypeImage.text!=""){
                        tvCommentName.addRule(RelativeLayout.START_OF,R.id.flPostDateOptionImage)
                        ivholder.tvPostMapTimeDurationImage.layoutParams = tvCommentName
                    }else{
                        tvCommentName.addRule(RelativeLayout.ALIGN_PARENT_END)
                        ivholder.tvPostMapTimeDurationImage.layoutParams = tvCommentName
                    }

                    /*if (p0.tvDeleteComment.visibility == View.VISIBLE){
                        tvCommentName.addRule(RelativeLayout.START_OF,R.id.tvDeleteComment)
                        tvComment.addRule(RelativeLayout.START_OF,R.id.tvDeleteComment)
                        p0.tvCommentatorName.layoutParams = tvCommentName
                        p0.tvCommentatorComment.layoutParams = tvComment
                    }else{
                        tvCommentName.addRule(RelativeLayout.ALIGN_PARENT_END)
                        tvComment.addRule(RelativeLayout.ALIGN_PARENT_END)
                        p0.tvCommentatorName.layoutParams = tvCommentName
                        p0.tvCommentatorComment.layoutParams = tvComment
                    }*/
                    var diagStartDate = ""

                    try {
                        val date1 = formatter.parse(uploadList.createdOn ?: "")
                        val target = SimpleDateFormat(OUTPUT_DATE_FORMAT2)
                        if (date1 != null) {
                            diagStartDate = target.format(date1)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    ivholder.tvPostDate.text = diagStartDate
                    ivholder.tvPostTitle.text = uploadList.title
                    ivholder.tvPostDescription.text = uploadList.feeds
                    ivholder.tvPostLikeCount.visibility = View.GONE
                    ivholder.tvPostCommentCount.visibility = View.GONE
                    ivholder.tvPostLikeImage.text = uploadList.likeCount.toString()

                    Glide.with(context).load(uploadList.file).into(ivholder.ivPostAwareImageFile)
                    if (fileType > 0){
                        ivholder.flPostFileImage.visibility = View.GONE
                    }else{
                        ivholder.flPostFileImage.visibility = View.VISIBLE
                    }
                    ivholder.lbHelpful.setOnLikeListener(object: OnLikeListener {
                        override fun liked(p0: LikeButton?) {
                            avoidDoubleClicks(p0!!)
                            isFromButton = true
                            currentPositionLike = ivholder.bindingAdapterPosition
                            notifyDataSetChanged()

                        }

                        override fun unLiked(p0: LikeButton?) {
                            avoidDoubleClicks(p0!!)
                            isFromButton = true
                            currentPositionLike = ivholder.bindingAdapterPosition
                            notifyDataSetChanged()
                        }

                    })
                    ivholder.tvPostComment.setOnClickListener {
                        mActivity.hideKeyboard()
                        avoidDoubleClicks(it)
                        bottomSheetDialog.dismiss()
                        mActivity.addFragment(
                            NeighborCommentFragment.newInstance(uploadObjectList,uploadList,false),
                            true,
                            true,
                            animationType = AnimationType.bottomtotop
                        )
//                        Toast.makeText(mActivity,mActivity.resources.getString(R.string.under_dev),Toast.LENGTH_SHORT).show()
                        /*currentPositionComment = ivholder.adapterPosition
                        notifyDataSetChanged()*/
                    }
                    ivholder.tvPostShare.setOnClickListener {
                        mActivity.hideKeyboard()
                        avoidDoubleClicks(it)
                        bottomSheetDialog.dismiss()
                        val description = "Hello this news posted by "+userName+"\n\n"+
                                "News Title:- "+(uploadList.title ?: "")+"\n"+
                                "News Type:- "+(uploadList.categoryName ?: "")+"\n"+
                                "News Description:- "+(uploadList.feeds ?: "")

                        if (fileType > 0){
                            if (fileType == 2){
                                val shareIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "*/*"
                                    putExtra(Intent.EXTRA_SUBJECT, uploadList.title ?: "")
                                    putExtra(Intent.EXTRA_TEXT, description)
                                }
                                startActivity(Intent.createChooser(shareIntent, resources.getText(R.string.send_to)))
                            }else{
                                setPermission(fileType, uploadList.title ?: "",description,uploadList.file ?: "")
//                                DownloadTask(uploadList.title,description).execute(uploadList.file ?: "")
                            }
                        } else if (fileType == 0) {
                            setPermission(fileType, uploadList.title ?: "",description,uploadList.file ?: "")
                            /*Comman_Methods.shareImages(
                                mActivity,
                                uploadList.file ?: "",
                                description,
                                uploadList.title
                            )*/
                        }
                    }
                }
            }
        }

        private fun setPermission(fileType: Int, title: String?, description: String, fileUrl: String) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
                    .permissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                    .onAccepted { permissions ->
                        if (permissions.size == 1) {
                            if (fileType > 0) {
                                if (fileType == 2) {
                                    val shareIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "*/*"
                                        putExtra(Intent.EXTRA_SUBJECT, title)
                                        putExtra(Intent.EXTRA_TEXT, description)
                                    }
                                    startActivity(
                                        Intent.createChooser(
                                            shareIntent,
                                            resources.getText(R.string.send_to)
                                        )
                                    )
                                } else {
                                    downLoadTask(title, description, fileUrl)
                                }
                            } else if (fileType == 0) {
                                Comman_Methods.shareImages(
                                    mActivity,
                                    fileUrl,
                                    description,
                                    title
                                )
                            }
                        }
                    }
                    .onDenied {
                        setPermission(fileType, title, description, fileUrl)
                    }
                    .onForeverDenied {
                        Utils.showSettingsAlert(mActivity)
                    }
                    .ask()
            } else {
                KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
                    .permissions(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                    .onAccepted { permissions ->
                        if (permissions.size == 2) {
                            if (fileType > 0) {
                                if (fileType == 2) {
                                    val shareIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "*/*"
                                        putExtra(Intent.EXTRA_SUBJECT, title)
                                        putExtra(Intent.EXTRA_TEXT, description)
                                    }
                                    startActivity(
                                        Intent.createChooser(
                                            shareIntent,
                                            resources.getText(R.string.send_to)
                                        )
                                    )
                                } else {
                                    downLoadTask(title, description, fileUrl)
                                }
                            } else if (fileType == 0) {
                                Comman_Methods.shareImages(
                                    mActivity,
                                    fileUrl,
                                    description,
                                    title
                                )
                            }
                        }
                    }
                    .onDenied {
                        setPermission(fileType, title, description, fileUrl)
                    }
                    .onForeverDenied {
                        Utils.showSettingsAlert(mActivity)
                    }
                    .ask()
            }
        }

        private fun downLoadTask(title: String?, description: String, urls: String) {
            val executor: ExecutorService = Executors.newSingleThreadExecutor()
            val handler = Handler(Looper.getMainLooper())
            Comman_Methods.isProgressShow(mActivity)
            executor.execute {
                val folder = Utils.getStorageRootPath(mActivity)
                if (!folder.exists()) {
                    folder.mkdir()
                }
                val subFolder = File(folder, "/News/")
                if (!subFolder.exists()) {
                    subFolder.mkdir()
                }
                val storeFileName = "$title.mov"
                val pdfFile = File(subFolder.toString() + File.separator + storeFileName)
                try {
                    pdfFile.createNewFile()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                val u = URL(urls)
                val conn = u.openConnection()
                val contentLength = conn.contentLength

                val stream = DataInputStream(u.openStream())

                val buffer = ByteArray(contentLength)
                stream.readFully(buffer)
                stream.close()

                val fos = DataOutputStream(FileOutputStream(pdfFile))
                fos.write(buffer)
                fos.flush()
                fos.close()
                handler.post {
                    Comman_Methods.isProgressHide()

                    if (pdfFile != null) {
                        val shareIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "*/*"
                            if (Uri.fromFile(pdfFile) != null) {
                                if (Build.VERSION.SDK_INT > 24){
                                    putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(mActivity, BuildConfig.APPLICATION_ID + ".provider",pdfFile))
                                }else{
                                    putExtra(Intent.EXTRA_STREAM, Uri.fromFile(pdfFile))
                                }
                            }
                            putExtra(Intent.EXTRA_SUBJECT, title)
                            putExtra(Intent.EXTRA_TEXT, description)
                        }
                        startActivity(Intent.createChooser(shareIntent, resources.getText(R.string.send_to)))
                    }
                }
            }
        }

        fun printDifference(startDate: Date, endDate: Date): String {
            //milliseconds
            var timeDifference = ""
            var different = endDate.time - startDate.time

            System.out.println("startDate : $startDate")
            System.out.println("endDate : $endDate")
            System.out.println("different : $different")

            val secondsInMilli = 1000L
            val minutesInMilli = secondsInMilli * 60
            val hoursInMilli = minutesInMilli * 60
            val daysInMilli = hoursInMilli * 24

            val elapsedDays = different / daysInMilli
            different %= daysInMilli

            val elapsedHours = different / hoursInMilli
            different %= hoursInMilli

            val elapsedMinutes = different / minutesInMilli
            different %= minutesInMilli

            val elapsedSeconds = different / secondsInMilli

            timeDifference = when {
                elapsedDays > 0L -> elapsedDays.toString()+ mActivity.resources.getString(R.string.str_day_ago)
                elapsedHours > 0L -> elapsedHours.toString()+ mActivity.resources.getString(R.string.str_hour_ago)
                elapsedMinutes > 0L -> elapsedMinutes.toString() + mActivity.resources.getString(R.string.str_min_ago)
                elapsedSeconds > 1L -> elapsedSeconds.toString() + mActivity.resources.getString(R.string.str_second_ago)
                else -> getString(R.string.str_just_now)
            }
            return timeDifference
        }

        fun callLikeCommentShareApi(uploadObject: FeedResponseResult, commentFeed: LstOfFeedLikeOrComment) {
            mActivity.isSpeedAvailable()
            val jsonObject = JsonObject()
            jsonObject.addProperty("ID",commentFeed.id)
            jsonObject.addProperty("Type",1)
            jsonObject.addProperty("Comments", "")
            jsonObject.addProperty("FeedID",uploadObject.iD)
            jsonObject.addProperty("ResponseBy",userId)
            jsonObject.addProperty("Date", Utils.getCurrentTimeStamp())

            Utils.postLikeCommentApi(mActivity, jsonObject, object : CommonApiListener {
                override fun postLikeComment(
                    status: Boolean,
                    likeCommentResult: LikeCommentResult?,
                    message: String,
                    responseMessage: String
                ) {
                    if (status) {
                        if (likeCommentResult != null) {
                            isFromButton = false
                            if (commentFeed.id == 0) {
                                commentFeed.id = likeCommentResult.iD
                                uploadObject.lstOfFeedLikeOrComments?.add(commentFeed)
                            } else {
                                uploadObject.lstOfFeedLikeOrComments?.remove(commentFeed)
                            }
                            isValueChanged = false
                        } else {
                            isFromApiFalse = true
                        }
                    } else {
                        isFromApiFalse = true
                    }
                }
            })
        }

        inner class ImageHolder(view: View):  BaseViewHolder(view){
            override fun clear() {

            }
            var sdvNewsUserImage: SimpleDraweeView = view.sdvNewsUserImage
            var flPostDateOption: FrameLayout = view.flPostDateOptionImage
            var tvPostSeparator: TextView = view.tvPostSeparator
            var tvPostTimeDuration: TextView = view.tvPostTimeDuration
            var tvPostMapTimeDurationImage: TextView = view.tvPostMapTimeDurationImage
            var tvPostViewed: TextView = view.tvPostViewed
            var tvPostDate: TextView = view.tvPostDate
            var tvPostOption: TextView = view.tvPostOption
            var tvPostTitle: TextView = view.tvPostTitle
            var tvPostDescription: TextView = view.tvPostDescription
            var tvPostLikeCount: TextView = view.tvPostLikeCount
            var tvPostCommentCount: TextView = view.tvPostCommentCount
            var tvPostLike: TextView = view.tvPostLike
            var tvPostComment: TextView = view.tvPostComment
            var tvPostShare: TextView = view.tvPostShare
            var tvPostTypeImage: TextView = view.tvPostTypeImage
            var tvPostLikeImage: TextView = view.tvPostLikeImage
            var ivPostAwareImageFile: ImageView = view.ivPostAwareImageFile
            var lbHelpful: LikeButton = view.lbHelpfulImage
            var rlImageParent: RelativeLayout = view.rlImageParent
            var flPostFileImage: FrameLayout = view.flPostFileImage
        }
        inner class NeighbourHolder(view: View): BaseViewHolder(view){
            override fun clear() {

            }

            override fun onBind(position: Int) {
                super.onBind(position)
            }

            var parent:View = view
            var sdvNewsUserVideo: SimpleDraweeView = view.sdvNewsUserVideo
            var flPostDateOption: FrameLayout = view.flPostDateOptionVideo
            var tvPostSeparator: TextView = view.tvPostSeparator
            var tvPostTimeDuration: TextView = view.tvPostTimeDuration
            var tvPostMapTimeDuration: TextView = view.tvPostMapTimeDurationVideo
            var tvPostViewed: TextView = view.tvPostMilesVideo
            var tvPostDate: TextView = view.tvPostDate
            var tvPostOption: TextView = view.tvPostOptionVideo
            var tvPostTitle: TextView = view.tvPostTitle
            var tvPostDescription: TextView = view.tvPostDescription
            var tvPostLikeCount: TextView = view.tvPostLikeCount
            var tvPostCommentCount: TextView = view.tvPostCommentCount
            var tvPostLike: TextView = view.tvPostLike
            var tvPostComment: TextView = view.tvPostComment
            var tvPostShare: TextView = view.tvPostShare
            var tvPostTypeVideo: TextView = view.tvPostTypeVideo
            var tvPostLikeVideo: TextView = view.tvPostLikeVideo
            var ivPlayVideo: ImageView = view.ivPlayVideo
            var lbHelpful: LikeButton = view.lbHelpfulVideo
            var rlVideoParent: RelativeLayout = view.rlVideoParent
            var progressBar: ProgressBar= view.progressBar
            var flPostFileVideo: FrameLayout= view.flPostFileVideoOld
            init {
                ivPlayVideo = view.ivPlayVideo
                progressBar = view.progressBar
                parent.tag = this
            }
        }
    }

    private fun daysDifference(startDate: Date, endDate: Date): Long {
        //milliseconds
        var timeDifference = ""
        var different = endDate.time - startDate.time

        System.out.println("startDate : $startDate")
        System.out.println("endDate : $endDate")
        System.out.println("different : $different")

        val secondsInMilli = 1000L
        val minutesInMilli = secondsInMilli * 60
        val hoursInMilli = minutesInMilli * 60
        val daysInMilli = hoursInMilli * 24

        val elapsedDays = different / daysInMilli
        different %= daysInMilli

        val elapsedHours = if (elapsedDays > 1){
            elapsedDays*24
        }else{
            different / hoursInMilli
        }
        different %= hoursInMilli

        val elapsedMinutes = different / minutesInMilli
        different %= minutesInMilli

        val elapsedSeconds = different / secondsInMilli

        return elapsedHours
    }

    private fun daysWiseFilter(days: Long, type: Int){
        /*if (Utils.getAreaRadiusList(mActivity)!=null){
            if (Utils.getAreaRadiusList(mActivity).size > 0) {
                areaRadiusList = ArrayList()
                areaRadiusList.addAll(Utils.getAreaRadiusList(mActivity))
                for (i: Int in 0 until areaRadiusList.size) {
                    if (areaRadiusList[i].memberId == appDatabase.loginDao().getAll().memberID) {
                        value = areaRadiusList[i].memberRadius
                    }
                }
            }else{
                value = 1/10.0
                value *= meterToMiles
            }
        }else{
            value = 1/10.0
            value *= meterToMiles
        }*/
        val duplicateUploadObjectList: ArrayList<FeedResponseResult> = ArrayList()
        map.clear()
        val builder = LatLngBounds.Builder()
        gpstracker = GpsTracker(mActivity)
        val latitude = gpstracker?.getLatitude() ?: 0.0
        val longitude = gpstracker?.getLongitude() ?: 0.0
        if (latitude!=0.0 && longitude!=0.0) {
            try {
                val pos = LatLng(latitude, longitude)
                val radius = value //radius in meters
                val zoomLevel = ((radius / meterToMiles) * multiplierForZoomLevel) + radius
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, calculateZoomLevel(zoomLevel.toInt()).toFloat()))
                val markerOptions = MarkerOptions().position(pos)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.home_marker))
                    .snippet("0")

                marker1 = map.addMarker(markerOptions)!!
                builder.include(pos)
//                drawOctagonGrid(pos, radius, 1)
            }catch (e: java.lang.Exception){
                e.printStackTrace()
            }
        }
        if (uploadObjectList.size > 0){
            for (i in 0 until uploadObjectList.size){
                val formatter = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
                val formatter2 = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
//                formatter.timeZone = TimeZone.getTimeZone("UTC")
                try {
                    val date1 = formatter.parse(uploadObjectList[i].createdOn ?: "")
                    val date2 = formatter2.parse(Utils.getCurrentTimeStamp())

                    val differenceTime = daysDifference(date1, date2)

                    if (type == 0){
                        if (differenceTime <= days){
                            duplicateUploadObjectList.add(uploadObjectList[i])
                        }
                    }else{
                        for (j in 0 until incidentTypeList.size) {
                            if (incidentTypeList[j].incidentSelected) {
                                if (differenceTime <= days && uploadObjectList[i].categoryID == incidentTypeList[j].incidentID){
                                    duplicateUploadObjectList.add(uploadObjectList[i])
                                }
                            }
                        }
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
            setMarkerInMap(duplicateUploadObjectList, builder)
        }
    }

    private fun calculateZoomLevel(radius: Int): Int {
        val equatorLength = 6378140.0 // in meters
        val display = mActivity.windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width = size.x
        val height = size.y
        val widthInPixels: Double = if (mActivity.resources.getBoolean(R.bool.isTablet)) height.toDouble() else width.toDouble()
        var metersPerPixel = equatorLength / 256
        var zoomLevel = 1
        while ((metersPerPixel * widthInPixels) > radius) {
            metersPerPixel /= 2
            zoomLevel++
        }
        return zoomLevel
    }
}
