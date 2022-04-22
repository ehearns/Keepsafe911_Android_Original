package com.keepSafe911.model.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class InAppPurchaseSubscriptionResponse {
    @SerializedName("Status")
    @Expose
    var isStatus: Boolean = false
    @SerializedName("Message")
    @Expose
    var message: String? = ""
    @SerializedName("Result")
    @Expose
    var result: InAppPurchaseSingleResult? = InAppPurchaseSingleResult()
    @SerializedName("ResponseMessage")
    @Expose
    var responseMessage: String? = ""

    inner class InAppPurchaseSingleResult {
        @SerializedName("result")
        @Expose
        var result: ArrayList<InAppPurchaseSubscriptionResult>? = ArrayList()
    }
}