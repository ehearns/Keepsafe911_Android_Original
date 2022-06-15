package com.keepSafe911.model.response.paypal

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class AmountWithBreakdown(): Parcelable {
    @SerializedName("gross_amount")
    @Expose
    var grossAmount: SubscriptionAmount? = SubscriptionAmount()

    @SerializedName("fee_amount")
    @Expose
    var feeAmount: SubscriptionAmount? = SubscriptionAmount()

    @SerializedName("net_amount")
    @Expose
    var netAmount: SubscriptionAmount? = SubscriptionAmount()

    constructor(parcel: Parcel) : this() {
        grossAmount = parcel.readParcelable(SubscriptionAmount::class.java.classLoader)
        feeAmount = parcel.readParcelable(SubscriptionAmount::class.java.classLoader)
        netAmount = parcel.readParcelable(SubscriptionAmount::class.java.classLoader)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(grossAmount, flags)
        parcel.writeParcelable(feeAmount, flags)
        parcel.writeParcelable(netAmount, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AmountWithBreakdown> {
        override fun createFromParcel(parcel: Parcel): AmountWithBreakdown {
            return AmountWithBreakdown(parcel)
        }

        override fun newArray(size: Int): Array<AmountWithBreakdown?> {
            return arrayOfNulls(size)
        }
    }
}