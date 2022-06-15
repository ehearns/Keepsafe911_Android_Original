package com.keepSafe911.fragments

import addFragment
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.keepSafe911.BuildConfig
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.MainBaseFragment
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.model.SubscriptionBean
import com.keepSafe911.model.response.SubscriptionTypeResult
import com.keepSafe911.utils.*
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_landing_two.*
import kotlinx.android.synthetic.main.item_subscription.view.*
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class LandingTwoFragment : MainBaseFragment(), View.OnClickListener {
    private var param1: String? = ""
    private var param2: String? = ""
    private lateinit var privacyTermsFragment: PrivacyTermsFragment
    private var subscriptionTypeResultList: ArrayList<SubscriptionTypeResult> = ArrayList()
    private var subscriptionAdapter: SubscriptionAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1, "")
            param2 = it.getString(ARG_PARAM2, "")
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
        return inflater.inflate(R.layout.fragment_landing_two, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvPrivacyPolicy.setOnClickListener(this)
        tvTermsCondition.setOnClickListener(this)
        tvContinueBasic.setOnClickListener(this)
        ivLandingBack.setOnClickListener(this)
        changeDrawableStart()
        callSubscriptionTypeApi()
        tvLandingTwoDetail.text = mActivity.resources.getString(R.string.str_landing_two_desc, 14)
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            LandingTwoFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private fun changeDrawableStart() {
        val privacyPolicyChecked = AppPreference.getBooleanPreference(mActivity, BuildConfig.privacyPolicyPrefKey)
        val termsCondition = AppPreference.getBooleanPreference(mActivity, BuildConfig.termsConditionPrefKey)
        val privacyDrawable = if (privacyPolicyChecked) R.drawable.ic_white_check else R.drawable.ic_white_blank
        val termsDrawable = if (termsCondition) R.drawable.ic_white_check else R.drawable.ic_white_blank
        tvPrivacyPolicy.setCompoundDrawablesWithIntrinsicBounds(privacyDrawable, 0, 0, 0)
        tvTermsCondition.setCompoundDrawablesWithIntrinsicBounds(termsDrawable, 0, 0, 0)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ivLandingBack -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                mActivity.onBackPressed()
            }
            R.id.tvPrivacyPolicy -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                openPrivacyTermsScreen(mActivity.resources.getString(R.string.str_privacy_policy), 0)
            }
            R.id.tvTermsCondition -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                openPrivacyTermsScreen(mActivity.resources.getString(R.string.str_terms_service), 1)
            }
            R.id.tvContinueBasic -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                if (privacyTermsValidation()) {
                    subscriptionAdapter?.reloadData()
                    mActivity.addFragment(
                        SignUpFragment.newInstance(true),
                        true,
                        true,
                        animationType = AnimationType.fadeInfadeOut
                    )
                }
            }
        }
    }

    private fun openPrivacyTermsScreen(screenName: String, linkType: Int) {
        if (this@LandingTwoFragment::privacyTermsFragment.isInitialized) {
            if (privacyTermsFragment != null) {
                if (privacyTermsFragment.isAdded) {
                    return
                }
            }
        }
        privacyTermsFragment = PrivacyTermsFragment.newInstance(screenName, linkType)
        privacyTermsFragment.privacyTermsListener = object : CommonApiListener{
            override fun privacyTermsChecked(type: Int, accepted: Boolean) {
                if (type == 0) {
                    AppPreference.saveBooleanPreference(mActivity, BuildConfig.privacyPolicyPrefKey, true)
                } else if (type == 1) {
                    AppPreference.saveBooleanPreference(mActivity, BuildConfig.termsConditionPrefKey, true)
                }
                changeDrawableStart()
            }
        }
        privacyTermsFragment.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.AppTheme)
        privacyTermsFragment.show(mActivity.supportFragmentManager, "privacyTermsFragment")
    }

    private fun privacyTermsValidation(): Boolean{
        val privacyPolicyChecked = AppPreference.getBooleanPreference(mActivity, BuildConfig.privacyPolicyPrefKey)
        val termsCondition = AppPreference.getBooleanPreference(mActivity, BuildConfig.termsConditionPrefKey)
        return if (!privacyPolicyChecked && !termsCondition) {
            mActivity.showMessage(mActivity.resources.getString(R.string.str_visit_privacy_terms))
            false
        } else if (!privacyPolicyChecked) {
            mActivity.showMessage(mActivity.resources.getString(R.string.str_visit_privacy))
            false
        } else if (!termsCondition) {
            mActivity.showMessage(mActivity.resources.getString(R.string.str_visit_terms))
            false
        } else {
            true
        }
    }

    private fun setupRecyclerview(subscriptionTypeResultList: ArrayList<SubscriptionTypeResult>) {
        var filterSubscriptionBean: ArrayList<SubscriptionTypeResult> = ArrayList()
        if (subscriptionTypeResultList.size > 0) {
            filterSubscriptionBean = subscriptionTypeResultList.filter { data -> data.id!! < 4 } as ArrayList<SubscriptionTypeResult>
        }
        subscriptionAdapter = SubscriptionAdapter(filterSubscriptionBean)
        rvLandingSubscription.layoutManager = LinearLayoutManager(mActivity, LinearLayoutManager.HORIZONTAL, false)
        rvLandingSubscription.adapter = subscriptionAdapter
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
                        when (subscriptionTypeResultList[i].id) {
                            1 -> {
                                tvLandingTwoDetail?.text = mActivity.resources.getString(R.string.str_landing_two_desc, days)
                            }
                        }
                    }
                }
            }
        })
    }

    inner class SubscriptionAdapter(var dataSubscriptionList: ArrayList<SubscriptionTypeResult>) : RecyclerView.Adapter<SubscriptionAdapter.ViewHolder>(){

        var adapterPosition: Int = -1

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): SubscriptionAdapter.ViewHolder {
            val view = LayoutInflater.from(activity).inflate(R.layout.item_subscription, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: SubscriptionAdapter.ViewHolder, position: Int) {
            if (dataSubscriptionList[position].id == 2) {
                holder.llSubscription.visibility = View.GONE
            } else {
                holder.llSubscription.visibility = View.VISIBLE
            }
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
                val imageHeight: Int = Comman_Methods.convertDpToPixels(80F, mActivity).toInt()
                priceLayoutParams.height = imageHeight
                holder.tvPrice.layoutParams = priceLayoutParams

                mActivity.addFragment(
                    SignUpFragment.newInstance(false, SubscriptionBean(
                        dataSubscriptionList[position].id ?: 0,
                        dataSubscriptionList[position].days ?: 0,
                        dataSubscriptionList[position].totalCost ?: 0.0,
                        dataSubscriptionList[position].planId ?: ""
                    )),
                    true,
                    true,
                    animationType = AnimationType.fadeInfadeOut)

            } else {
                holder.tvPrice.setTextColor(ContextCompat.getColor(mActivity, R.color.lightGray))
                val imageHeight: Int = Comman_Methods.convertDpToPixels(70F, mActivity).toInt()
                priceLayoutParams.height = imageHeight
                holder.tvPrice.layoutParams = priceLayoutParams
            }

            when (dataSubscriptionList[position].id) {
                1 -> {
                    holder.tvTypeSubs.text = mActivity.resources.getString(R.string.str_try_free_trial, dataSubscriptionList[position].days.toString())
                    holder.tvPrice.text = "$ " + dataSubscriptionList[position + 1].totalCost.toString()
                    holder.tvType.text = dataSubscriptionList[position + 1].title + " " + mActivity.resources.getString(R.string.subscription)
                    holder.tvTypeSubs.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15F)
                }
                2 -> {
                    holder.tvTypeSubs.text = dataSubscriptionList[position].title
                    holder.tvPrice.text = "$ " + dataSubscriptionList[position].totalCost.toString()
                    holder.tvType.text = mActivity.resources.getString(R.string.str_monthly) + " " + mActivity.resources.getString(R.string.subscription)
                    holder.tvTypeSubs.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20F)
                }
                3 -> {
                    holder.tvTypeSubs.text = dataSubscriptionList[position].title
                    holder.tvPrice.text = "$ " + dataSubscriptionList[position].totalCost.toString()
                    holder.tvType.text = mActivity.resources.getString(R.string.str_yearly) + " " + mActivity.resources.getString(R.string.subscription)
                    holder.tvTypeSubs.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20F)
                }
                4 -> {
                    holder.tvTypeSubs.text = dataSubscriptionList[position].title
                    holder.tvPrice.text = "$ " + dataSubscriptionList[position].totalCost.toString()
                    holder.tvType.text = mActivity.resources.getString(R.string.str_monthly) + " " + mActivity.resources.getString(R.string.subscription)
                    holder.tvTypeSubs.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15F)
                }
                5 -> {
                    holder.tvTypeSubs.text = dataSubscriptionList[position].title
                    holder.tvPrice.text = "$ " + dataSubscriptionList[position].totalCost.toString()
                    holder.tvType.text = mActivity.resources.getString(R.string.str_yearly) + " " + mActivity.resources.getString(R.string.subscription)
                    holder.tvTypeSubs.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15F)
                }
            }

            holder.llSubscription.setOnClickListener {
                mActivity.hideKeyboard()
                if (privacyTermsValidation()) {
                    Comman_Methods.avoidDoubleClicks(it)
                    adapterPosition = holder.bindingAdapterPosition
                    (rvLandingSubscription.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                        position,
                        20
                    )
                    notifyDataSetChanged()
                }
            }
        }

        fun reloadData() {
            adapterPosition = -1
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int {
            return dataSubscriptionList.size
        }
        inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
            var tvTypeSubs: TextView = itemView.tvTypeSubs
            var tvPrice: TextView = itemView.tvPrice
            var tvType: TextView = itemView.tvType
            var llSubscription: LinearLayout = itemView.llSubscription
        }
    }
}