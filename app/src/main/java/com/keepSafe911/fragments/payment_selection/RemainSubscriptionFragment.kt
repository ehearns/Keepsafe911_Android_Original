package com.keepSafe911.fragments.payment_selection


import AnimationType
import addFragment
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject
import com.keepSafe911.BuildConfig
import com.keepSafe911.R
import com.keepSafe911.fragments.LoginFragment
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.listner.PaymentOptionListener

import com.keepSafe911.model.FamilyMonitorResult
import com.keepSafe911.model.SubscriptionBean
import com.keepSafe911.model.response.SubscriptionTypeResult
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_subscription.*
import kotlinx.android.synthetic.main.fragment_subscription.btn_month
import kotlinx.android.synthetic.main.fragment_subscription.btn_year
import kotlinx.android.synthetic.main.fragment_subscription.prime_text
import kotlinx.android.synthetic.main.item_subscription.view.*
import kotlinx.android.synthetic.main.toolbar_header.*
import java.util.*
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val ARG_PARAM3 = "param3"
private const val ARG_PARAM4 = "param4"
private const val ARG_PARAM5 = "param5"

class RemainSubscriptionFragment : HomeBaseFragment(), View.OnClickListener {
    private var subscriptionTypeResultList: ArrayList<SubscriptionTypeResult> = ArrayList()
    private var showFreeTrial: Boolean = false
    private var showLogout: Boolean = false
    private var showBack: Boolean = false
    private var isFromMember: Boolean = false
    private var familyMonitorResult: FamilyMonitorResult = FamilyMonitorResult()
    private var selectedSubscribe: SubscriptionBean = SubscriptionBean()
    private lateinit var appDatabase: OldMe911Database

