package com.keepSafe911.model.response.paymentresponse

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Message_() : Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(code)
        dest?.writeString(text)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("code")
    @Expose
    var code: String? = ""
    @SerializedName("text")
    @Expose
    var text: String? = ""

    constructor(parcel: Parcel) : this() {
        code = parcel.readString()
        text = parcel.readString()
    }

    companion object CREATOR : Parcelable.Creator<Message_> {
        override fun createFromParcel(parcel: Parcel): Message_ {
            return Message_(parcel)
        }

        override fun newArray(size: Int): Array<Message_?> {
            return arrayOfNulls(size)
        }
    }

}