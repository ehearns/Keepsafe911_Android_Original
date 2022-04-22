package com.keepSafe911.model.response.hibp

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Each paste contains a number of attributes describing it. FIXME bad
 * description
 *
 * @author gideon
 */
class Paste(): Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(source)
        dest?.writeString(id)
        dest?.writeString(title)
        dest?.writeString(date)
        dest?.writeInt(emailCount)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("Source")
    @Expose
    var source: String? = ""
    @SerializedName("Id")
    @Expose
    var id: String? = ""
    @SerializedName("Title")
    @Expose
    var title: String? = ""
    @SerializedName("Date")
    @Expose
    var date: String? = ""
    @SerializedName("EmailCount")
    @Expose
    var emailCount: Int = 0

    constructor(parcel: Parcel) : this() {
        source = parcel.readString()
        id = parcel.readString()
        title = parcel.readString()
        date = parcel.readString()
        emailCount = parcel.readInt()
    }

    override fun toString(): String {
        return "Paste{source=$source, id=$id, title=$title, date=$date, emailCount=$emailCount}"
    }

    companion object CREATOR : Parcelable.Creator<Paste> {
        override fun createFromParcel(parcel: Parcel): Paste {
            return Paste(parcel)
        }

        override fun newArray(size: Int): Array<Paste?> {
            return arrayOfNulls(size)
        }
    }


}
