package com.keepSafe911.model.response.paymentresponse

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class PaymentResponse {

    @SerializedName("transactionResponse")
    @Expose
    var transactionResponse: TransactionResponse? = TransactionResponse()
    @SerializedName("refId")
    @Expose
    var refId: String? = ""
    @SerializedName("messages")
    @Expose
    var messages: Messages? = Messages()
}