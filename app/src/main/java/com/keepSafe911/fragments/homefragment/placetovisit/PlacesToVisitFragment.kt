package com.keepSafe911.fragments.homefragment.placetovisit

import addFragment
import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Point
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.keepSafe911.R
import com.keepSafe911.adapter.PlacesAdapter
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.fragments.homefragment.find.ExploreNearByListFragment
import com.keepSafe911.fragments.neighbour.AddNeighbourFragment
import com.keepSafe911.gps.GpsTracker
import com.keepSafe911.model.MapFilter
import com.keepSafe911.model.response.ApiResponse
import com.keepSafe911.model.response.PlacesResponse
import com.keepSafe911.model.response.PlacesResult
import com.keepSafe911.model.response.yelp.Business
import com.keepSafe911.model.response.yelp.Region
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.room.OldMe911Database
import java.util.*
import com.keepSafe911.utils.*
import com.keepSafe911.webservices.WebApiClient
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
import com.keepSafe911.BuildConfig
import com.kotlinpermissions.KotlinPermissions
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.listner.PositiveButtonListener
import hideKeyboard
import kotlinx.android.synthetic.main.bottom_live_member_sheet.*
import kotlinx.android.synthetic.main.fragment_places_to_visit.*
import kotlinx.android.synthetic.main.popup_livemember_marker.view.*
import kotlinx.android.synthetic.main.raw_call_message.view.*
import kotlinx.android.synthetic.main.raw_live_member_filter.view.*
import kotlinx.android.synthetic.main.toolbar_header.*
import retrofit2.Call
import retrofit2.Response
import sendSMS
import takeCall
import visitUrl
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import kotlin.collections.ArrayList
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class PlacesToVisitFragment : HomeBaseFragment(), OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, LocationListener, View.OnClickListener,
    GoogleMap.OnMapClickListener {


    override fun onMapClick(p0: LatLng) {
        clearYelpBottomSheet(true)
        val placeName = Utils.getCompleteAddressString(mActivity,p0.latitude, p0.longitude)
        setupGoogleMap(p0,placeName,true)
    }


    lateinit var appDatabase: OldMe911Database
    private lateinit var gMap: GoogleMap
    private var mGoogleApiClient: GoogleApiClient? = null
    private var mLocationRequest: LocationRequest? = null
    private var gpstracker: GpsTracker? = null
    var savePlacesToVisitList: ArrayList<PlacesResult> = ArrayList()
    var placesToVisitModel: PlacesResult = PlacesResult()
    var loginObject: LoginObject = LoginObject()
    lateinit var bounds: LatLngBounds
    private lateinit var mPlacesAdapter: PlacesAdapter
    private lateinit var latLng: LatLng
    private lateinit var placeDialog: BottomSheetDialog
    private var sheetBehavior: BottomSheetBehavior<*>? = null
    lateinit var mapPinAdapter: MapPinAdapter
    private var mapFilterLatitude: Double = 0.0
    private var mapFilterLongitude: Double = 0.0
    private var mapFilterTermName: String = ""
    private var mapFilterCategoryName: String = ""
    private var mapFilterID: Int = 0
    var yelpBusinessList: ArrayList<Business> = ArrayList()
    var yelpTotal: Int = 0
    var yelpRegion: Region = Region()
    var hashMapMarkerList: ArrayList<HashMap<String,Marker>> = ArrayList()
    internal lateinit var marker2: Marker
    lateinit var callMessageDialog: Dialog

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
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
        return inflater.inflate(R.layout.fragment_places_to_visit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity.disableDrawer()
        mActivity.hideKeyboard()
        appDatabase = OldMe911Database.getDatabase(mActivity)
        gpstracker = GpsTracker(mActivity)
        setHeader()
        savePlacesToVisitList = ArrayList()
        mvPlacesVisit.onCreate(savedInstanceState)
        mPlacesAdapter = PlacesAdapter(mActivity, R.layout.row_places)
        etSearchPlace.setAdapter(mPlacesAdapter)
        etSearchPlace.onItemClickListener = mAutocompleteClickListener


        /*for (i in 0 until appDatabase.placeToVisitDao().getAllPlaceVisit().size){
            val placeData = appDatabase.placeToVisitDao().getAllPlaceVisit()[i]
            if (loginObject.memberID == placeData.memberId){
                savePlacesToVisitList.add(placeData)
            }
        }*/

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
                    else -> {}
                }
            }
            override fun onSlide(@NonNull bottomSheet: View, slideOffset: Float) { }
        })
        tvMapPinFilterText.setOnClickListener(this)
        tvCloseMapFilter.setOnClickListener(this)
        ivCancelFilter.setOnClickListener(this)
        ivCancelFilter?.visibility = View.GONE
        tvMapPinFilter.text = mActivity.resources.getString(R.string.str_explore_by)
        rvMapPinFilter.layoutManager = GridLayoutManager(mActivity, 2, RecyclerView.VERTICAL, false)
        mapPinAdapter = MapPinAdapter(mActivity, MapFilter.getMapFilter)
        rvMapPinFilter.adapter = mapPinAdapter
        callGetPlaces(true)
        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            etSearchPlace.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        } else {
            etSearchPlace.imeOptions = EditorInfo.IME_ACTION_DONE
        }
    }

    override fun onClick(v: View?) {
        when(v?.id){
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
                clearYelpBottomSheet(true)
            }
        }
    }

    private fun clearYelpBottomSheet(isMapViewChange: Boolean){
        mActivity.hideKeyboard()
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
        yelpBusinessList = ArrayList()
        hideInfoWindow()
        changeFilterDesign()
        if (sheetBehavior?.state != BottomSheetBehavior.STATE_COLLAPSED) {
            sheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            rlFilterModule.visibility = View.VISIBLE
            tvCloseMapFilter.visibility = View.GONE
        }
        if (bottom_live_member_filter!=null) {
            bottom_live_member_filter.visibility = View.GONE
        }
        mapFilterLatitude = 0.0
        mapFilterLongitude = 0.0
        try {
            if (this::bounds.isInitialized) {
                if (bounds!=null) {
                    if (isMapViewChange) {
                        setBounds(false)
                    }
//                            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
//                            map.moveCamera(CameraUpdateFactory.zoomTo(ZOOM_VALUE))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideInfoWindow() {
        if (this::marker2.isInitialized) {
            if (marker2 != null) {
                if (marker2.isInfoWindowShown) {
                    marker2.hideInfoWindow()
                }
            }
        }
    }

    private fun setHeader() {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        mActivity.checkNavigationItem(5)
        loginObject = appDatabase.loginDao().getAll()
        tvHeader.text = mActivity.resources.getString(R.string.str_place_visit)
        Utils.setTextGradientColor(tvHeader)
        iv_menu.visibility = View.VISIBLE
        tvList.visibility = View.VISIBLE
        iv_menu.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.openDrawer()
        }
        tvList.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            val termsName = if (yelpBusinessList.size > 0){
                mapFilterCategoryName
            }else {
                mActivity.resources.getString(R.string.str_place_visit)
            }
            val updatedSavePlacesToVisitList: ArrayList<PlacesResult> = ArrayList()
            for (i in 0 until savePlacesToVisitList.size) {
                if ((savePlacesToVisitList[i].iD ?: 0) > 0) {
                    updatedSavePlacesToVisitList.add(savePlacesToVisitList[i])
                }
            }
            mActivity.addFragment(
                ExploreNearByListFragment.newInstance(yelpBusinessList.size <= 0,yelpBusinessList, termsName, updatedSavePlacesToVisitList,mapFilterLatitude,mapFilterLongitude),
                true,
                true,
                animationType = AnimationType.fadeInfadeOut
            )
        }
        mActivity.checkUserActive()
    }

    private val mAutocompleteClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
        mActivity.hideKeyboard()
        try {
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
            yelpBusinessList = ArrayList()
            val item = mPlacesAdapter.getItem(position)
            latLng = Utils.getLocationFromAddress(mActivity, item!!.toString())!!
            setupGoogleMap(latLng,item.toString(), true)
        }catch (e: java.lang.Exception){
            e.printStackTrace()
        }
    }

    private fun setupGoogleMap(ll: LatLng?, placeName: String, isAdding: Boolean) {
        if (checkGooglePlayServices()) {
            buildGoogleApiClient()
        }
        mvPlacesVisit.onResume()
        MapsInitializer.initialize(mActivity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission()
        } else {
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
        etSearchPlace.setText("")
        mvPlacesVisit.getMapAsync(OnMapReadyCallback { googleMap ->
            gMap = googleMap
            gMap.clear()
            gMap.uiSettings.isMyLocationButtonEnabled = false
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
                return@OnMapReadyCallback
            }
            gMap.setPadding(0,0,0,0)
            gMap.setInfoWindowAdapter(CustomInfoWindowAdapter())
            //                gMap.setMyLocationEnabled(true);

            if (ll != null) {
                var duplicatePlace = 0
                for (i in 0 until savePlacesToVisitList.size){
                    val savePlace = savePlacesToVisitList[i]
                    if (savePlace.latitude != ll.latitude && savePlace.longitude != ll.longitude){
                        duplicatePlace += 1
                    }
                }
                if (isAdding) {
                    if (savePlacesToVisitList.size > 0){
                        if (duplicatePlace == savePlacesToVisitList.size){
                            placesToVisitModel = PlacesResult()
                            placesToVisitModel.iD = 0
                            placesToVisitModel.userId = loginObject.memberID
                            placesToVisitModel.address = placeName
                            placesToVisitModel.latitude = ll.latitude
                            placesToVisitModel.longitude = ll.longitude
                            placesToVisitModel.visitDate = ""
                            savePlacesToVisitList.add(placesToVisitModel)
                        }
                    }else {
                        placesToVisitModel = PlacesResult()
                        placesToVisitModel.iD = 0
                        placesToVisitModel.userId = loginObject.memberID
                        placesToVisitModel.address = placeName
                        placesToVisitModel.latitude = ll.latitude
                        placesToVisitModel.longitude = ll.longitude
                        placesToVisitModel.visitDate = ""
                        savePlacesToVisitList.add(placesToVisitModel)
                    }
                }
                if (savePlacesToVisitList.size > 0) {
                    try {
                        val builder = LatLngBounds.Builder()
                        for (i in 0 until savePlacesToVisitList.size) {
                            val placesToVisitModel = savePlacesToVisitList[i]
                            val placeLatitude = placesToVisitModel.latitude ?: 0.0
                            val placeLongitude = placesToVisitModel.longitude ?: 0.0
                            val latLng = LatLng(placeLatitude, placeLongitude)
                            builder.include(latLng)
                            if ((savePlacesToVisitList[i].iD ?: 0) > 0){
                                val marker = gMap.addMarker(
                                    MarkerOptions().position(latLng)
                                        .title(placeLatitude.toString()+placeLongitude.toString())
                                        .snippet("own")
//                        .title(Utils.getCompleteAddressString(mActivity, ll.latitude, ll.longitude))
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_green_marker))
                                )
                            }else{
                                val marker = gMap.addMarker(
                                    MarkerOptions().position(latLng)
                                        .title(placeLatitude.toString()+placeLongitude.toString())
                                        .snippet("own")
//                        .title(Utils.getCompleteAddressString(mActivity, ll.latitude, ll.longitude))
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_red_marker))
                                )
                            }
                        }
                        bounds = builder.build()
                        setBounds(isAdding)
                    }catch (e: java.lang.Exception){
                        e.printStackTrace()
                    }
                }
            }
            gMap.setOnInfoWindowClickListener {
                if (yelpBusinessList.size > 0) {
                    for (j in 0 until yelpBusinessList.size) {
                        if (it.title == yelpBusinessList[j].id) {
                            showCallMessageDialog(yelpBusinessList[j])
                        }
                    }
                }
            }

            gMap.setOnMapClickListener(this@PlacesToVisitFragment)

            gMap.setOnMarkerClickListener(GoogleMap.OnMarkerClickListener { marker ->
                try {

//                    gMap.moveCamera(CameraUpdateFactory.newLatLng(targetPosition))
                    if (marker.snippet == "own") {
                        for (i in 0 until savePlacesToVisitList.size) {
                            val placesToVisitModel = savePlacesToVisitList[i]
                            val placeLatitude = placesToVisitModel.latitude ?: 0.0
                            val placeLongitude = placesToVisitModel.longitude ?: 0.0
                            val placeAddresses = placesToVisitModel.address ?: ""
                            if (marker.title == placeLatitude.toString() + placeLongitude.toString()) {
//                            marker.showInfoWindow()
                                marker.hideInfoWindow()
                                if (bottom_live_member_filter!=null){
                                    if (bottom_live_member_filter.visibility == View.VISIBLE) {
                                        if (sheetBehavior?.state != BottomSheetBehavior.STATE_COLLAPSED) {
                                            sheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
                                            rlFilterModule.visibility = View.VISIBLE
                                            tvCloseMapFilter.visibility = View.GONE
                                        }
                                    }
                                }
                                if (mapFilterLatitude != placeLatitude && mapFilterLongitude != placeLongitude) {
                                    clearYelpBottomSheet(false)
                                    gMap.moveCamera(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(placeLatitude, placeLongitude),
                                            NEAR_ZOOM_VALUE
                                        )
                                    )
                                } else if (yelpBusinessList.size > 0) {
                                    addYelpMarker(yelpBusinessList, mapFilterID)
                                } else {
                                    gMap.moveCamera(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(placeLatitude, placeLongitude),
                                            NEAR_ZOOM_VALUE
                                        )
                                    )
                                }
                                val placeAddress = if (placeAddresses != "") {
                                    placeAddresses
                                } else {
                                    Utils.getCompleteAddressString(mActivity, placeLatitude, placeLongitude)
                                }
                                val view = layoutInflater.inflate(
                                    R.layout.report_option_sheet,
                                    mActivity.window.decorView.rootView as ViewGroup,
                                    false
                                )
                                if (this::placeDialog.isInitialized) {
                                    if (placeDialog.isShowing) {
                                        placeDialog.dismiss()
                                    }
                                }
                                placeDialog = BottomSheetDialog(mActivity)
                                placeDialog.setContentView(view)
                                val mBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(view.parent as View)
                                mBehavior.isHideable = false
                                placeDialog.setOnShowListener {
                                    mBehavior.peekHeight = view.height
                                }
                                val tvSendReport: TextView? = placeDialog.findViewById(R.id.tvSendReport)
                                val tvViewReport: TextView? = placeDialog.findViewById(R.id.tvViewReport)
                                val tvDownloadReport: TextView? = placeDialog.findViewById(R.id.tvDownloadReport)
                                val tvCancelReport: TextView? = placeDialog.findViewById(R.id.tvCancelReport)
                                val tvPlaceTitle: TextView? = placeDialog.findViewById(R.id.tvPlaceTitle)
                                val tvRatePlace: TextView? = placeDialog.findViewById(R.id.tvRatePlace)
                                val ivSharePlace: ImageView? = placeDialog.findViewById(R.id.ivSharePlace)
                                tvPlaceTitle?.visibility = View.VISIBLE
                                var diagStartDate = ""
                                if (placesToVisitModel.visitDate != "") {
                                    var placeDate = ""
                                    var placeDate2 = ""
                                    val formatter = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
                                    val target = SimpleDateFormat(SHOW_DATE_TIME)
                                    val target2 = SimpleDateFormat(CHECK_DATE_TIME2)
                                    try {
                                        val date1 = formatter.parse(placesToVisitModel.visitDate ?: "")
                                        if (date1 != null) {
                                            placeDate = target.format(date1)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    try {
                                        var date1: Date? = null
                                        if (placesToVisitModel.visitDate != null) {
                                            date1 = formatter.parse(placesToVisitModel.visitDate ?: "")
                                        }
                                        if (date1 != null) {
                                            placeDate2 = target2.format(date1)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    if (placeDate != "") {
                                        diagStartDate = "\n" + mActivity.resources.getString(
                                            R.string.str_select_place_date,
                                            placeDate
                                        )
                                    } else if (placeDate2 != "") {
                                        diagStartDate = "\n" + mActivity.resources.getString(
                                            R.string.str_select_place_date,
                                            placeDate2
                                        )
                                    }
                                }
                                tvRatePlace?.visibility = View.VISIBLE
                                val placeAddressFinal =
                                    mActivity.resources.getString(R.string.str_select_place, placeAddress)
                                tvPlaceTitle?.text = placeAddressFinal + diagStartDate
                                if ((placesToVisitModel.iD ?: 0) > 0) {
                                    tvSendReport?.text = mActivity.resources.getString(R.string.str_explore_by)
                                    if (mapFilterLatitude != placeLatitude && mapFilterLongitude != placeLongitude) {
                                        tvSendReport?.visibility = View.VISIBLE
                                    }else{
                                        tvSendReport?.visibility = View.GONE
                                    }
                                    tvViewReport?.visibility = View.VISIBLE
                                    tvDownloadReport?.visibility = View.VISIBLE
                                    tvCancelReport?.visibility = View.VISIBLE
                                    ivSharePlace?.visibility = View.VISIBLE
                                    if (placesToVisitModel.isVisited == true){
                                        tvRatePlace?.visibility = View.VISIBLE
                                    }else{
                                        tvRatePlace?.visibility = View.GONE
                                    }
                                } else {
                                    tvSendReport?.visibility = View.VISIBLE
                                    tvSendReport?.text = mActivity.resources.getString(R.string.str_place_save)
                                    tvViewReport?.visibility = View.GONE
                                    tvDownloadReport?.visibility = View.GONE
                                    tvCancelReport?.visibility = View.VISIBLE
                                    ivSharePlace?.visibility = View.GONE
                                    tvRatePlace?.visibility = View.GONE
                                }

                                tvViewReport?.text = mActivity.resources.getString(R.string.str_place_update)
                                tvDownloadReport?.text = mActivity.resources.getString(R.string.str_place_delete)
                                tvCancelReport?.text = mActivity.resources.getString(R.string.cancel)

                                tvSendReport?.setOnClickListener {
                                    mActivity.hideKeyboard()
                                    Comman_Methods.avoidDoubleClicks(it)
                                    if ((placesToVisitModel.iD ?: 0) > 0) {
                                        mapFilterLatitude = placeLatitude
                                        mapFilterLongitude = placeLongitude
                                        if (bottom_live_member_filter!=null) {
                                            bottom_live_member_filter.visibility = View.VISIBLE
                                        }
                                        if (sheetBehavior?.state != BottomSheetBehavior.STATE_EXPANDED) {
                                            sheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                                            rlFilterModule.visibility = View.GONE
                                            tvCloseMapFilter.visibility = View.VISIBLE
                                        }
                                    }else {
                                        var duplicatePlace = 0
                                        if (savePlacesToVisitList.size > 0) {
                                            if (savePlacesToVisitList.size > 1) {
                                                for (k in 0 until savePlacesToVisitList.size) {
                                                    val savePlace = savePlacesToVisitList[k]
                                                    if (placesToVisitModel.latitude != savePlace.latitude && placesToVisitModel.longitude != savePlace.longitude) {
                                                        duplicatePlace += 1
                                                    }
                                                }
                                                if (duplicatePlace == savePlacesToVisitList.size - 1) {
                                                    showNumberPickerDialog(mActivity, i, placesToVisitModel)
                                                }
                                            } else {
                                                showNumberPickerDialog(mActivity, i, placesToVisitModel)
                                            }
                                        }
                                    }
                                    placeDialog.dismiss()
                                }
                                tvRatePlace?.setOnClickListener {
                                    mActivity.hideKeyboard()
                                    Comman_Methods.avoidDoubleClicks(it)
                                    showRatingPlaceDialog(mActivity, i, placesToVisitModel)
                                    placeDialog.dismiss()
                                }
                                tvViewReport?.setOnClickListener {
                                    mActivity.hideKeyboard()
                                    Comman_Methods.avoidDoubleClicks(it)
                                    showNumberPickerDialog(mActivity, i, placesToVisitModel)
                                    placeDialog.dismiss()
                                }
                                tvDownloadReport?.setOnClickListener {
                                    mActivity.hideKeyboard()
                                    if (savePlacesToVisitList.size > 0) {
                                        for (k in 0 until savePlacesToVisitList.size) {
                                            val savePlace = savePlacesToVisitList[k]
                                            if (placesToVisitModel.latitude == savePlace.latitude && placesToVisitModel.longitude == savePlace.longitude) {
                                                Comman_Methods.avoidDoubleClicks(it)
                                                removePlaceToVisit(placesToVisitModel, marker)
                                            }
                                        }
                                    }
                                    placeDialog.dismiss()
                                }
                                tvCancelReport?.setOnClickListener {
                                    mActivity.hideKeyboard()
                                    Comman_Methods.avoidDoubleClicks(it)
                                    if ((placesToVisitModel.iD ?: 0) <= 0) {
                                        savePlacesToVisitList.remove(placesToVisitModel)
                                        marker.remove()
                                        setupGoogleMap(LatLng(gpstracker?.getLatitude() ?: 0.0,gpstracker?.getLongitude() ?: 0.0),mActivity.resources.getString(R.string.back_home),false)
                                    }
                                    placeDialog.dismiss()
                                }
                                ivSharePlace?.setOnClickListener {
                                    mActivity.hideKeyboard()
                                    if ((placesToVisitModel.iD ?: 0) > 0) {
                                        if (placeLatitude!=0.0 && placeLongitude!=0.0){
                                            Comman_Methods.avoidDoubleClicks(it)
                                            mActivity.addFragment(
                                                AddNeighbourFragment.newInstance(true,placesToVisitModel),
                                                true,
                                                true,
                                                animationType = AnimationType.bottomtotop
                                            )
                                        }
                                    }
                                    placeDialog.dismiss()
                                }
                                placeDialog.setCancelable(false)
                                if (!placeDialog.isShowing) {
                                    placeDialog.show()
                                }
                            }
                        }
                    }else{
                        val projection = gMap.projection
                        val markerPosition = marker.position
                        val markerPoint = projection.toScreenLocation(markerPosition)
                        val targetPoint = Point(markerPoint.x, markerPoint.y - requireView().height / 3)
                        val targetPosition = projection.fromScreenLocation(targetPoint)
                        gMap.animateCamera(CameraUpdateFactory.newLatLng(targetPosition), 1000, null)
                        marker.showInfoWindow()
                    }
                    return@OnMarkerClickListener true
                }catch (e: java.lang.Exception){
                    e.printStackTrace()
                    return@OnMarkerClickListener false
                }
            })
        })
    }

    private fun callAddUpdatePlaces(jsonObject: JsonObject) {
        mActivity.isSpeedAvailable()
        Utils.addVisitPlacesApi(mActivity, jsonObject, object : CommonApiListener {
            override fun commonResponse(
                status: Boolean,
                message: String,
                responseMessage: String,
                result: String
            ) {
                if (status) {
                    callGetPlaces(true)
                }
            }
        })
    }

    private fun removePlaceToVisit(
        placesToVisitModel: PlacesResult,
        marker: Marker
    ) {
        Comman_Methods.isCustomPopUpShow(mActivity,
        title = mActivity.resources.getString(R.string.travel_ite_conf),
        message = mActivity.resources.getString(R.string.wish_travel_ite),
        positiveButtonListener = object : PositiveButtonListener {
            override fun okClickListener() {
                callDeletePlaceApi(placesToVisitModel, marker)
//            savePlacesToVisitList.remove(placesToVisitModel)
//            appDatabase.placeToVisitDao().deletePlace(placesToVisitModel)
//            marker.remove()
            }
        })
    }

    private fun callDeletePlaceApi(placesToVisitModel: PlacesResult, marker: Marker) {
        mActivity.isSpeedAvailable()
        Utils.deleteVisitedPlaces(mActivity, placesToVisitModel.iD ?: 0, object : CommonApiListener {
            override fun commonResponse(
                status: Boolean,
                message: String,
                responseMessage: String,
                result: String
            ) {
                if (status) {
                    savePlacesToVisitList.remove(placesToVisitModel)
                    marker.remove()
                    callGetPlaces(true)
                }
            }
        })
    }

    private fun callGetPlaces(isNearByShow: Boolean) {
        if (ConnectionUtil.isInternetAvailable(mActivity)){
            Comman_Methods.isProgressShow(mActivity)
            mActivity.isSpeedAvailable()
            val callDeletePlace = WebApiClient.getInstance(mActivity).webApi_without?.getVisitPlacesDetails(loginObject.memberID)
            callDeletePlace?.enqueue(object : retrofit2.Callback<PlacesResponse>{
                override fun onFailure(call: Call<PlacesResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                    setupGoogleMap(LatLng(gpstracker?.getLatitude() ?: 0.0,gpstracker?.getLongitude() ?: 0.0),mActivity.resources.getString(R.string.back_home),false)
                }

                override fun onResponse(call: Call<PlacesResponse>, response: Response<PlacesResponse>) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                if (it.status == true) {
                                    if (isNearByShow) {
                                        if (bottom_live_member_filter != null) {
                                            bottom_live_member_filter.visibility = View.GONE
                                        }
                                    }
                                    val placeList = it.result ?: ArrayList()
                                    if (placeList.size > 0) {
                                        if (savePlacesToVisitList.size > 0) {
                                            for (i in 0 until savePlacesToVisitList.size) {
                                                val savePlace = savePlacesToVisitList[i]
                                                for (j in 0 until placeList.size) {
                                                    val responsePlace = placeList[j]
                                                    if (savePlace.latitude == responsePlace.latitude && savePlace.longitude == responsePlace.longitude) {
                                                        savePlacesToVisitList[i] = responsePlace
                                                    }
                                                }
                                            }
                                        } else {
                                            savePlacesToVisitList = ArrayList()
                                            savePlacesToVisitList.addAll(placeList)
                                        }
                                        if (this@PlacesToVisitFragment::callMessageDialog.isInitialized) {
                                            if (callMessageDialog.isShowing) {
                                                callMessageDialog.dismiss()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                    }
                    setupGoogleMap(LatLng(gpstracker?.getLatitude() ?: 0.0,gpstracker?.getLongitude() ?: 0.0),mActivity.resources.getString(R.string.back_home),false)
                }
            })
        }else{
            Utils.showNoInternetMessage(mActivity)
            setupGoogleMap(LatLng(gpstracker?.getLatitude() ?: 0.0,gpstracker?.getLongitude() ?: 0.0),mActivity.resources.getString(R.string.back_home),false)
        }
    }

    private fun setBounds(isAdding: Boolean) {
        var count = 0
        if (savePlacesToVisitList.size > 0) {
            for (i in 0 until savePlacesToVisitList.size) {
                if (savePlacesToVisitList.size > 1){
                    val distance: Double = distance(savePlacesToVisitList[0].latitude ?: 0.0,savePlacesToVisitList[0].longitude ?: 0.0,savePlacesToVisitList[i].latitude ?: 0.0,savePlacesToVisitList[i].longitude ?: 0.0)
                    if (distance <= distanceLocationMeter){
                        count += 1
                    }
                }
            }
            when {
                savePlacesToVisitList.size == 1 -> gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(savePlacesToVisitList[0].latitude ?: 0.0,savePlacesToVisitList[0].longitude ?: 0.0),
                    NEAR_ZOOM_VALUE
                ))
                isAdding -> gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(savePlacesToVisitList[savePlacesToVisitList.size - 1].latitude ?: 0.0,savePlacesToVisitList[savePlacesToVisitList.size - 1].longitude ?: 0.0),
                    NEAR_ZOOM_VALUE
                ))
                count == savePlacesToVisitList.size -> gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(savePlacesToVisitList[0].latitude ?: 0.0,savePlacesToVisitList[0].longitude ?: 0.0),
                    ZOOM_VALUE
                ))
                else -> gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 250))
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

    private fun showNumberPickerDialog(
        activity: Activity,
        position: Int,
        placesToVisitModel: PlacesResult
    ) {
        val inflater = activity.layoutInflater
        val dialogLayout = inflater.inflate(R.layout.raw_place_date, null)

        val btnSet = dialogLayout.findViewById<Button>(R.id.btnSetFreq)
        val tvPlaceDate = dialogLayout.findViewById<TextView>(R.id.tvPlaceDate)
        val ivClosePopUp = dialogLayout.findViewById<ImageView>(R.id.iv_popup_dismiss)

        val mDialog = AlertDialog.Builder(activity)
        mDialog.setView(dialogLayout)
        val frequencyDialog = mDialog.create()

        if (placesToVisitModel.visitDate!=""){
            val formatter = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
            val target = SimpleDateFormat(SHOW_DATE_TIME)
            val target2 = SimpleDateFormat(CHECK_DATE_TIME2)
            var diagStartDate = ""
            var diagStartDate2 = ""
            try {
                var date1: Date? = null
                if (placesToVisitModel.visitDate != null) {
                    date1 = formatter.parse(placesToVisitModel.visitDate ?: "")
                }
                if (date1 != null) {
                    diagStartDate = target.format(date1)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                var date1: Date? = null
                if (placesToVisitModel.visitDate != null) {
                    date1 = formatter.parse(placesToVisitModel.visitDate ?: "")
                }
                if (date1 != null) {
                    diagStartDate2 = target2.format(date1)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            tvPlaceDate.text = if (diagStartDate!="")diagStartDate else diagStartDate2
        }
        tvPlaceDate.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            setDateTimeField(tvPlaceDate)
        }
        btnSet.setOnClickListener {
            mActivity.hideKeyboard()
            when {
                tvPlaceDate.text.toString().trim().isEmpty() -> {
                    mActivity.showMessage(mActivity.resources.getString(R.string.blank_place_date))
                }
                else -> {
                    Comman_Methods.avoidDoubleClicks(it)
                    val formatter = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
                    val target = SimpleDateFormat(SHOW_DATE_TIME)
                    var diagStartDate = ""
                    try {
                        var date1: Date? = null
                        if (tvPlaceDate.text.toString() != "") {
                            date1 = target.parse(tvPlaceDate.text.toString())
                        }
                        if (date1 != null) {
                            diagStartDate = formatter.format(date1)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    placesToVisitModel.visitDate = diagStartDate
                    placesToVisitModel.userId = loginObject.memberID
                    val jsonObject = JsonObject()
                    jsonObject.addProperty("ID",placesToVisitModel.iD)
                    jsonObject.addProperty("UserId",loginObject.memberID)
                    jsonObject.addProperty("Latitude",placesToVisitModel.latitude)
                    jsonObject.addProperty("Longitude",placesToVisitModel.longitude)
                    jsonObject.addProperty("Address",placesToVisitModel.address)
                    jsonObject.addProperty("VisitDate",placesToVisitModel.visitDate)
                    callAddUpdatePlaces(jsonObject)
                    savePlacesToVisitList[position] = placesToVisitModel
                    frequencyDialog.dismiss()
                }
            }
        }
        ivClosePopUp.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            frequencyDialog.dismiss()
        }
        frequencyDialog.setCancelable(false)
        frequencyDialog.show()
    }

    private fun showRatingPlaceDialog(activity: Activity,
                                      position: Int,
                                      placesToVisitModel: PlacesResult
    ){
        val inflater = activity.layoutInflater
        val dialogLayout = inflater.inflate(R.layout.place_rating_layout, null)

        val tvRatingPlaceName = dialogLayout.findViewById<TextView>(R.id.tvRatingPlaceName)
        val tvRatingCancel = dialogLayout.findViewById<TextView>(R.id.tvRatingCancel)
        val tvRatingOk = dialogLayout.findViewById<TextView>(R.id.tvRatingOk)
        val rbRatingPlace = dialogLayout.findViewById<RatingBar>(R.id.rbRatingPlace)

        val mDialog = AlertDialog.Builder(activity)
        mDialog.setView(dialogLayout)
        val ratingDialog = mDialog.create()

        tvRatingPlaceName.text = placesToVisitModel.address
        rbRatingPlace.rating = placesToVisitModel.rating?.toFloat() ?: 0F
        tvRatingCancel.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            ratingDialog.dismiss()
        }

        tvRatingOk.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            if (placesToVisitModel.rating != rbRatingPlace.rating.toDouble()) {
                placesToVisitModel.rating = rbRatingPlace.rating.toDouble()
                val jsonObject = JsonObject()
                jsonObject.addProperty("PlaceId", placesToVisitModel.iD)
                jsonObject.addProperty("Rating", rbRatingPlace.rating)
                callRatePlaceApi(jsonObject)
                savePlacesToVisitList[position] = placesToVisitModel
            }
            ratingDialog.dismiss()
        }

        ratingDialog.setCancelable(false)
        ratingDialog.show()
    }

    private fun callRatePlaceApi(jsonObject: JsonObject) {
        if (ConnectionUtil.isInternetAvailable(mActivity)){
            Comman_Methods.isProgressShow(mActivity)
            mActivity.isSpeedAvailable()
            val callRatePlace = WebApiClient.getInstance(mActivity).webApi_without?.rateToPlace(jsonObject)
            callRatePlace?.enqueue(object : retrofit2.Callback<ApiResponse>{
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                if (it.status) {
                                    callGetPlaces(false)
                                }
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        Utils.showSomeThingWrongMessage(mActivity)
                    }
                }
            })
        }else{
            Utils.showNoInternetMessage(mActivity)
        }
    }

    private fun setDateTimeField(et_date: TextView) {
        mActivity.hideKeyboard()
        val dateFormatter = SimpleDateFormat(OUTED_DATE, Locale.US)
        val newCalendar = Calendar.getInstance()
        if (et_date.text.toString().trim()!=""){
            try {
                newCalendar.time = dateFormatter.parse(et_date.text.toString().trim())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val datePickerDialog = DatePickerDialog(
            mActivity, { view, year, monthOfYear, dayOfMonth ->
                val newDate = Calendar.getInstance()
                newDate.set(year, monthOfYear, dayOfMonth)
//                et_date.text = dateFormatter.format(newDate.time)
                val dateString = dateFormatter.format(newDate.time)
                setTimeField(et_date, dateString)
            }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.minDate = Date().time
        datePickerDialog.show()
    }

    private fun setTimeField(et_date: TextView, dateString: String) {
        var timeString = ""
        mActivity.hideKeyboard()
        val dateFormatter = SimpleDateFormat(TIME_FORMAT, Locale.US)
        val newCalendar = Calendar.getInstance()
        if (et_date.text.toString().trim() != "") {
            try {
                newCalendar.time = dateFormatter.parse(et_date.text.toString().trim())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val timePickerDialog = TimePickerDialog(
            mActivity, { view, selectedHour, selectedMinute ->
                val newDate = Calendar.getInstance()
                newDate.set(0, 0, 0, selectedHour, selectedMinute)
                timeString = dateFormatter.format(newDate.time)
                et_date.text = "$dateString $timeString"
            }, newCalendar.get(Calendar.HOUR_OF_DAY), newCalendar.get(Calendar.MINUTE), false
        )
        timePickerDialog.setButton(
            DialogInterface.BUTTON_NEGATIVE, mActivity.resources.getString(R.string.cancel)
        ) { dialog, which -> timePickerDialog.dismiss() }
        timePickerDialog.show()
    }

    override fun onMapReady(p0: GoogleMap) {
        gMap = p0
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
        gMap.uiSettings.isMapToolbarEnabled = false
        gMap.uiSettings.isZoomControlsEnabled = false
        gMap.setInfoWindowAdapter(CustomInfoWindowAdapter())
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
        KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
            .permissions(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION)
            .onAccepted { permissions ->
                if (permissions.size == 2) {
                    if (ContextCompat.checkSelfPermission(
                            mActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                            mActivity,
                            Manifest.permission.ACCESS_COARSE_LOCATION
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

    @Synchronized
    protected fun buildGoogleApiClient() {
        mGoogleApiClient = GoogleApiClient.Builder(mActivity)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build()
        mGoogleApiClient?.connect()
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

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            setHeader()
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
                            try {
                                for (i in 0 until yelpBusinessList.size){
                                    if (hashMapMarkerList.size > 0){
                                        if (hashMapMarkerList[i][yelpBusinessList[i].id]!=null) {
                                            val marker = hashMapMarkerList[i][yelpBusinessList[i].id]
                                            marker?.remove()
                                        }
                                    }
                                }
                            }catch (e: java.lang.Exception){
                                e.printStackTrace()
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

    fun changeFilterDesign(){
        if (rvMapPinFilter!=null) {
            mapPinAdapter = MapPinAdapter(mActivity, MapFilter.getMapFilter)
            rvMapPinFilter.adapter = mapPinAdapter
            mapPinAdapter.notifyDataSetChanged()
            ivCancelFilter.visibility=View.GONE
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
                    addYelpMarker(yelpBusinessList, filterId)
                }else{
                    changeFilterDesign()
                    mActivity.showMessage(mActivity.resources.getString(R.string.str_no_map_filter,filterName))
                }
                yelpTotal = total
                yelpRegion = region
            }
        })
    }

    fun addYelpMarker(
        yelpBusinessListing: ArrayList<Business>,
        filterId: Int
    ) {
        val builder = LatLngBounds.Builder()
        for (i in 0 until yelpBusinessListing.size) {
            if (yelpBusinessListing[i].isClosed == false) {
                var yelpLatLng: LatLng? = null
                val coLatitude = yelpBusinessListing[i].coordinates?.latitude ?: 0.0
                val coLongitude = yelpBusinessListing[i].coordinates?.longitude ?: 0.0
                if (coLatitude != 0.0 && coLongitude != 0.0) {
                    yelpLatLng = LatLng(coLatitude, coLongitude)
                }else{
                    yelpBusinessListing[i].location?.let {
                        it.displayAddress?.let { display ->
                            var address = ""
                            for (k in 0 until display.size) {
                                val displayAddress = display[k]
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

                yelpLatLng?.let {
                    try {
                        val markerOptions4 = MarkerOptions().position(it)
                            .title(yelpBusinessListing[i].id ?: "")
                            .snippet("filter")
                            .icon(filterIcon)
                        val hashMapMarker = HashMap<String, Marker>()
                        marker2 = gMap.addMarker(markerOptions4)!!
                        hashMapMarker[yelpBusinessListing[i].id ?: ""] = marker2
                        hashMapMarkerList.add(hashMapMarker)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    builder.include(it)
                    try {
                        val filterBounds = builder.build()
                        gMap.moveCamera(
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
    }

    fun showCallMessageDialog(business: Business){
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.raw_call_message, null)
        val mDialog = android.app.AlertDialog.Builder(mActivity)
        mDialog.setView(dialogLayout)
        callMessageDialog = mDialog.create()
        callMessageDialog.window?.attributes?.windowAnimations = R.style.animationForDialog
        callMessageDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        if (business.phone!=null) {
            if (business.phone != "") {
                dialogLayout.tvCall.visibility = View.VISIBLE
                dialogLayout.tvSms.visibility = View.VISIBLE
            }else{
                dialogLayout.tvCall.visibility = View.GONE
                dialogLayout.tvSms.visibility = View.GONE
            }
        }else{
            dialogLayout.tvCall.visibility = View.GONE
            dialogLayout.tvSms.visibility = View.GONE
        }
        dialogLayout.tvDirection.visibility = View.GONE
        val coOrdinateLat = business.coordinates?.latitude ?: 0.0
        val coOrdinateLng = business.coordinates?.longitude ?: 0.0
        if (coOrdinateLat != 0.0 && coOrdinateLng != 0.0) {
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

    private inner class CustomInfoWindowAdapter : GoogleMap.InfoWindowAdapter {

        private var infoView: View = mActivity.layoutInflater.inflate(R.layout.popup_livemember_marker, null)

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
            gMap.uiSettings.isMapToolbarEnabled = false
            gMap.uiSettings.isZoomControlsEnabled = false
            if (sheetBehavior!=null) {
                if (sheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
                    sheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
            if (marker.snippet=="filter"){
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
                        val yelpDistance = yelpBusinessObject.distance ?: 0.0
                        tvYelpPlaceDistance.text =
                            if (yelpDistance > 0.0) DecimalFormat("##.##", decimalSymbols).format(yelpDistance / meterToMiles).toString() + " mi" else "- mi"
                        if (yelpBusinessObject.phone != "") {
                            tvYelpPlacePhone.visibility = View.VISIBLE
                            tvPlacePhone.visibility = View.VISIBLE
                            tvYelpPlacePhone.text = yelpBusinessObject.phone
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
                        val reviewCount = yelpBusinessObject.reviewCount ?: 0
                        tvYelpPlaceReview.text = "($reviewCount)"
                        tvPlaceCategory.text = mActivity.resources.getString(R.string.str_category) + ":- "
                        tvYelpPlaceAddress.text = ""
                        val locAddress = yelpBusinessObject.location?.displayAddress ?: ArrayList()
                        for (k in 0 until locAddress.size) {
                            val placeAddress = tvYelpPlaceAddress.text.toString().trim()
                            val displayAddress = locAddress[k]
                            if (k != 0) {
                                tvYelpPlaceAddress.text = "$placeAddress, $displayAddress"
                            } else {
                                tvYelpPlaceAddress.text = displayAddress
                            }
                        }
                        tvYelpPlaceCategory.text = ""
                        val yelpCategory = yelpBusinessObject.categories ?: ArrayList()
                        for (k in 0 until yelpCategory.size) {
                            val placeCategory = tvYelpPlaceCategory.text.toString().trim()
                            val displayAddress = yelpCategory[k].title ?: ""
                            if (k != 0) {
                                tvYelpPlaceCategory.text = "$placeCategory, $displayAddress"
                            } else {
                                tvYelpPlaceCategory.text = displayAddress
                            }
                        }
                    }
                }
            }
            return infoView
        }
    }
}