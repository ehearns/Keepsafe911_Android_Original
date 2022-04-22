package com.keepSafe911.fragments.missingchild

import addFragment
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.keepSafe911.BuildConfig
import com.keepSafe911.HomeActivity
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.model.DashBoardBean
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_missing_dash_board.*
import kotlinx.android.synthetic.main.raw_dashboard_new.view.*
import kotlinx.android.synthetic.main.toolbar_header.*
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class MissingDashBoardFragment : HomeBaseFragment() {
    
    private var param1: String? = ""
    private var param2: String? = ""
    var menuList: ArrayList<DashBoardBean> = ArrayList()
    lateinit var appDatabase: OldMe911Database
    private lateinit var dashBoardAdapter: MissingDashBoardAdapter

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
        return inflater.inflate(R.layout.fragment_missing_dash_board, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHeader()
        menuList = ArrayList()
        appDatabase = OldMe911Database.getDatabase(mActivity)
        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            rvMissingMenuItem.layoutManager = LinearLayoutManager(mActivity, RecyclerView.HORIZONTAL, false)
        } else {
            rvMissingMenuItem.layoutManager = LinearLayoutManager(mActivity, RecyclerView.VERTICAL, false)
        }
        menuList.add(
            DashBoardBean(
                R.drawable.ic_add_missing_child,
                "",
                mActivity.resources.getString(R.string.str_add_missing_child),
                0
            )
        )
        menuList.add(
            DashBoardBean(
                R.drawable.ic_find_missing_child_new,
                "",
                mActivity.resources.getString(R.string.str_find_missing_child),
                0
            )
        )
        dashBoardAdapter = MissingDashBoardAdapter(mActivity, menuList)
        rvMissingMenuItem.adapter = dashBoardAdapter
    }

    private fun setHeader() {
        mActivity.enableDrawer()
        mActivity.checkNavigationItem(10)
        iv_menu.visibility = View.VISIBLE
        ivMenuLogo.visibility = View.VISIBLE
        tvHeader.text = ""
        iv_menu.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.openDrawer()
        }
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
            MissingDashBoardFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    inner class MissingDashBoardAdapter(
        private val activity: HomeActivity?,
        private val menuList: ArrayList<DashBoardBean>
    ) : RecyclerView.Adapter<MissingDashBoardAdapter.DashBoardHolder>() {

        override fun onCreateViewHolder(p0: ViewGroup, position: Int): DashBoardHolder {
            return DashBoardHolder(
                LayoutInflater.from(activity).inflate(R.layout.raw_dashboard_new, p0, false)
            )
        }

        override fun getItemCount(): Int {
            return menuList.size
        }

        override fun onBindViewHolder(holder: DashBoardHolder, position: Int) {
            val weight = if (activity!!.resources.getBoolean(R.bool.isTablet)) {
                Utils.calculateNoOfColumns(activity, 2.0)
            } else {
                Utils.calculateNoOfColumns(activity, 1.0)
            }
            val height: Int = Utils.calculateNoOfRows(activity, 3.3)

            val layoutParams = holder.clParentRaw.layoutParams
            layoutParams.width = weight
            layoutParams.height = height
            holder.clParentRaw.layoutParams = layoutParams
            val imageWidth: Int = Comman_Methods.convertDpToPixels(250F, activity).toInt()
            val imageHeight: Int = Comman_Methods.convertDpToPixels(150F, activity).toInt()
            val imageLayoutParams = holder.flParent.layoutParams
            imageLayoutParams.width = imageWidth
            imageLayoutParams.height = imageHeight
            holder.flParent.layoutParams = imageLayoutParams

            holder.tvTopOptionName.text = menuList[position].textMenu
            holder.tvBottomOptionName.text = menuList[position].textMenu

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                holder.tvBottomOptionName.letterSpacing = 0.04F
                holder.tvTopOptionName.letterSpacing = 0.04F
            }
            if (menuList[position].imageMenu > 0) {
                holder.ivOptionImage.setImageResource(menuList[position].imageMenu)
            }

            holder.flParent.setOnClickListener {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
                when (position) {
                    0 -> {
                        activity.addFragment(
                            AddMissingChildFragment(),
                            true, true,
                            animationType = AnimationType.fadeInfadeOut
                        )
                    }
                    1 -> {
                        activity.addFragment(
                            MissingSubDashBoardFragment(),
                            true, true,
                            animationType = AnimationType.fadeInfadeOut
                        )
                    }
                }
            }
        }

        inner class DashBoardHolder(view: View) : RecyclerView.ViewHolder(view) {
            var tvTopOptionName: TextView = view.tvTopOptionName
            var tvBottomOptionName: TextView = view.tvBottomOptionName
            var ivOptionImage: ImageView = view.ivOptionImage
            var clParentRaw: RelativeLayout = view.clParentRaw
            val flParent: RelativeLayout = view.flParent
        }
    }
}