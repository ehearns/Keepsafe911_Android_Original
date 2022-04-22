package com.keepSafe911.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * Created by Akash patel on 24-05-2017.
 */

class ResultRoute {

    @SerializedName("name")
    @Expose
    var name: String? = ""
    @SerializedName("c")
    @Expose
    var c: Any? = ""
    @SerializedName("i")
    @Expose
    var i: String? = ""
    @SerializedName("locs")
    @Expose
    var locs: ArrayList<Loc>? = ArrayList()

}