    companion object {
        fun newInstance(
            showFreeTrial: Boolean = false,
            isFromMember: Boolean = false,
            memberDetail: FamilyMonitorResult = FamilyMonitorResult(),
            showLogout: Boolean = false,
            showBack: Boolean = false
        ): RemainSubscriptionFragment {
            val args = Bundle()
            args.putBoolean(ARG_PARAM1, showFreeTrial)
            args.putBoolean(ARG_PARAM2, isFromMember)
            args.putParcelable(ARG_PARAM3, memberDetail)
            args.putBoolean(ARG_PARAM4, showLogout)
            args.putBoolean(ARG_PARAM5, showBack)
            val fragment = RemainSubscriptionFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            showFreeTrial = it.getBoolean(ARG_PARAM1, false)
            isFromMember = it.getBoolean(ARG_PARAM2, false)
            familyMonitorResult = it.getParcelable(ARG_PARAM3) ?: FamilyMonitorResult()
            showLogout = it.getBoolean(ARG_PARAM4, false)
            showBack = it.getBoolean(ARG_PARAM5, false)
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
        return inflater.inflate(R.layout.fragment_subscription, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mActivity.disableDrawer()
        appDatabase = OldMe911Database.getDatabase(mActivity)
        rvSubscription?.layoutManager = LinearLayoutManager(mActivity, LinearLayoutManager.HORIZONTAL, false)
        /*if (showFreeTrial) {
            btn_free.visibility = View.VISIBLE
            add_one.visibility = View.VISIBLE
        } else {
            btn_free.visibility = View.GONE
            add_one.visibility = View.GONE
        }*/
        if (isFromMember) {
            prime_text?.text = mActivity.resources.getString(R.string.add_additional_user)
        } else {
            if (showFreeTrial) {
                prime_text?.text = mActivity.resources.getString(R.string.str_landing_two_desc, 14)
            } else {
                prime_text?.text = mActivity.resources.getString(R.string.prime_text)
            }
        }

        btn_logout?.visibility = View.GONE
        setHeader()
        callSubscriptionTypeApi()
        spannableData()
    }

    private fun setHeader() {
        if (showFreeTrial) {
            welcome_text?.text = mActivity.resources.getString(R.string.str_landing_two_title)
        } else {
            welcome_text?.text = mActivity.resources.getString(R.string.str_my_subscription)
        }
        /*tvHeader.text = mActivity.resources.getString(R.string.str_my_subscription)
        Utils.setTextGradientColor(tvHeader)
        tvHeader.setPadding(0, 0, 0, 0)
        if (showLogout) {
            if (!showBack) {
                tvHeader.setPadding(15, 0, 0, 0)
            }
        } else {
            if (showBack) {
                tvHeader.setPadding(0, 0, 50, 0)
            }
        }*/
        iv_back.visibility = if (showBack) View.VISIBLE else View.GONE
        ivLogout.visibility = if (showLogout) View.VISIBLE else View.GONE
        ivLogout.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.callLogOutPingApi(LOGOUT_RECORD_STATUS.toString(), true)
        }
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
        mActivity.checkUserActive()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            setHeader()
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
                    setupRecyclerview(subscriptionTypeResultList)
                    for (i in 0 until subscriptionTypeResultList.size) {
                        val days = subscriptionTypeResultList[i].days ?: 0
                        val totalCost: Double = subscriptionTypeResultList[i].totalCost ?: 0.0
                        when (subscriptionTypeResultList[i].id) {
                            1 -> {
                                btn_free?.text = mActivity.resources.getString(
                                    R.string.free_trial,
                                    days.toString()
                                )
                                if (showFreeTrial) {
                                    prime_text?.text = mActivity.resources.getString(R.string.str_landing_two_desc, days)
                                } else {
                                    prime_text?.text = mActivity.resources.getString(R.string.prime_text)
                                }
                            }
                            2 -> {
                                if (!isFromMember) {
                                    btn_month?.text = mActivity.resources.getString(
                                        R.string.sub_month_trial,
                                        totalCost.toString()
                                    )
                                }
                            }
                            3 -> {
                                if (!isFromMember) {
                                    btn_year?.text = mActivity.resources.getString(
                                        R.string.sub_year_trial,
                                        totalCost.toString()
                                    )
                                }
                            }
                            4 -> {
                                if (isFromMember) {
                                    btn_month?.text = mActivity.resources.getString(
                                        R.string.month_trial,
                                        totalCost.toString()
                                    )
                                }
                            }
                            5 -> {
                                if (isFromMember) {
                                    btn_year?.text = mActivity.resources.getString(
                                        R.string.year_trial,
                                        totalCost.toString()
                                    )
                                }
                            }
                        }
                        addClickListeners()
                    }
                }
            }
        })
    }

    private fun setupRecyclerview(subscriptionTypeResultList: ArrayList<SubscriptionTypeResult>) {
        var filterSubscriptionBean: ArrayList<SubscriptionTypeResult> = ArrayList()
        if (subscriptionTypeResultList.size > 0) {
            if (isFromMember) {
                if (subscriptionTypeResultList.size > 3) {
                    filterSubscriptionBean =
                        subscriptionTypeResultList.filter { data -> data.id!! in 4..5 } as ArrayList<SubscriptionTypeResult>
                }
            } else {
                filterSubscriptionBean = if (showFreeTrial) {
                    subscriptionTypeResultList.filter { data -> data.id!! < 4 } as ArrayList<SubscriptionTypeResult>
                } else {
                    subscriptionTypeResultList.filter { data -> data.id!! in 2..3 } as ArrayList<SubscriptionTypeResult>
                }
            }
        }
        val adapter = SubscriptionAdapter(filterSubscriptionBean)
        rvSubscription?.adapter = adapter
        adapter.notifyDataSetChanged()
    }

    private fun spannableData() {
        val definition1 = SpannableString(mActivity.resources.getString(R.string.skip_text))
        val clickableSpan: ClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                mActivity.addFragment(LoginFragment(), false, true, animationType = AnimationType.fadeInfadeOut)
            }
        }
        definition1.setSpan(
            ForegroundColorSpan(Color.GREEN),
            17,
            mActivity.resources.getString(R.string.skip_text).length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        definition1.setSpan(
            clickableSpan,
            17,
            mActivity.resources.getString(R.string.skip_text).length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tv_skip?.text = definition1
        tv_skip?.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun addClickListeners() {
        btn_free?.setOnClickListener(this)
        btn_month?.setOnClickListener(this)
        btn_year?.setOnClickListener(this)
        btn_logout?.setOnClickListener(this)
        btnSubscribe?.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_free -> {
                mActivity.hideKeyboard()
                if (subscriptionTypeResultList.size > 0) {
                    Comman_Methods.avoidDoubleClicks(v)
                    doingPayment(SubscriptionBean(
                        subscriptionTypeResultList[0].id ?: 0,
                        subscriptionTypeResultList[0].days ?: 0,
                        subscriptionTypeResultList[0].totalCost ?: 0.0,
                        subscriptionTypeResultList[0].planId ?: ""
                    ))
                }
            }
            R.id.btn_month -> {
                mActivity.hideKeyboard()
                if (subscriptionTypeResultList.size > 0) {
                    Comman_Methods.avoidDoubleClicks(v)
                    val subscriptionData = if (isFromMember) {
                        SubscriptionBean(
                            subscriptionTypeResultList[3].id ?: 0,
                            subscriptionTypeResultList[3].days ?: 0,
                            subscriptionTypeResultList[3].totalCost ?: 0.0,
                            subscriptionTypeResultList[3].planId ?: ""
                        )
                    } else {
                        SubscriptionBean(
                            subscriptionTypeResultList[1].id ?: 0,
                            subscriptionTypeResultList[1].days ?: 0,
                            subscriptionTypeResultList[1].totalCost ?: 0.0,
                            subscriptionTypeResultList[1].planId ?: ""
                        )
                    }
                    doingPayment(subscriptionData)
                }
            }
            R.id.btn_year -> {
                mActivity.hideKeyboard()
                if (subscriptionTypeResultList.size > 0) {
                    Comman_Methods.avoidDoubleClicks(v)
                    val subscriptionData = if (isFromMember) {
                        SubscriptionBean(
                            subscriptionTypeResultList[4].id ?: 0,
                            subscriptionTypeResultList[4].days ?: 0,
                            subscriptionTypeResultList[4].totalCost ?: 0.0,
                            subscriptionTypeResultList[4].planId ?: ""
                        )
                    } else {
                        SubscriptionBean(
                            subscriptionTypeResultList[2].id ?: 0,
                            subscriptionTypeResultList[2].days ?: 0,
                            subscriptionTypeResultList[2].totalCost ?: 0.0,
                            subscriptionTypeResultList[2].planId ?: ""
                        )
                    }
                    doingPayment(subscriptionData)
                }
            }
            R.id.btnSubscribe -> {
                mActivity.hideKeyboard()
                if (selectedSubscribe.subScriptionCode > 0) {
                    Comman_Methods.avoidDoubleClicks(v)
                    doingPayment(selectedSubscribe)
                }
            }
            R.id.btn_logout -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                mActivity.callLogOutPingApi(LOGOUT_RECORD_STATUS.toString(), true)
            }
        }
    }

    private fun doingPayment(subscriptionBean: SubscriptionBean) {
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
            subscriptionBean, object : PaymentOptionListener {
                override fun onCreditCardOption() {
                    mActivity.addFragment(
                        RemainPaymentMethodFragment.newInstance(
                            showFreeTrial, subscriptionBean,
                            isFromMember, familyMonitorResult
                        ), true, true, AnimationType.fadeInfadeOut
                    )
                }

                override fun onPayPalOption(
                    subscriptionId: String, firstName: String,
                    lastName: String, email: String
                ) {
                    mActivity.callRemainSubscriptionApi(subscriptionId,
                        subscriptionBean, 3,
                        familyMonitorResult, isFromMember, userFirstName, userLastName)
                }
            })
    }

    inner class SubscriptionAdapter(val subscriptionTypeResultList: ArrayList<SubscriptionTypeResult>) : RecyclerView.Adapter<SubscriptionAdapter.ViewHolder>(){

        var adapterPosition: Int = 0


        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): SubscriptionAdapter.ViewHolder {
            val view = LayoutInflater.from(activity).inflate(R.layout.item_subscription, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: SubscriptionAdapter.ViewHolder, position: Int) {
            if (subscriptionTypeResultList[position].id == 2 && showFreeTrial) {
                holder.llSubscription.visibility = View.GONE
            } else {
                holder.llSubscription.visibility = View.VISIBLE
            }
            val imageWidth: Int = if (showFreeTrial) {
//                Utils.calculateNoOfColumns(mActivity, 3.25)
                Utils.calculateNoOfColumns(mActivity, 2.50)
            } else {
                Utils.calculateNoOfColumns(mActivity, 2.1)
            }
//            val imageWidth: Int = Comman_Methods.convertDpToPixels(180F, activity!!).toInt()
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
                (rvSubscription?.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position,20)
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