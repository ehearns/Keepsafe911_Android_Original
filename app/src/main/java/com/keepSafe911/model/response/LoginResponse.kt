package com.keepSafe911.model.response

import com.keepSafe911.model.roomobj.LoginObject
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class LoginResponse {

    @SerializedName("Status")
    @Expose
    var status: Boolean = false

    @SerializedName("Message")
    @Expose
    var message: String? = ""

    @SerializedName("Result")
    @Expose
    var result: LoginObject? = LoginObject()

    @SerializedName("ResponseMessage")
    @Expose
    var responseMessage: String? = ""
}