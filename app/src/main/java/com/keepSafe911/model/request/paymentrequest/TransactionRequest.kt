package com.keepSafe911.model.request.paymentrequest

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class TransactionRequest() : Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(transactionType)
        dest?.writeString(amount)
        dest?.writeParcelable(payment, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("transactionType")
    @Expose
    var transactionType: String? = ""
    @SerializedName("amount")
    @Expose
    var amount: String? = ""
    @SerializedName("payment")
    @Expose
    var payment: Payment? = Payment()

    constructor(parcel: Parcel) : this() {
        transactionType = parcel.readString()
        amount = parcel.readString()
        payment = parcel.readParcelable(Payment::class.java.classLoader)
    }

    companion object CREATOR : Parcelable.Creator<TransactionRequest> {
        override fun createFromParcel(parcel: Parcel): TransactionRequest {
            return TransactionRequest(parcel)
        }

        override fun newArray(size: Int): Array<TransactionRequest?> {
            return arrayOfNulls(size)
        }
    }

}