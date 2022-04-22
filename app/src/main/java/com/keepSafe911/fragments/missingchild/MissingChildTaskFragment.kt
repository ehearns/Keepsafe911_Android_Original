package com.keepSafe911.fragments.missingchild

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.generic.GenericDraweeHierarchy
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
import com.facebook.drawee.generic.RoundingParams
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.keepSafe911.BuildConfig
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.model.FamilyMonitorResult
import com.keepSafe911.model.response.findmissingchild.MatchResult
import com.keepSafe911.model.response.findmissingchild.MissingChildTaskListResult
import com.keepSafe911.model.response.findmissingchild.MissingChildTaskModel
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_missing_child_task.*
import kotlinx.android.synthetic.main.toolbar_header.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val ARG_PARAM3 = "param3"
private const val ARG_PARAM4 = "param4"

class MissingChildTaskFragment : HomeBaseFragment(), View.OnClickListener {
    private var childId: Int = 0
    private var createdId: Int = 0
    lateinit var appDatabase: OldMe911Database
    var loginObject: LoginObject = LoginObject()
    var loginMemberId: Int = 0
    var loginMemberName: String = ""
    var loginMemberUrl: String = ""
    private var childTaskList: ArrayList<MissingChildTaskModel> = ArrayList()
    private var filteredTaskList: ArrayList<MissingChildTaskModel> = ArrayList()
    var memberList: ArrayList<FamilyMonitorResult> = ArrayList()
    private lateinit var memberBottomSheetDialog: BottomSheetDialog
    private lateinit var missingChildTaskAdapter: MissingChildTaskAdapter
    private var isAssignToUser: Boolean = false
    var isFrom: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isFrom = it.getBoolean(ARG_PARAM1, false)
            childId = it.getInt(ARG_PARAM2, 0)
            filteredTaskList = it.getParcelableArrayList(ARG_PARAM3) ?: ArrayList()
            createdId = it.getInt(ARG_PARAM4, 0)
        }
    }

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
        return inflater.inflate(R.layout.fragment_missing_child_task, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance(isFrom: Boolean, childId: Int, taskList: ArrayList<MissingChildTaskModel>, createdId: Int) =
            MissingChildTaskFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_PARAM1, isFrom)
                    putInt(ARG_PARAM2, childId)
                    putParcelableArrayList(ARG_PARAM3, taskList)
                    putInt(ARG_PARAM4, createdId)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHeader()
        btnTaskSubmit.text = mActivity.resources.getString(R.string.submit)
        btnTaskSubmit.setOnClickListener(this)
        val taskSubmitParam = btnTaskSubmit.layoutParams as ViewGroup.MarginLayoutParams
        val height = if (Comman_Methods.hasNavBar(mActivity)) {
            Utils.calculateNoOfRows(mActivity, 1.1)
        } else {
            Utils.calculateNoOfRows(mActivity, 1.15)
        }
        taskSubmitParam.setMargins(0, height,0,5)
    }

    private fun callTaskListApi() {
        Utils.missingChildTaskList(mActivity, object : CommonApiListener {
            override fun missingChildTaskListResponse(
                status: Boolean,
                missingChildTaskList: ArrayList<MissingChildTaskListResult>,
                message: String,
                responseMessage: String
            ) {
                childTaskList = ArrayList()
                childTaskList.addAll(filteredTaskList)
                for (i in 0 until missingChildTaskList.size) {
                    val missingChildTaskModel = MissingChildTaskModel()
                    missingChildTaskModel.id = 0
                    missingChildTaskModel.taskId = missingChildTaskList[i].id
                    missingChildTaskModel.question = missingChildTaskList[i].taskName
                    missingChildTaskModel.type = missingChildTaskList[i].type
                    missingChildTaskModel.hint = missingChildTaskList[i].hint
                    missingChildTaskModel.assignBy = loginObject.memberID
                    missingChildTaskModel.assignTo = 0
                    missingChildTaskModel.answerInBoolean = false
                    missingChildTaskModel.answerInText = ""
                    missingChildTaskModel.childReferenceId = childId
                    missingChildTaskModel.isShared = false
                    missingChildTaskModel.isOpened = false
                    missingChildTaskModel.isAssignTask = false
                    missingChildTaskModel.ownDisabled = false
                    missingChildTaskModel.reAssigned = false
                    childTaskList.add(missingChildTaskModel)
                }
                childTaskList = childTaskList.distinctBy { it.taskId } as ArrayList<MissingChildTaskModel>
                var sortByChildTask: ArrayList<MissingChildTaskModel> = ArrayList()
                try {
                    sortByChildTask = mutableListOf(childTaskList.sortWith(compareBy { child -> child.taskId ?: 0 })) as ArrayList<MissingChildTaskModel>
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                setAdapter(childTaskList)
                mActivity.startMissingChildTaskTime(this@MissingChildTaskFragment)
            }

            override fun onFailureResult() {
                setAdapter(ArrayList())
            }
        })
    }

    private fun setAdapter(dataList: ArrayList<MissingChildTaskModel>) {
        try {
            assignAvailable()
            missingChildTaskAdapter = MissingChildTaskAdapter(dataList)
            rvMissingTaskList.adapter = missingChildTaskAdapter
            missingChildTaskAdapter.notifyDataSetChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setHeader() {
        appDatabase = OldMe911Database.getDatabase(mActivity)
        loginObject = appDatabase.loginDao().getAll()
        loginMemberId = loginObject.memberID
        loginMemberName = (loginObject.firstName ?: "") + " " + (loginObject.lastName ?: "")
        loginMemberUrl = loginObject.profilePath ?: ""
        mActivity.disableDrawer()
        tvHeader.text = mActivity.resources.getString(R.string.str_task_missing_child).uppercase()
        Utils.setTextGradientColor(tvHeader)
        iv_back.visibility = View.VISIBLE
        rvMissingTaskList.layoutManager = LinearLayoutManager(mActivity, RecyclerView.VERTICAL, false)
        setAdapter(childTaskList)
        if (createdId == loginObject.memberID) {
            ivAssign.visibility = View.VISIBLE
            callTaskListApi()
        } else {
            tvHeader.setPadding(0, 0, 50, 0)
            childTaskList = ArrayList()
            childTaskList.addAll(filteredTaskList)
            var sortByChildTask: ArrayList<MissingChildTaskModel> = ArrayList()
            try {
                sortByChildTask = mutableListOf(childTaskList.sortWith(compareBy { child -> child.taskId ?: 0 })) as ArrayList<MissingChildTaskModel>
            } catch (e: Exception) {
                e.printStackTrace()
            }
            setAdapter(childTaskList)
            mActivity.startMissingChildTaskTime(this@MissingChildTaskFragment)
        }
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
        assignAvailable()
        ivAssign.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            changeViewType()
            assignTaskEnable(isAssignToUser)
//            getMemberList()
        }
        mActivity.checkUserActive()
    }

    private fun assignAvailable() {
        var count = 0
        for (i in 0 until childTaskList.size) {
            val assignTo = childTaskList[i].assignTo ?: 0
            if (assignTo > 0) {
                if (assignTo == loginObject.memberID) {
                    if (childTaskList[i].answerInBoolean == true) {
                        count += 1
                    }
                } else {
                    count += 1
                }
            }
        }
        if (count == childTaskList.size) {
            ivAssign.setImageResource(R.drawable.ic_un_invite)
            ivAssign.isEnabled = false
        } else {
            ivAssign.setImageResource(R.drawable.ic_invite)
            ivAssign.isEnabled = true
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            setHeader()
        } else {
            mActivity.stopMissingChildTask()
        }
    }

    private fun changeViewType() {
        isAssignToUser = if (isAssignToUser) {
            ivAssign.setImageResource(R.drawable.ic_invite)
            btnTaskSubmit.text = mActivity.resources.getString(R.string.submit)
            loginMemberId = loginObject.memberID
            loginMemberName = (loginObject.firstName ?: "") + " " + (loginObject.lastName ?: "")
            loginMemberUrl = loginObject.profilePath ?: ""
            false
        } else {
            ivAssign.setImageResource(R.drawable.ic_close_primary)
            btnTaskSubmit.text = mActivity.resources.getString(R.string.str_assign)
            true
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnTaskSubmit -> {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(v)
                if (isAssignToUser) {
                    val validAssign = checkAssignedOrNot()
                    if (validAssign) {
                        getMemberList()
                    } else {
                        mActivity.showMessage("Please select any task for assign.")
                    }
                } else {
                    submitTask(false)
                }
            }
        }
    }

    private fun submitTask(fromAssign: Boolean) {
        if (checkAllTaskComplete()) {
            val completedTaskData = completedTaskList()
            if (completedTaskData.size > 0) {
                mActivity.stopMissingChildTask()
                val jsonArray = JsonArray()
                for (i in 0 until completedTaskData.size) {
                    val childTask = completedTaskData[i]
                    val jsonObject = JsonObject()
                    jsonObject.addProperty("Id", childTask.id)
                    jsonObject.addProperty("TaskId", childTask.taskId)
                    jsonObject.addProperty("Question", childTask.question)
                    jsonObject.addProperty("Hint", childTask.hint)
                    jsonObject.addProperty("Type", childTask.type)
                    jsonObject.addProperty(
                        "AnswerInText",
                        (childTask.answerInText ?: "").toString().trim()
                    )
                    val answerGiven = childTask.answerInBoolean ?: false
                    val booleanAnswer = if ((childTask.id ?: 0) > 0) {
                        if (childTask.reAssigned) false else answerGiven
                    } else {
                        if (childTask.assignTo != createdId) false else answerGiven
                    }
                    jsonObject.addProperty("AnswerInBoolean", booleanAnswer)
                    jsonObject.addProperty("AssignBy", childTask.assignBy)
                    jsonObject.addProperty("AssignTo", if (childTask.reAssigned) childTask.reAssignedTo else childTask.assignTo)
                    jsonObject.addProperty(
                        "ChildReferenceId",
                        childTask.childReferenceId
                    )
                    val status: Int = when (childTask.type) {
                        0 -> {
                            if (booleanAnswer) {
                                1
                            } else {
                                0
                            }
                        }
                        1, 2 -> {
                            val textAnswer = childTask.answerInText ?: ""
                            if (textAnswer.trim().isNotEmpty()) {
                                1
                            } else {
                                0
                            }
                        }
                        else -> 0
                    }
                    jsonObject.addProperty("Status", status)
                    jsonObject.addProperty("CreatedOn", childTask.createdOn)
                    jsonObject.addProperty("NotificationBy", 2)
                    jsonArray.add(jsonObject)
                }
                Log.d("", "!@@@jsonArray: $jsonArray")
                Utils.addMissingChildTask(mActivity, jsonArray,
                    object : CommonApiListener {
                        override fun childTaskListResponse(
                            status: Boolean,
                            matchResultData: ArrayList<MatchResult>,
                            missingChildTaskList: ArrayList<MissingChildTaskModel>,
                            message: String,
                            responseMessage: String
                        ) {

                            if (status) {
                                if (fromAssign) {
                                    mActivity.showMessage("Task assigned successfully.")
                                } else {
                                    mActivity.showMessage(responseMessage)
                                }
                                var updatedData: ArrayList<MissingChildTaskModel> =
                                    ArrayList()
                                updatedData.addAll(missingChildTaskList)
                                updatedData.addAll(childTaskList)
                                updatedData =
                                    updatedData.distinctBy { it.taskId } as ArrayList<MissingChildTaskModel>
                                var sortByChildTask: ArrayList<MissingChildTaskModel> =
                                    ArrayList()
                                try {
                                    sortByChildTask =
                                        mutableListOf(updatedData.sortWith(compareBy { child ->
                                            child.taskId ?: 0
                                        })) as ArrayList<MissingChildTaskModel>
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                childTaskList = ArrayList()
                                childTaskList.addAll(updatedData)
                                setAdapter(childTaskList)
                                loginMemberId = loginObject.memberID
                                loginMemberName = (loginObject.firstName
                                    ?: "") + " " + (loginObject.lastName ?: "")
                                loginMemberUrl = loginObject.profilePath ?: ""
                                mActivity.onBackPressed()
                            } else {
                                mActivity.showMessage(responseMessage)
                            }
                            mActivity.startMissingChildTaskTime(this@MissingChildTaskFragment)
                        }

                        override fun onFailureResult() {
                            mActivity.startMissingChildTaskTime(this@MissingChildTaskFragment)
                        }
                    })
            } else {
                mActivity.showMessage("Please complete some task.")
            }
        } else {
            mActivity.showMessage("Please fill some detail where required.")
        }
    }

    fun checkAllTaskComplete(): Boolean {
        var isComplete = true
        for (i in childTaskList.indices) {
            val childTask = childTaskList[i]
            val assignTo = childTask.assignTo ?: 0
            if (assignTo > 0) {
                if (assignTo == loginObject.memberID) {
                    if (childTask.answerInBoolean == true) {
                        when (childTask.type) {
                            1, 2 -> {
                                if ((childTask.answerInText ?: "").toString().trim().isEmpty()) {
                                    isComplete = false
                                }
                            }
                        }
                    }
                }
            }
        }
        return isComplete
    }

    fun completedTaskList(): ArrayList<MissingChildTaskModel> {
        val completedTask: ArrayList<MissingChildTaskModel> = ArrayList()
        for (i in childTaskList.indices) {
            val childTask = childTaskList[i]
            val missingChildId = childTask.id ?: 0
            val missingAssignToId = childTask.assignTo ?: 0
            if (missingAssignToId > 0) {
                completedTask.add(childTask)
            }
        }
        return completedTask
    }

    fun assignTaskList(userId: Int, userName: String, userProfile: String) {
        for (i in childTaskList.indices) {
            val childTask = childTaskList[i]
            val missingChildId = childTask.id ?: 0
            val assignTo = childTask.assignTo ?: 0
            val assignedTick = childTask.answerInBoolean ?: false
            val assignTaskEnable = childTask.isAssignTask
            childTask.reAssignedTo = childTask.assignTo
            childTask.reAssignName = childTask.assignToName
            childTask.reAssignToProfileUrl = childTask.assignToProfileUrl
            childTask.reAssigned = false
            if (assignTo <= 0 || assignTo == loginObject.memberID) {
                if (assignTaskEnable) {
                    if (assignedTick) {
                        childTask.createdOn = Utils.getCurrentTimeStamp()
                        childTask.isAssignTask = false
                        if (missingChildId > 0) {
                            childTask.reAssigned = true
                            childTask.reAssignedTo = userId
                            childTask.reAssignName = userName
                            childTask.reAssignToProfileUrl = userProfile
                        } else {
                            childTask.assignTo = userId
                            childTask.assignToName = userName
                            childTask.assignToProfileUrl = userProfile
                            childTask.reAssignedTo = 0
                            childTask.reAssignName = ""
                            childTask.reAssignToProfileUrl = ""
                        }
                        childTaskList[i] = childTask
                        missingChildTaskAdapter.notifyItemChanged(i)
                    }
                }
            }
        }
    }

    fun assignTaskEnable(isAssignTask: Boolean) {
        for (i in childTaskList.indices) {
            val childTask = childTaskList[i]
            val missingChildId = childTask.id ?: 0
            val assignTo = childTask.assignTo ?: 0
            val assignedTick = childTask.answerInBoolean ?: false
            val newId = (missingChildId > 0) && (assignTo == loginObject.memberID)
            childTask.ownDisabled = false
            if (assignTo <= 0 || assignTo == loginObject.memberID) {
                if (childTask.isAssignTask) {
                    childTask.answerInBoolean = false
                }
                childTask.isAssignTask = isAssignTask
                if (missingChildId <= 0) {
                    childTask.answerInBoolean = false
                }
                if (newId) {
                    if (assignedTick) {
                        childTask.isAssignTask = false
                        childTask.ownDisabled = isAssignTask
                    }
                }

            } else {
                childTask.isAssignTask = false
            }
            childTaskList[i] = childTask
            missingChildTaskAdapter.notifyItemChanged(i)
        }
    }

    fun allTaskCompleted(): Boolean {
        return completedTaskList().size == childTaskList.size
    }

    private fun getMemberList() {
        Utils.familyMonitoringUserList(mActivity, object : CommonApiListener {
            override fun familyUserList(
                status: Boolean,
                userList: ArrayList<FamilyMonitorResult>,
                message: String
            ) {
                if (status) {
                    memberList = ArrayList()
                    for (k in 0 until userList.size) {
                        if (userList[k].iD != loginObject.memberID) {
                            memberList.add(userList[k])
                        }
                    }
                    openMemberListDialog(memberList)
                } else {
                    mActivity.showMessage(message)
                }
            }
        })
    }

    private fun openMemberListDialog(memberList: ArrayList<FamilyMonitorResult>) {

        val view = LayoutInflater.from(mActivity)
            .inflate(R.layout.popup_share_layout, mActivity.window.decorView.rootView as ViewGroup, false)
        if (this::memberBottomSheetDialog.isInitialized){
            if (memberBottomSheetDialog.isShowing){
                memberBottomSheetDialog.dismiss()
            }
        }
        memberBottomSheetDialog = BottomSheetDialog(mActivity,R.style.appBottomSheetDialogTheme)
        memberBottomSheetDialog.setContentView(view)
        val mBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(view.parent as View)
        mBehavior.isHideable = false
        memberBottomSheetDialog.setOnShowListener {
            mBehavior.peekHeight = view.height
        }
        val tvDialogTitle: TextView? = memberBottomSheetDialog.findViewById(R.id.tvShareTitle)
        tvDialogTitle?.text = mActivity.resources.getString(R.string.str_assign_task)
        val btnMemberSubmit: Button? = memberBottomSheetDialog.findViewById(R.id.btnMemberSubmit)
//        btnMemberSubmit?.visibility = View.VISIBLE
        val rvSocialMedia: RecyclerView? = memberBottomSheetDialog.findViewById(R.id.rvSocialMedia)
        rvSocialMedia?.layoutManager = GridLayoutManager(mActivity, 3, RecyclerView.VERTICAL, false)
        rvSocialMedia?.adapter = MemberListAdapter(mActivity, memberList)

        btnMemberSubmit?.setOnClickListener{
            memberBottomSheetDialog.dismiss()
        }

        memberBottomSheetDialog.show()
    }

    private fun checkAssignedOrNot(): Boolean {
        var count = 0
        for (i in 0 until childTaskList.size) {
            val childTask = childTaskList[i]
            if (childTask.isAssignTask) {
                if (childTask.answerInBoolean == true) {
                    count += 1
                }
            }
        }
        return count > 0
    }

    inner class MemberListAdapter(
        val context: Context,
        val memberList: ArrayList<FamilyMonitorResult>
    ) : RecyclerView.Adapter<MemberListAdapter.MemberListHolder>() {

        var selectedMemberId = -1

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): MemberListAdapter.MemberListHolder {
            val view =
                LayoutInflater.from(context).inflate(R.layout.row_member, parent, false)
            return MemberListHolder(view)
        }

        override fun onBindViewHolder(holder: MemberListAdapter.MemberListHolder, position: Int) {

            if (memberList[position].iD != selectedMemberId) {
                val builder = GenericDraweeHierarchyBuilder(mActivity.resources)
                val roundingParams : RoundingParams = RoundingParams.asCircle().setBorder(
                    ContextCompat.getColor(mActivity, R.color.caldroid_white), 2.0f)
                val hierarchy: GenericDraweeHierarchy = builder
                    .setRoundingParams(roundingParams)
                    .build()

                holder.sdvMemberImage.hierarchy = hierarchy
                holder.sdvMemberImage.background = ContextCompat.getDrawable(mActivity,R.drawable.upload_profile)
                holder.tvMemberName.setTextColor(ContextCompat.getColor(mActivity, R.color.colorAccent))
            }else{
                val builder = GenericDraweeHierarchyBuilder(mActivity.resources)
                val roundingParams : RoundingParams = RoundingParams.asCircle().setBorder(
                    ContextCompat.getColor(mActivity, R.color.colorPrimary), 4.0f)
                val hierarchy: GenericDraweeHierarchy = builder
                    .setRoundingParams(roundingParams)
                    .build()

                holder.sdvMemberImage.hierarchy = hierarchy
                holder.sdvMemberImage.background = ContextCompat.getDrawable(mActivity,R.drawable.upload_profile_green)
                holder.tvMemberName.setTextColor(ContextCompat.getColor(mActivity, R.color.colorPrimary))
            }
            val weight: Int = Utils.calculateNoOfColumns(context, 3.0)
            val height: Int = Utils.calculateNoOfRows(context, 6.0)
            val layoutParams = holder.clShareOption.layoutParams
            layoutParams.width = weight
            layoutParams.height = height
            holder.clShareOption.layoutParams = layoutParams

            holder.tvMemberName.text = (memberList[position].firstName ?: "") + " " + (memberList[position].lastName ?: "")
            holder.sdvMemberImage.loadFrescoImage(mActivity, memberList[position].image ?: "", 1)

            holder.sdvMemberImage.setOnClickListener {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
                loginMemberId = memberList[position].iD
                loginMemberName = (memberList[position].firstName ?: "") + " " + (memberList[position].lastName ?: "")
                loginMemberUrl = memberList[position].image ?: ""
                this.selectedMemberId = memberList[position].iD
                assignTaskList(loginMemberId, loginMemberName, loginMemberUrl)
                changeViewType()
                assignTaskEnable(isAssignToUser)
                memberBottomSheetDialog.dismiss()
                submitTask(true)
                notifyDataSetChanged()
            }
        }

        override fun getItemCount(): Int {
            return memberList.size
        }

        inner class MemberListHolder(view: View) : RecyclerView.ViewHolder(view) {
            val sdvMemberImage: SimpleDraweeView = itemView.findViewById(R.id.ivMember)
            val tvMemberName: TextView = itemView.findViewById(R.id.ivMemberName)
            val clShareOption: ConstraintLayout = view.findViewById(R.id.clShareOption)
        }
    }

    inner class MissingChildTaskAdapter(private val taskList: ArrayList<MissingChildTaskModel>): RecyclerView.Adapter<MissingChildTaskAdapter.MissingChildTaskHolder>() {

        private var adapterPosition: Int = -1
        inner class MissingChildTaskHolder(view: View): RecyclerView.ViewHolder(view) {
            val cbTaskQuestion: CheckBox = view.findViewById(R.id.cbTaskQuestion)
            val llTaskType: LinearLayout = view.findViewById(R.id.llTaskType)
            val llAssignData: LinearLayout = view.findViewById(R.id.llAssignData)
            val llDisableView: LinearLayout = view.findViewById(R.id.llDisableView)
            val tvAssignedName: TextView = view.findViewById(R.id.tvAssignedName)
            val tvAssignTime: TextView = view.findViewById(R.id.tvAssignTime)
            val ivAssignedProfile: SimpleDraweeView = view.findViewById(R.id.ivAssignedProfile)
            val ivCheckData: ImageView = view.findViewById(R.id.ivCheckData)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MissingChildTaskHolder {
            return MissingChildTaskHolder(
                LayoutInflater.from(mActivity).inflate(R.layout.raw_missing_child_task, parent, false)
            )
        }

        override fun onBindViewHolder(holder: MissingChildTaskHolder, position: Int) {
            val missingChildTask = taskList[position]
            val answerInBoolean = missingChildTask.answerInBoolean ?: false
            holder.cbTaskQuestion.setOnCheckedChangeListener(null)
            holder.cbTaskQuestion.text = missingChildTask.question ?: ""
            holder.cbTaskQuestion.isChecked = answerInBoolean
            val emergencyResponseId = missingChildTask.id ?: 0
            val assignTo = missingChildTask.assignTo ?: 0
            val status = missingChildTask.status ?: 0
            val isOwnOrNew = assignTo == loginObject.memberID || assignTo == 0
            holder.ivCheckData.visibility = View.GONE
            holder.llDisableView.visibility = View.GONE
            if (missingChildTask.isAssignTask) {
                holder.cbTaskQuestion.buttonDrawable = ContextCompat.getDrawable(mActivity, R.drawable.item_selector)
            } else {
                holder.cbTaskQuestion.buttonDrawable = null
                if (holder.cbTaskQuestion.isChecked) {
                    holder.ivCheckData.visibility = View.VISIBLE
                    holder.ivCheckData.setImageResource(R.drawable.ic_check_square)
                } else {
                    if (isOwnOrNew) {
                        holder.ivCheckData.visibility = View.VISIBLE
                        holder.ivCheckData.setImageResource(R.drawable.ic_square_o)
                    }
                }
            }
            if (emergencyResponseId > 0) {
                if (missingChildTask.isAssignTask) {
                    if (isOwnOrNew && missingChildTask.isAssignTask) {
                        holder.llDisableView.visibility = View.GONE
                    } else {
                        holder.llDisableView.visibility = View.VISIBLE
                    }
                    holder.cbTaskQuestion.isEnabled = isOwnOrNew && missingChildTask.isAssignTask
                    holder.ivCheckData.isEnabled = isOwnOrNew && missingChildTask.isAssignTask
                } else {
                    if (missingChildTask.ownDisabled) {
                        holder.llDisableView.visibility = View.VISIBLE
                        holder.cbTaskQuestion.isEnabled = false
                        holder.ivCheckData.isEnabled = false
                    } else {
                        if (isOwnOrNew) {
                            holder.llDisableView.visibility = View.GONE
                        } else {
                            holder.llDisableView.visibility = View.VISIBLE
                        }
                        holder.cbTaskQuestion.isEnabled = isOwnOrNew
                        holder.ivCheckData.isEnabled = isOwnOrNew
                    }
                }
            } else {
                holder.llDisableView.visibility = View.GONE
                holder.cbTaskQuestion.isEnabled = true
                holder.ivCheckData.isEnabled = true
            }
            missingChildTask.isShared = !isOwnOrNew
            val formatter1 = SimpleDateFormat(INPUT_DATE_FORMAT)
            val target = SimpleDateFormat(CHECK_DATE_TIME2)
            val taskCreateOn = missingChildTask.createdOn ?: Utils.getCurrentTimeStamp()
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

            holder.tvAssignedName.setTextColor(ContextCompat.getColor(mActivity, R.color.colorPrimaryDark))

            val doneAssignText = if (isOwnOrNew) {
                ""
            } else {
                if (emergencyResponseId > 0) {
                    when (missingChildTask.type) {
                        0 -> {
                            val answerGiven = missingChildTask.answerInBoolean ?: false
                            if (answerGiven) {
                                ""
                            } else {
                                if (isOwnOrNew) {
                                    ""
                                } else {
                                    holder.tvAssignedName.setTextColor(ContextCompat.getColor(mActivity, R.color.colorPrimary))
                                    "Assign: "
                                }
                            }
                        }
                        1, 2 -> {
                            val textAnswer = missingChildTask.answerInText ?: ""
                            if (textAnswer.trim().isNotEmpty()) {
                                ""
                            } else {
                                if (isOwnOrNew) {
                                    ""
                                } else {
                                    holder.tvAssignedName.setTextColor(ContextCompat.getColor(mActivity, R.color.colorPrimary))
                                    "Assign: "
                                }
                            }
                        } else -> ""
                    }
                } else {
                    holder.tvAssignedName.setTextColor(ContextCompat.getColor(mActivity, R.color.colorPrimary))
                    "Assign: "
                }
            }

            val displayLog = if (assignTo > 0) {
                doneAssignText + (missingChildTask.assignToName ?: "") + ", "
            } else {
                ""
            }

            val displayCreatedOn = if (assignTo > 0) {
                createOnTime
            } else {
                ""
            }

            val logVisibility = if (emergencyResponseId > 0) {
                !(isOwnOrNew && missingChildTask.isAssignTask)
            } else {
                false
            }
            if (logVisibility) {
                if (missingChildTask.reAssigned) {
                    holder.llAssignData.visibility = View.GONE
                } else {
                    when (missingChildTask.type) {
                        0 -> {
                            val answerGiven = missingChildTask.answerInBoolean ?: false
                            if (answerGiven) {
                                holder.llAssignData.visibility = View.VISIBLE
                            } else {
                                if (isOwnOrNew) {
                                    holder.llAssignData.visibility = View.GONE
                                } else {
                                    holder.llAssignData.visibility = View.VISIBLE
                                }
                            }
                        }
                        1, 2 -> {
                            val textAnswer = missingChildTask.answerInText ?: ""
                            if (isOwnOrNew) {
                                if (textAnswer.trim().isNotEmpty()) {
                                    holder.llAssignData.visibility = View.VISIBLE
                                } else {
                                    holder.llAssignData.visibility = View.GONE
                                }
                            } else {
                                holder.llAssignData.visibility = View.VISIBLE
                            }
                        }
                        else -> holder.llAssignData.visibility = View.GONE
                    }
                }
            } else {
                holder.llAssignData.visibility = View.GONE
            }

            holder.ivAssignedProfile.loadFrescoImage(mActivity, missingChildTask.assignToProfileUrl ?: "", 1)
            holder.tvAssignedName.text = displayLog
            holder.tvAssignTime.text = displayCreatedOn
//            holder.llTaskType.removeAllViews()
            if (position == adapterPosition) {
                if (isOwnOrNew) {
                    if (missingChildTask.isAssignTask) {
                        missingChildTask.isOpened = true
                    } else {
                        missingChildTask.isOpened = answerInBoolean
                    }
                } else {
                    missingChildTask.isOpened = true
                }
                viewManagementOutSide(holder.llTaskType, missingChildTask, missingChildTask.isOpened, isOwnOrNew)
            } else {
                missingChildTask.isOpened = true
                viewManagementOutSide(holder.llTaskType, missingChildTask, answerInBoolean, isOwnOrNew)
                /* if (emergencyResponseId > 0) {
                     viewManagementOutSide(holder.llTaskType, missingChildTask, answerInBoolean, isOwnOrNew)
                 } else {
                     holder.llTaskType.removeAllViews()
                 }*/
            }


            holder.cbTaskQuestion.setOnClickListener {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
                val isSelected = (it as CheckBox).isChecked
                missingChildTask.answerInBoolean = isSelected
                if (isSelected) {
                    missingChildTask.assignTo = loginMemberId
                    missingChildTask.assignToName = loginMemberName
                    missingChildTask.assignToProfileUrl = loginMemberUrl
                    missingChildTask.createdOn = Utils.getCurrentTimeStamp()
                } else {
                    if (emergencyResponseId <= 0) {
                        missingChildTask.assignTo = 0
                        missingChildTask.assignToName = ""
                        missingChildTask.assignToProfileUrl = ""
                        missingChildTask.createdOn = Utils.getCurrentTimeStamp()
                    }
                    if (isOwnOrNew) {
                        missingChildTask.answerInText = ""
                        missingChildTask.createdOn = Utils.getCurrentTimeStamp()
                    }
                    missingChildTask.reAssigned = false
                }
                adapterPosition = position
                notifyDataSetChanged()
            }

            holder.ivCheckData.setOnClickListener {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
                val isSelected = !holder.cbTaskQuestion.isChecked
                missingChildTask.answerInBoolean = isSelected
                if (isSelected) {
                    missingChildTask.assignTo = loginMemberId
                    missingChildTask.assignToName = loginMemberName
                    missingChildTask.assignToProfileUrl = loginMemberUrl
                    missingChildTask.createdOn = Utils.getCurrentTimeStamp()
                } else {
                    if (emergencyResponseId <= 0) {
                        missingChildTask.assignTo = 0
                        missingChildTask.assignToName = ""
                        missingChildTask.assignToProfileUrl = ""
                        missingChildTask.createdOn = Utils.getCurrentTimeStamp()
                    }
                    if (isOwnOrNew) {
                        missingChildTask.answerInText = ""
                        missingChildTask.createdOn = Utils.getCurrentTimeStamp()
                    }
                }
                adapterPosition = position
                notifyDataSetChanged()
            }
        }

        private fun viewManagementOutSide(llTaskType: LinearLayout, missingChildTask: MissingChildTaskModel, isOpened: Boolean, isOwn: Boolean) {
            llTaskType.removeAllViews()
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val lp1 = FrameLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val lp2 = FrameLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val emergencyResponseId = missingChildTask.id ?: 0
            val tenSize = Comman_Methods.convertDpToPixels(10F, mActivity).toInt()
            val marginStartSize = Comman_Methods.convertDpToPixels(25F, mActivity).toInt()
            val paddingSize = Comman_Methods.convertDpToPixels(3F, mActivity).toInt()
            if (isOpened) {
                when (missingChildTask.type) {
                    1 -> {
                        val noteFrameLayout = FrameLayout(mActivity)
                        noteFrameLayout.layoutParams = lp
                        val hintTextView = TextView(mActivity)
                        lp2.setMargins(marginStartSize,0,0,0)
                        hintTextView.text = missingChildTask.hint ?: ""
                        with(hintTextView) {
                            layoutParams = lp2
                            setTextColor(ContextCompat.getColor(mActivity, R.color.colorAccent))
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12F)
                            setPadding(paddingSize,0, paddingSize,0)
                            background = ContextCompat.getDrawable(mActivity, R.drawable.hint_back)
                        }
                        val customSingleEditText = EditText(mActivity)
                        lp1.topMargin = tenSize
                        customSingleEditText.inputType =
                            InputType.TYPE_TEXT_FLAG_CAP_WORDS or InputType.TYPE_TEXT_VARIATION_PERSON_NAME
                        customSingleEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
                        customSingleEditText.layoutParams = lp1
                        customSingleEditText.isSingleLine = true
                        customSingleEditText.background = ContextCompat.getDrawable(mActivity, R.drawable.edittext_white_back)
                        val answer: String = missingChildTask.answerInText ?: ""
                        customSingleEditText.setText(answer)
                        customSingleEditText.addTextChangedListener(object : TextWatcher{
                            override fun beforeTextChanged(
                                s: CharSequence?,
                                start: Int,
                                count: Int,
                                after: Int
                            ) { }

                            override fun onTextChanged(
                                s: CharSequence?,
                                start: Int,
                                before: Int,
                                count: Int
                            ) {
                                s?.let { char ->
                                    missingChildTask.answerInText = char.toString().trim()
                                }
                            }

                            override fun afterTextChanged(s: Editable?) { }

                        })
                        if (mActivity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                            customSingleEditText.imeOptions = EditorInfo.IME_ACTION_DONE
                        } else {
                            customSingleEditText.imeOptions =
                                EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_DONE
                        }
                        if (emergencyResponseId > 0) {
                            customSingleEditText.isFocusableInTouchMode = isOwn
                        } else {
                            customSingleEditText.isFocusableInTouchMode = true
                        }
                        noteFrameLayout.addView(customSingleEditText)
                        noteFrameLayout.addView(hintTextView)
                        llTaskType.addView(noteFrameLayout)
                    }
                    2 -> {
                        val noteFrameLayout = FrameLayout(mActivity)
                        noteFrameLayout.layoutParams = lp
                        val hintTextView = TextView(mActivity)
                        lp2.setMargins(marginStartSize,0,0,0)
                        hintTextView.text = missingChildTask.hint ?: ""
                        with(hintTextView) {
                            layoutParams = lp2
                            setTextColor(ContextCompat.getColor(mActivity, R.color.colorAccent))
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12F)
                            setPadding(paddingSize,0,paddingSize,0)
                            background = ContextCompat.getDrawable(mActivity, R.drawable.hint_back)
                        }
                        val customMultiLineEditText = EditText(mActivity)
                        lp1.topMargin = tenSize
                        customMultiLineEditText.inputType =
                            InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE
                        customMultiLineEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
                        customMultiLineEditText.layoutParams = lp1
                        customMultiLineEditText.isSingleLine = false
                        customMultiLineEditText.maxLines = 3
                        customMultiLineEditText.setLines(3)
                        customMultiLineEditText.movementMethod = ScrollingMovementMethod()
                        customMultiLineEditText.gravity = Gravity.START or Gravity.TOP
                        customMultiLineEditText.background = ContextCompat.getDrawable(mActivity, R.drawable.edittext_white_back)
                        val answer: String = missingChildTask.answerInText ?: ""
                        customMultiLineEditText.setText(answer)
                        customMultiLineEditText.addTextChangedListener(object : TextWatcher{
                            override fun beforeTextChanged(
                                s: CharSequence?,
                                start: Int,
                                count: Int,
                                after: Int
                            ) { }

                            override fun onTextChanged(
                                s: CharSequence?,
                                start: Int,
                                before: Int,
                                count: Int
                            ) {
                                s?.let { char ->
                                    missingChildTask.answerInText = char.toString().trim()
                                }
                            }

                            override fun afterTextChanged(s: Editable?) { }

                        })
                        if (mActivity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                            customMultiLineEditText.imeOptions = EditorInfo.IME_ACTION_DONE
                        } else {
                            customMultiLineEditText.imeOptions =
                                EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_ACTION_DONE
                        }
                        if (emergencyResponseId > 0) {
                            customMultiLineEditText.isFocusableInTouchMode = isOwn
                        } else {
                            customMultiLineEditText.isFocusableInTouchMode = true
                        }
                        noteFrameLayout.addView(customMultiLineEditText)
                        noteFrameLayout.addView(hintTextView)
                        llTaskType.addView(noteFrameLayout)

                    }
                    else -> {
                        llTaskType.removeAllViews()
                    }
                }
            } else {
                llTaskType.removeAllViews()
            }
        }

        override fun getItemCount(): Int {
            return taskList.size
        }
    }

    inner class CustomMissingChildTask : java.util.TimerTask() {
        private val handler = Handler(Looper.getMainLooper())
        override fun run() {
            Thread {
                handler.post {
                    try {
                        callMissingChildList(loginObject.memberID, childId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }.start()
        }
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
                var filteredChildTaskResponseList: ArrayList<MissingChildTaskModel> = ArrayList()
                filteredChildTaskResponseList.addAll(missingChildTaskList)
                filteredChildTaskResponseList = filteredChildTaskResponseList.distinctBy { it.taskId } as ArrayList<MissingChildTaskModel>
                var sortByChildTask: ArrayList<MissingChildTaskModel> = ArrayList()
                try {
                    sortByChildTask =
                        mutableListOf(filteredChildTaskResponseList.sortWith(compareBy { child ->
                            child.taskId ?: 0
                        })) as ArrayList<MissingChildTaskModel>
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                for (v in 0 until filteredChildTaskResponseList.size) {
                    val taskData = filteredChildTaskResponseList[v]
                    checkUpdatedOrNot(taskData)
                }
                if (loginObject.memberID != createdId) {
                    var removeItem = MissingChildTaskModel()
                    for (v in 0 until childTaskList.size) {
                        val taskData = childTaskList[v]
                        checkRemoveData(taskData, filteredChildTaskResponseList)?.let {
                            removeItem = it
                        }
                    }
                    if ((removeItem.id ?: 0) > 0) {
                        childTaskList.remove(removeItem)
                        var sortByChildTask: ArrayList<MissingChildTaskModel> = ArrayList()
                        try {
                            sortByChildTask =
                                mutableListOf(childTaskList.sortWith(compareBy { child ->
                                    child.taskId ?: 0
                                })) as ArrayList<MissingChildTaskModel>
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        setAdapter(childTaskList)
                    }
                }
            }
        })
    }

    private fun checkUpdatedOrNot(missingChildTask: MissingChildTaskModel) {
        var count = 0
        for (v in 0 until childTaskList.size) {
            val taskData = childTaskList[v]
            if (taskData.id == missingChildTask.id) {
                if (missingChildTask.assignTo != loginObject.memberID && missingChildTask.assignTo != 0) {
                    childTaskList[v] = missingChildTask
                    missingChildTaskAdapter.notifyItemChanged(v)
                } else if (missingChildTask.assignTo != taskData.assignTo){
                    childTaskList[v] = missingChildTask
                    missingChildTaskAdapter.notifyItemChanged(v)
                }
            } else {
                count += 1
            }
        }
        if (count == childTaskList.size) {
            childTaskList.add(missingChildTask)
            var sortByChildTask: ArrayList<MissingChildTaskModel> = ArrayList()
            try {
                sortByChildTask =
                    mutableListOf(childTaskList.sortWith(compareBy { child ->
                        child.taskId ?: 0
                    })) as ArrayList<MissingChildTaskModel>
            } catch (e: Exception) {
                e.printStackTrace()
            }
            setAdapter(childTaskList)
        }
    }

    private fun checkRemoveData(
        missingChildTask: MissingChildTaskModel,
        filteredChildTaskResponseList: ArrayList<MissingChildTaskModel>
    ): MissingChildTaskModel? {
        var count = 0
        for (v in 0 until filteredChildTaskResponseList.size) {
            val taskData = filteredChildTaskResponseList[v]
            if (taskData.id != missingChildTask.id) {
                count += 1
            }
        }
        return if (count == filteredChildTaskResponseList.size) missingChildTask else null
    }
}