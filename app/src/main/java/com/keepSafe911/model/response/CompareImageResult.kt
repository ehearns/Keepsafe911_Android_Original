package com.keepSafe911.model.response

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

class CompareImageResult(): Parcelable {
    @SerializedName("WasMatch")
    @Expose
    var wasMatch: String? = ""

    @SerializedName("Result1")
    @Expose
    var result1: String? = ""

    @SerializedName("Result2")
    @Expose
    var result2: String? = ""

    @SerializedName("Result3")
    @Expose
    var result3: String? = ""

    constructor(parcel: Parcel) : this() {
        wasMatch = parcel.readString()
        result1 = parcel.readString()
        result2 = parcel.readString()
        result3 = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(wasMatch)
        parcel.writeString(result1)
        parcel.writeString(result2)
        parcel.writeString(result3)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CompareImageResult> {
        override fun createFromParcel(parcel: Parcel): CompareImageResult {
            return CompareImageResult(parcel)
        }

        override fun newArray(size: Int): Array<CompareImageResult?> {
            return arrayOfNulls(size)
        }
    }

}