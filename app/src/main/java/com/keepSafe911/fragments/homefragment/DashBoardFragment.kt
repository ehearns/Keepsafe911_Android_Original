package com.keepSafe911.fragments.homefragment

import AnimationType
import addFragment
import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.*
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.generic.GenericDraweeHierarchy
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
import com.facebook.drawee.generic.RoundingParams
import com.facebook.drawee.view.SimpleDraweeView
import com.google.gson.JsonObject
import com.kotlinpermissions.KotlinPermissions
import com.keepSafe911.Annca
import com.keepSafe911.BuildConfig
import com.keepSafe911.HomeActivity
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.fragments.homefragment.find.AddMemberFragment
import com.keepSafe911.fragments.payment_selection.UpdateSubFragment
import com.keepSafe911.gps.GpsTracker
import com.keepSafe911.internal.configuration.AnncaConfiguration
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.listner.PositiveButtonListener
import com.keepSafe911.model.DashBoardBean
import com.keepSafe911.model.FamilyMonitorResult
import com.keepSafe911.model.SubscriptionBean
import com.keepSafe911.model.response.ApiResponse
import com.keepSafe911.model.response.LiveStreamResult
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import com.keepSafe911.webservices.WebApiClient
import com.vincent.videocompressor.VideoCompress
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_dash_board.*
import kotlinx.android.synthetic.main.raw_dashboard.view.*
import kotlinx.android.synthetic.main.raw_new_memberlist.view.*
import kotlinx.android.synthetic.main.toolbar_header.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class DashBoardFragment : HomeBaseFragment(), View.OnClickListener {
    override fun onClick(v: View?) {
        when (v?.id){
            R.id.ivDashUserPlus -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                if (!mActivity.purchaseSubscription()) {
                    if (appDatabase.memberDao().countMember() <= FIXED_USER_COUNT) {
                        mActivity.addFragment(
                            AddMemberFragment.newInstance(isFromList = true),
                            true, true, animationType = AnimationType.fadeInfadeOut
                        )
                    } else {
                        mActivity.showMessage(mActivity.resources.getString(R.string.member_limit))
                    }
                }
            }
            R.id.ivKeepSafe911 -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                if (!mActivity.purchaseSubscription()) {
                    open911Dialog(mActivity.resources.getString(R.string.str_emergency), 0)
                }
                /*if (!mActivity.purchaseSubscription()) {
                    mActivity.checkPermissions(false, true)
                }*/
            }
            R.id.iv_menu -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                mActivity.openDrawer()
            }
            R.id.ivLive -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                if (!mActivity.purchaseSubscription()) {
                    mActivity.checkPermissions(false, true)
                }
            }
        }
    }

    var memberList: ArrayList<FamilyMonitorResult> = ArrayList()
    var liveStreamList: ArrayList<LiveStreamResult> = ArrayList()
    var dummyLiveStreamList: ArrayList<LiveStreamResult> = ArrayList()
    var menuList: ArrayList<DashBoardBean> = ArrayList()
    lateinit var appDatabase: OldMe911Database
    lateinit var dashBoardAdapter: DashBoardAdapter
    lateinit var dashBoardMemberListAdapter: DashBoardMemberListAdapter
    var isFrom: Boolean = false
    var isCameraOpen: Boolean = false
    var sendMail: Boolean = false
    var uploadFile: File? = null
    private var gpstracker: GpsTracker? = null


    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    companion object {
        const val REPORT: Int = 2
        const val PWNED: Int = 5
        const val OPEN_CAMRA = 124
        const val CAPTURE_MEDIA:Int = 368

        fun newInstance(
            isFrom: Boolean
        ): DashBoardFragment {
            val args = Bundle()
            args.putBoolean(ARG_PARAM1, isFrom)
            val fragment = DashBoardFragment()
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
        return inflater.inflate(R.layout.fragment_dash_board, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        menuList = ArrayList()
        liveStreamList = ArrayList()
        setHeader()
        appDatabase = OldMe911Database.getDatabase(mActivity)
        gpstracker = GpsTracker(mActivity)

        ivKeepSafe911.setImageResource(R.drawable.ic_oldme_call)
        ivKeepSafe911.setOnClickListener(this)
        /*if (mActivity.resources.getBoolean(R.bool.isTablet))
            rvMenuItem.layoutManager = GridLayoutManager(mActivity, 3, RecyclerView.VERTICAL, false)
        else
            rvMenuItem.layoutManager = GridLayoutManager(mActivity, 2, RecyclerView.VERTICAL, false)*/
        rvMenuItem.layoutManager = LinearLayoutManager(mActivity, RecyclerView.VERTICAL, false)
        /*menuList.add(DashBoardBean(R.drawable.place_visit, "", mActivity.resources.getString(R.string.str_place_visit), 0))
        menuList.add(DashBoardBean(R.drawable.report, "", mActivity.resources.getString(R.string.report), 0))
        menuList.add(DashBoardBean(R.drawable.ic_deep_dark, "", mActivity.resources.getString(R.string.str_hibp), 0))*/
        dashBoardAdapter = DashBoardAdapter(mActivity, menuList, appDatabase.loginDao().getAll().isAdmin, appDatabase.loginDao().getAll().isReport)
        rvMenuItem.adapter = dashBoardAdapter

        rvDashImageList.layoutManager = LinearLayoutManager(mActivity, RecyclerView.HORIZONTAL,false)
        if (appDatabase.loginDao().getAll().isAdmin) {
            rvAdminData.visibility = View.VISIBLE
            val profilePath = appDatabase.loginDao().getAll().profilePath
            if (profilePath!=null) {
                sdvAdminImage.loadFrescoImage(mActivity,profilePath,1)
            }
            val firstName = if (appDatabase.loginDao().getAll().firstName!=null)appDatabase.loginDao().getAll().firstName else ""
            val lastName = if (appDatabase.loginDao().getAll().lastName!=null)appDatabase.loginDao().getAll().lastName else ""
            tvAdminName.text = "$firstName $lastName"
            listingViewVisibility()
            ivDashUserPlus.setOnClickListener(this)
            if (isFrom){
                callMemberApi(true)
            }else {
                if (appDatabase.memberDao().getAllMember().isNotEmpty()) {
                    setData()
                    callMemberApi(false)
                } else {
                    callMemberApi(true)
                }
            }
        }else{
            ivDashUserPlus.visibility = View.GONE
            rvAdminData.visibility = View.GONE
            listingViewVisibility()
        }

//        menuList.add(DashBoardBean(R.drawable.money, "", mActivity.resources.getString(R.string.money_back), 0))
        /*menuList.add(
            DashBoardBean(
                R.drawable.setting_change, "",
                mActivity.resources.getString(R.string.action_settings),
                0
            )
        )*/



        /*ivLogout.setOnClickListener {
            if (ConnectionUtil.isInternetAvailable(mActivity)) {
                mActivity.logoutConfirmDialog()
            } else {
                mActivity.showMwssage(mActivity.resources.getString(R.string.no_internet))
            }
        }*/
    }


    private fun setHeader() {
        mActivity.enableDrawer()
        mActivity.startLiveStreamTime(this@DashBoardFragment)
        mActivity.checkNavigationItem(0)
        iv_menu.visibility = View.VISIBLE
        ivMenuLogo.visibility = View.VISIBLE
        ivLive.visibility = View.VISIBLE
        iv_menu.setOnClickListener(this)
        ivLive.setOnClickListener(this)
        mActivity.checkUserActive()
    }

    inner class DashBoardAdapter(
        private val activity: HomeActivity,
        private val menuList: ArrayList<DashBoardBean>,
        private val admin: Boolean,
        private val isReport: Boolean
    ) : RecyclerView.Adapter<DashBoardAdapter.DashBoardHolder>() {

        override fun onCreateViewHolder(p0: ViewGroup, position: Int): DashBoardHolder {
            return DashBoardHolder(
                LayoutInflater.from(activity).inflate(R.layout.raw_dashboard, p0, false)
            )
        }

        override fun getItemCount(): Int {
            return menuList.size
        }

        override fun onBindViewHolder(holder: DashBoardHolder, position: Int) {
            /*val weight: Int
            val height: Int
            if (activity!!.resources.getBoolean(R.bool.isTablet)) {
                weight = Utils.calculateNoOfColumns(activity, 3.0)
                height = Utils.calculateNoOfRows(activity, 4.0)
            } else {
                weight = Utils.calculateNoOfColumns(activity, 2.0)
                height = Utils.calculateNoOfRows(activity, 4.5)
            }

            val layoutParams = holder.clParentRaw.layoutParams
            layoutParams.width = weight
            layoutParams.height = height
            holder.clParentRaw.layoutParams = layoutParams*/
            holder.tvORDivision.visibility = View.GONE
            holder.btnCompare.visibility = View.GONE
            val imageWidth: Int = Comman_Methods.convertDpToPixels(190F, activity).toInt()
            val imageHeight: Int = Comman_Methods.convertDpToPixels(190F, activity).toInt()
            val imageLayoutParams = holder.ivDashBoard.layoutParams
            /*imageLayoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            imageLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT*/
            imageLayoutParams.width = imageWidth
            imageLayoutParams.height = imageHeight
            holder.ivDashBoard.layoutParams = imageLayoutParams

            if (menuList[position].imageMenu > 0) {
                holder.ivDashBoard.setImageResource(menuList[position].imageMenu)
            }

            holder.tvDashBoard.text = menuList[position].textMenu

            if (menuList[position].liveMember > 0) {
                holder.tvLiveMember.visibility = View.VISIBLE
                holder.tvLiveMember.text = menuList[position].liveMember.toString()
            } else {
                holder.tvLiveMember.visibility = View.GONE
            }
            holder.flParent.setOnClickListener {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
                when (position) {
                    /*0 -> {
                        activity.checkNavigationItem(5)
                        activity.addFragment(
                            PlacesToVisitFragment(),
                            true,
                            true,
                            animationType = AnimationType.fadeInfadeOut
                        )
                    }
                    1 -> {
                         if (admin) {
                            activity.checkNavigationItem(6)
                            if (isReport) {
                                activity.addFragment(
                                    ReportPaymentHistoryFragment(),
                                    true,
                                    true,
                                    animationType = AnimationType.fadeInfadeOut
                                )
                            }else {
                                activity.addFragment(
                                    BusinessTrackFragment(),
                                    true,
                                    true,
                                    animationType = AnimationType.fadeInfadeOut
                                )
                            }
                        } else {
                            activity.checkNavigationItem(6)
                            activity.addFragment(
                                ReportFragment(),
                                true,
                                true,
                                animationType = AnimationType.fadeInfadeOut
                            )
                        }
                    }
                    2 -> {
                        activity.checkNavigationItem(9)
                            activity.addFragment(
                                SubDashBoardFragment.newInstance(com.keepSafe911.utils.PWNED),
                                true, true,
                                animationType = AnimationType.fadeInfadeOut)
                    }*/
                }
            }
        }

        inner class DashBoardHolder(view: View) : RecyclerView.ViewHolder(view) {
            var ivDashBoard: ImageView = view.ivDashBoard
            var tvDashBoard: TextView = view.tvDashBoardName
            var tvLiveMember: TextView = view.tvLiveMember
            var tvORDivision: TextView = view.tvORDivision
            var btnCompare: Button = view.btnCompare
            var clParentRaw: RelativeLayout = view.clParentRaw
            val flParent: FrameLayout = view.flParent
        }
    }

    fun open911Dialog(title: String, criteria: Int) {
        try {
            Comman_Methods.isCustomPopUpShow(mActivity,
                message = mActivity.resources.getString(R.string.message_911_alert),
                positiveButtonListener = object : PositiveButtonListener {
                    override fun okClickListener() {
                        if (criteria > 0) {
                            callEmergencyApi()
                        } else {
                            sendMail = true
                            setCameraRecordingPermission()
                        }
                    }
                })
        }catch (e: Exception){
            e.printStackTrace()
        }
    }


    fun nonMemberDialog(title: String, criteria: Int) {
        try {
            Comman_Methods.isCustomPopUpShow(mActivity,
                message = mActivity.resources.getString(R.string.no_user_message_911_alert),
                positiveButtonText = mActivity.resources.getString(R.string.str_ok),
                negativeButtonText = mActivity.resources.getString(R.string.cancel),
                positiveButtonListener = object : PositiveButtonListener {
                    override fun okClickListener() {
                        sendMail = criteria <= 0
                        setCameraRecordingPermission()
                    }
                })
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isCameraOpen) {
            if (this::dashBoardAdapter.isInitialized) {
                dashBoardAdapter.notifyDataSetChanged()
            }
        }
    }
    private fun callEmergencyApi() {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            gpstracker = GpsTracker(mActivity)
            val gpsLat = gpstracker?.getLatitude() ?: 0.0
            val gpsLng = gpstracker?.getLongitude() ?: 0.0
            Comman_Methods.isProgressShow(mActivity)
            mActivity.isSpeedAvailable()
            val loginObject: LoginObject = appDatabase.loginDao().getAll()
            val jsonObject: JsonObject = JsonObject()
            val currentAddress: String =
                if (gpsLat != 0.0 && gpsLng != 0.0) {
                    Utils.getCompleteAddressString(mActivity, gpsLat, gpsLng)
                } else {
                    val userLocation = loginObject.locationAddress ?: ""
                    if (userLocation != "") userLocation else
                        Utils.getCompleteAddressString(mActivity, loginObject.latitude, loginObject.longitude)
                }

            if (loginObject.isAdmin) {
                jsonObject.addProperty("AdminId",loginObject.memberID)
                jsonObject.addProperty("MemberID",0)
                jsonObject.addProperty("IsAdmin",true)
            }else{
                jsonObject.addProperty("AdminId",loginObject.adminID)
                jsonObject.addProperty("MemberID",loginObject.memberID)
                jsonObject.addProperty("IsAdmin",false)
            }
            jsonObject.addProperty("IsVideo", false)
            jsonObject.addProperty("LoginByApp", 2)
            jsonObject.addProperty("Address",currentAddress)
            val memberListCall = WebApiClient.getInstance(mActivity)
                .webApi_without?.emergency911(jsonObject)
            memberListCall?.enqueue(object : retrofit2.Callback<ApiResponse> {
                override fun onResponse(
                    call: Call<ApiResponse>,
                    response: Response<ApiResponse>
                ) {
                    val statusCode: Int = response.code()
                    val emergencyResponse = response.body()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {

                            Comman_Methods.isProgressHide()
                            if (emergencyResponse?.status == true) {
                                mActivity.showMessage(emergencyResponse.result)
//                                activity.showMwssage(activity.resources.getString(R.string.str_emergency_message))
                            } else {
                                mActivity.showMessage(emergencyResponse?.message ?: "")
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        Utils.showSomeThingWrongMessage(mActivity)
                    }
                    sendMail = false
                    setCameraRecordingPermission()
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                    sendMail = false
                    setCameraRecordingPermission()
                }
            })
        } else {
            Utils.showNoInternetMessage(mActivity)
        }
    }

    private fun callSendMailEmergency() {
        var currentAddress = ""
        var emergencyLat = 0.0
        var emergencyLng = 0.0
        val loginObject: LoginObject = appDatabase.loginDao().getAll()
        gpstracker = GpsTracker(mActivity)

        if (gpstracker?.CheckForLoCation() == true){
            emergencyLat = gpstracker?.getLatitude() ?: 0.0
            emergencyLng = gpstracker?.getLongitude() ?: 0.0

            currentAddress = Utils.getCompleteAddressString(mActivity, emergencyLat, emergencyLng)
        }


        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            Comman_Methods.isProgressShow(mActivity)
            mActivity.isSpeedAvailable()

            val decimalSymbols = DecimalFormatSymbols.getInstance(Locale.US)

            val memberID = if (loginObject.isAdmin) "0".toRequestBody(MEDIA_TYPE_TEXT) else loginObject.memberID.toString().toRequestBody(MEDIA_TYPE_TEXT)
            val adminId = if (loginObject.isAdmin) loginObject.memberID.toString().toRequestBody(MEDIA_TYPE_TEXT) else loginObject.adminID.toString().toRequestBody(MEDIA_TYPE_TEXT)
            val address = currentAddress.toRequestBody(MEDIA_TYPE_TEXT)
            val isVideo = "true".toRequestBody(MEDIA_TYPE_TEXT)
            val isAdmin = if (loginObject.isAdmin) "true".toRequestBody(MEDIA_TYPE_TEXT) else "false".toRequestBody(MEDIA_TYPE_TEXT)
            val loginByApp = "2".toRequestBody(MEDIA_TYPE_TEXT)
            val lat = DecimalFormat("#.######", decimalSymbols).format(emergencyLat).toRequestBody(MEDIA_TYPE_TEXT)
            val lng = DecimalFormat("#.######", decimalSymbols).format(emergencyLng).toRequestBody(MEDIA_TYPE_TEXT)
            val imageBody: RequestBody
            val part: MultipartBody.Part = when {
                uploadFile != null -> {
                    if (uploadFile?.exists() == true) {
                        imageBody = uploadFile!!.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("File", uploadFile?.name, imageBody)
                    } else {
                        imageBody = nullData.toRequestBody("multipart/form-data".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("File", null, imageBody)
                    }
                }
                else -> {
                    imageBody = nullData.toRequestBody("multipart/form-data".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("File", null, imageBody)
                }
            }
            val memberListCall = WebApiClient.getInstance(mActivity)
                .webApi_without?.sendEmailForEmergency(
                    memberID,
                    adminId,
                    address,
                    isVideo,
                    isAdmin,
                    lat,
                    lng,
                    loginByApp,
                    part
                )
            memberListCall?.enqueue(object : retrofit2.Callback<ApiResponse> {
                override fun onResponse(
                    call: Call<ApiResponse>,
                    response: Response<ApiResponse>
                ) {
                    val statusCode: Int = response.code()
                    val emergencyResponse = response.body()

                    Utils.muteRecognizer(mActivity)
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            if (emergencyResponse?.status == true) {
                                /*if (uploadFile != null) {
                                    if (uploadFile?.exists() == true) {
                                        uploadFile?.delete()
                                    }
                                }*/
                                mActivity.showMessage(emergencyResponse.result)
                                mActivity.show911CallPopUp()
                            } else {
//                                mActivity.showMessage(emergencyResponse.result)
                                mActivity.showMessage(mActivity.resources.getString(R.string.oops_error_message))
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        mActivity.showMessage(mActivity.resources.getString(R.string.oops_error_message))
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }
            })
        } else {
            Utils.showNoInternetMessage(mActivity)
        }
    }

    private fun setCameraRecordingPermission() {

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
                .permissions(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO
                )

                .onAccepted { permissions ->
                    mActivity.hideKeyboard()
                    if (permissions.size == 3) {
                        Utils.muteRecognizer(mActivity, true)
                        openAnncaVideoCapture()
                    }
                }
                .onDenied {
                    setCameraRecordingPermission()
                }
                .onForeverDenied {
                    mActivity.showMessage(mActivity.resources.getString(R.string.permission_app_set))
                }
                .ask()
        } else {
            KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
                .permissions(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO
                )

                .onAccepted { permissions ->
                    mActivity.hideKeyboard()
                    if (permissions.size == 4) {
                        Utils.muteRecognizer(mActivity, true)
                        openAnncaVideoCapture()
                    }
                }
                .onDenied {
                    setCameraRecordingPermission()
                }
                .onForeverDenied {
                    mActivity.showMessage(mActivity.resources.getString(R.string.permission_app_set))
                }
                .ask()
        }
    }

    private fun openAnncaVideoCapture() {
        isCameraOpen = true
        val root = Utils.getStorageRootPath(mActivity)
        if (!root.exists()) {
            root.mkdir()
        }
        val subFolder = File(root, "/Emergency_Video/")
        if (!subFolder.exists()) {
            subFolder.mkdir()
        }
        val timeStamp =
            SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(Date())
        val mediaFile = File(
            subFolder.path + File.separator +
                    "VID_" + timeStamp + ".mp4"
        )
        val fileSize: Long = 3 * 1024 * 1024
        val videoLimited: AnncaConfiguration.Builder =
            AnncaConfiguration.Builder(mActivity, CAPTURE_MEDIA)
        videoLimited.setMediaAction(AnncaConfiguration.MEDIA_ACTION_VIDEO)
        videoLimited.setMediaQuality(AnncaConfiguration.MEDIA_QUALITY_AUTO)
        videoLimited.setAutoRecord(true)
        videoLimited.setMinimumVideoDuration(15000)
        videoLimited.setVideoFileSize(fileSize)
        videoLimited.setSendMail(sendMail)
        videoLimited.setOutPutFilePath(mediaFile.absolutePath)
        Annca(videoLimited.build()).launchCamera()
    }
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            setHeader()
        } else {
            mActivity.stopLiveStreamTimer()
        }
    }

    inner class CustomLiveStreamTimerTask : java.util.TimerTask() {
        private val handler = Handler(Looper.getMainLooper())
        override fun run() {
            Thread {
                handler.post {
                    try {
                        callLiveStreamUserListApi()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }.start()
        }
    }

    private fun callLiveStreamUserListApi() {
        Utils.liveStreamUserListing(mActivity, object : CommonApiListener {
            override fun liveStreamListResponse(
                status: Boolean,
                userList: ArrayList<LiveStreamResult>,
                message: String,
                responseMessage: String
            ) {
                try {
                    liveStreamList = userList
                    val appDatabase = OldMe911Database.getDatabase(mActivity)
                    val loginObject = appDatabase.loginDao().getAll()
                    var liveStreamId: Int = 0
                    for (i in userList.indices) {
                        val user = userList[i]
                        val memberId = user.memberId ?: 0
                        val adminId = user.adminId ?: 0
                        val userId = if (memberId > 0) memberId else adminId
                        if (loginObject.memberID == userId) {
                            liveStreamId = user.id ?: 0
                        }
                    }
                    if (liveStreamId > 0) {
                        callDeleteLiveStreamApi(liveStreamId)
                    } else {
                        setLiveStreamData()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }, false)
    }

    fun checkUserLiveOrNot(userId: Int): LiveStreamResult {
        var isLive = LiveStreamResult()
        try {
            for (i in dummyLiveStreamList.indices) {
                val user = dummyLiveStreamList[i]
                val memberId = user.memberId ?: 0
                val adminId = user.adminId ?: 0
                val liveUserId = if (memberId > 0) memberId else adminId
                if (userId == liveUserId) {
                    isLive = user
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return isLive
    }

    private fun callDeleteLiveStreamApi(liveStreamId: Int) {
        Utils.deleteLiveStreamApi(mActivity, liveStreamId, object : CommonApiListener {
            override fun commonResponse(
                status: Boolean,
                message: String,
                responseMessage: String,
                result: String
            ) {
                setLiveStreamData()
            }
        }, false)
    }

    private fun callMemberApi(isLoading: Boolean) {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            mActivity.isSpeedAvailable()
            Utils.familyMonitoringUserList(mActivity, object : CommonApiListener{
                override fun familyUserList(
                    status: Boolean,
                    userList: ArrayList<FamilyMonitorResult>,
                    message: String
                ) {
                    if (status) {
                        memberList = ArrayList()
                        appDatabase.memberDao().dropTable()
                        appDatabase.memberDao().addAllMember(userList)
                        val loginupdate: LoginObject = appDatabase.loginDao().getAll()
                        val memberCount =
                            if (appDatabase.memberDao().countMember() != null) appDatabase.memberDao().countMember() else 0
                        if (loginupdate != null) {
                            if (memberCount > 0) {
                                loginupdate.totalMembers = memberCount - 1
                            } else {
                                loginupdate.totalMembers = 0
                            }
                            appDatabase.loginDao().updateLogin(loginupdate)
                        }
                        setData()
                    } else {
                        mActivity.showMessage(message)
                    }
                    listingViewVisibility()
                }

                override fun onFailureResult() {
                    if (memberList.size > 0) {
                        setAdapter()
                    }
                    listingViewVisibility()
                    mActivity.showMessage(mActivity.resources.getString(R.string.error_message))
                }

            }, isLoading)
        } else {
            setData()
        }
    }

    private fun setData() {
        memberList = ArrayList()
        for (i in appDatabase.memberDao().getAllMember().indices) {
            if (appDatabase.memberDao().getAllMember()[i].iD != appDatabase.loginDao().getAll().memberID) {
                memberList.add(appDatabase.memberDao().getAllMember()[i])
            }
        }
        setAdapter()
        try {
            listingViewVisibility()
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun listingViewVisibility() {
        if (rvDashImageList != null) {
            rvDashImageList.visibility = View.VISIBLE
        }
        if (rlDashAddUser != null) {
            rlDashAddUser.visibility = View.VISIBLE
        }
    }

    var hasLiveStreamUser = true

    private fun setLiveStreamData() {
        val loginObject = appDatabase.loginDao().getAll()
        val isAdmin = loginObject.isAdmin
        dummyLiveStreamList = ArrayList()
        for (i in liveStreamList.indices) {
            val user = liveStreamList[i]
            val memberId = user.memberId ?: 0
            val adminId = user.adminId ?: 0
            val userId = if (memberId > 0) memberId else adminId
            if (userId != loginObject.memberID) {
                dummyLiveStreamList.add(user)
            }
        }
        if (!isAdmin) {
            setAdapter(true)
            try {
                listingViewVisibility()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            var count = 0
            for (i in memberList.indices) {
                val isLive = checkUserLiveOrNot(memberList[i].iD)
                if ((isLive.id ?: 0) > 0) {
                    hasLiveStreamUser = true
                    dashBoardMemberListAdapter.notifyItemChanged(i)
                } else {
                    count +=1
                }
            }
            if (count == memberList.size) {
                if (hasLiveStreamUser) {
                    for (i in 0 until count) {
                        dashBoardMemberListAdapter.notifyItemChanged(i)
                    }
                    hasLiveStreamUser = false
                }
            }
        }
    }

    fun setAdapter(isLiveStream: Boolean = false) {
        try {
            if (rvDashImageList != null) {
                if (isLiveStream) {
                    val dashBoardLiveStreamAdapter = DashBoardLiveStreamAdapter(mActivity, dummyLiveStreamList)
                    dashBoardLiveStreamAdapter.notifyDataSetChanged()
                    rvDashImageList.adapter = dashBoardLiveStreamAdapter
                } else {
                    dashBoardMemberListAdapter = DashBoardMemberListAdapter(mActivity, memberList)
                    dashBoardMemberListAdapter.notifyDataSetChanged()
                    rvDashImageList.adapter = dashBoardMemberListAdapter
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {

            OPEN_CAMRA -> {
                if (data != null) {
                    val originalFileName = getFileName(data.data!!)
                    println("!@@@@videoUrlData = $originalFileName")
                    try {
                        if (sendMail) {
                            val root = Utils.getStorageRootPath(mActivity)
                            if (!root.exists()) {
                                root.mkdir()
                            }
                            val videoFile = File(root, File(originalFileName).name)
                            if (!videoFile.exists()) {
                                val videoCompress = VideoCompress.compressVideoLow(originalFileName,
                                    videoFile.path, 0, 0,
                                    object : VideoCompress.CompressListener {
                                        override fun onStart() {
                                            Comman_Methods.isProgressShow(mActivity)
                                        }

                                        override fun onSuccess() {
                                            Comman_Methods.isProgressHide()
                                            uploadFile = videoFile
                                            callSendMailEmergency()
                                        }

                                        override fun onFail() {
                                            Comman_Methods.isProgressHide()
                                        }

                                        override fun onProgress(percent: Float) {
                                        }
                                    })
                            } else {
                                uploadFile = videoFile
                                callSendMailEmergency()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            CAPTURE_MEDIA -> {
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        println("!@@@@@Media captured. = ${data.extras?.getString(AnncaConfiguration.Arguments.FILE_PATH)}")
                        val videoPath = if (data.extras?.getString(AnncaConfiguration.Arguments.FILE_PATH) != null) data.extras?.getString(AnncaConfiguration.Arguments.FILE_PATH) else ""
                        if (videoPath != null){
                            if (File(videoPath) != null) {
                                if (File(videoPath).exists()) {
                                    if (sendMail) {
                                        uploadFile = File(videoPath)
                                        callSendMailEmergency()
                                    } else {
                                        mActivity.showMessage(mActivity.resources.getString(R.string.str_video_store))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = mActivity.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
        }
        return result!!
    }

    inner class DashBoardMemberListAdapter(val context: Context, val memberList: ArrayList<FamilyMonitorResult>):
        RecyclerView.Adapter<DashBoardMemberListAdapter.DashBoardMemberListHolder>(){

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): DashBoardMemberListHolder {
            return DashBoardMemberListHolder(LayoutInflater.from(mActivity).inflate(R.layout.raw_new_memberlist, p0, false))
        }

        override fun getItemCount(): Int {
            return memberList.size
        }

        override fun onBindViewHolder(p0: DashBoardMemberListHolder, p1: Int) {

            /**
             * IsAdditionalMember =true && IsSubscription ==true ->View[Icon] is visible
             * IsAdditionalMember =true && IsSubscription ==false ->Only View[Icon] is visible
             * IsAdditionalMember =false && IsSubscription ==false ->Only View[Icon] is not - visible
             */

            /*if (memberList[p1].iD != appDatabase.loginDao().getAll().memberID) {
                val builder: GenericDraweeHierarchyBuilder = GenericDraweeHierarchyBuilder(mActivity.resources)
                val roundingParams : RoundingParams = RoundingParams.asCircle().setBorder(mActivity.resources.getColor(R.color.Date_bg), 1.0f)
                val hierarchy: GenericDraweeHierarchy = builder
                        .setRoundingParams(roundingParams)
                    .setPlaceholderImage(mActivity.resources.getDrawable(R.drawable.upload_profile))
                    .build()

                p0.sdvMemberImage.hierarchy = hierarchy
            }else{
                val builder: GenericDraweeHierarchyBuilder = GenericDraweeHierarchyBuilder(mActivity.resources)
                val roundingParams : RoundingParams = RoundingParams.asCircle().setBorder(mActivity.resources.getColor(R.color.special_green), 2.0f)
                val hierarchy: GenericDraweeHierarchy = builder
                    .setRoundingParams(roundingParams)
                    .setPlaceholderImage(mActivity.resources.getDrawable(R.drawable.upload_profile_green))
                    .build()

                p0.sdvMemberImage.hierarchy = hierarchy
            }*/
            p0.ivLiveMemberInitialize.visibility = View.GONE
            p0.tvLiveMemberInitialize.visibility = View.GONE
            p0.sdvMemberImage.loadFrescoImage(mActivity, memberList[p1].image ?: "", 1)
            p0.tvMemberListName.text = (memberList[p1].firstName ?: "") + " " + (memberList[p1].lastName ?: "")


            val isLive = checkUserLiveOrNot(memberList[p1].iD)

            if ((isLive.id ?: 0) > 0) {
                p0.tvLiveMemberInitialize.visibility = View.VISIBLE
                val roundValue: Float = Comman_Methods.convertDpToPixel(15f, mActivity)
                val builder: GenericDraweeHierarchyBuilder = GenericDraweeHierarchyBuilder(mActivity.resources)
                val roundingParams : RoundingParams = RoundingParams.fromCornersRadius(roundValue).setBorder(
                    ContextCompat.getColor(mActivity, R.color.colorPrimary), 1.0f)
                val hierarchy: GenericDraweeHierarchy = builder
                    .setRoundingParams(roundingParams)
                    .build()
                p0.sdvMemberImage.hierarchy = hierarchy
            } else {
                val roundValue: Float = Comman_Methods.convertDpToPixel(15f, mActivity)
                val builder: GenericDraweeHierarchyBuilder = GenericDraweeHierarchyBuilder(mActivity.resources)
                val roundingParams : RoundingParams = RoundingParams.fromCornersRadius(roundValue).setBorder(
                    ContextCompat.getColor(mActivity, R.color.caldroid_white), 1.0f)
                val hierarchy: GenericDraweeHierarchy = builder
                    .setRoundingParams(roundingParams)
                    .build()
                p0.sdvMemberImage.hierarchy = hierarchy
            }

            p0.rvMemberDetail.setOnClickListener {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
                if (!mActivity.purchaseSubscription()) {
                    val isLiveData = checkUserLiveOrNot(memberList[p1].iD)
                    val liveStreamId = isLiveData.id ?: 0
                    if (liveStreamId > 0) {
                        mActivity.openLiveStream(
                            false,
                            isLiveData.channelName ?: "",
                            liveStreamId.toString(),
                            isLiveData
                        )
                    } else {
                        if (memberList[p1].IsCancelled == true) {
                            Comman_Methods.isCustomPopUpShow(mActivity,
                                title = mActivity.resources.getString(R.string.str_upgrade),
                                message = mActivity.resources.getString(R.string.cancel_subscribe_user_message),
                                positiveButtonListener = object : PositiveButtonListener {
                                    override fun okClickListener() {
                                        mActivity.addFragment(
                                            UpdateSubFragment.newInstance(
                                                isFromMember = true, isEditUser = true,
                                                familyMonitorResult = memberList[p1],
                                                isCancelledSubscription = true
                                            ), true, true, AnimationType.fadeInfadeOut
                                        )
                                    }
                                })
                        } else {

                            if (memberList[p1].iD != appDatabase.loginDao().getAll().memberID) {
                                mActivity.addFragment(
                                    AddMemberFragment.newInstance(
                                        isUpdate = true,
                                        familyMonitorResult = memberList[p1],
                                        isFromList = true
                                    ), true, true, animationType = AnimationType.fadeInfadeOut
                                )
                            }

                        }
                    }
                }
            }
        }

        inner class DashBoardMemberListHolder(view: View): RecyclerView.ViewHolder(view){
            var rvMemberDetail: RelativeLayout = view.rvMemberDetail
            var sdvMemberImage: SimpleDraweeView = view.sdvMemberImage
            var tvMemberListName: TextView = view.tvMemberListName
            var ivLiveMemberInitialize: ImageView = view.ivLiveMemberInitialize
            var tvLiveMemberInitialize: TextView = view.tvLiveMemberInitialize
        }
    }

    inner class DashBoardLiveStreamAdapter(val context: Context, val liveStreamList: ArrayList<LiveStreamResult>):
        RecyclerView.Adapter<DashBoardLiveStreamAdapter.DashBoardLiveStreamHolder>(){

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): DashBoardLiveStreamHolder {
            return DashBoardLiveStreamHolder(LayoutInflater.from(mActivity).inflate(R.layout.raw_new_memberlist, p0, false))
        }

        override fun getItemCount(): Int {
            return liveStreamList.size
        }

        override fun onBindViewHolder(p0: DashBoardLiveStreamHolder, p1: Int) {
            val user = liveStreamList[p1]
            val roundValue: Float = Comman_Methods.convertDpToPixel(15f, mActivity)
            val builder: GenericDraweeHierarchyBuilder = GenericDraweeHierarchyBuilder(mActivity.resources)
            val roundingParams : RoundingParams = RoundingParams.fromCornersRadius(roundValue).setBorder(
                ContextCompat.getColor(mActivity, R.color.colorPrimary), 1.0f)
            val hierarchy: GenericDraweeHierarchy = builder
                .setRoundingParams(roundingParams)
                .build()

            val memberId = user.memberId ?: 0
            val adminId = user.adminId ?: 0
            val userId = if (memberId > 0) memberId else adminId
            val profileUrl = if (memberId > 0) (user.memberProfileUrl ?: "") else (user.adminProfileUrl ?: "")
            val userName = if (memberId > 0) (user.memberName ?: "") else (user.adminName ?: "")

            p0.sdvMemberImage.hierarchy = hierarchy
            p0.sdvMemberImage.loadFrescoImage(mActivity, profileUrl, 1)
            p0.tvMemberListName.text = userName
            p0.tvLiveMemberInitialize.visibility = View.VISIBLE

            p0.rvMemberDetail.setOnClickListener {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
                mActivity.openLiveStream(true, user.channelName ?: "", (user.id ?: 0).toString(), user)
            }
        }

        inner class DashBoardLiveStreamHolder(view: View): RecyclerView.ViewHolder(view){
            var rvMemberDetail: RelativeLayout = view.rvMemberDetail
            var sdvMemberImage: SimpleDraweeView = view.sdvMemberImage
            var tvMemberListName: TextView = view.tvMemberListName
            var tvLiveMemberInitialize: TextView = view.tvLiveMemberInitialize
        }
    }
}