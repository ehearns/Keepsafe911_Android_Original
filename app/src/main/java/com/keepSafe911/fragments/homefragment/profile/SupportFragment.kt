package com.keepSafe911.fragments.homefragment.profile


import ValidationUtil.Companion.isRequiredField
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.keepSafe911.BuildConfig
import com.keepSafe911.HomeActivity
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.utils.*
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_support.*
import kotlinx.android.synthetic.main.raw_geo_member.view.*
import kotlinx.android.synthetic.main.toolbar_header.*
import java.io.File
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class SupportFragment : HomeBaseFragment(), View.OnClickListener {

    var imageList = ArrayList<Uri>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
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
        return inflater.inflate(R.layout.fragment_support, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity.disableDrawer()
        setHeader()

        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            ed_subject.imeOptions = EditorInfo.IME_ACTION_NEXT or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            ed_description.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        } else {
            ed_subject.imeOptions = EditorInfo.IME_ACTION_NEXT
            ed_description.imeOptions = EditorInfo.IME_ACTION_DONE
        }

        btn_submitsupport.setOnClickListener(this)
        tv_selectfile.setOnClickListener(this)
    }

    private fun setHeader() {
        iv_back.visibility = VISIBLE
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
        tvHeader.setPadding(0, 0, 50, 0)
        tvHeader.text = mActivity.resources.getString(R.string.support)
        Utils.setTextGradientColor(tvHeader)
        mActivity.checkUserActive()
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.tv_selectfile -> {
                mActivity.hideKeyboard()
                if (imageList.size < 10) {
                    Comman_Methods.avoidDoubleClicks(view)
                    if (Build.MANUFACTURER == "samsung") {
                        val intent = Intent("com.sec.android.app.myfiles.PICK_DATA")
                        intent.putExtra("CONTENT_TYPE", "*/*")
                        intent.addCategory(Intent.CATEGORY_DEFAULT)
                        fileSelectedLauncher.launch(intent)
                    } else {
                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                        intent.type = "*/*"
                        fileSelectedLauncher.launch(intent)
                    }
                } else {
                    mActivity.showMessage(mActivity.resources.getString(R.string.you_reached_at_limit))
                }

            }
            R.id.btn_submitsupport -> {
                mActivity.hideKeyboard()
                if (checkforValidations()) {
                    Comman_Methods.avoidDoubleClicks(view)
                    sendfiletoEmail(imageList)
                }
            }
        }
    }

    private fun checkforValidations(): Boolean {
        return when {
            !isRequiredField(ed_subject.text.toString().trim()) -> {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_subject))
                false
            }
            else -> true
        }
    }

    private fun sendfiletoEmail(imageList: ArrayList<Uri>) {
        val emailIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
        emailIntent.type = "vnd.android.cursor.dir/email"
        val to = arrayOf("childsafety@keepsafe911.com")
        emailIntent.putExtra(Intent.EXTRA_EMAIL, to)
        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageList)
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, ed_subject.text.toString().trim())
        emailIntent.putExtra(Intent.EXTRA_TEXT, ed_description.text.toString().trim())
        mailScreenLauncher.launch(Intent.createChooser(emailIntent, "Sending File"))
    }

    var fileSelectedLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            // Get the user's selected place from the Intent.
            val data = result.data
            if (data != null) {
                val uri = data.data
                imageList.add(uri!!)
                setAdapter()
            }
        }
    }
    var mailScreenLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        mActivity.onBackPressed()
    }

    private fun setAdapter() {
        if (imageList.size > 0) {
            rv_files.visibility = VISIBLE
            rv_files?.layoutManager = LinearLayoutManager(mActivity)
            rv_files.adapter = SupportAdapter(imageList, mActivity)
        } else {
            rv_files.visibility = GONE
        }
    }

    inner class SupportAdapter(var fileList: ArrayList<Uri>, var mActivity: HomeActivity) :
        RecyclerView.Adapter<SupportAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): ViewHolder {
            val itemView = LayoutInflater.from(mActivity).inflate(R.layout.raw_geo_member, parent, false)
            return ViewHolder(itemView)
        }

        override fun getItemCount(): Int {
            return fileList.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val filePath = fileList[position]
            if (Build.MANUFACTURER == "samsung") {
                holder.tv_name.text = File(filePath.path ?: "").name
            }else {
                holder.tv_name.text = getFileName(filePath)
            }
            holder.img_close.setOnClickListener {
                fileList.removeAt(position)
                notifyDataSetChanged()
                if (imageList.size > 0) {
                    rv_files.visibility = VISIBLE
                } else {
                    rv_files.visibility = GONE
                }
            }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tv_name: TextView = view.tvGeoMemberName
            val img_close: ImageView = view.ivRemoveGeoMember
        }
    }

    fun getFileName(uri: Uri): String {
        var result: String? = ""
        if (uri.scheme == "content") {
            val cursor = mActivity.contentResolver.query(uri, null, null, null, null)
            cursor.use { cur ->
                if (cur != null && cur.moveToFirst()) {
                    result = cur.getString(cur.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null) {
                if (cut != -1) {
                    result = result?.substring(cut + 1)
                }
            }
        }
        return result ?: ""
    }
}
