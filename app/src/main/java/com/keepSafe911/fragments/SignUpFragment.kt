package com.keepSafe911.fragments

import AnimationType
import ValidationUtil.Companion.isPasswordMatch
import ValidationUtil.Companion.isPasswordValidate
import ValidationUtil.Companion.isRequiredField
import ValidationUtil.Companion.isValidEmail
import addFragment
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.*
import android.content.pm.ActivityInfo
import android.graphics.Typeface
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.*
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import checkLocationPermission
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.itextpdf.text.SpecialSymbol.index
import com.keepSafe911.BuildConfig
import com.keepSafe911.MainActivity
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.MainBaseFragment
import com.keepSafe911.fragments.payment_selection.PaymentMethodFragment
import com.keepSafe911.gps.GpsTracker
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.listner.PaymentOptionListener
import com.keepSafe911.listner.PositiveButtonListener
import com.keepSafe911.model.FamilyMonitorResult
import com.keepSafe911.model.PhoneCountryCode
import com.keepSafe911.model.SubscriptionBean
import com.keepSafe911.model.response.CommonValidationResponse
import com.keepSafe911.model.response.SubscriptionTypeResult
import com.keepSafe911.model.response.paypal.SubscriptionResponse
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import com.keepSafe911.utils.Comman_Methods.Companion.getdeviceVersion
import com.keepSafe911.webservices.WebApiClient
import com.kotlinpermissions.KotlinPermissions
import com.yanzhenjie.album.Album
import hideKeyboard
import kotlinx.android.synthetic.main.background_layout.*
import kotlinx.android.synthetic.main.fragment_edit_profile.*
import kotlinx.android.synthetic.main.fragment_sign_up.*
import kotlinx.android.synthetic.main.raw_country_search.view.*
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


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val ARG_PARAM3 = "param3"
private const val ARG_PARAM4 = "param4"

class SignUpFragment : MainBaseFragment(), View.OnClickListener, Listener {

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    private var batteryLevel = 0
    private var locationPermission = false
    lateinit var easyWayLocation: EasyWayLocation
    lateinit var dialog: Dialog
    private var familyMonitorResult = FamilyMonitorResult()
    lateinit var part: MultipartBody.Part
    lateinit var appDatabase: OldMe911Database
    var imageFile: File? = null
    private var passwordShowed: Boolean = false
    private var phoneCountryCodes: ArrayList<PhoneCountryCode> = ArrayList()
    private var countryCode: String = ""
    private var gpstracker: GpsTracker? = null
    var isUsernameValid  = true
    var isPromotionValid = false
    var isReferralValid = false
    var isPasswordValid = false
    private var settingDialogShown: Boolean = false
    private var selectedSubscription: SubscriptionTypeResult? = null
    private var isMissingChild: Boolean = false
    private var subscriptionBean: SubscriptionBean = SubscriptionBean()
    private lateinit var paypalBottomSheetDialog: BottomSheetDialog
    private lateinit var payPalPaymentFragment: PayPalPaymentFragment


