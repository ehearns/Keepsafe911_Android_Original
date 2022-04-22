package com.keepSafe911.model.response

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class MemberSubscriptionResult(): Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(message)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("Message")
    @Expose
    var message: String? = ""

    constructor(parcel: Parcel) : this() {
        message = parcel.readString()
    }

    companion object CREATOR : Parcelable.Creator<MemberSubscriptionResult> {
        override fun createFromParcel(parcel: Parcel): MemberSubscriptionResult {
            return MemberSubscriptionResult(parcel)
        }

        override fun newArray(size: Int): Array<MemberSubscriptionResult?> {
            return arrayOfNulls(size)
        }
    }

}