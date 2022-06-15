package com.keepSafe911.model.response.paypal

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class PaypalResponse {
    @SerializedName("Success")
    @Expose
    var success: Boolean? = false
    @SerializedName("Message")
    @Expose
    var message: String? = ""
    @SerializedName("Data")
    @Expose
    var data: Any? = Any()
}