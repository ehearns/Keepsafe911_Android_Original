package com.keepSafe911.fragments

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Bundle
import android.os.StrictMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatDelegate
import com.keepSafe911.BuildConfig
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.MainDialogBaseFragment
import com.keepSafe911.listner.PaymentOptionListener
import com.keepSafe911.utils.AppPreference
import com.keepSafe911.utils.Comman_Methods
import com.keepSafe911.utils.LocaleUtils
import com.keepSafe911.utils.Utils
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_paypal_payment.*
import kotlinx.android.synthetic.main.toolbar_header.*
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class PayPalPaymentFragment : MainDialogBaseFragment() {
    private var param1: String? = ""
    private var param2: String? = ""
    lateinit var paymentOptionListener: PaymentOptionListener

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
        return inflater.inflate(R.layout.fragment_paypal_payment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        tvHeader.setPadding(0, 0, 50, 0)
        tvHeader.text = mActivity.resources.getString(R.string.str_paypal_subscription)
        Utils.setTextGradientColor(tvHeader)
        iv_back.visibility = View.VISIBLE
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            if (wvPayPalPayment.canGoBack()) {
                wvPayPalPayment.goBack()
            } else {
                dismiss()
            }
        }

        wvPayPalPayment.settings.javaScriptEnabled = true
        val settings = wvPayPalPayment.settings
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.builtInZoomControls = true

        wvPayPalPayment.loadUrl(param1 ?: "")

        wvPayPalPayment.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (url.lowercase().contains("https://keepsafe911.com") ||
                    url.lowercase().contains("http://keepsafe911.com")) {
                    Comman_Methods.isProgressHide()
                    if (this@PayPalPaymentFragment::paymentOptionListener.isInitialized) {
                        if (paymentOptionListener != null) {
                            paymentOptionListener.onPayPalOption("")
                        }
                    }
                    dismiss()
                } else {
                    Comman_Methods.isProgressShow(mActivity)
                    mActivity.isSpeedAvailable()
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                Comman_Methods.isProgressHide()
            }
        }

        wvPayPalPayment.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress == 100) {
                    Comman_Methods.isProgressHide()
                } else {
                    Comman_Methods.isProgressShow(mActivity)
                    mActivity.isSpeedAvailable()
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String = "") =
            PayPalPaymentFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}