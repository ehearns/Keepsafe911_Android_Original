package com.keepSafe911.model.response.voicerecognition

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity(tableName = "phrasesTable")
class ManageVoiceRecognitionModel() : Parcelable {
    @PrimaryKey
    @SerializedName("Id")
    @Expose
    var id: Int? = 0

    @SerializedName("UserId")
    @Expose
    var userId: Int? = 0

    @SerializedName("VoiceText")
    @Expose
    var voiceText: String? = ""

    @SerializedName("CreatedOn")
    @Expose
    var createdOn: String? = ""

    constructor(parcel: Parcel) : this() {
        id = parcel.readInt()
        userId = parcel.readInt()
        voiceText = parcel.readString()
        createdOn = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id ?: 0)
        parcel.writeInt(userId ?: 0)
        parcel.writeString(voiceText)
        parcel.writeString(createdOn)
    }

    override fun toString(): String {
        return voiceText ?: ""
    }
    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ManageVoiceRecognitionModel> {
        override fun createFromParcel(parcel: Parcel): ManageVoiceRecognitionModel {
            return ManageVoiceRecognitionModel(parcel)
        }

        override fun newArray(size: Int): Array<ManageVoiceRecognitionModel?> {
            return arrayOfNulls(size)
        }
    }
}