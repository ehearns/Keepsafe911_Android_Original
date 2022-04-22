package com.keepSafe911.fragments.donation

import ValidationUtil.Companion.isRequiredField
import addFragment
import android.app.Activity
import android.content.Context
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
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject
import com.keepSafe911.BuildConfig
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.model.AsteriskPasswordTransformationMethod
import com.keepSafe911.model.response.CommonValidationResponse
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import com.keepSafe911.webservices.WebApiClient
import hideKeyboard
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltipUtils
import kotlinx.android.synthetic.main.background_layout.*
import kotlinx.android.synthetic.main.fragment_donation.*
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

class DonationFragment : HomeBaseFragment(), View.OnClickListener, EncryptTransactionCallback {
    private var param1: String? = ""
    private var param2: String? = ""
    private var passwordShowed: Boolean = false
    lateinit var appDatabase: OldMe911Database
    private var token: String = ""
    private lateinit var dialog: AlertDialog
    private var currentYear: Int = 0
    private var currentMonth: Int = 0
    private var transactionID: String = ""
    private var originalCardNumber: String = ""
    private var donationAmount: Double = 5.0

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1, "")
            param2 = it.getString(ARG_PARAM2, "")
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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_donation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHeader()
    }

    private fun setHeader() {
        appDatabase = OldMe911Database.getDatabase(mActivity)
        currentYear = Calendar.getInstance().get(Calendar.YEAR)
        currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        donationAmount = 5.0
        mActivity.disableDrawer()
        tvHeader.setPadding(0, 0, 50, 0)
        tvHeader.text = mActivity.resources.getString(R.string.str_donation).uppercase()
        Utils.setTextGradientColor(tvHeader)
        iv_back.visibility = View.VISIBLE
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
        btnCardDonation.setOnClickListener(this)
        tvDonationCVV.setOnClickListener(this)
        etCardDonationMonth.setOnClickListener(this)
        etCardDonationYear.setOnClickListener(this)

        etDonationCardNumber.transformationMethod = AsteriskPasswordTransformationMethod()

        etDonationCardNumber.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                if (count <= etDonationCardNumber.text.toString().length
                    && (etDonationCardNumber.text.toString().length == 4
                            || etDonationCardNumber.text.toString().length == 9
                            || etDonationCardNumber.text.toString().length == 14)
                ) {
                    etDonationCardNumber.setText(etDonationCardNumber.text.toString() + " ")
                    val pos = etDonationCardNumber.text.length
                    etDonationCardNumber.setSelection(pos)
                } else if (count >= etDonationCardNumber.text.toString().length
                    && (etDonationCardNumber.text.toString().length == 4
                            || etDonationCardNumber.text.toString().length == 9
                            || etDonationCardNumber.text.toString().length == 14)
                ) {

                    etDonationCardNumber.setText(
                        etDonationCardNumber.text.toString().substring(
                            0,
                            etDonationCardNumber.text.toString().length - 1
                        )
                    )
                    val pos = etDonationCardNumber.text.length
                    etDonationCardNumber.setSelection(pos)
                }
                count = etDonationCardNumber.text.toString().length
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(char: CharSequence?, start: Int, before: Int, count: Int) {
                val type = Comman_Methods.validate(char.toString())

                if (etDonationCardNumber.text.toString().length < 5) {
                    if (etCardDonationCVV.text.toString().isNotEmpty()) {
                        etCardDonationCVV.setText("")
                    }
                }

                when (type) {
                    CcnTypeEnum.AMERICAN_EXPRESS, CcnTypeEnum.DISCOVER, CcnTypeEnum.MASTERCARD -> {
                        val fArray = arrayOfNulls<InputFilter>(1)
                        fArray[0] = InputFilter.LengthFilter(4)
                        etCardDonationCVV.filters = fArray
                    }
                    CcnTypeEnum.VISA, CcnTypeEnum.MAESTRO -> {
                        val fArray = arrayOfNulls<InputFilter>(1)
                        fArray[0] = InputFilter.LengthFilter(3)
                        etCardDonationCVV.filters = fArray
                    }
                    else -> {
                        val fArray = arrayOfNulls<InputFilter>(1)
                        fArray[0] = InputFilter.LengthFilter(3)
                        etCardDonationCVV.filters = fArray
                    }
                }
            }
        })


        etDonationCardNumber.setOnTouchListener { v, event ->
            val DRAWABLE_LEFT = 0
            val DRAWABLE_TOP = 1
            val DRAWABLE_RIGHT = 2
            val DRAWABLE_BOTTOM = 3



            if (event.action == MotionEvent.ACTION_DOWN) {
                if (event.rawX >= (etDonationCardNumber.right - etDonationCardNumber.compoundDrawables[DRAWABLE_RIGHT].bounds.width())) {
                    mActivity.hideKeyboard()
                    if (etDonationCardNumber.text.toString().trim().isNotEmpty()) {

                        if (passwordShowed) {
                            etDonationCardNumber.transformationMethod = AsteriskPasswordTransformationMethod()
                            etDonationCardNumber.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.show_card_number, 0)
                            passwordShowed = false
                        } else {
                            etDonationCardNumber.transformationMethod = HideReturnsTransformationMethod.getInstance()
                            etDonationCardNumber.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.gone_card_number, 0)
                            passwordShowed = true
                        }
                    }
                    true
                }
            }
            false
        }

        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            etDonationFirstName.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etDonationLastName.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etDonationAmount.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etDonationCardNumber.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etCardDonationCVV.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        } else {
            etDonationFirstName.imeOptions = EditorInfo.IME_ACTION_NEXT
            etDonationLastName.imeOptions = EditorInfo.IME_ACTION_NEXT
            etDonationAmount.imeOptions = EditorInfo.IME_ACTION_DONE
            etDonationCardNumber.imeOptions = EditorInfo.IME_ACTION_DONE
            etCardDonationCVV.imeOptions = EditorInfo.IME_ACTION_DONE
        }
        val donationAmountList: ArrayList<String> = ArrayList()
        donationAmountList.add("5")
        donationAmountList.add("10")
        donationAmountList.add("25")
        donationAmountList.add("50")
        donationAmountList.add("100")
        donationAmountList.add("300")
        donationAmountList.add("500")
        donationAmountList.add("Other")
        rvDonationAmount.layoutManager = GridLayoutManager(mActivity, 4, RecyclerView.VERTICAL, false)
        rvDonationAmount.adapter = DonationPaymentAdapter(mActivity, donationAmountList)
        mActivity.checkUserActive()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            setHeader()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            DonationFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }

        var count: Int = 0

    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnCardDonation -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                if (checkForValidations()) {
                    if (etDonationAmount.visibility == View.VISIBLE && isRequiredField(
                            etDonationAmount.text.toString().trim()
                        ) && (etDonationAmount.text.toString().toInt() > 0)
                    ) {
                        donationAmount = etDonationAmount.text.toString().toDouble()
                    }
                    paymentApi()
                }
            }
            R.id.tvDonationCVV -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                val tooltip = SimpleTooltip.Builder(activity)
                    .anchorView(tvDonationCVV)
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
            }
            R.id.etCardDonationMonth -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                if (etCardDonationYear.text.toString() != "") {
                    if (currentYear == etCardDonationYear.text.toString().toInt()) {
                        if (etCardDonationMonth.text.toString() != "") {
                            if (currentMonth >= etCardDonationMonth.text.toString().toInt()) {
                                showNumberPickerDialog(mActivity, currentMonth, 12, etCardDonationMonth)
                            } else {
                                showNumberPickerDialog(mActivity, 1, 12, etCardDonationMonth)
                            }
                        }else{
                            showNumberPickerDialog(mActivity, currentMonth, 12, etCardDonationMonth)
                        }
                    } else {
                        showNumberPickerDialog(mActivity, 1, 12, etCardDonationMonth)
                    }
                }else{
                    showNumberPickerDialog(mActivity, 1, 12, etCardDonationMonth)
                }
            }
            R.id.etCardDonationYear -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                showNumberPickerDialog(mActivity, currentYear, currentYear + 50, etCardDonationYear)
            }
        }
    }

    private fun checkForValidations(): Boolean {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        return when {
            etDonationAmount.visibility == View.VISIBLE && !isRequiredField(etDonationAmount.text.toString().trim()) -> {
                mActivity.showMessage("Enter some amount for donate.")
                false
            }
            etDonationAmount.visibility == View.VISIBLE && isRequiredField(etDonationAmount.text.toString().trim()) && (etDonationAmount.text.toString().toInt() <= 0) -> {
                mActivity.showMessage("Enter valid amount for donate.")
                false
            }
            !isRequiredField(etDonationFirstName.text.toString().trim()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_first))
                false
            }
            !isRequiredField(etDonationLastName.text.toString().trim()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_last))
                false
            }
            !isRequiredField(etDonationCardNumber.text.toString()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_card))
                false
            }
            Comman_Methods.validate(etDonationCardNumber.text.toString()) == CcnTypeEnum.INVALID -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.card_invalid))
                false
            }
            !isRequiredField(etCardDonationMonth.text.toString()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_month))
                false
            }
            !isRequiredField(etCardDonationYear.text.toString()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_year))
                false
            }
            !isRequiredField(etCardDonationCVV.text.toString()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_cvv))
                false
            }
            etCardDonationYear.text.toString() != "" -> {
                if (currentYear == etCardDonationYear.text.toString().toInt()) {
                    return if (etCardDonationMonth.text.toString() != "") {
                        if (currentMonth > etCardDonationMonth.text.toString().toInt()) {
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
        /**
         * Payment Token Value =response.dataValue
         */
        Comman_Methods.isPaymentShow(mActivity, 0)
        Handler(Looper.getMainLooper()).postDelayed({
            Comman_Methods.isProgressHide()
            Comman_Methods.isPaymentHide()
            val accountNumber = if (originalCardNumber.length >= 4) originalCardNumber.takeLast(4) else ""
            donateAmountChild(accountNumber)
        }, 5000)


    }

    private fun paymentApi() {
        mActivity.isSpeedAvailable()
        originalCardNumber = etDonationCardNumber.text.replace("\\s".toRegex(), "")


        val merchantJsonObj = JsonObject()
        merchantJsonObj.addProperty("name", BuildConfig.loginID)
        merchantJsonObj.addProperty("transactionKey", BuildConfig.transactionKey)

        val creditJsonObj = JsonObject()
        creditJsonObj.addProperty("cardNumber", originalCardNumber)
        creditJsonObj.addProperty(
            "expirationDate",
            etCardDonationYear.text.toString() +"-"+ etCardDonationMonth.text.toString()
        )
        creditJsonObj.addProperty("cardCode", etCardDonationCVV.text.toString())


        val paymentJsonObj = JsonObject()
        paymentJsonObj.add("creditCard", creditJsonObj)


        val transactionJsonObj = JsonObject()
        transactionJsonObj.addProperty("transactionType", "authCaptureTransaction")
//        transactionJsonObj.addProperty("amount", "1.0")
        transactionJsonObj.addProperty("amount", donationAmount)
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
                    etCardDonationMonth.text.toString(), // MM
                    etCardDonationYear.text.toString()
                ) // YYYY
                    .cvvCode(etCardDonationCVV.text.toString()) // Optional
                    .cardHolderName(etDonationFirstName.text.toString().trim() + " " + etDonationLastName.text.toString().trim())// Optional
                    .build()

                val transactionObject =
                    TransactionObject.createTransactionObject(TransactionType.SDK_TRANSACTION_ENCRYPTION)// type of transaction object
                        .cardData(cardData) // card data to be encrypted
                        .merchantAuthentication(merchantAuthentication) //Merchant authentication
                        .build()

                apiClient.getTokenWithRequest(transactionObject, this@DonationFragment)
            }

        })
    }

    inner class DonationPaymentAdapter(private val context: Context, private var followUsImageArrayList: ArrayList<String>): RecyclerView.Adapter<DonationPaymentAdapter.DonationPaymentHolder>() {

        private var defaultSelectedPosition: Int = 0

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DonationPaymentHolder {
            return DonationPaymentHolder(LayoutInflater.from(context).inflate(R.layout.raw_donation_amount, parent, false))
        }

        override fun onBindViewHolder(holder: DonationPaymentHolder, position: Int) {
            val weight: Int = Utils.calculateNoOfColumns(context, 4.4)

            val layoutParams = holder.tvDonationAmount.layoutParams
            layoutParams.width = weight
            holder.tvDonationAmount.layoutParams = layoutParams

            if (defaultSelectedPosition == position) {
                holder.tvDonationAmount.setTextColor(ContextCompat.getColor(mActivity, R.color.caldroid_white))
                holder.tvDonationAmount.setBackgroundResource(R.drawable.donation_back)
            } else {
                holder.tvDonationAmount.setTextColor(ContextCompat.getColor(mActivity, R.color.colorPrimary))
                holder.tvDonationAmount.setBackgroundResource(R.drawable.edittext_white_back)
            }

            holder.tvDonationAmount.text = if (followUsImageArrayList.size - 1 == position) {
                followUsImageArrayList[position]
            } else {
                "$" + followUsImageArrayList[position]
            }
            holder.clDonationPayment.setOnClickListener {
                when (position) {
                    followUsImageArrayList.size - 1 -> {
                        etDonationAmount.visibility = View.VISIBLE
                    }
                    else -> {
                        etDonationAmount.visibility = View.GONE
                        donationAmount = followUsImageArrayList[position].toDouble()
                    }
                }
                defaultSelectedPosition = position
                notifyDataSetChanged()
            }
        }

        override fun getItemCount(): Int {
            return followUsImageArrayList.size
        }

        inner class DonationPaymentHolder(view: View): RecyclerView.ViewHolder(view){
            var tvDonationAmount: TextView = view.findViewById(R.id.tvDonationAmount)
            var clDonationPayment: ConstraintLayout = view.findViewById(R.id.clDonationPayment)
        }
    }

    private fun donateAmountChild(cardNumber: String) {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            val loginId = appDatabase.loginDao().getAll().memberID
            val donationObject = JsonObject()
            donationObject.addProperty("UserId", loginId)
            donationObject.addProperty("Token", token)
            donationObject.addProperty("InAppPurchasePassword", "")
            donationObject.addProperty("PayeID", transactionID)
            donationObject.addProperty("PayID", transactionID)
            donationObject.addProperty("Amount", donationAmount)
            donationObject.addProperty("PaymentDate", Utils.getCurrentTimeStamp())
            donationObject.addProperty("AccountNumber", cardNumber)
            donationObject.addProperty("DeviceType", 1)
            Comman_Methods.isProgressShow(mActivity)

            val callDonationAmount = WebApiClient.getInstance(mActivity).webApi_without?.childDonation(donationObject)
            callDonationAmount?.enqueue(object: retrofit2.Callback<CommonValidationResponse>{
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
                                    mActivity.addFragment(
                                        ThankYouFragment.newInstance(
                                            donationAmount.toString(),
                                            cardNumber
                                        ),
                                        true, true, AnimationType.fadeInfadeOut)
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
            Utils.showNoInternetMessage(mActivity)
        }
    }
}