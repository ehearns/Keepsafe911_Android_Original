package com.keepSafe911.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class LiveMemberResult() : Parcelable {

    @SerializedName("ID")
    @Expose
    var iD: Int = 0
    @SerializedName("CreatedBy")
    @Expose
    var createdBy: Int = 0
    @SerializedName("UserName")
    @Expose
    var userName: String? = ""
    @SerializedName("FirstName")
    @Expose
    var firstName: String? = ""
    @SerializedName("IsAdmin")
    @Expose
    var isAdmin: Boolean = false
    @SerializedName("MemberID")
    @Expose
    var memberID: Int = 0
    @SerializedName("FamiliyID")
    @Expose
    var familiyID: Int? = 0
    @SerializedName("MemberName")
    @Expose
    var memberName: String? = ""
    @SerializedName("ProfilePath")
    @Expose
    var profilePath: String? = ""
    @SerializedName("UUID")
    @Expose
    var uUID: String? = ""
    @SerializedName("StartDate")
    @Expose
    var startDate: String? = ""
    @SerializedName("LocalUniversalDate")
    @Expose
    var localUniversalDate: String? = ""
    @SerializedName("NotificationPermission")
    @Expose
    var notificationPermission: Boolean? = false
    @SerializedName("LocationPermission")
    @Expose
    var locationPermission: Boolean? = false
    @SerializedName("StartDateString")
    @Expose
    var startDateString: String? = ""
    @SerializedName("UniversalDate")
    @Expose
    var universalDate: String? = ""
    @SerializedName("RecordStatus")
    @Expose
    var recordStatus: Int = 0
    @SerializedName("LocationAddress")
    @Expose
    var locationAddress: String? = ""
    @SerializedName("MessageType")
    @Expose
    var messageType: Any? = null
    @SerializedName("Email")
    @Expose
    var email: String? = ""
    @SerializedName("Password")
    @Expose
    var password: String? = ""
    @SerializedName("BatteryLevel")
    @Expose
    var batteryLevel: String? = ""
    @SerializedName("Latitude")
    @Expose
    var latitude: Double = 0.toDouble()
    @SerializedName("Longitude")
    @Expose
    var longitude: Double = 0.toDouble()
    @SerializedName("DeviceDetails")
    @Expose
    var deviceDetails: String? = ""
    @SerializedName("lstDeviceDetails")
    @Expose
    var lstDeviceDetails: Any? = null
    @SerializedName("DeviceNumber")
    @Expose
    var deviceNumber: Int? = 0
    @SerializedName("DomainName")
    @Expose
    var domainName: String? = ""
    @SerializedName("FamilyName")
    @Expose
    var familyName: String? = ""
    @SerializedName("DeviceToken")
    @Expose
    var deviceToken: String? = ""
    @SerializedName("MemberUtcDateTime")
    @Expose
    var memberUtcDateTime: String? = ""
    @SerializedName("LastName")
    @Expose
    var lastName: String? = ""
    @SerializedName("sequirityQuestionID")
    @Expose
    var sequirityQuestionID: Int? = 0
    @SerializedName("SequirityAnswer")
    @Expose
    var sequirityAnswer: String? = ""
    @SerializedName("SubscriptionExpireDate")
    @Expose
    var subscriptionExpireDate: String? = ""
    @SerializedName("FreeTrail")
    @Expose
    var freeTrail: Boolean = false
    @SerializedName("count")
    @Expose
    var count: Int = 0
    @SerializedName("DeviceType")
    @Expose
    var deviceType: String? = ""
    @SerializedName("DeviceOS")
    @Expose
    var deviceOS: String? = ""
    @SerializedName("DeviceModelNo")
    @Expose
    var deviceModelNo: String? = ""
    @SerializedName("DeviceCompanyName")
    @Expose
    var deviceCompanyName: String? = ""
    @SerializedName("devicetypeid")
    @Expose
    var devicetypeid: Int = 0
    @SerializedName("DeviceInfo")
    @Expose
    var deviceInfo: String? = ""
    @SerializedName("message")
    @Expose
    var message: String? = ""
    @SerializedName("AuthToken")
    @Expose
    var authToken: String? = ""
    @SerializedName("UserID")
    @Expose
    var userID: Int = 0
    @SerializedName("DeviceTokenId")
    @Expose
    var deviceTokenId: String? = ""
    @SerializedName("Mobile")
    @Expose
    var mobile: String? = ""
    @SerializedName("Databasename")
    @Expose
    var databasename: Any? = null

    constructor(parcel: Parcel) : this() {
        iD = parcel.readInt()
        createdBy = parcel.readInt()
        userName = parcel.readString()
        firstName = parcel.readString()
        isAdmin = parcel.readByte() != 0.toByte()
        memberID = parcel.readInt()
        familiyID = parcel.readValue(Int::class.java.classLoader) as? Int
        memberName = parcel.readString()
        profilePath = parcel.readString()
        uUID = parcel.readString()
        startDate = parcel.readString()
        localUniversalDate = parcel.readString()
        notificationPermission = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        locationPermission = parcel.readValue(Boolean::class.java.classLoader) as? Boolean
        startDateString = parcel.readString()
        universalDate = parcel.readString()
        recordStatus = parcel.readInt()
        locationAddress = parcel.readString()
        email = parcel.readString()
        password = parcel.readString()
        batteryLevel = parcel.readString()
        latitude = parcel.readDouble()
        longitude = parcel.readDouble()
        deviceDetails = parcel.readString()
        deviceNumber = parcel.readValue(Int::class.java.classLoader) as? Int
        domainName = parcel.readString()
        familyName = parcel.readString()
        deviceToken = parcel.readString()
        memberUtcDateTime = parcel.readString()
        lastName = parcel.readString()
        sequirityQuestionID = parcel.readValue(Int::class.java.classLoader) as? Int
        sequirityAnswer = parcel.readString()
        subscriptionExpireDate = parcel.readString()
        freeTrail = parcel.readByte() != 0.toByte()
        count = parcel.readInt()
        deviceType = parcel.readString()
        deviceOS = parcel.readString()
        deviceModelNo = parcel.readString()
        deviceCompanyName = parcel.readString()
        devicetypeid = parcel.readInt()
        deviceInfo = parcel.readString()
        message = parcel.readString()
        authToken = parcel.readString()
        userID = parcel.readInt()
        deviceTokenId = parcel.readString()
        mobile = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(iD)
        parcel.writeInt(createdBy)
        parcel.writeString(userName)
        parcel.writeString(firstName)
        parcel.writeByte(if (isAdmin) 1 else 0)
        parcel.writeInt(memberID)
        parcel.writeValue(familiyID)
        parcel.writeString(memberName)
        parcel.writeString(profilePath)
        parcel.writeString(uUID)
        parcel.writeString(startDate)
        parcel.writeString(localUniversalDate)
        parcel.writeValue(notificationPermission)
        parcel.writeValue(locationPermission)
        parcel.writeString(startDateString)
        parcel.writeString(universalDate)
        parcel.writeInt(recordStatus)
        parcel.writeString(locationAddress)
        parcel.writeString(email)
        parcel.writeString(password)
        parcel.writeString(batteryLevel)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeString(deviceDetails)
        parcel.writeValue(deviceNumber)
        parcel.writeString(domainName)
        parcel.writeString(familyName)
        parcel.writeString(deviceToken)
        parcel.writeString(memberUtcDateTime)
        parcel.writeString(lastName)
        parcel.writeValue(sequirityQuestionID)
        parcel.writeString(sequirityAnswer)
        parcel.writeString(subscriptionExpireDate)
        parcel.writeByte(if (freeTrail) 1 else 0)
        parcel.writeInt(count)
        parcel.writeString(deviceType)
        parcel.writeString(deviceOS)
        parcel.writeString(deviceModelNo)
        parcel.writeString(deviceCompanyName)
        parcel.writeInt(devicetypeid)
        parcel.writeString(deviceInfo)
        parcel.writeString(message)
        parcel.writeString(authToken)
        parcel.writeInt(userID)
        parcel.writeString(deviceTokenId)
        parcel.writeString(mobile)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LiveMemberResult> {
        override fun createFromParcel(parcel: Parcel): LiveMemberResult {
            return LiveMemberResult(parcel)
        }

        override fun newArray(size: Int): Array<LiveMemberResult?> {
            return arrayOfNulls(size)
        }
    }


}