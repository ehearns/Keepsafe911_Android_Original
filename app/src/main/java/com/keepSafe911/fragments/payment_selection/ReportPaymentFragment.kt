package com.keepSafe911.fragments.payment_selection


import ValidationUtil.Companion.isRequiredField
import addFragment
import android.app.Activity
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat

import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.fragments.homefragment.profile.ReportPaymentHistoryFragment
import com.keepSafe911.gps.GpsJobService
import com.keepSafe911.gps.GpsService
import com.keepSafe911.model.AsteriskPasswordTransformationMethod
import com.keepSafe911.model.MemberBean
import com.keepSafe911.model.response.ApiResponse
import com.keepSafe911.model.response.FrequencyHistoryResult
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import com.keepSafe911.webservices.WebApiClient
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.keepSafe911.BuildConfig
import com.keepSafe911.listner.CommonApiListener
import hideKeyboard
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltipUtils
import isAppIsInBackground
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
private const val ARG_PARAM5 = "param5"
private const val ARG_PARAM6 = "param6"

class ReportPaymentFragment : HomeBaseFragment(), View.OnClickListener, EncryptTransactionCallback {

    private var passwordShowed: Boolean = false
    lateinit var appDatabase: OldMe911Database
    private var token: String = ""
    private var transactionID: String = ""
    private var count: Int = 0
    private lateinit var dialog: AlertDialog
    private var paymentMemberList: ArrayList<MemberBean> = ArrayList()
    private var frequencyRange: Int = 0
    private var paymentPrice: Double = 0.0
    private var paymentType: Int = 0
    private var frequencyPremiumReportList: ArrayList<FrequencyHistoryResult> = ArrayList()
    private var currentYear: Int = 0
    private var currentMonth: Int = 0
    private var frequencyPosition: Int = -1

    companion object{
        fun newInstance(
            frequencyRange: Int,
            paymentMemberList: ArrayList<MemberBean>,
            paymentPrice: Double,
            paymentType : Int,
            frequencyPremiumReportList: ArrayList<FrequencyHistoryResult>,
            frequencyPosition: Int
        ): ReportPaymentFragment {
            val args = Bundle()
            args.putInt(ARG_PARAM1, frequencyRange)
            args.putParcelableArrayList(ARG_PARAM2, paymentMemberList)
            args.putDouble(ARG_PARAM3, paymentPrice)
            args.putInt(ARG_PARAM4,paymentType)
            args.putParcelableArrayList(ARG_PARAM5,frequencyPremiumReportList)
            args.putInt(ARG_PARAM6,frequencyPosition)
            val fragment = ReportPaymentFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            frequencyRange = it.getInt(ARG_PARAM1, 0)
            paymentMemberList = it.getParcelableArrayList(ARG_PARAM2) ?: ArrayList()
            paymentPrice = it.getDouble(ARG_PARAM3,0.0)
            paymentType = it.getInt(ARG_PARAM4,0)
            frequencyPremiumReportList = it.getParcelableArrayList(ARG_PARAM5) ?: ArrayList()
            frequencyPosition = it.getInt(ARG_PARAM6,-1)
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
        appDatabase = OldMe911Database.getDatabase(mActivity)
        setHeader()

        tvPaymentNote.visibility = View.INVISIBLE
        btnCardPay.text = mActivity.resources.getString(R.string.pay) + " " + paymentPrice + " $"
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
                    if (!etCardNumber.text.toString().trim().isEmpty()) {

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
        mActivity.disableDrawer()
        currentYear = Calendar.getInstance().get(Calendar.YEAR)
        currentMonth = Calendar.getInstance().get(Calendar.MONTH)
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
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            Comman_Methods.isPaymentHide()
            callFrequencyPremiumReport(token, transactionID)
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
            etCardYear.text.toString() +"-"+ etCardMonth.text.toString()
        )
        creditJsonObj.addProperty("cardCode", etCardCVV.text.toString())

        val paymentJsonObj = JsonObject()
        paymentJsonObj.add("creditCard", creditJsonObj)

        val transactionJsonObj = JsonObject()
        transactionJsonObj.addProperty("transactionType", "authCaptureTransaction")
//            transactionJsonObj.addProperty("amount", "1.0")
        transactionJsonObj.addProperty("amount", paymentPrice)
        transactionJsonObj.add("payment", paymentJsonObj)

        val createJsonObj = JsonObject()
        createJsonObj.add("merchantAuthentication", merchantJsonObj)
        createJsonObj.addProperty("refId", "123456")
        createJsonObj.add("transactionRequest", transactionJsonObj)

        val mainJson = JsonObject()
        mainJson.add("createTransactionRequest", createJsonObj)

        Utils.paymentRelatedApi(mActivity, mainJson, object : CommonApiListener {

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

                apiClient.getTokenWithRequest(transactionObject, this@ReportPaymentFragment)
            }

        })
    }

