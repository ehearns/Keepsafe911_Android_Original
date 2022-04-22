package com.keepSafe911.fragments.homefragment.profile

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.keepSafe911.BuildConfig
import com.keepSafe911.utils.*
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_summarymap.*
import kotlinx.android.synthetic.main.toolbar_header.*
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class SummaryMapFragment : HomeBaseFragment(), OnMapReadyCallback {

    var lat: Double = 0.0
    var lng: Double = 0.0
    private lateinit var gMap: GoogleMap

    companion object {
        fun newInstance(lat: Double, lng: Double): SummaryMapFragment {
            val args = Bundle()
            args.putDouble(ARG_PARAM1, lat)
            args.putDouble(ARG_PARAM2, lng)
            val fragment = SummaryMapFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            lat = it.getDouble(ARG_PARAM1, 0.0)
            lng = it.getDouble(ARG_PARAM2, 0.0)
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
        return inflater.inflate(R.layout.fragment_summarymap, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity.disableDrawer()
        setHeader()
        summaryMap.onCreate(savedInstanceState)
        summaryMap.onResume()
        MapsInitializer.initialize(mActivity)
        summaryMap.getMapAsync(this)
    }

    private fun setHeader() {
        iv_close.visibility = View.VISIBLE
        tvHeader.text = mActivity.resources.getString(R.string.str_location)
        Utils.setTextGradientColor(tvHeader)
        tvHeader.setPadding(15, 0, 0, 0)
        iv_close.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
        mActivity.checkUserActive()
    }


    override fun onMapReady(p0: GoogleMap) {
        gMap = p0
        gMap.clear()
        gMap.uiSettings.isMyLocationButtonEnabled = false
        gMap.isIndoorEnabled = true
        /*val uiSettings = gMap.uiSettings
        uiSettings.isIndoorLevelPickerEnabled = true
        uiSettings.isMyLocationButtonEnabled = true
        uiSettings.isMapToolbarEnabled = true
        uiSettings.isCompassEnabled = true
        uiSettings.isZoomControlsEnabled = true*/
        gMap.uiSettings.setAllGesturesEnabled(true)

        if (lat != 0.0 && lng != 0.0) {
            val ll = LatLng(lat, lng)
            val latLng = LatLng(ll.latitude, ll.longitude)
            val marker = gMap.addMarker(
                MarkerOptions().position(latLng)
                    .title(Utils.getCompleteAddressString(mActivity, ll.latitude, ll.longitude))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_marker))
            )
            gMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
            gMap.setMinZoomPreference(15f)
            marker?.showInfoWindow()
        }
    }
}