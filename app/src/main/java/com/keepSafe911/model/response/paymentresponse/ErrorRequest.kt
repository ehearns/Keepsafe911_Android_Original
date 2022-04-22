package com.keepSafe911.model.response.paymentresponse

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class ErrorRequest() : Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(errorCode)
        dest?.writeString(errorText)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("errorCode")
    @Expose
    var errorCode: String? = ""
    @SerializedName("errorText")
    @Expose
    var errorText: String? = ""

    constructor(parcel: Parcel) : this() {
        errorCode = parcel.readString()
        errorText = parcel.readString()
    }

    companion object CREATOR : Parcelable.Creator<ErrorRequest> {
        override fun createFromParcel(parcel: Parcel): ErrorRequest {
            return ErrorRequest(parcel)
        }

        override fun newArray(size: Int): Array<ErrorRequest?> {
            return arrayOfNulls(size)
        }
    }
}