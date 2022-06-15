package com.keepSafe911.model.response.paymentresponse

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class SecureAcceptance(): Parcelable {
    @SerializedName("SecureAcceptanceUrl")
    @Expose
    var secureAcceptanceUrl: String? = ""

    constructor(parcel: Parcel) : this() {
        secureAcceptanceUrl = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(secureAcceptanceUrl)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SecureAcceptance> {
        override fun createFromParcel(parcel: Parcel): SecureAcceptance {
            return SecureAcceptance(parcel)
        }

        override fun newArray(size: Int): Array<SecureAcceptance?> {
            return arrayOfNulls(size)
        }
    }
}