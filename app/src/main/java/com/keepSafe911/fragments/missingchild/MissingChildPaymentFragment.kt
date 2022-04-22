package com.keepSafe911.fragments.missingchild

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
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.GsonBuilder

import com.keepSafe911.R
import com.keepSafe911.model.AsteriskPasswordTransformationMethod
import com.keepSafe911.webservices.WebApiClient
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.keepSafe911.BuildConfig
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.model.response.CommonValidationResponse
import com.keepSafe911.model.response.findmissingchild.MatchResult
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import hideKeyboard
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltipUtils
import kotlinx.android.synthetic.main.background_layout.*
import kotlinx.android.synthetic.main.fragment_payment_method.*
import kotlinx.android.synthetic.main.toolbar_header.*
import kotlinx.android.synthetic.main.toolbar_header.tvHeader
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
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val ARG_PARAM3 = "param3"

class MissingChildPaymentFragment : HomeBaseFragment(), View.OnClickListener, EncryptTransactionCallback {

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    private var passwordShowed: Boolean = false
    lateinit var appDatabase: OldMe911Database
    private var token: String = ""
    private var transactionID: String = ""
    private var count: Int = 0
    private lateinit var dialog: AlertDialog
    private var paymentPrice: Double = 99.0
    private var currentYear: Int = 0
    private var currentMonth: Int = 0
    private var missingChildData: MatchResult = MatchResult()
    private var originalCardNumber: String = ""

    companion object{
        fun newInstance(
            param1: String,
            param2: String,
            param3: MatchResult
        ): MissingChildPaymentFragment {
            val args = Bundle()
            args.putString(ARG_PARAM1, param1)
            args.putString(ARG_PARAM2, param2)
            args.putParcelable(ARG_PARAM3, param3)
            val fragment = MissingChildPaymentFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            missingChildData = it.getParcelable(ARG_PARAM3) ?: MatchResult()
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

        tvPaymentNote.visibility = View.INVISIBLE
        /*btnFreqCardPay.text =
            mActivity.resources.getString(R.string.pay) + " " + paymentPrice + " $"*/
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
    override fun onClick(v: View?) {
        when (v?.id){
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
                tvDetails.text = resources.getString(R.string.cvv_desc)
                tvDetails.movementMethod = ScrollingMovementMethod()
                tooltip.show()

                mActivity.hideKeyboard()
            }
            R.id.etCardYear -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                showNumberPickerDialog(mActivity, currentYear, currentYear + 50, etCardYear)
            }
            R.id.etCardMonth -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
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
            R.id.btnCardPay -> {
                mActivity.hideKeyboard()
                if (checkForValidations()) {
                    Comman_Methods.avoidDoubleClicks(v)
                    paymentApi()
                }
            }
        }
    }

    private fun setHeader() {
        appDatabase = OldMe911Database.getDatabase(mActivity)
        paymentPrice = 99.0
        currentYear = Calendar.getInstance().get(Calendar.YEAR)
        currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        iv_back.visibility = View.VISIBLE
        tvHeader.text = mActivity.resources.getString(R.string.payment).uppercase()
        Utils.setTextGradientColor(tvHeader)
        tvHeader.setPadding(0, 0, 50, 0)
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
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
        Handler(Looper.getMainLooper()).postDelayed({
            Comman_Methods.isProgressHide()
            Comman_Methods.isPaymentHide()
            callAddMissingChildApi(token, transactionID, originalCardNumber, missingChildData)
//            mActivity.addFragment(AddMissingChildFragment.newInstance(token, transactionID, originalCardNumber, true), true, true, AnimationType.fadeInfadeOut)
        }, 5000)

    }

