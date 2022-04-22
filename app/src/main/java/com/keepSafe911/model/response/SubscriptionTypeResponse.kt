package com.keepSafe911.model.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

import java.util.ArrayList

class SubscriptionTypeResponse {

    @SerializedName("Status")
    @Expose
    var status: Boolean? = false
    @SerializedName("Message")
    @Expose
    var message: String? = ""
    @SerializedName("Result")
    @Expose
    var result: ArrayList<SubscriptionTypeResult>? = ArrayList()
    @SerializedName("ResponseMessage")
    @Expose
    var responseMessage: String? = ""
}