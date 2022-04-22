package com.keepSafe911.model.response.yelp

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Category(): Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(alias)
        dest?.writeString(title)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("alias")
    @Expose
    var alias: String? = ""
    @SerializedName("title")
    @Expose
    var title: String? = ""

    constructor(parcel: Parcel) : this() {
        alias = parcel.readString()
        title = parcel.readString()
    }

    companion object CREATOR : Parcelable.Creator<Category> {
        override fun createFromParcel(parcel: Parcel): Category {
            return Category(parcel)
        }

        override fun newArray(size: Int): Array<Category?> {
            return arrayOfNulls(size)
        }
    }

}
