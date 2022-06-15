package com.keepSafe911.model.response.paypal

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Transaction(): Parcelable {
    @SerializedName("id")
    @Expose
    var id: String? = ""

    @SerializedName("status")
    @Expose
    var status: String? = ""

    @SerializedName("payer_email")
    @Expose
    var payerEmail: String? = ""

    @SerializedName("payer_name")
    @Expose
    var payerName: NameOne? = NameOne()

    @SerializedName("amount_with_breakdown")
    @Expose
    var amountWithBreakdown: AmountWithBreakdown? = AmountWithBreakdown()

    @SerializedName("time")
    @Expose
    var time: String? = ""

    constructor(parcel: Parcel) : this() {
        id = parcel.readString()
        status = parcel.readString()
        payerEmail = parcel.readString()
        payerName = parcel.readParcelable(NameOne::class.java.classLoader)
        amountWithBreakdown = parcel.readParcelable(AmountWithBreakdown::class.java.classLoader)
        time = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(status)
        parcel.writeString(payerEmail)
        parcel.writeParcelable(payerName, flags)
        parcel.writeParcelable(amountWithBreakdown, flags)
        parcel.writeString(time)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Transaction> {
        override fun createFromParcel(parcel: Parcel): Transaction {
            return Transaction(parcel)
        }

        override fun newArray(size: Int): Array<Transaction?> {
            return arrayOfNulls(size)
        }
    }
}