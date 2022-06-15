package com.keepSafe911.fragments.payment_selection

import ValidationUtil.Companion.isRequiredField
import addFragment
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.core.content.ContextCompat
import com.keepSafe911.R
import com.keepSafe911.model.AsteriskPasswordTransformationMethod
import com.keepSafe911.model.SubscriptionBean
import com.keepSafe911.model.response.paymentresponse.GoogleTransactionResponse
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.webservices.WebApiClient
import com.google.gson.JsonObject
import com.keepSafe911.BuildConfig
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.fragments.homefragment.profile.PaymentFragment
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.model.FamilyMonitorResult
import com.keepSafe911.model.response.GetFamilyMonitoringResponse
import com.keepSafe911.model.roomobj.LoginObject
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
import retrofit2.Call
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val ARG_PARAM3 = "param3"
private const val ARG_PARAM4 = "param4"

class RemainPaymentMethodFragment : HomeBaseFragment(), View.OnClickListener, EncryptTransactionCallback {

    private var showFreeTrial: Boolean = false
    private var isFromMember: Boolean = false
    private var subscriptionBean: SubscriptionBean = SubscriptionBean()
    private var familyMonitorResult: FamilyMonitorResult = FamilyMonitorResult()
    private lateinit var dialog: AlertDialog
    private var passwordShowed: Boolean = false
    lateinit var appDatabase: OldMe911Database
    private var token: String = ""


    companion object {
        fun newInstance(
            showFreeTrial: Boolean = false,
            subscriptionBean: SubscriptionBean = SubscriptionBean(),
            isFromMember: Boolean = false,
            memberDetail: FamilyMonitorResult = FamilyMonitorResult()
        ): RemainPaymentMethodFragment {
            val args = Bundle()
            args.putBoolean(ARG_PARAM1, showFreeTrial)
            args.putParcelable(ARG_PARAM2, subscriptionBean)
            args.putBoolean(ARG_PARAM3, isFromMember)
            args.putParcelable(ARG_PARAM4, memberDetail)
            val fragment = RemainPaymentMethodFragment()
            fragment.arguments = args
            return fragment
        }

        var count: Int = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            showFreeTrial = it.getBoolean(ARG_PARAM1, false)
            subscriptionBean = it.getParcelable(ARG_PARAM2) ?: SubscriptionBean()
            isFromMember = it.getBoolean(ARG_PARAM3, false)
            familyMonitorResult = it.getParcelable(ARG_PARAM4) ?: FamilyMonitorResult()
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
        mActivity.disableDrawer()
        setHeader()

        if (subscriptionBean.subScriptionCode < 1) {
            tvPaymentNote.visibility = View.INVISIBLE
        } else {
            tvPaymentNote.visibility = View.VISIBLE
        }

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
        mActivity.checkUserActive()
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

                apiClient.getTokenWithRequest(transactionObject, this@RemainPaymentMethodFragment)
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
        println("!@@@response.dataValue = ${response?.dataValue}")

        token = response?.dataValue ?: ""
        /**
         * Payment Token Value =response.dataValue
         */
        Comman_Methods.isPaymentShow(mActivity, 0)
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            Comman_Methods.isPaymentHide()
            callIPhoneUserSubscription()
        }, 5000)


    }

    private fun callIPhoneUserSubscription() {
        mActivity.callRemainSubscriptionApi(token,
            subscriptionBean, 1,
            familyMonitorResult, isFromMember,
            etFirstName.text.toString().trim(), etLastName.text.toString().trim())
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
            etCardYear.text.toString() +"-"+ etCardMonth.text.toString()
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

                                apiClient.getTokenWithRequest(transactionObject, this@RemainPaymentMethodFragment)
                            }else{
                                Comman_Methods.isProgressHide()
                                mActivity.showMessage(mActivity.resources.getString(R.string.transaction_declined))
                            }
                        }else{
                            Comman_Methods.isProgressHide()
                            mActivity.showMessage(mActivity.resources.getString(R.string.transaction_declined))
                        }
                    } else {

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