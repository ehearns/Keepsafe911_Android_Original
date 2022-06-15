package com.keepSafe911.model.response.paypal

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class DisplayData(): Parcelable {
    @SerializedName("business_email")
    @Expose
    var businessEmail: String? = ""

    constructor(parcel: Parcel) : this() {
        businessEmail = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(businessEmail ?: "")
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DisplayData> {
        override fun createFromParcel(parcel: Parcel): DisplayData {
            return DisplayData(parcel)
        }

        override fun newArray(size: Int): Array<DisplayData?> {
            return arrayOfNulls(size)
        }
    }
}