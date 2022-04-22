package com.keepSafe911.model.response.yelp

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class YelpResponse {

    @SerializedName("businesses")
    @Expose
    var businesses: ArrayList<Business>? = ArrayList()
    @SerializedName("total")
    @Expose
    var total: Int? = 0
    @SerializedName("region")
    @Expose
    var region: Region? = Region()

}
