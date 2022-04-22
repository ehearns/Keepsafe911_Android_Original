package com.keepSafe911.model

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity(tableName = "memberTable")
class FamilyMonitorResult() : Parcelable {


    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "fmID")
    var fmID: Int = 0

    @SerializedName("ID")
    @Expose
    var iD: Int = 0
    @SerializedName("FirstName")
    @Expose
    var firstName: String? = ""
    @SerializedName("LastName")
    @Expose
    var lastName: String? = ""
    @SerializedName("UserName")
    @Expose
    var userName: String? = ""
    @SerializedName("Email")
    @Expose
    var email: String? = ""
    @SerializedName("Password")
    @Expose
    var password: String? = ""
    @SerializedName("Mobile")
    @Expose
    var mobile: String? = ""
    @SerializedName("Image")
    @Expose
    var image: String? = ""
    @SerializedName("CreatedBy")
    @Expose
    var createdBy: Int = 0
    @SerializedName("CreatedOn")
    @Expose
    var createdOn: String? = ""
    @SerializedName("IsActive")
    @Expose
    var isActive: Boolean = false
    @SerializedName("IsDeleted")
    @Expose
    var isDeleted: Boolean? = false
    @SerializedName("IsMemberLogin")
    @Expose
    var isMemberLogin: Boolean? = false
    @SerializedName("TokenID")
    @Expose
    var tokenID: String? = ""
    @SerializedName("IsSms")
    @Expose
    var isSms: Boolean? = false
    @SerializedName("IsNotification")
    @Expose
    var isNotification: Boolean? = false
    @Ignore
    @SerializedName("tblFamilyMonitoringGeoFence")
    @Expose
    var tblFamilyMonitoringGeoFence: List<Any>? = ArrayList<Any>()
    @Ignore
    @SerializedName("tblGeoFenceMembers")
    @Expose
    var tblGeoFenceMembers: List<Any>? = ArrayList<Any>()
    @Ignore
    @SerializedName("tblGeoFenceSummary")
    @Expose
    var tblGeoFenceSummary: List<Any>? = ArrayList<Any>()
    @Ignore
    @SerializedName("tblFamilyMonitoringMemberTracking")
    @Expose
    var tblFamilyMonitoringMemberTracking: List<Any>? = ArrayList<Any>()

    @SerializedName("SubscripionUsers")
    @Expose
    var SubscripionUsers: Int = 0


    @SerializedName("Package")
    @Expose
    var Package: Int = 0
    @SerializedName("IsCancelled")
    @Expose
    var IsCancelled: Boolean? = false
    @SerializedName("IsAdditionalMember")
    @Expose
    var IsAdditionalMember: Boolean? = false
    @SerializedName("IsSubscription")
    @Expose
    var IsSubscription: Boolean? = false
    @SerializedName("Frequancy")
    @Expose
    var frequency:Int? = 0
    @SerializedName("IsReport")
    @Expose
    var IsReport:Boolean? = false
    @SerializedName("LoginByApp")
    @Expose
    var loginByApp: Int? = 0

    @ColumnInfo(name = "ReferralName")
    @SerializedName("ReferralName")
    @Expose
    var ReferralName: String? = ""

    @ColumnInfo(name = "ReferralCode")
    @SerializedName("ReferralCode")
    @Expose
    var ReferralCode: String? = ""

    @ColumnInfo(name = "PromocodeUrl")
    @SerializedName("PromocodeUrl")
    @Expose
    var PromocodeUrl: String? = ""

    @ColumnInfo(name = "Promocode")
    @SerializedName("Promocode")
    @Expose
    var Promocode: String? = ""

    @ColumnInfo(name = "IsChildMissing")
    @SerializedName("IsChildMissing")
    @Expose
    var isChildMissing: Boolean? = false

    constructor(parcel: Parcel) : this() {
        fmID = parcel.readInt()
        iD = parcel.readInt()
        firstName = parcel.readString()
        lastName = parcel.readString()
        userName = parcel.readString()
        email = parcel.readString()
        password = parcel.readString()
        mobile = parcel.readString()
        image = parcel.readString()
        createdBy = parcel.readInt()
        createdOn = parcel.readString()
        isActive = parcel.readByte() != 0.toByte()
        isDeleted = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        isMemberLogin = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        tokenID = parcel.readString()
        isSms = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        isNotification = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        SubscripionUsers = parcel.readInt()
        Package = parcel.readInt()
        IsCancelled = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        IsAdditionalMember = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        IsSubscription = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        frequency = parcel.readValue(Int::class.java.classLoader) as? Int
        IsReport = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        loginByApp = parcel.readValue(Int::class.java.classLoader) as? Int
        ReferralName = parcel.readString()
        ReferralCode = parcel.readString()
        PromocodeUrl = parcel.readString()
        Promocode = parcel.readString()
        isChildMissing = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(fmID)
        parcel.writeInt(iD)
        parcel.writeString(firstName)
        parcel.writeString(lastName)
        parcel.writeString(userName)
        parcel.writeString(email)
        parcel.writeString(password)
        parcel.writeString(mobile)
        parcel.writeString(image)
        parcel.writeInt(createdBy)
        parcel.writeString(createdOn)
        parcel.writeByte(if (isActive) 1 else 0)
        parcel.writeValue(isDeleted)
        parcel.writeValue(isMemberLogin)
        parcel.writeString(tokenID)
        parcel.writeValue(isSms)
        parcel.writeValue(isNotification)
        parcel.writeInt(SubscripionUsers)
        parcel.writeInt(Package)
        parcel.writeValue(IsCancelled)
        parcel.writeValue(IsAdditionalMember)
        parcel.writeValue(IsSubscription)
        parcel.writeValue(frequency)
        parcel.writeValue(IsReport)
        parcel.writeValue(loginByApp)
        parcel.writeString(ReferralName)
        parcel.writeString(ReferralCode)
        parcel.writeString(PromocodeUrl)
        parcel.writeString(Promocode)
        parcel.writeValue(isChildMissing)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "FamilyMonitorResult(fmID=$fmID, iD=$iD, firstName=$firstName, lastName=$lastName, userName=$userName, email=$email, password=$password, mobile=$mobile, image=$image, createdBy=$createdBy, createdOn=$createdOn, isActive=$isActive, isDeleted=$isDeleted, isMemberLogin=$isMemberLogin, tokenID=$tokenID, isSms=$isSms, isNotification=$isNotification, tblFamilyMonitoringGeoFence=$tblFamilyMonitoringGeoFence, tblGeoFenceMembers=$tblGeoFenceMembers, tblGeoFenceSummary=$tblGeoFenceSummary, tblFamilyMonitoringMemberTracking=$tblFamilyMonitoringMemberTracking, SubscripionUsers=$SubscripionUsers)"
    }

    companion object CREATOR : Parcelable.Creator<FamilyMonitorResult> {
        override fun createFromParcel(parcel: Parcel): FamilyMonitorResult {
            return FamilyMonitorResult(parcel)
        }

        override fun newArray(size: Int): Array<FamilyMonitorResult?> {
            return arrayOfNulls(size)
        }
    }

}