package com.keepSafe911.model.response.paypal

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class SubscriptionName(): Parcelable {
    @SerializedName("full_name")
    @Expose
    var fullName: String? = ""

    constructor(parcel: Parcel) : this() {
        fullName = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(fullName ?: "")
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SubscriptionName> {
        override fun createFromParcel(parcel: Parcel): SubscriptionName {
            return SubscriptionName(parcel)
        }

        override fun newArray(size: Int): Array<SubscriptionName?> {
            return arrayOfNulls(size)
        }
    }
}