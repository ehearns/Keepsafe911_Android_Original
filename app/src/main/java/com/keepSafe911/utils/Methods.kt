import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.graphics.Insets
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.gson.JsonObject
import com.keepSafe911.R
import com.keepSafe911.listner.PositiveButtonListener
import com.keepSafe911.model.response.ApiResponse
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.room.databasetable.GeoFenceNotification
import com.keepSafe911.utils.Utils
import com.keepSafe911.webservices.WebApiClient
import retrofit2.Call
import retrofit2.Response
import java.io.File


private var maxWidth = 0
private var maxHeight = 0


fun AppCompatActivity.hideKeyboard() {

    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    val view = currentFocus
    if (view != null) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}

fun AppCompatActivity.showKeyboard() {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
}

fun timeConversionWithMinutes(secondsDetail: Long): String {
    var seconds = secondsDetail / 1000
    var min = ""
    var sec = ""
    val MINUTES_IN_AN_HOUR = 60
    val SECONDS_IN_A_MINUTE = 60
    var minutes = seconds / SECONDS_IN_A_MINUTE
    seconds -= minutes * SECONDS_IN_A_MINUTE
    val hours = minutes / MINUTES_IN_AN_HOUR
    minutes -= hours * MINUTES_IN_AN_HOUR
    min = if (minutes < 10) {
        "0$minutes"
    } else {
        minutes.toString()
    }
    sec = if (seconds < 10) {
        "0$seconds"
    } else {
        seconds.toString()
    }
    return "$min:$sec"
}

fun timeConversionWithHours(secondsDetail: Long): String {
    var seconds = secondsDetail / 1000
    var hr = ""
    var min = ""
    var sec = ""
    val MINUTES_IN_AN_HOUR = 60
    val SECONDS_IN_A_MINUTE = 60
    var minutes = seconds / SECONDS_IN_A_MINUTE
    seconds -= minutes * SECONDS_IN_A_MINUTE
    val hours = minutes / MINUTES_IN_AN_HOUR
    minutes -= hours * MINUTES_IN_AN_HOUR
    hr = if (hours < 10) {
        "0$hours"
    } else {
        hours.toString()
    }
    min = if (minutes < 10) {
        "0$minutes"
    } else {
        minutes.toString()
    }
    sec = if (seconds < 10) {
        "0$seconds"
    } else {
        seconds.toString()
    }
    return "$hr:$min:$sec"
}

fun sendMail(context: Context, subject: String, extraText: String, emailId: String) {
    val intent = Intent(Intent.ACTION_SENDTO)
    intent.data = Uri.parse("mailto:") // only email apps should handle this
    intent.putExtra(Intent.EXTRA_EMAIL, Array<String>(20) { emailId })
    intent.putExtra(Intent.EXTRA_SUBJECT, subject)
    intent.putExtra(Intent.EXTRA_TEXT, extraText)

    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}

fun shareTo(context: Context) {
    val shareIntent = Intent()
    shareIntent.action = Intent.ACTION_SEND
    shareIntent.type = "text/plain"
    shareIntent.putExtra(Intent.EXTRA_TEXT, "This is my text to send.")

    if (shareIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(Intent.createChooser(shareIntent, "Share to"))
    }
}

fun getLongFromString(str: String): Long {
    if (!TextUtils.isEmpty(str)) {
        try {
            return java.lang.Long.parseLong(str)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
    return -1
}

fun avoidDoubleClicks(view: View) {
    val DELAY_IN_MS: Long = 900
    if (!view.isClickable) {
        return
    }
    view.isClickable = false
    view.postDelayed({ view.isClickable = true }, DELAY_IN_MS)
}

fun AppCompatActivity.getScreenWidth(isCutting: Boolean = true): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val tableMinus = if (isCutting) if (resources.getBoolean(R.bool.isTablet)) 65 else 10 else 0
        val windowMetrics = windowManager.currentWindowMetrics
        val insets: Insets = windowMetrics.windowInsets.getInsets(WindowInsets.Type.systemBars())
        windowMetrics.bounds.width() - (insets.left + insets.right + tableMinus)
    } else {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        displayMetrics.widthPixels
    }
}

