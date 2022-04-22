package com.keepSafe911.fragments.homefragment.profile

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.keepSafe911.BuildConfig
import com.keepSafe911.HomeActivity
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.utils.*
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_language.*
import kotlinx.android.synthetic.main.toolbar_header.*
import java.util.*

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class LanguageFragment : HomeBaseFragment() {

    private var languageList: ArrayList<String> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mActivity.requestedOrientation =
            if (mActivity.resources.getBoolean(R.bool.isTablet)) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        /*mActivity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )*/
        return inflater.inflate(R.layout.fragment_language, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity.disableDrawer()
        setHeader()
        setAdapter()
    }

    private fun setAdapter() {
        if (rvLanguage!=null) {
            languageList = ArrayList()
            languageList = ArrayList<String>(listOf(*resources.getStringArray(R.array.array_language)))
            rvLanguage.layoutManager = LinearLayoutManager(mActivity, RecyclerView.VERTICAL, false)
            rvLanguage.adapter = LanguageAdapter(mActivity, languageList)
        }
    }

    private fun setHeader() {
        tvHeader.text = mActivity.resources.getString(R.string.select_language)
        Utils.setTextGradientColor(tvHeader)
        tvHeader.setPadding(0, 0, 50, 0)
        iv_back.visibility = View.VISIBLE
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
        mActivity.checkUserActive()
    }

    class LanguageAdapter(private val mainActivity: HomeActivity, private val languageList: ArrayList<String>) :
        RecyclerView.Adapter<LanguageAdapter.LanguageHolder>() {
        var assistantPosition: Int = -1
        var isFirst = true
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): LanguageHolder {
            return LanguageHolder(
                LayoutInflater.from(mainActivity).inflate(R.layout.raw_language, p0, false)
            )
        }

        override fun getItemCount(): Int {
            return languageList.size
        }

        override fun onBindViewHolder(p0: LanguageHolder, p1: Int) {
            p0.tvLanguage.text = languageList[p1]

            if (isFirst) {
                assistantPosition = AppPreference.getIntPreference(mainActivity, BuildConfig.languagePrefKey)
            }

            p0.tvLanguage.setOnClickListener {
                isFirst = false
                AppPreference.saveIntPreference(mainActivity, BuildConfig.languagePrefKey, p1)
                assistantPosition = p0.bindingAdapterPosition
                if (p1 == 0) {
                    LocaleUtils.setLocale(Locale(LocaleUtils.LAN_ENGLISH))
                } else {
                    LocaleUtils.setLocale(Locale(LocaleUtils.LAN_SPANISH))
                }
                LocaleUtils.updateConfig(mainActivity, mainActivity.resources.configuration)
                mainActivity.changeNavigationLanguage()
                notifyDataSetChanged()
                mainActivity.onBackPressed()
            }
            if (assistantPosition == p1) {
                p0.tvLanguage.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0)
            } else {
                p0.tvLanguage.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
        }

        class LanguageHolder(view: View) : RecyclerView.ViewHolder(view) {
            var tvLanguage = view.findViewById<TextView>(R.id.tvLanguages)
        }
    }
}
