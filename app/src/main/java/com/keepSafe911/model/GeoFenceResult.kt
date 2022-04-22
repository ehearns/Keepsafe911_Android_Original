package com.keepSafe911.model

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity(tableName = "geoFenceTable")
class GeoFenceResult() : Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeInt(geoID)
        dest?.writeInt(iD)
        dest?.writeString(geoFenceName)
        dest?.writeString(description)
        dest?.writeString(startDate)
        dest?.writeString(startTime)
        dest?.writeString(endDate)
        dest?.writeString(endTime)
        dest?.writeString(address)
        dest?.writeString(message)
        dest?.writeString(ex)
        dest?.writeInt(radius)
        dest?.writeDouble(latitude)
        dest?.writeDouble(longitude)
        dest?.writeByte(if (isActive) 1 else 0)
        dest?.writeInt(adminID)
        dest?.writeString(createdOn)
        dest?.writeByte(if (isSubmit) 1 else 0)
        dest?.writeTypedList(lstDeleteGeoFence)
        dest?.writeTypedList(lstGeoFenceMembers)
    }

    override fun describeContents(): Int {
        return 0
    }

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "geoID")
    var geoID: Int = 0

    @ColumnInfo(name = "ID")
    @SerializedName("ID")
    @Expose
    var iD: Int = 0
    @ColumnInfo(name = "GeoFenceName")
    @SerializedName("GeoFenceName")
    @Expose
    var geoFenceName: String? = ""
    @ColumnInfo(name = "Description")
    @SerializedName("Description")
    @Expose
    var description: String? = ""
    @ColumnInfo(name = "StartDate")
    @SerializedName("StartDate")
    @Expose
    var startDate: String? = ""
    @ColumnInfo(name = "StartTime")
    @SerializedName("StartTime")
    @Expose
    var startTime: String? = ""
    @ColumnInfo(name = "EndDate")
    @SerializedName("EndDate")
    @Expose
    var endDate: String? = ""
    @ColumnInfo(name = "EndTime")
    @SerializedName("EndTime")
    @Expose
    var endTime: String? = ""
    @ColumnInfo(name = "Address")
    @SerializedName("Address")
    @Expose
    var address: String? = ""
    @ColumnInfo(name = "Message")
    @SerializedName("Message")
    @Expose
    var message: String? = ""
    @ColumnInfo(name = "ex")
    @SerializedName("ex")
    @Expose
    var ex: String? = "Enter"
    @ColumnInfo(name = "Radius")
    @SerializedName("Radius")
    @Expose
    var radius: Int = 0
    @ColumnInfo(name = "Latitude")
    @SerializedName("Latitude")
    @Expose
    var latitude: Double = 0.toDouble()
    @ColumnInfo(name = "Longitude")
    @SerializedName("Longitude")
    @Expose
    var longitude: Double = 0.toDouble()
    @ColumnInfo(name = "IsActive")
    @SerializedName("IsActive")
    @Expose
    var isActive: Boolean = false
    @ColumnInfo(name = "AdminID")
    @SerializedName("AdminID")
    @Expose
    var adminID: Int = 0
    @ColumnInfo(name = "CreatedOn")
    @SerializedName("CreatedOn")
    @Expose
    var createdOn: String? = ""
    @ColumnInfo(name = "IsSubmit")
    @SerializedName("IsSubmit")
    @Expose
    var isSubmit: Boolean = false
    @SerializedName("lstDeleteGeoFence")
    @Expose
    var lstDeleteGeoFence: ArrayList<LstDeleteGeoFence>? = ArrayList()
    @SerializedName("lstGeoFenceMembers")
    @Expose
    var lstGeoFenceMembers: ArrayList<LstGeoFenceMember>? = ArrayList()

    constructor(parcel: Parcel) : this() {
        geoID = parcel.readInt()
        iD = parcel.readInt()
        geoFenceName = parcel.readString()
        description = parcel.readString()
        startDate = parcel.readString()
        startTime = parcel.readString()
        endDate = parcel.readString()
        endTime = parcel.readString()
        address = parcel.readString()
        message = parcel.readString()
        ex = parcel.readString()
        radius = parcel.readInt()
        latitude = parcel.readDouble()
        longitude = parcel.readDouble()
        isActive = parcel.readByte() != 0.toByte()
        adminID = parcel.readInt()
        createdOn = parcel.readString()
        isSubmit = parcel.readByte() != 0.toByte()
        lstDeleteGeoFence = parcel.createTypedArrayList(LstDeleteGeoFence)
        lstGeoFenceMembers = parcel.createTypedArrayList(LstGeoFenceMember)
    }

    companion object CREATOR : Parcelable.Creator<GeoFenceResult> {
        override fun createFromParcel(parcel: Parcel): GeoFenceResult {
            return GeoFenceResult(parcel)
        }

        override fun newArray(size: Int): Array<GeoFenceResult?> {
            return arrayOfNulls(size)
        }

        fun create(serializedData: String): GeoFenceResult {
            // Use GSON to instantiate this class using the JSON representation of the state
            val gson = GsonBuilder().serializeNulls().create()
            return gson.fromJson<GeoFenceResult>(serializedData, GeoFenceResult::class.java)
        }
    }
}