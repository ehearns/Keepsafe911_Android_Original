package com.keepSafe911.model.response

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class CategoryResult(): Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeValue(id)
        dest?.writeString(name)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("Id")
    @Expose
    var id: Int? = 0
    @SerializedName("Name")
    @Expose
    var name: String? = ""

    constructor(parcel: Parcel) : this() {
        id = parcel.readValue(Int::class.java.classLoader) as? Int
        name = parcel.readString()
    }

    companion object CREATOR : Parcelable.Creator<CategoryResult> {
        override fun createFromParcel(parcel: Parcel): CategoryResult {
            return CategoryResult(parcel)
        }

        override fun newArray(size: Int): Array<CategoryResult?> {
            return arrayOfNulls(size)
        }
    }
}