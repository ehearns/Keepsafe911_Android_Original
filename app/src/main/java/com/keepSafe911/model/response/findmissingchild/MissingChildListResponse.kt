package com.keepSafe911.model.response.findmissingchild

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

class MissingChildListResponse {
    @SerializedName("Status")
    @Expose
    var status: Boolean? = false

    @SerializedName("Message")
    @Expose
    var message: String? = ""

    @SerializedName("Result")
    @Expose
    var result: MissingChildListResult? = MissingChildListResult()

    @SerializedName("ResponseMessage")
    @Expose
    var responseMessage: String? = ""
}