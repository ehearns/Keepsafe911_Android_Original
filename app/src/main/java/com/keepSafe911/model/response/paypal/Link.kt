package com.keepSafe911.model.response.paypal

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Link(): Parcelable {
    @SerializedName("href")
    @Expose
    var href: String? = ""

    @SerializedName("rel")
    @Expose
    var rel: String? = ""

    @SerializedName("method")
    @Expose
    var method: String? = ""

    @SerializedName("encType")
    @Expose
    var encType: String? = ""

    constructor(parcel: Parcel) : this() {
        href = parcel.readString()
        rel = parcel.readString()
        method = parcel.readString()
        encType = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(href ?: "")
        parcel.writeString(rel ?: "")
        parcel.writeString(method ?: "")
        parcel.writeString(encType ?: "")
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Link> {
        override fun createFromParcel(parcel: Parcel): Link {
            return Link(parcel)
        }

        override fun newArray(size: Int): Array<Link?> {
            return arrayOfNulls(size)
        }
    }
}