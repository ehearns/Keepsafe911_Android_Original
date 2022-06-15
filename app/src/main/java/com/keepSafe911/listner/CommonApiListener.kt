package com.keepSafe911.listner

import com.keepSafe911.model.FamilyMonitorResult
import com.keepSafe911.model.LikeCommentResult
import com.keepSafe911.model.ResultRoute
import com.keepSafe911.model.response.*
import com.keepSafe911.model.response.findmissingchild.MatchResult
import com.keepSafe911.model.response.findmissingchild.MissingChildTaskListResult
import com.keepSafe911.model.response.findmissingchild.MissingChildTaskModel
import com.keepSafe911.model.response.paypal.SubscriptionResponse
import com.keepSafe911.model.response.paypal.Transaction
import com.keepSafe911.model.response.voicerecognition.ManageVoiceRecognitionModel
import com.keepSafe911.model.response.yelp.Business
import com.keepSafe911.model.response.yelp.Region
import com.keepSafe911.model.roomobj.LoginObject

interface CommonApiListener {
    fun familyUserList(status: Boolean = false, userList: ArrayList<FamilyMonitorResult> = ArrayList(), message: String = "") {}
    fun categoryList(status: Boolean = false, categoryResult: ArrayList<CategoryResult> = ArrayList(), message: String = "") {}
    fun postLikeComment(status: Boolean = false, likeCommentResult: LikeCommentResult? = LikeCommentResult(), message: String = "", responseMessage: String = "") {}
    fun paymentTransaction(transactionId: String = "") {}
    fun loginResponse(status: Boolean = false, loginData: LoginObject? = LoginObject(), message: String = "", responseMessage: String = "") {}
    fun commonResponse(status: Boolean = false, message: String = "", responseMessage: String = "", result: String = "") {}
    fun commonResultResponse(status: Boolean = false, message: String = "", responseMessage: String = "", result: Any? = null) {}
    fun deviceCheckResponse(status: Boolean = false, message: String = "", responseMessage: String = "", result: DeviceSubscriptionResult = DeviceSubscriptionResult()) {}
    fun yelpDataResponse(businesses: ArrayList<Business> = ArrayList(), total: Int = 0, region: Region = Region()) {}
    fun subscriptionTypeResponse(status: Boolean = false, subscriptionTypeResult: ArrayList<SubscriptionTypeResult> = ArrayList(), message: String = "", responseMessage: String = "") {}
    fun memberRouteResponse(status: Boolean = false, resultRoute: ArrayList<ResultRoute> = ArrayList(), message: String = "", responseMessage: String = "") {}
    fun voiceRecognitionResponse(status: Boolean = false, voiceList: ArrayList<ManageVoiceRecognitionModel> = ArrayList(), message: String = "", responseMessage: String = "") {}
    fun liveStreamListResponse(status: Boolean = false, userList: ArrayList<LiveStreamResult> = ArrayList(), message: String = "", responseMessage: String = "") {}
    fun missingChildTaskListResponse(status: Boolean = false, missingChildTaskList: ArrayList<MissingChildTaskListResult> = ArrayList(), message: String = "", responseMessage: String = "") {}
    fun childTaskListResponse(status: Boolean = false, matchResultData: ArrayList<MatchResult> = ArrayList(), missingChildTaskList: ArrayList<MissingChildTaskModel> = ArrayList(), message: String = "", responseMessage: String = "") {}
    fun privacyTermsChecked(type: Int = 0, accepted: Boolean = false) {}
    fun upgradeSubscription(status: Boolean = false, familyData: FamilyMonitorResult? = FamilyMonitorResult(), message: String = "", responseMessage: String = "") {}
    fun onSingleSubscriptionSuccessResult(updateTimeCard: SubscriptionResponse = SubscriptionResponse()) {}
    fun onSubscriptionTransactionResult(transactionList: ArrayList<Transaction> = ArrayList()) {}
    fun onFailureResult() {}
}