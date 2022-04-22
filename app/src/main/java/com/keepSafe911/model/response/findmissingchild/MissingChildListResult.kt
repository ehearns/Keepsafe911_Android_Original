package com.keepSafe911.model.response.findmissingchild

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList

class MissingChildListResult {
    @SerializedName("status")
    @Expose
    var status: Boolean? = false

    @SerializedName("message")
    @Expose
    var message: Any? = Any()

    @SerializedName("numberOfFacesIdentified")
    @Expose
    var numberOfFacesIdentified: Int? = 0

    @SerializedName("recordaddedindb")
    @Expose
    var recordaddedindb: Int? = 0

    @SerializedName("Matches")
    @Expose
    var matches: ArrayList<ArrayList<MatchResult>>? = ArrayList()
}