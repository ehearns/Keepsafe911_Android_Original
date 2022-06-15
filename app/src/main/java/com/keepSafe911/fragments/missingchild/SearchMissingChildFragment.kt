package com.keepSafe911.fragments.missingchild

import addFragment
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.keepSafe911.BuildConfig
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.listner.OnLoadMoreListener
import com.keepSafe911.model.response.findmissingchild.MatchResult
import com.keepSafe911.model.response.findmissingchild.MissingChildListResponse
import com.keepSafe911.model.response.findmissingchild.MissingChildListResult
import com.keepSafe911.utils.*
import com.keepSafe911.webservices.WebApiClient
import hideKeyboard
import kotlinx.android.synthetic.main.popup_search_child.*
import kotlinx.android.synthetic.main.raw_item_load.view.*
import kotlinx.android.synthetic.main.toolbar_header.*
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class SearchMissingChildFragment : HomeBaseFragment() {
    private var param1: String? = ""
    private var param2: String? = ""
    private var searchMatchResult: ArrayList<MatchResult> = ArrayList()
    private val TYPE_LOAD = 2
    private val TYPE_NORMAL = 1
    var isLoadSearch: Boolean = true
    var searchOffsetCount = 1
    private lateinit var searchAdapter: SearchAdapter

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
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
        return inflater.inflate(R.layout.popup_search_child, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        searchMatchResult = ArrayList()
        setHeader()
        setSearchAdapter(searchMatchResult)
    }

    private fun setHeader() {

        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            etSearchChild.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        } else {
            etSearchChild.imeOptions = EditorInfo.IME_ACTION_DONE
        }
        etSearchChild.hint = mActivity.resources.getString(R.string.str_search_child)
        iv_back.visibility = View.VISIBLE
        etSearchChild.visibility = View.VISIBLE
        tvCancel.visibility = View.VISIBLE
        tvCancel.text = mActivity.resources.getString(R.string.str_search)
        tvCancel.isEnabled = false
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            etSearchChild.setText("")
            mActivity.onBackPressed()
        }
        tvCancel.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            if (etSearchChild.text.toString().isEmpty()) {
                mActivity.showMessage(mActivity.resources.getString(R.string.blank_missing_child_name))
            } else {
                searchOffsetCount = 1
                searchMatchResult = ArrayList()
                callGetMissingChildList(searchOffsetCount, etSearchChild.text.toString(), true)
            }
        }
        etSearchChild.setOnTouchListener { v, event ->
            val DRAWABLE_LEFT = 0
            val DRAWABLE_TOP = 1
            val DRAWABLE_RIGHT = 2
            val DRAWABLE_BOTTOM = 3


            if (event.action == MotionEvent.ACTION_DOWN) {
                if (etSearchChild.compoundDrawables != null) {
                    if (etSearchChild.compoundDrawables[DRAWABLE_RIGHT] != null) {
                        if (event.rawX >= (etSearchChild.right - etSearchChild.compoundDrawables[DRAWABLE_RIGHT].bounds.width())) {
                            if (etSearchChild.text.toString().trim().isNotEmpty()) {
                                mActivity.hideKeyboard()
                                etSearchChild.isFocusable = false
                                etSearchChild.isFocusableInTouchMode = false
                                etSearchChild.setText("")
                            }
                            true
                        } else {
                            etSearchChild.isFocusable = true
                            etSearchChild.isFocusableInTouchMode = true
                        }
                    } else {
                        etSearchChild.isFocusable = true
                        etSearchChild.isFocusableInTouchMode = true
                    }
                } else {
                    etSearchChild.isFocusable = true
                    etSearchChild.isFocusableInTouchMode = true
                }
            }
            false
        }


        etSearchChild.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().isNotEmpty()) {
                    if (!tvCancel.isEnabled) {
                        tvCancel.isEnabled = true
                    }
                    etSearchChild.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_x_close, 0)
                } else {
                    if (tvCancel.isEnabled) {
                        tvCancel.isEnabled = false
                    }
                    etSearchChild.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        mActivity.checkUserActive()
    }

    private fun setSearchAdapter(matchResultList: ArrayList<MatchResult>) {
        rvSearchPopChild.layoutManager = LinearLayoutManager(
            mActivity,
            RecyclerView.VERTICAL,
            false
        )
        searchAdapter = SearchAdapter(mActivity, matchResultList)
        rvSearchPopChild.adapter = searchAdapter
        searchAdapter.notifyDataSetChanged()
        if (matchResultList.size > 0) {
            rvSearchPopChild.visibility = View.VISIBLE
            tvSearchChildPopNoData.visibility = View.GONE
        } else {
            rvSearchPopChild.visibility = View.GONE
            tvSearchChildPopNoData.visibility = View.VISIBLE
        }
        /*searchAdapter.setOnLoadListener(object : OnLoadMoreListener{
            override fun onLoadMore() {
                if (isLoadSearch){
                    isLoadSearch = false
                }
            }
        })*/
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
            SearchMissingChildFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    inner class SearchAdapter(
        val context: Context,
        private val matchResultList: ArrayList<MatchResult>,
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

        var onLoadMoreListener: OnLoadMoreListener? = null

        init {
            if (rvSearchPopChild != null) {
                val linearLayoutManager = rvSearchPopChild.layoutManager as LinearLayoutManager
                rvSearchPopChild.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        val lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition()
                        if (lastVisibleItem == matchResultList.size - 1) {
                                onLoadMoreListener?.onLoadMore()
                        }
                    }
                })
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var viewHolder: RecyclerView.ViewHolder?  = null
            val inflater :LayoutInflater  = LayoutInflater.from(parent.context)

            when (viewType){
                TYPE_NORMAL -> {
                    val v2: View  = inflater.inflate(R.layout.raw_missing_child,
                        parent,
                        false)
                    viewHolder =  SearchHolder(v2)
                }
                TYPE_LOAD -> {
                    val v3: View  = inflater.inflate(R.layout.raw_item_load, parent, false)
                    viewHolder =  LoadingHolder(v3)
                }
            }
            return viewHolder!!
        }

        fun setOnLoadListener(onLoadMoreListener: OnLoadMoreListener){
            this.onLoadMoreListener = onLoadMoreListener
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when(holder.itemViewType){
                TYPE_NORMAL -> {
                    val normalHolder = holder as SearchHolder
                    val matchResultModel: MatchResult = matchResultList[position]

//            val url = URLEncoder.encode(matchResultModel.imageName, "UTF-8")

                    val cornerRadius = Comman_Methods.convertDpToPixels(10F, context)
                    val options = RequestOptions()
                        .centerCrop()
                        .transform(CenterCrop(), RoundedCorners(cornerRadius.roundToInt()))
                        .placeholder(R.drawable.ic_person_placeholder)
                        .error(R.drawable.ic_person_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .priority(Priority.LOW)

                    try {
                        Glide.with(context).load(matchResultModel.imageUrl).apply(options).into(normalHolder.ivMissingChildImage)
                    } catch (e: Exception) {
                        Glide.with(context.applicationContext).load(matchResultModel.imageUrl).apply(options)
                            .into(normalHolder.ivMissingChildImage)
                    }

                    val firstName: String = matchResultModel.firstName ?: ""
                    val lastName: String = matchResultModel.lastName ?: ""
                    normalHolder.tvMissingChildName.text = "$firstName $lastName"
                    val formatter = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
                    var missingDate = ""
                    try {
                        var date1: Date? = formatter.parse(matchResultModel.dateMissing ?: "")
                        val target = SimpleDateFormat(OUTPUT_DATE_FORMAT2)
                        if (date1 != null) {
                            missingDate = target.format(date1)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    normalHolder.tvMissingSince.text = missingDate
                    val missingCity = matchResultModel.missingCity ?: ""
                    val missingState =
                        if (matchResultModel.missingState != null) if (matchResultModel.missingState != "") ", " + matchResultModel.missingState else "" else ""
                    normalHolder.tvMissingFrom.text = "$missingCity$missingState"
                    val childAge =
                        if (matchResultModel.age != null) matchResultModel.age.toString() else "0"
                    normalHolder.tvMissingAge.text = childAge
                    /*val matchScore = if (matchResultModel.matchScore != null) DecimalFormat("##.##").format(
                        matchResultModel.matchScore ?: 0.0
                    ).toString() else "0.0"
                    normalHolder.tvMissingMatchScore.text = matchScore*/
                    normalHolder.tvMissingMatchScore.visibility = View.GONE
                    normalHolder.tvMissingMatchScoreHint.visibility = View.GONE
                    normalHolder.llMain.setOnClickListener{
                        mActivity.addFragment(
                            MissingChildDetailsFragment.newInstance(matchResultModel),
                            true, true,
                            animationType = AnimationType.fadeInfadeOut)
                    }
                }
                TYPE_LOAD -> {
                    val loadHolder: LoadingHolder = holder as LoadingHolder
                    loadHolder.progressBar.isIndeterminate = true
                    if (isLoadSearch){
                        loadHolder.progressBar.visibility = View.VISIBLE
                    }else{
                        loadHolder.progressBar.visibility = View.GONE
                    }
                }
            }

        }

        override fun getItemViewType(position: Int): Int {
            return if (matchResultList[position].id==0){
                TYPE_LOAD
            }else {
                TYPE_NORMAL
            }
        }

        override fun getItemCount(): Int {
            return matchResultList.size
        }

        inner class SearchHolder(view: View) : RecyclerView.ViewHolder(view) {
            var ivMissingChildImage: ImageView = view.findViewById(R.id.ivMissingChildImage)
            var tvMissingChildName: TextView = view.findViewById(R.id.tvMissingChildName)
            var tvMissingSince: TextView = view.findViewById(R.id.tvMissingSince)
            var tvMissingFrom: TextView = view.findViewById(R.id.tvMissingFrom)
            var tvMissingAge: TextView = view.findViewById(R.id.tvMissingAge)
            var tvMissingMatchScore: TextView = view.findViewById(R.id.tvMissingMatchScore)
            var tvMissingMatchScoreHint: TextView = view.findViewById(R.id.tvMissingMatchScoreHint)
            var llMain: LinearLayout = view.findViewById(R.id.llMain)
        }

        inner class LoadingHolder(view: View): RecyclerView.ViewHolder(view){
            var progressBar: ProgressBar = view.pbLoadMore
        }
    }

    private fun callGetMissingChildList(searchOffset: Int, searchText: String, isLoader: Boolean) {
        if (ConnectionUtil.isInternetAvailable(mActivity)) {
            Comman_Methods.isProgressShow(mActivity, isLoader)
            mActivity.isSpeedAvailable()
            val callMissingChildList = WebApiClient.getInstance(mActivity).webApi_without?.getSearchMissingChild(searchOffset, searchText)
            callMissingChildList?.enqueue(object : retrofit2.Callback<MissingChildListResponse> {
                override fun onResponse(call: Call<MissingChildListResponse>, response: Response<MissingChildListResponse>) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            missingDataFailure(isLoader)
                            response.body()?.let {
                                if (it.status == true) {
                                    val result = it.result ?: MissingChildListResult()
                                    val matches = result.matches ?: ArrayList()
                                    if (matches.size > 0) {
                                        var matchSize = 1
                                        for (i in 0 until matches.size) {
                                            searchMatchResult.addAll(matches[i])
                                            matchSize = matches[i].size
                                        }
                                        if (searchMatchResult.size > 0) {
                                            tvSearchChildPopNoData.visibility = View.GONE
                                            if (rvSearchPopChild != null) {
                                                rvSearchPopChild.visibility = View.VISIBLE
                                                val layoutManager = LinearLayoutManager(
                                                    mActivity,
                                                    RecyclerView.VERTICAL,
                                                    false
                                                )
                                                rvSearchPopChild.layoutManager = layoutManager
                                                searchMatchResult.add(MatchResult())
                                                searchAdapter =
                                                    SearchAdapter(mActivity, searchMatchResult)
                                                rvSearchPopChild.adapter = searchAdapter
                                                searchAdapter.notifyDataSetChanged()
                                                if (searchOffset > 1) {
                                                    if (searchMatchResult.size > 1) {
                                                        rvSearchPopChild.smoothScrollToPosition(
                                                            searchMatchResult.size - matchSize - 1
                                                        )
                                                    }
                                                }
                                            }
                                            searchOffsetCount += 1
                                            searchAdapter.setOnLoadListener(object :
                                                OnLoadMoreListener {
                                                override fun onLoadMore() {
                                                    if (isLoadSearch) {
                                                        callGetMissingChildList(
                                                            searchOffsetCount,
                                                            searchText,
                                                            false
                                                        )
                                                        isLoadSearch = false
                                                    }
                                                }
                                            })


                                            if (searchMatchResult.size > 0) {
                                                tvSearchChildPopNoData.visibility = View.GONE
                                                if (rvSearchPopChild != null) {
                                                    rvSearchPopChild.visibility = View.VISIBLE
                                                }
                                            } else {
                                                tvSearchChildPopNoData.visibility = View.VISIBLE
                                                if (rvSearchPopChild != null) {
                                                    rvSearchPopChild.visibility = View.GONE
                                                }
                                            }
                                            isLoadSearch = true
                                        } else {
                                            noMissingData(searchOffset)
                                        }
                                    } else {
                                        noMissingData(searchOffset)
                                    }
                                }else {
                                    noMissingData(searchOffset)
                                }
                            }
                        } else {
                            missingDataFailure(isLoader)
                        }
                    } else {
                        missingDataFailure(isLoader)
                    }
                }

                override fun onFailure(call: Call<MissingChildListResponse>, t: Throwable) {
                    missingDataFailure(isLoader)
                }
            })
        } else {
            Utils.showNoInternetMessage(mActivity)
        }
    }

    private fun noMissingData(searchOffset: Int) {
        if (searchMatchResult.size > 0) {
            if (rvSearchPopChild != null) {
                rvSearchPopChild.visibility = View.VISIBLE
                tvSearchChildPopNoData.visibility = View.GONE
                searchAdapter = SearchAdapter(mActivity, searchMatchResult)
                rvSearchPopChild.adapter = searchAdapter
                searchAdapter.notifyDataSetChanged()
                if (searchOffset > 0) {
                    if (searchMatchResult.size > 1) {
                        rvSearchPopChild.smoothScrollToPosition(searchMatchResult.size - 1)
                    }
                }
            }
        } else {
            if (rvSearchPopChild != null) {
                rvSearchPopChild.visibility = View.GONE
            }
            tvSearchChildPopNoData.visibility = View.VISIBLE
        }
        isLoadSearch = false
    }

    private fun missingDataFailure(isLoader: Boolean) {
        Comman_Methods.isProgressHide(isLoader)
        if (searchMatchResult.size > 0){
            if (searchMatchResult[searchMatchResult.size-1].id==0) {
                searchMatchResult.removeAt(searchMatchResult.size - 1)
                searchAdapter.notifyItemRemoved(searchMatchResult.size)
            }
        }
    }
}