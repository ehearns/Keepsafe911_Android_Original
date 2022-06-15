package com.keepSafe911.model.response.paypal

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class SubscriptionResponse(): Parcelable {
    @SerializedName("id")
    @Expose
    var id: String? = ""

    @SerializedName("plan_id")
    @Expose
    var planId: String? = ""

    @SerializedName("plan_overridden")
    @Expose
    var planOverridden: Boolean? = false

    @SerializedName("start_time")
    @Expose
    var startTime: String? = ""

    @SerializedName("quantity")
    @Expose
    var quantity: String? = ""

    @SerializedName("shipping_amount")
    @Expose
    var shippingAmount: SubscriptionAmount? = SubscriptionAmount()

    @SerializedName("subscriber")
    @Expose
    var subscriber: Subscriber? = Subscriber()

    @SerializedName("billing_info")
    @Expose
    var billingInfo: BillingInfo? = BillingInfo()

    @SerializedName("create_time")
    @Expose
    var createTime: String? = ""

    @SerializedName("update_time")
    @Expose
    var updateTime: String? = ""

    @SerializedName("links")
    @Expose
    var links: ArrayList<Link>? = ArrayList()

    @SerializedName("status")
    @Expose
    var status: String? = ""

    @SerializedName("status_update_time")
    @Expose
    var statusUpdateTime: String? = ""

    constructor(parcel: Parcel) : this() {
        id = parcel.readString()
        planId = parcel.readString()
        planOverridden = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        startTime = parcel.readString()
        quantity = parcel.readString()
        shippingAmount = parcel.readParcelable(SubscriptionAmount::class.java.classLoader)
        subscriber = parcel.readParcelable(Subscriber::class.java.classLoader)
        billingInfo = parcel.readParcelable(BillingInfo::class.java.classLoader)
        createTime = parcel.readString()
        updateTime = parcel.readString()
        links = parcel.createTypedArrayList(Link) ?: ArrayList()
        status = parcel.readString()
        statusUpdateTime = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(planId)
        parcel.writeValue(planOverridden)
        parcel.writeString(startTime)
        parcel.writeString(quantity)
        parcel.writeParcelable(shippingAmount, flags)
        parcel.writeParcelable(subscriber, flags)
        parcel.writeParcelable(billingInfo, flags)
        parcel.writeString(createTime)
        parcel.writeString(updateTime)
        parcel.writeTypedList(links)
        parcel.writeString(status)
        parcel.writeString(statusUpdateTime)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SubscriptionResponse> {
        override fun createFromParcel(parcel: Parcel): SubscriptionResponse {
            return SubscriptionResponse(parcel)
        }

        override fun newArray(size: Int): Array<SubscriptionResponse?> {
            return arrayOfNulls(size)
        }
    }
}