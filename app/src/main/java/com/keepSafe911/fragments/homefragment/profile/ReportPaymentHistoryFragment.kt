package com.keepSafe911.fragments.homefragment.profile


import addFragment
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.keepSafe911.BuildConfig

import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.fragments.homefragment.report.BusinessTrackFragment
import com.keepSafe911.fragments.homefragment.report.ReportFragment
import com.keepSafe911.model.MemberBean
import com.keepSafe911.model.response.FrequencyHistoryResult
import com.keepSafe911.model.response.FrequencyPremiumHistoryResponse
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import com.keepSafe911.webservices.WebApiClient
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_report_payment_history.*
import kotlinx.android.synthetic.main.toolbar_header.*
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList



private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ReportPaymentHistoryFragment : HomeBaseFragment(), View.OnClickListener {

    lateinit var appDatabase: OldMe911Database
    private var frequencyPremiumReportList: ArrayList<FrequencyHistoryResult> = ArrayList()
    var memberList: ArrayList<MemberBean> = ArrayList()

    companion object {
        fun newInstance(
            paymentMemberList: ArrayList<FrequencyHistoryResult>
        ): ReportPaymentHistoryFragment {
            val args = Bundle()
            args.putParcelableArrayList(ARG_PARAM1, paymentMemberList)
            val fragment = ReportPaymentHistoryFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            frequencyPremiumReportList = it.getParcelableArrayList(ARG_PARAM1) ?: java.util.ArrayList()
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
        return inflater.inflate(R.layout.fragment_report_payment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appDatabase = OldMe911Database.getDatabase(mActivity)
        mActivity.enableDrawer()
        fabAddFrequencyPremium.setOnClickListener(this)
        setHeader()
        callReportHistoryApi()
    }

    private fun setFrequencyAdapter() {
        if (frequencyPremiumReportList.size > 0){
            tvNoReportPayHistory.visibility = View.GONE
            rvReportPaymentHistory.visibility = View.VISIBLE
            rvReportPaymentHistory.layoutManager = LinearLayoutManager(mActivity, RecyclerView.VERTICAL,false)
            rvReportPaymentHistory.adapter = ReportPaymentAdapter(mActivity, frequencyPremiumReportList)
        }else{
            rvReportPaymentHistory.visibility = View.GONE
            tvNoReportPayHistory.visibility = View.VISIBLE
        }
    }

    private fun setHeader() {
        iv_menu.visibility = View.VISIBLE
        mActivity.checkNavigationItem(6)
        tvHeader.text = mActivity.resources.getString(R.string.frequency_history)
        Utils.setTextGradientColor(tvHeader)
        tvHeader.setPadding(0, 0, 50, 0)
        iv_menu.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.openDrawer()
        }
        mActivity.checkUserActive()
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.fabAddFrequencyPremium -> {
                mActivity.hideKeyboard()
                if (setData() > 0) {
                    Comman_Methods.avoidDoubleClicks(v)
                    mActivity.addFragment(
                        BusinessTrackFragment.newInstance(
                            false,
                            frequencyPremiumReportList,
                            FrequencyHistoryResult(),
                            -1,
                            true
                        ),
                        true,
                        true,
                        animationType = AnimationType.fadeInfadeOut
                    )
                } else {
                    mActivity.showMessage(mActivity.resources.getString(R.string.str_frequency_set))
                }
            }
        }
    }

    private fun setData(): Int {
        var memberSelected = 0
        for (i: Int in appDatabase.memberDao().getAllMember().indices) {
            val memberBean = MemberBean()
            memberBean.id = appDatabase.memberDao().getAllMember()[i].iD
            memberBean.isSelected = false
            memberBean.memberName =
                appDatabase.memberDao().getAllMember()[i].firstName + " " + appDatabase.memberDao().getAllMember()[i].lastName
            memberBean.memberEmail = appDatabase.memberDao().getAllMember()[i].email
            memberBean.memberImage = appDatabase.memberDao().getAllMember()[i].image
            memberList.add(memberBean)
        }
        if (frequencyPremiumReportList.size > 0) {
            memberSelected = setDataRemovedUser()
        }
        return memberSelected
    }

    private fun setDataRemovedUser(): Int {
        var compareMemberList: ArrayList<MemberBean> = ArrayList()
        val originalMemberList: ArrayList<MemberBean> = ArrayList()
        originalMemberList.addAll(memberList)

        for (i: Int in frequencyPremiumReportList.indices) {
            val frequencyPremiumReport = frequencyPremiumReportList[i]
            if (originalMemberList.size > 0) {
                for (k: Int in originalMemberList.indices) {
                    for (j: Int in frequencyPremiumReport.lstPremiumMember.indices) {
                        val frequencyPremiumUser = frequencyPremiumReport.lstPremiumMember[j]
                        if (originalMemberList[k].id == frequencyPremiumUser.memberID) {
                            val frequencyUser = MemberBean()
                            frequencyUser.id = frequencyPremiumUser.memberID
                            frequencyUser.memberName = frequencyPremiumUser.name
                            frequencyUser.memberEmail = frequencyPremiumUser.email
                            frequencyUser.isSelected = frequencyPremiumUser.isSelected
                            frequencyUser.isPaymentDone = frequencyPremiumReport.isPayment ?: false
                            compareMemberList.add(frequencyUser)
                        }
                    }
                }
            }
        }

        compareMemberList = compareMemberList.distinctBy { it.id } as ArrayList<MemberBean>
        for (k: Int in memberList.indices){
            val member = memberList[k]
            for (l: Int in compareMemberList.indices){
                val compareMember = compareMemberList[l]
                if (compareMember.id == member.id){
                    if (compareMember.isPaymentDone) {
                        originalMemberList.remove(member)
                    }
                }
            }
        }
        memberList = originalMemberList.distinctBy { it.id } as ArrayList<MemberBean>
        return memberList.size
    }

    inner class ReportPaymentAdapter(val context: Context, private val frequencyPremiumReportList: ArrayList<FrequencyHistoryResult>): RecyclerView.Adapter<ReportPaymentAdapter.ReportPaymentHolder>(){
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ReportPaymentHolder {
            return ReportPaymentHolder(LayoutInflater.from(context).inflate(R.layout.raw_report_payment_history, p0, false))
        }

        override fun getItemCount(): Int {
            return frequencyPremiumReportList.size
        }

        override fun onBindViewHolder(holder: ReportPaymentHolder, position: Int) {

            val frequencyPremiumReport = frequencyPremiumReportList[position]

            val formatter = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
            var diagStartDate = ""
            var diagEndDate = ""
            // with Am/Pm display
            val c = Calendar.getInstance()
            val formattedDate = formatter.format(c.time)
            val compareDate = formatter.parse(formattedDate)

            var date1: Date? = null
            var date2: Date? = null

            try {
                if (frequencyPremiumReport.paymentDate != null) {
                    date1 = formatter.parse(frequencyPremiumReport.paymentDate ?: "")
                }
                if (frequencyPremiumReport.paymentEndDate != null) {
                    date2 = formatter.parse(frequencyPremiumReport.paymentEndDate ?: "")
                }
                val target = SimpleDateFormat(CHECK_DATE_TIME4)
                if (date1 != null) {
                    diagStartDate = target.format(date1)
                }
                if (date2 != null) {
                    diagEndDate = target.format(date2)
                    /*if (compareDate < date2 || !frequencyPremiumReport.isPayment) {
                        holder.ivRepeatPayment.visibility = View.GONE
                    }else{
                        holder.ivRepeatPayment.visibility = View.VISIBLE
                    }*/
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            holder.tvPaymentAmount.text = mActivity.resources.getString(R.string.str_paid_amount)+": $ "+frequencyPremiumReport.amount.toString()
            holder.tvPaymentFrequency.text = mActivity.resources.getString(R.string.str_frequency) + ": " + frequencyPremiumReport.frequency.toString() + " " + mActivity.resources.getString(R.string.str_seconds)
            holder.tvPaymentType.text = if ((frequencyPremiumReport.paymentType ?: 0) > 0) {
                if (frequencyPremiumReport.paymentType == 2){
                    mActivity.resources.getString(R.string.str_yearly)
                }else{
                    mActivity.resources.getString(R.string.str_monthly)
                }
            } else { "" }
            for (l in 0 until frequencyPremiumReport.lstPremiumMember.size){
                if ( l == 0) {
                    holder.tvPaymentUserCount.text = mActivity.resources.getString(R.string.str_paid_user)+": " + frequencyPremiumReport.lstPremiumMember[l].name
                } else {
                    holder.tvPaymentUserCount.text = holder.tvPaymentUserCount.text.toString() +", " + frequencyPremiumReport.lstPremiumMember[l].name
                }
            }

            if (diagStartDate != ""){
                holder.tvPaymentStartDate.visibility = View.VISIBLE
            } else {
                holder.tvPaymentStartDate.visibility = View.GONE
            }

            if (diagEndDate != ""){
                holder.tvPaymentEndDate.visibility = View.VISIBLE
            }else{
                holder.tvPaymentEndDate.visibility = View.GONE
            }

            holder.tvPaymentStartDate.text = mActivity.resources.getString(R.string.start_date)+ ": " + diagStartDate
            holder.tvPaymentEndDate.text = mActivity.resources.getString(R.string.end_date)+ ": "  + diagEndDate

            holder.ivGenerateReport.setOnClickListener {
                mActivity.hideKeyboard()
                if (compareDate < date2 || frequencyPremiumReport.isPayment == false) {
                    Comman_Methods.avoidDoubleClicks(it)
                    mActivity.addFragment(
                        ReportFragment.newInstance(true, frequencyPremiumReport),
                        true,
                        true,
                        AnimationType.fadeInfadeOut
                    )
                }else{
                    mActivity.showMessage(mActivity.resources.getString(R.string.str_frequency_update))
                }
            }
            holder.ivRepeatPayment.setOnClickListener {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
                mActivity.addFragment(
                    BusinessTrackFragment.newInstance(true, frequencyPremiumReportList, frequencyPremiumReport, position, true),
                    true,
                    true,
                    animationType = AnimationType.fadeInfadeOut
                )
            }
        }

        inner class ReportPaymentHolder(view: View): RecyclerView.ViewHolder(view){
            var tvPaymentType: TextView = view.findViewById(R.id.tvPaymentType)
            var tvPaymentFrequency: TextView = view.findViewById(R.id.tvPaymentFrequency)
            var tvPaymentAmount: TextView = view.findViewById(R.id.tvPaymentAmount)
            var tvPaymentUserCount: TextView = view.findViewById(R.id.tvPaymentUserCount)
            var tvPaymentStartDate: TextView = view.findViewById(R.id.tvPaymentStartDate)
            var tvPaymentEndDate: TextView = view.findViewById(R.id.tvPaymentEndDate)
            var ivGenerateReport: ImageView = view.findViewById(R.id.ivGenerateReport)
            var ivViewUser: ImageView = view.findViewById(R.id.ivViewUser)
            var ivRepeatPayment: ImageView = view.findViewById(R.id.ivRepeatPayment)

        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            setHeader()
        }
    }

    private fun callReportHistoryApi(){
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            Comman_Methods.isProgressShow(mActivity)
            mActivity.isSpeedAvailable()
            val callFrequencyPremiumHistoryApi = WebApiClient.getInstance(mActivity).webApi_without?.
            getFrequencyPremiumReport(appDatabase.loginDao().getAll().adminID ?: 0)
            callFrequencyPremiumHistoryApi?.enqueue(object: retrofit2.Callback<FrequencyPremiumHistoryResponse>{
                override fun onFailure(call: Call<FrequencyPremiumHistoryResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(call: Call<FrequencyPremiumHistoryResponse>, response: Response<FrequencyPremiumHistoryResponse>) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            frequencyPremiumReportList = ArrayList()
                            response.body()?.let {
                                if (it.status == true) {
                                    val frequencyData = it.frequencyHistoryResult ?: ArrayList()
                                    if (frequencyData.size > 0) {
                                        frequencyPremiumReportList.addAll(frequencyData)
                                    }
                                }
                            }
                            setFrequencyAdapter()
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
}