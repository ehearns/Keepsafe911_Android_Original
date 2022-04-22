package com.keepSafe911.model.request

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity(tableName = "loginEntry")
class LoginRequest {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo
    var loginEntryID: Int = 0
    @SerializedName("DeviceTokenId")
    @Expose
    var deviceTokenId: String? = ""
    @SerializedName("RecordStatus")
    @Expose
    var recordStatus: String? = ""
    @SerializedName("NotificationPermission")
    @Expose
    var notificationPermission: String? = ""
    @SerializedName("DeviceOS")
    @Expose
    var deviceOS: String? = ""
    @SerializedName("Password")
    @Expose
    var password: String? = ""
    @SerializedName("LocationPermission")
    @Expose
    var locationPermission: String? = ""
    @SerializedName("UUID")
    @Expose
    var uuid: String? = ""
    @SerializedName("DeviceType")
    @Expose
    var deviceType: String? = ""
    @SerializedName("BatteryLevel")
    @Expose
    var batteryLevel: String? = ""
    @SerializedName("DeviceModel")
    @Expose
    var deviceModel: String? = ""
    @SerializedName("StartDate")
    @Expose
    var startDate: String? = ""
    @SerializedName("Email")
    @Expose
    var email: String? = ""
    @SerializedName("DeviceCompanyName")
    @Expose
    var deviceCompanyName: String? = ""
    @SerializedName("Latitude")
    @Expose
    var latitude: String? = ""
    @SerializedName("Longitude")
    @Expose
    var longitude: String? = ""
    @SerializedName("LocationAddress")
    @Expose
    var locationAddress: String? = ""
    @SerializedName("Mobile")
    @Expose
    var mobile: String? = ""
    @SerializedName("CreatedBy")
    @Expose
    var createdby: String? = ""
    @SerializedName("devicetypeid")
    @Expose
    var devicetypeid: String? = ""
    @SerializedName("DeviceToken")
    @Expose
    var deviceToken: String? = ""
    @SerializedName("UserName")
    @Expose
    var userName: String? = ""
    @SerializedName("ID")
    @Expose
    var id: Int? = 0
    @SerializedName("FirstName")
    @Expose
    var firstName: String? = ""
    @SerializedName("LastName")
    @Expose
    var lastName: String? = ""
    @SerializedName("ProfilePath")
    @Expose
    var profilePath: String? = ""
    @SerializedName("Frequency")
    @Expose
    var frequency: Int? = 0
    @SerializedName("LoginByApp")
    @Expose
    var loginByApp: Int? = 0

    override fun toString(): String {
        return "ClassPojo [DeviceTokenId = $deviceTokenId, RecordStatus = $recordStatus, NotificationPermission = $notificationPermission, DeviceOS = $deviceOS, Password = $password, LocationPermission = $locationPermission, UUID = $uuid, DeviceType = $deviceType, BatteryLevel = $batteryLevel, DeviceModel = $deviceModel, StartDate = $startDate, Email = $email, DeviceCompanyName = $deviceCompanyName, Latitude = $latitude, Longitude = $longitude, LocationAddress = $locationAddress, Mobile = $mobile, CreatedBy = $createdby, devicetypeid = $devicetypeid, DeviceToken = $deviceToken, UserName = $userName, ID = $id, FirstName = $firstName, LastName = $lastName, ProfilePath = $profilePath, Frequency = $frequency, LoginByApp = $loginByApp]"
    }
}