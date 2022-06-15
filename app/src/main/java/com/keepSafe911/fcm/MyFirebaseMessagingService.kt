/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.keepSafe911.fcm

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.keepSafe911.room.OldMe911Database
import com.keepSafe911.utils.Utils
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.keepSafe911.BuildConfig
import com.keepSafe911.HomeActivity
import com.keepSafe911.MainActivity
import com.keepSafe911.R
import com.keepSafe911.model.response.LiveStreamResult
import com.keepSafe911.utils.AppPreference
import io.agora.rtc.Constants
import java.util.*


class MyFirebaseMessagingService : FirebaseMessagingService() {
    internal var context: Context? = null
    private var broadcaster: LocalBroadcastManager? = null
    private var channelName: String? = ""
    private var liveStreamId: String? = ""
    private var missingChildId: String? = ""

    override fun onCreate() {
        super.onCreate()
        context = this@MyFirebaseMessagingService
        broadcaster = LocalBroadcastManager.getInstance(this)
    }

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        Log.d(TAG, "Refreshed token: $p0")
        AppPreference.saveStringPreference(applicationContext, BuildConfig.firebasePrefKey, p0)
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]
        //  (developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.from)

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            try {
                val data = remoteMessage.data
                val title = data["title"]
                val message = data["message"]
                channelName = data["channelName"]
                liveStreamId = data["liveStreamId"]
                missingChildId = data["MissingChildId"]
                Log.d("DataMsg", "handleDataMessage: $title  $message $channelName $liveStreamId $missingChildId")
                //sendNotification(title,message);
                Log.d(TAG, "Message data payload: $remoteMessage")
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        // Check if message contains a notification payload.
        if (remoteMessage.notification != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.notification?.body)
            val message = remoteMessage.notification?.body ?: ""
            val title = remoteMessage.notification?.title ?: ""
            if (isNotificationEnabled(context!!)) {
                sendNotification(title, message)

                Utils.notificationCount++
            }

        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }


    // [END receive_message]

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private fun sendNotification(contentTitle: String, messageBody: String) {
        Log.d(TAG, "sendNotification: $contentTitle$messageBody")
        val random: Random = Random()
        var intent = Intent()
//        lateinit var pendingIntent: PendingIntent
        val appDatabase: OldMe911Database = OldMe911Database.getDatabase(context!!)
        val login_data = appDatabase.loginDao().getAll()
        val dataChannel = channelName ?: ""
        val dataId = liveStreamId ?: ""
        if (login_data != null) {
            if (dataChannel.isNotEmpty()) {
                intent = Utils.moveToLiveStream(context!!, true, dataChannel, dataId, Constants.CLIENT_ROLE_AUDIENCE, LiveStreamResult())
                /*intent = Intent(context!!, LiveStreamActivity::class.java)
                intent.putExtra(com.keepSafe911.openlive.Constants.KEY_CLIENT_ROLE, Constants.CLIENT_ROLE_AUDIENCE)
                intent.putExtra(com.keepSafe911.openlive.Constants.KEY_CHANNEL_NAME, dataChannel)
                intent.putExtra(com.keepSafe911.openlive.Constants.KEY_CHANNEL_ID, dataId)
                intent.putExtra("liveStreamData", LiveStreamResult())
                intent.putExtra("isFromNotification", true)*/
            } else if (login_data.isAdmin) {
                if (messageBody.lowercase().contains("entered") || messageBody.lowercase().contains("exited")) {
                    intent = Intent(context!!, HomeActivity::class.java)
                    intent.putExtra("isFromNotification", true)
                }
            }else if (missingChildId!!.isNotEmpty()){
                intent = Intent(context!!, HomeActivity::class.java)
                intent.putExtra("isFromNotification", false)
                intent.putExtra(com.keepSafe911.openlive.Constants.MISSING_CHILD_ID, missingChildId)
            }
        } else {
            intent = Intent(context!!, MainActivity::class.java)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, random.nextInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        Log.d("CountForeground", "sendNotification: ")
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this)
            .setSmallIcon(Utils.getNotificationIcon())
            .setColor(Color.rgb(136, 26, 70))
            .setContentTitle(contentTitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = context!!.getString(R.string.default_notification_channel_id)
            val channel = NotificationChannel(channelId, contentTitle, NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = messageBody
            channel.enableLights(true)
            channel.lightColor = Color.rgb(136, 26, 70)
            channel.setShowBadge(true)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(channel)
            notificationBuilder.setChannelId(channelId)
        }
        notificationManager.notify(
            ++Utils.notificationCountID,
            notificationBuilder.build()
        )

        Log.d("CountBackGround", "sendNotification: ")
        // If the app is in background, firebase itself handles the notification
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        private const val CHECK_OP_NO_THROW = "checkOpNoThrow"
        private const val OP_POST_NOTIFICATION = "OP_POST_NOTIFICATION"

        private fun isNotificationEnabled(mContext: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val mAppOps = mContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val appInfo = mContext.applicationInfo
                val pkg = mContext.applicationContext.packageName
                val uid = appInfo.uid
                val appOpsClass: Class<*>
                try {
                    appOpsClass = Class.forName(AppOpsManager::class.java.name)
                    val checkOpNoThrowMethod =
                        appOpsClass.getMethod(CHECK_OP_NO_THROW, Integer.TYPE, Integer.TYPE, String::class.java)

                    val opPostNotificationValue = appOpsClass.getDeclaredField(OP_POST_NOTIFICATION)
                    val value = opPostNotificationValue.get(Int::class.java) as Int
                    return checkOpNoThrowMethod.invoke(
                        mAppOps, value, uid,
                        pkg
                    ) as Int == AppOpsManager.MODE_ALLOWED
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                return false
            } else {
                return false
            }
        }
    }
}