package com.keepSafe911.model

import android.os.Parcel
import android.os.Parcelable

class IncidentType(): Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeInt(incidentID)
        dest?.writeInt(incidentColor)
        dest?.writeString(incidentText)
        dest?.writeByte(if (incidentSelected) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    var incidentID: Int = 0
    var incidentColor: Int = 0
    var incidentText: String = ""
    var incidentSelected: Boolean = false

    constructor(parcel: Parcel) : this() {
        incidentID = parcel.readInt()
        incidentColor = parcel.readInt()
        incidentText = parcel.readString() ?: ""
        incidentSelected = parcel.readByte() != 0.toByte()
    }

    constructor(id: Int,i: Int, s: String, b: Boolean): this(){
        this.incidentID = id
        this.incidentColor = i
        this.incidentText = s
        this.incidentSelected = b
    }

    companion object CREATOR : Parcelable.Creator<IncidentType> {
        override fun createFromParcel(parcel: Parcel): IncidentType {
            return IncidentType(parcel)
        }

        override fun newArray(size: Int): Array<IncidentType?> {
            return arrayOfNulls(size)
        }
    }
}