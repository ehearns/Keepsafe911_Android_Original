package com.keepSafe911.fragments.homefragment.boundary

import AnimationType
import addFragment
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.generic.GenericDraweeHierarchy
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
import com.facebook.drawee.generic.RoundingParams
import com.facebook.drawee.view.SimpleDraweeView
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.model.GeoFenceResult
import com.keepSafe911.model.LstGeoFenceMember
import com.keepSafe911.model.MemberBean
import com.keepSafe911.model.response.AddUpdateGeoFenceResponse
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.webservices.WebApiClient
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.keepSafe911.BuildConfig
import com.keepSafe911.HomeActivity
import com.keepSafe911.R
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.listner.PositiveButtonListener
import com.keepSafe911.model.FamilyMonitorResult
import com.keepSafe911.utils.*
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_geo_member.*
import kotlinx.android.synthetic.main.raw_new_memberlist.view.*
import kotlinx.android.synthetic.main.row_select_member.view.*
import kotlinx.android.synthetic.main.toolbar_header.*
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class GeoMemberFragment : HomeBaseFragment(), View.OnClickListener {

    private var isUpdate = false
    private var geoMemberList: ArrayList<LstGeoFenceMember> = ArrayList()
    private var geoFenceResultList: ArrayList<GeoFenceResult> = ArrayList()
    private var memberList: ArrayList<MemberBean> = ArrayList()
    private lateinit var geoFenceResult: GeoFenceResult
    private var dateFormatter: SimpleDateFormat? = null
    private lateinit var datePickerDialog: DatePickerDialog
    private lateinit var timePickerDialog: TimePickerDialog
    private lateinit var appDatabase: OldMe911Database
    var deleteArrayList: ArrayList<Int> = ArrayList()
    private lateinit var geoMemberAdapter: GeoMemberAdapter

    companion object {
        fun newInstance(
            isUpdate: Boolean,
            geoFenceResult: GeoFenceResult
        ): GeoMemberFragment {
            val args = Bundle()
            args.putBoolean(ARG_PARAM1, isUpdate)
            args.putParcelable(ARG_PARAM2, geoFenceResult)
            val fragment = GeoMemberFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isUpdate = it.getBoolean(ARG_PARAM1, false)
            geoFenceResult = it.getParcelable(ARG_PARAM2) ?: GeoFenceResult()
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
        return inflater.inflate(R.layout.fragment_geo_member, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appDatabase = OldMe911Database.getDatabase(mActivity)
        deleteArrayList = ArrayList()
        setHeader()
        mActivity.disableDrawer()
        etGeoStartDateTime.setOnClickListener(this)
        etGeoEndDateTime.setOnClickListener(this)
        ivAddMultipleMember.setOnClickListener(this)
        btnAddGeoFence.setOnClickListener(this)
        callGetMember()


        val formatter = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
        var diagStartDate = ""
        var diagEndDate = ""
        var geoMemberStart = ""
        var geoMemberEnd = ""
        try {
            var date1: Date? = null
            var date2: Date? = null
            if (geoFenceResult.startDate != null) {
                date1 = formatter.parse(geoFenceResult.startDate ?: "")
            }
            if (geoFenceResult.endDate != null) {
                date2 = formatter.parse(geoFenceResult.endDate ?: "")
            }
            val target = SimpleDateFormat(INDIAN_DATE_TIME)
            if (date1 != null) {
                diagStartDate = target.format(date1)
            }
            if (date2 != null) {
                diagEndDate = target.format(date2)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            var date1: Date? = null
            var date2: Date? = null
            if (geoFenceResult.startDate != null) {
                date1 = formatter.parse(geoFenceResult.startDate ?: "")
            }
            if (geoFenceResult.endDate != null) {
                date2 = formatter.parse(geoFenceResult.endDate ?: "")
            }
            val target = SimpleDateFormat(CHECK_DATE_TIME3)
            if (date1 != null) {
                geoMemberStart = target.format(date1)
            }
            if (date2 != null) {
                geoMemberEnd = target.format(date2)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        etGeoStartDateTime.setText(if (diagStartDate != "") diagStartDate else geoMemberStart)
        etGeoEndDateTime.setText(if (diagEndDate != "") diagEndDate else geoMemberEnd)
    }

    private fun setAdapter(geoMemberListing: ArrayList<LstGeoFenceMember>) {
        geoMemberAdapter = GeoMemberAdapter(mActivity, geoMemberListing)
        rvGeoMember.adapter = geoMemberAdapter
        geoMemberAdapter.notifyDataSetChanged()
    }

    private fun setHeader() {
        mActivity.disableDrawer()
        iv_back.visibility = View.VISIBLE
        tvHeader.text = mActivity.resources.getString(R.string.geofence_detail)
        Utils.setTextGradientColor(tvHeader)
        tvHeader.setPadding(0, 0, 50, 0)
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.addFragment(
                AddGeofenceFragment.newInstance(
                    isUpdate,
                    geoFenceResult, false
                ),
                true,
                true,
                animationType = AnimationType.fadeInfadeOut
            )
        }
        mActivity.checkUserActive()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.etGeoStartDateTime -> {
                Comman_Methods.avoidDoubleClicks(v)
                setDateField(etGeoStartDateTime, 1)
            }
            R.id.etGeoEndDateTime -> {
                Comman_Methods.avoidDoubleClicks(v)
                setDateField(etGeoEndDateTime, 2)
            }
            R.id.ivAddMultipleMember -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                updateWorkerData()
                if (memberList.size > 0) {
                    /*for (i in 0 until memberList.size){
                        memberList[i].isSelected = false
                    }*/
                    showMemberListDialog(memberList)
                }
            }
            R.id.btnAddGeoFence -> {
                mActivity.hideKeyboard()
                if (checkValidation()) {
                    Comman_Methods.avoidDoubleClicks(v)
                    val formatter = SimpleDateFormat(INDIAN_DATE_TIME)
                    val formatter2 = SimpleDateFormat(CHECK_DATE_TIME3)
                    var diagStartDate = ""
                    var diagEndDate = ""
                    var geoStartDate = ""
                    var geoEndDate = ""
                    var diagStartTime = ""
                    var diagEndTime = ""
                    var geoStartTime = ""
                    var geoEndTime = ""
                    try {
                        var date1: Date? = null
                        var date2: Date? = null
                        if (etGeoStartDateTime.text.trim().isNotEmpty()) {
                            date1 = formatter.parse(etGeoStartDateTime.text.toString())
                        }
                        if (etGeoEndDateTime.text.trim().isNotEmpty()) {
                            date2 = formatter.parse(etGeoEndDateTime.text.toString())
                        }
                        val target = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
                        if (date1 != null) {
                            diagStartDate = target.format(date1)
                        }
                        if (date2 != null) {
                            diagEndDate = target.format(date2)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    try {
                        var date1: Date? = null
                        var date2: Date? = null
                        if (etGeoStartDateTime.text.trim().isNotEmpty()) {
                            date1 = formatter2.parse(etGeoStartDateTime.text.toString())
                        }
                        if (etGeoEndDateTime.text.trim().isNotEmpty()) {
                            date2 = formatter2.parse(etGeoEndDateTime.text.toString())
                        }
                        val target = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
                        if (date1 != null) {
                            geoStartDate = target.format(date1)
                        }
                        if (date2 != null) {
                            geoEndDate = target.format(date2)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    try {
                        var date1: Date? = null
                        var date2: Date? = null
                        if (etGeoStartDateTime.text.trim().isNotEmpty()) {
                            date1 = formatter.parse(etGeoStartDateTime.text.toString())
                        }
                        if (etGeoEndDateTime.text.trim().isNotEmpty()) {
                            date2 = formatter.parse(etGeoEndDateTime.text.toString())
                        }
                        val target = SimpleDateFormat(TIME_FORMAT)
                        if (date1 != null) {
                            diagStartTime = target.format(date1)
                        }
                        if (date2 != null) {
                            diagEndTime = target.format(date2)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    try {
                        var date1: Date? = null
                        var date2: Date? = null
                        if (etGeoStartDateTime.text.trim().isNotEmpty()) {
                            date1 = formatter2.parse(etGeoStartDateTime.text.toString())
                        }
                        if (etGeoEndDateTime.text.trim().isNotEmpty()) {
                            date2 = formatter2.parse(etGeoEndDateTime.text.toString())
                        }
                        val target = SimpleDateFormat(TIME_FORMAT_24)
                        if (date1 != null) {
                            geoStartTime = target.format(date1)
                        }
                        if (date2 != null) {
                            geoEndTime = target.format(date2)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    geoFenceResult.startDate = if (diagStartDate != "") diagStartDate else geoStartDate
                    geoFenceResult.endDate = if (diagEndDate != "") diagEndDate else geoEndDate
                    geoFenceResult.startTime = if (diagStartTime!="") diagStartTime else geoStartTime
                    geoFenceResult.endTime = if (diagEndTime!="") diagEndTime else geoEndTime
                    callAddUpdateGeoFenceApi()
                }
            }
        }
    }


    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            setHeader()
        }
    }

    private fun setDateField(et_date: EditText, value: Int) {
        var dateString = ""
        mActivity.hideKeyboard()
        dateFormatter = SimpleDateFormat(OUTED_DATE2, Locale.US)
        val newCalendar = Calendar.getInstance()
        if (et_date.text.toString().trim() != "") {
            try {
                newCalendar.time = dateFormatter?.parse(et_date.text.toString().trim())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        datePickerDialog = DatePickerDialog(
            mActivity, { view, year, monthOfYear, dayOfMonth ->
                val newDate = Calendar.getInstance()
                newDate.set(year, monthOfYear, dayOfMonth)
                dateString = dateFormatter?.format(newDate.time) ?: ""
                setTimeField(et_date, dateString)
                try {
                    val dateParse = dateFormatter?.parse(dateFormatter?.format(newDate.time))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.setButton(
            DialogInterface.BUTTON_NEGATIVE, mActivity.resources.getString(R.string.cancel)
        ) { dialog, which -> datePickerDialog.dismiss() }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            try {
                if (etGeoEndDateTime.text.toString().isNotEmpty() && value == 1) {
                    datePickerDialog.datePicker.minDate = Date().time
                    datePickerDialog.datePicker.maxDate = dateFormatter?.parse(etGeoEndDateTime.text.toString())?.time ?: Date().time
                } else if (etGeoStartDateTime.text.toString().isNotEmpty() && value == 2) {
                    datePickerDialog.datePicker.minDate = dateFormatter?.parse(etGeoStartDateTime.text.toString())?.time ?: Date().time
                } else {
                    datePickerDialog.datePicker.minDate = Date().time
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        datePickerDialog.show()
    }

    private fun setTimeField(et_date: EditText, dateString: String) {
        var timeString = ""
        mActivity.hideKeyboard()
        dateFormatter = SimpleDateFormat(TIME_FORMAT, Locale.US)
        val newCalendar = Calendar.getInstance()
        if (et_date.text.toString().trim() != "") {
            try {
                newCalendar.time = dateFormatter?.parse(et_date.text.toString().trim())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        timePickerDialog = TimePickerDialog(
            mActivity, { view, selectedHour, selectedMinute ->
                val newDate = Calendar.getInstance()
                newDate.set(0, 0, 0, selectedHour, selectedMinute)
                timeString = dateFormatter?.format(newDate.time) ?: ""
                et_date.setText("$dateString $timeString")
                try {
                    val dateParse = dateFormatter?.parse(dateFormatter?.format(newDate.time))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, newCalendar.get(Calendar.HOUR_OF_DAY), newCalendar.get(Calendar.MINUTE), false
        )
        timePickerDialog.setButton(
            DialogInterface.BUTTON_NEGATIVE, mActivity.resources.getString(R.string.cancel)
        ) { dialog, which -> timePickerDialog.dismiss() }
        timePickerDialog.show()
    }

    private fun checkValidation(): Boolean {
        var date1: Date? = null
        var date2: Date? = null
        var date3: Date? = null
        var date4: Date? = null
        try {
            if (etGeoStartDateTime.text.toString().trim().isNotEmpty()) {
                date1 = SimpleDateFormat(INDIAN_DATE_TIME).parse(etGeoStartDateTime.text.toString())
            }
            if (etGeoEndDateTime.text.toString().trim().isNotEmpty()) {
                date2 = SimpleDateFormat(INDIAN_DATE_TIME).parse(etGeoEndDateTime.text.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            if (etGeoStartDateTime.text.toString().trim().isNotEmpty()) {
                date3 = SimpleDateFormat(CHECK_DATE_TIME3).parse(etGeoStartDateTime.text.toString())
            }
            if (etGeoEndDateTime.text.toString().trim().isNotEmpty()) {
                date4 = SimpleDateFormat(CHECK_DATE_TIME3).parse(etGeoEndDateTime.text.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        when {
            etGeoStartDateTime.text.toString().trim().isEmpty() -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_select_date))
                return false
            }
            etGeoEndDateTime.text.toString().trim().isEmpty() -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_end_date))
                return false
            }
            geoMemberList.size == 0 -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.select_member_geo))
                return false
            }
            else -> {
                if (date1 != null && date2 != null) {
                    return if ((date1.time) + (1000 * 60 * 59) >= date2.time) {
                        mActivity.showMessage(mActivity.resources.getString(R.string.val_time))
                        false
                    } else {
                        true
                    }
                } else {
                    return if (date3 != null && date4 != null) {
                        if (((date3.time) + (1000 * 60 * 59) >= date4.time)) {
                            mActivity.showMessage(mActivity.resources.getString(R.string.val_time))
                            false
                        } else {
                            true
                        }
                    } else {
                        true
                    }
                }
            }
        }
    }

    private fun callAddUpdateGeoFenceApi() {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            Comman_Methods.isProgressShow(mActivity)
            mActivity.isSpeedAvailable()
            val jsonObject = JsonObject()
            jsonObject.addProperty("ID", if (isUpdate) geoFenceResult.iD else 0)
            jsonObject.addProperty("GeoFenceName", geoFenceResult.geoFenceName)
            jsonObject.addProperty("Description", geoFenceResult.description)
            jsonObject.addProperty("StartDate", geoFenceResult.startDate)
            jsonObject.addProperty("StartTime", geoFenceResult.startTime)
            jsonObject.addProperty("EndDate", geoFenceResult.endDate)
            jsonObject.addProperty("EndTime", geoFenceResult.endTime)
            jsonObject.addProperty("Radius", geoFenceResult.radius)
            jsonObject.addProperty("Latitude", geoFenceResult.latitude)
            jsonObject.addProperty("Longitude", geoFenceResult.longitude)
            jsonObject.addProperty("IsActive", true)
            jsonObject.addProperty("Address", geoFenceResult.address)
            jsonObject.addProperty("AdminID", appDatabase.loginDao().getAll().memberID)
            jsonObject.addProperty("CreatedOn", Utils.getCurrentTimeStamp())
            val memberID = JsonArray()
            if (geoMemberList.size > 0) {
                for (i in 0 until geoMemberList.size) {
                    if (geoMemberList[i].geoFenceID == 0) {
                        val memberObject = JsonObject()
                        memberObject.addProperty("MemberID", geoMemberList[i].memberID)
                        memberID.add(memberObject)
                    }
                }
            }
            val deletedMemberID = JsonArray()
            if (deleteArrayList.size > 0) {
                for (i in 0 until deleteArrayList.size) {
                    val deleteObject = JsonObject()
                    deleteObject.addProperty("MemberID", deleteArrayList[i])
                    deletedMemberID.add(deleteObject)
                }
            }
            jsonObject.add("lstGeoFenceMembers", memberID)
            jsonObject.add("lstDeleteGeoFence", deletedMemberID)

            val geoFenceAddUpdateCall =
                WebApiClient.getInstance(mActivity).webApi_without?.addUpdateGeoFence(jsonObject)
            geoFenceAddUpdateCall?.enqueue(object : retrofit2.Callback<AddUpdateGeoFenceResponse> {
                override fun onFailure(call: Call<AddUpdateGeoFenceResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(
                    call: Call<AddUpdateGeoFenceResponse>,
                    response: Response<AddUpdateGeoFenceResponse>
                ) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                if (it.isStatus) {
                                    if (it.result != null) {
                                        geoFenceResultList = ArrayList()
                                        geoFenceResultList.add(it.result!!)
                                        mActivity.addFragment(
                                            GeofenceFragment(),
                                            true,
                                            true,
                                            animationType = AnimationType.fadeInfadeOut
                                        )
                                    } else {
                                        mActivity.showMessage(it.responseMessage ?: "")
                                    }
                                } else {
                                    mActivity.showMessage(it.responseMessage ?: "")
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

    inner class GeoMemberAdapter(
        private val activity: HomeActivity,
        private val memberList: ArrayList<LstGeoFenceMember>
    ) :
        RecyclerView.Adapter<GeoMemberAdapter.GeoMemberHolder>() {
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): GeoMemberHolder {
            return GeoMemberHolder(LayoutInflater.from(activity).inflate(R.layout.raw_new_memberlist, p0, false))
        }

        override fun getItemCount(): Int {
            return memberList.size
        }

        override fun onBindViewHolder(p0: GeoMemberHolder, p1: Int) {
            val weight: Int = if (activity.resources.getBoolean(R.bool.isTablet)) {
                Utils.calculateNoOfColumns(activity, 4.8)
            } else {
                Utils.calculateNoOfColumns(activity, 3.6)
            }

            val layoutParams = p0.rvMemberDetail.layoutParams
            layoutParams.width = weight
            p0.rvMemberDetail.layoutParams = layoutParams

            val roundValue: Float = Comman_Methods.convertDpToPixel(15f, mActivity)
            val builder: GenericDraweeHierarchyBuilder = GenericDraweeHierarchyBuilder(mActivity.resources)
            val roundingParams : RoundingParams = RoundingParams.fromCornersRadius(roundValue).setBorder(
                ContextCompat.getColor(mActivity, R.color.list_backcolor), 1.0f)
            val hierarchy: GenericDraweeHierarchy = builder
                .setRoundingParams(roundingParams)
                .build()

            p0.sdvMemberImage.hierarchy = hierarchy
            p0.sdvMemberImage.loadFrescoImage(mActivity, memberList[p1].Image ?: "", 1)

            p0.tvGeoMemberName.text = memberList[p1].memberName ?: ""
            p0.ivRemoveGeoMember.visibility = View.VISIBLE
            p0.ivRemoveGeoMember.setOnClickListener {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
                showDeleteMemberDialog(memberList[p1])
            }
            p0.sdvMemberImage.setOnClickListener {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
                showDeleteMemberDialog(memberList[p1])
            }
        }

        private fun showDeleteMemberDialog(uploadedFilesName: LstGeoFenceMember) {
            Comman_Methods.isCustomPopUpShow(activity,
                message = activity.resources.getString(
                    R.string.DeleteGeoFenceMember, uploadedFilesName.memberName),
                positiveButtonText = activity.resources.getString(R.string.str_ok),
                negativeButtonText = activity.resources.getString(R.string.cancel),
                positiveButtonListener = object : PositiveButtonListener {
                    override fun okClickListener() {
                        if (uploadedFilesName.geoFenceID != 0) {
                            deleteArrayList.add(uploadedFilesName.memberID)
                        }
                        memberList.remove(uploadedFilesName)
                        notifyDataSetChanged()
                    }
                })
        }

        inner class GeoMemberHolder(view: View) : RecyclerView.ViewHolder(view) {
            var rvMemberDetail: RelativeLayout = view.rvMemberDetail
            var sdvMemberImage: SimpleDraweeView = view.sdvMemberImage
            var tvGeoMemberName: TextView = view.tvMemberListName
            var ivRemoveGeoMember: ImageView = view.ivRemoveGeoMember
        }
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
            SelectGeoMemberAdapter(
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
                for (k in 0 until duplicatememberList.size) {
                    if (duplicatememberList[k].isSelected) {
                        if (geoMemberList.size > k) {
                            for (i in k until k + 1) {
                                if (geoMemberList[i].memberID != duplicatememberList[k].id) {
                                    val lstGeoFenceMember = LstGeoFenceMember()
                                    lstGeoFenceMember.memberID = duplicatememberList[k].id ?: 0
                                    lstGeoFenceMember.geoFenceID = 0
                                    lstGeoFenceMember.geoFenceTime = ""
                                    lstGeoFenceMember.id = 0
                                    lstGeoFenceMember.isStatus = true
                                    lstGeoFenceMember.memberStatus = false
                                    lstGeoFenceMember.memberName = duplicatememberList[k].memberName
                                    lstGeoFenceMember.Image = duplicatememberList[k].memberImage
                                    geoMemberList.add(lstGeoFenceMember)
                                }
                            }
                        } else {
                            val lstGeoFenceMember = LstGeoFenceMember()
                            lstGeoFenceMember.memberID = duplicatememberList[k].id ?: 0
                            lstGeoFenceMember.geoFenceID = 0
                            lstGeoFenceMember.geoFenceTime = ""
                            lstGeoFenceMember.id = 0
                            lstGeoFenceMember.isStatus = true
                            lstGeoFenceMember.memberStatus = false
                            lstGeoFenceMember.memberName = duplicatememberList[k].memberName
                            lstGeoFenceMember.Image = duplicatememberList[k].memberImage
                            geoMemberList.add(lstGeoFenceMember)
                        }
                    }
                }
                geoMemberList = geoMemberList.distinctBy { data -> data.memberID } as ArrayList<LstGeoFenceMember>
                setAdapter(geoMemberList)
                dialog.dismiss()
            }
        }


    }

    fun isMemberSelected(memberList: ArrayList<MemberBean>): Boolean {
        for (j in memberList.indices) {
            var memberbean = MemberBean()
            memberbean = memberList.get(j)
            if (memberbean.isSelected) {
                return true
            }
        }
        return false
    }


    class SelectGeoMemberAdapter() : RecyclerView.Adapter<SelectGeoMemberAdapter.SelectGeoMemberHolder>(), Filterable {

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
                    tempMemberList = if (charString.isEmpty()) {
                        memberList
                    } else {
                        val filterList = ArrayList<MemberBean>()
                        for (row in memberList) {
                            val username = row.memberName ?: ""
                            if (username.trim().lowercase().contains(charString.lowercase())) {
                                filterList.add(row)
                            }
                        }
                        filterList
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

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): SelectGeoMemberHolder {
            return SelectGeoMemberHolder(
                LayoutInflater.from(activity).inflate(R.layout.row_select_member, p0, false)
            )
        }

        override fun getItemCount(): Int {
            return tempMemberList.size
        }

        override fun onBindViewHolder(p0: SelectGeoMemberHolder, p1: Int) {
            p0.checkWorker.text = tempMemberList[p1].memberName ?: ""
            p0.checkWorker.isChecked = tempMemberList[p1].isSelected
            p0.checkWorker.isEnabled = !tempMemberList[p1].isPaymentDone
//            p0.checkWorker.setTextColor(ContextCompat.getColor(activity, R.color.bgBlack))
            p0.checkWorker.setOnCheckedChangeListener(null)
            p0.checkWorker.setOnClickListener { v ->
                val isSelected = (v as CheckBox).isChecked
                tempMemberList[p1].isSelected = isSelected
            }
//            p0.checkWorker.isChecked = tempMemberList[p1].isSelected
        }

        class SelectGeoMemberHolder(view: View) : RecyclerView.ViewHolder(view) {
            val checkWorker: CheckBox = view.checkWorker
        }
    }

    private fun updateWorkerData() {
        for (i: Int in 0 until memberList.size) {
            val memberBean = memberList[i]
            memberBean.isSelected = false
            memberBean.isPaymentDone = false
            for (j in 0 until geoMemberList.size) {
                if (geoMemberList[j].memberID == memberBean.id) {
                    memberBean.isSelected = true
                    memberBean.isPaymentDone = true
                }
            }
            memberList[i] = memberBean
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
                        if (userList.isEmpty()){
                            mActivity.showMessage(mActivity.resources.getString(R.string.no_data))
                        }
                        memberList = ArrayList()
                        geoMemberList = ArrayList()
                        val geofenceMemberList = geoFenceResult.lstGeoFenceMembers ?: ArrayList()
                        for (i: Int in 0 until userList.size) {
                            val memberBean = MemberBean()
                            memberBean.id = userList[i].iD
                            memberBean.isSelected = false
                            memberBean.isPaymentDone = false
                            memberBean.memberName =
                                (userList[i].firstName ?: "") + " " + (userList[i].lastName ?: "")
                            memberBean.memberEmail = userList[i].email ?: ""
                            memberBean.memberImage = userList[i].image ?: ""
                            for (j in 0 until geofenceMemberList.size) {
                                if (geofenceMemberList[j].memberID == userList[i].iD) {
                                    memberBean.isSelected = true
                                    memberBean.isPaymentDone = true
                                    geoMemberList.add(geofenceMemberList[j])
                                }
                            }
                            memberList.add(memberBean)
                        }

                        when {
                            userList.size > 0 -> {
/*                                when {
                                    geofenceMemberList.size > 0 -> for (i in 0 until userList.size) {
                                        for (j in 0 until geofenceMemberList.size) {
                                            if (geofenceMemberList[j].memberID == userList[i].iD) {
                                                geoMemberList.add(geofenceMemberList[j])
                                            }
                                        }
                                    }
                                    else -> geoMemberList = ArrayList()
                                }*/
                            }
                            else -> geoMemberList = ArrayList()
                        }
//                                geoMemberList = geoFenceResult.lstGeoFenceMembers ?: ArrayList()
                        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
                            rvGeoMember.layoutManager =
                                GridLayoutManager(mActivity, 4, RecyclerView.VERTICAL, false)
                        } else {
                            rvGeoMember.layoutManager =
                                GridLayoutManager(mActivity, 3, RecyclerView.VERTICAL, false)
                        }
                        setAdapter(geoMemberList)
                    } else {
                        mActivity.showMessage(message)
                    }
                }

                override fun onFailureResult() {
                    Utils.showSomeThingWrongMessage(mActivity)
                }
            })
        } else {
            Utils.showNoInternetMessage(mActivity)
        }
    }
}