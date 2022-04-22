package com.keepSafe911.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class UpdateProfileResult {
    @SerializedName("Url")
    @Expose
    var url: String? = ""
}