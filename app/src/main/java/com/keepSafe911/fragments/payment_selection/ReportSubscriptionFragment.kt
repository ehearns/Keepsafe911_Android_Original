package com.keepSafe911.fragments.payment_selection

import addFragment
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.keepSafe911.BuildConfig
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.model.MemberBean
import com.keepSafe911.model.response.FrequencyHistoryResult
import com.keepSafe911.utils.AppPreference
import com.keepSafe911.utils.Comman_Methods
import com.keepSafe911.utils.LocaleUtils
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_report_subscription.*
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val ARG_PARAM3 = "param3"
private const val ARG_PARAM4 = "param4"

class ReportSubscriptionFragment : HomeBaseFragment(), View.OnClickListener {

    private var monthAmount: Double = 0.0
    private var yearAmount: Double = 0.0
    private var paymentMemberList: ArrayList<MemberBean> = ArrayList()
    private var frequencyRange: Int = 0
    private var frequencyPremiumReportList: ArrayList<FrequencyHistoryResult> = ArrayList()
    private var frequencyPosition: Int = -1

    companion object {
        fun newInstance(
            frequencyRange: Int,
            paymentMemberList: ArrayList<MemberBean>,
            frequencyPremiumReportList: ArrayList<FrequencyHistoryResult>,
            frequencyPosition: Int
        ): ReportSubscriptionFragment {
            val args = Bundle()
            args.putInt(ARG_PARAM1, frequencyRange)
            args.putParcelableArrayList(ARG_PARAM2, paymentMemberList)
            args.putParcelableArrayList(ARG_PARAM3, frequencyPremiumReportList)
            args.putInt(ARG_PARAM4,frequencyPosition)
            val fragment = ReportSubscriptionFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            frequencyRange = it.getInt(ARG_PARAM1, 0)
            paymentMemberList = it.getParcelableArrayList(ARG_PARAM2) ?: ArrayList()
            frequencyPremiumReportList = it.getParcelableArrayList(ARG_PARAM3) ?: ArrayList()
            frequencyPosition = it.getInt(ARG_PARAM4,-1)
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
        return inflater.inflate(R.layout.fragment_report_subscription, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity.disableDrawer()
        mActivity.checkUserActive()
        initializeData()
    }
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            mActivity.checkUserActive()
        }
    }
    private fun initializeData() {
        val content = SpannableString(mActivity.resources.getString(R.string.freq_subscription))
        content.setSpan(UnderlineSpan(), 0, content.length, 0)
        tvReportFreq.text = content
        prime_text.text = mActivity.resources.getString(
            R.string.str_note_frequency_payment,
            paymentMemberList.size
        )
        val decimalSymbols = DecimalFormatSymbols.getInstance(Locale.US)
        val convertMonth = DecimalFormat("#.##", decimalSymbols).format((singleUserPriceCalculation() * 30))
        val convertYear = DecimalFormat("#.##", decimalSymbols).format((singleUserPriceCalculation() * 365))
        yearAmount = DecimalFormat("#.##", decimalSymbols).format((convertYear.toDouble() + 19.99)).toDouble()
        monthAmount = DecimalFormat("#.##", decimalSymbols).format((convertMonth.toDouble() + 9.99)).toDouble()

        btnReportFreqMonth.text = getString(R.string.month_amt, monthAmount.toString())
        btnReportFreqYear.text = getString(R.string.year_amt, yearAmount.toString())

        btnReportFreqCancel.text = getString(R.string.cancel)

        btnReportFreqMonth.setOnClickListener(this)
        btnReportFreqYear.setOnClickListener(this)
        btnReportFreqCancel.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnReportFreqMonth -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                mActivity.addFragment(
                    ReportPaymentFragment.newInstance(
                        frequencyRange,
                        paymentMemberList,
                        monthAmount, 1, frequencyPremiumReportList, frequencyPosition
                    ),
                    addToBackStack = true,
                    ignoreIfCurrent = true,
                    animationType = AnimationType.fadeInfadeOut
                )
            }
            R.id.btnReportFreqYear -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                mActivity.addFragment(
                    ReportPaymentFragment.newInstance(
                        frequencyRange,
                        paymentMemberList,
                        yearAmount, 2, frequencyPremiumReportList, frequencyPosition
                    ),
                    addToBackStack = true,
                    ignoreIfCurrent = true,
                    animationType = AnimationType.fadeInfadeOut
                )
            }
            R.id.btnReportFreqCancel -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                mActivity.onBackPressed()
            }
        }
    }

    private fun singleUserPriceCalculation(): Double{
        return if (frequencyRange > 0) {
            val convertMinute: Double = (frequencyRange / 60).toDouble()
            val convertHour: Double = 60 / convertMinute
            val convertDay: Double = 24 * convertHour
            val paymentPrice: Double = (5 * convertDay) / 1000
            paymentPrice * if (paymentMemberList.size > 0) paymentMemberList.size else 1
        }else{
            0.0
        }
    }
}