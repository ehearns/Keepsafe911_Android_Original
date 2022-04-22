package com.keepSafe911.model.request.paymentrequest

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class MerchantAuthentication() : Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(name)
        dest?.writeString(transactionKey)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("name")
    @Expose
    var name: String? = ""
    @SerializedName("transactionKey")
    @Expose
    var transactionKey: String? = ""

    constructor(parcel: Parcel) : this() {
        name = parcel.readString()
        transactionKey = parcel.readString()
    }

    companion object CREATOR : Parcelable.Creator<MerchantAuthentication> {
        override fun createFromParcel(parcel: Parcel): MerchantAuthentication {
            return MerchantAuthentication(parcel)
        }

        override fun newArray(size: Int): Array<MerchantAuthentication?> {
            return arrayOfNulls(size)
        }
    }

}