package com.keepSafe911.fragments.missingchild

import addFragment
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import com.keepSafe911.BuildConfig
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.utils.*
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_acknowledge_payment.*
import kotlinx.android.synthetic.main.toolbar_header.*
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class AcknowledgePaymentFragment : HomeBaseFragment(), View.OnClickListener {
    private var param1: String? = ""
    private var param2: String? = ""

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
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
        return inflater.inflate(R.layout.fragment_acknowledge_payment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHeader()
    }

    private fun setHeader() {
        mActivity.disableDrawer()
        tvHeader.setPadding(0, 0, 50, 0)
        iv_back.visibility = View.VISIBLE
        tvHeader.text = mActivity.resources.getString(R.string.payment).uppercase()
        Utils.setTextGradientColor(tvHeader)
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
        btnAcknowledgePayment.setOnClickListener(this)
        val paymentAmount = "$99.0"
        val content = SpannableString(mActivity.resources.getString(R.string.str_acknowledge_message))
        val startIndex = mActivity.resources.getString(R.string.str_acknowledge_message).indexOf(
            paymentAmount)
        val endIndex = startIndex + paymentAmount.length
        content.setSpan(
            StyleSpan(Typeface.BOLD), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        content.setSpan(
            ForegroundColorSpan(Color.parseColor("#019CA0")),
            startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        tvConfirmMissingPayment.text = content
        btnAcknowledgePayment.text = mActivity.resources.getString(R.string.pay)+" " + paymentAmount
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
            AcknowledgePaymentFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnAcknowledgePayment -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                mActivity.addFragment(
                    AddMissingChildFragment(),
                    true, true,
                    animationType = AnimationType.fadeInfadeOut)
            }
        }
    }
}