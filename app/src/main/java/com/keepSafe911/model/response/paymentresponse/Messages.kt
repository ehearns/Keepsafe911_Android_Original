package com.keepSafe911.model.response.paymentresponse

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Messages() : Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(resultCode)
        dest?.writeTypedList(message)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("resultCode")
    @Expose
    var resultCode: String? = ""
    @SerializedName("message")
    @Expose
    var message: List<Message_>? = ArrayList<Message_>()

    constructor(parcel: Parcel) : this() {
        resultCode = parcel.readString()
        message = parcel.createTypedArrayList(Message_)
    }

    companion object CREATOR : Parcelable.Creator<Messages> {
        override fun createFromParcel(parcel: Parcel): Messages {
            return Messages(parcel)
        }

        override fun newArray(size: Int): Array<Messages?> {
            return arrayOfNulls(size)
        }
    }
}