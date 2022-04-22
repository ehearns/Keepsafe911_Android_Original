package com.keepSafe911.model.request.paymentrequest

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Payment() : Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeParcelable(opaqueData, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("opaqueData")
    @Expose
    var opaqueData: OpaqueData? = OpaqueData()

    constructor(parcel: Parcel) : this() {
        opaqueData = parcel.readParcelable(OpaqueData::class.java.classLoader)
    }

    companion object CREATOR : Parcelable.Creator<Payment> {
        override fun createFromParcel(parcel: Parcel): Payment {
            return Payment(parcel)
        }

        override fun newArray(size: Int): Array<Payment?> {
            return arrayOfNulls(size)
        }
    }

}