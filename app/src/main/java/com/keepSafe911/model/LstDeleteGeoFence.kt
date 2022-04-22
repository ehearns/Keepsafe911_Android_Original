package com.keepSafe911.model

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity(tableName = "deleteMemberTable")
class LstDeleteGeoFence() : Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeInt(deleteMemberID)
        dest?.writeInt(memberID)
    }

    override fun describeContents(): Int {
        return 0
    }

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "deleteMemberID")
    var deleteMemberID: Int = 0

    @SerializedName("MemberID")
    @Expose
    var memberID: Int = 0

    constructor(parcel: Parcel) : this() {
        deleteMemberID = parcel.readInt()
        memberID = parcel.readInt()
    }

    companion object CREATOR : Parcelable.Creator<LstDeleteGeoFence> {
        override fun createFromParcel(parcel: Parcel): LstDeleteGeoFence {
            return LstDeleteGeoFence(parcel)
        }

        override fun newArray(size: Int): Array<LstDeleteGeoFence?> {
            return arrayOfNulls(size)
        }

        fun create(serializedData: String): LstDeleteGeoFence {
            // Use GSON to instantiate this class using the JSON representation of the state
            val gson = GsonBuilder().serializeNulls().create()
            return gson.fromJson<LstDeleteGeoFence>(serializedData, LstDeleteGeoFence::class.java)
        }
    }

}