    private val mBatInfoReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        }
    }

    companion object {
        fun newInstance(
            isMissingChild: Boolean = false,
            subscriptionBean: SubscriptionBean = SubscriptionBean()
        ): SignUpFragment {
            val args = Bundle()
            args.putParcelable(ARG_PARAM1, subscriptionBean)
            args.putBoolean(ARG_PARAM2, isMissingChild)
            val fragment = SignUpFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            subscriptionBean = it.getParcelable(ARG_PARAM1) ?: SubscriptionBean()
            isMissingChild = it.getBoolean(ARG_PARAM2, false)
        }
        easyWayLocation = EasyWayLocation(requireActivity())
        requireActivity().registerReceiver(this.mBatInfoReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        //activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireActivity().requestedOrientation =
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
        openDialog()
        storeCountryCode()
        gpstracker = GpsTracker(mActivity)
        appDatabase = OldMe911Database.getDatabase(mActivity)
        btn_sign_in.setOnClickListener(this)
        img_nphotoUpload.setOnClickListener(this)
        imgPhotoUpload.setOnClickListener(this)
        tvCountrySelected.setOnClickListener(this)
        ivShowPassword.setOnClickListener(this)
        rvAddUserImageList.visibility = View.GONE
        imgPhotoUpload.visibility = View.GONE
        img_nphotoUpload.visibility = View.VISIBLE
//        rlAddUserAddUser.visibility = View.GONE
        /*tvFacebookOr.visibility = View.VISIBLE
        btnFacebookSignUp.visibility = View.VISIBLE*/
        btnFacebookSignUp.setOnClickListener(this)
        easyWayLocation.setListener(this)

        setHeader()

        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            etSignUserName.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etSignFirstName.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etSignLastName.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etSignPhone.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etSignEmail.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etSignPassword.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etSignCPassword.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_NEXT
            etSignReferralCode.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_DONE
            etSignPromotion.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_DONE
        } else {
            etSignUserName.imeOptions = EditorInfo.IME_ACTION_NEXT
            etSignFirstName.imeOptions = EditorInfo.IME_ACTION_NEXT
            etSignLastName.imeOptions = EditorInfo.IME_ACTION_NEXT
            etSignPhone.imeOptions = EditorInfo.IME_ACTION_NEXT
            etSignEmail.imeOptions = EditorInfo.IME_ACTION_NEXT
            etSignPassword.imeOptions = EditorInfo.IME_ACTION_NEXT
            etSignCPassword.imeOptions = EditorInfo.IME_ACTION_NEXT
            etSignReferralCode.imeOptions = EditorInfo.IME_ACTION_DONE
            etSignPromotion.imeOptions = EditorInfo.IME_ACTION_DONE
        }

        tvInviteNote.visibility = View.GONE
        tvPingFrequency.visibility = View.GONE
        flSignPingFreq.visibility = View.GONE
        etSignFrequency.visibility = View.GONE
        tvSmsAlert.visibility = View.VISIBLE
        switchSignMessage.visibility = View.VISIBLE
        switchSignMessage.setOnCheckedChangeListener { buttonView, isChecked ->
            if (etSignPhone.text.toString().trim().isNotEmpty()){
                switchSignMessage.isChecked = isChecked
            }else{
                switchSignMessage.isChecked = false
            }
        }
        etSignReferralCode.visibility = View.VISIBLE
//        cbPrivacyTerms.visibility = View.VISIBLE
        countryCode = phoneCountryCodes[UNITED__CODE_POSITION].countryCode
        tvCountrySelected.text = phoneCountryCodes[UNITED__CODE_POSITION].countryCode
        tvCountrySelected.setCompoundDrawablesWithIntrinsicBounds(
            phoneCountryCodes[UNITED__CODE_POSITION].flag,
            0,
            0,
            0
        )
        val signUpContent = mActivity.resources.getString(R.string.sign_up)
            .uppercase(Locale.getDefault())
        val signContent = mActivity.resources.getString(R.string.str_sign)
            .uppercase(Locale.getDefault())
        val contentTitle = SpannableString(signUpContent)
        contentTitle.setSpan(StyleSpan(Typeface.BOLD), 0, signContent.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvSignUpTitle.text = contentTitle

        if (isMissingChild) {
            btn_sign_in.text = mActivity.resources.getString(R.string.sign_up)
        } else {
            btn_sign_in.text = mActivity.resources.getString(R.string.next)
        }
        etSignEmail.isFocusableInTouchMode = true
        viewDash2.visibility = View.GONE
        tvAddMemberTitle.visibility = View.GONE
        etSignCPassword.visibility = View.VISIBLE
        etSignPassword.isFocusableInTouchMode = true
        etSignUserName.isFocusableInTouchMode = true

        etSignUserName.filters = arrayOf(Utils.filterUserName)
        etSignPassword.filters = arrayOf(Utils.filterPassword)
        etSignCPassword.filters = arrayOf(Utils.filterPassword)
        btnVerifyUsername.setOnClickListener{
            checkUserNameUniqueApi(etSignUserName.text.toString().trim(), 2)
        }

        btnVerifyPromotion.setOnClickListener {
            checkUserNameUniqueApi(etSignPromotion.text.toString().trim(), 4)
        }

        etSignUserName.addTextChangedListener(object: TextWatcher{
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

        })

        etSignPromotion.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(username: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (username.toString().isNotEmpty()){
                    btnVerifyPromotion.visibility = View.VISIBLE
                    isPromotionValid = false
                    selectedSubscription = null
                }else{
                    btnVerifyPromotion.visibility = View.GONE
                }
                imgVerifiedPromotion.visibility = View.GONE
                tvPromotionError.visibility  = View.GONE

            }
            override fun afterTextChanged(p0: Editable?) {}

        })

        etSignReferralCode.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().isNotEmpty()){
                    tvReferralError.visibility = View.GONE
                    val content = s?.length
                    if (content == 8){
                        checkUserNameUniqueApi(etSignReferralCode.text.toString().trim(), 3)
                    }
                }else{
                    imgReferralVerified.visibility = View.GONE
                    tvReferralError.visibility = View.GONE
                }
                imgReferralVerified.visibility = View.GONE
                tvReferralError.visibility  = View.GONE

            }
            override fun afterTextChanged(s: Editable?) {}

        })
        etSignPassword.addTextChangedListener(object: TextWatcher {
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

        })
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
                    when (type) {
                        2 -> {
                            imgVerified.visibility = View.GONE
                            btnVerifyUsername.visibility = View.GONE
                            tvUsernameError.visibility  = View.VISIBLE
                            isUsernameValid  = false
                            tvUsernameError.text = message
                        }
                        3 -> {
                            imgReferralVerified.visibility = View.GONE
                            tvReferralError.visibility = View.VISIBLE
                            tvReferralError.text = message
                            isReferralValid = false
                        }
                        4 -> {
                            imgVerifiedPromotion.visibility = View.GONE
                            btnVerifyPromotion.visibility = View.GONE
                            tvPromotionError.visibility = View.VISIBLE
                            tvPromotionError.text = message
                            isPromotionValid = false
                            selectedSubscription = null
                        }
                    }

                }else{
                    when (type) {
                        2 -> {
                            isUsernameValid = true
                            btnVerifyUsername.visibility = View.GONE
                            imgVerified.visibility = View.VISIBLE
                            tvUsernameError.visibility  = View.GONE
                        }
                        3 -> {
                            imgReferralVerified.visibility = View.VISIBLE
                            tvReferralError.visibility  = View.GONE
                            isReferralValid = true
                        }
                        4 -> {
                            imgVerifiedPromotion.visibility = View.VISIBLE
                            btnVerifyPromotion.visibility = View.GONE
                            tvPromotionError.visibility = View.GONE
                            isPromotionValid = true
                            val gson: Gson = GsonBuilder().create()
                            val responseTypeToken: TypeToken<SubscriptionTypeResult> =
                                object : TypeToken<SubscriptionTypeResult>() {}
                            val responseData: SubscriptionTypeResult? =
                                gson.fromJson(
                                    gson.toJson(result),
                                    responseTypeToken.type
                                )
                            selectedSubscription = responseData
                        }
                    }
                }
            }
        })
    }

    private fun openDialog() {
        try {
            Comman_Methods.isCustomPopUpShow(mActivity,
                message = mActivity.resources.getString(R.string.str_18_old),
                positiveButtonListener = object : PositiveButtonListener {
                    override fun okClickListener() {
//                        openMissingDialog()
                    }

                    override fun cancelClickLister() {
                        mActivity.finish()
                    }
                })
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun openMissingDialog() {
        try {
            Comman_Methods.isCustomPopUpShow(mActivity,
                message = mActivity.resources.getString(R.string.str_child_missing),
                positiveButtonListener = object : PositiveButtonListener {
                    override fun okClickListener() {
                        isMissingChild = true
                        btn_sign_in.text = mActivity.resources.getString(R.string.sign_up)
                    }

                    override fun cancelClickLister() {
                        isMissingChild = false
                        btn_sign_in.text = mActivity.resources.getString(R.string.next)
                    }
                })
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun storeCountryCode() {
        phoneCountryCodes = ArrayList()
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
        }
    }

    private fun setHeader() {
        tvHeader.text = ""
        iv_back.visibility = View.VISIBLE
//        ivTopBubble.visibility = View.VISIBLE
//        ivBottomBubble.visibility = View.VISIBLE
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_sign_in -> {
                etSignReferralCode.clearFocus()
                mActivity.hideKeyboard()
                if (checkForValidations()) {

                    Comman_Methods.avoidDoubleClicks(v)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        settingDialogShown = false
                        if (checkLocationPermission(mActivity)) {
                            setLocationPermission()
                        } else {
                            disclosurePopUp()
                        }
                    } else {
                        //callVerifyEmailAPI()
                        gotoPaymentScreen()
                    }
                }
            }
            R.id.imgPhotoUpload, R.id.img_nphotoUpload -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                /*if (imageFile!=null){
                    pictureOption()
                }else{
                    setPermission()
                }*/
                setPermission()
            }
            R.id.tvCountrySelected -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                storeCountryCode()
                openUploadCountryDialog(phoneCountryCodes)
            }
            R.id.btnFacebookSignUp -> {
                mActivity.hideKeyboard()
                mActivity.showMessage(mActivity.resources.getString(R.string.under_dev))
            }
            R.id.ivShowPassword -> {
                mActivity.hideKeyboard()
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
        }
    }

    private fun disclosurePopUp() {
        Comman_Methods.isCustomPopUpShow(mActivity,
            message = mActivity.resources.getString(R.string.str_location_permission_disclosure),
            positiveButtonText = mActivity.resources.getString(R.string.str_allow),
            negativeButtonText = mActivity.resources.getString(R.string.str_decline),
            positiveButtonListener = object : PositiveButtonListener {
                override fun okClickListener() {
                    setLocationPermission()
                }
            })
    }

    private fun pictureOption(){
        val view = LayoutInflater.from(mActivity)
            .inflate(R.layout.popup_picture_option_layout, mActivity.window.decorView.rootView as ViewGroup, false)
        val bottomSheetDialog = BottomSheetDialog(mActivity)
        bottomSheetDialog.setContentView(view)
        val mBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(view.parent as View)
        mBehavior.isHideable = false
        bottomSheetDialog.setOnShowListener {
            mBehavior.peekHeight = view.height
        }
        val ivPictureCancel: ImageView? = bottomSheetDialog.findViewById(R.id.ivPictureCancel)
        val fabAlbumOption: FloatingActionButton? = bottomSheetDialog.findViewById(R.id.fabAlbumOption)
        val fabRemoveOption: FloatingActionButton? = bottomSheetDialog.findViewById(R.id.fabRemoveOption)
        val tvAlbumOptionName: TextView? = bottomSheetDialog.findViewById(R.id.tvAlbumOptionName)
        val tvRemoveOptionName: TextView? = bottomSheetDialog.findViewById(R.id.tvRemoveOptionName)
        ivPictureCancel?.setOnClickListener {
            mActivity.hideKeyboard()
            bottomSheetDialog.dismiss()
        }
        fabRemoveOption?.setOnClickListener {
            mActivity.hideKeyboard()
            imageFile = null
            sdv_profile_image.loadFrescoImageFromFile(mActivity, File(""), 1)
            bottomSheetDialog.dismiss()
        }
        tvRemoveOptionName?.setOnClickListener {
            mActivity.hideKeyboard()
            imageFile = null
            sdv_profile_image.loadFrescoImageFromFile(mActivity, File(""), 1)
            bottomSheetDialog.dismiss()
        }
        fabAlbumOption?.setOnClickListener {
            mActivity.hideKeyboard()
            bottomSheetDialog.dismiss()
            Comman_Methods.avoidDoubleClicks(fabAlbumOption)
            setPermission()
        }
        tvAlbumOptionName?.setOnClickListener {
            mActivity.hideKeyboard()
            bottomSheetDialog.dismiss()
            setPermission()
        }
        bottomSheetDialog.setCancelable(false)
        bottomSheetDialog.show()
    }

    private fun callApi(paymentType: String, subscriptionId: String, paymentMethod: String) {
        easyWayLocation = EasyWayLocation(mActivity)
        easyWayLocation.setListener(this)
        if (gpstracker?.CheckForLoCation() == true) {
            callSignUpAPI(paymentType, subscriptionId, paymentMethod)
        } else {
            Utils.showLocationSettingsAlert(mActivity)
        }
    }

    private fun setLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
                .permissions(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
                .onAccepted { permissions ->
                    mActivity.hideKeyboard()
                    if (permissions.size == 3) {
                        locationPermission = true
                        //callVerifyEmailAPI()
                        gotoPaymentScreen()
                    }
                }
                .onDenied {
                    if (!settingDialogShown) {
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                            settingDialogShown = true
                        }
                        setLocationPermission()
                    }
                }
                .onForeverDenied {
                    if (!settingDialogShown) {
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                            settingDialogShown = true
                        }
                        mActivity.showMessage(mActivity.resources.getString(R.string.permission_app_set))
                        easyWayLocation = EasyWayLocation(mActivity)
                        easyWayLocation.setListener(this)
                        Utils.showSettingsAlert(mActivity)
                    }
                }
                .ask()
        } else {
            KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
                .permissions(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                .onAccepted { permissions ->
                    mActivity.hideKeyboard()
                    if (permissions.size == 2) {
                        locationPermission = true
                        //callVerifyEmailAPI()
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
    }

    private fun gotoPaymentScreen() {

        familyMonitorResult.iD = 0
        familyMonitorResult.createdBy = 0
        familyMonitorResult.createdOn = Comman_Methods.getcurrentDate()
        familyMonitorResult.email = etSignEmail.text.toString()
        familyMonitorResult.firstName = etSignFirstName.text.toString().trim()
        if (isReferralValid){
            familyMonitorResult.ReferralName = etSignReferralCode.text.toString().trim()
        }else{
            familyMonitorResult.ReferralName = ""
        }
        if (isPromotionValid) {
            familyMonitorResult.Promocode = etSignPromotion.text.toString().trim()
        } else {
            familyMonitorResult.Promocode = ""
        }
        familyMonitorResult.isChildMissing = isMissingChild


        when {
            imageFile != null -> familyMonitorResult.image = imageFile?.absolutePath ?: ""
            else -> familyMonitorResult.image = ""
        }
        familyMonitorResult.lastName = etSignLastName.text.toString().trim()
        familyMonitorResult.mobile = if (etSignPhone.text.toString().trim().isNotEmpty())countryCode + etSignPhone.text.toString().trim() else ""
        familyMonitorResult.password = etSignPassword.text.toString()
        familyMonitorResult.userName = etSignUserName.text.toString().trim()
        familyMonitorResult.isSms = if (etSignPhone.text.toString().trim().isNotEmpty()) switchSignMessage.isChecked else false

        when {
            isMissingChild -> {
                val paymentMethodData = if (selectedSubscription != null) if (selectedSubscription?.id!! > 0) selectedSubscription?.id!!.toString() else "" else ""
                callApi("1", "", paymentMethodData)
            }
            isPromotionValid -> {
                val promotionSubscription = SubscriptionBean(
                    selectedSubscription?.id ?: 0,
                    selectedSubscription?.days ?: 0,
                    selectedSubscription?.totalCost ?: 0.0,
                    selectedSubscription?.planId ?: ""
                )
                /*mActivity.addFragment(
                    PaymentMethodFragment.newInstance(
                        familyMonitorResult,
                        promotionSubscription
                    ), true, true, AnimationType.fadeInfadeOut
                )*/
                showPaypalOption(familyMonitorResult, promotionSubscription)
            }
            else -> {
                showPaypalOption(familyMonitorResult, subscriptionBean)
                /*mActivity.addFragment(
                    PaymentMethodFragment.newInstance(
                        familyMonitorResult,
                        subscriptionBean
                    ), true, true, AnimationType.fadeInfadeOut
                )*/
                /*mActivity.addFragment(
                    SubscriptionFragment.newInstance(
                        familyMonitorResult
                    ), true, true, AnimationType.fadeInfadeOut
                )*/
            }
        }
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
                                        sdv_profile_image.loadFrescoImageFromFile(
                                            mActivity,
                                            imageFile,
                                            1
                                        )
                                        imageFile =
                                            File(
                                                Comman_Methods.compressImage(
                                                    imageFile?.absolutePath ?: "",
                                                    mActivity
                                                )
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
            /*!isValidPasswordLength(etSignPassword.text.toString()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.pass_val))
                false
            }
            !isPasswordValidate(etSignPassword.text.toString()) ->{
                mActivity.showMessage(mActivity.resources.getString(R.string.pass_common_val))
                false
            }*/
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
            /*!cbPrivacyTerms.isChecked -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.str_visit_privacy_terms))
                false
            }*/
            else -> true
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(mBatInfoReceiver)
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

    private fun callVerifyEmailAPI() {
        mActivity.isSpeedAvailable()
        val main = JsonObject()
        main.addProperty("id", 0)
        main.addProperty("Email", etSignEmail.text.toString().trim())
        main.addProperty("Username", etSignUserName.text.toString().trim())
        main.addProperty("Mobile", if (etSignPhone.text.toString().trim().isNotEmpty()) countryCode + etSignPhone.text.toString() else "")

        Utils.verifyUserData(mActivity, main, object : CommonApiListener {
            override fun commonResponse(status: Boolean, message: String, responseMessage: String, result: String) {
                if (status) {
                    gotoPaymentScreen()
                }
            }
        })
    }

    private fun callSignUpAPI(paymentTypeData: String, subscriptionId: String, paymentMethod: String) {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            var referral_name : RequestBody = "".toRequestBody()
            var promoCode : RequestBody = "".toRequestBody()
            mActivity.isSpeedAvailable()
            val androidId: String = Settings.Secure.getString(mActivity.contentResolver, Settings.Secure.ANDROID_ID)

            Comman_Methods.isProgressShow(mActivity)

            val mobile_no =
                (if (etSignPhone.text.toString().trim().isNotEmpty()) countryCode + etSignPhone.text.toString().trim() else "").toRequestBody(
                    MEDIA_TYPE_TEXT
                )
            val email = etSignEmail.text.toString().trim().toRequestBody(MEDIA_TYPE_TEXT)
            val password = etSignPassword.text.toString().toRequestBody(MEDIA_TYPE_TEXT)
            val created_by: RequestBody = "0".toRequestBody(MEDIA_TYPE_TEXT)
            val user_id: RequestBody = "0".toRequestBody(MEDIA_TYPE_TEXT)

            val user_name = etSignUserName.text.toString().trim().toRequestBody(MEDIA_TYPE_TEXT)
            val first_name = etSignFirstName.text.toString().trim().toRequestBody(MEDIA_TYPE_TEXT)
            val last_name = etSignLastName.text.toString().trim().toRequestBody(MEDIA_TYPE_TEXT)
            if (isReferralValid){
                referral_name = etSignReferralCode.text.toString().trim().toRequestBody(MEDIA_TYPE_TEXT)
            }
            if (isPromotionValid) {
                promoCode = etSignPromotion.text.toString().trim().toRequestBody(MEDIA_TYPE_TEXT)
            }
            val device_model = Comman_Methods.getdeviceModel().toRequestBody(MEDIA_TYPE_TEXT)
            val device_company = Comman_Methods.getdevicename().toRequestBody(MEDIA_TYPE_TEXT)
            val device_os = getdeviceVersion().toRequestBody(MEDIA_TYPE_TEXT)
            val device_type = "Android".toRequestBody(MEDIA_TYPE_TEXT)
            val record_status: RequestBody = "0".toRequestBody(MEDIA_TYPE_TEXT)
            val decimalSymbols = DecimalFormatSymbols.getInstance(Locale.US)
            val longitude =
                DecimalFormat("##.######", decimalSymbols).format(easyWayLocation.longitude)
                    .toRequestBody(MEDIA_TYPE_TEXT)
            val lattitude =
                DecimalFormat("##.######", decimalSymbols).format(easyWayLocation.latitude)
                    .toRequestBody(MEDIA_TYPE_TEXT)
            val battery_level = batteryLevel.toString().toRequestBody(MEDIA_TYPE_TEXT)
            val start_date = Comman_Methods.getcurrentDate().toRequestBody(MEDIA_TYPE_TEXT)
            val device_token_id = "".toRequestBody(MEDIA_TYPE_TEXT)
            val device_uuid = androidId.toRequestBody(MEDIA_TYPE_TEXT)
            val location_permission = locationPermission.toString().toRequestBody(MEDIA_TYPE_TEXT)
            val location_address = ""
//                Utils.getCompleteAddressString(mActivity, easyWayLocation.latitude, easyWayLocation.longitude)
                .toRequestBody(MEDIA_TYPE_TEXT)
            val isAdminMem: RequestBody = "false".toRequestBody(MEDIA_TYPE_TEXT)
            val device_token = "".toRequestBody(MEDIA_TYPE_TEXT)
            val device_id = "1".toRequestBody(MEDIA_TYPE_TEXT)
            val paymentType = paymentTypeData.toRequestBody(MEDIA_TYPE_TEXT)
            val loginByApp = "2".toRequestBody(MEDIA_TYPE_TEXT)
            val imageBody: RequestBody
            val isSms: RequestBody =
                (if (etSignPhone.text.toString().trim().isNotEmpty()) switchSignMessage.isChecked.toString() else "false").toRequestBody(
                    MEDIA_TYPE_TEXT
                )
            val frequency = "60".toRequestBody(MEDIA_TYPE_TEXT)
            val payment_token = subscriptionId.toRequestBody(MEDIA_TYPE_TEXT)
            val payment_method = paymentMethod.toRequestBody(MEDIA_TYPE_TEXT)
            val notification_permission: RequestBody =
                if (NotificationManagerCompat.from(mActivity).areNotificationsEnabled()) {
                    "true".toRequestBody(MEDIA_TYPE_TEXT)
                } else {
                    "false".toRequestBody(MEDIA_TYPE_TEXT)
                }
            val isChildMissing: RequestBody =
                if (isMissingChild) {
                    "true".toRequestBody(MEDIA_TYPE_TEXT)
                } else {
                    "false".toRequestBody(MEDIA_TYPE_TEXT)
                }
            part = when {
                imageFile != null -> {
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
                    created_by,
                    user_name,
                    user_id,
                    first_name,
                    last_name,
                    device_model,
                    device_company,
                    device_id,
                    device_os,
                    device_token,
                    device_type,
                    record_status,
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
                    referral_name,
                    promoCode,
                    isChildMissing,
                paymentType)

            callRegisrationApi?.enqueue(object : Callback<CommonValidationResponse> {
                override fun onFailure(call: Call<CommonValidationResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(call: Call<CommonValidationResponse>, response: Response<CommonValidationResponse>) {
                    val statusCode: Int = response.code()

                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                if (it.status == true) {
                                    AppPreference.clearPrivacyTerms(mActivity)
                                    mActivity.addFragment(LoginFragment(), false, true, animationType = AnimationType.fadeInfadeOut)
                                } else {
                                    var errorResponse: String = ""
                                    try {
                                        val gson = GsonBuilder().create()
                                        val type = object : TypeToken<String>() {}.type
                                        errorResponse = gson.fromJson(gson.toJson(it.result), type)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    val messageData = when {
                                        (it.responseMessage ?: "").isNotEmpty() -> {
                                            it.responseMessage ?: ""
                                        }
                                        errorResponse.isNotEmpty() -> {
                                            errorResponse
                                        }
                                        else -> {
                                            it.message ?: ""
                                        }
                                    }
                                    mActivity.showMessage(messageData)
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

    private fun openUploadCountryDialog(phoneCountryCodes: ArrayList<PhoneCountryCode>) {
        val inflater = layoutInflater
        val dialogLayout1 = inflater.inflate(R.layout.search_country, null)
        val mDialog = AlertDialog.Builder(activity)
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
        dialogLayout1.rvCountryNameCodeFlag.layoutManager = LinearLayoutManager(mActivity, RecyclerView.VERTICAL, false)
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

        lateinit var context: MainActivity
        private var phoneCountryCodes: ArrayList<PhoneCountryCode> = ArrayList()
        private var duplicatePhoneCountryCodes: ArrayList<PhoneCountryCode> = ArrayList()
        private var adapterPosition = -1

        constructor(context: MainActivity, phoneCountryCodes: ArrayList<PhoneCountryCode>) : this() {
            this.context = context
            this.phoneCountryCodes = phoneCountryCodes
            this.duplicatePhoneCountryCodes = ArrayList()
            this.duplicatePhoneCountryCodes.addAll(phoneCountryCodes)
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): CountrySelectionHolder {
            return CountrySelectionHolder(LayoutInflater.from(context).inflate(R.layout.raw_country_search, p0, false))
        }

        override fun getItemCount(): Int {
            return duplicatePhoneCountryCodes.size
        }

        override fun onBindViewHolder(p0: CountrySelectionHolder, p1: Int) {
            val countryName = duplicatePhoneCountryCodes[p1].countryName
            when (duplicatePhoneCountryCodes[p1].code) {
                "CA" -> p0.tvCountryDetails.text = "$countryName (+1)"
                else -> p0.tvCountryDetails.text =
                    countryName + " (" + duplicatePhoneCountryCodes[p1].countryCode + ")"
            }
            p0.tvCountryDetails.setCompoundDrawablesWithIntrinsicBounds(duplicatePhoneCountryCodes[p1].flag, 0, 0, 0)
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
                            val countryName = row.countryName
                            val countryCode = row.countryCode
                            if (countryName.lowercase().contains(charString.lowercase()) ||
                                countryCode.lowercase().contains(charString.lowercase())
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

                override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
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
                when (duplicatePhoneCountryCodes[i].code) {
                    "CA" -> tvCountrySelected.text = "+1"
                    else -> tvCountrySelected.text = duplicatePhoneCountryCodes[i].countryCode
                }
                countryCode = duplicatePhoneCountryCodes[i].countryCode
                tvCountrySelected.setCompoundDrawablesWithIntrinsicBounds(duplicatePhoneCountryCodes[i].flag, 0, 0, 0)
            }
        }
    }

    private fun showPaypalOption(
        familyMonitorResult: FamilyMonitorResult,
        subscriptionBean: SubscriptionBean
    ) {
        val view = LayoutInflater.from(mActivity)
            .inflate(R.layout.popup_paypal_layout, mActivity.window.decorView.rootView as ViewGroup, false)
        if (this::paypalBottomSheetDialog.isInitialized) {
            if (paypalBottomSheetDialog.isShowing) {
                paypalBottomSheetDialog.dismiss()
            }
        }
        paypalBottomSheetDialog = BottomSheetDialog(mActivity, R.style.appBottomSheetDialogTheme)
        paypalBottomSheetDialog.setContentView(view)
        val mBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(view.parent as View)
        mBehavior.isHideable = false
        paypalBottomSheetDialog.setOnShowListener {
            mBehavior.peekHeight = view.height
        }
        val llCreditCard: LinearLayout? = paypalBottomSheetDialog.findViewById(R.id.llCreditCard)
        val tvCreditCard: TextView? = paypalBottomSheetDialog.findViewById(R.id.tvCreditCard)
        val ivPaypal: ImageView? = paypalBottomSheetDialog.findViewById(R.id.ivPaypal)
        llCreditCard?.setOnClickListener {
            paypalBottomSheetDialog.dismiss()
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.addFragment(
                PaymentMethodFragment.newInstance(
                    familyMonitorResult,
                    subscriptionBean
                ), true, true, AnimationType.fadeInfadeOut
            )
        }
        ivPaypal?.setOnClickListener {
            paypalBottomSheetDialog.dismiss()
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)

            Comman_Methods.isCustomTrackingPopUpShow(mActivity,
                message = mActivity.resources.getString(R.string.str_paypal_fee_warning),
                positiveButtonText = mActivity.resources.getString(R.string.str_ok),
                negativeButtonText = mActivity.resources.getString(R.string.cancel),
                positiveButtonListener = object : PositiveButtonListener {
                    override fun okClickListener() {
                        Utils.callDifferentPaypalApi(mActivity, 3,
                            planId = subscriptionBean.payPalPlanId,
                            firstName = familyMonitorResult.firstName ?: "",
                            lastName = familyMonitorResult.lastName ?: "",
                            email = familyMonitorResult.email ?: "",
                            commonApiListener = object : CommonApiListener {
                                override fun onSingleSubscriptionSuccessResult(updateTimeCard: SubscriptionResponse) {
                                    val subscriptionLink = updateTimeCard.links ?: ArrayList()
                                    val redirectLink = Utils.openLinkForPayment(subscriptionLink)
                                    openPaypalPaymentScreen(redirectLink, updateTimeCard.id ?: "", subscriptionBean.subScriptionCode.toString())
                                }
                            })
                    }
            })
        }
        paypalBottomSheetDialog.show()
    }

    private fun checkPaypalSubscription(subscriptionId: String, paymentMethod: String) {
        Utils.callDifferentPaypalApi(mActivity, 2, subscriptionId, commonApiListener = object : CommonApiListener {
            override fun onSingleSubscriptionSuccessResult(updateTimeCard: SubscriptionResponse) {
                when (updateTimeCard.status) {
                    "APPROVAL_PENDING" -> {
                        val subscriptionLink = updateTimeCard.links ?: ArrayList()
                        val redirectLink = Utils.openLinkForPayment(subscriptionLink)
                    }
                    "APPROVED", "ACTIVE" -> {
                        callApi("3", updateTimeCard.id ?: "", paymentMethod)
                    }
                    "SUSPENDED", "CANCELLED", "EXPIRED" -> {
                    }
                }
            }
        })
    }

    private fun openPaypalPaymentScreen(linkUrl: String, subscriptionIdData: String, paymentMethod: String) {
        if (this@SignUpFragment::payPalPaymentFragment.isInitialized) {
            if (payPalPaymentFragment != null) {
                if (payPalPaymentFragment.isAdded) {
                    return
                }
            }
        }
        payPalPaymentFragment = PayPalPaymentFragment.newInstance(linkUrl)
        payPalPaymentFragment.paymentOptionListener = object : PaymentOptionListener {
            override fun onCreditCardOption() {}

            override fun onPayPalOption(
                subscriptionId: String, firstName: String,
                lastName: String, email: String
            ) {
                checkPaypalSubscription(subscriptionIdData, paymentMethod)
            }
        }
        payPalPaymentFragment.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.AppTheme)
        payPalPaymentFragment.show(mActivity.supportFragmentManager, "payPalPaymentFragment")
    }
}