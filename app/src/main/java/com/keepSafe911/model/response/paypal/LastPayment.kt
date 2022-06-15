package com.keepSafe911.model.response.paypal

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class LastPayment(): Parcelable {
    @SerializedName("amount")
    @Expose
    var amount: SubscriptionAmount? = SubscriptionAmount()

    @SerializedName("time")
    @Expose
    var time: String? = ""

    constructor(parcel: Parcel) : this() {
        amount = parcel.readParcelable(SubscriptionAmount::class.java.classLoader)
        time = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(amount, flags)
        parcel.writeString(time)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LastPayment> {
        override fun createFromParcel(parcel: Parcel): LastPayment {
            return LastPayment(parcel)
        }

        override fun newArray(size: Int): Array<LastPayment?> {
            return arrayOfNulls(size)
        }
    }
}