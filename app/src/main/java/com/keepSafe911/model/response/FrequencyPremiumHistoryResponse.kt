package com.keepSafe911.model.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class FrequencyPremiumHistoryResponse {
    @SerializedName("Status")
    @Expose
    var status: Boolean? = false
    @SerializedName("Message")
    @Expose
    var message: String? = ""
    @SerializedName("Result")
    @Expose
    var frequencyHistoryResult: ArrayList<FrequencyHistoryResult>? = ArrayList()
    @SerializedName("ResponseMessage")
    @Expose
    var responseMessage: String? = ""
}