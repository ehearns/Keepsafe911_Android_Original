package com.keepSafe911.fragments.homefragment.report


import android.Manifest
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.gps.GpsTracker
import com.keepSafe911.model.Loc
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
import com.keepSafe911.utils.*
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_report_map.*
import kotlinx.android.synthetic.main.toolbar_header.*
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ReportMapFragment : HomeBaseFragment(), OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private var mGoogleApiClient: GoogleApiClient? = null
    private var mLocationRequest: LocationRequest? = null
    lateinit var map: GoogleMap
    private var gpstracker: GpsTracker? = null


    private var circles = ArrayList<Loc>()
    private var circlesFrom = ArrayList<Loc>()
    private var memRouteName: String? = ""
    lateinit var latlng: LatLng
    private var routeArray: ArrayList<LatLng> = ArrayList()
    private var duplicateRouteArray: ArrayList<LatLng> = ArrayList()
    lateinit var bottomSheetDialog: BottomSheetDialog


    companion object{
        fun newInstance(
            memberName:String,
            loc: ArrayList<Loc>
        ): ReportMapFragment {
            val args = Bundle()
            args.putString(ARG_PARAM1,memberName)
            args.putParcelableArrayList(ARG_PARAM2,loc)
            val fragment = ReportMapFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            memRouteName = it.getString(ARG_PARAM1, "")
            circlesFrom = it.getParcelableArrayList(ARG_PARAM2) ?: ArrayList()
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
        return inflater.inflate(R.layout.fragment_report_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHeader()
        gpstracker = GpsTracker(mActivity)
        if (checkGooglePlayServices()) {
            buildGoogleApiClient()
        }
        mvBusinessReport.onCreate(savedInstanceState)
        mvBusinessReport.onResume()
        MapsInitializer.initialize(mActivity)
        if ((mGoogleApiClient?.isConnecting == false) && (mGoogleApiClient?.isConnected == false)) {
            mGoogleApiClient?.connect()
        }
        mvBusinessReport.getMapAsync(this)
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
    }

    private fun setHeader() {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        mActivity.disableDrawer()
        tvHeader.setPadding(15, 0, 0, 0)
        tvHeader.text = memRouteName
        Utils.setTextGradientColor(tvHeader)
        iv_close.visibility = View.VISIBLE
        iv_close.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
        mActivity.checkUserActive()
    }

    private fun checkLocationPermission() {
        KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
            .permissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
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


    private fun callViewReport() {
        var loc: Loc = Loc()
        val builder = LatLngBounds.Builder()
        val different: ArrayList<ArrayList<LatLng>> = ArrayList()
        for ((count, j: Int) in (0 until circlesFrom.size).withIndex()) {
            loc = circlesFrom[j]
            val locLat = loc.lat ?: 0.0
            val locLng = loc.lng ?: 0.0
            latlng = LatLng(locLat, locLng)
            routeArray.add(latlng)
            if (locLat != 0.0 && locLng != 0.0) {
                duplicateRouteArray.add(latlng)
                builder.include(latlng)
                if (loc.recordStatus == "3" || j == circlesFrom.size - 1) {
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
                                MarkerOptions().position(latlng)
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
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
            val year: String = (loc.startDate ?: "").substring(0, 4)
            val date: String = (loc.startDate ?: "").substring(8, 10)
            val month: String = (loc.startDate ?: "").substring(5, 7)
            val finalDate: String = "$month/$date/$year"

            val loca = Loc()
            loca.adr = loc.adr
            loca.info = loc.info
            loca.startDate = finalDate

            circles.add(loc)
        }
        if (loc != null) {
            drawArrowHead(map, different)
            try {
                val bounds = builder.build()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            routeArray.clear()
            different.clear()
        } else {
            mActivity.showMessage(mActivity.resources.getString(R.string.no_route_date))
        }
    }


    private fun drawArrowHead(mMap: GoogleMap, latlng: ArrayList<ArrayList<LatLng>>) {

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

                if (mvBusinessReport != null && mvBusinessReport.findViewById<View>(Integer.parseInt("1")) != null) {
                    // Get the button view
                    val locationButton =
                        (mvBusinessReport.findViewById<View>(Integer.parseInt("1")).parent as View).findViewById<View>(
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

            if (mvBusinessReport != null && mvBusinessReport.findViewById<View>(Integer.parseInt("1")) != null) {
                // Get the button view
                val locationButton =
                    (mvBusinessReport.findViewById<View>(Integer.parseInt("1")).parent as View).findViewById<View>(
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
        callViewReport()
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
                        var date1: Date? = null
                        if (loc.startDate != null) {
                            date1 = formatter.parse(loc.startDate)
                        }
                        if (date1 != null) {
                            diagStartDate = target.format(date1)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    try {
                        var date1: Date? = null
                        if (loc.startDate != null) {
                            date1 = formatter1.parse(loc.startDate)
                        }
                        if (date1 != null) {
                            diagStartDate = target.format(date1)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val positionDate: TextView? = bottomSheetDialog.findViewById(R.id.position_date)
                    val memberName: TextView? = bottomSheetDialog.findViewById(R.id.member_name)
                    val positionAddress: TextView? = bottomSheetDialog.findViewById(R.id.position_address)
                    val tvPositionAddress: TextView? = bottomSheetDialog.findViewById(R.id.tvPositionAddress)
                    val positionTimeDifference: TextView? =
                        bottomSheetDialog.findViewById(R.id.position_time_difference)
                    val tvPositionTime: TextView? = bottomSheetDialog.findViewById(R.id.tvPositionTime)

                    positionDate?.text = diagStartDate
                    memberName?.text = memRouteName
                    val locAddress = loc.adr ?: ""
                    val locInfo = loc.info ?: ""
                    if (locAddress.trim().isNotEmpty()) {
                        positionAddress?.visibility = View.GONE
                        tvPositionAddress?.visibility = View.GONE
                    } else {
                        positionAddress?.visibility = View.VISIBLE
                        tvPositionAddress?.visibility = View.VISIBLE
                        positionAddress?.text = loc.adr
                    }
                    if (locInfo.trim().isNotEmpty()) {
                        positionTimeDifference?.visibility = View.GONE
                        tvPositionTime?.visibility = View.GONE
                    } else {
                        positionTimeDifference?.visibility = View.VISIBLE
                        tvPositionTime?.visibility = View.VISIBLE
                        positionTimeDifference?.text = loc.info
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
