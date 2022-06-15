package com.keepSafe911.fragments.homefragment.find

import AnimationType
import ValidationUtil.Companion.isPasswordMatch
import ValidationUtil.Companion.isPasswordValidate

import ValidationUtil.Companion.isRequiredField
import ValidationUtil.Companion.isValidEmail
import ValidationUtil.Companion.isValidPasswordLength
import addFragment
import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.*
import android.content.pm.ActivityInfo
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.facebook.drawee.generic.GenericDraweeHierarchy
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
import com.facebook.drawee.generic.RoundingParams
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.JsonObject
import com.keepSafe911.BuildConfig
import com.kotlinpermissions.KotlinPermissions
import com.keepSafe911.HomeActivity
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.fragments.homefragment.DashBoardFragment
import com.keepSafe911.fragments.payment_selection.UpdateSubFragment
import com.keepSafe911.gps.GpsTracker
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.listner.PositiveButtonListener
import com.keepSafe911.model.FamilyMonitorResult
import com.keepSafe911.model.PhoneCountryCode
import com.keepSafe911.model.SubscriptionBean
import com.keepSafe911.model.response.*
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import com.keepSafe911.utils.Comman_Methods.Companion.getdeviceVersion
import com.keepSafe911.webservices.WebApiClient
import com.yanzhenjie.album.Album
import hideKeyboard
import kotlinx.android.synthetic.main.background_layout.*
import kotlinx.android.synthetic.main.fragment_sign_up.*
import kotlinx.android.synthetic.main.raw_country_search.view.*
import kotlinx.android.synthetic.main.raw_new_memberlist.view.*
import kotlinx.android.synthetic.main.search_country.view.*
import kotlinx.android.synthetic.main.toolbar_header.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.collections.ArrayList


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val ARG_PARAM3 = "param3"
private const val ARG_PARAM4 = "param4"
private const val ARG_PARAM5 = "param5"
private const val ARG_PARAM6 = "param6"

class AddMemberFragment : HomeBaseFragment(), View.OnClickListener, Listener {

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    var bettery_level = 0
    var memberList: ArrayList<FamilyMonitorResult> = ArrayList()
    lateinit var addMemberListAdapter: AddMemberListAdapter
    private var location_per = false
    lateinit var easyWayLocation: EasyWayLocation
    private var isFromPayment: Boolean = false
    private var isUpdate: Boolean = false
    lateinit var dialog: Dialog
    private var isFromList: Boolean = false
    private var familyMonitorResult = FamilyMonitorResult()
    lateinit var part: MultipartBody.Part
    lateinit var appDatabase: OldMe911Database
    var imageFile: File? = null
    private var passwordShowed: Boolean = false
    private var phoneCountryCodes: ArrayList<PhoneCountryCode> = ArrayList()
    private var countryCode: String = ""
    private var gpstracker: GpsTracker? = null
    private var subscriptionBean: SubscriptionBean = SubscriptionBean()
    private var updateProfile: String = ""
    private var isRemoveProfile: Boolean = false
    private lateinit var removeProfileDialog: BottomSheetDialog
    var selectedMemberId: Int = 0
    var isUsernameValid = true
    var isPasswordValid = false
    var isFrom: Boolean = false

