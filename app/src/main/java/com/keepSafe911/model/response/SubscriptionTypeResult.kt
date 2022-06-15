package com.keepSafe911.model.response

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class SubscriptionTypeResult(): Parcelable {

    @SerializedName("Id")
    @Expose
    var id: Int? = 0
    @SerializedName("Title")
    @Expose
    var title: String? = ""
    @SerializedName("Days")
    @Expose
    var days: Int? = 0
    @SerializedName("Users")
    @Expose
    var users: Int? = 0
    @SerializedName("TotalCost")
    @Expose
    var totalCost: Double? = 0.0
    @SerializedName("PlanId")
    @Expose
    var planId: String? = ""

    constructor(parcel: Parcel) : this() {
        id = parcel.readValue(Int::class.java.classLoader) as? Int
        title = parcel.readString()
        days = parcel.readValue(Int::class.java.classLoader) as? Int
        users = parcel.readValue(Int::class.java.classLoader) as? Int
        totalCost = parcel.readValue(Double::class.java.classLoader) as? Double
        planId = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeString(title)
        parcel.writeValue(days)
        parcel.writeValue(users)
        parcel.writeValue(totalCost)
        parcel.writeString(planId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SubscriptionTypeResult> {
        override fun createFromParcel(parcel: Parcel): SubscriptionTypeResult {
            return SubscriptionTypeResult(parcel)
        }

        override fun newArray(size: Int): Array<SubscriptionTypeResult?> {
            return arrayOfNulls(size)
        }
    }
}