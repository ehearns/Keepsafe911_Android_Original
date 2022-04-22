package com.keepSafe911.model.response

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class LiveStreamResult() : Parcelable {
    @SerializedName("Id")
    @Expose
    var id: Int? = 0

    @SerializedName("MemberId")
    @Expose
    var memberId: Int? = 0

    @SerializedName("AdminId")
    @Expose
    var adminId: Int? = 0

    @SerializedName("ChannelName")
    @Expose
    var channelName: String? = ""

    @SerializedName("CreatedOn")
    @Expose
    var createdOn: String? = ""

    @SerializedName("Lat")
    @Expose
    var lat: Double? = 0.0

    @SerializedName("Lng")
    @Expose
    var lng: Double? = 0.0

    @SerializedName("IsAdmin")
    @Expose
    var isAdmin: Boolean? = false

    @SerializedName("latlongmessage")
    @Expose
    var latLongMessage: String? = ""

    @SerializedName("AdminName")
    @Expose
    var adminName: String? = ""

    @SerializedName("MemberName")
    @Expose
    var memberName: String? = ""

    @SerializedName("AdminProfileUrl")
    @Expose
    var adminProfileUrl: String? = ""

    @SerializedName("MemberProfileUrl")
    @Expose
    var memberProfileUrl: String? = ""

    constructor(parcel: Parcel) : this() {
        id = parcel.readValue(Int::class.java.classLoader) as? Int
        memberId = parcel.readValue(Int::class.java.classLoader) as? Int
        adminId = parcel.readValue(Int::class.java.classLoader) as? Int
        channelName = parcel.readString()
        createdOn = parcel.readString()
        lat = parcel.readValue(Double::class.java.classLoader) as? Double
        lng = parcel.readValue(Double::class.java.classLoader) as? Double
        isAdmin = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        latLongMessage = parcel.readString()
        adminName = parcel.readString()
        memberName = parcel.readString()
        adminProfileUrl = parcel.readString()
        memberProfileUrl = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeValue(memberId)
        parcel.writeValue(adminId)
        parcel.writeString(channelName)
        parcel.writeString(createdOn)
        parcel.writeValue(lat)
        parcel.writeValue(lng)
        parcel.writeValue(isAdmin)
        parcel.writeString(latLongMessage)
        parcel.writeString(adminName)
        parcel.writeString(memberName)
        parcel.writeString(adminProfileUrl)
        parcel.writeString(memberProfileUrl)
    }



    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LiveStreamResult> {
        override fun createFromParcel(parcel: Parcel): LiveStreamResult {
            return LiveStreamResult(parcel)
        }

        override fun newArray(size: Int): Array<LiveStreamResult?> {
            return arrayOfNulls(size)
        }
    }
}