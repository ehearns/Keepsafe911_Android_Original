package com.keepSafe911.model.response.paypal

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class NameOne(): Parcelable {
    @SerializedName("given_name")
    @Expose
    var givenName: String? = ""

    @SerializedName("surname")
    @Expose
    var surname: String? = ""

    constructor(parcel: Parcel) : this() {
        givenName = parcel.readString()
        surname = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(givenName ?: "")
        parcel.writeString(surname ?: "")
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<NameOne> {
        override fun createFromParcel(parcel: Parcel): NameOne {
            return NameOne(parcel)
        }

        override fun newArray(size: Int): Array<NameOne?> {
            return arrayOfNulls(size)
        }
    }
}