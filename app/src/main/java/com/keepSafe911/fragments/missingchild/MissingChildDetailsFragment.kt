package com.keepSafe911.fragments.missingchild

import addFragment
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import appInstalledOrNot
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import com.keepSafe911.BuildConfig
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.model.ShareModel
import com.keepSafe911.model.response.findmissingchild.MatchResult
import com.keepSafe911.model.response.findmissingchild.MissingChildTaskModel
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import hideKeyboard
import kotlinx.android.synthetic.main.dialog_missing_child_details.*
import kotlinx.android.synthetic.main.toolbar_header.*
import visitUrl
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class MissingChildDetailsFragment : HomeBaseFragment(), View.OnClickListener {
    private var matchBean: MatchResult = MatchResult()
    lateinit var appDatabase: OldMe911Database
    var loginObject: LoginObject = LoginObject()
    var childTaskResponseList: ArrayList<MissingChildTaskModel> = ArrayList()
    var filteredChildTaskResponseList: ArrayList<MissingChildTaskModel> = ArrayList()
    private lateinit var shareAppDialog: BottomSheetDialog
    private var missingChildId: String = ""
    private var isFromNotification: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            matchBean = it.getParcelable(ARG_PARAM1) ?: MatchResult()
            missingChildId = it.getString(ARG_PARAM2, "")
            childTaskResponseList = matchBean.lstChildTaskResponse ?: ArrayList()
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
        return inflater.inflate(R.layout.dialog_missing_child_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHeader()
        initData()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            setHeader()
        }
    }

    private fun setHeader() {
        appDatabase = OldMe911Database.getDatabase(mActivity)
        loginObject = appDatabase.loginDao().getAll()
        mActivity.disableDrawer()
        /*if (matchBean.allTaskCompleted == true) {
            tvShare.visibility = View.VISIBLE
            tvHeader.setPadding(0, 0, 0, 0)
        } else {
            tvShare.visibility = View.GONE
            tvHeader.setPadding(0, 0, 50, 0)
        }*/
        tvHeader.text = mActivity.resources.getString(R.string.str_missing_child_detail).uppercase()
        Utils.setTextGradientColor(tvHeader)
        iv_back.visibility = View.VISIBLE
        tvShare.visibility = View.VISIBLE
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
        tvShare.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            downLoadTask(matchBean)
        }
        mActivity.checkUserActive()
        val userId = matchBean.userId ?: 0
        if (userId > 0) {
            callMissingChildList(loginObject.memberID, matchBean.id ?: 0)
        } else {
            btnMissingTask.visibility = View.GONE
        }
        isFromNotification = false
        if (missingChildId.isNotEmpty()) {
            isFromNotification = true
            callMissingChildList(loginObject.memberID, missingChildId.toInt())
            missingChildId = ""
        }
    }

    private fun initData() {
        btnMissingTask.setOnClickListener(this)

        sdvChild.loadFrescoImage(mActivity, matchBean.imageUrl ?: "", 1)

        tvChildName.text = (matchBean.firstName ?: "") + " " + (matchBean.lastName ?: "")
        val missingCity = matchBean.missingCity ?: ""
        val missingState =
            if (matchBean.missingState != null) if (matchBean.missingState != "") ", " + matchBean.missingState else "" else ""
        tvMissingFrom.text = "$missingCity$missingState"
        var missingDate = ""
        val formatter = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
        try {
            var date1: Date? = null
            if (matchBean.dateMissing != null) {
                date1 = formatter.parse(matchBean.dateMissing ?: "")
            }
            val target = SimpleDateFormat(OUTPUT_DATE_FORMAT2)
            if (date1 != null) {
                missingDate = target.format(date1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        tvMissingSince.text = missingDate
        val childAge =
            if (matchBean.age != null) matchBean.age.toString() else "0"
        tvAge.text = childAge
        tvMissingSituation.text = matchBean.lastSeenSituation ?: "-"
        tvContactNo.text = matchBean.contactNumber ?: "-"
        tvHairColor.text = matchBean.hairColor ?: "-"
        tvEyeColor.text = matchBean.eyeColor ?: "-"
        tvHeight.text = String.format("%.2f", matchBean.height ?: 0.0)
        tvWeight.text = String.format("%.2f", matchBean.weight ?: 0.0)
        tvComplexion.text = matchBean.complexion ?: "-"

        tvGlasses.text = if (matchBean.isWearLenses == true)
            mActivity.resources.getString(R.string.yes) else mActivity.resources.getString(R.string.no)
        tvBraces.text = if (matchBean.isBracesOnTeeth == true)
            mActivity.resources.getString(R.string.yes) else mActivity.resources.getString(R.string.no)

        tvPhysicalAttributes.text = if (matchBean.isPhysicalAttributes == true)
            (matchBean.physicalAttributes ?: mActivity.resources.getString(R.string.no))
        else mActivity.resources.getString(R.string.no)
    }

    private fun callMissingChildList(memberID: Int, missingChildId: Int) {
        Utils.missingChildByUser(mActivity, memberID, missingChildId, object : CommonApiListener {
            override fun childTaskListResponse(
                status: Boolean,
                matchResultData: ArrayList<MatchResult>,
                missingChildTaskList: ArrayList<MissingChildTaskModel>,
                message: String,
                responseMessage: String
            ) {
                if (isFromNotification) {
                    for (k in 0 until matchResultData.size) {
                        if (missingChildId == matchResultData[k].id) {
                            matchBean = matchResultData[k]
                            initData()
                        }
                    }
                }
                val createdId = matchBean.userId ?: 0
                filteredChildTaskResponseList = ArrayList()
                filteredChildTaskResponseList.addAll(missingChildTaskList)
                if (missingChildTaskList.size > 0) {
                    btnMissingTask.visibility = View.VISIBLE
                    if (createdId == loginObject.memberID) {
                        filteredChildTaskResponseList.addAll(childTaskResponseList)
                    }
                } else {
                    if (createdId == loginObject.memberID) {
                        btnMissingTask.visibility = View.VISIBLE
                    } else {
                        btnMissingTask.visibility = View.GONE
                    }
                }
                filteredChildTaskResponseList = filteredChildTaskResponseList.distinctBy { it.taskId } as ArrayList<MissingChildTaskModel>
                if (filteredChildTaskResponseList.size > 0) {
                    var sortByChildTask: ArrayList<MissingChildTaskModel> = ArrayList()
                    try {
                        sortByChildTask =
                            mutableListOf(filteredChildTaskResponseList.sortWith(compareBy { child ->
                                child.taskId ?: 0
                            })) as ArrayList<MissingChildTaskModel>
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }, true)
    }

    companion object {
        fun newInstance(matchResult: MatchResult, missingChildId: String = ""): MissingChildDetailsFragment {
            val fragment = MissingChildDetailsFragment()
            val args = Bundle()
            args.putParcelable(ARG_PARAM1, matchResult)
            args.putString(ARG_PARAM2, missingChildId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btnMissingTask -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                mActivity.addFragment(MissingChildTaskFragment.newInstance(
                    false, matchBean.id ?: 0,
                    filteredChildTaskResponseList, matchBean.userId ?: 0),
                    true, true,
                    animationType = AnimationType.fadeInfadeOut)
            }
        }
    }

    private fun downLoadTask(missingChildData: MatchResult) {
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        Comman_Methods.isProgressShow(mActivity)

        executor.execute {
            val folder = Utils.getStorageRootPath(mActivity)
            if (!folder.exists()) {
                folder.mkdir()
            }
            val subFolder = File(folder, "/MissingChild/")
            if (!subFolder.exists()) {
                subFolder.mkdir()
            }
            val fileName = (missingChildData.firstName ?: "") + "_" + (missingChildData.lastName ?: "") + "_" + (missingChildData.id ?: 0)
            val storeFileName = "$fileName.pdf"
            val pdfFile = File(subFolder.toString() + File.separator + storeFileName)
            try {
                pdfFile.createNewFile()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val file: OutputStream = FileOutputStream(pdfFile)

            val document= Document()

            PdfWriter.getInstance(document, file)

            document.open()

            //Inserting Image in PDF

            /*var image = Image.getInstance ("src/pdf/java4s.png");
            image.scaleAbsolute(120f, 60f);//image width,height*/

            //Inserting Table in PDF

            val decimalSymbols = DecimalFormatSymbols.getInstance(Locale.US)

            val childName = (missingChildData.firstName ?: "") + " " + (missingChildData.lastName ?: "")
            var missingDate = ""
            val formatter = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
            try {
                var date1: Date? = null
                if (missingChildData.dateMissing != null) {
                    date1 = formatter.parse(missingChildData.dateMissing ?: "")
                }
                val target = SimpleDateFormat(OUTPUT_DATE_FORMAT2)
                if (date1 != null) {
                    missingDate = target.format(date1)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val yes = mActivity.resources.getString(R.string.yes)
            val no = mActivity.resources.getString(R.string.no)
            val missingCity = missingChildData.missingCity ?: ""
            val missingState =
                if (missingChildData.missingState != null) if (missingChildData.missingState != "") ", " + missingChildData.missingState else "" else ""
            val childAge = if (missingChildData.age != null) missingChildData.age.toString() else "0"
            val lastSeenSituation = missingChildData.lastSeenSituation ?: "-"
            val contactNumber = missingChildData.contactNumber ?: "-"
            val hairColor = missingChildData.hairColor ?: "-"
            val eyeColor = missingChildData.eyeColor ?: "-"
            val height = String.format("%.2f", missingChildData.height ?: 0.0)
            val weight = String.format("%.2f", missingChildData.weight ?: 0.0)
            val complexion = missingChildData.complexion ?: "-"
            val glasses =  if (missingChildData.isWearLenses == true) yes else no
            val teeth = if (missingChildData.isBracesOnTeeth == true) yes else no
            val physical = if (missingChildData.isPhysicalAttributes == true)
                (missingChildData.physicalAttributes ?: no) else no

            var mainParagraph: Paragraph = Paragraph()
            mainParagraph = Paragraph (childName, Font(
                Font.FontFamily.TIMES_ROMAN, 20f,
                Font.BOLD, BaseColor(89, 137, 71)
            ))
            mainParagraph.alignment = Element.ALIGN_CENTER

            emptyLine(mainParagraph)

            val childImage = Image.getInstance(missingChildData.imageUrl ?: "")
            childImage.scaleToFit(150f,150f)
            childImage.alignment = Image.ALIGN_CENTER
            mainParagraph.add(childImage)

            emptyLine(mainParagraph, 1)

            val table= PdfPTable(2)

            addDataInTable(table, mActivity.resources.getString(R.string.str_missing_since_prompt), missingDate)
            addDataInTable(table, mActivity.resources.getString(R.string.str_missing_from_prompt), "$missingCity$missingState")
            addDataInTable(table, mActivity.resources.getString(R.string.str_missing_age_prompt), childAge)
            addDataInTable(table, mActivity.resources.getString(R.string.str_missing_situation_prompt), lastSeenSituation)
            addDataInTable(table, mActivity.resources.getString(R.string.str_missing_phone_prompt), contactNumber)
            addDataInTable(table, mActivity.resources.getString(R.string.str_missing_hair_prompt), hairColor)
            addDataInTable(table, mActivity.resources.getString(R.string.str_missing_eye_prompt), eyeColor)
            addDataInTable(table, mActivity.resources.getString(R.string.str_height), height)
            addDataInTable(table, mActivity.resources.getString(R.string.str_weight), weight)
            addDataInTable(table, mActivity.resources.getString(R.string.str_missing_complexion), complexion)
            addDataInTable(table, mActivity.resources.getString(R.string.str_eye_glasses_lenses), glasses)
            addDataInTable(table, mActivity.resources.getString(R.string.str_braces_on_teeth), teeth)
            addDataInTable(table, mActivity.resources.getString(R.string.str_missing_physical), physical)

            mainParagraph.add(table)

            document.add(mainParagraph)
            document.newPage()

            var taskList = missingChildData.lstChildTaskResponse ?: ArrayList()
            taskList = taskList.distinctBy { it.taskId } as ArrayList<MissingChildTaskModel>
            var sortByChildTask: ArrayList<MissingChildTaskModel> = ArrayList()
            try {
                sortByChildTask =
                    mutableListOf(taskList.sortWith(compareBy { child ->
                        child.taskId ?: 0
                    })) as ArrayList<MissingChildTaskModel>
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (taskList.size > 0) {
                mainParagraph = Paragraph (mActivity.resources.getString(R.string.str_task_missing_child).uppercase(), Font(
                    Font.FontFamily.TIMES_ROMAN, 20f,
                    Font.BOLD, BaseColor(89, 137, 71)
                ))
                mainParagraph.alignment = Element.ALIGN_CENTER

                emptyLine(mainParagraph)
                document.add(mainParagraph)
            }
            for (i in taskList.indices){
                val taskData = taskList[i]
                val textAnswer = taskData.answerInText ?: ""
                val assignToName = taskData.assignToName ?: ""
                val formatter1 = SimpleDateFormat(INPUT_DATE_FORMAT)
                val target = SimpleDateFormat(CHECK_DATE_TIME2)
                val taskCreateOn = taskData.createdOn ?: Utils.getCurrentTimeStamp()
                var createOnTime = ""
                try {
                    var date1: Date? = null
                    if (taskCreateOn.isNotEmpty()) {
                        date1 = formatter1.parse(taskCreateOn)
                    }
                    if (date1 != null) {
                        createOnTime = target.format(date1)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }


                mainParagraph = Paragraph ("${i+1}. ${taskData.question}", Font(
                    Font.FontFamily.TIMES_ROMAN, 18f
                ))
                mainParagraph.alignment = Element.ALIGN_LEFT
                mainParagraph.spacingAfter = 20f
                document.add(mainParagraph)
                if (textAnswer.trim().isNotEmpty()) {
                    val answerParagraph = Paragraph(
                        "- $textAnswer", Font(
                            Font.FontFamily.TIMES_ROMAN, 18f, Font.BOLD
                        )
                    )
                    answerParagraph.alignment = Element.ALIGN_LEFT
                    mainParagraph.spacingAfter = 20f
                    document.add(answerParagraph)
                }

                val nameChunk = Chunk(assignToName, Font(
                    Font.FontFamily.TIMES_ROMAN, 18f,
                    Font.NORMAL, BaseColor(89, 137, 71)
                ))

                val timeChunk = Chunk(", $createOnTime", Font(
                    Font.FontFamily.TIMES_ROMAN, 18f
                ))

                val logParagraph = Paragraph()
                logParagraph.add(nameChunk)
                logParagraph.add(timeChunk)
                logParagraph.alignment = Element.ALIGN_RIGHT

                emptyLine(logParagraph)
                document.add(logParagraph)
            }

//            document.open()//PDF document opened........

//            document.add(Chunk.NEWLINE)   //Something like in HTML 🙂

//            document.add(mainParagraph)


            document.add(Chunk.NEWLINE)   //Something like in HTML 🙂

//            document.newPage()            //Opened new page

            document.close()

            file.close()
            handler.post {
                Comman_Methods.isProgressHide()

                if (pdfFile != null) {
                    shareOption(pdfFile)
                }
            }
        }
    }

    private fun addDataInTable(table: PdfPTable, hint: String, data: String) {
        val childMissingAgeHint = PdfPCell(Paragraph ( "$hint:", Font(
            Font.FontFamily.TIMES_ROMAN, 18f,
            Font.NORMAL, BaseColor(89, 137, 71)
        )))
        childMissingAgeHint.horizontalAlignment = Element.ALIGN_LEFT
        childMissingAgeHint.border = Rectangle.NO_BORDER
        childMissingAgeHint.setPadding(3.0f)
        table.addCell(childMissingAgeHint)

        val childMissingAge = PdfPCell(Paragraph (data, Font(
            Font.FontFamily.TIMES_ROMAN, 18f,
            Font.NORMAL
        )))
        childMissingAge.horizontalAlignment = Element.ALIGN_LEFT
        childMissingAge.border = Rectangle.NO_BORDER
        childMissingAge.setPadding(3.0f)
        table.addCell(childMissingAge)
    }

    private fun emptyLine(paragraph: Paragraph, lines: Int = 2) {
        for (i in 0 until lines) {
            paragraph.add(Paragraph(" "))
        }
    }

    private fun shareOption(pdfFile: File) {
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
        rvSocialMedia?.adapter = ShareAdapter(mActivity, ShareModel.getShareLinks, pdfFile)
        shareAppDialog.show()
    }

    inner class ShareAdapter(private val context: Context, private val shareList: ArrayList<ShareModel>,val pdfFile: File): RecyclerView.Adapter<ShareAdapter.ShareHolder>(){
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
                    val emailIntent = Intent(Intent.ACTION_SEND).apply {
                        action = Intent.ACTION_SEND
                        setPackage(sharePackageName)
                        type = "*/*"
                        if (Uri.fromFile(pdfFile) != null) {
                            if (Build.VERSION.SDK_INT > 24){
                                putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(mActivity, BuildConfig.APPLICATION_ID + ".provider",pdfFile))
                            }else{
                                putExtra(Intent.EXTRA_STREAM, Uri.fromFile(pdfFile))
                            }
                        }
                        putExtra(Intent.EXTRA_SUBJECT, mActivity.resources.getString(R.string.app_name))
                        putExtra(
                            Intent.EXTRA_TEXT,
                            (matchBean.firstName ?: "") + "_" + (matchBean.lastName ?: "")
                        )

                    }
                    startActivity(Intent.createChooser(emailIntent, "Sending Missing Child Data"))
                }else{
                    mActivity.visitUrl("http://play.google.com/store/apps/details?id=$sharePackageName")
                }
                if (this@MissingChildDetailsFragment::shareAppDialog.isInitialized) {
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