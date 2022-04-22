package com.keepSafe911.model.request.paymentrequest

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class CreateTransactionRequest() : Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeParcelable(merchantAuthentication, flags)
        dest?.writeString(refId)
        dest?.writeParcelable(transactionRequest, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("merchantAuthentication")
    @Expose
    var merchantAuthentication: MerchantAuthentication? = MerchantAuthentication()
    @SerializedName("refId")
    @Expose
    var refId: String? = ""
    @SerializedName("transactionRequest")
    @Expose
    var transactionRequest: TransactionRequest? = TransactionRequest()

    constructor(parcel: Parcel) : this() {
        merchantAuthentication = parcel.readParcelable(MerchantAuthentication::class.java.classLoader)
        refId = parcel.readString()
        transactionRequest = parcel.readParcelable(TransactionRequest::class.java.classLoader)
    }

    companion object CREATOR : Parcelable.Creator<CreateTransactionRequest> {
        override fun createFromParcel(parcel: Parcel): CreateTransactionRequest {
            return CreateTransactionRequest(parcel)
        }

        override fun newArray(size: Int): Array<CreateTransactionRequest?> {
            return arrayOfNulls(size)
        }
    }

}