    private val mBatInfoReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            bettery_level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isFromPayment = it.getBoolean(ARG_PARAM1, false)
            isUpdate = it.getBoolean(ARG_PARAM2, false)
            familyMonitorResult = it.getParcelable(ARG_PARAM3) ?: FamilyMonitorResult()
            subscriptionBean = it.getParcelable(ARG_PARAM4) ?: SubscriptionBean()
            isFromList = it.getBoolean(ARG_PARAM5, false)
            isFrom = it.getBoolean(ARG_PARAM6, false)
        }
        easyWayLocation = EasyWayLocation(mActivity)
        requireActivity().registerReceiver(
            this.mBatInfoReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        //activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
    }

    companion object {
        fun newInstance(
            isUpdate: Boolean = false,
            familyMonitorResult: FamilyMonitorResult = FamilyMonitorResult(),
            subscriptionBean: SubscriptionBean = SubscriptionBean(),
            isFromList: Boolean = false,
            isFromPayment: Boolean = false,
            isFrom: Boolean = false
        ): AddMemberFragment {
            val args = Bundle()
            args.putBoolean(ARG_PARAM1, isFromPayment)
            args.putBoolean(ARG_PARAM2, isUpdate)
            args.putParcelable(ARG_PARAM3, familyMonitorResult)
            args.putParcelable(ARG_PARAM4, subscriptionBean)
            args.putBoolean(ARG_PARAM5, isFromList)
            args.putBoolean(ARG_PARAM6, isFrom)
            val fragment = AddMemberFragment()
            fragment.arguments = args
            return fragment
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
        return inflater.inflate(R.layout.fragment_sign_up, container, false)
    }

    @SuppressLint("MissingPermission", "HardwareIds")
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        storeCountryCode()
        gpstracker = GpsTracker(mActivity)
        appDatabase = OldMe911Database.getDatabase(mActivity)
        btn_sign_in.setOnClickListener(this)
        img_nphotoUpload.setOnClickListener(this)
        imgPhotoUpload.setOnClickListener(this)
        ivShowPassword.setOnClickListener(this)
        tvCountrySelected.setOnClickListener(this)
        tvPingFrequency.setOnClickListener(this)
        tvSignUpTitle.visibility = View.GONE
        img_nphotoUpload.visibility = View.GONE
        imgPhotoUpload.visibility = View.VISIBLE

        val params = sdv_profile_image.layoutParams as RelativeLayout.LayoutParams
        params.removeRule(RelativeLayout.ALIGN_PARENT_END)
        sdv_profile_image.layoutParams = params

        /*tvFacebookOr.visibility = View.GONE
        btnFacebookSignUp.visibility = View.GONE*/
//        rlAddUserAddUser.visibility = View.VISIBLE
//        rlAddUserAddUser.setOnClickListener(this)
        easyWayLocation.setListener(this)

        if (appDatabase.loginDao().getAll().isAdmin) {
            rvAddUserImageList.layoutManager = LinearLayoutManager(
                mActivity,
                RecyclerView.HORIZONTAL,
                false
            )
            if (appDatabase.memberDao().getAllMember().isNotEmpty()) {
                rvAddUserImageList.visibility = View.VISIBLE
                setData()
            } else {
                rvAddUserImageList.visibility = View.GONE
                callMemberApi()
            }
        }else{
            rvAddUserImageList.visibility = View.GONE
//            rlAddUserAddUser.visibility = View.GONE
        }

        setHeader()

        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            etSignUserName.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etSignFirstName.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etSignLastName.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etSignPhone.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etSignEmail.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etSignPassword.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etSignFrequency.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        } else {
            etSignUserName.imeOptions = EditorInfo.IME_ACTION_NEXT
            etSignFirstName.imeOptions = EditorInfo.IME_ACTION_NEXT
            etSignLastName.imeOptions = EditorInfo.IME_ACTION_NEXT
            etSignPhone.imeOptions = EditorInfo.IME_ACTION_NEXT
            etSignEmail.imeOptions = EditorInfo.IME_ACTION_NEXT
            etSignPassword.imeOptions = EditorInfo.IME_ACTION_DONE
            etSignFrequency.imeOptions = EditorInfo.IME_ACTION_DONE
        }
        etSignUserName.filters = arrayOf(Utils.filterUserName)
        etSignPassword.filters = arrayOf(Utils.filterPassword)
        etSignCPassword.filters = arrayOf(Utils.filterPassword)

        if (isFromPayment) {
            btn_sign_in.text = mActivity.resources.getString(R.string.next)
        } else {
            btn_sign_in.text = mActivity.resources.getString(R.string.submit)
        }
        flSignReferral.visibility = View.GONE
        tvReferralError.visibility = View.GONE
        flSignPromotion.visibility = View.GONE
        viewDash2.visibility = View.VISIBLE
        etSignReferralCode.visibility = View.GONE
        rl_sms.visibility = View.GONE
        tvSmsAlert.visibility = View.GONE
        switchSignMessage.visibility = View.GONE
        flSignPingFreq.visibility =View.GONE
        if (isUpdate) {
            btnVerifyUsername.visibility = View.GONE
            selectedMemberId = familyMonitorResult.iD
            when {
                mActivity.resources.getBoolean(R.bool.isTablet) -> etSignEmail.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
                else -> etSignEmail.imeOptions = EditorInfo.IME_ACTION_DONE
            }
            tvInviteNote.visibility = View.GONE
            val profileImage: String = familyMonitorResult.image ?: ""
            sdv_profile_image.loadFrescoImage(mActivity, profileImage, 1)
            val image_name = profileImage.split("Family/")
            updateProfile = if (image_name[1]!="") image_name[1] else ""
//            tvHeader.text = mActivity.resources.getString(R.string.update_member)
            etSignUserName.setText(familyMonitorResult.userName ?: "")
            etSignPassword.setText(familyMonitorResult.password ?: "")
            etSignEmail.setText(familyMonitorResult.email ?: "")
            etSignFirstName.setText(familyMonitorResult.firstName ?: "")
            etSignLastName.setText(familyMonitorResult.lastName ?: "")
            val frequencyData = familyMonitorResult.frequency ?: 60
            val userMobile = familyMonitorResult.mobile ?: ""
            when {
                frequencyData < 0 -> {
                    when {
                        frequencyData < 60 -> etSignFrequency.setText("" + frequencyData * 60)
                        frequencyData > 14400 -> etSignFrequency.setText("14400")
                        else -> etSignFrequency.setText("" + frequencyData)
                    }
                } else -> etSignFrequency.setText("60")
            }
            if (userMobile.length > 10) {
                etSignPhone.setText(userMobile.takeLast(10))
                countryCode = userMobile.dropLast(10)
            } else {
                countryCode = phoneCountryCodes[UNITED__CODE_POSITION].countryCode
                etSignPhone.setText(userMobile)
            }
            etSignPassword.isFocusableInTouchMode = false
            etSignPassword.isFocusable = false
            flSignConfirmPassword.visibility = View.GONE
            etSignCPassword.visibility = View.GONE
            tvAddMemberTitle.visibility = View.GONE
            etSignUserName.isFocusableInTouchMode = false
            etSignUserName.isFocusable = false

            if (countryCode != "") {
                for (i in 0 until phoneCountryCodes.size) {
                    if (countryCode == phoneCountryCodes[i].countryCode) {
                        when (phoneCountryCodes[i].code) {
                            "CA" -> tvCountrySelected.text = "+1"
                            else -> tvCountrySelected.text = phoneCountryCodes[i].countryCode
                        }
                        tvCountrySelected.setCompoundDrawablesWithIntrinsicBounds(
                            phoneCountryCodes[i].flag,
                            0,
                            0,
                            0
                        )
                        return
                    } else {
                        tvCountrySelected.text = phoneCountryCodes[UNITED__CODE_POSITION].countryCode
                        tvCountrySelected.setCompoundDrawablesWithIntrinsicBounds(
                            phoneCountryCodes[UNITED__CODE_POSITION].flag,
                            0,
                            0,
                            0
                        )
                    }
                }
            } else {
                countryCode = phoneCountryCodes[UNITED__CODE_POSITION].countryCode
                tvCountrySelected.text = phoneCountryCodes[UNITED__CODE_POSITION].countryCode
                tvCountrySelected.setCompoundDrawablesWithIntrinsicBounds(
                    phoneCountryCodes[UNITED__CODE_POSITION].flag,
                    0,
                    0,
                    0
                )
            }
        } else {
            if (mActivity.resources.getBoolean(R.bool.isTablet)) {
                etSignPassword.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
                etSignCPassword.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_DONE
            } else {
                etSignPassword.imeOptions = EditorInfo.IME_ACTION_NEXT
                etSignCPassword.imeOptions = EditorInfo.IME_ACTION_DONE
            }
            tvInviteNote.visibility = View.VISIBLE
            countryCode = phoneCountryCodes[UNITED__CODE_POSITION].countryCode
            tvCountrySelected.text = phoneCountryCodes[UNITED__CODE_POSITION].countryCode
            tvCountrySelected.setCompoundDrawablesWithIntrinsicBounds(
                phoneCountryCodes[UNITED__CODE_POSITION].flag,
                0,
                0,
                0
            )
//            tvHeader.text = mActivity.resources.getString(R.string.add_member)
            flSignConfirmPassword.visibility = View.VISIBLE
            etSignCPassword.visibility = View.VISIBLE
            tvAddMemberTitle.visibility = View.VISIBLE
            etSignPassword.isFocusableInTouchMode = true
            etSignPassword.isFocusable = true
            etSignUserName.isFocusableInTouchMode = true
            etSignUserName.isFocusable = true
            etSignFrequency.setText("60")
            selectedMemberId = 0
        }

        etSignUserName.addTextChangedListener(usernameTextChangeListener)
        btnVerifyUsername.setOnClickListener{
            checkUserNameUniqueApi(etSignUserName.text.toString().trim(), 2)
        }
        etSignPassword.addTextChangedListener(passWordTextChangeListener)

    }

    private val usernameTextChangeListener = object: TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(username: CharSequence?, p1: Int, p2: Int, p3: Int) {
            if (username.toString().isNotEmpty()){
                btnVerifyUsername.visibility = View.VISIBLE
                isUsernameValid = false
            }else{
                btnVerifyUsername.visibility = View.GONE
            }
            imgVerified.visibility = View.GONE
            tvUsernameError.visibility  = View.GONE

        }
        override fun afterTextChanged(p0: Editable?) {}
    }
    private val passWordTextChangeListener = object: TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (s.toString().isNotEmpty()){
                tvPasswordError.visibility = View.VISIBLE
                if (!isPasswordValidate(s.toString().trim())){
                    tvPasswordError.visibility = View.VISIBLE
                    isPasswordValid = false
                }else{
                    isPasswordValid = true
                    tvPasswordError.visibility = View.GONE
                }

                /*  if (!isPasswordValidate(s.toString().trim()) ) {
                          tvPasswordError.visibility = View.VISIBLE
                          isPasswordValid = false
                      }else if(!isValidPasswordLength(s.toString().trim())){
                          tvPasswordError.visibility = View.VISIBLE
                          isPasswordValid = false
                      }else{
                          isPasswordValid = true
                          tvPasswordError.visibility = View.GONE
                  }*/
            }else{
                tvPasswordError.visibility = View.GONE
            }

        }
        override fun afterTextChanged(s: Editable?) {}

    }

    private fun checkUserNameUniqueApi(username : String, type: Int) {
        mActivity.isSpeedAvailable()
        val jsonObject = JsonObject()
        jsonObject.addProperty("Name",username)
        jsonObject.addProperty("Type", type)

        Utils.verifyUserNameData(mActivity, jsonObject, object : CommonApiListener {
            override fun commonResultResponse(status: Boolean, message: String, responseMessage: String, result: Any?) {
                if (!status){
                    mActivity.showMessage(message)
                    if (type == 2){
                        imgVerified.visibility = View.GONE
                        btnVerifyUsername.visibility = View.GONE
                        tvUsernameError.visibility  = View.VISIBLE
                        isUsernameValid  = false
                        tvUsernameError.text = message
                    }
                }else{
                    if (type == 2){
                        isUsernameValid = true
                        btnVerifyUsername.visibility = View.GONE
                        imgVerified.visibility = View.VISIBLE
                        tvUsernameError.visibility  = View.GONE
                    }
                }
            }
        })
    }

    private fun storeCountryCode() {
        phoneCountryCodes = java.util.ArrayList()
        val stringBuffer = StringBuffer()
        var bufferedReader: BufferedReader? = null
        try {
            bufferedReader = BufferedReader(InputStreamReader(mActivity.assets.open("Countrylistwithdialcode.json")))
            var temp: String? = ""
            while (run {
                    temp = bufferedReader.readLine()
                    temp
                } != null)
                stringBuffer.append(temp)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                bufferedReader?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
        val myjsonString = stringBuffer.toString()
        try {
            val jsonObjMain = JSONObject(myjsonString)
            val jsonArray = jsonObjMain.getJSONArray("CountryDialcode")
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val countryName = jsonObject.getString("name")
                val dialCode = jsonObject.getString("dial_code")
                val countryCode = jsonObject.getString("code")
                phoneCountryCodes.add(
                    PhoneCountryCode(
                        countryName,
                        dialCode,
                        countryCode,
                        Utils.countryFlagWithCode(mActivity)[i].flag,
                        false
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            setHeader()
            /*if (isFromList) {
                callApi()
            }*/
        }
    }

    private fun setHeader() {
        /*tvHeader.setPadding(0, 0, 50, 0)
        iv_back.visibility = View.VISIBLE*/
        mActivity.enableDrawer()
        mActivity.checkNavigationItem(3)
        tvHeader.text = ""
        ivMenuLogo.visibility = View.VISIBLE
        iv_menu.visibility = View.VISIBLE
        iv_menu.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.openDrawer()
        }
        if (isUpdate){
            isPasswordValid = true
            ivDeleteUser.visibility = View.VISIBLE
        }else{
            isPasswordValid = false
            ivDeleteUser.visibility = View.GONE
        }
        ivDeleteUser.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
//            logoutConfirmDialog(familyMonitorResult)
            if ((familyMonitorResult.IsAdditionalMember == true) && (familyMonitorResult.IsSubscription == true)) {
                if (familyMonitorResult.IsCancelled == true){
                    logoutConfirmDialog(
                        familyMonitorResult,
                        mActivity.resources.getString(R.string.delete_conf),
                        mActivity.resources.getString(
                            R.string.wish_delete
                        )
                    )
                }else{
                    logoutConfirmDialog(
                        familyMonitorResult,
                        mActivity.resources.getString(R.string.str_cancel_user),
                        mActivity.resources.getString(
                            R.string.wish_cancel_subscription
                        )
                    )
                }
            }else{
                logoutConfirmDialog(
                    familyMonitorResult,
                    mActivity.resources.getString(R.string.delete_conf),
                    mActivity.resources.getString(
                        R.string.wish_delete
                    )
                )
            }
        }
        mActivity.checkUserActive()
    }

    private fun logoutConfirmDialog(
        familyMonitorResult: FamilyMonitorResult,
        title: String,
        message: String
    ) {
        Comman_Methods.isCustomPopUpShow(mActivity, title, message,
            positiveButtonListener = object : PositiveButtonListener {
                override fun okClickListener() {
                    if ((familyMonitorResult.IsAdditionalMember == true) && (familyMonitorResult.IsSubscription == true)) {
                        mActivity.openClientCallDialog()
//                        callCancelSubscriptionApi(familyMonitorResult)
                    }else{
                        callDeleteApi(familyMonitorResult)
                    }
                }
            })
    }


    private fun callCancelSubscriptionApi(familyMonitorResult: FamilyMonitorResult) {
        mActivity.isSpeedAvailable()
        Utils.userSubscriptionCancel(mActivity, familyMonitorResult.iD, object : CommonApiListener {
            override fun commonResponse(
                status: Boolean,
                message: String,
                responseMessage: String,
                result: String
            ) {
                if (status) {
                    val memberData = familyMonitorResult
                    memberData.IsCancelled = true
                    memberData.isChildMissing = true
                    appDatabase.memberDao().updateMember(memberData)
//                            callDeleteApi(familyMonitorResult)
                }
                mActivity.onBackPressed()
                mActivity.showMessage(responseMessage)
            }
        })
    }

    private fun callDeleteApi(familyMonitorResult: FamilyMonitorResult) {
        mActivity.isSpeedAvailable()
        Utils.deleteUserApi(mActivity, familyMonitorResult.iD, object : CommonApiListener {
            override fun commonResponse(
                status: Boolean,
                message: String,
                responseMessage: String,
                result: String
            ) {
                if (status) {
                    memberList.remove(familyMonitorResult)

                    val appDatabase = OldMe911Database.getDatabase(mActivity)
                    appDatabase.memberDao().deleteMember(familyMonitorResult)
                    val loginUpdate: LoginObject = appDatabase.loginDao().getAll()
                    val memberCount = appDatabase.memberDao().countMember()
                    if (memberCount > 0) {
                        loginUpdate.totalMembers =
                            appDatabase.memberDao().countMember() - 1
                    } else {
                        loginUpdate.totalMembers = 0
                    }
                    appDatabase.loginDao().updateLogin(loginUpdate)
                    addMemberListAdapter.notifyDataSetChanged()
                    if (memberList.size > 0) {
                        rvAddUserImageList?.visibility = View.VISIBLE
                    } else {
                        rvAddUserImageList?.visibility = View.GONE
                    }
                    mActivity.onBackPressed()
                }
            }
        })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_sign_in -> {
                mActivity.hideKeyboard()
                if (checkForValidations()) {
                    Comman_Methods.avoidDoubleClicks(v)
                    if (isFromList) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            setLocationPermission()
                        } else {
                            /*if (isUpdate) {
                                callVerifyEmailAPI(familyMonitorResult.iD)
                            } else {
                                callVerifyEmailAPI(0)
                            }*/
                            gotoPaymentScreen()
                        }
                    }
                }
            }
            R.id.img_nphotoUpload, R.id.imgPhotoUpload -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                if (isUpdate) {
                    if (updateProfile != "") {
                        pictureOption()
                    } else {
                        setPermission()
                    }
                } else {
                    /*if (imageFile!=null){
                        pictureOption()
                    }else{
                        setPermission()
                    }*/
                    setPermission()
                }
            }
            R.id.ivShowPassword -> {
                if (etSignPassword.text.toString().isNotEmpty()) {
                    if (passwordShowed) {
                        etSignPassword.transformationMethod =
                            PasswordTransformationMethod.getInstance()
                        ivShowPassword.setImageResource(R.drawable.show_card_number)
                        passwordShowed = false
                    } else {
                        etSignPassword.transformationMethod =
                            HideReturnsTransformationMethod.getInstance()
                        ivShowPassword.setImageResource(R.drawable.gone_card_number)
                        passwordShowed = true
                    }
                    etSignPassword.setSelection(etSignPassword.text.toString().length)
                }
            }
            R.id.tvCountrySelected -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                storeCountryCode()
                openUploadCountryDialog(phoneCountryCodes)
            }
            R.id.tvPingFrequency -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
            }
        }
    }

    private fun pictureOption(){
        val view = LayoutInflater.from(mActivity)
            .inflate(
                R.layout.popup_picture_option_layout,
                mActivity.window.decorView.rootView as ViewGroup,
                false
            )
        if (this::removeProfileDialog.isInitialized){
            if (removeProfileDialog.isShowing){
                removeProfileDialog.dismiss()
            }
        }
        removeProfileDialog = BottomSheetDialog(mActivity)
        removeProfileDialog.setContentView(view)
        val mBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(view.parent as View)
        mBehavior.isHideable = false
        removeProfileDialog.setOnShowListener {
            mBehavior.peekHeight = view.height
        }
        val ivPictureCancel: ImageView? = removeProfileDialog.findViewById(R.id.ivPictureCancel)
        val fabAlbumOption: FloatingActionButton? = removeProfileDialog.findViewById(R.id.fabAlbumOption)
        val fabRemoveOption: FloatingActionButton? = removeProfileDialog.findViewById(R.id.fabRemoveOption)
        val tvAlbumOptionName: TextView? = removeProfileDialog.findViewById(R.id.tvAlbumOptionName)
        val tvRemoveOptionName: TextView? = removeProfileDialog.findViewById(R.id.tvRemoveOptionName)
        ivPictureCancel?.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            removeProfileDialog.dismiss()
        }
        fabRemoveOption?.setOnClickListener {
            mActivity.hideKeyboard()
            if (familyMonitorResult.iD > 0) {
                Comman_Methods.avoidDoubleClicks(it)
                callRemoveProfileImage(familyMonitorResult.iD)
            }
            /*imageFile = null
            updateProfile = ""
            isRemoveProfile = true
            sdv_profile_image.loadFrescoImageFromFile(mActivity, File(""), 1)*/
            removeProfileDialog.dismiss()
        }
        tvRemoveOptionName?.setOnClickListener {
            mActivity.hideKeyboard()
            if (familyMonitorResult.iD > 0) {
                Comman_Methods.avoidDoubleClicks(it)
                callRemoveProfileImage(familyMonitorResult.iD)
            }
            /*imageFile = null
            updateProfile = ""
            isRemoveProfile = true
            sdv_profile_image.loadFrescoImageFromFile(mActivity, File(""), 1)*/
            removeProfileDialog.dismiss()
        }
        fabAlbumOption?.setOnClickListener {
            mActivity.hideKeyboard()
            removeProfileDialog.dismiss()
            Comman_Methods.avoidDoubleClicks(fabAlbumOption)
            setPermission()
        }
        tvAlbumOptionName?.setOnClickListener {
            mActivity.hideKeyboard()
            removeProfileDialog.dismiss()
            Comman_Methods.avoidDoubleClicks(tvAlbumOptionName)
            setPermission()
        }
        removeProfileDialog.show()
    }

    private fun callRemoveProfileImage(memberId: Int) {
        mActivity.isSpeedAvailable()
        Utils.removeUserProfileImageApi(mActivity, memberId, object : CommonApiListener {
            override fun commonResponse(
                status: Boolean,
                message: String,
                responseMessage: String,
                result: String
            ) {
                if (status) {
//                    gotoPaymentScreen()
                    imageFile = null
                    updateProfile = ""
                    isRemoveProfile = true
                    sdv_profile_image.loadFrescoImageFromFile(mActivity, File(""), 1)
                    if (appDatabase.memberDao().getAllMember().isNotEmpty()) {
                        for (memberData in appDatabase.memberDao().getAllMember()) {
                            if (memberData.iD == familyMonitorResult.iD) {
                                memberData.image =
                                    "https://apiyfsn.azurewebsites.net/Uploads/Family/"
                                appDatabase.memberDao().updateMember(memberData)
                            }
                        }
                    }
                }
            }
        })
    }

    private fun callApi() {
        easyWayLocation = EasyWayLocation(mActivity)
        easyWayLocation.setListener(this)
        if (gpstracker?.CheckForLoCation() == true) {
            callSignUpAPI()
        } else {
            Utils.showLocationSettingsAlert(mActivity)
        }
    }

    private fun setLocationPermission() {
        KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
            .permissions(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

            .onAccepted { permissions ->
                mActivity.hideKeyboard()
                if (permissions.size == 2) {
                    location_per = true

                    /*if (isUpdate) {
                        callVerifyEmailAPI(familyMonitorResult.iD)
                    } else {
                        callVerifyEmailAPI(0)
                    }*/
                    gotoPaymentScreen()
                }

            }
            .onDenied {
                setLocationPermission()
            }
            .onForeverDenied {

                mActivity.showMessage(mActivity.resources.getString(R.string.permission_app_set))

                easyWayLocation = EasyWayLocation(mActivity)
                easyWayLocation.setListener(this)
                Utils.showSettingsAlert(mActivity)
            }
            .ask()
    }

    private fun gotoPaymentScreen() {
        val appDatabase = OldMe911Database.getDatabase(mActivity)
        val loginObject = appDatabase.loginDao().getAll()

        familyMonitorResult.firstName = etSignFirstName.text.toString().trim()
        familyMonitorResult.lastName = etSignLastName.text.toString().trim()
        familyMonitorResult.mobile = if (etSignPhone.text.toString().trim().isNotEmpty()) countryCode + etSignPhone.text.toString().trim() else ""
        familyMonitorResult.frequency = if (etSignFrequency.text.toString().isNotEmpty()) etSignFrequency.text.toString().toInt() else 60
        familyMonitorResult.ReferralName = loginObject.ReferralCode.toString().trim()
        familyMonitorResult.Promocode = ""
        familyMonitorResult.isChildMissing = loginObject.isChildMissing ?: false
        familyMonitorResult.image = imageFile?.absolutePath ?: ""
        if (isUpdate){
            if (familyMonitorResult.IsCancelled == true){

                Comman_Methods.isCustomPopUpShow(mActivity,
                    title = mActivity.resources.getString(R.string.str_upgrade),
                    message = mActivity.resources.getString(R.string.cancel_subscribe_user_message),
                    positiveButtonListener = object : PositiveButtonListener {
                        override fun okClickListener() {
                            mActivity.addFragment(
                                UpdateSubFragment.newInstance(
                                    isFromMember = true, isEditUser = true,
                                    familyMonitorResult = familyMonitorResult,
                                    isCancelledSubscription = true
                                ), true, true, AnimationType.fadeInfadeOut
                            )
                        }
                    })
            } else if ((familyMonitorResult.IsAdditionalMember == true) && (familyMonitorResult.IsSubscription == false)) {
                mActivity.addFragment(
                    UpdateSubFragment.newInstance(
                        isFromMember = true, isEditUser = true,
                        familyMonitorResult = familyMonitorResult,
                        isFrom = isFrom,
                        isCancelledSubscription = familyMonitorResult.IsCancelled ?: false
                    ), true, true, AnimationType.fadeInfadeOut
                )
            } else {
                callApi()
            }
        }else{
            familyMonitorResult.iD = 0
            familyMonitorResult.createdBy = appDatabase.loginDao().getAll().adminID ?: 0
            familyMonitorResult.createdOn = Comman_Methods.getcurrentDate()
            familyMonitorResult.email = etSignEmail.text.toString()
            familyMonitorResult.firstName = etSignFirstName.text.toString().trim()
            familyMonitorResult.lastName = etSignLastName.text.toString().trim()
            familyMonitorResult.mobile = if (etSignPhone.text.toString().trim().isNotEmpty()) countryCode + etSignPhone.text.toString().trim() else ""
            familyMonitorResult.password = etSignPassword.text.toString()
            familyMonitorResult.userName = etSignUserName.text.toString().trim()
            familyMonitorResult.isSms = switchSignMessage.isChecked
            familyMonitorResult.frequency = if (etSignFrequency.text.toString().isNotEmpty()) etSignFrequency.text.toString().toInt() else 60
            familyMonitorResult.Promocode = ""

            val mem_obj = appDatabase.memberDao().getFirstEntry()
            if (appDatabase.memberDao().countMember() <= (mem_obj.SubscripionUsers)) {
                callApi()
            } else {
                mActivity.addFragment(
                    UpdateSubFragment.newInstance(
                        isFromMember = true,
                        familyMonitorResult = familyMonitorResult,
                        isFrom = isFrom
                    ), true, true, AnimationType.fadeInfadeOut
                )

                /*Comman_Methods.isCustomPopUpShow(mActivity,
                    message = mActivity.resources.getString(R.string.add_user_desc_text),
                    positiveButtonText = mActivity.resources.getString(R.string.str_ok),
                    positiveButtonListener = object : PositiveButtonListener{})*/

            }
        }
        /*mActivity.addFragment(
            UpdatePayentFragment.newInstance(true, false, subscriptionBean, familyMonitorResult, false),
            true,
            true,
            AnimationType.fadeInfadeOut
        )*/
    }

    private fun setPermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
                .permissions(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                .onAccepted { permissions ->
                    mActivity.hideKeyboard()
                    if (permissions.size == 2) {
                        openCamera()
                    }
                }
                .onDenied {
                    setPermission()
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
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                .onAccepted { permissions ->
                    mActivity.hideKeyboard()
                    if (permissions.size == 3) {
                        openCamera()
                    }
                }
                .onDenied {
                    setPermission()
                }
                .onForeverDenied {
                    mActivity.showMessage(mActivity.resources.getString(R.string.permission_app_set))
                }
                .ask()
        }
    }

    private fun openCamera() {
        Album.image(mActivity) // Image and video mix options.
            .singleChoice() // Multi-Mode, Single-Mode: singleChoice().
            .columnCount(3) // The number of columns in the page list.
            .camera(true) // Whether the camera appears in the Item.
            .onResult { result ->
                if (result != null) {
                    if (result.size > 0) {
                        println("result[0].mimeType = ${result[0].mimeType}")
                        if (result[0].mimeType.contains("image")) {
                            if (result[0].path != null) {
                                println("result[0].path = ${result[0].path}")
                                if (result[0].path != "") {
                                    imageFile = File(result[0].path)
                                    if (imageFile?.exists() == true) {
                                        updateProfile = imageFile?.name ?: ""
                                        sdv_profile_image.loadFrescoImageFromFile(
                                            mActivity,
                                            imageFile,
                                            1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .onCancel { }
            .start()
    }

    private fun checkForValidations(): Boolean {
        return when {
            !isRequiredField(etSignUserName.text.toString().trim()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_user))
                false
            }
            !isUsernameValid->{
                mActivity.showMessage(mActivity.resources.getString(R.string.str_username_error))
                false
            }
            !isRequiredField(etSignFirstName.text.toString().trim()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_first))

                false
            }
            !isRequiredField(etSignLastName.text.toString().trim()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_last))
                false
            }
            /*!isRequiredField(etSignPhone.text.toString()) -> {
                mActivity.showMwssage(mActivity.resources.getString(R.string.blank_phone))
                false
            }*/
            isRequiredField(etSignPhone.text.toString()) && etSignPhone.text.toString().trim().length != 10 -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.phone_length))
                false
            }
            !isRequiredField(etSignEmail.text.toString().trim()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_email))
                false
            }
            !isValidEmail(etSignEmail.text.toString().trim()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.val_email))
                false
            }
            !isRequiredField(etSignPassword.text.toString()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_pass))

                false
            }
            !isUpdate && !isValidPasswordLength(etSignPassword.text.toString()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.pass_val))
                false
            }
