package com.keepSafe911.fragments.homefragment.find

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.gps.GpsTracker
import com.keepSafe911.model.Loc
import com.keepSafe911.model.MemberBean
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.room.OldMe911Database
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
import com.keepSafe911.BuildConfig
import com.kotlinpermissions.KotlinPermissions
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.model.FamilyMonitorResult
import com.keepSafe911.model.ResultRoute
import com.keepSafe911.utils.*
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_member_route.*
import kotlinx.android.synthetic.main.toolbar_header.*
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class MemberRouteFragment : HomeBaseFragment(), OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private var mGoogleApiClient: GoogleApiClient? = null
    private var mLocationRequest: LocationRequest? = null
    lateinit var map: GoogleMap
    internal var circles = ArrayList<Loc>()
    private var dateFormatter: SimpleDateFormat? = null
    private lateinit var datePickerDialog: DatePickerDialog
    private var dateparse: Date? = null
    private var memRouteName: String = ""
    lateinit var latlng: LatLng
    var routeArray: ArrayList<LatLng> = ArrayList()
    var duplicateRouteArray: ArrayList<LatLng> = ArrayList()
    var MemberID: Int = 0
    var memberList: ArrayList<MemberBean> = ArrayList()
    lateinit var appDatabase: OldMe911Database
    lateinit var bottomSheetDialog: BottomSheetDialog
    private var gpstracker: GpsTracker? = null
    private var settingDialogShown: Boolean = false


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
        return inflater.inflate(R.layout.fragment_member_route, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appDatabase = OldMe911Database.getDatabase(mActivity)
        gpstracker = GpsTracker(mActivity)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)


        val loginObject = appDatabase.loginDao().getAll()


        if (loginObject.isAdmin) {
            spinnerMember.visibility = View.VISIBLE
            callGetMember()
        } else {
            MemberID = loginObject.memberID
            spinnerMember.visibility = View.GONE
        }
        setHeader(loginObject)

        if (checkGooglePlayServices()) {
            buildGoogleApiClient()
        }
        et_routedate.setOnClickListener {
            Comman_Methods.avoidDoubleClicks(it)
            setDateTimeField(et_routedate)
        }
        map_memberRoute.onCreate(savedInstanceState)
        map_memberRoute.onResume()
        MapsInitializer.initialize(mActivity)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            settingDialogShown = false
            checkLocationPermission()
        }else{
            if (gpstracker?.CheckForLoCation() == false) {
                Utils.showLocationSettingsAlert(mActivity)
            }else {
                if (mGoogleApiClient == null) {
                    buildGoogleApiClient()
                }
            }
        }
        if ((mGoogleApiClient?.isConnecting == false) && (mGoogleApiClient?.isConnected == false)) {
            mGoogleApiClient?.connect()
        }
        map_memberRoute.getMapAsync(this)
    }


    private fun setHeader(loginObject: LoginObject) {

        tvHeader.text = mActivity.resources.getString(R.string.where_have_been)
        btnGO.visibility = View.VISIBLE
        iv_back.visibility = View.VISIBLE
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
        btnGO.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            circles.clear()
            map.clear()
            when {
                loginObject.isAdmin -> when {
                    spinnerMember.selectedItemPosition == 0 -> mActivity.showMessage(mActivity.resources.getString(R.string.selectWorker))
                    et_routedate.text.isEmpty() -> mActivity.showMessage(mActivity.resources.getString(R.string.please_select_date))
                    else -> callMemberRouteApi(MemberID, et_routedate.text.toString().trim())
                }
                et_routedate.text.isEmpty() -> mActivity.showMessage(mActivity.resources.getString(R.string.please_select_date))
                else -> callMemberRouteApi(MemberID, et_routedate.text.toString().trim())
            }
        }
        mActivity.checkUserActive()
    }

    private fun callGetMember() {
        memberList = ArrayList()
        val defaultmemberBean = MemberBean()
        defaultmemberBean.id = -1
        defaultmemberBean.memberName = mActivity.resources.getString(R.string.select_user)
        memberList.add(defaultmemberBean)
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            mActivity.isSpeedAvailable()
            Utils.familyMonitoringUserList(mActivity, object : CommonApiListener {
                override fun familyUserList(
                    status: Boolean,
                    userList: ArrayList<FamilyMonitorResult>,
                    message: String
                ) {
                    if (status) {
                        if (userList.isEmpty()) {
                            mActivity.showMessage(mActivity.resources.getString(R.string.no_data))
                        }
                        appDatabase.memberDao().dropTable()
                        appDatabase.memberDao().addAllMember(userList)
                        setData()
                    } else {
                        mActivity.showMessage(message)
                    }
                }

                override fun onFailureResult() {
                    mActivity.showMessage(mActivity.resources.getString(R.string.error_message))
                }
            })
        } else {
            setData()
        }
    }

    private fun setData() {
        memberList = ArrayList()
        for (i: Int in appDatabase.memberDao().getAllMember().indices) {
            val memberBean = MemberBean()
            memberBean.id = appDatabase.memberDao().getAllMember()[i].iD
            memberBean.memberName =
                appDatabase.memberDao().getAllMember()[i].firstName + " " + appDatabase.memberDao().getAllMember()[i].lastName
            memberBean.memberEmail = appDatabase.memberDao().getAllMember()[i].email
            memberBean.memberImage = appDatabase.memberDao().getAllMember()[i].image
            memberList.add(memberBean)
        }
        if (memberList.size > 0) {
            val memberAdapter = MemberAdapter(
                mActivity,
                memberList
            )
            spinnerMember.adapter = memberAdapter
        }
        spinnerMember.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                circles.clear()
                MemberID = memberList[position].id ?: 0
            }

        }
    }

    private fun callMemberRouteApi(memberID: Int, anniversaryDate: String) {
        mActivity.isSpeedAvailable()
        Utils.memberRouteListApi(mActivity, memberID, anniversaryDate, object : CommonApiListener {
            override fun memberRouteResponse(
                status: Boolean,
                resultRoute: ArrayList<ResultRoute>,
                message: String,
                responseMessage: String
            ) {
                if (status) {
                    var count = 0
                    var loc: Loc = Loc()
                    val builder = LatLngBounds.Builder()
                    val different: ArrayList<ArrayList<LatLng>> = ArrayList()
                    if (resultRoute.size > 0) {
                        for (i: Int in 0 until resultRoute.size) {
                            memRouteName = resultRoute[i].name ?: ""
                            val routeLoc = resultRoute[i].locs ?: ArrayList()
                            if (routeLoc.size > 0) {
                                for (j: Int in 0 until routeLoc.size) {
                                    loc = routeLoc[j]
                                    val locLat = loc.lat ?: 0.0
                                    val locLng = loc.lng ?: 0.0
                                    latlng = LatLng(locLat, locLng)
                                    routeArray.add(latlng)
                                    if (locLat != 0.0 && locLng != 0.0) {
                                        duplicateRouteArray.add(latlng)
                                        builder.include(latlng)
                                        if (loc.recordStatus == "3" || j == resultRoute[i].locs!!.size - 1) {
                                            different.add(duplicateRouteArray)
                                            duplicateRouteArray = ArrayList()
                                        }
                                        try {
                                            when (loc.recordStatus) {
                                                LOGIN_RECORD_STATUS.toString() -> map.addMarker(
                                                    MarkerOptions().position(latlng)
                                                        .position(routeArray[j])
                                                        .icon(
                                                            BitmapDescriptorFactory.defaultMarker(
                                                                BitmapDescriptorFactory.HUE_GREEN
                                                            )
                                                        )
                                                        .snippet(count.toString())
                                                )
                                                PIN_RECORD_STATUS.toString() -> if (!(loc.info ?: "").contains(
                                                        "00:00:00"
                                                    )
                                                ) {
                                                    map.addMarker(
                                                        MarkerOptions().position(
                                                            latlng
                                                        )
                                                            .position(routeArray[j])
                                                            .icon(
                                                                BitmapDescriptorFactory.defaultMarker(
                                                                    BitmapDescriptorFactory.HUE_YELLOW
                                                                )
                                                            )
                                                            .snippet(count.toString())
                                                    )
                                                }
                                                else -> map.addMarker(
                                                    MarkerOptions().position(latlng)
                                                        .position(routeArray[j])
                                                        .icon(
                                                            BitmapDescriptorFactory.defaultMarker(
                                                                BitmapDescriptorFactory.HUE_RED
                                                            )
                                                        )
                                                        .snippet(count.toString())
                                                )
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    val year: String =
                                        (loc.startDate ?: "").substring(0, 4)
                                    val date: String =
                                        (loc.startDate ?: "").substring(8, 10)
                                    val month: String =
                                        (loc.startDate ?: "").substring(5, 7)
                                    val finalDate: String = "$month/$date/$year"

                                    val loca = Loc()
                                    loca.adr = loc.adr
                                    loca.info = loc.info
                                    loca.startDate = finalDate

                                    circles.add(loc)
                                    count++
                                }
                                print("!@@@@@@@LatLng$different")
                            } else {
                                mActivity.showMessage(mActivity.resources.getString(R.string.no_route_date))
                            }
                        }
                        if (loc != null) {
                            DrawArrowHead(map, different)
                            // mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());
                            try {
                                val bounds = builder.build()
                                map.animateCamera(
                                    CameraUpdateFactory.newLatLngBounds(
                                        bounds,
                                        100
                                    )
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            resultRoute.clear()
                            routeArray.clear()
                            different.clear()
                        } else {
                            mActivity.showMessage(mActivity.resources.getString(R.string.no_route_date))
                        }
                    } else {
                        mActivity.showMessage(mActivity.resources.getString(R.string.no_route_date))
                    }
                }
            }
        })
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
    protected fun buildGoogleApiClient() {
        mGoogleApiClient = GoogleApiClient.Builder(mActivity)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build()
        mGoogleApiClient?.connect()
    }

    private fun checkLocationPermission() {
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
                .permissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
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
    }

    private fun setDateTimeField(et_date: EditText) {
        mActivity.hideKeyboard()
        dateFormatter = SimpleDateFormat(OUTED_DATE, Locale.US)
        val newCalendar = Calendar.getInstance()
        datePickerDialog = DatePickerDialog(
            mActivity,
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                val newDate = Calendar.getInstance()
                if (et_date.text.toString().trim()!=""){
                    try {
                        newCalendar.time = dateFormatter?.parse(et_date.text.toString().trim())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                newDate.set(year, monthOfYear, dayOfMonth)
                et_date.setText(dateFormatter?.format(newDate.time))
                try {
                    dateparse = dateFormatter?.parse(dateFormatter?.format(newDate.time))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.maxDate = Date().time
        datePickerDialog.show()
    }


    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            mActivity.hideKeyboard()
            /*if (appDatabase.loginDao().getAll().isAdmin) {
                mActivity.checkNavigationItem(3)
            } else {
                mActivity.checkNavigationItem(2)
            }*/
            mActivity.enableDrawer()
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
        }
    }

    private fun DrawArrowHead(mMap: GoogleMap, latlng: ArrayList<ArrayList<LatLng>>) {

        for (j in 0 until latlng.size) {
            for (i in 0 until latlng[j].size - 1) {
                val polylines = PolylineOptions()
                polylines.add(latlng[j][i], latlng[j][i + 1]).width(3f).color(Color.BLACK)
                mMap.addPolyline(polylines)
                val bearing = GetBearing(latlng[j][i], latlng[j][i + 1])

                // round it to a multiple of 3 and cast out 120s
                var adjBearing = (Math.round(bearing / 3) * 3).toDouble()
                while (adjBearing >= 120) {
                    adjBearing -= 120.0
                }

                val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
                StrictMode.setThreadPolicy(policy)

                // Get the corresponding triangle marker from Google
                val url: URL
                var image: Bitmap? = null

                try {
                    url =
                        URL("http://www.google.com/intl/en_ALL/mapfiles/dir_" + adjBearing.toInt().toString() + ".png")
                    try {
                        image = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (image != null) {

                    // Anchor is ratio in range [0..1] so value of 0.5 on x and y will center the marker image on the lat/long
                    val anchorX = 0.5f
                    val anchorY = 0.5f

                    var offsetX = 0
                    var offsetY = 0

                    // images are 24px x 24px
                    // so transformed image will be 48px x 48px

                    //315 range -- 22.5 either side of 315
                    if (bearing >= 292.5 && bearing < 335.5) {
                        offsetX = 24
                        offsetY = 24
                    } else if (bearing >= 247.5 && bearing < 292.5) {
                        offsetX = 24
                        offsetY = 12
                    } else if (bearing >= 202.5 && bearing < 247.5) {
                        offsetX = 24
                        offsetY = 0
                    } else if (bearing >= 157.5 && bearing < 202.5) {
                        offsetX = 12
                        offsetY = 0
                    } else if (bearing >= 112.5 && bearing < 157.5) {
                        offsetX = 0
                        offsetY = 0
                    } else if (bearing >= 67.5 && bearing < 112.5) {
                        offsetX = 0
                        offsetY = 12
                    } else if (bearing >= 22.5 && bearing < 67.5) {
                        offsetX = 0
                        offsetY = 24
                    } else {
                        offsetX = 12
                        offsetY = 24
                    }//0 range - 335.5 - 22.5
                    //45 range
                    //90 range
                    //135 range
                    //180 range
                    //225 range
                    //270 range

                    val wideBmp: Bitmap = Bitmap.createBitmap(image.width * 2, image.height * 2, image.config)
                    val wideBmpCanvas: Canvas
                    val src: Rect = Rect(0, 0, image.width, image.height)
                    val dest: Rect

                    // Create larger bitmap 4 times the size of arrow head image

                    wideBmpCanvas = Canvas(wideBmp)

                    dest = Rect(src)
                    dest.offset(offsetX, offsetY)

                    wideBmpCanvas.drawBitmap(image, src, dest, null)

                    val arrow = -1
                    try {
                        mMap.addMarker(
                            MarkerOptions()
                                .position(latlng[j][i + 1])
                                .flat(true)
                                .snippet("" + arrow)
                                .icon(BitmapDescriptorFactory.fromBitmap(wideBmp))
                                .anchor(anchorX, anchorY)
                        )
                    }catch (e: Exception){
                        e.printStackTrace()
                    }
                }
            }
        }
        // obtain the bearing between the last two points
    }

    private fun GetBearing(from: LatLng, to: LatLng): Double {
        val lat1 = from.latitude * Math.PI / 180.0
        val lon1 = from.longitude * Math.PI / 180.0
        val lat2 = to.latitude * Math.PI / 180.0
        val lon2 = to.longitude * Math.PI / 180.0

        // Compute the angle.
        var angle = -Math.atan2(
            Math.sin(lon1 - lon2) * Math.cos(lat2),
            Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2)
        )

        if (angle < 0.0)
            angle += Math.PI * 2.0

        // And convert result to degrees.
        angle *= (180.0 / Math.PI)

        return angle
    }


    internal class MemberAdapter(private val activity: Activity, private val list: ArrayList<MemberBean>) :
        BaseAdapter() {

        override fun getCount(): Int {
            return list.size
        }

        override fun getItem(i: Int): Any {
            return list[i]
        }

        override fun getItemId(i: Int): Long {
            return 0
        }

        override fun getView(position: Int, view: View?, viewGroup: ViewGroup): View {
            var view = view
            val holder: ViewHolder
            var bean = MemberBean()
            bean = list[position]

            if (view == null) {
                val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                view = inflater.inflate(android.R.layout.simple_list_item_1, null)

                holder = ViewHolder()
                view!!.tag = holder
            } else {
                holder = view.tag as ViewHolder
            }

            holder.tv_membername = view.findViewById(android.R.id.text1)
            holder.tv_membername?.isSingleLine = true
            holder.tv_membername?.textSize = 15f
            holder.tv_membername?.text = bean.memberName

            return view
        }

        inner class ViewHolder {
            internal var tv_membername: TextView? = null
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
                // mMap.setMyLocationEnabled(true);

                if (map_memberRoute != null && map_memberRoute.findViewById<View>(Integer.parseInt("1")) != null) {
                    // Get the button view
                    val locationButton =
                        (map_memberRoute.findViewById<View>(Integer.parseInt("1")).parent as View).findViewById<View>(
                            Integer.parseInt("2")
                        )
                    // and next place it, on bottom right (as Google Maps app)
                    val layoutParams = locationButton.layoutParams as RelativeLayout.LayoutParams
                    // position on right bottom
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                    layoutParams.setMargins(0, 0, 30, 30)

                }
            }
        } else {
            buildGoogleApiClient()
            // mMap.setMyLocationEnabled(true);

            if (map_memberRoute != null && map_memberRoute.findViewById<View>(Integer.parseInt("1")) != null) {
                // Get the button view
                val locationButton =
                    (map_memberRoute.findViewById<View>(Integer.parseInt("1")).parent as View).findViewById<View>(
                        Integer.parseInt("2")
                    )
                // and next place it, on bottom right (as Google Maps app)
                val layoutParams = locationButton.layoutParams as RelativeLayout.LayoutParams
                // position on right bottom
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                layoutParams.setMargins(0, 0, 30, 30)

            }
        }
        map.uiSettings.isMapToolbarEnabled = false
        map.uiSettings.isZoomControlsEnabled = false
        map.setOnMarkerClickListener(GoogleMap.OnMarkerClickListener { marker ->
            try {
                if (Integer.parseInt(marker.snippet) >= 0) {
                    val view = LayoutInflater.from(mActivity)
                        .inflate(R.layout.member_route_detail, mActivity.window.decorView.rootView as ViewGroup, false)
                    bottomSheetDialog = BottomSheetDialog(mActivity)
                    bottomSheetDialog.setContentView(view)
                    val mBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(view.parent as View)
                    mBehavior.isHideable = false
                    bottomSheetDialog.setOnShowListener {
                        mBehavior.peekHeight = view.height
                    }
                    val loc = circles[Integer.parseInt(marker.snippet)]
                    val formatter = SimpleDateFormat(DELIVER_DATE_FORMAT)
                    val formatter1 = SimpleDateFormat(INPUT_DATE_FORMAT)
                    val target = SimpleDateFormat(OUTED_DATE)
                    var diagStartDate = ""
                    try {
                        val date1: Date? = formatter.parse(loc.startDate ?: "")
                        if (date1 != null) {
                            diagStartDate = target.format(date1)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    try {
                        val date1: Date? = formatter1.parse(loc.startDate ?: "")
                        if (date1 != null) {
                            diagStartDate = target.format(date1)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val position_date: TextView? = bottomSheetDialog.findViewById(R.id.position_date)
                    val member_name: TextView? = bottomSheetDialog.findViewById(R.id.member_name)
                    val position_address: TextView? = bottomSheetDialog.findViewById(R.id.position_address)
                    val tvPositionAddress: TextView? = bottomSheetDialog.findViewById(R.id.tvPositionAddress)
                    val position_time_difference: TextView? =
                        bottomSheetDialog.findViewById(R.id.position_time_difference)
                    val tvPositionTime: TextView? = bottomSheetDialog.findViewById(R.id.tvPositionTime)

                    position_date?.text = diagStartDate
                    member_name?.text = memRouteName
                    val locAddress = loc.adr ?: ""
                    val locInfo = loc.info ?: ""
                    if (locAddress.trim().isNotEmpty()) {
                        position_address?.visibility = View.GONE
                        tvPositionAddress?.visibility = View.GONE
                    } else {
                        position_address?.visibility = View.VISIBLE
                        tvPositionAddress?.visibility = View.VISIBLE
                        position_address?.text = locAddress
                    }
                    if (locInfo.trim().isNotEmpty()) {
                        position_time_difference?.visibility = View.GONE
                        tvPositionTime?.visibility = View.GONE
                    } else {
                        position_time_difference?.visibility = View.VISIBLE
                        tvPositionTime?.visibility = View.VISIBLE
                        position_time_difference?.text = locInfo
                    }
                    bottomSheetDialog.show()
                }
                return@OnMarkerClickListener true
            } catch (e: Exception) {
                Log.e("MarkerClicked", e.toString())
                return@OnMarkerClickListener false
            }
        })
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

}
