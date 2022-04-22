package com.keepSafe911.webservices

import com.keepSafe911.model.request.LoginRequest
import com.keepSafe911.model.response.hibp.Breach
import com.keepSafe911.model.response.hibp.BreachedAccount
import com.keepSafe911.model.response.hibp.Paste
import com.keepSafe911.model.response.paymentresponse.GoogleTransactionResponse
import com.keepSafe911.model.response.paymentresponse.PaymentResponse
import com.keepSafe911.model.response.yelp.YelpResponse
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.keepSafe911.model.response.*
import com.keepSafe911.model.response.findmissingchild.DonationHistoryResponse
import com.keepSafe911.model.response.findmissingchild.MissingChildListResponse
import com.keepSafe911.model.response.findmissingchild.RunSearchResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*


interface WebApi {

    @POST("KeepSafe911/UserLogin")
    fun callLoginApi(@Body registrationRequest: LoginRequest): Call<LoginResponse>


    @Multipart
    @POST("KeepSafe911/UserRegistraionV4")
    fun callSignUpApi(
        @Part("Email") email_add: RequestBody,
        @Part("Mobile") mob_no: RequestBody,
        @Part("Password") password: RequestBody,
        @Part("CreatedBy") created_by: RequestBody,
        @Part("UserName") user_name: RequestBody,
        @Part("ID") user_id: RequestBody,
        @Part("FirstName") first_name: RequestBody,
        @Part("LastName") last_name: RequestBody,
        @Part("DeviceModel") device_model: RequestBody,
        @Part("DeviceCompanyName") device_company: RequestBody,
        @Part("devicetypeid") device_id: RequestBody,
        @Part("DeviceOS") device_os: RequestBody,
        @Part("DeviceToken") device_token: RequestBody,
        @Part("DeviceType") device_type: RequestBody,
        @Part("RecordStatus") record_status: RequestBody,
        @Part("Longitude") longitude: RequestBody,
        @Part("Latitude") lattitude: RequestBody,
        @Part("BatteryLevel") battery_level: RequestBody,
        @Part("DeviceTokenId") device_token_id: RequestBody,
        @Part("StartDate") start_date: RequestBody,
        @Part("UUID") device_uuid: RequestBody,
        @Part("LocationPermission") location_permission: RequestBody,
        @Part("LocationAddress") location_address: RequestBody,
        @Part("IsSms") isSMS: RequestBody,
        @Part profileImage: MultipartBody.Part,
        @Part("Token") payment_token: RequestBody,
        @Part("Package") payment_package: RequestBody,
        @Part("IsAdditionalMember") isAdmin_mem: RequestBody,
        @Part("NotificationPermission") notificationPermission: RequestBody,
        @Part("Frequency") frequency: RequestBody,
        @Part("LoginByApp") loginByApp: RequestBody,
        @Part("ReferralName") referralName : RequestBody,
        @Part("Promocode") promoCode: RequestBody,
        @Part("IsChildMissing") isChildMissing: RequestBody): Call<CommonValidationResponse>


    @GET("KeepSafe911/GetFamilyMonitoringUsersDetails")
    fun getFamilyMonitoringUsersDetails(@Query("AdminID") adminId: Int): Call<GetFamilyMonitoringResponse>

    @POST("KeepSafe911/GetRouteDirection")
    @FormUrlEncoded
    fun getRouteDirection(
        @Field("MemberID") memberID: Int,
        @Field("StartDate") startDate: String
    ): Call<MemberRouteResponse>

    @GET("KeepSafe911/GetLiveMembers")
    fun getLiveMembers(@Query("memberId") memberID: Int): Call<LiveMemberResponse>

    @GET("KeepSafe911/DeleteFamilyMonitoringUser")
    fun deleteUser(@Query("MemberID") memberID: Int): Call<ApiResponse>

    @GET("KeepSafe911/GetGeoFenceListingByAdmin")
    fun getGeofenceList(@Query("AdminID") adminId: Int): Call<GeoFenceListResponse>

    @POST("KeepSafe911/ManageFamilyMonitoringGeoFence")
    fun addUpdateGeoFence(@Body jsonObject: JsonObject): Call<AddUpdateGeoFenceResponse>

