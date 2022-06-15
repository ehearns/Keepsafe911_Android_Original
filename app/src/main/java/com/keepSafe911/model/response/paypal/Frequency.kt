package com.keepSafe911.model.response.paypal

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Frequency(): Parcelable {
    @SerializedName("interval_unit")
    @Expose
    var intervalUnit: String? = ""

    @SerializedName("interval_count")
    @Expose
    var intervalCount: Int? = 0

    constructor(parcel: Parcel) : this() {
        intervalUnit = parcel.readString()
        intervalCount = parcel.readValue(Int::class.java.classLoader) as? Int
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(intervalUnit ?: "")
        parcel.writeValue(intervalCount)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Frequency> {
        override fun createFromParcel(parcel: Parcel): Frequency {
            return Frequency(parcel)
        }

        override fun newArray(size: Int): Array<Frequency?> {
            return arrayOfNulls(size)
        }
    }

}