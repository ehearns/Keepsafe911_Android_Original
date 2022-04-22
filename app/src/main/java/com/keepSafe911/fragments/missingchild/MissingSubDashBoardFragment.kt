package com.keepSafe911.fragments.missingchild

import addFragment
import android.Manifest
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.keepSafe911.BuildConfig
import com.kotlinpermissions.KotlinPermissions
import com.keepSafe911.HomeActivity
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.fragments.homefragment.detection.IdentityDetectionFragment
import com.keepSafe911.model.DashBoardBean
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.AppPreference
import com.keepSafe911.utils.Comman_Methods
import com.keepSafe911.utils.LocaleUtils
import com.keepSafe911.utils.Utils
import com.yanzhenjie.album.Album
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_missing_sub_dash_board.*
import kotlinx.android.synthetic.main.raw_sub_dashboard.view.*
import kotlinx.android.synthetic.main.toolbar_header.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class MissingSubDashBoardFragment : HomeBaseFragment() {

    private var param1: String? = ""
    private var param2: String? = ""
    lateinit var appDatabase: OldMe911Database
    lateinit var dashBoardAdapter: MissingSubDashBoardAdapter
    var menuList: ArrayList<DashBoardBean> = ArrayList()

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
        return inflater.inflate(R.layout.fragment_missing_sub_dash_board, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHeader()
        appDatabase = OldMe911Database.getDatabase(mActivity)
        menuList = ArrayList()
        menuList.add(DashBoardBean(R.drawable.ic_dslr, "", mActivity.resources.getString(R.string.str_take_photo), 0))
        menuList.add(DashBoardBean(R.drawable.ic_gallery, "", mActivity.resources.getString(R.string.str_upload_photo), 0))
        rvMissingSubMenuItem.layoutManager = GridLayoutManager(
            mActivity,
            2,
            RecyclerView.VERTICAL,
            false
        )
        dashBoardAdapter = MissingSubDashBoardAdapter(
            mActivity,
            menuList
        )
        if (rvMissingSubMenuItem!=null) {
            rvMissingSubMenuItem.adapter = dashBoardAdapter
        }
    }

    private fun setHeader() {
        mActivity.disableDrawer()
        tvHeader.text = ""
        iv_back.visibility = View.VISIBLE
        ivMenuLogo.visibility = View.VISIBLE
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }

        val oldMeContent = mActivity.resources.getString(R.string.app_name)
        val findMissingContent = mActivity.resources.getString(R.string.missing_child_data_description)
        val startIndex = findMissingContent.indexOf(oldMeContent)
        val endIndex = startIndex + oldMeContent.length
        val content = SpannableString(findMissingContent)
        content.setSpan(
            StyleSpan(Typeface.BOLD), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        content.setSpan(
            ForegroundColorSpan(Color.parseColor("#881A46")),
            startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        tvFindMissingChildDescription.text = content
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
            MissingSubDashBoardFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    inner class MissingSubDashBoardAdapter(
        private val activity: HomeActivity?,
        private val menuList: ArrayList<DashBoardBean>
    ) : RecyclerView.Adapter<MissingSubDashBoardAdapter.DashBoardHolder>() {

        override fun onCreateViewHolder(p0: ViewGroup, position: Int): DashBoardHolder {
            return DashBoardHolder(
                LayoutInflater.from(activity).inflate(R.layout.raw_sub_dashboard, p0, false)
            )
        }

        override fun getItemCount(): Int {
            return menuList.size
        }

        override fun onBindViewHolder(holder: DashBoardHolder, position: Int) {
            val imageWidth: Int = Comman_Methods.convertDpToPixels(150F, activity!!).toInt()
            val imageHeight: Int = Comman_Methods.convertDpToPixels(100F, activity).toInt()

            val imageLayoutParams = holder.flParent.layoutParams
            imageLayoutParams.width = imageWidth
            imageLayoutParams.height = imageHeight
            holder.flParent.layoutParams = imageLayoutParams

            val weight: Int = Utils.calculateNoOfColumns(activity, 2.0)
            val height: Int = Utils.calculateNoOfRows(activity, 3.3)

            val layoutParams = holder.clParentRaw.layoutParams
            layoutParams.width = weight
            layoutParams.height = height
            holder.clParentRaw.layoutParams = layoutParams

            if (menuList[position].imageMenu > 0) {
                holder.ivOptionImage.setImageResource(menuList[position].imageMenu)
            }
            holder.tvBottomOptionName.text = menuList[position].textMenu

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                holder.tvBottomOptionName.letterSpacing = 0.04F
            }

            holder.flParent.setOnClickListener {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
                when (position) {
                    0 -> {
                        setPermission(position)
                    }
                    1 -> {
                        setPermission(position)
                    }
                }
            }

        }

        inner class DashBoardHolder(view: View) : RecyclerView.ViewHolder(view) {
            var tvBottomOptionName: TextView = view.tvBottomOptionName
            var ivOptionImage: ImageView = view.ivOptionImage
            var clParentRaw: RelativeLayout = view.clParentRaw
            val flParent: RelativeLayout = view.flParent
        }
    }

    private fun getLatestFile(): String {
        val timeStamp =
            SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(Date())
        val root = Utils.getStorageRootPath(mActivity)
        if (!root.exists()) {
            root.mkdir()
        }
        val originalFileName = File(
            root.path + File.separator +
                    "IMG_" + timeStamp + ".jpg"
        )
        return originalFileName.absolutePath
    }

    private fun setPermission(segment: Int) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
                .permissions(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )

                .onAccepted { permissions ->
                    mActivity.hideKeyboard()
                    if (permissions.size == 2) {
                        if (segment > 0) {
                            openCameraWithGallery()
                        } else {
                            openCamera()
                        }
                    }
                }
                .onDenied {
                    setPermission(segment)
                }
                .onForeverDenied {
                    mActivity.showMessage(mActivity.resources.getString(R.string.permission_app_set))
                }
                .ask()
        } else {
            KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
                .permissions(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )

                .onAccepted { permissions ->
                    mActivity.hideKeyboard()
                    if (permissions.size == 3) {
                        if (segment > 0) {
                            openCameraWithGallery()
                        } else {
                            openCamera()
                        }
                    }
                }
                .onDenied {
                    setPermission(segment)
                }
                .onForeverDenied {
                    mActivity.showMessage(mActivity.resources.getString(R.string.permission_app_set))
                }
                .ask()
        }
    }

    private fun openCamera() {
        Album.camera(mActivity)
            .image()
            .filePath(getLatestFile())
            .onResult { result ->
                if (result != null) {
                    if (result.isNotEmpty()) {
                        val imageFile = File(result)
                        if (imageFile != null) {
                            if (imageFile.exists()) {
                                mActivity.addFragment(
                                    IdentityDetectionFragment.newInstance(imageFile.absolutePath),
                                    true,
                                    true,
                                    animationType = AnimationType.fadeInfadeOut
                                )
                            }
                        }
                    }
                }
            }
            .onCancel {  }
            .start()
    }

    private fun openCameraWithGallery() {

        Album.image(mActivity) // Image and video mix options.
            .singleChoice() // Multi-Mode, Single-Mode: singleChoice().
            .columnCount(3) // The number of columns in the page list.
            .camera(false) // Whether the camera appears in the Item.
            .onResult { result ->
                if (result != null) {
                    if (result.size > 0) {
                        println("result[0].mimeType = ${result[0].mimeType}")
                        if (result[0].mimeType.contains("image")) {
                            if (result[0].path != null) {
                                println("result[0].path = ${result[0].path}")
                                if (result[0].path != "") {
                                    val imageFile = File(result[0].path)
                                    if (imageFile != null) {
                                        if (imageFile.exists()) {
                                            mActivity.addFragment(
                                                IdentityDetectionFragment.newInstance(imageFile.absolutePath),
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
            }
            .onCancel { }
            .start()
    }
}