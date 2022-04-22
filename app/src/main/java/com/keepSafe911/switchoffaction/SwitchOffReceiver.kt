package com.keepSafe911.switchoffaction

import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.keepSafe911.BuildConfig
import com.keepSafe911.gps.GpsService
import com.keepSafe911.gps.GpsTracker
import com.keepSafe911.listner.CommonApiListener
import com.keepSafe911.model.request.LoginRequest
import com.keepSafe911.model.roomobj.LoginObject
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.*
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class SwitchOffReceiver : BroadcastReceiver() {
    lateinit var context: Context
    override fun onReceive(context: Context?, intent: Intent?) {
        this.context = context!!
//        AsyncTracking(context).execute()
        callLogOutApi()
    }

    private fun callLogOutApi() {
        val androidId: String = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val appDatabase = OldMe911Database.getDatabase(context)
        val loginParameter: LoginObject = appDatabase.loginDao().getAll()
        if (loginParameter != null) {

            val login_json = LoginRequest()

            val gpsTracker = GpsTracker(context)
            val gpsLatitude = gpsTracker.getLatitude()
            val gpsLongitude = gpsTracker.getLongitude()

            val decimalSymbols = DecimalFormatSymbols.getInstance(Locale.US)
            val oldLatitudeString: String = if (loginParameter.latitude!=null) DecimalFormat("#.####", decimalSymbols).format(loginParameter.latitude) else "0.0"
            val oldLongitudeString: String = if (loginParameter.longitude!=null) DecimalFormat("#.####", decimalSymbols).format(loginParameter.longitude) else "0.0"

            val oldLatitude: Double = if (loginParameter.latitude!=null) loginParameter.latitude else 0.0
            val oldLongitude: Double = if (loginParameter.longitude!=null) loginParameter.longitude else 0.0

            val newLatitude = DecimalFormat("#.####", decimalSymbols).format(gpsLatitude)
            val newLongitude = DecimalFormat("#.####", decimalSymbols).format(gpsLongitude)

            val distance = FloatArray(2)

            Location.distanceBetween(
                oldLatitude, oldLongitude,
                gpsLatitude, gpsLongitude, distance
            )

            val distanceInKilometer = distance[0]/1000

            println("!@@@@@distance = ${distance[0]}")
            println("!@@@@@distanceInKilometer = ${distanceInKilometer}")

            val address = if (distance[0] > 50f){
                ""
            }else{
                loginParameter.locationAddress
            }
            /*val address = if (oldLatitudeString!=newLatitude && oldLongitudeString!=newLongitude)
                ""
            else loginParamter.locationAddress*/

            login_json.batteryLevel = Utils.GetBatterylevel(context).toString()
            login_json.deviceCompanyName = Comman_Methods.getdevicename()
            login_json.deviceModel = Comman_Methods.getdeviceModel()
            login_json.deviceOS = Comman_Methods.getdeviceVersion()
            login_json.uuid = androidId
            login_json.email = loginParameter.email
            login_json.latitude = DecimalFormat("#.######", decimalSymbols).format(gpsLatitude)
            login_json.longitude = DecimalFormat("#.######", decimalSymbols).format(gpsLongitude)
            login_json.recordStatus = "3"
            login_json.locationPermission = "true"
            login_json.locationAddress = address
            login_json.mobile = ""
            login_json.createdby = loginParameter.memberID.toString()
            login_json.userName = loginParameter.userName
            login_json.id = loginParameter.memberID
            login_json.firstName = loginParameter.firstName
            login_json.lastName = loginParameter.lastName
            login_json.profilePath = loginParameter.profilePath
            login_json.devicetypeid = "1"
            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                login_json.notificationPermission = "true"
            } else {
                login_json.notificationPermission = "false"
            }

            login_json.deviceTokenId = AppPreference.getStringPreference(context, BuildConfig.firebasePrefKey)
            login_json.deviceType = "Android"
            login_json.startDate = Comman_Methods.getcurrentDate()
            login_json.password = loginParameter.password
            login_json.frequency = loginParameter.frequency
            login_json.loginByApp = loginParameter.loginByApp ?: 2

            Utils.callLoginPingLogoutApi(context, login_json, object : CommonApiListener {
                override fun loginResponse(
                    status: Boolean,
                    loginData: LoginObject?,
                    message: String,
                    responseMessage: String
                ) {
                    if (status) {
                        val isServiceStarted = Utils.isMyServiceRunning(GpsService::class.java, context)
                        if (isServiceStarted) {
                            context.stopService(Intent(context, GpsService::class.java))
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                            jobScheduler.cancelAll()
                        }
                        /*val reciever = CheckInternetAvaliability()
                        try {
                            app_context.unregisterReceiver(reciever)
                            app_context.unregisterReceiver(this@SwitchOffReceiver)
                        }catch (e:Exception){
                            e.printStackTrace()
                        }*/
                        //delete all tables from database
                        appDatabase.loginDao().dropLogin()
                        appDatabase.loginRequestDao().dropTable()
                        appDatabase.geoFenceDao().dropGeoFence()
                        appDatabase.memberDao().dropTable()
                        appDatabase.geoFenceDao().dropGeoNotify()

                        val file = File(context.cacheDir.toString() + CACHE_FOLDER_NAME)
                        if (file.exists()) {
                            Comman_Methods.deleteDir(file)
                        }
                    }
                }
            }, false)
        }
    }
}