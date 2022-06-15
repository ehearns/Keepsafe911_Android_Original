package com.keepSafe911.fragments

import android.app.Dialog
import android.content.pm.ActivityInfo
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.keepSafe911.BuildConfig
import com.keepSafe911.fragments.commonfrag.MainBaseFragment
import com.keepSafe911.model.PhoneCountryCode
import com.keepSafe911.model.response.ApiResponse
import com.keepSafe911.webservices.WebApiClient
import com.keepSafe911.MainActivity
import com.keepSafe911.R
import com.keepSafe911.utils.*
import hideKeyboard
import kotlinx.android.synthetic.main.background_layout.*
import kotlinx.android.synthetic.main.fragment_forgot.*
import kotlinx.android.synthetic.main.raw_country_search.view.*
import kotlinx.android.synthetic.main.search_country.view.*
import kotlinx.android.synthetic.main.toolbar_header.*
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ForgotFragment : MainBaseFragment(), View.OnClickListener {

    private var phoneCountryCodes: ArrayList<PhoneCountryCode> = ArrayList()
    private var countryCode: String = ""
    lateinit var dialog: Dialog


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
        return inflater.inflate(R.layout.fragment_forgot, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHeader()
        storeCountryCode()
        tvForgotCountrySelected.text = phoneCountryCodes[UNITED__CODE_POSITION].countryCode
        countryCode = phoneCountryCodes[UNITED__CODE_POSITION].countryCode
        tvForgotCountrySelected.setCompoundDrawablesWithIntrinsicBounds(
            phoneCountryCodes[UNITED__CODE_POSITION].flag,
            0,
            0,
            0
        )
        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            etForgotMobile.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        } else {
            etForgotMobile.imeOptions = EditorInfo.IME_ACTION_DONE
        }

        val forgotPasswordContent = mActivity.resources.getString(R.string.forgot_password_title).uppercase()
        val forgotContent = mActivity.resources.getString(R.string.str_forgot).uppercase()
        val contentTitle = SpannableString(forgotPasswordContent)
        contentTitle.setSpan(StyleSpan(Typeface.BOLD), 0, forgotContent.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvForgotTitle.text = contentTitle

        btnForgotPassword.setOnClickListener(this)
        tvForgotCountrySelected.setOnClickListener(this)
    }

    private fun setHeader() {

        iv_back.visibility = View.VISIBLE
//        ivTopBubble.visibility = View.VISIBLE
//        ivBottomBubble.visibility = View.VISIBLE
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
    }

    private fun storeCountryCode() {
        phoneCountryCodes = ArrayList()
        val stringBuffer = StringBuffer()
        var bufferedReader: BufferedReader? = null
        try {
            bufferedReader = BufferedReader(InputStreamReader(mActivity.assets.open("Countrylistwithdialcode.json")))
            var temp: String? = ""
            while (run {
                    temp = bufferedReader.readLine()
                    temp
                } != null)
                stringBuffer.append(temp)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                bufferedReader?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
        val myjsonString = stringBuffer.toString()
        try {
            val jsonObjMain = JSONObject(myjsonString)
            val jsonArray = jsonObjMain.getJSONArray("CountryDialcode")
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val countryName = jsonObject.getString("name")
                val dialCode = jsonObject.getString("dial_code")
                val countryCode = jsonObject.getString("code")
                phoneCountryCodes.add(
                    PhoneCountryCode(
                        countryName, dialCode, countryCode,
                        Utils.countryFlagWithCode(mActivity)[i].flag, false
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            setHeader()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnForgotPassword -> {
                mActivity.hideKeyboard()
                if (etForgotMobile.text.toString().trim().isNotEmpty()) {
                    if (etForgotMobile.text.toString().trim().length != 10) {
                        mActivity.showMessage(mActivity.resources.getString(R.string.phone_length))
                    } else {
                        Comman_Methods.avoidDoubleClicks(v)
                        callForgotApi(countryCode + etForgotMobile.text.toString().trim())
                    }
                } else {
                    mActivity.showMessage(mActivity.resources.getString(R.string.blank_phone))
                }
            }
            R.id.tvForgotCountrySelected -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                storeCountryCode()
                openUploadCountryDialog(phoneCountryCodes)
            }
        }
    }

    private fun callForgotApi(mobile: String) {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            Comman_Methods.isProgressShow(mActivity)
            mActivity.isSpeedAvailable()
            val callForgotApi = WebApiClient.getInstance(mActivity).webApi_without?.callForgotPassword(mobile)
            callForgotApi?.enqueue(object : retrofit2.Callback<ApiResponse> {
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                if (it.status) {
                                    mActivity.showMessage(it.result)
                                    mActivity.onBackPressed()
                                } else {
                                    mActivity.showMessage(it.result)
                                }
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        Utils.showSomeThingWrongMessage(mActivity)
                    }
                }
            })
        } else {
            Utils.showNoInternetMessage(mActivity)
        }
    }

    private fun openUploadCountryDialog(phoneCountryCodes: ArrayList<PhoneCountryCode>) {
        val inflater = layoutInflater
        val dialogLayout1 = inflater.inflate(R.layout.search_country, null)
        val mDialog = android.app.AlertDialog.Builder(activity)
        mDialog.setView(dialogLayout1)

        if (this::dialog.isInitialized) {
            if (dialog != null) {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }
        }

        dialog = mDialog.create()
        dialog.window?.attributes?.windowAnimations = R.style.animationForDialog

        dialogLayout1.tvSearchCountryClose.setOnClickListener { dialog.dismiss() }
        dialogLayout1.rvCountryNameCodeFlag.layoutManager = LinearLayoutManager(mActivity, RecyclerView.VERTICAL, false)
        val adapter = CountrySelectionAdapter(mActivity, phoneCountryCodes)
        dialogLayout1.rvCountryNameCodeFlag.adapter = adapter
        dialogLayout1.tvSearchCountryHeader.text = mActivity.resources.getString(R.string.select_country)
        dialogLayout1.etSearchCountry.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter.filter(s)
            }

        })
        dialog.setCancelable(false)
        dialog.show()
    }

    inner class CountrySelectionAdapter() : RecyclerView.Adapter<CountrySelectionAdapter.CountrySelectionHolder>(),
        Filterable {

        lateinit var context: MainActivity
        private var phoneCountryCodes: ArrayList<PhoneCountryCode> = ArrayList()
        private var duplicatePhoneCountryCodes: ArrayList<PhoneCountryCode> = ArrayList()
        private var adapterPosition = -1

        constructor(context: MainActivity, phoneCountryCodes: ArrayList<PhoneCountryCode>) : this() {
            this.context = context
            this.phoneCountryCodes = phoneCountryCodes
            this.duplicatePhoneCountryCodes = ArrayList()
            this.duplicatePhoneCountryCodes.addAll(phoneCountryCodes)
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): CountrySelectionHolder {
            return CountrySelectionHolder(LayoutInflater.from(context).inflate(R.layout.raw_country_search, p0, false))
        }

        override fun getItemCount(): Int {
            return duplicatePhoneCountryCodes.size
        }

        override fun onBindViewHolder(p0: CountrySelectionHolder, p1: Int) {
            val countryName = duplicatePhoneCountryCodes[p1].countryName
            when (duplicatePhoneCountryCodes[p1].code) {
                "CA" -> p0.tvCountryDetails.text = "$countryName (+1)"
                else -> p0.tvCountryDetails.text =
                    countryName + " (" + duplicatePhoneCountryCodes[p1].countryCode + ")"
            }
            p0.tvCountryDetails.setCompoundDrawablesWithIntrinsicBounds(duplicatePhoneCountryCodes[p1].flag, 0, 0, 0)
            p0.tvCountryDetails.setOnClickListener {
                context.hideKeyboard()
                adapterPosition = p0.bindingAdapterPosition
                notifyDataSetChanged()
            }
            if (adapterPosition == p1) {
                duplicatePhoneCountryCodes[p1].isSelected = true
                dialog.dismiss()
                displaySelectedCountry()
            } else {
                duplicatePhoneCountryCodes[p1].isSelected = false
            }
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(charSequence: CharSequence): FilterResults {
                    val charString = charSequence.toString()
                    duplicatePhoneCountryCodes = if (charString.isEmpty()) {
                        phoneCountryCodes
                    } else {
                        val filterList = ArrayList<PhoneCountryCode>()
                        for (row in phoneCountryCodes) {
                            val countryName = row.countryName
                            val countryCode = row.countryCode
                            if (countryName.lowercase().contains(charString.lowercase()) ||
                                countryCode.lowercase().contains(charString.lowercase())
                            ) {
                                filterList.add(row)
                            }
                        }
                        filterList
                    }
                    val filterResults = FilterResults()
                    filterResults.values = duplicatePhoneCountryCodes
                    return filterResults
                }

                override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                    if (filterResults.values != null) {
                        duplicatePhoneCountryCodes = filterResults.values as ArrayList<PhoneCountryCode>
                    }
                    notifyDataSetChanged()
                }
            }
        }

        inner class CountrySelectionHolder(view: View) : RecyclerView.ViewHolder(view) {
            var tvCountryDetails: TextView = view.tvCountryDetails
        }
    }

    private fun displaySelectedCountry() {
        for (i in 0 until phoneCountryCodes.size) {
            if (phoneCountryCodes[i].isSelected) {
                when {
                    phoneCountryCodes[i].code == "CA" -> tvForgotCountrySelected.text = "+1"
                    else -> tvForgotCountrySelected.text = phoneCountryCodes[i].countryCode
                }
                countryCode = phoneCountryCodes[i].countryCode
                tvForgotCountrySelected.setCompoundDrawablesWithIntrinsicBounds(phoneCountryCodes[i].flag, 0, 0, 0)
            }
        }
    }
}