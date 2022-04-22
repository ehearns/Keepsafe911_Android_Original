package com.keepSafe911.model.response

import com.google.gson.annotations.SerializedName

data class ApiResponse(
    @SerializedName("Status") val status: Boolean,
    @SerializedName("Message") val message: String,
    @SerializedName("Result") val result: String,
    @SerializedName("ResponseMessage") val responseMessage: String
)