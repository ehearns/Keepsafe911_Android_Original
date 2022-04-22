package com.keepSafe911.model

import android.os.Parcel
import android.os.Parcelable

class SubscriptionBean() : Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeInt(subScriptionCode)
        dest?.writeInt(subScriptionDays)
        dest?.writeDouble(subScriptionCost)
    }

    override fun describeContents(): Int {
        return 0
    }

    constructor(code: Int, days: Int, cost: Double) : this() {
        subScriptionCode = code
        subScriptionDays = days
        subScriptionCost = cost
    }

    var subScriptionCode: Int = 0
    var subScriptionDays: Int = 0
    var subScriptionCost: Double = 1.0

    constructor(parcel: Parcel) : this() {
        subScriptionCode = parcel.readInt()
        subScriptionDays = parcel.readInt()
        subScriptionCost = parcel.readDouble()
    }

    companion object CREATOR : Parcelable.Creator<SubscriptionBean> {
        override fun createFromParcel(parcel: Parcel): SubscriptionBean {
            return SubscriptionBean(parcel)
        }

        override fun newArray(size: Int): Array<SubscriptionBean?> {
            return arrayOfNulls(size)
        }
    }
}