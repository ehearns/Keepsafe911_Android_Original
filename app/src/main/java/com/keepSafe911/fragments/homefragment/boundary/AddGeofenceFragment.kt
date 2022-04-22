package com.keepSafe911.fragments.homefragment.boundary


import AnimationType
import addFragment
import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.SeekBar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.model.GeoFenceResult
import com.keepSafe911.utils.LocaleUtils
import com.keepSafe911.utils.Utils
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.keepSafe911.gps.GpsTracker
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
import com.keepSafe911.BuildConfig
import com.kotlinpermissions.KotlinPermissions
import com.keepSafe911.R
import com.keepSafe911.adapter.PlacesAdapter
import com.keepSafe911.utils.AppPreference
import com.keepSafe911.utils.Comman_Methods
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_add_geofence.*
import kotlinx.android.synthetic.main.toolbar_header.*
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val ARG_PARAM3 = "param3"

class AddGeofenceFragment : HomeBaseFragment(), Listener, OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private var isUpdate = false
    private var isFrom = false
    private lateinit var gMap: GoogleMap
    private lateinit var mPlacesAdapter: PlacesAdapter
    private lateinit var latLng: LatLng
    private lateinit var geoFenceResult: GeoFenceResult
    private lateinit var circle: Circle
    private lateinit var easyWayLocation: EasyWayLocation

    private var mGoogleApiClient: GoogleApiClient? = null
    private var mLocationRequest: LocationRequest? = null
    private var gpstracker: GpsTracker? = null

    companion object {
        fun newInstance(isUpdate: Boolean, geoFenceResult: GeoFenceResult, isFrom: Boolean): AddGeofenceFragment {
            val args = Bundle()
            args.putBoolean(ARG_PARAM1, isUpdate)
            args.putBoolean(ARG_PARAM3, isFrom)
            args.putParcelable(ARG_PARAM2, geoFenceResult)
            val fragment = AddGeofenceFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isUpdate = it.getBoolean(ARG_PARAM1, false)
            isFrom = it.getBoolean(ARG_PARAM3, false)
            geoFenceResult = it.getParcelable(ARG_PARAM2) ?: GeoFenceResult()
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
        return inflater.inflate(R.layout.fragment_add_geofence, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHeader()
        mActivity.disableDrawer()
        gpstracker = GpsTracker(mActivity)

        mPlacesAdapter = PlacesAdapter(mActivity, R.layout.row_places)
        etGeoLocation.setAdapter(mPlacesAdapter)
        etGeoLocation.onItemClickListener = mAutocompleteClickListener
        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            etGeofenceName.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etGeoLocation.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        } else {
            etGeofenceName.imeOptions = EditorInfo.IME_ACTION_NEXT
            etGeoLocation.imeOptions = EditorInfo.IME_ACTION_DONE
        }

        if (etGeoLocation.text.trim().isNotEmpty()) {
            Utils.getLocationFromAddress(mActivity, etGeoLocation.text.trim().toString())
        }
        mvGeo_Fence.onCreate(savedInstanceState)
        if (geoFenceResult.latitude != 0.0 && geoFenceResult.longitude != 0.0) {
            setupGoogleMap(LatLng(geoFenceResult.latitude, geoFenceResult.longitude))
        } else {
            easyWayLocation = EasyWayLocation(mActivity)
            easyWayLocation.setListener(this)
            geoFenceResult.latitude = easyWayLocation.latitude
            geoFenceResult.longitude = easyWayLocation.longitude
            val current_latLng = LatLng(easyWayLocation.latitude, easyWayLocation.longitude)
            setupGoogleMap(current_latLng)
            sbGeoRadius.isEnabled = false
        }
        etGeofenceName.setText(if (geoFenceResult.geoFenceName != null) geoFenceResult.geoFenceName else "")
        etGeoLocation.setText(if (geoFenceResult.address != null) geoFenceResult.address else "")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sbGeoRadius.min = 1
        }
        sbGeoRadius.max = 10
        geoFenceResult.radius = if (geoFenceResult.radius / 1609 != 0) geoFenceResult.radius else 1609
        sbGeoRadius.progress = if (geoFenceResult.radius != 0) geoFenceResult.radius / 1609 else 1
        setMeter(sbGeoRadius.progress)
        sbGeoRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (progress < 1) {
                    geoFenceResult.radius = 1609
                } else {
                    geoFenceResult.radius = progress * 1609
                }
                circle.radius = geoFenceResult.radius.toDouble()

                setMeter(geoFenceResult.radius / 1609)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
        sbGeoRadius.isEnabled = !etGeoLocation.text.trim().isEmpty()
    }

    private fun setMeter(radius: Int) {
        tvSaveGeo.text = mActivity.resources.getString(R.string.cover_radius, radius)
    }

    private fun setHeader() {
        mActivity.disableDrawer()
        tvHeader.setPadding(0, 0, 50, 0)
        when {
            isUpdate -> tvHeader.text = mActivity.resources.getString(R.string.geofence_update)
            else -> tvHeader.text = mActivity.resources.getString(R.string.geofence)
        }
        Utils.setTextGradientColor(tvHeader)
        iv_back.visibility = View.VISIBLE
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
        btnAlertNext.setOnClickListener {
            mActivity.hideKeyboard()
            if (checkValidation()) {
                Comman_Methods.avoidDoubleClicks(it)
                geoFenceResult.geoFenceName = etGeofenceName.text.trim().toString()
                geoFenceResult.address = etGeoLocation.text.trim().toString()
                mActivity.addFragment(
                    GeoMemberFragment.newInstance(
                        isUpdate,
                        geoFenceResult
                    ), true, true, animationType = AnimationType.fadeInfadeOut
                )
            }
        }

        mActivity.checkUserActive()
    }


    private val mAutocompleteClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
        mActivity.hideKeyboard()
        gMap.clear()
        val item = mPlacesAdapter.getItem(position)
        latLng = Utils.getLocationFromAddress(mActivity, (item?.toString() ?: "")) ?: LatLng(0.0, 0.0)
        geoFenceResult.latitude = latLng.latitude
        geoFenceResult.longitude = latLng.longitude
        setupGoogleMap(latLng)
        sbGeoRadius.isEnabled = true
    }

    private fun setupGoogleMap(ll: LatLng?) {
        if (checkGooglePlayServices()) {
            buildGoogleApiClient()
        }
        mvGeo_Fence.onResume()
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
        mvGeo_Fence.getMapAsync(OnMapReadyCallback { googleMap ->
            gMap = googleMap
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
            gMap.isIndoorEnabled = true
            /*val uiSettings = gMap.uiSettings
            uiSettings.isIndoorLevelPickerEnabled = true
            uiSettings.isMyLocationButtonEnabled = true
            uiSettings.isMapToolbarEnabled = true
            uiSettings.isCompassEnabled = true
            uiSettings.isZoomControlsEnabled = true*/
            gMap.uiSettings.setAllGesturesEnabled(true)

            //                gMap.setMyLocationEnabled(true);

            if (ll != null) {
                try {
                    gMap.setMinZoomPreference(15f)
                    val latLng = LatLng(ll.latitude, ll.longitude)
                    val marker = gMap.addMarker(
                        MarkerOptions().position(latLng)
                            .title(Utils.getCompleteAddressString(mActivity, ll.latitude, ll.longitude))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_setting))
                    )
                    gMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                    gMap.setMaxZoomPreference(10f)
                    if (!etGeoLocation.text.trim().isEmpty()) {
                        marker?.showInfoWindow()
                        if (geoFenceResult.radius != 0) {
                            circle = gMap.addCircle(
                                CircleOptions()
                                    .center(LatLng(latLng.latitude, latLng.longitude))
                                    .radius(geoFenceResult.radius.toDouble())
                                    .strokeColor(android.R.color.transparent)
                                    .fillColor(R.color.dark_transparent)
                            )
                            setMeter(geoFenceResult.radius / 1609)
                        } else {
                            circle = gMap.addCircle(
                                CircleOptions()
                                    .center(LatLng(latLng.latitude, latLng.longitude))
                                    .radius(1609.0)
                                    .strokeColor(android.R.color.transparent)
                                    .fillColor(R.color.dark_transparent)
                            )
                        }
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        })
    }


    private fun checkValidation(): Boolean {
        return when {
            etGeofenceName.text.trim().isEmpty() -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.geofence_name_text))
                false
            }
            etGeoLocation.text.trim().isEmpty() -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.geofence_location_text))
                false
            }
            geoFenceResult.latitude == 0.0 && geoFenceResult.longitude == 0.0 -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.proper_location_text))
                false
            }
            else -> true
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        setHeader()
    }


    override fun locationCancelled() {

    }

    override fun locationOn() {
        if (easyWayLocation.getmListener != null) {
            easyWayLocation.beginUpdates()
        }
    }

    override fun onPositionChanged() {

    }

    override fun onMapReady(p0: GoogleMap) {

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
}
