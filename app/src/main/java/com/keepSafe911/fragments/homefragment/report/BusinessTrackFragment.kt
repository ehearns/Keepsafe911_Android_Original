package com.keepSafe911.fragments.homefragment.report

import AnimationType
import addFragment
import android.app.Dialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.StrictMode
import com.google.android.material.textfield.TextInputLayout
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.keepSafe911.BuildConfig
import com.keepSafe911.HomeActivity
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.fragments.payment_selection.ReportSubscriptionFragment
import com.keepSafe911.gps.GpsTracker
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.listner.PositiveButtonListener
import com.keepSafe911.model.FamilyMonitorResult
import com.keepSafe911.model.MemberBean
import com.keepSafe911.model.response.FrequencyHistoryResult
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_business_track.*
import kotlinx.android.synthetic.main.raw_geo_member.view.*
import kotlinx.android.synthetic.main.row_select_member.view.*
import kotlinx.android.synthetic.main.toolbar_header.*
import java.util.*
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val ARG_PARAM3 = "param3"
private const val ARG_PARAM4 = "param4"
private const val ARG_PARAM5 = "param5"

class BusinessTrackFragment : HomeBaseFragment(), View.OnClickListener {

    var memberList: ArrayList<MemberBean> = ArrayList()
    var MemberID: Int? = 0
    var MemberName: String? = ""
    private var isUpdate: Boolean = false
    lateinit var appDataBase: OldMe911Database
    private var gpstracker: GpsTracker? = null
    private var paymentMemberList: ArrayList<MemberBean> = ArrayList()
    private lateinit var geoMemberAdapter: PaymentMemberAdapter
    private var frequencyRange: Int = 0
    private lateinit var dialog: Dialog
    private var frequencyPremiumReportList: ArrayList<FrequencyHistoryResult> = ArrayList()
    private var frequencyPremiumReport: FrequencyHistoryResult = FrequencyHistoryResult()
    private var frequencyPosition: Int = -1
    var isFromHistory: Boolean = false

