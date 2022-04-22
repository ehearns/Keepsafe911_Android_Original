package com.keepSafe911.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Loc() : Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeValue(lat)
        dest?.writeValue(lng)
        dest?.writeValue(r)
        dest?.writeString(adr)
        dest?.writeString(info)
        dest?.writeValue(z)
        dest?.writeString(st)
        dest?.writeString(newDate)
        dest?.writeString(startDate)
        dest?.writeString(startDateValue)
        dest?.writeString(endDate)
        dest?.writeString(batteryLevel)
        dest?.writeString(loginjobImage)
        dest?.writeString(logoutjobImage)
        dest?.writeString(recordStatus)
        dest?.writeValue(offworkhour)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("Lat")
    @Expose
    var lat: Double? = 0.toDouble()
    @SerializedName("Lng")
    @Expose
    var lng: Double? = 0.toDouble()
    @SerializedName("R")
    @Expose
    var r: Int? = 0
    @SerializedName("Adr")
    @Expose
    var adr: String? = ""
    @SerializedName("Info")
    @Expose
    var info: String? = ""
    @SerializedName("z")
    @Expose
    var z: Int? = 0
    @SerializedName("st")
    @Expose
    var st: String? = ""
    @SerializedName("newDate")
    @Expose
    var newDate: String? = ""
    @SerializedName("startDate")
    @Expose
    var startDate: String? = ""
    @SerializedName("startDateValue")
    @Expose
    var startDateValue: String? = ""
    @SerializedName("endDate")
    @Expose
    var endDate: String? = ""
    @SerializedName("BatteryLevel")
    @Expose
    var batteryLevel: String? = ""
    @SerializedName("loginjobImage")
    @Expose
    var loginjobImage: String? = ""
    @SerializedName("logoutjobImage")
    @Expose
    var logoutjobImage: String? = ""
    @SerializedName("RecordStatus")
    @Expose
    var recordStatus: String? = ""
    @SerializedName("offworkhour")
    @Expose
    var offworkhour: Int? = 0

    constructor(parcel: Parcel) : this() {
        lat = parcel.readValue(Double::class.java.classLoader) as? Double
        lng = parcel.readValue(Double::class.java.classLoader) as? Double
        r = parcel.readValue(Int::class.java.classLoader) as? Int
        adr = parcel.readString()
        info = parcel.readString()
        z = parcel.readValue(Int::class.java.classLoader) as? Int
        st = parcel.readString()
        newDate = parcel.readString()
        startDate = parcel.readString()
        startDateValue = parcel.readString()
        endDate = parcel.readString()
        batteryLevel = parcel.readString()
        loginjobImage = parcel.readString()
        logoutjobImage = parcel.readString()
        recordStatus = parcel.readString()
        offworkhour = parcel.readValue(Int::class.java.classLoader) as? Int
    }

    companion object CREATOR : Parcelable.Creator<Loc> {
        override fun createFromParcel(parcel: Parcel): Loc {
            return Loc(parcel)
        }

        override fun newArray(size: Int): Array<Loc?> {
            return arrayOfNulls(size)
        }
    }


}