fun AppCompatActivity.getScreenHeight(isCutting: Boolean = true): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val tableMinus = if (isCutting) if (resources.getBoolean(R.bool.isTablet)) 10 else 65 else 0
        val metrics = windowManager.currentWindowMetrics
        val insets = metrics.windowInsets.getInsets(WindowInsets.Type.systemBars())
        metrics.bounds.height() - (insets.bottom + insets.top + tableMinus)
    } else {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        displayMetrics.heightPixels
    }
}

fun deleteLiveStreamFile(context: Context) {
    try {
        val root = Utils.getStorageRootPath(context)
        if (!root.exists()) {
            root.mkdir()
        }
        val subFolder = File(root, "/LiveStream/")
        if (subFolder.exists()) {
            try {
                if (subFolder.isDirectory) {
                    val children = subFolder.list()
                    for (i in children.indices) {
                        val childFile = File(subFolder, children[i])
                        childFile.delete()
                        if (childFile.exists()) {
                            childFile.canonicalFile.delete()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            subFolder.delete()
            if (subFolder.exists()) {
                subFolder.canonicalFile.delete()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun getMaxHeight(context: Context): Int {
    if (maxHeight != 0) {
        return maxHeight
    }

    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val display = wm.defaultDisplay
    val metrics = DisplayMetrics()
    display.getMetrics(metrics)
    val height = metrics.heightPixels
    maxHeight = height
    return maxHeight
}

fun showWebPage(context: Context, webUrlData: String) {
    var webUrl = webUrlData
    if (!webUrlData.startsWith("http://") && !webUrlData.startsWith("https://")) {
        webUrl = "http://$webUrlData"
    }
    val webpage = Uri.parse(webUrl)
    val intent = Intent(Intent.ACTION_VIEW, webpage)
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}

fun appInstalledOrNot(uri: String, context: Context): Boolean {
    val pm = context.packageManager
    return try {
        pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    } catch (e: java.lang.Exception){
        false
    }
}

fun animationView(view: View) {
    val animatorSet = AnimatorSet()

    val bounceAnimX = ObjectAnimator.ofFloat(view, "scaleX", 1.5f, 1f)

    val bounceAnimY = ObjectAnimator.ofFloat(view, "scaleY", 1.5f, 1f)

    animatorSet.play(bounceAnimX).with(bounceAnimY)
    animatorSet.duration = 300
    animatorSet.interpolator = AccelerateInterpolator()
    animatorSet.start()
}

fun animateImage(imageView: ImageView, animation: Animation): Boolean {

    return if (imageView.isSelected) {
        imageView.isSelected = false
        false
    } else {
        imageView.startAnimation(animation)
        imageView.isSelected = true
        true
    }
}

fun isAppIsInBackground(context: Context): Boolean {
    var isInBackground = true
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
        val runningProcesses = am.runningAppProcesses
        for (processInfo in runningProcesses) {
            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                for (activeProcess in processInfo.pkgList) {
                    if (activeProcess == context.packageName) {
                        isInBackground = false
                    }
                }
            }
        }
    } else {
        val taskInfo = am.getRunningTasks(1)
        val componentInfo = taskInfo[0].topActivity
        if (componentInfo?.packageName == context.packageName) {
            isInBackground = false
        }
    }
    return isInBackground
}

fun callSendNotificationApi(
    status: String,
    status_bool: Boolean,
    adminID: Int,
    iD: Int,
    lat: Float,
    lng: Float,
    context: Context
) {
    val jsonObject = JsonObject()
    jsonObject.addProperty("MemberID", adminID)
    jsonObject.addProperty("GeoFenceID", iD)
    jsonObject.addProperty("Status", status_bool)
    jsonObject.addProperty("GeoFenceTime", Utils.getTimeStamp())
    jsonObject.addProperty("NotificationMessage", status)
    jsonObject.addProperty("Latitude", lat)
    jsonObject.addProperty("Longitude", lng)
    jsonObject.addProperty("CreatedOn", Utils.getCurrentTimeStampWithMilliSeconds())
    jsonObject.addProperty("LoginByApp", 2)
    val sendNotificationCall =
        WebApiClient.getInstance(context).webApi_without?.sendNotificationOfGeoFence(jsonObject)
    sendNotificationCall?.enqueue(object : retrofit2.Callback<ApiResponse> {
        override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
        }

        override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {

        }

    })
}

fun storeGeofenceData(
    status: String,
    status_bool: Boolean,
    adminID: Int,
    iD: Int,
    lat: Float,
    lng: Float,
    context: Context
) {
    val appDataBase = OldMe911Database.getDatabase(context)
    val geofenceNotify = GeoFenceNotification()
    geofenceNotify.geoNotifyID = iD
    geofenceNotify.notifyMemberID = adminID
    geofenceNotify.notifyMessage = status
    geofenceNotify.notifyStatus = status_bool
    geofenceNotify.notifyTime = Utils.getTimeStamp()
    geofenceNotify.notifyCreateOn = Utils.getCurrentTimeStampWithMilliSeconds()
    geofenceNotify.notifyLat = lat.toDouble()
    geofenceNotify.notifyLong = lng.toDouble()
    appDataBase.geoFenceDao().addGeoNotify(geofenceNotify)
}

@RequiresApi(Build.VERSION_CODES.M)
fun checkLocationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return when {
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && context.checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && context.checkSelfPermission(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED -> {
                false
            }
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && context.checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED -> {
                false
            }
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && context.checkSelfPermission(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED -> {
                false
            }
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && context.checkSelfPermission(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED -> {
                false
            }
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED -> {
                false
            }
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED -> {
                false
            }
            context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED -> {
                false
            }
            else -> {
                true
            }
        }
    } else {
        return when {
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && context.checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED -> {
                false
            }
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED -> {
                false
            }
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED -> {
                false
            }
            else -> {
                true
            }
        }
    }
}

fun AppCompatActivity.enableGpsData() {
    val locationRequest = LocationRequest.create()
    locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    val builder = LocationSettingsRequest.Builder()
        .addLocationRequest(locationRequest)
    builder.setAlwaysShow(true) // this is the key ingredient
    val result =
        LocationServices.getSettingsClient(this).checkLocationSettings(builder.build())
    result.addOnSuccessListener { locationSettingsResponse ->

    }

    result.addOnFailureListener { e ->
        if (e is ResolvableApiException) {
            // Location settings are not satisfied, but this can be fixed
            // by showing the user a dialog.
            try {
                // Show the dialog by calling startResolutionForResult(),
                // and check the result in onActivityResult().
                val resolvable = e
                resolvable.startResolutionForResult(
                    this,
                    9999
                )
            } catch (sendEx: SendIntentException) {
                sendEx.printStackTrace()
            }
        }
    }
}
fun AppCompatActivity.takeCall(mobileNumber: String) {
    if (mobileNumber.isNotEmpty()) {
        val callIntent = Intent(Intent.ACTION_DIAL)
        callIntent.data = Uri.parse("tel:$mobileNumber")
        startActivity(callIntent)
    }
}
fun AppCompatActivity.sendSMS(mobileNumber: String) {
    if (mobileNumber.isNotEmpty()) {
        val smsUri: Uri = Uri.parse("smsto:$mobileNumber")
        val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri)
        smsIntent.putExtra("sms_body", "")
        startActivity(smsIntent)
    }
}
fun AppCompatActivity.visitUrl(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}