    private fun paymentApi() {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {

            Comman_Methods.isProgressShow(mActivity)
            mActivity.isSpeedAvailable()
            originalCardNumber = etCardNumber.text.replace("\\s".toRegex(), "")

            val merchant_json_obj = JsonObject()
            merchant_json_obj.addProperty("name", BuildConfig.loginID)
            merchant_json_obj.addProperty("transactionKey", BuildConfig.transactionKey)

            val credit_json_obj = JsonObject()
            credit_json_obj.addProperty("cardNumber", originalCardNumber)
            credit_json_obj.addProperty(
                "expirationDate",
                etCardYear.text.toString() + "-" + etCardMonth.text.toString()
            )
            credit_json_obj.addProperty("cardCode", etCardCVV.text.toString())

            val payment_json_obj = JsonObject()
            payment_json_obj.add("creditCard", credit_json_obj)

            val transaction_json_obj = JsonObject()
            transaction_json_obj.addProperty("transactionType", "authCaptureTransaction")
//            transaction_json_obj.addProperty("amount", "1.0")
            transaction_json_obj.addProperty("amount", paymentPrice)
            transaction_json_obj.add("payment", payment_json_obj)

            val create_json_obj = JsonObject()
            create_json_obj.add("merchantAuthentication", merchant_json_obj)
            create_json_obj.addProperty("refId", "123456")
            create_json_obj.add("transactionRequest", transaction_json_obj)

            val main_json = JsonObject()
            main_json.add("createTransactionRequest", create_json_obj)

            Utils.paymentRelatedApi(mActivity, main_json, object : CommonApiListener{
                override fun paymentTransaction(transactionId: String) {
                    transactionID = transactionId
                    val apiClient = AcceptSDKApiClient
                        .Builder(mActivity, AcceptSDKApiClient.Environment.PRODUCTION)
                        .connectionTimeout(90000).build()
                    val merchantAuthentication =
                        ClientKeyBasedMerchantAuthentication.createMerchantAuthentication(
                            BuildConfig.loginID,
                            BuildConfig.clientKey
                        )

                    merchantAuthentication.validateMerchantAuthentication(object :
                        ValidationCallback {
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

                    apiClient.getTokenWithRequest(transactionObject, this@MissingChildPaymentFragment)
                }
            })
        } else {
            mActivity.showMessage(mActivity.resources.getString(R.string.no_internet))
        }
    }

    private fun callAddMissingChildApi(token: String = "", transactionID: String = "", cardNumber: String = "", missingChildData: MatchResult) {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            Comman_Methods.isProgressShow(mActivity)
            mActivity.isSpeedAvailable()
            val imageBody: RequestBody
            val loginId = appDatabase.loginDao().getAll().memberID


            val missingId = (missingChildData.id ?: 0).toString().toRequestBody(MEDIA_TYPE_TEXT)
            val missingFirstName = (missingChildData.firstName ?: "").toString().toRequestBody(MEDIA_TYPE_TEXT)
            val missingLastName = (missingChildData.lastName ?: "").toString().toRequestBody(MEDIA_TYPE_TEXT)
            val missingCity = (missingChildData.missingCity ?: "").toString().toRequestBody(MEDIA_TYPE_TEXT)
            val missingState = (missingChildData.missingState ?: "").toString().toRequestBody(MEDIA_TYPE_TEXT)
            val missingDateMiss = (missingChildData.dateMissing ?: "").toString().toRequestBody(MEDIA_TYPE_TEXT)
            val missingAge = missingChildData.age.toString().toRequestBody(MEDIA_TYPE_TEXT)
            val missingToken = token.toRequestBody(MEDIA_TYPE_TEXT)
            val missingPayId = transactionID.toRequestBody(MEDIA_TYPE_TEXT)
            val purchasePassword = "".toRequestBody(MEDIA_TYPE_TEXT)
            val accountNumber = cardNumber.toRequestBody(MEDIA_TYPE_TEXT)
            val deviceType = "1".toRequestBody(MEDIA_TYPE_TEXT)
            val purchaseAmount = "99.0".toRequestBody(MEDIA_TYPE_TEXT)
            val lastSeenSituation = (missingChildData.lastSeenSituation ?: "").toString().toRequestBody(
                MEDIA_TYPE_TEXT
            )
            val contactNumber = (missingChildData.contactNumber ?: "").toString().toRequestBody(MEDIA_TYPE_TEXT)
            val hairColor = (missingChildData.hairColor ?: "").toString().toRequestBody(MEDIA_TYPE_TEXT)
            val eyeColor = (missingChildData.eyeColor ?: "").toString().toRequestBody(MEDIA_TYPE_TEXT)
            val height = String.format("%.2f",(missingChildData.height ?: 0.0)).toRequestBody(MEDIA_TYPE_TEXT)
            val weight = String.format("%.2f", (missingChildData.weight ?: 0.0)).toRequestBody(MEDIA_TYPE_TEXT)
            val complexion = (missingChildData.complexion ?: "").toString().toRequestBody(MEDIA_TYPE_TEXT)
            val isWearLenses = if (missingChildData.isWearLenses == true) "true".toRequestBody(MEDIA_TYPE_TEXT) else "false".toRequestBody(
                MEDIA_TYPE_TEXT
            )
            val isBracesOnTeeth = if (missingChildData.isBracesOnTeeth == true) "true".toRequestBody(
                MEDIA_TYPE_TEXT
            ) else "false".toRequestBody(MEDIA_TYPE_TEXT)
            val isPhysicalAttributes = if (missingChildData.isPhysicalAttributes == true) "true".toRequestBody(
                MEDIA_TYPE_TEXT
            ) else "false".toRequestBody(MEDIA_TYPE_TEXT)
            val physicalAttributes = (missingChildData.physicalAttributes ?: "").toString().toRequestBody(
                MEDIA_TYPE_TEXT
            )
            val userId = loginId.toString().toRequestBody(MEDIA_TYPE_TEXT)
            val paymentDate = Utils.getCurrentTimeStamp().toRequestBody(MEDIA_TYPE_TEXT)
            val missingChildUrl = missingChildData.imageUrl ?: ""
            val missingPart: MultipartBody.Part = when {
                missingChildUrl.trim().isNotEmpty() -> {
                    val imageFile = File(missingChildUrl)
                    if (imageFile.exists()) {
                        imageBody = imageFile.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("ImageName", imageFile.name, imageBody)
                    } else {
                        imageBody = nullData.toRequestBody("multipart/form-data".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("ImageName", null, imageBody)
                    }
                }
                else -> {
                    imageBody = nullData.toRequestBody("multipart/form-data".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("ImageName", null, imageBody)
                }
            }

            val callAddMissingChildDataApi = WebApiClient.getInstance(mActivity)
                .webApi_with_MultiPart?.addMissingChild(
                    missingId,
                    missingFirstName,
                    missingLastName,
                    missingCity,
                    missingState,
                    missingDateMiss,
                    missingAge,
                    missingToken,
                    missingPayId,
                    purchasePassword,
                    purchaseAmount,
                    paymentDate,
                    accountNumber,
                    deviceType,
                    userId,
                    lastSeenSituation,
                    contactNumber,
                    hairColor,
                    eyeColor,
                    height,
                    weight,
                    complexion,
                    isWearLenses,
                    isBracesOnTeeth,
                    isPhysicalAttributes,
                    physicalAttributes,
                    missingPart)

            callAddMissingChildDataApi?.enqueue(object: retrofit2.Callback<CommonValidationResponse>{
                override fun onResponse(
                    call: Call<CommonValidationResponse>,
                    response: Response<CommonValidationResponse>
                ) {
                    val statusCode: Int = response.code()

                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                mActivity.showMessage(it.responseMessage ?: "")
                                if (it.status == true) {
//                                    mActivity.onBackPressed()
                                    val gson: Gson = GsonBuilder().create()
                                    val responseTypeToken: TypeToken<MatchResult> =
                                        object :
                                            TypeToken<MatchResult>() {}
                                    val responseData: MatchResult? =
                                        gson.fromJson(
                                            gson.toJson(it.result),
                                            responseTypeToken.type
                                        )
                                    val missingChildResult = responseData ?: MatchResult()

                                    mActivity.addFragment(MissingChildTaskFragment.newInstance(
                                        true, missingChildResult.id ?: 0,
                                        missingChildResult.lstChildTaskResponse ?: ArrayList(), missingChildResult.userId ?: 0),
                                        true, true,
                                        animationType = AnimationType.fadeInfadeOut)
                                }
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        Utils.showSomeThingWrongMessage(mActivity)
                    }
                }

                override fun onFailure(call: Call<CommonValidationResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }
            })
        } else {
            mActivity.showMessage(mActivity.resources.getString(R.string.no_internet))
        }
    }
}