    @POST("KeepSafe911/GeoFenceActiveDeactiveByAdmin")
    @FormUrlEncoded
    fun activeDeActiveGeoFence(
        @Field("GeoFecnceID") geoFecnceID: Int,
        @Field("Status") status: Boolean
    ): Call<ApiResponse>

    @POST("KeepSafe911/SendNotificationOfGeoFenceMembers")
    fun sendNotificationOfGeoFence(@Body jsonObject: JsonObject): Call<ApiResponse>

    @GET("KeepSafe911Application/DownloadReport")
    fun downloadBusinessReport(
        @Query("MemberID") memberId: Int,
        @Query("StartDate") startDate: String,
        @Query("EndDate") endDate: String,
        @Query("Type") Miles: String,
        @Query("ApplicationType") applicationType: String
    ): Call<ResponseBody>

    @GET("KeepSafe911Application/DownloadReport")
    fun BusinessReport(
        @Query("MemberID") memberId: Int,
        @Query("StartDate") startDate: String,
        @Query("EndDate") endDate: String,
        @Query("Type") Miles: String,
        @Query("ApplicationType") applicationType: String
    ): Call<ApiResponse>

    @GET("KeepSafe911Application/ViewReport")
    fun viewBusinessReport(
        @Query("MemberID") memberId: Int,
        @Query("StartDate") startDate: String,
        @Query("EndDate") endDate: String,
        @Query("Type") Miles: String,
        @Query("ApplicationType") applicationType: String
    ): Call<MemberRouteResponse>

    @POST("KeepSafe911/ChangePassword")
    fun callChangePassApi(@Body jsonObject: JsonObject): Call<ApiResponse>

    @POST("KeepSafe911/UpdateProfileV1")
    @Multipart
    fun updateProfile(
        @Part("ID") memberID: RequestBody,
        @Part("FirstName") firstName: RequestBody,
        @Part("LastName") lastName: RequestBody,
        @Part("Mobile") mobile: RequestBody,
        @Part("Email") email: RequestBody,
        @Part("UpdateImage") updateImage: RequestBody,
        @Part profileImage: MultipartBody.Part
    ): Call<UpdateProfileResponse>

    @POST("KeepSafe911/UserOfflineData")
    fun userOfflineData(@Body jsonObject: JsonArray): Call<CommonValidationResponse>

    @POST("KeepSafe911/SendNotificationOfGeoFenceMembersOffline")
    fun notificationOfflineData(@Body jsonArray: JsonArray): Call<ApiResponse>

    @GET("KeepSafe911/GetNotificationSummary")
    fun getBoundarySummary(@Query("AdminID") adminId: Int): Call<BoundarySummaryResponse>

    @GET("KeepSafe911/verifyEmail")
    fun getVerifyEmail(@Query("AdminID") adminId: Int): Call<BoundarySummaryResponse>

    @POST("KeepSafe911/SmsAndNotificationActiveDeactiveByAdmin")
    @FormUrlEncoded
    fun activeDeActiveSMS(
        @Field("AdminID") adminId: Int,
        @Field("Status") status: Boolean,
        @Field("Type") type: String
    ): Call<ApiResponse>

    @POST("request.api")
    fun paymentRequest(@Body paymentRequest: JsonObject): Call<PaymentResponse>

    @POST("request.api")
    fun googlePaymentRequest(@Body paymentRequest: JsonObject): Call<GoogleTransactionResponse>

    @POST("KeepSafe911/CheckUserEmailUserNameMobile")
    @FormUrlEncoded
    fun validateEmailNameMobile(
        @Field("Email") email: String,
        @Field("Username") userName: String,
        @Field("Mobile") mobile: String
    ): Call<ValidationResponse>


    @POST("KeepSafe911/CheckUserEmailUserNameMobile")
    fun callVerifyEmail(@Body json_obj: JsonObject): Call<VerifyResponse>

    @GET("KeepSafe911UserSubscription/GetUserSubscriptionStatus")
    fun callCheckSubscription(@Query("UserID") user_id: String): Call<ApiResponse>

    @GET("KeepSafe911UserSubscription/DeactiveSubscription")
    fun callCancelSubscription(@Query("UserID") user_id: String): Call<ApiResponse>

