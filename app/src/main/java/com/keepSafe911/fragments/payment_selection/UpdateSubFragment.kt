package com.keepSafe911.fragments.payment_selection


import AnimationType
import addFragment
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.keepSafe911.BuildConfig
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.fragments.homefragment.profile.UpdatePayentFragment
import com.keepSafe911.gps.GpsTracker
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.listner.PaymentOptionListener
import com.keepSafe911.model.FamilyMonitorResult
import com.keepSafe911.model.SubscriptionBean
import com.keepSafe911.model.response.ApiResponse
import com.keepSafe911.model.response.CommonValidationResponse
import com.keepSafe911.model.response.SubscriptionTypeResult
import com.keepSafe911.model.response.UpgradeSubscriptionResponse
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import com.keepSafe911.webservices.WebApiClient
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_update_sub.*
import kotlinx.android.synthetic.main.fragment_update_sub.prime_text
import kotlinx.android.synthetic.main.item_subscription.view.*
import kotlinx.android.synthetic.main.toolbar_header.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val ARG_PARAM3 = "param3"
private const val ARG_PARAM4 = "param4"
private const val ARG_PARAM5 = "param5"
private const val ARG_PARAM6 = "param6"
private const val ARG_PARAM7 = "param7"
private const val ARG_PARAM8 = "param8"
private const val ARG_PARAM9 = "param9"

class UpdateSubFragment : HomeBaseFragment(), View.OnClickListener {

    var isFromActivity: Boolean = false
    var isFromMember: Boolean = false
    var isFromPayment: Boolean = false
    var isEditUser: Boolean = false
    private var isCancelledSubscription: Boolean = false
    lateinit var appDatabase: OldMe911Database
    var month_amount = 0.0
    var year_amount = 0.0
    var familyMonitorResult: FamilyMonitorResult = FamilyMonitorResult()
    private var subscriptionTypeResultList: ArrayList<SubscriptionTypeResult> = ArrayList()
    private var selectedSubscribe: SubscriptionBean = SubscriptionBean()
    private var selectedSubscriptionType: Int = -1
    var isFromCancelledAllData = false
    var isFrom: Boolean = false
    private var gpstracker: GpsTracker? = null

    companion object {
        fun newInstance(
            isFromActivity: Boolean = false,
            isFromMember: Boolean = false,
            isFromPayment: Boolean = false,
            isEditUser: Boolean = false,
            familyMonitorResult: FamilyMonitorResult = FamilyMonitorResult(),
            subscriptionTypeResultList: ArrayList<SubscriptionTypeResult> = ArrayList(),
            isFrom: Boolean = false,
            isCancelledSubscription: Boolean = false,
            isFromCancelledAllData: Boolean = false
        ): UpdateSubFragment {
            val args = Bundle()
            args.putBoolean(ARG_PARAM1, isFromActivity)
            args.putBoolean(ARG_PARAM2, isFromMember)
            args.putBoolean(ARG_PARAM3, isFromPayment)
            args.putBoolean(ARG_PARAM4, isEditUser)
            args.putParcelable(ARG_PARAM5, familyMonitorResult)
            args.putParcelableArrayList(ARG_PARAM6,subscriptionTypeResultList)
            args.putBoolean(ARG_PARAM7, isFrom)
            args.putBoolean(ARG_PARAM8, isCancelledSubscription)
            args.putBoolean(ARG_PARAM9, isFromCancelledAllData)
            val fragment = UpdateSubFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isFromActivity = it.getBoolean(ARG_PARAM1, false)
            isFromMember = it.getBoolean(ARG_PARAM2, false)
            isFromPayment = it.getBoolean(ARG_PARAM3, false)
            isEditUser = it.getBoolean(ARG_PARAM4, false)
            familyMonitorResult = it.getParcelable(ARG_PARAM5) ?: FamilyMonitorResult()
            subscriptionTypeResultList = it.getParcelableArrayList(ARG_PARAM6) ?: ArrayList()
            isFrom = it.getBoolean(ARG_PARAM7,false)
            isCancelledSubscription = it.getBoolean(ARG_PARAM8,false)
            isFromCancelledAllData = it.getBoolean(ARG_PARAM9,false)
        }
    }

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
        return inflater.inflate(R.layout.fragment_update_sub, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appDatabase = OldMe911Database.getDatabase(mActivity)
        mActivity.disableDrawer()
        initializeData()
    }

