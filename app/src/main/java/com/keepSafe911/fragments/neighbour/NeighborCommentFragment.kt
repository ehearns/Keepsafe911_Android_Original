package com.keepSafe911.fragments.neighbour

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView

import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.room.OldMe911Database
import com.google.gson.JsonObject
import com.keepSafe911.BuildConfig
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.listner.PositiveButtonListener
import com.keepSafe911.model.LikeCommentResult
import com.keepSafe911.model.response.FeedResponseResult
import com.keepSafe911.model.response.LstOfFeedLikeOrComment
import com.keepSafe911.utils.*
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_neighbor_comment.*
import kotlinx.android.synthetic.main.raw_comment.view.*
import kotlinx.android.synthetic.main.toolbar_header.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private const val ARG_PARAM3 = "param3"

class NeighborCommentFragment : HomeBaseFragment() {
    var isFrom: Boolean = false
    var isFromUser: ArrayList<FeedResponseResult> = ArrayList()
    companion object {
        fun newInstance(isFromUser: ArrayList<FeedResponseResult>, uploadObject: FeedResponseResult, isFrom: Boolean): NeighborCommentFragment {
            val args = Bundle()
            args.putParcelable(ARG_PARAM1, uploadObject)
            args.putBoolean(ARG_PARAM2, isFrom)
            args.putParcelableArrayList(ARG_PARAM3, isFromUser)
            val fragment = NeighborCommentFragment()
            fragment.arguments = args
            return fragment
        }
    }

    lateinit var appDatabase: OldMe911Database
    private var uploadObject: FeedResponseResult = FeedResponseResult()
    private var commentList: ArrayList<LstOfFeedLikeOrComment> = ArrayList()
    lateinit var neighborCommentAdapter: NeighborCommentAdapter
    var userId: Int = 0
    var fullName: String = ""
    var profilePath: String = ""
    var differenceTime: String = ""

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            uploadObject = it.getParcelable(ARG_PARAM1) ?: FeedResponseResult()
            isFrom = it.getBoolean(ARG_PARAM2, false)
            isFromUser = it.getParcelableArrayList(ARG_PARAM3) ?: ArrayList()
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
        LocaleUtils.updateConfig(mActivity, resources.configuration)
        /*mActivity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )*/
        return inflater.inflate(R.layout.fragment_neighbor_comment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity.disableDrawer()
        appDatabase = OldMe911Database.getDatabase(mActivity)
        userId = appDatabase.loginDao().getAll().memberID
        fullName = appDatabase.loginDao().getAll().firstName+" "+appDatabase.loginDao().getAll().lastName
        profilePath = appDatabase.loginDao().getAll().profilePath ?: ""
        setHeader()
        commentList = ArrayList()
        val likeCommentList = uploadObject.lstOfFeedLikeOrComments ?: ArrayList()
        for (i in 0 until likeCommentList.size) {
            if (likeCommentList[i].feedType == COMMENT) {
                commentList.add(likeCommentList[i])
            }
        }
        if (rvCommentList!=null) {
            if (commentList.size > 0) {
                tvFirstCommentator.visibility = View.GONE
                rvCommentList.visibility = View.VISIBLE
            } else {
                rvCommentList.visibility = View.GONE
                tvFirstCommentator.visibility = View.VISIBLE
            }
        }
        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            etNeighborComment.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        } else {
            etNeighborComment.imeOptions = EditorInfo.IME_ACTION_DONE
        }
        if (rvCommentList!=null) {
            rvCommentList.layoutManager = LinearLayoutManager(mActivity, RecyclerView.VERTICAL, false)
        }
        setCommentAdapter()

        val formatter = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
        val formatter2 = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
//                    formatter.timeZone = TimeZone.getTimeZone("UTC")
        try {
            val date1 = formatter.parse(uploadObject.createdOn ?: "")
            val date2 = formatter2.parse(Utils.getCurrentTimeStamp())

            differenceTime = printDifference(date1!!, date2!!)
        }catch (e: Exception){
            e.printStackTrace()
        }
        tvCommentPostTitle.text = uploadObject.title
        tvCommentatorTime.text = differenceTime
        sdvCommentImageFile.loadFrescoImage(mActivity, uploadObject.userImage ?: "", 1)

