package com.keepSafe911.model.response.paypal

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class SubscriptionAmount(): Parcelable {
    @SerializedName("currency_code")
    @Expose
    var currencyCode: String? = ""

    @SerializedName("value")
    @Expose
    var value: String? = ""

    constructor(parcel: Parcel) : this() {
        currencyCode = parcel.readString()
        value = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(currencyCode ?: "")
        parcel.writeString(value ?: "")
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SubscriptionAmount> {
        override fun createFromParcel(parcel: Parcel): SubscriptionAmount {
            return SubscriptionAmount(parcel)
        }

        override fun newArray(size: Int): Array<SubscriptionAmount?> {
            return arrayOfNulls(size)
        }
    }
}