    private fun setupRecyclerview(subscriptionTypeResultList: ArrayList<SubscriptionTypeResult>) {
        var filterSubscriptionBean: ArrayList<SubscriptionTypeResult> = ArrayList()
        if (subscriptionTypeResultList.size > 0) {
            if (isFromMember || isEditUser) {
                if (subscriptionTypeResultList.size > 3) {
                    filterSubscriptionBean =
                        subscriptionTypeResultList.filter { data -> data.id!! in 4..5 } as ArrayList<SubscriptionTypeResult>
                }
            } else {
                filterSubscriptionBean =
                    subscriptionTypeResultList.filter { data -> data.id!! in 2..3 } as ArrayList<SubscriptionTypeResult>
            }
        }
        selectedSubscriptionType = -1
        val adapter = SubscriptionAdapter(filterSubscriptionBean)
        rvUpdateSubscription.layoutManager = LinearLayoutManager(mActivity, LinearLayoutManager.HORIZONTAL, false)
        rvUpdateSubscription.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    private fun initializeData() {
        mActivity.checkUserActive()
        if (appDatabase.loginDao().getAll().isAdmin) {
            if (subscriptionTypeResultList.size == 0){
                callSubscriptionTypeApi()
            }else{
                setupRecyclerview(subscriptionTypeResultList)
                when {
                    isEditUser -> {

                        tv_update.text = (mActivity.resources.getString(R.string.str_user_subscription))
                        prime_text.text = mActivity.resources.getString(R.string.prime_text)
                        if (familyMonitorResult.IsSubscription == false) {
                            /*btn_month.text = getString(R.string.month_amt, subscriptionTypeResultList[3].totalCost?.toString())
                            btn_year.text = getString(R.string.year_amt, subscriptionTypeResultList[4].totalCost?.toString())*/

                        } else {
                            if (isCancelledSubscription){
                                /*btn_month.text = getString(R.string.month_amt, subscriptionTypeResultList[3].totalCost?.toString())
                                btn_year.text = getString(R.string.year_amt, subscriptionTypeResultList[4].totalCost?.toString())*/
                            }else {
                                if (familyMonitorResult.Package == subscriptionTypeResultList[4].id!!) {
                                    /*btn_year.setBackgroundColor(resources.getColor(R.color.text_lgray))
                                    btn_year.text = resources.getString(R.string.cancel_subscription)*/
                                } else {
                                    /*btn_month.setBackgroundColor(resources.getColor(R.color.text_lgray))
                                    btn_month.text = resources.getString(R.string.cancel_subscription)*/
                                }
                            }
                        }

                        year_amount = subscriptionTypeResultList[4].totalCost ?: 0.0
                        month_amount = subscriptionTypeResultList[3].totalCost ?: 0.0
                        btn_cancel.text = getString(R.string.cancel)
                    }
                    isFromActivity -> {

                        if (appDatabase.loginDao().getAll().isAdmin) {
                            if (isCancelledSubscription){
                                /*btn_month.visibility=View.GONE
                                btn_year.visibility=View.GONE
                                add_one.visibility=View.GONE*/
                                rvUpdateSubscription.visibility=View.GONE
                                btnUpdateSubscribe.visibility=View.GONE
                                tv_update.text = (mActivity.resources.getString(R.string.str_subscription_expire))
                                prime_text.text = mActivity.resources.getString(R.string.member_subscribe_text)
                                btn_cancel.text = getString(R.string.logout)
                            }else{
                                /*btn_month.visibility=View.VISIBLE
                                btn_year.visibility=View.VISIBLE
                                add_one.visibility=View.VISIBLE*/
                                rvUpdateSubscription.visibility=View.VISIBLE
                                btnUpdateSubscribe.visibility=View.VISIBLE
                                tv_update.text = (mActivity.resources.getString(R.string.upgrade_subscription))
                                prime_text.text = mActivity.resources.getString(R.string.prime_text)
                                btn_cancel.text = getString(R.string.logout)
                                /*btn_month.text =
                                    getString(R.string.month_trial, subscriptionTypeResultList[1].totalCost?.toString())
                                btn_year.text =
                                    getString(R.string.year_trial, subscriptionTypeResultList[2].totalCost?.toString())*/

                            }

                            month_amount = subscriptionTypeResultList[1].totalCost ?: 0.0
                            year_amount = subscriptionTypeResultList[2].totalCost ?: 0.0
                        }else{
                            tv_update.text = (mActivity.resources.getString(R.string.str_subscription_expire))
                            prime_text.text = mActivity.resources.getString(R.string.str_cancelled_removed)
                            btn_cancel.text = getString(R.string.logout)
                            /*btn_month.visibility=View.GONE
                            btn_year.visibility=View.GONE
                            add_one.visibility=View.GONE*/
                            rvUpdateSubscription.visibility=View.GONE
                            btnUpdateSubscribe.visibility=View.GONE
                        }
                    }
                    isFromMember -> {
                        tv_update.text = (mActivity.resources.getString(R.string.str_user_subscription))
                        prime_text.text = mActivity.resources.getString(R.string.add_additional_user)
                        /*btn_month.text = getString(R.string.month_amt, subscriptionTypeResultList[3].totalCost?.toString())
                        btn_year.text = getString(R.string.year_amt, subscriptionTypeResultList[4].totalCost?.toString())*/

                        year_amount = subscriptionTypeResultList[4].totalCost ?: 0.0
                        month_amount = subscriptionTypeResultList[3].totalCost ?: 0.0
                        btn_cancel.text = getString(R.string.cancel)
                    }
                    isFromPayment -> {

                        tv_update.text = (mActivity.resources.getString(R.string.upgrade_subscription))
                        prime_text.text = mActivity.resources.getString(R.string.prime_text)
                        /*btn_month.text = getString(R.string.month_trial, subscriptionTypeResultList[1].totalCost?.toString())
                        btn_year.text = getString(R.string.year_trial, subscriptionTypeResultList[2].totalCost?.toString())*/

                        month_amount = subscriptionTypeResultList[1].totalCost ?: 0.0
                        year_amount = subscriptionTypeResultList[2].totalCost ?: 0.0
                        btn_cancel.text = getString(R.string.cancel)

                        if (appDatabase.loginDao().getAll().Package == subscriptionTypeResultList[1].id!!.toString()) {
                            if (isCancelledSubscription){
                                val alpha = 1.0f
                                val alphaUp = AlphaAnimation(alpha, alpha)
                                alphaUp.fillAfter = true
                                /*btn_month.startAnimation(alphaUp)
                                btn_month.isEnabled = true*/
                            }else {
                                val alpha = 0.45f
                                val alphaUp = AlphaAnimation(alpha, alpha)
                                alphaUp.fillAfter = true
                                /*btn_month.startAnimation(alphaUp)
                                btn_month.isEnabled = false*/
                            }
                        } else {
                            val alpha = 1.0f
                            val alphaUp = AlphaAnimation(alpha, alpha)
                            alphaUp.fillAfter = true
                            /*btn_month.startAnimation(alphaUp)
                            btn_month.isEnabled = true*/
                        }
                        if (appDatabase.loginDao().getAll().Package == subscriptionTypeResultList[2].id!!.toString()) {
                            if (isCancelledSubscription){
                                val alpha = 1.0f
                                val alphaUp = AlphaAnimation(alpha, alpha)
                                alphaUp.fillAfter = true
                                /*btn_year.startAnimation(alphaUp)
                                btn_year.isEnabled = true*/
                            }else {
                                val alpha = 0.45f
                                val alphaUp = AlphaAnimation(alpha, alpha)
                                alphaUp.fillAfter = true
                                /*btn_year.startAnimation(alphaUp)
                                btn_year.isEnabled = false*/
                            }
                        } else {
                            val alpha = 1.0f
                            val alphaUp = AlphaAnimation(alpha, alpha)
                            alphaUp.fillAfter = true
                            /*btn_year.startAnimation(alphaUp)
                            btn_year.isEnabled = true*/
                        }
                    }
                }

                /*btn_month.setOnClickListener(this)
                btn_year.setOnClickListener(this)*/
                btn_cancel.setOnClickListener(this)
            }
        }else{
            setupRecyclerview(subscriptionTypeResultList)
            when {
                isEditUser -> {

                    tv_update.text = (mActivity.resources.getString(R.string.str_user_subscription))
                    prime_text.text = mActivity.resources.getString(R.string.prime_text)
                    if (familyMonitorResult.IsSubscription == false) {
                        /*btn_month.text = getString(R.string.month_amt, subscriptionTypeResultList[3].totalCost?.toString())
                        btn_year.text = getString(R.string.year_amt, subscriptionTypeResultList[4].totalCost?.toString())*/

                    } else {

                        if (isCancelledSubscription){
                            /*btn_month.text = getString(R.string.month_amt, subscriptionTypeResultList[3].totalCost?.toString())
                            btn_year.text = getString(R.string.year_amt, subscriptionTypeResultList[4].totalCost?.toString())*/
                        }else {
                            if (familyMonitorResult.Package == subscriptionTypeResultList[4].id!!) {
                                /*btn_year.setBackgroundColor(resources.getColor(R.color.text_lgray))
                                btn_year.text = resources.getString(R.string.cancel_subscription)*/
                            } else {
                                /*btn_month.setBackgroundColor(resources.getColor(R.color.text_lgray))
                                btn_month.text = resources.getString(R.string.cancel_subscription)*/
                            }
                        }
                    }

                    year_amount = subscriptionTypeResultList[4].totalCost ?: 0.0
                    month_amount = subscriptionTypeResultList[3].totalCost ?: 0.0
                    btn_cancel.text = getString(R.string.cancel)
                }
                isFromActivity -> {

                    if (appDatabase.loginDao().getAll().isAdmin) {
                        if (isCancelledSubscription){
                            /*btn_month.visibility=View.GONE
                            btn_year.visibility=View.GONE
                            add_one.visibility=View.GONE*/
                            rvUpdateSubscription.visibility=View.GONE
                            btnUpdateSubscribe.visibility=View.GONE
                            tv_update.text = (mActivity.resources.getString(R.string.str_subscription_expire))
                            prime_text.text = mActivity.resources.getString(R.string.member_subscribe_text)
                            btn_cancel.text = getString(R.string.logout)
                        }else{
                            /*btn_month.visibility=View.VISIBLE
                            btn_year.visibility=View.VISIBLE
                            add_one.visibility=View.VISIBLE*/
                            rvUpdateSubscription.visibility=View.VISIBLE
                            btnUpdateSubscribe.visibility=View.VISIBLE
                            tv_update.text = (mActivity.resources.getString(R.string.upgrade_subscription))
                            prime_text.text = mActivity.resources.getString(R.string.prime_text)
                            btn_cancel.text = getString(R.string.logout)
                            /*btn_month.text =
                                getString(R.string.month_trial, subscriptionTypeResultList[1].totalCost?.toString())
                            btn_year.text =
                                getString(R.string.year_trial, subscriptionTypeResultList[2].totalCost?.toString())*/

                        }

                        month_amount = subscriptionTypeResultList[1].totalCost ?: 0.0
                        year_amount = subscriptionTypeResultList[2].totalCost ?: 0.0
                    }else{
                        tv_update.text = (mActivity.resources.getString(R.string.str_subscription_expire))
                        prime_text.text = mActivity.resources.getString(R.string.str_cancelled_removed)
                        btn_cancel.text = getString(R.string.logout)
                        /*btn_month.visibility=View.GONE
                        btn_year.visibility=View.GONE
                        add_one.visibility=View.GONE*/
                        rvUpdateSubscription.visibility=View.GONE
                        btnUpdateSubscribe.visibility=View.GONE
                    }
                }
                isFromMember -> {
                    tv_update.text = (mActivity.resources.getString(R.string.str_user_subscription))
                    prime_text.text = mActivity.resources.getString(R.string.add_additional_user)
                    /*btn_month.text = getString(R.string.month_amt, subscriptionTypeResultList[3].totalCost?.toString())
                    btn_year.text = getString(R.string.year_amt, subscriptionTypeResultList[4].totalCost?.toString())*/

                    year_amount = subscriptionTypeResultList[4].totalCost ?: 0.0
                    month_amount = subscriptionTypeResultList[3].totalCost ?: 0.0
                    btn_cancel.text = getString(R.string.cancel)
                }
                isFromPayment -> {

                    tv_update.text = (mActivity.resources.getString(R.string.upgrade_subscription))
                    prime_text.text = mActivity.resources.getString(R.string.prime_text)
                    /*btn_month.text = getString(R.string.month_trial, subscriptionTypeResultList[1].totalCost?.toString())
                    btn_year.text = getString(R.string.year_trial, subscriptionTypeResultList[2].totalCost?.toString())*/

                    month_amount = subscriptionTypeResultList[1].totalCost ?: 0.0
                    year_amount = subscriptionTypeResultList[2].totalCost ?: 0.0
                    btn_cancel.text = getString(R.string.cancel)

                    if (appDatabase.loginDao().getAll().Package == subscriptionTypeResultList[1].id!!.toString()) {
                        if (isCancelledSubscription) {
                            val alpha = 1.0f
                            val alphaUp = AlphaAnimation(alpha, alpha)
                            alphaUp.fillAfter = true
                            /*btn_month.startAnimation(alphaUp)
                            btn_month.isEnabled = true*/
                        }else{
                            val alpha = 0.45f
                            val alphaUp = AlphaAnimation(alpha, alpha)
                            alphaUp.fillAfter = true
                            /*btn_month.startAnimation(alphaUp)
                            btn_month.isEnabled = false*/
                        }
                    } else {
                        val alpha = 1.0f
                        val alphaUp = AlphaAnimation(alpha, alpha)
                        alphaUp.fillAfter = true
                        /*btn_month.startAnimation(alphaUp)
                        btn_month.isEnabled = true*/
                    }
                    if (appDatabase.loginDao().getAll().Package == subscriptionTypeResultList[2].id!!.toString()) {
                        if (isCancelledSubscription){
                            val alpha = 1.0f
                            val alphaUp = AlphaAnimation(alpha, alpha)
                            alphaUp.fillAfter = true
                            /*btn_year.startAnimation(alphaUp)
                            btn_year.isEnabled = true*/
                        }else{
                            val alpha = 0.45f
                            val alphaUp = AlphaAnimation(alpha, alpha)
                            alphaUp.fillAfter = true
                            /*btn_year.startAnimation(alphaUp)
                            btn_year.isEnabled = false*/
                        }
                    } else {
                        val alpha = 1.0f
                        val alphaUp = AlphaAnimation(alpha, alpha)
                        alphaUp.fillAfter = true
                        /*btn_year.startAnimation(alphaUp)
                        btn_year.isEnabled = true*/
                    }
                }
            }

            /*btn_month.setOnClickListener(this)
            btn_year.setOnClickListener(this)*/
            btn_cancel.setOnClickListener(this)
        }
    }

    private fun changeDataOfSubscription() {
        if (selectedSubscribe.subScriptionCode > 0) {
            when {
                isEditUser -> {

                    if (familyMonitorResult.IsSubscription == false) {
                        btnUpdateSubscribe.text = mActivity.resources.getString(R.string.subscribe_now)
//                        btnUpdateSubscribe.background = ContextCompat.getDrawable(mActivity,R.drawable.button_back)
                    } else {
                        if (isCancelledSubscription) {
                            btnUpdateSubscribe.text = mActivity.resources.getString(R.string.subscribe_now)
//                            btnUpdateSubscribe.background = ContextCompat.getDrawable(mActivity,R.drawable.button_back)
                        } else {
                            if (familyMonitorResult.Package == selectedSubscribe.subScriptionCode) {
//                                btn_year.setBackgroundColor(resources.getColor(R.color.text_lgray))
//                                btnUpdateSubscribe.background = ContextCompat.getDrawable(mActivity,R.drawable.button_gray_back)
                                btnUpdateSubscribe.text = mActivity.resources.getString(R.string.cancel_subscription)
                            } else {
//                                btnUpdateSubscribe.background = ContextCompat.getDrawable(mActivity,R.drawable.button_back)
                                btnUpdateSubscribe.text = mActivity.resources.getString(R.string.subscribe_now)
                            }
                        }
                    }
                }
                isFromActivity -> {
                    btnUpdateSubscribe.text = mActivity.resources.getString(R.string.subscribe_now)
//                    btnUpdateSubscribe.background = ContextCompat.getDrawable(mActivity,R.drawable.button_back)
                }
                isFromMember -> {
                    btnUpdateSubscribe.text = mActivity.resources.getString(R.string.subscribe_now)
//                    btnUpdateSubscribe.background = ContextCompat.getDrawable(mActivity,R.drawable.button_back)
                }
                isFromPayment -> {

                    if (appDatabase.loginDao()
                            .getAll().Package == selectedSubscribe.subScriptionCode.toString()
                    ) {
                        if (isCancelledSubscription) {
                            val alpha = 1.0f
                            val alphaUp = AlphaAnimation(alpha, alpha)
                            alphaUp.fillAfter = true
                            btnUpdateSubscribe.startAnimation(alphaUp)
                            btnUpdateSubscribe.isEnabled = true
                        } else {
                            val alpha = 0.45f
                            val alphaUp = AlphaAnimation(alpha, alpha)
                            alphaUp.fillAfter = true
                            btnUpdateSubscribe.startAnimation(alphaUp)
                            btnUpdateSubscribe.isEnabled = false
                        }
                    } else {
                        val alpha = 1.0f
                        val alphaUp = AlphaAnimation(alpha, alpha)
                        alphaUp.fillAfter = true
                        btnUpdateSubscribe.startAnimation(alphaUp)
                        btnUpdateSubscribe.isEnabled = true
                    }
                }
            }
            btnUpdateSubscribe.setOnClickListener(this)
        } else {
            mActivity.showMessage(mActivity.resources.getString(R.string.str_sub_validation))
        }
    }


    private fun callSubscriptionTypeApi() {
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
                        setupRecyclerview(subscriptionTypeResultList)
                        when {
                            isEditUser -> {

                                tv_update.text = (mActivity.resources.getString(R.string.str_user_subscription))
                                prime_text.text =
                                    mActivity.resources.getString(R.string.prime_text)
                                if (familyMonitorResult.IsSubscription == false) {
                                    /*btn_month.text = getString(
                                        R.string.month_amt,
                                        subscriptionTypeResultList[3].totalCost?.toString()
                                    )
                                    btn_year.text = getString(
                                        R.string.year_amt,
                                        subscriptionTypeResultList[4].totalCost?.toString()
                                    )*/

                                } else {
                                    if (isCancelledSubscription) {
                                        /*btn_month.text = getString(
                                            R.string.month_amt,
                                            subscriptionTypeResultList[3].totalCost?.toString()
                                        )
                                        btn_year.text = getString(
                                            R.string.year_amt,
                                            subscriptionTypeResultList[4].totalCost?.toString()
                                        )*/
                                    } else {
                                        if (familyMonitorResult.Package == subscriptionTypeResultList[4].id!!) {
                                            /*btn_year.setBackgroundColor(
                                                resources.getColor(
                                                    R.color.text_lgray
                                                )
                                            )
                                            btn_year.text =
                                                resources.getString(R.string.cancel_subscription)*/
                                        } else {
                                            /*btn_month.setBackgroundColor(
                                                resources.getColor(
                                                    R.color.text_lgray
                                                )
                                            )
                                            btn_month.text =
                                                resources.getString(R.string.cancel_subscription)*/
                                        }
                                    }
                                }

                                year_amount =
                                    subscriptionTypeResultList[4].totalCost ?: 0.0
                                month_amount =
                                    subscriptionTypeResultList[3].totalCost ?: 0.0
                                btn_cancel.text = getString(R.string.cancel)
                            }
                            isFromActivity -> {

                                if (appDatabase.loginDao().getAll().isAdmin) {
                                    if (isCancelledSubscription) {
                                        /*btn_month.visibility = View.GONE
                                        btn_year.visibility = View.GONE
                                        add_one.visibility = View.GONE*/
                                        rvUpdateSubscription.visibility = View.GONE
                                        btnUpdateSubscribe.visibility = View.GONE
                                        tv_update.text = (mActivity.resources.getString(R.string.str_subscription_expire))
                                        prime_text.text =
                                            mActivity.resources.getString(R.string.member_subscribe_text)
                                        btn_cancel.text = getString(R.string.logout)
                                    } else {
                                        /*btn_month.visibility = View.VISIBLE
                                        btn_year.visibility = View.VISIBLE
                                        add_one.visibility = View.VISIBLE*/
                                        rvUpdateSubscription.visibility = View.VISIBLE
                                        btnUpdateSubscribe.visibility = View.VISIBLE

                                        tv_update.text = (mActivity.resources.getString(R.string.upgrade_subscription))
                                        prime_text.text =
                                            mActivity.resources.getString(R.string.prime_text)
                                        btn_cancel.text = getString(R.string.logout)
                                        /*btn_month.text =
                                            getString(
                                                R.string.month_trial,
                                                subscriptionTypeResultList[1].totalCost?.toString()
                                            )
                                        btn_year.text =
                                            getString(
                                                R.string.year_trial,
                                                subscriptionTypeResultList[2].totalCost?.toString()
                                            )*/

                                    }

                                    month_amount =
                                        subscriptionTypeResultList[1].totalCost ?: 0.0
                                    year_amount =
                                        subscriptionTypeResultList[2].totalCost ?: 0.0
                                } else {
                                    tv_update.text = (mActivity.resources.getString(R.string.str_subscription_expire))
                                    prime_text.text =
                                        mActivity.resources.getString(R.string.str_cancelled_removed)
                                    btn_cancel.text = getString(R.string.logout)
                                    /*btn_month.visibility = View.GONE
                                    btn_year.visibility = View.GONE
                                    add_one.visibility = View.GONE*/
                                    rvUpdateSubscription.visibility = View.GONE
                                    btnUpdateSubscribe.visibility = View.GONE
                                }
                            }
                            isFromMember -> {
                                val content =
                                    SpannableString(mActivity.resources.getString(R.string.mem_subscription))
                                content.setSpan(
                                    UnderlineSpan(),
                                    0,
                                    content.length,
                                    0
                                )
                                tv_update.text = (mActivity.resources.getString(R.string.str_user_subscription))
                                prime_text.text =
                                    mActivity.resources.getString(R.string.add_additional_user)
                                /*btn_month.text = getString(
                                    R.string.month_amt,
                                    subscriptionTypeResultList[3].totalCost?.toString()
                                )
                                btn_year.text = getString(
                                    R.string.year_amt,
                                    subscriptionTypeResultList[4].totalCost?.toString()
                                )*/

                                year_amount =
                                    subscriptionTypeResultList[4].totalCost ?: 0.0
                                month_amount =
                                    subscriptionTypeResultList[3].totalCost ?: 0.0
                                btn_cancel.text = getString(R.string.cancel)
                            }
                            isFromPayment -> {

                                tv_update.text = (mActivity.resources.getString(R.string.upgrade_subscription))
                                prime_text.text =
                                    mActivity.resources.getString(R.string.prime_text)
                                /*btn_month.text = getString(
                                    R.string.month_trial,
                                    subscriptionTypeResultList[1].totalCost?.toString()
                                )
                                btn_year.text = getString(
                                    R.string.year_trial,
                                    subscriptionTypeResultList[2].totalCost?.toString()
                                )*/

                                month_amount =
                                    subscriptionTypeResultList[1].totalCost ?: 0.0
                                year_amount =
                                    subscriptionTypeResultList[2].totalCost ?: 0.0
                                btn_cancel.text = getString(R.string.cancel)

                                if (appDatabase.loginDao().getAll().Package == subscriptionTypeResultList[1].id!!.toString()) {
                                    if (isCancelledSubscription) {
                                        val alpha = 1.0f
                                        val alphaUp = AlphaAnimation(alpha, alpha)
                                        alphaUp.fillAfter = true
                                        /*btn_month.startAnimation(alphaUp)
                                        btn_month.isEnabled = true*/
                                    } else {
                                        val alpha = 0.45f
                                        val alphaUp = AlphaAnimation(alpha, alpha)
                                        alphaUp.fillAfter = true
                                        /*btn_month.startAnimation(alphaUp)
                                        btn_month.isEnabled = false*/
                                    }
                                } else {
                                    val alpha = 1.0f
                                    val alphaUp = AlphaAnimation(alpha, alpha)
                                    alphaUp.fillAfter = true
                                    /*btn_month.startAnimation(alphaUp)
                                    btn_month.isEnabled = true*/
                                }
                                if (appDatabase.loginDao().getAll().Package == subscriptionTypeResultList[2].id!!.toString()) {
                                    if (isCancelledSubscription) {
                                        val alpha = 1.0f
                                        val alphaUp = AlphaAnimation(alpha, alpha)
                                        alphaUp.fillAfter = true
                                        /*btn_year.startAnimation(alphaUp)
                                        btn_year.isEnabled = true*/
                                    } else {
                                        val alpha = 0.45f
                                        val alphaUp = AlphaAnimation(alpha, alpha)
                                        alphaUp.fillAfter = true
                                        /*btn_year.startAnimation(alphaUp)
                                        btn_year.isEnabled = false*/
                                    }
                                } else {
                                    val alpha = 1.0f
                                    val alphaUp = AlphaAnimation(alpha, alpha)
                                    alphaUp.fillAfter = true
                                    /*btn_year.startAnimation(alphaUp)
                                    btn_year.isEnabled = true*/
                                }
                            }
                        }

                        /*btn_month.setOnClickListener(this@UpdateSubFragment)
                        btn_year.setOnClickListener(this@UpdateSubFragment)*/
                        btn_cancel.setOnClickListener(this@UpdateSubFragment)
                    }
                }
            }
        })
    }

    override fun onClick(v: View?) {
        val subscriptionPackage = appDatabase.loginDao().getAll().Package
        when (v?.id) {
            R.id.btn_month -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)

                if (isFromMember) {
                    val memberMonthSubscribe = SubscriptionBean(subscriptionTypeResultList[3].id ?: 0, subscriptionTypeResultList[3].days ?: 0, month_amount, subscriptionTypeResultList[3].planId ?: "")
                    if (familyMonitorResult.Package == subscriptionTypeResultList[3].id!! && familyMonitorResult.IsSubscription == true) {
                        if (isCancelledSubscription){
                            moveToPaymentScreen(isFromMember,
                                isFromPayment,
                                memberMonthSubscribe,
                                familyMonitorResult,
                                isFromActivity, isFrom)
                        }else {
                            openDialog()
                        }
                    } else {
                        moveToPaymentScreen(isFromMember,
                            isFromPayment,
                            memberMonthSubscribe,
                            familyMonitorResult,
                            isFromActivity, isFrom)
                        /*if (isEditUser) {

                        } else {
                            mActivity.addFragment(
                                AddMemberFragment.newInstance(
                                    false,
                                    FamilyMonitorResult(),
                                    SubscriptionBean(subscriptionTypeResultList[3].id ?: 0, subscriptionTypeResultList[3].days ?: 0, month_amount),
                                    false, true
                                ),
                                true,
                                true,
                                AnimationType.fadeInfadeOut
                            )
                        }*/
                    }
                } else if (isFromActivity || isFromPayment) {
                    if (subscriptionPackage == subscriptionTypeResultList[1].id!!.toString() && !isCancelledSubscription) {
                        mActivity.showMessage(mActivity.resources.getString(R.string.package_status))
                    } else {
                        moveToPaymentScreen(isFromMember,
                            isFromPayment,
                            SubscriptionBean(subscriptionTypeResultList[1].id ?: 0, subscriptionTypeResultList[1].days ?: 0, month_amount, subscriptionTypeResultList[1].planId ?: ""),
                            FamilyMonitorResult(),
                            isFromActivity, isFrom)
                    }
                }
            }
            R.id.btn_year -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)

                if (isFromMember) {
                    val memberYearSubscribe = SubscriptionBean(subscriptionTypeResultList[4].id ?: 0, subscriptionTypeResultList[4].days ?: 0, year_amount, subscriptionTypeResultList[4].planId ?: "")
                    if (familyMonitorResult.Package == subscriptionTypeResultList[4].id!!) {
                        if (isCancelledSubscription){
                            moveToPaymentScreen(isFromMember,
                                isFromPayment,
                                memberYearSubscribe,
                                familyMonitorResult,
                                isFromActivity, isFrom)
                        }else{
                            openDialog()
                        }
                    } else {
                        moveToPaymentScreen(isFromMember,
                            isFromPayment,
                            memberYearSubscribe,
                            familyMonitorResult,
                            isFromActivity, isFrom)
                        /*if (isEditUser) {
                            mActivity.addFragment(
                                UpdatePayentFragment.newInstance(
                                    true, true,
                                    SubscriptionBean(subscriptionTypeResultList[4].id ?: 0, subscriptionTypeResultList[4].days ?: 0, year_amount),
                                    familyMonitorResult,
                                    false
                                ),
                                true,
                                true,
                                AnimationType.fadeInfadeOut
                            )
                        } else {
                            mActivity.addFragment(
                                AddMemberFragment.newInstance(
                                    false,
                                    FamilyMonitorResult(),
                                    SubscriptionBean(subscriptionTypeResultList[4].id ?: 0, subscriptionTypeResultList[4].days ?: 0, year_amount),
                                    false, true
                                ),
                                true,
                                true,
                                AnimationType.fadeInfadeOut
                            )
                        }*/
                    }
                } else if (isFromActivity || isFromPayment) {
                    if (subscriptionPackage == subscriptionTypeResultList[2].id!!.toString() && !isCancelledSubscription) {
                        mActivity.showMessage(mActivity.resources.getString(R.string.package_status))
                    } else {
                        moveToPaymentScreen(isFromMember,
                            isFromPayment,
                            SubscriptionBean(subscriptionTypeResultList[2].id ?: 0, subscriptionTypeResultList[2].days ?: 0, year_amount, subscriptionTypeResultList[2].planId ?: ""),
                            FamilyMonitorResult(),
                            isFromActivity, isFrom)

                    }
                }
            }
            R.id.btn_cancel -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                if (btn_cancel.text == getString(R.string.logout)) {
                    mActivity.callLogOutPingApi(LOGOUT_RECORD_STATUS.toString(), true)
                } else {
                    mActivity.onBackPressed()
                }
            }
            R.id.btnUpdateSubscribe -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)

                when (selectedSubscriptionType) {
                    0, -1 -> {
                        mActivity.showMessage(mActivity.resources.getString(R.string.str_sub_validation))
                    }
                    1 -> {
                        if (isFromMember) {

                            if (familyMonitorResult.Package == selectedSubscribe.subScriptionCode && (familyMonitorResult.IsSubscription == true)) {
                                if (isCancelledSubscription){
                                    moveToPaymentScreen(isFromMember,
                                        isFromPayment,
                                        selectedSubscribe,
                                        familyMonitorResult,
                                        isFromActivity, isFrom)
                                }else {
                                    openDialog()
                                }
                            } else {
                                moveToPaymentScreen(isFromMember,
                                    isFromPayment,
                                    selectedSubscribe,
                                    familyMonitorResult,
                                    isFromActivity, isFrom)
                                /*if (isEditUser) {

                                } else {
                                    mActivity.addFragment(
                                        AddMemberFragment.newInstance(
                                            false,
                                            FamilyMonitorResult(),
                                            selectedSubscribe,
                                            false, true
                                        ),
                                        true,
                                        true,
                                        AnimationType.fadeInfadeOut
                                    )
                                }*/
                            }
                        } else if (isFromActivity || isFromPayment) {
                            if (subscriptionPackage == selectedSubscribe.subScriptionCode.toString() && !isCancelledSubscription) {
                                mActivity.showMessage(mActivity.resources.getString(R.string.package_status))
                            } else {
                                moveToPaymentScreen(isFromMember,
                                    isFromPayment,
                                    selectedSubscribe,
                                    FamilyMonitorResult(),
                                    isFromActivity, isFrom)
                            }
                        }
                    }
                    2 -> {
                        if (isFromMember) {
                            if (familyMonitorResult.Package == selectedSubscribe.subScriptionCode) {
                                if (isCancelledSubscription){
                                    moveToPaymentScreen(isFromMember,
                                        isFromPayment,
                                        selectedSubscribe,
                                        familyMonitorResult,
                                        isFromActivity, isFrom)
                                }else{
                                    openDialog()
                                }
                            } else {
                                moveToPaymentScreen(isFromMember,
                                    isFromPayment,
                                    selectedSubscribe,
                                    familyMonitorResult,
                                    isFromActivity, isFrom)
                                /*if (isEditUser) {
                                    mActivity.addFragment(
                                        UpdatePayentFragment.newInstance(
                                            true, true,
                                            selectedSubscribe,
                                            familyMonitorResult,
                                            false
                                        ),
                                        true,
                                        true,
                                        AnimationType.fadeInfadeOut
                                    )
                                } else {
                                    mActivity.addFragment(
                                        AddMemberFragment.newInstance(
                                            false,
                                            FamilyMonitorResult(),
                                            selectedSubscribe,
                                            false, true
                                        ),
                                        true,
                                        true,
                                        AnimationType.fadeInfadeOut
                                    )
                                }*/
                            }
                        } else if (isFromActivity || isFromPayment) {
                            if (subscriptionPackage == selectedSubscribe.subScriptionCode.toString() && !isCancelledSubscription) {
                                mActivity.showMessage(mActivity.resources.getString(R.string.package_status))
                            } else {
                                moveToPaymentScreen(isFromMember,
                                    isFromPayment,
                                    selectedSubscribe,
                                    FamilyMonitorResult(),
                                    isFromActivity, isFrom)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun moveToPaymentScreen(
        isFromMember: Boolean,
        isFromPayment: Boolean,
        subscriptionData: SubscriptionBean,
        familyMonitorResult: FamilyMonitorResult,
        isFromActivity: Boolean,
        isFrom: Boolean
    ) {
        var userFirstName: String = ""
        var userLastName: String = ""
        var userEmail: String = ""
        val loginObject = appDatabase.loginDao().getAll()

        if (isFromMember) {
            userFirstName = familyMonitorResult.firstName ?: ""
            userLastName = familyMonitorResult.lastName ?: ""
            userEmail = familyMonitorResult.email ?: ""
        } else {
            userFirstName = loginObject.firstName ?: ""
            userLastName = loginObject.lastName ?: ""
            userEmail = loginObject.email ?: ""
        }

        mActivity.openPaymentOptions(userFirstName, userLastName, userEmail,
            subscriptionData, object : PaymentOptionListener {
                override fun onCreditCardOption() {
                    mActivity.addFragment(
                        UpdatePayentFragment.newInstance(
                            isFromMember, isFromPayment,
                            subscriptionData,
                            familyMonitorResult,
                            isFromActivity, isFrom
                        ),
                        true, true, AnimationType.fadeInfadeOut
                    )
                }

                override fun onPayPalOption(
                    subscriptionId: String, firstName: String,
                    lastName: String, email: String
                ) {
                    if (!isFromPayment && !isFromMember && !isFromActivity) {

                    } else if (isFromMember && isFromPayment) {
                        callUpgradeSubscriptionApi(subscriptionData.subScriptionCode,
                            subscriptionId, userFirstName, userLastName)
                    } else if (isFromMember) {
                        gpstracker = GpsTracker(mActivity)
                        callSignUpAPI(subscriptionId, subscriptionData)
                    } else {
                        callUpgradeSubscriptionApi(subscriptionData.subScriptionCode,
                            subscriptionId, userFirstName, userLastName)
                    }
                }

            })
    }

    private fun callSignUpAPI(dataValue: String, subscriptionBean: SubscriptionBean) {

        println("!@@@familyMonitorResult.toString() = $familyMonitorResult")
        if (ConnectionUtil.isInternetAvailable(mActivity)) {

            val androidId: String = Settings.Secure.getString(mActivity.contentResolver, Settings.Secure.ANDROID_ID)

            Comman_Methods.isProgressShow(mActivity)
            mActivity.isSpeedAvailable()

            val appDatabase = OldMe911Database.getDatabase(mActivity)
            val loginObject = appDatabase.loginDao().getAll()

            val mobile_no = (familyMonitorResult.mobile ?: "").toRequestBody(MEDIA_TYPE_TEXT)
            val email = (familyMonitorResult.email ?: "").toRequestBody(MEDIA_TYPE_TEXT)
            val password = (familyMonitorResult.password ?: "").toRequestBody(MEDIA_TYPE_TEXT)
            val created_by: RequestBody = loginObject.memberID.toString().toRequestBody(MEDIA_TYPE_TEXT)
            val user_id: RequestBody = "0".toRequestBody(MEDIA_TYPE_TEXT)

            val user_name = (familyMonitorResult.userName ?: "").toRequestBody(MEDIA_TYPE_TEXT)
            val first_name = (familyMonitorResult.firstName ?: "").toRequestBody(MEDIA_TYPE_TEXT)
            val last_name = (familyMonitorResult.lastName ?: "").toRequestBody(MEDIA_TYPE_TEXT)
            val referral_name = (familyMonitorResult.ReferralName ?: "").toRequestBody(MEDIA_TYPE_TEXT)
            val promoCode = (familyMonitorResult.Promocode ?: "").toRequestBody(MEDIA_TYPE_TEXT)
            val device_model = Comman_Methods.getdeviceModel().toRequestBody(MEDIA_TYPE_TEXT)
            val device_company = Comman_Methods.getdevicename().toRequestBody(MEDIA_TYPE_TEXT)
            val device_os = Comman_Methods.getdeviceVersion().toRequestBody(MEDIA_TYPE_TEXT)
            val device_type = "Android".toRequestBody(MEDIA_TYPE_TEXT)
            val record_status: RequestBody = "0".toRequestBody(MEDIA_TYPE_TEXT)
            val decimalSymbols = DecimalFormatSymbols.getInstance(Locale.US)
            val longitude =
                DecimalFormat("##.######", decimalSymbols).format(gpstracker?.getLongitude() ?: 0.0)
                    .toRequestBody(MEDIA_TYPE_TEXT)
            val lattitude =
                DecimalFormat("##.######", decimalSymbols).format(gpstracker?.getLatitude() ?: 0.0)
                    .toRequestBody(MEDIA_TYPE_TEXT)
            val battery_level =
                Utils.GetBatterylevel(mActivity).toString().toRequestBody(MEDIA_TYPE_TEXT)
            val start_date = Comman_Methods.getcurrentDate().toRequestBody(MEDIA_TYPE_TEXT)
            val device_token_id = "".toRequestBody(MEDIA_TYPE_TEXT)
            val device_uuid = androidId.toRequestBody(MEDIA_TYPE_TEXT)
            val location_permission = "false".toRequestBody(MEDIA_TYPE_TEXT)
            val location_address = ""
                //                Utils.getCompleteAddressString(mActivity, gpstracker?.getLatitude() ?: 0.0, gpstracker?.getLongitude() ?: 0.0)
                .toRequestBody(MEDIA_TYPE_TEXT)
            val isAdminMem: RequestBody = "true".toRequestBody(MEDIA_TYPE_TEXT)
            val frequency = when {
                familyMonitorResult.frequency!=null -> if (familyMonitorResult.frequency!=0) {
                    familyMonitorResult.frequency.toString().toRequestBody(MEDIA_TYPE_TEXT)
                }else{
                    "60".toRequestBody(MEDIA_TYPE_TEXT)
                }
                else -> "60".toRequestBody(MEDIA_TYPE_TEXT)
            }
            val device_token = "".toRequestBody(MEDIA_TYPE_TEXT)

            val device_id = "1".toRequestBody(MEDIA_TYPE_TEXT)
            val paymentType = "3".toRequestBody(MEDIA_TYPE_TEXT)
            val loginByApp = "2".toRequestBody(MEDIA_TYPE_TEXT)
            val imageBody: RequestBody
            val isSms: RequestBody = "false".toRequestBody(MEDIA_TYPE_TEXT)
            val payment_token = dataValue.toRequestBody(MEDIA_TYPE_TEXT)
            val payment_method = subscriptionBean.subScriptionCode.toString().toRequestBody(MEDIA_TYPE_TEXT)
            val notification_permission: RequestBody =
                if (NotificationManagerCompat.from(mActivity).areNotificationsEnabled()) {
                    "true".toRequestBody(MEDIA_TYPE_TEXT)
                } else {
                    "false".toRequestBody(MEDIA_TYPE_TEXT)
                }
            val isChildMissing: RequestBody = "false".toRequestBody(MEDIA_TYPE_TEXT)
            val part = when {
                familyMonitorResult.image != "" -> {
                    familyMonitorResult.image = File(Comman_Methods.compressImage(familyMonitorResult.image ?: "", mActivity)).absolutePath ?: familyMonitorResult.image
                    val imageFile = File(familyMonitorResult.image ?: "")
                    if (imageFile.exists()) {
                        imageBody = imageFile.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("ProfilePath", imageFile.name, imageBody)
                    } else {
                        imageBody = nullData.toRequestBody("multipart/form-data".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("ProfilePath", null, imageBody)
                    }
                }
                else -> {
                    imageBody = nullData.toRequestBody("multipart/form-data".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("ProfilePath", null, imageBody)
                }
            }

            val callRegisrationApi = WebApiClient.getInstance(mActivity)
                .webApi_with_MultiPart?.callSignUpApi(
                    email,
                    mobile_no,
                    password,
                    created_by,
                    user_name,
                    user_id,
                    first_name,
                    last_name,
                    device_model,
                    device_company,
                    device_id,
                    device_os,
                    device_token,
                    device_type,
                    record_status,
                    longitude,
                    lattitude,
                    battery_level,
                    device_token_id,
                    start_date,
                    device_uuid,
                    location_permission,
                    location_address,
                    isSms,
                    part,
                    payment_token,
                    payment_method,
                    isAdminMem,
                    notification_permission,
                    frequency,
                    loginByApp,
                    referral_name,
                    promoCode,
                    isChildMissing,
                    paymentType)

            callRegisrationApi?.enqueue(object : retrofit2.Callback<CommonValidationResponse> {
                override fun onFailure(call: Call<CommonValidationResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(call: Call<CommonValidationResponse>, response: Response<CommonValidationResponse>) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                if (it.status == true) {
                                    if (familyMonitorResult.image!=null) {
                                        if (familyMonitorResult.image != "") {
                                            val imageFile = File(familyMonitorResult.image ?: "")
                                            if (imageFile != null) {
                                                if (imageFile.exists()) {
                                                    imageFile.delete()
                                                    if (imageFile.exists()) {
                                                        imageFile.canonicalFile.delete()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    mActivity.gotoDashBoard()
                                } else {
                                    var errorResponse: String = ""
                                    try {
                                        val gson = GsonBuilder().create()
                                        val type = object : TypeToken<String>() {}.type
                                        errorResponse = gson.fromJson(gson.toJson(it.result), type)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    mActivity.showMessage(errorResponse)
                                }
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        Utils.showSomeThingWrongMessage(mActivity)
                    }
                }
            })
        } else {
            Utils.showNoInternetMessage(mActivity)
        }
    }

    private fun callUpgradeSubscriptionApi(trial_code: Int, dataValue: String, userFirstName: String, userLastName: String) {
        mActivity.isSpeedAvailable()
        val loginObject = appDatabase.loginDao().getAll()
        val jsonObject = JsonObject()
        jsonObject.addProperty("FirstName", userFirstName)
        jsonObject.addProperty("LastName", userLastName)
        if (isFromMember && isFromPayment) {
            jsonObject.addProperty("UserID", familyMonitorResult.iD)
        } else {
            jsonObject.addProperty("UserID", loginObject.memberID)
        }
        jsonObject.addProperty("Package", trial_code)
        jsonObject.addProperty("Token", dataValue)
        jsonObject.addProperty("PaymentType", 3)

        Utils.upgradeSubscriptionApi(mActivity, jsonObject, object : CommonApiListener{
            override fun upgradeSubscription(
                status: Boolean,
                familyData: FamilyMonitorResult?,
                message: String,
                responseMessage: String
            ) {
                if (status) {
                    if (familyData != null) {
                        if (isFromMember && isFromPayment) {
                            val memberData = familyMonitorResult
                            memberData.Package = familyData.Package
                            memberData.payId = familyData.payId
                            memberData.paymentType = 3
                            memberData.isChildMissing = false
                            appDatabase.memberDao().updateMember(memberData)
                        } else {
                            val loginupdate: LoginObject = appDatabase.loginDao().getAll()
                            loginupdate.Package = familyData.Package.toString()
                            loginupdate.payId = familyData.payId ?: ""
                            loginupdate.paymentType = 3
                            loginupdate.isChildMissing = false
                            loginupdate.isFromIos = false
                            appDatabase.loginDao().updateLogin(loginupdate)
                        }
                    }
                    if (isFromActivity && isFromPayment) {
                        mActivity.gotoDashBoard()
                    } else if (isFromMember && isFromPayment) {
                        mActivity.showMessage(responseMessage)
                        mActivity.gotoDashBoard()
                    } else if (isFromPayment) {
                        mActivity.showMessage(responseMessage)
                        mActivity.onBackPressed()
                    } else if (isFromActivity) {
                        mActivity.gotoDashBoard()
                    }

                    /*val isServiceStarted =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            Utils.isJobServiceRunning(mActivity)
                        } else {
                            Utils.isMyServiceRunning(GpsService::class.java, mActivity)
                        }

                    try {
                        if (isAppIsInBackground(mActivity)) {
                            LocalBroadcastManager.getInstance(mActivity)
                                .registerReceiver(
                                    mActivity.mMessageReceiver,
                                    IntentFilter("LiveMemberCount")
                                )
                            if (!isServiceStarted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                val jobScheduler =
                                    mActivity.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                                val componentName = ComponentName(
                                    mActivity.packageName,
                                    GpsJobService::class.java.name
                                )
                                val jobInfo =
                                    JobInfo.Builder(GPSSERVICEJOBID, componentName)
                                        .setMinimumLatency(1000)
                                        .setOverrideDeadline((241 * 60000).toLong())
                                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                                        .setPersisted(true).build()
                                val resultCode = jobScheduler.schedule(jobInfo)
                                if (resultCode == JobScheduler.RESULT_SUCCESS) {
                                    Log.d(
                                        GpsJobService::class.java.name,
                                        "job scheduled"
                                    )
                                } else {
                                    Log.d(
                                        GpsJobService::class.java.name,
                                        "job schedule failed"
                                    )
                                }
                            } else if (!isServiceStarted) {
                                mActivity.startService(
                                    Intent(
                                        mActivity,
                                        GpsService::class.java
                                    )
                                )
                            }
                        } else {
                            LocalBroadcastManager.getInstance(mActivity)
                                .registerReceiver(
                                    mActivity.mMessageReceiver,
                                    IntentFilter("LiveMemberCount")
                                )
                            if (!isServiceStarted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                val jobScheduler =
                                    mActivity.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                                val componentName = ComponentName(
                                    mActivity.packageName,
                                    GpsJobService::class.java.name
                                )
                                val jobInfo =
                                    JobInfo.Builder(GPSSERVICEJOBID, componentName)
                                        .setMinimumLatency(1000)
                                        .setOverrideDeadline((241 * 60000).toLong())
                                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                                        .setPersisted(true).build()
                                val resultCode = jobScheduler.schedule(jobInfo)
                                if (resultCode == JobScheduler.RESULT_SUCCESS) {
                                    Log.d(
                                        GpsJobService::class.java.name,
                                        "job scheduled"
                                    )
                                } else {
                                    Log.d(
                                        GpsJobService::class.java.name,
                                        "job schedule failed"
                                    )
                                }
                            } else if (!isServiceStarted) {
                                mActivity.startService(
                                    Intent(
                                        mActivity,
                                        GpsService::class.java
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }*/
                } else {
                    mActivity.showMessage(message)
                }
            }
        })
    }

    private fun openDialog() {
        mActivity.openClientCallDialog()
        /*Comman_Methods.isCustomPopUpShow(mActivity,
        message = mActivity.resources.getString(R.string.cancellation_message),
        positiveButtonListener = object : PositiveButtonListener {
            override fun okClickListener() {
                callCancelSubscriptionApi()
            }
        })*/
    }

    private fun callCancelSubscriptionApi() {
        mActivity.isSpeedAvailable()
        Utils.userSubscriptionCancel(mActivity, familyMonitorResult.iD, object : CommonApiListener {
            override fun commonResponse(
                status: Boolean,
                message: String,
                responseMessage: String,
                result: String
            ) {
                if (status) {
                    val memberData = familyMonitorResult
                    memberData.IsCancelled = true
                    memberData.isChildMissing = true
                    appDatabase.memberDao().updateMember(memberData)
                    Comman_Methods.isProgressHide()
                    mActivity.onBackPressed()
                }
            }
        })
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            initializeData()
        }
    }

    inner class SubscriptionAdapter(val subscriptionTypeResultList: ArrayList<SubscriptionTypeResult>) : RecyclerView.Adapter<SubscriptionAdapter.ViewHolder>(){

        var adapterPosition: Int = -1


        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): SubscriptionAdapter.ViewHolder {
            val view = LayoutInflater.from(activity).inflate(R.layout.item_subscription, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: SubscriptionAdapter.ViewHolder, position: Int) {
            val imageWidth: Int = Utils.calculateNoOfColumns(mActivity, 2.50)
            val imageLayoutParams = holder.llSubscription.layoutParams
            /*imageLayoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            imageLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT*/
            imageLayoutParams.width = imageWidth
            holder.llSubscription.layoutParams = imageLayoutParams

            val priceLayoutParams = holder.tvPrice.layoutParams
            if (position == 0) {
                holder.llSubscription.background = ContextCompat.getDrawable(mActivity, R.drawable.subscription_back)
                holder.tvTypeSubs.background = ContextCompat.getDrawable(mActivity, R.drawable.subscription_type_back)
                holder.tvType.background = ContextCompat.getDrawable(mActivity, R.drawable.subscription_bottom_back)
            } else {
                holder.llSubscription.background = ContextCompat.getDrawable(mActivity, R.drawable.subscription_gray_back)
                holder.tvTypeSubs.background = ContextCompat.getDrawable(mActivity, R.drawable.subscription_type_gray)
                holder.tvType.background = ContextCompat.getDrawable(mActivity, R.drawable.subscription_bottom_gray)
            }
            if(adapterPosition == position) {
//                holder.tvPrice.setTextColor(ContextCompat.getColor(mActivity, R.color.colorPrimary))
                holder.tvPrice.setTextColor(ContextCompat.getColor(mActivity, R.color.lightGray))
                selectedSubscribe = SubscriptionBean(
                    subscriptionTypeResultList[position].id ?: 0,
                    subscriptionTypeResultList[position].days ?: 0,
                    subscriptionTypeResultList[position].totalCost ?: 0.0,
                    subscriptionTypeResultList[position].planId ?: ""
                )
                val imageHeight: Int = Comman_Methods.convertDpToPixels(80F, mActivity).toInt()
                priceLayoutParams.height = imageHeight
                holder.tvPrice.layoutParams = priceLayoutParams
                when (subscriptionTypeResultList[position].id) {
                    1 -> {
                        selectedSubscriptionType = -1
                    }
                    2 -> {
                        selectedSubscriptionType = 1
                    }
                    3 -> {
                        selectedSubscriptionType = 2
                    }
                    4 -> {
                        selectedSubscriptionType = 1
                    }
                    5 -> {
                        selectedSubscriptionType = 2
                    }
                }
                changeDataOfSubscription()
            } else {
                holder.tvPrice.setTextColor(ContextCompat.getColor(mActivity, R.color.lightGray))
                val imageHeight: Int = Comman_Methods.convertDpToPixels(70F, mActivity).toInt()
                priceLayoutParams.height = imageHeight
                holder.tvPrice.layoutParams = priceLayoutParams
            }

            when (subscriptionTypeResultList[position].id) {
                1 -> {
                    holder.tvTypeSubs.text = mActivity.resources.getString(R.string.str_try_free_trial, subscriptionTypeResultList[position].days.toString())
                    holder.tvPrice.text = "$ " + subscriptionTypeResultList[position + 1].totalCost.toString()
                    holder.tvType.text = subscriptionTypeResultList[position + 1].title + " " + mActivity.resources.getString(R.string.subscription)
                    holder.tvTypeSubs.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15F)
                }
                2 -> {
                    holder.tvTypeSubs.text = subscriptionTypeResultList[position].title
                    holder.tvPrice.text = "$ " + subscriptionTypeResultList[position].totalCost.toString()
                    holder.tvType.text = mActivity.resources.getString(R.string.str_monthly) + " " + mActivity.resources.getString(R.string.subscription)
                    holder.tvTypeSubs.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20F)
                }
                3 -> {
                    holder.tvTypeSubs.text = subscriptionTypeResultList[position].title
                    holder.tvPrice.text = "$ " + subscriptionTypeResultList[position].totalCost.toString()
                    holder.tvType.text = mActivity.resources.getString(R.string.str_yearly) + " " + mActivity.resources.getString(R.string.subscription)
                    holder.tvTypeSubs.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20F)
                }
                4 -> {
                    holder.tvTypeSubs.text = subscriptionTypeResultList[position].title
                    holder.tvPrice.text = "$ " + subscriptionTypeResultList[position].totalCost.toString()
                    holder.tvType.text = mActivity.resources.getString(R.string.str_monthly) + " " + mActivity.resources.getString(R.string.subscription)
                    holder.tvTypeSubs.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15F)
                }
                5 -> {
                    holder.tvTypeSubs.text = subscriptionTypeResultList[position].title
                    holder.tvPrice.text = "$ " + subscriptionTypeResultList[position].totalCost.toString()
                    holder.tvType.text = mActivity.resources.getString(R.string.str_yearly) + " " + mActivity.resources.getString(R.string.subscription)
                    holder.tvTypeSubs.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15F)
                }
            }

            holder.llSubscription.setOnClickListener {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
                adapterPosition = holder.bindingAdapterPosition
                (rvUpdateSubscription.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position,20)
                notifyDataSetChanged()
            }
        }

        override fun getItemCount(): Int {
            return subscriptionTypeResultList.size
        }
        inner class ViewHolder(itemview : View) : RecyclerView.ViewHolder(itemview) {
            var tvTypeSubs: TextView = itemview.tvTypeSubs
            var tvPrice: TextView = itemview.tvPrice
            var tvType: TextView = itemview.tvType
            var llSubscription: LinearLayout = itemview.llSubscription
        }
    }
}