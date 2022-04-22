package com.keepSafe911.fragments.homefragment


import AnimationType
import addFragment
import android.content.*
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import appInstalledOrNot
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.keepSafe911.BuildConfig
import com.keepSafe911.HomeActivity
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.fragments.homefragment.find.MemberRouteFragment
import com.keepSafe911.fragments.homefragment.hibp.EmailCompromisedFragment
import com.keepSafe911.fragments.homefragment.hibp.PasswordCompromisedFragment
import com.keepSafe911.fragments.homefragment.profile.*
import com.keepSafe911.fragments.homefragment.report.BusinessTrackFragment
import com.keepSafe911.fragments.homefragment.report.ReportFragment
import com.keepSafe911.model.DashBoardBean
import com.keepSafe911.model.ShareModel
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_sub_dash_board.*
import kotlinx.android.synthetic.main.raw_dashboard.view.*
import kotlinx.android.synthetic.main.toolbar_header.*
import visitUrl
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class SubDashBoardFragment : HomeBaseFragment() {

    var menuList: ArrayList<DashBoardBean> = ArrayList()
    lateinit var appDatabase: OldMe911Database
    lateinit var dashBoardAdapter: DashBoardAdapter
    var option: Int = 0
    var imagePathForCapture: String = ""
    private lateinit var shareAppDialog: BottomSheetDialog

    companion object {
        fun newInstance(option: Int): SubDashBoardFragment {
            val args = Bundle()
            args.putInt(ARG_PARAM1, option)
            val fragment = SubDashBoardFragment()
            fragment.arguments = args
            return fragment
        }

    }

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            option = it.getInt(ARG_PARAM1, 0)
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
        return inflater.inflate(R.layout.fragment_sub_dash_board, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHeader()
        mActivity.enableDrawer()
        menuList = ArrayList()
//        ivHomeBack.isClickable = true
        appDatabase = OldMe911Database.getDatabase(mActivity)
        val loginData = appDatabase.loginDao().getAll()
        if (option == SETTING_KEY) {
            rvSubMenuItem.layoutManager = LinearLayoutManager(
                mActivity,
                RecyclerView.VERTICAL,
                false
            )
        } else {
            if (mActivity.resources.getBoolean(R.bool.isTablet))
                rvSubMenuItem.layoutManager = GridLayoutManager(
                    mActivity,
                    3,
                    RecyclerView.VERTICAL,
                    false
                )
            else
                rvSubMenuItem.layoutManager = GridLayoutManager(
                    mActivity,
                    2,
                    RecyclerView.VERTICAL,
                    false
                )

        }
//        menuList.add(DashBoardBean(R.drawable.home,"",0))
        if (option == FIND_YOUR_SELF_KEY) {
//            tvSubTitle.text = mActivity.resources.getString(R.string.find_your_self)
            menuList.add(
                DashBoardBean(
                    R.drawable.ic_road,
                    "",
                    mActivity.resources.getString(R.string.where_have_been),
                    0
                )
            )
        } else if (option == REPORT_KEY) {
//            tvSubTitle.text = mActivity.resources.getString(R.string.report)
            menuList.add(
                DashBoardBean(
                    R.drawable.child_monitor, "",
                    mActivity.resources.getString(R.string.child_monitoring),
                    0
                )
            )
            menuList.add(
                DashBoardBean(
                    R.drawable.business_exp, "",
                    mActivity.resources.getString(R.string.business_track),
                    0
                )
            )
        } else if (option == SETTING_KEY) {
            mActivity.checkNavigationItem(14)
//            tvSubTitle.text = mActivity.resources.getString(R.string.action_settings)
            menuList.add(
                DashBoardBean(
                    R.drawable.user,
                    "",
                    mActivity.resources.getString(R.string.profile),
                    0
                )
            )
            menuList.add(
                DashBoardBean(
                    R.drawable.lock,
                    "",
                    mActivity.resources.getString(R.string.change_password),
                    0
                )
            )
            menuList.add(
                DashBoardBean(
                    R.drawable.app_setting,
                    "",
                    mActivity.resources.getString(R.string.app_settings),
                    0
                )
            )

            if (loginData!=null) {
                if (loginData.isAdmin!=null) {
                    if (loginData.isAdmin) {
                        if (loginData.isChildMissing == false) {
                            menuList.add(
                                DashBoardBean(
                                    R.drawable.payment, "",
                                    mActivity.resources.getString(R.string.payment),
                                    0
                                )
                            )
                        }
                    }
                }
            }
            if (loginData.isChildMissing == false) {
                menuList.add(
                    DashBoardBean(
                        R.drawable.ic_mic,
                        "",
                        mActivity.resources.getString(R.string.str_voice_recognition),
                        0
                    )
                )
            }
            menuList.add(
                DashBoardBean(
                    R.drawable.sms,
                    "",
                    mActivity.resources.getString(R.string.support),
                    0
                )
            )
            menuList.add(
                DashBoardBean(
                    R.drawable.rate_app,
                    "",
                    mActivity.resources.getString(R.string.rate_us),
                    0
                )
            )
            menuList.add(
                DashBoardBean(
                    R.drawable.ic_share_app,
                    "",
                    mActivity.resources.getString(R.string.str_share_friend),
                    0
                )
            )
        } else if (option == PWNED){
            mActivity.checkNavigationItem(9)
            menuList.add(
                DashBoardBean(
                    R.drawable.ic_pwned_email, "", mActivity.resources.getString(
                        R.string.str_email_compromised
                    ), 0
                )
            )
            menuList.add(
                DashBoardBean(
                    R.drawable.ic_pwned_password, "", mActivity.resources.getString(
                        R.string.str_password_compromised
                    ), 0
                )
            )
        } else {
//            tvSubTitle.text = ""
        }
        dashBoardAdapter = DashBoardAdapter(
            mActivity,
            menuList,
            option,
            loginData.isAdmin, (loginData.isChildMissing ?: false)
        )
        if (rvSubMenuItem!=null) {
            rvSubMenuItem.adapter = dashBoardAdapter
        }

        /* ivHomeBack.setOnClickListener {
             mActivity.hideKeyboard()
             mActivity.onBackPressed()
             ivHomeBack.isClickable = false
         }*/

        if (loginData!=null) {
            if (loginData.profilePath != null) {
                //Store Image into local
                Comman_Methods.createDir(mActivity)
            }
        }
    }

    private fun setHeader() {
        tvHeader.setPadding(0, 0, 50, 0)
        when (option) {
            SETTING_KEY -> {
                tvHeader.text = mActivity.resources.getString(R.string.action_settings)
            }
            PWNED -> {
                tvHeader.text = mActivity.resources.getString(R.string.str_hibp)
            }
            else -> {
                tvHeader.text = ""
            }
        }
        Utils.setTextGradientColor(tvHeader)
        iv_menu.visibility = View.VISIBLE
        iv_menu.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.openDrawer()
        }
        mActivity.checkUserActive()
    }

    inner class DashBoardAdapter(
        private val activity: HomeActivity,
        private val menuList: ArrayList<DashBoardBean>,
        private val option: Int,
        private val admin: Boolean,
        private val isChildMissing: Boolean
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(p0: ViewGroup, position: Int): RecyclerView.ViewHolder {
            return when (position){
                SETTING_KEY -> {
                    SettingHolder(LayoutInflater.from(activity).inflate(R.layout.raw_setting_layout, p0, false))
                }
                else -> {
                    DashBoardHolder(
                        LayoutInflater.from(activity).inflate(R.layout.raw_dashboard, p0, false)
                    )
                }
            }
        }

        override fun getItemCount(): Int {
            return menuList.size
        }

        override fun getItemViewType(position: Int): Int {
            return if (option == SETTING_KEY){
                SETTING_KEY
            }else {
                option
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when(holder.itemViewType){
                SETTING_KEY -> {
                    val settingHolder: SettingHolder =  holder as SettingHolder
                    if (menuList[position].imageMenu > 0) {
                        settingHolder.ivSideData.setImageResource(menuList[position].imageMenu)
                    }
                    settingHolder.tvSideData.text = menuList[position].textMenu
                    settingHolder.clSettingParent.setOnClickListener {
                        mActivity.hideKeyboard()
                        Comman_Methods.avoidDoubleClicks(it)
                        when (position) {
                            0 -> {
                                mActivity.addFragment(EditProfileFragment(), true, true, animationType = AnimationType.fadeInfadeOut)
                            }
                            1 -> {
                                mActivity.addFragment(ChangePasswordFragment(), true, true, animationType = AnimationType.fadeInfadeOut)
                            }
                            2 -> {
                                mActivity.addFragment(SettingsFragment(), true, true, AnimationType.fadeInfadeOut)
                            }
                            3 -> {
                                if (admin) {
                                    if (isChildMissing) {
                                        mActivity.addFragment(SupportFragment(), true, true, animationType = AnimationType.fadeInfadeOut)
                                    } else {
                                        mActivity.addFragment(PaymentFragment(), true, true, AnimationType.fadeInfadeOut)
                                    }
                                } else {
                                    mActivity.addFragment(VoiceRecognitionFragment(), true, true, animationType = AnimationType.fadeInfadeOut)
                                }
                            }
                            4 -> {
                                if (admin) {
                                    if (isChildMissing) {
                                        try {
                                            mActivity.visitUrl("market://details?id=" + mActivity.packageName)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            mActivity.visitUrl("http://play.google.com/store/apps/details?id=" + mActivity.packageName)
                                        }
                                    } else {
                                        mActivity.addFragment(VoiceRecognitionFragment(), true, true, AnimationType.fadeInfadeOut)
                                    }
                                } else {
                                    mActivity.addFragment(SupportFragment(), true, true, animationType = AnimationType.fadeInfadeOut)
                                }
                            }
                            5 -> {
                                if (admin) {
                                    if (isChildMissing) {
                                        shareOption()
                                    } else {
                                        mActivity.addFragment(SupportFragment(), true, true, animationType = AnimationType.fadeInfadeOut)
                                    }
                                } else {
                                    try {
                                        mActivity.visitUrl("market://details?id=" + mActivity.packageName)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        mActivity.visitUrl("http://play.google.com/store/apps/details?id=" + mActivity.packageName)
                                    }
                                }
                            }
                            6 -> {
                                if (admin) {
                                    if (!isChildMissing) {
                                        try {
                                            mActivity.visitUrl("market://details?id=" + mActivity.packageName)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            mActivity.visitUrl("http://play.google.com/store/apps/details?id=" + mActivity.packageName)
                                        }
                                    }
                                } else {
                                    shareOption()
                                }
                            }
                            7 -> {
                                shareOption()
                            }
                        }
                    }
                }
                else -> {
                    val nhholder: DashBoardHolder =  holder as DashBoardHolder
                    val imageWidth: Int = Comman_Methods.convertDpToPixels(100F, activity).toInt()
                    val imageHeight: Int = Comman_Methods.convertDpToPixels(100F, activity).toInt()

                    val imageLayoutParams = nhholder.ivDashBoard.layoutParams
                    imageLayoutParams.width = imageWidth
                    imageLayoutParams.height = imageHeight
                    nhholder.ivDashBoard.layoutParams = imageLayoutParams

                    val weight: Int
                    val height: Int
                    if (activity.resources.getBoolean(R.bool.isTablet)) {
                        weight = Utils.calculateNoOfColumns(activity, 3.0)
                        height = Utils.calculateNoOfRows(activity, 4.0)
                    } else {
                        weight = Utils.calculateNoOfColumns(activity, 2.0)
                        height = Utils.calculateNoOfRows(activity, 4.5)
                    }

                    val layoutParams = nhholder.clParentRaw.layoutParams
                    layoutParams.width = weight
                    layoutParams.height = height
                    nhholder.clParentRaw.layoutParams = layoutParams

                    nhholder.tvORDivision.visibility = View.GONE
                    nhholder.btnCompare.visibility = View.GONE

                    if (menuList[position].imageMenu > 0) {
                        nhholder.ivDashBoard.setImageResource(menuList[position].imageMenu)
                    }
                    val tvParams = nhholder.tvDashBoard.layoutParams as RelativeLayout.LayoutParams

                    nhholder.tvDashBoard.text = menuList[position].textMenu


                    if (menuList[position].liveMember > 0) {
                        nhholder.tvLiveMember.visibility = View.VISIBLE
                        nhholder.tvLiveMember.text = menuList[position].liveMember.toString()
                    } else {
                        nhholder.tvLiveMember.visibility = View.GONE
                    }
                    nhholder.flParent.setOnClickListener {
                        mActivity.hideKeyboard()
                        Comman_Methods.avoidDoubleClicks(it)
                        when (position) {
                            0 -> {
                                when (option) {
                                    FIND_YOUR_SELF_KEY -> activity.addFragment(
                                        MemberRouteFragment(),
                                        true,
                                        true,
                                        animationType = AnimationType.fadeInfadeOut
                                    )
                                    REPORT_KEY -> activity.addFragment(
                                        ReportFragment(),
                                        true,
                                        true,
                                        animationType = AnimationType.fadeInfadeOut
                                    )
                                    PWNED -> activity.addFragment(
                                        EmailCompromisedFragment(),
                                        true,
                                        true,
                                        animationType = AnimationType.fadeInfadeOut
                                    )
                                }
                            }
                            1 -> {
                                when (option) {
                                    REPORT_KEY -> activity.addFragment(
                                        BusinessTrackFragment(),
                                        true,
                                        true,
                                        animationType = AnimationType.fadeInfadeOut
                                    )
                                    PWNED -> activity.addFragment(
                                        PasswordCompromisedFragment(),
                                        true,
                                        true,
                                        animationType = AnimationType.fadeInfadeOut
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        inner class DashBoardHolder(view: View) : RecyclerView.ViewHolder(view) {
            var ivDashBoard: ImageView = view.ivDashBoard
            var tvDashBoard: TextView = view.tvDashBoardName
            var tvLiveMember: TextView = view.tvLiveMember
            var tvORDivision: TextView = view.tvORDivision
            var btnCompare: Button = view.btnCompare
            var clParentRaw: RelativeLayout = view.clParentRaw
            val flParent: FrameLayout = view.flParent
        }

        inner class SettingHolder(view: View): RecyclerView.ViewHolder(view){
            var clSettingParent: ConstraintLayout = view.findViewById(R.id.clSettingParent)
            var ivSideData: ImageView = view.findViewById(R.id.ivSideData)
            var tvSideData: TextView = view.findViewById(R.id.tvSideData)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            setHeader()
        }
    }

    private fun shareOption(){
        val view = LayoutInflater.from(mActivity)
            .inflate(R.layout.popup_share_layout, mActivity.window.decorView.rootView as ViewGroup, false)
        if (this::shareAppDialog.isInitialized){
            if (shareAppDialog.isShowing){
                shareAppDialog.dismiss()
            }
        }
        shareAppDialog = BottomSheetDialog(mActivity,R.style.appBottomSheetDialogTheme)
        shareAppDialog.setContentView(view)
        val mBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(view.parent as View)
        mBehavior.isHideable = false
        shareAppDialog.setOnShowListener {
            mBehavior.peekHeight = view.height
        }
        val rvSocialMedia: RecyclerView? = shareAppDialog.findViewById(R.id.rvSocialMedia)
        rvSocialMedia?.layoutManager = GridLayoutManager(mActivity, 3, RecyclerView.VERTICAL, false)
        rvSocialMedia?.adapter = ShareAdapter(mActivity, ShareModel.getShareLinks)
        shareAppDialog.show()
    }

    inner class ShareAdapter(private val context: Context, private val shareList: ArrayList<ShareModel>): RecyclerView.Adapter<ShareAdapter.ShareHolder>(){
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ShareHolder {
            return ShareHolder(LayoutInflater.from(context).inflate(R.layout.raw_share_option, p0, false))
        }

        override fun getItemCount(): Int {
            return shareList.size
        }

        override fun onBindViewHolder(holder: ShareHolder, position: Int) {
            val weight: Int = Utils.calculateNoOfColumns(context, 3.0)
            val height: Int = Utils.calculateNoOfRows(context, 6.0)
            val layoutParams = holder.clShareOption.layoutParams
            layoutParams.width = weight
            layoutParams.height = height
            holder.clShareOption.layoutParams = layoutParams
            holder.ivShareOption.setImageResource(shareList[position].shareLogo)
            holder.tvShareOption.text = context.resources.getString(shareList[position].shareText)
            holder.ivShareOption.setOnClickListener {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
                var sharePackageName: String = ""
                when (position){
                    0 -> sharePackageName = "com.instagram.android"
                    1 -> sharePackageName = "com.twitter.android"
                    2 -> sharePackageName = "com.facebook.katana"
                    3 -> sharePackageName = "com.google.android.gm"
                    4 -> sharePackageName = "com.linkedin.android"
                    5 -> sharePackageName = "com.whatsapp"
                }
                if (appInstalledOrNot(sharePackageName,mActivity)) {
                    val emailIntent = Intent(Intent.ACTION_SEND)
                    emailIntent.type = "text/plain"
                    emailIntent.setPackage(sharePackageName)
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, mActivity.resources.getString(R.string.app_name))
                    emailIntent.putExtra(
                        Intent.EXTRA_TEXT,
                        "http://play.google.com/store/apps/details?id=" + mActivity.packageName
                    )
                    startActivity(Intent.createChooser(emailIntent, "Sending App Link"))
                }else{
                    mActivity.visitUrl("http://play.google.com/store/apps/details?id=$sharePackageName")
                }
                if (this@SubDashBoardFragment::shareAppDialog.isInitialized) {
                    if (shareAppDialog.isShowing) {
                        shareAppDialog.dismiss()
                    }
                }
            }
        }

        inner class ShareHolder(view: View): RecyclerView.ViewHolder(view){
            var clShareOption: ConstraintLayout = view.findViewById(R.id.clShareOption)
            var ivShareOption: ImageView = view.findViewById(R.id.ivShareOption)
            var tvShareOption: TextView = view.findViewById(R.id.tvShareOption)
        }
    }
}