    @GET("KeepSafe911UserSubscription/GetUserSubscription")
    fun getSubscriptionData(@Query("UserID") UserID: String): Call<UpdateSubscriptionResponse>

    @POST("KeepSafe911UserSubscription/ChangePaymentCard")
    fun callChangePaymentCard(@Body jsn_obj: JsonObject): Call<ApiResponse>

    @POST("KeepSafe911UserSubscription/UpgradeUserSubscription")
    fun callUpdateSubscription(@Body jsonObject: JsonObject): Call<ApiResponse>

    @GET("KeepSafe911UserSubscription/GetUserSubscriptionHistory")
    fun callSubscriptionHistory(@Query("UserID") user_id: Int): Call<SubScriptionHistoryResponse>

    @GET("KeepSafe911UserSubscription/CheckDeviceSubscription")
    fun callCheckDeviceSubscription(@Query("userid") user_id: Int): Call<DeviceSubscriptionResponse>

    @GET("KeepSafe911UserSubscription/InAppPurchaseSubscriptionDetails")
    fun callInAppPurchaseSubscription(@Query("userid") user_id: Int): Call<InAppPurchaseSubscriptionResponse>

    @POST("KeepSafe911/RegisterUserSubscription")
    fun callRegisterUserSubscription(@Body jsonObject: JsonObject): Call<GetFamilyMonitoringResponse>

    @GET("KeepSafe911/ForGotPassword")
    fun callForgotPassword(@Query("Mobile") mobile: String): Call<ApiResponse>

    @GET("KeepSafe911/ChangeAdminFrequancy")
    fun callChangePingFrequency(@Query("AdminID") adminId: Int,
                                @Query("Frequancy") frequency: Int): Call<ApiResponse>

    @GET("KeepSafe911/GetAppVersion")
    fun getAppVersion(@Query("Type") type:Int): Call<ApiResponse>

    //NeighborAlert
    /*@GET("NewsFeed/GetFeedList")
    fun getNewsFeed(@Query("PageSize") pageSize: Int): Call<GetFeedResponse>*/

    @GET("KeepSafe911NewsFeed/GetFeedList_v2")
    fun getNewsFeed(@Query("PageSize") pageSize: Int, @Query("AdminId") adminId: Int): Call<GetFeedResponse>

    @Multipart
    @POST("KeepSafe911NewsFeed/FeedPost")
    fun feedPost(@Part("ID") id: RequestBody,
                 @Part("Title") title: RequestBody,
                 @Part("Feeds") feeds: RequestBody,
                 @Part("Location") location: RequestBody,
                 @Part("Lat") lat: RequestBody,
                 @Part("Long") lng: RequestBody,
                 @Part("Type") type: RequestBody,
                 @Part("CreatedBy") created_by: RequestBody,
                 @Part("CategoryID") category: RequestBody,
                 @Part("FileType") fileType: RequestBody,
                 @Part("CreatedOn") createdOn: RequestBody,
                 @Part feedImageVideo: MultipartBody.Part): Call<FeedResponse>

    @POST("KeepSafe911NewsFeed/LikeOrComment")
    fun likeComment(@Body jsonObject: JsonObject): Call<LikeCommentResponse>

    @GET("KeepSafe911NewsFeed/GetCategoryList")
    fun categoryList(): Call<CategoryListResponse>

    @GET("KeepSafe911NewsFeed/DeleteFeed")
    fun deleteFeed(@Query("FeedID") feedId: Int): Call<ApiResponse>

    @GET("autocomplete")
    fun getTextBasedCategory(@Query("text") text: String,
                             @Query("latitude") latitude: Double,
                             @Query("longitude") longitude: Double)

    @GET("businesses/search")
    fun getTermBasedFilter(@Query("term") text: String,
                           @Query("latitude") latitude: Double,
                           @Query("longitude") longitude: Double,
                           @Query("categories") categories: String): Call<YelpResponse>

    @GET("KeepSafe911/getSubscriptiontype")
    fun getSubscriptionType(): Call<SubscriptionTypeResponse>

    @GET("KeepSafe911/CheckMemberSubscription")
    fun checkMemberSubscription(@Query("MemberId") memberID: Int): Call<MemberSubscription>

