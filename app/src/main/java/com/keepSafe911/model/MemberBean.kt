package com.keepSafe911.model

import android.os.Parcel
import android.os.Parcelable

class MemberBean() : Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeValue(id)
        dest?.writeString(memberName)
        dest?.writeString(memberEmail)
        dest?.writeString(memberImage)
        dest?.writeByte(if (isSelected) 1 else 0)
        dest?.writeByte(if (isPaymentDone) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    var id: Int? = 0
    var memberName: String? = ""
    var memberEmail: String? = ""
    var memberImage: String? = ""
    var isSelected: Boolean = false
    var isPaymentDone: Boolean = false

    constructor(parcel: Parcel) : this() {
        id = parcel.readValue(Int::class.java.classLoader) as? Int
        memberName = parcel.readString()
        memberEmail = parcel.readString()
        memberImage = parcel.readString()
        isSelected = parcel.readByte() != 0.toByte()
        isPaymentDone = parcel.readByte() != 0.toByte()
    }

    companion object CREATOR : Parcelable.Creator<MemberBean> {
        override fun createFromParcel(parcel: Parcel): MemberBean {
            return MemberBean(parcel)
        }

        override fun newArray(size: Int): Array<MemberBean?> {
            return arrayOfNulls(size)
        }
    }
}