package com.keepSafe911.model.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class DeviceSubscriptionResponse {
    @SerializedName("Status")
    @Expose
    var isStatus: Boolean = false
    @SerializedName("Message")
    @Expose
    var message: String? = ""
    @SerializedName("Result")
    @Expose
    var result: DeviceSubscriptionResult? = DeviceSubscriptionResult()
    @SerializedName("ResponseMessage")
    @Expose
    var responseMessage: String? = ""
}