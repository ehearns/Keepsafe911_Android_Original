package com.keepSafe911.model.response.findmissingchild

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

class DonationHistoryResult(): Parcelable {
    @SerializedName("ID")
    @Expose
    var iD: Int? = 0

    @SerializedName("UserId")
    @Expose
    var userId: Int? = 0

    @SerializedName("Token")
    @Expose
    var token: String? = ""

    @SerializedName("InAppPurchasePassword")
    @Expose
    var inAppPurchasePassword: String? = ""

    @SerializedName("PayeID")
    @Expose
    var payeID: String? = ""

    @SerializedName("Amount")
    @Expose
    var amount: Double? = 0.0

    @SerializedName("PaymentDate")
    @Expose
    var paymentDate: String? = ""

    @SerializedName("AccountNumber")
    @Expose
    var accountNumber: String? = ""

    @SerializedName("IsPaid")
    @Expose
    var isPaid: Boolean? = false

    @SerializedName("DeviceType")
    @Expose
    var deviceType: Int? = 0

    constructor(parcel: Parcel) : this() {
        iD = parcel.readValue(Int::class.java.classLoader) as? Int
        userId = parcel.readValue(Int::class.java.classLoader) as? Int
        token = parcel.readString()
        inAppPurchasePassword = parcel.readString()
        payeID = parcel.readString()
        amount = parcel.readValue(Double::class.java.classLoader) as? Double
        paymentDate = parcel.readString()
        accountNumber = parcel.readString()
        isPaid = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        deviceType = parcel.readValue(Int::class.java.classLoader) as? Int
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(iD)
        parcel.writeValue(userId)
        parcel.writeString(token)
        parcel.writeString(inAppPurchasePassword)
        parcel.writeString(payeID)
        parcel.writeValue(amount)
        parcel.writeString(paymentDate)
        parcel.writeString(accountNumber)
        parcel.writeValue(isPaid)
        parcel.writeValue(deviceType)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DonationHistoryResult> {
        override fun createFromParcel(parcel: Parcel): DonationHistoryResult {
            return DonationHistoryResult(parcel)
        }

        override fun newArray(size: Int): Array<DonationHistoryResult?> {
            return arrayOfNulls(size)
        }
    }
}