    private fun callFrequencyPremiumReport(token: String, transactionId: String) {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
//            Comman_Methods.isProgressShow(mActivity)
            mActivity.isSpeedAvailable()
            val jsonArray = JsonArray()
            for (i in 0 until paymentMemberList.size){
                jsonArray.add(paymentMemberList[i].id)
            }
            val jsonObject = JsonObject()
            jsonObject.addProperty("ID",0)
            jsonObject.add("MemberID", jsonArray)
            jsonObject.addProperty("Frequency", frequencyRange)
            jsonObject.addProperty("PaymentType", paymentType)
            jsonObject.addProperty("Amount", paymentPrice)
            jsonObject.addProperty("PaymentDate", Utils.getCurrentTimeStamp())
            jsonObject.addProperty("Token", token)
            jsonObject.addProperty("PayeID", transactionId)
            jsonObject.addProperty("AdminId", appDatabase.loginDao().getAll().adminID)
            jsonObject.addProperty("IsPayment",true)

            val callPremiumFrequencyReport = WebApiClient.getInstance(mActivity).webApi_without?.
            frequencyPremiumReport(jsonObject)
            callPremiumFrequencyReport?.enqueue(object : retrofit2.Callback<ApiResponse>{
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            if (response.body()?.status == true) {
                                var freq: Int = 0
                                for (i in 0 until paymentMemberList.size) {
                                    if (paymentMemberList[i].id == appDatabase.loginDao().getAll().memberID) {

                                        val loginupdate: LoginObject =
                                            appDatabase.loginDao().getAll()
                                        loginupdate.frequency = frequencyRange
                                        loginupdate.isReport = true
                                        appDatabase.loginDao().updateLogin(loginupdate)

                                        val isServiceStarted =
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                Utils.isJobServiceRunning(mActivity)
                                            } else {
                                                Utils.isMyServiceRunning(
                                                    GpsService::class.java,
                                                    mActivity
                                                )
                                            }
                                        if (isServiceStarted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                            val jobScheduler =
                                                mActivity.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                                            jobScheduler.cancel(GPSSERVICEJOBID)
                                        } else if (isServiceStarted) {
                                            mActivity.stopService(
                                                Intent(
                                                    mActivity,
                                                    GpsService::class.java
                                                )
                                            )
                                        }
                                        val isServiceStoped =
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                Utils.isJobServiceRunning(mActivity)
                                            } else {
                                                Utils.isMyServiceRunning(
                                                    GpsService::class.java,
                                                    mActivity
                                                )
                                            }
                                        try {
                                            if (isAppIsInBackground(mActivity)) {
                                                if (!isServiceStoped && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                    val jobScheduler =
                                                        mActivity.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                                                    val componentName = ComponentName(
                                                        mActivity.packageName,
                                                        GpsJobService::class.java.name
                                                    )
                                                    val jobInfo =
                                                        JobInfo.Builder(
                                                            GPSSERVICEJOBID,
                                                            componentName
                                                        )
                                                            .setMinimumLatency(1000)
                                                            .setOverrideDeadline((241 * 60000).toLong())
                                                            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                                                            .setPersisted(true).build()
                                                    val resultCode = jobScheduler.schedule(jobInfo)
                                                    if (resultCode == JobScheduler.RESULT_SUCCESS) {
                                                        Log.d(
                                                            GpsJobService::class.java.name,
                                                            "job scheduled"
                                                        )
                                                    } else {
                                                        Log.d(
                                                            GpsJobService::class.java.name,
                                                            "job schedule failed"
                                                        )
                                                    }
                                                } else if (!isServiceStoped) {
                                                    mActivity.startService(
                                                        Intent(
                                                            mActivity,
                                                            GpsService::class.java
                                                        )
                                                    )
                                                }
                                            } else {
                                                if (!isServiceStoped && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                    val jobScheduler =
                                                        mActivity.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                                                    val componentName = ComponentName(
                                                        mActivity.packageName,
                                                        GpsJobService::class.java.name
                                                    )
                                                    val jobInfo =
                                                        JobInfo.Builder(
                                                            GPSSERVICEJOBID,
                                                            componentName
                                                        )
                                                            .setMinimumLatency(1000)
                                                            .setOverrideDeadline((241 * 60000).toLong())
                                                            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                                                            .setPersisted(true).build()
                                                    val resultCode = jobScheduler.schedule(jobInfo)
                                                    if (resultCode == JobScheduler.RESULT_SUCCESS) {
                                                        Log.d(
                                                            GpsJobService::class.java.name,
                                                            "job scheduled"
                                                        )
                                                    } else {
                                                        Log.d(
                                                            GpsJobService::class.java.name,
                                                            "job schedule failed"
                                                        )
                                                    }
                                                } else if (!isServiceStoped) {
                                                    mActivity.startService(
                                                        Intent(
                                                            mActivity,
                                                            GpsService::class.java
                                                        )
                                                    )
                                                }
                                            }
                                        } catch (e: java.lang.Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }

                                mActivity.addFragment(
                                    ReportPaymentHistoryFragment(),
                                    true,
                                    true,
                                    animationType = AnimationType.fadeInfadeOut
                                )
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        mActivity.showMessage(mActivity.resources.getString(R.string.error_message))
                    }
                }
            })
        } else {
            Utils.showNoInternetMessage(mActivity)
        }
    }
}