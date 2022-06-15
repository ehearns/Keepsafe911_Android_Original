package com.keepSafe911.model.response.paypal

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class SubscriptionAddress(): Parcelable {
    @SerializedName("address_line_1")
    @Expose
    var addressLine1: String? = ""

    @SerializedName("address_line_2")
    @Expose
    var addressLine2: String? = ""

    @SerializedName("admin_area_2")
    @Expose
    var adminArea2: String? = ""

    @SerializedName("admin_area_1")
    @Expose
    var adminArea1: String? = ""

    @SerializedName("postal_code")
    @Expose
    var postalCode: String? = ""

    @SerializedName("country_code")
    @Expose
    var countryCode: String? = ""

    constructor(parcel: Parcel) : this() {
        addressLine1 = parcel.readString()
        addressLine2 = parcel.readString()
        adminArea2 = parcel.readString()
        adminArea1 = parcel.readString()
        postalCode = parcel.readString()
        countryCode = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(addressLine1 ?: "")
        parcel.writeString(addressLine2 ?: "")
        parcel.writeString(adminArea2 ?: "")
        parcel.writeString(adminArea1 ?: "")
        parcel.writeString(postalCode ?: "")
        parcel.writeString(countryCode ?: "")
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SubscriptionAddress> {
        override fun createFromParcel(parcel: Parcel): SubscriptionAddress {
            return SubscriptionAddress(parcel)
        }

        override fun newArray(size: Int): Array<SubscriptionAddress?> {
            return arrayOfNulls(size)
        }
    }
}