package com.keepSafe911.fragments.homefragment.detection

import addFragment
import android.app.Dialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.keepSafe911.BuildConfig
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.fragments.missingchild.MissingChildDetailsFragment
import com.keepSafe911.model.response.findmissingchild.MatchResult
import com.keepSafe911.model.response.findmissingchild.RunSearchResponse
import com.keepSafe911.utils.*
import com.keepSafe911.webservices.WebApiClient
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_identity_detection.*
import kotlinx.android.synthetic.main.toolbar_header.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Response
import takeCall
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class IdentityDetectionFragment : HomeBaseFragment() {

    private var identificationUrl: String = ""
    private lateinit var oldMe911CallDialog: BottomSheetDialog
    private lateinit var shareAppDialog: Dialog
    var rangeMatchResult: ArrayList<MatchResult> = ArrayList()

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    companion object {
        fun newInstance(
            identificationUrl: String
        ): IdentityDetectionFragment {
            val args = Bundle()
            args.putString(ARG_PARAM1, identificationUrl)
            val fragment = IdentityDetectionFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            identificationUrl = it.getString(ARG_PARAM1, "")
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
        return inflater.inflate(R.layout.fragment_identity_detection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHeader()
        val options = RequestOptions()
            .fitCenter()
            .placeholder(R.drawable.ic_person_placeholder)
            .error(R.drawable.ic_person_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .priority(Priority.LOW)

        try {
            Glide.with(mActivity).load(identificationUrl).apply(options).into(ivDetectPhoto)
        } catch (e: Exception) {
            Glide.with(mActivity.applicationContext).load(identificationUrl).apply(options)
                .into(ivDetectPhoto)
        }
//        tvFaceIdentified.text = mActivity.resources.getString(R.string.str_face_identify, 0)
        Handler(Looper.getMainLooper()).postDelayed({
            if (ConnectionUtil.isInternetAvailable(mActivity)) {
                val imageFile = File(identificationUrl)
                if (imageFile != null) {
                    if (imageFile.exists()) {
                        callReSearchImage(imageFile)
                    } else {
                        mActivity.showMessage(mActivity.resources.getString(R.string.file_does_not_exists))
                    }
                } else {
                    mActivity.showMessage(mActivity.resources.getString(R.string.file_does_not_exists))
                }
            } else {
                Utils.showNoInternetMessage(mActivity)
            }
        }, 700)
    }

    private fun callReSearchImage(identificationUrl: File) {
        Comman_Methods.isProgressShow(mActivity)
        mActivity.isSpeedAvailable()

        val compressIdentificationUrl =
            File(Comman_Methods.compressImage(identificationUrl.absolutePath, mActivity))
        val imageBody: RequestBody =
            compressIdentificationUrl.asRequestBody("multipart/form-data".toMediaTypeOrNull())

        val part: MultipartBody.Part = MultipartBody.Part.createFormData(
            "file1",
            compressIdentificationUrl.name,
            imageBody
        )

        val findChildCall = WebApiClient.getInstance(mActivity)
            .webApi_with_MultiPart?.runSearchImage(part)
        findChildCall?.enqueue(object : retrofit2.Callback<RunSearchResponse> {
            override fun onResponse(
                call: Call<RunSearchResponse>,
                response: Response<RunSearchResponse>
            ) {
                val matchResult: ArrayList<MatchResult> = ArrayList()
                rangeMatchResult = ArrayList()
                if (response.isSuccessful) {
                    response.body()?.let {
                        val matches = it.matches ?: ArrayList()
                        if (matches.size > 0) {
                            for (i in matches.indices) {
                                matchResult.addAll(matches[i])
                            }
                            for (v in matchResult.indices) {
                                val matchScore = matchResult[v].matchScore ?: 0.0
                                if (matchScore < 0.3) {
                                    rangeMatchResult.add(matchResult[v])
                                }
                            }
                            /*try {
                                matchResult.sortByDescending { it.matchScore }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }*/
                            if (rangeMatchResult.size > 0) {
                                visibleRecycler(true)
                                Handler(Looper.getMainLooper()).postDelayed({
                                    showMatchFoundMessage(mActivity.resources.getString(R.string.str_match_found), 1)
                                }, 500)
                            } else {
                                visibleRecycler(false)
                                showMatchFoundMessage(mActivity.resources.getString(R.string.str_no_match_found), 0)
                            }
                        } else {
                            visibleRecycler(false)
                            showMatchFoundMessage(mActivity.resources.getString(R.string.str_no_match_found), 0)
                        }
                    }
                }
                setReSearchAdapter(rangeMatchResult)
                Comman_Methods.isProgressHide()
                deleteExtraFile(compressIdentificationUrl)
            }

            override fun onFailure(call: Call<RunSearchResponse>, t: Throwable) {
                Comman_Methods.isProgressHide()
                visibleRecycler(false)
                deleteExtraFile(compressIdentificationUrl)
                showMatchFoundMessage(mActivity.resources.getString(R.string.str_no_match_found), 0)
            }
        })
    }

    fun showMatchFoundMessage(imageMatchMessage: String, resultColor: Int){
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.popup_compare_image_result, null)
        val mDialog = android.app.AlertDialog.Builder(mActivity)
        mDialog.setView(dialogLayout)
        if (this::shareAppDialog.isInitialized){
            if (shareAppDialog.isShowing){
                shareAppDialog.dismiss()
            }
        }
        shareAppDialog = mDialog.create()
        shareAppDialog.window?.attributes?.windowAnimations = R.style.animationForDialog
        shareAppDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val tvFaceMatchFoundResult: TextView = dialogLayout.findViewById(R.id.tvFaceMatchFoundResult)
        val ivFaceMatchFoundResult: ImageView = dialogLayout.findViewById(R.id.ivFaceMatchFoundResult)
        val btnCompareOk: Button = dialogLayout.findViewById(R.id.btnCompareOk)

        /**
         * getting start and end index of result text.
         */

        val content = SpannableString(imageMatchMessage)

        if (resultColor > 0) {
            content.setSpan(
                ForegroundColorSpan(Color.GREEN),
                0,
                imageMatchMessage.length,
                0
            )
            ivFaceMatchFoundResult.setImageDrawable(ContextCompat.getDrawable(mActivity, R.drawable.ic_correct_mark))
//            tvFaceMatchFoundResult.setTextColor(mActivity.resources.getColor(R.color.special_green))
        } else {
            content.setSpan(
                ForegroundColorSpan(Color.RED),
                0,
                imageMatchMessage.length,
                0
            )
            ivFaceMatchFoundResult.setImageDrawable(ContextCompat.getDrawable(mActivity, R.drawable.ic_incorrect_mark))
//            tvFaceMatchFoundResult.setTextColor(mActivity.resources.getColor(R.color.color_red))
        }

        tvFaceMatchFoundResult.text = content
//        tvFaceMatchFoundResult.text = imageMatchMessage

        btnCompareOk.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            shareAppDialog.dismiss()
            if (resultColor > 0) {
                show911CallPopUp()
            }
//            mActivity.onBackPressed()
        }

        // Do code here

        shareAppDialog.setCancelable(false)
        shareAppDialog.show()
    }

    private fun show911CallPopUp() {
        val view = LayoutInflater.from(mActivity)
            .inflate(
                R.layout.popup_call_layout,
                mActivity.window.decorView.rootView as ViewGroup,
                false
            )
        if (this::oldMe911CallDialog.isInitialized) {
            if (oldMe911CallDialog.isShowing) {
                oldMe911CallDialog.dismiss()
            }
        }
        oldMe911CallDialog = BottomSheetDialog(mActivity, R.style.appBottomSheetDialogTheme)
        oldMe911CallDialog.setContentView(view)
        val mBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(view.parent as View)
        mBehavior.isHideable = false
        oldMe911CallDialog.setOnShowListener {
            mBehavior.peekHeight = view.height
        }
        val tvCallOk: TextView? = oldMe911CallDialog.findViewById(R.id.tvCallOk)
        val tvCallCancel: TextView? = oldMe911CallDialog.findViewById(R.id.tvCallCancel)
        tvCallCancel?.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            oldMe911CallDialog.dismiss()
        }
        tvCallOk?.setOnClickListener {
            mActivity.takeCall("911")
            oldMe911CallDialog.dismiss()
        }
        oldMe911CallDialog.show()
    }

    private fun deleteExtraFile(deleteFiles: File) {
        try {
            if (deleteFiles.exists()) {
                deleteFiles.delete()
                if (deleteFiles.exists()) {
                    deleteFiles.canonicalFile.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun visibleRecycler(isListAvailable: Boolean) {
        if (isListAvailable) {
            tvNoMatchFound.visibility = View.GONE
            rvFaceIdentified.visibility = View.VISIBLE
        } else {
            tvNoMatchFound.visibility = View.VISIBLE
            rvFaceIdentified.visibility = View.GONE
        }
    }

    private fun setReSearchAdapter(matchResultList: ArrayList<MatchResult>) {
        rvFaceIdentified.layoutManager = LinearLayoutManager(
            mActivity,
            RecyclerView.VERTICAL,
            false
        )
        val reSearchAdapter = ReSearchAdapter(mActivity, matchResultList)
        rvFaceIdentified.adapter = reSearchAdapter
        reSearchAdapter.notifyDataSetChanged()
    }

    private fun setHeader() {
        tvHeader.setPadding(0, 0, 50, 0)
        tvHeader.text = mActivity.resources.getString(R.string.str_find_missing_child).uppercase()
        Utils.setTextGradientColor(tvHeader)
        iv_back.visibility = View.VISIBLE

        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }

        val plainText =
            mActivity.resources.getString(R.string.str_no_match_found) + "\n" + mActivity.resources.getString(
                R.string.str_try_another_image
            )
        val tryAgain = mActivity.resources.getString(R.string.str_try_another_image)

        /**
         * getting start and end index of privacy policy text.
         */
        val privacyPolicyStartIndex = plainText.indexOf(tryAgain)
        val privacyPolicyEndIndex = plainText.indexOf(tryAgain) + tryAgain.length

        val content = SpannableString(plainText)
        content.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(widget)
                mActivity.onBackPressed()
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.isUnderlineText = false
            }
        }, privacyPolicyStartIndex, privacyPolicyEndIndex, 0)

        content.setSpan(
            ForegroundColorSpan(Color.BLACK),
            privacyPolicyStartIndex,
            privacyPolicyEndIndex,
            0
        )

        content.setSpan(
            UnderlineSpan(),
            privacyPolicyStartIndex,
            privacyPolicyEndIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        content.setSpan(RelativeSizeSpan(0.7f), privacyPolicyStartIndex, privacyPolicyEndIndex, 0)

        tvNoMatchFound.text = content
        tvNoMatchFound.movementMethod = LinkMovementMethod.getInstance()
        mActivity.checkUserActive()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            setHeader()
        }
    }

    inner class ReSearchAdapter(val context: Context, private val matchResultList: ArrayList<MatchResult>) : RecyclerView.Adapter<ReSearchAdapter.ReSearchHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReSearchHolder {
            return ReSearchHolder(
                LayoutInflater.from(context).inflate(
                    R.layout.raw_missing_child,
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: ReSearchHolder, position: Int) {
            val matchResultModel: MatchResult = matchResultList[position]

//            val url = URLEncoder.encode(matchResultModel.imageName, "UTF-8")

            val options = RequestOptions()
                .centerCrop()
                .placeholder(R.drawable.ic_person_placeholder)
                .error(R.drawable.ic_person_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .priority(Priority.LOW)

            try {
                Glide.with(context).load(matchResultModel.imageUrl).apply(options).into(holder.ivMissingChildImage)
            } catch (e: Exception) {
                Glide.with(context.applicationContext).load(matchResultModel.imageUrl).apply(options)
                    .into(holder.ivMissingChildImage)
            }

            val firstName: String = matchResultModel.firstName ?: ""
            val lastName: String = matchResultModel.lastName ?: ""
            holder.tvMissingChildName.text = "$firstName $lastName"
            val formatter = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
            var missingDate = ""
            try {
                var date1: Date? = null
                if (matchResultModel.dateMissing != null) {
                    date1 = formatter.parse(matchResultModel.dateMissing ?: "")
                }
                val target = SimpleDateFormat(OUTPUT_DATE_FORMAT2)
                if (date1 != null) {
                    missingDate = target.format(date1)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            holder.tvMissingSince.text = missingDate
            val missingCity = matchResultModel.missingCity ?: ""
            val missingState =
                if (matchResultModel.missingState != null) if (matchResultModel.missingState != "") ", " + matchResultModel.missingState else "" else ""
            holder.tvMissingFrom.text = "$missingCity$missingState"
            val childAge =
                if (matchResultModel.age != null) matchResultModel.age.toString() else "0"
            holder.tvMissingAge.text = childAge
            val decimalSymbols = DecimalFormatSymbols.getInstance(Locale.US)
            val matchScore = if (matchResultModel.matchScore != null) DecimalFormat("##.##", decimalSymbols).format(
                matchResultModel.matchScore ?: 0.00
            ).toString() else "0.0"
            holder.tvMissingMatchScore.text = matchScore
            holder.llMain.setOnClickListener{
                mActivity.addFragment(
                    MissingChildDetailsFragment.newInstance(matchResultModel),
                    true, true,
                    animationType = AnimationType.fadeInfadeOut)
            }
        }

        override fun getItemCount(): Int {
            return matchResultList.size
        }

        inner class ReSearchHolder(view: View) : RecyclerView.ViewHolder(view) {
            var ivMissingChildImage: ImageView = view.findViewById(R.id.ivMissingChildImage)
            var tvMissingChildName: TextView = view.findViewById(R.id.tvMissingChildName)
            var tvMissingSince: TextView = view.findViewById(R.id.tvMissingSince)
            var tvMissingFrom: TextView = view.findViewById(R.id.tvMissingFrom)
            var tvMissingAge: TextView = view.findViewById(R.id.tvMissingAge)
            var tvMissingMatchScore: TextView = view.findViewById(R.id.tvMissingMatchScore)
            var llMain: LinearLayout = view.findViewById(R.id.llMain)
        }
    }
}