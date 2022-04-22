package com.keepSafe911.model

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

class BoundarySummaryResult {

    @SerializedName("ID")
    @Expose
    var iD: Int = 0
    @SerializedName("MemberID")
    @Expose
    var memberID: Int = 0
    @SerializedName("GeoFenceID")
    @Expose
    var geoFenceID: Int? = 0
    @SerializedName("Status")
    @Expose
    var status: Boolean = false
    @SerializedName("IsTextMessage")
    @Expose
    var isTextMessage: Boolean? = false
    @SerializedName("GeoFenceTime")
    @Expose
    var geoFenceTime: String? = ""
    @SerializedName("NotificationMessage")
    @Expose
    var notificationMessage: String? = ""
    @SerializedName("DeletedMembers")
    @Expose
    var deletedMembers: Any? = null
    @SerializedName("MemberName")
    @Expose
    var memberName: String? = ""
    @SerializedName("GeoFenceName")
    @Expose
    var geoFenceName: String? = ""
    @SerializedName("Latitude")
    @Expose
    var latitude: Double = 0.toDouble()
    @SerializedName("Longitude")
    @Expose
    var longitude: Double = 0.toDouble()
    @SerializedName("CreatedOn")
    @Expose
    var createdOn: String? = ""
}