/*
            !isPasswordValidate(etSignPassword.text.toString()) ->{
                mActivity.showMessage(mActivity.resources.getString(R.string.pass_common_val))
                false
            }
*/
            !isPasswordValid->{
                mActivity.showMessage(mActivity.resources.getString(R.string.valid_pass))
                false
            }
            etSignCPassword.visibility == View.VISIBLE && !isRequiredField(etSignCPassword.text.toString()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_conf_pass))
                false
            }
            etSignCPassword.visibility == View.VISIBLE && !isPasswordMatch(
                etSignPassword.text.toString(),
                etSignCPassword.text.toString()
            ) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.match_pass))

                false
            }
            /*etSignFrequency.text.toString().trim().isEmpty() ->{
                mActivity.showMwssage(mActivity.resources.getString(R.string.freq_blank))
                false
            }*/
            etSignFrequency.text.toString().toInt() < 60 ->{
                mActivity.showMessage(mActivity.resources.getString(R.string.recommended_freq))
                false
            }
            etSignFrequency.text.toString().toInt() > 14400 ->{
                mActivity.showMessage(mActivity.resources.getString(R.string.recommended_freq))
                false
            }
            else -> true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mActivity.unregisterReceiver(mBatInfoReceiver)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            EasyWayLocation.LOCATION_SETTING_REQUEST_CODE -> {
                easyWayLocation.onActivityResult(resultCode)
            }
        }
    }

    override fun locationCancelled() {

    }

    override fun locationOn() {
        if (easyWayLocation.getmListener != null) {
            easyWayLocation.beginUpdates()
        }
    }

    override fun onPositionChanged() {
        // easyWayLocation.showAlertDialog("", "", null)
    }

    private fun callVerifyEmailAPI(id: Int) {
        mActivity.isSpeedAvailable()
        val main = JsonObject()
        main.addProperty("id", id)
        main.addProperty("Email", etSignEmail.text.toString().trim())
        main.addProperty("Username", etSignUserName.text.toString().trim())
        main.addProperty(
            "Mobile",
            if (etSignPhone.text.toString().trim()
                    .isNotEmpty()
            ) countryCode + etSignPhone.text.toString() else ""
        )

        Utils.verifyUserData(mActivity, main, object : CommonApiListener {
            override fun commonResponse(status: Boolean, message: String, responseMessage: String, result: String) {
                if (status) {
                    /*if (isRemoveProfile){
                        if (familyMonitorResult.iD > 0) {
                            callRemoveProfileImage(familyMonitorResult.iD)
                        }
                    }else{
                        gotoPaymentScreen()
                    }*/
                    gotoPaymentScreen()
                }
            }
        })
    }

    private fun callSignUpAPI() {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {

            val androidId: String = Settings.Secure.getString(
                mActivity.contentResolver,
                Settings.Secure.ANDROID_ID
            )

            Comman_Methods.isProgressShow(mActivity)
            mActivity.isSpeedAvailable()

            val mobile_no =
                (if (etSignPhone.text.toString().trim().isNotEmpty()) countryCode + etSignPhone.text.toString().trim() else "").toRequestBody(
                    MEDIA_TYPE_TEXT
                )
            val email = etSignEmail.text.toString().trim().toRequestBody(MEDIA_TYPE_TEXT)
            val password = etSignPassword.text.toString().toRequestBody(MEDIA_TYPE_TEXT)
            val appDatabase = OldMe911Database.getDatabase(mActivity)
            val loginObject = appDatabase.loginDao().getAll()
            val createdBy: RequestBody = loginObject.memberID.toString().toRequestBody(MEDIA_TYPE_TEXT)
            val userId: RequestBody = if (isUpdate) {
                familyMonitorResult.iD.toString().toRequestBody(MEDIA_TYPE_TEXT)
            } else {
                "0".toRequestBody(MEDIA_TYPE_TEXT)
            }

            val userName = etSignUserName.text.toString().trim().toRequestBody(MEDIA_TYPE_TEXT)
            val firstName = etSignFirstName.text.toString().trim().toRequestBody(MEDIA_TYPE_TEXT)
            val lastName = etSignLastName.text.toString().trim().toRequestBody(MEDIA_TYPE_TEXT)
            val referralName = loginObject.ReferralCode.toString().trim().toRequestBody(MEDIA_TYPE_TEXT)
            val deviceModel = Comman_Methods.getdeviceModel().toRequestBody(MEDIA_TYPE_TEXT)
            val deviceCompany = Comman_Methods.getdevicename().toRequestBody(MEDIA_TYPE_TEXT)
            val deviceOs = getdeviceVersion().toRequestBody(MEDIA_TYPE_TEXT)
            val deviceType = "Android".toRequestBody(MEDIA_TYPE_TEXT)
            val recordStatus: RequestBody = "0".toRequestBody(MEDIA_TYPE_TEXT)
            val decimalSymbols = DecimalFormatSymbols.getInstance(Locale.US)
            val longitude =
                DecimalFormat("##.######", decimalSymbols).format(easyWayLocation.longitude)
                    .toRequestBody(MEDIA_TYPE_TEXT)
            val lattitude =
                DecimalFormat("##.######", decimalSymbols).format(easyWayLocation.latitude)
                    .toRequestBody(MEDIA_TYPE_TEXT)
            val battery_level = bettery_level.toString().toRequestBody(MEDIA_TYPE_TEXT)
            val start_date = Comman_Methods.getcurrentDate().toRequestBody(MEDIA_TYPE_TEXT)
            val device_token_id = "".toRequestBody(MEDIA_TYPE_TEXT)
            val device_uuid = androidId.toRequestBody(MEDIA_TYPE_TEXT)
            val location_permission = location_per.toString().toRequestBody(MEDIA_TYPE_TEXT)
            val location_address = ""
//                Utils.getCompleteAddressString(mActivity, easyWayLocation.latitude, easyWayLocation.longitude)
                .toRequestBody(MEDIA_TYPE_TEXT)
            val isAdminMem: RequestBody = if (familyMonitorResult.IsAdditionalMember == true) {
                "true".toRequestBody(MEDIA_TYPE_TEXT)
            } else {
                "false".toRequestBody(MEDIA_TYPE_TEXT)
            }
            val promoCode = "".toRequestBody(MEDIA_TYPE_TEXT)
            val frequency: RequestBody =
                etSignFrequency.text.toString().toRequestBody(MEDIA_TYPE_TEXT)
            val createdOn: RequestBody = Utils.getCurrentTimeStamp().toRequestBody(MEDIA_TYPE_TEXT)
            val device_token = "".toRequestBody(MEDIA_TYPE_TEXT)
            val device_id = "1".toRequestBody(MEDIA_TYPE_TEXT)
            val paymentType = "1".toRequestBody(MEDIA_TYPE_TEXT)
            val loginByApp = "2".toRequestBody(MEDIA_TYPE_TEXT)
            val imageBody: RequestBody
            val isSms: RequestBody = "false".toRequestBody(MEDIA_TYPE_TEXT)
            val payment_token = "".toRequestBody(MEDIA_TYPE_TEXT)
            val payment_method = "".toRequestBody(MEDIA_TYPE_TEXT)
            val notification_permission: RequestBody =
                if (NotificationManagerCompat.from(mActivity).areNotificationsEnabled()) {
                    "true".toRequestBody(MEDIA_TYPE_TEXT)
                } else {
                    "false".toRequestBody(MEDIA_TYPE_TEXT)
                }
            val isChildMissing: RequestBody = "false".toRequestBody(MEDIA_TYPE_TEXT)
            part = when {
                imageFile != null -> {
                    imageFile = File(
                        Comman_Methods.compressImage(
                            imageFile?.absolutePath ?: "",
                            mActivity
                        )
                    )
                    if (imageFile?.exists() == true) {
                        imageBody = imageFile!!.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("ProfilePath", imageFile?.name, imageBody)
                    } else {
                        imageBody = nullData.toRequestBody("multipart/form-data".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("ProfilePath", null, imageBody)
                    }
                }
                else -> {
                    imageBody = nullData.toRequestBody("multipart/form-data".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("ProfilePath", null, imageBody)
                }
            }


            val callRegisrationApi = WebApiClient.getInstance(mActivity)
                .webApi_with_MultiPart?.callSignUpApi(
                    email,
                    mobile_no,
                    password,
                    createdBy,
                    userName,
                    userId,
                    firstName,
                    lastName,
                    deviceModel,
                    deviceCompany,
                    device_id,
                    deviceOs,
                    device_token,
                    deviceType,
                    recordStatus,
                    longitude,
                    lattitude,
                    battery_level,
                    device_token_id,
                    start_date,
                    device_uuid,
                    location_permission,
                    location_address,
                    isSms,
                    part,
                    payment_token,
                    payment_method,
                    isAdminMem,
                    notification_permission,
                    frequency,
                    loginByApp,
                    referralName,
                    promoCode,
                    isChildMissing,
                    paymentType)

            callRegisrationApi?.enqueue(object : Callback<CommonValidationResponse> {
                override fun onFailure(call: Call<CommonValidationResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(call: Call<CommonValidationResponse>, response: Response<CommonValidationResponse>) {
                    val statusCode: Int = response.code()
                    val login_response = response.body()

                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            if (login_response?.status == true) {
//                            mActivity.onBackPressed()
                                if (imageFile != null) {
                                    if (imageFile?.exists() == true) {
                                        imageFile?.delete()
                                        if (imageFile?.exists() == true) {
                                            imageFile?.canonicalFile?.delete()
                                        }
                                    }
                                }
                                if (isFrom) {
                                    mActivity.addFragment(
                                        LiveMemberFragment.newInstance(true),
                                        true,
                                        true,
                                        animationType = AnimationType.fadeInfadeOut
                                    )
                                } else {
                                    mActivity.addFragment(
                                        DashBoardFragment.newInstance(true),
                                        true,
                                        true,
                                        animationType = AnimationType.fadeInfadeOut
                                    )
                                }
                            } else {
                                mActivity.showMessage(login_response?.message ?: "")
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

    private fun openUploadCountryDialog(phoneCountryCodes: ArrayList<PhoneCountryCode>) {
        val inflater = layoutInflater
        val dialogLayout1 = inflater.inflate(R.layout.search_country, null)
        val mDialog = AlertDialog.Builder(mActivity)
        mDialog.setView(dialogLayout1)

        if (this::dialog.isInitialized) {
            if (dialog != null) {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }
        }

        dialog = mDialog.create()
        dialog.window?.attributes?.windowAnimations = R.style.animationForDialog

        dialogLayout1.tvSearchCountryClose.setOnClickListener { dialog.dismiss() }
        dialogLayout1.rvCountryNameCodeFlag.layoutManager = LinearLayoutManager(
            mActivity,
            RecyclerView.VERTICAL,
            false
        )
        val adapter = CountrySelectionAdapter(mActivity, phoneCountryCodes)
        dialogLayout1.rvCountryNameCodeFlag.adapter = adapter
        dialogLayout1.tvSearchCountryHeader.text = mActivity.resources.getString(R.string.select_country)
        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            dialogLayout1.etSearchCountry.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        } else {
            dialogLayout1.etSearchCountry.imeOptions = EditorInfo.IME_ACTION_DONE
        }
        dialogLayout1.etSearchCountry.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter.filter(s)
            }

        })
        dialog.setCancelable(false)
        dialog.show()
    }

    inner class CountrySelectionAdapter() : RecyclerView.Adapter<CountrySelectionAdapter.CountrySelectionHolder>(),
        Filterable {

        lateinit var context: HomeActivity
        private var phoneCountryCodes: ArrayList<PhoneCountryCode> = ArrayList()
        private var duplicatePhoneCountryCodes: ArrayList<PhoneCountryCode> = ArrayList()
        private var adapterPosition = -1

        constructor(context: HomeActivity, phoneCountryCodes: ArrayList<PhoneCountryCode>) : this() {
            this.context = context
            this.phoneCountryCodes = phoneCountryCodes
            this.duplicatePhoneCountryCodes = ArrayList()
            this.duplicatePhoneCountryCodes.addAll(phoneCountryCodes)
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): CountrySelectionHolder {
            return CountrySelectionHolder(
                LayoutInflater.from(context).inflate(
                    R.layout.raw_country_search,
                    p0,
                    false
                )
            )
        }

        override fun getItemCount(): Int {
            return duplicatePhoneCountryCodes.size
        }

        override fun onBindViewHolder(p0: CountrySelectionHolder, p1: Int) {
            when (duplicatePhoneCountryCodes[p1].code) {
                "CA" -> p0.tvCountryDetails.text =
                    duplicatePhoneCountryCodes[p1].countryName + " (+1)"
                else -> p0.tvCountryDetails.text =
                    duplicatePhoneCountryCodes[p1].countryName + " (" + duplicatePhoneCountryCodes[p1].countryCode + ")"
            }
            p0.tvCountryDetails.setCompoundDrawablesWithIntrinsicBounds(
                duplicatePhoneCountryCodes[p1].flag,
                0,
                0,
                0
            )
            p0.tvCountryDetails.setOnClickListener {
                context.hideKeyboard()
                adapterPosition = p0.bindingAdapterPosition
                notifyDataSetChanged()
            }
            if (adapterPosition == p1) {
                duplicatePhoneCountryCodes[p1].isSelected = true
                dialog.dismiss()
            } else {
                duplicatePhoneCountryCodes[p1].isSelected = false
            }
            displaySelectedCountry(duplicatePhoneCountryCodes)
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(charSequence: CharSequence): FilterResults {
                    val charString = charSequence.toString()
                    duplicatePhoneCountryCodes = if (charString.isEmpty()) {
                        phoneCountryCodes
                    } else {
                        val filterList = ArrayList<PhoneCountryCode>()
                        for (row in phoneCountryCodes) {
                            if (row.countryName.lowercase().contains(charString.lowercase()) || row.countryCode.lowercase().contains(
                                    charString.lowercase()
                                )
                            ) {
                                filterList.add(row)
                            }
                        }
                        filterList
                    }
                    val filterResults = FilterResults()
                    filterResults.values = duplicatePhoneCountryCodes
                    return filterResults
                }

                override fun publishResults(
                    charSequence: CharSequence,
                    filterResults: FilterResults
                ) {
                    if (filterResults.values != null) {
                        duplicatePhoneCountryCodes = filterResults.values as ArrayList<PhoneCountryCode>
                    }
                    notifyDataSetChanged()
                }
            }
        }

        inner class CountrySelectionHolder(view: View) : RecyclerView.ViewHolder(view) {
            var tvCountryDetails: TextView = view.tvCountryDetails
        }
    }

    private fun displaySelectedCountry(duplicatePhoneCountryCodes: ArrayList<PhoneCountryCode>) {
        for (i in 0 until duplicatePhoneCountryCodes.size) {
            if (duplicatePhoneCountryCodes[i].isSelected) {
                when {
                    duplicatePhoneCountryCodes[i].code == "CA" -> tvCountrySelected.text = "+1"
                    else -> tvCountrySelected.text = duplicatePhoneCountryCodes[i].countryCode
                }
                countryCode = duplicatePhoneCountryCodes[i].countryCode
                tvCountrySelected.setCompoundDrawablesWithIntrinsicBounds(
                    duplicatePhoneCountryCodes[i].flag,
                    0,
                    0,
                    0
                )
            }
        }
    }


    private fun callMemberApi() {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            mActivity.isSpeedAvailable()
            Utils.familyMonitoringUserList(mActivity, object : CommonApiListener{
                override fun familyUserList(
                    status: Boolean,
                    userList: ArrayList<FamilyMonitorResult>,
                    message: String
                ) {
                    if (userList.size > 0) {
                        rvAddUserImageList.visibility = View.VISIBLE
                    } else {
                        rvAddUserImageList.visibility = View.GONE
                    }
                    if (status) {
                        memberList = ArrayList()
                        appDatabase.memberDao().dropTable()
                        appDatabase.memberDao().addAllMember(userList)
                        val loginupdate: LoginObject = appDatabase.loginDao().getAll()
                        val memberCount = appDatabase.memberDao().countMember()
                        if (memberCount > 0) {
                            loginupdate.totalMembers =
                                appDatabase.memberDao().countMember() - 1
                        } else {
                            loginupdate.totalMembers = 0
                        }
                        appDatabase.loginDao().updateLogin(loginupdate)
                        setData()
                    } else {
                        mActivity.showMessage(message)
                    }
                }

                override fun onFailureResult() {
                    if (memberList.size > 0) {
                        rvAddUserImageList.visibility = View.VISIBLE
                        setAdapter()
                    } else {
                        rvAddUserImageList.visibility = View.GONE
                    }
                }

            })
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
        if (memberList.size > 0) {
            rvAddUserImageList.visibility = View.VISIBLE
            setAdapter()
        } else {
            rvAddUserImageList.visibility = View.GONE
        }
    }

    fun setAdapter() {
        addMemberListAdapter = AddMemberListAdapter(mActivity, memberList)
        addMemberListAdapter.notifyDataSetChanged()
        rvAddUserImageList.adapter = addMemberListAdapter
    }

    inner class AddMemberListAdapter(
        val context: Context,
        val memberList: ArrayList<FamilyMonitorResult>
    ): RecyclerView.Adapter<AddMemberListAdapter.AddMemberListHolder>(){

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): AddMemberListHolder {
            return AddMemberListHolder(
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

        override fun onBindViewHolder(p0: AddMemberListHolder, p1: Int) {
            val roundValue: Float = Comman_Methods.convertDpToPixel(15f, mActivity)
            if (memberList[p1].iD != selectedMemberId) {
                val builder = GenericDraweeHierarchyBuilder(mActivity.resources)
                val roundingParams : RoundingParams = RoundingParams.fromCornersRadius(roundValue).setBorder(
                    ContextCompat.getColor(mActivity, R.color.caldroid_white), 2.0f)
                val hierarchy: GenericDraweeHierarchy = builder
                    .setRoundingParams(roundingParams)
                    .build()

                p0.sdvMemberImage.hierarchy = hierarchy
                p0.sdvMemberImage.background = ContextCompat.getDrawable(mActivity,R.drawable.square_upload_profile)
                p0.tvMemberListName.setTextColor(ContextCompat.getColor(mActivity, R.color.colorAccent))
            }else{
                val builder = GenericDraweeHierarchyBuilder(mActivity.resources)
                val roundingParams : RoundingParams = RoundingParams.fromCornersRadius(roundValue).setBorder(ContextCompat.getColor(mActivity, R.color.colorPrimary), 2.0f)
                val hierarchy: GenericDraweeHierarchy = builder
                    .setRoundingParams(roundingParams)
                    .build()

                p0.sdvMemberImage.hierarchy = hierarchy
                p0.sdvMemberImage.background = ContextCompat.getDrawable(mActivity,R.drawable.square_upload_profile_green)
                p0.tvMemberListName.setTextColor(ContextCompat.getColor(mActivity, R.color.colorPrimary))
            }
            p0.sdvMemberImage.loadFrescoImage(mActivity, memberList[p1].image ?: "", 1)
            p0.tvMemberListName.text = memberList[p1].firstName + " " + memberList[p1].lastName
            p0.ivLiveMemberInitialize.visibility = View.GONE
            p0.rvMemberDetail.setOnClickListener {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
                if (memberList[p1].IsCancelled == true){

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
                }else{

                    mActivity.hideKeyboard()
                    if (memberList[p1].iD != appDatabase.loginDao().getAll().memberID) {
                        selectedMemberId = memberList[p1].iD
                        isFromPayment = false
                        isUpdate = true
                        familyMonitorResult = memberList[p1]
                        subscriptionBean = SubscriptionBean()
                        isFromList = true
                        isFrom = false

                        initView()
                    }
                    notifyDataSetChanged()
                }
            }
        }

        inner class AddMemberListHolder(view: View): RecyclerView.ViewHolder(view){
            var rvMemberDetail: RelativeLayout = view.rvMemberDetail
            var sdvMemberImage: SimpleDraweeView = view.sdvMemberImage
            var tvMemberListName: TextView = view.tvMemberListName
            var ivLiveMemberInitialize: ImageView = view.ivLiveMemberInitialize
        }
    }

    fun initView(){
        setHeader()

        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            etSignUserName.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etSignFirstName.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etSignLastName.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etSignPhone.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etSignEmail.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etSignPassword.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etSignFrequency.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        } else {
            etSignUserName.imeOptions = EditorInfo.IME_ACTION_NEXT
            etSignFirstName.imeOptions = EditorInfo.IME_ACTION_NEXT
            etSignLastName.imeOptions = EditorInfo.IME_ACTION_NEXT
            etSignPhone.imeOptions = EditorInfo.IME_ACTION_NEXT
            etSignEmail.imeOptions = EditorInfo.IME_ACTION_NEXT
            etSignPassword.imeOptions = EditorInfo.IME_ACTION_DONE
            etSignFrequency.imeOptions = EditorInfo.IME_ACTION_DONE
        }

        if (isFromPayment) {
            btn_sign_in.text = mActivity.resources.getString(R.string.next)
        } else {
            btn_sign_in.text = mActivity.resources.getString(R.string.submit)
        }
        flSignReferral.visibility = View.GONE
        tvReferralError.visibility = View.GONE
        flSignPromotion.visibility = View.GONE
        etSignReferralCode.visibility = View.GONE
        rl_sms.visibility = View.GONE
        tvSmsAlert.visibility = View.GONE
        switchSignMessage.visibility = View.GONE
        flSignPingFreq.visibility =View.GONE
        etSignPassword.removeTextChangedListener(passWordTextChangeListener)
        etSignUserName.removeTextChangedListener(usernameTextChangeListener)
        if (passwordShowed) {
            etSignPassword.transformationMethod =
                PasswordTransformationMethod.getInstance()
            ivShowPassword.setImageResource(R.drawable.show_card_number)
            passwordShowed = false
        }
        if (isUpdate) {
            when {
                mActivity.resources.getBoolean(R.bool.isTablet) -> etSignPhone.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
                else -> etSignPhone.imeOptions = EditorInfo.IME_ACTION_DONE
            }
            isPasswordValid = true
            isUsernameValid = true
            btnVerifyUsername.visibility = View.GONE
            imgVerified.visibility = View.GONE
            tvUsernameError.visibility  = View.GONE
            tvInviteNote.visibility = View.GONE
            val profileImage: String = familyMonitorResult.image ?: ""
            sdv_profile_image.loadFrescoImage(mActivity, profileImage, 1)
            val image_name = profileImage.split("Family/")
            updateProfile = if (image_name[1]!="") image_name[1] else ""
//            tvHeader.text = mActivity.resources.getString(R.string.update_member)
            etSignUserName.setText(familyMonitorResult.userName ?: "")
            etSignPassword.setText(familyMonitorResult.password ?: "")
            etSignEmail.setText(familyMonitorResult.email ?: "")
            etSignFirstName.setText(familyMonitorResult.firstName ?: "")
            etSignLastName.setText(familyMonitorResult.lastName ?: "")
            val frequency = familyMonitorResult.frequency ?: 60
            val userMobile = familyMonitorResult.mobile ?: ""
            if (frequency != 0) {
                if (frequency < 60){
                    etSignFrequency.setText("" + (frequency * 60))
                }else {
                    etSignFrequency.setText("" + frequency)
                }
            }else{
                etSignFrequency.setText("60")
            }
            if (userMobile.length > 10) {
                etSignPhone.setText(userMobile.takeLast(10))
                countryCode = userMobile.dropLast(10)
            } else {
                countryCode = phoneCountryCodes[UNITED__CODE_POSITION].countryCode
                etSignPhone.setText(userMobile)
            }
            etSignPassword.isFocusableInTouchMode = false
            etSignPassword.isFocusable = false
            flSignConfirmPassword.visibility = View.GONE
            etSignCPassword.visibility = View.GONE
            tvAddMemberTitle.visibility = View.GONE
            etSignUserName.isFocusableInTouchMode = false
            etSignUserName.isFocusable = false

            if (countryCode != "") {
                for (i in 0 until phoneCountryCodes.size) {
                    if (countryCode == phoneCountryCodes[i].countryCode) {
                        when (phoneCountryCodes[i].code) {
                            "CA" -> tvCountrySelected.text = "+1"
                            else -> tvCountrySelected.text = phoneCountryCodes[i].countryCode
                        }
                        tvCountrySelected.setCompoundDrawablesWithIntrinsicBounds(
                            phoneCountryCodes[i].flag,
                            0,
                            0,
                            0
                        )
                        return
                    } else {
                        tvCountrySelected.text = phoneCountryCodes[UNITED__CODE_POSITION].countryCode
                        tvCountrySelected.setCompoundDrawablesWithIntrinsicBounds(
                            phoneCountryCodes[UNITED__CODE_POSITION].flag,
                            0,
                            0,
                            0
                        )
                    }
                }
            } else {
                countryCode = phoneCountryCodes[UNITED__CODE_POSITION].countryCode
                tvCountrySelected.text = phoneCountryCodes[UNITED__CODE_POSITION].countryCode
                tvCountrySelected.setCompoundDrawablesWithIntrinsicBounds(
                    phoneCountryCodes[UNITED__CODE_POSITION].flag,
                    0,
                    0,
                    0
                )
            }
        } else {
            isPasswordValid = false
            sdv_profile_image.background = ContextCompat.getDrawable(mActivity,R.drawable.upload_profile_green)
            etSignUserName.setText("")
            etSignPassword.setText("")
            etSignEmail.setText("")
            etSignFirstName.setText("")
            etSignLastName.setText("")
            etSignFrequency.setText("")
            etSignPhone.setText("")
            if (mActivity.resources.getBoolean(R.bool.isTablet)) {
                etSignPassword.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
                etSignCPassword.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_DONE
            } else {
                etSignPassword.imeOptions = EditorInfo.IME_ACTION_NEXT
                etSignCPassword.imeOptions = EditorInfo.IME_ACTION_DONE
            }
            tvInviteNote.visibility = View.VISIBLE
            countryCode = phoneCountryCodes[UNITED__CODE_POSITION].countryCode
            tvCountrySelected.text = phoneCountryCodes[UNITED__CODE_POSITION].countryCode
            tvCountrySelected.setCompoundDrawablesWithIntrinsicBounds(
                phoneCountryCodes[UNITED__CODE_POSITION].flag,
                0,
                0,
                0
            )
//            tvHeader.text = mActivity.resources.getString(R.string.add_member)
            flSignConfirmPassword.visibility = View.VISIBLE
            etSignCPassword.visibility = View.VISIBLE
            tvAddMemberTitle.visibility = View.VISIBLE
            etSignPassword.isFocusableInTouchMode = true
            etSignPassword.isFocusable = true
            etSignUserName.isFocusableInTouchMode = true
            etSignUserName.isFocusable = true
            etSignFrequency.setText("60")
            etSignUserName.addTextChangedListener(usernameTextChangeListener)
        }
        etSignPassword.addTextChangedListener(passWordTextChangeListener)
    }
}