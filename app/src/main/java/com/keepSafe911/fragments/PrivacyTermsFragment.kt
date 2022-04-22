package com.keepSafe911.fragments

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatDelegate
import com.keepSafe911.BuildConfig
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.MainDialogBaseFragment
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.utils.AppPreference
import com.keepSafe911.utils.Comman_Methods
import com.keepSafe911.utils.LocaleUtils
import com.keepSafe911.utils.Utils
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_privacy_terms.*
import kotlinx.android.synthetic.main.toolbar_header.*
import java.util.*
import kotlin.math.roundToInt

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class PrivacyTermsFragment : MainDialogBaseFragment(), View.OnClickListener {
    private var param1: String? = ""
    private var type: Int = 0
    lateinit var privacyTermsListener: CommonApiListener

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1, "")
            type = it.getInt(ARG_PARAM2, 0)
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
        return inflater.inflate(R.layout.fragment_privacy_terms, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val marginTenData = Comman_Methods.convertDpToPixels(10f, mActivity)
        val marginData = if (mActivity.resources.getBoolean(R.bool.isTablet)) Comman_Methods.convertDpToPixels(220f, mActivity) else Comman_Methods.convertDpToPixels(20f, mActivity)
        val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(marginData.roundToInt(), marginTenData.roundToInt(), marginData.roundToInt(), marginTenData.roundToInt())
        btnPrivacyTerms.layoutParams = params
        btnPrivacyTerms.setOnClickListener(this)
        val settings = wvPrivacyTerms.settings
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.builtInZoomControls = true

        tvHeader.setPadding(0, 0, 50, 0)
        tvHeader.text = param1 ?: ""
        Utils.setTextGradientColor(tvHeader)
        iv_back.visibility = View.VISIBLE
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            dismiss()
        }

        when (type) {
            0 -> {
                wvPrivacyTerms.loadUrl(BuildConfig.privacyPolicyLink)
            }
            1 -> {
                wvPrivacyTerms.loadUrl(BuildConfig.termsServiceLink)
            }
            else -> {
                wvPrivacyTerms.loadUrl("")
            }
        }

        wvPrivacyTerms.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Comman_Methods.isProgressShow(mActivity)
                mActivity.isSpeedAvailable()
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                Comman_Methods.isProgressHide()
            }
        }
        wvPrivacyTerms.webChromeClient = object : WebChromeClient() {
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
        fun newInstance(param1: String, param2: Int) =
            PrivacyTermsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putInt(ARG_PARAM2, param2)
                }
            }
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btnPrivacyTerms -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                if (this@PrivacyTermsFragment::privacyTermsListener.isInitialized) {
                    if (privacyTermsListener != null) {
                        privacyTermsListener.privacyTermsChecked(type, true)
                    }
                }
                dismiss()
            }
        }
    }
}