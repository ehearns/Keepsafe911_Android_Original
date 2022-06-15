package com.keepSafe911.model.response.paypal

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlin.collections.ArrayList

class ErrorResponse(): Parcelable {
    @SerializedName("name")
    @Expose
    var name: String? = ""

    @SerializedName("message")
    @Expose
    var message: String? = ""

    @SerializedName("debug_id")
    @Expose
    var debugId: String? = ""

    @SerializedName("details")
    @Expose
    var details: ArrayList<ErrorDetail>? = ArrayList()

    @SerializedName("links")
    @Expose
    var links: ArrayList<Link>? = ArrayList()

    constructor(parcel: Parcel) : this() {
        name = parcel.readString()
        message = parcel.readString()
        debugId = parcel.readString()
        details = parcel.createTypedArrayList(ErrorDetail) ?: ArrayList()
        links = parcel.createTypedArrayList(Link) ?: ArrayList()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name ?: "")
        parcel.writeString(message ?: "")
        parcel.writeString(debugId ?: "")
        parcel.writeTypedList(details)
        parcel.writeTypedList(links)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ErrorResponse> {
        override fun createFromParcel(parcel: Parcel): ErrorResponse {
            return ErrorResponse(parcel)
        }

        override fun newArray(size: Int): Array<ErrorResponse?> {
            return arrayOfNulls(size)
        }
    }
}