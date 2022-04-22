package com.keepSafe911.fragments.homefragment.report

import AnimationType
import addFragment
import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.keepSafe911.BuildConfig
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.gps.GpsTracker
import com.keepSafe911.model.Loc
import com.keepSafe911.model.MemberBean
import com.keepSafe911.model.response.ApiResponse
import com.keepSafe911.model.response.FrequencyHistoryResult
import com.keepSafe911.model.response.MemberRouteResponse
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.Comman_Methods.Companion.isProgressHide
import com.keepSafe911.utils.Comman_Methods.Companion.isProgressShow
import com.keepSafe911.webservices.WebApi
import com.keepSafe911.webservices.WebApiClient
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kotlinpermissions.KotlinPermissions
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.model.FamilyMonitorResult
import com.keepSafe911.model.ResultRoute
import com.keepSafe911.utils.*
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_report.*
import kotlinx.android.synthetic.main.toolbar_header.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ReportFragment : HomeBaseFragment(), View.OnClickListener {

    var memberList: ArrayList<MemberBean> = ArrayList()
    var MemberID: Int = 0
    var MemberName: String = ""
    var MemberEmail: String = ""
    var type: String = ""
    private var dateFormatter: SimpleDateFormat? = null
    private lateinit var datePickerDialog: DatePickerDialog
    private var dateparse: Date? = null
    lateinit var appDataBase: OldMe911Database
    private var sendEmail: Boolean = false
    private var viewReport: Boolean = false
    private var isFrom: Boolean = false
    private var circles = ArrayList<Loc>()
    private var memRouteName: String = ""
    private var gpstracker: GpsTracker? = null
    private var frequencyPremiumReport: FrequencyHistoryResult = FrequencyHistoryResult()

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnGenerateReport -> {
                mActivity.hideKeyboard()
                if (checkValidation()) {
                    Comman_Methods.avoidDoubleClicks(v)
                    if (spinReportType.selectedItemPosition > 2){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            checkLocationPermission()
                        } else {
                            if (gpstracker?.CheckForLoCation() == false) {
                                Utils.showLocationSettingsAlert(mActivity)
                            } else {
                                callMemberRouteReport(MemberID,
                                    etCMSDate.text.toString())
                            }
                        }
                    }else {
                        if (MemberID > 0 && MemberName != "") {
                            val view = layoutInflater.inflate(
                                R.layout.report_option_sheet,
                                mActivity.window.decorView.rootView as ViewGroup,
                                false
                            )
                            val dialog = BottomSheetDialog(mActivity)
                            dialog.setContentView(view)
                            val mBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(view.parent as View)
                            mBehavior.isHideable = false
                            dialog.setOnShowListener {
                                mBehavior.peekHeight = view.height
                            }
                            val tvSendReport: TextView? = dialog.findViewById<TextView>(R.id.tvSendReport)
                            val tvViewReport: TextView? = dialog.findViewById<TextView>(R.id.tvViewReport)
                            val tvDownloadReport: TextView? = dialog.findViewById<TextView>(R.id.tvDownloadReport)
                            val tvCancelReport: TextView? = dialog.findViewById<TextView>(R.id.tvCancelReport)
                            val tvPlaceTitle: TextView? = dialog.findViewById<TextView>(R.id.tvPlaceTitle)

                            tvSendReport?.visibility = View.VISIBLE
                            tvViewReport?.visibility = View.VISIBLE
                            tvDownloadReport?.visibility = View.VISIBLE
                            tvCancelReport?.visibility = View.VISIBLE

                            tvSendReport?.text = mActivity.resources.getString(R.string.send_email_report)
                            tvViewReport?.text = mActivity.resources.getString(R.string.view_report_report)
                            tvDownloadReport?.text = mActivity.resources.getString(R.string.download_report_report)
                            tvCancelReport?.text = mActivity.resources.getString(R.string.cancel)

                            tvSendReport?.setOnClickListener {
                                Comman_Methods.avoidDoubleClicks(it)
                                sendEmail = true
                                viewReport = false
                                callApi()
                                dialog.dismiss()
                            }
                            tvViewReport?.setOnClickListener {
                                Comman_Methods.avoidDoubleClicks(it)
                                sendEmail = false
                                viewReport = true
                                callApi()
                                dialog.dismiss()
                            }
                            tvDownloadReport?.setOnClickListener {
                                Comman_Methods.avoidDoubleClicks(it)
                                sendEmail = false
                                viewReport = false
                                callApi()
                                dialog.dismiss()
                            }
                            tvCancelReport?.setOnClickListener {
                                dialog.dismiss()
                            }
                            dialog.setCancelable(false)
                            dialog.show()
                        }
                    }
                }
            }
            R.id.etCMSDate -> {
                Comman_Methods.avoidDoubleClicks(v)
                dateparse = null
                setDateField(etCMSDate)
            }
            R.id.etCMEDate -> {
                if (etCMSDate.text.toString().trim().isNotEmpty()) {
                    Comman_Methods.avoidDoubleClicks(v)
                    setDateField(etCMEDate)
                }else{
                    mActivity.showMessage(mActivity.resources.getString(R.string.request_start_date))
                }
            }
            /*R.id.btnViewReport -> {
                mActivity.hideKeyboard()
                if (checkValidation()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        checkLocationPermission()
                    } else {
                        if (gpstracker?.CheckForLoCation() == false) {
                            Utils.showSettingsAlert(mActivity)
                        } else {
                            if (spinReportType.selectedItemPosition > 2){
                                callMemberRouteReport(MemberID,
                                    etCMSDate.text.toString())
                            }else {
                                callViewReport(
                                    MemberID,
                                    etCMSDate.text.toString(),
                                    etCMEDate.text.toString(),
                                    type
                                )
                            }
                        }
                    }
                }
            }*/
        }
    }

    private fun callApi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setPermission()
        } else {
            callDownloadReport(
                MemberID,
                etCMSDate.text.toString(),
                etCMEDate.text.toString(),
                MemberName,
                MemberEmail,
                type
            )
        }
    }

    companion object {
        fun newInstance(
            isFrom: Boolean,
            frequencyPremiumReport: FrequencyHistoryResult
        ): ReportFragment {
            val args = Bundle()
            args.putBoolean(ARG_PARAM1, isFrom)
            args.putParcelable(ARG_PARAM2, frequencyPremiumReport)
            val fragment = ReportFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isFrom = it.getBoolean(ARG_PARAM1, false)
            frequencyPremiumReport = it.getParcelable(ARG_PARAM2) ?: FrequencyHistoryResult()
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
        return inflater.inflate(R.layout.fragment_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appDataBase = OldMe911Database.getDatabase(mActivity)
        gpstracker = GpsTracker(mActivity)
        if (appDataBase.loginDao().getAll().isAdmin) {
            spinChildMember.visibility = View.VISIBLE
            if (isFrom){
                paymentMemberList()
            }else{
                callGetMember(appDataBase.loginDao().getAll().memberID)
            }
        } else {
            MemberID = appDataBase.loginDao().getAll().memberID
            MemberName = appDataBase.loginDao().getAll().firstName + " " + appDataBase.loginDao().getAll().lastName
            spinChildMember.visibility = View.GONE
        }
        setHeader()
//        spinReportType.setTitle(mActivity.resources.getString(R.string.str_select_type_report))
        spinReportType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (position){
                    0 -> {
                        type = ""
                        etCMEDate.visibility = View.VISIBLE
                        btnGenerateReport.text = mActivity.resources.getString(R.string.generate_report)
//                        tvHeader.text = mActivity.resources.getString(R.string.reports)
                    }
                    1 ->{
                        type = "Miles"
                        etCMEDate.visibility = View.VISIBLE
                        btnGenerateReport.text = mActivity.resources.getString(R.string.generate_report)
//                        tvHeader.text = mActivity.resources.getString(R.string.business_track)
                    }
                    2 -> {
                        type = ""
                        etCMEDate.visibility = View.VISIBLE
                        btnGenerateReport.text = mActivity.resources.getString(R.string.generate_report)
//                        tvHeader.text = mActivity.resources.getString(R.string.child_monitoring)
                    }
                    3 -> {
                        etCMEDate.visibility = View.GONE
                        btnGenerateReport.text = mActivity.resources.getString(R.string.view_report_map)
//                        tvHeader.text = mActivity.resources.getString(R.string.where_have_been)
                    }
                }
            }
        }

        spinChildMember.setTitle(mActivity.resources.getString(R.string.select_user))
        btnGenerateReport.setOnClickListener(this)
        etCMSDate.setOnClickListener(this)
        etCMEDate.setOnClickListener(this)
    }

    private fun setHeader() {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        mActivity.checkNavigationItem(6)
        tvHeader.text = mActivity.resources.getString(R.string.reports)
        Utils.setTextGradientColor(tvHeader)
        when (spinReportType.selectedItemPosition) {
            0 -> {
                etCMEDate.visibility = View.VISIBLE
                btnGenerateReport.text = mActivity.resources.getString(R.string.generate_report)
//                tvHeader.text = mActivity.resources.getString(R.string.reports)
            }
            1 -> {
                etCMEDate.visibility = View.VISIBLE
                btnGenerateReport.text = mActivity.resources.getString(R.string.generate_report)
//                tvHeader.text = mActivity.resources.getString(R.string.business_track)
            }
            2 -> {
                etCMEDate.visibility = View.VISIBLE
                btnGenerateReport.text = mActivity.resources.getString(R.string.generate_report)
//                tvHeader.text = mActivity.resources.getString(R.string.child_monitoring)
            }
            3 -> {
                etCMEDate.visibility = View.GONE
                btnGenerateReport.text = mActivity.resources.getString(R.string.view_report_map)
//                tvHeader.text = mActivity.resources.getString(R.string.where_have_been)
            }
        }
        tvHeader.setPadding(0, 0, 50, 0)
        if (appDataBase.loginDao().getAll().isAdmin){
            iv_back.visibility = View.VISIBLE
            iv_menu.visibility = View.GONE
            mActivity.disableDrawer()
        }else{
            mActivity.enableDrawer()
            iv_back.visibility = View.GONE
            iv_menu.visibility = View.VISIBLE
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


    private fun checkLocationPermission() {
        KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
            .permissions(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION)
            .onAccepted { permissions ->
                if (permissions.size == 2) {
                    if (ContextCompat.checkSelfPermission(
                            mActivity, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                            mActivity, Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        if (gpstracker?.CheckForLoCation() == false) {
                            Utils.showLocationSettingsAlert(mActivity)
                        } else {
                            if (spinReportType.selectedItemPosition > 2) {
                                callMemberRouteReport(
                                    MemberID,
                                    etCMSDate.text.toString()
                                )
                            }
                        }
                    }
                }
            }
            .onDenied {
                checkLocationPermission()
            }
            .onForeverDenied {
            }
            .ask()
    }

    private fun paymentMemberList(){
        memberList = ArrayList()
        val defaultmemberBean = MemberBean()
        defaultmemberBean.id = -1
        defaultmemberBean.memberName = mActivity.resources.getString(R.string.select_user)
        memberList.add(defaultmemberBean)
        for (k in 0 until frequencyPremiumReport.lstPremiumMember.size){
            if (frequencyPremiumReport.lstPremiumMember[k].isDeleted == false) {
                val frequencyUser = MemberBean()
                frequencyUser.id = frequencyPremiumReport.lstPremiumMember[k].memberID
                frequencyUser.memberName = frequencyPremiumReport.lstPremiumMember[k].name
                frequencyUser.memberEmail = frequencyPremiumReport.lstPremiumMember[k].email
                frequencyUser.isSelected = frequencyPremiumReport.lstPremiumMember[k].isSelected
                frequencyUser.isPaymentDone =
                    frequencyPremiumReport.lstPremiumMember[k].isPaymentDone
                memberList.add(frequencyUser)
            }
        }
        particularMemberList()
    }

    private fun callGetMember(memberID: Int) {
        memberList = ArrayList()
        val defaultmemberBean = MemberBean()
        defaultmemberBean.id = -1
        defaultmemberBean.memberName = mActivity.resources.getString(R.string.select_user)
        memberList.add(defaultmemberBean)
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
            memberBean.memberName =
                appDataBase.memberDao().getAllMember()[i].firstName + " " + appDataBase.memberDao().getAllMember()[i].lastName
            memberBean.memberEmail = appDataBase.memberDao().getAllMember()[i].email
            memberList.add(memberBean)
        }
        if (memberList.size > 0) {
            val memberListName = ArrayList<String>()
            for (i in 0 until memberList.size) {
                memberListName.add(memberList[i].memberName ?: "")
            }
            val memberName =
                ArrayAdapter(mActivity, R.layout.comman_textview, memberListName)
            spinChildMember.adapter = memberName
        }
        spinChildMember.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                MemberID = memberList[position].id ?: 0
                MemberName = memberList[position].memberName ?: ""
                MemberEmail = memberList[position].memberEmail ?: ""
            }
        }
    }

    private fun particularMemberList(){
        if (memberList.size > 0) {
            val memberListName = ArrayList<String>()
            for (i in 0 until memberList.size) {
                memberListName.add(memberList[i].memberName ?: "")
            }
            val memberName =
                ArrayAdapter(mActivity, R.layout.comman_textview, memberListName)
            spinChildMember.adapter = memberName
        }
        spinChildMember.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                MemberID = memberList[position].id ?: 0
                MemberName = memberList[position].memberName ?: ""
                MemberEmail = memberList[position].memberEmail ?: ""
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            setHeader()
        }
    }

    private fun setDateField(et_date: EditText) {
        mActivity.hideKeyboard()
        dateFormatter = SimpleDateFormat(OUTED_DATE, Locale.US)
        val newCalendar = Calendar.getInstance()
        if (et_date.text.toString().trim() != "") {
            try {
                newCalendar.time = dateFormatter?.parse(et_date.text.toString().trim())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        datePickerDialog = DatePickerDialog(
            mActivity,
            { view, year, monthOfYear, dayOfMonth ->
                val newDate = Calendar.getInstance()
                newDate.set(year, monthOfYear, dayOfMonth)
                et_date.setText(dateFormatter?.format(newDate.time))
                try {
                    try {
                        if (dateparse == null) {
                            dateparse = dateFormatter?.parse(dateFormatter?.format(newDate.time))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.setButton(
            DialogInterface.BUTTON_NEGATIVE, mActivity.resources.getString(R.string.cancel)
        ) { dialog, which -> datePickerDialog.dismiss() }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            if (dateparse != null) {
                datePickerDialog.datePicker.minDate = dateparse?.time ?: Date().time
            }
        }
        datePickerDialog.datePicker.maxDate = Date().time
        datePickerDialog.show()
    }

    private fun checkValidation(): Boolean {
        return when {
            spinReportType.selectedItemPosition == 0 -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_report_type))
                false
            }
            appDataBase.loginDao().getAll().isAdmin && memberList[spinChildMember.selectedItemPosition].id == -1 -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.selectWorker))
                false
            }
            etCMSDate.text.toString().trim().isEmpty() -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.request_start_date))
                false
            }
            etCMEDate.visibility == View.VISIBLE && etCMEDate.text.toString().trim().isEmpty() -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.request_end_date))
                false
            }
            etCMEDate.visibility == View.VISIBLE && SimpleDateFormat(OUTED_DATE).parse(etCMSDate.text.toString()).time > SimpleDateFormat(
                OUTED_DATE
            ).parse(
                etCMEDate.text.toString()
            ).time -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.request_proper_date))
                false
            }
            else -> true
        }
    }

    private fun callViewReport(
        memberID: Int,
        diagStartDate: String,
        diagEndDate: String,
        type: String
    ) {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            isProgressShow(mActivity)
            val callDownloadReport = WebApiClient.getInstance(mActivity).webApi_without?.viewBusinessReport(
                memberID,
                diagStartDate,
                diagEndDate,
                type,
                "Oldme911"
            )
            callDownloadReport?.enqueue(object : retrofit2.Callback<MemberRouteResponse> {
                override fun onFailure(call: Call<MemberRouteResponse>?, t: Throwable) {
                    isProgressHide()
                }

                override fun onResponse(call: Call<MemberRouteResponse>, response: Response<MemberRouteResponse>) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            isProgressHide()
                            response.body()?.let {
                                if (it.status == true) {
                                    val reportList = it.result ?: ArrayList()
                                    if (reportList.size > 0) {
                                        circles = ArrayList()
                                        for (i in 0 until reportList.size) {
                                            memRouteName = reportList[i].name ?: ""
                                            val reportLocList = reportList[i].locs ?: ArrayList()
                                            if (reportLocList.size > 0) {
                                                circles.addAll(reportLocList)
                                            }
                                        }
                                        mActivity.addFragment(
                                            ReportMapFragment.newInstance(
                                                memRouteName,
                                                circles
                                            ), true, true, AnimationType.bottomtotop
                                        )
                                    } else {
                                        mActivity.showMessage(mActivity.resources.getString(R.string.no_route_date))
                                    }
                                } else {
                                    mActivity.showMessage(it.message ?: "")
                                }
                            }
                        }
                    } else {
                        isProgressHide()
                        Utils.showSomeThingWrongMessage(mActivity)
                    }
                }
            })
        } else {
            Utils.showNoInternetMessage(mActivity)
        }
    }


    private fun callMemberRouteReport(
        memberID: Int,
        diagStartDate: String
    ) {
        Utils.memberRouteListApi(mActivity, memberID, diagStartDate, object : CommonApiListener {
            override fun memberRouteResponse(
                status: Boolean,
                resultRoute: ArrayList<ResultRoute>,
                message: String,
                responseMessage: String
            ) {
                if (status) {
                    if (resultRoute.size > 0) {
                        circles = ArrayList()
                        for (i in 0 until resultRoute.size) {
                            memRouteName = resultRoute[i].name ?: ""
                            val routeLoc = resultRoute[i].locs ?: ArrayList()
                            if (routeLoc.size > 0) {
                                circles.addAll(routeLoc)
                            }
                        }
                        mActivity.addFragment(
                            ReportMapFragment.newInstance(
                                memRouteName,
                                circles
                            ), true, true, AnimationType.bottomtotop
                        )
                    } else {
                        mActivity.showMessage(mActivity.resources.getString(R.string.no_route_date))
                    }
                }
            }
        })
    }

    private fun callDownloadReport(
        memberID: Int,
        diagStartDate: String,
        diagEndDate: String,
        memberName: String,
        memerEmail: String,
        type: String
    ) {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            isProgressShow(mActivity)
            val callDownloadReport = WebApiClient.getInstance(mActivity).webApi_without?.downloadBusinessReport(
                memberID,
                diagStartDate,
                diagEndDate,
                type,
                "Oldme911"
            )
            callDownloadReport?.enqueue(object : retrofit2.Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>?, t: Throwable) {
                    isProgressHide()
                }

                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        downloadFile(
                            BuildConfig.BASE_URL + "Oldme911Application/DownloadReport?MemberID=" + memberID + "&StartDate=" + diagStartDate + "&EndDate=" + diagEndDate + "&Type="+type+"&ApplicationType=Oldme911",
                            memberName,
                            memberID,
                            diagStartDate,
                            diagEndDate,
                            memerEmail,
                            type
                        )
                    } else {
                        isProgressHide()
                        mActivity.showMessage(response.errorBody().toString())
                    }
                }
            })
        } else {
            Utils.showNoInternetMessage(mActivity)
        }
    }

    fun downloadFile(
        fileURL: String,
        memberName: String,
        memberID: Int,
        diagStartDate: String,
        diagEndDate: String,
        memerEmail: String,
        type: String
    ) {

        isProgressShow(mActivity)
        mActivity.isSpeedAvailable()
        var pdfFile: File? = null
        try {
            val url = URL(fileURL)
            val httpConn: HttpURLConnection = url.openConnection() as HttpURLConnection
            httpConn.doInput = true
            httpConn.requestMethod = "GET"
            httpConn.setRequestProperty("Content-Type", "application/" + "json")

            httpConn.connect()

            val responseCode: Int = httpConn.responseCode
            // always check HTTP response code first
            if (responseCode == HttpURLConnection.HTTP_OK) {
                var fileName = ""
                val disposition = httpConn.getHeaderField("Content-Disposition")
                val contentType = httpConn.contentType
                val contentLength = httpConn.contentLength

                if (disposition != null) {
                    // extracts file name from header field
                    val index: Int = disposition.indexOf("filename=")
                    if (index > 0) {
                        fileName = disposition.substring(
                            index + 10,
                            disposition.length - 1
                        )
                    }
                } else {
                    // extracts file name from URL
                    val callDownloadReport = WebApiClient.getInstance(mActivity).webApi_without?.BusinessReport(
                        memberID,
                        diagStartDate,
                        diagEndDate,
                        type,
                        "Oldme911"
                    )
                    callDownloadReport?.enqueue(object : retrofit2.Callback<ApiResponse> {
                        override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                            isProgressHide()
                            sendEmail = false
                            viewReport = false
                        }

                        override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                            isProgressHide()
                            if (response.isSuccessful) {
                                response.body()?.let {
                                    if (!it.status) {
                                        isProgressHide()
                                        sendEmail = false
                                        viewReport = false
                                        mActivity.showMessage(it.responseMessage ?: "")
                                    }
                                }
                            }
                        }
                    })
                }

                println("Content-Type = $contentType")
                println("Content-Disposition = $disposition")
                println("Content-Length = $contentLength")
                println("fileName = $fileName")

                // opens input stream from the HTTP connection
                var inputStream = httpConn.inputStream
                inputStream = BufferedInputStream(inputStream, contentLength)
                val folder = Utils.getStorageRootPath(mActivity)
                if (!folder.exists()) {
                    folder.mkdir()
                }
                val subFolder: File = File(folder, if (type=="Miles") "/MilesTraveledReport/" else "/MemberRouteReport/")
                if (!subFolder.exists()) {
                    subFolder.mkdir()
                }
                val storeFileName = memberName + "_" + fileName
                pdfFile = File(subFolder.toString() + File.separator + storeFileName)
                try {
                    pdfFile.createNewFile()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // opens an output stream to save into file
                val outputStream = FileOutputStream(pdfFile)
                var bytesRead = 0
                val buffer = ByteArray(contentLength)
                while ({ bytesRead = inputStream.read(buffer); bytesRead }() != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.close()
                httpConn.disconnect()
                if (!sendEmail && !viewReport) {
                    mActivity.showMessage(mActivity.resources.getString(R.string.file_download))
                }
                isProgressHide()
                httpConn.disconnect()
            } else {
                sendEmail = false
                viewReport = false
                mActivity.showMessage(httpConn.responseMessage)
                println("No file to download. Server replied HTTP code: $responseCode")
                httpConn.disconnect()
                isProgressHide()
            }
        } catch (e: Exception) {
            sendEmail = false
            viewReport = false
            isProgressHide()
            e.printStackTrace()
        }
        if (sendEmail) {
            try {
                val emailIntent = Intent(Intent.ACTION_SEND)
                emailIntent.type = "plain/text"
                emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(memerEmail))
                emailIntent.putExtra(
                    Intent.EXTRA_SUBJECT,
                    memberName + " " + mActivity.resources.getString(R.string.child_monitoring) + " " + mActivity.resources.getString(
                        R.string.report
                    )
                )
                if (Uri.fromFile(pdfFile) != null) {
                    if (Build.VERSION.SDK_INT > 24){
                        emailIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(mActivity, BuildConfig.APPLICATION_ID + ".provider",pdfFile!!))
                    }else{
                        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(pdfFile))
                    }
                }
                emailIntent.putExtra(Intent.EXTRA_TEXT, "")
                this.startActivity(
                    Intent.createChooser(
                        emailIntent,
                        mActivity.resources.getString(R.string.send_email)
                    )
                )
            } catch (t: Exception) {
                t.printStackTrace()
            }
        } else if (viewReport) {
            mActivity.hideKeyboard()
            mActivity.addFragment(
                PlaceReportFragment.newInstance(pdfFile.toString()),
                true,
                true,
                AnimationType.fadeInfadeOut
            )
        }
    }


    private fun setPermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
                .permissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                .onAccepted { permissions ->
                    if (permissions.size == 1) {
                        callDownloadReport(
                            MemberID,
                            etCMSDate.text.toString().trim(),
                            etCMEDate.text.toString().trim(),
                            MemberName,
                            MemberEmail,
                            when (spinReportType.selectedItemPosition) {
                                0 -> ""
                                1 -> "Miles"
                                2 -> ""
                                else -> ""
                            }
                        )
                    }
                }
                .onDenied {
                    setPermission()
                }
                .onForeverDenied {
                    Utils.showSettingsAlert(mActivity)
                }
                .ask()
        } else {
            KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
                .permissions(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                .onAccepted { permissions ->
                    if (permissions.size == 2) {
                        callDownloadReport(
                            MemberID,
                            etCMSDate.text.toString().trim(),
                            etCMEDate.text.toString().trim(),
                            MemberName,
                            MemberEmail,
                            when (spinReportType.selectedItemPosition) {
                                0 -> ""
                                1 -> "Miles"
                                2 -> ""
                                else -> ""
                            }
                        )
                    }
                }
                .onDenied {
                    setPermission()
                }
                .onForeverDenied {
                    Utils.showSettingsAlert(mActivity)
                }
                .ask()
        }
    }
}
