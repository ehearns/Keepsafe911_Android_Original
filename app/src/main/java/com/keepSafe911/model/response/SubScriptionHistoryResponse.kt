package com.keepSafe911.model.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class SubScriptionHistoryResponse {

    @SerializedName("Status")
    @Expose
    var isStatus: Boolean = false
    @SerializedName("Message")
    @Expose
    var message: String? = ""
    @SerializedName("Result")
    @Expose
    var result: ArrayList<SubScriptionResult>? = ArrayList()
    @SerializedName("ResponseMessage")
    @Expose
    var responseMessage: String? = ""

}