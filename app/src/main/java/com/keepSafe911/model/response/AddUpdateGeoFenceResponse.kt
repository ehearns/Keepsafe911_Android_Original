package com.keepSafe911.model.response

import com.keepSafe911.model.GeoFenceResult
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class AddUpdateGeoFenceResponse {

    @SerializedName("Status")
    @Expose
    var isStatus: Boolean = false
    @SerializedName("Message")
    @Expose
    var message: String? = ""
    @SerializedName("ResponseMessage")
    @Expose
    var responseMessage: String? = ""


    @SerializedName("Result")
    @Expose
    var result: GeoFenceResult? = GeoFenceResult()

}