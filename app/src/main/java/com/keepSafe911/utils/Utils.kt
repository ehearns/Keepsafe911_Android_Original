package com.keepSafe911.utils

import android.app.ActivityManager
import android.app.AlertDialog
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Address
import android.location.Geocoder
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.gms.maps.model.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.keepSafe911.LiveStreamActivity
import com.keepSafe911.R
import com.keepSafe911.gps.GpsTracker
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.listner.PositiveButtonListener
import com.keepSafe911.model.AreaRadius
import com.keepSafe911.model.FlagCountryCode
import com.keepSafe911.model.request.LoginRequest
import com.keepSafe911.model.response.*
import com.keepSafe911.model.response.findmissingchild.MatchResult
import com.keepSafe911.model.response.findmissingchild.MissingChildTaskListResult
import com.keepSafe911.model.response.findmissingchild.MissingChildTaskModel
import com.keepSafe911.model.response.paymentresponse.PaymentResponse
import com.keepSafe911.model.response.voicerecognition.ManageVoiceRecognitionModel
import com.keepSafe911.model.response.yelp.Region
import com.keepSafe911.model.response.yelp.YelpResponse
import com.keepSafe911.openlive.Constants
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.webservices.WebApiClient
import deleteLiveStreamFile
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.InputStreamReader
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt


object Utils {

    var notificationCount = 0
    var notificationCountID = 0
    var lowerAudioManager: AudioManager? = null
    private val suffixes = TreeMap<Long, String>()

    init {
        suffixes[1_000L] = "k"
        suffixes[1_000_000L] = "M"
        suffixes[1_000_000_000L] = "G"
        suffixes[1_000_000_000_000L] = "T"
        suffixes[1_000_000_000_000_000L] = "P"
        suffixes[1_000_000_000_000_000_000L] = "E"
    }

    fun numberFormat(value: Long): String {
        //Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
        if (value == java.lang.Long.MIN_VALUE) return numberFormat(java.lang.Long.MIN_VALUE + 1)
        if (value < 0) return "-" + numberFormat(-value)
        if (value < 1000) return java.lang.Long.toString(value) //deal with easy case

        val e = suffixes.floorEntry(value)
        val divideBy = e.key
        val suffix = e.value

        val truncated = value / (divideBy!! / 10) //the number part of the output times 10
        val hasDecimal = truncated < 100 && truncated / 10.0 != (truncated / 10).toDouble()
        return if (hasDecimal) (truncated / 10.0).toString() + suffix else (truncated / 10).toString() + suffix
    }


    fun calculateNoOfColumns(context: Context, v: Double): Int {
        val displayMetrics = context.resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels.toFloat()
        return (dpWidth / v).toInt()
    }

    fun calculateNoOfRows(context: Context, v: Double): Int {
        val displayMetrics = context.resources.displayMetrics
        val dpWidth = displayMetrics.heightPixels.toFloat()
        return (dpWidth / v).toInt()
    }

    fun getCurrentTimeStamp(): String {
        val c = Calendar.getInstance()
        val df = SimpleDateFormat(INPUT_CHECK_DATE_FORMAT)
        return df.format(c.time)
    }

    fun getCurrentTimeStampWithMilliSeconds(): String {
        val c = Calendar.getInstance()
        val df = SimpleDateFormat(DELIVER_DATE_FORMAT)
        return df.format(c.time)
    }

    fun getTimeStamp(): String {
        val c = Calendar.getInstance()
        val df = SimpleDateFormat(TIME_FORMAT_24)
        return df.format(c.time)
    }

