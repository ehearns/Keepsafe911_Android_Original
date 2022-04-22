package com.keepSafe911.fragments.homefragment.profile


import ValidationUtil.Companion.isPasswordMatch
import ValidationUtil.Companion.isPasswordValidate
import ValidationUtil.Companion.isRequiredField
import ValidationUtil.Companion.isValidPasswordLength
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo

import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.model.response.ApiResponse
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.webservices.WebApiClient
import com.google.gson.JsonObject
import com.keepSafe911.BuildConfig
import com.keepSafe911.utils.*
import hideKeyboard
import kotlinx.android.synthetic.main.background_layout.*
import kotlinx.android.synthetic.main.fragment_change_password.*
import kotlinx.android.synthetic.main.toolbar_header.*
import retrofit2.Call
import retrofit2.Response
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ChangePasswordFragment : HomeBaseFragment(), View.OnClickListener {

    lateinit var appDatabase: OldMe911Database

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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
        return inflater.inflate(R.layout.fragment_change_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appDatabase = OldMe911Database.getDatabase(mActivity)
        mActivity.disableDrawer()
        setHeader()
        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            etOldPassword.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etNewPassword.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            etCConfirmPassword.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        } else {
            etOldPassword.imeOptions = EditorInfo.IME_ACTION_NEXT
            etNewPassword.imeOptions = EditorInfo.IME_ACTION_NEXT
            etCConfirmPassword.imeOptions = EditorInfo.IME_ACTION_DONE
        }

        val changePasswordContent = mActivity.resources.getString(R.string.change_password).uppercase()
        val changeContent = mActivity.resources.getString(R.string.str_change).uppercase()
        val contentTitle = SpannableString(changePasswordContent)
        contentTitle.setSpan(StyleSpan(Typeface.BOLD), 0, changeContent.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvChangePasswordInTitle.text = contentTitle

        val tv = TypedValue()
        var actionBarHeight = 45
        if (mActivity.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, mActivity.resources.displayMetrics)
        }

        val height = Utils.calculateNoOfRows(mActivity, 4.0)
        clChangePassword.setPadding(0, height - actionBarHeight,0,0)

        btn_submit.setOnClickListener(this)
    }

    private fun setHeader() {
        mActivity.disableDrawer()
        tvHeader.text = ""
//        tvHeader.text = mActivity.resources.getString(R.string.change_password)
        tvHeader.setPadding(0, 0, 50, 0)
        iv_back.visibility = View.VISIBLE
//        ivTopBubble.visibility = View.VISIBLE
//        ivBottomBubble.visibility = View.VISIBLE
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
        mActivity.checkUserActive()
    }

    override fun onClick(v: View?) {

        when (v?.id) {
            R.id.btn_submit -> {
                mActivity.hideKeyboard()
                if (checkForValidations()) {
                    Comman_Methods.avoidDoubleClicks(v)
                    callChangePassApi()
                }
            }
        }
    }

    private fun checkForValidations(): Boolean {

        return when {
            !isRequiredField(etOldPassword.text.toString()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_pass))
                false
            }
            etOldPassword.text.toString() != appDatabase.loginDao().getAll().password -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.old_password_match))
                false
            }
            !isRequiredField(etNewPassword.text.toString()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_pass))
                false
            }
            !isValidPasswordLength(etNewPassword.text.toString()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.pass_val))
                false
            }
            !isPasswordValidate(etNewPassword.text.toString()) ->{
                mActivity.showMessage(mActivity.resources.getString(R.string.str_password_error))
                false
            }
            isPasswordMatch(etOldPassword.text.toString(), etNewPassword.text.toString()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.old_new_password))
                false
            }
            !isRequiredField(etCConfirmPassword.text.toString()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_pass))
                false
            }
            !isPasswordMatch(etNewPassword.text.toString(), etCConfirmPassword.text.toString()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.match_pass))
                false
            }
            else -> true
        }
    }


    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            setHeader()
        }
    }

    private fun callChangePassApi() {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            Comman_Methods.isProgressShow(mActivity)
            mActivity.isSpeedAvailable()
            val change_json = JsonObject()
            change_json.addProperty("OldPassword", etOldPassword.text.toString())
            change_json.addProperty("NewPassword", etNewPassword.text.toString())
            change_json.addProperty("UserID", appDatabase.loginDao().getAll().memberID)

            val changeResponseCall = WebApiClient.getInstance(mActivity)
                .webApi_without?.callChangePassApi(change_json)


            changeResponseCall?.enqueue(object : retrofit2.Callback<ApiResponse> {

                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val statusCode: Int = response.code()
                    val change_response = response.body()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()

                            if (change_response?.status == true) {
                                val loginupdate: LoginObject = appDatabase.loginDao().getAll()
                                loginupdate.password = etNewPassword.text.toString()
                                appDatabase.loginDao().updateLogin(loginupdate)

                                val fing_obj = appDatabase.loginDao().getfingerLoginData()
                                if (fing_obj != null) {
                                    fing_obj.user_password = etNewPassword.text.toString()
                                    appDatabase.loginDao().updateFingerLogin(fing_obj)
                                }

                                val rem_data = appDatabase.loginDao().getRemember()
                                if (rem_data!=null) {
                                    rem_data.user_password = etNewPassword.text.toString()
                                    appDatabase.loginDao().updateRemember(rem_data)
                                }
                                mActivity.onBackPressed()
                            }
                            mActivity.showMessage(change_response?.responseMessage ?: "")
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        Utils.showSomeThingWrongMessage(mActivity)
                    }
                }
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }
            })
        } else {
            Utils.showNoInternetMessage(mActivity)
        }
    }
}