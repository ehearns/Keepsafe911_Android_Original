package com.keepSafe911.offlineservice

import android.content.*
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.BatteryManager
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import com.keepSafe911.model.request.LoginRequest
import com.keepSafe911.model.response.ApiResponse
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.room.databasetable.GeoFenceNotification
import com.keepSafe911.utils.Comman_Methods
import com.keepSafe911.utils.ConnectionUtil
import com.keepSafe911.webservices.WebApiClient
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Response
import java.util.*
import com.keepSafe911.R
import com.keepSafe911.model.response.CommonValidationResponse
import com.keepSafe911.utils.Utils


class CheckInternetAvaliability : BroadcastReceiver() {
    private lateinit var dbhelper: OldMe911Database
    private lateinit var con: Context
    private var user_table_list: ArrayList<LoginRequest> = ArrayList()
    private var user_notify_list: ArrayList<GeoFenceNotification> = ArrayList()
    private var isConnected = false
    private var battery_level = 0


    private val mBatInfoReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            battery_level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        }
    }


    override fun onReceive(context: Context, intent: Intent) {
        Log.v(LOG_TAG, "Receieved notification about network status")
        con = context
        isNetworkAvailable(context)
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (connectivity != null) {
            val info = connectivity.allNetworkInfo

            if (info != null) {
                for (i in info.indices) {
                    if (info[i].state == NetworkInfo.State.CONNECTED) {
                        if (!isConnected) {
                            Log.e(LOG_TAG, "Now you are connected to Internet!")
                            isConnected = true
                            //do your processing here ---
                            //if you need to post any data to the server or get status
                            //update from the server
                            try {
                                StartSendingDataToServer()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                        }
                        if (!isConnectionFast(info[i].type,info[i].subtype)){
                            Utils.showToastMessage(context, "Bad Network Connection")
                        }
                        return true
                    }
                }
            }
        }

        Log.v(LOG_TAG, "You are not connected to Internet!")

        isConnected = false
        return false
    }

    private fun isConnectionFast(type: Int, subType: Int): Boolean {
        return when (type) {
            ConnectivityManager.TYPE_WIFI -> true
            ConnectivityManager.TYPE_MOBILE -> when (subType) {
                TelephonyManager.NETWORK_TYPE_1xRTT -> false // ~ 50-100 kbps
                TelephonyManager.NETWORK_TYPE_CDMA -> false // ~ 14-64 kbps
                TelephonyManager.NETWORK_TYPE_EDGE -> false // ~ 50-100 kbps
                TelephonyManager.NETWORK_TYPE_EVDO_0 -> true // ~ 400-1000 kbps
                TelephonyManager.NETWORK_TYPE_EVDO_A -> true // ~ 600-1400 kbps
                TelephonyManager.NETWORK_TYPE_GPRS -> false // ~ 100 kbps
                TelephonyManager.NETWORK_TYPE_HSDPA -> true // ~ 2-14 Mbps
                TelephonyManager.NETWORK_TYPE_HSPA -> true // ~ 700-1700 kbps
                TelephonyManager.NETWORK_TYPE_HSUPA -> true // ~ 1-23 Mbps
                TelephonyManager.NETWORK_TYPE_UMTS -> true // ~ 400-7000 kbps
                /*
                  * Above API level 7, make sure to set android:targetSdkVersion
                  * to appropriate level to use these
                  */
                TelephonyManager.NETWORK_TYPE_EHRPD // API level 11
                -> true // ~ 1-2 Mbps
                TelephonyManager.NETWORK_TYPE_EVDO_B // API level 9
                -> true // ~ 5 Mbps
                TelephonyManager.NETWORK_TYPE_HSPAP // API level 13
                -> true // ~ 10-20 Mbps
                TelephonyManager.NETWORK_TYPE_IDEN // API level 8
                -> false // ~25 kbps
                TelephonyManager.NETWORK_TYPE_LTE // API level 11
                -> true // ~ 10+ Mbps
                // Unknown
                TelephonyManager.NETWORK_TYPE_UNKNOWN -> false
                else -> false
            }
            else -> false
        }
    }

    fun StartSendingDataToServer() {
        if (isConnected) {
            Log.e("Connected", "Connected")
            dbhelper = OldMe911Database.getDatabase(con)
            user_table_list = dbhelper.loginRequestDao().getAllLoginRequestDetail() as ArrayList<LoginRequest>
            var bean = LoginRequest()

            if (user_table_list.size > 0) {
                val jsonArray = JsonArray()

                for (j in 0 until user_table_list.size) {
                    bean = user_table_list[j]
                    Log.e("example---", "" + bean.toString())

                    /*val address = Utils.encodeString(
                        Utils.getCompleteAddressString(
                            con,
                            bean.latitude?.toDouble() ?: 0.0, bean.longitude?.toDouble() ?: 0.0
                        )
                    )*/

                    try {
                        val jsonObj = JsonObject()

                        jsonObj.addProperty("DeviceModel", Comman_Methods.getdeviceModel())
                        jsonObj.addProperty("UserName", bean.userName)
                        jsonObj.addProperty("Password", bean.password)
                        jsonObj.addProperty("RecordStatus", bean.recordStatus)
                        jsonObj.addProperty("groupid", "")
                        jsonObj.addProperty("Longitude", bean.longitude)
                        jsonObj.addProperty("Latitude", bean.latitude)
                        jsonObj.addProperty("MemberID", bean.id)
                        jsonObj.addProperty("BatteryLevel", battery_level)
                        jsonObj.addProperty("DeviceCompanyName", Comman_Methods.getdevicename())
                        jsonObj.addProperty("id", bean.id)
                        jsonObj.addProperty("StartDate", bean.startDate)
                        jsonObj.addProperty("UUID", bean.uuid)
                        jsonObj.addProperty("devicetypeid", bean.devicetypeid)
                        jsonObj.addProperty("DeviceToken", bean.deviceToken)
                        jsonObj.addProperty("DeviceOS", Comman_Methods.getdeviceVersion())
                        jsonObj.addProperty("DeviceModelNo", Comman_Methods.getdeviceModel())
                        jsonObj.addProperty("LocationAddress", bean.locationAddress)
                        jsonObj.addProperty("DeviceType", "Android")
                        jsonObj.addProperty("LoginByApp", 2)

                        jsonArray.add(jsonObj)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                callOfflineApi(jsonArray)
            } else {
                Log.e("Warning :", "No user table records available")
                sendNotificationOfflineData()
            }
        }
    }

    fun sendNotificationOfflineData() {
        if (isConnected) {
            dbhelper = OldMe911Database.getDatabase(con)
            user_notify_list = dbhelper.geoFenceDao().getAllGeoNotify() as ArrayList<GeoFenceNotification>
            if (user_notify_list.size > 0) {
                val jsonArray = JsonArray()
                for (k in 0 until user_notify_list.size) {
                    val bean = user_notify_list[k]
                    try {
                        val jsonObj = JsonObject()
                        jsonObj.addProperty("MemberID", bean.notifyMemberID)
                        jsonObj.addProperty("GeoFenceID", bean.geoNotifyID)
                        jsonObj.addProperty("Status", bean.notifyStatus)
                        jsonObj.addProperty("GeoFenceTime", bean.notifyTime)
                        jsonObj.addProperty("NotificationMessage", bean.notifyMessage)
                        jsonObj.addProperty("Latitude", bean.notifyLat)
                        jsonObj.addProperty("Longitude", bean.notifyLong)
                        jsonObj.addProperty("CreatedOn", bean.notifyCreateOn)
                        jsonObj.addProperty("LoginByApp", 2)

                        jsonArray.add(jsonObj)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                callOfflineNotificationApi(jsonArray)
            } else {
                Log.e("Warning :", "No notify table records available")
            }
        }
    }

    private fun callOfflineNotificationApi(jsonArray: JsonArray) {
        if (ConnectionUtil.isInternetAvailable(con)) {
            Comman_Methods.isProgressShow(con)
            val callOfflineNotification =
                WebApiClient.getInstance(con).webApi_without?.notificationOfflineData(jsonArray)
            callOfflineNotification?.enqueue(object : retrofit2.Callback<ApiResponse> {
                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                }

                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            if (response.body()?.status == true) {
                                Utils.showToastMessage(con, con.getString(R.string.offline_geofence_data))

                                /*try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        val isServiceStarted =
                                            Utils.isJobServiceRunning(con)
                                        if (!isServiceStarted) {
                                            val jobScheduler =
                                                con.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                                            val componentName = ComponentName(
                                                con.packageName,
                                                GpsJobService::class.java.name
                                            )
                                            val jobInfo =
                                                JobInfo.Builder(GPSSERVICEJOBID, componentName)
                                                    .setMinimumLatency(1000)
                                                    .setOverrideDeadline((241 * 60000).toLong())
                                                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                                                    .setPersisted(true).build()
                                            val resultCode = jobScheduler.schedule(jobInfo)
                                            if (resultCode == JobScheduler.RESULT_SUCCESS) {
                                                Log.d(
                                                    GpsJobService::class.java.name,
                                                    "job scheduled"
                                                )
                                            } else {
                                                Log.d(
                                                    GpsJobService::class.java.name,
                                                    "job schedule failed"
                                                )
                                            }
                                        }
                                    } else {
                                        val isServiceStarted =
                                            Utils.isMyServiceRunning(GpsService::class.java, con)
                                        if (!isServiceStarted) {
                                            con.startService(Intent(con, GpsService::class.java))
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }*/
                                dbhelper.geoFenceDao().dropGeoNotify()
                            } else {
                                Utils.showSomeThingWrongMessage(con)
                            }
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        Utils.showSomeThingWrongMessage(con)
                    }
                }

            })
        }
    }

    /**
     * APi Method for - > UserOfflineData
     */
    private fun callOfflineApi(jsonMainObject: JsonArray) {
        if (ConnectionUtil.isInternetAvailable(con)) {
            Comman_Methods.isProgressShow(con)
            val callOfflinePing = WebApiClient.getInstance(con).webApi_without?.userOfflineData(jsonMainObject)
            callOfflinePing?.enqueue(object : retrofit2.Callback<CommonValidationResponse> {
                override fun onFailure(call: Call<CommonValidationResponse>, t: Throwable) {
                    Comman_Methods.isProgressHide()
                    sendNotificationOfflineData()
                }

                override fun onResponse(call: Call<CommonValidationResponse>, response: Response<CommonValidationResponse>) {
                    val statusCode: Int = response.code()
                    if (statusCode == 200) {
                        if (response.isSuccessful) {
                            Comman_Methods.isProgressHide()
                            response.body()?.let {
                                if (it.status == true) {
                                    Utils.showToastMessage(con, con.getString(R.string.offline_location_data))
                                    /*try {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                            val isServiceStarted =
                                                Utils.isJobServiceRunning(con)
                                            if (!isServiceStarted) {
                                                val jobScheduler =
                                                    con.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                                                val componentName = ComponentName(
                                                    con.packageName,
                                                    GpsJobService::class.java.name
                                                )
                                                val jobInfo =
                                                    JobInfo.Builder(GPSSERVICEJOBID, componentName)
                                                        .setMinimumLatency(1000)
                                                        .setOverrideDeadline((241 * 60000).toLong())
                                                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                                                        .setPersisted(true).build()
                                                val resultCode = jobScheduler.schedule(jobInfo)
                                                if (resultCode == JobScheduler.RESULT_SUCCESS) {
                                                    Log.d(
                                                        GpsJobService::class.java.name,
                                                        "job scheduled"
                                                    )
                                                } else {
                                                    Log.d(
                                                        GpsJobService::class.java.name,
                                                        "job schedule failed"
                                                    )
                                                }
                                            }
                                        } else {
                                            val isServiceStarted =
                                                Utils.isMyServiceRunning(GpsService::class.java, con)
                                            if (!isServiceStarted) {
                                                con.startService(Intent(con, GpsService::class.java))
                                            }
                                        }
                                    } catch (e: java.lang.Exception) {
                                        e.printStackTrace()
                                    }*/
                                    dbhelper.loginRequestDao().dropTable()
                                } else {
                                    Utils.showSomeThingWrongMessage(con)
                                }
                            }
                            sendNotificationOfflineData()
                        }
                    } else {
                        Comman_Methods.isProgressHide()
                        sendNotificationOfflineData()
                    }
                }
            })
        }
    }

    companion object {
        private val LOG_TAG = "CheckNetworkStatus"
    }
}