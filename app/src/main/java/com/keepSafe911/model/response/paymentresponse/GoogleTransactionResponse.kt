package com.keepSafe911.model.response.paymentresponse

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class GoogleTransactionResponse {

    @SerializedName("transactionResponse")
    @Expose
    var transactionResponse: GoogleTransactionData? = GoogleTransactionData()
    @SerializedName("refId")
    @Expose
    var refId: String? = ""
    @SerializedName("messages")
    @Expose
    var messages: Messages? = Messages()

}