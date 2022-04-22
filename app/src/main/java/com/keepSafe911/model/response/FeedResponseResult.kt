package com.keepSafe911.model.response

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class FeedResponseResult(): Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeValue(iD)
        dest?.writeString(title)
        dest?.writeString(feeds)
        dest?.writeString(type)
        dest?.writeString(location)
        dest?.writeValue(_lat)
        dest?.writeValue(_long)
        dest?.writeString(file)
        dest?.writeString(createdOn)
        dest?.writeValue(createdBy)
        dest?.writeValue(categoryID)
        dest?.writeValue(isDeleted)
        dest?.writeString(categoryName)
        dest?.writeString(addedBy)
        dest?.writeValue(fileType)
        dest?.writeString(userImage)
        dest?.writeTypedList(lstOfFeedLikeOrComments)
        dest?.writeValue(isLiked)
        dest?.writeValue(likeCount)
        dest?.writeValue(isCommented)
        dest?.writeValue(commentCount)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("ID")
    @Expose
    var iD: Int? = 0
    @SerializedName("Title")
    @Expose
    var title: String? = ""
    @SerializedName("Feeds")
    @Expose
    var feeds: String? = ""
    @SerializedName("Type")
    @Expose
    var type: String? = ""
    @SerializedName("Location")
    @Expose
    var location: String? = ""
    @SerializedName("Lat")
    @Expose
    var _lat: Double? = 0.0
    @SerializedName("Long")
    @Expose
    var _long: Double? = 0.0
    @SerializedName("File")
    @Expose
    var file: String? = ""
    @SerializedName("CreatedOn")
    @Expose
    var createdOn: String? = ""
    @SerializedName("CreatedBy")
    @Expose
    var createdBy: Int? = 0
    @SerializedName("CategoryID")
    @Expose
    var categoryID: Int? = 0
    @SerializedName("IsDeleted")
    @Expose
    var isDeleted: Boolean? = false
    @SerializedName("CategoryName")
    @Expose
    var categoryName: String? = ""
    @SerializedName("AddedBy")
    @Expose
    var addedBy: String? = ""
    @SerializedName("FileType")
    @Expose
    var fileType: Int? = 0
    @SerializedName("UserImage")
    @Expose
    var userImage: String? = ""
    @SerializedName("lstOfFeedLikeOrComments")
    @Expose
    var lstOfFeedLikeOrComments: ArrayList<LstOfFeedLikeOrComment>? = ArrayList()

    var isLiked: Boolean? = false
    var likeCount: Int? = 0
    var isCommented: Boolean? = false
    var commentCount: Int? = 0

    constructor(parcel: Parcel) : this() {
        iD = parcel.readValue(Int::class.java.classLoader) as? Int
        title = parcel.readString()
        feeds = parcel.readString()
        type = parcel.readString()
        location = parcel.readString()
        _lat = parcel.readValue(Double::class.java.classLoader) as? Double
        _long = parcel.readValue(Double::class.java.classLoader) as? Double
        file = parcel.readString()
        createdOn = parcel.readString()
        createdBy = parcel.readValue(Int::class.java.classLoader) as? Int
        categoryID = parcel.readValue(Int::class.java.classLoader) as? Int
        isDeleted = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        categoryName = parcel.readString()
        addedBy = parcel.readString()
        fileType = parcel.readValue(Int::class.java.classLoader) as? Int
        userImage = parcel.readString()
        lstOfFeedLikeOrComments = parcel.createTypedArrayList(LstOfFeedLikeOrComment)
        isLiked = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        likeCount = parcel.readValue(Int::class.java.classLoader) as? Int
        isCommented = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        commentCount = parcel.readValue(Int::class.java.classLoader) as? Int
    }

    companion object CREATOR : Parcelable.Creator<FeedResponseResult> {
        override fun createFromParcel(parcel: Parcel): FeedResponseResult {
            return FeedResponseResult(parcel)
        }

        override fun newArray(size: Int): Array<FeedResponseResult?> {
            return arrayOfNulls(size)
        }
    }
}