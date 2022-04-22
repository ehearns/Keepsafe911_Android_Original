package com.keepSafe911.fragments.donation

import addFragment
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.keepSafe911.BuildConfig
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.model.response.findmissingchild.DonationHistoryResponse
import com.keepSafe911.model.response.findmissingchild.DonationHistoryResult
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import com.keepSafe911.webservices.WebApiClient
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_donation_history.*
import kotlinx.android.synthetic.main.toolbar_header.*
import retrofit2.Call
import retrofit2.Response
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class DonationHistoryFragment : HomeBaseFragment(), View.OnClickListener {

    private var param1: String? = ""
    private var param2: String? = ""
    lateinit var appDatabase: OldMe911Database
    private var donationHistoryList: ArrayList<DonationHistoryResult> = ArrayList()

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
        return inflater.inflate(R.layout.fragment_donation_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHeader()
        val addGeofenceParam = ivAddDonation.layoutParams as ViewGroup.MarginLayoutParams
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

    private fun setHeader() {
        appDatabase = OldMe911Database.getDatabase(mActivity)
        mActivity.enableDrawer()
        tvHeader.setPadding(0, 0, 50, 0)
        tvHeader.text = mActivity.resources.getString(R.string.str_donation).uppercase()
        Utils.setTextGradientColor(tvHeader)
        iv_menu.visibility = View.VISIBLE
        iv_menu.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.openDrawer()
        }
        ivAddDonation.setOnClickListener(this)
        rvDonationHistory.layoutManager = LinearLayoutManager(mActivity, RecyclerView.VERTICAL, false)
        val userId = appDatabase.loginDao().getAll().memberID
        callDonationHistoryApi(userId)
        mActivity.checkUserActive()
    }

    private fun callDonationHistoryApi(userId: Int) {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            Comman_Methods.isProgressShow(mActivity)
            mActivity.isSpeedAvailable()
            val donationHistoryApi = WebApiClient.getInstance(mActivity).webApi_without?.donationHistory(userId)
            donationHistoryApi?.enqueue(object : retrofit2.Callback<DonationHistoryResponse> {
                override fun onResponse(
                    call: Call<DonationHistoryResponse>,
                    response: Response<DonationHistoryResponse>
                ) {
                    donationHistoryList = ArrayList()
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                if (it.status == true) {
                                    donationHistoryList = it.result ?: ArrayList()
                                }
                            }
                        } else{
                            Comman_Methods.isProgressHide()
                        }
                        setHistoryAdapter(donationHistoryList)
                    } else {
                        Comman_Methods.isProgressHide()
                        Utils.showSomeThingWrongMessage(mActivity)
                    }
                }

                override fun onFailure(call: Call<DonationHistoryResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                    setHistoryAdapter(donationHistoryList)
                }
            })
        } else {
            Utils.showNoInternetMessage(mActivity)
        }
    }

    private fun setHistoryAdapter(donationList: ArrayList<DonationHistoryResult>) {
        tvNoPayment.text = mActivity.resources.getString(R.string.no_donation_payment)
        if (donationList.size > 0) {
            rvDonationHistory.visibility = View.VISIBLE
            tvNoPayment.visibility = View.GONE
        } else {
            rvDonationHistory.visibility = View.GONE
            tvNoPayment.visibility = View.VISIBLE
        }
        val paymentHistoryAdapter = PaymentHistoryAdapter(donationList)
        rvDonationHistory.adapter = paymentHistoryAdapter
        paymentHistoryAdapter.notifyDataSetChanged()
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
            DonationHistoryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ivAddDonation -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                mActivity.addFragment(DonationFragment(), true, true, AnimationType.fadeInfadeOut)
            }
        }
    }

    inner class PaymentHistoryAdapter(private val donationList: ArrayList<DonationHistoryResult>): RecyclerView.Adapter<PaymentHistoryAdapter.PaymentHistoryHolder>() {

        inner class PaymentHistoryHolder(view: View): RecyclerView.ViewHolder(view) {
            val tvCardNo: TextView = view.findViewById(R.id.tvCardNo)
            val tvPaymentCardNumber: TextView = view.findViewById(R.id.tvPaymentCardNumber)
            val tvPayDate: TextView = view.findViewById(R.id.tvPayDate)
            val tvPaymentDate: TextView = view.findViewById(R.id.tvPaymentDate)
            val tvPaymentAmount: TextView = view.findViewById(R.id.tvPaymentAmount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentHistoryHolder {
            return PaymentHistoryHolder(
                LayoutInflater.from(context).inflate(R.layout.raw_donation_history, parent, false)
            )
        }

        override fun onBindViewHolder(holder: PaymentHistoryHolder, position: Int) {
            holder.tvCardNo.text = mActivity.resources.getString(R.string.card_number) + " : "
            holder.tvPayDate.text = mActivity.resources.getString(R.string.str_pay_date) + " : "
            var diagStartDate = ""

            val donation = donationList[position]
            val paymentDate = donation.paymentDate ?: ""
            val donationAmount = donation.amount ?: 0.0
            val cardNumber = donation.accountNumber ?: ""

            val formatter = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
            try {
                var date1: Date? = null
                if (paymentDate.isNotEmpty()) {
                    date1 = formatter.parse(paymentDate)
                }
                val target = SimpleDateFormat(OUTPUT_DATE_FORMAT2)
                if (date1 != null) {
                    diagStartDate = target.format(date1)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val decimalSymbols = DecimalFormatSymbols.getInstance(Locale.US)
            holder.tvPaymentAmount.text = "$"+ DecimalFormat("##.##", decimalSymbols).format(donationAmount)
            holder.tvPaymentCardNumber.text = if (cardNumber.isNotEmpty()) "\u25CF\u25CF\u25CF\u25CF \u25CF\u25CF\u25CF\u25CF \u25CF\u25CF\u25CF\u25CF $cardNumber" else ""
            holder.tvPaymentDate.text = diagStartDate
        }

        override fun getItemCount(): Int {
            return donationList.size
        }
    }
}