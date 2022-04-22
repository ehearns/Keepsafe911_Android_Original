package com.keepSafe911.model.request.paymentrequest

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class PaymentRequest() : Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeParcelable(createTransactionRequest, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("createTransactionRequest")
    @Expose
    var createTransactionRequest: CreateTransactionRequest? = CreateTransactionRequest()

    constructor(parcel: Parcel) : this() {
        createTransactionRequest = parcel.readParcelable(CreateTransactionRequest::class.java.classLoader)
    }

    companion object CREATOR : Parcelable.Creator<PaymentRequest> {
        override fun createFromParcel(parcel: Parcel): PaymentRequest {
            return PaymentRequest(parcel)
        }

        override fun newArray(size: Int): Array<PaymentRequest?> {
            return arrayOfNulls(size)
        }
    }

}