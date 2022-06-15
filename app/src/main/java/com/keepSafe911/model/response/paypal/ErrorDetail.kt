package com.keepSafe911.model.response.paypal

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class ErrorDetail(): Parcelable {
    @SerializedName("issue")
    @Expose
    var issue: String? = ""

    @SerializedName("description")
    @Expose
    var description: String? = ""

    constructor(parcel: Parcel) : this() {
        issue = parcel.readString()
        description = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(issue ?: "")
        parcel.writeString(description ?: "")
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ErrorDetail> {
        override fun createFromParcel(parcel: Parcel): ErrorDetail {
            return ErrorDetail(parcel)
        }

        override fun newArray(size: Int): Array<ErrorDetail?> {
            return arrayOfNulls(size)
        }
    }
}