    @GET("KeepSafe911VisitPlace/GetVisitPlacesDetails")
    fun getVisitPlacesDetails(@Query("UserId") userId: Int): Call<PlacesResponse>

    @GET("KeepSafe911VisitPlace/DeleteVisitPlaces")
    fun deleteVisitPlaces(@Query("PlaceId") placeId: Int): Call<ApiResponse>

    @POST("KeepSafe911VisitPlace/ManageVisitPlaces")
    fun addUpdateVisitPlaces(@Body jsonObject: JsonObject): Call<ApiResponse>

    @GET("KeepSafe911/RemoveProfilePicture")
    fun removeUserProfilePicture(@Query("MemberId") memberID: Int): Call<ApiResponse>

    @POST("KeepSafe911VisitPlace/RatingToPlace")
    fun rateToPlace(@Body jsonObject: JsonObject): Call<ApiResponse>

    @POST("KeepSafe911/SendNotificationOfEmergencyAlert")
    fun emergency911(@Body jsonObject: JsonObject): Call<ApiResponse>

    @GET("range/{hash5}")
    fun searchByRange(@Path("hash5") hash5: String): Call<String>

    @GET("breachedaccount/{account}")
    fun getAllBreachesForAccount(
        @Path(value = "account", encoded = false) account: String
    ): Call<ArrayList<BreachedAccount>>

    @get:GET("breaches")
    val getAllBreaches: Call<ArrayList<Breach>>

    @GET("breach/{name}")
    fun getBreach(@Path(value = "name", encoded = false) name: String): Call<Breach>

    @GET("pasteaccount/{account}")
    fun getAllPastesForAccount(
        @Path(
            value = "account",
            encoded = false
        ) account: String
    ): Call<ArrayList<Paste>>

    @get:GET("dataclasses")
    val dataClasses: Call<ArrayList<String>>

    @POST("KeepSafe911/FrequencyPremiumReport")
    fun frequencyPremiumReport(@Body jsonObject: JsonObject): Call<ApiResponse>

    @GET("KeepSafe911/GetFrequencyPremiumReport")
    fun getFrequencyPremiumReport(@Query("AdminId") adminId: Int): Call<FrequencyPremiumHistoryResponse>

    @GET("KeepSafe911/CheckFrequencyPaymentReport")
    fun checkFrequencyPaymentReport(@Query("MemberID") memberID: Int): Call<CheckFrequencyResponse>

    /*@Multipart
    @POST("KeepSafe911/SendVideoInEmailForEmeregency")
    fun sendEmailForEmergency(
        @Part("MemberId") memberID: RequestBody,
        @Part("AdminId") adminId: RequestBody,
        @Part("Address") address: RequestBody,
        @Part("IsVideo") isVideo: RequestBody,
        @Part("IsAdmin") isAdmin: RequestBody,
        @Part("LoginByApp") loginByApp: RequestBody,
        @Part emergencyFile: MultipartBody.Part
    ): Call<ApiResponse>*/

    @Multipart
    @POST("KeepSafe911/SendVideoInEmailForEmeregency_v1")
    fun sendEmailForEmergency(
        @Part("MemberId") memberID: RequestBody,
        @Part("AdminId") adminId: RequestBody,
        @Part("Address") address: RequestBody,
        @Part("IsVideo") isVideo: RequestBody,
        @Part("IsAdmin") isAdmin: RequestBody,
        @Part("lat") latitude: RequestBody,
        @Part("lng") longitude: RequestBody,
        @Part("LoginByApp") loginByApp: RequestBody,
        @Part emergencyFile: MultipartBody.Part
    ): Call<ApiResponse>

    @Multipart
    @POST("KeepSafe911/RunSearch")
    fun runSearchImage(
        @Part file1: MultipartBody.Part
    ): Call<RunSearchResponse>

    @Multipart
    @POST("KeepSafe911/SimpleCompare")
    fun compareImage(
        @Part file1: MultipartBody.Part,
        @Part file2: MultipartBody.Part
    ): Call<CommonValidationResponse>

    /*email = 1
    userName = 2
    referralCode = 3
    promoCode = 4*/
    @POST("KeepSafe911/Checkvalidation")
    fun callCommonValidationApi(@Body jsonObject: JsonObject) : Call<CommonValidationResponse>

