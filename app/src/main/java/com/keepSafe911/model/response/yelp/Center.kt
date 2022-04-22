package com.keepSafe911.model.response.yelp

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Center(): Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeValue(longitude)
        dest?.writeValue(latitude)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("longitude")
    @Expose
    var longitude: Double? = 0.0
    @SerializedName("latitude")
    @Expose
    var latitude: Double? = 0.0

    constructor(parcel: Parcel) : this() {
        longitude = parcel.readValue(Double::class.java.classLoader) as? Double
        latitude = parcel.readValue(Double::class.java.classLoader) as? Double
    }

    companion object CREATOR : Parcelable.Creator<Center> {
        override fun createFromParcel(parcel: Parcel): Center {
            return Center(parcel)
        }

        override fun newArray(size: Int): Array<Center?> {
            return arrayOfNulls(size)
        }
    }

}
