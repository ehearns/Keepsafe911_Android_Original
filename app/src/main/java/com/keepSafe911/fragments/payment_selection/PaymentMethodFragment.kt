package com.keepSafe911.fragments.payment_selection

import AnimationType
import ValidationUtil.Companion.isRequiredField
import addFragment
import android.Manifest
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.ScrollingMovementMethod
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.keepSafe911.R
import com.keepSafe911.fragments.LoginFragment
import com.keepSafe911.fragments.commonfrag.MainBaseFragment
import com.keepSafe911.gps.GpsTracker
import com.keepSafe911.model.AsteriskPasswordTransformationMethod
import com.keepSafe911.model.FamilyMonitorResult
import com.keepSafe911.model.SubscriptionBean
import com.keepSafe911.model.response.paymentresponse.GoogleTransactionResponse
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.webservices.WebApiClient
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.keepSafe911.BuildConfig
import com.kotlinpermissions.KotlinPermissions
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.model.response.CommonValidationResponse
import com.keepSafe911.utils.*
import hideKeyboard
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltipUtils
import kotlinx.android.synthetic.main.fragment_payment_method.*
import kotlinx.android.synthetic.main.toolbar_header.*
import net.authorize.acceptsdk.AcceptSDKApiClient
import net.authorize.acceptsdk.ValidationCallback
import net.authorize.acceptsdk.datamodel.merchant.ClientKeyBasedMerchantAuthentication
import net.authorize.acceptsdk.datamodel.transaction.CardData
import net.authorize.acceptsdk.datamodel.transaction.TransactionObject
import net.authorize.acceptsdk.datamodel.transaction.TransactionType
import net.authorize.acceptsdk.datamodel.transaction.callbacks.EncryptTransactionCallback
import net.authorize.acceptsdk.datamodel.transaction.response.EncryptTransactionResponse
import net.authorize.acceptsdk.datamodel.transaction.response.ErrorTransactionResponse
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
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class PaymentMethodFragment : MainBaseFragment(), View.OnClickListener, EncryptTransactionCallback {

    private var familyMonitorResult = FamilyMonitorResult()
    private var subscriptionBean: SubscriptionBean = SubscriptionBean()
    private lateinit var dialog: AlertDialog
    private var gpstracker: GpsTracker? = null
    private var location_per = false
    lateinit var part: MultipartBody.Part
    private var passwordShowed: Boolean = false
    lateinit var appDatabase: OldMe911Database
    private var token: String = ""
    private var settingDialogShown: Boolean = false

    companion object {
        fun newInstance(
            familyMonitorResult: FamilyMonitorResult,
            subscriptionBean: SubscriptionBean
        ): PaymentMethodFragment {
            val args = Bundle()
            args.putParcelable(ARG_PARAM1, familyMonitorResult)
            args.putParcelable(ARG_PARAM2, subscriptionBean)
            val fragment = PaymentMethodFragment()
            fragment.arguments = args
            return fragment
        }

        var count: Int = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            familyMonitorResult = it.getParcelable(ARG_PARAM1) ?: FamilyMonitorResult()
            subscriptionBean = it.getParcelable(ARG_PARAM2) ?: SubscriptionBean()
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
        return inflater.inflate(R.layout.fragment_payment_method, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHeader()

        if (subscriptionBean.subScriptionCode < 1) {
            tvPaymentNote.visibility = View.INVISIBLE
        } else {
            tvPaymentNote.visibility = View.VISIBLE
        }

        gpstracker = GpsTracker(mActivity)
        appDatabase = OldMe911Database.getDatabase(mActivity)



        when (subscriptionBean.subScriptionCode) {
            1, 6 -> {
                tvPaymentNote.text = mActivity.resources.getString(R.string.free_trial_desc, subscriptionBean.subScriptionDays.toString())
                btnCardPay.text = mActivity.resources.getString(R.string.add)
            }
            2 -> {
                tvPaymentNote.text = mActivity.resources.getString(R.string.month_payment, subscriptionBean.subScriptionCost.toString())
                btnCardPay.text =
                    mActivity.resources.getString(R.string.pay) + " " + subscriptionBean.subScriptionCost + " $"
            }
            3 -> {
                tvPaymentNote.text = mActivity.resources.getString(R.string.year_payment, subscriptionBean.subScriptionCost.toString())
                btnCardPay.text =
                    mActivity.resources.getString(R.string.pay) + " " + subscriptionBean.subScriptionCost + " $"
            }
            4 -> {
                tvPaymentNote.text = mActivity.resources.getString(R.string.mem_month_payment, subscriptionBean.subScriptionCost.toString())
                btnCardPay.text =
                    mActivity.resources.getString(R.string.pay) + " " + subscriptionBean.subScriptionCost + " $"
            }
            5 -> {
                tvPaymentNote.text = mActivity.resources.getString(R.string.mem_year_payment, subscriptionBean.subScriptionCost.toString())
                btnCardPay.text =
                    mActivity.resources.getString(R.string.pay) + " " + subscriptionBean.subScriptionCost + " $"
            }
            else -> {
                tvPaymentNote.text = ""
                btnCardPay.text = mActivity.resources.getString(R.string.add)
            }
        }



        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            etFirstName.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etLastName.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etCardNumber.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etCardCVV.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        } else {
            etFirstName.imeOptions = EditorInfo.IME_ACTION_NEXT
            etLastName.imeOptions = EditorInfo.IME_ACTION_NEXT
            etCardNumber.imeOptions = EditorInfo.IME_ACTION_DONE
            etCardCVV.imeOptions = EditorInfo.IME_ACTION_DONE
        }
        clickListeners()
        etCardNumber.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                if (count <= etCardNumber.text.toString().length
                    && (etCardNumber.text.toString().length == 4
                            || etCardNumber.text.toString().length == 9
                            || etCardNumber.text.toString().length == 14)
                ) {
                    etCardNumber.setText(etCardNumber.text.toString() + " ")
                    val pos = etCardNumber.text.length
                    etCardNumber.setSelection(pos)
                } else if (count >= etCardNumber.text.toString().length
                    && (etCardNumber.text.toString().length == 4
                            || etCardNumber.text.toString().length == 9
                            || etCardNumber.text.toString().length == 14)
                ) {

                    etCardNumber.setText(
                        etCardNumber.text.toString().substring(
                            0,
                            etCardNumber.text.toString().length - 1
                        )
                    )
                    val pos = etCardNumber.text.length
                    etCardNumber.setSelection(pos)
                }
                count = etCardNumber.text.toString().length
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(char: CharSequence?, start: Int, before: Int, count: Int) {
                val type = Comman_Methods.validate(char.toString())

                if (etCardNumber.text.toString().length < 5) {
                    if (etCardCVV.text.toString().isNotEmpty()) {
                        etCardCVV.setText("")
                    }
                }

                when (type) {
                    CcnTypeEnum.AMERICAN_EXPRESS, CcnTypeEnum.DISCOVER, CcnTypeEnum.MASTERCARD -> {
                        val fArray = arrayOfNulls<InputFilter>(1)
                        fArray[0] = InputFilter.LengthFilter(4)
                        etCardCVV.filters = fArray
                    }
                    CcnTypeEnum.VISA, CcnTypeEnum.MAESTRO -> {
                        val fArray = arrayOfNulls<InputFilter>(1)
                        fArray[0] = InputFilter.LengthFilter(3)
                        etCardCVV.filters = fArray
                    }
                    else -> {
                        val fArray = arrayOfNulls<InputFilter>(1)
                        fArray[0] = InputFilter.LengthFilter(3)
                        etCardCVV.filters = fArray
                    }
                }
            }
        })


        etCardNumber.setOnTouchListener { v, event ->
            val DRAWABLE_LEFT = 0
            val DRAWABLE_TOP = 1
            val DRAWABLE_RIGHT = 2
            val DRAWABLE_BOTTOM = 3



            if (event.action == MotionEvent.ACTION_DOWN) {
                if (event.rawX >= (etCardNumber.right - etCardNumber.compoundDrawables[DRAWABLE_RIGHT].bounds.width())) {
                    mActivity.hideKeyboard()
                    if (etCardNumber.text.toString().trim().isNotEmpty()) {

                        if (passwordShowed) {
                            etCardNumber.transformationMethod = AsteriskPasswordTransformationMethod()
                            etCardNumber.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.show_card_number, 0)
                            passwordShowed = false
                        } else {
                            etCardNumber.transformationMethod = HideReturnsTransformationMethod.getInstance()
                            etCardNumber.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.gone_card_number, 0)
                            passwordShowed = true
                        }
                    }
                    true
                }
            }
            false
        }
    }

    private fun clickListeners() {
        tvCVV.setOnClickListener(this)
        btnCardPay.setOnClickListener(this)
        etCardYear.setOnClickListener(this)
        etCardMonth.setOnClickListener(this)


        etCardNumber.transformationMethod = AsteriskPasswordTransformationMethod()
    }

    private fun setHeader() {
        iv_back.visibility = View.VISIBLE
        tvHeader.text = mActivity.resources.getString(R.string.pay_method)
        Utils.setTextGradientColor(tvHeader)
        tvHeader.setPadding(0, 0, 50, 0)
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tvCVV -> {
                val tooltip = SimpleTooltip.Builder(activity)
                    .anchorView(tvCVV)
                    .gravity(Gravity.TOP)
                    .modal(true)
                    .animated(false)
                    .arrowColor(ContextCompat.getColor(mActivity, R.color.caldroid_white))
                    .animationDuration(2000)
                    .animationPadding(SimpleTooltipUtils.pxFromDp(50f))
                    .contentView(R.layout.dashboarddetail, R.id.tvDetails)
                    .focusable(true)
                    .dismissOnInsideTouch(false)
                    .transparentOverlay(true)
                    .build()

                val tvDetails: TextView = tooltip.findViewById(R.id.tvDetails)
                val tvTitle: TextView = tooltip.findViewById(R.id.tvTitle)
                tvDetails.text = mActivity.resources.getString(R.string.cvv_desc)
                tvDetails.movementMethod = ScrollingMovementMethod()
                tooltip.show()

                mActivity.hideKeyboard()
            }
            R.id.btnCardPay -> {
                mActivity.hideKeyboard()
                if (checkForValidations()) {
                    Comman_Methods.avoidDoubleClicks(v)
                    paymentApi()
                }
            }
            R.id.etCardYear -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                showNumberPickerDialog(mActivity, currentYear, currentYear + 50, etCardYear)
            }
            R.id.etCardMonth -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
                if (etCardYear.text.toString() != "") {
                    if (currentYear == etCardYear.text.toString().toInt()) {
                        if (etCardMonth.text.toString() != "") {
                            if (currentMonth >= etCardMonth.text.toString().toInt()) {
                                showNumberPickerDialog(mActivity, currentMonth, 12, etCardMonth)
                            } else {
                                showNumberPickerDialog(mActivity, 1, 12, etCardMonth)
                            }
                        }else{
                            showNumberPickerDialog(mActivity, currentMonth, 12, etCardMonth)
                        }
                    } else {
                        showNumberPickerDialog(mActivity, 1, 12, etCardMonth)
                    }
                }else{
                    showNumberPickerDialog(mActivity, 1, 12, etCardMonth)
                }
            }
            /*R.id.rbCardPayment -> {
                mActivity.hideKeyboard()

                rlCardPayment.visibility = View.VISIBLE
            }*/

        }
    }

    private fun callSetVoidApi(trans_id: String) {
        mActivity.isSpeedAvailable()

        val merchantJsonObj = JsonObject()
        merchantJsonObj.addProperty("name", BuildConfig.loginID)
        merchantJsonObj.addProperty("transactionKey", BuildConfig.transactionKey)


        val transactionJsonObj = JsonObject()
        transactionJsonObj.addProperty("transactionType", "voidTransaction")
        transactionJsonObj.addProperty("refTransId", trans_id)


        val createJsonObj = JsonObject()
        createJsonObj.add("merchantAuthentication", merchantJsonObj)
        createJsonObj.addProperty("refId", "123456")
        createJsonObj.add("transactionRequest", transactionJsonObj)


        val mainJson = JsonObject()
        mainJson.add("createTransactionRequest", createJsonObj)

        Utils.paymentRelatedApi(mActivity, mainJson, object : CommonApiListener {

            override fun paymentTransaction(transactionId: String) {
                val originalCardNumber: String = etCardNumber.text.replace("\\s".toRegex(), "")
                val apiClient = AcceptSDKApiClient
                    .Builder(mActivity, AcceptSDKApiClient.Environment.PRODUCTION)
                    .connectionTimeout(90000).build()
                val merchantAuthentication =
                    ClientKeyBasedMerchantAuthentication.createMerchantAuthentication(
                        BuildConfig.loginID,
                        BuildConfig.clientKey
                    )


                merchantAuthentication.validateMerchantAuthentication(object : ValidationCallback {
                    override fun OnValidationFailed(errorTransactionResponse: ErrorTransactionResponse?) {

                    }

                    override fun OnValidationSuccessful() {


                    }
                })


                val cardData = CardData.Builder(
                    originalCardNumber,
                    etCardMonth.text.toString(), // MM
                    etCardYear.text.toString()
                ) // YYYY
                    .cvvCode(etCardCVV.text.toString()) // Optional
                    .cardHolderName(etFirstName.text.toString().trim() + " " + etLastName.text.toString().trim())// Optional
                    .build()

                val transactionObject =
                    TransactionObject.createTransactionObject(TransactionType.SDK_TRANSACTION_ENCRYPTION)// type of transaction object
                        .cardData(cardData) // card data to be encrypted
                        .merchantAuthentication(merchantAuthentication) //Merchant authentication
                        .build()

                apiClient.getTokenWithRequest(transactionObject, this@PaymentMethodFragment)
            }

        }, true)
    }

    private fun checkForValidations(): Boolean {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        return when {
            !isRequiredField(etFirstName.text.toString().trim()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_first))
                false
            }
            !isRequiredField(etLastName.text.toString().trim()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_last))
                false
            }
            !isRequiredField(etCardNumber.text.toString()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_card))
                false
            }
            Comman_Methods.validate(etCardNumber.text.toString()) == CcnTypeEnum.INVALID -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.card_invalid))
                false
            }
            !isRequiredField(etCardMonth.text.toString()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_month))
                false
            }
            !isRequiredField(etCardYear.text.toString()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_year))
                false
            }
            !isRequiredField(etCardCVV.text.toString()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_cvv))
                false
            } etCardYear.text.toString() != "" -> {
                if (currentYear == etCardYear.text.toString().toInt()) {
                    return if (etCardMonth.text.toString() != "") {
                        if (currentMonth > etCardMonth.text.toString().toInt()) {
                            mActivity.showMessage(mActivity.resources.getString(R.string.wrong_card_month))
                            false
                        } else {
                            true
                        }
                    }else{
                        mActivity.showMessage(mActivity.resources.getString(R.string.blank_month))
                        false
                    }
                }
                true
            }
            else -> true
        }

    }

    private fun showNumberPickerDialog(activity: Activity, minValue: Int, maxValue: Int, editText: EditText) {
        val inflater = activity.layoutInflater
        val dialogLayout = inflater.inflate(R.layout.popup_payment_date, null)

        val btnSet = dialogLayout.findViewById<Button>(R.id.btnSet)
        val numberPicker = dialogLayout.findViewById<NumberPicker>(R.id.numberPicker)
        numberPicker.minValue = minValue
        numberPicker.maxValue = maxValue
        numberPicker.wrapSelectorWheel = false
        numberPicker.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS

        val mDialog = AlertDialog.Builder(activity)
        mDialog.setView(dialogLayout)
        dialog = mDialog.create()
        numberPicker.setOnValueChangedListener { picker, oldVal, newVal ->
            editText.setText(
                String.format("%02d", newVal)
            )
        }
        btnSet.setOnClickListener {
            editText.setText(String.format("%02d", numberPicker.value))
            dialog.dismiss()
        }
        dialog.setCancelable(false)
        dialog.show()
    }

    override fun onErrorReceived(error: ErrorTransactionResponse?) {
        Comman_Methods.isProgressHide()
        Comman_Methods.isPaymentShow(mActivity)
        mActivity.showMessage(error?.firstErrorMessage?.messageCode + " : " + error?.firstErrorMessage?.messageText)
    }

    override fun onEncryptionFinished(response: EncryptTransactionResponse?) {
        println("!@@@response?.dataValue = ${response?.dataValue}")

        token = response?.dataValue ?: ""
        gpstracker = GpsTracker(mActivity)
        /**
         * Payment Token Value =response?.dataValue
         */
        Comman_Methods.isPaymentShow(mActivity, 0)
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            Comman_Methods.isPaymentHide()
            callSignUpAPI()
        }, 5000)


    }

    private fun paymentApi() {
        mActivity.isSpeedAvailable()
        val originalCardNumber: String = etCardNumber.text.replace("\\s".toRegex(), "")


        val merchantJsonObj = JsonObject()
        merchantJsonObj.addProperty("name", BuildConfig.loginID)
        merchantJsonObj.addProperty("transactionKey", BuildConfig.transactionKey)

        val creditJsonObj = JsonObject()
        creditJsonObj.addProperty("cardNumber", originalCardNumber)
        creditJsonObj.addProperty(
            "expirationDate",
            etCardYear.text.toString() + "-" +etCardMonth.text.toString()
        )
        creditJsonObj.addProperty("cardCode", etCardCVV.text.toString())


        val paymentJsonObj = JsonObject()
        paymentJsonObj.add("creditCard", creditJsonObj)


        val transactionJsonObj = JsonObject()
        transactionJsonObj.addProperty("transactionType", "authOnlyTransaction")
        transactionJsonObj.addProperty("amount", subscriptionBean.subScriptionCost)
        transactionJsonObj.add("payment", paymentJsonObj)


        val createJsonObj = JsonObject()
        createJsonObj.add("merchantAuthentication", merchantJsonObj)
        createJsonObj.addProperty("refId", "123456")
        createJsonObj.add("transactionRequest", transactionJsonObj)


        val mainJson = JsonObject()
        mainJson.add("createTransactionRequest", createJsonObj)

        Utils.paymentRelatedApi(mActivity, mainJson, object : CommonApiListener {
            override fun paymentTransaction(transactionId: String) {
                callSetVoidApi(transactionId)
            }
        })
    }


    private fun paymentGooglePayApi(googlePayToken: String) {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {

            Comman_Methods.isProgressShow(mActivity)
            mActivity.isSpeedAvailable()


            val merchantJsonObj = JsonObject()
            merchantJsonObj.addProperty("name", BuildConfig.loginID)
            merchantJsonObj.addProperty("transactionKey", BuildConfig.transactionKey)

            val opaqueData_obj = JsonObject()
            opaqueData_obj.addProperty("dataDescriptor", "COMMON.ANDROID.INAPP.PAYMENT")
            opaqueData_obj.addProperty("dataValue", googlePayToken)


            val paymentJsonObj = JsonObject()
            paymentJsonObj.add("opaqueData", opaqueData_obj)


            val transactionJsonObj = JsonObject()
            transactionJsonObj.addProperty("transactionType", "authCaptureTransaction")
            transactionJsonObj.addProperty("amount", subscriptionBean.subScriptionCost)
            transactionJsonObj.add("payment", paymentJsonObj)


            val createJsonObj = JsonObject()
            createJsonObj.add("merchantAuthentication", merchantJsonObj)
            createJsonObj.addProperty("refId", "123456")
            createJsonObj.add("transactionRequest", transactionJsonObj)


            val mainJson = JsonObject()
            mainJson.add("createTransactionRequest", createJsonObj)

            val paymentApi = WebApiClient.getInstance(mActivity).webApi_payment?.googlePaymentRequest(mainJson)
            paymentApi?.enqueue(object : retrofit2.Callback<GoogleTransactionResponse> {
                override fun onFailure(call: Call<GoogleTransactionResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(call: Call<GoogleTransactionResponse>, response: Response<GoogleTransactionResponse>) {
                    if (response.isSuccessful) {
//                        Comman_Methods.isProgressHide()
                        val payres = response.body()

                        if (response.body()?.messages?.resultCode == "Ok") {
                            response.body()?.refId

                            if (response.body()?.transactionResponse?.messages!=null){
                                val apiClient = AcceptSDKApiClient
                                    .Builder(mActivity, AcceptSDKApiClient.Environment.PRODUCTION)
                                    .connectionTimeout(30000).build()
                                val merchantAuthentication =
                                    ClientKeyBasedMerchantAuthentication.createMerchantAuthentication(
                                        BuildConfig.loginID,
                                        BuildConfig.clientKey
                                    )


                                merchantAuthentication.validateMerchantAuthentication(object : ValidationCallback {
                                    override fun OnValidationFailed(errorTransactionResponse: ErrorTransactionResponse?) {

                                    }

                                    override fun OnValidationSuccessful() {


                                    }
                                })
                                val transactionObject =
                                    TransactionObject.createTransactionObject(TransactionType.SDK_TRANSACTION_ENCRYPTION)// type of transaction object
                                        .merchantAuthentication(merchantAuthentication) //Merchant authentication
                                        .build()

                                apiClient.getTokenWithRequest(transactionObject, this@PaymentMethodFragment)
                            }else{
                                Comman_Methods.isProgressHide()
                                mActivity.showMessage(mActivity.resources.getString(R.string.transaction_declined))
                            }
                        }else{
                            Comman_Methods.isProgressHide()
                            mActivity.showMessage(mActivity.resources.getString(R.string.transaction_declined))
                        }
                    } else { }
                }
            })
        } else {
            Utils.showNoInternetMessage(mActivity)
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
                        location_per = true
                        callApi()
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
                        location_per = true
                        callApi()
                    }
                }
                .onDenied {
                    setLocationPermission()
                }
                .onForeverDenied {
                    mActivity.showMessage(mActivity.resources.getString(R.string.permission_app_set))
                    Utils.showSettingsAlert(mActivity)
                }
                .ask()
        }
    }

    private fun callApi() {
        if (gpstracker?.CheckForLoCation() == true) {
            // callSignUpAPI()
        } else {
            Utils.showLocationSettingsAlert(mActivity)
        }
    }

    private fun callSignUpAPI() {

        if (ConnectionUtil.isInternetAvailable(mActivity)) {

            val androidId: String = Settings.Secure.getString(mActivity.contentResolver, Settings.Secure.ANDROID_ID)

            Comman_Methods.isProgressShow(mActivity)
            mActivity.isSpeedAvailable()
            val referral_name = (familyMonitorResult.ReferralName ?: "").toRequestBody(MEDIA_TYPE_TEXT)
            val promoCode = (familyMonitorResult.Promocode ?: "").toRequestBody(MEDIA_TYPE_TEXT)
            val mobile_no = (familyMonitorResult.mobile ?: "").toRequestBody(MEDIA_TYPE_TEXT)
            val email = (familyMonitorResult.email ?: "").toRequestBody(MEDIA_TYPE_TEXT)
            val password = (familyMonitorResult.password ?: "").toRequestBody(MEDIA_TYPE_TEXT)
            val created_by: RequestBody = "0".toRequestBody(MEDIA_TYPE_TEXT)
            val user_id: RequestBody = "0".toRequestBody(MEDIA_TYPE_TEXT)
            val user_name = (familyMonitorResult.userName ?: "").toRequestBody(MEDIA_TYPE_TEXT)
            val first_name = (familyMonitorResult.firstName ?: "").toRequestBody(MEDIA_TYPE_TEXT)
            val last_name = (familyMonitorResult.lastName ?: "").toRequestBody(MEDIA_TYPE_TEXT)
            val device_model = Comman_Methods.getdeviceModel().toRequestBody(MEDIA_TYPE_TEXT)
            val device_company = Comman_Methods.getdevicename().toRequestBody(MEDIA_TYPE_TEXT)
            val device_os = Comman_Methods.getdeviceVersion().toRequestBody(MEDIA_TYPE_TEXT)
            val device_type = "Android".toRequestBody(MEDIA_TYPE_TEXT)
            val record_status: RequestBody = "1".toRequestBody(MEDIA_TYPE_TEXT)
            val decimalSymbols = DecimalFormatSymbols.getInstance(Locale.US)
            val longitude =
                DecimalFormat("##.######", decimalSymbols).format(gpstracker?.getLongitude() ?: 0.0)
                    .toRequestBody(MEDIA_TYPE_TEXT)
            val lattitude =
                DecimalFormat("##.######", decimalSymbols).format(gpstracker?.getLatitude() ?: 0.0)
                    .toRequestBody(MEDIA_TYPE_TEXT)
            val battery_level =
                Utils.GetBatterylevel(mActivity).toString().toRequestBody(MEDIA_TYPE_TEXT)
            val start_date = Comman_Methods.getcurrentDate().toRequestBody(MEDIA_TYPE_TEXT)
            val device_token_id = "".toRequestBody(MEDIA_TYPE_TEXT)
            val device_uuid = androidId.toRequestBody(MEDIA_TYPE_TEXT)
            val location_permission = location_per.toString().toRequestBody(MEDIA_TYPE_TEXT)
            val paymentToken = token.toRequestBody(MEDIA_TYPE_TEXT)
            val payment_method =
                subscriptionBean.subScriptionCode.toString().toRequestBody(MEDIA_TYPE_TEXT)


            val isAdminMem: RequestBody = "false".toRequestBody(MEDIA_TYPE_TEXT)

            val location_address = ""
//            Utils.getCompleteAddressString(mActivity, gpstracker?.getLatitude() ?: 0.0, gpstracker?.getLongitude() ?: 0.0)
                .toRequestBody(MEDIA_TYPE_TEXT)
            val frequency = "60".toRequestBody(MEDIA_TYPE_TEXT)

            val device_token = "".toRequestBody(MEDIA_TYPE_TEXT)
            val device_id = "1".toRequestBody(MEDIA_TYPE_TEXT)
            val paymentType = "1".toRequestBody(MEDIA_TYPE_TEXT)
            val loginByApp = "2".toRequestBody(MEDIA_TYPE_TEXT)
            val imageBody: RequestBody
            val isSms: RequestBody =
                familyMonitorResult.isSms.toString().toRequestBody(MEDIA_TYPE_TEXT)
            val notification_permission: RequestBody =
                if (NotificationManagerCompat.from(mActivity).areNotificationsEnabled()) {
                    "true".toRequestBody(MEDIA_TYPE_TEXT)
                } else {
                    "false".toRequestBody(MEDIA_TYPE_TEXT)
                }
            val isChildMissing: RequestBody = "false".toRequestBody(MEDIA_TYPE_TEXT)
            part = when {
                familyMonitorResult.image != "" -> {
                    familyMonitorResult.image = File(Comman_Methods.compressImage(familyMonitorResult.image ?: "", mActivity)).absolutePath ?: familyMonitorResult.image
                    val imageFile = File(familyMonitorResult.image ?: "")
                    if (imageFile.exists()) {
                        imageBody = imageFile.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("ProfilePath", imageFile.name, imageBody)
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
                    paymentToken,
                    payment_method,
                    isAdminMem,
                    notification_permission,
                    frequency,
                    loginByApp,
                    referral_name,
                    promoCode,
                    isChildMissing,
                    paymentType)


            callRegisrationApi?.enqueue(object : retrofit2.Callback<CommonValidationResponse> {
                override fun onFailure(call: Call<CommonValidationResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(call: Call<CommonValidationResponse>, response: Response<CommonValidationResponse>) {
                    val statusCode: Int = response.code()

                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let { login_response ->
                                if (login_response.status == true) {
                                    val imageFile = File(familyMonitorResult.image ?: "")
                                    if (imageFile != null) {
                                        if (imageFile.exists()) {
                                            imageFile.delete()
                                            if (imageFile.exists()) {
                                                imageFile.canonicalFile.delete()
                                            }
                                        }
                                    }

                                    val package_text = when (subscriptionBean.subScriptionDays) {
                                        14 -> {
                                            mActivity.resources.getString(R.string.day_14)
                                        }
                                        30 -> {
                                            mActivity.resources.getString(R.string.month_1)
                                        }
                                        365 -> {
                                            mActivity.resources.getString(R.string.year_1)
                                        }
                                        else -> ""
                                    }
                                    AppPreference.clearPrivacyTerms(mActivity)

                                    mActivity.showMessage(mActivity.resources.getString(R.string.pay_message, package_text))
                                    mActivity.addFragment(
                                        LoginFragment(), false, true,
                                        animationType = AnimationType.fadeInfadeOut
                                    )


                                } else {
                                    var errorResponse: String = ""
                                    try {
                                        val gson = GsonBuilder().create()
                                        val type = object : TypeToken<String>() {}.type
                                        errorResponse = gson.fromJson(gson.toJson(login_response.result), type)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    mActivity.showMessage(errorResponse)
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

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            setHeader()
        }
    }
}