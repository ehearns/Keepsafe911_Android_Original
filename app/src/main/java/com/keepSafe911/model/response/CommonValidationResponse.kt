package com.keepSafe911.model.response

import com.google.gson.annotations.SerializedName

data class CommonValidationResponse(
    @SerializedName("Status") val status: Boolean? = false,
    @SerializedName("Message") val message: String? = "",
    @SerializedName("Result") val result: Any,
    @SerializedName("ResponseMessage") val responseMessage: String? = ""
)