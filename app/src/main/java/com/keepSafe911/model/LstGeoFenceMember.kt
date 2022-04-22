package com.keepSafe911.model

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity(tableName = "geoFenceMemberTable")
class LstGeoFenceMember() : Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeInt(geoMemberListID)
        dest?.writeInt(id)
        dest?.writeInt(memberID)
        dest?.writeInt(geoFenceID)
        dest?.writeByte(if (isStatus) 1 else 0)
        dest?.writeString(geoFenceTime)
        dest?.writeByte(if (memberStatus) 1 else 0)
        dest?.writeString(memberName)
        dest?.writeString(Image)
        dest?.writeByte(if (isSelected) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "geoMemberListID")
    var geoMemberListID: Int = 0

    @SerializedName("ID")
    @Expose
    var id: Int = 0
    @SerializedName("MemberID")
    @Expose
    var memberID: Int = 0
    @SerializedName("GeoFenceID")
    @Expose
    var geoFenceID: Int = 0
    @SerializedName("Status")
    @Expose
    var isStatus: Boolean = false
    @SerializedName("GeoFenceTime")
    @Expose
    var geoFenceTime: String? = ""
    @SerializedName("MemberStatus")
    @Expose
    var memberStatus: Boolean = false
    @Ignore
    @SerializedName("DeletedMembers")
    @Expose
    var deletedMembers: Any? = null
    @SerializedName("MemberName")
    @Expose
    var memberName: String? = ""
    @SerializedName("Image")
    @Expose
    var Image: String? = ""
    @ColumnInfo(name = "isSelected")
    var isSelected: Boolean = false

    constructor(parcel: Parcel) : this() {
        geoMemberListID = parcel.readInt()
        id = parcel.readInt()
        memberID = parcel.readInt()
        geoFenceID = parcel.readInt()
        isStatus = parcel.readByte() != 0.toByte()
        geoFenceTime = parcel.readString()
        memberStatus = parcel.readByte() != 0.toByte()
        memberName = parcel.readString()
        Image = parcel.readString()
        isSelected = parcel.readByte() != 0.toByte()
    }

    companion object CREATOR : Parcelable.Creator<LstGeoFenceMember> {
        override fun createFromParcel(parcel: Parcel): LstGeoFenceMember {
            return LstGeoFenceMember(parcel)
        }

        override fun newArray(size: Int): Array<LstGeoFenceMember?> {
            return arrayOfNulls(size)
        }

        fun create(serializedData: String): LstGeoFenceMember {
            // Use GSON to instantiate this class using the JSON representation of the state
            val gson = GsonBuilder().serializeNulls().create()
            return gson.fromJson<LstGeoFenceMember>(serializedData, LstGeoFenceMember::class.java)
        }
    }

}