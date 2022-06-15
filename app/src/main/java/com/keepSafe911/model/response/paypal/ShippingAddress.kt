package com.keepSafe911.model.response.paypal

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class ShippingAddress(): Parcelable {
    @SerializedName("name")
    @Expose
    var name: SubscriptionName? = SubscriptionName()

    @SerializedName("address")
    @Expose
    var address: SubscriptionAddress? = SubscriptionAddress()

    constructor(parcel: Parcel) : this() {
        name = parcel.readParcelable(SubscriptionName::class.java.classLoader)
        address = parcel.readParcelable(SubscriptionAddress::class.java.classLoader)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(name, flags)
        parcel.writeParcelable(address, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ShippingAddress> {
        override fun createFromParcel(parcel: Parcel): ShippingAddress {
            return ShippingAddress(parcel)
        }

        override fun newArray(size: Int): Array<ShippingAddress?> {
            return arrayOfNulls(size)
        }
    }
}