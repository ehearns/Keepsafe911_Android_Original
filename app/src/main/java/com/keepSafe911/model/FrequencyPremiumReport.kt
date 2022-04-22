package com.keepSafe911.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class FrequencyPremiumReport(): Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeInt(id)
        dest?.writeInt(frequency)
        dest?.writeInt(paymentType)
        dest?.writeDouble(amount)
        dest?.writeString(paymentDate)
        dest?.writeString(token)
        dest?.writeString(payeID)
        dest?.writeByte(if (isPayment) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("ID")
    @Expose
    var id: Int = 0
    @SerializedName("MemberID")
    @Expose
    var memberID: ArrayList<MemberBean> = ArrayList()
    @SerializedName("Frequency")
    @Expose
    var frequency: Int = 0
    @SerializedName("PaymentType")
    @Expose
    var paymentType: Int = 0
    @SerializedName("Amount")
    @Expose
    var amount: Double = 0.0
    @SerializedName("PaymentDate")
    @Expose
    var paymentDate: String = ""
    @SerializedName("Token")
    @Expose
    var token: String = ""
    @SerializedName("PayeID")
    @Expose
    var payeID: String = ""
    @SerializedName("IsPayment")
    @Expose
    var isPayment: Boolean = false

    constructor(parcel: Parcel) : this() {
        id = parcel.readInt()
        frequency = parcel.readInt()
        paymentType = parcel.readInt()
        amount = parcel.readDouble()
        paymentDate = parcel.readString() ?: ""
        token = parcel.readString() ?: ""
        payeID = parcel.readString() ?: ""
        isPayment = parcel.readByte() != 0.toByte()
    }

    companion object CREATOR : Parcelable.Creator<FrequencyPremiumReport> {
        override fun createFromParcel(parcel: Parcel): FrequencyPremiumReport {
            return FrequencyPremiumReport(parcel)
        }

        override fun newArray(size: Int): Array<FrequencyPremiumReport?> {
            return arrayOfNulls(size)
        }
    }

}