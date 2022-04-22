package com.keepSafe911.model.response

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class FrequencyHistoryResult(): Parcelable {

    @SerializedName("lstPremiumMember")
    @Expose
    var lstPremiumMember: ArrayList<FrequencyUser> = ArrayList()
    @SerializedName("AdminID")
    @Expose
    var adminId : Int? = 0
    @SerializedName("Frequency")
    @Expose
    var frequency: Int? = 0
    @SerializedName("PaymentType")
    @Expose
    var paymentType: Int? = 0
    @SerializedName("Amount")
    @Expose
    var amount: Double? = 0.0
    @SerializedName("PaymentDate")
    @Expose
    var paymentDate: String? = ""
    @SerializedName("PaymentEndDate")
    @Expose
    var paymentEndDate: String? = ""
    @SerializedName("IsPayment")
    @Expose
    var isPayment: Boolean? = false

    constructor(parcel: Parcel) : this() {
        adminId = parcel.readValue(Int::class.java.classLoader) as? Int
        frequency = parcel.readValue(Int::class.java.classLoader) as? Int
        paymentType = parcel.readValue(Int::class.java.classLoader) as? Int
        amount = parcel.readValue(Double::class.java.classLoader) as? Double
        paymentDate = parcel.readString()
        paymentEndDate = parcel.readString()
        isPayment = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(adminId)
        parcel.writeValue(frequency)
        parcel.writeValue(paymentType)
        parcel.writeValue(amount)
        parcel.writeString(paymentDate)
        parcel.writeString(paymentEndDate)
        parcel.writeValue(isPayment)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FrequencyHistoryResult> {
        override fun createFromParcel(parcel: Parcel): FrequencyHistoryResult {
            return FrequencyHistoryResult(parcel)
        }

        override fun newArray(size: Int): Array<FrequencyHistoryResult?> {
            return arrayOfNulls(size)
        }
    }
}