package com.keepSafe911.model.response.paypal

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlin.collections.ArrayList

class SubscriptionTransactionResponse(): Parcelable {
    @SerializedName("transactions")
    @Expose
    var transactions: ArrayList<Transaction>? = ArrayList()

    @SerializedName("links")
    @Expose
    var links: ArrayList<Link>? = ArrayList()

    constructor(parcel: Parcel) : this() {
        transactions = parcel.createTypedArrayList(Transaction.CREATOR) ?: ArrayList()
        links = parcel.createTypedArrayList(Link) ?: ArrayList()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeTypedList(transactions)
        parcel.writeTypedList(links)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SubscriptionTransactionResponse> {
        override fun createFromParcel(parcel: Parcel): SubscriptionTransactionResponse {
            return SubscriptionTransactionResponse(parcel)
        }

        override fun newArray(size: Int): Array<SubscriptionTransactionResponse?> {
            return arrayOfNulls(size)
        }
    }
}