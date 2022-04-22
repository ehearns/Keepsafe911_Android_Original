package com.keepSafe911.fragments.donation

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.keepSafe911.BuildConfig
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.utils.AppPreference
import com.keepSafe911.utils.Comman_Methods
import com.keepSafe911.utils.LocaleUtils
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_thank_you.*
import kotlinx.android.synthetic.main.toolbar_header.*
import java.util.*
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ThankYouFragment : HomeBaseFragment() {
    private var param1: String? = ""
    private var param2: String? = ""

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
        return inflater.inflate(R.layout.fragment_thank_you, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHeader()
    }

    private fun setHeader() {
        mActivity.disableDrawer()
        tvHeader.text = ""
        ivMenuLogo.visibility = View.VISIBLE
        iv_back.visibility = View.VISIBLE
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
        val followUsImages: ArrayList<Int> = ArrayList()
        followUsImages.add(R.drawable.ic_follow_twitter)
        followUsImages.add(R.drawable.ic_follow_instagram)
        followUsImages.add(R.drawable.ic_follow_facebook)
        tvDonationDetail.text = mActivity.resources.getString(R.string.str_donation_detail)
        rvFollowUs.layoutManager = LinearLayoutManager(mActivity, RecyclerView.HORIZONTAL, false)
        rvFollowUs.adapter = FollowUsAdapter(mActivity, followUsImages)
        mActivity.checkUserActive()
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
            ThankYouFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    inner class FollowUsAdapter(private val context: Context, private var followUsImageArrayList: ArrayList<Int>): RecyclerView.Adapter<FollowUsAdapter.FollowUsHolder>() {

        inner class FollowUsHolder(view: View): RecyclerView.ViewHolder(view){
            var ivFollow: ImageView = view.findViewById(R.id.ivFollow)
            var clFollow: ConstraintLayout = view.findViewById(R.id.clFollow)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowUsHolder {
            return FollowUsHolder(LayoutInflater.from(context).inflate(R.layout.raw_follow_us, parent, false))
        }

        override fun onBindViewHolder(holder: FollowUsHolder, position: Int) {
            holder.ivFollow.setImageResource(followUsImageArrayList[position])
            holder.clFollow.setOnClickListener {
                when (position) {
                    0 -> {

                    }
                    1 -> {

                    }
                    2 -> {

                    }
                    else -> {

                    }
                }
            }
        }

        override fun getItemCount(): Int {
            return followUsImageArrayList.size
        }
    }
}