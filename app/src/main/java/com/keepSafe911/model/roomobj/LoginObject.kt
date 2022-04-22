package com.keepSafe911.model.roomobj

import androidx.annotation.Nullable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.keepSafe911.model.GeoFenceResult
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import org.jetbrains.annotations.NotNull

@Entity(tableName = "loginTable")
class LoginObject {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0
    @ColumnInfo(name = "MemberID")
    @SerializedName("MemberID")
    @Expose
    var memberID: Int = 0
    @ColumnInfo(name = "FamilyID")
    @SerializedName("FamilyID")
    @Expose
    var familyID: Int = 0
    @ColumnInfo(name = "Email")
    @SerializedName("Email")
    @Expose
    var email: String? = ""
    @ColumnInfo(name = "Password")
    @SerializedName("Password")
    @Expose
    var password: String? = ""
    @ColumnInfo(name = "RecordStatus")
    @SerializedName("RecordStatus")
    @Expose
    var recordStatus: Int = 0
    @ColumnInfo(name = "UUID")
    @SerializedName("UUID")
    @Expose
    var uUID: String? = ""
    @ColumnInfo(name = "LocationAddress")
    @SerializedName("LocationAddress")
    @Expose
    var locationAddress: String? = ""
    @ColumnInfo(name = "Latitude")
    @SerializedName("Latitude")
    @Expose
    var latitude: Double = 0.toDouble()
    @ColumnInfo(name = "Longitude")
    @SerializedName("Longitude")
    @Expose
    var longitude: Double = 0.toDouble()
    @ColumnInfo(name = "StartDate")
    @SerializedName("StartDate")
    @Expose
    var startDate: String? = ""
    @ColumnInfo(name = "DeviceDetails")
    @SerializedName("DeviceDetails")
    @Expose
    var deviceDetails: String? = ""
    @ColumnInfo(name = "IsAdmin")
    @SerializedName("IsAdmin")
    @Expose
    var isAdmin: Boolean = false
    @ColumnInfo(name = "UserName")
    @SerializedName("UserName")
    @Expose
    var userName: String? = ""
    @ColumnInfo(name = "ProfilePath")
    @SerializedName("ProfilePath")
    @Expose
    var profilePath: String? = ""
    @ColumnInfo(name = "FirstName")
    @SerializedName("FirstName")
    @Expose
    var firstName: String? = ""
    @ColumnInfo(name = "LastName")
    @SerializedName("LastName")
    @Expose
    var lastName: String? = ""
    @ColumnInfo(name = "sequirityQuestionID")
    @SerializedName("sequirityQuestionID")
    @Expose
    var sequirityQuestionID: Int? = 0
    @ColumnInfo(name = "SequirityAnswer")
    @SerializedName("SequirityAnswer")
    @Expose
    var sequirityAnswer: String? = ""
    @ColumnInfo(name = "DomainName")
    @SerializedName("DomainName")
    @Expose
    var domainName: String? = ""
    @ColumnInfo(name = "count")
    @SerializedName("count")
    @Expose
    var count: Int? = 0
    @ColumnInfo(name = "SubscriptionExpireDate")
    @SerializedName("SubscriptionExpireDate")
    @Expose
    var subscriptionExpireDate: String? = ""
    @ColumnInfo(name = "FreeTrail")
    @SerializedName("FreeTrail")
    @Expose
    var freeTrail: Boolean = false
    @ColumnInfo(name = "MemberUtcDateTime")
    @SerializedName("MemberUtcDateTime")
    @Expose
    var memberUtcDateTime: String? = ""
    @ColumnInfo(name = "EventGeoFanceListing")
    @SerializedName("EventGeoFanceListing")
    @Expose
    var eventGeoFanceListing: String? = ""
    @ColumnInfo(name = "Mobile")
    @SerializedName("Mobile")
    @Expose
    var mobile: String? = ""
    @ColumnInfo(name = "IsSms")
    @SerializedName("IsSms")
    @Expose
    var IsSms: Boolean = false


    @ColumnInfo(name = "IsNotification")
    @SerializedName("IsNotification")
    @Expose
    var IsNotification: Boolean? = false

    @Nullable
    @ColumnInfo(name = "IsSubscription")
    @SerializedName("IsSubscription")
    @Expose
    var IsSubscription: Boolean = false

    @NotNull
    @Nullable
    @ColumnInfo(name = "SubscriptionStartDate")
    @SerializedName("SubscriptionStartDate")
    @Expose
    var SubscriptionStartDate: String = ""


    @Nullable
    @ColumnInfo(name = "SubscriptionEndDate")
    @SerializedName("SubscriptionEndDate")
    @Expose
    var SubscriptionEndDate: String = ""

    @Nullable
    @ColumnInfo(name = "Package")
    @SerializedName("Package")
    @Expose
    var Package: String = ""


    var time_interval_days: Int = 0

    @ColumnInfo(name = "Frequency")
    @SerializedName("Frequency")
    @Expose
    var frequency: Int? = 0

    @ColumnInfo(name = "TotalMembers")
    @SerializedName("TotalMembers")
    @Expose
    var totalMembers: Int? = 0

    @ColumnInfo(name = "IsFromIos")
    @SerializedName("IsFromIos")
    @Expose
    var isFromIos: Boolean = false

    @ColumnInfo(name = "IsReport")
    @SerializedName("IsReport")
    @Expose
    var isReport: Boolean = false

    @ColumnInfo(name = "lstFamilyMonitoringGeoFence")
    @SerializedName("lstFamilyMonitoringGeoFence")
    @Expose
    var lstFamilyMonitoringGeoFence: ArrayList<GeoFenceResult> = ArrayList()

    @ColumnInfo(name = "AdminID")
    @SerializedName("AdminID")
    @Expose
    var adminID: Int? = 0

    @ColumnInfo(name = "LoginByApp")
    @SerializedName("LoginByApp")
    @Expose
    var loginByApp: Int? = 0

    @ColumnInfo(name = "IsAdditionalMember")
    @SerializedName("IsAdditionalMember")
    @Expose
    var IsAdditionalMember: Boolean? = false

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

    @ColumnInfo(name = "ClientMobileNumber")
    @SerializedName("ClientMobileNumber")
    @Expose
    var clientMobileNumber: String? = ""

    @ColumnInfo(name = "ClientImageUrl")
    @SerializedName("ClientImageUrl")
    @Expose
    var clientImageUrl: String? = ""

    @ColumnInfo(name = "CurrentSubscriptionEndDate")
    @SerializedName("CurrentSubscriptionEndDate")
    @Expose
    var currentSubscriptionEndDate: String? = ""

    @ColumnInfo(name = "LiveStreamDuration")
    @SerializedName("LiveStreamDuration")
    @Expose
    var liveStreamDuration: Int? = 0

    @ColumnInfo(name = "AdminName")
    @SerializedName("AdminName")
    @Expose
    var adminName: String? = ""

    @ColumnInfo(name = "IsAdminLoggedIn")
    @SerializedName("IsAdminLoggedIn")
    @Expose
    var isAdminLoggedIn: Boolean? = false

    companion object {
        fun create(serializedData: String): LoginObject {
            // Use GSON to instantiate this class using the JSON representation of the state
            val gson = GsonBuilder().serializeNulls().create()
            return gson.fromJson<LoginObject>(serializedData, LoginObject::class.java)
        }
    }
}