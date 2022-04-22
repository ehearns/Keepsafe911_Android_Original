package com.keepSafe911.model.response
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


class PlacesResult() : Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeValue(iD)
        dest?.writeValue(userId)
        dest?.writeValue(latitude)
        dest?.writeValue(longitude)
        dest?.writeString(address)
        dest?.writeString(visitDate)
        dest?.writeValue(createdBy)
        dest?.writeValue(isDeleted)
        dest?.writeString(message)
        dest?.writeValue(isVisited)
        dest?.writeValue(rating)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("ID")
    @Expose
    var iD: Int? = 0
    @SerializedName("UserId")
    @Expose
    var userId: Int? = 0
    @SerializedName("Latitude")
    @Expose
    var latitude: Double? = 0.0
    @SerializedName("Longitude")
    @Expose
    var longitude: Double? = 0.0
    @SerializedName("Address")
    @Expose
    var address: String? = ""
    @SerializedName("VisitDate")
    @Expose
    var visitDate: String? = ""
    @SerializedName("CreatedBy")
    @Expose
    var createdBy: Int? = 0
    @SerializedName("IsDeleted")
    @Expose
    var isDeleted: Boolean? = false
    @SerializedName("Message")
    @Expose
    var message: String? = ""
    @SerializedName("IsVisited")
    @Expose
    var isVisited: Boolean? = false
    @SerializedName("Rating")
    @Expose
    var rating: Double? = 0.0

    constructor(parcel: Parcel) : this() {
        iD = parcel.readValue(Int::class.java.classLoader) as? Int
        userId = parcel.readValue(Int::class.java.classLoader) as? Int
        latitude = parcel.readValue(Double::class.java.classLoader) as? Double
        longitude = parcel.readValue(Double::class.java.classLoader) as? Double
        address = parcel.readString()
        visitDate = parcel.readString()
        createdBy = parcel.readValue(Int::class.java.classLoader) as? Int
        isDeleted = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        message = parcel.readString()
        isVisited = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        rating = parcel.readValue(Double::class.java.classLoader) as? Double
    }

    companion object CREATOR : Parcelable.Creator<PlacesResult> {
        override fun createFromParcel(parcel: Parcel): PlacesResult {
            return PlacesResult(parcel)
        }

        override fun newArray(size: Int): Array<PlacesResult?> {
            return arrayOfNulls(size)
        }
    }

}