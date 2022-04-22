package com.keepSafe911.model.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class VerifyResponse {
    @SerializedName("Status")
    @Expose
    var status: Boolean = false

    @SerializedName("Message")
    @Expose
    var message: String? = ""

    @SerializedName("Result")
    @Expose
    var result: VerifyObject? = VerifyObject()

    @SerializedName("ResponseMessage")
    @Expose
    var responseMessage: String? = ""
}


class VerifyObject {
    @SerializedName("Status")
    @Expose
    var status: Boolean = false

    @SerializedName("Email")
    @Expose
    var Email: String? = ""

    @SerializedName("Message")
    @Expose
    var Message: String? = ""

    @SerializedName("Username")
    @Expose
    var Username: String? = ""

    @SerializedName("Mobile")
    @Expose
    var Mobile: String? = ""

}