    companion object {
        fun newInstance(
            isUpdate: Boolean,
            frequencyPremiumReportList: ArrayList<FrequencyHistoryResult>,
            frequencyPremiumReport: FrequencyHistoryResult,
            frequencyPosition: Int,
            isFromHistory: Boolean
        ): BusinessTrackFragment {
            val args = Bundle()
            args.putBoolean(ARG_PARAM1, isUpdate)
            args.putParcelableArrayList(ARG_PARAM2, frequencyPremiumReportList)
            args.putParcelable(ARG_PARAM3,frequencyPremiumReport)
            args.putInt(ARG_PARAM4,frequencyPosition)
            args.putBoolean(ARG_PARAM5,isFromHistory)
            val fragment = BusinessTrackFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnPayFrequency -> {
                mActivity.hideKeyboard()
                if (checkValidation()) {
                    Comman_Methods.avoidDoubleClicks(v)
                    mActivity.addFragment(
                        ReportSubscriptionFragment.newInstance(frequencyRange, paymentMemberList, frequencyPremiumReportList, frequencyPosition),
                        true,
                        true,
                        AnimationType.fadeInfadeOut
                    )
                }
            }
            R.id.ivAddMultiplePaymentMember -> {
                mActivity.hideKeyboard()
                if (memberList.size > 0) {
                    Comman_Methods.avoidDoubleClicks(v)
                    for (i in 0 until memberList.size){
                        memberList[i].isSelected = false
                    }
                    for (i in 0 until memberList.size) {
                        if (paymentMemberList.size > 0) {
                            for (n: Int in paymentMemberList.indices) {
                                val paymentMember = paymentMemberList[n]
                                if (memberList[i].id == paymentMember.id) {
                                    memberList[i].isSelected = true
                                }
                            }
                        }
                    }
                    memberList = memberList.distinctBy { it.id } as ArrayList<MemberBean>
                    showMemberListDialog(memberList)
                }else{
                    mActivity.showMessage(mActivity.resources.getString(R.string.str_frequency_set))
                }
            }
            R.id.etFrequencyPayment -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                if (frequencyRange > 14400){
                    frequencyRange = 14400
                }
                openFrequencyPopUp(frequencyRange)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isUpdate = it.getBoolean(ARG_PARAM1,false)
            frequencyPremiumReportList = it.getParcelableArrayList(ARG_PARAM2) ?: ArrayList()
            frequencyPremiumReport = it.getParcelable(ARG_PARAM3) ?: FrequencyHistoryResult()
            frequencyPosition = it.getInt(ARG_PARAM4,-1)
            isFromHistory = it.getBoolean(ARG_PARAM5, false)
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
        return inflater.inflate(R.layout.fragment_business_track, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appDataBase = OldMe911Database.getDatabase(mActivity)
        gpstracker = GpsTracker(mActivity)
        setHeader()
        if (isUpdate){
            frequencyRange = frequencyPremiumReport.frequency ?: 0
            paymentMemberList = ArrayList()
            for (k in 0 until frequencyPremiumReport.lstPremiumMember.size){
                val frequencyUser = MemberBean()
                frequencyUser.id = frequencyPremiumReport.lstPremiumMember[k].memberID
                frequencyUser.memberName = frequencyPremiumReport.lstPremiumMember[k].name
                frequencyUser.memberEmail = frequencyPremiumReport.lstPremiumMember[k].email
                frequencyUser.isSelected = frequencyPremiumReport.lstPremiumMember[k].isSelected
                frequencyUser.isPaymentDone = frequencyPremiumReport.lstPremiumMember[k].isPaymentDone
                paymentMemberList.add(frequencyUser)
            }
            if (frequencyRange > 0) {
                if (frequencyRange >= 60){
                    if (frequencyRange <= 14400){
                        etFrequencyPayment.setText(
                            "$frequencyRange " + mActivity.resources.getString(
                                R.string.str_seconds
                            )
                        )
                    }else{
                        frequencyRange = 14400
                        etFrequencyPayment.setText(
                            "$frequencyRange " + mActivity.resources.getString(
                                R.string.str_seconds
                            )
                        )
                    }
                }else{
                    frequencyRange = 60
                    etFrequencyPayment.setText(mActivity.resources.getString(R.string.str_60_seconds))
                }
            }else{
                etFrequencyPayment.setText("")
            }
        }else{
            frequencyRange = 0
            paymentMemberList = ArrayList()
            etFrequencyPayment.setText("")
        }
        if (appDataBase.loginDao().getAll().isAdmin) {
            callGetMember(appDataBase.loginDao().getAll().memberID)
        } else {
            MemberID = appDataBase.loginDao().getAll().memberID
            MemberName = appDataBase.loginDao().getAll().firstName + " " + appDataBase.loginDao().getAll().lastName
        }
        etFrequencyPayment.setOnClickListener(this)
        btnPayFrequency.setOnClickListener(this)
        ivAddMultiplePaymentMember.setOnClickListener(this)
    }

    private fun setHeader() {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        mActivity.enableDrawer()
        mActivity.checkNavigationItem(6)
        tvHeader.text = mActivity.resources.getString(R.string.reports)
        Utils.setTextGradientColor(tvHeader)
        changeNoteForPayment()
        tvHeader.setPadding(0, 0, 50, 0)
        if (isFromHistory){
            iv_back.visibility = View.VISIBLE
            iv_menu.visibility = View.GONE
            mActivity.disableDrawer()
        }else{
            iv_back.visibility = View.GONE
            iv_menu.visibility = View.VISIBLE
            mActivity.enableDrawer()
        }
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
        iv_menu.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.openDrawer()
        }
        mActivity.checkUserActive()
    }

    internal fun showMemberListDialog(duplicatememberList: ArrayList<MemberBean>) {

        val inflater = mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        val dialoglayout = inflater!!.inflate(R.layout.popup_invitemember_list, null)

        val rvGeoMemberList = dialoglayout.findViewById<RecyclerView>(R.id.rvGeoMemberList)
        val popup_tv_done = dialoglayout.findViewById<TextView>(R.id.tv_invite_done)
        val tv_cancel = dialoglayout.findViewById<TextView>(R.id.tv_inviteCancle)
        val tvInviteMember = dialoglayout.findViewById<TextView>(R.id.tvInviteMember)
        val etGeoMemberSearch = dialoglayout.findViewById<EditText>(R.id.etGeoMemberSearch)
        tvInviteMember.text = mActivity.resources.getString(R.string.select_user)
        val adapter =
            SelectPaymentMemberAdapter(
                mActivity,
                duplicatememberList
            )
        rvGeoMemberList.layoutManager = LinearLayoutManager(mActivity, RecyclerView.VERTICAL, false)
        rvGeoMemberList.adapter = adapter
        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            etGeoMemberSearch.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        } else {
            etGeoMemberSearch.imeOptions = EditorInfo.IME_ACTION_DONE
        }
        etGeoMemberSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter.filter(s)
            }

        })

        val mDialog = AlertDialog.Builder(mActivity)
        mDialog.setView(dialoglayout)


        val dialog = mDialog.create()
        dialog.setCancelable(true)
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        tv_cancel.setOnClickListener { dialog.dismiss() }
        popup_tv_done.setOnClickListener {
            mActivity.hideKeyboard()
            if (!isMemberSelected(duplicatememberList)) {
                mActivity.showMessage(mActivity.resources.getString(R.string.selectWorker))
            } else {
                Comman_Methods.avoidDoubleClicks(it)
                paymentMemberList = ArrayList()
                for (k in 0 until duplicatememberList.size) {
                    if (duplicatememberList[k].isSelected) {
                        val lstGeoFenceMember = MemberBean()
                        lstGeoFenceMember.id = duplicatememberList[k].id
                        lstGeoFenceMember.memberName = duplicatememberList[k].memberName
                        paymentMemberList.add(lstGeoFenceMember)
                    }
                    /*if (paymentMemberList.size > k) {
                        for (i in k until k + 1) {
                            if (paymentMemberList[i].id != duplicatememberList[k].id) {
                                val lstGeoFenceMember = MemberBean()
                                lstGeoFenceMember.id = duplicatememberList[k].id
                                lstGeoFenceMember.memberName = duplicatememberList[k].memberName
                                paymentMemberList.add(lstGeoFenceMember)
                            }
                        }
                    } else {
                        val lstGeoFenceMember = MemberBean()
                        lstGeoFenceMember.id = duplicatememberList[k].id
                        lstGeoFenceMember.memberName = duplicatememberList[k].memberName
                        paymentMemberList.add(lstGeoFenceMember)
                    }*/
                }
                paymentMemberList = paymentMemberList.distinctBy { data -> data.id } as ArrayList<MemberBean>
                setAdapter(paymentMemberList)
                dialog.dismiss()
            }
        }
    }

    private fun setAdapter(geoMemberListing: ArrayList<MemberBean>) {
        changeNoteForPayment()
        geoMemberAdapter = PaymentMemberAdapter(mActivity, geoMemberListing)
        rvPaymentMember.layoutManager = LinearLayoutManager(mActivity, RecyclerView.VERTICAL, false)
        rvPaymentMember.adapter = geoMemberAdapter
        geoMemberAdapter.notifyDataSetChanged()
    }
    private fun isMemberSelected(memberList: ArrayList<MemberBean>): Boolean {
        for (j in memberList.indices) {
            var memberbean = MemberBean()
            memberbean = memberList.get(j)
            if (memberbean.isSelected) {
                return true
            }
        }
        return false
    }

    private fun callGetMember(memberID: Int) {
        memberList = ArrayList()
        /*val defaultmemberBean = MemberBean()
        defaultmemberBean.id = -1
        defaultmemberBean.memberName = mActivity.resources.getString(R.string.select_user)
        memberList.add(defaultmemberBean)*/
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
        for (i: Int in appDataBase.memberDao().getAllMember().indices) {
            val memberBean = MemberBean()
            memberBean.id = appDataBase.memberDao().getAllMember()[i].iD
            memberBean.isSelected = false
            memberBean.memberName =
                appDataBase.memberDao().getAllMember()[i].firstName + " " + appDataBase.memberDao().getAllMember()[i].lastName
            memberBean.memberEmail = appDataBase.memberDao().getAllMember()[i].email
            memberList.add(memberBean)
            if (frequencyPremiumReportList.size == 0){
                if (appDataBase.loginDao().getAll().memberID == appDataBase.memberDao().getAllMember()[i].iD) {
                    paymentMemberList = ArrayList()
                    memberBean.isSelected = true
                    paymentMemberList.add(memberBean)
                }
            }
        }
        if (frequencyPremiumReportList.size > 0) {
            setDataRemovedUser()
        }else{
            setAdapter(paymentMemberList)
        }
    }

    private fun setDataRemovedUser() {
        var compareMemberList: ArrayList<MemberBean> = ArrayList()
        val originalMemberList: ArrayList<MemberBean> = ArrayList()
        paymentMemberList = ArrayList()
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

        if (isUpdate){
            if (frequencyPremiumReport.lstPremiumMember.size > 0){
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
                            if (appDataBase.loginDao().getAll().memberID == frequencyPremiumUser.memberID) {
                                frequencyUser.isSelected = true
                            }
                            paymentMemberList.add(frequencyUser)
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
        if (memberList.size > 0) {
            for (m: Int in memberList.indices) {
                val member = memberList[m]
                if (appDataBase.loginDao().getAll().memberID == member.id) {
                    member.isSelected = true
                    paymentMemberList.add(member)
                }
            }
        }
        paymentMemberList = paymentMemberList.distinctBy { it.id } as ArrayList<MemberBean>
        setAdapter(paymentMemberList)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            setHeader()
        }
    }

    private fun checkValidation(): Boolean {
        return when {
            etFrequencyPayment.text.toString().trim().isEmpty() -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_frequency_type))
                false
            }
            paymentMemberList.size == 0 -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.select_member_frequency))
                false
            }
            else -> true
        }
    }

    inner class SelectPaymentMemberAdapter() : RecyclerView.Adapter<SelectPaymentMemberAdapter.SelectPaymentMemberHolder>(), Filterable {

        private lateinit var activity: HomeActivity
        private var memberList: ArrayList<MemberBean> = ArrayList()
        private var tempMemberList: ArrayList<MemberBean> = ArrayList()

        constructor(activity: HomeActivity, memberList: ArrayList<MemberBean>) : this() {
            this.activity = activity
            this.memberList = memberList
            this.tempMemberList = ArrayList()
            this.tempMemberList.addAll(memberList)
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(charSequence: CharSequence): Filter.FilterResults {
                    val charString = charSequence.toString()
                    if (charString.isEmpty()) {
                        tempMemberList = memberList
                    } else {
                        val filterList = ArrayList<MemberBean>()
                        for (row in memberList) {
                            val username = row.memberName ?: ""
                            if (username.trim().lowercase().contains(charString.lowercase())) {
                                filterList.add(row)
                            }
                        }
                        tempMemberList = filterList
                    }
                    val filterResults = Filter.FilterResults()
                    filterResults.values = tempMemberList
                    return filterResults
                }

                override fun publishResults(charSequence: CharSequence, filterResults: Filter.FilterResults) {
                    if (filterResults.values != null) {
                        tempMemberList = filterResults.values as ArrayList<MemberBean>
                    }
                    notifyDataSetChanged()
                }
            }
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): SelectPaymentMemberHolder {
            return SelectPaymentMemberHolder(
                LayoutInflater.from(activity).inflate(R.layout.row_select_member, p0, false)
            )
        }

        override fun getItemCount(): Int {
            return tempMemberList.size
        }

        override fun onBindViewHolder(p0: SelectPaymentMemberHolder, p1: Int) {
            p0.checkWorker.text = tempMemberList[p1].memberName
            p0.checkWorker.isChecked = tempMemberList[p1].isSelected
            if (tempMemberList[p1].id == appDataBase.loginDao().getAll().memberID){
                p0.checkWorker.isChecked = true
                p0.checkWorker.isEnabled = false
                tempMemberList[p1].isSelected = true
            }
            p0.checkWorker.setTextColor(ContextCompat.getColor(activity, R.color.caldroid_black))
            p0.checkWorker.setOnCheckedChangeListener(null)
            p0.checkWorker.setOnClickListener { v ->
                val isSelected = (v as CheckBox).isChecked
                tempMemberList[p1].isSelected = isSelected
            }
//            p0.checkWorker.isChecked = tempMemberList[p1].isSelected
        }

        inner class SelectPaymentMemberHolder(view: View) : RecyclerView.ViewHolder(view) {
            val checkWorker: CheckBox = view.checkWorker
        }
    }

    inner class PaymentMemberAdapter(
        private val activity: HomeActivity,
        private val frequencyMemberList: ArrayList<MemberBean>
    ) :
        RecyclerView.Adapter<PaymentMemberAdapter.PaymentMemberHolder>() {
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): PaymentMemberHolder {
            return PaymentMemberHolder(LayoutInflater.from(activity).inflate(R.layout.raw_geo_member, p0, false))
        }

        override fun getItemCount(): Int {
            return frequencyMemberList.size
        }

        override fun onBindViewHolder(p0: PaymentMemberHolder, p1: Int) {
            p0.tvGeoMemberName.text = frequencyMemberList[p1].memberName
            p0.ivRemoveGeoMember.setOnClickListener {
                mActivity.hideKeyboard()
                if (frequencyMemberList[p1].id != appDataBase.loginDao().getAll().memberID) {
                    Comman_Methods.avoidDoubleClicks(it)
                    showDeleteMemberDialog(frequencyMemberList[p1])
                }
            }
        }

        private fun showDeleteMemberDialog(uploadedFilesName: MemberBean) {
            Comman_Methods.isCustomPopUpShow(activity,
                message = activity.resources.getString(
                    R.string.DeletePaymentMember, uploadedFilesName.memberName),
                positiveButtonListener = object : PositiveButtonListener {
                    override fun okClickListener() {
                        frequencyMemberList.remove(uploadedFilesName)
                        memberList.add(uploadedFilesName)
                        notifyDataSetChanged()
                        changeNoteForPayment()
                    }
                })
        }

        inner class PaymentMemberHolder(view: View) : RecyclerView.ViewHolder(view) {
            var tvGeoMemberName: TextView = view.tvGeoMemberName
            var ivRemoveGeoMember: ImageView = view.ivRemoveGeoMember
        }
    }

    fun openFrequencyPopUp(frequencyRangeOption: Int){
        val inflateView = mActivity.layoutInflater
        val dialogLayout1 = inflateView.inflate(R.layout.custom_frequency_dialog, null)
        val iv_popup_dismiss: ImageView = dialogLayout1.findViewById(R.id.iv_popup_dismiss)
        val tv1Minute: TextView = dialogLayout1.findViewById(R.id.tv1Minute)
        val tv3Minute: TextView = dialogLayout1.findViewById(R.id.tv3Minute)
        val tv5Minute: TextView = dialogLayout1.findViewById(R.id.tv5Minute)
        val tvOtherMinute: TextView = dialogLayout1.findViewById(R.id.tvOtherMinute)
        val tl_ping_admin: TextInputLayout = dialogLayout1.findViewById(R.id.tl_ping_admin)
        val etPaymentFrequency: EditText = dialogLayout1.findViewById(R.id.etPaymentFrequency)
        val btnFrequencySubmit: Button = dialogLayout1.findViewById(R.id.btnFrequencySubmit)
        val mDialog = android.app.AlertDialog.Builder(activity)
        mDialog.setView(dialogLayout1)

        if (this::dialog.isInitialized){
            if (dialog.isShowing){
                dialog.dismiss()
            }
        }
        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            etPaymentFrequency.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        } else {
            etPaymentFrequency.imeOptions = EditorInfo.IME_ACTION_DONE
        }
        dialog = mDialog.create()
        dialog.window?.attributes?.windowAnimations = R.style.animationForDialog
        if (frequencyRange >=60){
            when (frequencyRange){
                60 -> {
                    tv1Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check, 0)
                    tv3Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    tv5Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    tvOtherMinute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    tl_ping_admin.visibility = View.GONE
                    btnFrequencySubmit.visibility = View.GONE
                }
                180 -> {
                    tv1Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    tv3Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check, 0)
                    tv5Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    tvOtherMinute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    tl_ping_admin.visibility = View.GONE
                    btnFrequencySubmit.visibility = View.GONE
                }
                300 -> {
                    tv1Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    tv3Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    tv5Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check, 0)
                    tvOtherMinute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    tl_ping_admin.visibility = View.GONE
                    btnFrequencySubmit.visibility = View.GONE
                }
                else -> {
                    if (frequencyRange > 14400){
                        frequencyRange = 14400
                    }
                    tv1Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    tv3Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    tv5Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    tvOtherMinute.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check, 0)
                    tl_ping_admin.visibility = View.VISIBLE
                    btnFrequencySubmit.visibility = View.VISIBLE
                    etPaymentFrequency.setText(frequencyRange.toString())
                }
            }
        }else{
            frequencyRange = 0
            tv1Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            tv3Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            tv5Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            tvOtherMinute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            tl_ping_admin.visibility = View.GONE
            btnFrequencySubmit.visibility = View.GONE
        }
        iv_popup_dismiss.setOnClickListener {
            frequencyRange = frequencyRangeOption
            if (frequencyRangeOption > 0) {
                etFrequencyPayment.setText(
                    "$frequencyRange " + mActivity.resources.getString(
                        R.string.str_seconds
                    )
                )
            }else{
                etFrequencyPayment.setText("")
            }
            dialog.dismiss()
        }
        tv1Minute.setOnClickListener {
            tv1Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check, 0)
            tv3Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            tv5Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            tvOtherMinute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            tl_ping_admin.visibility = View.GONE
            btnFrequencySubmit.visibility = View.GONE
            frequencyRange = 60
            etFrequencyPayment.setText(mActivity.resources.getString(R.string.str_60_seconds))
            dialog.dismiss()
        }
        tv3Minute.setOnClickListener {
            tv1Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            tv3Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check, 0)
            tv5Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            tvOtherMinute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            tl_ping_admin.visibility = View.GONE
            btnFrequencySubmit.visibility = View.GONE
            frequencyRange = 180
            etFrequencyPayment.setText(mActivity.resources.getString(R.string.str_180_seconds))
            dialog.dismiss()
        }
        tv5Minute.setOnClickListener {
            tv1Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            tv3Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            tv5Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check, 0)
            tvOtherMinute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            tl_ping_admin.visibility = View.GONE
            btnFrequencySubmit.visibility = View.GONE
            frequencyRange = 300
            etFrequencyPayment.setText(mActivity.resources.getString(R.string.str_300_seconds))
            dialog.dismiss()
        }
        tvOtherMinute.setOnClickListener {
            tv1Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            tv3Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            tv5Minute.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            tvOtherMinute.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check, 0)
            frequencyRange = frequencyRangeOption
            if (frequencyRange > 0){
                if (frequencyRange > 14400){
                    frequencyRange = 14400
                }
                etPaymentFrequency.setText(frequencyRange.toString())
            }else {
                etPaymentFrequency.setText("")
            }
            tl_ping_admin.visibility = View.VISIBLE
            btnFrequencySubmit.visibility = View.VISIBLE
        }
        btnFrequencySubmit.setOnClickListener {
            mActivity.hideKeyboard()
            when {
                etPaymentFrequency.text.toString().trim().isEmpty() -> {
                    mActivity.showMessage(mActivity.resources.getString(R.string.freq_blank))
                }
                etPaymentFrequency.text.toString().toInt() < 60 -> {
                    mActivity.showMessage(mActivity.resources.getString(R.string.recommended_freq))
                }
                etPaymentFrequency.text.toString().toInt() > 14400 -> {
                    mActivity.showMessage(mActivity.resources.getString(R.string.recommended_freq))
                }
                else -> {
                    frequencyRange = etPaymentFrequency.text.toString().toInt()
                    dialog.dismiss()
                }
            }
            etFrequencyPayment.setText(frequencyRange.toString()+" "+mActivity.resources.getString(R.string.str_seconds))
        }
        dialog.setCancelable(false)
        dialog.show()
    }

    fun changeNoteForPayment(){
        if (paymentMemberList.size > 0) {
            tvPaymentFrequencyUser.visibility = View.GONE
            tvPaymentFrequencyUser.text = mActivity.resources.getString(
                R.string.str_note_frequency_payment,
                paymentMemberList.size
            )
        } else {
            tvPaymentFrequencyUser.visibility = View.GONE
        }
    }
}