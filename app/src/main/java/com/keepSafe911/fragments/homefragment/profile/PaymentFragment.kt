package com.keepSafe911.fragments.homefragment.profile


import AnimationType
import addFragment
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import com.keepSafe911.BuildConfig
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.fragments.payment_selection.UpdateSubFragment
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.listner.PaymentOptionListener
import com.keepSafe911.listner.PositiveButtonListener
import com.keepSafe911.model.SubscriptionBean
import com.keepSafe911.model.response.*
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.webservices.WebApiClient
import com.keepSafe911.utils.*
import hideKeyboard
import kotlinx.android.synthetic.main.background_layout.*
import kotlinx.android.synthetic.main.fragment_payment.*
import kotlinx.android.synthetic.main.toolbar_header.*
import retrofit2.Call
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class PaymentFragment : HomeBaseFragment(), View.OnClickListener {

    lateinit var appDatabase: OldMe911Database
    private var isCancelledSubscription: Boolean = false
    private var subscriptionTypeResultList: ArrayList<SubscriptionTypeResult> = ArrayList()
    private var inAppPurchaseResultList: ArrayList<InAppPurchaseSubscriptionResult> = ArrayList()
    private var singleInAppResult: InAppPurchaseSubscriptionResult = InAppPurchaseSubscriptionResult()
    private var deviceType: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mActivity.requestedOrientation =
            if (mActivity.resources.getBoolean(R.bool.isTablet)) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        LocaleUtils.setLocale(
            Locale(
                if (AppPreference
                        .getIntPreference(requireActivity()
                            , BuildConfig.languagePrefKey) == 0
                ) LocaleUtils.LAN_ENGLISH else LocaleUtils.LAN_SPANISH
            )
        )
        LocaleUtils.updateConfig(mActivity, mActivity.resources.configuration)
        /*mActivity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )*/
        return inflater.inflate(R.layout.fragment_payment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appDatabase = OldMe911Database.getDatabase(mActivity)
        mActivity.disableDrawer()
        setHeader()
        checkDeviceSubscriptionApi(true)
//        callgetUpdateSubscriptionApi(true)
        btn_sub_history.setOnClickListener(this)
        btn_change_card.setOnClickListener(this)
    }

    private fun callSubscriptionTypeApi(loader: Boolean) {
        mActivity.isSpeedAvailable()
        Utils.subscriptionTypeListApi(mActivity, object : CommonApiListener {
            override fun subscriptionTypeResponse(
                status: Boolean,
                subscriptionTypeResult: ArrayList<SubscriptionTypeResult>,
                message: String,
                responseMessage: String
            ) {
                subscriptionTypeResultList = ArrayList()
                if (status) {
                    subscriptionTypeResultList.addAll(subscriptionTypeResult)
                    if (subscriptionTypeResultList.size > 0) {
                        val subscriptionPackage = appDatabase.loginDao().getAll().Package
                        if (tv_package != null) {
                            if (isCancelledSubscription || subscriptionPackage == "0") {
                                tv_package.text =
                                    mActivity.resources.getString(R.string.no_subscription_active)
                            } else {
                                when (subscriptionPackage) {
                                    subscriptionTypeResultList[1].id!!.toString() -> tv_package.text =
                                        mActivity.resources.getString(
                                            R.string.current_subscription_month,
                                            subscriptionTypeResultList[1].totalCost!!.toString()
                                        )
                                    subscriptionTypeResultList[2].id!!.toString() -> tv_package.text =
                                        mActivity.resources.getString(
                                            R.string.current_subscription_year,
                                            subscriptionTypeResultList[2].totalCost!!.toString()
                                        )
                                    subscriptionTypeResultList[0].id!!.toString() -> tv_package.text =
                                        mActivity.resources.getString(R.string.current_subscription_free_trial)
                                    subscriptionTypeResultList[5].id!!.toString() -> tv_package.text =
                                        mActivity.resources.getString(R.string.current_subscription_monthly_free_trial)
                                }
                            }
                        }
                        btn_upgrade.setOnClickListener(this@PaymentFragment)
                    }
                }
            }
        }, loader)
    }
    private fun setHeader() {

        iv_back.visibility = View.VISIBLE
        tvHeader.text = getString(R.string.payTitle)
        Utils.setTextGradientColor(tvHeader)
        tvHeader.setPadding(0, 0, 50, 0)
        iv_back.setOnClickListener {
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
        mActivity.checkUserActive()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_upgrade -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                when (deviceType) {
                    1, 3 -> {
                        mActivity.addFragment(
                            UpdateSubFragment.newInstance(isFromPayment = true,
                                subscriptionTypeResultList = subscriptionTypeResultList,
                                isCancelledSubscription = isCancelledSubscription
                            ), true, true, AnimationType.fadeInfadeOut
                        )
                    }
                    else -> {
                        mActivity.showMessage(mActivity.resources.getString(R.string.update_from_itune))
                    }
                }
            }
            R.id.btn_change_card -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                when (deviceType) {
                    1 -> {
                        mActivity.addFragment(
                            UpdatePayentFragment.newInstance(
                                subscriptionBean = SubscriptionBean(CHANGE_PAYMENT, 0, FREE_PAYMENT, ""),
                            ),
                            true, false, animationType = AnimationType.fadeInfadeOut
                        )
                    }
                    3 -> {
                        mActivity.showMessage(mActivity.resources.getString(R.string.sub_from_paypal))
                    }
                    else -> {
                        mActivity.showMessage(mActivity.resources.getString(R.string.sub_from_itune))
                    }
                }
            }
            R.id.btn_cancel -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
//                openDialog()
                mActivity.openClientCallDialog()
            }
            R.id.btn_sub_history -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                when (deviceType) {
                    1, 3 -> {
                        mActivity.addFragment(SubscriptionHistoryFragment(),
                            true, true, AnimationType.fadeInfadeOut
                        )
                    }
                    else -> {
                        mActivity.showMessage(mActivity.resources.getString(R.string.sub_from_itune))
                    }
                }
            }
        }
    }

    private fun checkDeviceSubscriptionApi(versionCheck: Boolean) {
        val loginData = appDatabase.loginDao().getAll()
        val isAdditionalMember = loginData.IsAdditionalMember ?: false
        val userId = when {
            loginData.isAdmin -> {
                loginData.memberID
            }
            isAdditionalMember -> {
                loginData.memberID
            }
            else -> {
                loginData.adminID
            }
        }

        Utils.userDeviceSubscription(mActivity, userId ?: 0, object : CommonApiListener {
            override fun deviceCheckResponse(
                status: Boolean,
                message: String,
                responseMessage: String,
                result: DeviceSubscriptionResult
            ) {
                deviceType = result.subscriptionTookFrom ?: 0
                when (result.subscriptionTookFrom) {
                    0 -> {
                        tv_package.text =
                            mActivity.resources.getString(R.string.no_subscription_active)
                        if (btn_cancel!=null) {
                            val alpha = 0.45f
                            val alphaUp = AlphaAnimation(alpha, alpha)
                            alphaUp.fillAfter = true
                            btn_cancel.startAnimation(alphaUp)
                            btn_cancel.isEnabled = false
                            btn_cancel.isClickable = false
                        }
                        btn_sub_history.visibility = View.GONE
                    }
                    1 -> {
                        btn_sub_history.visibility = View.VISIBLE
                        callgetUpdateSubscriptionApi(versionCheck)
                    }
                    2 -> {
                        if (btn_cancel!=null) {
                            val alpha = 0.45f
                            val alphaUp = AlphaAnimation(alpha, alpha)
                            alphaUp.fillAfter = true
                            btn_cancel.startAnimation(alphaUp)
                            btn_cancel.isEnabled = false
                            btn_cancel.isClickable = false
                        }
                        btn_sub_history.visibility = View.GONE
                        callInAppPurchaseSubscriptionApi()
                    }
                    3 -> {
                        btn_sub_history.visibility = View.VISIBLE
                        checkPayPalAccountStatus(versionCheck)
                    }
                }
            }
        })
    }

    private fun checkPayPalAccountStatus(versionCheck: Boolean, isFromCancel: Boolean = false, isLoader: Boolean = true) {
        val loginData = appDatabase.loginDao().getAll()
        val subscriptionId = loginData.payId ?: ""

        mActivity.checkPaypalSubscription(subscriptionId, object : PaymentOptionListener {
            override fun onCreditCardOption() {}

            override fun onPayPalOption(
                subscriptionId: String, firstName: String,
                lastName: String, email: String
            ) {
                if (!isFromCancel) {
                    ed_customer_name.setText("$firstName $lastName")
                    ed_cardno.setText("")
                    isCancelledSubscription = false
                    performCancelButtonAlpha(loginData)
                    callSubscriptionTypeApi(versionCheck)
                }
            }

            override fun onPaymentExpired() {
                if (!isFromCancel) {
                    ed_customer_name.setText("")
                    ed_cardno.setText("")
                    isCancelledSubscription = true
                    if (btn_cancel!=null) {
                        val alpha = 0.45f
                        val alphaUp = AlphaAnimation(alpha, alpha)
                        alphaUp.fillAfter = true
                        btn_cancel.startAnimation(alphaUp)
                        btn_cancel.isEnabled = false
                        btn_cancel.isClickable = false
                    }
                }
            }
        }, loader = isLoader)
    }

    private fun callInAppPurchaseSubscriptionApi() {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            Comman_Methods.isProgressShow(mActivity)
            mActivity.isSpeedAvailable()
            val loginData = appDatabase.loginDao().getAll()
            val isAdditionalMember = loginData.IsAdditionalMember ?: false
            val userId = when {
                loginData.isAdmin -> {
                    loginData.memberID
                }
                isAdditionalMember -> {
                    loginData.memberID
                }
                else -> {
                    loginData.adminID
                }
            }
            val subResponseCall = WebApiClient.getInstance(mActivity)
                .webApi_without?.callInAppPurchaseSubscription(userId ?: 0)

            subResponseCall?.enqueue(object : retrofit2.Callback<InAppPurchaseSubscriptionResponse> {
                override fun onFailure(call: Call<InAppPurchaseSubscriptionResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(
                    call: Call<InAppPurchaseSubscriptionResponse>,
                    response: Response<InAppPurchaseSubscriptionResponse>
                ) {
                    Comman_Methods.isProgressHide()
                    if (response.isSuccessful) {
                        response.body()?.let {
                            it.result?.let { singleResult ->
                                inAppPurchaseResultList = ArrayList()
                                inAppPurchaseResultList = singleResult.result ?: ArrayList()
                                if (inAppPurchaseResultList.size > 0) {
                                    inAppPurchaseResultList = inAppPurchaseResultList.sortedByDescending { inApp -> inApp.purchaseDateMs?.toLong() }.toMutableList() as ArrayList<InAppPurchaseSubscriptionResult>
                                    singleInAppResult = inAppPurchaseResultList[0]
                                    btn_upgrade.setOnClickListener(this@PaymentFragment)

                                    var timeDifference = ""
                                    try {
                                        var date1: Date? = null
                                        var date2: Date? = null
                                        val purchaseDate = singleInAppResult.purchaseDateMs ?: ""
                                        val expireDate = singleInAppResult.expiresDateMs ?: ""
                                        if (purchaseDate.isNotEmpty()) {
                                            val startCal = Calendar.getInstance()
                                            startCal.timeInMillis = purchaseDate.toLong()
                                            date1 = startCal.time
                                        }
                                        if (expireDate.isNotEmpty()) {
                                            val expCal = Calendar.getInstance()
                                            expCal.timeInMillis = expireDate.toLong()
                                            date2 = expCal.time
                                        }

                                        var different = date2!!.time - date1!!.time

                                        println("startDate : $date1")
                                        println("endDate : $date2")
                                        println("different : $different")

                                        val secondsInMilli = 1000L
                                        val minutesInMilli = secondsInMilli * 60
                                        val hoursInMilli = minutesInMilli * 60
                                        val daysInMilli = hoursInMilli * 24

                                        val elapsedDays = different / daysInMilli
                                        different %= daysInMilli

                                        val packageCost = when {
                                            elapsedDays < 15L -> 0.10
                                            elapsedDays in 15..31 -> 4.99
                                            elapsedDays > 32L -> 49.99
                                            else -> 0.00
                                        }

                                        timeDifference = when {
                                            elapsedDays < 4L -> mActivity.resources.getString(R.string.current_subscription_free_trial)
                                            elapsedDays in 4..31 -> mActivity.resources.getString(R.string.current_subscription_month, packageCost.toString())
                                            elapsedDays > 32L -> mActivity.resources.getString(R.string.current_subscription_year, packageCost.toString())
                                            else -> mActivity.resources.getString(R.string.no_subscription_active)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    tv_package.text = timeDifference
                                }
                            }
                        }
                    }
                }
            })
        } else {
            Utils.showNoInternetMessage(mActivity)
        }
    }

    private fun callgetUpdateSubscriptionApi(loader: Boolean) {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            if (loader) {
                Comman_Methods.isProgressShow(mActivity)
            }
            mActivity.isSpeedAvailable()
            val subResponseCall = WebApiClient.getInstance(mActivity)
                .webApi_without?.getSubscriptionData(appDatabase.loginDao().getAll().memberID.toString())

            subResponseCall?.enqueue(object : retrofit2.Callback<UpdateSubscriptionResponse> {
                override fun onFailure(call: Call<UpdateSubscriptionResponse>, t: Throwable) {
                    if (loader) {
                        Comman_Methods.isProgressHide()
                    }
                    if (btn_cancel!=null) {
                        btn_cancel.setOnClickListener(this@PaymentFragment)
                    }
                    callSubscriptionTypeApi(loader)
                }

                override fun onResponse(
                    call: Call<UpdateSubscriptionResponse>,
                    response: Response<UpdateSubscriptionResponse>
                ) {
                    if (loader) {
                        Comman_Methods.isProgressHide()
                    }
                    if (response.isSuccessful) {

                        response.body()?.let { updateResponse ->
                            if (updateResponse.isStatus) {
                                updateResponse.result?.let { result ->
                                    val firstName = result.FirstName ?: ""
                                    val lastName = result.LastName ?: ""
                                    val cardNumber = result.CardNumber ?: ""
                                    if (result.FirstName!=null) {
                                        if (firstName.contains("_")) {
                                            ed_customer_name.setText(firstName.split("_")[0] + " " + lastName)
                                        } else {
                                            ed_customer_name.setText("$firstName $lastName")
                                        }
                                    }else{
                                        ed_customer_name.setText("")
                                    }
                                    val loginUpdate: LoginObject = appDatabase.loginDao().getAll()
                                    ed_cardno.setText(cardNumber)
                                    isCancelledSubscription = result.isCancelled ?: false
                                    performCancelButtonAlpha(loginUpdate)
                                }
                            } else {
                                mActivity.showMessage(updateResponse.message ?: "")
                            }
                        }
                    }
                    callSubscriptionTypeApi(loader)
                }
            })
        } else {
            Utils.showNoInternetMessage(mActivity)
        }
    }

    private fun performCancelButtonAlpha(loginUpdate: LoginObject) {
        if (btn_cancel!=null) {
            if (isCancelledSubscription || loginUpdate.Package == "0") {
                val alpha = 0.45f
                val alphaUp = AlphaAnimation(alpha, alpha)
                alphaUp.fillAfter = true
                btn_cancel.startAnimation(alphaUp)
                btn_cancel.isEnabled = false
                btn_cancel.isClickable = false
            } else {
                val alpha = 1.0f
                val alphaUp = AlphaAnimation(alpha, alpha)
                alphaUp.fillAfter = true
                btn_cancel.startAnimation(alphaUp)
                btn_cancel.isEnabled = true
                btn_cancel.isClickable = true
                btn_cancel.setOnClickListener(this@PaymentFragment)
            }
        }
    }


    private fun openDialog() {
        Comman_Methods.isCustomPopUpShow(mActivity,
            message = mActivity.resources.getString(R.string.cancellation_message),
            positiveButtonListener = object : PositiveButtonListener {
                override fun okClickListener() {
                    callCancelSubscriptionApi()
                }
            })
    }

    private fun callCancelSubscriptionApi() {
        mActivity.isSpeedAvailable()
        val loginObject: LoginObject = appDatabase.loginDao().getAll()
        val memberId = loginObject.memberID
        val paymentType = loginObject.paymentType ?: 0
        Utils.userSubscriptionCancel(mActivity, memberId, object : CommonApiListener {
            override fun commonResponse(
                status: Boolean,
                message: String,
                responseMessage: String,
                result: String
            ) {
                if (status) {
                    if (btn_cancel != null) {
                        val alpha = 0.45f
                        val alphaUp = AlphaAnimation(alpha, alpha)
                        alphaUp.fillAfter = true
                        btn_cancel.startAnimation(alphaUp)
                        btn_cancel.isEnabled = false
                        btn_cancel.isClickable = false
                    }
                    val loginupdate: LoginObject = appDatabase.loginDao().getAll()
                    loginupdate.Package = "0"
                    loginupdate.isChildMissing = true
                    appDatabase.loginDao().updateLogin(loginupdate)
                    mActivity.callLogOutPingApi(
                        LOGOUT_RECORD_STATUS.toString(),
                        true
                    )
                } else {
                    if (btn_cancel != null) {
                        val alpha = 1.0f
                        val alphaUp = AlphaAnimation(alpha, alpha)
                        alphaUp.fillAfter = true
                        btn_cancel.startAnimation(alphaUp)
                        btn_cancel.isEnabled = true
                        btn_cancel.isClickable = true
                    }
                }
                if (paymentType == 3) {
                    checkPayPalAccountStatus(
                        versionCheck = false,
                        isFromCancel = true,
                        isLoader = false
                    )
                } else {
                    callCheckSubscriptionApi(responseMessage)
                }
            }
        })
    }

    private fun callCheckSubscriptionApi(responseMessage: String) {
        mActivity.isSpeedAvailable()
        Utils.userSubscriptionCheck(mActivity, object : CommonApiListener {
            override fun commonResponse(
                status: Boolean,
                message: String,
                responseMessage: String,
                result: String
            ) {
//                mActivity.showMessage(responseMessage)
//                mActivity.onBackPressed()
            }

            override fun onFailureResult() {
//                mActivity.showMessage(responseMessage)
//                mActivity.onBackPressed()
            }
        }, true)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && isAdded) {
            setHeader()
            checkDeviceSubscriptionApi(false)
//            callgetUpdateSubscriptionApi(false)
        }
    }
}
