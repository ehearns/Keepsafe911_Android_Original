package com.keepSafe911.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

class LikeCommentResult(): Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeValue(iD)
        dest?.writeValue(type)
        dest?.writeString(comments)
        dest?.writeString(date)
        dest?.writeValue(feedID)
        dest?.writeValue(isDeleted)
        dest?.writeValue(responseBy)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("ID")
    @Expose
    var iD: Int? = 0
    @SerializedName("Type")
    @Expose
    var type: Int? = 0
    @SerializedName("Comments")
    @Expose
    var comments: String? = ""
    @SerializedName("Date")
    @Expose
    var date: String? = ""
    @SerializedName("FeedID")
    @Expose
    var feedID: Int? = 0
    @SerializedName("IsDeleted")
    @Expose
    var isDeleted: Boolean? = false
    @SerializedName("ResponseBy")
    @Expose
    var responseBy: Int? = 0
    @SerializedName("tblFamilyMonitoringUser")
    @Expose
    var tblFamilyMonitoringUser: Any? = null
    @SerializedName("tblFeed")
    @Expose
    var tblFeed: Any? = null

    constructor(parcel: Parcel) : this() {
        iD = parcel.readValue(Int::class.java.classLoader) as? Int
        type = parcel.readValue(Int::class.java.classLoader) as? Int
        comments = parcel.readString()
        date = parcel.readString()
        feedID = parcel.readValue(Int::class.java.classLoader) as? Int
        isDeleted = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        responseBy = parcel.readValue(Int::class.java.classLoader) as? Int
    }

    companion object CREATOR : Parcelable.Creator<LikeCommentResult> {
        override fun createFromParcel(parcel: Parcel): LikeCommentResult {
            return LikeCommentResult(parcel)
        }

        override fun newArray(size: Int): Array<LikeCommentResult?> {
            return arrayOfNulls(size)
        }
    }
}