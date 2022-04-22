package com.keepSafe911.fragments.homefragment.profile


import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.keepSafe911.BuildConfig

import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.model.MemberBean
import com.keepSafe911.model.response.SubScriptionHistoryResponse
import com.keepSafe911.model.response.SubScriptionResult
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import com.keepSafe911.webservices.WebApiClient
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_subscription_history.*
import kotlinx.android.synthetic.main.raw_subscription_history.view.*
import kotlinx.android.synthetic.main.toolbar_header.*
import retrofit2.Call
import retrofit2.Response
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class SubscriptionHistoryFragment : HomeBaseFragment() {

    var subscriptionHistoryList: ArrayList<SubScriptionResult> = ArrayList()
    var memberList: ArrayList<MemberBean> = ArrayList()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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
        return inflater.inflate(R.layout.fragment_subscription_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity.disableDrawer()
        setHeader()
        callSubscriptionHistoryApi()
    }

    private fun callSubscriptionHistoryApi() {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            Comman_Methods.isProgressShow(mActivity)
            mActivity.isSpeedAvailable()
            subscriptionHistoryList = ArrayList()
            val appDatabase = OldMe911Database.getDatabase(mActivity)
            val loginObject = appDatabase.loginDao().getAll()
            val callSubscriptionHistory =
                WebApiClient.getInstance(mActivity).webApi_without?.callSubscriptionHistory(loginObject.memberID)

            callSubscriptionHistory?.enqueue(object : retrofit2.Callback<SubScriptionHistoryResponse> {
                override fun onFailure(call: Call<SubScriptionHistoryResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(call: Call<SubScriptionHistoryResponse>, response: Response<SubScriptionHistoryResponse>) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                if (it.isStatus) {
                                    subscriptionHistoryList = it.result ?: ArrayList()
                                    if (subscriptionHistoryList.size > 0) {
                                        tvNoSubHistory.visibility = View.GONE
                                        rvSubscriptionHistory.visibility = View.VISIBLE
                                    } else {
                                        tvNoSubHistory.visibility = View.VISIBLE
                                        rvSubscriptionHistory.visibility = View.GONE
                                    }
                                    setAdapter()
                                } else {
                                    tvNoSubHistory.visibility = View.VISIBLE
                                    rvSubscriptionHistory.visibility = View.GONE
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
            tvNoSubHistory.visibility = View.VISIBLE
            rvSubscriptionHistory.visibility = View.GONE
            Utils.showNoInternetMessage(mActivity)
        }
    }

    private fun setAdapter() {
        if (rvSubscriptionHistory!=null) {
            rvSubscriptionHistory.layoutManager = LinearLayoutManager(mActivity, RecyclerView.VERTICAL, false)
            val subscriptionHistoryAdapter =
                SubscriptionHistoryAdapter(mActivity, subscriptionHistoryList)
            rvSubscriptionHistory.adapter = subscriptionHistoryAdapter
            subscriptionHistoryAdapter.notifyDataSetChanged()
        }
    }

    private fun setHeader() {
        iv_back.visibility = View.VISIBLE
        tvHeader.text = mActivity.resources.getString(R.string.subscription_history)
        Utils.setTextGradientColor(tvHeader)
        tvHeader.setPadding(0, 0, 50, 0)
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
        mActivity.checkUserActive()
    }

    class SubscriptionHistoryAdapter(
        private val context: Context,
        private val subScriptionResult: ArrayList<SubScriptionResult>
    ) :
        RecyclerView.Adapter<SubscriptionHistoryAdapter.SubscriptionHolder>() {
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): SubscriptionHolder {
            return SubscriptionHolder(
                LayoutInflater.from(context).inflate(R.layout.raw_subscription_history, p0, false)
            )
        }

        override fun getItemCount(): Int {
            return subScriptionResult.size
        }

        override fun onBindViewHolder(holder: SubscriptionHolder, position: Int) {
            val subscriptionStartDate: String = subScriptionResult[position].startDate ?: ""
            val subscriptionEndDate: String = subScriptionResult[position].endDate ?: ""
            val subscriptionRenewalDate: String = subScriptionResult[position].renewalDate ?: ""
            val subscriptionCost: Double = subScriptionResult[position].cost ?: 0.0
            val subscriptionPackageName: String = subScriptionResult[position].packageName ?: ""
            val formatter1 = SimpleDateFormat(INPUT_DATE_FORMAT)
            val target = SimpleDateFormat(OUTPUT_DATE_FORMAT)
            var diagStartDate = ""
            var diagEndDate = ""
            var upgradeDate = ""
            try {
                val date1: Date? = formatter1.parse(subscriptionStartDate)
                val date2: Date? = formatter1.parse(subscriptionEndDate)
                val date3: Date? = formatter1.parse(subscriptionRenewalDate)
                if (date1 != null) {
                    diagStartDate = target.format(date1)
                }
                if (date2 != null) {
                    diagEndDate = target.format(date2)
                }
                if (date3 != null) {
                    upgradeDate = target.format(date3)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val decimalSymbols = DecimalFormatSymbols.getInstance(Locale.US)
            holder.tvSubscriptionCost.text = context.resources.getString(R.string.subscrip_cost) + DecimalFormat("##.##", decimalSymbols).format(subscriptionCost)
            holder.tvSubscriptionEndDate.text = context.resources.getString(R.string.subscrip_end_date) + diagEndDate
            holder.tvSubscriptionStartDate.text =
                context.resources.getString(R.string.start_date) + ": " + "\n" + diagStartDate
            if (subScriptionResult[position].isActive == true) {
                if (upgradeDate != "") {
                    holder.tvSubscriptionRenewDate.text =
                        context.resources.getString(R.string.renew_date) + ": \n" + upgradeDate
                } else {
                    holder.tvSubscriptionRenewDate.text = ""
                }
            } else {
                holder.tvSubscriptionRenewDate.text = ""
            }
            holder.tvSubscriptionType.text = if (subscriptionPackageName.isNotEmpty()) context.resources.getString(R.string.str_subscrip)+subscriptionPackageName else ""

            if (subScriptionResult[position].isActive == true) {
                val wordSpan = SpannableString(
                    context.resources.getString(R.string.subscrip_status) + context.resources.getString(R.string.active)
                )
                wordSpan.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(context, R.color.Date_bg)),
                    wordSpan.length - 6, wordSpan.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                holder.tvSubscriptionStatus.text = wordSpan
            } else {
                val wordSpan = SpannableString(
                    context.resources.getString(R.string.subscrip_status) + context.resources.getString(R.string.de_active)
                )
                wordSpan.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(context, R.color.color_red)),
                    wordSpan.length - 9, wordSpan.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                holder.tvSubscriptionStatus.text = wordSpan
            }
        }

        class SubscriptionHolder(view: View) : RecyclerView.ViewHolder(view) {
            var tvSubscriptionStartDate: TextView = view.tvSubscriptionStartDate
            var tvSubscriptionEndDate: TextView = view.tvSubscriptionEndDate
            var tvSubscriptionRenewDate: TextView = view.tvSubscriptionRenewDate
            var tvSubscriptionType: TextView = view.tvSubscriptionType
            var tvSubscriptionCost: TextView = view.tvSubscriptionCost
            var tvSubscriptionStatus: TextView = view.tvSubscriptionStatus
        }
    }
}