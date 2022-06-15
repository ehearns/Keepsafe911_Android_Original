package com.keepSafe911.fragments.neighbour


import AnimationType
import addFragment
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.location.Location
import android.net.Uri
import android.os.*
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.gms.maps.model.LatLng
import com.google.gson.JsonObject
import com.kotlinpermissions.KotlinPermissions
import com.keepSafe911.BuildConfig
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.gps.GpsTracker
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.listner.OnLoadMoreListener
import com.keepSafe911.listner.PositiveButtonListener
import com.keepSafe911.model.BaseViewHolder
import com.keepSafe911.model.LikeCommentResult
import com.keepSafe911.model.response.*
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import com.keepSafe911.utils.Comman_Methods.Companion.avoidDoubleClicks
import com.keepSafe911.webservices.WebApiClient
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_neighbour_new.*
import kotlinx.android.synthetic.main.raw_item_load.view.*
import kotlinx.android.synthetic.main.raw_neighbour_image.view.*
import kotlinx.android.synthetic.main.raw_neighbour_vedio.view.tvCategoryNameVideo
import kotlinx.android.synthetic.main.raw_neighbour_vedio.view.tvPostComment
import kotlinx.android.synthetic.main.raw_neighbour_vedio.view.tvPostCommentCount
import kotlinx.android.synthetic.main.raw_neighbour_vedio.view.tvPostDate
import kotlinx.android.synthetic.main.raw_neighbour_vedio.view.tvPostDescription
import kotlinx.android.synthetic.main.raw_neighbour_vedio.view.tvPostLikeCount
import kotlinx.android.synthetic.main.raw_neighbour_vedio.view.tvPostMilesVideo
import kotlinx.android.synthetic.main.raw_neighbour_vedio.view.tvPostOptionVideo
import kotlinx.android.synthetic.main.raw_neighbour_vedio.view.tvPostSeparator
import kotlinx.android.synthetic.main.raw_neighbour_vedio.view.tvPostShare
import kotlinx.android.synthetic.main.raw_neighbour_vedio.view.tvPostTitle
import kotlinx.android.synthetic.main.raw_neighbour_vedio.view.tvVideoNotSupported
import kotlinx.android.synthetic.main.raw_neighbour_vedio_new.view.*
import kotlinx.android.synthetic.main.toolbar_header.*
import retrofit2.Call
import retrofit2.Response
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.math.abs


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

//• = \u2022, ● = \u25CF, ○ = \u25CB, ▪ = \u25AA, ■ = \u25A0, □ = \u25A1, ► = \u25BA
class NeighbourFragment : HomeBaseFragment(), View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {
    override fun onRefresh() {
        feedResponseList = ArrayList()
        pageSizeLoad = 0
        callNeighborlyListApi(pageSizeLoad,true)
    }

