package com.keepSafe911.model.response.paymentresponse

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class UserField() : Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(name)
        dest?.writeString(value)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("name")
    @Expose
    var name: String? = ""
    @SerializedName("value")
    @Expose
    var value: String? = ""

    constructor(parcel: Parcel) : this() {
        name = parcel.readString()
        value = parcel.readString()
    }

    companion object CREATOR : Parcelable.Creator<UserField> {
        override fun createFromParcel(parcel: Parcel): UserField {
            return UserField(parcel)
        }

        override fun newArray(size: Int): Array<UserField?> {
            return arrayOfNulls(size)
        }
    }

}