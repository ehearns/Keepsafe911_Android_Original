package com.keepSafe911.model.response

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class SubScriptionResult() : Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeValue(iD)
        dest?.writeString(billingDate)
        dest?.writeString(subscriptionID)
        dest?.writeString(billingStartDate)
        dest?.writeString(packageName)
        dest?.writeString(upgradeDate)
        dest?.writeValue(isUpgrade)
        dest?.writeValue(users)
        dest?.writeValue(days)
        dest?.writeString(endDate)
        dest?.writeString(startDate)
        dest?.writeString(paymentDate)
        dest?.writeString(renewalDate)
        dest?.writeValue(isActive)
        dest?.writeString(subscribeDate)
        dest?.writeValue(subscriptionTypeID)
        dest?.writeValue(cost)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("ID")
    @Expose
    var iD: Int? = 0
    @SerializedName("BillingDate")
    @Expose
    var billingDate: String? = ""
    @SerializedName("SubscriptionID")
    @Expose
    var subscriptionID: String? = ""
    @SerializedName("BillingStartDate")
    @Expose
    var billingStartDate: String? = ""
    @SerializedName("PackageName")
    @Expose
    var packageName: String? = ""
    @SerializedName("UpgradeDate")
    @Expose
    var upgradeDate: String? = ""
    @SerializedName("IsUpgrade")
    @Expose
    var isUpgrade: Boolean? = false
    @SerializedName("Users")
    @Expose
    var users: Int? = 0
    @SerializedName("Days")
    @Expose
    var days: Int? = 0
    @SerializedName("EndDate")
    @Expose
    var endDate: String? = ""
    @SerializedName("StartDate")
    @Expose
    var startDate: String? = ""
    @SerializedName("PaymentDate")
    @Expose
    var paymentDate: String? = ""
    @SerializedName("RenewalDate")
    @Expose
    var renewalDate: String? = ""
    @SerializedName("IsActive")
    @Expose
    var isActive: Boolean? = false
    @SerializedName("SubscribeDate")
    @Expose
    var subscribeDate: String? = ""
    @SerializedName("SubscriptionTypeID")
    @Expose
    var subscriptionTypeID: Int? = 0
    @SerializedName("Cost")
    @Expose
    var cost: Double? = 0.0

    constructor(parcel: Parcel) : this() {
        iD = parcel.readValue(Int::class.java.classLoader) as? Int
        billingDate = parcel.readString()
        subscriptionID = parcel.readString()
        billingStartDate = parcel.readString()
        packageName = parcel.readString()
        upgradeDate = parcel.readString()
        isUpgrade = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        users = parcel.readValue(Int::class.java.classLoader) as? Int
        days = parcel.readValue(Int::class.java.classLoader) as? Int
        endDate = parcel.readString()
        startDate = parcel.readString()
        paymentDate = parcel.readString()
        renewalDate = parcel.readString()
        isActive = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        subscribeDate = parcel.readString()
        subscriptionTypeID = parcel.readValue(Int::class.java.classLoader) as? Int
        cost = parcel.readValue(Double::class.java.classLoader) as? Double
    }

    companion object CREATOR : Parcelable.Creator<SubScriptionResult> {
        override fun createFromParcel(parcel: Parcel): SubScriptionResult {
            return SubScriptionResult(parcel)
        }

        override fun newArray(size: Int): Array<SubScriptionResult?> {
            return arrayOfNulls(size)
        }
    }
}