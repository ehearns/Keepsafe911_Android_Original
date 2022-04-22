package com.keepSafe911.fragments.homefragment.hibp


import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.keepSafe911.BuildConfig

import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.model.PwnedHash
import com.keepSafe911.utils.*
import com.keepSafe911.utils.Comman_Methods.Companion.isProgressHide
import com.keepSafe911.utils.Comman_Methods.Companion.isProgressShow
import com.keepSafe911.webservices.WebApiClient
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_password_compromised.*
import kotlinx.android.synthetic.main.toolbar_header.*
import retrofit2.Call
import retrofit2.Response
import java.security.MessageDigest
import java.util.*

class PasswordCompromisedFragment : HomeBaseFragment(), View.OnClickListener {

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
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
        return inflater.inflate(R.layout.fragment_password_compromised, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            etPwnedPassword.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        } else {
            etPwnedPassword.imeOptions = EditorInfo.IME_ACTION_DONE
        }
        setHeader()
        btnSearchPassword.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id){
            R.id.btnSearchPassword -> {
                mActivity.hideKeyboard()
                if (etPwnedPassword.text.toString()!=""){
                    Comman_Methods.avoidDoubleClicks(v)
                    isHashPasswordPwned(makeHash(etPwnedPassword.text.toString()))
                }else{
                    tvPasswordConfirmation.visibility = View.GONE
                    mActivity.showMessage(mActivity.resources.getString(R.string.blank_pass))
                }
            }
        }
    }

    private fun isHashPasswordPwned(pwHash: String) {
        val hash5 = pwHash.substring(0, 5)
        val suffix = pwHash.takeLast(5)
        if (ConnectionUtil.isInternetAvailable(mActivity)){
            callSearchByRange(hash5,suffix)
        }else{
            Utils.showNoInternetMessage(mActivity)
        }
    }

    private fun makeHash(password: String): String {
        return hashString(password)
    }
    private fun hashString(input: String): String {
        val HEX_CHARS = "0123456789ABCDEF"
        val bytes = MessageDigest
            .getInstance("SHA-1")
            .digest(input.toByteArray())
        val result = StringBuilder(bytes.size * 2)

        bytes.forEach {
            val i = it.toInt()
            result.append(HEX_CHARS[i shr 4 and 0x0f])
            result.append(HEX_CHARS[i and 0x0f])
        }

        return result.toString().uppercase()
    }

    private fun setHeader() {
        mActivity.checkNavigationItem(9)
        mActivity.disableDrawer()
        tvHeader.text = mActivity.resources.getString(R.string.str_password_compromised)
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

    private fun callSearchByRange(hash5: String, suffix: String) {
        isProgressShow(mActivity)
        val pwnedHashList : ArrayList<PwnedHash> = ArrayList()
        val loginResponseCall= WebApiClient.getInstance(mActivity)
            .webApi_PwnedPasswordsService?.searchByRange(hash5)

        loginResponseCall?.enqueue(object : retrofit2.Callback<String>{
            override fun onFailure(call: Call<String>, t: Throwable) {
                isProgressHide()
            }

            override fun onResponse(call: Call<String>, response: Response<String>) {
                val statusCode:Int= response.code()
                isProgressHide()
                tvPasswordConfirmation.visibility = View.VISIBLE
                if (response.isSuccessful && statusCode==200) {
                    response.body()?.let {
                        if (it != "") {
                            val bothList = listOf(it.replace("\r","").split("\n"))
                            for (string in bothList) {
                                for(check in string) {
                                    val both = check.split(":")
                                    val hash = both[0]
                                    val count = both[1].toInt(10)
                                    val pwnedHash = PwnedHash(hash, count)
                                    pwnedHashList.add(pwnedHash)
                                }
                            }
                            var passwordFreq = 0
                            if (pwnedHashList.size > 0) {
                                val outcome = pwnedHashList.filter { hash -> hash.hash.contains(suffix) }
                                passwordFreq =
                                    if (outcome != null) if (outcome.isNotEmpty()) outcome[0].count else 0 else 0
                            }
                            if (passwordFreq > 0){
                                tvPasswordConfirmation.background = ContextCompat.getDrawable(mActivity,R.drawable.used_password)
                                tvPasswordConfirmation.text = mActivity.resources.getString(R.string.str_used_password, passwordFreq)
                            }else{
                                tvPasswordConfirmation.background = ContextCompat.getDrawable(mActivity,R.drawable.correct_password)
                                tvPasswordConfirmation.text = mActivity.resources.getString(R.string.str_correct_password)
                            }
                        }
                    }
                }else{
                    tvPasswordConfirmation.background = ContextCompat.getDrawable(mActivity,R.drawable.correct_password)
                    tvPasswordConfirmation.text = mActivity.resources.getString(R.string.str_correct_password)
                    when (statusCode) {
                        400 -> {
//                            mActivity.showMessage("Bad request — the account does not comply with an acceptable format (i.e. it's an empty string)")
                        }
                        401 -> {
//                            mActivity.showMessage("Unauthorised — the API key provided was not valid")
                        }
                        403 -> {
//                            mActivity.showMessage("Forbidden — no user agent has been specified in the request")
                        }
                        404 -> {
//                            mActivity.showMessage("Not found — the account could not be found and has therefore not been pwned")
                        }
                        429 -> {
//                            mActivity.showMessage("Too many requests — the rate limit has been exceeded")
                        }
                        503 -> {
//                            mActivity.showMessage("Service unavailable — usually returned by Cloudflare if the underlying service is not available")
                        }
                        else -> {
//                            mActivity.showMessage("Unknown error code $statusCode")
                        }
                    }
                }
            }
        })
    }
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            setHeader()
        }
    }
}
