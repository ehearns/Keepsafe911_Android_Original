package com.keepSafe911.room.databasetable

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.GsonBuilder

@Entity(tableName = "geoFenceNotification")
class GeoFenceNotification() : Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeInt(notifyID)
        dest?.writeInt(geoNotifyID)
        dest?.writeByte(if (notifyStatus) 1 else 0)
        dest?.writeString(notifyMessage)
        dest?.writeString(notifyTime)
        dest?.writeInt(notifyMemberID)
        dest?.writeString(notifyCreateOn)
        dest?.writeValue(notifyLat)
        dest?.writeValue(notifyLong)
    }

    override fun describeContents(): Int {
        return 0
    }

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "notifyID")
    var notifyID: Int = 0

    @ColumnInfo(name = "geoNotifyID")
    var geoNotifyID: Int = 0
    @ColumnInfo(name = "notifyStatus")
    var notifyStatus: Boolean = false
    @ColumnInfo(name = "notifyMessage")
    var notifyMessage: String? = ""
    @ColumnInfo(name = "notifyTime")
    var notifyTime: String? = ""
    @ColumnInfo(name = "notifyMemberID")
    var notifyMemberID: Int = 0
    @ColumnInfo(name = "notifyCreateOn")
    var notifyCreateOn: String? = ""
    @ColumnInfo(name = "notifyLat")
    var notifyLat: Double? = 0.0
    @ColumnInfo(name = "notifyLong")
    var notifyLong: Double? = 0.0


    constructor(parcel: Parcel) : this() {
        notifyID = parcel.readInt()
        geoNotifyID = parcel.readInt()
        notifyStatus = parcel.readByte() != 0.toByte()
        notifyMessage = parcel.readString()
        notifyTime = parcel.readString()
        notifyMemberID = parcel.readInt()
        notifyCreateOn = parcel.readString()
        notifyLat = parcel.readValue(Double::class.java.classLoader) as? Double
        notifyLong = parcel.readValue(Double::class.java.classLoader) as? Double
    }

    companion object CREATOR : Parcelable.Creator<GeoFenceNotification> {
        override fun createFromParcel(parcel: Parcel): GeoFenceNotification {
            return GeoFenceNotification(parcel)
        }

        override fun newArray(size: Int): Array<GeoFenceNotification?> {
            return arrayOfNulls(size)
        }

        fun create(serializedData: String): GeoFenceNotification {
            // Use GSON to instantiate this class using the JSON representation of the state
            val gson = GsonBuilder().serializeNulls().create()
            return gson.fromJson<GeoFenceNotification>(serializedData, GeoFenceNotification::class.java)
        }
    }
}