package com.keepSafe911.model.response

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class CheckFrequencyResult(): Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeValue(isReport)
        dest?.writeValue(frequency)
        dest?.writeString(memberUtcDateTime)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("IsReport")
    @Expose
    var isReport: Boolean? = false
    @SerializedName("Frequency")
    @Expose
    var frequency: Int? = 0
    @SerializedName("MemberUtcDateTime")
    @Expose
    var memberUtcDateTime: String? = ""

    constructor(parcel: Parcel) : this() {
        isReport = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        frequency = parcel.readValue(Int::class.java.classLoader) as? Int
        memberUtcDateTime = parcel.readString()
    }

    companion object CREATOR : Parcelable.Creator<CheckFrequencyResult> {
        override fun createFromParcel(parcel: Parcel): CheckFrequencyResult {
            return CheckFrequencyResult(parcel)
        }

        override fun newArray(size: Int): Array<CheckFrequencyResult?> {
            return arrayOfNulls(size)
        }
    }
}