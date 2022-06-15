package com.keepSafe911.model.response.paypal

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class CycleExecution(): Parcelable {
    @SerializedName("tenure_type")
    @Expose
    var tenureType: String? = ""

    @SerializedName("sequence")
    @Expose
    var sequence: Int? = 0

    @SerializedName("cycles_completed")
    @Expose
    var cyclesCompleted: Int? = 0

    @SerializedName("cycles_remaining")
    @Expose
    var cyclesRemaining: Int? = 0

    @SerializedName("total_cycles")
    @Expose
    var totalCycles: Int? = 0

    constructor(parcel: Parcel) : this() {
        tenureType = parcel.readString()
        sequence = parcel.readValue(Int::class.java.classLoader) as? Int
        cyclesCompleted = parcel.readValue(Int::class.java.classLoader) as? Int
        cyclesRemaining = parcel.readValue(Int::class.java.classLoader) as? Int
        totalCycles = parcel.readValue(Int::class.java.classLoader) as? Int
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(tenureType ?: "")
        parcel.writeValue(sequence)
        parcel.writeValue(cyclesCompleted)
        parcel.writeValue(cyclesRemaining)
        parcel.writeValue(totalCycles)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CycleExecution> {
        override fun createFromParcel(parcel: Parcel): CycleExecution {
            return CycleExecution(parcel)
        }

        override fun newArray(size: Int): Array<CycleExecution?> {
            return arrayOfNulls(size)
        }
    }
}