    private val TYPE_IMAGE = 2
    private val TYPE_LOAD = 3
    private val TYPE_VIDEO = 1
    private var gpstracker: GpsTracker? = null
    var userId: Int = 0
    var isLoad: Boolean = true
    lateinit var appDatabase: OldMe911Database
    private lateinit var neighbourAdapter: NeighbourAdapter
    var feedResponseList:ArrayList<FeedResponseResult> = ArrayList()
    var pageSizeLoad: Int = 0
    var fullName: String = ""
    var loginData: LoginObject = LoginObject()

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    /*companion object {
        fun newInstance(
            isFromUser: ArrayList<FeedResponseResult>
        ): NeighbourFragment {
            val args = Bundle()
            args.putParcelableArrayList(ARG_PARAM1, isFromUser)
            val fragment = NeighbourFragment()
            fragment.arguments = args
            return fragment
        }
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
//            feedResponseList = it.getParcelableArrayList(ARG_PARAM1) ?: ArrayList()
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
        return inflater.inflate(R.layout.fragment_neighbour_new, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appDatabase = OldMe911Database.getDatabase(mActivity)
        gpstracker = GpsTracker(mActivity)
        loginData = appDatabase.loginDao().getAll()
        userId = loginData.memberID
        fullName = (loginData.firstName ?: "")+" "+(loginData.lastName ?: "")
        setHeader()
        mActivity.checkNavigationItem(2)
        feedResponseList = ArrayList()
        val layoutManager = LinearLayoutManager(mActivity, RecyclerView.VERTICAL, false)
        if (rvNeighbourNewList!=null) {
            rvNeighbourNewList.layoutManager = layoutManager
            neighbourAdapter = NeighbourAdapter(mActivity, feedResponseList, userId)
            rvNeighbourNewList.adapter = neighbourAdapter
        }
        callNeighborlyListApi(pageSizeLoad,true)

        ivAddNeighbour.setOnClickListener(this)
        srlNeighborNewList.setOnRefreshListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id){
            R.id.ivAddNeighbour -> {
                mActivity.hideKeyboard()
                avoidDoubleClicks(v)
                if (rvNeighbourNewList!=null) {
                    if (rvNeighbourNewList.visibility == View.VISIBLE) {
                        rvNeighbourNewList.onRelease()
                    }
                }
                mActivity.addFragment(
                    AddNeighbourFragment(),
                    true,
                    true,
                    animationType = AnimationType.bottomtotop
                )
            }
        }
    }

    private fun callNeighborlyListApi(pageSize: Int,load: Boolean) {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            if (srlNeighborNewList.isRefreshing) {
                srlNeighborNewList.isRefreshing = false
            }
            if (load) {
                Comman_Methods.isProgressShow(mActivity)
            }
            mActivity.isSpeedAvailable()
            val adminID: Int = appDatabase.loginDao().getAll().adminID ?: 0
            val callNeighborlyApi = WebApiClient.getInstance(mActivity).webApi_without?.getNewsFeed(pageSize, adminID)
            callNeighborlyApi?.enqueue(object : retrofit2.Callback<GetFeedResponse>{
                override fun onFailure(call: Call<GetFeedResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                    if (feedResponseList.size > 0){
                        if (feedResponseList[feedResponseList.size-1].iD==0) {
                            feedResponseList.removeAt(feedResponseList.size - 1)
                            neighbourAdapter.notifyItemRemoved(feedResponseList.size)
                        }
                    }
                }

                override fun onResponse(call: Call<GetFeedResponse>, response: Response<GetFeedResponse>) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            if (feedResponseList.size > 0) {
                                if (feedResponseList[feedResponseList.size - 1].iD == 0) {
                                    feedResponseList.removeAt(feedResponseList.size - 1)
                                    neighbourAdapter.notifyItemRemoved(feedResponseList.size)
                                }
                            }
                            response.body()?.let {
                                if (it.status == true) {
                                    val feedDataResult = it.result ?: ArrayList()
                                    if (feedDataResult.size > 0) {
                                        for (i in 0 until feedDataResult.size) {
                                            val familyMonitorResult = FeedResponseResult()
                                            familyMonitorResult.iD =
                                                feedDataResult[i].iD ?: 0
                                            familyMonitorResult.title =
                                                feedDataResult[i].title ?: ""
                                            familyMonitorResult.feeds =
                                                feedDataResult[i].feeds ?: ""
                                            familyMonitorResult.type =
                                                feedDataResult[i].type ?: ""
                                            familyMonitorResult.location =
                                                feedDataResult[i].location ?: ""
                                            familyMonitorResult._lat =
                                                feedDataResult[i]._lat ?: 0.0
                                            familyMonitorResult._long =
                                                feedDataResult[i]._long ?: 0.0
                                            familyMonitorResult.file =
                                                feedDataResult[i].file ?: ""
                                            familyMonitorResult.createdOn =
                                                feedDataResult[i].createdOn ?: ""
                                            familyMonitorResult.createdBy =
                                                feedDataResult[i].createdBy ?: 0
                                            familyMonitorResult.categoryID =
                                                feedDataResult[i].categoryID ?: 0
                                            familyMonitorResult.isDeleted =
                                                feedDataResult[i].isDeleted ?: false
                                            familyMonitorResult.categoryName =
                                                feedDataResult[i].categoryName ?: ""
                                            familyMonitorResult.addedBy =
                                                feedDataResult[i].addedBy ?: ""
                                            familyMonitorResult.fileType =
                                                feedDataResult[i].fileType ?: 0
                                            familyMonitorResult.userImage =
                                                feedDataResult[i].userImage ?: ""
                                            familyMonitorResult.lstOfFeedLikeOrComments?.addAll(
                                                feedDataResult[i].lstOfFeedLikeOrComments ?: ArrayList()
                                            )
                                            val familyFeedList = familyMonitorResult.lstOfFeedLikeOrComments ?: ArrayList()
                                            for (j in 0 until familyFeedList.size) {
                                                val feedLike = familyFeedList[j]
                                                val feedType = feedLike.feedType ?: 0
                                                when (feedType) {
                                                    1 -> familyMonitorResult.likeCount =
                                                        familyMonitorResult.likeCount?.plus(1)
                                                    else -> familyMonitorResult.likeCount =
                                                        familyMonitorResult.likeCount
                                                }
                                                when (feedType) {
                                                    2 -> familyMonitorResult.commentCount =
                                                        familyMonitorResult.commentCount?.plus(1)
                                                    else -> familyMonitorResult.commentCount =
                                                        familyMonitorResult.commentCount
                                                }
                                                when {
                                                    feedType == 1 && feedLike.responseBy == userId -> familyMonitorResult.isLiked =
                                                        true
                                                    else -> familyMonitorResult.isLiked =
                                                        familyMonitorResult.isLiked
                                                }
                                                when {
                                                    feedType == 2 && feedLike.responseBy == userId -> familyMonitorResult.isCommented =
                                                        true
                                                    else -> familyMonitorResult.isCommented =
                                                        familyMonitorResult.isCommented
                                                }

                                            }
                                            feedResponseList.add(familyMonitorResult)
                                        }
                                        tvNeighbourNoData.visibility = View.GONE
                                        if (rvNeighbourNewList != null) {
                                            rvNeighbourNewList.visibility = View.VISIBLE
                                            rvNeighbourNewList.setVideoInfoList(feedResponseList)
                                            val layoutManager = LinearLayoutManager(
                                                mActivity, RecyclerView.VERTICAL, false)
                                            rvNeighbourNewList.layoutManager = layoutManager
                                            feedResponseList.add(FeedResponseResult())
                                            neighbourAdapter = NeighbourAdapter(
                                                mActivity, feedResponseList, userId)
                                            rvNeighbourNewList.adapter = neighbourAdapter
                                            neighbourAdapter.notifyDataSetChanged()
                                            if (pageSize > 0) {
                                                if (feedResponseList.size > 1) {
                                                    rvNeighbourNewList.smoothScrollToPosition(
                                                        feedResponseList.size - feedDataResult.size - 1
                                                    )
                                                }
                                            }
                                        }
                                        pageSizeLoad += 1
                                        neighbourAdapter.setOnLoadListener(object :
                                            OnLoadMoreListener {
                                            override fun onLoadMore() {
                                                if (isLoad) {
                                                    callNeighborlyListApi(pageSizeLoad, false)
                                                    isLoad = false
                                                }
                                            }
                                        })


                                        if (feedResponseList.size > 0) {
                                            tvNeighbourNoData.visibility = View.GONE
                                            if (rvNeighbourNewList != null) {
                                                rvNeighbourNewList.visibility = View.VISIBLE
                                            }
                                        } else {
                                            tvNeighbourNoData.visibility = View.VISIBLE
                                            if (rvNeighbourNewList != null) {
                                                rvNeighbourNewList.visibility = View.GONE
                                            }
                                        }
                                        isLoad = true
                                    } else {
                                        if (feedResponseList.size > 0) {
                                            if (rvNeighbourNewList != null) {
                                                rvNeighbourNewList.visibility = View.VISIBLE
                                                tvNeighbourNoData.visibility = View.GONE
                                                neighbourAdapter = NeighbourAdapter(
                                                    mActivity,
                                                    feedResponseList,
                                                    userId
                                                )
                                                rvNeighbourNewList.adapter = neighbourAdapter
                                                neighbourAdapter.notifyDataSetChanged()
                                                if (pageSize > 0) {
                                                    if (feedResponseList.size > 1) {
                                                        rvNeighbourNewList.smoothScrollToPosition(
                                                            feedResponseList.size - 1
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            if (rvNeighbourNewList != null) {
                                                rvNeighbourNewList.visibility = View.GONE
                                            }
                                            tvNeighbourNoData.visibility = View.VISIBLE
                                        }
                                        isLoad = false
                                    }
                                } else {
                                    if (feedResponseList.size > 0) {
                                        if (rvNeighbourNewList != null) {
                                            rvNeighbourNewList.visibility = View.VISIBLE
                                            tvNeighbourNoData.visibility = View.GONE
                                            neighbourAdapter =
                                                NeighbourAdapter(mActivity, feedResponseList, userId)
                                            rvNeighbourNewList.adapter = neighbourAdapter
                                            neighbourAdapter.notifyDataSetChanged()
                                        }
                                    } else {
                                        if (rvNeighbourNewList != null) {
                                            rvNeighbourNewList.visibility = View.GONE
                                        }
                                        tvNeighbourNoData.visibility = View.VISIBLE
                                    }
                                    isLoad = false
                                }
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        if (feedResponseList.size > 0){
                            if (feedResponseList[feedResponseList.size-1].iD==0) {
                                feedResponseList.removeAt(feedResponseList.size - 1)
                                neighbourAdapter.notifyItemRemoved(feedResponseList.size)
                            }
                        }
                        Utils.showSomeThingWrongMessage(mActivity)
                    }
                }
            })
        } else {
            Utils.showNoInternetMessage(mActivity)
        }
    }

    private fun setHeader() {
        val policy =
            StrictMode.ThreadPolicy.Builder()
                .permitAll().build()
        StrictMode.setThreadPolicy(policy)
        tvHeader.text = mActivity.resources.getString(R.string.str_neighborhood_news)
        Utils.setTextGradientColor(tvHeader)
        mActivity.enableDrawer()
        tvMap.visibility = View.VISIBLE
        /*iv_back.visibility = View.VISIBLE
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            if (rvNeighbourNewList.visibility == View.VISIBLE) {
                rvNeighbourNewList.onRelease()
            }
            mActivity.onBackPressed()
        }*/
        iv_menu.visibility = View.VISIBLE
        iv_menu.setOnClickListener {
            mActivity.hideKeyboard()
            /*if (rvNeighbourNewList.visibility == View.VISIBLE) {
                rvNeighbourNewList.onRelease()
            }*/
            avoidDoubleClicks(it)
            mActivity.openDrawer()
        }
        tvMap.setOnClickListener {
            mActivity.hideKeyboard()
            avoidDoubleClicks(it)
            if (rvNeighbourNewList!=null) {
                if (rvNeighbourNewList.visibility == View.VISIBLE) {
                    rvNeighbourNewList.onRelease()
                }
            }
            mActivity.addFragment(
                NeighbourMapFragment.newInstance(feedResponseList),
                true,
                true,
                animationType = AnimationType.leftInfadeOut
            )
        }
        mActivity.checkUserActive()
    }

    inner class NeighbourAdapter (
        val context: Context,
        private var uploadList: ArrayList<FeedResponseResult>,
        private val userId: Int
    ): RecyclerView.Adapter<BaseViewHolder>() {

        var currentPositionLike:Int = -1
        var currentPositionComment:Int = -1
        var isFromButton = false
        var isValueChange = false
        var isFirstTime = true
        var isFromApiFalse = false
        var onLoadMoreListener: OnLoadMoreListener? = null

        init {
            if (rvNeighbourNewList!=null) {
                val linearLayoutManager = rvNeighbourNewList.layoutManager as LinearLayoutManager
                rvNeighbourNewList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        val firstVisibleItem = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
                        val lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition()
                        srlNeighborNewList?.isRefreshing = (firstVisibleItem == 0)
                        if (lastVisibleItem == uploadList.size -1) {
                            onLoadMoreListener?.onLoadMore()
                        }
                    }
                })
            }
        }
        override fun onCreateViewHolder(viewGroup:  ViewGroup, viewType: Int): BaseViewHolder {
            var viewHolder: BaseViewHolder?  = null
            val inflater :LayoutInflater  = LayoutInflater.from(viewGroup.context)

            when (viewType){
                TYPE_VIDEO ->{
                    val v1: View  = inflater.inflate(R.layout.raw_neighbour_vedio_new, viewGroup, false)
                    viewHolder =  NeighbourHolder(v1)

                }
                TYPE_IMAGE -> {
                    val v2: View  = inflater.inflate(R.layout.raw_neighbour_image, viewGroup, false)
                    viewHolder =  ImageHolder(v2)
                }
                TYPE_LOAD -> {
                    val v3: View  = inflater.inflate(R.layout.raw_item_load, viewGroup, false)
                    viewHolder =  LoadingHolder(v3)
                }
            }
            return viewHolder!!

        }

        fun setOnLoadListener(onLoadMoreListener: OnLoadMoreListener){
            this.onLoadMoreListener = onLoadMoreListener
        }


        override fun getItemViewType(position: Int): Int {
            return if (uploadList[position].iD==0){
                TYPE_LOAD
            } else {
                TYPE_VIDEO
            }
        }
        override fun getItemCount(): Int {
            return uploadList.size
        }

        override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
            holder.onBind(position)
            if (currentPositionLike == position){
                if (isFromButton) {
                    if (!isValueChange) {
//                    uploadList[position].isLiked = uploadList[position].isLiked == false
                        if (uploadList[position].isLiked == false) {
                            val commentFeed = LstOfFeedLikeOrComment()
                            uploadList[position].likeCount = uploadList[position].likeCount?.plus(1)
                            uploadList[position].isLiked = true
                            commentFeed.comments = ""
                            commentFeed.date = Utils.getCurrentTimeStamp()
                            commentFeed.feedID = uploadList[position].iD
                            commentFeed.feedType = 1
                            commentFeed.responseBy = loginData.memberID
                            commentFeed.name = fullName
                            commentFeed.profileUrl = loginData.profilePath ?: ""
                            commentFeed.isDeleted = true
                            callLikeCommentShareApi(uploadList[position], commentFeed)
                        } else {
                            if (uploadList[position].likeCount!! > 0) {
                                for (i in 0 until uploadList[position].lstOfFeedLikeOrComments!!.size) {
                                    val updateData = uploadList[position].lstOfFeedLikeOrComments!![i]
                                    if ((updateData.feedType ?: 0) == 1 && updateData.responseBy == userId) {
                                        if (uploadList[position].likeCount!! > 0) {
                                            uploadList[position].likeCount = uploadList[position].likeCount?.minus(1)
                                        }
                                        uploadList[position].isLiked = false
                                        callLikeCommentShareApi(
                                            uploadList[position],
                                            updateData
                                        )
                                    }
                                }
                            }
                        }
                        isValueChange = true
                    }
                } else if (isFromApiFalse){
                    uploadList[position].isLiked = (uploadList[position].isLiked == false)
                }
            }else{
                uploadList[position].isLiked = uploadList[position].isLiked
            }

            var color = 0
            when (uploadList[position].categoryID ?: 0){
                1 -> {
                    color = R.color.caldroid_yellow
                }
                2 -> {
                    color = R.color.color_red
                }
                3 -> {
                    color = R.color.special_green
                }
                4 -> {
                    color = android.R.color.holo_purple
                }
                5 -> {
                    color = R.color.event_color_04
                }
                6 -> {
                    color = R.color.color_purple
                }
                else -> {
                    color = R.color.bgBlack
                }
            }

            val originLatLng = LatLng(gpstracker?.getLatitude() ?: 0.0,gpstracker?.getLongitude() ?: 0.0)
            val destLatLng = LatLng(uploadList[position]._lat ?: 0.0,uploadList[position]._long ?: 0.0)

            val locationA = Location("point A")
            locationA.latitude = originLatLng.latitude
            locationA.longitude = originLatLng.longitude
            val locationB = Location("point B")
            locationB.latitude = destLatLng.latitude
            locationB.longitude = destLatLng.longitude

            var distance = locationA.distanceTo(locationB)
            if (distance < 0f){
                distance = abs(distance)
            }
            val decimalSymbols = DecimalFormatSymbols.getInstance(Locale.US)

            val wordSpan = SpannableString("\u25CF " + uploadList[position].categoryName)
            wordSpan.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(context, color)),
                0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            when(holder.itemViewType){
                TYPE_VIDEO ->{
                    val nhholder: NeighbourHolder =  holder as NeighbourHolder
                    nhholder.rlVideoParent.background = ContextCompat.getDrawable(mActivity, R.drawable.underline_shape_gray)
                    if (uploadList[position].userImage!=null) {
                        nhholder.sdvNewsUserVideo.loadFrescoImage(mActivity, uploadList[position].userImage ?: "", 1)
                    }
                    if (uploadList[position].isLiked == true) {
                        nhholder.tvPostLikeVideo.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_gray, 0, 0, 0)
                    } else {
                        nhholder.tvPostLikeVideo.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_outline, 0, 0, 0)
                    }
                    nhholder.tvCategoryNameVideo.visibility = View.VISIBLE
                    nhholder.tvCategoryNameVideo.text = wordSpan
                    nhholder.tvCategoryNameNoVideo.text = wordSpan
                    val userName = uploadList[position].addedBy ?: mActivity.resources.getString(R.string.str_neighbor)
                    val fileType = uploadList[position].fileType ?: 0
                    nhholder.tvPostSeparator.text = userName
                    if (uploadList[position].createdBy == userId) {
                        nhholder.tvPostOption.visibility = View.VISIBLE
                    } else {
                        nhholder.tvPostOption.visibility = View.GONE
                    }
                    val tvCommentName = nhholder.tvPostSeparator.layoutParams as RelativeLayout.LayoutParams
                    if (nhholder.tvPostOption.visibility == View.VISIBLE){
                        tvCommentName.addRule(RelativeLayout.START_OF,R.id.flPostDateOptionVideoNew)
                        nhholder.tvPostSeparator.layoutParams = tvCommentName
                    }else{
                        tvCommentName.addRule(RelativeLayout.ALIGN_PARENT_END)
                        nhholder.tvPostSeparator.layoutParams = tvCommentName
                    }
                    var differenceTime = ""
                    val formatter = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
                    val formatter2 = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
