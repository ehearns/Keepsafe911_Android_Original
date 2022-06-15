package com.keepSafe911.model.response.paypal

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlin.collections.ArrayList

class BillingInfo(): Parcelable {
    @SerializedName("outstanding_balance")
    @Expose
    var outstandingBalance: SubscriptionAmount? = SubscriptionAmount()

    @SerializedName("cycle_executions")
    @Expose
    var cycleExecutions: ArrayList<CycleExecution>? = ArrayList()

    @SerializedName("last_payment")
    @Expose
    var lastPayment: LastPayment? = LastPayment()

    @SerializedName("next_billing_time")
    @Expose
    var nextBillingTime: String? = ""

    @SerializedName("failed_payments_count")
    @Expose
    var failedPaymentsCount: Int? = 0

    constructor(parcel: Parcel) : this() {
        outstandingBalance = parcel.readParcelable(SubscriptionAmount::class.java.classLoader)
        cycleExecutions = parcel.createTypedArrayList(CycleExecution) ?: ArrayList()
        lastPayment = parcel.readParcelable(LastPayment::class.java.classLoader)
        nextBillingTime = parcel.readString()
        failedPaymentsCount = parcel.readValue(Int::class.java.classLoader) as? Int
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(outstandingBalance, flags)
        parcel.writeTypedList(cycleExecutions)
        parcel.writeParcelable(lastPayment, flags)
        parcel.writeString(nextBillingTime)
        parcel.writeValue(failedPaymentsCount)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BillingInfo> {
        override fun createFromParcel(parcel: Parcel): BillingInfo {
            return BillingInfo(parcel)
        }

        override fun newArray(size: Int): Array<BillingInfo?> {
            return arrayOfNulls(size)
        }
    }
}