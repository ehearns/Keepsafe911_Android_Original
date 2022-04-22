package com.keepSafe911.geofenceservice

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.keepSafe911.model.GeoFenceResult
import com.keepSafe911.utils.inside
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

class GeoFenceUtil(
    private val iGeoListener: IGeoListener,
    private val geoFenceList: ArrayList<GeoFenceResult>
) {

    /**
     * check user is enter in geofence or exit from geofence
     */
    fun checkForGeoEntryExit(
        userLocation: Location,
        distance: Distance
    ) {
        val userLatLong = LatLng(userLocation.latitude, userLocation.longitude)
        val targetLatLng = LatLng(distance.latLng.latitude, distance.latLng.longitude)

        val distanceInMeters = SphericalUtil.computeDistanceBetween(userLatLong, targetLatLng)
        val dataRadius = distance.geofenceModel.radius ?: 0

        if (distanceInMeters < dataRadius.toDouble()) {
            if (inside) {
                inside = false
                iGeoListener.showNotificationEntryEvent(distance, userLocation)
            }
        } else if (distanceInMeters > dataRadius.toDouble()) {
            if (!inside) {
                inside = true
                iGeoListener.showNotificationExitEvent(distance, userLocation)
            }
        }
    }

    /**
     * get latitude and longitude from geofence list
     */
    private fun extractAllGroFenceData(): ArrayList<LatLng> {
        val arrayList = arrayListOf<LatLng>()
        for (geoData in geoFenceList) {
            val lat = geoData.latitude
            val long = geoData.longitude
            arrayList.add(LatLng(lat, long))

        }
        return arrayList
    }

    /**
     * filter geofence list according to user current location
     */
    fun filterLatLong(userLatLong: LatLng): ArrayList<Distance> {
        val finalList = arrayListOf<Distance>()
        for ((i, latLong) in extractAllGroFenceData().withIndex()) {
            val dist = distance(
                userLatLong.latitude,
                userLatLong.longitude,
                latLong.latitude,
                latLong.longitude
            )
            finalList.add(
                Distance(
                    dist,
                    latLong,
                    geoFenceList[i]
                )
            )
        }
        return finalList
    }

    /**
     * calculate distance between user location and geofence list location
     */
    private fun distance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val theta = lon1 - lon2
        var dist =
            (sin(deg2rad(lat1)) * sin(deg2rad(lat2))
                    + (cos(deg2rad(lat1)) * cos(deg2rad(lat2))
                    * cos(deg2rad(theta))))
        dist = acos(dist)
        dist = rad2deg(dist)
        dist *= 60
        dist *= 1852
        return dist
    }

    private fun deg2rad(deg: Double): Double {
        return deg * Math.PI / 180.0
    }

    private fun rad2deg(rad: Double): Double {
        return rad * 180.0 / Math.PI
    }
}