//                    formatter.timeZone = TimeZone.getTimeZone("UTC")
                    try {
                        val date1 = formatter.parse(uploadList[position].createdOn ?: "")
                        val date2 = formatter2.parse(Utils.getCurrentTimeStamp())

                        differenceTime = printDifference(date1!!, date2!!)
                    }catch (e: Exception){
                        e.printStackTrace()
                    }
                    if (fileType > 0){
                        if (fileType == 2){
                            nhholder.flPostFileVideo.visibility = View.GONE
                            nhholder.ivPlayVideo.visibility = View.GONE
                            nhholder.rlNoMediaVideo.visibility = View.VISIBLE
                        }else{
                            nhholder.ivPlayVideo.visibility = View.VISIBLE
                            nhholder.flPostFileVideo.visibility = View.VISIBLE
                            nhholder.rlNoMediaVideo.visibility = View.GONE
                        }
                        nhholder.ivPostAwareImageFileNew.visibility = View.GONE
                    }else{
                        nhholder.ivPlayVideo.visibility = View.GONE
                        nhholder.rlNoMediaVideo.visibility = View.GONE
                        nhholder.flPostFileVideo.visibility = View.VISIBLE
                        nhholder.ivPostAwareImageFileNew.visibility = View.VISIBLE
                        Glide.with(context).load(uploadList[position].file ?: "").into(nhholder.ivPostAwareImageFileNew)
                    }
                    nhholder.tvPostTimeDuration.text = differenceTime
                    nhholder.tvPostMilesVideo.text = DecimalFormat("##.#", decimalSymbols).format(distance/1609) +" MI"
                    nhholder.tvPostMilesNoVideo.text = DecimalFormat("##.#", decimalSymbols).format(distance/1609) +" MI"
                    var diagStartDate = ""

                    try {
                        val date1 = formatter.parse(uploadList[position].createdOn ?: "")
                        val target = SimpleDateFormat(OUTPUT_DATE_FORMAT2)
                        if (date1 != null) {
                            diagStartDate = target.format(date1)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    nhholder.tvPostDate.text = diagStartDate
                    nhholder.tvPostTitle.text = uploadList[position].title ?: ""
                    nhholder.tvPostDescription.text = uploadList[position].feeds ?: ""
                    nhholder.tvPostLikeCount.text = uploadList[position].likeCount?.toString()
                    nhholder.tvPostLikeCountNo.text = uploadList[position].likeCount?.toString()
                    nhholder.tvPostCommentCount.text = uploadList[position].commentCount?.toString()
                    nhholder.tvPostCommentCountNo.text = uploadList[position].commentCount?.toString()
                    if (Comman_Methods.isValid(uploadList[position].file ?: "")){
                        nhholder.tvVideoNotSupported.visibility = View.GONE
                    }else {
                        nhholder.tvVideoNotSupported.visibility = View.VISIBLE
                    }
                    nhholder.tvPostLikeVideo.setOnClickListener {
                        avoidDoubleClicks(it)
                        isFromButton = true
                        currentPositionLike = nhholder.bindingAdapterPosition
                        notifyDataSetChanged()
                    }
                    nhholder.tvPostComment.setOnClickListener {
                        mActivity.hideKeyboard()
                        avoidDoubleClicks(it)
                        if (rvNeighbourNewList!=null) {
                            rvNeighbourNewList.onRelease()
                        }
                        mActivity.addFragment(
                            NeighborCommentFragment.newInstance(uploadList, uploadList[position],true),
                            true,
                            true,
                            animationType = AnimationType.bottomtotop
                        )
//                        mActivity.showMwssage(mActivity.resources.getString(R.string.under_dev))
                        /*isFromButton = true
                        currentPositionComment = nhholder.bindingAdapterPosition
                        notifyDataSetChanged()*/
                    }
                    nhholder.tvPostShare.setOnClickListener {
                        mActivity.hideKeyboard()
                        avoidDoubleClicks(it)

                        val description = "Hello this news posted by "+userName+"\n\n"+
                                "News Title:- "+(uploadList[position].title ?: "")+"\n"+
                                "News Type:- "+(uploadList[position].categoryName ?: "")+"\n"+
                                "News Description:- "+(uploadList[position].feeds ?: "")

                        if (fileType > 0){
                            if (fileType == 2){
                                val shareIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/*"
                                    putExtra(Intent.EXTRA_SUBJECT, uploadList[position].title ?: "")
                                    putExtra(Intent.EXTRA_TEXT, description)
                                }
                                startActivity(Intent.createChooser(shareIntent, resources.getText(R.string.send_to)))
                            }else{
                                setPermission(fileType, uploadList[position].title ?: "",description,uploadList[position].file ?: "")
                            }
                        } else if (fileType == 0) {
                            setPermission(fileType, uploadList[position].title ?: "",description,uploadList[position].file ?: "")
                            /*Comman_Methods.shareImages(
                                mActivity,
                                uploadList[position].file ?: "",
                                description,
                                uploadList[position].title ?: ""
                            )*/
                        }
                    }
                    nhholder.tvPostOption.setOnClickListener {
                        mActivity.hideKeyboard()
                        avoidDoubleClicks(it)
                        if (rvNeighbourNewList!=null) {
                            rvNeighbourNewList.onPausePlayer()
                        }
//                        mActivity.showMwssage(mActivity.resources.getString(R.string.under_dev))
                        deletePost(uploadList[position], true)
                    }
                    nhholder.ivPlayVideo.setOnClickListener {
                        mActivity.hideKeyboard()
                        avoidDoubleClicks(it)
                        if (rvNeighbourNewList!=null) {
                            if (isFirstTime) {
                                if (position == 0 && uploadList[0].fileType == 1) {
                                    Handler(Looper.getMainLooper()).post { rvNeighbourNewList.playVideo() }
                                    rvNeighbourNewList.scrollToPosition(0)
                                } else {
                                    rvNeighbourNewList.playVideo()
                                }
                                isFirstTime = false
                            } else {
                                rvNeighbourNewList.onRestartPlayer()
                            }
                        }
                    }
                }
                TYPE_IMAGE -> {
                    val ivholder: ImageHolder = holder as ImageHolder

                    ivholder.rlImageParent.background = ContextCompat.getDrawable(mActivity, R.drawable.underline_shape_gray)
                    if (uploadList[position].userImage!=null) {
                        ivholder.sdvNewsUserImage.loadFrescoImage(mActivity, uploadList[position].userImage ?: "", 1)
                    }
                    if (uploadList[position].isLiked == true) {
                        ivholder.tvPostLikeImage.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_gray, 0, 0, 0)
                    } else {
                        ivholder.tvPostLikeImage.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_outline, 0, 0, 0)
                    }
                    ivholder.tvCategoryNameImage.visibility = View.VISIBLE
                    ivholder.tvCategoryNameImage.text = wordSpan
                    ivholder.tvCategoryNameNoImage.text = wordSpan
                    val userName = uploadList[position].addedBy ?: mActivity.resources.getString(R.string.str_neighbor)
                    val fileType = uploadList[position].fileType ?: 0
                    ivholder.tvPostSeparator.text = userName
                    if (uploadList[position].createdBy == userId) {
                        ivholder.tvPostOption.visibility = View.VISIBLE
                    } else {
                        ivholder.tvPostOption.visibility = View.GONE
                    }

                    val tvCommentName = ivholder.tvPostSeparator.layoutParams as RelativeLayout.LayoutParams
                    if (ivholder.tvPostOption.visibility == View.VISIBLE){
                        tvCommentName.addRule(RelativeLayout.START_OF,R.id.flPostDateOptionImage)
                        ivholder.tvPostSeparator.layoutParams = tvCommentName
                    }else{
                        tvCommentName.addRule(RelativeLayout.ALIGN_PARENT_END)
                        ivholder.tvPostSeparator.layoutParams = tvCommentName
                    }

                    /*if (uploadList[position].createdBy == userId) {
                        ivholder.tvPostSeparator.text =
                            "\u0009 " + appDatabase.loginDao().getAll().firstName + " " + appDatabase.loginDao().getAll().lastName
                        holder.tvPostOption.visibility = View.VISIBLE
                    } else {
                        ivholder.tvPostSeparator.text = "\u0009 " + mActivity.resources.getString(R.string.str_neighbor)
                        holder.tvPostOption.visibility = View.GONE
                    }*/
                    var differenceTime = ""
                    val formatter = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
                    val formatter2 = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
//                    formatter.timeZone = TimeZone.getTimeZone("UTC")
                    try {
                        val date1 = formatter.parse(uploadList[position].createdOn ?: "")
                        val date2 = formatter2.parse(Utils.getCurrentTimeStamp())

                        differenceTime = printDifference(date1!!, date2!!)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    ivholder.tvPostTimeDuration.text = differenceTime
                    ivholder.tvPostViewed.text = DecimalFormat("##.#", decimalSymbols).format(distance / 1609) + " MI"
                    ivholder.tvPostMilesNoImage.text = DecimalFormat("##.#", decimalSymbols).format(distance / 1609) + " MI"
                    var diagStartDate = ""

                    try {
                        val date1 = formatter.parse(uploadList[position].createdOn ?: "")
                        val target = SimpleDateFormat(OUTPUT_DATE_FORMAT2)
                        if (date1 != null) {
                            diagStartDate = target.format(date1)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    ivholder.tvPostDate.text = diagStartDate
                    ivholder.tvPostTitle.text = uploadList[position].title ?: ""
                    ivholder.tvPostDescription.text = uploadList[position].feeds ?: ""
                    ivholder.tvPostLikeCount.text = uploadList[position].likeCount?.toString()
                    ivholder.tvPostLikeCountNoImage.text = uploadList[position].likeCount?.toString()
                    ivholder.tvPostCommentCount.text = uploadList[position].commentCount?.toString()
                    ivholder.tvPostCommentCountNoImage.text = uploadList[position].commentCount?.toString()
                    if (fileType > 0) {
                        ivholder.flPostFile.visibility = View.GONE
                        ivholder.rlNoMediaImage.visibility = View.VISIBLE
                    } else {
                        ivholder.flPostFile.visibility = View.VISIBLE
                        ivholder.rlNoMediaImage.visibility = View.GONE
                        Glide.with(context).load(uploadList[position].file ?: "").into(ivholder.ivPostAwareImageFile)
                    }
                    ivholder.tvPostLikeImage.setOnClickListener {
                        avoidDoubleClicks(it)
                        isFromButton = true
                        currentPositionLike = ivholder.bindingAdapterPosition
                        notifyDataSetChanged()
                    }
                    ivholder.tvPostComment.setOnClickListener {
                        mActivity.hideKeyboard()
                        avoidDoubleClicks(it)
                        if (rvNeighbourNewList!=null) {
                            rvNeighbourNewList.onRelease()
                        }
//                        mActivity.showMwssage(mActivity.resources.getString(R.string.under_dev))
                        mActivity.addFragment(
                            NeighborCommentFragment.newInstance(uploadList, uploadList[position],true),
                            true,
                            true,
                            animationType = AnimationType.bottomtotop
                        )
                        /*isFromButton = true
                        currentPositionComment = ivholder.adapterPosition
                        notifyDataSetChanged()*/

                    }
                    ivholder.tvPostShare.setOnClickListener {
                        mActivity.hideKeyboard()
                        avoidDoubleClicks(it)

                        val description = "Hello this news posted by "+userName+"\n\n"+
                                "News Title:- "+(uploadList[position].title ?: "")+"\n"+
                                "News Type:- "+(uploadList[position].categoryName ?: "")+"\n"+
                                "News Description:- "+(uploadList[position].feeds ?: "")

                        if (fileType > 0){
                            if (fileType == 2){
                                val shareIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/*"
                                    putExtra(Intent.EXTRA_SUBJECT, uploadList[position].title ?: "")
                                    putExtra(Intent.EXTRA_TEXT, description)
                                }
                                startActivity(Intent.createChooser(shareIntent, resources.getText(R.string.send_to)))
                            }else{
                                setPermission(fileType, uploadList[position].title ?: "",description,uploadList[position].file ?: "")
//                                DownloadTask(uploadList[position].title ?: "",description).execute(uploadList[position].file ?: "")
                            }
                        } else if (fileType == 0) {
                            setPermission(fileType, uploadList[position].title ?: "",description,uploadList[position].file ?: "")
                            /*Comman_Methods.shareImages(
                                mActivity,
                                uploadList[position].file ?: "",
                                description,
                                uploadList[position].title ?: ""
                            )*/
                        }
                    }
                    ivholder.tvPostOption.setOnClickListener {
                        mActivity.hideKeyboard()
                        avoidDoubleClicks(it)
//                        mActivity.showMwssage(mActivity.resources.getString(R.string.under_dev))
                        deletePost(uploadList[position],false)
                    }
                }
                TYPE_LOAD -> {
                    val loadHolder: LoadingHolder = holder as LoadingHolder
                    loadHolder.progressBar.isIndeterminate = true
                    if (isLoad){
                        loadHolder.progressBar.visibility = View.VISIBLE
                    }else{
                        loadHolder.progressBar.visibility = View.GONE
                    }
                }
            }
        }

        fun printDifference(startDate: Date, endDate: Date): String {
            //milliseconds
            var timeDifference = ""
            var different = endDate.time - startDate.time

            println("startDate : $startDate")
            println("endDate : $endDate")
            println("different : $different")

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

        private fun deletePost(uploadObject: FeedResponseResult, isVideo: Boolean) {
            mActivity.hideKeyboard()
            Comman_Methods.isCustomPopUpShow(mActivity,
            title = mActivity.resources.getString(R.string.del_post),
            message = mActivity.resources.getString(R.string.del_post_conf),
            positiveButtonListener = object : PositiveButtonListener {
                override fun okClickListener() {
                    callDeleteApi(uploadObject)
                }

                override fun cancelClickLister() {
                    if (isVideo) {
                        if (rvNeighbourNewList!=null) {
                            rvNeighbourNewList.onRestartPlayer()
                        }
                    }
                }
            })
        }
        private fun callLikeCommentShareApi(uploadObject: FeedResponseResult, commentFeed: LstOfFeedLikeOrComment) {
            mActivity.isSpeedAvailable()
            val jsonObject = JsonObject()
            jsonObject.addProperty("ID",commentFeed.id)
            jsonObject.addProperty("Type",1)
            jsonObject.addProperty("Comments", "")
            jsonObject.addProperty("FeedID",uploadObject.iD)
            jsonObject.addProperty("ResponseBy",userId)
            jsonObject.addProperty("Date", Utils.getCurrentTimeStamp())
            Utils.postLikeCommentApi(mActivity, jsonObject, object : CommonApiListener {
                override fun postLikeComment(
                    status: Boolean,
                    likeCommentResult: LikeCommentResult?,
                    message: String,
                    responseMessage: String
                ) {
                    if (status) {
                        isFromButton = false
                        if (likeCommentResult != null) {
                            isFromButton = false
                            if (commentFeed.id == 0) {
                                commentFeed.id = likeCommentResult.iD
                                uploadObject.lstOfFeedLikeOrComments?.add(commentFeed)
                            } else {
                                uploadObject.lstOfFeedLikeOrComments?.remove(
                                    commentFeed
                                )
                            }
                            isValueChange = false
                        } else {
                            isFromApiFalse = true
                        }
                    } else {
                        isFromApiFalse = true
                    }
                }
            })
        }

        private fun callDeleteApi(uploadObject: FeedResponseResult) {
            if (ConnectionUtil.isInternetAvailable(mActivity)) {
                Comman_Methods.isProgressShow(mActivity)
                mActivity.isSpeedAvailable()
                val callDeleteFeed = WebApiClient.getInstance(mActivity).webApi_without?.deleteFeed(uploadObject.iD ?: 0)
                callDeleteFeed?.enqueue(object : retrofit2.Callback<ApiResponse>{
                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        Comman_Methods.isProgressHide()
                    }

                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        val statusCode: Int = response.code()
                        if (statusCode == 200) {
                            if (response.isSuccessful) {
                                Comman_Methods.isProgressHide()
                                response.body()?.let {
                                    if (it.status) {
                                        if (rvNeighbourNewList != null) {
                                            uploadList.remove(uploadObject)
                                            if (uploadList.size > 0) {
                                                tvNeighbourNoData.visibility = View.GONE
                                                rvNeighbourNewList.visibility = View.VISIBLE
                                            } else {
                                                tvNeighbourNoData.visibility = View.VISIBLE
                                                rvNeighbourNewList.visibility = View.GONE
                                            }
                                        }
                                        isLoad = false
                                        notifyDataSetChanged()
                                    }
                                }
                            }
                        } else {
                            Comman_Methods.isProgressHide()
                            Utils.showSomeThingWrongMessage(mActivity)
                        }
                    }
                })
            } else {
                Utils.showNoInternetMessage(mActivity)
            }
        }

        inner class ImageHolder(view: View): BaseViewHolder(view){
            override fun clear() {

            }

            var sdvNewsUserImage: SimpleDraweeView = view.sdvNewsUserImage
            var tvPostSeparator: TextView = view.tvPostSeparator
            var tvPostTimeDuration: TextView = view.tvPostMapTimeDurationImage
            var tvPostViewed: TextView = view.tvPostViewed
            var tvPostDate: TextView = view.tvPostDate
            var tvPostOption: TextView = view.tvPostOption
            var tvPostTitle: TextView = view.tvPostTitle
            var tvPostDescription: TextView = view.tvPostDescription
            var tvPostLikeCount: TextView = view.tvPostLikeCount
            var tvPostCommentCount: TextView = view.tvPostCommentCount
            var tvPostComment: TextView = view.tvPostComment
            var tvPostShare: TextView = view.tvPostShare
            var tvPostLikeImage: TextView = view.tvPostLikeImage
            var tvCategoryNameImage: TextView = view.tvCategoryNameImage
            var flPostFile: FrameLayout = view.flPostFileImage
            var ivPostAwareImageFile: ImageView = view.ivPostAwareImageFile
            var rlImageParent: RelativeLayout = view.rlImageParent
            var rlNoMediaImage: RelativeLayout = view.rlNoMediaImage
            var tvCategoryNameNoImage: TextView = view.tvCategoryNameNoImage
            var tvPostMilesNoImage: TextView = view.tvPostMilesNoImage
            var tvPostLikeCountNoImage: TextView = view.tvPostLikeCountNoImage
            var tvPostCommentCountNoImage: TextView = view.tvPostCommentCountNoImage
        }
        inner class NeighbourHolder(view: View): BaseViewHolder(view){
            override fun clear() {

            }

            var parent: View = view
            var sdvNewsUserVideo: SimpleDraweeView = view.sdvNewsUserVideoNew
            var tvPostSeparator: TextView = view.tvPostSeparator
            var tvPostTimeDuration: TextView = view.tvPostMapTimeDurationVideo
            var tvPostMilesVideo: TextView = view.tvPostMilesVideo
            var tvPostDate: TextView = view.tvPostDate
            var tvPostOption: TextView = view.tvPostOptionVideo
            var tvPostTitle: TextView = view.tvPostTitle
            var tvPostDescription: TextView = view.tvPostDescription
            var tvPostLikeCount: TextView = view.tvPostLikeCount
            var tvPostCommentCount: TextView = view.tvPostCommentCount
            var tvPostComment: TextView = view.tvPostComment
            var tvPostShare: TextView = view.tvPostShare
            var tvPostLikeVideo: TextView = view.tvPostLikeVideo
            var tvCategoryNameVideo: TextView = view.tvCategoryNameVideo
            var tvVideoNotSupported: TextView = view.tvVideoNotSupported
            var ivPostAwareImageFileNew: ImageView = view.ivPostAwareImageFileNew
            var ivPlayVideo: ImageView = view.ivPlayVideoNew
            var flPostFileVideo: FrameLayout = view.flPostFileVideo
            var rlVideoParent: RelativeLayout = view.rlVideoParent
            var rlNoMediaVideo: RelativeLayout = view.rlNoMediaVideo
            var tvCategoryNameNoVideo: TextView = view.tvCategoryNameNoVideo
            var tvPostMilesNoVideo: TextView = view.tvPostMilesNoVideo
            var tvPostLikeCountNo: TextView = view.tvPostLikeCountNo
            var tvPostCommentCountNo: TextView = view.tvPostCommentCountNo
            var progressBar: ProgressBar = view.progressBar
            init {
                ivPlayVideo = view.ivPlayVideoNew
                progressBar = view.progressBar
                parent.tag = this
            }
        }

        inner class LoadingHolder(view: View): BaseViewHolder(view){
            override fun clear() {

            }

            var progressBar: ProgressBar = view.pbLoadMore
        }
    }
    private fun setPermission(fileType: Int, title: String?, description: String, fileUrl: String) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
                .permissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                .onAccepted { permissions ->
                    if (permissions.size == 1) {
                        if (fileType > 0) {
                            if (fileType == 2) {
                                val shareIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/*"
                                    putExtra(Intent.EXTRA_SUBJECT, title)
                                    putExtra(Intent.EXTRA_TEXT, description)
                                }
                                startActivity(
                                    Intent.createChooser(
                                        shareIntent,
                                        resources.getText(R.string.send_to)
                                    )
                                )
                            } else {
                                downLoadTask(title, description, fileUrl)
                            }
                        } else if (fileType == 0) {
                            Comman_Methods.shareImages(
                                mActivity,
                                fileUrl,
                                description,
                                title
                            )
                        }
                    }
                }
                .onDenied {
                    setPermission(fileType, title, description, fileUrl)
                }
                .onForeverDenied {
                    Utils.showSettingsAlert(mActivity)
                }
                .ask()
        } else {
            KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
                .permissions(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                .onAccepted { permissions ->
                    if (permissions.size == 2) {
                        if (fileType > 0) {
                            if (fileType == 2) {
                                val shareIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/*"
                                    putExtra(Intent.EXTRA_SUBJECT, title)
                                    putExtra(Intent.EXTRA_TEXT, description)
                                }
                                startActivity(
                                    Intent.createChooser(
                                        shareIntent,
                                        resources.getText(R.string.send_to)
                                    )
                                )
                            } else {
                                downLoadTask(title, description, fileUrl)
                            }
                        } else if (fileType == 0) {
                            Comman_Methods.shareImages(
                                mActivity,
                                fileUrl,
                                description,
                                title
                            )
                        }
                    }
                }
                .onDenied {
                    setPermission(fileType, title, description, fileUrl)
                }
                .onForeverDenied {
                    Utils.showSettingsAlert(mActivity)
                }
                .ask()
        }
    }

    private fun downLoadTask(title: String?, description: String, urls: String) {
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        Comman_Methods.isProgressShow(mActivity)
        executor.execute {
            val folder = Utils.getStorageRootPath(mActivity)
            if (!folder.exists()) {
                folder.mkdir()
            }
            val subFolder = File(folder, "/News/")
            if (!subFolder.exists()) {
                subFolder.mkdir()
            }
            val storeFileName = "$title.mov"
            val pdfFile = File(subFolder.toString() + File.separator + storeFileName)
            try {
                pdfFile.createNewFile()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val u = URL(urls)
            val conn = u.openConnection()
            val contentLength = conn.contentLength

            val stream = DataInputStream(u.openStream())

            val buffer = ByteArray(contentLength)
            stream.readFully(buffer)
            stream.close()

            val fos = DataOutputStream(FileOutputStream(pdfFile))
            fos.write(buffer)
            fos.flush()
            fos.close()
            handler.post {
                Comman_Methods.isProgressHide()

                if (pdfFile != null) {

                    val shareIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "*/*"
                        if (Uri.fromFile(pdfFile) != null) {
                            if (Build.VERSION.SDK_INT > 24){
                                putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(mActivity, BuildConfig.APPLICATION_ID + ".provider",pdfFile))
                            }else{
                                putExtra(Intent.EXTRA_STREAM, Uri.fromFile(pdfFile))
                            }
                        }
                        putExtra(Intent.EXTRA_SUBJECT, title)
                        putExtra(Intent.EXTRA_TEXT, description)
                    }
                    startActivity(Intent.createChooser(shareIntent, resources.getText(R.string.send_to)))
                }
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden){
            if (rvNeighbourNewList != null)
                rvNeighbourNewList.onRelease()
        }
    }
}