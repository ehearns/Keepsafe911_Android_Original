package com.keepSafe911.model.response.hibp

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class BreachedAccount(): Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(name)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("Name")
    @Expose
    var name: String? = ""

    constructor(parcel: Parcel) : this() {
        name = parcel.readString()
    }

    companion object CREATOR : Parcelable.Creator<BreachedAccount> {
        override fun createFromParcel(parcel: Parcel): BreachedAccount {
            return BreachedAccount(parcel)
        }

        override fun newArray(size: Int): Array<BreachedAccount?> {
            return arrayOfNulls(size)
        }
    }

}