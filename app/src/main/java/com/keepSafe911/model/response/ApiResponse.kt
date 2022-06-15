package com.keepSafe911.model.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ApiResponse(
    @SerializedName("Status")
    @Expose
    val status: Boolean,
    @SerializedName("Message")
    @Expose
    val message: String,
    @SerializedName("Result")
    @Expose
    val result: String,
    @SerializedName("ResponseMessage")
    @Expose
    val responseMessage: String
)