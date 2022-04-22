package com.keepSafe911.fragments.homefragment.profile

import AnimationType
import addFragment
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.keepSafe911.BuildConfig
import com.keepSafe911.HomeActivity
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.model.BoundarySummaryResult
import com.keepSafe911.model.FamilyMonitorResult
import com.keepSafe911.model.MemberBean
import com.keepSafe911.model.response.BoundarySummaryResponse
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import com.keepSafe911.webservices.WebApiClient
import hideKeyboard
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltipUtils
import kotlinx.android.synthetic.main.fragment_boundary_log.*
import kotlinx.android.synthetic.main.toolbar_header.*
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class BoundaryLogFragment : HomeBaseFragment(), View.OnClickListener {

    lateinit var appDataBase: OldMe911Database
    var mMemberID: Int = -1
    var option: Int? = 1
    var MemberName: String = ""
    var memberList: ArrayList<MemberBean> = ArrayList()
    var boundarySummaryResult: ArrayList<BoundarySummaryResult> = ArrayList()
    var duplicateBoundarySummaryResult: ArrayList<BoundarySummaryResult> = ArrayList()

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
        return inflater.inflate(R.layout.fragment_boundary_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appDataBase = OldMe911Database.getDatabase(mActivity)
        mActivity.disableDrawer()
        setHeader()
        if (appDataBase.loginDao().getAll().isAdmin) {
            spiSummaryMember.visibility = View.VISIBLE
            ibFilter.visibility = View.VISIBLE
            callGetMember()
        } else {
            spiSummaryMember.visibility = View.GONE
            ibFilter.visibility = View.GONE
            mMemberID = appDataBase.loginDao().getAll().memberID
        }

        ibFilter.setOnClickListener(this)
    }

    override fun onClick(v: View?) {

        when (v?.id) {
            R.id.ibFilter -> {
                mActivity.hideKeyboard()
                val tooltip = SimpleTooltip.Builder(mActivity)
                    .anchorView(ibFilter)
                    .text(mActivity.resources.getString(R.string.summary))
                    .gravity(Gravity.BOTTOM)
                    .modal(true)
                    .animated(false)
                    .arrowColor(ContextCompat.getColor(mActivity, R.color.caldroid_white))
                    .animationDuration(2000)
                    .animationPadding(SimpleTooltipUtils.pxFromDp(50f))
                    .contentView(R.layout.summaryoption)
                    .focusable(true)
                    .dismissOnInsideTouch(false)
                    .transparentOverlay(true)
                    .build()

                val tvSummaryBoth: TextView = tooltip.findViewById(R.id.tvSummaryAll)
                val tvSummaryEnter: TextView = tooltip.findViewById(R.id.tvSummaryEnter)
                val tvSummaryExit: TextView = tooltip.findViewById(R.id.tvSummaryExit)
                tvSummaryBoth.text = mActivity.resources.getString(R.string.both_boundary)
                tvSummaryEnter.text = mActivity.resources.getString(R.string.enter_boundary)
                tvSummaryExit.text = mActivity.resources.getString(R.string.exit_boundary)
                when (option) {
                    1 -> {
                        tvSummaryBoth.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0)
                        tvSummaryEnter.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                        tvSummaryExit.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    }
                    2 -> {
                        tvSummaryBoth.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                        tvSummaryEnter.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0)
                        tvSummaryExit.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    }
                    3 -> {
                        tvSummaryBoth.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                        tvSummaryEnter.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                        tvSummaryExit.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0)
                    }
                }
                tooltip.show()
                tvSummaryBoth.setOnClickListener {
                    option = 1
                    filterList(mMemberID)
                    tooltip.dismiss()
                }
                tvSummaryEnter.setOnClickListener {
                    option = 2
                    filterList(mMemberID)
                    tooltip.dismiss()
                }
                tvSummaryExit.setOnClickListener {
                    option = 3
                    filterList(mMemberID)
                    tooltip.dismiss()
                }
            }
        }
    }

    private fun callGetMember() {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            mActivity.isSpeedAvailable()
            Utils.familyMonitoringUserList(mActivity, object : CommonApiListener {
                override fun familyUserList(
                    status: Boolean,
                    userList: ArrayList<FamilyMonitorResult>,
                    message: String
                ) {
                    if (status) {
                        if (userList.isEmpty()) {
                            mActivity.showMessage(mActivity.resources.getString(R.string.no_data))
                        }
                        appDataBase.memberDao().dropTable()
                        appDataBase.memberDao().addAllMember(userList)
                        setData()
                    } else {
                        mActivity.showMessage(message)
                    }
                }

                override fun onFailureResult() {
                    mActivity.showMessage(mActivity.resources.getString(R.string.error_message))
                }
            })
        } else {
            setData()
        }
    }

    private fun setData() {
        memberList = ArrayList()
        val defaultmemberBean = MemberBean()
        defaultmemberBean.id = -1
        defaultmemberBean.memberName = getString(R.string.all)
        memberList.add(defaultmemberBean)
        for (i: Int in appDataBase.memberDao().getAllMember().indices) {
            val memberBean = MemberBean()
            memberBean.id = appDataBase.memberDao().getAllMember()[i].iD
            memberBean.memberName =
                appDataBase.memberDao().getAllMember()[i].firstName + " " + appDataBase.memberDao().getAllMember()[i].lastName
            memberBean.memberEmail = appDataBase.memberDao().getAllMember()[i].email
            memberBean.memberImage = appDataBase.memberDao().getAllMember()[i].image
            memberList.add(memberBean)
        }
        if (memberList.size > 0) {
            val memberListName = ArrayList<String>()
            for (i in 0 until memberList.size) {
                memberListName.add(memberList[i].memberName ?: "")
            }
            val memberName =
                ArrayAdapter(mActivity, android.R.layout.simple_list_item_1, memberListName)
            spiSummaryMember.adapter = memberName
        }
        callBoundarySummaryApi(appDataBase.loginDao().getAll().memberID)
        spiSummaryMember.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                mMemberID = memberList[position].id ?: -1
                MemberName = memberList[position].memberName ?: ""
                filterList(mMemberID)
            }
        }
    }


    private fun setHeader() {
        val notificationManager = mActivity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        iv_back.visibility = View.VISIBLE
        mActivity.disableDrawer()
        tvHeader.text = mActivity.resources.getString(R.string.summary)
        Utils.setTextGradientColor(tvHeader)
        tvHeader.setPadding(0, 0, 50, 0)
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

    private fun callBoundarySummaryApi(memberId: Int) {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            Comman_Methods.isProgressShow(mActivity)
            mActivity.isSpeedAvailable()
            val callBoundarySummary = WebApiClient.getInstance(mActivity).webApi_without?.
                getBoundarySummary(memberId)
            callBoundarySummary?.enqueue(object : retrofit2.Callback<BoundarySummaryResponse> {
                override fun onFailure(call: Call<BoundarySummaryResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(
                    call: Call<BoundarySummaryResponse>,
                    response: Response<BoundarySummaryResponse>
                ) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                if (it.isStatus) {
                                    val boundaryList = it.result ?: ArrayList()
                                    if (boundaryList.size > 0) {
                                        boundarySummaryResult = ArrayList()
                                        duplicateBoundarySummaryResult = ArrayList()
                                        boundarySummaryResult.addAll(boundaryList)
                                        duplicateBoundarySummaryResult.addAll(boundaryList)
                                        filterList(mMemberID)
                                    } else {
                                        tvSummaryNoData.visibility = View.VISIBLE
                                        rvSummary.visibility = View.GONE
                                    }
                                } else {
                                    tvSummaryNoData.visibility = View.VISIBLE
                                    rvSummary.visibility = View.GONE
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
            tvSummaryNoData.visibility = View.VISIBLE
            rvSummary.visibility = View.GONE
        }
    }

    fun filterList(memberId: Int) {
        duplicateBoundarySummaryResult = ArrayList()
        /**
         * Filter for ALL Members
         */
        if (memberId == -1 && option == 1) {
            duplicateBoundarySummaryResult.addAll(boundarySummaryResult)
        } else if (memberId == -1 && option == 2) {
            for (i in 0 until boundarySummaryResult.size) {
                if (boundarySummaryResult[i].status) {
                    duplicateBoundarySummaryResult.add(boundarySummaryResult[i])
                }
            }
        } else if (memberId == -1 && option == 3) {
            for (i in 0 until boundarySummaryResult.size) {
                if (!boundarySummaryResult[i].status) {
                    duplicateBoundarySummaryResult.add(boundarySummaryResult[i])
                }
            }

        }
        /**
         * Filter for SELECTED MEMBER
         */
        else if (memberId > 0 && option == 1) {
            for (i in 0 until boundarySummaryResult.size) {
                if (boundarySummaryResult[i].memberID == memberId) {
                    duplicateBoundarySummaryResult.add(boundarySummaryResult[i])
                }
            }
        } else if (memberId > 0 && option == 2) {
            for (i in 0 until boundarySummaryResult.size) {
                if (boundarySummaryResult[i].memberID == memberId) {
                    if (boundarySummaryResult[i].status) {
                        duplicateBoundarySummaryResult.add(boundarySummaryResult[i])
                    }
                }
            }
        } else if (memberId > 0 && option == 3) {
            for (i in 0 until boundarySummaryResult.size) {
                if (boundarySummaryResult[i].memberID == memberId) {
                    if (!boundarySummaryResult[i].status) {
                        duplicateBoundarySummaryResult.add(boundarySummaryResult[i])
                    }
                }
            }
        }
        duplicateBoundarySummaryResult.reverse()
        setAdapter(duplicateBoundarySummaryResult)
    }

    private fun setAdapter(boundarySummaryResult: ArrayList<BoundarySummaryResult>) {
        if (rvSummary!=null) {
            if (boundarySummaryResult.size > 0) {
                tvSummaryNoData.visibility = View.GONE
                rvSummary.visibility = View.VISIBLE
                rvSummary.layoutManager = LinearLayoutManager(mActivity, RecyclerView.VERTICAL, false)
                val summaryAdapter = SummaryAdapter(mActivity, boundarySummaryResult)
                rvSummary.adapter = summaryAdapter
                summaryAdapter.notifyDataSetChanged()
            } else {
                tvSummaryNoData.visibility = View.VISIBLE
                rvSummary.visibility = View.GONE
            }
        }
    }

    class SummaryAdapter(private val context: HomeActivity, private val summaryList: ArrayList<BoundarySummaryResult>) :
        RecyclerView.Adapter<SummaryAdapter.SummaryHolder>() {

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): SummaryHolder {
            return SummaryHolder(LayoutInflater.from(context).inflate(R.layout.raw_summary, p0, false))
        }

        override fun getItemCount(): Int {
            return summaryList.size
        }

        override fun onBindViewHolder(holder: SummaryHolder, position: Int) {
            holder.tvSummaryName.text = if (summaryList[position].geoFenceName!=null) summaryList[position].geoFenceName else ""
            holder.tvSummaryMemberName.text = if (summaryList[position].memberName!=null) summaryList[position].memberName else ""
            holder.tvSummaryStatus.text =
                if (summaryList[position].status) context.getString(R.string.enter) else context.getString(R.string.exit)
            val formatter = SimpleDateFormat(DELIVER_DATE_FORMAT)
            val formatter2 = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
            val target = SimpleDateFormat(SHOW_DATE_TIME)
            var diagStartDate = ""
            var anotherDate = ""
            try {
                var date1: Date? = null
                if (summaryList[position].createdOn != null) {
                    date1 = formatter.parse(summaryList[position].createdOn ?: "")
                }

                if (date1 != null) {
                    diagStartDate = target.format(date1)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                var date1: Date? = null
                if (summaryList[position].createdOn != null) {
                    date1 = formatter2.parse(summaryList[position].createdOn ?: "")
                }
                if (date1 != null) {
                    anotherDate = target.format(date1)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            holder.tvSummaryTime.text = if (diagStartDate == "") anotherDate else diagStartDate
            holder.ivViewMap.setOnClickListener {
                if (summaryList[position].latitude != 0.0 && summaryList[position].longitude != 0.0) {
                    Comman_Methods.avoidDoubleClicks(it)
                    context.addFragment(
                        SummaryMapFragment.newInstance(
                            summaryList[position].latitude,
                            summaryList[position].longitude
                        ), true, true, AnimationType.bottomtotop
                    )
                } else {
                    context.showMessage(context.resources.getString(R.string.summaryText))
                }
            }
        }

        class SummaryHolder(view: View) : RecyclerView.ViewHolder(view) {
            var tvSummaryName: TextView = view.findViewById(R.id.tvSummaryName)
            var tvSummaryMemberName: TextView = view.findViewById(R.id.tvSummaryMemberName)
            var tvSummaryTime: TextView = view.findViewById(R.id.tvSummaryTime)
            var tvSummaryStatus: TextView = view.findViewById(R.id.tvSummaryStatus)
            var ivViewMap: ImageView = view.findViewById(R.id.ivViewMap)
        }
    }
}