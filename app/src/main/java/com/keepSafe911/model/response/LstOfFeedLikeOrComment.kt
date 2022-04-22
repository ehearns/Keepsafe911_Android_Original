package com.keepSafe911.model.response

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class LstOfFeedLikeOrComment(): Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeValue(id)
        dest?.writeValue(feedType)
        dest?.writeString(comments)
        dest?.writeValue(responseBy)
        dest?.writeString(date)
        dest?.writeValue(feedID)
        dest?.writeValue(isDeleted)
        dest?.writeString(name)
        dest?.writeString(profileUrl)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("ID")
    @Expose
    var id: Int? = 0
    @SerializedName("FeedType")
    @Expose
    var feedType: Int? = 0
    @SerializedName("Comments")
    @Expose
    var comments: String? = ""
    @SerializedName("ResponseBy")
    @Expose
    var responseBy: Int? = 0
    @SerializedName("Date")
    @Expose
    var date: String? = ""
    @SerializedName("FeedID")
    @Expose
    var feedID: Int? = 0
    @SerializedName("IsDeleted")
    @Expose
    var isDeleted: Boolean? = false
    @SerializedName("Name")
    @Expose
    var name: String? = ""
    @SerializedName("ProfileUrl")
    @Expose
    var profileUrl: String? = ""

    constructor(parcel: Parcel) : this() {
        id = parcel.readValue(Int::class.java.classLoader) as? Int
        feedType = parcel.readValue(Int::class.java.classLoader) as? Int
        comments = parcel.readString()
        responseBy = parcel.readValue(Int::class.java.classLoader) as? Int
        date = parcel.readString()
        feedID = parcel.readValue(Int::class.java.classLoader) as? Int
        isDeleted = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        name = parcel.readString()
        profileUrl = parcel.readString()
    }

    companion object CREATOR : Parcelable.Creator<LstOfFeedLikeOrComment> {
        override fun createFromParcel(parcel: Parcel): LstOfFeedLikeOrComment {
            return LstOfFeedLikeOrComment(parcel)
        }

        override fun newArray(size: Int): Array<LstOfFeedLikeOrComment?> {
            return arrayOfNulls(size)
        }
    }

}