    @GET("KeepSafe911/CheckUserActiveStatus")
    fun callCheckActiveStatus(@Query("Id") id: Int) : Call<ApiResponse>

    @GET("KeepSafe911/GetMissingChildList")
    fun getMissingChildList(@Query("OffSet") offset: Int) : Call<MissingChildListResponse>

    @GET("KeepSafe911/GetMissingChildTaskList")
    fun getMissingChildTaskList(): Call<CommonValidationResponse>

    @GET("KeepSafe911/GetMissingChildByUser")
    fun getMissingChildByUser(@Query("Id") id: Int): Call<CommonValidationResponse>

    @POST("KeepSafe911/AddChildEmergencyTaskResponse")
    fun addChildEmergencyTaskResponse(@Body jsonArray: JsonArray): Call<CommonValidationResponse>

    @GET("KeepSafe911/SearchMissingChild")
    fun getSearchMissingChild(@Query("offSet") offset: Int, @Query("Name") name: String) : Call<MissingChildListResponse>

    @POST("KeepSafe911/MissingChildDonation")
    fun childDonation(@Body donationObject: JsonObject): Call<CommonValidationResponse>

    @GET("KeepSafe911/DonationHistory")
    fun donationHistory(@Query("Userid") userId: Int): Call<DonationHistoryResponse>

    @GET("KeepSafe911/AddedMissingChildHistory")
    fun missingChildHistory(@Query("UserId") userId: Int): Call<MissingChildListResponse>

    @Multipart
    @POST("KeepSafe911/AddMissingChild_v1")//KeepSafe911/AddMissingChild
    fun addMissingChild(
        @Part("Id") missingId: RequestBody,
        @Part("FirstName") firstName: RequestBody,
        @Part("LastName") lastName: RequestBody,
        @Part("MissingCity") missingCity: RequestBody,
        @Part("MissingState") missingState: RequestBody,
        @Part("DateMissing") missingDate: RequestBody,
        @Part("Age") Age: RequestBody,
        @Part("Token") paymentToken: RequestBody,
        @Part("PayId") paymentId: RequestBody,
        @Part("InAppPurchasePassword") purchasePassword: RequestBody,
        @Part("Amount") payAmount: RequestBody,
        @Part("PaymentDate") payDate: RequestBody,
        @Part("AccountNumber") accountNumber: RequestBody,
        @Part("DeviceType") deviceType: RequestBody,
        @Part("UserId") userId: RequestBody,
        @Part("LastSeenSituation") lastSeenSituation: RequestBody,
        @Part("ContactNumber") contactNumber: RequestBody,
        @Part("HairColor") hairColor: RequestBody,
        @Part("EyeColor") eyeColor: RequestBody,
        @Part("Height") height: RequestBody,
        @Part("Weight") weight: RequestBody,
        @Part("Complexion") complexion: RequestBody,
        @Part("IsWearLenses") isWearLenses: RequestBody,
        @Part("IsbracesOnTeeth") isBracesOnTeeth: RequestBody,
        @Part("IsPhysicalAttributes") isPhysicalAttributes: RequestBody,
        @Part("PhysicalAttributes") physicalAttributes: RequestBody,
        @Part missingProfileImage: MultipartBody.Part,
    ): Call<CommonValidationResponse>

    @GET("KeepSafe911/GetVoiceRecognitionList")
    fun getVoiceRecognitionList(@Query("UserId") adminId: Int): Call<CommonValidationResponse>

    @POST("KeepSafe911/ManageVoiceRecognition")
    fun manageVoiceRecognition(@Body manageUserVRObj: JsonObject): Call<CommonValidationResponse>

    @GET("KeepSafe911/DeleteVoiceRecognition")
    fun deleteVoiceRecognition(@Query("Id") id: Int): Call<CommonValidationResponse>

    @GET("KeepSafe911/GetLiveStreamList")
    fun liveStreamList(@Query("AdminId") adminId: Int): Call<CommonValidationResponse>

    @POST("KeepSafe911/SendLiveStreamNotification")
    fun liveStreamNotification(@Body jsonObject: JsonObject): Call<CommonValidationResponse>

    @GET("KeepSafe911/DeleteLiveStream")
    fun deleteLiveStream(@Query("Id") id: Int): Call<ApiResponse>
}