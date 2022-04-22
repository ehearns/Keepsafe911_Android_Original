package com.keepSafe911.model.response.yelp

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Location() : Parcelable{
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(address1)
        dest?.writeString(address2)
        dest?.writeString(address3)
        dest?.writeString(city)
        dest?.writeString(zipCode)
        dest?.writeString(country)
        dest?.writeString(state)
        dest?.writeStringList(displayAddress)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("address1")
    @Expose
    var address1: String? = ""
    @SerializedName("address2")
    @Expose
    var address2: String? = ""
    @SerializedName("address3")
    @Expose
    var address3: String? = ""
    @SerializedName("city")
    @Expose
    var city: String? = ""
    @SerializedName("zip_code")
    @Expose
    var zipCode: String? = ""
    @SerializedName("country")
    @Expose
    var country: String? = ""
    @SerializedName("state")
    @Expose
    var state: String? = ""
    @SerializedName("display_address")
    @Expose
    var displayAddress: ArrayList<String>? = ArrayList()

    constructor(parcel: Parcel) : this() {
        address1 = parcel.readString()
        address2 = parcel.readString()
        address3 = parcel.readString()
        city = parcel.readString()
        zipCode = parcel.readString()
        country = parcel.readString()
        state = parcel.readString()
        displayAddress = parcel.createStringArrayList()
    }

    companion object CREATOR : Parcelable.Creator<Location> {
        override fun createFromParcel(parcel: Parcel): Location {
            return Location(parcel)
        }

        override fun newArray(size: Int): Array<Location?> {
            return arrayOfNulls(size)
        }
    }

}
