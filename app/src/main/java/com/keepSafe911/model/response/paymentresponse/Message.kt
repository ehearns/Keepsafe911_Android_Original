package com.keepSafe911.model.response.paymentresponse

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Message() : Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(code)
        dest?.writeString(description)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("code")
    @Expose
    var code: String? = ""
    @SerializedName("description")
    @Expose
    var description: String? = ""

    constructor(parcel: Parcel) : this() {
        code = parcel.readString()
        description = parcel.readString()
    }

    companion object CREATOR : Parcelable.Creator<Message> {
        override fun createFromParcel(parcel: Parcel): Message {
            return Message(parcel)
        }

        override fun newArray(size: Int): Array<Message?> {
            return arrayOfNulls(size)
        }
    }
}