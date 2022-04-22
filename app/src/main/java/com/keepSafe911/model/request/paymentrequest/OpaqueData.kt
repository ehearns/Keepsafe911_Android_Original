package com.keepSafe911.model.request.paymentrequest

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class OpaqueData() : Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(dataDescriptor)
        dest?.writeString(dataValue)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("dataDescriptor")
    @Expose
    var dataDescriptor: String? = ""
    @SerializedName("dataValue")
    @Expose
    var dataValue: String? = ""

    constructor(parcel: Parcel) : this() {
        dataDescriptor = parcel.readString()
        dataValue = parcel.readString()
    }

    companion object CREATOR : Parcelable.Creator<OpaqueData> {
        override fun createFromParcel(parcel: Parcel): OpaqueData {
            return OpaqueData(parcel)
        }

        override fun newArray(size: Int): Array<OpaqueData?> {
            return arrayOfNulls(size)
        }
    }

}