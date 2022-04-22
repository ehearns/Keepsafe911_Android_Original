package com.keepSafe911.model.response

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class FrequencyUser() : Parcelable{
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeValue(memberID)
        dest?.writeString(name)
        dest?.writeString(email)
        dest?.writeValue(isDeleted)
        dest?.writeByte(if (isSelected) 1 else 0)
        dest?.writeByte(if (isPaymentDone) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("MemberId")
    @Expose
    var memberID: Int? = 0
    @SerializedName("Name")
    @Expose
    var name: String? = ""
    @SerializedName("Email")
    @Expose
    var email: String? = ""
    @SerializedName("IsDeleted")
    @Expose
    var isDeleted: Boolean? = false

    var isSelected: Boolean = false
    var isPaymentDone: Boolean = false

    constructor(parcel: Parcel) : this() {
        memberID = parcel.readValue(Int::class.java.classLoader) as? Int
        name = parcel.readString()
        email = parcel.readString()
        isDeleted = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        isSelected = parcel.readByte() != 0.toByte()
        isPaymentDone = parcel.readByte() != 0.toByte()
    }

    companion object CREATOR : Parcelable.Creator<FrequencyUser> {
        override fun createFromParcel(parcel: Parcel): FrequencyUser {
            return FrequencyUser(parcel)
        }

        override fun newArray(size: Int): Array<FrequencyUser?> {
            return arrayOfNulls(size)
        }
    }
}