        sdvCommentUserImage.loadFrescoImage(mActivity, profilePath, 1)
        etNeighborComment.movementMethod = ScrollingMovementMethod()
        etNeighborComment.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.send_post, 0)
        val viewDataPaddingValue = Comman_Methods.convertDpToPixels(10F, mActivity)
        etNeighborComment.setOnTouchListener { v, event ->
            val DRAWABLE_LEFT = 0
            val DRAWABLE_TOP = 1
            val DRAWABLE_RIGHT = 2
            val DRAWABLE_BOTTOM = 3

            if (event.action == MotionEvent.ACTION_DOWN) {
                if (event.rawX >= (etNeighborComment.right - (etNeighborComment.compoundDrawables[DRAWABLE_RIGHT].bounds.width() + viewDataPaddingValue))) {
                    if (etNeighborComment.text.toString().trim().isNotEmpty()) {
                        mActivity.hideKeyboard()
                        etNeighborComment.isFocusableInTouchMode = false
                        etNeighborComment.isFocusable = false
                        callLikeCommentShareApi(uploadObject, etNeighborComment.text.toString().trim(),
                            LstOfFeedLikeOrComment()
                        )
                        etNeighborComment.setText("")
                    }
                    true
                } else {
                    etNeighborComment.isFocusableInTouchMode = true
                    etNeighborComment.isFocusable = true

                }
            }
            false
        }
    }

    fun setCommentAdapter(){
        if (rvCommentList!=null) {
            if (commentList.size > 0) {
                tvFirstCommentator.visibility = View.GONE
                rvCommentList.visibility = View.VISIBLE
            } else {
                rvCommentList.visibility = View.GONE
                tvFirstCommentator.visibility = View.VISIBLE
            }
            neighborCommentAdapter = NeighborCommentAdapter(mActivity, commentList, uploadObject)
            rvCommentList.adapter = neighborCommentAdapter
            neighborCommentAdapter.notifyDataSetChanged()
        }
    }

    private fun setHeader() {
        tvHeader.text = mActivity.resources.getString(R.string.str_comment)
        tvHeader.setPadding(0, 0, 50, 0)
        Utils.setTextGradientColor(tvHeader)
        iv_back.visibility = View.VISIBLE
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
        mActivity.checkUserActive()
    }

    fun printDifference(startDate: Date, endDate: Date): String {
        //milliseconds
        var timeDifference = ""
        var different = endDate.time - startDate.time

        System.out.println("startDate : $startDate")
        System.out.println("endDate : $endDate")
        System.out.println("different : $different")

        val secondsInMilli = 1000L
        val minutesInMilli = secondsInMilli * 60
        val hoursInMilli = minutesInMilli * 60
        val daysInMilli = hoursInMilli * 24
        val monthInMilli = daysInMilli * 30

        val elapsedMonths = different / monthInMilli
        different %= monthInMilli

        val elapsedDays = different / daysInMilli
        different %= daysInMilli

        val elapsedHours = different / hoursInMilli
        different %= hoursInMilli

        val elapsedMinutes = different / minutesInMilli
        different %= minutesInMilli

        val elapsedSeconds = different / secondsInMilli

        timeDifference = when {
            elapsedMonths > 0L -> elapsedDays.toString()+ mActivity.resources.getString(R.string.str_month_ago)
            elapsedDays > 0L -> elapsedDays.toString()+ mActivity.resources.getString(R.string.str_day_ago)
            elapsedHours > 0L -> elapsedHours.toString()+ mActivity.resources.getString(R.string.str_hour_ago)
            elapsedMinutes > 0L -> elapsedMinutes.toString() + mActivity.resources.getString(R.string.str_min_ago)
            elapsedSeconds > 1L -> elapsedSeconds.toString() + mActivity.resources.getString(R.string.str_second_ago)
            else -> getString(R.string.str_just_now)
        }
        return timeDifference
    }

    fun callLikeCommentShareApi(uploadObject: FeedResponseResult, comment: String, commentFeed: LstOfFeedLikeOrComment) {
        mActivity.isSpeedAvailable()
        val jsonObject = JsonObject()
        jsonObject.addProperty("ID",commentFeed.id)
        jsonObject.addProperty("Type", 2)
        jsonObject.addProperty("Comments", comment)
        jsonObject.addProperty("FeedID",uploadObject.iD)
        jsonObject.addProperty("ResponseBy",userId)
        jsonObject.addProperty("Date", Utils.getCurrentTimeStamp())

        Utils.postLikeCommentApi(mActivity, jsonObject, object: CommonApiListener {
            override fun postLikeComment(
                status: Boolean,
                likeCommentResult: LikeCommentResult?,
                message: String,
                responseMessage: String
            ) {
                if (status) {
                    if (commentFeed.id == 0) {
                        uploadObject.commentCount = uploadObject.commentCount?.plus(1)
                        uploadObject.isCommented = true
                        val lstOfFeedLikeOrComment = LstOfFeedLikeOrComment()
                        lstOfFeedLikeOrComment.comments = comment
                        lstOfFeedLikeOrComment.date = Utils.getCurrentTimeStamp()
                        lstOfFeedLikeOrComment.feedID = uploadObject.iD
                        lstOfFeedLikeOrComment.feedType = 2
                        lstOfFeedLikeOrComment.responseBy = userId
                        lstOfFeedLikeOrComment.name = fullName
                        lstOfFeedLikeOrComment.profileUrl = profilePath
                        lstOfFeedLikeOrComment.id = likeCommentResult?.iD ?: 0
                        lstOfFeedLikeOrComment.isDeleted = true
                        commentList.add(lstOfFeedLikeOrComment)
                        uploadObject.lstOfFeedLikeOrComments?.add(
                            lstOfFeedLikeOrComment
                        )
                        etNeighborComment.setText("")
                    } else {
                        uploadObject.commentCount = uploadObject.commentCount?.minus(1)
                        uploadObject.isCommented = false
                        commentList.remove(commentFeed)
                        uploadObject.lstOfFeedLikeOrComments?.remove(commentFeed)
                    }
                    for (i in 0 until isFromUser.size) {
                        if (uploadObject.iD == isFromUser[i].iD) {
                            isFromUser[i] = uploadObject
                        }
                    }
                    setCommentAdapter()
                } else {
                    mActivity.showMessage(responseMessage)
                }
            }
        })
    }
    inner class NeighborCommentAdapter(
        val context: Context,
        val likeCommentList: ArrayList<LstOfFeedLikeOrComment>,
        val uploadObject: FeedResponseResult
    ): RecyclerView.Adapter<NeighborCommentAdapter.NeighborCommentHolder>(){
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): NeighborCommentHolder {
            return NeighborCommentHolder(
                LayoutInflater.from(activity).inflate(R.layout.raw_comment, p0, false)
            )
        }

        override fun getItemCount(): Int {
            return likeCommentList.size
        }

        override fun onBindViewHolder(p0: NeighborCommentHolder, p1: Int) {
            if (likeCommentList[p1].profileUrl!=null) {
                p0.sdvCommentedUserImage.loadFrescoImage(mActivity, likeCommentList[p1].profileUrl ?: "", 1)
            }
            likeCommentList[p1].isDeleted = likeCommentList[p1].isDeleted ?: false
            p0.tvCommentatorName.text = likeCommentList[p1].name ?: ""
            p0.tvCommentatorComment.text = likeCommentList[p1].comments ?: ""
            var differenceTime = ""
            val formatter = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
            val formatter2 = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
//            formatter.timeZone = TimeZone.getTimeZone("UTC")
            try {
                val date1 = if (likeCommentList[p1].isDeleted == true) {
                    formatter2.parse(likeCommentList[p1].date ?: "")
                }else{
                    formatter.parse(likeCommentList[p1].date ?: "")
                }
                val date2 = formatter2.parse(Utils.getCurrentTimeStamp())

                differenceTime = printDifference(date1!!, date2!!)
            }catch (e: Exception){
                e.printStackTrace()
            }
            p0.tvCommentatorTime.text = differenceTime
            if (userId == likeCommentList[p1].responseBy){
                p0.tvDeleteComment.visibility = View.VISIBLE
            }else{
                p0.tvDeleteComment.visibility = View.GONE
            }
            val tvCommentName = p0.tvCommentatorName.layoutParams as RelativeLayout.LayoutParams
            val tvComment = p0.tvCommentatorComment.layoutParams as RelativeLayout.LayoutParams
            if (p0.tvDeleteComment.visibility == View.VISIBLE){
                tvCommentName.addRule(RelativeLayout.START_OF,R.id.tvDeleteComment)
                tvComment.addRule(RelativeLayout.START_OF,R.id.tvDeleteComment)
                p0.tvCommentatorName.layoutParams = tvCommentName
                p0.tvCommentatorComment.layoutParams = tvComment
            }else{
                tvCommentName.addRule(RelativeLayout.ALIGN_PARENT_END)
                tvComment.addRule(RelativeLayout.ALIGN_PARENT_END)
                p0.tvCommentatorName.layoutParams = tvCommentName
                p0.tvCommentatorComment.layoutParams = tvComment
            }
            p0.tvDeleteComment.setOnClickListener {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
//                mActivity.showMwssage(mActivity.resources.getString(R.string.under_dev))
                deleteComment(uploadObject, likeCommentList[p1], likeCommentList[p1].id ?: 0)
            }
        }

        private fun deleteComment(
            uploadObject: FeedResponseResult,
            lstOfFeedLikeOrComment: LstOfFeedLikeOrComment,
            id: Int
        ) {
            mActivity.hideKeyboard()
            Comman_Methods.isCustomPopUpShow(mActivity,
            title = mActivity.resources.getString(R.string.del_comment),
            message = mActivity.resources.getString(R.string.del_comm_conf),
            positiveButtonListener = object : PositiveButtonListener {
                override fun okClickListener() {
                    callLikeCommentShareApi(uploadObject,"", lstOfFeedLikeOrComment)
                }
            })
        }

        fun printDifference(startDate: Date, endDate: Date): String {
            //milliseconds
            var timeDifference = ""
            var different = endDate.time - startDate.time

            System.out.println("startDate : $startDate")
            System.out.println("endDate : $endDate")
            System.out.println("different : $different")

            val secondsInMilli = 1000L
            val minutesInMilli = secondsInMilli * 60
            val hoursInMilli = minutesInMilli * 60
            val daysInMilli = hoursInMilli * 24
            val monthInMilli = daysInMilli * 30

            val elapsedMonths = different / monthInMilli
            different %= monthInMilli

            val elapsedDays = different / daysInMilli
            different %= daysInMilli

            val elapsedHours = different / hoursInMilli
            different %= hoursInMilli

            val elapsedMinutes = different / minutesInMilli
            different %= minutesInMilli

            val elapsedSeconds = different / secondsInMilli

            timeDifference = when {
                elapsedMonths > 0L -> elapsedDays.toString()+ mActivity.resources.getString(R.string.str_month_ago)
                elapsedDays > 0L -> elapsedDays.toString()+ mActivity.resources.getString(R.string.str_day_ago)
                elapsedHours > 0L -> elapsedHours.toString()+ mActivity.resources.getString(R.string.str_hour_ago)
                elapsedMinutes > 0L -> elapsedMinutes.toString() + mActivity.resources.getString(R.string.str_min_ago)
                elapsedSeconds > 1L -> elapsedSeconds.toString() + mActivity.resources.getString(R.string.str_second_ago)
                else -> getString(R.string.str_just_now)
            }
            return timeDifference
        }
        inner class NeighborCommentHolder(view: View): RecyclerView.ViewHolder(view){
            var sdvCommentedUserImage: SimpleDraweeView = view.sdvCommentedUserImage
            var tvCommentatorName:TextView = view.tvCommentatorName
            var tvCommentatorComment:TextView = view.tvCommentatorComment
            var tvCommentatorTime:TextView = view.tvCommentatorTime
            var tvDeleteComment:TextView = view.tvDeleteComment
        }
    }
}
