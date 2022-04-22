package com.keepSafe911.model.response.yelp

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Business(): Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(id)
        dest?.writeString(alias)
        dest?.writeString(name)
        dest?.writeString(imageUrl)
        dest?.writeValue(isClosed)
        dest?.writeString(url)
        dest?.writeValue(reviewCount)
        dest?.writeTypedList(categories)
        dest?.writeValue(rating)
        dest?.writeParcelable(coordinates, flags)
        dest?.writeString(price)
        dest?.writeParcelable(location, flags)
        dest?.writeString(phone)
        dest?.writeString(displayPhone)
        dest?.writeValue(distance)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("id")
    @Expose
    var id: String? = ""
    @SerializedName("alias")
    @Expose
    var alias: String? = ""
    @SerializedName("name")
    @Expose
    var name: String? = ""
    @SerializedName("image_url")
    @Expose
    var imageUrl: String? = ""
    @SerializedName("is_closed")
    @Expose
    var isClosed: Boolean? = false
    @SerializedName("url")
    @Expose
    var url: String? = ""
    @SerializedName("review_count")
    @Expose
    var reviewCount: Int? = 0
    @SerializedName("categories")
    @Expose
    var categories: ArrayList<Category>? = ArrayList()
    @SerializedName("rating")
    @Expose
    var rating: Double? = 0.0
    @SerializedName("coordinates")
    @Expose
    var coordinates: Coordinates? = Coordinates()
    @SerializedName("transactions")
    @Expose
    var transactions: ArrayList<Any>? = ArrayList()
    @SerializedName("price")
    @Expose
    var price: String? = ""
    @SerializedName("location")
    @Expose
    var location: Location? = Location()
    @SerializedName("phone")
    @Expose
    var phone: String? = ""
    @SerializedName("display_phone")
    @Expose
    var displayPhone: String? = ""
    @SerializedName("distance")
    @Expose
    var distance: Double? = 0.0

    constructor(parcel: Parcel) : this() {
        id = parcel.readString()
        alias = parcel.readString()
        name = parcel.readString()
        imageUrl = parcel.readString()
        isClosed = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        url = parcel.readString()
        reviewCount = parcel.readValue(Int::class.java.classLoader) as? Int
        categories = parcel.createTypedArrayList(Category)
        rating = parcel.readValue(Double::class.java.classLoader) as? Double
        coordinates = parcel.readParcelable(Coordinates::class.java.classLoader)
        price = parcel.readString()
        location = parcel.readParcelable(Location::class.java.classLoader)
        phone = parcel.readString()
        displayPhone = parcel.readString()
        distance = parcel.readValue(Double::class.java.classLoader) as? Double
    }

    companion object CREATOR : Parcelable.Creator<Business> {
        override fun createFromParcel(parcel: Parcel): Business {
            return Business(parcel)
        }

        override fun newArray(size: Int): Array<Business?> {
            return arrayOfNulls(size)
        }
    }

}
