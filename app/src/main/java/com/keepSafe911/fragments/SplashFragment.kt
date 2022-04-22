package com.keepSafe911.fragments

import AnimationType
import addFragment
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.keepSafe911.BuildConfig
import com.keepSafe911.fragments.commonfrag.MainBaseFragment
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.LocaleUtils
import com.keepSafe911.HomeActivity
import com.keepSafe911.R
import com.keepSafe911.openlive.Constants
import com.keepSafe911.utils.AppPreference
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val ARG_PARAM3 = "param3"

class SplashFragment : MainBaseFragment() {

    var channelName: String = ""
    var id: String = ""
    var missingChildId: String = ""
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
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            channelName = it.getString(ARG_PARAM1, "") ?: ""
            id = it.getString(ARG_PARAM2, "") ?: ""
            missingChildId = it.getString(ARG_PARAM3, "") ?: ""
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Handler(Looper.getMainLooper()).postDelayed({
            val appDatabase: OldMe911Database = OldMe911Database.getDatabase(mActivity)
            if (appDatabase.loginDao().getAll() != null) {
                mActivity.finish()
                mActivity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                val intent = Intent(mActivity, HomeActivity::class.java)
                intent.putExtra(Constants.KEY_CHANNEL_NAME, channelName)
                intent.putExtra(Constants.KEY_CHANNEL_ID, id)
                intent.putExtra(Constants.MISSING_CHILD_ID, missingChildId)
                mActivity.startActivity(intent)
            } else {
                mActivity.addFragment(
                    LandingOneFragment(),
                    false,
                    true,
                    animationType = AnimationType.fadeInfadeOut
                )
//                mActivity.addFragment(SubscriptionFragment(), false, true, animationType = AnimationType.fadeInfadeOut)
            }
        }, 2000)
    }

    companion object {
        fun newInstance(channelName: String, id: String, missingChildId: String): SplashFragment {
            val args = Bundle()
            args.putString(ARG_PARAM1, channelName)
            args.putString(ARG_PARAM2, id)
            args.putString(ARG_PARAM3, missingChildId)
            val fragment = SplashFragment()
            fragment.arguments = args
            return fragment
        }
    }
}