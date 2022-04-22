package com.keepSafe911.model.response

import com.keepSafe911.model.ResultRoute
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlin.collections.ArrayList

class MemberRouteResponse {

    @SerializedName("Status")
    @Expose
    var status: Boolean? = false
    @SerializedName("Message")
    @Expose
    var message: String? = ""
    @SerializedName("Result")
    @Expose
    var result: ArrayList<ResultRoute>? = ArrayList()
    @SerializedName("ResponseMessage")
    @Expose
    var responseMessage: String? = ""
}
