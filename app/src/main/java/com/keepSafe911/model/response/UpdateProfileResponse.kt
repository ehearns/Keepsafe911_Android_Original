package com.keepSafe911.model.response

import com.keepSafe911.model.UpdateProfileResult
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class UpdateProfileResponse {

    @SerializedName("Status")
    @Expose
    var isStatus: Boolean = false
    @SerializedName("Message")
    @Expose
    var message: String? = ""
    @SerializedName("Result")
    @Expose
    var result: UpdateProfileResult? = UpdateProfileResult()
    @SerializedName("ResponseMessage")
    @Expose
    var responseMessage: String? = ""

}