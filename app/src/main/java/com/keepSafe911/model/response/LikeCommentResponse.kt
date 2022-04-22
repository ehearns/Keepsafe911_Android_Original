package com.keepSafe911.model.response

import com.keepSafe911.model.LikeCommentResult
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class LikeCommentResponse {
    @SerializedName("Status")
    @Expose
    var status: Boolean? = false
    @SerializedName("Message")
    @Expose
    var message: String? = ""
    @SerializedName("Result")
    @Expose
    var result: LikeCommentResult?  = LikeCommentResult()
    @SerializedName("ResponseMessage")
    @Expose
    var responseMessage: String? = ""
}