package com.keepSafe911.model.response.paypal

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Subscriber(): Parcelable {
    @SerializedName("shipping_address")
    @Expose
    var shippingAddress: ShippingAddress? = ShippingAddress()

    @SerializedName("name")
    @Expose
    var name: NameOne? = NameOne()

    @SerializedName("email_address")
    @Expose
    var emailAddress: String? = ""

    @SerializedName("payer_id")
    @Expose
    var payerId: String? = ""

    constructor(parcel: Parcel) : this() {
        shippingAddress = parcel.readParcelable(ShippingAddress::class.java.classLoader)
        name = parcel.readParcelable(NameOne::class.java.classLoader)
        emailAddress = parcel.readString()
        payerId = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(shippingAddress, flags)
        parcel.writeParcelable(name, flags)
        parcel.writeString(emailAddress ?: "")
        parcel.writeString(payerId ?: "")
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Subscriber> {
        override fun createFromParcel(parcel: Parcel): Subscriber {
            return Subscriber(parcel)
        }

        override fun newArray(size: Int): Array<Subscriber?> {
            return arrayOfNulls(size)
        }
    }
}