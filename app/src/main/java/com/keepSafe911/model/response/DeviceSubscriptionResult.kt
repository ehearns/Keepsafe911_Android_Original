package com.keepSafe911.model.response

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class DeviceSubscriptionResult() : Parcelable {
    @SerializedName("Subscription_status")
    @Expose
    var status: Boolean = false
    @SerializedName("Subscripiton_tookfrm")
    @Expose
    var subscriptionTookFrom : Int? = 0

    constructor(parcel: Parcel) : this() {
        status = parcel.readByte() != 0.toByte()
        subscriptionTookFrom = parcel.readValue(Int::class.java.classLoader) as? Int
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (status) 1 else 0)
        parcel.writeValue(subscriptionTookFrom)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DeviceSubscriptionResult> {
        override fun createFromParcel(parcel: Parcel): DeviceSubscriptionResult {
            return DeviceSubscriptionResult(parcel)
        }

        override fun newArray(size: Int): Array<DeviceSubscriptionResult?> {
            return arrayOfNulls(size)
        }
    }
}