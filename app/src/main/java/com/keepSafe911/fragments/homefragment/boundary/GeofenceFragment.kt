package com.keepSafe911.fragments.homefragment.boundary

import AnimationType
import addFragment
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.generic.GenericDraweeHierarchy
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
import com.facebook.drawee.generic.RoundingParams
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.keepSafe911.BuildConfig
import com.keepSafe911.HomeActivity
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.listner.PositiveButtonListener
import com.keepSafe911.model.GeoFenceResult
import com.keepSafe911.model.LstGeoFenceMember
import com.keepSafe911.model.response.ApiResponse
import com.keepSafe911.model.response.GeoFenceListResponse
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import com.keepSafe911.webservices.WebApiClient
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_geofence.*
import kotlinx.android.synthetic.main.raw_geo_fence.view.*
import kotlinx.android.synthetic.main.raw_new_memberlist.view.*
import kotlinx.android.synthetic.main.toolbar_header.*
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class GeofenceFragment : HomeBaseFragment(), View.OnClickListener {
    override fun onClick(v: View?) {
        when (v?.id){
            R.id.ivAddGeofence -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                hideAllSearchView()
                mActivity.addFragment(
                    AddGeofenceFragment.newInstance(
                        false,
                        GeoFenceResult(), true
                    ), true, true, animationType = AnimationType.fadeInfadeOut
                )
            }
        }
    }

    lateinit var appDatabase: OldMe911Database
    lateinit var geofenceAdapter: GeofenceAdapter
    private var geofenceList: ArrayList<GeoFenceResult> = ArrayList()
    private var copyGeofenceList: ArrayList<GeoFenceResult> = ArrayList()
    var isFrom: Boolean = false
    private lateinit var shareAppDialog: BottomSheetDialog

    companion object {

        fun newInstance(isFrom: Boolean): GeofenceFragment {
            val args = Bundle()
            args.putBoolean(ARG_PARAM1, isFrom)
            val fragment = GeofenceFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isFrom = it.getBoolean(ARG_PARAM1, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
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
        return inflater.inflate(R.layout.fragment_geofence, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appDatabase = OldMe911Database.getDatabase(mActivity)
        setHeader()
        setAdapter()
        ivAddGeofence.setOnClickListener(this)
        callGetGeoFenceApi()
        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            etSearchChild.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        } else {
            etSearchChild.imeOptions = EditorInfo.IME_ACTION_DONE
        }

        val addGeofenceParam = ivAddGeofence.layoutParams as ViewGroup.MarginLayoutParams
        val height = if (Comman_Methods.hasNavBar(mActivity)) {
            if (mActivity.resources.getBoolean(R.bool.isTablet)) {
                Utils.calculateNoOfRows(mActivity, 1.2)
            } else {
                Utils.calculateNoOfRows(mActivity, 1.1)
            }
        } else {
            if (mActivity.resources.getBoolean(R.bool.isTablet)) {
                Utils.calculateNoOfRows(mActivity, 1.25)
            } else {
                Utils.calculateNoOfRows(mActivity, 1.15)
            }
        }
        addGeofenceParam.setMargins(0, height, 0, 20)
    }

    private fun callGetGeoFenceApi() {
        geofenceList = ArrayList()
        copyGeofenceList = ArrayList()
        copyGeofenceList.addAll(
            appDatabase.geoFenceDao()
                .getAllGeoFenceDetail() as ArrayList<GeoFenceResult>
        )
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            Comman_Methods.isProgressShow(mActivity)
            mActivity.isSpeedAvailable()
            val geofenceApiCall = WebApiClient.getInstance(mActivity)
                .webApi_without?.getGeofenceList(appDatabase.loginDao().getAll().memberID)
            geofenceApiCall?.enqueue(object : retrofit2.Callback<GeoFenceListResponse> {
                override fun onFailure(call: Call<GeoFenceListResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(
                    call: Call<GeoFenceListResponse>,
                    response: Response<GeoFenceListResponse>
                ) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                if (it.isStatus) {
                                    val resultData = it.result ?: ArrayList()
                                    if (resultData.size > 0) {
                                        appDatabase.geoFenceDao().dropGeoFence()
                                        appDatabase.geoFenceDao()
                                            .addAllGeoFence(resultData)
                                        val actualGeofenceList: ArrayList<GeoFenceResult> =
                                            ArrayList()
                                        actualGeofenceList.addAll(
                                            appDatabase.geoFenceDao().getAllGeoFenceDetail()
                                        )
                                        if (actualGeofenceList.size > 0) {
                                            if (copyGeofenceList.size > 0) {
                                                for (i in 0 until actualGeofenceList.size) {
                                                    for (j in 0 until copyGeofenceList.size) {
                                                        if (actualGeofenceList[i].iD == copyGeofenceList[j].iD) {
                                                            actualGeofenceList[i].ex =
                                                                if (copyGeofenceList[j].ex != null) copyGeofenceList[j].ex else "Enter"
                                                            appDatabase.geoFenceDao()
                                                                .updateGeoFence(
                                                                    actualGeofenceList[i]
                                                                )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        for (i in 0 until resultData.size) {
                                            if (resultData[i].isActive) {
                                                geofenceList.add(resultData[i])
                                            }
                                        }
                                        if (geofenceList.size > 0) {
                                            tvGeofenseListNoData.visibility = View.GONE
                                            rvGeofenseList.visibility = View.VISIBLE
                                        } else {
                                            tvGeofenseListNoData.visibility = View.VISIBLE
                                            rvGeofenseList.visibility = View.GONE
                                        }
//                                    geofenceList.addAll(appDatabase.geoFenceDao().getAllGeoFenceDetail())
                                        setAdapter()
                                    } else {
                                        tvGeofenseListNoData.visibility = View.VISIBLE
                                        rvGeofenseList.visibility = View.GONE
                                    }
                                } else {
                                    mActivity.showMessage(it.message ?: "")
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
/*
            if (appDatabase.geoFenceDao().countGeoFence() > 0) {
                for (i in appDatabase.geoFenceDao().getAllGeoFenceDetail().indices) {
                    if (appDatabase.geoFenceDao().getAllGeoFenceDetail()[i].isActive) {
                        geofenceList.add(appDatabase.geoFenceDao().getAllGeoFenceDetail()[i])
                    }
                }
                if (geofenceList.size > 0) {
                    tvGeofenseListNoData.visibility = View.GONE
                    rvGeofenseList.visibility = View.VISIBLE
                } else {
                    tvGeofenseListNoData.visibility = View.VISIBLE
                    rvGeofenseList.visibility = View.GONE
                }
//                geofenceList.addAll(appDatabase.geoFenceDao().getAllGeoFenceDetail())
                setAdapter()
            } else {
                tvGeofenseListNoData.visibility = View.VISIBLE
                rvGeofenseList.visibility = View.GONE
            }
*/
        }
    }

    private fun setAdapter() {
        if (rvGeofenseList!=null) {
            rvGeofenseList.layoutManager = LinearLayoutManager(
                mActivity, RecyclerView.VERTICAL, false)
            geofenceAdapter = GeofenceAdapter(mActivity, geofenceList)
            rvGeofenseList.adapter = geofenceAdapter
            geofenceAdapter.notifyDataSetChanged()
        }
        etSearchChild.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                geofenceAdapter.filter.filter(s)
                if (s?.toString()?.isNotEmpty() == true) {
                    etSearchChild.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_x_close, 0)
                } else {
                    etSearchChild.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun hideAllSearchView() {
        tvCancel.visibility = View.GONE
        etSearchChild.setText("")
        etSearchChild.visibility = View.GONE
        ivSearchChild.visibility = View.VISIBLE
        iv_menu.visibility = View.VISIBLE
    }

    private fun setHeader() {
        mActivity.enableDrawer()
        mActivity.checkNavigationItem(4)
        tvHeader.text = mActivity.resources.getString(R.string.boundary_list)
        Utils.setTextGradientColor(tvHeader)

        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            etSearchChild.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        } else {
            etSearchChild.imeOptions = EditorInfo.IME_ACTION_DONE
        }

        mActivity.tvCancelSearch = tvCancel
        mActivity.etSearchTravel = etSearchChild
        mActivity.ivSearchTravel = ivSearchChild
        mActivity.ivMenu = iv_menu

        tvCancel.text = mActivity.resources.getString(R.string.cancel)
        etSearchChild.hint = mActivity.resources.getString(R.string.search_geofence_name)

        ivSearchChild.visibility = View.VISIBLE
        ivSearchChild.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            tvCancel.visibility = View.VISIBLE
            etSearchChild.visibility = View.VISIBLE
            ivSearchChild.visibility = View.GONE
            iv_menu.visibility = View.GONE
        }
        tvCancel.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            hideAllSearchView()
        }
        iv_menu.visibility = View.VISIBLE
        iv_menu.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.openDrawer()
        }
        etSearchChild.setOnTouchListener { v, event ->
            val DRAWABLE_LEFT = 0
            val DRAWABLE_TOP = 1
            val DRAWABLE_RIGHT = 2
            val DRAWABLE_BOTTOM = 3


            if (event.action == MotionEvent.ACTION_DOWN) {
                if (etSearchChild.compoundDrawables != null) {
                    if (etSearchChild.compoundDrawables[DRAWABLE_RIGHT] != null) {
                        if (event.rawX >= (etSearchChild.right - etSearchChild.compoundDrawables[DRAWABLE_RIGHT].bounds.width())) {
                            if (etSearchChild.text.toString().trim().isNotEmpty()) {
                                mActivity.hideKeyboard()
                                etSearchChild.isFocusable = false
                                etSearchChild.isFocusableInTouchMode = false
                                etSearchChild.setText("")
                            }
                            true
                        } else {
                            etSearchChild.isFocusable = true
                            etSearchChild.isFocusableInTouchMode = true
                        }
                    } else {
                        etSearchChild.isFocusable = true
                        etSearchChild.isFocusableInTouchMode = true
                    }
                } else {
                    etSearchChild.isFocusable = true
                    etSearchChild.isFocusableInTouchMode = true
                }
            }
            false
        }
        mActivity.checkUserActive()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            setHeader()
        }
    }

    inner class GeofenceAdapter() : RecyclerView.Adapter<GeofenceAdapter.GeofenceHolder>(), Filterable {

        private lateinit var activity: HomeActivity
        private var geofenceList: ArrayList<GeoFenceResult> = ArrayList()
        private var duplicateGeofenceList: ArrayList<GeoFenceResult> = ArrayList()

        constructor(activity: HomeActivity, geofenceList: ArrayList<GeoFenceResult>) : this() {
            this.activity = activity
            this.geofenceList = geofenceList
            duplicateGeofenceList = ArrayList()
            duplicateGeofenceList.addAll(geofenceList)
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): GeofenceHolder {
            return GeofenceHolder(
                LayoutInflater.from(activity).inflate(R.layout.raw_geo_fence, p0, false)
            )
        }

        override fun getItemCount(): Int {
            return duplicateGeofenceList.size
        }

        override fun onBindViewHolder(holder: GeofenceHolder, position: Int) {
            holder.tvGeofenceTitle.text = if (duplicateGeofenceList[position].geoFenceName!=null) ": "+ duplicateGeofenceList[position].geoFenceName else ""
            val formatter = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
            var diagStartDate = ""
            var diagEndDate = ""
            var geoStartDate = ""
            var geoEndDate = ""
            // with Am/Pm display
            try {
                val date1: Date? = formatter.parse(duplicateGeofenceList[position].startDate ?: "")
                val date2: Date? = formatter.parse(duplicateGeofenceList[position].endDate ?: "")
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
            // without Am/Pm display
            try {
                val date1: Date? = formatter.parse(duplicateGeofenceList[position].startDate ?: "")
                val date2: Date? = formatter.parse(duplicateGeofenceList[position].endDate ?: "")
                val target = SimpleDateFormat(CHECK_DATE_TIME3)
                if (date1 != null) {
                    geoStartDate = target.format(date1)
                }
                if (date2 != null) {
                    geoEndDate = target.format(date2)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            holder.tvGeoStartDate.text = ": "+if (diagStartDate != "") diagStartDate else geoStartDate
            holder.tvGeoEndDate.text = ": " + if (diagEndDate != "") diagEndDate else geoEndDate

            val memberListImage = duplicateGeofenceList[position].lstGeoFenceMembers ?: ArrayList()
            if (memberListImage.size > 0) {
                val firstImage = memberListImage[0].Image
                holder.sdvGeoMemberImage.loadFrescoImage(mActivity, firstImage ?: "", 1)
            }
            holder.switchGeo.setOnCheckedChangeListener(null)
            holder.switchGeo.setOnClickListener {
                activity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
            }
            holder.switchGeo.isChecked = duplicateGeofenceList[position].isActive
            holder.ivEditGeo.setOnClickListener {
                if (duplicateGeofenceList[position].isActive) {
                    activity.hideKeyboard()
                    Comman_Methods.avoidDoubleClicks(it)
                    hideAllSearchView()
                    activity.addFragment(
                        AddGeofenceFragment.newInstance(
                            true,
                            duplicateGeofenceList[position], true
                        ),
                        true,
                        true,
                        animationType = AnimationType.fadeInfadeOut
                    )
                } else {
                    activity.showMessage(
                        activity.resources.getString(
                            R.string.activate_geofence,
                            duplicateGeofenceList[position].geoFenceName
                        )
                    )
                }
            }
            holder.ivDeleteGeo.setOnClickListener {
                activity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
                travelDeleteDialog(duplicateGeofenceList[position])
            }
            holder.flUserList.setOnClickListener {
                activity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
                val memberList = duplicateGeofenceList[position].lstGeoFenceMembers
                if (memberList != null) {
                    if (memberList.size > 0) {
                        shareOption(memberList)
                    }
                }
            }
        }

        private fun travelDeleteDialog(geoFenceResult: GeoFenceResult) {
            Comman_Methods.isCustomPopUpShow(activity,
                title = activity.resources.getString(R.string.travel_conf),
                message = activity.resources.getString(R.string.wish_alert),
                positiveButtonListener = object : PositiveButtonListener {
                    override fun okClickListener() {
                        callActiveDeactiveApi(geoFenceResult)
                    }
                })
        }

        private fun callActiveDeactiveApi(
            geoFenceResult: GeoFenceResult
        ) {
            if (ConnectionUtil.isInternetAvailable(activity)) {
                Comman_Methods.isProgressShow(activity)
                mActivity.isSpeedAvailable()
                val callActivateDeactivate =
                    WebApiClient.getInstance(activity).webApi_without?.activeDeActiveGeoFence(
                        geoFenceResult.iD,
                        false
                    )
                callActivateDeactivate?.enqueue(object : retrofit2.Callback<ApiResponse> {
                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        Comman_Methods.isProgressHide()
                    }

                    override fun onResponse(
                        call: Call<ApiResponse>,
                        response: Response<ApiResponse>
                    ) {
                        val statusCode: Int = response.code()
                        if (statusCode == 200) {
                            if (response.isSuccessful) {
                                Comman_Methods.isProgressHide()
                                response.body()?.let {
                                    if (it.status) {
                                        geoFenceResult.isActive = !geoFenceResult.isActive
                                        duplicateGeofenceList.remove(geoFenceResult)
                                        geofenceList.remove(geoFenceResult)
                                        if (geofenceList.size > 0) {
                                            tvGeofenseListNoData.visibility = View.GONE
                                            rvGeofenseList.visibility = View.VISIBLE
                                        } else {
                                            tvGeofenseListNoData.visibility = View.VISIBLE
                                            rvGeofenseList.visibility = View.GONE
                                        }
                                    } else {
                                        activity.showMessage(it.message ?: "")
                                    }
                                }
                                notifyDataSetChanged()
                            }
                        } else {
                            Comman_Methods.isProgressHide()
                            Utils.showSomeThingWrongMessage(mActivity)
                        }
                    }
                })
            } else {
                notifyDataSetChanged()
                Utils.showNoInternetMessage(mActivity)
            }
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(charSequence: CharSequence): FilterResults {
                    val charString = charSequence.toString()
                    if (charString.isEmpty()) {
                        duplicateGeofenceList = geofenceList
                    } else {
                        val filterList = ArrayList<GeoFenceResult>()
                        for (row in geofenceList) {
                            val username = row.geoFenceName ?: ""
                            if (username.trim().lowercase().contains(charString.lowercase())) {
                                filterList.add(row)
                            }
                        }
                        duplicateGeofenceList = filterList
                    }
                    val filterResults = FilterResults()
                    filterResults.values = duplicateGeofenceList
                    return filterResults
                }

                override fun publishResults(
                    charSequence: CharSequence,
                    filterResults: FilterResults
                ) {
                    if (filterResults.values != null) {
                        duplicateGeofenceList = filterResults.values as ArrayList<GeoFenceResult>
                    }
                    notifyDataSetChanged()
                }
            }
        }

        inner class GeofenceHolder(view: View) : RecyclerView.ViewHolder(view) {
            var flUserList: FrameLayout = view.flUserList
            var sdvGeoMemberImage: SimpleDraweeView = view.sdvGeoMemberImage
            var tvGeofenceTitle: TextView = view.tvGeofenceTitle
            var tvGeoStartDate: TextView = view.tvGeoStartDate
            var tvGeoEndDate: TextView = view.tvGeoEndDate
            var switchGeo: SwitchCompat = view.switchGeo
            var ivEditGeo: ImageView = view.ivEditGeo
            var ivDeleteGeo: ImageView = view.ivDeleteGeo
        }
    }

    private fun shareOption(memberList: ArrayList<LstGeoFenceMember>){
        val view = LayoutInflater.from(mActivity)
            .inflate(
                R.layout.popup_geo_member,
                mActivity.window.decorView.rootView as ViewGroup,
                false
            )
        if (this::shareAppDialog.isInitialized){
            if (shareAppDialog.isShowing){
                shareAppDialog.dismiss()
            }
        }
        shareAppDialog = BottomSheetDialog(mActivity, R.style.appBottomSheetDialogTheme)
        shareAppDialog.setContentView(view)
        val mBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(view.parent as View)
        mBehavior.isHideable = false
        shareAppDialog.setOnShowListener {
            mBehavior.peekHeight = view.height
        }
        val rvAlertMember: RecyclerView? = shareAppDialog.findViewById(R.id.rvAlertMember)
        rvAlertMember?.layoutManager = GridLayoutManager(mActivity, 4, RecyclerView.VERTICAL, false)
        rvAlertMember?.adapter = GeoMemberAdapter(mActivity, memberList)
        shareAppDialog.show()
    }

    inner class GeoMemberAdapter(
        private val activity: HomeActivity,
        private val memberList: ArrayList<LstGeoFenceMember>
    ) :
        RecyclerView.Adapter<GeoMemberAdapter.GeoMemberHolder>() {
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): GeoMemberHolder {
            return GeoMemberHolder(
                LayoutInflater.from(activity).inflate(
                    R.layout.raw_new_memberlist,
                    p0,
                    false
                )
            )
        }

        override fun getItemCount(): Int {
            return memberList.size
        }

        override fun onBindViewHolder(p0: GeoMemberHolder, p1: Int) {
            val weight: Int = Utils.calculateNoOfColumns(activity, 4.0)
            val layoutParams = p0.rvMemberDetail.layoutParams
            layoutParams.width = weight
            p0.rvMemberDetail.layoutParams = layoutParams
            val roundValue: Float = Comman_Methods.convertDpToPixel(15f, mActivity)
            val builder = GenericDraweeHierarchyBuilder(mActivity.resources)
            val roundingParams : RoundingParams = RoundingParams.fromCornersRadius(roundValue).setBorder(
                ContextCompat.getColor(mActivity, R.color.caldroid_white), 1.0f
            )
            val hierarchy: GenericDraweeHierarchy = builder
                .setRoundingParams(roundingParams)
                .build()

            p0.sdvMemberImage.hierarchy = hierarchy
            p0.sdvMemberImage.loadFrescoImage(mActivity, (memberList[p1].Image ?: ""), 1)

            p0.tvGeoMemberName.text = memberList[p1].memberName
        }

        inner class GeoMemberHolder(view: View) : RecyclerView.ViewHolder(view) {
            var rvMemberDetail: RelativeLayout = view.rvMemberDetail
            var sdvMemberImage: SimpleDraweeView = view.sdvMemberImage
            var tvGeoMemberName: TextView = view.tvMemberListName
            var ivRemoveGeoMember: ImageView = view.ivRemoveGeoMember
        }
    }
}