    fun getCompleteAddressString(context: Context, LATITUDE: Double, LONGITUDE: Double): String {
        var strAdd = ""
        val addresses: List<Address>
        val geocoder = Geocoder(context, Locale.getDefault())

        try {
            addresses = geocoder.getFromLocation(
                LATITUDE,
                LONGITUDE,
                1
            ) // Here 1 represent max location result to returned, by documents it recommended 1 to 5

            if (addresses.isNotEmpty()) {
                val address =
                    addresses[0].getAddressLine(0) // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                val address1 =
                    addresses[0].getAddressLine(1) // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                val city = addresses[0].locality
                val state = addresses[0].adminArea
                val country = addresses[0].countryName
                val postalCode = addresses[0].postalCode
                val knownName = addresses[0].featureName // Only if available else return NULL
                if (address!=null) {
                    val finaladdress = address
                    strAdd = finaladdress
                }
                Log.e("address", "getCompleteAddressString: $strAdd")
                Log.e("address", "getCompleteAddressString: $address---->$address1")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return strAdd
    }

    fun getNotificationIcon(): Int {
        val useWhiteIcon = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP
        return if (useWhiteIcon) R.mipmap.ic_launcher_push_ks else R.mipmap.ic_launcher_origin_ks
    }

    fun encodeString(address: String): String {

        return Normalizer.normalize(address, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    fun GetUniqueDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    fun isMyServiceRunning(serviceClass: Class<*>, context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun isJobServiceRunning(context: Context): Boolean{
        val scheduler : JobScheduler= context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        for (jobInfo: JobInfo in scheduler.allPendingJobs) {
            if ( jobInfo.id == GPSSERVICEJOBID ) {
                return true
            }
        }
        return false
    }

    fun GetBatterylevel(context: Context): Int {
        val batteryIntent = context.applicationContext.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        return batteryIntent?.getIntExtra("level", -1) ?: -1
    }

    fun showSettingsAlert(context: Context) {
        Comman_Methods.isCustomPopUpShow(context,
        message = context.resources.getString(R.string.str_allow_permission),
        positiveButtonText = context.resources.getString(R.string.action_settings),
        singlePositive = true,
        positiveButtonListener = object : PositiveButtonListener {
            override fun okClickListener() {
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts("package", context.packageName, null)
                intent.data = uri
                context.startActivity(intent)
            }
        })
    }

    fun showLocationSettingsAlert(context: Context) {
        Comman_Methods.isCustomPopUpShow(context,
        title = context.resources.getString(R.string.GPS_is),
        message = context.resources.getString(R.string.Gps_not_enabled),
        positiveButtonText = context.resources.getString(R.string.str_setting_cancel),
        singlePositive = true,
        positiveButtonListener = object : PositiveButtonListener {
            override fun okClickListener() {
                val gpsTracker = GpsTracker(context)
                if (!gpsTracker.CheckForLoCation()) {
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    context.startActivity(intent)
                }
            }
        })
    }

    fun AutocompletePlaces(context: Context, input: String): ArrayList<*>? {
        var resultList: ArrayList<Any>? = ArrayList()

        var conn: HttpURLConnection? = null
        val jsonResults = StringBuilder()
        try {
            val sb =
                StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON)
            // this api key is from s3ceid@gmail.com account project name Oldme App
            sb.append("?key=" + context.resources.getString(R.string.firebase_live_key))
//            sb.append("?key=AIzaSyCPC14ziyfSjC4xsvfwo22IIp0zHm9IY1c")
            //sb.append("&components=country:fr");
            sb.append("&input=" + URLEncoder.encode(input, "utf8"))
            val url = URL(sb.toString())
            Log.e("Url", "" + url)
            conn = url.openConnection() as HttpURLConnection
            val inp = InputStreamReader(conn.inputStream)

            // Load the results into a StringBuilder
            var read: Int
            val buff = CharArray(1024)
            while (inp.read(buff).let { read = it; it != -1 }) {
                jsonResults.append(buff, 0, read)
            }
        } catch (e: Exception) {
            return resultList
        } finally {
            conn?.disconnect()
        }

        try {
            // Create a JSON object hierarchy from the results
            val jsonObj = JSONObject(jsonResults.toString())
            val predsJsonArray = jsonObj.getJSONArray("predictions")

            // Extract the Place descriptions from the results
            resultList = ArrayList(predsJsonArray.length())
            for (i in 0 until predsJsonArray.length()) {
                resultList.add(predsJsonArray.getJSONObject(i).getString("description"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return resultList
    }

    fun getLocationFromAddress(context: Context, strAddress: String): LatLng? {

        val coder = Geocoder(context)
        val address: List<Address>?
        var p1: LatLng? = null

        try {
            // May throw an IOException
            address = coder.getFromLocationName(strAddress, 5)
            if (address == null) {
                return null
            }
            val location = address[0]
            p1 = LatLng(location.latitude, location.longitude)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return p1
    }

    fun saveAreaRadiusList(context: Context, areaRadiusList: ArrayList<AreaRadius>) {
        val preferences = context.getSharedPreferences("areaRadiusSharedPreference", 0)
        val editor = preferences.edit()
        val gson = Gson()
        val json = gson.toJson(areaRadiusList)
        editor.putString("areaRadiusEditor", json)
        editor.apply()
    }

    fun getAreaRadiusList(context: Context): ArrayList<AreaRadius> {
        val preferences = context.getSharedPreferences("areaRadiusSharedPreference", 0)
        val json = preferences.getString("areaRadiusEditor", "")
        val gson = Gson()
        var familyNameList = ArrayList<AreaRadius>()
        val type = object: TypeToken<List<AreaRadius>>() {
        }.type
        familyNameList = if (json!=""){
            gson.fromJson(json, type)
        }else{
            ArrayList()
        }
        return familyNameList
    }


    fun countryFlagWithCode(context: Context): ArrayList<FlagCountryCode> {
        val flagCountryCode: ArrayList<FlagCountryCode> = ArrayList()
        flagCountryCode.add(FlagCountryCode("AF", R.drawable.af_flag))
        flagCountryCode.add(FlagCountryCode("AX", R.drawable.ax_flag))
        flagCountryCode.add(FlagCountryCode("AL", R.drawable.al_flag))
        flagCountryCode.add(FlagCountryCode("DZ", R.drawable.dz_flag))
        flagCountryCode.add(FlagCountryCode("AS", R.drawable.as_flag))
        flagCountryCode.add(FlagCountryCode("AD", R.drawable.ad_flag))
        flagCountryCode.add(FlagCountryCode("AO", R.drawable.ao_flag))
        flagCountryCode.add(FlagCountryCode("AI", R.drawable.ai_flag))
        flagCountryCode.add(FlagCountryCode("AQ", R.drawable.aq_flag))
        flagCountryCode.add(FlagCountryCode("AG", R.drawable.ag_flag))
        flagCountryCode.add(FlagCountryCode("AR", R.drawable.ar_flag))
        flagCountryCode.add(FlagCountryCode("AM", R.drawable.am_flag))
        flagCountryCode.add(FlagCountryCode("AW", R.drawable.aw_flag))
        flagCountryCode.add(FlagCountryCode("AU", R.drawable.au_flag))
        flagCountryCode.add(FlagCountryCode("AT", R.drawable.at_flag))
        flagCountryCode.add(FlagCountryCode("AZ", R.drawable.az_flag))
        flagCountryCode.add(FlagCountryCode("BS", R.drawable.bs_flag))
        flagCountryCode.add(FlagCountryCode("BH", R.drawable.bh_flag))
        flagCountryCode.add(FlagCountryCode("BD", R.drawable.bd_flag))
        flagCountryCode.add(FlagCountryCode("BB", R.drawable.bb_flag))
        flagCountryCode.add(FlagCountryCode("BY", R.drawable.by_flag))
        flagCountryCode.add(FlagCountryCode("BE", R.drawable.be_flag))
        flagCountryCode.add(FlagCountryCode("BZ", R.drawable.bz_flag))
        flagCountryCode.add(FlagCountryCode("BJ", R.drawable.bj_flag))
        flagCountryCode.add(FlagCountryCode("BM", R.drawable.bm_flag))
        flagCountryCode.add(FlagCountryCode("BT", R.drawable.bt_flag))
        flagCountryCode.add(FlagCountryCode("BO", R.drawable.bo_flag))
        flagCountryCode.add(FlagCountryCode("BA", R.drawable.ba_flag))
        flagCountryCode.add(FlagCountryCode("BW", R.drawable.bw_flag))
        flagCountryCode.add(FlagCountryCode("BV", R.drawable.bv_flag))
        flagCountryCode.add(FlagCountryCode("BR", R.drawable.br_flag))
        flagCountryCode.add(FlagCountryCode("IO", R.drawable.io_flag))
        flagCountryCode.add(FlagCountryCode("BN", R.drawable.bn_flag))
        flagCountryCode.add(FlagCountryCode("BG", R.drawable.bg_flag))
        flagCountryCode.add(FlagCountryCode("BF", R.drawable.bf_flag))
        flagCountryCode.add(FlagCountryCode("BI", R.drawable.bi_flag))
        flagCountryCode.add(FlagCountryCode("KH", R.drawable.kh_flag))
        flagCountryCode.add(FlagCountryCode("CM", R.drawable.cm_flag))
        flagCountryCode.add(FlagCountryCode("CA", R.drawable.ca_flag))
        flagCountryCode.add(FlagCountryCode("CV", R.drawable.cv_flag))
        flagCountryCode.add(FlagCountryCode("KY", R.drawable.ky_flag))
        flagCountryCode.add(FlagCountryCode("CF", R.drawable.cf_flag))
        flagCountryCode.add(FlagCountryCode("TD", R.drawable.td_flag))
        flagCountryCode.add(FlagCountryCode("CL", R.drawable.cl_flag))
        flagCountryCode.add(FlagCountryCode("CN", R.drawable.cn_flag))
        flagCountryCode.add(FlagCountryCode("CX", R.drawable.cx_flag))
        flagCountryCode.add(FlagCountryCode("CC", R.drawable.cc_flag))
        flagCountryCode.add(FlagCountryCode("CO", R.drawable.co_flag))
        flagCountryCode.add(FlagCountryCode("KM", R.drawable.km_flag))
        flagCountryCode.add(FlagCountryCode("CG", R.drawable.cg_flag))
        flagCountryCode.add(FlagCountryCode("CD", R.drawable.cd_flag))
        flagCountryCode.add(FlagCountryCode("CK", R.drawable.ck_flag))
        flagCountryCode.add(FlagCountryCode("CR", R.drawable.cr_flag))
        flagCountryCode.add(FlagCountryCode("CI", R.drawable.ci_flag))
        flagCountryCode.add(FlagCountryCode("HR", R.drawable.hr_flag))
        flagCountryCode.add(FlagCountryCode("CU", R.drawable.cu_flag))
        flagCountryCode.add(FlagCountryCode("CY", R.drawable.cy_flag))
        flagCountryCode.add(FlagCountryCode("CZ", R.drawable.cz_flag))
        flagCountryCode.add(FlagCountryCode("DK", R.drawable.dk_flag))
        flagCountryCode.add(FlagCountryCode("DJ", R.drawable.dj_flag))
        flagCountryCode.add(FlagCountryCode("DM", R.drawable.dm_flag))
        flagCountryCode.add(FlagCountryCode("DO", R.drawable.do_flag))
        flagCountryCode.add(FlagCountryCode("EC", R.drawable.ec_flag))
        flagCountryCode.add(FlagCountryCode("EG", R.drawable.eg_flag))
        flagCountryCode.add(FlagCountryCode("SV", R.drawable.sv_flag))
        flagCountryCode.add(FlagCountryCode("GQ", R.drawable.gq_flag))
        flagCountryCode.add(FlagCountryCode("ER", R.drawable.er_flag))
        flagCountryCode.add(FlagCountryCode("EE", R.drawable.ee_flag))
        flagCountryCode.add(FlagCountryCode("ET", R.drawable.et_flag))
        flagCountryCode.add(FlagCountryCode("FK", R.drawable.fk_flag))
        flagCountryCode.add(FlagCountryCode("FO", R.drawable.fo_flag))
        flagCountryCode.add(FlagCountryCode("FJ", R.drawable.fj_flag))
        flagCountryCode.add(FlagCountryCode("FI", R.drawable.fi_flag))
        flagCountryCode.add(FlagCountryCode("FR", R.drawable.fr_flag))
        flagCountryCode.add(FlagCountryCode("GF", R.drawable.gf_flag))
        flagCountryCode.add(FlagCountryCode("PF", R.drawable.pf_flag))
        flagCountryCode.add(FlagCountryCode("TF", R.drawable.tf_flag))
        flagCountryCode.add(FlagCountryCode("GA", R.drawable.ga_flag))
        flagCountryCode.add(FlagCountryCode("GM", R.drawable.gm_flag))
        flagCountryCode.add(FlagCountryCode("GE", R.drawable.ge_flag))
        flagCountryCode.add(FlagCountryCode("DE", R.drawable.de_flag))
        flagCountryCode.add(FlagCountryCode("GH", R.drawable.gh_flag))
        flagCountryCode.add(FlagCountryCode("GI", R.drawable.gi_flag))
        flagCountryCode.add(FlagCountryCode("GR", R.drawable.gr_flag))
        flagCountryCode.add(FlagCountryCode("GL", R.drawable.gl_flag))
        flagCountryCode.add(FlagCountryCode("GD", R.drawable.gd_flag))
        flagCountryCode.add(FlagCountryCode("GP", R.drawable.gp_flag))
        flagCountryCode.add(FlagCountryCode("GU", R.drawable.gu_flag))
        flagCountryCode.add(FlagCountryCode("GT", R.drawable.gt_flag))
        flagCountryCode.add(FlagCountryCode("GG", R.drawable.gg_flag))
        flagCountryCode.add(FlagCountryCode("GN", R.drawable.gn_flag))
        flagCountryCode.add(FlagCountryCode("GW", R.drawable.gw_flag))
        flagCountryCode.add(FlagCountryCode("GY", R.drawable.gy_flag))
        flagCountryCode.add(FlagCountryCode("HT", R.drawable.ht_flag))
        flagCountryCode.add(FlagCountryCode("HM", R.drawable.hm_flag))
        flagCountryCode.add(FlagCountryCode("VA", R.drawable.va_flag))
        flagCountryCode.add(FlagCountryCode("HN", R.drawable.hn_flag))
        flagCountryCode.add(FlagCountryCode("HK", R.drawable.hk_flag))
        flagCountryCode.add(FlagCountryCode("HU", R.drawable.hu_flag))
        flagCountryCode.add(FlagCountryCode("IS", R.drawable.is_flag))
        flagCountryCode.add(FlagCountryCode("IN", R.drawable.in_flag))
        flagCountryCode.add(FlagCountryCode("ID", R.drawable.id_flag))
        flagCountryCode.add(FlagCountryCode("IR", R.drawable.ir_flag))
        flagCountryCode.add(FlagCountryCode("IQ", R.drawable.iq_flag))
        flagCountryCode.add(FlagCountryCode("IE", R.drawable.ie_flag))
        flagCountryCode.add(FlagCountryCode("IM", R.drawable.im_flag))
        flagCountryCode.add(FlagCountryCode("IL", R.drawable.il_flag))
        flagCountryCode.add(FlagCountryCode("IT", R.drawable.it_flag))
        flagCountryCode.add(FlagCountryCode("JM", R.drawable.jm_flag))
        flagCountryCode.add(FlagCountryCode("JP", R.drawable.jp_flag))
        flagCountryCode.add(FlagCountryCode("JE", R.drawable.je_flag))
        flagCountryCode.add(FlagCountryCode("JO", R.drawable.jo_flag))
        flagCountryCode.add(FlagCountryCode("KZ", R.drawable.kz_flag))
        flagCountryCode.add(FlagCountryCode("KE", R.drawable.ke_flag))
        flagCountryCode.add(FlagCountryCode("KI", R.drawable.ki_flag))
        flagCountryCode.add(FlagCountryCode("KP", R.drawable.kp_flag))
        flagCountryCode.add(FlagCountryCode("KR", R.drawable.kr_flag))
        flagCountryCode.add(FlagCountryCode("XK", R.drawable.xk_flag))
        flagCountryCode.add(FlagCountryCode("KW", R.drawable.kw_flag))
        flagCountryCode.add(FlagCountryCode("KG", R.drawable.kg_flag))
        flagCountryCode.add(FlagCountryCode("LA", R.drawable.la_flag))
        flagCountryCode.add(FlagCountryCode("LV", R.drawable.lv_flag))
        flagCountryCode.add(FlagCountryCode("LB", R.drawable.lb_flag))
        flagCountryCode.add(FlagCountryCode("LS", R.drawable.ls_flag))
        flagCountryCode.add(FlagCountryCode("LR", R.drawable.lr_flag))
        flagCountryCode.add(FlagCountryCode("LY", R.drawable.ly_flag))
        flagCountryCode.add(FlagCountryCode("LI", R.drawable.li_flag))
        flagCountryCode.add(FlagCountryCode("LT", R.drawable.lt_flag))
        flagCountryCode.add(FlagCountryCode("LU", R.drawable.lu_flag))
        flagCountryCode.add(FlagCountryCode("MO", R.drawable.mo_flag))
        flagCountryCode.add(FlagCountryCode("MK", R.drawable.mk_flag))
        flagCountryCode.add(FlagCountryCode("MG", R.drawable.mg_flag))
        flagCountryCode.add(FlagCountryCode("MW", R.drawable.mw_flag))
        flagCountryCode.add(FlagCountryCode("MY", R.drawable.my_flag))
        flagCountryCode.add(FlagCountryCode("MV", R.drawable.mv_flag))
        flagCountryCode.add(FlagCountryCode("ML", R.drawable.ml_flag))
        flagCountryCode.add(FlagCountryCode("MT", R.drawable.mt_flag))
        flagCountryCode.add(FlagCountryCode("MH", R.drawable.mh_flag))
        flagCountryCode.add(FlagCountryCode("MQ", R.drawable.mq_flag))
        flagCountryCode.add(FlagCountryCode("MR", R.drawable.mr_flag))
        flagCountryCode.add(FlagCountryCode("MU", R.drawable.mu_flag))
        flagCountryCode.add(FlagCountryCode("YT", R.drawable.yt_flag))
        flagCountryCode.add(FlagCountryCode("MX", R.drawable.mx_flag))
        flagCountryCode.add(FlagCountryCode("FM", R.drawable.fm_flag))
        flagCountryCode.add(FlagCountryCode("MD", R.drawable.md_flag))
        flagCountryCode.add(FlagCountryCode("MC", R.drawable.mc_flag))
        flagCountryCode.add(FlagCountryCode("MN", R.drawable.mn_flag))
        flagCountryCode.add(FlagCountryCode("ME", R.drawable.me_flag))
        flagCountryCode.add(FlagCountryCode("MS", R.drawable.ms_flag))
        flagCountryCode.add(FlagCountryCode("MA", R.drawable.ma_flag))
        flagCountryCode.add(FlagCountryCode("MZ", R.drawable.mz_flag))
        flagCountryCode.add(FlagCountryCode("MM", R.drawable.mm_flag))
        flagCountryCode.add(FlagCountryCode("NA", R.drawable.na_flag))
        flagCountryCode.add(FlagCountryCode("NR", R.drawable.nr_flag))
        flagCountryCode.add(FlagCountryCode("NP", R.drawable.np_flag))
        flagCountryCode.add(FlagCountryCode("NL", R.drawable.nl_flag))
        flagCountryCode.add(FlagCountryCode("AN", R.drawable.an_flag))
        flagCountryCode.add(FlagCountryCode("NC", R.drawable.nc_flag))
        flagCountryCode.add(FlagCountryCode("NZ", R.drawable.nz_flag))
        flagCountryCode.add(FlagCountryCode("NI", R.drawable.ni_flag))
        flagCountryCode.add(FlagCountryCode("NE", R.drawable.ne_flag))
        flagCountryCode.add(FlagCountryCode("NG", R.drawable.ng_flag))
        flagCountryCode.add(FlagCountryCode("NU", R.drawable.nu_flag))
        flagCountryCode.add(FlagCountryCode("NF", R.drawable.nf_flag))
        flagCountryCode.add(FlagCountryCode("MP", R.drawable.mp_flag))
        flagCountryCode.add(FlagCountryCode("N0", R.drawable.no_flag))
        flagCountryCode.add(FlagCountryCode("OM", R.drawable.om_flag))
        flagCountryCode.add(FlagCountryCode("PK", R.drawable.pk_flag))
        flagCountryCode.add(FlagCountryCode("PW", R.drawable.pw_flag))
        flagCountryCode.add(FlagCountryCode("PS", R.drawable.ps_flag))
        flagCountryCode.add(FlagCountryCode("PA", R.drawable.pa_flag))
        flagCountryCode.add(FlagCountryCode("PG", R.drawable.pg_flag))
        flagCountryCode.add(FlagCountryCode("PY", R.drawable.py_flag))
        flagCountryCode.add(FlagCountryCode("PE", R.drawable.pe_flag))
        flagCountryCode.add(FlagCountryCode("PH", R.drawable.ph_flag))
        flagCountryCode.add(FlagCountryCode("PN", R.drawable.pn_flag))
        flagCountryCode.add(FlagCountryCode("PL", R.drawable.pl_flag))
        flagCountryCode.add(FlagCountryCode("PT", R.drawable.pt_flag))
        flagCountryCode.add(FlagCountryCode("PR", R.drawable.pr_flag))
        flagCountryCode.add(FlagCountryCode("QA", R.drawable.qa_flag))
        flagCountryCode.add(FlagCountryCode("RO", R.drawable.ro_flag))
        flagCountryCode.add(FlagCountryCode("RU", R.drawable.ru_flag))
        flagCountryCode.add(FlagCountryCode("RW", R.drawable.rw_flag))
        flagCountryCode.add(FlagCountryCode("RE", R.drawable.re_flag))
        flagCountryCode.add(FlagCountryCode("BL", R.drawable.bl_flag))
        flagCountryCode.add(FlagCountryCode("SH", R.drawable.sh_flag))
        flagCountryCode.add(FlagCountryCode("KN", R.drawable.kn_flag))
        flagCountryCode.add(FlagCountryCode("LC", R.drawable.lc_flag))
        flagCountryCode.add(FlagCountryCode("MF", R.drawable.mf_flag))
        flagCountryCode.add(FlagCountryCode("PM", R.drawable.pm_flag))
        flagCountryCode.add(FlagCountryCode("VC", R.drawable.vc_flag))
        flagCountryCode.add(FlagCountryCode("WS", R.drawable.ws_flag))
        flagCountryCode.add(FlagCountryCode("SM", R.drawable.sm_flag))
        flagCountryCode.add(FlagCountryCode("ST", R.drawable.st_flag))
        flagCountryCode.add(FlagCountryCode("SA", R.drawable.sa_flag))
        flagCountryCode.add(FlagCountryCode("SN", R.drawable.sn_flag))
        flagCountryCode.add(FlagCountryCode("RS", R.drawable.rs_flag))
        flagCountryCode.add(FlagCountryCode("SC", R.drawable.sc_flag))
        flagCountryCode.add(FlagCountryCode("SL", R.drawable.sl_flag))
        flagCountryCode.add(FlagCountryCode("SG", R.drawable.sg_flag))
        flagCountryCode.add(FlagCountryCode("SK", R.drawable.sk_flag))
        flagCountryCode.add(FlagCountryCode("SI", R.drawable.si_flag))
        flagCountryCode.add(FlagCountryCode("SB", R.drawable.sb_flag))
        flagCountryCode.add(FlagCountryCode("SO", R.drawable.so_flag))
        flagCountryCode.add(FlagCountryCode("ZA", R.drawable.za_flag))
        flagCountryCode.add(FlagCountryCode("SS", R.drawable.ss_flag))
        flagCountryCode.add(FlagCountryCode("GS", R.drawable.gs_flag))
        flagCountryCode.add(FlagCountryCode("ES", R.drawable.es_flag))
        flagCountryCode.add(FlagCountryCode("LK", R.drawable.lk_flag))
        flagCountryCode.add(FlagCountryCode("SD", R.drawable.sd_flag))
        flagCountryCode.add(FlagCountryCode("SR", R.drawable.sr_flag))
        flagCountryCode.add(FlagCountryCode("SJ", R.drawable.sj_flag))
        flagCountryCode.add(FlagCountryCode("SZ", R.drawable.sz_flag))
        flagCountryCode.add(FlagCountryCode("SE", R.drawable.se_flag))
        flagCountryCode.add(FlagCountryCode("CH", R.drawable.ch_flag))
        flagCountryCode.add(FlagCountryCode("SY", R.drawable.sy_flag))
        flagCountryCode.add(FlagCountryCode("TW", R.drawable.tw_flag))
        flagCountryCode.add(FlagCountryCode("TJ", R.drawable.tj_flag))
        flagCountryCode.add(FlagCountryCode("TZ", R.drawable.tz_flag))
        flagCountryCode.add(FlagCountryCode("TH", R.drawable.th_flag))
        flagCountryCode.add(FlagCountryCode("TL", R.drawable.tl_flag))
        flagCountryCode.add(FlagCountryCode("TG", R.drawable.tg_flag))
        flagCountryCode.add(FlagCountryCode("TK", R.drawable.tk_flag))
        flagCountryCode.add(FlagCountryCode("TO", R.drawable.to_flag))
        flagCountryCode.add(FlagCountryCode("TT", R.drawable.tt_flag))
        flagCountryCode.add(FlagCountryCode("TN", R.drawable.tn_flag))
        flagCountryCode.add(FlagCountryCode("TR", R.drawable.tr_flag))
        flagCountryCode.add(FlagCountryCode("TM", R.drawable.tm_flag))
        flagCountryCode.add(FlagCountryCode("TC", R.drawable.tc_flag))
        flagCountryCode.add(FlagCountryCode("TV", R.drawable.tv_flag))
        flagCountryCode.add(FlagCountryCode("UG", R.drawable.ug_flag))
        flagCountryCode.add(FlagCountryCode("UA", R.drawable.ua_flag))
        flagCountryCode.add(FlagCountryCode("AE", R.drawable.ae_flag))
        flagCountryCode.add(FlagCountryCode("GB", R.drawable.gb_flag))
        flagCountryCode.add(FlagCountryCode("US", R.drawable.us_flag))
        flagCountryCode.add(FlagCountryCode("UY", R.drawable.uy_flag))
        flagCountryCode.add(FlagCountryCode("UZ", R.drawable.uz_flag))
        flagCountryCode.add(FlagCountryCode("VU", R.drawable.vu_flag))
        flagCountryCode.add(FlagCountryCode("VE", R.drawable.ve_flag))
        flagCountryCode.add(FlagCountryCode("VN", R.drawable.vn_flag))
        flagCountryCode.add(FlagCountryCode("VG", R.drawable.vg_flag))
        flagCountryCode.add(FlagCountryCode("VI", R.drawable.vi_flag))
        flagCountryCode.add(FlagCountryCode("WF", R.drawable.wf_flag))
        flagCountryCode.add(FlagCountryCode("YE", R.drawable.ye_flag))
        flagCountryCode.add(FlagCountryCode("ZM", R.drawable.zm_flag))
        flagCountryCode.add(FlagCountryCode("ZW", R.drawable.zw_flag))
        return flagCountryCode
    }

    /**
     * check external storage permission is there to add file in it.
     */
    fun sdCardIsAvailable(context: Context): Boolean {
        return if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            context.getExternalFilesDir(null)?.canWrite() ?: false
        } else false
    }

    /**
     * create folder of application to create a file in it.
     */
    fun getStorageRootPath(context: Context): File {
        return if (sdCardIsAvailable(context)) {
            File(
                context.getExternalFilesDir(null),
                "KeepSafe911"
            )
        } else {
            File(context.filesDir, "KeepSafe911")
        }
    }

    fun setTextGradientColor(tv: TextView) {
        /*val textShader: Shader = LinearGradient(0f, 0f, 0f, tv.textSize, intArrayOf(
            Color.parseColor("#881A46"),
            Color.parseColor("#F48089")
        ), null, Shader.TileMode.CLAMP)

        tv.paint.shader = textShader*/
    }

    fun muteRecognizer(context: Context, enable: Boolean = false){
        try {
            val amanager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if (enable) {
                    amanager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)
                    amanager.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_UNMUTE, 0)
                    amanager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
                    amanager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0)
                    amanager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0)
                } else {
                    amanager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0)
                    amanager.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_MUTE, 0)
                    amanager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
                    amanager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0)
                    amanager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0)
                }
            } else {
                lowerVersionMusicMute(context, enable)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun lowerVersionMusicMute(context: Context, enable: Boolean = false) {
        try {
            lowerAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            lowerAudioManager?.let {
                if (enable) {
                    it.setStreamMute(AudioManager.STREAM_NOTIFICATION, false)
                    it.setStreamMute(AudioManager.STREAM_ALARM, false)
                    it.setStreamMute(AudioManager.STREAM_MUSIC, false)
                    it.setStreamMute(AudioManager.STREAM_RING, false)
                    it.setStreamMute(AudioManager.STREAM_SYSTEM, false)
                } else {
                    it.setStreamMute(AudioManager.STREAM_NOTIFICATION, true)
                    it.setStreamMute(AudioManager.STREAM_ALARM, true)
                    it.setStreamMute(AudioManager.STREAM_MUSIC, true)
                    it.setStreamMute(AudioManager.STREAM_RING, true)
                    it.setStreamMute(AudioManager.STREAM_SYSTEM, true)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun moveToLiveStream(context: Context, isFromNotification: Boolean, channelName: String, liveStreamId: String, role: Int, liveStreamResult: LiveStreamResult = LiveStreamResult()): Intent {
        deleteLiveStreamFile(context)
        val intent = Intent(context, LiveStreamActivity::class.java)
        intent.putExtra(Constants.KEY_CLIENT_ROLE, role)
        intent.putExtra(Constants.KEY_CHANNEL_NAME, channelName)
        intent.putExtra(Constants.KEY_CHANNEL_ID, liveStreamId)
        intent.putExtra("liveStreamData", liveStreamResult)
        intent.putExtra("isFromNotification", isFromNotification)
        return intent
    }

    fun showNoInternetMessage(context: Context) {
        showToastMessage(context.applicationContext, context.resources.getString(R.string.no_internet))
    }

    fun showSomeThingWrongMessage(context: Context) {
        showToastMessage(context.applicationContext, context.resources.getString(R.string.error_message))
    }

    fun familyMonitoringUserList(context: Context, commonApiListener: CommonApiListener, isLoading: Boolean = true) {
        try {
            val appDatabase = OldMe911Database.getDatabase(context)
            val memberID = appDatabase.loginDao().getAll().adminID ?: 0
            Comman_Methods.isProgressShow(context, isLoading)
            val memberListCall = WebApiClient.getInstance(context)
                .webApi_without?.getFamilyMonitoringUsersDetails(memberID)
            memberListCall?.enqueue(object : Callback<GetFamilyMonitoringResponse> {
                override fun onResponse(
                    call: Call<GetFamilyMonitoringResponse>,
                    response: Response<GetFamilyMonitoringResponse>
                ) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide(isLoading)
                            response.body()?.let {
                                commonApiListener.familyUserList(it.isStatus, it.result ?: ArrayList(), it.message ?: "")
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide(isLoading)
                        commonApiListener.onFailureResult()
                    }
                }

                override fun onFailure(call: Call<GetFamilyMonitoringResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide(isLoading)
                    commonApiListener.onFailureResult()
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun categoryList(context: Context, commonApiListener: CommonApiListener) {
        if (ConnectionUtil.isInternetAvailable(context)) {
            Comman_Methods.isProgressShow(context)
            val memberListCall = WebApiClient.getInstance(context)
                .webApi_without?.categoryList()
            memberListCall?.enqueue(object : Callback<CategoryListResponse> {
                override fun onResponse(
                    call: Call<CategoryListResponse>,
                    response: Response<CategoryListResponse>
                ) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                commonApiListener.categoryList(
                                    it.status ?: false,
                                    it.result ?: ArrayList(),
                                    it.message ?: ""
                                )
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        commonApiListener.onFailureResult()
                    }
                }

                override fun onFailure(call: Call<CategoryListResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                    commonApiListener.onFailureResult()
                }
            })
        } else {
            showNoInternetMessage(context)
            commonApiListener.onFailureResult()
        }
    }

    fun missingChildTaskList(context: Context, commonApiListener: CommonApiListener) {
        if (ConnectionUtil.isInternetAvailable(context)) {
            Comman_Methods.isProgressShow(context)
            val memberListCall = WebApiClient.getInstance(context)
                .webApi_without?.getMissingChildTaskList()
            memberListCall?.enqueue(object : Callback<CommonValidationResponse> {
                override fun onResponse(
                    call: Call<CommonValidationResponse>,
                    response: Response<CommonValidationResponse>
                ) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {

                                val gson: Gson = GsonBuilder().create()
                                val responseTypeToken: TypeToken<ArrayList<MissingChildTaskListResult>> =
                                    object :
                                        TypeToken<ArrayList<MissingChildTaskListResult>>() {}
                                val responseList: ArrayList<MissingChildTaskListResult>? =
                                    gson.fromJson(
                                        gson.toJson(it.result),
                                        responseTypeToken.type
                                    )

                                commonApiListener.missingChildTaskListResponse(
                                    it.status ?: false,
                                    responseList ?: ArrayList(),
                                    it.message ?: "",
                                    it.responseMessage ?: ""
                                )
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        commonApiListener.onFailureResult()
                    }
                }

                override fun onFailure(call: Call<CommonValidationResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                    commonApiListener.onFailureResult()
                }
            })
        } else {
            showNoInternetMessage(context)
            commonApiListener.onFailureResult()
        }
    }

    fun addMissingChildTask(context: Context, jsonArray: JsonArray, commonApiListener: CommonApiListener) {
        if (ConnectionUtil.isInternetAvailable(context)) {
            Comman_Methods.isProgressShow(context)
            val memberListCall = WebApiClient.getInstance(context)
                .webApi_without?.addChildEmergencyTaskResponse(jsonArray)
            memberListCall?.enqueue(object : Callback<CommonValidationResponse> {
                override fun onResponse(
                    call: Call<CommonValidationResponse>,
                    response: Response<CommonValidationResponse>
                ) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {

                                val gson: Gson = GsonBuilder().create()
                                val responseTypeToken: TypeToken<ArrayList<MissingChildTaskModel>> =
                                    object :
                                        TypeToken<ArrayList<MissingChildTaskModel>>() {}
                                val responseList: ArrayList<MissingChildTaskModel>? =
                                    gson.fromJson(
                                        gson.toJson(it.result),
                                        responseTypeToken.type
                                    )

                                commonApiListener.childTaskListResponse(
                                    it.status ?: false,
                                    ArrayList(),
                                    responseList ?: ArrayList(),
                                    it.message ?: "",
                                    it.responseMessage ?: ""
                                )
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        commonApiListener.onFailureResult()
                    }
                }

                override fun onFailure(call: Call<CommonValidationResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                    commonApiListener.onFailureResult()
                }
            })
        } else {
            showNoInternetMessage(context)
        }
    }

    fun missingChildByUser(context: Context, userId: Int, missingChildId: Int, commonApiListener: CommonApiListener, isLoader: Boolean = false) {
        if (ConnectionUtil.isInternetAvailable(context)) {
            Comman_Methods.isProgressShow(context, isLoader)
            val memberListCall = WebApiClient.getInstance(context)
                .webApi_without?.getMissingChildByUser(userId)
            memberListCall?.enqueue(object : Callback<CommonValidationResponse> {
                override fun onResponse(
                    call: Call<CommonValidationResponse>,
                    response: Response<CommonValidationResponse>
                ) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide(isLoader)
                            response.body()?.let {

                                val gson: Gson = GsonBuilder().create()
                                val responseTypeToken: TypeToken<ArrayList<MatchResult>> =
                                    object :
                                        TypeToken<ArrayList<MatchResult>>() {}
                                val responseList: ArrayList<MatchResult>? =
                                    gson.fromJson(
                                        gson.toJson(it.result),
                                        responseTypeToken.type
                                    )

                                var filteredMatch: ArrayList<MatchResult> = ArrayList()
                                try {
                                    filteredMatch = responseList?.filter { matchResult -> matchResult.id?.equals(missingChildId) ?: false } as ArrayList<MatchResult>
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                val filteredTaskList: ArrayList<MissingChildTaskModel> = ArrayList()
                                for (k in 0 until filteredMatch.size) {
                                    val filterData = filteredMatch[k]
                                    filteredTaskList.addAll(filterData.lstChildTaskResponse ?: ArrayList())
                                }

                                commonApiListener.childTaskListResponse(
                                    it.status ?: false,
                                    filteredMatch,
                                    filteredTaskList ?: ArrayList(),
                                    it.message ?: "",
                                    it.responseMessage ?: ""
                                )
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide(isLoader)
                        commonApiListener.onFailureResult()
                    }
                }

                override fun onFailure(call: Call<CommonValidationResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide(isLoader)
                    commonApiListener.onFailureResult()
                }
            })
        } else {
            showNoInternetMessage(context)
        }
    }

    fun paymentRelatedApi(context: Context, jsonObject: JsonObject, commonApiListener: CommonApiListener, isVoidPayment: Boolean = false) {
        if (ConnectionUtil.isInternetAvailable(context)) {

            Comman_Methods.isProgressShow(context)
            val paymentApi = WebApiClient.getInstance(context).webApi_payment?.paymentRequest(jsonObject)
            paymentApi?.enqueue(object : Callback<PaymentResponse> {
                override fun onFailure(call: Call<PaymentResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(call: Call<PaymentResponse>, response: Response<PaymentResponse>) {
                    if (response.isSuccessful) {
//                        Comman_Methods.isProgressHide()

                        println("!@@ Void response = $response")
                        response.body()?.let {
                            it.refId
                            if (isVoidPayment) {
                                it.messages?.let { msg ->
                                    if (msg.resultCode == "Ok") {
                                        commonApiListener.paymentTransaction()
                                    } else {
                                        Comman_Methods.isProgressHide()
                                        Comman_Methods.isPaymentShow(context)
//                                        showToastMessage(context, context.resources.getString(R.string.transaction_declined))
                                    }
                                }
                            } else {
                                it.transactionResponse?.let { trans ->
                                    when (trans.responseCode ?: "") {
                                        "1" -> {
                                            commonApiListener.paymentTransaction(trans.transId ?: "")
                                        }
                                        "4" -> {
                                            Comman_Methods.isProgressHide()
                                            Comman_Methods.isPaymentShow(context)
                                            /*for (i in 0 until trans.messages?.size!!) {
                                                val messageRes = trans.messages!![i]
                                                showToastMessage(context, messageRes.text ?: "")
                                            }*/
                                        }
                                        "3" -> {
                                            Comman_Methods.isProgressHide()
                                            Comman_Methods.isPaymentShow(context)
//                                            showToastMessage(context, context.resources.getString(R.string.transaction_declined))
                                        }
                                        else -> {
                                            Comman_Methods.isProgressHide()
                                            Comman_Methods.isPaymentShow(context)
//                                            showToastMessage(context, context.resources.getString(R.string.transaction_declined))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                    }
                }
            })
        } else {
            showNoInternetMessage(context)
        }
    }

    fun callLoginPingLogoutApi(context: Context, loginJson: LoginRequest, commonApiListener: CommonApiListener, isLoader: Boolean = true) {
        if (ConnectionUtil.isInternetAvailable(context)) {
            if (isLoader) {
                Comman_Methods.isProgressShow(context, isLoader)
            } else {
                Comman_Methods.isProgressHide()
            }
            val loginResponseCall = WebApiClient.getInstance(context)
                .webApi_without?.callLoginApi(loginJson)

            loginResponseCall?.enqueue(object : Callback<LoginResponse> {

                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.code() == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide(isLoader)
                            response.body()?.let {
                                commonApiListener.loginResponse(it.status, it.result, it.message ?: "", it.responseMessage ?: "")
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide(isLoader)
                        commonApiListener.onFailureResult()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide(isLoader)
                    commonApiListener.onFailureResult()
                }
            })
        } else {
            showNoInternetMessage(context)
        }
    }

    fun verifyUserData(context: Context, jsonObject: JsonObject, commonApiListener: CommonApiListener) {
        if (ConnectionUtil.isInternetAvailable(context)) {
            Comman_Methods.isProgressShow(context)

            val callVerifyUserData = WebApiClient.getInstance(context).webApi_without?.callVerifyEmail(jsonObject)

            callVerifyUserData?.enqueue(object : Callback<VerifyResponse> {
                override fun onFailure(call: Call<VerifyResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(call: Call<VerifyResponse>, response: Response<VerifyResponse>) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                it.result?.let { verify ->
                                    if (!it.status) {
                                        showToastMessage(context, verify.Message ?: "")
                                    }
                                }
                                commonApiListener.commonResponse(it.status)
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        showSomeThingWrongMessage(context)
                    }
                }
            })
        } else {
            showNoInternetMessage(context)
        }
    }

    fun verifyUserNameData(context: Context, jsonObject: JsonObject, commonApiListener: CommonApiListener) {
        if (ConnectionUtil.isInternetAvailable(context)) {
            Comman_Methods.isProgressShow(context)
            val callCheckUsernameUniqueAPi =
                WebApiClient.getInstance(context).webApi_without?.callCommonValidationApi(
                    jsonObject
                )

            callCheckUsernameUniqueAPi?.enqueue(object : Callback<CommonValidationResponse> {
                override fun onFailure(call: Call<CommonValidationResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(
                    call: Call<CommonValidationResponse>,
                    response: Response<CommonValidationResponse>
                ) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                commonApiListener.commonResultResponse(
                                    it.status ?: false,
                                    it.message ?: "",
                                    it.responseMessage ?: "",
                                    it.result
                                )
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        showSomeThingWrongMessage(context)
                    }
                }
            })
        } else {
            showNoInternetMessage(context)
        }
    }

    fun userSubscriptionCheck(context: Context, commonApiListener: CommonApiListener, isFromPayment: Boolean = false) {
        try {
            if (ConnectionUtil.isInternetAvailable(context)) {
                val appDatabase = OldMe911Database.getDatabase(context)
                val memberId = appDatabase.loginDao().getAll().memberID
                val subResponseCall = WebApiClient.getInstance(context).webApi_without?.callCheckSubscription(memberId.toString())

                subResponseCall?.enqueue(object : Callback<ApiResponse> {
                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        commonApiListener.onFailureResult()
                    }

                    override fun onResponse(
                        call: Call<ApiResponse>,
                        response: Response<ApiResponse>) {
                        if (response.code() == 200) {
                            if (response.isSuccessful) {
                                if (isFromPayment) {
                                    commonApiListener.commonResponse()
                                } else {
                                    response.body()?.let {
                                        commonApiListener.commonResponse(
                                            it.status,
                                            it.message,
                                            it.responseMessage,
                                            it.result
                                        )
                                    }
                                }
                            }
                        } else {
                            if (!isFromPayment) {
                                showSomeThingWrongMessage(context)
                            }
                        }
                    }
                })
            } else {
                showNoInternetMessage(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun userSubscriptionCancel(context: Context, memberId: Int,commonApiListener: CommonApiListener) {
        if (ConnectionUtil.isInternetAvailable(context)) {
            Comman_Methods.isProgressShow(context)
            val subResponseCall = WebApiClient.getInstance(context).webApi_without?.callCancelSubscription(memberId.toString())

            subResponseCall?.enqueue(object : Callback<ApiResponse> {
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                    commonApiListener.onFailureResult()
                }

                override fun onResponse(
                    call: Call<ApiResponse>,
                    response: Response<ApiResponse>) {
                    if (response.code() == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                commonApiListener.commonResponse(
                                    it.status,
                                    it.message,
                                    it.responseMessage,
                                    it.result
                                )
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        showSomeThingWrongMessage(context)
                    }
                }
            })
        } else {
            showNoInternetMessage(context)
        }
    }

    fun userDeviceSubscription(context: Context, memberId: Int,commonApiListener: CommonApiListener, isLoader: Boolean = true) {
        if (ConnectionUtil.isInternetAvailable(context)) {
            Comman_Methods.isProgressShow(context, isLoader)
            val subResponseCall = WebApiClient.getInstance(context).webApi_without?.callCheckDeviceSubscription(memberId)

            subResponseCall?.enqueue(object : Callback<DeviceSubscriptionResponse> {
                override fun onFailure(call: Call<DeviceSubscriptionResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide(isLoader)
                    commonApiListener.onFailureResult()
                }

                override fun onResponse(
                    call: Call<DeviceSubscriptionResponse>,
                    response: Response<DeviceSubscriptionResponse>) {
                    Comman_Methods.isProgressHide(isLoader)
                    if (response.isSuccessful) {
                        response.body()?.let {
                            it.result?.let { result ->
                                commonApiListener.deviceCheckResponse(it.isStatus, it.message ?: "", it.responseMessage ?: "", result)
                            }
                        }
                    }
                }
            })
        } else {
            showNoInternetMessage(context)
        }
    }

    fun postLikeCommentApi(context: Context, jsonObject: JsonObject,commonApiListener: CommonApiListener) {
        if (ConnectionUtil.isInternetAvailable(context)) {
            Comman_Methods.isProgressShow(context)
            val subResponseCall = WebApiClient.getInstance(context).webApi_without?.likeComment(jsonObject)

            subResponseCall?.enqueue(object : Callback<LikeCommentResponse> {
                override fun onFailure(call: Call<LikeCommentResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                    commonApiListener.onFailureResult()
                }

                override fun onResponse(
                    call: Call<LikeCommentResponse>,
                    response: Response<LikeCommentResponse>) {
                    if (response.code() == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                commonApiListener.postLikeComment(
                                    it.status ?: false,
                                    it.result,
                                    it.message ?: "",
                                    it.responseMessage ?: ""
                                )
                            }
                        } else {
                            commonApiListener.postLikeComment(false)
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        showSomeThingWrongMessage(context)
                    }
                }
            })
        } else {
            showNoInternetMessage(context)
        }
    }

    fun yelpTermBasedFilterApi(context: Context, term: String, latitude: Double, longitude: Double, category: String, commonApiListener: CommonApiListener) {
        if (ConnectionUtil.isInternetAvailable(context)) {
            Comman_Methods.isProgressShow(context)
            val callYelpMapFilterApi = WebApiClient.getInstance(context).webApi_yelp?.getTermBasedFilter(term,latitude,longitude,category)
            callYelpMapFilterApi?.enqueue(object : Callback<YelpResponse>{
                override fun onFailure(call: Call<YelpResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(call: Call<YelpResponse>, response: Response<YelpResponse>) {
                    if (response.isSuccessful){
                        Comman_Methods.isProgressHide()
                        response.body()?.let {
                            commonApiListener.yelpDataResponse(
                                it.businesses ?: ArrayList(),
                                it.total ?: 0,
                                it.region ?: Region()
                            )
                        }
                    }
                }
            })
        } else {
            showNoInternetMessage(context)
        }
    }

    fun subscriptionTypeListApi(context: Context, commonApiListener: CommonApiListener, isLoader: Boolean = true) {
        if (ConnectionUtil.isInternetAvailable(context)) {
            Comman_Methods.isProgressShow(context, isLoader)
            val subscriptionTypeApi = WebApiClient.getInstance(context).webApi_without?.getSubscriptionType()
            subscriptionTypeApi?.enqueue(object : Callback<SubscriptionTypeResponse>{
                override fun onFailure(call: Call<SubscriptionTypeResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide(isLoader)
                }

                override fun onResponse(
                    call: Call<SubscriptionTypeResponse>,
                    response: Response<SubscriptionTypeResponse>
                ) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide(isLoader)
                            response.body()?.let {
                                commonApiListener.subscriptionTypeResponse(it.status ?: false,
                                    it.result ?: ArrayList(), it.message ?: "", it.responseMessage ?: "")
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide(isLoader)
                        showSomeThingWrongMessage(context)
                    }
                }
            })
        } else {
            showNoInternetMessage(context)
        }
    }

    fun voiceRecognitionListing(context: Context, commonApiListener: CommonApiListener, isLoader: Boolean = true) {
        try {
            if (ConnectionUtil.isInternetAvailable(context)) {

                Comman_Methods.isProgressShow(context, isLoader)
                val appDatabase = OldMe911Database.getDatabase(context)
                val memberId = appDatabase.loginDao().getAll().memberID
                val subscriptionTypeApi =
                    WebApiClient.getInstance(context).webApi_without?.getVoiceRecognitionList(
                        memberId
                    )
                subscriptionTypeApi?.enqueue(object : Callback<CommonValidationResponse> {
                    override fun onFailure(call: Call<CommonValidationResponse>, t: Throwable) {
                        Comman_Methods.isProgressHide(isLoader)
                    }

                    override fun onResponse(
                        call: Call<CommonValidationResponse>,
                        response: Response<CommonValidationResponse>
                    ) {
                        val statusCode: Int = response.code()
                        if (statusCode == 200) {
                            if (response.isSuccessful) {
                                Comman_Methods.isProgressHide(isLoader)
                                response.body()?.let {
                                    val gson: Gson = GsonBuilder().create()
                                    val responseTypeToken: TypeToken<ArrayList<ManageVoiceRecognitionModel>> =
                                        object :
                                            TypeToken<ArrayList<ManageVoiceRecognitionModel>>() {}
                                    val responseList: ArrayList<ManageVoiceRecognitionModel>? =
                                        gson.fromJson(
                                            gson.toJson(it.result),
                                            responseTypeToken.type
                                        )

                                    val voiceRecognitionList = responseList ?: ArrayList()
                                    appDatabase.loginDao().dropPhrases()
                                    appDatabase.loginDao().addAllPhrases(voiceRecognitionList)

                                    commonApiListener.voiceRecognitionResponse(
                                        it.status ?: false,
                                        voiceRecognitionList,
                                        it.message ?: "",
                                        it.responseMessage ?: ""
                                    )
                                }
                            }
                        } else {
                            Comman_Methods.isProgressHide(isLoader)
                            showSomeThingWrongMessage(context)
                        }
                    }
                })
            } else {
                showNoInternetMessage(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun liveStreamUserListing(context: Context, commonApiListener: CommonApiListener, isLoader: Boolean = false) {
        try {
            if (ConnectionUtil.isInternetAvailable(context)) {

                var liveStreamUserList: ArrayList<LiveStreamResult> = ArrayList()
                Comman_Methods.isProgressShow(context, isLoader)
                val appDatabase = OldMe911Database.getDatabase(context)
                val loginObject = appDatabase.loginDao().getAll()
                val adminId = loginObject.adminID ?: 0
                val subscriptionTypeApi =
                    WebApiClient.getInstance(context).webApi_without?.liveStreamList(adminId)
                subscriptionTypeApi?.enqueue(object : Callback<CommonValidationResponse> {
                    override fun onFailure(call: Call<CommonValidationResponse>, t: Throwable) {
                        Comman_Methods.isProgressHide(isLoader)
                    }

                    override fun onResponse(
                        call: Call<CommonValidationResponse>,
                        response: Response<CommonValidationResponse>
                    ) {
                        val statusCode: Int = response.code()
                        if (statusCode == 200) {
                            if (response.isSuccessful) {
                                Comman_Methods.isProgressHide(isLoader)
                                response.body()?.let {
                                    val gson: Gson = GsonBuilder().create()
                                    val responseTypeToken: TypeToken<ArrayList<LiveStreamResult>> =
                                        object :
                                            TypeToken<ArrayList<LiveStreamResult>>() {}
                                    val responseList: ArrayList<LiveStreamResult>? =
                                        gson.fromJson(
                                            gson.toJson(it.result),
                                            responseTypeToken.type
                                        )

                                    liveStreamUserList = responseList ?: ArrayList()
                                    liveStreamUserList.reverse()
                                    liveStreamUserList = liveStreamUserList.distinctBy { data -> data.memberId ?: 0 } as ArrayList<LiveStreamResult>

                                    commonApiListener.liveStreamListResponse(
                                        it.status ?: false, liveStreamUserList,
                                        it.message ?: "", it.responseMessage ?: ""
                                    )
                                }
                            }
                        } else {
                            Comman_Methods.isProgressHide(isLoader)
                            showSomeThingWrongMessage(context)
                        }
                    }
                })
            } else {
                showNoInternetMessage(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteVisitedPlaces(context: Context, placeId: Int, commonApiListener: CommonApiListener) {
        if (ConnectionUtil.isInternetAvailable(context)) {
            Comman_Methods.isProgressShow(context)
            val callDeletePlace = WebApiClient.getInstance(context).webApi_without?.deleteVisitPlaces(placeId)
            callDeletePlace?.enqueue(object : Callback<ApiResponse>{
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                commonApiListener.commonResponse(it.status, it.message, it.responseMessage, it.result)
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        showSomeThingWrongMessage(context)
                    }
                }
            })
        } else {
            showNoInternetMessage(context)
        }
    }

    fun addVisitPlacesApi(context: Context, jsonObject: JsonObject, commonApiListener: CommonApiListener) {
        if (ConnectionUtil.isInternetAvailable(context)) {
            Comman_Methods.isProgressShow(context)
            val callDeletePlace = WebApiClient.getInstance(context).webApi_without?.addUpdateVisitPlaces(jsonObject)
            callDeletePlace?.enqueue(object : Callback<ApiResponse>{
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                commonApiListener.commonResponse(it.status, it.message, it.responseMessage, it.result)
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        showSomeThingWrongMessage(context)
                    }
                }
            })
        } else {
            showNoInternetMessage(context)
        }
    }

    fun removeUserProfileImageApi(context: Context, memberId: Int, commonApiListener: CommonApiListener, isLoader: Boolean = false) {
        if (ConnectionUtil.isInternetAvailable(context)) {
            Comman_Methods.isProgressShow(context, isLoader)
            val callDeletePlace = WebApiClient.getInstance(context).webApi_without?.removeUserProfilePicture(memberId)
            callDeletePlace?.enqueue(object : Callback<ApiResponse>{
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide(isLoader)
                }

                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            response.body()?.let {
                                commonApiListener.commonResponse(it.status, it.message, it.responseMessage, it.result)
                                showToastMessage(context, it.responseMessage)
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide(isLoader)
                        showSomeThingWrongMessage(context)
                    }
                }
            })
        } else {
            showNoInternetMessage(context)
        }
    }

    fun memberRouteListApi(context: Context, memberId: Int, date: String, commonApiListener: CommonApiListener) {
        if (ConnectionUtil.isInternetAvailable(context)) {
            Comman_Methods.isProgressShow(context)
            val callDownloadReport = WebApiClient.getInstance(context).webApi_without?.getRouteDirection(memberId, date)
            callDownloadReport?.enqueue(object : Callback<MemberRouteResponse> {
                override fun onFailure(call: Call<MemberRouteResponse>?, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(call: Call<MemberRouteResponse>, response: Response<MemberRouteResponse>) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                if (it.status == true) {
                                    commonApiListener.memberRouteResponse(it.status ?: false,
                                        it.result ?: ArrayList(), it.message ?: "",
                                        it.responseMessage ?: "")
                                } else {
                                    showToastMessage(context,it.message ?: "")
                                }
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        showSomeThingWrongMessage(context)
                    }
                }
            })
        } else {
            showNoInternetMessage(context)
        }
    }

    fun deleteUserApi(context: Context, memberId: Int, commonApiListener: CommonApiListener) {
        if (ConnectionUtil.isInternetAvailable(context)) {
            Comman_Methods.isProgressShow(context)
            val callDownloadReport = WebApiClient.getInstance(context).webApi_without?.deleteUser(memberId)
            callDownloadReport?.enqueue(object : Callback<ApiResponse> {
                override fun onFailure(call: Call<ApiResponse>?, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                commonApiListener.commonResponse(it.status,
                                    it.message,
                                    it.responseMessage)
                                showToastMessage(context, it.message)
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        showSomeThingWrongMessage(context)
                    }
                }
            })
        } else {
            showNoInternetMessage(context)
        }
    }

    fun deleteLiveStreamApi(context: Context, id: Int, commonApiListener: CommonApiListener, isLoading: Boolean = true) {
        if (ConnectionUtil.isInternetAvailable(context)) {
            Comman_Methods.isProgressShow(context, isLoading)
            val callDownloadReport = WebApiClient.getInstance(context).webApi_without?.deleteLiveStream(id)
            callDownloadReport?.enqueue(object : Callback<ApiResponse> {
                override fun onFailure(call: Call<ApiResponse>?, t: Throwable) {
                    Comman_Methods.isProgressHide(isLoading)
                }

                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide(isLoading)
                            response.body()?.let {
                                commonApiListener.commonResponse(
                                    it.status,
                                    it.message,
                                    it.responseMessage,
                                    it.result)
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide(isLoading)
                        showSomeThingWrongMessage(context)
                    }
                }
            })
        } else {
            showNoInternetMessage(context)
        }
    }

    fun combineVideos(paths: ArrayList<String>, width: Int?, height: Int?, output: String): Array<String> {
        val inputs: ArrayList<String> = ArrayList()
        var query: String? = ""
        var queryAudio: String? = ""
        for (i in 0 until paths.size) {
            //for input
            inputs.add("-i")
            inputs.add(paths[i])

            //for video setting with width and height
            query = query?.trim()
            query += "[" + i + ":v]scale=${width}x${height},setdar=$width/$height[" + i + "v];"

            //for video and audio combine {without audio this query not supported so applied this function}
            queryAudio = queryAudio?.trim()
            queryAudio += "[" + i + "v][" + i + ":a]"
        }
        return getResult(inputs, query, queryAudio, paths, output)
    }

    private fun getResult(inputs: ArrayList<String>, query: String?, queryAudio: String?, paths: ArrayList<String>, output: String): Array<String> {
        inputs.apply {
            add("-f")
            add("lavfi")
            add("-t")
            add("0.1")
            add("-i")
            add("anullsrc")
            add("-filter_complex")
            add(query + queryAudio + "concat=n=" + paths.size + ":v=1:a=1 [v][a]")
            add("-map")
            add("[v]")
            add("-map")
            add("[a]")
            add("-preset")
            add("ultrafast")
            add(output)
        }
        return inputs.toArray(arrayOfNulls<String>(inputs.size))
    }

    private var toast: Toast? = null

    fun showToastMessage(context: Context?, message: String) {
        try {
            toast?.cancel()
            if (context != null) {
                if (message.trim().isNotEmpty()) {
                    toast = Toast(context)
                    val inflater: LayoutInflater? =
                        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
                    val view: View? = inflater?.inflate(R.layout.toast, null)
                    val marginData = if (context.resources.getBoolean(R.bool.isTablet))
                        Comman_Methods.convertDpToPixels(210f, context)
                    else Comman_Methods.convertDpToPixels(10f, context)

                    val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    params.setMargins(marginData.roundToInt(), 0, marginData.roundToInt(), 0)

                    val tv: TextView? = view?.findViewById(R.id.txtToast)
                    tv?.text = message
                    tv?.layoutParams = params
                    toast?.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 150)
                    toast?.duration = Toast.LENGTH_SHORT
                    toast?.view = view
                    toast?.show()
//                val handler = Handler(Looper.getMainLooper())
//                handler.postDelayed({ toast?.cancel() }, 1200)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}