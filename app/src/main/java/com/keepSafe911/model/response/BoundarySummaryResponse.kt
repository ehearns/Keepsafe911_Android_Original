package com.keepSafe911.model.response

import com.keepSafe911.model.BoundarySummaryResult
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class BoundarySummaryResponse {

    @SerializedName("Status")
    @Expose
    var isStatus: Boolean = false
    @SerializedName("Message")
    @Expose
    var message: String? = ""
    @SerializedName("Result")
    @Expose
    var result: ArrayList<BoundarySummaryResult>? = ArrayList()
    @SerializedName("ResponseMessage")
    @Expose
    var responseMessage: String? = ""
}