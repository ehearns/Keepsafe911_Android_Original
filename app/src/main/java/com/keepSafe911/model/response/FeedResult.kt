package com.keepSafe911.model.response

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

class FeedResult(): Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeValue(iD)
        dest?.writeString(title)
        dest?.writeString(feeds)
        dest?.writeString(type)
        dest?.writeString(location)
        dest?.writeValue(_lat)
        dest?.writeValue(_long)
        dest?.writeString(imageUrl)
        dest?.writeString(createdOn)
        dest?.writeValue(createdBy)
        dest?.writeValue(categoryID)
        dest?.writeValue(isDeleted)
        dest?.writeValue(fileType)
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
    @SerializedName("ImageUrl")
    @Expose
    var imageUrl: String? = ""
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
    @SerializedName("FileType")
    @Expose
    var fileType: Int? = 0
    @SerializedName("tblCategory")
    @Expose
    var tblCategory: Any? = null
    @SerializedName("tblFeedResponse")
    @Expose
    var tblFeedResponse: ArrayList<Any>? = null

    constructor(parcel: Parcel) : this() {
        iD = parcel.readValue(Int::class.java.classLoader) as? Int
        title = parcel.readString()
        feeds = parcel.readString()
        type = parcel.readString()
        location = parcel.readString()
        _lat = parcel.readValue(Double::class.java.classLoader) as? Double
        _long = parcel.readValue(Double::class.java.classLoader) as? Double
        imageUrl = parcel.readString()
        createdOn = parcel.readString()
        createdBy = parcel.readValue(Int::class.java.classLoader) as? Int
        categoryID = parcel.readValue(Int::class.java.classLoader) as? Int
        isDeleted = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        fileType = parcel.readValue(Int::class.java.classLoader) as? Int
    }

    companion object CREATOR : Parcelable.Creator<FeedResult> {
        override fun createFromParcel(parcel: Parcel): FeedResult {
            return FeedResult(parcel)
        }

        override fun newArray(size: Int): Array<FeedResult?> {
            return arrayOfNulls(size)
        }
    }

}