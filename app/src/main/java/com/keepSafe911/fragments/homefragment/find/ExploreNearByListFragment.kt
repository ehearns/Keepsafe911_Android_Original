package com.keepSafe911.fragments.homefragment.find


import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.location.Location
import android.net.Uri
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.google.gson.JsonObject
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import com.kotlinpermissions.KotlinPermissions
import com.keepSafe911.BuildConfig
import com.keepSafe911.R
import com.keepSafe911.fragments.commonfrag.HomeBaseFragment
import com.keepSafe911.gps.GpsTracker
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.listner.PositiveButtonListener
import com.keepSafe911.model.response.PlacesResult
import com.keepSafe911.model.response.yelp.Business
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import hideKeyboard
import kotlinx.android.synthetic.main.fragment_explore_near_by_list.*
import kotlinx.android.synthetic.main.raw_call_message.view.*
import kotlinx.android.synthetic.main.toolbar_header.*
import sendSMS
import takeCall
import visitUrl
import java.io.*
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
private const val ARG_PARAM3 = "param3"
private const val ARG_PARAM4 = "param4"
private const val ARG_PARAM5 = "param5"
private const val ARG_PARAM6 = "param6"

class ExploreNearByListFragment : HomeBaseFragment() {

    lateinit var appDatabase: OldMe911Database
    private var liveMemberList: ArrayList<Business> = ArrayList()
    private var filterName: String = ""
    private var savePlacesToVisitList: ArrayList<PlacesResult> = ArrayList()
    private var gpstracker: GpsTracker? = null
    private var fileLatitude: Double = 0.0
    private var fileLongitude: Double = 0.0
    var isFrom: Boolean = false

