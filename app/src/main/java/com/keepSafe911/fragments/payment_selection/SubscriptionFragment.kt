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
import com.keepSafe911.BuildConfig
import com.keepSafe911.R
import com.keepSafe911.fragments.LoginFragment
import com.keepSafe911.fragments.commonfrag.MainBaseFragment
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.model.FamilyMonitorResult
import com.keepSafe911.model.SubscriptionBean
import com.keepSafe911.model.response.SubscriptionTypeResult
import com.keepSafe911.utils.*
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_subscription.*
import kotlinx.android.synthetic.main.item_subscription.view.*
import kotlinx.android.synthetic.main.toolbar_header.*
import java.util.*
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class SubscriptionFragment : MainBaseFragment(), View.OnClickListener {
    private var subscriptionTypeResultList: ArrayList<SubscriptionTypeResult> = ArrayList()
    private var familyMonitorResult = FamilyMonitorResult()
    private var selectedSubscribe: SubscriptionBean = SubscriptionBean()

    companion object {
        fun newInstance(
            familyMonitorResult: FamilyMonitorResult
        ): SubscriptionFragment {
            val args = Bundle()
            args.putParcelable(ARG_PARAM1, familyMonitorResult)
            val fragment = SubscriptionFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            familyMonitorResult = it.getParcelable(ARG_PARAM1) ?: FamilyMonitorResult()
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
        btn_logout.visibility = View.GONE
        callSubscriptionTypeApi()
        spannableData()
        setHeader()
    }

    private fun setHeader() {
        iv_back.visibility = View.VISIBLE
        tvHeader.text = mActivity.resources.getString(R.string.str_my_subscription)
        Utils.setTextGradientColor(tvHeader)
        tvHeader.setPadding(0, 0, 50, 0)
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
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
                                btn_free.text = mActivity.resources.getString(
                                    R.string.free_trial,
                                    days.toString()
                                )
                            }
                            2 -> {
                                btn_month.text = mActivity.resources.getString(
                                    R.string.sub_month_trial,
                                    totalCost.toString()
                                )
                            }
                            3 -> {
                                btn_year.text = mActivity.resources.getString(
                                    R.string.sub_year_trial,
                                    totalCost.toString()
                                )
                            }
                        }
                    }
                    addClicklistneres()
                }
            }
        })
    }

    private fun setupRecyclerview(subscriptionTypeResultList: ArrayList<SubscriptionTypeResult>) {
        val adapter = SubscriptionAdapter(subscriptionTypeResultList)
        rvSubscription.layoutManager = LinearLayoutManager(mActivity, LinearLayoutManager.HORIZONTAL, false)
        rvSubscription.adapter = adapter
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
        tv_skip.text = definition1
        tv_skip.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun addClicklistneres() {
        btn_free.setOnClickListener(this)
        btn_month.setOnClickListener(this)
        btn_year.setOnClickListener(this)
        btnSubscribe.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_free -> {
                mActivity.hideKeyboard()
                if (subscriptionTypeResultList.size > 0) {
                    Comman_Methods.avoidDoubleClicks(v)
                    mActivity.addFragment(
                        PaymentMethodFragment.newInstance(
                            familyMonitorResult,
                            SubscriptionBean(
                                subscriptionTypeResultList[0].id ?: 0,
                                subscriptionTypeResultList[0].days ?: 0,
                                subscriptionTypeResultList[0].totalCost ?: 0.0,
                                subscriptionTypeResultList[0].planId ?: ""
                            )
                        ), true, true, AnimationType.fadeInfadeOut
                    )
                }
            }
            R.id.btn_month -> {
                mActivity.hideKeyboard()
                if (subscriptionTypeResultList.size > 0) {
                    Comman_Methods.avoidDoubleClicks(v)
                    mActivity.addFragment(
                        PaymentMethodFragment.newInstance(
                            familyMonitorResult,
                            SubscriptionBean(
                                subscriptionTypeResultList[1].id ?: 0,
                                subscriptionTypeResultList[1].days ?: 0,
                                subscriptionTypeResultList[1].totalCost ?: 0.0,
                                subscriptionTypeResultList[1].planId ?: ""
                            )
                        ), true, true, AnimationType.fadeInfadeOut
                    )
                }
            }
            R.id.btn_year -> {
                mActivity.hideKeyboard()
                if (subscriptionTypeResultList.size > 0) {
                    Comman_Methods.avoidDoubleClicks(v)
                    mActivity.addFragment(
                        PaymentMethodFragment.newInstance(
                            familyMonitorResult,
                            SubscriptionBean(
                                subscriptionTypeResultList[2].id ?: 0,
                                subscriptionTypeResultList[2].days ?: 0,
                                subscriptionTypeResultList[2].totalCost ?: 0.0,
                                subscriptionTypeResultList[2].planId ?: ""
                            )
                        ), true, true, AnimationType.fadeInfadeOut
                    )
                }
            }
            R.id.btnSubscribe -> {
                mActivity.hideKeyboard()
                if (selectedSubscribe.subScriptionCode > 0) {
                    Comman_Methods.avoidDoubleClicks(v)
                    mActivity.addFragment(
                        PaymentMethodFragment.newInstance(
                            familyMonitorResult,
                            selectedSubscribe
                        ), true, true, AnimationType.fadeInfadeOut
                    )
                }
            }
        }
    }

    inner class SubscriptionAdapter(subscriptionTypeResultList: ArrayList<SubscriptionTypeResult>) : RecyclerView.Adapter<SubscriptionAdapter.ViewHolder>(){

        var adapterPosition: Int = 0


        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): SubscriptionAdapter.ViewHolder {
            val view = LayoutInflater.from(activity).inflate(R.layout.item_subscription, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: SubscriptionAdapter.ViewHolder, position: Int) {
            val imageWidth: Int = Utils.calculateNoOfColumns(mActivity, 3.25)
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
                (rvSubscription.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position,20)
                notifyDataSetChanged()
            }
        }

        override fun getItemCount(): Int {
            return 3
        }
        inner class ViewHolder(itemview : View) : RecyclerView.ViewHolder(itemview) {
            var tvTypeSubs: TextView = itemview.tvTypeSubs
            var tvPrice: TextView = itemview.tvPrice
            var tvType: TextView = itemview.tvType
            var llSubscription: LinearLayout = itemview.llSubscription
        }
    }
}