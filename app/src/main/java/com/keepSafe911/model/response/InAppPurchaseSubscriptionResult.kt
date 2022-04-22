package com.keepSafe911.model.response

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class InAppPurchaseSubscriptionResult() : Parcelable {
    @SerializedName("quantity")
    @Expose
    var quantity: String? = ""

    @SerializedName("product_id")
    @Expose
    var productId: String? = ""

    @SerializedName("transaction_id")
    @Expose
    var transactionId: String? = ""

    @SerializedName("original_transaction_id")
    @Expose
    var originalTransactionId: String? = ""

    @SerializedName("purchase_date")
    @Expose
    var purchaseDate: String? = ""

    @SerializedName("purchase_date_ms")
    @Expose
    var purchaseDateMs: String? = ""

    @SerializedName("purchase_date_pst")
    @Expose
    var purchaseDatePst: String? = ""

    @SerializedName("original_purchase_date")
    @Expose
    var originalPurchaseDate: String? = ""

    @SerializedName("original_purchase_date_ms")
    @Expose
    var originalPurchaseDateMs: String? = ""

    @SerializedName("original_purchase_date_pst")
    @Expose
    var originalPurchaseDatePst: String? = ""

    @SerializedName("expires_date")
    @Expose
    var expiresDate: String? = ""

    @SerializedName("expires_date_ms")
    @Expose
    var expiresDateMs: String? = ""

    @SerializedName("expires_date_pst")
    @Expose
    var expiresDatePst: String? = ""

    @SerializedName("web_order_line_item_id")
    @Expose
    var webOrderLineItemId: String? = ""

    @SerializedName("is_trial_period")
    @Expose
    var isTrialPeriod: String? = ""

    @SerializedName("is_in_intro_offer_period")
    @Expose
    var isInIntroOfferPeriod: String? = ""

    constructor(parcel: Parcel) : this() {
        quantity = parcel.readString()
        productId = parcel.readString()
        transactionId = parcel.readString()
        originalTransactionId = parcel.readString()
        purchaseDate = parcel.readString()
        purchaseDateMs = parcel.readString()
        purchaseDatePst = parcel.readString()
        originalPurchaseDate = parcel.readString()
        originalPurchaseDateMs = parcel.readString()
        originalPurchaseDatePst = parcel.readString()
        expiresDate = parcel.readString()
        expiresDateMs = parcel.readString()
        expiresDatePst = parcel.readString()
        webOrderLineItemId = parcel.readString()
        isTrialPeriod = parcel.readString()
        isInIntroOfferPeriod = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(quantity)
        parcel.writeString(productId)
        parcel.writeString(transactionId)
        parcel.writeString(originalTransactionId)
        parcel.writeString(purchaseDate)
        parcel.writeString(purchaseDateMs)
        parcel.writeString(purchaseDatePst)
        parcel.writeString(originalPurchaseDate)
        parcel.writeString(originalPurchaseDateMs)
        parcel.writeString(originalPurchaseDatePst)
        parcel.writeString(expiresDate)
        parcel.writeString(expiresDateMs)
        parcel.writeString(expiresDatePst)
        parcel.writeString(webOrderLineItemId)
        parcel.writeString(isTrialPeriod)
        parcel.writeString(isInIntroOfferPeriod)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<InAppPurchaseSubscriptionResult> {
        override fun createFromParcel(parcel: Parcel): InAppPurchaseSubscriptionResult {
            return InAppPurchaseSubscriptionResult(parcel)
        }

        override fun newArray(size: Int): Array<InAppPurchaseSubscriptionResult?> {
            return arrayOfNulls(size)
        }
    }

}