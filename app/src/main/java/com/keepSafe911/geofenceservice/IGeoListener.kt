package com.keepSafe911.geofenceservice

import android.location.Location

interface IGeoListener {
    fun showNotificationEntryEvent(distance: Distance, userLocation: Location)

    fun showNotificationExitEvent(distance: Distance, userLocation: Location)
}