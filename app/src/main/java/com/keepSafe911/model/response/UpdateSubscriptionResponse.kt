package com.keepSafe911.model.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class UpdateSubscriptionResponse {
    @SerializedName("Status")
    @Expose
    var isStatus: Boolean = false

    @SerializedName("Message")
    @Expose
    var message: String? = ""

    @SerializedName("Result")
    @Expose
    var result: UpdateSubscriptionresult? = UpdateSubscriptionresult()

    @SerializedName("ResponseMessage")
    @Expose
    var responseMessage: String? = ""


}

class UpdateSubscriptionresult {

    @SerializedName("CardNumber")
    @Expose
    var CardNumber: String? = ""

    @SerializedName("FirstName")
    @Expose
    var FirstName: String? = ""

    @SerializedName("LastName")
    @Expose
    var LastName: String? = ""

    @SerializedName("iscancelled")
    @Expose
    var isCancelled: Boolean? = false

    override fun toString(): String {
        return "ClassPojo [CardNumber = $CardNumber, FirstName = $FirstName, LastName = $LastName, iscancelled = $isCancelled]"
    }

}


