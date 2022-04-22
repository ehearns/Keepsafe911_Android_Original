package com.keepSafe911.geofenceservice

import com.google.android.gms.maps.model.LatLng
import com.keepSafe911.model.GeoFenceResult

data class Distance(var distance: Double, var latLng: LatLng,var geofenceModel: GeoFenceResult)