package com.keepSafe911.model.response

import com.keepSafe911.model.GeoFenceResult
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*

class GeoFenceListResponse {

    @SerializedName("Status")
    @Expose
    var isStatus: Boolean = false
    @SerializedName("Message")
    @Expose
    var message: String? = ""
    @SerializedName("Result")
    @Expose
    var result: ArrayList<GeoFenceResult>? = ArrayList()
    @SerializedName("ResponseMessage")
    @Expose
    var responseMessage: String? = ""

}