    companion object {
        fun newInstance(isFrom: Boolean,
                        yelpList: ArrayList<Business>, filterName: String, placeVisit: ArrayList<PlacesResult>,
                        fileLatitude: Double, fileLongitude: Double
        ): ExploreNearByListFragment {
            val args = Bundle()
            args.putParcelableArrayList(ARG_PARAM1, yelpList)
            args.putString(ARG_PARAM2,filterName)
            args.putParcelableArrayList(ARG_PARAM3, placeVisit)
            args.putBoolean(ARG_PARAM4, isFrom)
            args.putDouble(ARG_PARAM5, fileLatitude)
            args.putDouble(ARG_PARAM6, fileLongitude)
            val fragment = ExploreNearByListFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            liveMemberList = it.getParcelableArrayList(ARG_PARAM1) ?: ArrayList()
            filterName = it.getString(ARG_PARAM2,"")
            savePlacesToVisitList = it.getParcelableArrayList(ARG_PARAM3) ?: ArrayList()
            isFrom = it.getBoolean(ARG_PARAM4,false)
            fileLatitude = it.getDouble(ARG_PARAM5,0.0)
            fileLongitude = it.getDouble(ARG_PARAM6,0.0)

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
        return inflater.inflate(R.layout.fragment_explore_near_by_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appDatabase = OldMe911Database.getDatabase(mActivity)
        gpstracker = GpsTracker(mActivity)
        getBundleData()
        setHeader()
    }

    private fun getBundleData() {
        rvLiveMemberList.layoutManager = LinearLayoutManager(mActivity,RecyclerView.VERTICAL,false)
        when {
            liveMemberList.size > 0 -> {
                rvLiveMemberList.visibility = View.VISIBLE
                tvLiveMemberNoData.visibility = View.GONE
                var sortedLiveMemberList: ArrayList<Business> = ArrayList()
                try {
                    sortedLiveMemberList = mutableListOf(liveMemberList.sortWith(compareBy { nearByList -> nearByList.distance ?: 0.0})) as ArrayList<Business>
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                rvLiveMemberList.adapter = LiveMemberListAdapter(mActivity,liveMemberList)
            }
            savePlacesToVisitList.size > 0 -> {
                rvLiveMemberList.visibility = View.VISIBLE
                tvLiveMemberNoData.visibility = View.GONE
                rvLiveMemberList.adapter = PlaceToVisitListAdapter(mActivity,savePlacesToVisitList)
            }
            else -> {
                rvLiveMemberList.visibility = View.GONE
                tvLiveMemberNoData.visibility = View.VISIBLE
            }
        }
    }

    private fun setHeader() {
        mActivity.disableDrawer()
        tvHeader.text = filterName
        if (liveMemberList.size > 0){
            tvHeader.setPadding(0, 0, 0, 0)
            tvShare.visibility = View.VISIBLE
        }else{
            tvHeader.setPadding(0, 0, 50, 0)
            tvShare.visibility = View.GONE
        }
        Utils.setTextGradientColor(tvHeader)
//        tvHeader.setPadding(0, 0, 50, 0)
        iv_back.visibility = View.VISIBLE
        iv_back.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            mActivity.onBackPressed()
        }
        tvShare.setOnClickListener {
            mActivity.hideKeyboard()
            Comman_Methods.avoidDoubleClicks(it)
            setPermission()
        }
        mActivity.checkUserActive()
    }

    inner class LiveMemberListAdapter(val context: Context, private val liveMemberList: ArrayList<Business>): RecyclerView.Adapter<LiveMemberListAdapter.LiveMemberListHolder>(){
        lateinit var callMessageDialog: Dialog
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): LiveMemberListHolder {
            return LiveMemberListHolder((LayoutInflater.from(context).inflate(R.layout.raw_live_member_list, p0,false)))
        }

        override fun getItemCount(): Int {
            return liveMemberList.size
        }

        override fun onBindViewHolder(p0: LiveMemberListHolder, p1: Int) {
            val yelpBusinessObject = liveMemberList[p1]
            p0.tvYelpPlaceName.text = if (yelpBusinessObject.name != null) {
                yelpBusinessObject.name
            } else {
                ""
            }
            val decimalSymbols = DecimalFormatSymbols.getInstance(Locale.US)
            p0.tvYelpPlaceRating.text = DecimalFormat("##.##", decimalSymbols).format(yelpBusinessObject.rating).toString()
            p0.rbYelpPlaceRating.rating = yelpBusinessObject.rating?.toFloat() ?: 0F
            p0.rbYelpPlaceRating.isEnabled = false
            p0.tvYelpPlaceDistance.text =
                if (yelpBusinessObject.distance!=null) DecimalFormat("##.##", decimalSymbols).format((yelpBusinessObject.distance ?: 0.0) / meterToMiles).toString() + " mi" else "- mi"
            when {
                yelpBusinessObject.phone != "" -> {
                    p0.tvYelpPlacePhone.visibility = View.VISIBLE
                    p0.tvPlacePhone.visibility = View.VISIBLE
                    p0.tvYelpPlacePhone.text = yelpBusinessObject.phone
                }
                else -> {
                    p0.tvYelpPlacePhone.visibility = View.GONE
                    p0.tvPlacePhone.visibility = View.GONE
                }
            }
            when {
                yelpBusinessObject.price != "" -> {
                    p0.tvYelpPlacePrice.visibility = View.VISIBLE
                    p0.tvPlacePrice.visibility = View.VISIBLE
                    p0.tvYelpPlacePrice.text = yelpBusinessObject.price ?: ""
                }
                else -> {
                    p0.tvYelpPlacePrice.visibility = View.GONE
                    p0.tvPlacePrice.visibility = View.GONE
                }
            }
            val reviewCount = yelpBusinessObject.reviewCount ?: 0
            p0.tvYelpPlaceReview.text = "($reviewCount)"
            p0.tvPlaceCategory.text = mActivity.resources.getString(R.string.str_category) + ":- "
            yelpBusinessObject.location?.let { location ->
                location.displayAddress?.let { address ->
                    for (k in 0 until address.size) {
                        val displayAddress = address[k]
                        if (k != 0) {
                            p0.tvYelpPlaceAddress.text = p0.tvYelpPlaceAddress.text.toString() + ", " + displayAddress
                        } else {
                            p0.tvYelpPlaceAddress.text = displayAddress
                        }
                    }

                }
            }
            yelpBusinessObject.categories?.let { category ->
                for (k in 0 until category.size) {
                    val displayAddress = category[k]
                    if (k != 0) {
                        p0.tvYelpPlaceCategory.text =
                            p0.tvYelpPlaceCategory.text.toString() + ", " + displayAddress.title
                    } else {
                        p0.tvYelpPlaceCategory.text = displayAddress.title
                    }
                }
            }
            p0.rlNearBy.setOnClickListener{
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
                showCallMessageDialog(yelpBusinessObject)
            }
        }

        inner class LiveMemberListHolder(view: View): RecyclerView.ViewHolder(view){
            val tvYelpPlaceName: TextView= view.findViewById(R.id.tvYelpPlaceName)
            val tvYelpPlaceRating: TextView= view.findViewById(R.id.tvYelpPlaceRating)
            val tvYelpPlaceReview: TextView= view.findViewById(R.id.tvYelpPlaceReview)
            val tvYelpPlaceDistance: TextView= view.findViewById(R.id.tvYelpPlaceDistance)
            val tvPlaceDistance: TextView= view.findViewById(R.id.tvPlaceDistance)
            val tvYelpPlacePhone: TextView= view.findViewById(R.id.tvYelpPlacePhone)
            val tvPlacePhone: TextView= view.findViewById(R.id.tvPlacePhone)
            val tvYelpPlacePrice: TextView= view.findViewById(R.id.tvYelpPlacePrice)
            val tvPlacePrice: TextView= view.findViewById(R.id.tvPlacePrice)
            val tvYelpPlaceCategory: TextView= view.findViewById(R.id.tvYelpPlaceCategory)
            val tvYelpPlaceAddress: TextView= view.findViewById(R.id.tvYelpPlaceAddress)
            val tvPlaceCategory: TextView= view.findViewById(R.id.tvPlaceCategory)
            val rbYelpPlaceRating: RatingBar = view.findViewById(R.id.rbYelpPlaceRating)
            val rlNearBy: RelativeLayout = view.findViewById(R.id.rlNearBy)

        }
        fun showCallMessageDialog(business: Business){
            val inflater = layoutInflater
            val dialogLayout = inflater.inflate(R.layout.raw_call_message, null)
            val mDialog = android.app.AlertDialog.Builder(mActivity)
            mDialog.setView(dialogLayout)
            callMessageDialog = mDialog.create()
            callMessageDialog.window?.attributes?.windowAnimations = R.style.animationForDialog
            callMessageDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            if (business.phone!=null) {
                if (business.phone != "") {
                    dialogLayout.tvCall.visibility = View.VISIBLE
                    dialogLayout.tvSms.visibility = View.VISIBLE
                }else{
                    dialogLayout.tvCall.visibility = View.GONE
                    dialogLayout.tvSms.visibility = View.GONE
                }
            }else{
                dialogLayout.tvCall.visibility = View.GONE
                dialogLayout.tvSms.visibility = View.GONE
            }
            dialogLayout.tvDirection.visibility = View.GONE
            val coOrdinateLat = business.coordinates?.latitude ?: 0.0
            val coOrdinateLng = business.coordinates?.longitude ?: 0.0
            if (coOrdinateLat != 0.0 && coOrdinateLng != 0.0) {
                dialogLayout.tvDirection.visibility = View.VISIBLE
            } else {
                dialogLayout.tvDirection.visibility = View.GONE
            }
            dialogLayout.tvCall.setOnClickListener {
                mActivity.hideKeyboard()
                if (Comman_Methods.isSimExists(mActivity)) {
                    Comman_Methods.avoidDoubleClicks(it)
                    mActivity.takeCall(business.phone ?: "")
                }else{
                    mActivity.showMessage(mActivity.resources.getString(R.string.str_sim_prob))
                }
                callMessageDialog.dismiss()
            }
            dialogLayout.tvSms.setOnClickListener {
                mActivity.hideKeyboard()
                if (Comman_Methods.isSimExists(mActivity)) {
                    Comman_Methods.avoidDoubleClicks(it)
                    mActivity.sendSMS(business.phone ?: "")
                }else{
                    mActivity.showMessage(mActivity.resources.getString(R.string.str_sim_prob))
                }
                callMessageDialog.dismiss()
            }
            dialogLayout.tvDirection.setOnClickListener {
                mActivity.hideKeyboard()
                var lat = 0.0
                var lng = 0.0
                business.coordinates?.let { cor ->
                    lat = cor.latitude ?: 0.0
                    lng = cor.longitude ?: 0.0
                }
                if (lat != 0.0 && lng != 0.0) {
                    Comman_Methods.avoidDoubleClicks(it)
                    mActivity.visitUrl("google.navigation:q=$lat,$lng")
                }
                callMessageDialog.dismiss()
            }
            callMessageDialog.setCancelable(true)
            callMessageDialog.show()
        }

    }


    inner class PlaceToVisitListAdapter(val context: Context, private val savePlacesToVisitList: ArrayList<PlacesResult>): RecyclerView.Adapter<PlaceToVisitListAdapter.PlaceToVisitListHolder>(){
        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): PlaceToVisitListHolder {
            return PlaceToVisitListHolder(LayoutInflater.from(context).inflate(R.layout.raw_place_to_visit_list,p0,false))
        }

        override fun getItemCount(): Int {
            return savePlacesToVisitList.size
        }

        override fun onBindViewHolder(p0: PlaceToVisitListHolder, p1: Int) {
            val placesToVisitModel = savePlacesToVisitList[p1]
            val originLatLng = LatLng(gpstracker?.getLatitude() ?: 0.0,gpstracker?.getLongitude() ?: 0.0)
            val destLatLng = LatLng(placesToVisitModel.latitude ?: 0.0,placesToVisitModel.longitude ?: 0.0)

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
            p0.tvPlaceToVisitMiles.text = DecimalFormat("##.#", decimalSymbols).format(distance/1609) +" MI"
            var diagStartDate = ""
            p0.tvPlaceToVisitTitle.text = placesToVisitModel.address
            p0.rbPlaceRating.rating = placesToVisitModel.rating?.toFloat() ?: 0F
            p0.rbPlaceRating.isEnabled = false
            if (placesToVisitModel.visitDate != "") {
                var placeDate = ""
                var placeDate2 = ""
                val formatter = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
                val target = SimpleDateFormat(SHOW_DATE_TIME)
                val target2 = SimpleDateFormat(CHECK_DATE_TIME2)
                try {
                    val date1 = formatter.parse(placesToVisitModel.visitDate ?: "")
                    if (date1 != null) {
                        placeDate = target.format(date1)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    val date1 = formatter.parse(placesToVisitModel.visitDate ?: "")
                    if (date1 != null) {
                        placeDate2 = target2.format(date1)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                when {
                    placeDate != "" -> diagStartDate = placeDate
                    placeDate2 != "" -> diagStartDate = placeDate2
                }
                p0.tvPlaceToVisitDate.text = diagStartDate
            }
            p0.ivDeletePlaceToVisit.setOnClickListener {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
                removePlaceToVisit(placesToVisitModel)
            }
            p0.ivEditPlaceToVisit.setOnClickListener {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
                showNumberPickerDialog(mActivity,p1,placesToVisitModel)
            }
        }

        inner class PlaceToVisitListHolder(view: View): RecyclerView.ViewHolder(view){
            var tvPlaceToVisitTitle: TextView = view.findViewById(R.id.tvPlaceToVisitTitle)
            var tvPlaceToVisitDate: TextView = view.findViewById(R.id.tvPlaceToVisitDate)
            var tvPlaceToVisitMiles: TextView = view.findViewById(R.id.tvPlaceToVisitMiles)
            var rbPlaceRating: RatingBar = view.findViewById(R.id.rbPlaceRating)
            var ivEditPlaceToVisit: ImageView = view.findViewById(R.id.ivEditPlaceToVisit)
            var ivDeletePlaceToVisit: ImageView = view.findViewById(R.id.ivDeletePlaceToVisit)
        }

        private fun removePlaceToVisit(placesToVisitModel: PlacesResult) {
            Comman_Methods.isCustomPopUpShow(mActivity,
                title = mActivity.resources.getString(R.string.travel_ite_conf),
                message = mActivity.resources.getString(R.string.wish_travel_ite),
                positiveButtonListener = object : PositiveButtonListener {
                    override fun okClickListener() {
                        callDeletePlaceApi(placesToVisitModel)
//                    savePlacesToVisitList.remove(placesToVisitModel)
//                    appDatabase.placeToVisitDao().deletePlace(placesToVisitModel)
                    }
                })
        }

        private fun callDeletePlaceApi(placesToVisitModel: PlacesResult) {
            mActivity.isSpeedAvailable()
            Utils.deleteVisitedPlaces(mActivity, placesToVisitModel.iD ?: 0, object : CommonApiListener {
                override fun commonResponse(
                    status: Boolean,
                    message: String,
                    responseMessage: String,
                    result: String
                ) {
                    if (status) {
                        savePlacesToVisitList.remove(placesToVisitModel)
                        if (savePlacesToVisitList.size > 0) {
                            rvLiveMemberList.visibility = View.VISIBLE
                            tvLiveMemberNoData.visibility = View.GONE
                        } else {
                            rvLiveMemberList.visibility = View.GONE
                            tvLiveMemberNoData.visibility = View.VISIBLE
                        }
                        notifyDataSetChanged()
                    }
                }
            })
        }

        private fun showNumberPickerDialog(
            activity: Activity,
            position: Int,
            placesToVisitModel: PlacesResult
        ) {
            val inflater = activity.layoutInflater
            val dialogLayout = inflater.inflate(R.layout.raw_place_date, null)

            val btnSet = dialogLayout.findViewById<Button>(R.id.btnSetFreq)
            val tvPlaceDate = dialogLayout.findViewById<TextView>(R.id.tvPlaceDate)
            val ivClosePopUp = dialogLayout.findViewById<ImageView>(R.id.iv_popup_dismiss)

            val mDialog = AlertDialog.Builder(activity)
            mDialog.setView(dialogLayout)
            val frequencyDialog = mDialog.create()

            if (placesToVisitModel.visitDate!=""){
                val formatter = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
                val target = SimpleDateFormat(SHOW_DATE_TIME)
                val target2 = SimpleDateFormat(CHECK_DATE_TIME2)
                var diagStartDate = ""
                var diagStartDate2 = ""
                try {
                    var date1: Date? = null
                    if (placesToVisitModel.visitDate != null) {
                        date1 = formatter.parse(placesToVisitModel.visitDate ?: "")
                    }
                    if (date1 != null) {
                        diagStartDate = target.format(date1)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    var date1: Date? = null
                    if (placesToVisitModel.visitDate != null) {
                        date1 = formatter.parse(placesToVisitModel.visitDate ?: "")
                    }
                    if (date1 != null) {
                        diagStartDate2 = target2.format(date1)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                tvPlaceDate.text = if (diagStartDate!="")diagStartDate else diagStartDate2
            }
            tvPlaceDate.setOnClickListener {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
                setDateTimeField(tvPlaceDate)
            }
            btnSet.setOnClickListener {
                mActivity.hideKeyboard()
                when {
                    tvPlaceDate.text.toString().trim().isEmpty() -> {
                        mActivity.showMessage(mActivity.resources.getString(R.string.blank_place_date))
                    }
                    else -> {
                        Comman_Methods.avoidDoubleClicks(it)
                        val formatter = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
                        val target = SimpleDateFormat(SHOW_DATE_TIME)
                        var diagStartDate = ""
                        try {
                            var date1: Date? = null
                            if (tvPlaceDate.text.toString() != "") {
                                date1 = target.parse(tvPlaceDate.text.toString())
                            }
                            if (date1 != null) {
                                diagStartDate = formatter.format(date1)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        placesToVisitModel.visitDate = diagStartDate
                        placesToVisitModel.userId = appDatabase.loginDao().getAll().memberID
                        val jsonObject = JsonObject()
                        jsonObject.addProperty("ID",placesToVisitModel.iD)
                        jsonObject.addProperty("UserId",placesToVisitModel.userId)
                        jsonObject.addProperty("Latitude",placesToVisitModel.latitude)
                        jsonObject.addProperty("Longitude",placesToVisitModel.longitude)
                        jsonObject.addProperty("Address",placesToVisitModel.address)
                        jsonObject.addProperty("VisitDate",placesToVisitModel.visitDate)
                        callAddUpdatePlaces(jsonObject, placesToVisitModel, position)
                        frequencyDialog.dismiss()
                    }
                }
            }
            ivClosePopUp.setOnClickListener {
                mActivity.hideKeyboard()
                Comman_Methods.avoidDoubleClicks(it)
                frequencyDialog.dismiss()
            }
            frequencyDialog.setCancelable(false)
            frequencyDialog.show()
        }

        private fun callAddUpdatePlaces(
            jsonObject: JsonObject,
            placesToVisitModel: PlacesResult,
            position: Int
        ) {
            mActivity.isSpeedAvailable()
            Utils.addVisitPlacesApi(mActivity, jsonObject, object : CommonApiListener {
                override fun commonResponse(
                    status: Boolean,
                    message: String,
                    responseMessage: String,
                    result: String
                ) {
                    if (status) {
                        savePlacesToVisitList[position] = placesToVisitModel
                        notifyDataSetChanged()
                    }
                }
            })
        }

        private fun setDateTimeField(et_date: TextView) {
            mActivity.hideKeyboard()
            val dateFormatter = SimpleDateFormat(OUTED_DATE, Locale.US)
            val newCalendar = Calendar.getInstance()
            if (et_date.text.toString().trim()!=""){
                try {
                    newCalendar.time = dateFormatter.parse(et_date.text.toString().trim())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val datePickerDialog = DatePickerDialog(
                mActivity, { view, year, monthOfYear, dayOfMonth ->
                    val newDate = Calendar.getInstance()
                    newDate.set(year, monthOfYear, dayOfMonth)
//                et_date.text = dateFormatter.format(newDate.time)
                    val dateString = dateFormatter.format(newDate.time)
                    setTimeField(et_date, dateString)
                }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.datePicker.minDate = Date().time
            datePickerDialog.show()
        }

        private fun setTimeField(et_date: TextView, dateString: String) {
            var timeString = ""
            mActivity.hideKeyboard()
            val dateFormatter = SimpleDateFormat(TIME_FORMAT, Locale.US)
            val newCalendar = Calendar.getInstance()
            if (et_date.text.toString().trim() != "") {
                try {
                    newCalendar.time = dateFormatter.parse(et_date.text.toString().trim())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val timePickerDialog = TimePickerDialog(
                mActivity, { view, selectedHour, selectedMinute ->
                    val newDate = Calendar.getInstance()
                    newDate.set(0, 0, 0, selectedHour, selectedMinute)
                    timeString = dateFormatter.format(newDate.time)
                    et_date.text = "$dateString $timeString"
                }, newCalendar.get(Calendar.HOUR_OF_DAY), newCalendar.get(Calendar.MINUTE), false
            )
            timePickerDialog.setButton(
                DialogInterface.BUTTON_NEGATIVE, mActivity.resources.getString(R.string.cancel)
            ) { dialog, which -> timePickerDialog.dismiss() }
            timePickerDialog.show()
        }
    }

    private fun setPermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            KotlinPermissions.with(mActivity) // where this is an FragmentActivity instance
                .permissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
                .onAccepted { permissions ->
                    if (permissions.size == 1) {
                        downLoadTask()
                    }
                }
                .onDenied {
                    setPermission()
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
                        downLoadTask()
                    }
                }
                .onDenied {
                    setPermission()
                }
                .onForeverDenied {
                    Utils.showSettingsAlert(mActivity)
                }
                .ask()
        }
    }

    private fun downLoadTask() {
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        Comman_Methods.isProgressShow(mActivity)

        executor.execute {
            val folder = Utils.getStorageRootPath(mActivity)
            if (!folder.exists()) {
                folder.mkdir()
            }
            val subFolder = File(folder, "/ExploreNearBy/")
            if (!subFolder.exists()) {
                subFolder.mkdir()
            }
            val storeFileName = filterName + "_" + fileLatitude + fileLongitude + ".pdf"
            val pdfFile = File(subFolder.toString() + File.separator + storeFileName)
            try {
                pdfFile.createNewFile()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val file: OutputStream = FileOutputStream(pdfFile)

            val document= Document()

            PdfWriter.getInstance(document, file)


            //Inserting Image in PDF

            /*var image = Image.getInstance ("src/pdf/java4s.png");
            image.scaleAbsolute(120f, 60f);//image width,height*/

            //Inserting Table in PDF

            val table= PdfPTable(1)

            val cell = PdfPCell(Paragraph (filterName))

            cell.colspan = 1
            cell.horizontalAlignment = Element.ALIGN_CENTER
            cell.setPadding (5.0f)
//            cell.backgroundColor = BaseColor (140, 221, 8)

            table.addCell(cell)

            /*table.addCell("Name")
            table.addCell("Rating")
            table.addCell("Review")
            table.addCell("Distance")
            table.addCell("Phone")
            table.addCell("Price")
            table.addCell("Category")
            table.addCell("Address")
            */
            for (i in liveMemberList.indices){

                val yelpBusinessObject = liveMemberList[i]
                var categoryList: String = ""
                var displayAddress: String = ""
                yelpBusinessObject.location?.let { location ->
                    location.displayAddress?.let { address ->
                        for (k in 0 until address.size) {
                            displayAddress = if (k != 0) {
                                displayAddress + ", " + address[k]
                            } else {
                                address[k]
                            }
                        }

                    }
                }
                yelpBusinessObject.categories?.let { category ->
                    for (k in 0 until category.size) {
                        categoryList = if (k != 0) {
                            categoryList + ", " + (category[k].title ?: "")
                        } else {
                            category[k].title ?: ""
                        }
                    }
                }

                val decimalSymbols = DecimalFormatSymbols.getInstance(Locale.US)

                table.addCell("\n"+if (yelpBusinessObject.name != null) { mActivity.resources.getString(R.string.str_nearname)+yelpBusinessObject.name+ "\n" } else { "" }+mActivity.resources.getString(R.string.str_rating)+"- "+DecimalFormat("##.##", decimalSymbols).format(yelpBusinessObject.rating).toString()+"\n"+
                        if (liveMemberList[i].reviewCount!=null) mActivity.resources.getString(R.string.str_review)+liveMemberList[i].reviewCount.toString()+"\n" else {""}+
                        mActivity.resources.getString(R.string.str_distance)+if (liveMemberList[i].distance!=null){DecimalFormat("##.##", decimalSymbols).format(liveMemberList[i].distance ?: 0.0 / meterToMiles).toString() + " mi"} else {"- mi"}+"\n" +
                        if (liveMemberList[i].phone!="") mActivity.resources.getString(R.string.str_phone)+liveMemberList[i].phone+"\n" else {""}+
                        if (liveMemberList[i].price!="") mActivity.resources.getString(R.string.str_price)+liveMemberList[i].price+"\n" else {""}+
                        if (categoryList!="") mActivity.resources.getString(R.string.str_category)+":- "+categoryList+"\n" else {""}+
                        if (displayAddress!="") mActivity.resources.getString(R.string.str_address)+displayAddress else {""}+"\n\n")
                /*table.addCell(DecimalFormat("##.##", decimalSymbols).format(yelpBusinessObject.rating).toString())
                table.addCell(liveMemberList[i].reviewCount.toString())
                table.addCell(if (liveMemberList[i].distance!=null) DecimalFormat("##.##", decimalSymbols).format((liveMemberList[i].distance ?: 0.0) / meterToMiles).toString() + " mi" else "- mi")
                table.addCell(liveMemberList[i].phone)
                table.addCell(liveMemberList[i].price)

                table.addCell(categoryList)
                table.addCell(displayAddress)*/
            }
            table.setSpacingBefore(20.0f)       // Space Before table starts, like margin-top in CSS
            table.setSpacingAfter(20.0f)        // Space After table starts, like margin-Bottom in CSS

            //Inserting List in PDF

            /*val list=List(true,30f)
            list.add(ListItem("Java4s"))
            list.add(ListItem("Php4s"))
            list.add(ListItem("Some Thing..."))*/

            //Text formating in PDF
            /*val chunk= Chunk("Welcome To Java4s Programming Blog...")

            chunk.setUnderline(+1f,-2f)//1st co-ordinate is for line width,2nd is space between
            val chunk1= Chunk("Php4s.com")
            chunk1.setUnderline(+4f,-8f)
            chunk1.setBackground(BaseColor (17, 46, 193))*/

            //Now Insert Every Thing Into PDF Document
            document.open()//PDF document opened........

//            document.add(image)

//            document.add(Chunk.NEWLINE)   //Something like in HTML 🙂

            document.add(table)

//            document.add(chunk)
//            document.add(chunk1)

            document.add(Chunk.NEWLINE)   //Something like in HTML 🙂

//            document.newPage()            //Opened new page

            document.close()

            file.close()
            handler.post {
                Comman_Methods.isProgressHide()

                if (pdfFile != null) {
                    val shareIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "*/*"
                        if (Uri.fromFile(pdfFile) != null) {
                            if (Build.VERSION.SDK_INT > 24){
                                putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(mActivity, BuildConfig.APPLICATION_ID + ".provider", pdfFile))
                            }else{
                                putExtra(Intent.EXTRA_STREAM, Uri.fromFile(pdfFile))
                            }
                        }
                        putExtra(Intent.EXTRA_SUBJECT, filterName)
                        putExtra(Intent.EXTRA_TEXT, "")
                    }
                    startActivity(Intent.createChooser(shareIntent, resources.getText(R.string.send_to)))
                }
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (isAdded && !hidden) {
            setHeader()
        }
    }
}
