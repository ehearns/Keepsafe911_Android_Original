package com.keepSafe911.gps

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.keepSafe911.utils.ConnectionUtil

class GpsTracker() : Service(), LocationListener {
    // Declaring a Location Manager
    protected var locationManager: LocationManager? = null
    private lateinit var mContext: Context
    // flag for GPS status
    private var isGPSEnabled = false
    // flag for network status
    private var isNetworkEnabled = false
    // flag for GPS status
    private var canGetLocation = false
    private var location: Location? = null // location
    private var latitude: Double = 0.toDouble() // latitude
    private var longitude: Double = 0.toDouble() // longitude
    val myBinder: Binder = MyLocalBinder()


    constructor(context: Context) : this() {
        this.mContext = context
        getLocation()
    }

    fun isGPSEnable(): Boolean {
        locationManager = mContext
            .getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
    }

    fun isNetWorkProviderEnable(): Boolean {
        locationManager = mContext
            .getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false
    }

    fun getLocation(): Location? {
        try {
            locationManager = mContext
                .getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // getting GPS status
            isGPSEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false

            // getting network status
            isNetworkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
            } else {
                this.canGetLocation = true
                // First get location from Network Provider
                if (ConnectionUtil.isInternetAvailable(mContext)) {
                    if (isNetworkEnabled) {

                        locationManager?.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this
                        )
                        if (locationManager != null) {
                            location = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                            if (location != null) {
                                latitude = location?.latitude ?: 0.0
                                longitude = location?.longitude ?: 0.0
                            }
                        }
                    }
                }
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager?.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this
                        )
                        if (locationManager != null) {
                            location = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                            if (location != null) {
                                latitude = location?.latitude ?: 0.0
                                longitude = location?.longitude ?: 0.0
                            }
                        }
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return location
    }

    /**
     * Function to get latitude
     */
    fun getLatitude(): Double {
        if (location != null) {
            latitude = location?.latitude ?: 0.0
        }
        return latitude
    }

    fun stopLocationUpdates() {
        if (locationManager != null) {
            locationManager?.removeUpdates(this)
        }
    }
    /*
    for location
         */
    fun CheckForLoCation(): Boolean {
        try {
            locationManager = mContext
                .getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            //getting GPS status
            isGPSEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false

            //getting network status
            isNetworkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false

            return (isGPSEnabled && isNetworkEnabled)
            // no network provider is enabled

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    /**
     * Function to get longitude
     */
    fun getLongitude(): Double {
        if (location != null) {
            longitude = location?.longitude ?: 0.0
        }
        return longitude
    }

    /**
     * Function to check GPS/wifi enabled
     *
     * @return boolean
     */
    fun canGetLocation(): Boolean {
        return this.canGetLocation
    }

    override fun onLocationChanged(location: Location) {
        Log.d("!@@@GPSTrackerLocation", "onLocationChanged: "+location.latitude+ ", "+ location.longitude)
    }

    override fun onProviderDisabled(provider: String) {}

    override fun onProviderEnabled(provider: String) {}

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

    override fun onBind(arg0: Intent?): IBinder? {
        return myBinder
    }

    inner class MyLocalBinder : Binder() {
        fun getService(): GpsTracker {
            return this@GpsTracker
        }
    }

    companion object {

        // The minimum distance to change Updates in meters
        private val MIN_DISTANCE_CHANGE_FOR_UPDATES: Long = 10 // 10 meters
        // The minimum time between updates in milliseconds
        private val MIN_TIME_BW_UPDATES = (1000 * 40).toLong() // 40 seconds
    }
}
