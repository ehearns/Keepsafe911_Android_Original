package com.keepSafe911.model.response.yelp

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Coordinates(): Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeValue(latitude)
        dest?.writeValue(longitude)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("latitude")
    @Expose
    var latitude: Double? = 0.0
    @SerializedName("longitude")
    @Expose
    var longitude: Double? = 0.0

    constructor(parcel: Parcel) : this() {
        latitude = parcel.readValue(Double::class.java.classLoader) as? Double
        longitude = parcel.readValue(Double::class.java.classLoader) as? Double
    }

    companion object CREATOR : Parcelable.Creator<Coordinates> {
        override fun createFromParcel(parcel: Parcel): Coordinates {
            return Coordinates(parcel)
        }

        override fun newArray(size: Int): Array<Coordinates?> {
            return arrayOfNulls(size)
        }
    }

}
