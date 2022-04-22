package com.keepSafe911.fragments.homefragment.hibp


import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.keepSafe911.BuildConfig
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.model.response.hibp.Breach
import com.keepSafe911.model.response.hibp.BreachedAccount
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import com.keepSafe911.utils.Comman_Methods.Companion.isProgressHide
import com.keepSafe911.utils.Comman_Methods.Companion.isProgressShow
import com.keepSafe911.webservices.WebApiClient
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_email_compromised.*
import kotlinx.android.synthetic.main.toolbar_header.*
import retrofit2.Call
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList

class EmailCompromisedFragment : HomeBaseFragment(), View.OnClickListener {

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    lateinit var appDatabase: OldMe911Database
    var breachList: ArrayList<Breach> = ArrayList()

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
        return inflater.inflate(R.layout.fragment_email_compromised, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (mActivity.resources.getBoolean(R.bool.isTablet)) {
            etPwnedUserName.imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        } else {
            etPwnedUserName.imeOptions = EditorInfo.IME_ACTION_DONE
        }
        setHeader()
        callBreachApi()
        btnSearchEmail.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id){
            R.id.btnSearchEmail -> {
                mActivity.hideKeyboard()
                if (etPwnedUserName.text.toString().trim()!=""){
                    if (ConnectionUtil.isInternetAvailable(mActivity)){
                        Comman_Methods.avoidDoubleClicks(v)
                        callBreachAccountApi()
                    }else{
                        Utils.showNoInternetMessage(mActivity)
                    }
                }else{
                    mActivity.showMessage(mActivity.resources.getString(R.string.blank_email_username))
                    if (breachList.size > 0){
                        setBreachAdapter(breachList, mActivity.resources.getString(R.string.str_all_breaches,breachList.size), mActivity.resources.getString(R.string.str_no_breaches))
                    }
                }
            }
        }
    }

    private fun setHeader() {
        appDatabase = OldMe911Database.getDatabase(mActivity)
        mActivity.checkNavigationItem(9)
        mActivity.disableDrawer()
        tvHeader.text = mActivity.resources.getString(R.string.str_email_compromised)
        Utils.setTextGradientColor(tvHeader)
        tvHeader.setPadding(0, 0, 50, 0)
        iv_back.visibility = View.VISIBLE
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
        mActivity.checkUserActive()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            setHeader()
        }
    }

    private fun callBreachApi(){
        isProgressShow(mActivity)
        val callBreachAccountApi = WebApiClient.getInstance(mActivity).webApi_HaveIBeenPwndService?.getAllBreaches
        callBreachAccountApi?.enqueue(object : retrofit2.Callback<ArrayList<Breach>>{
            override fun onFailure(call: Call<ArrayList<Breach>>, t: Throwable) {
                rvPwnedBreaches.visibility = View.GONE
                tvPwnedNoData.visibility = View.VISIBLE
                tvPwnedNoData.text = mActivity.resources.getString(R.string.str_no_breaches)
                tvBreachesCount.visibility = View.VISIBLE
                tvBreachesCount.text = mActivity.resources.getString(R.string.str_all_breaches,breachList.size)
                isProgressHide()
            }

            override fun onResponse(
                call: Call<ArrayList<Breach>>,
                response: Response<ArrayList<Breach>>
            ) {
                val statusCode:Int= response.code()
                isProgressHide()
                if (response.isSuccessful && statusCode==200) {
                    breachList = ArrayList()
                    breachList.addAll(response.body() ?: ArrayList())
                    setBreachAdapter(breachList,mActivity.resources.getString(R.string.str_all_breaches,breachList.size), mActivity.resources.getString(R.string.str_no_breaches))
                    tvBreachesCount.visibility = View.VISIBLE
                    tvBreachesCount.text = mActivity.resources.getString(R.string.str_all_breaches,breachList.size)
                }else{
                    checkApiExceptionError(statusCode, mActivity.resources.getString(R.string.str_no_breaches))
                }
            }
        })
    }

    private fun callBreachAccountApi(){
        isProgressShow(mActivity)
        val callBreachAccount = WebApiClient.getInstance(mActivity).webApi_HaveIBeenPwndService?.getAllBreachesForAccount(etPwnedUserName.text.toString().trim())
        callBreachAccount?.enqueue(object : retrofit2.Callback<ArrayList<BreachedAccount>>{
            override fun onFailure(call: Call<ArrayList<BreachedAccount>>, t: Throwable) {
                rvPwnedBreaches.visibility = View.GONE
                tvPwnedNoData.visibility = View.VISIBLE
                tvPwnedNoData.text = mActivity.resources.getString(R.string.no_data)
                tvBreachesCount.visibility = View.VISIBLE
                tvBreachesCount.text = mActivity.resources.getString(R.string.str_all_breaches,breachList.size)
                isProgressHide()
            }

            override fun onResponse(
                call: Call<ArrayList<BreachedAccount>>,
                response: Response<ArrayList<BreachedAccount>>
            ) {
                val statusCode:Int= response.code()
                isProgressHide()
                if (response.isSuccessful && statusCode==200) {
                    val breachAccountList: ArrayList<BreachedAccount> = ArrayList()
                    breachAccountList.addAll(response.body() ?: ArrayList())
//                    setBreachAccountAdapter(breachAccountList)
                    val breachAccountSeparationList: ArrayList<Breach> = ArrayList()
                    if (breachList.size > 0) {
                        if (breachAccountList.size > 0){
                            for (i in 0 until breachAccountList.size){
                                val breachAccountData = breachAccountList[i]
                                for (j in 0 until breachList.size){
                                    val breachData = breachList[j]
                                    if (breachAccountData.name == breachData.name){
                                        breachAccountSeparationList.add(breachData)
                                    }
                                }
                            }
                            tvBreachesCount.visibility = View.VISIBLE
                            tvBreachesCount.text = mActivity.resources.getString(R.string.str_account_breaches, breachAccountSeparationList.size, etPwnedUserName.text.toString())
                            setBreachAdapter(breachAccountSeparationList,mActivity.resources.getString(R.string.str_account_breaches, breachAccountSeparationList.size, etPwnedUserName.text.toString()),mActivity.resources.getString(R.string.str_good_account, etPwnedUserName.text.toString().trim()))
                        }else{
                            rvPwnedBreaches.visibility = View.GONE
                            tvPwnedNoData.visibility = View.VISIBLE
                            tvPwnedNoData.text =
                                mActivity.resources.getString(R.string.str_good_account, etPwnedUserName.text.toString().trim())
                            tvBreachesCount.visibility = View.GONE
                        }
                    }else{
                        rvPwnedBreaches.visibility = View.GONE
                        tvPwnedNoData.visibility = View.VISIBLE
                        tvPwnedNoData.text = mActivity.resources.getString(R.string.str_no_breaches)
                        tvBreachesCount.visibility = View.VISIBLE
                        tvBreachesCount.text = mActivity.resources.getString(R.string.str_all_breaches,breachList.size)
                    }
                }else{
                    checkApiExceptionError(statusCode, mActivity.resources.getString(R.string.str_good_account, etPwnedUserName.text.toString().trim()))
                }
            }
        })
    }

    fun checkApiExceptionError(statusCode: Int, text: String){
        tvBreachesCount.visibility = View.GONE
        rvPwnedBreaches.visibility = View.GONE
        tvPwnedNoData.visibility = View.VISIBLE
        tvPwnedNoData.text = text
        when (statusCode) {
            400 -> {
//                mActivity.showMessage("Bad request — the account does not comply with an acceptable format (i.e. it's an empty string)")
            }
            401 -> {
//                mActivity.showMessage("Unauthorised — the API key provided was not valid")
            }
            403 -> {
//                mActivity.showMessage("Forbidden — no user agent has been specified in the request")
            }
            404 -> {
//                mActivity.showMessage("Not found — the account could not be found and has therefore not been pwned")
            }
            429 -> {
//                mActivity.showMessage("Too many requests — the rate limit has been exceeded")
            }
            503 -> {
//                mActivity.showMessage("Service unavailable — usually returned by Cloudflare if the underlying service is not available")
            }
            else -> {
//                mActivity.showMessage("Unknown error code $statusCode")
            }
        }
    }

    fun setBreachAdapter(breachList: ArrayList<Breach>, breachCount: String, text: String){
        if (breachList.size > 0){
            tvBreachesCount.text = breachCount
            rvPwnedBreaches.layoutManager = LinearLayoutManager(mActivity,RecyclerView.VERTICAL,false)
            rvPwnedBreaches.visibility = View.VISIBLE
            tvPwnedNoData.visibility = View.GONE
            val breachDataAdapter = BreachDataAdapter(mActivity, breachList)
            rvPwnedBreaches.adapter = breachDataAdapter
            breachDataAdapter.notifyDataSetChanged()
        }else {
            rvPwnedBreaches.visibility = View.GONE
            tvPwnedNoData.visibility = View.VISIBLE
            tvPwnedNoData.text = text
        }
    }

    inner class BreachDataAdapter(val context: Context, private val breachList: ArrayList<Breach>): RecyclerView.Adapter<BreachDataAdapter.BreachViewHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BreachViewHolder {
            return BreachViewHolder(LayoutInflater.from(context).inflate(R.layout.breach_layout, parent, false))
        }

        override fun getItemCount(): Int {
            return breachList.size
        }

        override fun onBindViewHolder(holder: BreachViewHolder, position: Int) {
            val breachData = breachList[position]
//            Picasso.get().load(breachData.logoPath).into(holder.ivIcon)
            Glide.with(context).load(breachData.logoPath).into(holder.ivIcon)
            holder.tvBreachName.text = breachData.name
            holder.tvBreachDomain.text = breachData.domain

            /*val formatter = SimpleDateFormat(DELIVER_DATE_FORMAT)
            val formatter2 = SimpleDateFormat(OUTPUT_DATE_FORMAT)
            val target = SimpleDateFormat(PARSE_DATE_FORMAT)
            val target2 = SimpleDateFormat(INPUT_DATE_FORMAT)
            var diagStartDate = ""
            var anotherDate = ""
            var modifiedDate = ""
            try {
                var date1: Date? = null
                var date2: Date? = null
                if (breachData.addedDate != null) {
                    date1 = formatter.parse(breachData.addedDate)
                }

                if (date1 != null) {
                    diagStartDate = target2.format(date1)
                }
                if (breachData.modifiedDate != null) {
                    date2 = formatter.parse(breachData.modifiedDate)
                }

                if (date2 != null) {
                    modifiedDate = target2.format(date2)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                var date3: Date? = null
                if (breachData.breachDate != null) {
                    date3 = formatter2.parse(breachData.breachDate)
                }
                if (date3 != null) {
                    anotherDate = target.format(date3)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            holder.tvBreachDate.text = "Breach Date: "+anotherDate
            holder.tvAddedDate.text = "Added Date: "+diagStartDate
            holder.tvModifiedDate.text = "Modified Date: "+modifiedDate
            holder.tvPwnCount.text = "Pwn Count: "+breachData.pwnCount.toString()
            holder.tvDescription.text = "Description: "+breachData.description
            if (breachData.dataClasses!=null) {
                if (breachData.dataClasses!!.size > 0) {
                    for (i in 0 until breachData.dataClasses!!.size) {
                        if (i > 1) {
                            holder.tvDataClasses.text =
                                holder.tvDataClasses.text.toString() +", "+ breachData.dataClasses!![i]
                        } else {
                            holder.tvDataClasses.text =
                                "Data Classes: " + breachData.dataClasses!![i]
                        }
                    }
                }
            }
            holder.tvVerified.isEnabled = breachData.isIsVerified
            holder.tvFabricated.isEnabled = breachData.isIsFabricated
            holder.tvSensitive.isEnabled = breachData.isIsSensitive
            holder.tvRetired.isEnabled = breachData.isIsRetired
            holder.tvSpamList.isEnabled = breachData.isIsSpamList*/
        }

        inner class BreachViewHolder(view: View): RecyclerView.ViewHolder(view){
            var ivIcon: ImageView = view.findViewById(R.id.ivIcon)
            var tvBreachName: TextView = view.findViewById(R.id.tvBreachName)
            var tvBreachDomain: TextView = view.findViewById(R.id.tvBreachDomain)
            var tvBreachDate: TextView = view.findViewById(R.id.tvBreachDate)
            var tvAddedDate: TextView = view.findViewById(R.id.tvAddedDate)
            var tvModifiedDate: TextView = view.findViewById(R.id.tvModifiedDate)
            var tvPwnCount: TextView = view.findViewById(R.id.tvPwnCount)
            var tvDescription: TextView = view.findViewById(R.id.tvDescription)
            var tvDataClasses: TextView = view.findViewById(R.id.tvDataClasses)
            var tvVerified: TextView = view.findViewById(R.id.tvVerified)
            var tvFabricated: TextView = view.findViewById(R.id.tvFabricated)
            var tvSensitive: TextView = view.findViewById(R.id.tvSensitive)
            var tvRetired: TextView = view.findViewById(R.id.tvRetired)
            var tvSpamList: TextView = view.findViewById(R.id.tvSpamList)
        }
    }
}
