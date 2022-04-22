package com.keepSafe911.model

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull

@Entity(tableName = "placesToVisit")
class PlacesToVisitModel(): Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeInt(placesId)
        dest?.writeInt(memberId)
        dest?.writeString(placeName)
        dest?.writeString(placeDateTime)
        dest?.writeDouble(placeLatitude)
        dest?.writeDouble(placeLongitude)
        dest?.writeByte(if (placeForFuture) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    @PrimaryKey(autoGenerate = true)
    @NotNull
    var placesId: Int = 0
    var memberId: Int = 0
    var placeName: String = ""
    var placeDateTime: String = ""
    var placeLatitude: Double = 0.0
    var placeLongitude: Double = 0.0
    var placeForFuture: Boolean = false

    constructor(parcel: Parcel) : this() {
        placesId = parcel.readInt()
        memberId = parcel.readInt()
        placeName = parcel.readString() ?: ""
        placeDateTime = parcel.readString() ?: ""
        placeLatitude = parcel.readDouble()
        placeLongitude = parcel.readDouble()
        placeForFuture = parcel.readByte() != 0.toByte()
    }

    companion object CREATOR : Parcelable.Creator<PlacesToVisitModel> {
        override fun createFromParcel(parcel: Parcel): PlacesToVisitModel {
            return PlacesToVisitModel(parcel)
        }

        override fun newArray(size: Int): Array<PlacesToVisitModel?> {
            return arrayOfNulls(size)
        }
    }
}