package com.keepSafe911.model.response.findmissingchild

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class MissingChildTaskListResult() : Parcelable {

    @SerializedName("Id")
    @Expose
    var id: Int? = 0

    @SerializedName("TaskName")
    @Expose
    var taskName: String? = ""

    @SerializedName("Type")
    @Expose
    var type: Int? = 0

    @SerializedName("Hint")
    @Expose
    var hint: String? = ""

    constructor(parcel: Parcel) : this() {
        id = parcel.readValue(Int::class.java.classLoader) as? Int
        taskName = parcel.readString()
        type = parcel.readValue(Int::class.java.classLoader) as? Int
        hint = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeString(taskName)
        parcel.writeValue(type)
        parcel.writeString(hint)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MissingChildTaskListResult> {
        override fun createFromParcel(parcel: Parcel): MissingChildTaskListResult {
            return MissingChildTaskListResult(parcel)
        }

        override fun newArray(size: Int): Array<MissingChildTaskListResult?> {
            return arrayOfNulls(size)
        }
    }

}