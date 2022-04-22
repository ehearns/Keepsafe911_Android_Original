package com.keepSafe911.model.response.hibp

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * A HaveIBeenPwned breach
 *
 * @author gideon
 */
class Breach(): Parcelable {
    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(title)
        dest?.writeString(name)
        dest?.writeString(domain)
        dest?.writeString(breachDate)
        dest?.writeString(addedDate)
        dest?.writeString(modifiedDate)
        dest?.writeLong(pwnCount)
        dest?.writeString(description)
        dest?.writeByte(if (isIsVerified) 1 else 0)
        dest?.writeByte(if (isIsFabricated) 1 else 0)
        dest?.writeByte(if (isIsSensitive) 1 else 0)
        dest?.writeByte(if (isIsRetired) 1 else 0)
        dest?.writeByte(if (isIsSpamList) 1 else 0)
        dest?.writeString(logoPath)
    }

    override fun describeContents(): Int {
        return 0
    }

    @SerializedName("Title")
    @Expose
    var title: String? = ""
    @SerializedName("Name")
    @Expose
    var name: String? = ""
    @SerializedName("Domain")
    @Expose
    var domain: String? = ""
    @SerializedName("BreachDate")
    @Expose
    var breachDate: String? = ""
    @SerializedName("AddedDate")
    @Expose
    var addedDate: String? = ""
    @SerializedName("ModifiedDate")
    @Expose
    var modifiedDate: String? = ""
    @SerializedName("PwnCount")
    @Expose
    var pwnCount: Long = 0
    @SerializedName("Description")
    @Expose
    var description: String? = ""
    @SerializedName("DataClasses")
    @Expose
    var dataClasses: ArrayList<String>? = ArrayList()
    @SerializedName("IsVerified")
    @Expose
    var isIsVerified: Boolean = false
    @SerializedName("IsFabricated")
    @Expose
    var isIsFabricated: Boolean = false
    @SerializedName("IsSensitive")
    @Expose
    var isIsSensitive: Boolean = false
    @SerializedName("IsRetired")
    @Expose
    var isIsRetired: Boolean = false
    @SerializedName("IsSpamList")
    @Expose
    var isIsSpamList: Boolean = false
    @SerializedName("LogoPath")
    @Expose
    var logoPath: String? = ""

    constructor(parcel: Parcel) : this() {
        title = parcel.readString()
        name = parcel.readString()
        domain = parcel.readString()
        breachDate = parcel.readString()
        addedDate = parcel.readString()
        modifiedDate = parcel.readString()
        pwnCount = parcel.readLong()
        description = parcel.readString()
        isIsVerified = parcel.readByte() != 0.toByte()
        isIsFabricated = parcel.readByte() != 0.toByte()
        isIsSensitive = parcel.readByte() != 0.toByte()
        isIsRetired = parcel.readByte() != 0.toByte()
        isIsSpamList = parcel.readByte() != 0.toByte()
        logoPath = parcel.readString()
    }

    override fun toString(): String {
        return "Breach{title=$title, name=$name, domain=$domain, breachDate=$breachDate, addedDate=$addedDate, modifiedDate=$modifiedDate, pwnCount=$pwnCount, description=$description, logoPath=$logoPath, dataClasses=$dataClasses, isVerified=$isIsVerified, isFabricated=$isIsFabricated, isSensitive=$isIsSensitive, isRetired=$isIsRetired, isSpamList=$isIsSpamList}"
    }

    companion object CREATOR : Parcelable.Creator<Breach> {
        override fun createFromParcel(parcel: Parcel): Breach {
            return Breach(parcel)
        }

        override fun newArray(size: Int): Array<Breach?> {
            return arrayOfNulls(size)
        }
    }
}
