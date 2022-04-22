package com.keepSafe911.model.response.findmissingchild

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*

class RunSearchResponse {
    @SerializedName("numberOfFacesIdentified")
    @Expose
    var numberOfFacesIdentified: Int? = 0

    @SerializedName("Matches")
    @Expose
    var matches: ArrayList<ArrayList<MatchResult>>? = ArrayList()
}